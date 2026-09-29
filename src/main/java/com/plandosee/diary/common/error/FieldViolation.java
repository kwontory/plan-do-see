package com.plandosee.diary.common.error;

/**
 * One broken input rule: the field it belongs to (null for the whole form), the error code (also the
 * message key) and optional message arguments. Never carries text.
 */
public record FieldViolation(String field, String code, Object... args) {

    private static final Object[] NO_ARGS = new Object[0];

    public FieldViolation {
        if (code == null) {
            throw new IllegalArgumentException("code");
        }
        args = args == null ? NO_ARGS : args.clone();
    }

    @Override
    public Object[] args() {
        return args.clone();
    }
}
