# Backend

Watch Safety의 Spring Boot API, MySQL 저장소, FCM 알림 계층입니다.

## 주요 도메인

- User / Guardian / Device / Pairing
- Location
- SafeZone
- SafetyEvent
- ReturnHomeRequest
- DeviceStatus

## 초기 구현 순서

1. Spring Boot 프로젝트와 환경별 설정
2. 사용자·보호자·디바이스 연결 모델
3. 수동 SOS와 최신 위치 API
4. FCM Push 전달
5. 안전 이벤트 목록
6. 안전구역과 귀가 요청

비밀 값과 서비스 계정 파일은 저장소에 커밋하지 않습니다.
