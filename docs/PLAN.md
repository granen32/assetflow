# 인증 기능 구현 계획

목표: 마스터 문서에 따라 로그인·JWT를 거래 기능보다 먼저 구현한다. 사용자 선택: 로그인·JWT 우선.

- [x] Spring Security, BCrypt 비밀번호 해시, JWT 서명·만료 검증
- [x] Flyway V2로 members.password_hash 추가. 기존 데이터 보존, 비밀번호 없는 회원 로그인 거절
- [x] 회원가입/로그인/로그아웃/현재 회원/CSRF API
- [x] 계좌 API의 memberId 입력 제거, JWT 주체로 소유권 검사
- [x] FE 회원가입·로그인·세션 복원·로그아웃, API 클라이언트 변경
- [x] 인증·CSRF·타인 계좌 접근 차단 테스트, FE 검증
- [x] API 계약·실행 안내·검증 결과 갱신

설계: JWT는 HttpOnly/SameSite=Strict 쿠키에 저장한다. 변경 요청은 CSRF 토큰을 함께 보낸다. JWT 만료는 30분이며 이번 범위에 refresh token은 없다. 로그아웃은 쿠키를 삭제하며 이미 유출된 JWT를 서버에서 즉시 폐기하는 기능은 후속 범위다. 기존 memberId 기반 공개 조회·생성 경로는 제거한다.

검증: BE 테스트 10개 통과(인증 통합 6개 포함), FE typecheck/lint/build 통과. 이후 사용자 요청으로 JDK 21을 사용자 공용 `.jdks/corretto-21.0.13_0`에 설치하고 사용자 JAVA_HOME/PATH를 등록했다. 기존 환경변수는 `.local-tools/java-env-backup.json`에 백업했다. Gradle 8.14.3 Wrapper를 생성했고 SHA256을 고정했다. 한글 경로의 인코딩은 gradle.properties의 COMPAT 설정으로 OS 기본값을 사용한다.

남은 검증: PostgreSQL 실DB 및 브라우저 FE↔BE 통합. 이전 검증 시 Docker가 없어 PostgreSQL을 실행하지 못했다. 이번 요청에서 Wrapper는 추가했으며 JAR 패키징은 수행하지 않았다. 다음에는 README의 환경변수를 설정하고 PostgreSQL을 준비한 뒤 가입 → 계좌 생성 → 로그아웃 → 다른 회원 접근 차단을 브라우저로 확인한다.

거래·카테고리·월별 통계는 이번 범위에 포함하지 않는다. 인증 구현 파일은 `backend/src/main/java/com/assetflow/auth/`, V2 migration, 회원·계좌 DTO/Controller/Service, `frontend/src/app/page.tsx`, `frontend/src/lib/api.ts` 및 관련 문서다.
