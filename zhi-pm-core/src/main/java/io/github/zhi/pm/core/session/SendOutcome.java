package io.github.zhi.pm.core.session;

public enum SendOutcome {
    SENT,
    OVERFLOW,
    TERMINATED,
    CANCELLED,
    NON_SERIALIZED,
    FAILED;

    public boolean sent() {
        return this == SENT;
    }
}
