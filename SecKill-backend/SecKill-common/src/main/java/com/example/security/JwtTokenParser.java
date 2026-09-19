package com.example.security;



public interface JwtTokenParser {
     LoginUser parse(String token);
}
