package com.example.client;

import com.example.client.dto.ProductDTO;
import com.example.exception.BaseBusinessException;
import com.example.result.ErrorCode;
import com.example.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 熔断降级语义测试：正常透传、降级快速失败（绝不假装成功）。
 */
class ProductClientGuardTest {

    private final ProductClient productClient = mock(ProductClient.class);
    private final ProductClientGuard guard = new ProductClientGuard(productClient);

    @Test
    void 正常调用_透传给Feign客户端() {
        Result<ProductDTO> expected = Result.success(new ProductDTO());
        when(productClient.getProduct(1L)).thenReturn(expected);

        assertSame(expected, guard.getProduct(1L));
        verify(productClient).getProduct(1L);
    }

    @Test
    void 降级_查商品失败_快速失败不下假单() {
        BaseBusinessException e = assertThrows(BaseBusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(guard, "getProductFallback", 1L, new RuntimeException("down")));
        assertEquals(ErrorCode.ORDER_CREATE_FAILED, e.getErrorCode());
    }

    @Test
    void 降级_扣库存失败_快速失败不超卖() {
        BaseBusinessException e = assertThrows(BaseBusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(guard, "deductStockFallback", 1L, 1, new RuntimeException("down")));
        assertEquals(ErrorCode.ORDER_CREATE_FAILED, e.getErrorCode());
    }

    @Test
    void 降级_回补库存失败_抛异常交由调用方回滚() {
        BaseBusinessException e = assertThrows(BaseBusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(guard, "restoreStockFallback", 1L, 1, new RuntimeException("down")));
        assertEquals(ErrorCode.SYSTEM_ERROR, e.getErrorCode());
    }
}
