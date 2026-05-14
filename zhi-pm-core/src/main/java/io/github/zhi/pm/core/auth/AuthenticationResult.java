package io.github.zhi.pm.core.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AuthenticationResult {
    private final boolean authenticated;
    private final String userId;
    private final Map<String, String> attributes;

    public AuthenticationResult(boolean authenticated, String userId, Map<String, String> attributes) {
        this.authenticated = authenticated;
        this.userId = userId;
        this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public static AuthenticationResult authenticated(String userId) {
        return new AuthenticationResult(true, userId, Collections.emptyMap());
    }

    public static AuthenticationResult authenticated(String userId, Map<String, String> attributes) {
        return new AuthenticationResult(true, userId, attributes);
    }

    public static AuthenticationResult rejected() {
        return new AuthenticationResult(false, null, Collections.emptyMap());
    }

    public boolean authenticated() { return authenticated; }
    public String userId() { return userId; }
    public Map<String, String> attributes() { return attributes; }
}
