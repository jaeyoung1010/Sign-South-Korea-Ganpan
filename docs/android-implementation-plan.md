# Sign South Korea Ganpan Android 구현 계획서

| 항목 | 내용 |
|---|---|
| 문서 버전 | 1.0 |
| 작성일 | 2026-10-17 |
| 개발 인원 | Android 개발자 1명 |
| 총 계획 | 45 Phase, 약 45 man-day |
| 구현 언어 | Kotlin |
| UI | Jetpack Compose |
| 기술 설계 기준 | [android-implementation-design.md](android-implementation-design.md) |
| 제품·요구사항 기준 | [PRD-SRS.md](PRD-SRS.md) |
| 기능 기준 | [functional-specification.md](functional-specification.md) |
| 최우선 화면 기준 | [../index.html](../index.html) |

## 1. 목적

이 문서는 현재 HTML 프로토타입과 요구사항 전체를 Android 앱으로 구현하기 위한 1인 개발 실행 계획이다. 각 Phase는 개발, 테스트, 리팩터링, 문서 갱신과 커밋까지 포함해 약 1 man-day 안에 완료할 수 있는 크기로 나눈다.

계획의 완료 범위는 다음과 같다.

1. 프로토타입 모바일 프레임 안의 모든 화면과 인터랙션
2. PRD·SRS의 Must와 Should 요구사항
3. 기능 명세의 F-APP-01부터 F-LEGAL-01까지 26개 기능
4. 영어와 일본어 UI 및 언어권별 독립 저술 학습 콘텐츠
5. 오프라인, 복구, 접근성, 성능, 보안, 분석, 알림과 정책 문서
6. 구현 설계서의 경량 헥사고날, Hilt 의존성 주입, TDD와 품질 게이트

## 2. 기준 우선순위와 충돌 처리

이번 구현에서는 사용자의 지시에 따라 현재 HTML 프로토타입을 기능 우선순위의 최상위에 둔다.

1. 현재 HTML 프로토타입의 화면 구성, 카피, 사용자 흐름과 인터랙션
2. 사용자가 대화에서 확정한 동작: 왼쪽 Got it, 오른쪽 Review, 사진 비가림, 기기 경계 처리, 카드 스택, 두 음성 선택
3. PRD·SRS의 데이터, 오프라인, 복구, 품질, 보안과 출시 요구사항
4. 기능 명세의 상세 예외와 인수 조건
5. Android 구현 설계서의 아키텍처와 테스트 규칙
6. Android 플랫폼 관례

프로토타입의 임시 동작이 SRS에서 완성 동작으로 구체화된 경우에는 시각과 진입 위치는 프로토타입을 유지하고 기능은 완성 동작을 구현한다. 예를 들어 `See all`, 정책 문서, 복습 전체 목록, 알림, 저장은 토스트 시뮬레이션이 아니라 실제 로컬 기능으로 만든다.

프로토타입 바깥쪽의 `Jump to a screen`과 `Reset prototype`은 실제 사용자 기능이 아니다. 누락하지 않고 `debug` 빌드의 QA 메뉴와 테스트용 딥 링크로 구현하되, `release` 빌드에는 포함하지 않는다.

## 3. 산정 전제

- 1 man-day는 한 개발자의 약 8시간이며 코드, 테스트, 리뷰, 문서와 커밋을 포함한다.
- 한 명이 Phase 1부터 순서대로 작업하며 병렬 개발은 가정하지 않는다.
- 각 Phase 시작 시 이전 Phase의 모든 품질 게이트가 통과해 있어야 한다.
- 현재 저장소의 20개 사진과 Typecast MP3 20개를 사용한다. manifest 파일은 음성 파일 수에 포함하지 않는다.
- 영어·일본어 콘텐츠의 네이티브 검수는 출시 승인 조건이다. 외부 검수 대기 시간은 man-day에 포함하지 않지만, 반영과 재검증 작업은 Phase 37에 포함한다.
- 개인정보 처리방침과 이용약관의 법률 문안 및 공개 URL은 제품 소유자가 제공한다고 가정한다. 개발자는 번들 문서와 연결·오프라인 전환을 구현한다.
- Play Console 등록, 스토어 심사 대기와 정책 검토의 외부 소요 시간은 45 man-day 밖이다.
- 신규 요구사항은 현재 Phase를 늘리지 않고 별도 변경 요청으로 산정한다.

권장 일정은 주 5일 기준 9주다. 결함 수정과 외부 검수 재작업에 대비한 달력상 여유는 별도로 10~15% 확보한다.

## 4. 모든 Phase에 적용하는 작업 규칙

### 4.1 TDD 순서

각 Phase는 반드시 다음 순서로 진행한다.

1. 해당 요구사항과 프로토타입 동작을 테스트 이름으로 고정한다.
2. 실패하는 JVM, repository contract, ViewModel, Compose 또는 E2E 테스트를 먼저 작성한다.
3. 테스트를 통과하는 최소 구현을 작성한다.
4. 포트 경계와 중복을 정리하고 전체 회귀 테스트를 실행한다.
5. 요구사항 추적표와 필요한 스크린샷·성능 증거를 갱신한다.
6. 정적 검사와 관련 테스트가 모두 통과한 상태만 커밋한다.

### 4.2 Phase 공통 완료 조건

- 새 비즈니스 규칙은 Android SDK를 모르는 순수 Kotlin 테스트가 있다.
- Composable은 DAO, DataStore, Media3, WorkManager를 직접 호출하지 않는다.
- 외부 효과는 출력 포트를 거치며 Hilt가 실제 어댑터를 주입한다.
- 사용자 노출 문자열은 영어와 일본어 리소스 또는 언어별 콘텐츠 파일에 있다.
- 오류, 로딩, 빈 상태, 프로세스 재생성, TalkBack 영향을 함께 검토한다.
- 변경 파일에 format, lint, detekt, unit test와 `git diff --check`가 통과한다.
- Phase의 종료 조건을 검증하지 못하면 다음 Phase로 넘어가지 않는다.

### 4.3 브랜치와 커밋

- 하나의 Phase는 하나 이상의 작은 Red/Green 커밋으로 작업할 수 있다.
- Phase 종료 시 동작 가능한 통합 커밋을 남긴다.
- 커밋 메시지는 결과 중심의 한국어 문장으로 작성한다.
- 실패 테스트를 삭제하거나 `@Ignore`로 우회해 Phase를 완료 처리하지 않는다.

## 5. 마일스톤

| 마일스톤 | Phase | 누적 man-day | 검증 가능한 결과 |
|---|---:|---:|---|
| M0 품질·아키텍처 기반 | 1~6 | 6 | CI가 실행되고 20개 콘텐츠와 40개 음성을 검증하는 빈 앱 |
| M1 첫 실행과 홈 | 7~13 | 13 | 영어·일본어 구조로 온보딩을 마치고 프로토타입형 홈 진입 |
| M2 핵심 학습 루프 | 14~24 | 24 | 세션 생성, 카드 학습, 스와이프, Undo, 복구와 완료 |
| M3 전체 제품 기능 | 25~35 | 35 | 확대, 신고, 음성, 설정, Progress, 알림, 정책, 분석 |
| M4 다국어·접근성 | 36~40 | 40 | 영어·일본어 완전성, TalkBack, 큰 글자, 화면 크기 대응 |
| M5 안정화·출시 | 41~45 | 45 | 오프라인·성능·E2E·보안·릴리스 인수 기준 통과 |

## 6. Phase별 구현 계획

### Phase 1 — Android 프로젝트와 자동 품질 게이트 (~1 man-day)

**목표**

Kotlin Android 앱을 빌드하고 모든 이후 Phase에 동일하게 적용할 품질 기반을 만든다.

**테스트 먼저**

- 빈 프로젝트 JVM test와 instrumentation smoke test가 CI에서 실패하는 상태를 먼저 확인한다.
- 금지된 동적 dependency version과 비밀 문자열을 탐지하는 검증 테스트를 작성한다.

**구현**

- Java 17, Kotlin, Compose, version catalog와 `debug`·`release` build type을 설정한다.
- ktlint 또는 Spotless, Android Lint, detekt, unit test, secret scan을 Gradle task로 묶는다.
- CI에 커밋 게이트와 테스트 결과·lint artifact 업로드를 추가한다.
- API 26 최소 지원과 최신 안정 SDK compile/target 기준을 고정한다.

**종료 조건**

- 새 checkout에서 한 명령으로 debug APK와 모든 품질 task가 성공한다.
- 의도적으로 넣은 lint 오류와 비밀값 fixture가 CI를 실패시키는 것을 확인한다.

**추적**: 구현 설계 3장, 4장, 26장; NFR-SEC-001

### Phase 2 — 경량 헥사고날 모듈과 Hilt 조립점 (~1 man-day)

**목표**

변경에 강한 의존성 방향을 코드로 강제한다.

**테스트 먼저**

- domain이 Android·Compose·Room·Hilt를 참조하면 실패하는 architecture test를 작성한다.
- fake port를 주입한 use case smoke test와 Hilt test binding 교체 테스트를 작성한다.

**구현**

- `app`, `domain`, `data`, `platform`, `designsystem`, 네 feature 모듈과 `test-support`를 만든다.
- input/output port, application use case, adapter, feature 의존 방향을 Gradle로 제한한다.
- `@HiltAndroidApp`, Activity composition root와 qualifier 규칙을 설정한다.

**종료 조건**

- domain은 순수 JVM으로 빌드된다.
- production adapter와 fake adapter를 바꿔 동일 use case test가 통과한다.

**추적**: 구현 설계 3장, 5~7장

### Phase 3 — 테스트 지원 도구와 계약 테스트 골격 (~1 man-day)

**목표**

매 Phase가 빠르게 Red→Green을 수행할 공통 테스트 자산을 만든다.

**테스트 먼저**

- 고정 Clock, locale fixture, stateful fake repository의 예상 동작 테스트를 작성한다.
- repository contract가 fake와 아직 비어 있는 production adapter에서 같은 결과를 요구하도록 만든다.

**구현**

- coroutine MainDispatcher rule, fake clock, fixture builder, temporary database helper를 추가한다.
- LearningRepository, SessionRepository, SettingsRepository 계약 테스트 추상 클래스를 만든다.
- Compose semantics test tag 명명 규칙과 screenshot test harness를 만든다.

**종료 조건**

- fake와 실제 adapter가 같은 계약 suite를 실행할 구조가 마련된다.
- 테스트 실패 로그가 카드 ID, 상태와 기대값을 식별 가능하게 보여준다.

**추적**: 구현 설계 17장, 28장

### Phase 4 — 프로토타입 디자인 시스템과 debug QA 메뉴 (~1 man-day)

**목표**

프로토타입의 밝은 여성 취향·게임 감성 표현을 재사용 가능한 Compose 구성으로 옮긴다.

**테스트 먼저**

- 버튼 상태, 48dp 터치 영역, 명암비와 debug 메뉴의 release 미포함 테스트를 작성한다.
- 대표 컴포넌트의 light theme screenshot baseline을 만든다.

**구현**

- 색상, typography, shape, thick outline, shadow, spacing, icon과 공통 버튼·칩·카드·sheet·toast를 만든다.
- 시스템 bar와 safe inset을 처리한다.
- HTML 바깥 패널의 화면 점프와 Reset을 `debugImplementation` QA 메뉴와 test deeplink로 구현한다.
- release variant에서 QA entry point와 reset receiver를 제거한다.

**종료 조건**

- 프로토타입의 핵심 버튼·카드·sheet가 preview와 screenshot test에서 일치한다.
- release APK에서 QA 메뉴 문자열과 component가 검색되지 않는다.

**추적**: 프로토타입 Prototype navigation, 공통 UI; NFR-A11Y-001, NFR-A11Y-002

### Phase 5 — 앱 내비게이션과 화면 상태 골격 (~1 man-day)

**목표**

프로토타입의 전체 화면을 타입 안전한 route와 단방향 상태 흐름으로 연결한다.

**테스트 먼저**

- Welcome, Tutorial, Level, Interests, Home, Learn, Done, Progress, Settings, Legal route 전이 테스트를 작성한다.
- Android 뒤로가기와 overlay 우선 닫기 규칙 테스트를 작성한다.

**구현**

- Navigation 3 route와 top-level Learn·Progress·Settings 탭을 구성한다.
- screen별 immutable UiState, UiAction, one-shot UiEffect 골격을 만든다.
- bottom sheet, full-screen viewer와 toast host의 back stack 정책을 넣는다.

**종료 조건**

- debug QA 메뉴에서 프로토타입의 모든 주요 화면으로 이동할 수 있다.
- 임시 화면에도 feature가 repository나 platform adapter를 직접 참조하지 않는다.

**추적**: 프로토타입 `screen-welcome`~`screen-settings`; 기능 명세 6장

대상 화면 ID는 `screen-welcome`, `screen-tutorial`, `screen-level`, `screen-interests`, `screen-home`, `screen-learn`, `screen-done`, `screen-progress`, `screen-settings`다.

### Phase 6 — 20개 카드와 40개 음성의 번들 콘텐츠 파이프라인 (~1 man-day)

**목표**

현재 HTML의 20개 카드, 사진, 포커스 좌표와 두 음성 세트를 타입 안전하게 번들한다.

**테스트 먼저**

- 카드 20개, 이미지 20개, Typecast MP3 20개의 일대일 매핑 테스트를 작성한다.
- 중복 ID, 잘못된 순서, 0~100 밖의 포커스 좌표, 누락 word part와 손상 URI를 실패시킨다.

**구현**

- base card manifest와 locale별 `LocalizedCardCopy` JSON schema를 만든다.
- Kotlinx Serialization 기반 BundleContentAdapter와 checksum·version 검증기를 구현한다.
- 생성 이미지 출처·권리·검수 metadata를 저장한다.
- 웹용 manifest는 Android resource URI로 변환하되 카드 slug는 그대로 유지한다.

**종료 조건**

- release asset validation이 20/20/20/20을 보고한다.
- 카드 하나나 음성 하나를 임시로 빼면 검증이 정확히 실패한다.

**추적**: PRD·SRS 15장; AC-021; AC-29; AC-30

### Phase 7 — Room 스키마와 마이그레이션 기반 (~1 man-day)

**목표**

학습 상태, 세션, 판정 이력, 신고 큐를 원자적으로 저장할 데이터베이스를 만든다.

**테스트 먼저**

- `card_progress`, `sessions`, `session_cards`, `rating_snapshot`, `card_reports` DAO 테스트를 작성한다.
- 세션 생성·판정·Undo·완료 transaction의 rollback 테스트를 작성한다.
- v1→v2 migration fixture test 골격을 만든다.

**구현**

- Room entity, DAO, index, foreign key와 LocalDate/Instant converter를 구현한다.
- 판정 이전 snapshot과 한 번의 Undo를 지원하는 transaction adapter를 만든다.
- 앱 전용 저장소와 데이터베이스 암시적 로그 금지 규칙을 적용한다.

**종료 조건**

- in-memory와 file-backed Room contract test가 통과한다.
- 중간 단계 예외에서 세션 인덱스와 progress가 부분 저장되지 않는다.

**추적**: F-SES-01, F-SES-02, F-CARD-04, F-CARD-06; NFR-OFF-002, NFR-SEC-003

### Phase 8 — Proto DataStore 설정 저장소 (~1 man-day)

**목표**

온보딩과 모든 사용자 설정을 타입 안전하게 저장한다.

**테스트 먼저**

- 기본값 20장, 로마자 Off, 자동 음성 Off, Typecast, 알림 Off, 시스템 기반 Reduce motion 테스트를 작성한다.
- write/read, 프로세스 재생성, corruption fallback과 enum unknown 값 테스트를 작성한다.

**구현**

- onboarding, level, interests, session length, language tags, romanization, autoplay, voice, motion, reminder 필드의 Proto schema를 만든다.
- UserSettingsRepository adapter와 migration/corruption handler를 구현한다.
- settings write는 즉시 완료 결과를 반환하고 Flow가 새 값을 방출하게 한다.

**종료 조건**

- 실제 DataStore와 fake가 같은 contract test를 통과한다.
- 손상 fixture가 안전한 기본값으로 복구되고 진단 이벤트를 발생시킨다.

**추적**: F-APP-01, F-ONB-03, F-ONB-04, F-SET-01, F-SET-02, F-AUD-02, F-REM-01; AC-005

### Phase 9 — 앱 시작, 콘텐츠 이관과 복구 화면 (~1 man-day)

**목표**

저장 상태를 안전하게 읽고 Welcome 또는 Home으로 정확히 진입한다.

**테스트 먼저**

- 신규 설치, 온보딩 완료, 활성 세션, 설정 읽기 실패, Room open 실패와 content version 변경 테스트를 작성한다.
- DataStore와 Room을 병렬로 읽되 잘못된 중간 화면이 노출되지 않는 ViewModel 테스트를 작성한다.

**구현**

- InitializeApp use case와 splash/loading/error/retry state를 구현한다.
- 카드 ID 기반 content migration과 유효한 마지막 판정까지만 복구하는 정책을 연결한다.
- 앱 시작 분석 이벤트의 개인 정보 없는 payload를 정의한다.

**종료 조건**

- AC-001, AC-002와 프로세스 재생성 진입 테스트가 통과한다.
- Room 장애에서 앱이 종료되지 않고 Retry와 진단 경로를 제공한다.

**추적**: F-APP-01; AC-001, AC-002; NFR-OFF-003, NFR-OFF-004

### Phase 10 — Welcome 제품 소개 (~1 man-day)

**목표**

프로토타입의 첫 화면과 가치 설명을 Compose로 완성한다.

**테스트 먼저**

- 신규 사용자만 Welcome을 보고 `Get started`가 Tutorial로 이동하는 UI 테스트를 작성한다.
- 뒤로가기 종료, 계정 불필요 안내와 이미지 콜라주 semantics 테스트를 작성한다.

**구현**

- `Read Korea in the real world`, 설명, 세 장 이미지 콜라주와 CTA를 구현한다.
- edge-to-edge와 다양한 화면 높이에서 CTA가 가려지지 않게 한다.
- `onboarding_started` 이벤트를 연결한다.

**종료 조건**

- 프로토타입과 360·390·480dp screenshot 비교가 허용 오차 안에 있다.
- 영어와 임시 일본어 resource에서 하드코딩 문자열 검사가 통과한다.

**추적**: F-ONB-01; FR-ONB-001, FR-ONB-002, FR-ONB-003

### Phase 11 — 조작 튜토리얼 (~1 man-day)

**목표**

실제 학습 기록과 분리된 `분식` 예제로 정답 확인과 방향을 가르친다.

**테스트 먼저**

- Show answer 전/후, 예시 판정 후 Continue 활성화, 뒤로가기와 학습 DB 미변경 테스트를 작성한다.
- 화면 문구가 `← Got it`, `Review →` 순서인지 회귀 테스트를 작성한다.

**구현**

- 프로토타입의 tutorial card, prompt, answer와 practice direction control을 구현한다.
- 튜토리얼 state는 saved state로만 유지하고 Rating use case를 호출하지 않는다.

**종료 조건**

- 튜토리얼 완료 전후 Room row count가 같다.
- 왼쪽 Got it, 오른쪽 Review 안내 screenshot이 승인된다.

**추적**: F-ONB-02; FR-ONB-004, FR-ONB-005

### Phase 12 — 수준과 관심 분야 온보딩 (~1 man-day)

**목표**

프로토타입의 Level과 Interests 화면을 저장 가능한 흐름으로 완성한다.

**테스트 먼저**

- 세 수준 단일 선택, 선택 전 Continue 비활성, Skip=Beginner 테스트를 작성한다.
- 관심 분야 0개·다중 선택, 완료 저장 후 Home 이동 순서 테스트를 작성한다.

**구현**

- Beginner, Elementary, Not sure choice card를 구현한다.
- 6개 interest toggle card와 `Start learning`을 구현한다.
- 저장 성공 이후에만 Home으로 전환하고 `onboarding_completed`를 기록한다.

**종료 조건**

- AC-003, AC-004가 DataStore 실제 adapter로 통과한다.
- 저장 실패에서는 완료로 이동하지 않고 재시도할 수 있다.

**추적**: F-ONB-03, F-ONB-04; FR-ONB-006, FR-ONB-007, FR-ONB-008, FR-ONB-009, FR-ONB-010; AC-003, AC-004

### Phase 13 — Learn 홈과 하단 내비게이션 (~1 man-day)

**목표**

프로토타입의 홈 정보를 실제 저장 데이터로 표시한다.

**테스트 먼저**

- 기본 20장·8분, 5장·2분, 10장·4분 표시와 Start/Resume 상태 테스트를 작성한다.
- streak, review/new mix, 최근 7일, 네 카테고리와 See all sheet 테스트를 작성한다.
- Learn·Progress·Settings 탭 state 보존 테스트를 작성한다.

**구현**

- brand bar, streak, today's session, mix chips, category row, week chart와 bottom navigation을 구현한다.
- 카테고리별 정확한 카드 수와 전체 카테고리 bottom sheet를 제공한다.
- 활성 세션이 있으면 `Pick up where you left off`와 진행 위치를 표시한다.

**종료 조건**

- 고정 숫자가 아니라 repository fixture가 바뀌면 홈 표시가 함께 바뀐다.
- 프로토타입 Home screenshot과 스크롤·safe area가 일치한다.

**추적**: F-HOME-01; FR-SES-001, FR-SES-002, FR-SES-003, FR-SES-005

### Phase 14 — 복습 우선순위와 SessionBuilder 순수 규칙 (~1 man-day)

**목표**

길이, Due, 신규, 카테고리, 관심 분야와 수준으로 결정적인 세션 목록을 만든다.

**테스트 먼저**

- 5·10·20장, Due 0·30%·70%, 카테고리 선두, 중복 방지와 부족 콘텐츠 property test를 작성한다.
- 같은 seed에서는 같은 순서, 서로 다른 카드 풀에서도 범위를 넘지 않는 테스트를 작성한다.

**구현**

- SessionBuilder와 CandidateScorer를 순수 Kotlin으로 구현한다.
- 일반 세션, 카테고리 세션, 1장 연습 세션 유형을 분리한다.
- 리뷰 재삽입은 아직 수행하지 않고 policy hook을 둔다.

**종료 조건**

- Android 없이 10,000개 무작위 입력 property test가 통과한다.
- 20장 기본 번들 세션은 20개 고유 카드를 한 번씩 포함한다.

**추적**: F-SES-01; FR-SES-003, FR-SES-004, FR-SES-005

### Phase 15 — 세션 생성 transaction과 시작 흐름 (~1 man-day)

**목표**

Home의 시작·카테고리 선택을 저장된 활성 세션으로 연결한다.

**테스트 먼저**

- 세션과 session_cards 저장 완료 후 Learn 이동, 저장 실패 시 화면 유지 테스트를 작성한다.
- 기존 완료 세션, 활성 세션 Resume, 새 카테고리 세션 교체 확인 테스트를 작성한다.

**구현**

- StartSession input port와 Room transaction adapter를 구현한다.
- `session_started` 이벤트와 due/new/category 속성을 연결한다.
- 시작 중 이중 탭을 mutex로 막고 loading state를 표시한다.

**종료 조건**

- 시작 버튼 연타에도 활성 세션은 하나만 생성된다.
- 앱 강제 종료 직후에도 세션 카드 목록이 완전하게 복구된다.

**추적**: F-SES-01; FR-SES-005; NFR-OFF-002, NFR-OFF-003

### Phase 16 — Learn 화면 골격과 세션 진행 상태 (~1 man-day)

**목표**

프로토타입의 상단 상태와 학습 stage를 실제 세션에 연결한다.

**테스트 먼저**

- `1 of n`, 진행 막대, 닫기, 더보기, 카드 index 변경 UI 테스트를 작성한다.
- 완료 index, 빈·손상 세션과 잘못된 카드 ID 오류 상태 테스트를 작성한다.

**구현**

- LearnViewModel과 immutable SessionUiState를 만든다.
- top app bar, progress, stage, action 영역과 안전한 오류 placeholder를 구현한다.
- 세션 화면 진입 시 활성 index와 카드 콘텐츠를 결합한다.

**종료 조건**

- 5·10·20장 fixture의 위치와 진행률이 정확하다.
- 손상 카드 하나가 전체 앱 실행을 막지 않고 정답 학습을 계속할 수 있다.

**추적**: FR-SES-006; F-CARD-01; NFR-OFF-004

### Phase 17 — 3장 카드 스택과 사진 렌더링 (~1 man-day)

**목표**

사진이 모바일 stage를 채우고 뒤에 두 카드가 쌓이는 프로토타입형 카드를 만든다.

**테스트 먼저**

- 활성·다음·다다음 레이어 순서와 다음 카드 접근성 숨김 테스트를 작성한다.
- Fit 전경, blur 배경, 포커스 좌표 변환, 좌우 비절단 screenshot test를 작성한다.
- 다음 두 장만 prefetch하고 20장을 한꺼번에 decode하지 않는 adapter test를 작성한다.

**구현**

- Coil 기반 PhotoSurface, blur backdrop, focus frame과 다음 두 card preview를 구현한다.
- 사진 62%, 정보 38% 기준과 큰 글자에서 사진 최소 높이를 적용한다.
- 이미지 실패 placeholder와 정답 비누설 alt text를 연결한다.

**종료 조건**

- 20개 사진 각각의 포커스 프레임 screenshot 검사와 메모리 계측이 통과한다.
- 화면 폭 360·390·480dp에서 간판 원본 좌우가 잘리지 않는다.

**추적**: F-CARD-01; AC-007, AC-008; NFR-PERF-004

### Phase 18 — 질문과 정답 상태 전환 (~1 man-day)

**목표**

Show answer 전후를 프로토타입처럼 한 카드 안에서 전환하고 사진을 가리지 않는다.

**테스트 먼저**

- 질문 상태의 숨김 항목과 정답 상태의 필수 정보 테스트를 작성한다.
- Answer, 한국어, 의미, word parts, Street note, 방향 안내와 선택적 발음 가이드 테스트를 작성한다.
- 긴 텍스트가 정보 영역 안에서만 스크롤되고 사진을 덮지 않는 screenshot test를 작성한다.

**구현**

- CardQuestion과 CardAnswer Composable, Show answer action과 state reducer를 구현한다.
- answer reveal 이벤트를 한 번 기록하고 정보 영역 스크롤을 초기화한다.
- pronunciation guide는 현재 설명 언어에 맞는 값을 Answer에서만 표시한다.

**종료 조건**

- AC-011, AC-019와 기능 명세 AC-09, AC-10, AC-20이 통과한다.
- 영어·일본어의 가장 긴 fixture에서도 사진 영역이 유지된다.

**추적**: F-CARD-02; AC-011, AC-019

### Phase 19 — GesturePolicy와 방향 회귀 테스트 (~1 man-day)

**목표**

스와이프 축, 임계값, 방향과 정답 전 차단을 순수 Kotlin 규칙으로 고정한다.

**테스트 먼저**

- 12dp 축 잠금, 가로/세로 1.2배, ±30dp 제한, 28dp 안내, 88dp 판정과 90dp overlay 비율 테스트를 작성한다.
- 왼쪽=Got it, 오른쪽=Review, 정확히 임계값, NaN·무한대·다중 포인터 property/fuzz test를 작성한다.

**구현**

- GesturePolicy, DragAxis, DragFeedback, RatingDecision value object를 구현한다.
- 임계값은 한 구성 객체에서만 관리한다.
- 정답 전 결과는 어떤 경우에도 Rating을 반환하지 않게 타입으로 제한한다.

**종료 조건**

- 방향을 의도적으로 뒤집으면 핵심 회귀 테스트가 실패한다.
- 모든 경계값과 무작위 좌표에서 결과가 결정적이다.

**추적**: F-CARD-03; AC-006, AC-007, AC-008, AC-009; 기능 명세 AC-11~AC-14

### Phase 20 — Compose 드래그, 기기 경계, 키보드와 카드 스택 보간 (~1 man-day)

**목표**

순수 제스처 규칙을 실제 터치·마우스·트랙패드에 연결한다.

**테스트 먼저**

- 포인터가 stage 밖으로 나갈 때 좌표 clamp, 종료, 이후 move 무시 테스트를 작성한다.
- 세로 스크롤 유지, next/third card 밝기·크기 보간, ArrowLeft·ArrowRight·Escape 테스트를 작성한다.

**구현**

- pointer input adapter, velocity 무관 거리 판정, pointer cancellation과 boundary release를 구현한다.
- 이동 중 Got it/Review 텍스트 overlay, 회전과 카드 preview 보간을 구현한다.
- 물리 키보드와 Android 뒤로가기를 같은 input port로 연결한다.

**종료 조건**

- 데스크톱 Android/에뮬레이터에서 커서가 창 밖으로 나가도 카드가 따라가지 않는다.
- ArrowLeft=Got it, ArrowRight=Review 방향 테스트가 통과한다.

**추적**: F-CARD-03, F-CARD-08; AC-010; 기능 명세 AC-15, AC-28

### Phase 21 — 판정 transaction과 카드 퇴장 (~1 man-day)

**목표**

정답 후 판정을 저장하고 성공한 경우에만 다음 카드를 활성화한다.

**테스트 먼저**

- Got it/Review에 따른 progress, due date, 세션 index와 snapshot 원자성 테스트를 작성한다.
- 저장 실패 rollback, Retry, 이중 입력과 애니메이션 중 입력 차단 테스트를 작성한다.

**구현**

- RateCard use case와 RoomLearningRepository transaction을 구현한다.
- 왼쪽 퇴장 Got it, 오른쪽 퇴장 Review 애니메이션과 다음 카드 승격을 연결한다.
- haptic과 `card_rated` 이벤트에 gesture/accessibility 입력 출처를 기록한다.

**종료 조건**

- 저장 실패 시 카드가 다음으로 넘어가지 않으며 재시도 후 한 번만 판정된다.
- 퇴장 방향과 표시 overlay가 프로토타입과 일치한다.

**추적**: F-CARD-03; NFR-OFF-002; AC-007, AC-008

### Phase 22 — 사진 비가림 토스트와 Undo (~1 man-day)

**목표**

판정 피드백을 사진 아래에 표시하고 직전 판정을 완전히 되돌린다.

**테스트 먼저**

- 문구, 4초 만료, 사진 비가림 금지, 새 판정 시 이전 Undo 폐기 테스트를 작성한다.
- progress snapshot, index, answer 상태, due schedule의 transaction 복원 테스트를 작성한다.

**구현**

- Learn 영역 전용 Snackbar/Toast host와 Undo action을 구현한다.
- UndoRating use case로 마지막 snapshot을 복원하고 이전 카드를 Answer 상태로 연다.
- `rating_undone` 이벤트와 `Last choice undone` 피드백을 연결한다.

**종료 조건**

- AC-012와 기능 명세 AC-16, AC-17이 실제 Room adapter로 통과한다.
- 360dp와 큰 글자에서 토스트가 사진을 덮지 않는다.

**추적**: F-CARD-04; AC-012

### Phase 23 — 세션 나가기, 재개와 프로세스 복구 (~1 man-day)

**목표**

중단 위치를 잃지 않고 Home에서 정확히 Resume한다.

**테스트 먼저**

- Close·Back의 확인 sheet, Keep learning, Leave for now와 Home resume card 테스트를 작성한다.
- process death, Activity recreation, 앱 background/foreground와 불일치 record 복구 테스트를 작성한다.

**구현**

- PauseSession, ResumeSession use case와 session status transition을 구현한다.
- 현재 index와 판정은 Room, 일시 UI 상태는 SavedStateHandle에 분리한다.
- `session_paused`, `session_resumed` 이벤트를 연결한다.

**종료 조건**

- AC-014와 기능 명세 AC-18이 process kill instrumentation test에서 통과한다.
- 완료 세션은 Home에 Resume으로 노출되지 않는다.

**추적**: F-SES-02; FR-SES-007, FR-SES-008; AC-014; NFR-OFF-003

### Phase 24 — 세션 완료 화면과 최종 집계 (~1 man-day)

**목표**

마지막 판정 후 정확한 결과와 다음 행동을 표시한다.

**테스트 먼저**

- 5·10·20장 종료, Got it+Review=전체, 다음 복습과 Review 0일 때 버튼 숨김 테스트를 작성한다.
- 완료 transaction과 활성 세션 제거 테스트를 작성한다.

**구현**

- Session complete, total, score grid, next review, Back home, See review cards를 구현한다.
- 완료 저장과 마지막 판정을 하나의 transaction으로 처리한다.
- `session_completed`에 duration을 포함한다.

**종료 조건**

- AC-013과 기능 명세 AC-19가 통과한다.
- 완료 직후 process death에도 다시 Learn으로 들어가지 않는다.

**추적**: F-DONE-01; FR-SES-009, FR-SES-010; AC-013

### Phase 25 — 전체 화면 이미지 뷰어 (~1 man-day)

**목표**

질문·정답 상태에서 사진을 확대하고 기존 카드 상태를 보존한다.

**테스트 먼저**

- 질문 라벨 숨김, 정답 한국어 라벨 표시, 닫기·Back 상태 보존 테스트를 작성한다.
- pinch, double tap, 최소/최대 배율과 회전 후 상태 테스트를 작성한다.

**구현**

- edge-to-edge viewer, zoom/pan gesture, double-tap reset과 top close action을 구현한다.
- card answer state와 scroll position을 변경하지 않는 별도 overlay route로 만든다.

**종료 조건**

- AC-024와 기능 명세 이미지 확대 시나리오가 통과한다.
- TalkBack에서 이미지 설명과 닫기 순서가 올바르다.

**추적**: F-CARD-05; AC-024

### Phase 26 — 카드 신고와 로컬 큐 (~1 man-day)

**목표**

프로토타입의 신고 sheet를 완전한 로컬 저장 기능으로 만든다.

**테스트 먼저**

- 다섯 사유, 단일 선택, 미선택 Send 비활성, 500자 제한과 개인정보 안내 테스트를 작성한다.
- submit 저장, cancel/background dismiss, 중복 submit과 Room 실패 테스트를 작성한다.

**구현**

- report bottom sheet, ReportCard use case와 LOCAL_ONLY queue adapter를 구현한다.
- 제출 성공 후 form을 초기화하고 감사 toast를 표시한다.
- 자유 입력은 분석·오류 로그에서 제외한다.

**종료 조건**

- AC-023과 기능 명세 카드 신고 테스트가 통과한다.
- 네트워크 권한 없이도 신고 row가 안전하게 저장된다.

**추적**: F-CARD-06; AC-023; NFR-SEC-004

### Phase 27 — Media3 오디오 재생 어댑터 (~1 man-day)

**목표**

두 번들 음성 세트를 정답 상태에서 안정적으로 재생한다.

**테스트 먼저**

- 카드/voice별 URI, 재탭 시 처음부터 재생, 카드 변경·종료·background 시 정지 테스트를 작성한다.
- audio focus 거부, 누락·손상 파일과 provider 이름 오류 테스트를 작성한다.

**구현**

- AudioPlayerPort의 Media3 adapter와 Hilt singleton 수명주기 관리를 구현한다.
- 정답 상태의 speaker action과 재생 state를 연결한다.
- 오디오 실패는 비차단 toast와 개인 정보 없는 진단 event로 처리한다.

**종료 조건**

- AC-015, AC-016과 기능 명세 AC-21, AC-22, AC-25가 통과한다.
- 비행기 모드에서 40개 파일의 재생 smoke test가 성공한다.

**추적**: F-AUD-01; AC-015, AC-016, AC-021; NFR-PERF-005, NFR-OFF-001

### Phase 28 — 음성 선택과 자동 재생 (~1 man-day)

**목표**

Answer당 Typecast 은경 발음을 한 번 자동 재생한다.

**테스트 먼저**

- 기본 Typecast, 선택 즉시 기존 재생 중지·저장, 재실행 유지 테스트를 작성한다.
- 자동 재생 Answer 진입당 한 번, recomposition 중복 금지와 실패 비차단 테스트를 작성한다.

**구현**

- 프로토타입 voice selection sheet, 선택 toast와 voice adapter dispatch를 구현한다.
- Answer instance ID를 사용해 자동 재생 중복을 막는다.
- `voice_changed`, `audio_played(auto_play)` 이벤트를 연결한다.

**종료 조건**

- AC-017, AC-018과 기능 명세 AC-23, AC-24가 process recreation 포함 테스트에서 통과한다.

**추적**: F-AUD-02, F-SET-01; AC-017, AC-018

### Phase 29 — 학습 설정과 Reduce motion (~1 man-day)

**목표**

프로토타입의 나머지 학습·접근성 설정을 실제 동작과 연결한다.

**테스트 먼저**

- 5·10·20 session length와 현재 활성 세션 불변, romanization Answer-only 테스트를 작성한다.
- 앱 설정과 시스템 Remove animations의 OR 규칙, 카드 fade 20ms 이하 테스트를 작성한다.

**구현**

- Session length sheet, romanization, auto-play switch와 Reduce motion switch를 완성한다.
- motion policy를 카드 퇴장, 복귀, preview 보간과 화면 전환에 주입한다.
- settings 변경을 즉시 DataStore에 저장한다.

**종료 조건**

- FR-SES-004와 기능 명세 AC-06, AC-20이 통과한다.
- Reduce motion이어도 저장, 햅틱과 텍스트 피드백은 동일하다.

**추적**: F-SET-01, F-SET-02; FR-SES-004; NFR-A11Y-006

### Phase 30 — 복습 간격과 Review 재삽입 정책 (~1 man-day)

**목표**

Due, mastery와 같은 세션 내 재학습을 요구사항대로 계산한다.

**테스트 먼저**

- Review=1일·stage 0, 첫 Got it=3일, 연속 7·21·60·90일과 Mastered 테스트를 작성한다.
- overdue/review-count 우선, 3~5장 뒤 재삽입, 남은 카드 부족 시 미삽입 property test를 작성한다.
- DST, 시간대 변경, 자정 경계와 고정 Clock 테스트를 작성한다.

**구현**

- ReviewPolicy, DuePriority와 ReinsertionPolicy를 순수 Kotlin으로 구현한다.
- RateCard transaction과 SessionBuilder에 정책을 연결한다.

**종료 조건**

- 모든 interval과 시간 경계 test가 장치 시간에 의존하지 않고 통과한다.
- Review한 카드의 재삽입은 전체 세션 완료 계산을 깨뜨리지 않는다.

**추적**: F-SES-01, F-PROG-01; PRD·SRS 13.2

### Phase 31 — Progress 집계와 7일 통계 (~1 man-day)

**목표**

프로토타입의 Progress 숫자와 그래프를 Room의 실제 데이터로 계산한다.

**테스트 먼저**

- Learned 고유 카드, Mastered, Due now, 최근 7일과 카테고리별 수·비율 query 테스트를 작성한다.
- 중복 판정, Undo, 시간대와 데이터 0개 상태 테스트를 작성한다.

**구현**

- ObserveProgress input port와 집계 DAO를 구현한다.
- 최근 Review 최대 3개와 전체 Review paging query를 추가한다.
- Home의 week chart와 streak도 같은 집계 공급원을 사용한다.

**종료 조건**

- 프로토타입의 24·8·3 고정값이 코드에 남지 않는다.
- Undo 직후 모든 집계가 자동 갱신된다.

**추적**: F-PROG-01; 기능 명세 AC-26

### Phase 32 — Progress 화면, 복습 상세와 1장 연습 (~1 man-day)

**목표**

Progress와 Needs review의 모든 프로토타입 동작을 완성한다.

**테스트 먼저**

- stat cards, week bars, category progress, 최근 3개와 See all 목록 테스트를 작성한다.
- 상세 sheet의 사진·한국어·설명·Street note, Close scroll 복원과 Practice 1장 세션 테스트를 작성한다.

**구현**

- Progress screen과 빈 상태, 전체 review list를 구현한다.
- review detail bottom sheet와 Practice this sign use case를 연결한다.
- 완료 화면의 See review cards도 같은 route로 이동한다.

**종료 조건**

- F-PROG-01·02의 모든 표시가 실제 Room fixture와 일치한다.
- 1장 연습을 완료해도 기존 중단 세션 처리 규칙이 손상되지 않는다.

**추적**: F-PROG-01, F-PROG-02

### Phase 33 — 복습 알림과 권한 흐름 (~1 man-day)

**목표**

Due 카드가 있을 때만 현지 시각 19:00에 알림을 보낸다.

**테스트 먼저**

- Off 기본값, 사용자 action 직후 권한 요청, 승인·거부, Due 0·1+, Off 취소 테스트를 작성한다.
- 시간대, 재부팅, 앱 업데이트와 notification tap deep link 테스트를 작성한다.

**구현**

- ReminderSchedulerPort의 WorkManager adapter와 Notification channel을 구현한다.
- Android 13 권한 흐름과 하위 버전 분기를 연결한다.
- 알림 탭은 Home에서 review 시작 action을 강조한다.

**종료 조건**

- AC-022와 WorkManager test driver 시나리오가 통과한다.
- 권한 거부 시 setting은 Off이며 시스템 설정 안내만 제공한다.

**추적**: F-REM-01; AC-022

### Phase 34 — 게스트 안내, 정책 문서와 앱 정보 (~1 man-day)

**목표**

프로토타입 Account·About 영역을 출시 가능한 정보 기능으로 만든다.

**테스트 먼저**

- 게스트 저장 범위, 이메일 미저장, 삭제 시 손실 안내와 sync 비활성 테스트를 작성한다.
- Privacy·Terms online Custom Tab, 실패·오프라인 bundled document fallback과 back 테스트를 작성한다.

**구현**

- Account & sync sheet를 구현하고 Connect account는 숨기거나 비활성화한다.
- 정책 native fallback screen, version·effective date와 앱 버전 표시를 구현한다.
- release 문서 URL과 bundled checksum을 build config가 아닌 공개 resource로 관리한다.

**종료 조건**

- 프로토타입의 임시 `would open here` toast가 release 코드에 없다.
- 비행기 모드에서 두 정책 문서를 읽고 Settings로 돌아올 수 있다.

**추적**: F-ACC-01, F-LEGAL-01; NFR-SEC-002, NFR-SEC-005

### Phase 35 — 분석, 오류 로깅과 개인정보 경계 (~1 man-day)

**목표**

SRS의 모든 분석 이벤트를 포트 뒤에 구현하고 민감 정보 유출을 막는다.

**테스트 먼저**

- 14개 이벤트의 이름·필수 속성 contract test와 중복 발행 테스트를 작성한다.
- 자유 입력, 파일 전체 경로, API 키와 사용자 식별자가 payload/log에 들어가면 실패하는 redaction test를 작성한다.

**구현**

- AnalyticsPort와 DiagnosticLoggerPort adapter를 만든다.
- app/open, onboarding, session, answer, audio, rating, undo, pause/resume/complete, voice/reminder/report 이벤트를 연결한다.
- debug는 local log, release는 공급자 독립 adapter로 두고 사용자 추적 ID를 생성하지 않는다.

**종료 조건**

- SRS 20장의 모든 이벤트 contract가 통과한다.
- APK와 로그 fixture에서 TTS 키·신고 상세·개인 식별 정보가 검출되지 않는다.

**추적**: PRD·SRS 19.5, 20장; NFR-SEC-001, NFR-SEC-004

### Phase 36 — 영어 UI와 학습 콘텐츠 완성 (~1 man-day)

**목표**

영어 사용자가 프로토타입의 모든 기능을 자연스러운 표현으로 사용할 수 있게 한다.

**테스트 먼저**

- 사용자 노출 string key, plurals, placeholder와 20개 LocalizedCardCopy 완전성 테스트를 작성한다.
- 질문/정답 alt text, word parts, street note와 pronunciation guide 누락 테스트를 작성한다.

**구현**

- `values/strings.xml`, notification, accessibility, 오류와 정책 navigation 문구를 완성한다.
- 영어 20개 카드 설명을 여행자 맥락으로 독립 저술한 승인본으로 정리한다.
- 고정 문자열을 모두 resource 또는 locale content로 이동한다.

**종료 조건**

- 영어 key와 카드 콘텐츠가 100%이며 hardcoded user string lint가 0건이다.
- native English review status가 manifest에 기록된다.

**추적**: 구현 설계 14장; 전체 F 기능의 영어 표현

### Phase 37 — 일본어 UI·카드 콘텐츠의 독립 현지화 (~1 man-day)

**목표**

영어 직역이 아닌 일본인 여행자 관점의 일본어 경험을 완성한다.

**테스트 먼저**

- 일본어 string key 100%, 20개 카드, word parts, alt text, 카타카나 pronunciation guide와 reviewer metadata 테스트를 작성한다.
- 영어 문장 구조를 그대로 복제한 금칙 fixture와 미승인 콘텐츠가 release를 실패시키는 gate를 작성한다.

**구현**

- `values-ja/strings.xml`과 `cards-ja.json` 승인본을 통합한다.
- 일본 지명 표기 관습, 교통·여행 맥락과 경어를 반영한 독립 설명을 사용한다.
- 네이티브 검수 피드백을 반영하고 content version을 갱신한다.

**종료 조건**

- 일본어 UI·카드·접근성 문구·알림이 100% 완전하다.
- native Japanese와 한국어 사실 검수 승인이 없으면 release task가 실패한다.

**추적**: 구현 설계 14.4~14.8; 다국어 제품 요구

### Phase 38 — 앱별 언어 선택과 향후 언어 확장성 (~1 man-day)

**목표**

영어·일본어 전환과 향후 언어 추가를 enum 수정 없이 지원한다.

**테스트 먼저**

- 기기 en·ja·기타 최초 언어, 앱 설정 전환, 재실행 유지와 손상 ja→en fallback 테스트를 작성한다.
- UiLanguage와 LearningExplanationLanguage 분리 및 BCP-47 validation 테스트를 작성한다.

**구현**

- Android 13 per-app language와 하위 버전 AppCompat locale adapter를 구현한다.
- Settings language row와 English·日本語 sheet를 프로토타입 디자인으로 추가한다.
- LanguageTag manifest와 locale별 콘텐츠 repository를 연결한다.

**종료 조건**

- en↔ja 전환 뒤 모든 현재 화면과 다음 카드가 올바른 언어로 갱신된다.
- 제3 언어 fixture를 추가할 때 domain enum과 use case 수정이 필요하지 않다.

**추적**: 구현 설계 14.1~14.3, 14.9; 다국어 제품 요구

### Phase 39 — TalkBack, 키보드와 비시각 학습 경로 (~1 man-day)

**목표**

화면 판정 버튼 없이도 TalkBack만으로 전체 세션을 완료한다.

**테스트 먼저**

- 상단 상태→사진→질문/정답→action 읽기 순서와 custom action 테스트를 작성한다.
- 정답 전 custom action 차단, 정답 후 Got it/Review의 동일 transaction과 toast·Undo 테스트를 작성한다.
- 색상 외 텍스트·햅틱, 오디오 없는 전체 정보와 content description test를 작성한다.

**구현**

- 카드 semantics에 Mark as Got it/Mark for review action을 추가한다.
- decorative preview와 blur image를 접근성 트리에서 숨기고 focus restore를 구현한다.
- 모든 control 48dp, heading/role/state description을 검수한다.

**종료 조건**

- AC-020과 기능 명세 AC-27이 TalkBack instrumentation test와 수동 QA에서 통과한다.
- 영어·일본어 모두 음성 없이 핵심 정보를 읽을 수 있다.

**추적**: F-CARD-07; AC-020; NFR-A11Y-001, NFR-A11Y-003, NFR-A11Y-004, NFR-A11Y-005, NFR-A11Y-008

### Phase 40 — 화면 크기, 200% 글자와 시각 회귀 (~1 man-day)

**목표**

프로토타입의 시각 구조를 360~480dp와 큰 글자에서 유지한다.

**테스트 먼저**

- 360·390·480dp, API 26·최신 API, en·ja, 100·130·200% font screenshot matrix를 만든다.
- gesture/3-button navigation, cutout, bold text, high contrast와 landscape 안정성 테스트를 작성한다.

**구현**

- 고정 높이를 adaptive constraint로 교체하고 정보 영역 스크롤·inset 처리를 마무리한다.
- contrast, focus indicator, 긴 일본어 wrap과 최소 사진 높이를 조정한다.
- 프로토타입 기준 screenshot diff 허용 영역을 문서화한다.

스크린샷 회귀 매트릭스는 `LearnSessionScreenTest`의 `learn-answer-{locale}-{width}-{fontScale}` 케이스로 고정한다. API 26과 최신 API에서 같은 케이스를 실행하며, 시스템 상태바·내비게이션 바·cutout inset 영역(최대 4dp)과 비동기 사진 디코드 전 placeholder 영역은 diff 허용 영역으로 제외한다. 그 외 카드, 정보 패널, 포커스 테두리와 주요 버튼 영역은 픽셀 diff 승인을 요구한다.

**종료 조건**

- 200% 글자에서도 Show answer, audio, swipe, Undo, 완료가 가능하다.
- 핵심 화면 screenshot 회귀가 승인 baseline과 일치한다.

**추적**: NFR-A11Y-002, NFR-A11Y-006, NFR-A11Y-007

### Phase 41 — 오프라인, 손상 데이터와 오류 복구 (~1 man-day)

**목표**

네트워크와 일부 자산·저장 오류가 없어도 핵심 학습을 지속하거나 명확히 복구한다.

**테스트 먼저**

- 비행기 모드 전체 흐름, 사진 누락, 음성 누락, Room write 실패, session mismatch와 정책 연결 실패 테스트를 작성한다.
- DataStore·Room migration, 앱 업데이트, 부분 content version 변경 테스트를 작성한다.

**구현**

- `Photo unavailable`, provider 오류, 저장 Retry, record quarantine와 bundled policy fallback을 연결한다.
- 유효한 판정까지만 복구하고 데이터 손실 범위를 사용자에게 짧게 알린다.
- adapter 오류를 typed failure로 변환하고 UI가 예외 문자열을 직접 노출하지 않게 한다.

**종료 조건**

- AC-021과 기능 명세 AC-29가 비행기 모드 E2E에서 통과한다.
- 손상 파일 하나와 불일치 row 하나가 앱 시작을 막지 않는다.

**추적**: NFR-OFF-001, NFR-OFF-002, NFR-OFF-003, NFR-OFF-004; AC-021

### Phase 42 — 시작·카드·오디오 성능 최적화 (~1 man-day)

**목표**

SRS의 시작, 입력 반응, 프레임, 이미지 메모리와 오디오 목표를 계측해 달성한다.

**테스트 먼저**

- cold start, card swipe frame timing, answer reveal, audio start와 image memory benchmark를 작성한다.
- 기준치를 넘으면 CI nightly가 실패하도록 threshold를 설정한다.

**구현**

- Baseline Profile, startup path lazy loading, Coil size/caching/prefetch와 Compose recomposition을 최적화한다.
- drag 중 allocation과 blur 재계산을 제거하고 성능이 낮을 때 안전한 효과 축소를 적용한다.

**종료 조건**

- 중급 기준 기기에서 첫 화면 2초 이내, 입력 시각 반응 100ms 이내, audio 목표 300ms 이내다.
- swipe는 목표 60fps이며 45fps 아래 연속 하락이 출시 차단 기준을 넘지 않는다.

**추적**: NFR-PERF-001, NFR-PERF-002, NFR-PERF-003, NFR-PERF-004, NFR-PERF-005

### Phase 43 — 전체 자동 E2E와 요구사항 추적 게이트 (~1 man-day)

**목표**

모든 기능·인수 조건을 자동 시나리오와 양방향 추적표로 닫는다.

**테스트 먼저**

- 신규 en 사용자, 신규 ja 사용자, 방향 회귀, Undo, 복구, 두 음성, 오프라인, 언어 전환, TalkBack, 경계, 알림, 오류 복구 E2E를 작성한다.
- F/FR/NFR/AC ID 중 테스트·Phase·코드 tag 연결이 없는 항목에서 실패하는 validator를 작성한다.

**구현**

- Hilt fake를 사용하는 빠른 E2E와 실제 Room/DataStore/Media3를 사용하는 통합 E2E를 분리한다.
- flaky 재실행은 진단용으로만 두고 첫 실패를 숨기지 않는다.
- test report에 requirement ID와 screenshot/video artifact를 연결한다.

**종료 조건**

- 이 문서 9장의 모든 요구사항 ID가 구현·테스트 증거를 가진다.
- 전체 자동 suite가 clean emulator에서 연속 3회 통과한다.

**추적**: 기능 명세 P-01~P-10, 구현 설계 E2E-01~E2E-12, 전체 AC

### Phase 44 — 실제 기기 QA와 결함 안정화 (~1 man-day)

**목표**

자동화로 놓치기 쉬운 실제 입력·오디오·접근성·시각 문제를 제거한다.

**테스트 먼저**

- 발견된 각 결함은 재현 자동 테스트 또는 명시적 수동 회귀 case를 먼저 추가한다.

**구현·검증**

- API 26과 최신 안정 Android의 중급 실제 기기에서 핵심 흐름을 수행한다.
- gesture/3-button navigation, TalkBack, mouse/trackpad 경계, headset, background, 알림, 시간대 변경을 확인한다.
- 20개 이미지의 한국어 표기와 40개 음성 대응을 한 번씩 확인한다.
- 영어·일본어 화면을 프로토타입과 나란히 비교하고 P0/P1 결함을 수정한다.

**종료 조건**

- PRD·SRS 23.3의 출시 차단 결함이 0개다.
- 발견된 모든 수정에 회귀 증거가 남아 있다.

**추적**: PRD·SRS 23장; 전체 기능과 NFR

### Phase 45 — 보안·릴리스 인수와 전달 (~1 man-day)

**목표**

서명된 release 후보가 모든 문서와 출시 조건을 만족하는지 최종 확인한다.

**테스트 먼저**

- release AAB secret scan, permission allowlist, backup policy, locale/content completeness와 정책 version gate를 실행한다.
- clean install·upgrade·uninstall data loss 안내, 비행기 모드와 최종 smoke E2E를 수행한다.

**구현·정리**

- R8/ProGuard, signing 분리, versioning, release notes와 known issues를 정리한다.
- 디버그 QA 메뉴, API key, 생성 스크립트용 secret과 불필요 permission이 release에 없음을 확인한다.
- PRD·SRS, 기능 명세, 구현 설계, 본 계획의 상태와 최종 trace report를 고정한다.
- signed AAB, mapping, test report, screenshot, asset manifest와 checksum을 전달한다.

**종료 조건**

- AC-001~AC-024와 기능 명세 AC-01~AC-30이 모두 Pass다.
- NFR 출시 차단 항목, Android vitals 준비와 정책 연결이 확인된다.
- 배포 승인 외에 남은 구현 작업이 없다.

**추적**: NFR-SEC-001, NFR-SEC-002, NFR-SEC-003, NFR-SEC-004, NFR-SEC-005; AC-30

## 7. 프로토타입 기능 전체 커버리지

| 프로토타입 기능 | 출시 구현 | Phase |
|---|---|---:|
| 바깥 화면 바로가기 | debug QA 메뉴·test deeplink | 4~5 |
| Reset prototype | debug 데이터 초기화, release 제외 | 4, 8 |
| Welcome 이미지 콜라주·Get started | 동일 사용자 흐름 | 10 |
| Tutorial의 분식 질문·정답·방향 연습 | 기록과 분리된 튜토리얼 | 11 |
| Level 3종·Skip | DataStore 저장 | 12 |
| Interests 6종 다중 선택 | 0개 허용, DataStore 저장 | 12 |
| 홈 브랜드·streak·오늘 세션·mix | 실제 Room 집계 | 13, 31 |
| 네 카테고리·See all | 실제 카테고리 sheet와 세션 시작 | 13~15 |
| 주간 막대 | 실제 7일 데이터 | 13, 31 |
| Learn·Progress·Settings 하단 탭 | state 보존 내비게이션 | 5, 13 |
| 순서·전체 수·진행 막대 | 실제 세션 연결 | 16 |
| 활성 카드 뒤 2장 카드 스택 | 사진 prefetch 포함 | 17 |
| Fit 사진·blur 배경·포커스 프레임 | 20장 모두 적용 | 17 |
| 질문·Show answer | 사진 유지, 아래 정보 전환 | 18 |
| 정답·한국어·의미·Words·Street note | locale별 독립 콘텐츠 | 18, 36~38 |
| 왼쪽 Got it·오른쪽 Review | 거리·축·overlay·퇴장 | 19~21 |
| 기기 밖 포인터 종료 | 터치·마우스·트랙패드 | 20 |
| 사진 아래 판정 toast·Undo | 4초, Room 복원 | 22 |
| 닫기와 Leave 확인 | 저장·Home Resume | 23 |
| 더보기·Report this card | 로컬 신고 큐 | 26 |
| 이미지 확대·라벨 | pinch·double tap·상태 보존 | 25 |
| 발음 재생 | Typecast 오프라인 | 27~28 |
| Session complete | 실제 집계·다음 복습 | 24 |
| Progress 통계·그래프·review 목록 | 실제 Room 집계 | 31~32 |
| Review 카드 상세·Practice | 1장 세션 | 32 |
| Session length·romanization·auto audio | 즉시 저장, 다음 상태 반영 | 28~29 |
| Review reminder | 권한·19:00·Due 조건 | 33 |
| Account & sync | 게스트 범위 안내, sync 비활성 | 34 |
| Reduce motion | 시스템 설정과 결합 | 29 |
| Privacy·Terms·App version | 실제 문서와 offline fallback | 34 |
| 공통 sheet·viewer·toast·back | 접근성·state 복원 포함 | 4~5, 22, 25~34 |
| ArrowLeft·ArrowRight·Escape | Got it·Review·Leave | 20 |

## 8. 기능 ID 추적표

| 기능 ID | 구현 Phase | 핵심 자동 검증 |
|---|---:|---|
| F-APP-01 | 8~9 | 초기 route·migration·복구 |
| F-ONB-01 | 10 | Welcome UI·내비게이션 |
| F-ONB-02 | 11 | tutorial 기록 분리·방향 |
| F-ONB-03 | 12 | level 단일 선택·Skip |
| F-ONB-04 | 12 | interests 0개·다중 선택 |
| F-HOME-01 | 13, 31 | 홈 표시·실제 집계 |
| F-SES-01 | 14~16, 30 | 세션 policy·transaction |
| F-SES-02 | 23 | leave·resume·process death |
| F-CARD-01 | 16~17 | stack·image·focus |
| F-CARD-02 | 18 | question·answer·비가림 |
| F-CARD-03 | 19~21 | threshold·direction·rating |
| F-CARD-04 | 22 | toast·Undo transaction |
| F-CARD-05 | 25 | viewer·zoom·상태 보존 |
| F-CARD-06 | 26 | validation·local queue |
| F-CARD-07 | 39 | TalkBack custom action |
| F-CARD-08 | 20 | device boundary |
| F-AUD-01 | 27 | Media3·failure·lifecycle |
| F-AUD-02 | 28 | voice selection·persistence |
| F-DONE-01 | 24 | counts·complete transaction |
| F-PROG-01 | 30~32 | review policy·aggregates·UI |
| F-PROG-02 | 32 | detail·one-card practice |
| F-SET-01 | 28~29 | length·guide·autoplay |
| F-SET-02 | 29 | system/app motion policy |
| F-REM-01 | 33 | permission·WorkManager |
| F-ACC-01 | 34 | guest data scope |
| F-LEGAL-01 | 34 | online·offline policy |

## 9. 요구사항·인수 조건 전체 추적

### 9.1 PRD·SRS 기능 요구사항

| ID | Phase | ID | Phase |
|---|---:|---|---:|
| FR-ONB-001 | 10~12 | FR-SES-001 | 13 |
| FR-ONB-002 | 10 | FR-SES-002 | 13 |
| FR-ONB-003 | 10 | FR-SES-003 | 8, 13~14 |
| FR-ONB-004 | 11 | FR-SES-004 | 14, 29 |
| FR-ONB-005 | 11 | FR-SES-005 | 13~15 |
| FR-ONB-006 | 12 | FR-SES-006 | 16 |
| FR-ONB-007 | 12 | FR-SES-007 | 23 |
| FR-ONB-008 | 12 | FR-SES-008 | 23 |
| FR-ONB-009 | 12 | FR-SES-009 | 24 |
| FR-ONB-010 | 12 | FR-SES-010 | 24 |

### 9.2 비기능 요구사항

| ID | Phase | 검증 |
|---|---:|---|
| NFR-PERF-001 | 42 | cold start macrobenchmark |
| NFR-PERF-002 | 42 | input-to-frame benchmark |
| NFR-PERF-003 | 42 | swipe frame timing |
| NFR-PERF-004 | 17, 42 | 2장 prefetch·memory |
| NFR-PERF-005 | 27, 42 | audio start timing |
| NFR-OFF-001 | 27, 41 | 비행기 모드 E2E |
| NFR-OFF-002 | 7, 21, 41 | 판정 transaction |
| NFR-OFF-003 | 9, 23, 41 | process death 복구 |
| NFR-OFF-004 | 9, 16, 41 | 손상 격리 |
| NFR-A11Y-001 | 4, 39 | 48dp scanner |
| NFR-A11Y-002 | 4, 40 | contrast scanner |
| NFR-A11Y-003 | 39 | semantics order |
| NFR-A11Y-004 | 39 | custom actions |
| NFR-A11Y-005 | 20~21, 39 | text·haptic·색상 |
| NFR-A11Y-006 | 29, 40 | remove animations |
| NFR-A11Y-007 | 40 | 200% font matrix |
| NFR-A11Y-008 | 18, 39 | audio 없는 학습 |
| NFR-SEC-001 | 1, 35, 45 | secret scan |
| NFR-SEC-002 | 34, 45 | permission allowlist |
| NFR-SEC-003 | 7, 45 | app-private storage |
| NFR-SEC-004 | 26, 35, 45 | report validation·redaction |
| NFR-SEC-005 | 34, 45 | backup-policy parity |

### 9.3 PRD·SRS 인수 조건

| ID | Phase | ID | Phase |
|---|---:|---|---:|
| AC-001 | 9~10 | AC-013 | 24 |
| AC-002 | 9, 12 | AC-014 | 23 |
| AC-003 | 12 | AC-015 | 27 |
| AC-004 | 12 | AC-016 | 27 |
| AC-005 | 8, 29 | AC-017 | 28 |
| AC-006 | 19~20 | AC-018 | 28 |
| AC-007 | 19~21 | AC-019 | 18, 29 |
| AC-008 | 19~21 | AC-020 | 39 |
| AC-009 | 19~20 | AC-021 | 27, 41 |
| AC-010 | 20 | AC-022 | 33 |
| AC-011 | 18 | AC-023 | 26 |
| AC-012 | 22 | AC-024 | 25 |

### 9.4 기능 명세 인수 조건

| ID | Phase | ID | Phase |
|---|---:|---|---:|
| AC-01 | 9~10 | AC-16 | 22 |
| AC-02 | 9, 12 | AC-17 | 22 |
| AC-03 | 12 | AC-18 | 23 |
| AC-04 | 12 | AC-19 | 24 |
| AC-05 | 8, 13 | AC-20 | 18, 29 |
| AC-06 | 13, 29 | AC-21 | 6, 27 |
| AC-07 | 17 | AC-22 | 6, 27 |
| AC-08 | 17 | AC-23 | 28 |
| AC-09 | 18~21 | AC-24 | 28 |
| AC-10 | 18 | AC-25 | 27 |
| AC-11 | 19~21 | AC-26 | 31 |
| AC-12 | 19~21 | AC-27 | 39 |
| AC-13 | 18~22 | AC-28 | 20 |
| AC-14 | 19~20 | AC-29 | 41 |
| AC-15 | 20 | AC-30 | 1, 35, 45 |

## 10. 테스트 실행 계층

| 계층 | 매 Phase | PR | nightly | release |
|---|---|---|---|---|
| format·lint·detekt·secret scan | 필수 | 필수 | 필수 | 필수 |
| domain·application JVM test | 필수 | 필수 | 필수 | 필수 |
| repository contract·Room·DataStore | 관련 Phase | 필수 | 필수 | 필수 |
| ViewModel·Compose semantics | 관련 Phase | 필수 | 필수 | 필수 |
| screenshot en·ja | 변경 화면 | 필수 | 전체 matrix | 전체 matrix |
| Hilt 통합·E2E | smoke | 필수 핵심 | 전체 | 전체 3회 |
| property·fuzz | 관련 규칙 | 필수 | seed 확대 | 필수 |
| Macrobenchmark·Baseline Profile | 해당 없음 | smoke | 필수 | 필수 |
| 실제 기기·TalkBack·오프라인 | 해당 없음 | 선택 | 주간 | 필수 |
| AAB permission·secret·asset 검사 | 해당 없음 | 해당 없음 | smoke | 필수 |

## 11. 출시 차단 기준

다음 중 하나라도 발생하면 Phase 45를 완료할 수 없다.

- 왼쪽과 오른쪽 판정이 프로토타입 안내와 다르다.
- 정답 확인 전에 학습 기록이 저장된다.
- 정답, toast 또는 sheet가 간판 사진을 가린다.
- 포인터가 기기 밖으로 나간 뒤 카드가 계속 따라간다.
- 판정·Undo·완료 transaction에서 학습 기록이 유실된다.
- 음성 선택과 실제 재생 파일 세트가 다르다.
- 영어 또는 일본어 UI·카드 콘텐츠가 하나라도 누락되거나 미승인이다.
- TalkBack만으로 Show answer, 음성 재생, 판정과 완료가 불가능하다.
- 비행기 모드에서 20개 카드, 두 음성, 저장 또는 정책 fallback이 동작하지 않는다.
- API key, 신고 자유 입력, 개인 식별 정보나 불필요한 permission이 release 산출물에 포함된다.
- P0·P1 결함, 반복 flaky test, ANR 또는 재현 가능한 crash가 남아 있다.

## 12. 변경 관리

프로토타입이나 SRS가 변경되면 다음 순서로 계획을 갱신한다.

1. 변경된 화면·기능·요구사항 ID를 식별한다.
2. 기존 port와 domain policy 영향 여부를 판단한다.
3. 실패하는 회귀 테스트를 먼저 추가한다.
4. 영향을 받는 Phase를 재개하거나 새 Phase를 추가해 man-day를 다시 산정한다.
5. 프로토타입 기능 커버리지와 요구사항 추적표를 함께 갱신한다.
6. en·ja 콘텐츠, 접근성, 오프라인과 migration 영향을 빠짐없이 재검증한다.

45 man-day는 현재 확정된 HTML과 문서 범위의 기준선이다. 범위가 바뀌면 기간도 함께 바뀌며, 품질 Phase를 줄여 기능 증가분을 흡수하지 않는다.
