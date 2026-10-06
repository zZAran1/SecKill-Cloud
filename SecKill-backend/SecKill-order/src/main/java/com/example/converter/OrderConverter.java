package com.example.converter;

import com.example.model.entity.Order;
import com.example.model.vo.OrderVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OrderConverter {
    OrderConverter INSTANCE = Mappers.getMapper(OrderConverter.class);
    OrderVO toVO(Order order);
}