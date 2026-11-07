# 디자인 인계 초안

현재 화면은 기능 프로토타입이다. 디자인 도착 전 컴포넌트와 화면을 전부 교체 가능한 상태라고 가정하지 않는다.

- 신규 간격·터치·모서리·고도·모션 토큰 시작점: `app/src/main/java/com/example/hyundiaboysranger/ui/theme/DesignTokens.kt`.
- 색상: `AccessibleColors.kt`, `Theme.kt`, `Color.kt`. 타이포그래피: `Type.kt`와 `MainActivity.kt`의 확대 처리. 아직 단일 토큰으로 통합되지 않았다.
- 화면/데이터 연결은 `SCREEN_SPEC.md` 참고. UI와 로직이 섞인 파일이 있어 디자인 교체 전에 상태·이벤트 분리가 필요하다.
- 현재 공통 컴포넌트는 `MainActivity.kt`의 InfoCard/NavButton/BackButton 및 Material3 컴포넌트. 경고·레슨·슬라이더 컴포넌트는 별도 계층으로 미분리.
- 문자열은 일부만 `app/src/main/res/values/strings.xml`에 있다. 기존 하드코딩과 음성 대사는 추후 리소스로 이관해야 한다.
- 에셋 이름 매핑·200% 글자 화면 검증·차량 비율 검증은 미완료. 실차 안전 검증 전 출시하지 않는다.
