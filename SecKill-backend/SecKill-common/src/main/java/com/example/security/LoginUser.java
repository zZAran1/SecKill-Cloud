package com.example.security;

import lombok.Data;

@Data
public class LoginUser {
    private Long id;
    private String username;
    private String role;
    private String status;
    private String jti;
}
