package com.example.seckillcommon.exception;

public class UserException extends BaseBusinessException {
    public UserException(String msg) {
        super(1000,msg);
    }
}
