package com.example.model;

public final class RefundStatus {

    //待审核（用户已提交申请，等待管理员审核）
    public static final int PENDING = 0;

    //退款成功（审核通过且渠道退款完成）
    public static final int SUCCESS = 1;

    //已拒绝（管理员驳回，订单恢复已支付）
    public static final int REJECTED = 2;

    //已取消（用户主动撤回申请，订单恢复已支付）
    public static final int CANCELLED = 3;

    /**状态描述，返回给前端展示用*/
    public static String desc(int status) {
        switch (status) {
            case PENDING: return "待审核";
            case SUCCESS: return "退款成功";
            case REJECTED: return "已拒绝";
            case CANCELLED: return "已取消";
            default: return "未知状态";
        }
    }

    private RefundStatus() {
    }
}
