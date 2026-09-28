package com.plandosee.diary.common.error;

/**
 * A request that is well-formed but violates a domain rule. The message is a safe, user-facing Korean sentence.
 */
public class DomainRuleException extends RuntimeException {

    private final String field;

    public DomainRuleException(String field, String userMessage) {
        super(userMessage);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
