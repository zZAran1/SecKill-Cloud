package com.example.exception;

import com.example.result.ErrorCode;

public class ProductException extends BaseBusinessException {
    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ProductException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
