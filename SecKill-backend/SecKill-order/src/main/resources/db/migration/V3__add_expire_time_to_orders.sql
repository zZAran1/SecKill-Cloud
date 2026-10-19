-- 订单支付截止时间：超时未支付自动关闭订单
ALTER TABLE orders
    ADD COLUMN expire_time DATETIME NULL COMMENT '支付截止时间，超时未支付自动关闭' AFTER status;

-- 回填存量订单：按创建时间 + 15 分钟推算
UPDATE orders SET expire_time = DATE_ADD(create_time, INTERVAL 15 MINUTE) WHERE expire_time IS NULL;