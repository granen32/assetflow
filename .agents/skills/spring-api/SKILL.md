---
name: spring-api
description: AssetFlow의 Spring REST API를 계층 규칙에 맞춰 설계, 구현 및 검증한다.
---

# Spring API Workflow

1. 기존 API·DB·도메인 규칙을 읽는다.
2. 요청 DTO, 응답 DTO, 오류 코드, Service 책임을 정의한다.
3. 데이터 소유자 검증과 트랜잭션 범위를 확인한다.
4. 코드와 테스트를 함께 작성한다.
5. `gradle test`와 가능하다면 통합 테스트를 실행한다.
6. API 변경 사항을 README 또는 docs에 반영한다.
