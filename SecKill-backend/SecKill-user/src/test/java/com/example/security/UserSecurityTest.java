package com.example.security;

import com.example.config.SecurityConfig;
import com.example.controller.UserController;
import com.example.model.vo.TokenVO;
import com.example.model.vo.UserInfoVO;
import com.example.result.ErrorCode;
import com.example.service.UserService;
import com.example.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenParser jwtTokenParser;

    @MockBean
    private TokenBlacklist tokenBlacklist;

    @MockBean
    private JwtUtil jwtUtil;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 无token访问受保护接口_返回401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void 有效token_正常访问() throws Exception {
        LoginUser loginUser = buildLoginUser("jti-valid");
        when(jwtTokenParser.parse("valid-token")).thenReturn(loginUser);
        when(tokenBlacklist.isBlacklisted("jti-valid")).thenReturn(false);
        when(userService.getUserInfo(1L)).thenReturn(new UserInfoVO());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void 黑名单token_返回401() throws Exception {
        LoginUser loginUser = buildLoginUser("jti-logout");
        when(jwtTokenParser.parse("logout-token")).thenReturn(loginUser);
        when(tokenBlacklist.isBlacklisted("jti-logout")).thenReturn(true);

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer logout-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 登录接口无需token() throws Exception {
        TokenVO vo = new TokenVO();
        vo.setToken("TOKEN");
        when(userService.login(any())).thenReturn(vo);

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000000\",\"password\":\"Abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("TOKEN"));
    }

    private LoginUser buildLoginUser(String jti) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(1L);
        loginUser.setUsername("tester");
        loginUser.setRole("2");
        loginUser.setStatus("1");
        loginUser.setJti(jti);
        return loginUser;
    }
}
