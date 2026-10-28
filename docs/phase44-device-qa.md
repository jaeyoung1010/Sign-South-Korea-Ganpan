# Phase 44 실제 기기 QA 기록

이 문서는 Phase 44의 수동 QA 실행 기록과 자동 회귀 테스트의 증거 위치를 고정한다. 실제 기기 실행이 필요한 항목은 `manual-pending` 상태로 시작하며, 실행 후 상태와 artifact 경로를 같은 항목에 기록한다.

## 실행 매트릭스

| 축 | 대상 |
|---|---|
| Android | API 26, 최신 안정 API |
| 내비게이션 | gesture, 3-button |
| 화면 | 360dp, 390dp, 480dp, cutout, landscape |
| 접근성 | TalkBack, 200% font scale, bold text, high contrast, Reduce motion |
| 주변기기 | mouse/trackpad, wired/Bluetooth headset |
| 상태 | offline, background/foreground, process recreation, timezone change |
| 콘텐츠 | 이미지 20개, Typecast 음성 20개, en/ja |

`P0/P1 blockers: 0`

`P0/P1 blockers: 0`은 현재 알려진 출시 차단 결함 수이며, `manual-pending` 항목의 실행 완료를 의미하지 않는다. 수동 실행이 끝나면 결과와 캡처·영상 경로를 이 문서에 추가한다.

## 회귀 케이스

### QA-01

- API 26 gesture navigation에서 온보딩 → 5장 학습 → 완료 흐름을 수행한다.
- 상태: manual-pending
- 기대: 시스템 gesture 영역과 카드 판정 gesture가 충돌하지 않는다.

### QA-02

- 최신 안정 Android gesture navigation에서 360/390/480dp 카드와 200% 글꼴을 확인한다.
- 상태: manual-pending
- 기대: 카드 stack, 정답 정보, 주요 액션이 잘리지 않는다.

### QA-03

- API 26 3-button navigation에서 질문·정답·닫기 흐름을 수행한다.
- 상태: manual-pending
- 기대: 하단 내비게이션 영역을 침범하지 않고 뒤로가기 동작이 유지된다.

### QA-04

- 최신 안정 Android 3-button navigation에서 가로·세로와 큰 글자 조합을 확인한다.
- 상태: manual-pending
- 기대: 판정 버튼과 정보 패널이 화면 밖으로 밀리지 않는다.

### QA-05

- TalkBack만으로 Show answer, Play audio, Got it, Review, Undo, 완료를 수행한다.
- 상태: manual-pending
- 기대: 사용자 지정 액션 순서와 음성 안내가 기능을 완료하는 데 충분하다.

### QA-06

- mouse/trackpad 포인터를 카드 frame 밖으로 이동한 뒤 재진입한다.
- 상태: automated
- 기대: 마지막 경계 좌표에서 포인터를 종료하고 카드가 바깥 포인터를 따라가지 않는다.

### QA-07

- 유선/Bluetooth headset을 연결한 상태에서 두 voice를 교차 재생하고 background/foreground를 반복한다.
- 상태: manual-pending
- 기대: 선택한 provider의 파일이 재생되고 중복 callback이나 유령 재생이 없다.

### QA-08

- 카드 정답과 음성 재생 중 background → foreground, process recreation을 수행한다.
- 상태: manual-pending
- 기대: 현재 카드와 저장된 세션 위치를 복구하고 재생 리소스를 안전하게 정리한다.

### QA-09

- Due 0/1/복수, 알림 권한 거부·승인, timezone 변경을 확인한다.
- 상태: manual-pending
- 기대: 권한 승인과 Due 조건에서만 알림이 예약되고 locale 문구가 맞다.

### QA-10

- 20개 이미지의 한국어 표기, focus frame, 누락 격리를 확인한다.
- 상태: automated
- 기대: 카드·이미지 20개가 일대일로 매핑되고 하나의 손상 파일이 전체를 막지 않는다.

### QA-11

- Typecast MP3 20개를 재생해 카드 ID 매핑을 확인한다.
- 상태: automated
- 기대: 40개 파일이 모두 존재하고 provider 선택과 실제 URI가 일치한다.

### QA-12

- 영어 UI와 카드 콘텐츠를 프로토타입 screenshot과 나란히 비교한다.
- 상태: manual-pending
- 기대: 승인된 baseline과 카드·정보 패널·주요 버튼 영역이 일치한다.

### QA-13

- 일본어 UI와 카드 콘텐츠를 native reviewer와 함께 프로토타입 screenshot과 비교한다.
- 상태: manual-pending
- 기대: 영어 문장 누락 없이 일본어 설명·음성 선택·레이아웃이 일치한다.

### QA-14

- PRD·SRS 23.3의 출시 차단 조건과 발견 결함 목록을 다시 확인한다.
- 상태: manual-pending
- 기대: P0/P1 결함, 반복 flaky test, ANR, 재현 가능한 crash가 없다.

## 증거 기록 규칙

- 자동 테스트는 매니페스트의 test reference와 Gradle report를 함께 보존한다.
- 실제 기기 케이스는 `device-api-navigation-locale-fontscale` 형식의 artifact 디렉터리에 screenshot/video와 실행 시각을 저장한다.
- 결함을 발견하면 먼저 재현 테스트 또는 이 문서의 명시적 수동 회귀 케이스를 추가하고, 수정 커밋과 연결한다.
