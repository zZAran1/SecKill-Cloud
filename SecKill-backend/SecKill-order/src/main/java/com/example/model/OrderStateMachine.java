package com.example.model;

import com.example.exception.BaseBusinessException;
import com.example.result.ErrorCode;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态机：集中定义合法状态流转，所有订单状态变更必须先经过 {@link #verify} 校验，
 * 避免状态流转规则散落在各业务代码里导致非法跳转（如已取消订单被改成已支付）。
 *
 * 流转图：
 *   待支付 ──支付成功──▶ 已支付 ──确认完成──▶ 已完成
 *     │                   │  ▲                  │
 *     │取消/超时关闭       │  └─退款驳回/撤回─────┤
 *     ▼                   └─申请退款─┐           │申请退款（售后）
 *   已取消                            ▼           ▼
 *                                 退款中 ──退款成功──▶ 已退款
 */
public final class OrderStateMachine {

    /**key = 当前状态，value = 允许流转到的状态集合*/
    private static final Map<Integer, Set<Integer>> TRANSITIONS = Map.of(
            OrderStatus.UNPAID, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.COMPLETED, OrderStatus.REFUNDING),
            OrderStatus.REFUNDING, Set.of(OrderStatus.REFUNDED, OrderStatus.PAID),
            //已完成仍可发起售后；已取消/已退款是终态，不允许再流转
            OrderStatus.COMPLETED, Set.of(OrderStatus.REFUNDING),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.REFUNDED, Set.of());

    /**
     * 校验状态流转是否合法，非法流转抛业务异常。
     *
     * @param from 当前状态（调用方应传入数据库查出的真实状态）
     * @param to   目标状态
     */
    public static void verify(int from, int to) {
        Set<Integer> allowed = TRANSITIONS.get(from);
        if (allowed == null) {
            throw new BaseBusinessException(ErrorCode.ORDER_STATUS_ERROR,
                    "订单状态非法：" + OrderStatus.desc(from));
        }
        if (!allowed.contains(to)) {
            throw new BaseBusinessException(ErrorCode.ORDER_STATUS_ERROR,
                    "当前订单状态为「" + OrderStatus.desc(from) + "」，不允许变更为「" + OrderStatus.desc(to) + "」");
        }
    }

    private OrderStateMachine() {
    }
}
