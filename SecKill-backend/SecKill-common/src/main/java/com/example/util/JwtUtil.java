package com.example.util;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long expirationTime;
    public JwtUtil(@Value("${jwt.private-key:}") String privateBase64Secret,
                   @Value("${jwt.public-key:}") String publicBase64Secret,
                   @Value("${jwt.expiration}") long expirationTime){
        this.publicKey = parsePublicKey(publicBase64Secret);
        this.privateKey = privateBase64Secret.isEmpty()?null : parsePrivateKey(privateBase64Secret);
        this.expirationTime=expirationTime;
    }

    public String generateToken (Long userId, String username, String role, String status) {
        if(privateKey == null){
            throw new IllegalStateException("本服务未配置私钥，不能签发令牌");
        }
        return Jwts.builder()
            .id(java.util.UUID.randomUUID().toString())
            .subject(String.valueOf(userId))
            .claim( "username" , username)
            .claim( "role" , role)
            .claim( "status" , status)
            .issuedAt( new Date())
            .expiration( new Date (System.currentTimeMillis() + expirationTime))
            .signWith(privateKey)
            .compact();
    }

    public Claims parseToken (String token) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private PublicKey parsePublicKey (String publicBase64Secret) {
        try{
            byte[] decoded = Base64.getDecoder().decode(publicBase64Secret);
            return  KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        }catch (Exception e){
            throw new IllegalStateException("公钥解析失败，请检查jwt.public-key配置",e);
        }
    }

    private PrivateKey parsePrivateKey (String privateBase64Secret) {
        try{
            byte[] decoded = Base64.getDecoder().decode(privateBase64Secret);
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        }catch (Exception e){
            throw new IllegalStateException("私钥解析失败，请检查jwt.private-key配置",e);
        }
    }
}
