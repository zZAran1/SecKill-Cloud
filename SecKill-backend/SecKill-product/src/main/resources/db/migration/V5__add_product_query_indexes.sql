-- 商品列表/分页查询：WHERE status = ? [AND name LIKE ...] ORDER BY create_time DESC
ALTER TABLE products
    ADD KEY idx_status_createtime (status, create_time);

-- 秒杀活动分页查询：WHERE status = ? ORDER BY create_time DESC
ALTER TABLE seckill_activity
    ADD KEY idx_status_createtime (status, create_time);