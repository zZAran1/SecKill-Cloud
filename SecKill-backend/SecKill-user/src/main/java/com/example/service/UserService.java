package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.dto.ChangePasswordDTO;
import com.example.model.dto.LoginDTO;
import com.example.model.dto.RegisterDTO;
import com.example.model.dto.UpdateProfileDTO;
import com.example.model.entity.Users;
import com.example.model.vo.TokenVO;
import com.example.model.vo.UserInfoVO;
import com.example.result.PageResult;

public interface UserService extends IService<Users> {
    void register(RegisterDTO dto);
    TokenVO login(LoginDTO dto);

    /**用 refresh token 换新 token 对（旧 refresh 一次性使用，重放拒绝）*/
    TokenVO refresh(String refreshToken);

    UserInfoVO getUserInfo(Long userId);

    /**退出登录：拉黑当前 access token（网关处理）并吊销 refresh token*/
    void logout();

    /**修改个人资料（只更新传入的字段）*/
    UserInfoVO updateProfile(Long userId, UpdateProfileDTO dto);

    /**修改密码（需校验原密码）*/
    void changePassword(Long userId, ChangePasswordDTO dto);

    /**管理员：分页查询用户*/
    PageResult<UserInfoVO> pageUsers(String keyword, long page, long size);

    /**管理员：封禁/解封账号，状态与网关黑名单联动*/
    void updateUserStatus(Long userId, Integer status);
}
