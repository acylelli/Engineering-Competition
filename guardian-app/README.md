# Guardian Android App

보호자·가족이 워치 사용자의 상태와 위치를 확인하고 대응하는 Android 앱입니다.

## 주요 화면

- Guardian Home
- Current Location Map
- Safe Zone Management
- SOS Alert
- Safety Event History
- Return Home Request
- User / Watch Status

## 홈 화면 필수 정보

- 현재 안전 상태
- 마지막 위치와 갱신 시각
- 워치 배터리
- 안전구역 상태
- 최근 긴급 이벤트

## 구현 현황

### 완료: 1단계 디자인 시스템

- Safe / Warning / Emergency 상태 색상
- 타이포그래피, 간격, 모서리 토큰
- 공통 카드, 상태 배지, 상단바
- 일반 및 긴급 행동 버튼

### 완료: 2단계 앱 구조

- 홈, 지도, 이벤트, 설정 하단 내비게이션
- 안전구역 목록·추가, SOS, 사용자 상태 경로 등록
- 기능별 UI 패키지 분리
- 실제 화면 구현 전 플레이스홀더 화면 구성

### 완료: 3단계 화면 구현

- 홈 대시보드 UI와 화면 전용 Mock 상태 완료
- 안전 상태, 워치 배터리·착용 상태, 빠른 메뉴, 최근 이벤트 구현
- 홈 빠른 메뉴와 지도·안전구역·이벤트 경로 연결
- Mock 지도 기반 현재 위치와 귀가 요청 UI
- 안전구역 목록, 알림 스위치, 구역 추가 폼과 반경 설정
- SOS 긴급 상세, 발생 타임라인, 전화·지도 액션
- 날짜별 이벤트 기록과 유형 필터
- 설정, 알림 토글, 사용자·워치 상태 상세
- 각 화면 Compose Preview 제공

### 완료: 4단계 Mock 데이터 연결

- 공통 Domain 모델과 `GuardianSnapshot` 정의
- `GuardianRepository` 계약과 `MockGuardianRepository` 구현
- 앱 범위 `GuardianViewModel` 및 `StateFlow` 상태 관리
- 안전구역 추가·알림 스위치 상태를 화면 간 공유
- 귀가 요청 시 요청 상태와 이벤트 기록 갱신
- 알림 설정 변경값 유지
- 위치·워치 상태 새로고침 Mock 동작
- Repository 상태 변경 단위 테스트

### 다음 연동 순서

1. 실제 지도 SDK 연결
2. 서버 API와 ViewModel 연결
3. FCM 긴급 알림 연결
4. 워치 위치·배터리·안전 이벤트 실데이터 연결

## 현재 초기 설정

- Android Studio 2026.1.1 호환 Gradle 프로젝트
- Kotlin 및 Jetpack Compose
- 패키지: `com.watchsafety.guardian`
- `minSdk 26`, `compileSdk 37`, `targetSdk 36`
- 디자인 시안의 기본 Safe / Emergency / Primary 색상 토큰

Android Studio에서 `guardian-app` 폴더를 프로젝트로 엽니다. SDK는 Android Studio에 설치된 Android SDK를 사용하고, Gradle JDK는 `Embedded JDK`로 설정합니다.

```powershell
.\gradlew.bat assembleDebug
```

Firebase와 지도 SDK는 최종 패키지명 확정 후 연결합니다. `google-services.json`과 API 키는 Git에 커밋하지 않습니다.
