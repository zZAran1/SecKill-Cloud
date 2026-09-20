package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.dto.LoginDTO;
import com.example.model.dto.RegisterDTO;
import com.example.model.entity.Users;
import com.example.model.vo.TokenVO;
import com.example.model.vo.UserInfoVO;

public interface UserService extends IService<Users> {
    void register(RegisterDTO dto);
    TokenVO login(LoginDTO dto);
    UserInfoVO getUserInfo(Long userId);
    void logout(String token);
}
