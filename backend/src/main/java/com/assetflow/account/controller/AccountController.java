package com.assetflow.account.controller;

import com.assetflow.account.dto.AccountResponse;
import com.assetflow.account.dto.CreateAccountRequest;
import com.assetflow.account.dto.UpdateAccountRequest;
import com.assetflow.account.service.AccountService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

/** 모든 계좌 요청은 인증 필터 통과 후 실행되며, 회원 ID는 요청 JSON/query가 아닌 JWT에서 가져온다. */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = service.create(Long.valueOf(jwt.getSubject()), request);
        // 생성은 201과 새 리소스의 Location으로 표현하고 Entity 대신 DTO를 반환한다.
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.id())).body(response);
    }

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) { return service.list(Long.valueOf(jwt.getSubject())); }

    @GetMapping("/{id}")
    public AccountResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(Long.valueOf(jwt.getSubject()), id);
    }

    @PatchMapping("/{id}")
    public AccountResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody UpdateAccountRequest request) {
        return service.rename(Long.valueOf(jwt.getSubject()), id, request);
    }
}
