package com.assetflow.auth.service;

import com.assetflow.auth.dto.LoginRequest;
import com.assetflow.auth.dto.RegisterRequest;
import com.assetflow.global.exception.BusinessException;
import com.assetflow.member.domain.Member;
import com.assetflow.member.dto.MemberResponse;
import com.assetflow.member.repository.MemberRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final MemberRepository members;
    private final PasswordEncoder passwords;
    private final String dummyHash;

    public AuthService(MemberRepository members, PasswordEncoder passwords) {
        this.members = members;
        this.passwords = passwords;
        // 존재하지 않는 회원도 BCrypt 검사를 수행해 계정 유무에 따른 시간 차이를 줄인다.
        this.dummyHash = passwords.encode("not-a-real-member-password");
    }

    @Transactional
    public MemberResponse register(RegisterRequest request) {
        validatePasswordBytes(request.password());
        String email = request.email().trim();
        if (members.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 등록된 이메일입니다.");
        }
        if (request.nickname().trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "닉네임을 입력해 주세요.");
        }
        Member member = new Member(email, request.nickname().trim(), passwords.encode(request.password()));
        return MemberResponse.from(members.saveAndFlush(member));
    }

    @Transactional(readOnly = true)
    public MemberResponse login(LoginRequest request) {
        validatePasswordBytes(request.password());
        Member member = members.findByEmail(request.email().trim()).orElse(null);
        String hash = member == null || member.getPasswordHash() == null ? dummyHash : member.getPasswordHash();
        boolean matches = passwords.matches(request.password(), hash);
        if (!matches || member == null || member.getPasswordHash() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해 주세요.");
        }
        return MemberResponse.from(member);
    }

    private void validatePasswordBytes(String password) {
        // BCrypt는 글자 수가 아닌 UTF-8 바이트 길이를 제한한다.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
        }
    }
}
