# 전체 시뮬레이터 QA 보고서

- 실행일: 2026-10-22 (Asia/Seoul)
- 대상: `com.signkorean.app.debug` / Sign Korean 0.1.0-debug
- 환경: Android Emulator `SignKorean_API_36` (API 36, arm64)
- 빌드·설치: `./gradlew :app:installDebug` 성공
- 지원 언어: 영어(`en`), 일본어(`ja`) — 두 언어 모두 실제 화면에서 점검
- 코드 변경: QA 발견사항 수정 후 `QA-and-Fix` 브랜치에 반영

## 범위와 방법

초기 앱 데이터를 지운 뒤 영어로 일반 온보딩을 실제 조작했다. 이후 설정에서 일본어로 전환하여 동일 설치본에서 화면과 문구를 점검했다. 일반 사용자 흐름에서 닿는 화면을 우선 캡처했고, 닿기 어려운 화면은 debug QA의 화면 진입 경로로 보조 캡처했다. 이 보조 경로는 화면 확인용이며, 기능 검증은 일반 사용자 흐름에서 수행했다.

## 캡처 범위

| 범위 | 근거 |
| --- | --- |
| 영어 온보딩 | `01-welcome-en.png` ~ `12-home-en.png` |
| 영어 카테고리·학습·신고 | `13-category-list-en.png` ~ `21-report-submitted-en.png` |
| 영어 설정 | `22-settings-en.png`, `23-language-picker-en.png` |
| 일본어 실제 화면 | `24-settings-ja.png` ~ `42-terms-tap-ja.png` |
| 일본어 전체 화면 보조 캡처 | `ja-screen-welcome.png`, `ja-screen-tutorial.png`, `ja-screen-level.png`, `ja-screen-interests.png`, `ja-screen-home.png`, `ja-screen-learn.png`, `ja-screen-done.png`, `ja-screen-progress.png`, `ja-screen-settings.png` |
| 수정 후 재검증 | `52-regression-session-en.png`, `53-show-answer-fixed.png`, `54-settings-voice-fixed.png`, `55-settings-session-length-fixed.png`, `57-home-ja-fixed.png`, `59-legal-ja-after-reload-fix.png`, `60-terms-ja-fixed.png`, `62-level-card-fixed-ja.png`, `63-interests-card-fixed-ja.png`, `64-loading-photo-final.png` |

## 통과한 주요 흐름

- 앱 시작 후 환영 화면이 정상 표시된다.
- `Get started` → 튜토리얼 → `Show answer` → `Got it` / `Review` → `Continue` 흐름은 동작한다.
- 레벨 화면의 `Skip`, 관심사 화면의 `Start learning`은 홈으로 이동한다.
- 홈의 `See all`, 카테고리 시트 표시/닫기, 일일 세션 시작, 사진 뷰어 열기/닫기, `More` 메뉴, 카드 신고 사유 선택 및 신고 전송은 동작한다.
- 설정의 언어 선택에서 영어 → 일본어 전환이 동작한다.
- 일본어 설정에서 알림 리마인더를 켰을 때 Android 알림 권한 대화상자가 표시되며, 거부 후 “Android 설정에서 활성화 가능” 안내가 표시된다.
- 진행 상황 빈 상태와 설정·법적 정보 화면이 표시된다.
- `Account & sync`는 현재 버전에서 클라우드 동기화를 지원하지 않는다는 설명과 비활성화된 연결 버튼을 표시한다. 이는 명시된 제품 제한으로 확인했다.
- `Read online`은 외부 브라우저(Chrome)를 실행한다. 시뮬레이터의 Chrome 최초 실행 화면 때문에 문서 URL의 최종 도달 여부는 판정하지 않았다.

## 결함

### QA-01 — 온보딩 선택 카드가 반응하지 않음

| 항목 | 내용 |
| --- | --- |
| 중요도 | **P1** |
| 근거 스크린샷 | [레벨 카드 탭 후에도 선택 없음](09-level-beginner-en.png), [관심사 카드 탭 후에도 선택 없음](11-interests-selected-en.png) |
| 재현 | 1) 온보딩의 레벨 화면에서 `Beginner`, `Elementary`, `Not sure`를 각각 탭 2) 각 카드의 선택 상태가 바뀌지 않고 `Continue`가 계속 비활성화됨. 관심사 화면에서도 `Landmarks`를 탭했지만 체크 상태가 바뀌지 않음. |
| 설명 | 선택 가능한 카드로 접근성에 노출되고 탭 대상도 활성화되어 있으나, 화면의 선택 상태와 다음 단계 활성화에 반영되지 않는다. 사용자는 `Skip`으로만 진행할 수 있어 개인화 선택을 완료할 수 없다. |
| 고쳐야 할 대상 | `feature-onboarding/src/main/kotlin/com/signkorean/feature/onboarding/OnboardingChoices.kt`의 `LevelScreen`/`InterestsScreen`, `app/src/main/java/com/signkorean/app/OnboardingDestinations.kt`의 액션 전달, `app/src/main/java/com/signkorean/app/onboarding/OnboardingViewModel.kt`의 상태 저장 경로 |
| 수정 방향 | 실제 디바이스 UI 테스트를 추가해 카드 탭 후 `selectedLevel`/`selectedInterests`가 바뀌고 `Continue`가 활성화되는지 검증한다. 선택형 카드의 클릭 modifier와 이벤트 전달·상태 수집을 함께 추적한다. |

### QA-02 — 학습 세션의 `Show answer`가 동작하지 않음

| 항목 | 내용 |
| --- | --- |
| 중요도 | **P1** |
| 근거 스크린샷 | [탭 전후에도 Question 상태인 학습 카드](18-learn-answer-en.png) |
| 재현 | 1) 홈에서 `Start session` 2) 카드의 `Show answer`를 여러 차례 탭 3) 질문 상태가 유지되며 뜻·발음·평가 버튼이 나타나지 않음. |
| 설명 | 학습의 핵심 행동인 답 공개가 불가능해 카드 평가와 세션 완료로 진행할 수 없다. 튜토리얼 화면의 같은 성격의 버튼은 정상 동작하므로, 실제 세션 경로에 국한된 결함이다. |
| 고쳐야 할 대상 | `feature-learn/src/main/kotlin/com/signkorean/feature/learn/LearnScreens.kt`의 `RevealAnswer` 액션, `app/src/main/java/com/signkorean/app/learn/LearnViewModel.kt`의 `revealAnswer()`, `domain`의 `RevealAnswer` 구현 및 세션 저장소 경로 |
| 수정 방향 | `RevealAnswerResult`와 현재 카드 단계 전환을 실제 기기 통합 테스트로 검증한다. `Unavailable` 결과가 발생한다면 사용자에게 오류를 표시하고, 정상 세션에서는 Question → Answer 상태 전환을 보장한다. |

### QA-03 — 일본어 홈/카테고리 화면에 영어가 남아 있음

| 항목 | 내용 |
| --- | --- |
| 중요도 | **P2** |
| 근거 스크린샷 | [일본어 홈·카테고리 시트의 영어 카테고리명과 요일](33-category-loading-10s-ja.png) |
| 재현 | 1) 설정에서 일본어 선택 2) 홈과 `See all` 카테고리 시트 열기 3) `Landmarks`, `Neighborhoods`, `Markets`, `Transport` 및 `F/S/S/M/T/W/T`가 영어로 표시됨. |
| 설명 | 앱 탐색 문구는 일본어인데 카테고리와 요일은 영어라 언어가 혼용된다. 일본어 사용자에게 현지화된 경험이 아니다. |
| 고쳐야 할 대상 | `domain/src/main/kotlin/com/signkorean/domain/usecase/LoadLearnHomeUseCase.kt`의 하드코딩된 `CATEGORY_LABELS`, `app/src/main/java/com/signkorean/app/home/LearnHomeViewModel.kt`의 `Locale.ENGLISH` 요일 포맷 |
| 수정 방향 | UI locale에 맞는 리소스 기반 카테고리명과 요일 포맷을 사용한다. 도메인 모델에는 카테고리 key만 유지하고, 표시 문자열은 UI 계층에서 현지화한다. |

### QA-04 — 일본어 UI에서 법적 문서 본문이 영어로 표시됨

| 항목 | 내용 |
| --- | --- |
| 중요도 | **P2** |
| 근거 스크린샷 | [일본어 법적 정보 화면의 영어 Privacy policy 본문](28-legal-ja.png) |
| 재현 | 1) 일본어 설정 2) `Legal` → `Privacy policy` 3) 탭 제목·설명은 일본어지만 문서 제목과 본문이 영어로 표시됨. |
| 설명 | 법적 정보는 사용자가 정확히 이해해야 하는 내용인데, 일본어 사용자에게 영문 오프라인 사본만 제공된다. 단순히 버튼 레이블만 번역된 상태라 현지화 완료로 볼 수 없다. |
| 고쳐야 할 대상 | `assets/legal/privacy-policy.md`, `assets/legal/terms-of-use.md`, 그리고 locale별 문서 선택을 담당하는 정책 문서 로딩 경로 |
| 수정 방향 | 검수된 일본어 법적 문서 사본을 추가하고 앱 locale에 따라 로드한다. 번역본의 버전·발효일·온라인 원문 연결 정책도 함께 명시한다. |

### QA-05 — 설정에서 선택한 음성·세션 길이가 화면 요약에 반영되지 않음

| 항목 | 내용 |
| --- | --- |
| 중요도 | **P2** |
| 근거 스크린샷 | [Gemini 선택 토스트와 기존 음성 표시](35-voice-gemini-ja.png), [5장 선택 토스트와 기존 20장 표시](37-session-length-5-selected-ja.png) |
| 재현 | 1) 일본어 설정에서 `Voice` → `Gemini Flash · Kore` 선택 2) “선택했습니다” 토스트가 표시된 뒤 설정 요약은 `Typecast · Eunkyung`로 유지됨. 3) `Session length` → `5 cards · ~2 min` 선택 4) 토스트는 “5장을 선택했습니다”이지만 요약은 `20 · 約8分`으로 유지됨. |
| 설명 | 성공 토스트는 변경 완료를 알리지만 직후 화면이 이전 값을 표시한다. 사용자는 실제 저장값을 알 수 없고, 이후 학습 세션이 어떤 설정을 적용하는지 신뢰할 수 없다. |
| 고쳐야 할 대상 | `app/src/main/java/com/signkorean/app/settings/SettingsViewModel.kt`의 `selectVoice`/`selectSessionLength`, `data/src/main/kotlin/com/signkorean/data/settings/ProtoUserSettingsRepository.kt`의 설정 방출 경로, 설정 화면의 상태 수집 경로 |
| 수정 방향 | 변경 완료 효과를 보내기 전에 저장소의 최신 `UserSettings`가 UI 상태로 다시 방출되는지 확인한다. 선택 성공 후 요약·선택표시·다음 학습 세션이 동일한 값을 사용한다는 실제 기기 회귀 테스트를 추가한다. |

### QA-06 — 법적 정보의 `Terms of use` 탭이 문서를 전환하지 않음

| 항목 | 내용 |
| --- | --- |
| 중요도 | **P2** |
| 근거 스크린샷 | [이용약관 탭을 탭한 뒤에도 Privacy policy 본문이 유지됨](42-terms-tap-ja.png) |
| 재현 | 1) 일본어 설정 → `Legal` 2) 오른쪽 `Terms of use` 버튼 탭 3) 문서 제목·본문이 `# Privacy policy`로 유지되며 탭 전환이 일어나지 않음. |
| 설명 | 사용자가 약관을 열 수 없어 필수 법적 정보를 확인할 수 없다. 버튼은 활성 상태로 노출되어 있어 단순한 미지원 기능으로 보이지 않는다. |
| 고쳐야 할 대상 | `feature-settings/src/main/kotlin/com/signkorean/feature/settings/SettingsScreens.kt`의 `LegalUiAction.SelectDocument` 전달, `app/src/main/java/com/signkorean/app/MainActivity.kt`의 legal 액션 연결, `app/src/main/java/com/signkorean/app/legal/LegalViewModel.kt`의 선택 상태 갱신 |
| 수정 방향 | 두 탭의 클릭 이벤트와 `selectedDocumentId` 상태가 즉시 갱신되는지 확인하고, 각 탭이 서로 다른 번들 문서를 표시한다는 기기 UI 테스트를 추가한다. |

### QA-07 — `Loading session…` 이후 실제 카드 사진이 표시되지 않음

| 항목 | 내용 |
| --- | --- |
| 중요도 | **P1** |
| 근거 스크린샷 | [수정 전 무한 로딩](46-loading-after-start-session.png), [수정 후 실제 카드 사진](64-loading-photo-final.png) |
| 재현 | 1) 기존 세션이 있는 상태에서 앱을 재시작 2) 학습 화면 진입 3) `Loading session…`이 계속 표시되고 카드·사진·답 공개 버튼이 나타나지 않음. |
| 설명 | 세션 로더는 Active 결과를 반환했지만 Navigation3 엔트리 공급자가 최초 `Loading` 상태를 캡처해 화면이 갱신되지 않았다. 결과적으로 사진을 포함한 학습 카드 전체가 보이지 않았다. |
| 고쳐야 할 대상 | `app/src/main/java/com/signkorean/app/MainActivity.kt`의 navigation entry provider, `app/src/main/java/com/signkorean/app/learn/LearnViewModel.kt`의 사진 prefetch 경로, `app/src/main/java/com/signkorean/app/learn/CoilPhotoPrefetcher.kt` |
| 수정 방향 | 상태를 캡처하는 `remember`를 제거해 최신 UI 상태로 엔트리를 구성하고, 다음 카드 사진 prefetch는 IO 스코프에서 수행해 UI를 막지 않도록 했다. |

## 수정 후 재검증 결과

| 항목 | 재현 전 상태 | 수정 후 결과 | 증거 |
| --- | --- | --- | --- |
| QA-01 온보딩 선택 카드 | 탭해도 선택되지 않음 | 레벨·관심사 카드가 `checked=true`로 바뀌고 다음 버튼이 활성화됨 | `62-level-card-fixed-ja.png`, `63-interests-card-fixed-ja.png` |
| QA-02 답 공개 | 질문 상태에서 멈춤 | `Answer`, 번역, 단어, 발음 버튼이 표시됨 | `53-show-answer-fixed.png` |
| QA-03 일본어 홈 | 카테고리·요일이 영어 | `名所`, `街並み`, `金`, `土` 등 일본어 표시 | `57-home-ja-fixed.png` |
| QA-04 일본어 법적 문서 | 본문이 영어 | 일본어 개인정보처리방침 본문 표시 | `59-legal-ja-after-reload-fix.png` |
| QA-05 설정 요약 | 저장 토스트 후 이전 값 유지 | 음성·세션 길이 요약이 즉시 새 값으로 갱신 | `54-settings-voice-fixed.png`, `55-settings-session-length-fixed.png` |
| QA-06 이용약관 탭 | Privacy 본문이 유지됨 | 이용약관 탭에서 일본어 약관 본문으로 전환 | `60-terms-ja-fixed.png` |
| QA-07 세션 사진 로딩 | `Loading session…` 무한 표시 | `1 / 20`, 실제 사진, 답 공개 버튼 표시 | `64-loading-photo-final.png` |

## 현지화 평가

- 영어: 온보딩·튜토리얼·설정의 문장은 자연스럽고 목적이 명확했다. 예: “Read Korea in the real world”, “You can use the app without an account.”
- 일본어: 온보딩과 설정의 일반 UI 문구는 기계적 직역으로 보이지 않고 자연스럽다. 예: `現実の街で韓国語を読もう`, `答えを見る`, `復習リマインダー`.
- 수정 후 일본어 홈 카테고리·요일, 법적 문서 본문과 이용약관 탭을 재검증했다. `57-home-ja-fixed.png`, `59-legal-ja-after-reload-fix.png`, `60-terms-ja-fixed.png`에서 영어 잔류 없이 자연스러운 일본어 문구가 표시된다.

## 미확정/제한 사항

- 카테고리 선택은 짧게 `Loading session…`을 보인 뒤 홈으로 돌아왔다. 명확한 성공·실패 피드백이 없어 의도 여부를 판단할 근거가 부족하다. 재수정 시 카테고리 세션 시작의 완료 상태를 명시적으로 보여 주는지 함께 검증해야 한다.
- 수정 전 P1 결함으로 중단했던 답 공개·사진 표시 경로를 수정 후 재개했다. `53-show-answer-fixed.png`에서 답·발음·평가 영역이 표시되고, `64-loading-photo-final.png`에서 실제 카드 사진이 표시된다.

## 재검증 기준

1. 레벨·관심사 카드 탭이 선택 상태와 버튼 활성화를 즉시 갱신한다 — `62`, `63` 통과.
2. 실제 학습 카드의 `Show answer`가 답·발음·평가 액션을 표시한다 — `53` 통과.
3. 일본어 홈, 카테고리 시트, 요일, 법적 문서에 영어 UI 텍스트가 남지 않는다 — `57`, `59`, `60` 통과.
4. 음성·세션 길이 선택 후 화면 요약이 즉시 갱신된다 — `54`, `55` 통과.
5. 개인정보처리방침·이용약관 탭이 각각의 문서를 연다 — `59`, `60` 통과.
6. `Loading session…` 이후 실제 사진과 카드가 표시된다 — `64` 통과.
