-- 我的订单分页：WHERE user_id = ? [AND status = ?] ORDER BY create_time DESC
ALTER TABLE orders
    ADD KEY idx_user_createtime (user_id, create_time);

-- 管理端订单分页：WHERE [status = ?] ORDER BY create_time DESC
ALTER TABLE orders
    ADD KEY idx_status_createtime (status, create_time);