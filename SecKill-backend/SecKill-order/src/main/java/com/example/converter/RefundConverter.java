package com.example.converter;

import com.example.model.entity.Refund;
import com.example.model.vo.RefundVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RefundConverter {
    RefundConverter INSTANCE = Mappers.getMapper(RefundConverter.class);
    RefundVO toVO(Refund refund);
}
