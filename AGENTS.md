# AssetFlow — Full-stack Codex Harness

## 목적
- Java/Spring Boot를 **직접 학습**하면서 자산 관리 SaaS를 만든다.
- 기본 모드는 `PAIR`. 사용자가 `LEARNING`을 지정하면 핵심 Java 코드를 대신 완성하지 않는다.
- Spring Security/JWT 인증을 사용한다. 회원 ID는 JWT subject에서 가져오며 클라이언트 입력으로 소유자를 결정하지 않는다. 외부 배포 전 추가 보안 검토가 필요하다.

## 기술과 구조
- `backend/`: Java 21, Spring Boot 3.5, Gradle, PostgreSQL, Spring Data JPA, Flyway.
- `frontend/`: Next.js App Router, React, TypeScript, CSS.
- `docs/api-contract.md`는 FE/BE가 공유하는 API 계약의 기준이다.
- 기능별 패키지(`auth`, `member`, `account`)와 `controller/service/repository/domain/dto` 계층을 사용한다.
- Controller: HTTP와 검증, Service: 유스케이스·트랜잭션, Repository: 영속화, Entity: 도메인 상태.
- Entity를 HTTP 응답으로 직접 반환하지 않는다. 금액은 `BigDecimal`을 사용한다.
- 필드 주입 대신 생성자 주입. JPA 연관관계는 기본적으로 LAZY를 명시한다.
- DB 스키마는 `db/migration/V*__*.sql`로 변경하고 `ddl-auto=validate`를 유지한다.
- FE API 함수는 `frontend/src/lib/api.ts`에 모은다.
- API 변경 시 BE DTO, FE 타입, API 계약 문서를 함께 수정한다.

## 작업 절차
1. 관련 클래스와 `docs/architecture.md`, `docs/decisions.md`를 읽는다.
2. 요구사항·영향 범위·실패 케이스를 먼저 요약한다.
3. 구조 변경 또는 DB 변경은 구현 전에 사용자와 설계를 확인한다.
4. 최소 단위 구현 후 관련 테스트를 실행한다. FE는 `npm run typecheck`, `npm run lint`, `npm run build`, BE는 `gradle test`를 권장한다.
5. API·DB·설계가 변경되면 문서를 갱신한다.
6. 테스트 실행 여부를 명확히 보고한다. 실행하지 못했으면 통과했다고 말하지 않는다.
7. FE/BE 동시 변경 시 회원 생성 → 계좌 생성 → 계좌 조회를 확인한다.

## 작업 모드
- `LEARNING`: 개념 → 힌트 → 사용자 구현 → 피드백 순서. 정답 코드는 명시적으로 요청받을 때 제공한다.
- `PAIR`(기본): 설계·핵심 구현은 사용자와 논의하고 반복 코드는 보조한다.
- `AUTO`: 명시적으로 위임받은 반복 작업만 자동 구현한다.

## 완료 조건
- 컴파일 및 관련 테스트 통과
- 정상·예외 시나리오 검토
- 데이터 소유자 검증(인증 구현 이후)
- 주석은 이유를 설명하고, 자명한 문법을 반복하지 않음
- 변경 내역과 배운 Java 개념 정리
