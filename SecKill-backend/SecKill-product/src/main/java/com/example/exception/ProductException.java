package com.example.exception;

import com.example.result.ErrorCode;

public class ProductException extends BaseBusinessException{
    public ProductException(ErrorCode errorCode){
        super(errorCode);
    }
}
