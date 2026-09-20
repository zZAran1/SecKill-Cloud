package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.constant.UserConstants;
import com.example.converter.UserConverter;
import com.example.exception.UserException;
import com.example.mapper.UserMapper;
import com.example.model.dto.LoginDTO;
import com.example.model.dto.RegisterDTO;
import com.example.model.entity.Users;
import com.example.model.vo.TokenVO;
import com.example.model.vo.UserInfoVO;
import com.example.service.UserService;
import com.example.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,Users> implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void register(RegisterDTO dto){
        if(count(new LambdaQueryWrapper<Users>().eq(Users::getUsername, dto.getUsername()))>0){
            throw new UserException("该用户名已被使用");
        }
        if(count(new LambdaQueryWrapper<Users>().eq(Users::getPhone,dto.getPhone()))>0){
            throw new UserException("该手机号已被注册");
        }
        Users users= UserConverter.INSTANCE.toEntity(dto);
        users.setPassword(passwordEncoder.encode(dto.getPassword()));
        users.setStatus(UserConstants.STATUS_ENABLED);
        users.setRole(UserConstants.ROLE_USER);
        this.save(users);
    }

    @Override
    public TokenVO login(LoginDTO dto){
        Users db_user=this.lambdaQuery()
                .eq(Users::getPhone,dto.getPhone())
                .one();
        if(db_user==null || !passwordEncoder.matches(dto.getPassword(),db_user.getPassword())){
            throw new UserException("手机号或密码错误");
        }
        if(db_user.getStatus()!= UserConstants.STATUS_ENABLED){
            throw new UserException("该账号已被封禁");
        }
        TokenVO tokenVO=new TokenVO();
        tokenVO.setToken(jwtUtil.generateToken(db_user.getId(),db_user.getUsername(),String.valueOf(db_user.getRole()),String.valueOf(db_user.getStatus())));
        return tokenVO;
    }

    @Override
    public UserInfoVO getUserInfo(Long userId){
        Users user=getById(userId);
        if(user==null){
            throw new UserException("用户不存在");
        }
        return UserConverter.INSTANCE.toVO(user);
    }

}
