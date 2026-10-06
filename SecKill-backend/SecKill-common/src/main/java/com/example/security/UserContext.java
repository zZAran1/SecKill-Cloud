package com.example.security;

import com.example.exception.BaseBusinessException;
import com.example.result.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class UserContext {
    private UserContext() {}

    public static LoginUser get(){
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String userId = attrs==null?null:attrs.getRequest().getHeader("X-User-Id");
        if(userId==null){
            throw new BaseBusinessException(ErrorCode.UNAUTHORIZED);
        }
        HttpServletRequest request=attrs.getRequest();
        LoginUser user=new LoginUser();
        user.setId(Long.valueOf(userId));
        user.setUsername(request.getHeader("X-Username"));
        user.setRole(request.getHeader("X-Role"));
        user.setStatus(request.getHeader("X-Status"));
        return user;
    }

    public static Long getUserId(){return get().getId();}
    public static String getRole(){return get().getRole();}
    public static String getStatus(){return get().getStatus();}
}
