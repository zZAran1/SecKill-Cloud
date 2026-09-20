package com.example.security;

public interface TokenBlacklist {
    void blacklist(String jti,long ttlMillis);
    boolean isBlacklisted(String jti);
}
