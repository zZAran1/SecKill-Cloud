package com.example.controller;

import com.example.annotation.RequiresRole;
import com.example.model.dto.ChangePasswordDTO;
import com.example.model.dto.LoginDTO;
import com.example.model.dto.RegisterDTO;
import com.example.model.dto.UpdateProfileDTO;
import com.example.model.dto.UpdateUserStatusDTO;
import com.example.model.vo.TokenVO;
import com.example.model.vo.UserInfoVO;
import com.example.result.PageResult;
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

    /**修改个人资料*/
    @PutMapping("/me")
    public Result<UserInfoVO> updateProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        return Result.success("资料修改成功", userService.updateProfile(UserContext.getUserId(), dto));
    }

    /**修改密码*/
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        userService.changePassword(UserContext.getUserId(), dto);
        return Result.success("密码修改成功");
    }

    /**管理员：分页查询用户*/
    @GetMapping("/page")
    @RequiresRole({"0","1"})
    public Result<PageResult<UserInfoVO>> pageUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.pageUsers(keyword, page, size));
    }

    /**管理员：封禁/解封账号*/
    @PutMapping("/{id}/status")
    @RequiresRole({"0","1"})
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestBody @Valid UpdateUserStatusDTO dto) {
        userService.updateUserStatus(id, dto.getStatus());
        return Result.success("账号状态已更新");
    }
}
