# 화면 명세 초안 (Phase 0)

현재 Compose 화면은 아직 완전히 UiState/UiEvent와 분리되지 않았다. 아래는 실제 코드 기준이며, 상태와 이벤트는 향후 분리 대상이다.

| 화면 | 목적·상태 | 이벤트·데이터 | 현재 파일 |
|---|---|---|---|
| 주차 확인 | P/속도/화면 제한을 확인, 실패 시 잠금 | ParkingEvidence 갱신, 재확인 | ParkedApp.kt, ParkingMonitor.kt |
| 홈·사용자 유형 | 기능 선택, 고령자 간단/초보자 상세 | 화면 이동·프리셋, UserSettings | MainActivity.kt |
| 설정·백업 | 글자·색상·음성 인식·설정 복원 | 저장/확인/취소 | MainActivity.kt, SettingsManagementScreen.kt, LocalSettingsBackupScreen.kt |
| 출발 전 점검 | 사용자가 수동으로 확인 | 체크/해제/초기화 | DepartureChecklist.kt |
| 경로 준비 | 기존 내비 정보·즐겨찾기와 옵션 요청 | NaviHelper 콜백/사용자 요청 | NavigationScreen.kt, NavigationSession.kt |
| 와이퍼 연습·복습 | OFF→ON→OFF·동의 기록 | SDK 상태·시작·완료·삭제 | PracticeScreens.kt, WiperReader.kt, ReviewRepository.kt |
| 차량 상태 | 문 상태·두 경고 신호 | 조회·신호 결과 | VehicleHealthScreen.kt, VehicleHealthReader.kt |

공통 컴포넌트는 아직 MainActivity 内의 InfoCard/NavButton 등으로 제한된다. 아이콘 리소스의 중앙 매핑은 미구현. 모든 화면의 200% 레이아웃과 차량 가로 비율은 미검증이다.
