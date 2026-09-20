package com.example.handler;

import com.example.exception.BaseBusinessException;
import com.example.exception.CommonException;
import com.example.exception.UserException;
import com.example.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.print.attribute.ResolutionSyntax;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        String msg=e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("参数校验失败");
        return Result.error(400,msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e){
        log.warn("请求体解析失败：{}",e.getMessage());
        return Result.error(400,"请求体格式错误，请检查JSON格式");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e){
        log.warn("唯一约束冲突：{}",e.getMessage());
        return Result.error(409,"数据已存在，请勿重复提交");
    }
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e){
        log.error("未捕获异常",e);
        return Result.error("系统繁忙，请稍后再试");
    }
}
