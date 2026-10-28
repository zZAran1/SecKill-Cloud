CREATE TABLE refunds (
                         id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
                         refund_no        VARCHAR(32)   NOT NULL COMMENT '退款单号（业务唯一）',
                         order_no         VARCHAR(32)   NOT NULL COMMENT '订单号',
                         payment_no       VARCHAR(32)   NULL COMMENT '支付单号',
                         user_id          BIGINT        NOT NULL COMMENT '退款用户 ID',
                         amount           DECIMAL(10,2) NOT NULL COMMENT '退款金额（元，全额退款）',
                         reason           VARCHAR(255)  NOT NULL COMMENT '退款原因',
                         status           TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审核 1退款成功 2已拒绝 3已取消',
                         audit_by         BIGINT        NULL COMMENT '审核管理员用户 ID',
                         audit_remark     VARCHAR(255)  NULL COMMENT '审核备注（拒绝原因等）',
                         channel_refund_no VARCHAR(64)  NULL COMMENT '渠道退款流水号',
                         create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (id),
                         UNIQUE KEY uk_refund_no (refund_no),
                         KEY idx_order_no (order_no),
                         KEY idx_user_id (user_id),
                         KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单表';

-- 退款模块接入：支付单补充「已退款」状态
ALTER TABLE payments
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1支付成功 2支付失败 3已关闭 4已退款';
