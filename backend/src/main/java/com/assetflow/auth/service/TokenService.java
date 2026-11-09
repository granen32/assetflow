package com.assetflow.auth.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    public static final String ISSUER = "assetflow";
    public static final String COOKIE_NAME = "assetflow_access";
    private static final Duration LIFETIME = Duration.ofMinutes(30);
    private final JwtEncoder encoder;
    private final boolean secure;

    public TokenService(JwtEncoder encoder, @Value("${assetflow.auth.cookie-secure}") boolean secure) {
        this.encoder = encoder;
        this.secure = secure;
    }

    public ResponseCookie loginCookie(Long memberId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject(memberId.toString())
                .issuedAt(now).expiresAt(now.plus(LIFETIME)).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return cookie(token, LIFETIME);
    }

    public ResponseCookie logoutCookie() { return cookie("", Duration.ZERO); }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value).httpOnly(true).secure(secure)
                .sameSite("Strict").path("/").maxAge(maxAge).build();
    }
}
