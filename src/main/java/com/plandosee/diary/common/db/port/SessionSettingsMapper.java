package com.plandosee.diary.common.db.port;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Transaction-local PostgreSQL settings. The value is a bound parameter, never spliced into SQL.
 */
@Mapper
public interface SessionSettingsMapper {

    /** SET LOCAL statement_timeout for the current transaction only; returns the value now in effect. */
    String setLocalStatementTimeout(@Param("value") String value);

    /** Current statement_timeout of this session (tests and diagnostics). */
    String showStatementTimeout();
}
