package com.example.model;

import com.example.exception.BaseBusinessException;
import com.example.result.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStateMachineTest {

    private void assertReject(int from, int to) {
        BaseBusinessException e = assertThrows(BaseBusinessException.class,
                () -> OrderStateMachine.verify(from, to),
                "不应放行流转：" + OrderStatus.desc(from) + " → " + OrderStatus.desc(to));
        assertEquals(ErrorCode.ORDER_STATUS_ERROR, e.getErrorCode());
    }

    @Test
    void 合法流转_全部放行() {
        // 主链路
        assertDoesNotThrow(() -> OrderStateMachine.verify(OrderStatus.UNPAID, OrderStatus.PAID));
        assertDoesNotThrow(() -> OrderStateMachine.verify(OrderStatus.PAID, OrderStatus.COMPLETED));
        // 关单链路
        assertDoesNotThrow(() -> OrderStateMachine.verify(OrderStatus.UNPAID, OrderStatus.CANCELLED));
        // 售后链路
        assertDoesNotThrow(() -> OrderStateMachine.verify(OrderStatus.PAID, OrderStatus.REFUNDING));
        assertDoesNotThrow(() -> OrderStateMachine.verify(OrderStatus.COMPLETED, OrderStatus.REFUNDING));
        assertDoesNotThrow(() -> OrderStateMachine.verify(OrderStatus.REFUNDING, OrderStatus.REFUNDED));
        // 退款驳回/撤回
        assertDoesNotThrow(() -> OrderStateMachine.verify(OrderStatus.REFUNDING, OrderStatus.PAID));
    }

    @Test
    void 终态不允许再流转() {
        assertReject(OrderStatus.CANCELLED, OrderStatus.PAID);
        assertReject(OrderStatus.CANCELLED, OrderStatus.UNPAID);
        assertReject(OrderStatus.REFUNDED, OrderStatus.PAID);
        assertReject(OrderStatus.REFUNDED, OrderStatus.REFUNDING);
    }

    @Test
    void 非法跳转被拒绝() {
        // 未支付不能直接完成/退款
        assertReject(OrderStatus.UNPAID, OrderStatus.COMPLETED);
        assertReject(OrderStatus.UNPAID, OrderStatus.REFUNDED);
        // 已支付不能走取消（必须退款），完成后不能回已支付
        assertReject(OrderStatus.PAID, OrderStatus.CANCELLED);
        assertReject(OrderStatus.COMPLETED, OrderStatus.PAID);
        // 退款中不能确认完成
        assertReject(OrderStatus.REFUNDING, OrderStatus.COMPLETED);
    }

    @Test
    void 未知状态抛异常() {
        assertReject(99, OrderStatus.PAID);
    }
}
