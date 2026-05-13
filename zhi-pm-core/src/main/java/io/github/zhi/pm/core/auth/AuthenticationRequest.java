package io.github.zhi.pm.core.auth;

import java.util.List;
import java.util.Map;

public final class AuthenticationRequest {
    private final String token;
    private final Map<String, List<String>> headers;
    private final String remoteAddress;

    public AuthenticationRequest(String token, Map<String, List<String>> headers, String remoteAddress) {
        this.token = token;
        this.headers = headers;
        this.remoteAddress = remoteAddress;
    }

    public String token() { return token; }
    public Map<String, List<String>> headers() { return headers; }
    public String remoteAddress() { return remoteAddress; }
}
