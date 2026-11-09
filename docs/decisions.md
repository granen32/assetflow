# 설계 결정 기록

## ADR-001: JPA와 Flyway
- JPA로 객체·테이블 매핑을 학습한다.
- 스키마는 Flyway SQL로 관리한다.
- 운영 스키마가 코드 실행 중 임의 변경되지 않도록 `ddl-auto=validate`를 사용한다.

## ADR-002: 금액은 BigDecimal
- 부동소수점 반올림 오차를 피하기 위해 Java `BigDecimal`, PostgreSQL `NUMERIC(19,2)`를 사용한다.
- 다중 통화 환산은 첫 MVP에서 제공하지 않는다.

## ADR-003: ID 기반 임시 학습 API (ADR-004로 대체)
- 초기 스캐폴딩에서는 클라이언트 memberId를 사용했다. 인증 구현으로 제거했다.

## ADR-004: 거래보다 인증 우선
- 2026-11-09 사용자 선택에 따라 Spring Security/JWT를 거래 기능보다 먼저 구현한다.
- Spring OAuth2 Resource Server로 JWT를 검증하고 비밀번호는 BCrypt로 해시한다.
- JWT는 HttpOnly/SameSite=Strict 쿠키에 두며 CSRF 검증을 유지한다.
- JWT_SECRET은 환경변수로 주입하고 최소 32바이트를 요구한다. HTTPS 환경에서는 AUTH_COOKIE_SECURE=true로 설정한다.
- V2는 password_hash 컬럼만 추가한다. 기존 회원·계좌는 보존하고 비밀번호 없는 회원의 로그인을 금지한다.
- 로그인 유효기간은 30분. refresh token과 즉시 폐기 목록은 이번 범위에서 제외한다.
- 기존 회원 공개 API를 제거하고 `/auth/me`로 현재 회원만 조회한다.
- 로컬 학습 상태를 유지한다. 외부 배포 전 로그인 시도 제한·TLS·토큰 폐기 정책 등을 검토한다.
