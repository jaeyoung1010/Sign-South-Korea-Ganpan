# Sign South Korea Ganpan 실기기 QA 보고서

## 테스트 개요

- 테스트 일시: 2026-10-28
- 디바이스: Samsung SM-G930S, Android 8.0
- 패키지: `com.signkorean.app.debug`
- 버전: `0.1.0-debug`
- 테스트 언어: English, 日本語
- 스크린샷: [`/tmp/sign-qa-screenshots.zip`](/tmp/sign-qa-screenshots.zip)

## 테스트 범위

- 온보딩: Welcome, Tutorial, Level, Interests
- 메인: Learn, Progress, Settings, Language
- 학습: 답안 공개, 좌우 스와이프, 오디오, 사진 보기, 신고, 세션 나가기
- 모든 화면에 표시된 버튼·토글·하단 탭·톱니바퀴 동작
- 디바이스 뒤로가기
- 지원 언어별 문구 및 레이아웃
- 런처 아이콘 라벨 `SSKG`

## 정상 확인

- `Reveal the answer first` 팝업으로 학습이 막히는 현상은 재현되지 않았다.
- `Marked as Got it` 팝업은 표시되지 않았다.
- 좌측 스와이프는 Got it, 우측 스와이프는 Review로 처리되는 흐름을 확인했다.
- 신고 작성·전송·취소, 세션 나가기·계속 학습, 언어 변경은 동작했다.
- Japanese 화면의 `今週`, `学習`, `進捗` 등 주요 문구는 현지화되어 있었다.
- Google Gemini TTS 선택 항목은 설정 및 학습 화면에서 확인되지 않았다.
- 런처 아이콘 아래에 `SSKG`가 표시되었다.

## 발견사항

### P1 — 설정에서 뒤로가기 후 하단 네비게이션이 사라짐

- 근거: [`sign-qa-10-back-from-settings.png`](/tmp/sign-qa-10-back-from-settings.png), [`sign-qa-11-second-back.png`](/tmp/sign-qa-11-second-back.png), [`sign-qa-29-back-settings-ja.png`](/tmp/sign-qa-29-back-settings-ja.png)
- 재현: Learn 홈에서 톱니바퀴를 눌러 Settings로 이동한 뒤 디바이스 뒤로가기를 한 번 누른다.
- 결과: 홈 콘텐츠는 보이지만 `Learn / Progress` 하단 바가 사라진다. 뒤로가기를 한 번 더 눌러야 복구된다. English와 Japanese 모두 재현되었다.
- 대상: `app/src/main/java/com/signkorean/app/MainActivity.kt`의 `rememberNavBackStack` 동기화 및 `TopLevelTabs` 렌더링
- 수정 방향: 화면 렌더링·하단 탭·선택된 탭이 동일한 동기화 네비게이션 상태를 사용하도록 정리하고, 뒤로가기 직후에도 하단 탭을 즉시 표시한다.

### P1 — Review 스와이프 이후 카드가 간헐적으로 멈춤

- 근거: [`sign-qa-39-ja-after-right-swipe.png`](/tmp/sign-qa-39-ja-after-right-swipe.png), [`sign-qa-42-ja-after-left-3.png`](/tmp/sign-qa-42-ja-after-left-3.png), [`sign-qa-46-ja-after-swipe-final.png`](/tmp/sign-qa-46-ja-after-swipe-final.png)
- 재현 관찰: Japanese 학습에서 Review 스와이프 직후 같은 답안 카드가 남고 여러 번의 스와이프가 반응하지 않았다. 앱을 재실행한 뒤에는 같은 카드에서 다시 진행되었다.
- 정상 동작도 확인되었으므로 입력 자체가 항상 실패하는 문제는 아니며, Review 처리·애니메이션 완료·다음 카드 로드 사이의 타이밍 문제로 추정된다.
- 대상: `feature-learn/src/main/kotlin/com/signkorean/feature/learn/CardPointerInput.kt`, `LearnCardStack.kt`, `app/src/main/java/com/signkorean/app/learn/LearnViewModel.kt`
- 수정 방향: `RateCard → RatingAnimationFinished → 다음 카드 로드` 상태 전환을 명시적으로 보장하고, 전환 중 입력 잠금이 영구적으로 남지 않도록 기기 통합 테스트를 추가한다.

### P1 — 신고 바텀시트에서 디바이스 뒤로가기가 시트를 닫지 않음

- 근거: [`sign-qa-50-ja-back-report-sheet.png`](/tmp/sign-qa-50-ja-back-report-sheet.png), [`sign-qa-51-ja-second-back-report.png`](/tmp/sign-qa-51-ja-second-back-report.png)
- 재현: 학습 화면에서 More → 신고 화면을 연 뒤 디바이스 뒤로가기를 누른다.
- 결과: 신고 시트가 닫히지 않고 입력란으로 포커스가 이동한다. 두 번째 뒤로가기에도 시트가 남는다.
- 대상: `MainActivity.kt`의 `BackHandler`, `AppOverlayHost`, `feature-learn/src/main/kotlin/com/signkorean/feature/learn/ReportSheet.kt`
- 수정 방향: 최상단 모달이 디바이스 뒤로가기를 우선 처리하도록 하고, 입력 포커스·키보드를 정리한 뒤 한 번의 뒤로가기로 시트를 닫는다.

### P2 — English Progress 통계 카드의 단어 줄바꿈 오류

- 근거: [`sign-qa-23-progress-en.png`](/tmp/sign-qa-23-progress-en.png)
- 결과: `Mastered`가 `Mastere / d`, `Due now`가 `Due / now`로 표시된다.
- 대상: `feature-progress/src/main/kotlin/com/signkorean/feature/progress/ProgressScreen.kt`의 `ProgressStatCard`
- 수정 방향: 카드 폭·폰트·줄바꿈 정책을 반응형으로 조정하고, 단어 중간 줄바꿈을 방지한다.

### P2 — Japanese 홈 제목에서 마지막 한 글자만 다음 줄로 떨어짐

- 근거: [`sign-qa-43-ja-after-relaunch.png`](/tmp/sign-qa-43-ja-after-relaunch.png)
- 결과: `韓国の街を読んでみましょう`가 마지막 `う`만 별도 줄에 표시된다.
- 대상: `feature-learn/src/main/kotlin/com/signkorean/feature/learn/LearnScreens.kt`, `feature-learn/src/main/res/values-ja/strings.xml`
- 수정 방향: 일본어 전용 제목 크기·최대 폭을 조정하거나 한 줄에 맞는 자연스러운 짧은 문구를 사용한다.

### P2 — English 스트릭 단수형이 `1 days`로 표시됨

- 근거: [`sign-qa-restored.png`](/tmp/sign-qa-restored.png)
- 결과: 스트릭이 1일일 때 `1 days`가 표시된다.
- 대상: `LearnScreens.kt`의 `pluralStringResource` 호출 및 `learn_streak_days` 앱 언어별 복수형 처리
- 수정 방향: 시스템 언어가 아니라 앱에서 선택한 언어의 복수형 규칙으로 리소스를 해석한다.

## 수정 내용 및 재검증

QA 결과를 바탕으로 다음 항목을 수정했다.

- 네비게이션 하단 바가 실제로 그려지는 백스택과 같은 경로를 사용하도록 정리해 Settings에서 한 번 뒤로간 뒤에도 `Learn / Progress`가 유지되도록 했다.
- Report 바텀시트에 우선순위가 높은 디바이스 뒤로가기 핸들러를 추가해 한 번의 뒤로가기로 시트를 닫도록 했다.
- 카드 포인터 입력을 초기 이벤트 패스에서 관찰하도록 바꿔 답안 영역의 세로 스크롤이 가로 스와이프를 삼키지 않도록 했다.
- Progress 통계 카드의 내부 여백과 라벨 줄바꿈을 조정해 English 단어가 중간에서 끊기지 않도록 했다.
- Japanese 홈 제목을 자연스럽게 줄이고, English 스트릭에 `1 day` 단수 리소스를 추가했다.
- 학습 카드의 `What does this sign mean?` 질문 단계를 제거하고, 카드가 로드되면 답안을 바로 표시하도록 했다. 답안 공개 기록은 백그라운드에서 자동 저장한다.
- 별도 `View sign photo` 버튼과 사진 위 화살표 아이콘을 제거하고, 카드 사진을 눌러 전체 화면 뷰어를 열도록 했다. 카드 사진은 비율을 유지한 채 영역을 가득 채우도록 조정했다.
- 작은 화면에서 카드가 고정 높이로 끝나던 여백을 화면 높이에 맞춰 확장하고, 사진·설명 콘텐츠가 카드 전체 높이를 사용하도록 조정했다.
- `1 day` 회귀 테스트를 추가했다.

수정 후 검증 결과:

- `./gradlew test --no-daemon` 통과
- `./gradlew :app:assembleDebug --no-daemon` 통과
- SM-G930S 실기기에 새 APK 설치 후 Settings → 디바이스 뒤로가기에서 하단 탭 유지 확인
- Progress 화면에서 `Mastered`, `Due now`가 단어 중간에서 줄바꿈되지 않음 확인
- Report 바텀시트에서 디바이스 뒤로가기 1회로 시트가 닫힘 확인
- 학습 답안에서 좌우 스와이프 후 `10 of 21 → 11 of 21 → 12 of 22`로 카드 진행 확인
- 새 APK에서 학습 진입 직후 `What does this sign mean?`와 `Show answer` 없이 답안이 바로 표시되는 것을 확인
- 새 APK에서 카드 하단의 `View sign photo`가 사라지고 사진 클릭 시 전체 화면 뷰어가 열리는 것을 확인
- 검증 후 시작 시점의 앱 데이터를 다시 복원함

## 테스트 종료 상태

탐색 QA 중 변경된 학습 진행·언어·설정 데이터는 시작 전 백업으로 복원했다. 탐색 QA 종료 후 발견사항을 기준으로 소스를 수정하고 위 재검증을 수행했다.
