package com.example.security;

import com.example.exception.UserException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class UserContext {
    private UserContext(){
    }

    public static LoginUser get(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication ==null || !(authentication.getPrincipal() instanceof LoginUser loginUser)){
            throw new UserException("未登录或登录已过期");
        }
        return loginUser;
    }

    public static Long getUserId(){
        return get().getId();
    }

    public static String getRole(){
        return get().getRole();
    }

    public static String getStatus(){
        return get().getStatus();
    }
}
