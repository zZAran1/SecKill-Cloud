package com.example.model;

public final class OrderStatus {

    //待支付
    public static final int UNPAID =0;

    //已支付
    public static final int PAID =1;

    //已取消（超时关闭/用户取消）
    public static final int CANCELLED =2;

    //已完成（确认收货/交易结束）
    public static final int COMPLETED =3;

    //退款中（已支付订单发起退款，等待审核/到账）
    public static final int REFUNDING =4;

    //已退款
    public static final int REFUNDED =5;

    /**状态描述，返回给前端展示用*/
    public static String desc(int status) {
        switch (status) {
            case UNPAID: return "待支付";
            case PAID: return "已支付";
            case CANCELLED: return "已取消";
            case COMPLETED: return "已完成";
            case REFUNDING: return "退款中";
            case REFUNDED: return "已退款";
            default: return "未知状态";
        }
    }

    private OrderStatus(){}
}
