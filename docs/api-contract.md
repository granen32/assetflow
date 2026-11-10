# FE/BE API 계약 (v0.2)

Base path: `/api/v1`. 브라우저는 Next.js 동일 출처 프록시를 사용한다.

## 인증

| Method | URL | Request | Response |
|---|---|---|---|
| GET | `/auth/csrf` | 없음 | `{token, headerName}` + CSRF 쿠키 |
| POST | `/auth/register` | `{email, nickname, password}` | 201 Member + JWT 쿠키 |
| POST | `/auth/login` | `{email, password}` | 200 Member + JWT 쿠키 |
| POST | `/auth/logout` | 없음 | 204, JWT 쿠키 삭제 |
| GET | `/auth/me` | JWT 쿠키 | 200 Member |

`Member`: `{id: number, email: string, nickname: string}`. 비밀번호·해시는 응답에 포함하지 않는다.

- 가입 비밀번호: 8자 이상, UTF-8 기준 최대 72바이트. 이메일 최대 255자, 닉네임 최대 80자.
- JWT는 `assetflow_access` HttpOnly/SameSite=Strict 쿠키에 보관한다. 유효기간은 30분이다. FE는 JWT를 읽거나 localStorage에 저장하지 않는다.
- 모든 POST/PATCH 요청 전에 `/auth/csrf`를 호출하고 반환된 `headerName` 헤더에 `token`을 넣는다. CSRF 쿠키도 함께 보내야 한다. 반환 토큰은 Spring Security가 마스킹한 값이다.
- 가입·로그인·로그아웃에도 CSRF 검증을 수행한다. 만료된 JWT 쿠키가 있어도 CSRF 발급과 재로그인은 가능하다.
- 토큰 갱신 API는 없다. 만료 후 재로그인한다. 로그아웃은 쿠키를 삭제하며 발급된 JWT를 서버에서 즉시 폐기하지는 않는다.

## 계좌

가입·로그인·CSRF 발급·로그아웃 이외의 API에는 JWT 쿠키가 필요하다.

| Method | URL | Request | Response |
|---|---|---|---|
| POST | `/accounts` | `{name, type, openingBalance, currency}` | 201 Account |
| GET | `/accounts` | 없음 | Account[] |
| GET | `/accounts/{id}` | 없음 | Account |
| PATCH | `/accounts/{id}` | `{name}` | Account |

`Account`: `{id: number, memberId: number, name: string, type: 'CHECKING'|'SAVINGS'|'CASH', balance: number, currency: string}`

회원 ID는 JWT subject로 결정한다. body/query의 `memberId`는 사용하지 않는다. 다른 회원의 계좌 상세·수정은 404를 반환한다. 계좌 목록에는 본인 계좌만 포함된다.

이름은 공백 불가·최대 100자, 초기 잔액은 0 이상·정수 최대 17자리·소수 최대 2자리, 통화는 영문 대문자 3자리다.

## 오류와 호환성

실패 응답: `{ "message": "오류 설명" }`.

- 400: 입력 검증/JSON 형식 오류
- 401: 미로그인, 유효하지 않거나 만료된 JWT, 로그인 실패
- 403: CSRF 누락·불일치 또는 접근 거절
- 404: 없는 회원/계좌 또는 다른 회원의 계좌
- 409: 중복 이메일 또는 DB 저장 조건 충돌

기존 `POST /members`, `GET /members/{id}`는 제거했다. 기존 비밀번호 없는 학습 회원은 로그인할 수 없으며 데이터는 보존된다. 별도 이메일로 새 회원가입을 한다. 기존 계좌를 신규 회원에게 자동 이전하지 않는다.

## 남은 범위

- Java 금액은 BigDecimal, JSON은 number를 유지한다. FE의 큰 수 정밀도 보장은 후속 계약 설계가 필요하다.
- `balance`는 현재 초기 잔액이다. 거래·통계·부채·저축 목표는 미구현이다.
- HTTPS, 로그인 시도 제한, refresh/revocation 정책 등 외부 배포를 위한 검토는 남아 있다. 현재는 로컬 학습 환경이다.

## 본인 닉네임 수정 (검증 진행 중)

PATCH `/members/me`: `{nickname: string}` → 200 Member. JWT subject로 본인을 결정하며 CSRF가 필요하다. 입력은 공백 불가·최대 80자이며 저장 시 앞뒤 공백을 제거한다. 400/401/403/404는 기존 오류 계약을 따른다. Service에 쓰기 트랜잭션을 적용했으며 DB 영속화 및 API 통합 검증이 남아 있다.
