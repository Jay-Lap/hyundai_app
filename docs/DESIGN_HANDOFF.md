# 디자인 인계 초안

현재 화면은 기능 프로토타입이다. 디자인 도착 전 컴포넌트와 화면을 전부 교체 가능한 상태라고 가정하지 않는다.

- 신규 간격·터치·모서리·고도·모션 토큰 시작점: `app/src/main/java/com/example/hyundiaboysranger/ui/theme/DesignTokens.kt`.
- 색상: `AccessibleColors.kt`, `Theme.kt`, `Color.kt`. 타이포그래피: `Type.kt`와 `MainActivity.kt`의 확대 처리. 아직 단일 토큰으로 통합되지 않았다.
- 화면/데이터 연결은 `SCREEN_SPEC.md` 참고. UI와 로직이 섞인 파일이 있어 디자인 교체 전에 상태·이벤트 분리가 필요하다.
- 차량 상태 조회와 공조·김서림 안내에는 상태·이벤트와 일부 문자열·터치 크기 토큰을 적용했다. 나머지 화면 전체의 분리나 실제 화면 검증이 완료된 것은 아니다.
- 홈 메뉴의 목적지와 문자열, 출발 전 점검의 일부 문구·터치 크기를 자원과 토큰으로 분리했다. 경고 카드의 저장 가능 상태와 추가 이벤트·표시를 분리하고 각 버튼에 항목 이름을 포함한 접근성 이름을 부여했다. 200%·좁은 폭의 계측 테스트는 컴파일만 했다.
- `SettingsManagementScreen.kt`의 설정 파일 이동과 초기화 확인 UI, `PracticeScreens.kt`의 저장 경고 항목과 복습 동의·조작 설명 일부를 문자열·터치 토큰과 상태/이벤트로 분리했다. 항목별 복습 버튼은 이름을 포함하고 공조·김서림 완료 잠금은 레슨별 접근성 이름을 갖는다. 280×220dp/200% 관련 계측 테스트는 컴파일만 했으며 실제 잘림·스크린리더는 미검증이다.
- 체크포인트 후 `SettingsTextSizeControl.kt`로 슬라이더 임시 상태를 분리하고 음성 안내·내부 백업 확인/결과·설정/복습 기록 불명 안내와 복습 이력 문구를 일부 리소스로 옮겼다. `SettingsAppearanceControls.kt`는 대비·모드·상세도의 표시와 변경 이벤트를, `PracticeScreens.kt`는 복습 삭제 확인 UI의 확인/취소 이벤트를 분리한다. 오늘 복습 항목·경고 사전 동의·내비 연결 제어·점검 조회 결과의 표시도 분리하고 일부 문자열·터치 토큰을 적용했다. `NavigationSession` 결과 안내·차량 상태·주차와 음성 기본 CompositionLocal 등에는 하드코딩이 남는다. 200% 좁은 화면 테스트는 컴파일만 했으며 실제 기기·스크린리더 검증은 미실행이다.
- 현재 공통 컴포넌트는 `MainActivity.kt`의 InfoCard/NavButton/BackButton 및 Material3 컴포넌트. 경고·레슨·슬라이더 컴포넌트는 별도 계층으로 미분리.
- 문자열은 일부만 `app/src/main/res/values/strings.xml`에 있다. 기존 하드코딩과 음성 대사는 추후 리소스로 이관해야 한다.
- 에셋 이름 매핑·200% 글자 화면 검증·차량 비율 검증은 미완료. 실차 안전 검증 전 출시하지 않는다.
- 경로 비교 임시 화면은 `RouteComparisonScreen.kt`, 실제/시연 후보 분리는 `RouteComparison.kt`와 `src/demo/.../VariantRouteProvider.kt`이다. 시연 후보를 실서비스 화면에 옮기지 않는다.
- 경로 준비의 `PreparationConcern.kt`는 사용자가 직접 선택한 학습 주제를 김서림·와이퍼 설명과 경고 사전으로 연결한다. 실제 경로 분석·차량 신호가 아니며 선택은 복습 기록이 아니다. 200% 좁은 폭의 선택 계측 테스트는 컴파일만 했고 기기 화면은 미검증이다.
- 경고 사전은 `WarningCatalogScreen.kt`, 자료 초안은 `res/raw/warning_catalog.tsv`이다. 차종별 매뉴얼 검증 전 일반 학습 자료로 표시한다.
