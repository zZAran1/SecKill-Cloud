-- 我的退款分页：WHERE user_id = ? [AND status = ?] ORDER BY create_time DESC
ALTER TABLE refunds
    ADD KEY idx_user_createtime (user_id, create_time);

-- 管理端退款分页：WHERE [status = ?] ORDER BY create_time DESC
ALTER TABLE refunds
    ADD KEY idx_status_createtime (status, create_time);
