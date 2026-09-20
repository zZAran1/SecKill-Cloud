package com.example.converter;

import com.example.model.dto.RegisterDTO;
import com.example.model.entity.Users;
import com.example.model.vo.UserInfoVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserConverter {
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);
    Users toEntity(RegisterDTO dto);
    UserInfoVO toVO(Users users);
}
