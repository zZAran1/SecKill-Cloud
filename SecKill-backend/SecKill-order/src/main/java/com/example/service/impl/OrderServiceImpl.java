package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.client.ProductClient;
import com.example.client.dto.ProductDTO;
import com.example.converter.OrderConverter;
import com.example.exception.BaseBusinessException;
import com.example.mapper.OrderMapper;
import com.example.model.dto.CreateOrderDTO;
import com.example.model.entity.Order;
import com.example.model.vo.OrderVO;
import com.example.result.ErrorCode;
import com.example.result.Result;
import com.example.security.UserContext;
import com.example.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final ProductClient productClient;

    public OrderServiceImpl(ProductClient productClient) {
        this.productClient = productClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderDTO dto) {
        // 1. 网关注入的身份（无 X-User-Id 头时 UserContext 自动抛 401）
        Long userId = UserContext.getUserId();

        // 2. Feign 调商品服务取真实价格，绝不信任客户端传价
        Result<ProductDTO> result = productClient.getProduct(dto.getProductId());
        ProductDTO product = (result == null) ? null : result.getData();
        if (result == null || result.getCode() == null || result.getCode() != 200 || product == null) {
            throw new BaseBusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BaseBusinessException(ErrorCode.ORDER_CREATE_FAILED, "商品已下架");
        }

        // 3. 组装订单落库（0 = 待支付；自增 id 由 MyBatis-Plus 回填）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setActivityId(null);
        order.setQuantity(dto.getQuantity());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
        order.setStatus(0);
        this.save(order);

        return OrderConverter.INSTANCE.toVO(order);
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long random = ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
        return date + random;
    }
}