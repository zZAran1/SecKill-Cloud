package com.example.exception;

import lombok.Getter;

@Getter
public class BaseBusinessException extends RuntimeException{
    private final int code;
    public BaseBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
