package com.example.model.vo;

import lombok.Data;

@Data
public class TokenVO {
    /**访问令牌（短时效，接口调用用）*/
    private String token;
    /**刷新令牌（长时效，仅用于换新 token，一次性使用）*/
    private String refreshToken;
    /**access token 剩余有效秒数，前端据此提前续期*/
    private Long expiresIn;
}
