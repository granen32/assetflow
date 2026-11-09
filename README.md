# AssetFlow — Full-stack Personal Finance SaaS

개인 자산 관리 서비스를 만들면서 **Java/Spring Boot + Next.js + Codex Harness**를 학습하는 프로젝트입니다.

## 현재 구현 (Phase 1)
- **FE**: 대시보드, 회원가입·로그인·로그아웃·로그인 복원, 계좌 등록·목록·잔액 요약, 로딩/오류 UI
- **BE**: Spring Security/JWT/CSRF, BCrypt, 인증 회원별 계좌 생성·목록·상세·이름 변경, JPA, Flyway, 단위/인증 통합 테스트
- **Harness**: `AGENTS.md`, FE/BE/API 계약별 Skill 5개, 설계 기록

> 로컬 학습용 프로젝트입니다. 인증은 구현했지만 외부 배포 전 보안 검토가 남아 있습니다. 잔액은 초기 잔액이며 거래 집계는 아직 없습니다. 기존 비밀번호 없는 학습 회원은 로그인할 수 없으므로 새 이메일로 가입합니다.

## 폴더 구성

```text
assetflow/
├── frontend/          # Next.js + TypeScript
├── backend/           # Java 21 + Spring Boot
├── docs/              # 설계와 API 계약
├── .agents/skills/    # Harness 작업 절차
├── AGENTS.md          # AI 개발 규칙
└── compose.yaml       # PostgreSQL
```

## 준비물

공개 저장소로 올리기 전에는 [공개 준비 안내](docs/public-release.md)에 따라 현재 파일과 Git 이력을 검사합니다. 기존 원격 저장소를 재생성해도 로컬 이력은 남습니다.
- JDK 21
- Gradle 전역 설치 불필요 (프로젝트의 Gradle Wrapper 사용)
- Node.js 20.9+ (권장: 22 LTS 이상)
- Docker Desktop

## 로컬 실행 (터미널 3개)

```powershell
# 1. 프로젝트 루트에서 DB 실행
docker compose up -d

# 2. 별도 PowerShell에서 JWT 비밀 키 설정 후 백엔드 실행
# 키를 출력하거나 커밋하지 않는다. 실행 터미널에만 설정된다.
$jwtBytes = New-Object byte[] 48
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtRandom.Dispose()
cd backend
.\gradlew.bat bootRun

# 3. 새 터미널에서 프론트엔드 실행
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:3000` 접속. Next.js가 `/api/*`를 `http://localhost:8080/api/*`로 프록시합니다.

새 터미널에서 `java -version`이 Java 21로 표시되는지 확인합니다. JDK 21의 설치 폴더를 JAVA_HOME에, 그 하위 bin 폴더를 사용자 PATH에 등록합니다. 기존 앱에서 새 터미널을 열어도 반영되지 않으면 터미널 앱 또는 Codex를 완전히 종료한 뒤 다시 실행합니다.

JWT_SECRET은 최소 32바이트가 필요합니다. `.env.example`을 복사하는 것만으로 Spring Boot에 적용되지는 않습니다. 같은 키를 유지하면 서버 재시작 후에도 만료 전 토큰을 검증할 수 있습니다. HTTPS 환경에서는 `AUTH_COOKIE_SECURE=true`를 설정합니다.

프로젝트는 Gradle 8.14.3과 배포 ZIP의 SHA256을 Wrapper에 고정합니다. 최초 실행 시 공식 배포 파일을 다운로드합니다. 전역 `gradle` 명령 대신 아래처럼 실행합니다.

```powershell
cd backend
.\gradlew.bat test
```

한글 경로의 worker 인코딩 문제를 막도록 `backend/gradle.properties`에서 JVM 파일 인코딩을 OS 기본값에 맞춥니다. Java 소스는 UTF-8로 컴파일합니다. 다른 PC에서는 JDK 21만 별도로 준비하면 됩니다.

## 첫 시나리오
1. '시작하기'를 눌러 이메일·닉네임·비밀번호로 가입한다. 가입 성공 시 로그인된다.
2. 계좌 이름/유형/초기 잔액을 입력한다.
3. 등록된 계좌가 목록과 총 보유 자산 카드에 반영되는지 확인한다.
4. 새로고침 후 쿠키로 로그인 상태가 복원되는지 확인한다.
5. 로그아웃하고 다른 회원으로 로그인해 기존 회원 계좌가 보이지 않는지 확인한다.

## 검증 명령

```bash
cd backend && .\gradlew.bat test
cd frontend && npm run typecheck && npm run lint && npm run build
```

## 학습용 코드 읽는 순서

전체 요청 흐름과 파일별 역할은 [백엔드 코드 해설](docs/backend-walkthrough.md)에 정리했습니다. 소스 안의 한국어 주석과 함께 읽습니다.
1. `backend/src/main/java/com/assetflow/member/domain/Member.java`
2. `backend/src/main/java/com/assetflow/member/service/MemberService.java`
3. `backend/src/main/java/com/assetflow/account/service/AccountService.java`
4. `backend/src/test/java/com/assetflow/account/service/AccountServiceTest.java`
5. `frontend/src/lib/api.ts`
6. `frontend/src/app/page.tsx`
7. `AGENTS.md` → `.agents/skills/` → `docs/api-contract.md`

## 학습 과제
- **Java**: 계좌 이름 변경 로직에 공백 검증을 넣고 단위 테스트 작성
- **FE**: 계좌 수정 모달을 만들어 PATCH API 연결
- **Full-stack**: HttpOnly 쿠키, CSRF 토큰, 401/403/404의 차이 확인
- **Harness**: API 요청 필드를 변경한 뒤 `fullstack-contract` Skill로 영향 범위 검토

## 다음 단계
거래 내역 Entity 및 API → 실제 잔액 집계 → PostgreSQL 통합 테스트 → CI/CD 순으로 확장합니다. 현재 계약은 [docs/api-contract.md](docs/api-contract.md), 실행 근거와 미검증 항목은 [docs/verification.md](docs/verification.md)에 기록합니다.
# assetflow
# assetflow
