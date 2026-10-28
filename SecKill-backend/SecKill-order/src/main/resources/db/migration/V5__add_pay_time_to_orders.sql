-- 支付模块接入：记录支付成功时间，并把状态机新增的 3已完成/4退款中/5已退款 写入字段注释
ALTER TABLE orders
    ADD COLUMN pay_time DATETIME NULL COMMENT '支付成功时间' AFTER status;

ALTER TABLE orders
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已取消 3已完成 4退款中 5已退款';
