package com.plandosee.diary.common.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * Every TIMESTAMPTZ read. PostgreSQL's {@code -infinity} / {@code infinity} arrive from the driver as
 * OffsetDateTime.MIN / MAX, which Java cannot move to another zone, so one such execution record used to break the
 * todo page, the evidence lists and the whole export. They are read as null here, in one place: "a time that cannot
 * be shown". Null is shown as nothing by the date-time fragment and exported as null; no value is made up. Since V5
 * such values can no longer be stored; this protects rows written before. Writing is unchanged.
 */
@MappedTypes(OffsetDateTime.class)
public class OffsetDateTimeTypeHandler extends BaseTypeHandler<OffsetDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OffsetDateTime parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return displayable(rs.getObject(columnName, OffsetDateTime.class));
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return displayable(rs.getObject(columnIndex, OffsetDateTime.class));
    }

    @Override
    public OffsetDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return displayable(cs.getObject(columnIndex, OffsetDateTime.class));
    }

    /** Null for the infinite values; any other instant unchanged. */
    static OffsetDateTime displayable(OffsetDateTime value) {
        if (value == null || value.equals(OffsetDateTime.MIN) || value.equals(OffsetDateTime.MAX)) {
            return null;
        }
        return value;
    }
}
