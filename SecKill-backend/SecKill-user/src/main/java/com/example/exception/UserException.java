package com.example.exception;

public class UserException extends BaseBusinessException {
    public UserException(String msg) {
        super(1000,msg);
    }
}
