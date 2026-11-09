# 로컬 검증 기록

## 최신: 공개용 소스 정리 (2026-11-09)

- 환경변수 변형 파일·개인 키·로그·DB 파일·산출물 제외 규칙을 추가했다. `.env.example`은 유지한다.
- 개인 PC 경로와 개인 라이브러리 파일 링크를 공개 문서에서 제거했다. backend/bin 27개는 Git 추적만 해제했으며 실제 파일은 보존했다.
- `node scripts/check-public.mjs`: 현재 파일 64개, 바이너리 내용 제외 1개, 탐지 0, 종료 코드 0.
- 임시 검사 입력으로 개인 경로와 고정 JWT 키가 검출되는 것을 확인했다. 임시 파일은 제거했다.
- `node scripts/check-public.mjs --history`: 기존 이력에서 개인 경로 8건, 개인 링크 1건, 산출물 27건, noreply가 아닌 커밋 이메일 3건을 검출하여 종료 코드 1. 남은 이력이 있음을 올바르게 보고한 결과이며 해결 완료가 아니다.
- ignore 적용과 diff 공백 검사를 확인했다. 애플리케이션 로직은 변경하지 않아 FE/BE 빌드·테스트는 다시 실행하지 않았다.
- 커밋·푸시·로컬 Git 이력 초기화는 수행하지 않았다. 사용자의 원격 저장소 재생성 이후에도 로컬 기존 이력을 그대로 올리지 않도록 별도 처리가 필요하다. 안내는 `docs/public-release.md`에 있다.

## 최신: 사용자 공용 Java / Wrapper 설정 (2026-11-09)

- 사용자 요청으로 검증된 JDK 21을 사용자 공용 JDK 폴더에 복사하고 사용자 JAVA_HOME/PATH에 등록했다. 기존 환경변수는 Git에서 제외한 `.local-tools/java-env-backup.json`에 백업했다. 시스템 범위 환경변수와 기존 Java 설치는 변경하지 않았다.
- 영구 환경변수를 새 프로세스 환경에 적용해 `java -version`이 Java 21인 것을 확인했다. PATH의 기존 Oracle Java 21도 유지되며 Wrapper는 JAVA_HOME의 Corretto 21을 사용한다.
- Gradle 8.14.3 Wrapper 4개 파일을 생성하고 배포 SHA256을 고정했다. 최초 Wrapper 다운로드도 완료했다.
- `backend/gradle.properties`에 `-Dfile.encoding=COMPAT`을 설정했다. OS 기본 인코딩을 사용해 한글 사용자 경로의 worker 로드 오류를 방지한다. 소스 컴파일은 UTF-8을 유지한다.
- `backend/`에서 `.\gradlew.bat test --no-daemon` 실행: **BUILD SUCCESSFUL**, 4개 task 실제 실행. 인코딩 추가 옵션 없이 테스트 10개 통과.
- 실행 안내를 README에 반영했다. 새 터미널에서 `.\gradlew.bat bootRun`을 사용한다. JWT_SECRET과 DB 실행은 별도 준비 사항이다. 이번 작업에서 애플리케이션 서버는 기동하지 않았다.

## 최신: 로그인·JWT 구현 검증 (2026-11-09)

사용자 선택: 거래 구현보다 로그인·JWT 우선. 마스터 문서는 프로젝트 루트에 반영했다.

| 항목 | 결과 | 근거 |
|---|---|---|
| JDK 21 / Gradle 8.14.3 | 준비 완료 | 공식 배포 ZIP을 `.local-tools/`에 다운로드, SHA256 대조 후 해제. 시스템 PATH는 변경하지 않음 |
| BE 컴파일·테스트 | 통과 | Java 21 + Gradle 8.14.3, `BUILD SUCCESSFUL`, 테스트 10개, 실패/오류/스킵 0 |
| 인증 통합 테스트 | 6개 통과 | 실제 Spring Security 필터 + MockMvc + JPA + Flyway V1/V2 + H2 PostgreSQL 모드 |
| FE typecheck | 통과 | 종료 코드 0 |
| FE lint | 통과 | 종료 코드 0, 규칙 비활성화 없음 |
| FE build | 통과 | Next.js 프로덕션 빌드, 종료 코드 0 |
| Wrapper 생성 / 실행 JAR 패키징 | 미실행 | 추가 명령이 승인되지 않아 실행하지 않음 |
| PostgreSQL 실DB | 미검증 | Docker 실행 환경 없음. H2 검증과 구분 |
| 브라우저 FE↔BE 통합 | 미검증 | 이번 검증은 BE MockMvc 통합 테스트와 FE 정적 검사/빌드까지 |

인증 통합 테스트는 다음을 확인했다.

- 회원가입, BCrypt 해시 저장·검증, 응답에 비밀번호/해시 미포함, HttpOnly/SameSite 쿠키, 현재 회원 복원
- 로그인 성공, 잘못된 비밀번호·없는 회원·비밀번호 없는 기존 회원의 401 응답
- 중복 이메일 409, 짧은 비밀번호 및 UTF-8 72바이트 초과 비밀번호 400
- 가입/로그인/로그아웃/계좌 변경 요청의 CSRF 누락 403, 미로그인 조회 401
- 가입 → 초기 잔액 0 계좌 생성 → 목록 조회 → 이름 변경, JWT 회원 ID 사용
- 다른 회원 ID를 query/body에 보내도 타인 계좌 목록·상세·수정 접근 불가
- 변조·만료 JWT 거절, 만료 쿠키 상태에서 재로그인 가능, 로그아웃 쿠키 삭제

BE 실행 명령 (`backend/`, JAVA_HOME은 프로젝트 `.local-tools/jdk/jdk21.0.13_0`):

```powershell
& '../.local-tools/gradle/gradle-8.14.3/bin/gradle.bat' test --no-daemon '-Dorg.gradle.jvmargs=-Dfile.encoding=MS949'
```

초기 Gradle 실행은 샌드박스 native DLL 접근 실패, 다음 실행은 한글 경로의 worker classpath 인코딩 불일치로 실패했다. 권한을 허용한 실행에서 Gradle daemon 인코딩을 Windows native.encoding(MS949)에 맞추자 통과했다. JavaCompile은 UTF-8을 명시했다. 테스트를 제외하거나 완화하지 않았다.

테스트 상세는 `backend/build/reports/tests/test/index.html`, XML 집계는 `backend/build/test-results/test/TEST-*.xml`에 있다. 출력은 현재 PC의 생성 산출물이다.

JWT_SECRET은 최소 32바이트 환경변수다. 실제 운영 키는 저장하지 않았다. 로그아웃은 쿠키 삭제이며 서버 측 토큰 즉시 폐기/refresh/로그인 시도 제한은 후속 범위다. 기존 회원·계좌는 보존하며 비밀번호 없는 회원의 로그인은 거절한다. 외부 배포 검증은 수행하지 않았다.

## 이전: 인증 구현 전 스캐폴딩 검증

검증일: 2026-11-09 (한국 시간)

공유 인계 문서의 P0 검증과 거래 관리 설계 제안을 현재 로컬 파일 기준으로 수행한다.

## 환경

- 프로젝트 루트에 `.git`이 없어 `git status --short` 실패. 기존 파일을 삭제하거나 초기화하지 않았다.
- Node.js v24.14.1, npm 11.11.0 확인.
- Java는 PATH에 없다. 사용자 `.jdks`에서 OpenJDK 26.0.1을 발견했지만 프로젝트가 요구하는 Java 21은 확인하지 못했다.
- Gradle은 PATH에 없고 프로젝트의 Gradle Wrapper도 없다. 사용자 캐시에 Gradle 9.7.1/9.8.1이 있으나 프로젝트용 빌드 환경으로 검증되지 않았다.
- Docker 명령을 찾을 수 없다.

## 실행 결과

| 검증 | 결과 | 근거 |
|---|---|---|
| `gradle test` (backend) | 실행 불가 | Gradle 명령 없음. Java 컴파일·테스트 미검증 |
| `docker compose up -d` | 실행 불가 | Docker 명령 없음. PostgreSQL 연결 미검증 |
| `npm install` (frontend) | 통과 | 네트워크 권한으로 재실행, 종료 코드 0. package-lock.json 생성 |
| `npm run typecheck` | 통과 | 수정 후 종료 코드 0 |
| `npm run lint` | 통과 | Effect 관련 오류 2건 수정 후 종료 코드 0 |
| `npm run build` | 통과 | Next.js 16.4.5, 컴파일·타입 검사·정적 페이지 생성 완료, 종료 코드 0 |
| `npm run start -- --hostname 127.0.0.1 --port 3000` | 기동 확인 | Ready 출력 확인. 브라우저 접속은 시간 초과, 화면 검증 미완료 |
| 회원 생성 → 계좌 등록 → 조회·합계 | 미실행 | BE·DB 실행 환경 필요 |

## 다음 단계

1. JDK 21, 프로젝트와 호환되는 Gradle 및 Docker를 준비하고 BE 테스트와 DB 연결을 확인한다.
2. FE·BE를 실행해 회원 생성, 초기 잔액 0인 계좌 및 저축 계좌 생성, 목록·합계·새로고침 유지 동작을 확인한다.
3. 다른 회원 계좌 접근, 존재하지 않는 계좌, 잘못된 입력도 확인한다.

## 변경과 한계

- `page.tsx`: localStorage 회원 복원을 `useSyncExternalStore` 구독으로 변경하고 JSON 구조를 검증한다. 계좌 초기 조회는 비동기 결과에서 상태를 갱신하고, 회원 전환 후 이전 Effect 요청의 반영을 방지한다. Lint 규칙은 유지했다.
- `package-lock.json`: 설치된 FE 의존성 버전을 고정한다. 설치 출력에 moderate 취약점 8건이 보고됐으며, 상세 분석이나 자동 버전 변경은 수행하지 않았다.
- `tsconfig.json`: Next.js가 개발용 생성 타입 경로 `.next/dev/types/**/*.ts`를 추가했다.
- `next-env.d.ts`: Next.js 빌드가 생성 타입 참조를 갱신했다.
- 빌드·기동 시 홈 디렉터리의 별도 package-lock.json을 무시한다는 경고가 있다. 빌드는 통과했으며 프로젝트 밖 파일은 변경하지 않았다.
- 서버 기동은 실제 브라우저 렌더링 또는 회원·계좌 API 연동 성공의 증거가 아니다.
- 로컬 HTTP 조회는 샌드박스에서 소켓 접근 제한으로 실패했고 권한을 확장한 재시도에서도 10초 시간 초과했다. HTTP 200 응답은 확인하지 못했다. 검증용 서버는 확인 후 종료했다.

거래 기능은 [설계 초안](transaction-design.md)에 정리했다. 구현 전에 PAIR 모드의 설계 확인 절차를 따른다.
