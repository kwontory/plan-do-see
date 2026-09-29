package com.plandosee.diary.common.error;

import java.util.List;

/**
 * A request that is well-formed but violates a domain rule.
 * It carries only the offending field, an error code, and optional message arguments. The code is also the
 * message key in messages.properties; the web layer turns it into a field error or a flash key.
 * <p>
 * A command check reports every broken field at once. {@link #violations()} lists them in field order;
 * {@link #field()}, {@link #code()} and {@link #args()} are the first one (callers that show a single notice, such as
 * a flash, use those). FormErrors puts every violation on the form.
 */
public class DomainRuleException extends RuntimeException {

    private final String field;
    private final String code;
    private final Object[] args;
    private final List<FieldViolation> violations;

    public DomainRuleException(String field, String code, Object... args) {
        this(List.of(new FieldViolation(field, code, args)));
    }

    public DomainRuleException(List<FieldViolation> violations) {
        super(first(violations).code());
        this.violations = List.copyOf(violations);
        FieldViolation first = this.violations.getFirst();
        this.field = first.field();
        this.code = first.code();
        this.args = first.args();
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

    /** Every broken rule, first one first; never empty. */
    public List<FieldViolation> violations() {
        return violations;
    }

    private static FieldViolation first(List<FieldViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            throw new IllegalArgumentException("violations");
        }
        return violations.getFirst();
    }
}
