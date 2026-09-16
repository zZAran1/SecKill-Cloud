package com.example.seckillcommon.util;

import com.example.seckillcommon.exception.UserException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String base64Secret;
    @Value("${jwt.expiration}")
    private long expirationTime;
    private SecretKey secretKey;
    private SecretKey getSigningKey() {
        if (secretKey == null) {
            byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return secretKey;
    }

    public String generateToken(String userId,String username,String roleId,String statusId) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("username",username);
        claims.put("roleId",roleId);
        claims.put("statusId",statusId);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try{
            parseToken(token);
            return true;
        }catch (Exception e) {
            return false;
        }
    }

    //获取userId
    public String getUserId(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (ExpiredJwtException e) {
            throw new UserException("登录已过期");
        } catch (Exception e) {
            throw new UserException("登录失效");
        }
    }
    public String getRole(String token) {
        try {
            return parseToken(token).get("role", String.class);
        } catch (ExpiredJwtException e) {
            throw new UserException("登录已过期");
        } catch (Exception e) {
            throw new UserException("登录失效");
        }
    }
    public String getStatus(String token) {
        try{
            return parseToken(token).get("status", String.class);
        }catch (ExpiredJwtException e) {
            throw new UserException("登录已过期");
        }catch (Exception e) {
            throw new UserException("登录失效");
        }
    }
}
