package com.example.exception;

import com.example.result.ErrorCode;

public class UserException extends BaseBusinessException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
