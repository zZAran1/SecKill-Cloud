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
    USER_PASSWORD_WRONG( 1006 , "原密码错误" ),
    PRODUCT_NOT_FOUND(2001,"商品不存在"),
    PRODUCT_STOCK_INSUFFICIENT(2002, "库存不足"),
    ORDER_CREATE_FAILED(3001,"下单失败，请稍后再试"),
    ORDER_NOT_FOUND(3002,"订单不存在"),
    ORDER_CLOSED( 3003 , "订单已关闭或已取消" ),
    ORDER_STATUS_ERROR( 3004 , "当前订单状态不允许该操作" ),
    PAYMENT_NOT_FOUND( 5001 , "支付单不存在" ),
    PAYMENT_SIGN_ERROR( 5002 , "支付回调验签失败" ),
    PAYMENT_STATUS_ERROR( 5003 , "支付单状态异常，请勿重复操作" ),
    REFUND_NOT_FOUND( 6001 , "退款单不存在" ),
    REFUND_STATUS_ERROR( 6002 , "当前退款单状态不允许该操作" ),
    REFUND_EXISTS( 6003 , "该订单已有进行中的退款申请" ),
    SECKILL_ACTIVITY_NOT_FOUND(4001,"秒杀活动不存在"),
    SECKILL_NOT_STARTED(4002,"秒杀未开始"),
    SECKILL_ENDED(4003,"秒杀已结束"),
    SECKILL_SOLD_OUT(4004,"手慢了，库存已抢完"),
    SECKILL_REPEAT(4005,"每人限购一次，请勿重复参与");

    private final int code;
    private final String message;
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
