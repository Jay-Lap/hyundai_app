# SDK 호출 및 생명주기 점검

## 2026-11-08 Vehicle SDK 안전 차단 적용

- 실제 앱의 `VehicleSdkAccess.enabled`는 모든 빌드에서 `false`다. `WiperReader`, `VehicleHealthReader`(문 위치·잠금, 안전벨트·워셔액, 네 문 점검), `VehicleDoorReader`의 Vehicle 생성은 공통 `create`를 통과하며 차단 상태에서 팩토리를 실행하지 않는다. 초기화·capability/get·release도 호출할 인스턴스가 없다. 진단의 차량 상태 화면도 같은 reader와 차단 표시를 사용한다. 기존 설치 APK는 이 변경이 반영되지 않는다.
- 화면에는 `차량 연동 안전 점검 중`을 표시하고 실신호 조회 및 와이퍼 연습 시작을 막는다. 신호 없는 와이퍼 조작 설명, 공조·김서림 설명, 경고등 사전, 저장된 복습, 수동 문 점검과 설정은 사용할 수 있다. 실제 SDK 차단과 Android Car 주차 FeatureGate는 별개이며 기어·속도·UX가 불확실하면 기존처럼 화면을 잠근다.
- 이 차단은 기존 release 경합을 고친 것이 아니다. 장래 활성화는 사용자 설정·숨겨진 버튼이 아닌 소스 변경과 검토가 필요하다. 활성화 전 공급자에게 일회성 요청 취소 방법, 동기 예외 후 콜백 가능성, 콜백 반환과 release 순서, 서비스 연결 유실 중 미응답 요청의 종료·해제, 스레드·동시성 조건을 확인하고, 미응답·화면 이탈·복구·자원 상한을 Fake와 별도 Pleos 기기에서 검증해야 한다. 계약이 불명확하면 비활성 상태를 유지한다.
- 아래 과거 기록은 차단 적용 전 동작과 기존 위험을 설명한다. 차단 후 이 코드 경로가 실행되지 않는다는 정적·Fake 근거와 실제 기기 동작은 구분한다.

## 2026-11-08 release 계약 추가 조사 — 미해결

- [비동기 조회 안내](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object)는 콜백이 끝나기 전 release하지 않도록 명시한다. [시작 예제](https://document.pleos.ai/en/docs/pleos-only/getting-started)는 콜백 처리 후 또는 SDK가 더 이상 필요하지 않은 생명주기 시점에 해제하는 예를 들지만, 미응답 요청이 남은 상태의 안전성을 보장하지는 않는다. [리스너 문서](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/callback)의 unregister는 상태 변화 구독 해제이며 일회성 capability/get 취소라는 근거는 없다.
- 설치된 Vehicle 2.3.2 소스의 `Vehicle`, `Door`, `Wiper`, `Safety` 공개 선언에서 일회성 요청의 cancel, 연결 해제 완료 알림 또는 콜백 반환 완료를 앱에 별도로 알리는 공개 메서드를 찾지 못했다. `initialize()`가 반환되었다는 사실은 서비스 연결 성공이나 미응답 요청 완료의 증거가 아니다. 소스 검색 결과는 SDK 전체의 모든 가능한 API에 대한 부재 증명이 아니다.
- 설계 모델: UI 완료·타임아웃·취소와 SDK 요청 완료를 따로 추적하고, 미완료 요청이 있을 때 닫기 요청은 대기시키며 새 요청과 인스턴스 수에 상한을 둔다. Fake는 콜백이 실제 반환된 뒤 테스트 하네스가 별도 완료 이벤트를 전달한다. 테스트에서는 모든 요청의 명시적 완료 전에 release하지 않고 미응답 요청이 상한을 차지한다. 이 Fake 전제는 실제 SDK가 제공한다고 확인되지 않았다. 콜백 안에서 완료 카운트를 줄이거나 별도 스레드에 release를 게시하는 것만으로 콜백이 반환된 순서를 증명할 수 없다.
- **실제 앱에는 이 모델을 연결하지 않았다.** 현재 세 Vehicle reader는 여전히 시간 초과·화면 이탈 등에서 미응답 콜백보다 먼저 release할 수 있고, 재조회나 화면 재진입에 대해 프로세스 전체의 미완료 요청·인스턴스 상한을 보장하지 않는다. UI의 시간 초과·세대 차단은 SDK 요청의 완료나 안전한 해제의 증거가 아니다. 안전한 계약 확인 전 실제 차량 SDK 조회의 사용을 중단하고 반복 재조회·재연결을 삼가야 한다. 프로세스 종료나 서비스 연결 해제도 이전 요청의 안전한 완료라고 단정하지 않는다.
- 해결 조건: 공급자에게 일회성 호출의 취소·요청 종료 및 동기 예외 이후 전달 가능성, 콜백 반환과 release의 순서, 연결 유실 시 미응답 요청과 해제, 스레드 요구사항을 확인해야 한다. 안전한 종료 신호가 없다면 프로세스 범위에서 제한된 수의 SDK 인스턴스를 보유하고 미응답 시 신규 요청을 중지하는 방식을 별도 설계·검증해야 하며, 자원이 남거나 이후 기능을 사용할 수 없는 한계를 UI에 알려야 한다. 아직 구현·기기 검증하지 않았다.

2026-11-08 정적 조사. 아래는 API 존재와 앱 코드 분석이며 차량 서비스 동작을 관찰한 결과가 아니다. [Vehicle 공식 안내](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object)는 initialize → 도메인 객체 → capability/get의 비동기 콜백 → release를 설명하고 콜백 완료 전 release하지 말라고 한다. [NaviHelper 초기화 안내](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/initialization-and-release)는 초기 연결 콜백 후 리스너 등록·호출, 사용 종료 시 리스너 제거 후 release, 재사용 시 새 인스턴스를 설명한다. 확인한 문서에서 호출·콜백의 필수 스레드는 특정하지 못했다. 로컬 Vehicle 2.3.2의 Vehicle/Door/Safety/Wiper와 NaviHelper 3.0.5의 NaviHelper/listener 소스에서도 확인한 스레드 어노테이션이나 명시적 메인 디스패처 표시는 없었다. 이는 전체 구현이나 스레드 안전성을 증명하지 않으며 SDK 소스는 공개 스레드 계약이 아니다.

| 호출 위치 | 초기화 및 실제 호출 | 실패·시간 초과·취소·종료 | 남은 한계 |
|---|---|---|---|
| `WiperReader`, `PracticeScreen` | Vehicle initialize, getWiper, checkWiperStateCapabilities, getCurrentWiperState; 연습 종료 release | 읽기당 2초, 실패는 SignalResult, 취소·늦은 콜백 무효화, 화면/주차 유실 시 코루틴 취소 | 동기 SDK 호출이 멈추면 타임아웃·release 시점 미보장. 타임아웃/취소 후 미응답 콜백보다 release가 먼저일 수 있어 문서의 순서와 충돌 가능 |
| `VehicleHealthReader.read`, `VehicleHealthScreen` | initialize; getDoor/getSafety; 문 위치·잠금 capability/get, 안전벨트·워셔액 경고 capability/get; 종료 release | 신호당 2.5초, 전체 12초; 도메인별 예외 격리, 코루틴 취소 및 화면 이탈 | 동기 호출 중단 불가 가능; 미응답 콜백보다 release가 먼저일 수 있음; 실제 병렬 호출 지원과 스레드 계약 미검증 |
| `VehicleHealthReader.readChecklistDoors`, `DepartureChecklist` | initialize/getDoor; 문 위치 capability와 네 영역 get; 종료 release | 신호당 2.5초, 전체 12초; 주차 유실·오래된 요청·수동 해제·화면 이탈 시 세대 무효화; 네 값 모두 닫힘일 때만 확인 | 미응답 콜백보다 release가 먼저일 수 있음; 실제 네 영역 지원·동시 호출 미검증 |
| `VehicleDoorReader`, `MainActivity` | initialize/getDoor; 운전석 문 위치 capability/get; 화면 종료 release | 10초, 실패 표시; 요청 번호·dispose·완료 플래그로 늦은 결과 차단 | 호출은 UI 스레드에서 시작하므로 동기 호출이 막히면 지연 가능; 미응답 콜백보다 release가 먼저일 수 있음 |
| `NavigationSession`, `NavigationScreen` | NaviHelper initialize(onConnected/onFailed), addListener; getRouteStateInfo/getDestinationInfo/getBookmarkInfo, changeRouteOption/requestRoute/cancelRoute; removeListener/release | 15초 후 결과 불명으로 연결 해제, 세대 번호로 이전 연결 콜백 차단; 화면 이탈·주차 잠금 시 해제; 재연결은 새 인스턴스 | 같은 연결의 이벤트에는 요청 ID가 없어 이전 요청인지 증명 불가. 옵션 콜백·재조회도 인과관계를 단정하지 않음. 탐색 완료·종료 이벤트만으로 요청 완료 판정하지 않음 |

주차 상태는 Pleos Vehicle이 아닌 `AndroidCarParkingMonitor`와 `VehicleStateHub`에서 읽으며, 기어·속도·화면 허용 중 하나라도 불명확하거나 2.5초보다 오래되면 FeatureGate가 잠긴다. 화면은 주차 증거 유실 시 제거된다. 코드가 요청한 취소와 SDK 내부의 IPC 취소·완전한 자원 해제는 다르다. 콜백이 끝내 오지 않을 때 안전한 release/취소 방법과 스레드 요구사항을 공급자 문서 또는 실환경으로 확인하기 전에는 완전히 해결됐다고 주장하지 않는다.
