package com.example.seckillcommon.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class BCryptPasswordUtil {
    //哈希密码
    public  String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    //验证密码
    public  boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
