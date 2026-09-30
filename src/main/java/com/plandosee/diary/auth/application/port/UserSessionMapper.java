package com.plandosee.diary.auth.application.port;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Ends server sessions by their principal index (Spring Session JDBC PRINCIPAL_NAME = the user id), inside the
 * caller's transaction. Reading and writing single sessions is left to Spring Session (ADR-34).
 */
@Mapper
public interface UserSessionMapper {

    /** Deletes every session of the person (attributes go with ON DELETE CASCADE); returns how many. */
    int deleteByPrincipalName(@Param("principalName") String principalName);
}
