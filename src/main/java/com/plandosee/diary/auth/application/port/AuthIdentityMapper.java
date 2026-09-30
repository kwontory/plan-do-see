package com.plandosee.diary.auth.application.port;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.plandosee.diary.auth.domain.AuthIdentityRow;
import com.plandosee.diary.auth.domain.LocalCredentialRow;

@Mapper
public interface AuthIdentityMapper {

    /** A duplicate (provider, provider_user_id) fails on uq_auth_identities_provider_user. */
    int insert(AuthIdentityRow identity);

    /** The LOCAL login with that stored login id of an active person, with its password hash; null when none. */
    LocalCredentialRow findLocalCredentialByLoginId(@Param("loginId") String loginId);

    /** The LOCAL login of the active person, with its password hash; null when none. */
    LocalCredentialRow findLocalCredentialByUserId(@Param("userId") UUID userId);

    int touchLastLogin(@Param("identityId") UUID identityId, @Param("at") OffsetDateTime at);

    /** Physically deletes every login of the person (account deletion). */
    int deleteByUserId(@Param("userId") UUID userId);
}
