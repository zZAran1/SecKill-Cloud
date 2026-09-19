package com.example.handler;

import com.example.exception.BaseBusinessException;
import com.example.exception.CommonException;
import com.example.exception.UserException;
import com.example.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log= LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(BaseBusinessException.class)
    public Result<Void> handleBaseBusiness(BaseBusinessException e){
        return Result.error(e.getCode(),e.getMessage());
    }
    @ExceptionHandler(CommonException.class)
    public Result<Void> handleCommonException(CommonException e){
        return Result.error(e.getCode(),e.getMessage());
    }
    @ExceptionHandler(UserException.class)
    public Result<Void> handleUserException(UserException e){
        return Result.error(e.getCode(),e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e){
        log.error("未捕获异常",e);
        return Result.error("系统繁忙，请稍后再试");
    }
}
