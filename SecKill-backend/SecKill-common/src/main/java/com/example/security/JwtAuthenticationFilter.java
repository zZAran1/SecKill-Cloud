package com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenParser jwtTokenParser;
    private final TokenBlacklist tokenBlacklist;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    public JwtAuthenticationFilter(JwtTokenParser jwtTokenParser,TokenBlacklist tokenBlacklist) {
        this.jwtTokenParser = jwtTokenParser;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头拿 Token
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try{
                LoginUser user=jwtTokenParser.parse(token);
                if(tokenBlacklist.isBlacklisted(user.getJti())){
                    SecurityContextHolder.clearContext();
                }else{
                    UsernamePasswordAuthenticationToken authentication =new UsernamePasswordAuthenticationToken(user,null,List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }catch (Exception e){
                log.debug("Token 解析失败：{}",e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 4. 放行
        filterChain.doFilter(request, response);

    }
}
