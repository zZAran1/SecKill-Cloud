package com.example.result;

import lombok.Getter;

@Getter
public enum ErrorCode {
    
    SUCCESS(200,"成功"),
    BAD_REQUEST(400,"请求参数错误"),
    UNAUTHORIZED(401,"未登录或登录已过期"),
    FORBIDDEN( 403 , "没有访问权限" ),
    CONFLICT( 409 , "数据已存在，请勿重复提交" ),
    SYSTEM_ERROR( 500 , "系统繁忙，请稍后再试" ),
    USER_NOT_FOUND(1001,"用户不存在"),
    USER_ALREADY_EXISTS( 1002 , "用户名、邮箱或手机号已被注册" ),
    LOGIN_FAILED( 1003 , "手机号或密码错误" ),
    ACCOUNT_DISABLED( 1004 , "该账号已被封禁" ),
    NOT_FOUND(404,"请求接口不存在"),
    LOGIN_LOCKED(1005,"失败次数过多，账号已锁定，请10分钟后再试"),
    PRODUCT_NOT_FOUND(2001,"商品不存在"),
    ORDER_CREATE_FAILED(3001,"下单失败，请稍后再试");

    private final int code;
    private final String message;
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
