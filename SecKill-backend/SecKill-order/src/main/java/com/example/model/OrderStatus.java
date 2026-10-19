package com.example.model;

public final class OrderStatus {

    //待支付
    public static final int UNPAID =0;

    //已支付
    public static final int PAID =1;

    //已取消（超时关闭/用户取消）
    public static final int CANCELLED =2;

    /**状态描述，返回给前端展示用*/
    public static String desc(int status) {
        switch (status) {
            case UNPAID: return "待支付";
            case PAID: return "已支付";
            case CANCELLED: return "已取消";
            default: return "未知状态";
        }
    }

    private OrderStatus(){}
}
