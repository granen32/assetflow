package com.assetflow.member.service;

import com.assetflow.global.exception.BusinessException;
import com.assetflow.member.dto.MemberResponse;
import com.assetflow.member.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository repository;

    // 생성자 주입: 외부 의존성을 명시하고 테스트에서 가짜 Repository를 전달하기 쉽다.
    public MemberService(MemberRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public MemberResponse get(Long id) {
        // 현재 HTTP 진입점은 /auth/me다. 이 메서드 자체가 호출자의 소유권을 검증하는 것은 아니다.
        // Controller가 JWT에서 추출한 ID를 전달한다는 호출 경계를 함께 이해해야 한다.
        return MemberResponse.from(repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.")));
    }
}
