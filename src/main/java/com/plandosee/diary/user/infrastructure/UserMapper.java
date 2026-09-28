package com.plandosee.diary.user.infrastructure;

import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.user.domain.UserRow;

@Mapper
public interface UserMapper {

    UserRow findActiveById(@Param("userId") UUID userId);
}
