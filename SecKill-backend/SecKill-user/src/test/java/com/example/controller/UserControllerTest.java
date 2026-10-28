package com.example.controller;

import com.example.model.dto.LoginDTO;
import com.example.model.dto.RegisterDTO;
import com.example.model.vo.TokenVO;
import com.example.model.vo.UserInfoVO;
import com.example.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

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
    void 注册_参数合法_返回成功并调用服务层() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegisterDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(userService).register(any(RegisterDTO.class));
    }

    @Test
    void 注册_密码不含数字_被校验拦截返回400() throws Exception {
        RegisterDTO dto = buildRegisterDTO();
        dto.setPassword("abcdefgh");   // 纯字母，违反 @Pattern

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())            // 全局异常处理器返回Result，HTTP永远是200
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 登录成功_返回token() throws Exception {
        TokenVO vo = new TokenVO();
        vo.setToken("TOKEN");
        when(userService.login(any())).thenReturn(vo);

        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800000000");
        dto.setPassword("Abc12345");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("TOKEN"));
    }

    @Test
    void 获取用户信息_已登录_返回数据() throws Exception {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(1L);
        vo.setUsername("tester");
        vo.setEmail("a@b.com");
        when(userService.getUserInfo(1L)).thenReturn(vo);

        // 身份来自网关注入的 X-User-Id 请求头（UserContext 读请求头，不读 SecurityContext）
        mockMvc.perform(get("/users/me").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("tester"))
                .andExpect(jsonPath("$.data.email").value("a@b.com"));
    }

    @Test
    void 获取用户信息_未登录_返回401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void 访问不存在的接口_返回404() throws Exception {
        mockMvc.perform(get("/users/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
