package com.example.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long expirationTime;
    public JwtUtil(@Value("${jwt.secret}") String base64Secret,@Value("${jwt.expiration}") long expirationTime){
        this.secretKey= Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
        this.expirationTime=expirationTime;
    }

    public String generateToken (Long userId, String username, String role, String status) { return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim( "username" , username)
            .claim( "role" , role)
            .claim( "status" , status)
            .issuedAt( new Date())
            .expiration( new Date (System.currentTimeMillis() + expirationTime))
            .signWith(secretKey)
            .compact();
    }

    public Claims parseToken (String token) { return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
