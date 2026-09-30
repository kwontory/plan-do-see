package com.plandosee.diary.user.application.port;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.user.domain.UserRow;

@Mapper
public interface UserMapper {

    UserRow findActiveById(@Param("userId") UUID userId);

    /** New person row; email columns stay NULL. */
    int insert(UserRow user);

    /** 1 when the active user was renamed, 0 when there is no such active user. */
    int updateNickname(@Param("userId") UUID userId, @Param("nickname") String nickname,
                       @Param("updatedAt") OffsetDateTime updatedAt);

    /** Physically deletes the person row (account deletion, ADR-35); every owned row must be gone first. */
    int delete(@Param("userId") UUID userId);

    /**
     * Locks the active person row without waiting (FOR NO KEY UPDATE NOWAIT): serializes writes that must see the
     * person's other rows first (execution overlap check). Does not block inserts that only reference the row.
     * Another holder is SQLState 55P03 at once. Null when there is no such active person.
     */
    UUID lockActive(@Param("userId") UUID userId);
}
