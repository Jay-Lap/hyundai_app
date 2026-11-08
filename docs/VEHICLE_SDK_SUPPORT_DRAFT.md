# Pleos Connect Vehicle SDK 2.3.2 기술 문의 초안

상태: 검토용 초안. 아직 발송하지 않았습니다.

## 제목

Vehicle SDK 2.3.2 비동기 조회의 취소·해제 계약 및 오류 진단 문의

## 확인된 환경과 사실

- Android 앱에서 `ai.pleos.playground:Vehicle:2.3.2`를 사용합니다. 설치된 Gradle 메타데이터에서는 `vehicle-data:1.1.11`이 런타임 의존성으로 표시됩니다.
- [공식 도메인 객체 안내](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object)는 capability/get 결과가 비동기 콜백으로 전달되며 콜백 완료 전에 `release()`하지 않도록 설명합니다. [콜백 안내](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/callback)의 unregister는 상태 변화 구독에 대한 안내입니다.
- 확인한 설치 SDK 공개 선언과 문서에서 일회성 capability/get 요청의 취소나 미응답 요청이 완전히 종료됐음을 확인하는 방법은 찾지 못했습니다. 이는 그러한 방법이 없다는 단정이 아닙니다.
- 과거 Pleos 에뮬레이터 조회에서 네 문의 잠금 지원 영역이 보고되지 않았고, 안전벨트·워셔액 조회는 일반 오류로 표시됐습니다. 오류의 원인과 실제 차량 동작은 확인하지 못했습니다. 현재 앱 소스는 안전 점검을 위해 Vehicle 조회를 기본 차단하며, 이미 설치된 이전 APK에는 이 차단이 적용되지 않습니다.
- [공식 오류 안내](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-error-code)는 `onFailed`가 `Exception`을 전달하고 `VehicleException`이면 `errorCode`를 확인하는 방법을 설명합니다. 현재 설치된 2.3.2 컴파일 경로에서는 해당 예외·오류 코드 타입을 문서대로 참조하지 못했습니다. SDK 내부 구현에는 의존하지 않고 있습니다.

## 확인 부탁드리는 사항

1. Vehicle SDK 2.3.2에서 capability/get의 `onComplete` 또는 `onFailed`가 오지 않을 때 지원되는 **공개 취소 API**가 있나요? 취소 성공과 차량 서비스 측 요청의 실제 종료를 각각 어떤 신호로 확인할 수 있나요? 동기 호출이 예외를 던진 뒤에도 콜백이 도착할 수 있나요?
2. 화면 이탈, 앱 측 타임아웃, 코루틴 취소 또는 서비스 연결 해제 시 미응답 요청이 있으면 `release()`를 안전하게 호출할 수 있는 정확한 조건은 무엇인가요? 연결 종료 알림만으로 모든 요청이 끝났다고 판단할 수 있나요?
3. 마지막 `onComplete`/`onFailed` **내부에서** `release()`를 호출해도 되나요? 콜백이 반환된 이후여야 한다면 앱이 반환 완료를 관찰하고 release할 수 있는 공개 방법, 필요한 실행 스레드 및 예제를 알려주세요.
4. 서비스가 복구되지 않거나 콜백이 오지 않을 때 신규 요청과 Vehicle 인스턴스 수를 어떻게 제한하고 언제 재연결해야 하나요? 여러 capability/get 동시 호출 및 앱 종료 시 권장되는 자원 관리 계약도 알려주세요.
5. 문 잠금 capability의 지원 영역 목록에 문 영역이 보고되지 않을 때, 미지원·권한·서비스 상태를 구분할 수 있는 공개 API와 확인 순서는 무엇인가요? 안전벨트 미착용 경고와 워셔액 부족 경고의 일반 오류도 어떤 공개 capability, 권한, 오류 정보로 진단할 수 있나요? 빈 영역이나 일반 오류를 특정 원인으로 단정해도 되는 조건이 있나요?
6. 2.3.2에서 `VehicleException.errorCode`를 사용하기 위한 지원되는 Gradle 의존성 선언과 공개 패키지·타입은 무엇인가요? 설치된 배포본과 문서의 타입 가시성이 다를 때 권장 진단 방법을 알려주세요. 예외 원문이나 차량 정보 없이 공유할 수 있는 진단 항목도 안내 부탁드립니다.

가능하면 버전이 명시된 공식 문서 또는 최소 재현 코드와 함께 답변 부탁드립니다. 실제 차량 신호나 요청 완료 여부는 확인된 것으로 가정하지 않고, 계약 확인과 별도 테스트 전까지 안전 차단을 유지하겠습니다.
