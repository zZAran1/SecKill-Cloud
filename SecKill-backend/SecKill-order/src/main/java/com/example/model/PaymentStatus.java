package com.example.model;

public final class PaymentStatus {

    //待支付
    public static final int UNPAID = 0;

    //支付成功
    public static final int SUCCESS = 1;

    //支付失败（渠道回调失败/订单已关闭等）
    public static final int FAILED = 2;

    //已关闭（支付单作废）
    public static final int CLOSED = 3;

    //已退款
    public static final int REFUNDED = 4;

    /**状态描述，返回给前端展示用*/
    public static String desc(int status) {
        switch (status) {
            case UNPAID: return "待支付";
            case SUCCESS: return "支付成功";
            case FAILED: return "支付失败";
            case CLOSED: return "已关闭";
            case REFUNDED: return "已退款";
            default: return "未知状态";
        }
    }

    private PaymentStatus() {
    }
}
