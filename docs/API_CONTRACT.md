# Initial API Contract

구현 전 세 명이 함께 검토할 초안입니다. 실제 경로와 필드는 변경할 수 있지만, 이벤트 이름은 세 모듈에서 동일하게 사용합니다.

## 공통 이벤트 타입

```text
SAFE_ZONE_EXITED
SAFE_ZONE_ENTERED
FALL_SUSPECTED
FALL_CONFIRMED_SAFE
SOS_MANUAL
SOS_AUTOMATIC
RETURN_HOME_REQUESTED
NAVIGATION_STARTED
ARRIVED_HOME
BATTERY_LOW
DEVICE_STATUS_UPDATED
```

## 공통 이벤트 예시

```json
{
  "eventId": "uuid",
  "type": "SOS_MANUAL",
  "userId": "uuid",
  "deviceId": "uuid",
  "occurredAt": "2026-08-14T15:30:00+09:00",
  "location": {
    "latitude": 37.0,
    "longitude": 127.0,
    "accuracyMeters": 15.0
  },
  "metadata": {
    "source": "WATCH"
  }
}
```

## API 초안

| Method | Path | 용도 |
| --- | --- | --- |
| `POST` | `/api/v1/pairings` | 사용자·보호자·워치 연결 |
| `POST` | `/api/v1/devices/{deviceId}/locations` | 워치 위치 업로드 |
| `GET` | `/api/v1/users/{userId}/location/latest` | 보호자 최신 위치 조회 |
| `POST` | `/api/v1/safety-events` | SOS·낙상·영역 이벤트 저장 |
| `GET` | `/api/v1/users/{userId}/safety-events` | 안전 이벤트 목록 조회 |
| `GET` | `/api/v1/users/{userId}/safe-zones` | 안전구역 목록 조회 |
| `POST` | `/api/v1/users/{userId}/safe-zones` | 안전구역 생성 |
| `PATCH` | `/api/v1/safe-zones/{safeZoneId}` | 안전구역 수정 |
| `DELETE` | `/api/v1/safe-zones/{safeZoneId}` | 안전구역 삭제 |
| `POST` | `/api/v1/users/{userId}/return-home-requests` | 보호자의 귀가 요청 |
| `POST` | `/api/v1/devices/{deviceId}/status` | 배터리·연결 상태 업로드 |

## 공통 결정 필요 항목

- 인증 방식과 토큰 만료 정책
- 워치가 휴대폰 없이 직접 서버와 통신할지 여부
- 위치 갱신 주기와 위험 상태 전환 기준
- 안전구역 판정을 워치·서버 중 어디에서 수행할지
- 이벤트 중복 전송에 사용할 idempotency key
- 위치 및 이벤트 보관 기간
