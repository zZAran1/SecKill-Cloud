package com.example.exception;

import com.example.result.ErrorCode;
import lombok.Getter;

@Getter
public class BaseBusinessException extends RuntimeException{
    private final ErrorCode errorCode;

    public BaseBusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    public BaseBusinessException (ErrorCode errorCode, String message) {
        super (message);
        this .errorCode = errorCode;
    }
}
