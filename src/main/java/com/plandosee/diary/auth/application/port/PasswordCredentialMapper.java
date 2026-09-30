package com.plandosee.diary.auth.application.port;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PasswordCredentialMapper {

    int insert(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash,
               @Param("changedAt") OffsetDateTime changedAt);

    /** 1 when the person's hash was replaced. */
    int updateHash(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash,
                   @Param("changedAt") OffsetDateTime changedAt);

    /** Physically deletes the person's password hash (account deletion). */
    int deleteByUserId(@Param("userId") UUID userId);
}
