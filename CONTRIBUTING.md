# Contributing

이 저장소는 3인 팀 개발을 기준으로 운영합니다.

## 권장 역할

- Wear OS: 센서, 낙상 판정, 워치 UI, GPS
- Guardian Android: 지도, 안전구역 관리, 알림·이벤트 UI
- Backend: API, MySQL, FCM, 위치·이벤트 저장

공통 API와 이벤트 스키마는 한 명이 임의로 변경하지 않고 PR에서 함께 검토합니다.

## 브랜치

- `main`: 통합 및 시연 가능한 상태
- `feat/<module>-<topic>`: 기능 개발
- `fix/<module>-<topic>`: 버그 수정
- `docs/<topic>`: 문서 수정

예시:

```text
feat/wear-fall-detection
feat/guardian-safe-zone-map
feat/backend-sos-api
```

## 커밋

```text
feat(wear): add manual SOS countdown
feat(backend): persist safety events
fix(guardian): show stale location timestamp
docs: define SOS event payload
```

## Pull Request 규칙

1. 기능 하나를 작은 PR 하나로 제출합니다.
2. 변경 이유와 테스트 방법을 작성합니다.
3. API 또는 이벤트 형식 변경 시 `docs/API_CONTRACT.md`를 함께 수정합니다.
4. 실제 기기 동작이 필요한 기능은 기기명과 결과를 기록합니다.
5. 최소 한 명의 팀원 리뷰 후 `main`에 병합합니다.

## 완료 기준

- 정상 흐름뿐 아니라 네트워크·GPS·권한 오류 상태를 처리합니다.
- 로그에 민감한 위치 정보나 인증 값을 남기지 않습니다.
- 데모 시나리오와 관련된 기능은 실제 워치·스마트폰 통합 테스트를 수행합니다.
