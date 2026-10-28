package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.converter.RefundConverter;
import com.example.exception.BaseBusinessException;
import com.example.mapper.RefundMapper;
import com.example.model.RefundStatus;
import com.example.model.dto.CreateRefundDTO;
import com.example.model.entity.Refund;
import com.example.model.vo.OrderVO;
import com.example.model.vo.RefundVO;
import com.example.result.ErrorCode;
import com.example.result.PageResult;
import com.example.security.UserContext;
import com.example.service.OrderService;
import com.example.service.PaymentService;
import com.example.service.RefundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class RefundServiceImpl extends ServiceImpl<RefundMapper, Refund> implements RefundService {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public RefundServiceImpl(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO apply(CreateRefundDTO dto) {
        // 1. 订单必须存在且归属当前用户（getOrder 内部已完成归属/管理员校验）
        OrderVO order = orderService.getOrder(dto.getOrderNo());

        // 2. 同一订单不允许有进行中的退款申请
        long pending = this.count(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderNo, dto.getOrderNo())
                .eq(Refund::getStatus, RefundStatus.PENDING));
        if (pending > 0) {
            throw new BaseBusinessException(ErrorCode.REFUND_EXISTS);
        }

        // 3. 订单状态机流转：已支付/已完成→退款中（条件更新，防并发重复申请）
        orderService.toRefunding(dto.getOrderNo());

        // 4. 落退款单，金额为订单全额
        Refund refund = new Refund();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderNo(dto.getOrderNo());
        refund.setPaymentNo(paymentService.getByOrder(dto.getOrderNo()).getPaymentNo());
        refund.setUserId(order.getUserId());
        refund.setAmount(order.getTotalAmount());
        refund.setReason(dto.getReason());
        refund.setStatus(RefundStatus.PENDING);
        this.save(refund);
        return toVO(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByOrder(String orderNo) {
        Refund refund = getByOrderAndStatus(orderNo, RefundStatus.PENDING);
        if (refund == null) {
            throw new BaseBusinessException(ErrorCode.REFUND_NOT_FOUND, "没有待处理的退款申请");
        }
        // 只能撤回自己的申请
        if (!refund.getUserId().equals(UserContext.getUserId())) {
            throw new BaseBusinessException(ErrorCode.FORBIDDEN);
        }
        // 条件更新占住退款单（防与管理员审核并发）
        boolean updated = updateStatus(refund.getRefundNo(), RefundStatus.PENDING, RefundStatus.CANCELLED, null, null);
        if (!updated) {
            throw new BaseBusinessException(ErrorCode.REFUND_STATUS_ERROR, "退款单已被处理，请刷新后重试");
        }
        // 订单恢复已支付
        orderService.backToPaid(refund.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO approve(String refundNo) {
        Refund refund = getByRefundNo(refundNo);
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BaseBusinessException(ErrorCode.REFUND_STATUS_ERROR, "只有待审核的退款单能审核");
        }
        // 1. 条件更新先占住退款单（防两个管理员并发双审导致渠道重复退款）
        boolean claimed = updateStatus(refundNo, RefundStatus.PENDING, RefundStatus.SUCCESS,
                UserContext.getUserId(), null);
        if (!claimed) {
            throw new BaseBusinessException(ErrorCode.REFUND_STATUS_ERROR, "该退款单已被处理");
        }
        // 2. 渠道退款 + 支付单置已退款（模拟渠道即时成功；真实渠道失败抛异常回滚本地状态）
        String channelRefundNo = paymentService.refund(refund.getOrderNo());
        // 3. 订单状态机流转：退款中→已退款，并回补库存
        orderService.toRefunded(refund.getOrderNo());
        // 4. 补记渠道退款流水
        this.update(new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getRefundNo, refundNo)
                .set(Refund::getChannelRefundNo, channelRefundNo));
        return toVO(getByRefundNo(refundNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO reject(String refundNo, String remark) {
        Refund refund = getByRefundNo(refundNo);
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BaseBusinessException(ErrorCode.REFUND_STATUS_ERROR, "只有待审核的退款单能审核");
        }
        // 条件更新占住退款单（防与用户撤回/其他管理员并发）
        boolean updated = updateStatus(refundNo, RefundStatus.PENDING, RefundStatus.REJECTED,
                UserContext.getUserId(), remark);
        if (!updated) {
            throw new BaseBusinessException(ErrorCode.REFUND_STATUS_ERROR, "该退款单已被处理");
        }
        // 订单恢复已支付
        orderService.backToPaid(refund.getOrderNo());
        return toVO(getByRefundNo(refundNo));
    }

    @Override
    public RefundVO getRefund(String refundNo) {
        Refund refund = getByRefundNo(refundNo);
        String role = UserContext.getRole();
        boolean isAdmin = "0".equals(role) || "1".equals(role);
        if (!isAdmin && !refund.getUserId().equals(UserContext.getUserId())) {
            throw new BaseBusinessException(ErrorCode.FORBIDDEN);
        }
        return toVO(refund);
    }

    @Override
    public PageResult<RefundVO> pageMyRefunds(Integer status, long page, long size) {
        Long userId = UserContext.getUserId();
        Page<Refund> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getUserId, userId)
                        .eq(status != null, Refund::getStatus, status)
                        .orderByDesc(Refund::getCreateTime));
        return toPageResult(result, page, size);
    }

    @Override
    public PageResult<RefundVO> pageRefunds(String orderNo, Long userId, Integer status, long page, long size) {
        Page<Refund> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Refund>()
                        .eq(StringUtils.hasText(orderNo), Refund::getOrderNo, orderNo)
                        .eq(userId != null, Refund::getUserId, userId)
                        .eq(status != null, Refund::getStatus, status)
                        .orderByDesc(Refund::getCreateTime));
        return toPageResult(result, page, size);
    }

    // ---------- 私有辅助 ----------

    private Refund getByRefundNo(String refundNo) {
        Refund refund = this.getOne(new LambdaQueryWrapper<Refund>().eq(Refund::getRefundNo, refundNo));
        if (refund == null) {
            throw new BaseBusinessException(ErrorCode.REFUND_NOT_FOUND);
        }
        return refund;
    }

    private Refund getByOrderAndStatus(String orderNo, int status) {
        return this.getOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderNo, orderNo)
                .eq(Refund::getStatus, status));
    }

    /**条件更新流转退款单状态，from 对不上返回 false（并发已被处理）*/
    private boolean updateStatus(String refundNo, int from, int to, Long auditBy, String auditRemark) {
        LambdaUpdateWrapper<Refund> wrapper = new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getRefundNo, refundNo)
                .eq(Refund::getStatus, from)
                .set(Refund::getStatus, to);
        if (auditBy != null) {
            wrapper.set(Refund::getAuditBy, auditBy);
        }
        if (StringUtils.hasText(auditRemark)) {
            wrapper.set(Refund::getAuditRemark, auditRemark);
        }
        return this.update(wrapper);
    }

    private PageResult<RefundVO> toPageResult(Page<Refund> result, long page, long size) {
        return PageResult.of(
                result.getRecords().stream().map(RefundConverter.INSTANCE::toVO).toList(),
                result.getTotal(), page, size);
    }

    private RefundVO toVO(Refund refund) {
        return RefundConverter.INSTANCE.toVO(refund);
    }

    /**退款单号：R + 毫秒时间戳 + 3 位随机数，与订单号生成风格一致*/
    private String generateRefundNo() {
        return "R" + System.currentTimeMillis()
                + String.valueOf(ThreadLocalRandom.current().nextInt(100, 1000));
    }
}
