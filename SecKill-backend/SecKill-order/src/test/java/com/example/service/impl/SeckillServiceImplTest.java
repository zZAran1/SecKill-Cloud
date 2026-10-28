package com.example.service.impl;

import com.example.config.RabbitMQConfig;
import com.example.constant.SeckillRedisKeys;
import com.example.exception.BaseBusinessException;
import com.example.message.SeckillOrderMessage;
import com.example.model.dto.SeckillActivityDTO;
import com.example.result.ErrorCode;
import com.example.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 秒杀链路单元测试：mock Redis/MQ，覆盖时间窗口、防重、售罄、消息发送失败回补等分支。
 * 身份来自网关注入的 X-User-Id 请求头（UserContext 读请求头）。
 */
class SeckillServiceImplTest {

    private static final Long ACTIVITY_ID = 10L;
    private static final String ACTIVITY_KEY = SeckillRedisKeys.ACTIVITY_KEY + ACTIVITY_ID;

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RabbitTemplate rabbitTemplate;
    private OrderService orderService;
    private SeckillServiceImpl seckillService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        rabbitTemplate = mock(RabbitTemplate.class);
        orderService = mock(OrderService.class);
        seckillService = new SeckillServiceImpl(redisTemplate, objectMapper, rabbitTemplate, orderService);

        // 模拟网关注入身份头
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private String activityJson(LocalDateTime start, LocalDateTime end) throws Exception {
        SeckillActivityDTO dto = new SeckillActivityDTO();
        dto.setId(ACTIVITY_ID);
        dto.setProductId(100L);
        dto.setSeckillPrice(new BigDecimal("9.90"));
        dto.setStartTime(start);
        dto.setEndTime(end);
        return objectMapper.writeValueAsString(dto);
    }

    private void stubRunningActivity() throws Exception {
        when(valueOperations.get(ACTIVITY_KEY))
                .thenReturn(activityJson(LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(25)));
    }

    private void assertErrorCode(Executable executable, ErrorCode expected) {
        BaseBusinessException e = assertThrows(BaseBusinessException.class, executable);
        assertEquals(expected, e.getErrorCode());
    }

    @Test
    void 活动未预热_抛活动不存在() {
        when(valueOperations.get(ACTIVITY_KEY)).thenReturn(null);
        assertErrorCode(() -> seckillService.seckill(ACTIVITY_ID), ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
    }

    @Test
    void 秒杀未开始_拒绝参与() throws Exception {
        when(valueOperations.get(ACTIVITY_KEY))
                .thenReturn(activityJson(LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusMinutes(35)));
        assertErrorCode(() -> seckillService.seckill(ACTIVITY_ID), ErrorCode.SECKILL_NOT_STARTED);
    }

    @Test
    void 秒杀已结束_拒绝参与() throws Exception {
        when(valueOperations.get(ACTIVITY_KEY))
                .thenReturn(activityJson(LocalDateTime.now().minusMinutes(35), LocalDateTime.now().minusMinutes(5)));
        assertErrorCode(() -> seckillService.seckill(ACTIVITY_ID), ErrorCode.SECKILL_ENDED);
    }

    @Test
    void 重复参与_拒绝并提示限购() throws Exception {
        stubRunningActivity();
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(-5L);
        assertErrorCode(() -> seckillService.seckill(ACTIVITY_ID), ErrorCode.SECKILL_REPEAT);
    }

    @Test
    void 库存售罄_拒绝并提示手慢了() throws Exception {
        stubRunningActivity();
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(-4L);
        assertErrorCode(() -> seckillService.seckill(ACTIVITY_ID), ErrorCode.SECKILL_SOLD_OUT);
    }

    @Test
    void 抢购成功_发送异步建单消息() throws Exception {
        stubRunningActivity();
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        seckillService.seckill(ACTIVITY_ID);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.SECKILL_ORDER_EXCHANGE),
                eq(RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY),
                any(SeckillOrderMessage.class),
                any(CorrelationData.class));
    }

    @Test
    void 建单消息发送失败_回补库存并报错() throws Exception {
        stubRunningActivity();
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        doThrow(new RuntimeException("MQ 不可用"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(SeckillOrderMessage.class), any(CorrelationData.class));

        assertErrorCode(() -> seckillService.seckill(ACTIVITY_ID), ErrorCode.SYSTEM_ERROR);

        // 失败必须回补 Redis 库存并解除限购标记，否则库存被永久占死
        verify(redisTemplate).execute(eq(SeckillRedisKeys.RESTORE_SCRIPT), anyList(), any());
    }
}
