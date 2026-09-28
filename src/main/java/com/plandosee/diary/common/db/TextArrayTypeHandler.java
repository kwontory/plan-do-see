package com.plandosee.diary.common.db;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * PostgreSQL {@code TEXT[]} to and from {@code List<String>} (todo_revisions.tag_names, ADR-16). Not registered for
 * all lists: mapper XML names it explicitly with typeHandler=... on the one column that needs it. This class lives
 * outside mybatis.type-handlers-package on purpose.
 */
public class TextArrayTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        Array array = ps.getConnection().createArrayOf("text", parameter.toArray(new String[0]));
        ps.setArray(i, array);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    private static List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        Object values = array.getArray();
        List<String> list = new ArrayList<>();
        for (Object value : Arrays.asList((Object[]) values)) {
            list.add((String) value);
        }
        return List.copyOf(list);
    }
}
