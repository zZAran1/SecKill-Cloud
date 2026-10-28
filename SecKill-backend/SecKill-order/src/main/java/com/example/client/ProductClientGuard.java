package com.example.client;

import com.example.client.dto.ProductDTO;
import com.example.exception.BaseBusinessException;
import com.example.result.ErrorCode;
import com.example.result.Result;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

/**
 * 商品服务调用熔断包装：Feign 调用统一从这里走。
 *
 * 降级语义是「快速失败并返回明确错误」，绝不假装成功——
 * 秒杀/下单场景宁可让用户重试，也不能产生无库存扣减的订单。
 * 熔断打开后直接走 fallback，不再把线程耗在必然失败的下游调用上；
 * 半开状态自动探测，下游恢复后调用链自动恢复。
 */
@Component
public class ProductClientGuard {

    private static final String BREAKER = "productClient";

    private final ProductClient productClient;

    public ProductClientGuard(ProductClient productClient) {
        this.productClient = productClient;
    }

    @CircuitBreaker(name = BREAKER, fallbackMethod = "getProductFallback")
    public Result<ProductDTO> getProduct(Long id) {
        return productClient.getProduct(id);
    }

    @CircuitBreaker(name = BREAKER, fallbackMethod = "deductStockFallback")
    public Result<Void> deductStock(Long id, Integer quantity) {
        return productClient.deductStock(id, quantity);
    }

    @CircuitBreaker(name = BREAKER, fallbackMethod = "restoreStockFallback")
    public Result<Void> restoreStock(Long id, Integer quantity) {
        return productClient.restoreStock(id, quantity);
    }

    // ---------- 降级方法：抛业务异常快速失败，异常信息面向用户可读 ----------

    @SuppressWarnings("unused")
    private Result<ProductDTO> getProductFallback(Long id, Throwable t) {
        throw new BaseBusinessException(ErrorCode.ORDER_CREATE_FAILED, "商品服务暂时不可用，请稍后再试");
    }

    @SuppressWarnings("unused")
    private Result<Void> deductStockFallback(Long id, Integer quantity, Throwable t) {
        throw new BaseBusinessException(ErrorCode.ORDER_CREATE_FAILED, "库存服务暂时不可用，请稍后再试");
    }

    @SuppressWarnings("unused")
    private Result<Void> restoreStockFallback(Long id, Integer quantity, Throwable t) {
        // 回补失败不能吞掉：关单/退款场景库存不回补会造成资损，交由调用方回滚或人工补偿
        throw new BaseBusinessException(ErrorCode.SYSTEM_ERROR, "库存回补服务暂时不可用，订单操作已回滚");
    }
}
