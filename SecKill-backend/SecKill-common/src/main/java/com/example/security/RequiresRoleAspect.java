package com.example.security;

import com.example.annotation.RequiresRole;
import com.example.exception.BaseBusinessException;
import com.example.result.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

@Aspect
@Component
public class RequiresRoleAspect {

    @Before("@annotation(requiresRole)")
    public void checkRole(RequiresRole requiresRole) {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BaseBusinessException(ErrorCode.UNAUTHORIZED);
        }

        HttpServletRequest request = attrs.getRequest();
        String roleHeader = request.getHeader("X-Role");
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new BaseBusinessException(ErrorCode.UNAUTHORIZED);
        }

        Set<String> allowed = Set.of(requiresRole.value());
        if (!allowed.contains(roleHeader)) {
            throw new BaseBusinessException(ErrorCode.FORBIDDEN);
        }
    }
}