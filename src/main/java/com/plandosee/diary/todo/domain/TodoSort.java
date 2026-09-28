package com.plandosee.diary.todo.domain;

/**
 * ADR-06 sort allowlist. The ORDER BY for each value is in TodoMapper.xml. The on-screen description is the
 * message key sort.&lt;NAME&gt;.description (ADR-13) and must be changed together with that SQL (T06-C20).
 */
public enum TodoSort {
    DUE,
    PRIORITY,
    CREATED;

    public static TodoSort fromParam(String value) {
        if (value != null) {
            for (TodoSort sort : values()) {
                if (sort.name().equals(value)) {
                    return sort;
                }
            }
        }
        return DUE;
    }
}
