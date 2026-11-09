---
name: fullstack-contract
description: Spring Boot DTO와 Next.js API 클라이언트 사이의 계약을 교차 검증한다.
---

# Full-stack API Contract Review

1. method/path/query/body를 BE Controller와 FE API 함수에서 비교한다.
2. 필수 필드, enum, 금액, null 가능성, 오류 응답을 비교한다.
3. 인증을 추가할 때 임시 memberId 파라미터를 제거한다.
4. 성공/실패의 실제 브라우저 동작을 확인한다.
5. 변경을 `docs/api-contract.md`에 기록한다.
