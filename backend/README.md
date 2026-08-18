# Backend

Watch Safety의 1차 백엔드는 Supabase로 구성합니다. PostgreSQL, Auth, Row Level Security(RLS), Realtime을 보호자 앱과 연결하며, FCM 푸시는 후속 단계에서 별도로 추가합니다.

## 주요 도메인

- User / Guardian / Device / Pairing
- Location
- SafeZone
- SafetyEvent
- ReturnHomeRequest
- DeviceStatus

## 현재 구현

- 서울 리전 Supabase 프로젝트 생성
- 보호자·사용자·워치·위치·안전구역·안전 이벤트·귀가 요청·알림 설정 테이블
- 보호자별 데이터 격리를 위한 RLS 정책
- 위치, 워치 상태, 안전구역, 이벤트, 귀가 요청, 알림 설정 Realtime 발행
- 앱 첫 실행용 데모 데이터 생성 함수
- 스키마 원본: [`supabase/schema.sql`](supabase/schema.sql)

## 후속 작업

1. 임시 익명 로그인을 실제 이메일/소셜 로그인으로 교체
2. Wear OS 앱과 사용자·워치 페어링 규칙 확정
3. FCM 긴급 푸시와 서버 측 이벤트 처리
4. 실제 위치 수집·안전구역 판정 로직 연결

비밀 값과 서비스 계정 파일은 저장소에 커밋하지 않습니다.
