package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.dto.CreateRefundDTO;
import com.example.model.entity.Refund;
import com.example.model.vo.RefundVO;
import com.example.result.PageResult;

public interface RefundService extends IService<Refund> {

    /**用户提交退款申请（订单进入退款中，待管理员审核）*/
    RefundVO apply(CreateRefundDTO dto);

    /**用户撤回待审核的退款申请，订单恢复已支付*/
    void cancelByOrder(String orderNo);

    /**管理员审核通过：渠道退款→订单转已退款→回补库存*/
    RefundVO approve(String refundNo);

    /**管理员拒绝：订单恢复已支付*/
    RefundVO reject(String refundNo, String remark);

    /**退款单详情（本人或管理员）*/
    RefundVO getRefund(String refundNo);

    /**我的退款单（分页），status 不传查全部*/
    PageResult<RefundVO> pageMyRefunds(Integer status, long page, long size);

    /**管理员：条件分页查询退款单*/
    PageResult<RefundVO> pageRefunds(String orderNo, Long userId, Integer status, long page, long size);
}
