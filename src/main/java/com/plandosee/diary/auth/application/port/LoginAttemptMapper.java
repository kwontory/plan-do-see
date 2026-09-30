package com.plandosee.diary.auth.application.port;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.auth.domain.LoginAttemptRow;

/**
 * login_attempts (ADR-37). The counts stop at {@code cap} (the limit being checked), so a flood of attempts never
 * makes a check slower.
 */
@Mapper
public interface LoginAttemptMapper {

    int insert(LoginAttemptRow attempt);

    /** LOGIN failures of this login key from this address after {@code since}, at most {@code cap}. */
    int countLoginFailuresByKeyAndIp(@Param("loginKeyHash") String loginKeyHash, @Param("clientIp") String clientIp,
                                     @Param("since") OffsetDateTime since, @Param("cap") int cap);

    /** LOGIN failures from this address after {@code since}, any login key, at most {@code cap}. */
    int countLoginFailuresByIp(@Param("clientIp") String clientIp, @Param("since") OffsetDateTime since,
                               @Param("cap") int cap);

    /** SIGNUP attempts from this address after {@code since}, at most {@code cap}. */
    int countSignupsByIp(@Param("clientIp") String clientIp, @Param("since") OffsetDateTime since,
                         @Param("cap") int cap);

    /** Clears the LOGIN failures of this login key from this address (after a successful login). */
    int deleteLoginFailures(@Param("loginKeyHash") String loginKeyHash, @Param("clientIp") String clientIp);

    /** Deletes up to {@code limit} rows older than {@code before}; rows locked by another clean-up are skipped. */
    int deleteOlderThan(@Param("before") OffsetDateTime before, @Param("limit") int limit);

    /** Deletes every login failure of this login key, from any address (account deletion). */
    int deleteByLoginKey(@Param("loginKeyHash") String loginKeyHash);
}
