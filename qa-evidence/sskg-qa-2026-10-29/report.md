# Sign South Korea Ganpan (SSKG) QA 보고서

## 1. 실행 정보

| 항목 | 내용 |
| --- | --- |
| 테스트 일시 | 2026-10-29 (KST) |
| 테스트 대상 | `com.signkorean.app.debug` |
| 브랜치 | `Redesign` |
| 테스트 빌드 | `app-debug.apk` (`e188060`, SHA-256 `d638aef5ca9ddb1d49fea08503057a9dea8dd27e734b86b4d086b7c04d4ec14b`) |
| 장치 | Samsung SM-G930S, Android 8.0.0 (API 26), 1080×1920 |
| 설치 방식 | ADB로 APK 설치 후 앱 데이터 초기화(`pm clear`)하고 온보딩부터 재실행 |
| 코드 변경 | 없음. QA 증적 이미지와 본 보고서만 추가 |

## 2. 테스트 범위

영어, 일본어, 중국어(간체)를 각각 선택해 온보딩, 홈, 공지사항, 설정, 언어 선택, 프로필, 진행률, 카테고리, 학습 세션, 이미지 전체 보기, 신고 시트, 법률 정보, 계정·동기화 화면을 확인했다.

다음 동작을 실제 장치에서 실행했다.

- 온보딩의 `Get started`, `Show answer`, `Got it`, `Continue`, 레벨·관심사 선택, 학습 시작
- 홈의 공지사항·설정·프로필·진행률·카테고리·`See all`
- 설정의 언어, 세션 길이, 로마자·자동 재생·애니메이션·복습 알림 토글, 법률 정보, 계정·동기화
- 학습 카드의 왼쪽/오른쪽 스와이프, 사진 탭(전체 화면), `More`, 신고 시트 스크롤 및 취소
- Android 하드웨어 뒤로가기와 각 화면의 화면 내 뒤로가기

전체 스크린샷은 다음 폴더에 저장했다.

- [영어 증적](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/en>)
- [일본어 증적](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/ja>)
- [중국어 증적](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/zh>)
- [공통 뒤로가기 증적](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/shared>)

## 3. 결과 요약

| 영역 | 결과 |
| --- | --- |
| 온보딩 및 초기 설정 | 통과 |
| 영어·일본어·중국어 현지화 | 법률 오류 문구를 제외하면 통과 |
| 홈·공지·설정·프로필·진행 화면 | 주요 흐름 통과 |
| 카테고리·세션 교체 | 통과 |
| 카드 좌우 스와이프 | 재현 테스트에서 통과 |
| 카드 사진 탭/전체 화면 | 통과 |
| Android 뒤로가기 | 세션·공지·설정에서 통과; 프로필 입력 포커스 상태의 첫 번째 뒤로가기는 키보드 닫기로 소비됨 |
| 법률 문서 열람 | 실패 — 아래 P1 이슈 |
| 복습 알림 토글 | UX 동작 확인 필요 — 아래 P2 이슈 |

## 4. 발견 사항

### P1 — 법률 문서가 모든 언어에서 로드되지 않음

- **중요도:** 높음(P1, 출시 전 수정 권장)
- **근거 스크린샷:**
  - [영어 법률 화면](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/en/23-legal.png>)
  - [일본어 법률 화면](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/ja/10-legal.png>)
  - [중국어 법률 화면](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/zh/14-legal.png>)
- **설명:** `Privacy policy`/`Terms of use` 버튼은 표시되지만 문서 영역에 영어·일본어·중국어 모두 `One or more policy copies could not be loaded.`가 나타난다. 일본어는 `同梱のポリシーを読み込めませんでした。`와 영어 오류가 함께 표시되고, 중국어도 `无法加载内置政策。` 아래에 영어 오류가 남는다. 두 정책 버튼을 각각 눌러도 실제 문서가 열리지 않고 오류 상태가 유지된다.
- **고쳐야 할 대상:** 번들 정책 파일의 Android asset 패키징 및 런타임 로더(`BundledPolicyDocumentRepository`와 Gradle asset/source-set 설정), 언어별 정책 리소스 매핑.
- **수정 방향:** debug/release APK에 각 언어의 개인정보 처리방침과 이용약관 파일이 실제 포함되는지 빌드 산출물에서 검증하고, 로더가 현재 언어 → 기본 언어 순서로 읽도록 수정한다. 문서가 없는 경우에는 영어 오류를 그대로 노출하지 말고 해당 언어의 명확한 안내를 제공한다. 개인정보/이용약관 버튼을 누르면 본문이 표시되는 UI 테스트를 추가한다.

### P2 — 복습 알림 토글을 켤 수 없는 상태에서 원인이 충분히 설명되지 않음

- **중요도:** 중간(P2, 사용성/동작 정책 확인 필요)
- **근거 스크린샷:**
  - [영어 복습 알림 탭 결과](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/en/24-review-reminder-tap.png>)
  - [중국어 복습 알림 탭 결과](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/zh/13-review-reminder-tap.png>)
- **설명:** 복습할 카드가 없는 초기 상태에서 `Review reminder`/`复习提醒` 토글을 눌렀다. 스위치는 켜지지 않고 `Review reminders are off.`/`复习提醒已关闭。` 토스트가 표시된다. API 26 장치라 Android 알림 권한 팝업이 원인은 아니며, 사용자는 토글을 켰는데도 꺼진 이유를 알기 어렵다.
- **고쳐야 할 대상:** `SettingsViewModel`의 알림 활성화·`NoDueCards` 처리와 `WorkManagerReminderScheduler`의 스케줄 설정, 토글 주변 안내 문구.
- **수정 방향:** 제품 정책이 “복습 카드가 있을 때만 활성화”라면 토글을 비활성화하고 “복습 카드가 생기면 켤 수 있습니다”처럼 이유를 표시한다. 항상 켜는 정책이라면 토글 상태는 저장하고 카드가 생기는 시점에 예약하도록 바꾼다. 두 경우 모두 탭 후 토글 상태와 메시지가 일치해야 한다.

## 5. 사용자 제보 항목 재현 결과

### 카드 스와이프

좌→우·우→좌 스와이프를 여러 카드에서 실행했다. 영어에서는 `1 of 20 → 2 of 20`, 다음 검토 동작에서 `3 of 21`로 이동했고, 중국어에서도 `第 1/20 张 → 第 2/20 张`으로 이동했다. 카드 내용과 이미지가 교체되며 세션이 멈추지 않았다. 이번 실행에서는 `Loading session..` 지연 문구도 재현되지 않았다.

`2 of 20 → 3 of 21`처럼 검토 카드가 다시 세션에 포함되어 분모가 증가하는 동작은 확인되었다. 이것이 의도된 복습 큐 정책이면 통과로 보며, 의도가 “고정된 세션 길이”라면 제품 기준을 정한 뒤 별도 수정이 필요하다. 근거는 [영어 오른쪽 스와이프 결과](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/en/18-session-after-swipe-right.png>)다.

### Android 하드웨어 뒤로가기

세션에서 뒤로가기를 눌러 홈으로 돌아왔고, 공지사항·설정에서도 이전 화면으로 돌아왔다. 프로필 입력 포커스가 남아 있는 상태에서는 첫 번째 뒤로가기가 키보드를 닫고 두 번째 뒤로가기에 홈으로 이동했다. 따라서 이번 빌드에서는 “뒤로가기를 누르면 설정으로 이동”하는 오동작은 재현되지 않았으며, 첫 번째 입력 화면 동작은 Android IME의 일반적인 키보드 닫기 동작으로 판단한다. [프로필 뒤로가기 관찰 증적](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/shared/16-back-profile-failure.png>)을 참고한다.

## 6. 현지화 점검

- 영어: `Ready to read Korea?`, `Explore by category`, `Today's session` 등 문맥에 맞게 표시되며, 주요 버튼 동작을 확인했다.
- 일본어: 홈·공지·설정·프로필·진행률·카테고리 교체·세션 문구가 일본어로 표시됐다. 요일과 카테고리명도 일본어로 표시됐다.
- 중국어(간체): 홈·공지·설정·프로필·진행률·카테고리 교체·세션 문구가 중국어로 표시됐다. 요일과 카테고리명도 중국어로 표시됐다.
- 공통 문제: 법률 로딩 실패 시 영어 오류 문구가 일본어·중국어 화면에 혼입된다. 이는 P1 이슈에 포함한다.
- 테스트 입력으로 프로필에 `QA%20Traveler`를 입력한 캡처가 있으나, 이는 ADB 공백 입력 방식 때문에 생긴 테스트 데이터이며 제품의 번역/표시 결함으로 판정하지 않았다.

## 7. 결론

핵심 학습 흐름, 카드 스와이프, 이미지 확대, 언어 전환, Android 뒤로가기는 이번 실기기 테스트에서 정상 동작했다. 다만 법률 문서를 전혀 열 수 없는 문제는 세 언어에서 공통으로 재현되므로 출시 전 수정이 필요하다. 복습 알림은 현재 정책과 UI 상태가 일치하지 않아 제품 결정 후 동작과 안내 문구를 함께 정리해야 한다.

본 QA 실행에서는 소스 코드를 수정하지 않았다.

## 8. 보고서 반영 후 수정 검증

보고서의 P1·P2 항목을 수정한 새 debug APK를 같은 실기기에 재설치해 재검증했다.

- 영어·일본어·중국어의 법률 화면에서 `Offline copy` 안내와 해당 언어의 개인정보 처리방침 본문이 표시됐다. 정책 버튼을 전환해도 로딩 오류가 나타나지 않았다.
- 중국어 이용약관도 선택 후 본문이 표시됐다.
- 복습 카드가 0개인 상태에서 복습 알림 토글을 켜면 스위치가 켜진 상태로 유지됐다. 알림 작업은 매일 확인하도록 예약하고, 실제 복습 카드가 생긴 경우에만 알림을 게시하도록 변경했다.
- [수정 후 영어 법률 화면](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/fix-verification/en-privacy.png>)
- [수정 후 일본어 법률 화면](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/fix-verification/ja-privacy.png>)
- [수정 후 중국어 법률 화면](</Users/choijaeyoung/Documents/ChatGPT/한국 간판 읽기 앱 만들기/qa-evidence/sskg-qa-2026-10-29/fix-verification/zh-privacy.png>)
