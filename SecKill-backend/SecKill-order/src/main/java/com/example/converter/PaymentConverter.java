package com.example.converter;

import com.example.model.entity.Payment;
import com.example.model.vo.PaymentVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentConverter {
    PaymentConverter INSTANCE = Mappers.getMapper(PaymentConverter.class);
    PaymentVO toVO(Payment payment);
}
