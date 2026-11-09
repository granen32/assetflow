# 아키텍처 이해하기

## 왜 모듈형 모놀리스인가?
초기에 서버를 여러 개로 나누면 배포·통신·관측 비용이 커진다. 따라서 하나의 Spring Boot 서버에 기능별 패키지를 두고, 나중에 기능 경계를 분리할 수 있도록 한다.

## 요청 처리 흐름
`Browser → Next.js (/api proxy) → Spring Controller → Service → Repository → PostgreSQL`

- `frontend/src/app/page.tsx`: 화면 상태와 사용자 이벤트를 관리한다.
- `frontend/src/lib/api.ts`: FE 공통 API 클라이언트와 DTO 타입을 정의한다.
- `frontend/next.config.ts`: 로컬 개발 시 `/api` 호출을 BE의 8080 포트로 프록시한다.

- `Controller`: JSON을 DTO로 변환하고 `@Valid`로 형식을 검사한다.
- `auth/config/SecurityConfig`: JWT 서명·만료·발급자 검증, CSRF와 접근 제한을 설정한다.
- `auth/service/AuthService`: 가입·비밀번호 검증, `TokenService`: JWT 발급과 쿠키 설정을 담당한다.
- `Service`: 회원 존재 여부, 계좌 소유 여부 등 업무 규칙을 처리한다. `@Transactional` 범위를 책임진다.
- `Repository`: Spring Data JPA가 기본 CRUD 구현을 만들어 준다.
- `Entity`: 저장할 데이터와 변경 메서드를 표현한다.
- `DTO`: 외부에 노출할 데이터 형태를 명확히 통제한다.

## 첫 주의 단순화
- JWT subject에서 회원 ID를 가져온다. 클라이언트 memberId로 계좌 소유자를 결정하지 않는다.
- 잔액은 `openingBalance`만 표시한다. 거래 테이블 도입 후 계산 로직을 추가한다.
- 회원가입은 BCrypt 해시를 저장한다. 이전 학습 회원의 해시는 null이며 로그인을 거절한다.
- 삭제 API는 자산 거래 및 감사 이력 정책을 정한 뒤 구현한다.

## 다음 학습 단계
1. 인증·쿠키·CSRF 흐름과 사용자별 소유권 검증 이해
2. 거래 엔티티와 수입/지출/이체 로직
3. JPA 통합 테스트(Testcontainers)
4. 계좌 동시 변경 및 통계 쿼리 최적화

## 이번 인증 구현에서 읽을 Java 개념

- `PasswordEncoder`는 인터페이스이고 실제 구현은 BCryptPasswordEncoder다. 해시를 복호화하지 않고 `matches`로 입력 비밀번호를 비교한다.
- `RegisterRequest`/`LoginRequest`는 record DTO다. 비밀번호가 기본 toString에 남지 않도록 재정의했다.
- `SecurityFilterChain`은 Controller 전에 실행된다. JWT가 유효하지 않으면 Controller에 도달하기 전에 401을 반환한다.
- `@AuthenticationPrincipal Jwt`는 검증된 토큰이다. 계좌 서비스에는 여기서 꺼낸 회원 ID만 전달한다.
- `@Transactional` 가입에서 회원을 저장한다. 동시에 같은 이메일로 가입하면 DB UNIQUE 제약이 최종 중복을 막고 409로 응답한다.
- HttpOnly는 브라우저 JavaScript의 쿠키 읽기를 막는다. 브라우저가 자동으로 쿠키를 전송하므로 변경 요청의 CSRF 검증도 필요하다. Spring의 [CSRF 설명](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)을 참고한다.
