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

/** JWT의 발급과 쿠키 수명을 한곳에서 관리한다. 비밀번호 검증이나 회원 조회는 수행하지 않는다. */
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
        // subject는 인증된 회원의 식별자다. 이메일·비밀번호 등 불필요한 정보는 토큰에 넣지 않는다.
        // JWT는 서명으로 위조를 확인하는 데이터이지, 내용이 암호화된 비밀 저장소가 아니다.
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject(memberId.toString())
                .issuedAt(now).expiresAt(now.plus(LIFETIME)).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return cookie(token, LIFETIME);
    }

    // 동일한 이름·경로의 쿠키를 만료시킨다. 이미 발급된 토큰의 서버 측 즉시 폐기는 구현하지 않았다.
    public ResponseCookie logoutCookie() { return cookie("", Duration.ZERO); }

    private ResponseCookie cookie(String value, Duration maxAge) {
        // HttpOnly는 JS의 쿠키 읽기를 막고, Secure는 HTTPS 전송을 요구한다.
        // 로컬 HTTP에서는 secure=false를 사용하며 HTTPS 배포에서는 true로 설정해야 한다.
        return ResponseCookie.from(COOKIE_NAME, value).httpOnly(true).secure(secure)
                .sameSite("Strict").path("/").maxAge(maxAge).build();
    }
}
