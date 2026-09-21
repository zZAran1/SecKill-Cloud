package com.example.service.impl;

import com.example.exception.UserException;
import com.example.mapper.UserMapper;
import com.example.model.dto.LoginDTO;
import com.example.model.dto.RegisterDTO;
import com.example.model.entity.Users;
import com.example.model.vo.TokenVO;
import com.example.result.ErrorCode;
import com.example.security.LoginFailLimiter;
import com.example.security.TokenBlacklist;
import com.example.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtUtil jwtUtil;
    @Mock
    TokenBlacklist tokenBlacklist;
    @Mock
    LoginFailLimiter loginFailLimiter;
    @Mock
    UserMapper userMapper;

    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(passwordEncoder, jwtUtil, tokenBlacklist, loginFailLimiter);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    private RegisterDTO buildRegisterDTO() {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("a@b.com");
        dto.setUsername("tester");
        dto.setPassword("Abc12345");
        dto.setPhone("13800000000");
        dto.setAge(20);
        dto.setSex("M");
        return dto;
    }

    @Test
    void 注册成功_密码加密后入库() {
        RegisterDTO dto = buildRegisterDTO();
        when(passwordEncoder.encode("Abc12345")).thenReturn("ENCODED");

        userService.register(dto);

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("ENCODED", captor.getValue().getPassword());
    }

    @Test
    void 注册_唯一索引冲突_抛USER_ALREADY_EXISTS() {
        RegisterDTO dto = buildRegisterDTO();
        when(passwordEncoder.encode(any())).thenReturn("ENCODED");
        when(userMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_email"));

        UserException e = assertThrows(UserException.class, () -> userService.register(dto));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, e.getErrorCode());
    }

    @Test
    void 登录_密码错误_计数并抛异常() {
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800000000");
        dto.setPassword("wrong");

        Users db = new Users();
        db.setPassword("ENCODED");
        db.setStatus(1);
        db.setRole(2);
        when(userMapper.selectOne(any())).thenReturn(db);
        when(passwordEncoder.matches("wrong", "ENCODED")).thenReturn(false);

        UserException e = assertThrows(UserException.class, () -> userService.login(dto));
        assertEquals(ErrorCode.LOGIN_FAILED, e.getErrorCode());
        verify(loginFailLimiter).onFail("13800000000");
    }

    @Test
    void 登录成功_清零计数并返回token() {
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800000000");
        dto.setPassword("Abc12345");

        Users db = new Users();
        db.setId(1L);
        db.setUsername("tester");
        db.setPassword("ENCODED");
        db.setStatus(1);
        db.setRole(2);
        when(userMapper.selectOne(any())).thenReturn(db);
        when(passwordEncoder.matches("Abc12345", "ENCODED")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any(), any(), any())).thenReturn("TOKEN");

        TokenVO vo = userService.login(dto);

        assertEquals("TOKEN", vo.getToken());
        verify(loginFailLimiter).onSuccess("13800000000");
    }
}
