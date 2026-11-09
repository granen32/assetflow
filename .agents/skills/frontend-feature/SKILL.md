---
name: frontend-feature
description: AssetFlow Next.js 화면을 API 계약, 타입 안정성, 사용성에 맞춰 구현하고 검증한다.
---

# Frontend Feature

1. `docs/api-contract.md`와 `frontend/src/lib/api.ts`를 읽는다.
2. 로딩·빈 상태·성공·실패 상태를 정의한다.
3. API 호출은 화면 컴포넌트 내부에 산재시키지 않는다.
4. 사용자 입력과 예상 실패를 처리한다.
5. `npm run typecheck`, `npm run lint`, `npm run build`를 실행한다.
6. BE 필드 변경 시 DTO, FE 타입, 문서를 함께 수정한다.
