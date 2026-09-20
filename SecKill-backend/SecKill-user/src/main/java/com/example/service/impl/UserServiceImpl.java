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
import com.example.result.ErrorCode;
import com.example.security.LoginFailLimiter;
import com.example.security.TokenBlacklist;
import com.example.service.UserService;
import com.example.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,Users> implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final LoginFailLimiter limiter;

    public UserServiceImpl(PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           TokenBlacklist tokenBlacklist,
                           LoginFailLimiter limiter) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.limiter = limiter;
    }

    @Override
    public void register(RegisterDTO dto){
        Users users= UserConverter.INSTANCE.toEntity(dto);
        users.setPassword(passwordEncoder.encode(dto.getPassword()));
        users.setStatus(UserConstants.STATUS_ENABLED);
        users.setRole(UserConstants.ROLE_USER);
        try{
            this.save(users);
        }catch (DuplicateKeyException e){
            throw new UserException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    @Override
    public TokenVO login(LoginDTO dto){
        limiter.checkNotLocked(dto.getPhone());
        Users dbUser=this.lambdaQuery()
                .eq(Users::getPhone,dto.getPhone())
                .one();
        if(dbUser==null || !passwordEncoder.matches(dto.getPassword(),dbUser.getPassword())){
            limiter.onFail(dto.getPhone());
            throw new UserException(ErrorCode.LOGIN_FAILED);
        }
        if(dbUser.getStatus()!= UserConstants.STATUS_ENABLED){
            throw new UserException(ErrorCode.ACCOUNT_DISABLED);
        }
        TokenVO tokenVO=new TokenVO();
        tokenVO.setToken(jwtUtil.generateToken(dbUser.getId(),dbUser.getUsername(),String.valueOf(dbUser.getRole()),String.valueOf(dbUser.getStatus())));
        limiter.onSuccess(dto.getPhone());
        return tokenVO;
    }

    @Override
    public UserInfoVO getUserInfo(Long userId){
        Users user=getById(userId);
        if(user==null){
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        return UserConverter.INSTANCE.toVO(user);
    }

    @Override
    public void logout(String token){
        Claims claims=jwtUtil.parseToken(token);
        long remaining =claims.getExpiration().getTime() - System.currentTimeMillis();
        if(remaining>0){
            tokenBlacklist.blacklist(claims.getId(),remaining);
        }
    }
}
