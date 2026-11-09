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
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        MemberResponse member = auth.register(request);
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
        return members.get(Long.valueOf(jwt.getSubject()));
    }
}
