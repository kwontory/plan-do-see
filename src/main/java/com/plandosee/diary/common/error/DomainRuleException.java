package com.plandosee.diary.common.error;

/**
 * A request that is well-formed but violates a domain rule (ADR-13).
 * It carries only the offending field, an error code, and optional message arguments. The code is also the
 * message key in messages.properties; the web layer turns it into a field error or a flash key.
 */
public class DomainRuleException extends RuntimeException {

    private static final Object[] NO_ARGS = new Object[0];

    private final String field;
    private final String code;
    private final Object[] args;

    public DomainRuleException(String field, String code, Object... args) {
        super(code);
        this.field = field;
        this.code = code;
        this.args = args == null ? NO_ARGS : args.clone();
    }

    public String field() {
        return field;
    }

    public String code() {
        return code;
    }

    public Object[] args() {
        return args.clone();
    }
}
