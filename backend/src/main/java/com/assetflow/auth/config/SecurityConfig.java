package com.assetflow.auth.config;

import com.assetflow.auth.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * 요청이 Controller에 도달하기 전에 인증과 CSRF를 검사하는 보안 설정.
 * 로그인 여부(JWT 검증)는 여기서, 특정 계좌의 소유 여부는 AccountService에서 확인한다.
 */
@Configuration
public class SecurityConfig {
    @Bean
    SecretKeySpec jwtKey(@Value("${assetflow.auth.jwt-secret}") String secret) {
        // HS256은 발급과 검증에 같은 비밀 키를 쓴다. 바이트 길이 검사만으로 무작위성까지 보장되지는 않는다.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET은 최소 32바이트여야 합니다.");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKeySpec key) {
        // 직접 서명 알고리즘을 구현하지 않고 Nimbus 라이브러리에 JWT 생성을 맡긴다.
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKeySpec key) {
        // 서명에 사용할 알고리즘을 고정하고, 기본 시간 검증에 발급자 검증을 추가한다.
        // 여기서 파싱된 Jwt만 @AuthenticationPrincipal로 Controller에 전달된다.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.ISSUER));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    BearerTokenResolver cookieTokenResolver() {
        // Resource Server의 검증 기능은 재사용하되 토큰을 읽는 위치는 인증 쿠키로 한정한다.
        // 현재 구현은 Authorization 헤더에 전달한 Bearer 토큰을 읽지 않는다.
        return request -> {
            // 만료된 쿠키가 있어도 재로그인·로그아웃과 CSRF 토큰 발급은 가능해야 한다.
            String path = request.getRequestURI();
            if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")
                    || path.equals("/api/v1/auth/csrf") || path.equals("/api/v1/auth/logout")) return null;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (cookie.getName().equals(TokenService.COOKIE_NAME)) return cookie.getValue();
                }
            }
            return null;
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenResolver resolver,
            @Value("${assetflow.auth.cookie-secure}") boolean secure) throws Exception {
        CookieCsrfTokenRepository csrf = new CookieCsrfTokenRepository();
        // 인증 쿠키는 브라우저가 자동 전송하므로 JWT 방식에서도 CSRF 검증을 유지한다.
        // FE는 CSRF 쿠키를 직접 읽는 대신 /auth/csrf 응답의 마스킹된 토큰을 헤더에 넣는다.
        csrf.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Strict").secure(secure));
        // 인증 상태를 서버 HTTP 세션에 보관하지 않는다. 요청마다 JWT를 다시 검증한다.
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .csrf(config -> config.csrfTokenRepository(csrf))
                // permitAll은 로그인 요구를 풀 뿐 CSRF 검증까지 해제하지는 않는다.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        // 미인증과 접근 거절은 MVC 예외 처리에 도달하기 전에도 발생할 수 있다.
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "로그인이 필요합니다."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, 403, "요청 권한 또는 CSRF 토큰을 확인해 주세요.")))
                .oauth2ResourceServer(oauth -> oauth.bearerTokenResolver(resolver).jwt(jwt -> {})
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "로그인이 만료되었거나 유효하지 않습니다.")))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        // 필터 단계 오류도 FE가 읽는 {message} 계약에 맞춘다. message에는 아래 고정 문구만 전달한다.
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
