package com.example.converter;

import com.example.model.dto.RegisterDTO;
import com.example.model.entity.Users;
import com.example.model.vo.UserInfoVO;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-20T18:06:16+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Eclipse Adoptium)"
)
public class UserConverterImpl implements UserConverter {

    @Override
    public Users toEntity(RegisterDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Users users = new Users();

        users.setUsername( dto.getUsername() );
        users.setPassword( dto.getPassword() );
        users.setPhone( dto.getPhone() );
        users.setEmail( dto.getEmail() );
        users.setSex( dto.getSex() );
        users.setAge( dto.getAge() );

        return users;
    }

    @Override
    public UserInfoVO toVO(Users users) {
        if ( users == null ) {
            return null;
        }

        UserInfoVO userInfoVO = new UserInfoVO();

        userInfoVO.setId( users.getId() );
        userInfoVO.setUsername( users.getUsername() );
        userInfoVO.setSex( users.getSex() );
        userInfoVO.setAge( users.getAge() );
        userInfoVO.setPhone( users.getPhone() );
        userInfoVO.setEmail( users.getEmail() );
        userInfoVO.setRole( users.getRole() );
        userInfoVO.setStatus( users.getStatus() );

        return userInfoVO;
    }
}
