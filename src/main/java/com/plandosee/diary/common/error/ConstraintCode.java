package com.plandosee.diary.common.error;

/**
 * What a known DB constraint means for the user. field is the form field to mark (null for a global form
 * error); code is the error code (a message key) with optional message arguments.
 */
public record ConstraintCode(String constraint, String field, String code, Object... args) {

    public ConstraintCode {
        args = args == null ? new Object[0] : args.clone();
    }

    @Override
    public Object[] args() {
        return args.clone();
    }
}
