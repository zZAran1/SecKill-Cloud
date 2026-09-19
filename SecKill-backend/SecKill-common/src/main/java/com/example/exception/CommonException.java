package com.example.exception;

public class CommonException extends BaseBusinessException{
    public CommonException(String msg) {
        super(1001,msg);
    }
}
