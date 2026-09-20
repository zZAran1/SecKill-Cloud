package com.example.service.impl;

import com.example.security.JwtTokenParser;
import com.example.security.LoginUser;
import com.example.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenParserImpl implements JwtTokenParser {
    private final JwtUtil jwtUtil;
    public JwtTokenParserImpl(JwtUtil jwtUtil) {
        this.jwtUtil=jwtUtil;
    }

    @Override
    public LoginUser parse(String token) {
        Claims claims=jwtUtil.parseToken(token);
        LoginUser loginUser=new LoginUser();
        loginUser.setUsername(claims.get("username",String.class));
        loginUser.setRole(claims.get("role",String.class));
        loginUser.setStatus(claims.get("status",String.class));
        loginUser.setJti(claims.getId());
        loginUser.setId(Long.valueOf(claims.getSubject()));
        return loginUser;
    }
}
