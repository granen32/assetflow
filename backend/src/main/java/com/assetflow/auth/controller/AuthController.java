package com.assetflow.auth.controller;

import com.assetflow.auth.dto.LoginRequest;
import com.assetflow.auth.dto.RegisterRequest;
import com.assetflow.auth.service.AuthService;
import com.assetflow.auth.service.TokenService;
import com.assetflow.member.dto.MemberResponse;
import com.assetflow.member.service.MemberService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 입력 검증·HTTP 상태·쿠키 설정을 연결한다. 비밀번호 판단은 AuthService에 위임한다. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final TokenService tokens;
    private final MemberService members;

    public AuthController(AuthService auth, TokenService tokens, MemberService members) {
        this.auth = auth;
        this.tokens = tokens;
        this.members = members;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        // Spring Security가 요청에 연결한 CSRF 토큰이다. JWT와 목적·수명이 다른 값이다.
        // 기본 요청 핸들러가 마스킹한 값을 FE에 주며 이후 변경 요청의 헤더로 돌려받는다.
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        MemberResponse member = auth.register(request);
        // 가입 성공 시 로그인 쿠키도 발급한다. 응답 본문에는 회원 DTO만 포함한다.
        return ResponseEntity.created(URI.create("/api/v1/auth/me"))
                .header(HttpHeaders.SET_COOKIE, tokens.loginCookie(member.id()).toString()).body(member);
    }

    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request) {
        MemberResponse member = auth.login(request);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, tokens.loginCookie(member.id()).toString()).body(member);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, tokens.logoutCookie().toString()).build();
    }

    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal Jwt jwt) {
        // 쿠키의 문자열을 직접 믿지 않는다. 필터가 검증한 JWT의 subject로 현재 회원을 조회한다.
        return members.get(Long.valueOf(jwt.getSubject()));
    }
}
