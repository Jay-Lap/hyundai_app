# 기능 및 출시 준비 상태 — Phase 0 진행 중

## 2026-11-08 주차 제한 화면 조회값 표현 보강

- 사용자 터미널의 이전 화면 빌드 성공 이후 독립 후속 변경: 주차 제한 화면의 기어·화면 사용 여부를 `true`/`false` 대신 `P단`/`P단 아님`, `허용`/`제한`으로 표시하고 미확인은 그대로 둔다. 관측 시각이 있는 값에는 마지막 조회값이며 현재와 다를 수 있다는 안내를 추가했다. UI 표현만 바꾸었으며 주차 FeatureGate와 차량 신호 로직은 변경하지 않았다.
- 표시 전용 Compose 테스트에 불리언 표시와 마지막 조회값 안내 검증을 추가하고 기존 미확인 표시 테스트를 수정했다. 사용자 데이터·실제 SDK를 읽지 않는다. 문자열 XML 파싱·리소스 중복 확인과 `git diff --check` 통과.
- 이전 `parking-notice-ui-init.gradle` 사용자 성공 보고는 이 후속 변경 전 결과다. 후속 변경의 단위 테스트·debug/demo/release 빌드·계측 APK 컴파일·lint는 미실행이며, 기존 에이전트 Gradle 접근 거부를 반복하지 않았다. 사용자 터미널의 별도 출력용 `build/parking-readable-status-init.gradle`을 준비했다. 실제 화면·200%·스크린리더·기기와 실차는 미검증이다.

## 2026-11-08 사용자 터미널 주차 제한 화면 검증

- **사용자 보고:** Android Studio Terminal에서 `parking-notice-ui-init.gradle`을 사용한 별도 출력의 `testDebugUnitTest`, `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug`가 `BUILD SUCCESSFUL`로 종료됐다. 에이전트가 직접 실행하거나 독립 확인한 결과는 아니다. 정확한 테스트 수와 lint 경고 수는 미확인이다.
- 계측 테스트는 APK 컴파일만 했고 기기에서는 실행하지 않았다. 실제 화면·200%·스크린리더 검증, 주차 속성 신선도 및 실차 안전성은 미검증이다.

## 2026-11-08 주차 제한 안내 화면 정리

- 독립 화면 작업: 주차 제한 화면의 문구를 문자열 리소스로 이관하고 표시 부분을 `ParkingLockedNotice`로 분리했다. 좁은 화면에서 스크롤할 수 있게 했으며, 선택 라이브러리 미확인과 호환성 오류가 같은 경로로 들어오므로 특정 Pleos 이미지에 라이브러리가 없다고 단정하던 문구를 차량 연결 기능을 확인하지 못했다는 안내로 바꿨다. 주차 판정·신호 조회·Vehicle SDK 안전 차단은 변경하지 않았다.
- 280×220dp·글자 200%에서 미확인 상태와 안내에 스크롤로 접근하는 표시 전용 Compose 테스트를 작성했다. 사용자 저장소·차량 SDK는 사용하지 않으며 기존 주차 신호 Fake 시나리오를 반복하지 않는다.
- 문자열 XML 파싱·신규 이름 중복 확인과 `git diff --check` 통과. 이 변경 후 테스트·debug/demo/release 빌드·계측 APK 컴파일·lint는 미실행이다. 기존 에이전트 Gradle 배포본 접근 거부를 반복하지 않았으며 사용자 터미널 별도 출력용 `build/parking-notice-ui-init.gradle`을 준비했다. 실제 화면·200%·스크린리더·기기·실차 검증은 미실행이다. 사용자 데이터와 기존 APK는 변경하지 않았다.

## 2026-11-08 사용자 터미널 가상 주차 계약 검증

- **사용자 보고:** Android Studio Terminal에서 `parking-hypothesis-check-init.gradle`을 사용한 별도 출력의 `testDebugUnitTest`, `assembleDebug`, `assembleDemo`, `assembleRelease`, `lintDebug`가 `BUILD SUCCESSFUL`로 종료됐다. 에이전트가 직접 실행하거나 독립 확인한 결과는 아니다. 정확한 테스트 수와 lint 경고 수는 확인되지 않았다.
- 기기 테스트와 앱 설치는 하지 않았다. Fake의 계약·연결 감시는 가정이며 운영 FeatureGate에 연결되지 않았다. 실제 Pleos 신호·연결 계약과 실차 안전은 미확인이다.

## 2026-11-08 주차 신선도 Fake 가상 계약과 감시

- `ParkingFreshnessModelTest.kt`에 가상의 검증된 신호 계약, 활성 연결 감시, 감시·증거의 연결 세대를 별도로 주입했다. 해당 가정과 P·정지·UX 허용이 모두 맞을 때만 테스트 전용 게이트가 열리며 D단·이동·오류·연결 유실·감시 중단에서 다시 잠기도록 시험을 작성했다. 새 연결은 이전 세대의 가상 증거를 재사용하지 않는다. 계약·감시·증거가 없으면 확인 안 됨이다.
- 이는 테스트 Fake가 받는 가정이며 실제 Pleos의 timestamp·신호 검증·연결 감시 계약을 확인했다는 뜻이 아니다. ONCHANGE 기어가 변화 없이 주기적으로 갱신된다고 가정하지 않았고 운영 FeatureGate와 Vehicle SDK 동작은 바꾸지 않았다. 운영 게이트의 속성 시각·연결 경합 및 실차 안전은 미검증이다.
- 별도 출력의 `testDebugUnitTest assembleDebug assembleDemo assembleRelease lintDebug`를 한 번 시도했으나 구성 중 Gradle 배포본 JAR 접근 거부로 종료 코드 1. 컴파일·단위 테스트·빌드·lint 태스크는 시작되지 않았다. 재시도·권한 우회·기기 접근은 하지 않았고 별도 출력용 `build/parking-hypothesis-check-init.gradle`을 준비했다.

## 2026-11-08 사용자 터미널 신선도 Fake 수정 검증

- **사용자 보고:** Android Studio Terminal에서 `parking-freshness-syntax-fix-init.gradle`을 사용한 별도 출력 검증이 `BUILD SUCCESSFUL`로 종료됐다. `testDebugUnitTest`, `assembleDebug`, `assembleDemo`, `assembleRelease`, `lintDebug` 통과. 에이전트가 직접 실행하거나 독립 확인한 결과는 아니다.
- 정확한 테스트 수와 lint 경고 수는 사용자 보고에서 확인되지 않았다. 기기 실행과 앱 설치는 하지 않았으며, Fake 통과는 실제 Pleos 신호의 신선도나 실차 안전 확인이 아니다.
- 독립 소스 대조로 테스트 모델은 모든 결과를 차단하며 운영 FeatureGate와 연결되지 않았음을 재확인했다. 기존 게이트의 2.5초 테스트는 앱의 폴링 증거 시각만 대상으로 한다. 200ms 화면 재평가 트리거가 실제 기기의 잠금 지연 상한을 보장하는지도 미검증이다. 구체적인 테스트 공백은 `PARKING_FRESHNESS_VALIDATION.md`에 기록했다.

## 2026-11-08 주차 신선도 Fake 테스트 구문 수정

- 사용자 터미널에서 `parking-freshness-model-init.gradle` 검증이 `BUILD FAILED`로 끝났고 미래 속성 시각 테스트의 반복문 주변 구문 오류가 보고됐다. 테스트의 `for` 괄호 누락을 확인해 두 미래 시각 사례를 별도 목록으로 분리했다. 테스트 시나리오와 보수적 결과를 유지했고 운영 `FeatureGate`에 연결하지 않았다.
- 수정 후 에이전트의 별도 출력 검증은 Gradle 구성 단계에서 `GeneratedClassCompilationException` 및 배포본 JAR의 `AccessDeniedException`으로 종료 코드 1이었다. 테스트·빌드·lint 태스크가 시작되지 않아 수정 후 컴파일과 테스트 결과는 미확인이다. 접근 거부 후 반복·우회하거나 기기에 접근하지 않았다. 사용자 터미널의 재검증용 설정은 `build/parking-freshness-syntax-fix-init.gradle`이다.

## 2026-11-08 주차 신선도 테스트 전용 Fake 모델

- `ParkingFreshnessModelTest.kt`에 기어·속도의 값·상태·timestamp, 조회 시작/완료 시각, 조회 전·중·후 및 현재 연결 상태와 세대, UX 제한을 따로 주입하는 테스트 전용 Fake를 작성했다. P·정지라도 신선도 계약이 없어 확인 안 됨이고, D·이동·불가/오류, 오래된 시각, 미래 시각, 서로 다른 속성 시각, 조회 중·직후 연결 유실과 재연결을 모델링한다. ONCHANGE 기어의 동일 timestamp를 오래된 것으로 단정하지 않는다.
- 이 모델은 실험적 판정만 표현하며 운영 `FeatureGate`에 연결되지 않았다. 운영 코드는 여전히 속성 timestamp를 읽지 않으므로 오래된 속성·조회 직후 연결 유실을 감지하거나 그때 잠긴다고 보장하지 않는다. Fake는 폴링 중 연결 변화가 관찰된다고 가정하며 Android Car가 해당 감지를 제공한다는 증거가 아니다.
- 작성한 테스트·빌드는 아직 실행하지 못했다. 직전 에이전트 환경의 Gradle 배포본 접근 거부를 반복하거나 우회하지 않았고 `build/parking-freshness-model-init.gradle`만 사용자 터미널의 별도 출력용으로 준비했다. 실제 차량 데이터·앱 데이터·설치 APK·기기 접근은 없다. 실차 신선도·안전은 미검증이다.

## 2026-11-08 사용자 터미널 주차 폴링 검증

- **사용자 보고:** Android Studio Terminal에서 `parking-poll-check-init.gradle`을 사용한 별도 출력 검증이 `BUILD SUCCESSFUL`로 종료됐다. `testDebugUnitTest`, `assembleDebug`, `assembleDemo`, `assembleRelease`, `lintDebug`가 통과했다. 에이전트가 직접 실행하거나 독립 확인한 결과는 아니다. 테스트 수와 lint 경고 수는 보고되지 않았다.
- 해당 명령에는 계측 테스트 실행·기기 실행·앱 설치가 포함되지 않았다. 실제 차량 속성 timestamp의 신선도와 조회 직후 연결 유실은 여전히 미검증이다. 이 결과는 속성별 timestamp를 처리하거나 그 불확실성에서 FeatureGate가 잠긴다는 증거가 아니다.
- 독립 후속 작업으로 `PARKING_FRESHNESS_VALIDATION.md`에 공급자 확인 사항과 별도 Fake·기기 검증 계획을 정리했다. 실행 결과가 아닌 계획이며 코드·사용자 데이터·설치 APK를 변경하거나 기기에 접근하지 않았다.

## 2026-11-08 주차 증거 폴링 시간 보강

- Android Car 기어·속도·UX 조회의 `observedAt`을 완료 시각에서 시작 시각으로 바꿔 느린 조회에 기존 2.5초 FeatureGate 한도를 적용한다. 조회 완료 시 연결이 끊겼거나 시작·완료 시각 순서가 잘못되면 미상으로 처리한다. Fake 테스트를 작성했으나 아직 실행하지 않았다.
- 공식 문서상 기어는 ONCHANGE, 속도는 CONTINUOUS이며 속성 timestamp는 `elapsedRealtimeNanos()` 기준이다. 변화 없는 기어의 timestamp 갱신과 Pleos 동작은 미확인이라 시간만으로 기어의 신선도를 판단하지 않았다. 구현은 속성별 timestamp를 읽지 않으므로 오래된/미래/서로 다른 속성 시각 및 조회 직후 연결 유실을 탐지하지 못하며, 이 불확실성에서 게이트가 허용되지 않는다고 보장할 수 없다. 상세 근거와 시험 누락은 `SDK_CALL_AUDIT.md` 참조.
- 기존 Gradle 배포본 접근 거부를 반복하지 않아 이번 단위 테스트·debug/demo/release 빌드·lint는 미실행이며 `build/parking-poll-check-init.gradle`은 사용자 터미널용 별도 출력 설정이다. ADB·에뮬레이터·실차와 실제 차량 신호는 미실행이다. `getStatus()` 및 기존 데이터·설정·APK는 바꾸지 않았다.

## 2026-11-08 주차 API 경고 독립 조사

- `AndroidCarParkingMonitor.kt`의 45·46행은 기어·속도 `CarPropertyValue.status`가 `STATUS_AVAILABLE`일 때만 값을 쓴다. 공식 [CarPropertyValue 문서](https://developer.android.com/reference/android/car/hardware/CarPropertyValue)는 `getStatus()`의 대안으로 `getPropertyStatus()`를 제시하며, 상태가 사용 가능일 때만 값을 사용하라고 설명한다. 프로젝트가 참조하는 Android 37 `android.car.jar`를 `javap`으로 조회했을 때 두 메서드 선언은 출력됐지만 JAR 접근 거부도 발생해 조회 전체가 성공한 것은 아니다. Pleos 런타임의 메서드 지원과 동등 동작은 미확인이므로 주차 코드는 바꾸지 않았다.
- `ParkingMonitor.kt`의 `PrivateApi`는 선택 공유 라이브러리 `android.car`의 `Car` 존재를 `Class.forName`으로 확인하는 행에 발생한다. 매니페스트는 `android:required="false"`이고, 클래스가 없거나 링크 오류가 나면 빈 주차 증거로 잠긴다. Android 공식 [`uses-library` 문서](https://developer.android.com/guide/topics/manifest/uses-library-element)는 선택 라이브러리의 런타임 존재 확인에 리플렉션을 사용할 수 있다고 설명한다. 이 일반 지침만으로 Pleos의 클래스 노출·호환성이나 lint 경고 해소를 보증하지 않는다.
- 기어·속도 조회는 IO에서 실행되며 [CarPropertyManager 문서](https://developer.android.com/reference/android/car/hardware/property/CarPropertyManager)는 동기 조회에 수 초가 걸릴 수 있고 메인 스레드 밖에서 실행해야 한다고 안내한다. 현재 관측 시각은 속성 자체의 timestamp가 아니라 읽기 완료 시각이다. 실제 속성 시각과 지연·연결 유실의 영향은 기기 검증이 필요하며 신선도 안전성을 이 조사만으로 보증하지 않는다.
- 이번 조사는 문서·소스·기존 lint 보고서 읽기이며 신규 빌드·테스트·기기 실행은 하지 않았다. 다음 작업은 Pleos 설치 이미지에서 대체 API의 존재·상태 의미·시간 기준과 불확실 시 잠금을 검증할 수 있는 계약 및 안전한 테스트 환경 확인이다. 현재 경고와 FeatureGate 동작은 유지한다.

## 2026-11-08 사용자 터미널 복습 스냅샷 검증

- **사용자 보고:** Android Studio Terminal에서 `review-copy-visibility-init.gradle`을 사용한 별도 출력 검증이 `BUILD SUCCESSFUL`로 종료됐다. `testDebugUnitTest`, `assembleDebug`, `assembleDemo`, `assembleRelease`, `lintDebug`가 통과했고 `ReviewRepository`의 `copy()` 경고는 출력되지 않았다. `AndroidCarParkingMonitor`의 deprecated 경고는 남았다. 에이전트가 직접 실행하거나 결과를 독립 확인한 것은 아니다.
- 계측 테스트와 기기 실행은 이번 명령에 포함되지 않았다. 테스트 수와 lint 경고 수는 이번 보고에 없어 미확인이다. 기존 에이전트 접근 거부 기록은 당시 결과로 유지한다.

## 2026-11-08 복습 스냅샷 Kotlin copy 가시성 점검

- `ReviewRepository.kt`의 `ReviewRecoverySnapshot`은 `internal` 생성자를 가진 data class다. 생성자 자체의 내용 검증은 없으며, 손상된 데이터 미리보기 후 초기화할 때 저장 상태가 여전히 손상 상태이고 현재 값이 스냅샷과 동일한지 비교한다. 소스·테스트 전체에서 이 타입의 `copy()` 호출은 찾지 못했다. 기존 `Map` 내용과 비교 방식은 변경하지 않았다.
- Kotlin 2.2.10 표준 라이브러리에 포함된 `@ConsistentCopyVisibility`를 이 선언에만 적용해 생성된 `copy()`의 가시성을 생성자와 일치시킨다. 이는 전역 경고 억제가 아니라 해당 언어 동작을 선택하는 것이다. 외부 모듈에서 이전 공개 `copy()`에 의존했다면 소스·바이너리 호환성 영향이 있을 수 있으나 이 프로젝트 내 호출은 발견되지 않았다. 복습 저장·마이그레이션·손상/미래 버전·쓰기 결과 불명 처리 코드는 바꾸지 않았다.
- 변경 후 `git diff --check`는 통과했다. 직전 에이전트 Gradle 실행에서 배포본 JAR 접근 거부가 확인된 환경이므로 같은 실패를 반복하거나 제한을 우회하지 않았고 이번 단위 테스트·debug/demo/release 빌드·lint는 미실행이다. 사용자 터미널용 별도 출력 init 스크립트 `build/review-copy-visibility-init.gradle`을 준비했으며 빌드 결과와 경고 제거 여부는 재검증이 필요하다. 사용자 데이터·기존 APK와 기기는 건드리지 않았다.
- `AndroidCarParkingMonitor.kt`의 deprecated API는 주차 안전·공식 API 호환성 조사 전이므로 변경하지 않았으며 경고로 남는다. 실제 기기 및 계측 테스트는 미실행이다.

## 2026-11-08 사용자 터미널 와이퍼 안내 수정 검증

- **사용자 보고:** Android Studio Terminal에서 `wiper-import-fix-init.gradle`을 사용한 별도 출력 검증이 `BUILD SUCCESSFUL`로 종료됐다. `testDebugUnitTest`, `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug` 통과. 에이전트가 직접 실행하거나 결과를 독립 확인한 것은 아니다. 테스트 수와 lint 경고 수는 이번 보고에 없어 미확인이다.
- `assembleDebugAndroidTest`는 계측 테스트 APK 컴파일 결과이며 계측 테스트의 기기 실행은 미실행이다. 앱 설치도 하지 않았다. 실제 화면·접근성·차량 동작의 통과를 의미하지 않는다. 아래 에이전트의 기존 접근 거부 기록은 당시 결과로 유지한다.

## 2026-11-08 와이퍼 안내 계측 테스트 import 수정

- 사용자 로컬 Android Studio Terminal의 별도 출력 빌드에서는 Gradle 구성이 진행됐으나 `WiperStartControlUiTest.kt`의 `assert` 미해결 참조로 테스트 컴파일이 실패했다고 보고됐다. 설치된 Compose UI 테스트 1.10.4 소스에서 해당 API가 `androidx.compose.ui.test`의 `SemanticsNodeInteraction` 확장 함수임을 확인해 테스트에 import만 추가했다. 앱 동작과 SDK 차단 조건은 변경하지 않았다.
- 에이전트는 별도 `build/wiper-import-fix-output`을 지정해 `testDebugUnitTest assembleDebug assembleDemo assembleRelease assembleDebugAndroidTest lintDebug --offline --no-daemon --stacktrace`를 한 번 시도했으나 종료 코드 1. 구성 단계의 `GeneratedClassCompilationException` 원인은 Gradle 배포본 `lib/gradle-core-api-9.6.0.jar`에 대한 `AccessDeniedException`으로, ZIP 파일 시스템을 닫는 과정에서 발생했다. 권한 거부가 발생해 반복이나 제한 우회는 하지 않았다.
- 테스트·세 빌드·계측 APK 컴파일·lint 태스크는 이번 에이전트 실행에서 시작되지 않았으므로 수정 후 컴파일 성공 여부와 테스트 수는 미확인이다. 계측 테스트와 기기 검증도 실행하지 않았다. `git diff --check`는 통과했다.

## 2026-11-08 와이퍼 안내 빌드 구성 실패 기록 조사

- 마지막 와이퍼 안내 빌드의 기존 콘솔·Gradle 데몬 로그에는 `GeneratedClassCompilationException: Unable to compile generated classes`만 남아 있고 `Caused by` 체인은 없다. 해당 실행의 첫 번째 원인은 기존 기록만으로 미확인이다.
- 같은 실행 환경의 **이전 별도 진단** stacktrace에서는 `GeneratedClassCompilationException` 다음 원인이 Gradle 9.6.0 배포본의 `lib/gradle-core-api-9.6.0.jar`에 대한 `java.nio.file.AccessDeniedException`이며, ZIP 파일 시스템을 닫는 과정에서 발생했다. 마지막 실행과 동일한 원인일 가능성은 있으나, 마지막 실행에서 직접 확인한 사실은 아니다. 권한 거부의 구체적 원인은 여전히 미확인이다.
- `build/wiper-notice-verification-init.gradle`은 `gradle.beforeProject`에서 각 프로젝트의 build directory를 `build/wiper-notice-verification/${p.name}`으로 지정한다. 파일 삭제·앱 설치·데이터 조작 동작은 없으며, 해당 출력 폴더는 마지막 실패 후 생성되지 않았다. Gradle 스크립트 전체와 외부 플러그인의 부수 효과까지 보증하는 근거는 아니다.
- 이번에는 기존 기록만 읽었으며 Gradle 명령·단위 테스트·빌드·계측 테스트·기기 접근을 실행하지 않았다. 와이퍼 변경의 컴파일과 기기 동작은 계속 미확인이고 사용자 데이터와 기존 APK는 건드리지 않았다.

## 2026-11-08 Pleos 화면 점검 사용자 보고와 와이퍼 안내

- **사용자 보고 확인:** 걱정 상황 선택에서 설명·사전으로 이동하고 경로 준비로 돌아오는 흐름과 관련 안내, 선택 후 복습 기록이 늘지 않는 동작. 설정·복습 복원과 내부 백업·복원도 확인했다. 사용자 유형별 차이는 고령자 ‘오늘 복습’ 카드만 확인했다.
- **부분 확인:** 글자 200%는 사용자가 확인한 화면에서 통과했으나 화면 이름이 기록되지 않아 확인 화면을 특정하거나 전체 화면 통과로 표시할 수 없다. 주차 FeatureGate는 미실행이다.
- **사용자 관찰:** 와이퍼 연습 시작 버튼은 비활성이었고 사용자가 비활성 이유를 명확히 찾지 못했다. 앱 크래시는 없었으나 시스템 서비스 종료의 원인과 앱 관련성은 미확인이다.
- **시작 전 상태:** 설정과 현재 설정의 비교는 미확인이다. 시작 전 값을 추정해 변경하지 않았으며 일치 여부를 단정하지 않는다.
- **소스 보강:** 기존 차단 문구는 버튼에서 떨어져 있어 시작 버튼 바로 앞에 이유와 설명 열람 가능 안내를 추가하고 버튼의 접근성 상태 설명에도 같은 이유를 제공한다. Vehicle SDK 차단, 연습 완료 판정과 주차 FeatureGate는 유지한다. 별도 화면 컴포넌트의 200%·좁은 화면·비활성 상태·접근성 의미를 확인하는 계측 테스트를 작성했으며 실행 여부는 별도로 기록한다.
- **이번 검증:** 격리 출력용 init 스크립트와 기존 격리 Gradle 홈에서 `testDebugUnitTest assembleDebug assembleDemo assembleRelease assembleDebugAndroidTest lintDebug --offline --no-daemon`을 시도했으나 Gradle 구성 중 `GeneratedClassCompilationException`으로 종료 코드 1. 태스크가 실행되지 않아 테스트 수와 lint 결과가 없고 새 코드와 계측 APK의 컴파일도 미확인이다. 이전 접근 거부와 같은 종류의 오류지만 이번 실행의 상세 원인은 재확인하지 않았다. `git diff --check`와 문자열 XML 파싱·중복 확인은 통과했다. Python 실행은 프로그램을 찾지 못해 실패했으며 XML 검사는 PowerShell로 수행했다.
- **기기 상태:** 위 Pleos 결과는 사용자 보고이며 에이전트가 독립 재실행한 결과가 아니다. 이번 소스 보강 후의 실제 화면·스크린리더·200%·기기·실차 검증은 미실행이다. 사용자 데이터·기존 설치 APK에 접근하거나 이를 변경하지 않았다.

## 2026-11-08 사용자 로컬 터미널 검증 결과

- 아래는 **사용자가 로컬 터미널에서 직접 실행해 제공한 결과**이며 에이전트가 재실행하거나 독립 확인한 결과가 아니다: `.\gradlew.bat testDebugUnitTest assembleDebug assembleDemo assembleRelease lintDebug --offline --console=plain` → `BUILD SUCCESSFUL` (1분 13초). 단위 테스트 97개 실행, 실패 0·오류 0·스킵 0; debug/demo/release 빌드 성공.
- 사용자 보고 lint: 오류 0·경고 10. `GradleDependency` 6(core-ktx, test junit, espresso, lifecycle-runtime-ktx, activity-compose, compose-bom), `AndroidGradlePluginVersion` 1(9.5.0→9.5.1), `NewerVersionAvailable` 1(Kotlin Compose 플러그인), `ObsoleteLintCustomCheck` 1(lifecycle lint 검사 2개 생략), `PrivateApi` 1(`ParkingMonitor.kt` 리플렉션). 이전 에이전트 보고 7건 대비 추가된 3건은 compose-bom·AGP·Kotlin Compose 플러그인의 버전 알림이며 증가 원인은 미확인이다. SDK 호환성이 검증되지 않아 의존성 업그레이드는 하지 않았다.
- 남은 컴파일러 경고: `AndroidCarParkingMonitor.kt` 45·46행 deprecated API, `ReviewRepository.kt` 11행 data class `copy` 가시성. 남은 백로그: `ParkingMonitor.kt`의 `PrivateApi` 리플렉션 의존성 점검, lifecycle lint 검사 복구를 위한 라이브러리 업그레이드 검토(시연 이후).
- 사용자 보고 실행에는 `assembleDebugAndroidTest`가 포함되지 않았다. 계측 APK는 이번에 빌드하지 않았고 기기·에뮬레이터 검증도 미실행이다. 아래의 에이전트 접근 거부 진단은 원인 미확인으로 유지한다.

## 2026-11-08 미커밋 백업 및 Gradle 진단

- 프로젝트 밖에 기존 추적 변경의 binary 패치, 미추적 파일 10개와 `run-app.cmd` 사본을 백업하고 복사본 11개의 SHA-256 일치를 확인했다. `.idea/`, APK와 빌드·캐시는 제외했다. 이 기록은 백업 후 추가되어 해당 패치에 포함되지 않는다.
- Gradle wrapper는 9.6.0이고 실행한 배포본도 9.6.0이다. 실행 JBR은 Java 25.0.4이며 daemon 설정은 Java 25다. 앱의 Java 11은 컴파일 대상이다.
- 기존 격리 홈과 init 스크립트, 오프라인 옵션을 사용한 검증 명령은 구성 단계에서 실패했다. stacktrace의 직접 예외는 Gradle 배포본 `gradle-core-api-9.6.0.jar`에 대한 `AccessDeniedException`이다. 권한 거부의 구체적 원인과 해결책은 미확인이다.
- 동일 환경의 init 없는 `help --offline --no-daemon --console=plain`, 그리고 오프라인만 제외한 `help --no-daemon --console=plain`은 각각 종료 코드 1로 같은 생성 클래스 컴파일 오류가 났다. 단위 테스트, 세 빌드, lint는 구성 실패로 이번에 실행하지 못했다. 기존 보고의 97개 테스트 결과는 이번 재검증 결과가 아니다.
- ADB·에뮬레이터·실차는 접근하지 않아 미실행이다. 권한이나 사용자 데이터, 설치 APK를 변경하지 않았으며 스테이징·커밋·push도 하지 않았다.

## 2026-11-08 걱정 상황 연결 정리·기기 검증

- 커밋: 기준 `95cc2a1`, 스테이징 영역은 비어 있다. 직전 기능의 `PreparationConcern.kt`, `PreparationConcernTest.kt`, `PreparationConcernUiTest.kt`는 별도 파일이지만 화면 연결이 포함된 `MainActivity.kt`, `PracticeScreens.kt`, `WarningCatalogScreen.kt`, `strings.xml`, `FEATURE_STATUS.md`에는 이전 설정·복습·리소스 작업도 함께 있다. `SCREEN_SPEC.md`, `DESIGN_HANDOFF.md`도 기존 작업과 함께 미커밋이다. 사용자의 다른 변경 혼합 시 스테이징하지 말라는 지시에 따라 어떤 파일도 스테이징하거나 커밋하지 않았다. `run-app.cmd`, `.idea/`도 그대로 보존하고 push하지 않았다.
- lint: 기준 커밋 생성 직전 `build/settings-review-ui-isolated/app/reports/lint-results-debug.txt`와 직전 작업 후 `build/preparation-flow-isolated/app/reports/lint-results-debug.txt`를 비교했다. 둘 다 오류 0·경고 7이며 항목은 라이브러리 `ObsoleteLintCustomCheck` 1, `ParkingMonitor.kt`의 `PrivateApi` 1, `gradle/libs.versions.toml`의 `GradleDependency` 5로 동일하다. 신규 경고는 발견되지 않았다. 기준 보고서는 커밋 생성 직전 기록이며 이번에 별도 기준 체크아웃을 빌드한 결과는 아니다. 라이브러리 일부 lint 검사는 건너뛰어졌다.
- 코드 구현: 세 주제 연결과 복귀 경로, 수동 선택이며 실경로·차량 상태 분석이 아니고 복습 기록에 저장하지 않는다는 문구가 코드에 있다. 실제 화면이 표시됐다는 근거는 아니다.
- 빌드 확인: 직전 `build/preparation-flow-isolated`에서 단위 테스트 97개, debug·demo·release 및 계측 APK 빌드, lint가 성공했다. 이번에는 기존 보고서를 읽었고 재빌드하지 않았다. 별도 APK 빌드는 설치 성공이나 에뮬레이터 동작을 의미하지 않는다.
- Pleos 에뮬레이터 확인: 미실행. 이전 ADB 접근 거부가 해결됐다는 새 근거가 없어 재접근하지 않았다. 세 주제 선택/복귀, 문구, 기록 미생성·자동 감지 부재, 글자 200%·좁은 화면·계측 실행, 세 사용자 유형, 설정/복습/백업/FeatureGate 회귀는 모두 미실행이다.
- 복구: 기기에 접근하거나 테스트를 시작하지 않아 시작 전 설정·복습·차량 상태를 읽거나 내부 백업을 만들지 않았고, 설치·설정·차량 상태 변경이나 복원도 실행하지 않았다. 시작 전과 종료 후 기기 상태가 동일하다는 독립 검증도 미실행이다. 설치된 앱이 최신 빌드인지 미확인이고 이전 APK에는 Vehicle SDK 차단이 없을 수 있다.
- 다음 판단: 다른 미커밋 화면 작업을 먼저 별도 커밋으로 정리하거나, 혼합 파일의 부분 스테이징을 허용할지 사용자가 결정해야 한다. 실제 기기 테스트는 ADB 접근 거부 해결 근거와 기존 내부 백업 보존 방법을 확인한 후에만 진행한다.

## 2026-11-08 출발 전 걱정 상황 학습 연결

- 구현: 경로 준비 화면에 `PreparationConcern.kt`의 수동 학습 주제 세 가지(앞유리 김서림·와이퍼 조작·경고등 의미)를 추가했다. 사용자가 선택하면 각각 김서림 설명·기존 와이퍼 화면·경고등 사전으로 이동하며 경로 준비로 돌아올 수 있다. 선택은 복습 기록으로 저장하지 않고 실제 경로 분석이나 차량 신호가 아니라는 안내를 표시한다. 경고등은 실제로 감지된 것처럼 특정 항목을 자동 선택하지 않는다. 고령자에게는 짧은 안내와 기존 큰 터치 토큰을 사용한다.
- 안전·데이터: Vehicle SDK 기본 차단, 주차 FeatureGate, 와이퍼 OFF→ON→OFF 로직, 복습 동의·기존 기록 보호를 유지한다. 김서림 완료 판정은 잠겨 있고 선택만으로 기록·차량 제어 명령을 만들지 않는다. 이는 실제 경로 후보·도로 폭 분석이나 차량 상태를 제공하는 기능이 아니다.
- 검증: `PreparationConcernTest.kt`에서 주제별 목적지와 Vehicle 차단·설명 완료 잠금을 확인했다. `PreparationConcernUiTest.kt`는 저장소·SDK 없이 280×220dp와 글자 200%에서 안내와 선택을 검증하도록 작성했으나 기기에서 실행하지 않았다. `build/preparation-flow-isolated`에서 단위 테스트 97개, debug·demo·release·계측 APK 빌드 및 lint 성공(오류 0·경고 7; 일부 라이브러리 lint 검사 생략). Vehicle SDK 차단 정적 검사 통과(반사·의존성 내부·실행 동작은 검사 범위 밖). 기존 APK 58개 해시는 전후 일치했다.
- 미실행·한계: 기기·에뮬레이터·실차, 화면 흐름·200%·스크린리더 및 실제 SDK IPC는 미실행이다. 기존 설치 APK와 사용자 데이터는 변경하지 않았으며, 기존 설치 앱이 이 빌드인지도 확인하지 못했다. SDK 안전 해제 계약 및 실경로 속성은 미확인이다. 걱정 선택과 기존 수동 점검·복습 사이의 전체 화면 흐름은 기기 회귀가 필요하고 수동 열람/직접 확인 기록 기능은 아직 구현하지 않았다. 원격 push·공급자 문의 발송은 하지 않았다.

## 2026-11-08 오프라인 화면 일괄 후속

- 구현: 설정 제목, 오늘 복습 항목, 경고 사전 추가·동의/결과, 내비 화면의 옵션·연결/조회·확인창, 출발 전 문 조회 결과 문구를 문자열 리소스로 옮겼다. `TodayReviewItemState`, `WarningConsentEvent`, `NavigationConnectionState`/이벤트와 `DoorSnapshotSection`으로 표시 UI를 분리했다. 내비 연결·즐겨찾기·옵션과 점검 버튼에 터치 토큰을 적용하고 즐겨찾기 접근성 이름에 항목 이름과 주소를 포함했다. SDK 신호·경로 요청 및 설정/복습 데이터 처리는 변경하지 않았다.
- 접근성·테스트: `OfflineComponentsAccessibilityTest.kt`에 SDK나 사용자 저장소 없이 280×220dp·글자 200%에서 내비 연결 버튼 스크롤/선택과 경고 사전 동의/취소 이벤트를 확인하는 계측 테스트를 추가했다. 기존 오늘 복습·점검·동의 및 데이터 보호 테스트도 유지했다. 계측 APK는 컴파일만 했으며 줄바꿈·화면 잘림·스크린리더와 실제 입력은 기기에서 미실행이다.
- 빌드: `build/ui-batch-isolated`에서 전체 단위 테스트 96개와 debug·demo·release 및 계측 APK 빌드, lint 성공(오류 0·경고 7; 일부 라이브러리 lint 검사 생략). Vehicle SDK 차단 정적 검사 통과(반사·의존성 내부·실행 동작 제외). 기존 로컬 APK 55개 해시 전후 일치. 사용자 데이터·설치 APK·`run-app.cmd`·`.idea/`는 변경하지 않았다.
- 한계: `NavigationSession`과 `NavigationObservation`의 연결·요청 결과 안내, 차량 상태·주차 화면 및 일부 복습/경고 상세 메시지, 음성 CompositionLocal 기본값에는 하드코딩이 남는다. 모든 화면의 상태/이벤트 분리와 실제 200%·다양한 비율·스크린리더 검증은 미완료다. ADB 접근 거부 해결 근거가 없어 기기·에뮬레이터·실차 및 실제 SDK IPC는 미실행. Vehicle SDK 기본 꺼짐과 주차 FeatureGate를 유지하며 이전 설치 APK에는 차단이 없을 수 있다. 공급자 문의를 발송하거나 원격 push하지 않았다.
- Git: 기준 `95cc2a1`; 이전 및 이번 변경은 미커밋·미스테이징이며 기존 `run-app.cmd` 수정과 `.idea/`도 그대로 남는다.

## 2026-11-08 설정 표시·복습 삭제 화면 후속

- 구현: `SettingsAppearanceControls.kt`에서 대비·화면 모드·안내 상세도의 표시와 변경 이벤트를 분리하고 문구·접근성 이름·터치 크기를 자원과 토큰으로 옮겼다. `PracticeScreens.kt`에서는 복습 삭제 확인 UI의 확인/취소 이벤트를 분리하고 이력·저장 상태·삭제 범위·실패 안내 및 와이퍼 복습 기록 실패 문구를 리소스로 옮겼다. 기존 저장·동의·마이그레이션·손상/미래 버전·기록 결과 불명 처리와 삭제 확인 조건은 유지하며 저장 형식은 바꾸지 않았다.
- 검증: `SettingsAppearanceTest.kt`로 설정 변경 시 글자 크기·경로 선호 등 다른 필드가 보존되는지 확인했다. `AppearanceReviewDialogUiTest.kt`에는 280×220dp·글자 200%의 대비/모드 조작 및 삭제 확인/취소의 표시 전용 Fake 테스트를 추가해 컴파일했다. `build/appearance-followup-isolated`에서 단위 테스트 96개와 debug·demo·release·계측 APK 빌드 및 lint 통과(오류 0·경고 7; 라이브러리 일부 검사 생략). 계측 테스트는 기기에서 미실행이다.
- 안전·보존: Vehicle SDK 차단 정적 검사 통과(반사·의존성 내부·실행 동작은 범위 밖). Android Car 주차 FeatureGate는 변경하지 않았다. 기존 APK 51개의 해시 전후 일치. 사용자 설정·복습·백업·설치 APK·`run-app.cmd`·`.idea/`는 건드리지 않았다. ADB 접근 거부 해결 근거가 없어 재접근하지 않았으며 실제 화면·200%·스크린리더·에뮬레이터·실차 및 SDK IPC는 미실행이다. 이미 설치된 이전 APK에는 안전 차단이 없을 수 있다.
- 남은 일: 설정 제목·음성 기본 CompositionLocal, 오늘 복습, 경고 사전 추가/동의, 내비·점검·차량 상태 등에는 하드코딩과 상태/이벤트 분리 작업이 남는다. 전체 화면의 동적 글자 줄바꿈·스크린리더 검증과 SDK release 계약은 미완료다. 기준 커밋은 `95cc2a1`이며 체크포인트 이후 기존 변경과 이번 변경은 미커밋·미스테이징 상태로 보존한다. 원격 push와 공급자 문의 발송은 하지 않았다.

## 2026-11-08 로컬 체크포인트 및 화면 후속

- 체크포인트: 기존 스테이징 영역이 비어 있음을 확인한 뒤 앱 코드·테스트·문서와 필요한 빌드 설정·정적 검사 도구를 로컬 커밋 `95cc2a1343f31b7c28a78d9bc32e659fe42565da`로 기록했다. 기존 `run-app.cmd` 수정과 `.idea/`는 커밋에서 제외하고 그대로 보존했다. APK·키 파일 등의 경로와 일반적인 비밀값 패턴을 검사해 후보에서 발견하지 못했다. 이는 모든 유형의 비밀이나 이전 Git 이력 전체에 대한 보증은 아니다. 원격 push·공급자 문의 발송은 하지 않았다.
- 체크포인트 이후 미커밋 구현: 설정 글자 크기 슬라이더의 임시 상태·저장 완료 이벤트를 `SettingsTextSizeControl.kt`로 분리했다. 음성 안내, 내부 백업의 기록 결과·교체/복원 확인, 설정·복습 기록 결과 불명과 재시도 안내, 복습 이력 표시를 문자열 자원으로 옮겼다. 복습 이력은 기존처럼 모호한 종료 여부를 진행 중으로 표시한다. 설정·백업·동의·마이그레이션·손상/미래 버전·기록 결과 불명 보호, 사용자 확인 흐름은 기존 저장소 경로를 유지한다.
- 검증: 분리된 `build/checkpoint-followup-isolated`에서 단위 테스트 95개, debug·demo·release와 계측 APK 빌드, lint 통과. 첫 lint에서는 Compose 리소스 접근 오류 13개가 발생해 수정했고 재실행은 오류 0·경고 7이다. 일부 라이브러리 lint 검사는 건너뛰어졌다. 글자 200%·280×220dp의 슬라이더 접근성/조절 계측 테스트는 작성·컴파일만 했다. Vehicle 차단 정적 검사는 세 빌드에서 통과했으며 실행 전체를 증명하지는 않는다.
- 보존·미실행: 기존 APK 47개 해시 전후 일치. 사용자 데이터·설치 APK·`run-app.cmd`·`.idea/`는 변경하지 않았고 기존 로컬 변경은 유지했다. ADB 접근 거부 해결 근거가 없어 재접근하지 않았으며 실제 화면·200%·스크린리더·계측·실차 테스트는 미실행이다. 기존 설치 APK에는 Vehicle 안전 차단이 없을 수 있다.
- 남은 일: 설정 화면의 대비·모드·상세도 문구, 음성 CompositionLocal 기본값, 복습 삭제 확인·오류와 일부 백업 상태 및 화면별 상태/이벤트 분리는 아직 하드코딩으로 남는다. SDK 조기 release 계약은 미해결이며 안전 차단과 주차 FeatureGate를 유지한다. 이 섹션과 체크포인트 이후 UI 변경은 위 커밋에 포함되지 않은 작업 트리 변경이다.

## 2026-11-08 설정·복습·조작 설명 화면 후속

- 구현 파일: `SettingsManagementScreen.kt`에서 파일 설정 이동·복원·초기화 문구를 `strings.xml`로 옮기고 초기화 확인 상태·이벤트와 UI를 분리했다. `PracticeScreens.kt`에서 저장된 경고등 항목의 표시 상태·열기/삭제 이벤트를 분리하고, 복습 동의 안내·항목 이름 등 주요 문구를 자원화했다. 설정/복습/와이퍼 버튼에 최소 터치 토큰을 적용하고 공조·김서림 완료 잠금의 접근성 이름에 레슨 이름을 포함했다. 기존 저장·마이그레이션·동의·손상/미래 버전·기록 결과 불명 및 삭제 확인 로직은 유지했다.
- 테스트 파일: `SettingsResetStateTest.kt`는 초기화 확인 상태를 검사한다. `SettingsReviewAccessibilityTest.kt`는 별도 저장소도 열지 않는 표시 전용 콜백으로 280×220dp·글자 200%에서 초기화 취소/확인 및 항목별 복습 열기/삭제를 확인하도록 작성했다. 기존 설정·복습·백업·차단 단위 테스트도 유지했다. 계측 테스트는 컴파일만 했고 실제 기기에서 실행하지 않았다.
- 빌드: `build/settings-review-ui-isolated`에서 단위 테스트 94개, debug·demo·release와 계측 APK 빌드 및 lint 성공. lint 오류 0·경고 7로 이전과 같고 라이브러리 일부 검사는 API 불일치로 건너뛰어졌다. Vehicle SDK 차단 정적 검사도 통과했으나 반사·의존성 내부·실행 동작은 검사 범위 밖이다. 기존 APK 43개 해시 전후 일치, 사용자 데이터·설치 APK·`run-app.cmd`·`.idea/`는 변경하지 않았다.
- 남은 일: 설정의 슬라이더·음성 안내·내부 백업과 복습 기록/삭제 확인·실패 안내에는 하드코딩이 남으며 화면 전체의 상태·이벤트 분리는 미완이다. 색상·타이포그래피 통합도 미완이다. 200% 줄바꿈·다양한 비율·스크린리더 및 실제 확인창 상호작용은 기기에서 미실행이다. ADB 거부 해결 근거가 없어 재접근하지 않았고 Vehicle SDK 차단·주차 FeatureGate는 유지했다. 이미 설치된 이전 APK에는 안전 차단이 없다. 공급자 문의는 발송하지 않았다.

## 2026-11-08 차량 SDK 없는 화면·접근성 후속

- 구현: 홈 목적지 목록을 `HomeDestination`과 문자열 리소스로 분리하고 공통 버튼에 터치 크기 토큰을 적용했다. 경고 사전 항목을 `WarningCardState`·추가 이벤트·표시 UI로 분리하고 56dp 최소 터치 높이와 항목 이름을 포함한 접근성 이름을 적용했다. 출발 전 점검 체크 행과 조회 버튼의 일부 문구·크기를 리소스·토큰으로 옮겼다. 저장 동의·복습 기록·손상/미래 버전·결과 불명 처리와 수동 점검/초기화는 기존 경로를 유지했다.
- 검토: 설정·내 복습·와이퍼 및 공조/김서림 설명의 기존 흐름을 확인했다. 이번에는 해당 화면 전체를 이관하지 않았으며 하드코딩 문자열, 상태/이벤트 분리와 좁은 화면의 실제 검증이 남는다. SDK 안전 차단과 Android Car 주차 FeatureGate는 유지하고 공급자 문의 초안은 발송하지 않았다.
- 검증: 단위 테스트 93개 통과(경고 카드의 저장 가능 상태·홈 목적지 및 기존 설정·복습·안전 테스트 포함). 280×220dp와 글자 200%에서 경고 카드 스크롤·항목별 접근성 이름·선택을 확인하는 Compose 계측 테스트를 추가해 컴파일했다. 기기에서 실행하지 않아 줄바꿈·잘림·스크린리더·실제 동작은 확인되지 않았다.
- 빌드: `build/offline-accessibility-isolated`에서 debug·demo·release 및 계측 APK 빌드와 lint 성공. lint 전 0 오류·7 경고, 후 0 오류·7 경고로 동일: 라이브러리 lint API 불일치 1, Android Car 비공개 API 관련 1, 의존성 업데이트 제안 5. 일부 라이브러리 lint 검사는 건너뛰어졌다. Vehicle 차단 정적 검사 통과(반사·의존성 내부·실행 동작 제외). 기존 APK 39개 해시 전후 일치; 사용자 데이터·설치 APK·`run-app.cmd`·`.idea/`는 변경하지 않았다.
- 미실행: 이전 ADB 접근 거부 해결 근거가 없어 기기·에뮬레이터·계측·실차에 접근하지 않았다. 200% 화면과 다양한 비율 및 차량 신호·실경로는 실제 확인하지 않았고 기존 설치 APK에는 안전 차단이 없다.

## 2026-11-08 Vehicle 안전 차단 후속 점검

- 공급자 문의: [VEHICLE_SDK_SUPPORT_DRAFT.md](VEHICLE_SDK_SUPPORT_DRAFT.md)에 실제 관측과 계약 확인 요청을 구분해 작성했다. 일회성 요청 취소·완료, timeout/화면 이탈/연결 유실 시 release, 콜백 내부·반환 후 해제, 요청·인스턴스 제한, 잠금 영역 미보고와 벨트·워셔액 오류 및 공개 오류 타입을 질문한다. 초안은 전송하지 않았다.
- 정적 점검: `scripts/check-vehicle-safety.ps1`은 main/debug/demo/release의 Kotlin·Java 소스에서 Vehicle SDK 참조와 세 생성 위치의 공통 차단을 확인하고, 세 빌드의 reader 계열 컴파일 클래스에서 Vehicle 객체 할당 명령이 없는지 확인한다. 별도 임시 가짜 소스에 무차단 생성문을 넣었을 때 거부되는 것도 확인했다. 검사 범위는 지정 소스셋과 reader 클래스이며 반사·네이티브·의존성 내부 및 실제 실행의 전체 안전성을 증명하지 않는다. Android Car 주차 판정과 NaviHelper는 Vehicle SDK 차단과 별개다.
- Fake 검증: 차단 상태에서 유효한 주차 증거를 사용해 Fake 설정 저장, 수동 문 체크, 사전 데이터 파싱과 복습 저장이 동작하고, 공조·김서림 설명 상태는 열 수 있으나 완료 판정은 잠김을 확인했다. 불확실한 주차 증거는 계속 잠긴다. 공조·김서림 화면 상호작용 계측 테스트도 추가해 컴파일했다. 기존 차단 reader·문 체크·경고 사전·설정·복습 계측 테스트 역시 기기 미실행이다. 실제 화면·저장장치 동작을 새로 확인한 것은 아니다.
- 빌드: `build/support-audit-isolated`에서 단위 테스트 91개, debug·demo·release 및 계측 APK 빌드와 lint 성공. lint 오류 0·경고 7이고 일부 라이브러리 lint 검사는 건너뛰어졌다. 기존 APK 35개 해시 전후 일치; 사용자 데이터·설치 APK·`run-app.cmd`·`.idea/`는 변경하지 않았다.
- 한계: 기존 설치 APK에는 이 차단이 없다. ADB 접근 거부의 해결 근거가 없어 재접근하지 않았고 에뮬레이터·계측·실차·실제 SDK IPC는 미실행이다. SDK 조기 release 계약은 여전히 미해결이며 공급자 확인과 별도 기기 검증 전 기본 차단을 유지한다.

## 2026-11-08 Vehicle SDK 안전 차단

- 구현: `VehicleSdkAccess.enabled`를 모든 빌드에서 `false`로 고정하고 Vehicle 생성 세 곳을 공통 팩토리로 감쌌다. 와이퍼, 네 문 및 운전석 문 위치·잠금, 안전벨트·워셔액, 진단 차량 상태에서 실제 Vehicle 인스턴스 생성·조회·해제 경로가 차단된다. 상태 결과는 `SafetyBlocked`이며 화면에는 `차량 연동 안전 점검 중`을 표시한다. 와이퍼 시작·SDK 조회 버튼은 비활성화되고 차단은 완료나 정상 신호로 처리되지 않는다.
- 범위: 와이퍼 조작 설명·공조/김서림 설명·설정·저장된 복습·경고등 사전 및 수동 점검은 유지한다. Android Car 기반 주차 FeatureGate는 별도이며 P·속도·UX 증거가 불확실하면 기존처럼 잠긴다. 사용자 설정이나 숨겨진 화면으로 안전 차단을 해제할 수 없다. 사용자가 이미 설치한 이전 APK에는 이 차단이 적용되지 않는다.
- 검증: Fake 팩토리에서 차단 시 생성·조회·해제 호출이 모두 0이고 차단된 와이퍼 값이 완료 판정을 진행시키지 않으며 네 문 자동 확인도 안 됨을 단위 테스트했다. 차단 문구·비활성 버튼·Fake reader 미생성과 실제 reader 진입점 반환을 확인하는 계측 테스트를 작성해 컴파일했지만 기기에서 실행하지 않았다. 기존 Fake 문 점검 테스트는 Fake 주입 시에만 화면 차단을 풀어 계속 보존했으며 실제 SDK 팩토리는 기본 정책으로 차단된다.
- 빌드: 분리된 `build/safety-stop-isolated`에서 단위 테스트 89개와 debug·demo·release 및 계측 APK 빌드, lint 통과. lint 오류 0·경고 7이며 일부 라이브러리 검사는 건너뛰어졌다. 알려진 시연·진단 표식의 release 제외 검사 통과. 기존 APK 31개 해시 전후 일치; 사용자 데이터·설치 APK·`run-app.cmd`·`.idea/`는 변경하지 않았다.
- 한계: 이 조치는 release 경합을 해결하지 않고 해당 SDK 호출을 기본 차단한다. 이전 ADB 거부 해결 근거가 없어 에뮬레이터·계측·실차 및 실제 SDK IPC는 미실행이다. 공급자에게 확인할 계약과 재활성화 조건은 [SDK_CALL_AUDIT.md](SDK_CALL_AUDIT.md)에 기록했다.

## 2026-11-08 Vehicle release 계약 후속 — 미해결

- 공식 문서와 설치 Vehicle 2.3.2 공개 선언을 다시 확인했다. 미응답 일회성 capability/get의 취소·완료 또는 연결 해제 후 안전한 release 조건은 확인하지 못했다. 리스너 unregister는 일회성 요청 취소가 아니다. 계약 근거와 호출별 위험, 공급자 확인 사항은 [SDK_CALL_AUDIT.md](SDK_CALL_AUDIT.md)에 기록했다.
- 구현: 실제 Vehicle reader의 release 동작은 변경하지 않았으며 여전히 미응답 콜백 전에 release할 수 있다. 또한 앱 전체의 미완료 요청과 SDK 인스턴스 수를 제한하지 않는다. 안전한 종료 신호를 확인하지 못한 상태에서 Fake의 콜백 반환 완료 이벤트를 실제 API인 것처럼 사용하지 않았다. 기존 주차·와이퍼·문 점검 및 데이터 코드는 변경하지 않았다.
- 모델 테스트: 별도의 Fake가 콜백 반환 완료를 명시적으로 알려준다고 가정하고 정상·실패·중복, 타임아웃 후 늦은 콜백, 화면 이탈, 영구 미응답, 동시 요청 및 종료 경쟁을 시험했다. 이 모델은 실제 어댑터에 연결되지 않았고 실제 SDK의 release 안전성을 증명하지 않는다. 기존 코루틴 취소 전파 단위 테스트도 유지했다.
- 빌드: 별도 `build/release-contract-isolated`에서 단위 테스트 87개, debug·demo·release 빌드와 `lintDebug` 통과. lint 오류 0·경고 7이며 라이브러리 일부 검사는 건너뛰어졌다. 기존 APK 28개 해시는 전후 일치했다. 사용자 데이터·설치 APK·`run-app.cmd`·`.idea/`는 변경하지 않았다.
- 기기·실차: ADB 접근 거부 해결 근거가 없어 재접근하지 않았고 실제 SDK IPC·에뮬레이터·실차는 미실행이다. 문제가 해결될 때까지 실제 차량 SDK 조회 사용을 중단하고 반복 재조회·재연결을 삼가야 한다. 프로세스 종료·서비스 연결 해제를 이전 요청의 안전한 완료로 단정할 수 없다.

## 2026-11-08 SDK 호출·생명주기 점검

- 호출별 목록과 처리·문서 대조는 [SDK_CALL_AUDIT.md](SDK_CALL_AUDIT.md)에 기록했다. 확인한 공식 문서와 설치된 진입점 소스에서 필수 호출·콜백 스레드를 특정하지 못했다. 기존 FeatureGate는 불확실하거나 오래된 기어·속도·주차 화면 증거에서 계속 잠긴다.
- 구현: 와이퍼 및 차량 상태 신호의 단발 콜백 차단을 공통 `CallbackGate`로 분리했다. 차량 상태에서 각 문의 위치와 잠금 읽기 예외를 서로 격리했다. 내비 탐색 완료·종료 이벤트는 요청 ID가 없어 해당 요청 완료로 간주하지 않으며, 기본 내비 확인을 안내하고 시간 초과 시 결과 불명으로 처리한다. 조회 중 목적지 변경 이벤트가 오면 진행 중인 조회를 무효화하고 다시 연결하게 한다. 같은 연결에서 다른 오래된 콜백과 새 조회 콜백을 식별할 수 없는 한계는 남는다.
- 단위 검증: Fake 콜백에서 성공·실패·중복·시간 초과·취소·늦은 응답·재시도를 검증했고, 내비 관찰 문구와 기존 주차 읽기 실패 후 복구·네 문 확인·와이퍼·저장 보호 테스트를 유지했다. 이는 실제 Vehicle/NaviHelper 서비스의 IPC나 리스너 해제를 실행한 테스트가 아니다.
- 빌드: 분리된 `build/lifecycle-audit-isolated`에서 단위 테스트 83개, debug·demo·release와 계측 테스트 APK 빌드, `lintDebug` 성공. lint 오류 0·경고 7, 라이브러리 일부 lint 검사는 건너뛰어졌다. 다섯 개 시연 표식이 release에 없는 기존 검사도 통과했으나 전체 격리를 증명하지 않는다. 기존 APK 24개 해시 전후 일치. 기존 사용자 데이터·설치 APK·`run-app.cmd`·`.idea/`는 건드리지 않았다.
- 기기·실차: 이전 ADB 접근 거부 해결 근거가 없어 재접근하지 않았고 에뮬레이터·Pleos·실차 및 계측 테스트는 미실행이다. 앱의 직접 로그 호출은 검색되지 않았고 진단에는 고정 이벤트만 남는다. SDK 내부·차량 서비스 로그 제어는 확인하지 못했다.
- 남은 중요 위험: 공식 Vehicle 문서는 비동기 콜백 완료 전 `release()`하지 말라고 하지만, 현재 시간 초과·취소·화면 이탈 뒤 미응답 SDK 호출을 둔 채 release할 수 있다. SDK가 콜백을 끝내 보내지 않는 경우 안전한 취소·해제 계약과 동기 호출 중단 가능성을 확인하지 못해 해결되지 않았다. 실제 스레드·동시 호출·연결 복구와 안전한 종료는 기기 및 공급자 확인이 필요하다.

## 2026-11-08 SDK 오류·개인정보·접근성 후속

- SDK 오류 조사: [공식 오류 문서](https://document.pleos.ai/api-reference/connect-sdk-pleos/Vehicle/vehicle-error-code)는 `onFailed`의 `Exception`이 `VehicleException`인 경우 `errorCode`를 확인하라고 안내한다. 설치된 Vehicle 2.3.2의 Gradle metadata와 오프라인 `debugCompileClasspath`/`debugRuntimeClasspath`를 대조한 결과 `vehicle-data:1.1.11`은 런타임에만 포함된다. 그 AAR에는 `ErrorCode.class`가 있지만 컴파일 경로에는 없다. Vehicle AAR의 예외 패키지에는 `VehicleException.class` 대신 `a.class`가 들어 있다. 따라서 이 배포본에서 문서의 타입을 공개 이름으로 참조하는 호환 경로를 확인하지 못했다. 내부 클래스나 반사에 의존하지 않고 SDK 버전도 바꾸지 않았다. `SecurityException` 이외 확인되지 않은 예외는 일반 오류로 유지한다. 공식 문서 자체도 SDK 버전에 따라 내용이 다를 수 있다고 명시한다. 실제 오류 콜백·권한·서비스 검증은 미실행이다.
- 개인정보: 앱 소스에서 Android Log·println·스택 출력 호출을 찾지 못했으며, debug/demo 내비 진단은 임의 문자열 인수를 제거해 고정된 이벤트 이름만 메모리에 남긴다. 해당 제한의 단위 테스트를 추가했다. SDK 소스의 Vehicle 및 NaviHelper 로그 구현은 `internal`이며 오류 메시지를 기록하는 경로가 있다. 확인한 공개 문서에서 앱의 SDK 로그 제어 수단은 발견하지 못했다. SDK·차량 서비스 로그나 그 내용 전체는 통제·검증되지 않았고, 이번 조사에서 사용자 위치·목적지·차량 식별자·토큰을 기록하거나 기기 로그를 수집하지 않았다.
- UI·접근성: 오늘 복습의 제목·빈 상태·동의 안내를 문자열 자원으로, 일반 버튼 최소 높이를 공통 토큰으로 이관했다. 별도 메모리 항목과 좁은 320×240dp 영역, 글자 크기 200%로 고령자 복습 항목을 스크롤·선택하는 Compose 계측 테스트를 추가했다. 이 테스트는 컴파일만 했으며 실제 잘림·다양한 비율·기기 화면의 접근성 검증을 완료한 것은 아니다. 나머지 화면의 상태·이벤트와 문자열·컴포넌트 분리는 계속 필요하다.
- release 격리: 기존 네 표식에 두 번째 가상 경로 표식을 추가하고 DEX뿐 아니라 APK의 모든 압축 해제 항목을 UTF-8·UTF-16LE의 알려진 표식으로 검사했다. demo에서는 다섯 표식이 있고 release에서는 없었다. 별도 가짜 APK의 비 DEX 자산에 UTF-16LE 표식을 넣자 검사에서 거부됨도 확인했다. 난독화·다른 인코딩·표식 없는 구현 및 실제 실행 중 격리는 이 검사로 증명되지 않는다.
- 테스트·빌드: 별도 `build/privacy-ui-isolated`에서 단위 테스트 80개 통과, debug·demo·release 및 계측 테스트 APK 빌드와 `lintDebug` 성공. lint는 오류 0·경고 7이고 라이브러리 lint 검사 일부가 API 불일치로 건너뛰어졌다. 기존 APK 20개 SHA-256은 전후 일치한다. 사용자 설정·복습·백업, 설치 APK, `run-app.cmd`, `.idea/`는 변경하지 않았다.
- 에뮬레이터·기기·실차: 이전 ADB 접근 거부가 해결된 근거가 없어 재접근하지 않았고 모두 미실행이다. 저장 마이그레이션 등의 기존 단위 테스트는 통과했지만 계측·실제 저장장치 동작을 새로 검증한 것은 아니다. release 서명, Pleos 마켓, 실차 안전 검증과 SDK 로그 위험은 남아 있어 출시 준비 완료가 아니다.

## 2026-11-08 Phase 통합 후속 — 부분 구현 및 검증

- 경로 조사: [NaviHelper 3.0.5 공식 소개](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/intro), [경로 옵션](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/routes), [이벤트](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/event-listeners) 및 로컬 3.0.5 소스의 공개 요청·리스너를 대조했다. 경로 옵션·현재 상태·TBT 등의 API 존재는 확인했으나 비교 후보별 도로 폭·좁은 구간을 제공한다는 근거는 찾지 못했다. 실제 후보 데이터 수신은 검증하지 않았고 실제 경로 점수를 만들지 않았다. 이는 모든 SDK 버전에서 영원히 제공되지 않는다는 뜻이 아니다.
- SDK 구현 변경: 설치된 소스 자료에는 Vehicle 오류 코드가 있으나 현재 컴파일 경로에서 해당 타입을 참조할 수 없어 코드별 분류는 적용하지 않았다. `SecurityException`만 권한 거부로 분류하고 나머지 예외는 일반 오류로 처리하며 원인을 추정하지 않는다. 차량 상태 조회에서는 한 도메인 예외가 다른 도메인의 결과를 덮지 않도록 분리하고 취소는 전파한다. 와이퍼와 차량 상태의 SDK 읽기 및 해제를 IO 스레드로 이동했다. 운전석 문 조회는 읽기 불가와 영역 미보고도 구분한다. 실제 신호·오류 수신은 아직 확인하지 않았다.
- 레슨 구현 변경: 공조·김서림의 차종별 설명서 확인 안내를 펼쳐 볼 수 있게 했으며 `신호 확인 필요`와 완료 판정 잠금을 유지한다. 실제 신호를 가정하거나 제어 명령을 보내지 않는다. 와이퍼 완료 판정과 복습 저장은 유지한다.
- 개인정보: 앱의 진단 코드는 위치·목적지·즐겨찾기 값 자체를 기록하지 않도록 유지했다. SDK 소스에는 오류 메시지를 기록하는 경로가 있어 SDK 및 서비스 전체 로그 내용이 항상 비민감하다고 보장하지 못한다. 이번 작업에서는 내비 요청이나 사용자 데이터를 실행·기록하지 않았다.
- 테스트·빌드: 기존 출력과 분리한 `build/phase-followup-isolated`에서 `testDebugUnitTest` 79개 통과, debug·demo·release 및 `assembleDebugAndroidTest` 빌드 성공. 계측 테스트 APK는 컴파일만 했고 기기에서 실행하지 않았다. `lintDebug`는 오류 0·경고 7: 라이브러리 lint 검사 API 불일치 1, 주차 모니터의 비공개 API 반사 사용 1, 의존성 업데이트 제안 5. 해당 라이브러리의 일부 lint 검사는 건너뛰어졌다. Kotlin 컴파일러의 주차 API deprecated 및 복습 모델 `copy()` 가시성 경고도 남는다.
- release 분리: 새 demo·release APK의 DEX에서 시연 경로·진단 화면·내비 진단·옵션 추적에 해당하는 네 표식이 demo에만 있음을 검사했다. 이는 전체 바이너리나 실행 시 분리를 보증하지 않는다. 기존 APK 16개의 SHA-256은 이 작업 전후 일치했다.
- 에뮬레이터·화면·200% 글자·Pleos 실제 신호·실차: 모두 미실행. 이전 ADB 접근 거부의 해결 근거가 없어 재접근하지 않았다. 동기 SDK 호출이 반환하지 않을 때 실제 취소·해제 시점은 검증되지 않았다. 사용자 설정·복습·백업과 설치 APK는 시험하지 않았다.
- 남은 일: SDK 오류 타입의 실제 배포 API와 권한·수명주기 검증, 실제 경로 후보 및 도로 속성 확인, 공조·김서림 신호와 레슨 완료 조건, 나머지 화면 상태/이벤트·리소스·공통 컴포넌트 이관 및 접근성 실측, SDK 로그의 민감정보 가능성 검토가 필요하다. release는 빌드만 확인했으며 배포 서명·Pleos 마켓 심사·실차 안전 검증이 없어 출시 준비 완료가 아니다.

## 2026-11-08 출발 전 문 점검 SDK 연결

- 구현: 주차 FeatureGate가 허용하는 동안 사용자가 `네 문 상태 조회`를 누르면 Vehicle SDK의 문 위치 capability와 네 영역의 현재값만 읽는다. 앞 왼쪽·앞 오른쪽·뒤 왼쪽·뒤 오른쪽이 모두 읽기 가능하고 각 범위가 `0..1`이며 값이 `0`일 때만 문 항목에 `SDK로 확인됨`을 표시하고 자동 체크한다. 문 하나라도 열림·미지원/영역 미보고·권한 거부·시간 초과·오류·값 없음·해석 불가이거나 결과가 부분적이면 자동 확인하지 않는다. 잠금 확인이나 차량 제어 명령은 보내지 않는다. 공식 [Capability 안내](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/check-capability)와 [Door Position 예제](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app), 로컬 Vehicle 2.3.2 소스의 조회 API와 네 영역 상수를 대조했다. 차량별 범위와 지원 여부는 런타임 capability로 확인한다.
- 상태: 조회 완료 시각과 마지막 조회값이라는 안내를 표시하며 문별 응답 시각은 다를 수 있다. 조회 시작 시점부터 최대 15초만 자동 확인에 사용하고, 약 200ms 주기의 화면 감시에서 오래된 값과 유효하지 않은 주차 증거를 해제한다. 주차 조건은 기존 FeatureGate의 P·정지·UX 허용과 2.5초 이내 증거다. 수동 체크는 SDK 자동 확인과 별도로 보관하며 사용자는 SDK 체크를 직접 해제할 수 있다. 수동 해제나 전체 초기화는 대기 중 요청도 무효화한다. 화면을 벗어나면 코루틴 취소와 reader 해제를 요청하고 세대 번호로 늦은 결과를 차단한다. 동기 SDK 호출이 반환되지 않는 경우 실제 서비스 해제 시점까지 보장하지는 못한다.
- 테스트: 순수 Fake 신호로 네 문 닫힘, 각 문 열림·미확인 및 모든 실패 유형, 누락 영역, 15초 경계와 주차 유실, 수동 체크 분리·해제·초기화, 이전 요청·화면 종료 후 늦은 응답을 검증했다. 주입한 Fake reader로 수동/SDK 표시·초기화·화면 이탈 및 대기 중 수동 해제를 확인하는 Compose 계측 테스트도 작성했으나 기기에서 실행하지 않았다.
- 빌드: 분리된 `build/door-check-isolated`에서 `testDebugUnitTest` 75개와 debug·demo·release 빌드, `assembleDebugAndroidTest`, `lintDebug` 성공. lint는 오류 0·경고 7이며 계측 APK는 컴파일만 했다. 기존 APK 12개는 빌드 전후 SHA-256이 각각 같았다. 사용자 데이터·설치된 APK·`run-app.cmd`·`.idea/`는 건드리지 않았다.
- 기기 및 한계: ADB 접근 거부의 해결 근거가 없어 다시 접근하지 않았다. **Pleos 에뮬레이터, 실제 차량, 화면·접근성 및 실제 네 문 capability/상태와 서비스 취소·해제는 미실행**이다. SDK 응답은 일회성 마지막 조회값이며 자동 확인이 현재 차량 상태나 출발 안전을 보장하지 않는다.

## 2026-11-08 오늘 복습과 상세 안내 제안

- 홈은 저장 동의가 있고 복습 저장소가 정상일 때 저장된 와이퍼·경고등 학습 항목을 갱신 시각 순으로 최대 3개 보여준다. 고령자 유형은 최신 항목 1개를 더 큰 버튼으로 보여준다. 각 버튼은 와이퍼 연습 또는 선택한 경고등 사전 항목으로 연결한다. 동의가 없거나 손상·미래 버전·쓰기 결과 불명인 경우 항목을 읽어 표시하지 않으며 홈에서 기록을 새로 만들지 않는다. `오늘`은 날짜별 학습 여부가 아니라 홈에 표시하는 복습 목록의 이름이다.
- 반복 판정 기준: 보존된 최근 와이퍼 이력의 **가장 최근 네 이벤트**가 최신순으로 `직접 중단 B → 시작 B → 직접 중단 A → 시작 A`이고, A와 B의 회차 UUID가 서로 다르고 각각 이력에 정확히 두 번만 등장하며 시간 순서가 맞아야 한다. 각 시작·중단에는 이 구현이 기록한 형식 표시와 동일한 회차 ID가 필요하다. 완료, 중복 ID, 추가 이벤트, 역순 시각, 기존 식별자 없는 기록 또는 불확실한 저장 결과가 끼면 제안하지 않는다. 와이퍼 화면의 `연습 중단 · 홈으로`를 사용해 저장된 명시적 중단만 해당하며, 시스템 뒤로 가기·프로세스 종료·신호 오류만으로는 중단을 추정하지 않는다. 시작과 완료를 각각 미완료 회차로 계산하지 않는다.
- 제안은 상세 안내가 꺼져 있고 설정이 쓰기 가능한 경우에만 표시한다. 사용자가 `상세 안내 켜기`를 누른 뒤 확인창에서 `켜기`를 선택할 때만 설정 저장을 호출한다. 취소·닫기·나중에 보기는 설정을 쓰지 않고, 같은 근거의 제안은 현재 화면 상태에서 다시 보이지 않는다. 설정 저장은 잠금 안에서 최신 설정을 읽고 상세 안내 값만 변경해 글자 크기·화면 모드·대비·경로 선호와 복습 기록을 유지한다. 실패 또는 결과 불명에는 성공 문구를 보여주지 않고 기존 보호 경로를 사용한다.
- 검증: JVM Fake 테스트로 동의 없음·빈 목록에서 읽기만 하는지, 고령자 1개/초보자·직접 설정 최대 3개, 정렬, 명시적 중단 두 회차, 완료·기존 이력·식별자 중복·모호한 순서, 취소 시 무쓰기, 수락과 다른 설정 보존, 실패·결과 불명 및 같은 기록 재시도를 확인했다. 와이퍼 완료가 감지될 때 기록 처리가 화면 상태 변경으로 먼저 취소되지 않도록 완료 처리 후 화면 상태를 바꾸며, 실제 기기 동작은 미검증이다. 테스트는 Android JSON 대신 기존 테스트 전용 대체 구현을 사용했다. 고유한 별도 저장소의 Android 계측 테스트와 in-memory Compose 취소·표시 테스트를 작성했으나 기기에서 실행하지 않았다.
- 빌드: 기존 APK와 분리된 `build/today-isolated`에서 `testDebugUnitTest` 69개, `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug` 성공. lint는 오류 0·경고 7이며 계측 APK는 컴파일만 했다. 기존 앱 출력과 이전 분리 출력의 APK 8개는 빌드 전후 SHA-256이 각각 같았다. 사용자 데이터·설치된 APK·`run-app.cmd`·`.idea/`는 건드리지 않았다.
- 한계: 이전 ADB 접근 거부의 해결 근거가 없어 재접근하지 않았다. 에뮬레이터·실차, 실제 기기의 화면 표시·접근성·200% 글자 크기 및 제안 수락 흐름은 미실행이다. 이전 이력에는 회차 ID가 없어 그 이력만으로 반복 미완료를 판정하지 않는다. 기록 실패 후 프로세스가 종료되면 결과 불명 보호의 메모리 정보는 사라질 수 있으며 디스크 복구와 물리적 영속성은 보장하지 않는다.

## 2026-11-08 공통 복습: 와이퍼와 경고등 학습

- 구현: 복습 스키마 2에 종류·항목 ID·상태·갱신 시각을 가진 `review_items`와 와이퍼 공통 이력 `review_history`를 추가했다. 정상 레거시/버전 1에서 옮길 때 기존 와이퍼 최신 필드와 `history` 원문을 그대로 남기고, 공통 이력에는 출처와 원래 이력 객체도 보존한다. 형식 변환은 같은 SharedPreferences 편집의 단일 `commit()`이며 반복 실행은 쓰지 않는다. 원본·공통 기록이 불일치하거나 예약 필드 충돌·손상·미래 버전이면 자동으로 덮어쓰지 않는다. 변환 크기 제한 등으로 공통 이력을 안전하게 만들 수 없으면 원본을 보존하고 새 형식으로의 전환을 거부한다. 이 경우 해당 데이터의 전환은 미완료이며 안내가 표시된다.
- 기존 와이퍼 이력은 마이그레이션 시 잘라내지 않는다. 이후 새 와이퍼 연습을 저장하면 기존과 같은 최근 20개 이력 정책을 적용하며, 유지되는 기존 이력 객체의 추가 필드도 보존한다. 경고 항목은 최대 255개이며 항목 ID가 현재 사전에 없는 경우에도 삭제 전까지 보존하고 찾을 수 없다고 표시한다. 이전 버전 앱이 스키마 2를 읽으면 미래 버전으로 보호하므로 구버전과의 양방향 편집은 제공하지 않는다.
- 경고등 사전의 명시적 `복습에 추가`만 학습 항목을 저장한다. 동의가 꺼져 있으면 이유를 설명하고 동의·추가 또는 취소를 선택한다. 중복 추가는 새 기록을 만들지 않는다. 내 복습은 와이퍼와 경고등 학습을 구분하고 경고 항목을 해당 사전 화면으로 다시 연다. 경고 기록은 **차량 감지 신호가 아닌 사용자 선택 학습 항목**이다.
- 와이퍼 삭제는 와이퍼 최신 기록과 이력만, 경고 삭제는 선택한 경고 항목만 확인 후 삭제한다. 저장 동의 철회는 와이퍼·이력과 모든 경고 학습 항목을 삭제한다는 확인을 거친다. 기존 손상·미래 버전 보호와 결과 불명 상태의 다른 쓰기 차단·같은 작업 재시도는 유지한다. `commit()` 결과가 불명일 때 원본이 반드시 복원되었다고 단정하지 않는다.
- JVM Fake 테스트는 무버전·버전 1 와이퍼 원본/이력과 공통 이력 이관, 반복, 추가 필드 보존, 동의·중복·삭제 범위, 실패·예외·결과 불명·취소·재시도, 손상·미래 버전 및 불일치 보호를 검증했다. Android 실제 JSON/SharedPreferences 및 Compose 동의·취소를 고유 이름의 별도 저장소로 검사하는 계측 테스트도 작성했으나 기기 실행은 하지 않았다. JVM은 테스트 전용 JSON 대체 구현을 사용한다.
- 검증: 분리한 `build/review-isolated` 출력에서 `testDebugUnitTest` 61개, `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug` 성공. 계측 APK는 컴파일만 했으며 lint 기존 경고가 남는다. 이전 ADB 접근 거부의 해결 근거가 없어 재접근하지 않았고, 에뮬레이터·실차·200% 화면 검증은 미실행이다. 기존 사용자 데이터와 설치된 APK를 건드리지 않았고, 빌드 전후 기존 `app/build/outputs/apk`의 APK 4개 SHA-256이 각각 일치했다. `run-app.cmd`, `.idea/`는 수정하지 않았다.
- 한계: 실제 Android 저장소의 장애·프로세스 종료 및 물리 디스크 복구는 검증하지 않았다. SharedPreferences는 메모리와 디스크가 다를 수 있어 결과 불명 시 새로운 화면 값을 저장 성공으로 취급하지 않으며, 재시도 정보는 프로세스 종료 후 사라진다. 새로운 화면·삭제 확인·경고 사전 이동의 실제 기기 접근성/배치 검증은 미실행이다.

## 2026-11-08 lint 경고 분류와 정리

- 기준 `lintDebug`: 오류 0, 경고 **22**, 힌트 **2**. 아래는 수정 전 보고서에 나온 모든 항목의 규칙·파일별 분류다. 건수는 해당 규칙의 보고 건수다.

| 구분 | 규칙·건수 | 파일과 판정 |
|---|---|---|
| 앱 코드 정리 | `RedundantLabel` 1 | `app/src/main/AndroidManifest.xml`: Activity가 application과 같은 이름을 중복 지정. 중복 제거 후 application 라벨을 상속한다. |
| 앱 코드 정리 | `ObsoleteSdkInt` 1 | `ui/theme/Theme.kt`: minSdk 34에서 Android 12 이상 검사가 항상 참. dynamicColor 선택 분기 자체는 유지했다. |
| 앱 리소스 정리 | `UnusedResources` 7 | `res/values/colors.xml`: `purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`, `black`, `white`. 전체 앱 소스에서 정적 참조와 `getIdentifier` 사용을 검색한 후 사용되지 않는 선언만 제거했다. |
| 빌드 설정 정리 | `UseTomlInstead` 3 | `app/build.gradle.kts`: Vehicle, NaviHelper, error-prone annotations 좌표를 `gradle/libs.versions.toml`로 이동. Gson도 같은 방식으로 이동했으며 모든 좌표·버전 및 Gson의 비전이 설정은 유지했다. |
| 앱 코드 힌트 | `AutoboxingStateCreation` 2 | `MainActivity.kt`의 글자 크기 슬라이더 Float, `NavigationSession.kt`의 시각 Long을 각각 원시형 Compose state로 변경했다. 값과 동작은 유지하도록 했다. |
| 기기 확인 필요 | `PrivateApi` 1 | `ParkingMonitor.kt`: 선택적 `android.car.Car` 존재 확인 리플렉션. 라이브러리 부재·불일치에서 주차 기능을 차단하는 경로와 관련되어 유지했다. 비 SDK 접근은 기기·플랫폼별 제약 가능성이 있으며, 대체 시 Pleos 및 실차에서 라이브러리 부재·연결 해제·안전 차단을 검증해야 한다. |
| 의존성·도구 | `ObsoleteLintCustomCheck` 1 | 변환된 `lifecycle-runtime`의 `lint.jar`: 현재 lint API와 맞지 않아 `UnsafeLifecycleWhenUsage`, `RepeatOnLifecycleWrongUsage` 검사가 **실행되지 않았다**. Lifecycle/lint 조합을 향후 호환 버전으로 검토하고 빌드·생명주기 회귀 확인이 필요하다. |
| 업데이트 안내 | `AndroidGradlePluginVersion` 1, `NewerVersionAvailable` 1 | `gradle/libs.versions.toml`: 보고서는 AGP 9.5.0→9.5.1, Kotlin Compose 플러그인 2.2.10→2.4.21을 제안했다. 적용하지 않았으며 Gradle·Kotlin·Compose 및 Pleos SDK 호환성 확인이 필요하다. 이 제안은 해당 lint 보고서 시점의 정보다. |
| 업데이트 안내 | `GradleDependency` 6 | `gradle/libs.versions.toml`: core-ktx 1.10.1→1.19.1, test ext junit 1.1.5→1.3.0, espresso 3.5.1→3.7.0, lifecycle-runtime-ktx 2.6.1→2.11.0, activity-compose 1.8.0→1.13.0, Compose BOM 2026.02.01→2026.09.00을 보고서가 제안했다. 적용하지 않았으며 앱·테스트 API와 Compose/Lifecycle 조합, 기기 동작의 회귀 확인이 필요하다. |

- 수정 후 `lintDebug`: 오류 **0**, 경고 **10**, 힌트 **0**. 남은 경고는 위 `PrivateApi` 1, `ObsoleteLintCustomCheck` 1, 버전 안내 8이다. 기준 보고서에 접근성 전용 lint 경고는 없었으며, 경고 억제나 lint 설정 완화는 하지 않았다.
- 검증: `testDebugUnitTest` 53개 통과, `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug` 성공. 계측 APK는 컴파일만 했다. Kotlin 컴파일 경고와 네이티브 라이브러리 strip 경고는 남아 있으며 위 lint 건수와는 별개다. Release 서명·설치·실행은 확인하지 않았다.
- 기기 접근: 기존 ADB 거부의 해결 근거가 없어 재시도하지 않았다. 에뮬레이터·실차, 200% 글자 크기에서의 화면 배치·접근성, 와이퍼·복습 UI 회귀는 미실행이다. 사용자 데이터, 기기에 설치된 APK, `run-app.cmd`, `.idea/`는 건드리지 않았다. Gradle 빌드는 `app/build/outputs`의 로컬 APK 산출물을 다시 생성했다. 빌드 전 산출물의 별도 사본은 만들지 않았으므로 그 로컬 산출물의 보존을 주장하지 않는다.

## 2026-11-08 복습 JSON 실패·재시작 검증 후속

- 구현: 복습 저장 상태 검사와 기록·이력 읽기를 동일한 SharedPreferences 잠금 안에서 수행해, 기록 중간에 메모리 값을 정상 상태로 읽는 경합을 줄였다. 기존 파일 형식과 마이그레이션은 바꾸지 않았다.
- Fake 검증: 복습 시작·완료·삭제·동의 철회마다 `commit()` 실패와 예외를 주입했다. 미기록 모형에서는 이전 디스크 기록과 별도 백업 모형이 유지되고, 불명 상태에서 다른 쓰기와 미확인 읽기가 차단되며 재시도 취소는 쓰기를 수행하지 않는다. 명시적 재시도는 처음 요청한 값으로 기록한다. 실패 보고에도 이미 디스크에 반영됐을 수 있는 모형도 검증했다. 따라서 실제 실패 후 원본이 반드시 보존된다고 보장하지 않는다.
- 프로세스 재시작: 쓰기 미반영·반영 각각의 디스크 스냅샷으로 새 저장소 객체를 만들어, 재시도 정보가 사라지고 별도 쓰기 없이 스냅샷만 읽는 상황을 모의했다. 손상 JSON과 미래 스키마 스냅샷은 계속 보호되는지 확인했다. 실제 프로세스 강제 종료나 파일 시스템 장애는 실행하지 않았다.
- UI: 결과 불명 안내에 이미 기록됐을 가능성, 재시도하지 않아도 이전 작업이 취소되거나 복원되지 않는다는 점, 재시작 후에도 이전 작업의 성공 여부를 알 수 없다는 점을 표시한다. 재시도 전 작업 이름과 삭제 가능성을 보여주는 확인창에서 취소할 수 있다. 코드에서 손상·미래 버전은 별도 분기로 유지한다. 불명 상태 확인창의 취소·재시도와 손상 안내 부재를 검사하는 Compose 계측 테스트를 작성하고 컴파일했지만 기기에서 실행하지 않아 실제 표시·접근성이나 사용자의 이해는 확인하지 못했다.
- 실행 결과: `testDebugUnitTest`, `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug` 성공. 복습 JSON 실패·예외·재시작 Fake 테스트 4개 통과. JVM에서는 Android `org.json` 대신 테스트 전용 Gson 기반 대체 구현을 사용했으므로 Android JSON 구현 자체와 플랫폼 디스크 동작을 검증한 것은 아니다. 테스트 전용 의존성만 추가했다. 기존 lint·컴파일 경고가 남아 있다.
- 기기 및 한계: 이전 ADB 접근 거부의 해결 근거가 없어 재접근하지 않았다. 에뮬레이터 계측·실차 검증은 미실행이며 사용자 저장소·권한·시작 APK를 변경하지 않았다. 재시도 정보는 여전히 프로세스 메모리에만 있고, 재시작 이후 이전 작업의 성공 여부·원본 자동 복구·부분 기록·전원 장애나 여러 파일의 원자성은 보장하지 않는다.

## 2026-11-08 디스크 기록 결과 처리 후속

기존 SharedPreferences 파일과 스키마를 유지한다. `apply()` 쓰기를 결과를 확인하는 `commit()`으로 바꾸고, Android가 `true`를 반환한 경우에만 성공으로 취급한다. `false` 또는 예외는 실제 기록 여부가 **불명**이며 메모리 읽기로 판정하지 않는다. 같은 프로세스의 같은 저장소 객체들은 불명 상태를 공유하여 다른 쓰기를 차단하고, 사용자가 명시적으로 선택한 경우에만 원래 작업을 재시도한다. 재시도를 선택하지 않아도 이전 작업이 취소·롤백되었다고 주장하지 않는다. 잘못된 입력이나 보호된 스키마는 쓰기 전에 거부한다. 커밋은 UI에서 IO 스레드로 실행한다.

| 경로 | 현재 판정과 보호 |
|---|---|
| 설정 변경·음성·일반 초기화 | `commit()` 성공 / 불명 / 사전 거부. 불명 시 설정 변경을 막고 작업을 표시하여 재시도 안내. 화면에는 이전에 확인한 값 또는 임시 값이 남을 수 있다. |
| 설정 파일·내부 백업 복원 | 현재 설정 저장과 같은 보호기 사용; 성공 반환 뒤에만 복원 성공 표시. 파일 선택 취소는 쓰지 않는다. |
| 설정 형식 갱신·손상 초기화 | 정상 레거시만 갱신; 손상 원본 미리보기와 명시적 확인, 미래 버전 보호를 유지. 실패 시 불명 상태로 격리한다. |
| 복습 동의·철회, 기록 저장·삭제 | 각 단일 저장소 `commit()` 결과를 확인한다. 불명 시 기록·동의 상태를 확정해 표시하지 않고 다른 쓰기를 차단한다. 철회는 기록 필드만 제거하고 알 수 없는 필드를 보존한다. |
| 복습 형식 갱신·손상 초기화 | 정상 레거시만 갱신, 손상 확인 및 미래 버전 보호 유지. 불명 상태는 같은 작업 재시도 또는 프로세스 재시작 후 확인을 안내한다. |
| 내부 백업 저장·교체 | 기존 백업이 손상되어 읽히지 않으면 덮어쓰지 않는다. 실패 후 메모리 백업을 디스크 결과로 취급하지 않고 다른 백업 쓰기·복원을 차단한다. |
| SAF 파일 내보내기 | 출력 스트림 닫기까지 오류를 확인하되 제공자가 디스크에 영속 기록했는지는 확인하지 못하므로 파일 위치 확인을 안내한다. |

- 테스트: `SharedPreferences` 인터페이스 Fake로 설정·복습 삭제/초기화·내부 백업의 정상 결과, 메모리만 변경된 실패, 실제 디스크 모형에도 반영된 결과 불명, 재시도, 손상·미래 버전 보호와 알 수 없는 필드 보존을 확인한다. 공통 보호기 Fake는 사전 거부, 예외, 미확정 중 다른 쓰기 차단, 재시도 취소 및 거부된 재시도도 확인한다. 프로세스 재시작은 두 가지 디스크 스냅샷으로만 모의했다. 복습 JSON 기록 저장의 실패 주입과 실제 Android SharedPreferences 구현의 중단·재시도는 기기에서 검증하지 못했다.
- 빌드: Windows에서 `testDebugUnitTest`, `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug` 오프라인 성공. 추가 Fake 테스트 13개 통과; 계측 APK는 컴파일만 했다. lint 및 Kotlin 경고는 남아 있다. release 서명·기기 실행은 확인하지 않았다.
- 한계: 보호기와 재시도 내용은 프로세스 메모리에만 있다. 프로세스 종료 후에는 디스크에 남은 내용을 다시 읽을 뿐 실패한 작업의 이전 값이나 기록 여부를 자동으로 복구하지 못한다. XML 손상 전체 감지, 전원 장애·부분 기록·저장장치의 물리적 영속성, 여러 파일에 걸친 원자적 복구는 보장하지 않는다. 다른 프로세스나 앱 외부에서 같은 파일을 수정하는 경우도 다루지 않는다. 새로운 저장 형식이나 별도 마이그레이션은 도입하지 않았다.
- 에뮬레이터·실차: 이전 실행에서 ADB 접근이 거부되어 이번에는 재시도하거나 우회하지 않았다. 기기 설치·계측 테스트·실차 테스트는 미실행이다. 사용자 설정·복습·백업, 기기의 시작 APK, `run-app.cmd`와 `.idea/`는 이번 작업에서 변경하지 않았다.

## 2026-11-08 이전 디스크 기록 실패 점검 당시 — 미완료 기록

- 구현: 내부 백업 `commit()` 실패 후 SharedPreferences 메모리 값을 새 백업으로 표시하지 않도록 하고, 파일 출력과 설정 복원·음성 설정 문구에 디스크 확인의 한계를 표시했다. 기존 저장소 알고리즘과 손상·미래 버전 검사, 사용자 데이터는 변경하지 않았다.
- 확인된 미해결: 설정 저장과 복습 동의·기록·삭제는 `apply()`를 사용해 디스크 성공을 감지하지 못한다. 설정·복습의 손상 초기화와 내부 백업은 `commit()` 결과를 확인하지만 실패해도 메모리는 달라질 수 있다. 이후 읽기와 재시도에서 디스크 상태를 확정하거나 기존 백업 및 손상 원본의 복구를 보장하지 못한다. 일반 설정 UI, 복습 UI, 마이그레이션 및 초기화에도 실패 안내와 상태 관리가 필요하다. 저장소 인터페이스·Fake를 이용한 실패/재시도/취소 테스트는 아직 구현하지 않았다. 따라서 요청한 실패 처리 작업은 완료되지 않았다.
- 빌드: 최종 문구 변경 후 Windows에서 `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest` 오프라인 성공. 기존 단위 테스트만 실행했으며 새 실패 주입 테스트는 없다. 계측 APK는 컴파일만 했다.
- 에뮬레이터: 이번 실행에서 ADB의 사용자 디렉터리 접근이 거부되어 기기 목록 확인과 기기 테스트를 하지 않았다. 기기 설치, 사용자 저장소 및 시작 APK 변경은 하지 않았다.
- 실차: 이번 실행에서 검증하지 않았다. 디스크 오류나 프로세스 종료 후 복구, SAF 제공자의 영속성은 검증하지 않았다.
- 참고: 첨부 자료의 프롬프트를 자동 실행하지 않았다. 공식 Vehicle SDK 설정 문서와 로컬 Gradle 캐시 모두 Vehicle 2.3.2를 확인했으며 SDK 버전은 변경하지 않았다.

기존 에뮬레이터 확인은 사용자가 전달한 이전 실행 결과이며 이번 수정본의 재검증이 아니다. 실차 확인은 전 항목 미실행.

| 기능 | 현재 할 수 있는 일 | 확인 상태 | 남은 이슈 |
|---|---|---|---|
| 사용자 유형·설정 | 프리셋·화면·경로 선호, 버전 1 마이그레이션 및 손상 보호 | 별도 저장소 계측 검증; 실제 사용자 설정 파일은 변경하지 않음 | 전체 설정 화면·실차 회귀 및 접근성 미검증 |
| 와이퍼·복습 | OFF→ON→OFF와 동의 후 기록, 레슨 어댑터 | 어댑터 단위 테스트, 이전 에뮬레이터 확인 | 이번 변경 후 Pleos 회귀 미실행 |
| 내부 백업 | 설정 백업/복원 | 이전 에뮬레이터 확인 | 이번 변경 후 회귀 미실행; 파일 선택기는 실패 원인 미확인 |
| 출발 전 점검 | 수동 체크 및 네 문 일회성 SDK 조회 | Fake 상태 단위 테스트; 계측 APK 컴파일 | 최신 변경의 Pleos·실차·접근성 검증 미실행 |
| 문 위치 | SDK 상태 조회 | 이전 에뮬레이터 확인 | 실차 미실행, 모든 오류 구분 미완 |
| 문 잠금·벨트·워셔액 | 읽기 및 capability·오류 구분 일부 구현 | Pleos 조회: 잠금 영역 미보고, 벨트·워셔액 일반 오류 | 실제 값·일반 오류 원인 미확인. 실차 미검증 |
| 주차·FeatureGate | P·정지·UX 제한 판정 및 재연결 시도 | 단위 검증, Pleos에서 홈 진입 확인 | 주행 전환·신호 유실·재연결 실기기 회귀 미완 |
| 내비게이션 | 옵션 요청·진단, demo 전용 가상 경로 비교 | 비교 단위 테스트·빌드, 이전 Pleos 연결 콜백 | 실경로 후보·변경 전후 실동작 미확인 |
| 경고 사전 | 일반적인 다섯 항목 학습 자료 | 파싱 단위 테스트·빌드 | 차종별 매뉴얼·실시간 감지·Pleos UI 미확인 |
| 음성 설정 | Android 인식기 있을 때 실행 | 코드 구현 | 서비스 이용 및 발화 실기기 미실행 |
| debug 진단 | 주차 중 차량 조회와 내비 요청·콜백 진단 | Pleos debug 화면 및 일부 신호 확인 | demo·전체 신호·상세 capability·오류 원인 검증 필요 |
| 시연용 빌드 | demo 빌드 타입·시연용 고정 표시 | 코드 구현, 빌드 결과는 아래 참조 | 시연 데이터 미구현, release 내용물 검증 필요 |

## 출시 준비도

- 안정성: **미충족** — 일부 신호 오류 분류를 확장했으나 SDK 전체 IO·오류 분류·재연결 보장은 미완.
- 생명주기: **미충족** — 와이퍼 초기화 실패 정리 보강; 허브와 모든 구독의 기기 수명·경합 검증 필요.
- 데이터: **미충족** — 기존 와이퍼 복습의 비파괴 버전 처리와 손상 기록 확인·명시적 초기화를 보강했으나 공통 복습 스키마·모든 손상 형식의 복구는 미완.
- 테스트: **미충족** — 선택 단위 테스트와 분리 저장소 계측 테스트를 실행했으나 전체 SDK 및 기기 회귀 미완.
- 접근성: **미확인** — 복습 동의 스위치의 이름을 추가했으나 전체 터치 영역·대비·200% 기기 검증 미완.
- 문자열/현지화: **미충족** — 상당수 하드코딩 잔존.
- 권한: **미확인** — 서비스별 실제 권한 결과와 사유 UX 검증 필요.
- 빌드/서명: **미충족** — demo 기기 검증 및 release 난독화·서명 점검 필요.
- Pleos 마켓: **확인 필요** — 실제 심사·차량 검증은 수행하지 않았다.
- 디자인 교체 구조: **미충족** — 토큰은 출발점만, UiState/UiEvent·컴포넌트·에셋 매핑 미완.

Phase 0는 완료되지 않았다. 출시 가능한 제품으로 판단할 근거가 없다.

## 2026-11-07 후속: 사용자 설정 스키마와 손상 보호

- 설정 SharedPreferences에 스키마 버전 1을 추가했다. 버전이 없는 정상 레거시는 기존 필드와 사용자 유형별 기본값을 유지해 원자적으로 마이그레이션하며, 반복 실행 시 동일한 값을 유지한다. 글자 크기는 미지정이면 유형 기본값을 사용하는 선택 필드다. 알 수 없는 별도 키는 보존한다.
- 잘못된 자료형, 범위 밖 값, 버전 1의 필수 항목 누락, 미래 버전을 별도로 표시하고 설정 변경·음성 저장·백업 내보내기/복원을 막는다. 미래 버전은 손상으로 단정하거나 초기화하지 않는다. 버전 없는 레거시의 누락은 과거 형식의 기본값으로 처리하되 잘못된 값은 보호한다.
- 손상 시 기기 화면에서 원본을 먼저 확인한 뒤 별도 확인을 해야 현재 설정만 기본값으로 바꾼다. 취소하거나 확인 전에 값이 바뀌면 원본을 유지한다. 미리 보기가 4096자를 초과하면 초기화를 막는다. 기존 내부 백업과 복습은 초기화 대상이 아니다. 손상된 내부 백업을 없는 백업으로 오인해 덮어쓰지 않도록 화면에서 저장을 차단한다.
- 파일 백업의 `pleos-assist-settings-v1` 형식과 기존 내부 백업 형식은 유지했다. 정상 레거시·유형별 기본값·반복 마이그레이션, 오류 유형·미래 버전, 초기화 취소/확인·변경된 미리보기, 내부/파일 백업 복원은 고유 테스트 저장소의 Pleos 계측 테스트 7개 통과. 순수 검증·파일 백업 선택 JVM 테스트 4개 통과. 기존 사용자 데이터와는 별도이며 테스트 저장 파일은 삭제하지 않았다.
- debug/demo/release 빌드, 계측 APK 컴파일, lintDebug 통과(오류 0, 경고 26). release의 알려진 demo/진단 표식 4개 검사 통과; 전체 release 격리·서명·실행은 미검증이다. UI 테스트는 독립 테스트 화면이며 전체 설정 화면·파일 선택기·실차 회귀는 미실행이다. SharedPreferences가 손상된 XML을 빈 값으로 반환하는 경우의 판별과 디스크 기록 실패 후 복구는 미검증이다.
- Pleos 사용자 10의 기존 설정·복습·내부 백업 파일 해시는 검증 전후 각각 일치했다. 테스트 시작 시의 앱·테스트 APK를 데이터 유지 설치로 되돌려 해시 일치를 확인했으며 같은 앱을 다시 열었다. 화면 내부 상태는 복원 여부 미확인이고, 현재 설치된 앱은 새 수정본이 아닌 테스트 전 APK다. 기어·와이퍼·Virtual Console은 조작하지 않았다.

## 2026-11-07 후속: 복습 손상 안내와 확인 후 초기화

- 정상·손상·미래 스키마를 구분한다. 손상 시 원본 내용을 기기 화면에 표시한 뒤 사용자가 별도 확인 창에서 명시적으로 초기화한 경우에만 기존 복습 필드를 제거한다. 취소하거나 미리 본 뒤 저장 내용이 바뀌면 원본을 유지한다. 유효한 동의와 알 수 없는 별도 필드는 유지한다. 미리 보기가 4096자를 초과하면 일부만 표시하고 초기화를 막는다.
- 미래 스키마는 손상으로 표기하지 않고 읽기 전용으로 보존한다. 저장·삭제·동의 변경 및 손상 초기화 경로로 수정하지 않는다. 손상 또는 미래 버전에서는 새 연습 기록 저장도 차단한다. 파일 수준 XML 손상의 완전한 판별·복원과 초기화 디스크 쓰기 실패 상황은 미검증이다.
- 정상 레거시 기록, 필드 누락·0 이하 시각, 미래 버전, 원본 보기 후 취소·명시적 확인, 변경된 미리보기 거부, 손상 데이터 덮어쓰기 방지를 테스트했다. 선택 JVM 테스트 2개 및 고유 테스트 저장소를 쓰는 Pleos 계측 테스트 5개 통과. 고유 테스트 저장 파일은 삭제하지 않았다.
- debug/demo/release 빌드, 계측 APK 컴파일, lintDebug 통과(오류 0, 경고 26). Pleos에서는 독립 테스트 화면·저장소를 사용했으며 실제 사용자 복습 화면에 손상 데이터를 넣지는 않았다. 실차·200% 화면 전체·접근성 기기 회귀는 미실행이다.
- 기존 사용자 10 설정·복습 파일은 검증 전후 읽기 전용 해시가 각각 같았다. 테스트 시작 시의 앱·테스트 APK를 데이터 유지 설치로 되돌리고 해시 일치를 확인했다. 앱은 다시 열었으나 화면 내부 상태 복원은 미확인이다. 기어·와이퍼·콘솔은 조작하지 않았다.

## 2026-11-07 후속: 복습 기록 무결성

- 복습 기록의 시각·완료 여부가 일부만 존재하거나 시각이 0 이하인 경우 기본값으로 완성하지 않고 손상으로 처리한다. 잘못된 자료형·미래 스키마도 기존과 같이 보호하며 지나치게 큰 이력 문자열은 파싱하지 않는다. 손상 기록은 자동 마이그레이션·새 연습 저장으로 덮어쓰지 않는다. 원본의 자동 복원이나 모든 XML 손상 감지는 아직 미구현이다.
- 정상 레거시·빈 기록·미래 버전과 불완전 기록을 구분하는 선택 JVM 테스트 2개 통과. Pleos 사용자 10의 고유 테스트 저장소에서 불완전 레코드가 마이그레이션·저장으로 바뀌지 않는 계측 테스트 1개 통과했다. 테스트 저장 파일은 삭제하지 않았다.
- debug/demo/release 빌드, 계측 APK 컴파일, lintDebug 통과(오류 0, 경고 26). demo/release DEX의 알려진 표식 4개 검사 통과; 전체 release 바이너리·실행 검증은 아니다.
- 테스트 전후 기존 사용자 설정·복습 파일 해시는 각각 동일했고, 검증 시작 시 앱·테스트 APK를 데이터 유지 설치로 되돌려 해시 일치를 확인했다. 포그라운드 앱은 다시 열었으나 화면 내부 상태는 미확인이다. 기어·와이퍼·Virtual Console은 조작하지 않았다. 와이퍼 ON/OFF 전환 및 실차 검증은 미실행이다.

## 2026-11-07 콘솔 조작 없는 후속 작업

- 차량 상태 허브의 읽기 함수가 예외를 내면 미확인 상태를 내보내 잠근 뒤 다음 조회를 계속한다. 코루틴 취소는 잡아서 재시도하지 않고 그대로 전파한다. 새 단위 테스트로 예외→미확인→다음 조회와 취소 전파를 확인했다. 이는 실제 차량 서비스의 연결 해제·복구 검증이 아니다.
- demo 경로 비교의 점수 계산을 64비트로 바꿔 큰 입력에서 정수 오버플로로 순위가 뒤집히는 것을 방지했다. 큰 입력을 포함한 단위 테스트를 추가했다. 실제 도로·경로 후보의 제공 및 안전 점수의 유효성은 여전히 미확인이다.
- `scripts/check-release-isolation.ps1`을 추가해 demo DEX에 있는 가상 경로·진단 표식 4개가 release DEX에 없는지 반복 확인할 수 있게 했다. Windows PowerShell 5.1에서 실행해 4개 표식 검사 통과. 표식 검사는 바이너리 전체나 실행 중 격리를 보증하지 않는다.
- `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug`와 관련 선택 단위 테스트 5개 통과. 린트는 오류 0개, 경고 26개. 계측 APK는 컴파일만 했으며 이번 변경의 Pleos 에뮬레이터·와이퍼 전환·실차 검증은 미실행이다. 이번 작업에서 사용자 데이터, 설치 앱, 기어·와이퍼 및 콘솔을 조작하지 않았다.

## 2026-11-07 와이퍼 읽기 검증과 중단 후 정리

- 중단 전 Pleos 사용자 10에서 현재 변경본의 `WiperReader` 읽기 전용 계측 테스트는 SDK `Value(false)`를 받았다. 이는 OFF 상태 한 번을 에뮬레이터 SDK로 읽었다는 뜻이다. ON→OFF 전환, 연습 화면 완료 판정 및 실차는 미검증이다. 브라우저 URL을 안전하게 판별하지 못해 Virtual Console 조작을 중단했으며 이후 우회하거나 재시도하지 않았다.
- 검증 직전 설치 앱 및 테스트 앱의 APK를 `phase0-before-recovery-test` 보관본 해시와 대조해 일치함을 확인했다. 중단 후 설치본은 검증용 빌드 산출물 해시와 각각 일치했다. 앱 패키지 `com.example.hyundiaboysranger`는 시작본과 검증본 모두 versionCode 1/versionName 1.0, 테스트 패키지는 모두 `com.example.hyundiaboysranger.test`이며 테스트 APK badging의 버전 필드는 비어 있었다(설치 정보 versionCode 0). 네 APK의 검증된 Android Debug 서명 인증서 SHA-256은 모두 동일했다.
- 두 보관본을 `adb install -r`로 재설치했다. 삭제·초기화는 하지 않았으며 복원 후 앱·테스트 APK 해시가 각각 검증 전 보관본과 일치했다. 사용자 10의 `user_settings.xml`, `practice_review.xml` 해시는 검증 전·정리 전·정리 후 각각 동일했다. 앱을 다시 열었으나 화면 내부 상태는 완전히 복원됐는지 확인하지 않았다. 더 이전에 보관하지 못했던 APK가 복구됐다는 뜻은 아니다.
- 검증 시작 시 ADB 포워딩 목록은 비어 있었고 중단 후 `emulator-5554 tcp:8088 tcp:8088`이 존재했다. 이번 작업에서 생성한 해당 포워딩만 제거했고 목록이 다시 비어 있음을 확인했다. 로컬의 빌드·감사 APK 사본과 테스트 파일은 삭제하지 않았다. 기어와 와이퍼를 변경하지 않아 차량 신호 복원 조작은 없었다.
- 후속 독립 작업으로 와이퍼 연습 화면의 안내·단계·상태·버튼 문구를 문자열 리소스로 옮겼다. 와이퍼 판정 로직은 변경하지 않았다. debug/demo/release 빌드, 계측 테스트 APK 컴파일, `lintDebug`, 와이퍼 판정 관련 선택 단위 테스트 3개 통과; 린트 경고 26개는 남아 있다. 이 문자열 수정본의 Pleos UI·와이퍼 전환 및 실차 시험은 미실행이다. 현재 에뮬레이터에는 검증 전 APK가 복원돼 있어 새 문자열 수정본이 설치된 상태가 아니다.

## Phase 1 — 경로 비교 (부분 구현)

- SDK의 실제 비교 후보·도로 폭·회전·좁은 구간 제공은 확인되지 않았다. 일반 빌드는 비교 불가를 표시하며 가상의 경로를 제공하지 않는다. 실제 큰길 선호 순위 반영과 선택 경로 연계는 미구현이다.
- demo 소스에만 가상 후보를 두고 선호에 따른 임시 순위를 표시한다. 점수는 실제 도로 데이터나 안전 판정이 아니다. debug/demo/release 빌드와 후보·선호 정렬 단위 테스트 통과. Pleos 시연·실제 SDK 비교는 미실행이다.

## Phase 2 — 레슨 기반 (부분 구현)

- 와이퍼 판정을 기존 상태 기계에 위임하는 레슨 인터페이스를 추가했고 공조·김서림은 신호 미확인으로 잠금 표시한다. 새로운 와이퍼 어댑터 판정 단위 테스트와 debug 빌드 통과. 기존 주차 게이트를 사용한다.
- 공통 단계별 시간 제한·힌트·건너뛰기·음성 및 새 레슨 실신호 확인은 미구현이다. Pleos 와이퍼 실신호 회귀와 주행 전환 중단은 미실행이다.

## Phase 3 — 경고 사전 (부분 구현)

- 안전벨트·워셔액·타이어·엔진·브레이크의 일반 학습 자료를 파일로 분리하고 사전 화면을 추가했다. 차종별 매뉴얼 확인 전 초안이며 실제 경고 감지나 심각도 판정이 아니다. 파싱 단위 테스트와 debug 빌드 통과.
- 실시간 경고 구독·음성·시연 이벤트·복습 연계, 프로필별 화면과 실차 매뉴얼 매칭은 미구현이다. Pleos 화면 및 실경고 검증은 미실행이다.

## Phase 4 — 복습·출발 전 점검 (부분 구현)

- 주차 중 수동 점검에서 복습과 경로 준비로 이동하는 버튼을 추가했다. debug 빌드 통과; 화면 이동의 기기 회귀는 미실행이다.
- 레슨·경고·경로의 공통 복습 스키마 확장·프로필 제안·문 자동 확인은 미구현이다. 아래 후속 작업에서 기존 와이퍼 기록의 비파괴 버전 표시만 추가했다. 문 신호가 수신되더라도 아직 자동으로 체크하지 않는다.

## Phase 5 — 주행 중 보조 (부분 구현)

- 기존 FeatureGate는 주차 근거가 없거나 만료되면 전체 조작 화면을 잠근다. 운전자 판단과 차량 기본 안내 우선 문구를 추가했으며 debug 빌드 통과. 이 잠금은 주행 중 앱 사용 승인이나 실차 검증을 뜻하지 않는다.
- TTS·오디오 포커스·음성 설정 반영·교차로/차선·ADAS 연동 및 demo 주행 스크립트는 미구현이다. 실제 P/D 전환과 오디오 동작은 미실행이다.

## Phase 6 — 인계·출시 점검 (부분 구현)

- `DEMO_SCRIPT.md`, `RELEASE_CHECKLIST.md`, `SCREEN_SPEC.md`, `DESIGN_HANDOFF.md`에 현재 기능과 제한을 반영했다. 차량 조도·야간 신호는 미확인으로 기존 시스템/수동 설정을 유지한다.
- 최종 `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug` 및 관련 선택 JVM 테스트 8개 통과. 계측 APK는 컴파일만 했다. 기존 린트 경고는 남아 있으며 추가한 경로·레슨·경고 화면 관련 린트 경고는 확인되지 않았다. release 병합 manifest에 휴대폰 미리보기 Activity와 debuggable 선언이 없는 것은 확인했으나 전체 바이너리의 시연·진단 제거 여부는 미검증이다.
- Pleos 앱 설치·사용자 저장 데이터·기어·차량 신호를 이번 작업에서는 변경하거나 실행하지 않았다. 새 화면의 Pleos/실차 회귀, 200%·접근성·성능 실측, 앱 전체 문자열·토큰 이관, 서명·배포 및 마켓 심사는 미실행 또는 미완이다. 출시 준비도는 여전히 미충족이다.

## 2026-11-07 후속: SDK 소스·마이그레이션·release 격리

- 설치된 NaviHelper 3.0.5 소스의 공개 요청·리스너와 `RouteStateInfo`를 살펴봤다. 현재 경로 상태·옵션 및 TBT 등의 API는 있으나 비교 후보별 도로 폭·회전 수·좁은 구간을 반환하는 공개 API는 확인하지 못했다. SDK 전체 또는 이후 버전에서 절대 제공하지 않는다는 뜻은 아니다. 실제 경로 점수는 미구현이다.
- Vehicle 2.3.2 소스에서 와이퍼 상태 capability/조회/구독, HVAC AC·defroster capability/구독 및 안전벨트·워셔액 조회/구독 API를 확인했다. API 존재는 차량별 권한·영역·실제 수신 확인이 아니며, 공조 레슨과 미확인 경고를 실제 동작으로 표시하지 않는다.
- 기존 와이퍼 복습 필드를 유지하며 유효한 레거시 기록에 스키마 버전 1을 추가한다. 이미 버전 1인 경우 재실행해도 내용이 유지되며, 손상됐거나 미래 버전인 경우 덮어쓰지 않는다. Pleos의 고유 테스트 저장소에서 마이그레이션·재실행·미래 버전 보호 계측 테스트 1개 통과. 레슨/경고/경로를 통합하는 새 저장 모델은 아직 없다.
- 출발 전 수동 점검 화면의 문구를 문자열 리소스로 옮겼다. Pleos에서 글자 배율 200%인 독립 Compose 테스트 화면의 첫 점검 항목이 이름을 가진 토글로 동작하는 계측 테스트 1개 통과. 전체 화면의 잘림·대비·스크린리더 및 터치 영역 실측은 미완이다.
- release DEX 검사에서 개발용 진단 화면 문자열과 로그 문자열이 남아 있는 것을 발견해 진단 화면·기록기를 빌드별 소스로 분리했다. 최종 demo DEX에는 가상 경로 표식과 진단 표식이 있고 release DEX에서는 검사한 4개 표식이 발견되지 않았다. 이는 선택한 표식 검사이지 바이너리 전체나 실행 중 격리의 완전한 증명은 아니다. release 서명·실행 검증은 미실행이다.
- debug/demo/release 빌드, 계측 APK 컴파일, lintDebug 및 선택 JVM 테스트 7개 통과. 와이퍼 OFF/ON/OFF·신호 유실 판정은 단위 테스트로 확인했다. 실제 와이퍼 신호의 에뮬레이터 회귀와 실차 검증은 미실행이며 린트 경고는 남아 있다.
- Pleos 검증 시작 시 사용자 10의 설정·복습 해시와 설치 APK를 기록했다. 마이그레이션은 고유 테스트 저장소만 사용했고 그 테스트 파일은 삭제하지 않았다. 접근성 테스트도 사용자 저장소를 변경하지 않았다. 각 검증 후 이번 시작 시의 앱·테스트 APK를 다시 설치해 해시 일치, 사용자 설정·복습 파일 해시 일치를 확인했다. 같은 앱을 다시 열었으나 화면 내부 상태는 복원 여부 미확인이다. 기어·차량 신호는 변경하지 않았다. 이전에 보관하지 못한 APK의 복구를 의미하지 않는다.

## 2026-11-07 Windows 후속 작업: 신호·저장·접근성 기반

- 문 위치와 와이퍼의 읽기 불가·영역 미보고·권한 거부·타임아웃·일반 오류·미확인 값을 구분한다. 와이퍼 초기화 실패 시 해당 객체를 해제하고 재사용하지 않으며, 미확인 상태는 연습 완료에 사용하지 않는다. 전 SDK 신호, 운전석 단독 조회, 주차 기어·속도, SDK 쓰레드·해제 경합 및 실제 서비스 재연결의 분류와 검증은 미완이다. 미지원이나 정상 여부를 원인 없이 추정하지 않는다.
- 설정 필드의 잘못된 자료형은 읽기 시 해당 필드만 기본값을 사용하고 원본을 자동으로 다시 쓰지 않는다. 기존 senior 설정에 누락된 대비·상세도 기본값은 유지한다. 내부 백업의 잘못된 자료형은 복원 불가로 처리한다. 손상된 복습 기록은 원본을 보존하고 새 연습이 덮어쓰지 않게 막으며 화면에 알린다. 전체 저장 스키마 버전 관리·원본 복구·손상된 XML 자체의 정확한 판별은 미완이다.
- 복습 저장 동의 스위치에 접근성 이름을 추가하고 관련 안내 문자열을 리소스로 분리했다. 전체 화면 문자열 현지화·스크린리더·200% 레이아웃·대비·터치 영역 실측은 미완이다.
- `assembleDebug`, `assembleDemo`, `assembleRelease`, `assembleDebugAndroidTest`, `lintDebug` 및 변경과 관련된 선택 JVM 테스트 6개 통과. 이미 완료한 Fake 주차 신호 테스트는 반복 실행하지 않았다. Pleos 사용자 10에서 고유한 테스트 저장소를 사용한 손상·기존 형식·복습 계측 테스트 9개 통과. 새로운 SDK 신호 조회·접근성 기기 회귀는 실행하지 않았다.
- Pleos 검증 시작 시 기존 앱과 테스트 APK, 포그라운드 앱 및 사용자 데이터 해시를 기록했다. 검증 후 **이번 실행 시작 시의** 두 APK를 다시 설치해 해시 일치를 확인하고 같은 앱을 열었다. 설정·복습 파일의 해시도 전후 일치했다. 차량 기어·신호는 변경하지 않았다. 화면 내부 상태의 완전한 복원은 확인하지 않았다. 그보다 이전 실행에서 보관하지 못한 APK가 복구됐다는 뜻은 아니다.

## 2026-11-07 Windows 후속 작업: Fake 주차 신호와 내비게이션 옵션

- 완료: Fake 입력을 `VehicleStateHub`와 `FeatureGate`에 연결해 주차 → 주행 → 신호 유실 → 오래된 신호 → 새 주차 신호를 검증했다. 읽기가 멈추면 마지막 증거가 2.5초를 초과한 뒤 잠기고 수집 취소가 끝나는지도 검증했다. 이는 실제 Android Car 연결·기어 전환 시험이 아니다.
- 완료: 옵션 요청 전 값·요청·콜백·재조회 값을 분리해 표시한다. 일치하지 않는 콜백은 계속 기다리고, 일치하는 콜백 후 경로 상태를 재조회한다. 재조회 불일치·활성 경로 없음·시간 초과는 적용 성공으로 표시하지 않는다. 콜백에 요청 식별자가 없어 동일 콜백이나 재조회와 요청의 인과관계는 확정하지 않는다. 상태 전이 단위 테스트는 SDK 콜백·서비스 자체를 실행하지 않는다.
- 관측 유지: 직전 Pleos 조회에서 네 문 잠금은 지원 영역 목록에 보고되지 않았고, 벨트·워셔액은 일반 서비스 오류였다. 원인은 확인되지 않았으며 정상·미지원·에뮬레이터 결함으로 추정하지 않는다.
- Windows `assembleDebug`, `assembleDemo`, `assembleRelease`, `testDebugUnitTest` 23개, `assembleDebugAndroidTest`, `lintDebug` 통과. 계측 APK는 이번 실행에서 컴파일만 했다. 린트·컴파일 경고는 남아 있다.
- Pleos 사용자 10의 기존 `user_settings.xml`과 `practice_review.xml`을 변경 전후 읽기 전용 SHA-256으로 비교했고 각각 일치했다. 기존 데이터 유지 설치와 앱 실행을 수행했으며 설정·복습 기록은 삭제하거나 초기화하지 않았다. 차량 기어·신호 등 에뮬레이터 상태는 변경하지 않았다.
- 에뮬레이터에는 시작 시 기존 debug 앱이 설치되어 있었고 이번에 새 debug APK로 업데이트했다. 업데이트 전 APK를 별도 보관하지 않아 설치된 앱 버전과 포그라운드 화면은 시작 상태로 복구하지 못했다. 이는 위의 사용자 데이터 파일 일치와 구분한다.
- 남음: 실제 P/D 전환과 서비스 유실·재연결, 내비 옵션 요청·콜백·재조회와 실패 원인, 전 신호/SDK 수명주기, 파일 선택기·음성·와이퍼·demo 및 실차 회귀. release 서명·배포 검증, 접근성·저장 손상 복구·현지화·디자인 구조도 미완이다. 아래 이전 실행 기록은 해당 시점의 결과다.

## 2026-11-07 Windows 후속 작업: 파일 선택기

- 파일 저장과 가져오기 인텐트를 각각 확인해 처리할 Activity가 없으면 해당 버튼을 비활성화하고 내부 백업을 안내한다. 앱 재개 시 다시 확인한다.
- Activity가 검색되어도 실제 선택 화면이 표시되거나 파일 작업이 성공한다는 보장은 없다. 취소 결과와 실행 실패는 계속 별도로 안내하며, 이전에 화면이 표시되지 않은 원인은 미확인이다.
- Windows에서 `assembleDebug`, `testDebugUnitTest` 통과. 새 동작의 에뮬레이터 실행과 Activity 유무에 따른 화면 검증은 아직 하지 않았다. Phase 0의 나머지 항목도 미완료다.

## 2026-11-07 Windows 후속 작업: 신호·진단·회귀

- 문 잠금과 벨트의 영역 미보고, 세 항목의 읽기 불가, 빈 값·권한 거부·타임아웃·일반 오류를 구분한다. SDK가 제공하지 않은 원인이나 실차 정상 상태는 추정하지 않는다. 초기화 실패·타임아웃도 잠금에 전달한다. 문 위치 및 다른 신호의 상세 분류는 미완이다.
- debug/demo 주차 화면 안에 개발용 진단을 추가했다. 차량 조회와 내비게이션 연결·요청·콜백·시간 초과를 화면에 표시하며 내비 진단은 최근 12개만 메모리에 보관하고 목적지·주소·좌표는 기록하지 않는다. 옵션 콜백은 요청 식별자가 없으므로 결과 재조회를 안내한다. 별도 SDK 인터페이스와 전 신호 진단은 미완이다.
- Android Car 연결 중에는 최대 5초 기다리고, 연결이 끊기거나 대기가 만료되면 다음 읽기에서 재연결을 시도한다. 기기에서 서비스 유실·재연결과 동시 종료 경합은 검증하지 않았다. FeatureGate는 기존의 전체 화면 공통 주차 게이트를 유지한다.
- Windows `assembleDebug`, `assembleDemo`, `assembleRelease`, `testDebugUnitTest`, `lintDebug` 통과; 직전 소스에서 `assembleDebugAndroidTest`도 통과했다. 린트·컴파일 경고가 남아 있다. release 서명·동작·진단 제외 전체 검증은 미실시다.
- Pleos 2.1.0에서 기존 데이터 유지 업데이트 설치와 앱 실행·주차 후 홈 진입을 확인했다. debug 진단 조회에서 네 문 위치는 닫힘, 네 문 잠금은 SDK 영역 목록 미보고, 벨트·워셔액은 일반 서비스 오류로 표시됐다. 내비 서비스 연결 콜백을 확인했으나 경로 옵션 요청·적용과 실패 원인은 확인하지 못했다. 최종 주차 재연결 코드의 유실/복구 회귀는 미실행이다.
- 사용자 저장소를 건드리지 않는 별도 테스트 저장소를 사용하는 설정·내부 백업·복습 계측 테스트 6개 통과. 실제 기존 설정과 복습의 내용 비교, 와이퍼, D 전환, 파일 선택기, 음성, demo와 실차 회귀는 미실행이다. 실행 중 에뮬레이터가 한 차례 offline이 됐다가 복구됐으며 원인은 미확인이다.

## 이번 빌드와 수락 기준 결과 (2026-11-07 KST)

- 코드: SignalResult·FeatureGate·콜드 Flow 차량 상태 허브, 잠금/안전벨트/워셔액의 일부 오류 분류, demo 빌드와 고정 시연 표시, 토큰 및 화면·디자인 문서 초안을 추가했다.
- 빌드 **통과**: `assembleDebug`, `assembleDemo`, `assembleRelease`, `testDebugUnitTest`, `assembleDebugAndroidTest`, `lintDebug`. 단위 테스트 18개. Release 산출물은 **미서명**이며 서명·설치·실행은 미확인. 기존 경고가 있어 린트 경고 0건이라고 주장하지 않는다.
- 앱 계측 테스트 APK는 컴파일만 했으며 실행하지 않았다. 이번 코드의 Windows 에뮬레이터 회귀·Pleos 실기기 테스트는 **미실행**이다. 이전 확인된 기능의 회귀가 없다고 단정할 수 없다.
- 신호 결과 수락 기준 **미충족**: 잠금·안전벨트·워셔액의 명시적 SecurityException/시간 초과/일반 오류/값 없음만 분리했다. 접근 불가와 미지원은 신호 없음으로 묶인다. 문 위치·기어·속도·와이퍼 및 SDK 전체는 미이관이다. 서비스가 명시하지 않는 한 에뮬레이터 미제공으로 추정하지 않는다.
- SDK 진단 **미완료**: debug/demo 진단 화면·원인별 지원 영역·accessType 조회/로그, 내비게이션 전후 상태 추적 및 대체 인터페이스 미구현. 이전 Windows 셸 오류로 실제 callback을 받지 못했고 이번에는 기기 접근이 없다.
- 파일 선택기·음성 **미완료**: 음성 서비스가 없을 때 버튼을 숨기고 이유를 표시하지만 발화 실행 미확인. SAF 선택기가 표시되지 않는 원인은 미확인이고, 미지원 여부를 신뢰성 있게 감지해 버튼을 숨기는 기능은 미구현.
- FeatureGate·허브 **부분 구현**: 주차 제한 화면에 동일한 P/속도/UX 조건을 적용하고, 테스트 신호를 주입해 흐름과 실패 닫힘을 검증했다. UI 전체 기능별 독립 제한, 상태 유형 세분화, 재연결·전 신호 이관은 미완.
- debug/demo/release **부분 구현**: demo 앱 ID를 분리하고 화면 상단에 ‘시연용’을 표시한다. 실제 시뮬레이션은 새로 추가하지 않았다. release의 병합 manifest에 debug 휴대폰 Activity가 없음을 확인했으나, 전체 바이너리·난독화 동작 및 시장 등록은 미검증이다.
- 디자인 기반 **초안**: 토큰 객체와 2개 인계 문서만 추가했다. 모든 화면을 토큰/문자열/UiState/UiEvent로 이관하지 않았고 공통 컴포넌트 및 에셋 매핑도 미완이다.

## Windows Codex 인계 (사용자 전달)

- Windows Codex는 `Get-Location` 전에 `helper_unknown_error: setup refresh had errors`가 발생해 현재 Windows Git 상태와 최신 커밋을 확인하지 못했다. 마지막으로 확인됐던 로컬 상태는 `run-app.cmd` 수정 및 추적되지 않는 `.idea/`였으며, 현재도 같은지는 확인되지 않았다. 자동으로 덮어쓰거나 삭제하지 않는다.
- 사용자가 전달한 이전 Pleos 결과: 설정 유지, 와이퍼 연습·복습 복원, 내부 백업·복원과 앞 왼쪽 문 열림 반영을 확인했다. 잠금·안전벨트·워셔액은 값 미확인, 경로 옵션 변경은 확인 응답 없이 재조회에서 기존 추천으로 남았다. 이 결과는 `7661435` 적용 후 회귀 테스트가 아니다.
- 이전 Windows `assembleDebug` 성공은 최신 커밋의 Windows 빌드 성공을 의미하지 않는다. `7661435`는 별도 Linux 클라우드에서 debug/demo/release 빌드와 단위 테스트를 실행했다.
