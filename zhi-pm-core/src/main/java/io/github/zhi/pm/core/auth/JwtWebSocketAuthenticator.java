package io.github.zhi.pm.core.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.util.Map;
import javax.crypto.SecretKey;
import reactor.core.publisher.Mono;

public final class JwtWebSocketAuthenticator implements WebSocketAuthenticator {
    private final JwtParser parser;
    private final String userIdClaim;
    private final boolean allowGuestWhenNoToken;

    private JwtWebSocketAuthenticator(JwtParser parser, String userIdClaim, boolean allowGuestWhenNoToken) {
        this.parser = parser;
        this.userIdClaim = userIdClaim;
        this.allowGuestWhenNoToken = allowGuestWhenNoToken;
    }

    public static JwtWebSocketAuthenticator withHmacSecret(String secret, String issuer, String audience,
                                                            String userIdClaim, boolean allowGuestWhenNoToken) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        JwtParser parser = buildParser(key, issuer, audience);
        return new JwtWebSocketAuthenticator(parser, userIdClaim, allowGuestWhenNoToken);
    }

    public static JwtWebSocketAuthenticator withSigningKey(Key signingKey, String issuer, String audience,
                                                            String userIdClaim, boolean allowGuestWhenNoToken) {
        JwtParser parser = buildParser(signingKey, issuer, audience);
        return new JwtWebSocketAuthenticator(parser, userIdClaim, allowGuestWhenNoToken);
    }

    private static JwtParser buildParser(Key key, String issuer, String audience) {
        JwtParserBuilder builder;
        if (key instanceof SecretKey sk) {
            builder = Jwts.parser().verifyWith(sk);
        } else if (key instanceof PublicKey pk) {
            builder = Jwts.parser().verifyWith(pk);
        } else {
            throw new IllegalArgumentException("Key must be SecretKey or PublicKey");
        }
        if (issuer != null && !issuer.isBlank()) {
            builder.requireIssuer(issuer);
        }
        if (audience != null && !audience.isBlank()) {
            builder.requireAudience(audience);
        }
        return builder.build();
    }

    @Override
    public Mono<AuthenticationResult> authenticate(AuthenticationRequest request) {
        return Mono.fromSupplier(() -> {
            String token = request == null ? null : request.token();
            if (token == null || token.isBlank()) {
                return allowGuestWhenNoToken
                        ? AuthenticationResult.authenticated("guest")
                        : AuthenticationResult.rejected();
            }
            try {
                Claims claims = parser.parseSignedClaims(token).getPayload();
                String userId = claims.get(userIdClaim, String.class);
                if (userId == null || userId.isBlank()) {
                    return AuthenticationResult.rejected();
                }
                return AuthenticationResult.authenticated(userId, Map.of(
                        "jwt.issuer", claims.getIssuer() != null ? claims.getIssuer() : "",
                        "jwt.subject", claims.getSubject() != null ? claims.getSubject() : ""
                ));
            } catch (JwtException | IllegalArgumentException e) {
                return AuthenticationResult.rejected();
            }
        });
    }
}
