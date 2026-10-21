# Sign Korean Android 구현 설계서

| 항목 | 내용 |
|---|---|
| 문서 버전 | 1.0 |
| 상태 | 구현 기준 확정본 |
| 작성일 | 2026-10-17 |
| 플랫폼 | Android |
| 언어 | Kotlin |
| UI | Jetpack Compose |
| 기준 제품 문서 | [PRD-SRS.md](PRD-SRS.md) |
| 기준 기능 문서 | [functional-specification.md](functional-specification.md) |
| 기준 프로토타입 | [../index.html](../index.html) |
| 기본 사용자 언어 | 영어 |
| 출시 지원 언어 | 영어, 일본어 |

## 1. 목적

이 문서는 Sign Korean의 Android 앱을 실제로 구현하기 위한 기술 설계서다. 제품 요구사항과 프로토타입을 Kotlin 코드, 모듈, 포트, 어댑터, 저장 구조, 상태 전이, 다국어 콘텐츠, 테스트와 CI 규칙으로 변환한다.

핵심 목표는 다음과 같다.

1. 기능 변경이 화면, 저장소, 외부 라이브러리 전체로 번지지 않게 한다.
2. 비즈니스 규칙을 Android 프레임워크와 분리해 빠른 JVM 테스트가 가능하게 한다.
3. Room, DataStore, Media3, WorkManager를 교체하거나 확장할 수 있게 포트 뒤에 둔다.
4. 영어와 일본어를 처음부터 동등한 출시 언어로 다룬다.
5. 문자 번역이 아니라 해당 문화권 사용자가 이해하기 쉬운 설명을 독립적으로 작성한다.
6. TDD와 자동 품질 게이트를 구현의 필수 조건으로 삼는다.
7. 카드 방향, 저장, 복습, 음성, 오프라인처럼 오류 비용이 큰 영역을 다층 테스트한다.

절대적인 무결함은 현실적으로 보장할 수 없다. 이 설계는 결함을 조기에 발견하고, 영향 범위를 제한하고, 재발을 자동으로 차단하는 것을 품질 목표로 삼는다.

## 2. 기준과 우선순위

해석 충돌 시 다음 순서로 판단한다.

1. PRD·SRS의 제품 범위, 비기능 요구사항, 인수 기준
2. 기능 명세의 화면별 상태와 기능 규칙
3. 이 문서의 기술 구조와 구현 제약
4. HTML 프로토타입의 시각 배치, 카피, 모션 체감
5. Android 플랫폼 관례

다음 핵심 동작은 변경하지 않는다.

- 정답 확인 전에는 카드 판정을 저장하지 않는다.
- 왼쪽 스와이프는 Got it이다.
- 오른쪽 스와이프는 Review다.
- 정답 상태에 보이는 판정 버튼을 두지 않는다.
- 접근성 사용자는 TalkBack 사용자 지정 액션으로 같은 기능을 실행한다.
- 활성 카드 뒤에 다음 두 카드가 보인다.
- 사진은 핵심 간판을 자르지 않는다.
- 정답과 토스트는 사진을 덮지 않는다.
- Typecast 은경과 Gemini Flash Kore 중 하나를 선택한다.
- 20개 카드와 두 음성 세트는 오프라인에서 동작한다.

## 3. 설계 원칙

### 3.1 경량 헥사고날

헥사고날 아키텍처는 도메인 전체를 수십 개 추상화로 감싸는 목적이 아니다. 다음 경계에만 포트를 둔다.

- 영속 저장
- 번들 콘텐츠 읽기
- 오디오 재생
- 알림 예약
- 앱 언어 적용
- 시간
- 분석
- 신고 보관

순수 계산, 단일 화면 전용 변환, 단순 데이터 클래스에는 인터페이스를 만들지 않는다. 포트는 교체 가능성, 테스트 대역 필요성, 트랜잭션 경계 중 하나가 실제로 있을 때만 만든다.

### 3.2 의존성 규칙

- domain은 Android, Compose, Room, Hilt, Media3, WorkManager를 참조하지 않는다.
- application은 domain과 포트만 참조한다.
- adapters는 application이 정의한 출력 포트를 구현한다.
- feature는 입력 포트와 UI 모델만 참조한다.
- app은 Hilt 구성과 내비게이션을 담당하는 조립 지점이다.
- 참조 방향은 항상 바깥에서 안쪽으로 향한다.

Android 공식 권장 구조도 UI와 데이터 계층 분리, repository, 단일 진실 공급원, 단방향 데이터 흐름을 권장한다. 이 설계는 그 원칙을 헥사고날 경계에 맞춰 적용한다. 참고: [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations), [Guide to app architecture](https://developer.android.com/topic/architecture).

### 3.3 단일 진실 공급원

| 데이터 | 단일 진실 공급원 |
|---|---|
| 카드의 한국어 원문과 자산 경로 | 번들 콘텐츠 manifest |
| 영어·일본어 설명 | 언어별 번들 콘텐츠 |
| 카드 학습 상태 | Room |
| 활성 세션 | Room |
| 사용자 설정 | Proto DataStore |
| 현재 화면 표시 상태 | ViewModel의 immutable StateFlow |
| 현재 오디오 재생 | AudioPlayerPort 구현체 |

Room에 저장된 학습 상태만 Got it, Review, Due, Mastered 계산의 근거가 된다. UI 내부 카운터나 애니메이션 상태를 학습 기록으로 사용하지 않는다.

### 3.4 단방향 데이터 흐름

UI 흐름은 다음 한 방향을 지킨다.

1. Composable이 UiAction을 발생시킨다.
2. ViewModel이 입력 포트 또는 순수 reducer를 호출한다.
3. application이 포트로 도메인 상태를 변경한다.
4. repository가 Flow로 새 상태를 내보낸다.
5. ViewModel이 immutable UiState를 만든다.
6. Composable은 UiState만 렌더링한다.

Composable이 DAO, DataStore, Media3 Player, WorkManager에 직접 접근해서는 안 된다.

### 3.5 TDD

모든 기능 변경은 Red → Green → Refactor 순서를 따른다.

- Red: 인수 조건을 재현하는 실패 테스트를 먼저 작성한다.
- Green: 테스트를 통과하는 가장 단순한 구현을 작성한다.
- Refactor: 중복과 결합을 제거하고 전체 테스트를 다시 통과시킨다.
- Commit: 관련 테스트와 정적 검사가 모두 통과한 상태만 커밋한다.

버그 수정은 재현 테스트 없이 시작하지 않는다. 재현 테스트가 실패하는 것을 확인한 뒤 수정하며, 같은 테스트가 회귀 방지 자산으로 남아야 한다.

## 4. 기술 선택

| 영역 | 선택 | 이유 |
|---|---|---|
| 언어 | Kotlin | Android 공식 언어, 코루틴과 sealed type 활용 |
| Java toolchain | 17 | Compose와 Hilt 도구 체인 기준 |
| UI | Jetpack Compose | 상태 기반 UI와 테스트 semantics |
| 상태 | ViewModel + StateFlow | 수명주기와 UDF |
| 내비게이션 | Navigation 3 | 여러 화면과 상태 복원 |
| DI | Hilt | Android 수명주기와 테스트 바인딩 교체 |
| 구조화 저장 | Room | 트랜잭션, 관계, 마이그레이션 |
| 설정 저장 | Proto DataStore | 타입 안전한 소규모 설정 |
| 직렬화 | Kotlinx Serialization | 번들 JSON 콘텐츠 |
| 오디오 | AndroidX Media3 | 파일 재생과 오디오 포커스 |
| 백그라운드 | WorkManager | 복습 알림 계산과 재예약 |
| 이미지 | Coil | 번들 이미지 디코딩, prefetch, 캐시 |
| 시간 | java.time + ClockPort | 시간대 안전성과 테스트 가능성 |
| 단위 테스트 | JUnit, kotlinx-coroutines-test | JVM 빠른 테스트 |
| 속성 테스트 | Kotest property | 경계값과 조합 검증 |
| UI 테스트 | Compose UI Test | semantics 기반 동작 검증 |
| DI 통합 테스트 | Hilt Android Test | production binding 교체 |
| 성능 | Macrobenchmark, Baseline Profile | 시작·프레임 성능 회귀 검출 |

라이브러리 버전은 libs.versions.toml 한 곳에서 관리하고 개발 시작 시점의 상호 호환되는 최신 안정 버전으로 고정한다. 동적 버전과 plus 버전 표기는 금지한다.

Hilt는 Android 객체의 수명주기별 컨테이너와 테스트 binding 교체를 제공한다. 참고: [Dependency injection with Hilt](https://developer.android.com/training/dependency-injection/hilt-android), [Hilt testing guide](https://developer.android.com/training/dependency-injection/hilt-testing).

## 5. 모듈 구조

### 5.1 Gradle 모듈

| 모듈 | 책임 | 허용 의존성 |
|---|---|---|
| app | Application, Activity, Hilt 조립, navigation graph | 모든 feature와 adapter |
| domain | 엔터티, 값 객체, 포트, use case, 순수 규칙 | Kotlin 표준 라이브러리 |
| data | Room, Proto DataStore, 번들 JSON, repository adapter | domain |
| platform | Media3, WorkManager, LocaleManager, haptic, analytics adapter | domain |
| designsystem | 색상, 타이포, 컴포넌트, 아이콘, 접근성 helper | Compose |
| feature-onboarding | 온보딩 화면과 ViewModel | domain, designsystem |
| feature-learn | 홈, 세션, 완료 화면과 ViewModel | domain, designsystem |
| feature-progress | 진행·상세 화면과 ViewModel | domain, designsystem |
| feature-settings | 설정, 언어, 알림, 정책 화면과 ViewModel | domain, designsystem |
| test-support | fake port, fixture, coroutine rule, locale fixture | domain |

초기 코드 규모가 작을 때 feature 모듈을 더 나누지 않는다. 한 feature의 변경이 다른 feature의 재빌드를 지속적으로 유발하거나 팀 소유권이 분리될 때만 세분화한다.

### 5.2 금지 의존성

- domain → Android SDK
- domain → Hilt annotation
- feature → Room DAO
- feature → DataStore
- feature → Media3
- data → Compose
- platform → feature
- adapter 간 직접 호출
- static service locator
- 전역 mutable singleton

### 5.3 패키지 예시

    com.signkorean.app
    com.signkorean.domain.model
    com.signkorean.domain.port.input
    com.signkorean.domain.port.output
    com.signkorean.domain.usecase
    com.signkorean.domain.policy
    com.signkorean.data.asset
    com.signkorean.data.db
    com.signkorean.data.preferences
    com.signkorean.data.repository
    com.signkorean.platform.audio
    com.signkorean.platform.locale
    com.signkorean.platform.notification
    com.signkorean.feature.learn
    com.signkorean.feature.progress
    com.signkorean.feature.settings

## 6. 헥사고날 구성

### 6.1 입력 어댑터

입력 어댑터는 사용자의 행동을 application use case로 전달한다.

- Compose screen
- ViewModel
- WorkManager Worker
- Android notification action
- 앱 시작 bootstrap

ViewModel은 도메인 규칙을 직접 구현하지 않는다. 입력을 검증하고 use case를 호출하며 결과를 UiState와 UiEffect로 변환한다.

### 6.2 입력 포트와 use case

| Use case | 책임 |
|---|---|
| ResolveLaunchDestination | 온보딩과 세션 상태로 시작 화면 결정 |
| CompleteOnboarding | 수준·관심 분야·완료 상태 원자 저장 |
| BuildSession | Due·New·카테고리로 세션 구성 |
| ObserveActiveSession | 활성 세션과 현재 카드 관찰 |
| RevealAnswer | 현재 카드의 Answer 상태 저장 |
| RateCard | Got it 또는 Review 원자 처리 |
| UndoLastRating | 직전 판정과 복습 상태 롤백 |
| PauseSession | 현재 인덱스와 상태 저장 |
| CompleteSession | 합계 검증 후 완료 저장 |
| ObserveHome | 홈 세션·주간 상태 구성 |
| ObserveProgress | Learned·Mastered·Due 계산 |
| PracticeSingleCard | 카드 상세에서 1장 세션 생성 |
| UpdateSettings | 세션 길이·표기·오디오·모션 저장 |
| SelectVoice | 음성 변경과 현재 재생 중지 |
| PlayPronunciation | 현재 카드·선택 음성 재생 |
| ConfigureReminder | 권한 상태와 설정에 따라 예약 |
| ChangeAppLanguage | 영어·일본어 앱 언어 적용 |
| SubmitCardReport | 유효성 검사 후 로컬 큐 저장 |

단순 getter마다 use case 클래스를 만들지 않는다. 여러 포트를 조정하거나 재사용되는 도메인 규칙이 있을 때만 use case로 둔다.

### 6.3 출력 포트

| 포트 | 핵심 메서드 |
|---|---|
| CardCatalog | observeCards, getCard, getLocalizedCopy, validateBundle |
| LearningRepository | observeProgress, getReviewState, rateAtomically, undoAtomically |
| SessionRepository | create, observeActive, pause, advance, complete |
| UserSettingsRepository | observe, update, setLanguage |
| AudioPlayerPort | play, stop, observePlayback |
| ReminderSchedulerPort | schedule, cancel, observePermission |
| AppLocalePort | getSupported, getCurrent, setCurrent |
| CardReportRepository | enqueue, observePending |
| AnalyticsSink | record |
| ClockPort | now, zoneId |

RateCard의 저장은 LearningRepository와 SessionRepository가 따로 커밋하지 않는다. data 모듈의 하나의 transaction adapter가 두 테이블 변경을 같은 Room transaction으로 묶는다.

### 6.4 출력 어댑터

| 포트 | production adapter | test adapter |
|---|---|---|
| CardCatalog | BundledJsonCardCatalog | FakeCardCatalog |
| LearningRepository | RoomLearningRepository | InMemoryLearningRepository |
| SessionRepository | RoomSessionRepository | InMemorySessionRepository |
| UserSettingsRepository | ProtoDataStoreSettingsRepository | FakeSettingsRepository |
| AudioPlayerPort | Media3AudioPlayer | FakeAudioPlayer |
| ReminderSchedulerPort | WorkManagerReminderScheduler | FakeReminderScheduler |
| AppLocalePort | AndroidAppLocaleAdapter | FakeAppLocale |
| CardReportRepository | RoomCardReportRepository | InMemoryCardReportRepository |
| AnalyticsSink | ConsentAwareAnalyticsAdapter | RecordingAnalyticsSink |
| ClockPort | SystemClockAdapter | FakeClock |

## 7. 의존성 주입 설계

### 7.1 원칙

- 모든 production 의존성은 constructor injection 또는 Hilt module provider로 조립한다.
- domain의 생성자는 Hilt annotation을 사용하지 않는다.
- Android Context는 adapter 내부에만 주입한다.
- ViewModel은 입력 포트 또는 필요한 use case만 받는다.
- 테스트는 ViewModel과 use case를 직접 생성하고 fake를 주입한다.
- instrumentation test만 Hilt graph를 사용한다.

### 7.2 Hilt composition root

app 모듈은 다음 module만 가진다.

| Module | 제공 대상 | Scope |
|---|---|---|
| DatabaseModule | AppDatabase, DAO | Singleton |
| DataAdapterModule | repository 구현체를 port에 binding | Singleton |
| SettingsModule | Proto DataStore, settings repository | Singleton |
| PlatformModule | audio, locale, clock, analytics | Singleton 또는 명시된 scope |
| ReminderModule | WorkManager scheduler | Singleton |
| UseCaseModule | 순수 use case 생성 | unscoped |

Media3 Player는 Singleton으로 전역 공유하지 않는다. 앱 전역 AudioPlayerPort adapter는 내부에서 한 개의 player를 관리하되 학습 화면의 lifecycle command에 따라 준비·중지·해제한다.

### 7.3 Qualifier

Context처럼 실제로 같은 타입의 의미가 다른 경우에만 qualifier를 쓴다.

- ApplicationContext
- IoDispatcher
- DefaultDispatcher
- BundledContentJson

Typecast와 Gemini는 동일 인터페이스 구현 두 개로 만들지 않는다. 하나의 BundledAudioCatalog가 VoiceId와 CardId로 자산을 찾는다.

### 7.4 테스트 binding

- JVM 단위 테스트는 Hilt를 시작하지 않는다.
- feature 통합 테스트는 test-support의 fake를 직접 주입한다.
- app 수준 instrumentation test는 TestInstallIn으로 production adapter를 fake로 교체한다.
- 한 테스트만 다른 상태가 필요하면 BindValue를 사용한다.
- 테스트가 production 서버, 실제 알림, 실제 분석으로 나가서는 안 된다.

## 8. 도메인 모델

### 8.1 값 객체

| 타입 | 불변식 |
|---|---|
| CardId | 빈 문자열 금지, 번들 전체에서 고유 |
| SessionId | 고유 UUID |
| LanguageTag | BCP 47 형식 |
| VoiceId | TYPECAST_EUNKYUNG 또는 GEMINI_KORE |
| SessionLength | 5, 10, 20 중 하나 |
| FocusRect | x, y, width, height가 0~100이며 우하단이 100 이하 |
| ReviewStage | 0~5 또는 Mastered |
| CardPosition | 0 이상, session size 미만 |
| ReportDetails | 0~500 Unicode code point |

값 객체는 생성 시 검증하고 잘못된 상태를 만들 수 없게 한다.

### 8.2 핵심 엔터티

| 엔터티 | 주요 필드 |
|---|---|
| SignCard | id, order, korean, category, image, focus, audioByVoice |
| LocalizedCardCopy | cardId, languageTag, meaning, pronunciationGuide, wordParts, streetNote, altText |
| LearningProgress | cardId, stage, lastRating, dueAt, counts, mastered |
| LearningSession | id, orderedCardIds, currentIndex, ratings, status, timestamps |
| UserSettings | level, interests, sessionLength, locale, romanization, autoAudio, voice, motion, reminder |
| CardReport | id, cardId, reason, details, createdAt, state |

### 8.3 Rating

Rating은 GOT_IT과 REVIEW만 가진다. UI 좌우 방향은 domain Rating에 직접 저장하지 않는다. GestureDecision이 방향을 Rating으로 변환한 뒤 use case에 전달한다.

이 분리는 향후 제스처 방향이 바뀌거나 접근성 입력이 추가돼도 학습 데이터가 방향에 결합되지 않게 한다.

### 8.4 복습 정책

ReviewPolicy는 순수 Kotlin 함수다.

| 입력 | 다음 단계 | 다음 복습 |
|---|---:|---|
| 신규 + Review | 0 | 다음 날 |
| 신규 + Got it | 1 | 3일 뒤 |
| stage 0~4 + Review | 0 | 다음 날 |
| stage 1 + Got it | 2 | 7일 뒤 |
| stage 2 + Got it | 3 | 21일 뒤 |
| stage 3 + Got it | 4 | 60일 뒤 |
| stage 4 + Got it | 5 | 90일 뒤 |
| stage 5 + Got it | Mastered | 90일 뒤 유지 점검 |

날짜 계산은 FakeClock과 ZoneId를 입력으로 받는다. 자정, 일광 절약 시간, 시간대 변경에서 Instant 비교가 깨지지 않게 한다.

### 8.5 세션 구성 정책

SessionBuilder는 다음 입력을 받는 순수 함수다.

- 요청 길이
- Due 카드
- New 카드
- 선택 카테고리
- 관심 분야
- 수준
- deterministic seed

규칙은 다음과 같다.

1. 일반 세션은 Due 30%, New 70%를 목표로 한다.
2. Due가 많으면 Due 비율을 70%까지 높인다.
3. 선택 카테고리 세션은 해당 카테고리를 먼저 채운다.
4. 같은 카드 ID를 중복 선택하지 않는다.
5. Review 재삽입은 최초 세션 목록과 별도의 runtime queue로 처리한다.
6. 테스트에서 seed를 고정해 순서를 재현할 수 있어야 한다.

## 9. 저장 설계

### 9.1 Room 테이블

#### card_progress

| 열 | 형식 | 제약 |
|---|---|---|
| card_id | TEXT | Primary key |
| stage | INTEGER | 0~5 |
| mastered | INTEGER | Boolean |
| last_rating | TEXT nullable | GOT_IT, REVIEW |
| last_reviewed_at | INTEGER nullable | epoch millis |
| next_review_at | INTEGER nullable | epoch millis |
| review_count | INTEGER | 0 이상 |
| got_it_count | INTEGER | 0 이상 |
| version | INTEGER | optimistic state version |

#### sessions

| 열 | 형식 | 제약 |
|---|---|---|
| session_id | TEXT | Primary key |
| status | TEXT | ACTIVE, PAUSED, COMPLETED, ABANDONED |
| current_index | INTEGER | 0 이상 |
| requested_length | INTEGER | 1, 5, 10, 20 |
| source | TEXT | DAILY, CATEGORY, SINGLE_CARD |
| category | TEXT nullable | category enum |
| created_at | INTEGER | epoch millis |
| updated_at | INTEGER | epoch millis |
| completed_at | INTEGER nullable | epoch millis |

#### session_cards

| 열 | 형식 | 제약 |
|---|---|---|
| session_id | TEXT | Foreign key |
| position | INTEGER | 복합 Primary key |
| card_id | TEXT | 번들 CardId |
| source | TEXT | DUE, NEW, REINSERTED |
| answer_revealed_at | INTEGER nullable | epoch millis |
| rating | TEXT nullable | GOT_IT, REVIEW |
| rated_at | INTEGER nullable | epoch millis |

#### card_reports

| 열 | 형식 | 제약 |
|---|---|---|
| report_id | TEXT | Primary key |
| card_id | TEXT | CardId |
| reason | TEXT | enum |
| details | TEXT | 500자 이하 |
| created_at | INTEGER | epoch millis |
| sync_state | TEXT | LOCAL_ONLY, PENDING, SENT, FAILED |

### 9.2 Room 트랜잭션

다음 작업은 반드시 transaction이다.

- 세션 생성과 session_cards 삽입
- 판정과 card_progress 갱신, session_cards 갱신, current_index 증가
- Undo와 progress snapshot 복원, session index 감소
- 마지막 판정과 세션 완료
- 콘텐츠 버전 마이그레이션

Undo를 위해 판정 직전 LearningProgress snapshot을 session_cards의 별도 history table 또는 직전 한 건의 serialized snapshot으로 저장한다. 1차 출시는 한 번의 Undo만 지원하므로 세션당 가장 최근 snapshot 하나만 유지해도 된다.

### 9.3 Proto DataStore

Settings message는 다음 필드를 가진다.

- schema_version
- onboarding_completed
- level
- repeated interests
- session_length
- app_language_tag
- show_romanization
- auto_play_audio
- voice_id
- reduce_motion
- reminder_enabled
- reminder_local_hour
- reminder_local_minute

새 필드는 기본값을 정의한다. enum number를 재사용하지 않는다. 삭제한 필드 번호는 reserved 처리한다. corruption handler는 안전한 기본 설정으로 복구하고 진단 이벤트를 기록한다.

DataStore는 작은 설정에, Room은 관계와 부분 업데이트가 필요한 학습 데이터에 사용한다. 이 구분은 Android 공식 가이드와 일치한다. 참고: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore), [Room](https://developer.android.com/training/data-storage/room).

### 9.4 번들 콘텐츠

권장 자산 구조는 다음과 같다.

    assets/content/manifest.json
    assets/content/cards.base.json
    assets/content/en/cards.json
    assets/content/ja/cards.json
    assets/images/signs/01-hongdae-entrance.webp
    assets/audio/typecast/01-hongdae-entrance.mp3
    assets/audio/gemini/01-hongdae-entrance.wav

cards.base.json은 언어와 무관한 데이터를 가진다.

- id
- order
- korean
- category
- imageAsset
- focus
- audioByVoice
- contentVersion

언어별 cards.json은 LocalizedCardCopy만 가진다. 한 언어의 설명이 다른 언어 파일을 참조하지 않는다.

## 10. UI 아키텍처

### 10.1 화면 계약

각 feature는 다음 네 타입을 기본으로 가진다.

- ScreenRoute: ViewModel 취득과 lifecycle 수집
- Screen: UiState와 callback만 받는 stateless Composable
- UiState: 렌더링 가능한 immutable data
- UiAction: 사용자 입력의 sealed interface

일회성 결과는 UiEffect로 전달한다.

- navigation
- Snackbar
- permission request
- accessibility announcement
- open external policy

UI 상태에 Android Context, Player, DAO, Job을 넣지 않는다.

### 10.2 test tag

test tag는 화면 문구와 분리한 안정된 ID를 사용한다.

| 요소 | test tag |
|---|---|
| 카드 스택 | learn.card.stack |
| 활성 카드 | learn.card.active |
| Show answer | learn.answer.reveal |
| 음성 재생 | learn.audio.play |
| 확대 | learn.image.zoom |
| 세션 닫기 | learn.session.close |
| 토스트 | global.feedback |
| Undo | learn.rating.undo |
| 음성 선택 | settings.voice |

번역된 텍스트를 주 selector로 쓰지 않는다. 사용자 관점 문구 검증이 필요한 테스트만 locale별 텍스트를 직접 확인한다.

### 10.3 내비게이션

app 모듈이 top-level graph를 소유하고 각 feature가 destination을 제공한다. feature끼리 route 문자열을 직접 알지 않는다. navigation command는 typed destination으로 표현한다.

프로세스 복원 시 destination보다 영속 domain 상태를 우선한다. 예를 들어 세션 route가 복원됐지만 Room에 활성 세션이 없으면 Learn 홈으로 안전하게 이동한다.

### 10.4 화면별 ViewModel

| 화면 | ViewModel | 주요 입력 포트 |
|---|---|---|
| Launch | LaunchViewModel | ResolveLaunchDestination |
| Onboarding | OnboardingViewModel | CompleteOnboarding |
| Learn home | HomeViewModel | ObserveHome, BuildSession |
| Card session | LearnViewModel | ObserveActiveSession, RevealAnswer, RateCard, UndoLastRating, PlayPronunciation |
| Complete | CompletionViewModel | CompleteSession |
| Progress | ProgressViewModel | ObserveProgress, PracticeSingleCard |
| Settings | SettingsViewModel | UpdateSettings, SelectVoice, ChangeAppLanguage, ConfigureReminder |

## 11. 카드 제스처 설계

### 11.1 순수 GesturePolicy

제스처 판정 수학은 Composable 밖의 순수 클래스로 둔다.

입력:

- answerRevealed
- start point
- current point
- stage bounds
- density
- previous axis lock

출력:

- Ignored
- VerticalScroll
- Dragging(dxDp, overlay, strength)
- SnapBack(reason)
- Commit(GOT_IT)
- Commit(REVIEW)

### 11.2 임계값

| 항목 | 값 |
|---|---:|
| 축 판별 시작 | 12dp 초과 |
| 가로 우세 비율 | abs(dx) > abs(dy) × 1.2 |
| 정답 전 표시 제한 | ±30dp |
| 정답 전 안내 기준 | 28dp 초과 |
| 정답 후 판정 | 88dp 초과 |
| 오버레이 최대 강도 | 90dp |

정확히 88dp는 판정하지 않는다. 88dp를 초과해야 한다. 부동소수점 비교는 테스트에서 87.99, 88.00, 88.01을 확인한다.

### 11.3 방향 매핑

| dx | 판정 | 오버레이 | 퇴장 |
|---|---|---|---|
| 음수 | Got it | 카드 오른쪽 GOT IT | 왼쪽 |
| 양수 | Review | 카드 왼쪽 REVIEW | 오른쪽 |

방향을 Boolean으로 표현하지 않는다. SwipeDirection과 Rating enum을 명시적으로 매핑한다.

### 11.4 기기 경계

pointer 좌표는 stage bounds에 clamp한다. PointerEvent가 bounds를 벗어나거나 cancel되면 마지막 clamp 좌표로 한 번 판정한 뒤 drag state를 종료한다. 이후 move event는 새 down event 전까지 무시한다.

다음 상황을 모두 같은 정책으로 처리한다.

- 손가락이 화면 가장자리에서 끊김
- 마우스가 device frame 밖으로 이동
- 창 포커스 상실
- 시스템 제스처가 pointer를 취소
- 두 번째 손가락이 들어옴

### 11.5 중복 입력 방지

RateCard가 시작되면 ratingInFlight를 true로 두고 다음을 막는다.

- 두 번째 스와이프
- 키보드 화살표
- TalkBack 판정 액션
- 빠른 Undo 연타

Room transaction 결과 후 다음 카드 또는 오류 상태로 이동한다.

## 12. 학습 세션 상태 머신

### 12.1 상태

| 상태 | 허용 입력 |
|---|---|
| Loading | Retry, Exit |
| Question | Reveal, Zoom, Report, Exit, SwipeAttempt |
| Answer | PlayAudio, Zoom, Report, Exit, Swipe, AccessibilityRate |
| Rating | 없음 |
| Error | Retry, Exit |
| Completed | Home, ReviewCards |

### 12.2 전이

| 현재 | 이벤트 | 다음 | 영속 변경 |
|---|---|---|---|
| Loading | Loaded | Question | 없음 |
| Question | Reveal | Answer | reveal timestamp |
| Question | SwipeAttempt | Question | 없음 |
| Answer | SwipeLeft | Rating | Got it transaction |
| Answer | SwipeRight | Rating | Review transaction |
| Answer | AccessibleGotIt | Rating | Got it transaction |
| Answer | AccessibleReview | Rating | Review transaction |
| Rating | Success, more | Question | index 증가 |
| Rating | Success, last | Completed | session complete |
| Rating | Failure | Answer.Error | rollback |
| Question/Answer | Leave | Paused | session pause |
| Next Question | Undo | Previous Answer | rating rollback |

### 12.3 애니메이션과 저장 순서

사용자는 즉각 반응을 느껴야 하지만 데이터 일관성이 우선이다.

1. 입력 즉시 카드 settling 애니메이션 시작
2. 동시에 Room transaction 시작
3. 저장 성공 시 애니메이션 완료 후 다음 카드 확정
4. 저장 실패 시 카드 중앙 복원, 이전 Answer 유지, Retry 표시

애니메이션이 끝나기 전에 저장이 완료돼도 최소 모션을 유지한다. Reduce motion에서는 20ms 이하 페이드로 줄인다.

## 13. 오디오 설계

### 13.1 자산 매핑

AudioAssetKey는 CardId와 VoiceId의 조합이다. BundledAudioCatalog는 manifest를 읽어 정확한 URI를 돌려준다.

| VoiceId | 자산 |
|---|---|
| TYPECAST_EUNKYUNG | MP3 20개 |
| GEMINI_KORE | WAV 20개 |

앱은 TTS 제공자 API를 호출하지 않는다. 생성 API key, voice API response, 생성 스크립트는 Android app module과 배포 산출물에 포함하지 않는다.

### 13.2 재생 상태

- Idle
- Preparing
- Playing(cardId, voiceId)
- Failed(cardId, voiceId, reason)

새 재생 요청은 기존 재생을 중지하고 0초부터 시작한다. 화면이 background로 가거나 세션을 떠나면 stop한다.

### 13.3 자동 재생 중복 방지

AnswerOpened event에 고유 eventId를 부여한다. ViewModel 재생성이나 Compose recomposition은 같은 eventId를 다시 재생하지 않는다. 사용자가 수동 버튼을 누르는 것은 별도 요청이므로 항상 재생한다.

### 13.4 오류

- manifest 누락: provider 이름을 포함한 비차단 오류
- 파일 decode 실패: 오류 토스트와 진단 이벤트
- audio focus 거부: 다시 탭 안내
- 헤드폰 분리: 기본 Android 정책을 따르고 자동 재시작하지 않음

## 14. 다국어·현지화 설계

### 14.1 지원 언어

| LanguageTag | 표시명 | 출시 상태 | fallback |
|---|---|---|---|
| en | English | 필수 | 최종 fallback |
| ja | 日本語 | 필수 | 런타임 손상 시 en |

기기 언어가 영어나 일본어면 처음 실행 시 해당 언어를 사용한다. 다른 언어면 영어를 사용한다. 사용자는 Android 앱별 언어 설정과 앱 Settings에서 언어를 바꿀 수 있다.

Android 13 이상은 시스템의 앱별 언어 설정에 노출하고, 하위 버전은 AndroidX application locale API로 같은 값을 적용한다. 참고: [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages).

### 14.2 UI 언어와 학습 콘텐츠 언어 분리

두 개념을 분리한다.

- UiLanguage: 버튼, 제목, 오류, 알림, 정책 navigation
- LearningExplanationLanguage: 영어 의미, 단어 풀이, Street note, 사진 alt text

1차 출시에서는 둘이 같은 값을 사용한다. 데이터 모델은 분리해 향후 UI는 영어지만 설명은 일본어인 조합도 수용할 수 있게 한다.

한국어 원문과 한국어 발음 음성은 언어 선택과 무관하게 동일하다.

### 14.3 UI 문자열

- 기본 res/values/strings.xml은 완전한 영어 세트다.
- res/values-ja/strings.xml은 완전한 일본어 세트다.
- Kotlin과 Composable에 사용자 노출 문자열을 하드코딩하지 않는다.
- 문장을 문자열 조각으로 이어 붙이지 않는다.
- 개수는 plurals를 사용한다.
- 날짜, 시간, 숫자는 현재 locale formatter를 사용한다.
- 접근성 설명과 notification channel 이름도 번역 자원에 포함한다.

Android는 기본 resource와 locale qualifier로 언어를 선택하며 기본 resource가 완전해야 안전하게 fallback한다. 참고: [Localize your app](https://developer.android.com/guide/topics/resources/localization), [String resources](https://developer.android.com/guide/topics/resources/string-resource.html).

### 14.4 학습 설명은 번역이 아니라 현지화 저술

LocalizedCardCopy는 원문의 문장 구조를 옮기는 번역 결과가 아니다. 각 언어 사용자가 한국 여행 맥락에서 이해할 수 있게 독립적으로 작성한 설명이다.

공통 사실:

- 장소의 공식 한국어 표기
- 핵심 의미
- 지역과 이용 맥락
- 금지하거나 주의해야 할 사실

언어별 저술 가능 요소:

- 익숙한 유사 개념
- 설명 순서
- 외래어 표기
- 발음 보조
- 문화적 배경의 상세도
- 존대와 말투

예시:

| 항목 | 영어 | 일본어 |
|---|---|---|
| 홍대입구 의미 | Hongdae Entrance | 弘大入口（ホンデイック） |
| 맥락 시작 | A gateway to Hongdae, known for… | カフェやライブハウスが集まる弘大エリアの玄関口です。 |
| 발음 보조 | hongdae ipgu | ホンデイック |

일본어 설명은 영어 문장을 직역하지 않고 일본인 여행자가 이미 알고 있을 가능성이 높은 지명 표기, 교통 관습, 한자 어휘를 고려해 작성한다.

### 14.5 PronunciationGuide

romanization이라는 UI 이름은 영어 기준 표현이다. 내부 모델은 PronunciationGuide로 일반화한다.

- en: 라틴 문자 기반 읽기 도움
- ja: 가타카나 기반 읽기 도움
- ko: 사용하지 않음

읽기 도움은 한국어 음가를 완전히 대체하지 않으며 기본값은 Off다. 일본어 가타카나는 한국어 발음을 근사하므로 한국어 음성 재생을 우선 안내한다.

### 14.6 현지화 콘텐츠 스키마

LocalizedCardCopy는 다음 필드를 가진다.

| 필드 | 설명 |
|---|---|
| cardId | base 카드 연결 |
| languageTag | BCP 47 |
| displayMeaning | 사용자에게 가장 자연스러운 의미 |
| pronunciationGuide | 언어권별 읽기 도움 |
| wordParts | 한국어 조각과 현지화 설명 |
| streetNote | 여행자 관점 설명 |
| imageAltTextQuestion | 정답을 누설하지 않는 사진 설명 |
| imageAltTextAnswer | 정답을 포함한 사진 설명 |
| searchTerms | 미래 검색용 동의어 |
| contentVersion | 언어별 버전 |
| reviewer | 내부 검수 식별자 |
| reviewedAt | 검수 시각 |
| factReviewer | 한국어 원문 사실 검수 식별자 |
| factReviewedAt | 한국어 원문 사실 검수 시각 |

### 14.7 콘텐츠 제작 워크플로

1. 한국어 전문가가 카드의 사실 brief를 작성한다.
2. 영어 native content writer가 영어 설명을 독립 저술한다.
3. 일본어 native content writer가 일본 여행자 관점의 일본어 설명을 독립 저술한다.
4. 각 언어의 두 번째 검수자가 자연스러움, 문화 적합성, 여행 맥락을 검수한다.
5. 한국어 전문가가 두 설명의 사실 일치를 다시 확인한다.
6. 자동 lint가 필수 필드, 길이, 금칙어, 숫자, 고유명사를 검사한다.
7. locale별 screenshot과 실제 기기 preview를 확인한다.
8. 승인된 언어 파일만 release manifest에 포함한다.

기계 번역 결과를 그대로 출시 콘텐츠로 사용하지 않는다. 자동 도구는 초안 참고와 누락 검사에만 사용할 수 있고 native reviewer의 승인이 필요하다.

### 14.8 언어 완전성 게이트

출시 언어는 다음 조건을 모두 만족해야 한다.

- UI string key 100% 존재
- 20개 카드 LocalizedCardCopy 100% 존재
- wordParts 누락 0개
- alt text 누락 0개
- placeholder 일치 100%
- native review 승인 100%
- pseudo-locale와 200% 글꼴 UI 테스트 통과

일본어 카드 하나라도 누락되면 일본어를 release locale로 노출하지 않는다. 출시 후 런타임 손상처럼 예외적인 상황에서만 영어로 fallback하고 오류를 기록한다.

### 14.9 향후 언어 추가

새 언어는 다음만 추가한다.

1. LanguageTag 등록
2. Android UI resource 세트
3. 언어별 cards.json
4. native editorial checklist
5. locale test fixture와 screenshot baseline
6. locale completeness gate

도메인 enum에 ENGLISH, JAPANESE만 하드코딩하지 않는다. LanguageTag value object와 지원 manifest를 사용한다.

### 14.10 레이아웃 확장성

- 고정 높이 텍스트 상자를 피한다.
- 일본어 줄바꿈과 장문을 기준으로 최소 30% 길이 증가를 허용한다.
- 텍스트가 버튼 아이콘과 겹치지 않게 intrinsic size와 max lines 정책을 명시한다.
- future RTL 검증을 위해 ar-XB pseudo-locale를 CI에 포함한다.
- 문자열에 화살표로 방향을 표현할 때 locale layout direction과 학습 판정 방향을 혼동하지 않는다.
- 현재 판정 방향은 물리적 왼쪽·오른쪽이므로 RTL에서도 반전하지 않는다.

## 15. 알림 설계

ReminderSchedulerPort는 다음 입력을 받는다.

- enabled
- local time
- current ZoneId
- due count provider
- app language

Worker는 실행 시점에 Due를 다시 조회한다. 예약 당시 Due 수를 신뢰하지 않는다. 0이면 알림을 내지 않는다.

알림 문구는 locale resource와 plurals를 사용한다.

- en: 1 sign is ready to review / n signs are ready to review
- ja: 復習する看板が1件あります / 復習する看板がn件あります

일본어는 영어의 단복수 형태를 모사하지 않고 자연스러운 수량 표현을 사용한다.

## 16. 오류와 복구

### 16.1 오류 유형

| DomainError | 원인 | UI 처리 |
|---|---|---|
| ContentMissing | 카드·언어 콘텐츠 누락 | 해당 카드 건너뛰기 또는 오류 화면 |
| AssetMissing | 이미지·음성 파일 누락 | 대체 UI, 학습 계속 |
| PersistenceFailure | Room·DataStore 실패 | Retry, 현재 상태 유지 |
| InvalidSession | 인덱스·카드 불일치 | 유효 지점 복구 또는 홈 |
| AudioUnavailable | decode·focus 실패 | 비차단 토스트 |
| NotificationDenied | 권한 거부 | 설정 Off와 시스템 설정 안내 |
| UnsupportedLocale | 지원하지 않는 language tag | 영어 fallback |

adapter exception을 UI까지 노출하지 않는다. adapter는 exception을 typed error로 변환한다.

### 16.2 복구 우선순위

1. 사용자 학습 기록 보존
2. 현재 카드 상태 보존
3. 정답 읽기 가능 유지
4. 음성·애니메이션 같은 보조 기능 축소
5. 안전한 홈 이동

### 16.3 동시성

- repository public write는 IO dispatcher에서 실행한다.
- 같은 sessionId의 rating command는 Mutex 또는 database transaction gate로 직렬화한다.
- ViewModel은 in-flight action을 상태로 노출한다.
- Flow는 distinctUntilChanged로 불필요한 UI 갱신을 줄인다.
- 오래된 화면의 비동기 결과는 sessionId와 cardId가 현재와 같을 때만 적용한다.

## 17. TDD 실행 규칙

### 17.1 기능 개발 절차

1. PRD 인수 기준을 Given / When / Then 테스트 이름으로 변환한다.
2. domain 또는 ViewModel의 실패 테스트를 작성한다.
3. 필요한 포트를 최소 형태로 정의한다.
4. fake port로 use case를 통과시킨다.
5. production adapter의 contract test를 작성한다.
6. Compose component test를 작성한다.
7. 대표 user flow instrumentation test를 추가한다.
8. 구현과 테스트를 함께 refactor한다.
9. 모든 quality gate가 통과한 뒤 커밋한다.

### 17.2 테스트 더블 원칙

- 상태를 가진 협력 객체는 fake를 우선한다.
- 호출 횟수 자체가 요구사항일 때만 mock을 사용한다.
- framework 동작을 우리 단위 테스트에서 다시 검증하지 않는다.
- fake는 production port와 같은 contract test를 통과해야 한다.
- 랜덤 데이터는 seed를 출력해 재현 가능하게 한다.

Android 공식 테스트 가이드도 작은 테스트를 많이 두고 큰 테스트를 적게 두는 피라미드, hermetic fake, DI를 통한 대역 교체를 권장한다. 참고: [Testing strategies](https://developer.android.com/training/testing/fundamentals/strategies), [Use test doubles](https://developer.android.com/training/testing/fundamentals/test-doubles).

### 17.3 테스트 피라미드

| 계층 | 목표 비중 | 실행 위치 | 실행 시점 |
|---|---:|---|---|
| Unit | 65~70% | JVM | 모든 커밋 |
| Component | 20~25% | JVM 또는 emulator | 모든 PR |
| Feature integration | 5~10% | emulator | 모든 PR |
| Application E2E | 핵심 흐름 | emulator·실기기 | main·nightly |
| Release candidate | 전체 체크리스트 | 다기기 | 출시 전 |

비중은 테스트 수의 방향이며 품질 점수로 사용하지 않는다. 가장 낮은 비용의 계층에서 요구사항을 충분히 검증하는 것이 우선이다.

## 18. 단위 테스트 설계

### 18.1 GesturePolicy

| Test ID | Given | When | Then |
|---|---|---|---|
| UT-GES-001 | Question | dx -100 | Got it 저장 없이 SnapBack |
| UT-GES-002 | Question | dx +100 | Review 저장 없이 SnapBack |
| UT-GES-003 | Answer | dx -88.00 | 판정 없음 |
| UT-GES-004 | Answer | dx -88.01 | Got it |
| UT-GES-005 | Answer | dx +88.00 | 판정 없음 |
| UT-GES-006 | Answer | dx +88.01 | Review |
| UT-GES-007 | Answer | dx 11.99 | axis 미결정 |
| UT-GES-008 | Answer | horizontal ratio 1.19 | vertical 유지 |
| UT-GES-009 | Answer | horizontal ratio 1.21 | horizontal lock |
| UT-GES-010 | frame 밖 move | cancel 후 추가 move | 위치 추가 변경 없음 |
| UT-GES-011 | left drag | overlay | GOT IT은 오른쪽 |
| UT-GES-012 | right drag | overlay | REVIEW는 왼쪽 |

### 18.2 ReviewPolicy

- 신규 Review는 다음 날
- 신규 Got it은 3일 뒤
- 1→2는 7일
- 2→3은 21일
- 3→4는 60일
- 4→5는 90일
- 어떤 stage의 Review도 0과 다음 날
- stage 5 Got it은 Mastered
- 자정 직전·직후
- DST 진입·종료
- 한국에서 일본으로 시간대 변경
- clock이 과거로 조정된 경우 negative due가 생기지 않음

### 18.3 SessionBuilder

- 길이 5, 10, 20
- Due 30% 목표
- Due 부족 시 New로 채움
- Due 과다 시 최대 70%
- 같은 CardId 중복 없음
- 선택 카테고리 우선
- 관심 분야 가중치
- 고정 seed에서 동일 순서
- 카드 0개, 1개, 요청 길이보다 적은 경우
- Review 재삽입이 3~5장 뒤
- 마지막 2장 남았을 때 안전한 재삽입

### 18.4 RateCard

- Question에서는 거부
- Answer Got it transaction
- Answer Review transaction
- progress와 session index 함께 변경
- 저장 실패 시 둘 다 변경 없음
- 빠른 두 번 입력 중 한 번만 적용
- 마지막 카드 판정에서 completed
- total = gotIt + review
- 오래된 cardId command 무시

### 18.5 Undo

- history 없음은 안전한 no-op
- Got it 복원
- Review 복원
- stage, dueAt, counts 전체 복원
- currentIndex 복원
- Answer 상태 복원
- 다음 판정 후 이전 Undo 불가
- process 재생성 후 최근 snapshot Undo
- rollback 실패 시 원래 판정 유지

### 18.6 Settings

- 기본 20, romanization Off, auto audio Off, Typecast
- 허용되지 않은 session length 거부
- voice 선택 저장
- 지원하지 않는 locale은 영어
- DataStore corruption 기본값 복구
- 진행 세션 중 길이 변경은 현재 세션에 미반영

### 18.7 Localization

- en, ja UI key 완전성
- en, ja 카드 20개 완전성
- base CardId와 locale CardId 집합 동일
- placeholder 이름과 수 동일
- wordParts 한 개 이상
- alt text Question은 정답 문자열을 직접 포함하지 않음
- Japanese content에 미승인 English fallback 없음
- plural count 0, 1, 2, 20
- locale switch 후 현재 cardId와 session index 유지

## 19. 어댑터·통합 테스트

### 19.1 Room

- entity read/write
- rate transaction atomicity
- Undo transaction atomicity
- foreign key와 unique constraint
- process 재생성 후 active session 복구
- schema 1→2, 2→3 migration
- migration 후 progress 수 보존
- corrupted row 격리
- 동시에 같은 session 판정 시 한 건만 성공

Room database 자체는 Android 기기의 in-memory database로 검증하고 migration test를 별도로 둔다. 참고: [Test and debug your Room database](https://developer.android.com/training/data-storage/room/testing-db).

### 19.2 DataStore

- 모든 기본값
- 업데이트 Flow 방출
- process 재시작 유지
- corruption handler
- unknown enum 안전 fallback
- locale와 voice migration

### 19.3 번들 콘텐츠

- manifest schema
- 카드 20개
- 이미지 20개 존재·최소 크기
- Typecast MP3 20개
- Gemini WAV 20개
- 파일 prefix와 CardId 일치
- 중복 경로 0개
- FocusRect 범위
- en·ja 콘텐츠 100%
- 배포 artifact에 API key 문자열 없음

### 19.4 Media3

- 올바른 voice와 card URI 선택
- 기존 재생 중지 후 새 재생
- background에서 stop
- missing asset typed error
- decode error typed error
- audio focus 거부
- Answer recomposition 중 자동 재생 한 번

### 19.5 WorkManager

- reminder Off면 work 없음
- permission denied면 work 없음
- Due 0이면 notification 없음
- Due 1과 복수 locale 문구
- 현지 19:00 계산
- 시간대 변경 재예약
- 기기 재부팅 후 유지

### 19.6 Hilt

- production graph compile
- fake graph로 전체 flow 실행
- TestInstallIn이 analytics, audio, reminder를 외부 side effect 없는 fake로 교체

## 20. ViewModel 테스트

각 ViewModel은 Turbine 또는 동등한 Flow test로 상태 순서를 확인한다.

### 20.1 LearnViewModel

- Loading → Question
- Reveal → Answer
- Answer → Rating → next Question
- final Rating → Completed effect
- persistence failure → Answer.Error
- PlayAudio state
- voice 변경 중 재생 중지
- Undo 복원
- process saved state와 Room 불일치 시 Room 우선
- locale 변경 후 같은 카드 유지

### 20.2 SettingsViewModel

- settings Flow 반영
- session length 선택
- romanization toggle
- auto audio toggle
- voice selection
- locale selection en ↔ ja
- notification permission result
- reduce motion

### 20.3 ProgressViewModel

- 빈 상태
- Learned, Mastered, Due 계산
- category aggregation
- 최근 7일 day boundary
- Needs review 최근 3개
- single-card session effect

## 21. Compose UI 테스트

Compose 테스트는 semantics를 기준으로 한다. 공식 Compose testing API는 semantics, 동기화, 사용자 액션을 제공한다. 참고: [Test your Compose layout](https://developer.android.com/develop/ui/compose/testing).

### 21.1 화면별

- Welcome 카피와 Get started
- Tutorial에서 Show answer 전후
- Tutorial 방향 왼쪽 Got it, 오른쪽 Review
- Level 미선택 Continue disabled
- Interests 0개 완료
- Home 새 세션과 Resume
- 카드 뒤 두 preview가 접근성 트리에서 숨겨짐
- Question에 정답 정보 없음
- Answer에 한국어, 의미, Words, Street note
- Answer에서 화면 판정 버튼 없음
- toast가 photo bounds와 겹치지 않음
- image viewer의 label 공개 조건
- completion 합계
- settings 두 voice radio state
- Progress 동적 통계

### 21.2 화면 크기와 글꼴

각 핵심 화면을 다음 조합으로 screenshot test한다.

- 폭 360dp, 390dp, 480dp
- font scale 1.0, 1.3, 2.0
- en, ja
- normal motion, reduce motion
- light theme

최소 조합은 핵심 카드 Question·Answer와 Settings에서 전부 실행한다. 나머지 화면은 pairwise 조합으로 실행 시간을 관리한다.

### 21.3 접근성

- 모든 액션에 content description 또는 text semantics
- 최소 touch target
- heading semantics
- switch와 radio selected state
- progress range
- custom Got it, Review action
- live region 판정 알림
- traversal order
- 중복 배경 이미지 제외

Compose 접근성 자동 검사를 활성화하고 TalkBack 수동 검증을 함께 수행한다. 참고: [Compose accessibility testing](https://developer.android.com/develop/ui/compose/accessibility/testing).

## 22. Application E2E 테스트

### E2E-01 신규 사용자

영어 기기에서 설치 → 온보딩 → 5장 세션 → 완료 → Progress까지 실행한다.

### E2E-02 일본어 사용자

일본어 기기에서 설치 → 일본어 온보딩 → 일본어 카드 설명 → 한국어 음성 → 완료한다. 영어 문장이 섞이지 않아야 한다.

### E2E-03 방향 회귀

왼쪽 Got it, 오른쪽 Review를 각각 실행해 DB, 토스트, 완료 합계, 복습 목록까지 확인한다.

### E2E-04 세션 복구

세 장 판정 → 앱 강제 종료 → 재실행 → Resume → 네 번째 카드부터 진행한다.

### E2E-05 Undo

Review → Undo → 동일 카드 Answer → Got it → DB와 완료 합계 확인.

### E2E-06 음성

Typecast 재생 → Gemini 선택 → 같은 카드 재생 → 앱 재실행 → Gemini 유지.

### E2E-07 오프라인

비행기 모드에서 온보딩 이후 20장, 두 음성, Progress, Settings를 완료한다.

### E2E-08 언어 전환

세션 4번째 Answer에서 en → ja 변경 → 같은 카드·같은 index·일본어 설명 유지 → ja → en 복귀.

### E2E-09 접근성

TalkBack custom action만으로 5장 세션 완료. 스와이프가 없어도 가능해야 한다.

### E2E-10 기기 경계

마우스 입력이 가능한 emulator에서 frame 밖 이동 후 카드가 포인터를 따라가지 않는지 확인한다.

### E2E-11 알림

Due 0, Due 1, Due 복수, permission deny, timezone 변경에서 알림을 확인한다.

### E2E-12 오류 복구

이미지 누락, 음성 누락, DB write failure fake를 각각 주입해 핵심 정보와 안전한 Retry가 유지되는지 확인한다.

## 23. 속성·퍼즈 테스트

### 23.1 SessionBuilder 속성

임의 카드 집합과 seed에서 다음 불변식을 검사한다.

- 결과 크기는 요청 길이 이하
- 모든 CardId는 입력 집합에 존재
- 중복 CardId 없음
- Due 비율 0~70%
- currentIndex는 결과 범위

### 23.2 ReviewPolicy 속성

- nextReviewAt은 now보다 뒤
- Review 후 stage는 항상 0
- Got it이 반복될 때 간격은 감소하지 않음
- mastered는 stage 5 성공 전 false

### 23.3 GesturePolicy 퍼즈

무작위 dx, dy, bounds, cancel 순서에서 다음을 검사한다.

- Question은 절대 Commit을 내지 않음
- 음수 dx는 Review가 되지 않음
- 양수 dx는 Got it이 되지 않음
- clamp 후 좌표는 bounds 내부
- cancel 후 추가 move는 상태를 바꾸지 않음

### 23.4 Content property

- locale별 CardId 집합 동일
- 모든 자산 URI 유일
- 모든 문자열은 유효 Unicode
- details 500자 제한
- focus rect가 이미지 내부

## 24. 성능 테스트

### 24.1 목표

| 항목 | 목표 |
|---|---|
| cold start | 중급 기기 2초 이내 첫 화면 |
| 카드 입력 반응 | 100ms 이내 시각 반응 |
| 카드 스와이프 | 60fps 목표, 연속 45fps 미만 금지 |
| 음성 시작 | 요청 후 300ms 목표 |
| memory | 원본 20장을 동시 decode하지 않음 |

### 24.2 측정

- Macrobenchmark cold and warm startup
- FrameTimingMetric 카드 스와이프
- Memory profiler 20장 세션
- Baseline Profile 생성
- release minified build에서 측정
- en과 ja 둘 다 측정
- 360dp 저사양 emulator와 대표 실기기

## 25. 보안과 개인정보 테스트

- git secret scan
- APK/AAB strings에서 Typecast·Gemini API key pattern scan
- BuildConfig와 resources에 key 없음
- 불필요한 INTERNET permission 검토
- camera, microphone, location, contacts permission 없음
- report details 로그 금지
- analytics consent Off에서 event 전송 없음
- exported component 최소화
- backup rule이 정책과 일치
- minified release에서 debug menu와 test asset 제외

## 26. CI 품질 게이트

### 26.1 모든 커밋

- Gradle configuration validation
- ktfmt 또는 Spotless
- detekt
- Android Lint
- domain·ViewModel unit test
- content schema·locale completeness·asset mapping
- secret scan

### 26.2 Pull request

- 모든 커밋 게이트
- component test
- Room·DataStore adapter test
- Compose UI test 핵심 조합
- Hilt graph test
- screenshot diff en·ja
- changed module coverage
- PRD requirement와 test ID 연결 확인

### 26.3 main

- feature integration
- application E2E
- migration test
- offline test
- accessibility automated check
- Macrobenchmark smoke

### 26.4 nightly

- 전체 emulator matrix
- API 26, 중간 버전, 최신 안정 버전
- phone 360dp, phone 390dp, foldable, tablet
- font scale 2.0
- en, ja, en-XA, ar-XB
- randomized property test 10,000 case
- monkey input smoke

### 26.5 release candidate

- minified signed AAB
- 실제 기기 수동 TalkBack
- 일본어 native linguistic QA
- 영어 native linguistic QA
- 오프라인 20장 완주
- 두 음성 40개 sample이 아닌 전체 자동 확인
- performance budget
- secret scan
- privacy·terms 실제 링크

## 27. 품질 기준

### 27.1 Coverage

| 영역 | line | branch |
|---|---:|---:|
| domain policy와 use case | 95% 이상 | 90% 이상 |
| data·platform adapter | 85% 이상 | 80% 이상 |
| ViewModel | 90% 이상 | 85% 이상 |

coverage 수치는 누락 신호일 뿐 품질의 단독 증거가 아니다. 다음 critical logic은 branch 100%를 요구한다.

- 왼쪽·오른쪽 Rating 매핑
- 88dp 임계값
- ReviewPolicy 단계
- RateCard transaction
- Undo
- locale fallback
- voice asset mapping

### 27.2 Flaky test

- 동일 커밋에서 재실행으로 통과한 테스트는 flaky로 기록한다.
- flaky test를 단순 재시도만으로 숨기지 않는다.
- 원인을 고치거나 격리된 non-blocking suite로 옮길 때 담당자와 기한을 남긴다.
- critical flow의 flaky test가 있으면 release를 차단한다.

### 27.3 테스트 가치

private 함수나 framework 자체 구현을 테스트하지 않는다. 외부에서 관찰 가능한 행동, domain 불변식, adapter contract를 테스트한다.

## 28. Adapter contract test

같은 port를 구현하는 fake와 production adapter가 동일한 contract를 통과하게 한다.

### LearningRepository contract

- 없는 cardId 조회 결과
- 첫 rating
- 동일 command idempotency
- Undo
- concurrent rating
- Flow emission order

### SessionRepository contract

- create
- pause/resume
- advance
- complete
- abandoned session
- process recreation

### UserSettingsRepository contract

- defaults
- update
- concurrent updates
- unknown enum
- corruption recovery

이 방식은 unit test fake가 production과 다른 의미를 가지는 문제를 줄인다.

## 29. 구현 순서

### 단계 0: 품질 기반

- Gradle multi-module
- version catalog
- formatting, lint, detekt
- test-support
- CI
- content schema validator
- secret scan

종료 조건: 빈 앱에서도 모든 quality gate가 실행된다.

### 단계 1: domain

- model과 value object
- ports
- ReviewPolicy
- SessionBuilder
- GesturePolicy
- RateCard와 Undo
- unit·property test

종료 조건: Android SDK 없이 핵심 규칙 전체가 JVM test로 통과한다.

### 단계 2: data

- asset schema와 en·ja 콘텐츠
- Room schema와 transaction
- Proto DataStore
- repositories
- migration과 contract test

종료 조건: fake와 production adapter가 같은 contract를 통과한다.

### 단계 3: app shell

- Hilt composition root
- Navigation 3
- designsystem
- locale 적용
- onboarding
- Learn home

종료 조건: 영어와 일본어로 온보딩 후 홈 진입.

### 단계 4: 학습 코어

- 카드 stack
- Question·Answer
- gesture와 boundary
- Undo
- 이미지 뷰어
- 완료
- TalkBack action

종료 조건: 5·10·20장 E2E와 방향 회귀 테스트 통과.

### 단계 5: media·settings

- Media3 adapter
- 두 voice
- auto play
- settings
- language picker
- reduce motion

종료 조건: en·ja와 두 음성 E2E 통과.

### 단계 6: progress·reminder

- Progress
- card detail
- review schedule
- WorkManager reminder
- report queue
- legal

종료 조건: 복습·알림·오프라인 시나리오 통과.

### 단계 7: hardening

- accessibility
- performance
- process death
- migration
- locale QA
- release security

종료 조건: release candidate gate 전부 통과.

## 30. Architecture Decision Record

### ADR-001 경량 헥사고날

결정: domain 중심 포트와 adapter를 사용하되 실제 외부 경계만 추상화한다.

결과: 테스트와 교체가 쉬우며 작은 앱에서 불필요한 추상화는 제한한다.

### ADR-002 순수 Kotlin domain

결정: domain에서 Android와 Hilt를 참조하지 않는다.

결과: JVM 테스트가 빠르고 프레임워크 변경 영향이 줄어든다.

### ADR-003 Hilt composition root

결정: app 모듈에서 production binding을 조립한다.

결과: Android lifecycle 통합과 test binding 교체를 표준화한다.

### ADR-004 Room SSOT

결정: 학습·세션 상태의 단일 진실 공급원은 Room이다.

결과: process death와 오프라인에서 일관성을 유지한다.

### ADR-005 Proto DataStore

결정: 설정은 타입 안전한 Proto DataStore를 사용한다.

결과: schema evolution과 corruption recovery가 명확하다.

### ADR-006 번들 콘텐츠와 음성

결정: 1차 출시는 사진·영어·일본어·두 음성을 번들한다.

결과: 네트워크 없이 동작하며 APK 크기 증가는 수용한다.

### ADR-007 현지화 콘텐츠 분리

결정: 한국어 사실 데이터와 언어별 설명을 별도 파일로 관리한다.

결과: 영어를 경유하지 않고 일본어를 독립 저술할 수 있다.

### ADR-008 물리 방향 고정

결정: RTL locale에서도 왼쪽 Got it, 오른쪽 Review를 반전하지 않는다.

결과: 학습 muscle memory가 언어와 무관하게 유지된다.

### ADR-009 TDD와 fake 우선

결정: 기능 테스트를 먼저 쓰고 mock보다 stateful fake를 우선한다.

결과: 테스트가 구현 세부보다 행동을 검증한다.

## 31. 요구사항·테스트 추적

| 요구사항 | 구현 | 대표 테스트 |
|---|---|---|
| 왼쪽 Got it | GesturePolicy, RateCard | UT-GES-004, E2E-03 |
| 오른쪽 Review | GesturePolicy, RateCard | UT-GES-006, E2E-03 |
| 정답 전 차단 | GesturePolicy | UT-GES-001~002, P-02 |
| frame 경계 | GesturePolicy | UT-GES-010, E2E-10 |
| Undo | UndoLastRating | UT Undo suite, E2E-05 |
| 세션 복구 | RoomSessionRepository | adapter test, E2E-04 |
| 두 음성 | AudioCatalog, Media3 adapter | asset test, E2E-06 |
| en·ja | locale adapter, localized content | locale suite, E2E-02·08 |
| 오프라인 | bundled adapters | E2E-07 |
| TalkBack | Compose semantics | accessibility suite, E2E-09 |
| 알림 | WorkManager adapter | integration suite, E2E-11 |

## 32. 구현 완료 정의

기능은 다음 조건을 모두 충족해야 완료다.

- 관련 인수 기준이 명확하다.
- 실패 테스트가 먼저 작성됐다.
- domain과 adapter 경계를 지켰다.
- production code에 service locator와 전역 mutable state가 없다.
- en·ja UI와 카드 콘텐츠가 모두 존재한다.
- 일본어 설명은 native review를 통과했다.
- 단위·component·integration test가 통과한다.
- 접근성 semantics와 자동 검사가 통과한다.
- 프로세스 재생성, 오프라인, 오류 상태를 검증했다.
- 성능 budget을 넘지 않는다.
- secret scan과 Android Lint가 통과한다.
- 문서와 코드의 test ID 추적이 갱신됐다.
- 변경 내용에 맞는 커밋 메시지로 커밋됐다.

## 33. 구현 시작 체크리스트

- [ ] package name과 applicationId 확정
- [ ] Android 프로젝트와 version catalog 생성
- [ ] CI 실행 환경 확정
- [ ] 영어·일본어 native reviewer 지정
- [ ] 20개 일본어 LocalizedCardCopy 작성
- [ ] locale_config에 en, ja 등록
- [ ] Room schema version 1 확정
- [ ] Proto Settings schema version 1 확정
- [ ] Hilt production·test module 구성
- [ ] 첫 failing test로 GesturePolicy 방향 매핑 작성
- [ ] 20개 사진·40개 음성·2개 언어 콘텐츠 validator 연결
- [ ] 비밀값 제외 규칙과 release artifact scan 연결
