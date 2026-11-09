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

/** 비밀번호 검증과 회원 저장을 담당한다. HTTP 쿠키 발급은 Controller와 TokenService의 책임이다. */
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
        // DTO의 @Size는 글자 수를 검사하므로 BCrypt의 바이트 길이 제한도 별도로 검사한다.
        validatePasswordBytes(request.password());
        String email = request.email().trim();
        // 사용자에게 중복을 먼저 안내한다. 동시 가입 경쟁의 최종 방어는 DB의 UNIQUE 제약이다.
        if (members.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 등록된 이메일입니다.");
        }
        if (request.nickname().trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "닉네임을 입력해 주세요.");
        }
        Member member = new Member(email, request.nickname().trim(), passwords.encode(request.password()));
        // 평문 대신 해시만 저장한다. flush로 INSERT를 수행해 DB 제약 위반을 응답 전에 확인한다.
        // flush는 commit과 다르며 트랜잭션이 실패하면 이 저장도 롤백된다.
        return MemberResponse.from(members.saveAndFlush(member));
    }

    @Transactional(readOnly = true)
    public MemberResponse login(LoginRequest request) {
        validatePasswordBytes(request.password());
        Member member = members.findByEmail(request.email().trim()).orElse(null);
        String hash = member == null || member.getPasswordHash() == null ? dummyHash : member.getPasswordHash();
        boolean matches = passwords.matches(request.password(), hash);
        // 해시가 null인 기존 학습 회원은 로그인할 수 없다. 실패 문구는 계정 유무와 무관하게 같다.
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
