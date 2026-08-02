package com.portfolio.releasegovernance.domain;

public class GovernanceRuleException extends RuntimeException {
    private final String code;

    public GovernanceRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
