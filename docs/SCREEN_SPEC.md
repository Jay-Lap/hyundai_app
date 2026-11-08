# 화면 명세 초안 (Phase 0)

현재 Compose 화면은 아직 완전히 UiState/UiEvent와 분리되지 않았다. 아래는 실제 코드 기준이며, 상태와 이벤트는 향후 분리 대상이다.

| 화면 | 목적·상태 | 이벤트·데이터 | 현재 파일 |
|---|---|---|---|
| 주차 확인 | P/속도/화면 제한을 확인, 실패 시 잠금 | ParkingEvidence 갱신, 재확인 | ParkedApp.kt, ParkingMonitor.kt |
| 홈·사용자 유형 | 기능 선택, 고령자 간단/초보자 상세 | HomeDestination 화면 이동·프리셋, UserSettings | MainActivity.kt |
| 설정·백업 | 글자·색상·음성 인식·설정 복원 | 저장/확인/취소, 초기화 확인 상태·이벤트 | MainActivity.kt, SettingsManagementScreen.kt, LocalSettingsBackupScreen.kt |
| 출발 전 점검 | 수동 확인과 네 문 조회의 SDK 확인 구분 | 체크/해제/초기화·주차 중 문 조회 | DepartureChecklist.kt, DoorChecklist.kt |
| 경로 준비 | 기존 내비 정보·즐겨찾기와 옵션 요청 | NaviHelper 콜백/사용자 요청 | NavigationScreen.kt, NavigationSession.kt |
| 와이퍼 연습·복습 | OFF→ON→OFF·동의 기록, SDK 조회는 안전 차단 중 | SDK 상태·시작·완료·삭제, 저장된 경고 항목 열기/삭제 이벤트 | PracticeScreens.kt, WiperReader.kt, ReviewRepository.kt |
| 차량 상태 | 문 상태·두 경고 신호 | 조회·신호 결과·화면 상태/이벤트 | VehicleHealthScreen.kt, VehicleHealthReader.kt, VehicleHealthUiState.kt |
| 경로 비교 | 실제 후보가 없으면 확인 불가, demo에는 가상 예시 | 선호·공급자 후보·임시 점수 | RouteComparisonScreen.kt, RouteComparison.kt |
| 경고 사전 학습 | 일반 학습 자료, 실시간 감지 아님 | WarningCard 상태·추가 이벤트, 사용자가 선택한 복습 추가 | WarningCatalogScreen.kt, WarningCatalog.kt, res/raw/warning_catalog.tsv |

추가 레슨은 공조·김서림의 일반 설명을 열 수 있지만 신호가 확인되지 않아 완료 판정은 잠금 표시한다. 출발 전 점검에서 복습과 경로 준비로 이동할 수 있다. 차량 상태·추가 레슨·경고 카드에는 일부 상태·이벤트를 분리했으며 전체 이관은 완성되지 않았다.

공통 컴포넌트는 아직 MainActivity 内의 InfoCard/NavButton 등으로 제한된다. 아이콘 리소스의 중앙 매핑은 미구현. 모든 화면의 200% 레이아웃과 차량 가로 비율은 미검증이다.
