package com.example.controller;

import com.example.model.dto.LoginDTO;
import com.example.model.dto.RegisterDTO;
import com.example.model.vo.TokenVO;
import com.example.model.vo.UserInfoVO;
import com.example.result.Result;
import com.example.security.UserContext;
import com.example.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public Result<Void> register(@RequestBody @Valid RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<TokenVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success("登录成功",userService.login(dto));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(userService.getUserInfo(UserContext.getUserId()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success("退出登录成功");
    }
}
