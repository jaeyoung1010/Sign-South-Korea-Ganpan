# Sign Korean 통합 PRD·SRS

| 항목 | 내용 |
|---|---|
| 문서 버전 | 1.0 Final |
| 문서 상태 | Android 1차 출시 설계·개발 기준 |
| 최종 갱신일 | 2026-10-17 |
| 제품 유형 | Android 모바일 학습 앱 |
| 기본 UI 언어 | 영어 |
| 주요 사용자 | 한글을 대략 읽을 수 있는 한국 방문 예정자·방문객 |
| 인터랙션 기준 | [`../index.html`](../index.html) |
| 기능 상세 기준 | [`functional-specification.md`](functional-specification.md) |

---

## 1. 문서 목적과 기준 우선순위

이 문서는 Sign Korean의 제품 요구사항 문서(PRD)와 소프트웨어 요구사항 명세서(SRS)를 통합한 Android 앱 개발 기준이다. 제품 목표, 출시 범위, 화면 구조, 기능, 상태, 데이터, Android 구현 방향, 품질 및 인수 조건을 한 문서에 정의한다.

기획·디자인·개발·콘텐츠·QA가 해석을 달리할 경우 다음 순서로 판단한다.

1. 이 문서의 제품 범위·데이터·품질·출시 요구사항
2. [`functional-specification.md`](functional-specification.md)의 화면별 기능·상태·예외 규칙
3. [`../index.html`](../index.html)의 화면 구성, 시각 스타일, 카피, 전환과 제스처 체감
4. Android 플랫폼 관례와 접근성 요구사항

프로토타입과 문서가 충돌하면 기능 의미는 문서가 우선한다. 단, 카드의 좌우 판정 방향과 사진·정답 영역 배치는 현재 프로토타입을 확정 기준으로 한다.

## 2. 제품 정의

### 2.1 한 문장 정의

Sign Korean은 한국의 인기 여행지와 거리에서 볼 수 있는 한글 간판 사진을 카드로 학습하고, 정답 확인 후 좌우 스와이프로 기억 여부를 분류하는 Android 앱이다.

### 2.2 해결할 문제

한글 자모를 읽을 수 있는 외국인도 실제 거리에서는 다음 어려움을 겪는다.

- 교재와 다른 간판 글꼴, 배치, 조명, 배경 때문에 글자를 알아보기 어렵다.
- 글자를 읽어도 장소명이나 단어의 뜻과 쓰임을 모른다.
- 지도·번역 앱은 즉시 해석에는 유용하지만 반복 학습을 만들지 않는다.
- 일반 어학 앱은 여행 중 실제로 볼 가능성이 높은 표현의 우선순위가 낮다.
- 여행 전후 사용자는 긴 강의보다 짧고 시각적인 학습을 선호한다.

제품은 **사진 관찰 → 소리 내어 읽기 → 정답 확인 → 발음 듣기 → 자기평가 → 복습**의 짧은 루프를 제공한다.

### 2.3 제품 가치

- **현장성:** 관광객이 많이 찾는 장소의 간판과 장소명을 이미지 맥락으로 익힌다.
- **즉시성:** 앱 실행 후 최소 조작으로 2~8분 학습을 시작한다.
- **기억 강화:** 정답을 본 뒤 기억 여부를 스스로 판정하고 다시 만난다.
- **낮은 부담:** 게스트로 시작하며 핵심 콘텐츠와 음성은 오프라인에서도 작동한다.
- **명확한 피드백:** 실패라는 표현 대신 `Review`와 `Got it`을 사용한다.

## 3. 목표 사용자

### 3.1 1차 사용자: 여행 준비자

- 한국 방문을 준비 중인 18세 이상 외국인
- 한글 자모와 기본 음가를 알고 천천히 읽을 수 있음
- 서울·부산·전주·제주·경주 등 인기 여행지에 관심이 있음
- 하루 3~5분 동안 실제 장소명을 익히고 싶음
- 앱 인터페이스로 기본 영어를 사용할 수 있음

### 3.2 2차 사용자: 단기 방문객

- 한국에 도착했거나 여행을 마친 사용자
- 거리에서 본 장소명과 표현을 다시 알아보고 싶음
- 데이터 연결이 불안정한 이동 중에도 학습하고 싶음

### 3.3 제외 사용자

- 한글 자모부터 배워야 하는 완전 입문자
- TOPIK 고급 문법·작문·듣기 평가가 필요한 사용자
- 실시간 카메라 번역이나 길찾기가 주목적인 사용자
- 자유 대화, 발음 채점, 교사 피드백이 필요한 사용자

## 4. 출시 목표와 성공 기준

### 4.1 Android 1차 출시 목표

1. 첫 사용자가 설명 없이 온보딩을 마치고 첫 세션을 완료한다.
2. 사용자가 사진 속 한글을 보고 정답과 단어 구성을 이해한다.
3. 정답 확인 후 왼쪽 `Got it`, 오른쪽 `Review`로 카드를 분류한다.
4. Typecast 은경과 Gemini Flash Kore 중 발음 음성을 선택한다.
5. 20개 기본 카드는 이미지와 두 음성 세트를 포함해 오프라인에서 동작한다.
6. 세션 중단, 프로세스 종료, 기기 회전 후에도 학습 위치를 복구한다.
7. 진행 화면에서 학습·숙련·복습 상태를 확인한다.

### 4.2 제품 지표

| 지표 | 정의 | 1차 목표 |
|---|---|---|
| 온보딩 완료율 | 시작 대비 관심 분야 완료 | 75% 이상 |
| 첫 세션 시작률 | 온보딩 완료 대비 세션 시작 | 70% 이상 |
| 세션 완료율 | 시작된 세션 중 마지막 판정 완료 | 65% 이상 |
| 정답 확인율 | 노출 카드 중 정답을 연 카드 | 95% 이상 |
| 7일 재방문율 | 설치 후 7일 이내 재사용 | 20% 이상 |
| 오디오 사용률 | 정답 카드 중 오디오 재생 | 25% 이상 |
| 비정상 종료 없는 사용자 비율 | Android vitals 기준 | 99.5% 이상 |

지표 수집 동의를 받지 않은 사용자의 학습 데이터는 외부 분석 시스템으로 전송하지 않는다.

### 4.3 비목표

- iOS 앱
- 실시간 카메라 촬영과 OCR
- 자유 문장 번역
- 발음 녹음·채점
- 지도, 위치 권한, 주변 장소 추천
- 사용자 사진 업로드와 공개 피드
- 소셜 기능, 랭킹, 광고, 결제, 구독
- 운영자가 앱 밖에서 콘텐츠를 편집하는 CMS
- 1차 출시의 서버 계정·다중 기기 동기화

## 5. 확정 제품 결정

| 항목 | 확정값 |
|---|---|
| 제품명 | Sign Korean |
| 플랫폼 | Android 전용 |
| UI 언어 | 영어 |
| 학습 언어 | 한국어 |
| 계정 | 게스트·기기 로컬 저장 우선 |
| 기본 콘텐츠 | 인기 여행지 간판 20장 |
| 기본 세션 길이 | 20장 |
| 선택 가능한 세션 길이 | 5장, 10장, 20장 |
| 카드 판정 방향 | 왼쪽 `Got it`, 오른쪽 `Review` |
| 판정 방식 | 정답 확인 후 스와이프 전용 |
| 화면 판정 버튼 | 제공하지 않음 |
| 접근성 대체 판정 | TalkBack 사용자 지정 액션과 키보드 화살표 |
| 로마자 표기 | 기본 꺼짐 |
| 자동 음성 재생 | 기본 꺼짐 |
| 기본 음성 | Typecast · 은경 |
| 선택 음성 | Typecast · 은경 / Gemini Flash · Kore |
| 음성 제공 방식 | 앱에 내려받아 둔 파일 재생, 런타임 TTS API 호출 없음 |
| 알림 | 기본 꺼짐, 복습 카드가 있을 때만 지정 시간 알림 |
| 핵심 사용 모드 | 오프라인 우선 |

## 6. 제품 원칙

1. 한 카드에는 한 개의 대표 표현만 학습시킨다.
2. 카드 앞면에는 정답 한국어, 영어 뜻, 로마자 표기를 노출하지 않는다.
3. 정답 확인 전에는 좌우 판정이 완료되지 않는다.
4. 스와이프 방향은 모든 화면과 접근성 액션에서 바뀌지 않는다.
5. 사진을 가리는 피드백, 토스트, 판정 버튼을 두지 않는다.
6. 정답은 사진 아래의 독립 영역에 표시해 사진을 덮지 않는다.
7. 현재 카드 뒤에 다음 두 카드가 쌓여 있음을 보여준다.
8. 장식적 게임 감성은 유지하되 텍스트 가독성·터치 영역·명암을 우선한다.
9. 학습 핵심 기능은 계정과 네트워크 없이 작동한다.
10. 앱에는 TTS 제공자의 API 키를 포함하지 않는다.

## 7. 정보 구조와 내비게이션

### 7.1 최상위 구조

앱의 하단 내비게이션은 세 탭으로 고정한다.

| 탭 | 목적 | 주요 기능 |
|---|---|---|
| Learn | 학습 진입 | 오늘의 세션, 카테고리, 주간 활동, 중단 세션 재개 |
| Progress | 결과 확인 | Learned, Mastered, Due now, 카테고리별 진척, 복습 카드 |
| Settings | 환경 설정 | 세션 길이, 표기, 자동 재생, 음성, 알림, 계정 안내, 접근성, 정책 |

학습 세션과 완료 화면에서는 하단 내비게이션을 숨긴다.

### 7.2 화면 목록

| 화면 ID | 화면명 | 프로토타입 대응 | 출시 요구 수준 |
|---|---|---|---|
| APP-00 | Launch | 초기 상태 판별 | 필수 |
| ONB-01 | Welcome | `screen-welcome` | 필수 |
| ONB-02 | How it works | `screen-tutorial` | 필수 |
| ONB-03 | Your Korean | `screen-level` | 필수 |
| ONB-04 | Your interests | `screen-interests` | 필수 |
| HOME-01 | Learn home | `screen-home` | 필수 |
| LEARN-01 | Card question | `screen-learn` question | 필수 |
| LEARN-02 | Card answer | `screen-learn` answer | 필수 |
| LEARN-03 | Image viewer | `imageViewer` | 필수 |
| LEARN-04 | Leave session | `genericSheet` | 필수 |
| REPORT-01 | Report card | `reportSheet` | 필수 |
| DONE-01 | Session complete | `screen-done` | 필수 |
| PROG-01 | Progress | `screen-progress` | 필수 |
| PROG-02 | Card detail | Progress의 bottom sheet | 필수 |
| SET-01 | Settings | `screen-settings` | 필수 |
| SET-02 | Session length | 설정 bottom sheet | 필수 |
| SET-03 | Pronunciation voice | 설정 bottom sheet | 필수 |
| SET-04 | Review reminder | 설정 bottom sheet | 필수 |
| ACC-01 | Guest account info | 설정 bottom sheet | 필수 |
| LEGAL-01 | Privacy / Terms | 설정의 정책 항목 | 필수 |

## 8. 핵심 사용자 여정

### 8.1 첫 이용

1. 앱이 로컬 설정에서 온보딩 완료 여부를 확인한다.
2. Welcome에서 실제 거리 한국어 학습 가치를 확인한다.
3. 튜토리얼 카드에서 `Show answer`를 누르고 판정 개념을 익힌다.
4. 한글 수준을 하나 선택하거나 건너뛴다.
5. 관심 분야를 복수 선택하거나 선택 없이 진행한다.
6. Learn 홈에서 기본 20장 세션을 시작한다.
7. 사진을 관찰하고 소리 내어 읽은 뒤 정답을 연다.
8. 필요하면 발음을 듣고 왼쪽 `Got it` 또는 오른쪽 `Review`로 스와이프한다.
9. 완료 화면에서 결과와 다음 복습 수를 확인한다.

### 8.2 재방문

1. 온보딩을 완료한 사용자는 Learn 홈으로 이동한다.
2. 중단 세션이 있으면 `Resume session`과 완료 위치를 표시한다.
3. 중단 세션이 없으면 설정된 길이로 새 세션을 시작한다.
4. 복습 예정 카드와 새 카드를 섞어 제시한다.
5. Progress에서 결과와 복습 목록을 확인한다.

### 8.3 판정 실수 복구

1. 판정 직후 사진 아래와 조작 영역 사이에 결과 토스트가 나타난다.
2. 사용자는 4초 안에 `Undo`를 누른다.
3. 직전 카드가 정답 공개 상태로 복구된다.
4. 직전 결과와 복습 상태가 취소된다.
5. 다음 판정이 발생하면 그 이전 판정은 화면에서 되돌릴 수 없다.

### 8.4 음성 변경

1. Settings에서 `Pronunciation voice`를 연다.
2. `Typecast · 은경` 또는 `Gemini Flash · Kore`를 선택한다.
3. 선택 시 시트가 닫히고 선택 확인 토스트가 표시된다.
4. 이후 수동 재생과 자동 재생은 선택한 음성 세트를 사용한다.
5. 앱을 다시 실행해도 선택값을 유지한다.

## 9. 온보딩 요구사항

| ID | 우선순위 | 요구사항 |
|---|---|---|
| FR-ONB-001 | Must | 첫 실행 시 4단계 온보딩을 표시한다. |
| FR-ONB-002 | Must | Welcome은 실제 거리·상점·역의 한국어를 익힌다는 가치를 영어로 설명한다. |
| FR-ONB-003 | Must | 계정 없이 사용할 수 있음을 Welcome에서 알린다. |
| FR-ONB-004 | Must | 튜토리얼은 정답 확인 전과 후를 구분한다. |
| FR-ONB-005 | Must | 튜토리얼 판정은 학습 기록에 포함하지 않는다. |
| FR-ONB-006 | Must | 수준은 Beginner, Elementary, Not sure 중 하나를 선택한다. |
| FR-ONB-007 | Must | 수준 건너뛰기의 저장값은 Beginner다. |
| FR-ONB-008 | Must | 관심 분야는 Food, Cafés, Transport, Shopping, Places, Notices를 복수 선택할 수 있다. |
| FR-ONB-009 | Must | 관심 분야를 선택하지 않아도 완료할 수 있다. |
| FR-ONB-010 | Must | 완료 상태를 먼저 저장한 뒤 Learn 홈으로 이동한다. |

## 10. 홈과 세션 요구사항

| ID | 우선순위 | 요구사항 |
|---|---|---|
| FR-SES-001 | Must | 홈은 세션 카드 수와 예상 시간을 표시한다. |
| FR-SES-002 | Must | 예상 시간은 5장 약 2분, 10장 약 4분, 20장 약 8분으로 표시한다. |
| FR-SES-003 | Must | 기본 세션 길이는 20장이다. |
| FR-SES-004 | Must | 사용자는 설정에서 5, 10, 20장 중 하나를 선택한다. |
| FR-SES-005 | Must | 홈은 Landmarks, Neighborhoods, Markets, Transport 카테고리 진입을 제공한다. |
| FR-SES-006 | Must | 학습 중 화면에는 현재 순서와 전체 수, 진행 막대를 표시한다. |
| FR-SES-007 | Must | 중단 세션은 완료 위치와 함께 홈에서 재개할 수 있다. |
| FR-SES-008 | Must | 세션을 나가려 할 때 유지 여부를 bottom sheet로 확인한다. |
| FR-SES-009 | Must | 마지막 카드 판정 후 완료 화면으로 이동한다. |
| FR-SES-010 | Must | 세션 완료 화면은 전체, Got it, Review, 다음 복습을 표시한다. |

## 11. 카드 학습 요구사항

### 11.1 카드 스택과 이미지

- 활성 카드 뒤에 다음 카드와 다다음 카드의 사진을 각각 다른 오프셋으로 표시한다.
- 활성 카드를 움직이면 다음 카드가 확대·밝아지고 다다음 카드가 한 단계 앞으로 이동하는 느낌을 준다.
- 카드의 사진 영역과 정보 영역은 함께 이동한다.
- 카드 가로 너비는 학습 스테이지의 사용 가능 너비를 채운다.
- 사진은 비율을 유지하는 `ContentScale.Fit` 방식으로 표시하고, 남는 영역은 같은 사진의 블러 처리된 확대 배경으로 채운다.
- 간판 텍스트를 포함한 원본 사진의 좌우를 잘라내지 않는다.
- 학습 대상 간판에는 콘텐츠 데이터의 백분율 좌표로 포커스 프레임을 표시한다.
- Android 글꼴 크기 확대 시 사진 영역 최소 높이를 지키고 정보 영역만 스크롤한다.

### 11.2 질문 상태

질문 상태는 다음 요소를 표시한다.

- 간판 사진과 포커스 프레임
- 이미지 확대 액션
- 콘텐츠 카테고리와 `TRAVEL` 보조 칩
- `What does this sign say?`
- `Say it out loud, then check the answer.`
- 하단 `Show answer`

정답, 번역, 단어 풀이, 로마자 표기, 발음 버튼은 숨긴다.

### 11.3 정답 상태

`Show answer`를 누르면 같은 카드에서 사진을 유지하고, 사진 아래 정보 영역을 다음 내용으로 교체한다.

- 방향 안내: `← Got it · Review →`
- 한국어 표기
- 선택한 음성의 재생 버튼
- 영어 의미
- 설정이 켜진 경우 로마자 표기
- 단어·형태소 단위 풀이
- `Street note`

정답 영역은 카드 하단 38%를 기준으로 배치하고 내부 스크롤을 허용한다. 사진 영역은 상단 62%를 유지한다. 정답 시트나 토스트가 사진을 덮어서는 안 된다.

### 11.4 스와이프 판정

| 방향 | 결과 | 시각 피드백 | 카드 퇴장 |
|---|---|---|---|
| 왼쪽 | Got it | 카드 오른쪽에 `GOT IT` | 왼쪽 |
| 오른쪽 | Review | 카드 왼쪽에 `REVIEW` | 오른쪽 |

세부 규칙은 다음과 같다.

1. 12dp 이상 이동한 뒤 가로 이동량이 세로 이동량의 1.2배를 넘으면 가로 드래그로 잠근다.
2. 정답 전 가로 이동은 ±30dp로 제한한다.
3. 정답 전 28dp를 넘겨 놓으면 중앙 복귀 후 `Check the answer first`를 표시한다.
4. 정답 후 가로 이동이 88dp를 넘으면 판정한다.
5. 임계값 미만이면 스프링 애니메이션으로 중앙에 복귀한다.
6. 포인터가 학습 스테이지 또는 실제 기기 창 밖으로 나가면 마지막 기기 경계 좌표까지만 반영하고 드래그를 종료한다.
7. 앱은 포인터가 기기 밖으로 나간 뒤 계속 따라가지 않는다.
8. 정답 상태에 화면 판정 버튼을 표시하지 않는다.
9. 물리 키보드의 왼쪽 화살표는 Got it, 오른쪽 화살표는 Review로 동작한다.
10. TalkBack 사용자는 카드의 사용자 지정 접근성 액션 `Got it`과 `Review`를 사용할 수 있다.

### 11.5 판정 후 피드백

- 카드가 판정 방향으로 화면 밖으로 이동한다.
- 다음 카드가 활성 위치로 이동한다.
- 토스트는 사진을 가리지 않도록 학습 조작 영역 바로 위에 표시한다.
- 토스트 문구는 `Marked as Got it` 또는 `Marked as Review`다.
- 토스트에는 `Undo`가 있으며 4초 후 자동으로 사라진다.

### 11.6 이미지 확대

- 질문과 정답 상태 모두에서 전체 화면 이미지 뷰어를 열 수 있다.
- 질문 상태에서는 정답 라벨을 숨긴다.
- 정답 상태에서는 하단에 한국어 라벨을 표시한다.
- Android 뒤로가기는 뷰어만 닫고 카드 상태를 보존한다.
- 핀치 확대와 두 번 탭 확대를 지원하며 최소 배율로 복귀할 수 있다.

### 11.7 카드 신고

신고 사유는 다음 다섯 가지다.

1. Korean text is wrong
2. English meaning is wrong
3. Audio is wrong
4. Image is inappropriate or private
5. Something else

한 사유를 선택해야 제출할 수 있다. 상세 내용은 선택 사항이며 최대 500자다. 개인 정보를 입력하지 말라는 안내를 표시한다. 1차 출시에서 네트워크 신고 수집을 운영하지 않으면 로컬 큐에 저장하고 네트워크 기능 도입 시 전송한다.

## 12. 음성 요구사항

### 12.1 음성 세트

| ID | 표시명 | 제공자·음성 | 형식 | 파일 수 |
|---|---|---|---|---|
| typecast | Typecast · 은경 | Typecast `은경`, `ssfm-v30` | MP3 | 20 |
| gemini | Gemini Flash · Kore | Google Gemini Flash TTS `Kore` | WAV 24kHz mono 16-bit | 20 |

두 세트는 각 카드 ID와 일대일로 대응한다. 런타임에 외부 TTS API를 호출하지 않으며 생성용 API 키를 APK, 리소스, BuildConfig, 로그에 포함하지 않는다.

### 12.2 재생 규칙

- 기본 선택은 Typecast · 은경이다.
- 사용자가 선택한 음성 ID는 DataStore에 저장한다.
- 새 음성을 재생하면 기존 재생을 중지하고 처음부터 재생한다.
- 정답 상태에서만 수동 재생 버튼을 제공한다.
- 자동 재생이 켜져 있으면 정답 상태가 열린 직후 한 번 재생한다.
- 자동 재생 실패가 정답 표시와 카드 판정을 막아서는 안 된다.
- 파일 누락·손상 시 선택한 제공자 이름을 포함한 비차단 오류를 표시한다.
- 오디오 포커스를 얻지 못하면 `Tap the audio button again to play.`를 표시한다.
- 앱이 백그라운드로 이동하거나 화면이 닫히면 재생을 중지한다.

## 13. 진행·복습 요구사항

### 13.1 진행 화면

Progress는 다음 정보를 표시한다.

- `Learned`: 한 번 이상 판정한 고유 카드 수
- `Mastered`: 숙련 기준을 충족한 카드 수
- `Due now`: 현재 복습 예정 카드 수
- 최근 7일 학습량
- 카테고리별 Learned 수와 진행 막대
- 최근 Review 카드 최대 3개

복습 카드를 누르면 사진, 한국어, 영어 의미, Street note와 `Practice this sign`을 포함한 bottom sheet를 연다.

### 13.2 복습 규칙

1. 첫 `Review`는 다음 날 복습으로 예약한다.
2. 첫 `Got it`은 3일 뒤 복습으로 예약한다.
3. Got it 연속 성공은 7일, 21일, 60일, 90일 순으로 간격을 늘린다.
4. Review는 숙련 단계를 0으로 되돌리고 다음 날로 예약한다.
5. 90일 단계 성공은 Mastered로 표시한다.
6. 기한을 넘긴 카드와 Review 횟수가 많은 카드를 우선한다.
7. 기본 세션은 Due 카드 최대 30%와 새 카드로 구성하되, Due가 많으면 70%까지 높인다.
8. 같은 세션에서 Review한 카드는 남은 카드가 충분할 때 3~5장 뒤에 한 번 다시 삽입한다.

## 14. 설정 요구사항

| 설정 | 값 | 기본값 | 저장 범위 |
|---|---|---|---|
| Session length | 5 / 10 / 20 | 20 | DataStore |
| Show romanization | On / Off | Off | DataStore |
| Auto-play audio | On / Off | Off | DataStore |
| Pronunciation voice | typecast / gemini | typecast | DataStore |
| Review reminder | Off / 7:00 PM | Off | DataStore + WorkManager |
| Reduce in-app motion | On / Off | 시스템 설정을 초기값으로 사용 | DataStore |

### 14.1 알림

- 알림은 사용자가 켤 때 Android 알림 권한을 요청한다.
- 권한 요청은 설정 토글을 켜는 사용자 행동 직후에만 표시한다.
- Due 카드가 1개 이상일 때만 로컬 알림을 예약한다.
- 기본 시간은 현지 시각 오후 7시다.
- 시간대가 바뀌면 다음 실행 또는 예약 작업에서 현지 시각으로 재예약한다.
- 알림을 끄면 예약 작업을 취소한다.

### 14.2 계정과 정책

- 1차 출시는 게스트 로컬 저장 방식이다.
- Account & sync는 로컬 저장 범위를 명확히 알린다.
- 계정 연결 기능은 서버 동기화가 제공되기 전까지 출시 빌드에서 활성화하지 않는다.
- Privacy policy와 Terms of use는 앱 내 WebView가 아닌 Custom Tab 또는 네이티브 텍스트 화면으로 연다.
- 정책 화면은 오프라인 시 마지막 번들 버전을 제공한다.

## 15. 기본 콘텐츠 요구사항

### 15.1 20개 카드 목록

| 순서 | 한국어 | 영어 의미 | 카테고리 |
|---:|---|---|---|
| 1 | 홍대입구 | Hongdae Entrance | NEIGHBORHOOD |
| 2 | 경복궁 | Gyeongbokgung Palace | LANDMARK |
| 3 | 북촌한옥마을 | Bukchon Hanok Village | NEIGHBORHOOD |
| 4 | 남산서울타워 | N Seoul Tower | LANDMARK |
| 5 | 명동 | Myeongdong | NEIGHBORHOOD |
| 6 | 광장시장 | Gwangjang Market | MARKET |
| 7 | 남대문시장 | Namdaemun Market | MARKET |
| 8 | 이태원 | Itaewon | NEIGHBORHOOD |
| 9 | 한강공원 | Hangang Park | LANDMARK |
| 10 | 성수동 | Seongsu-dong | NEIGHBORHOOD |
| 11 | 인사동 | Insadong | NEIGHBORHOOD |
| 12 | 익선동 | Ikseon-dong | NEIGHBORHOOD |
| 13 | 동대문디자인플라자 | Dongdaemun Design Plaza | LANDMARK |
| 14 | 해운대 | Haeundae | LANDMARK |
| 15 | 감천문화마을 | Gamcheon Culture Village | NEIGHBORHOOD |
| 16 | 전주한옥마을 | Jeonju Hanok Village | NEIGHBORHOOD |
| 17 | 제주올레길 | Jeju Olle Trail | LANDMARK |
| 18 | 불국사 | Bulguksa Temple | LANDMARK |
| 19 | 부산역 | Busan Station | TRANSPORT |
| 20 | 서울역 | Seoul Station | TRANSPORT |

### 15.2 카드 데이터 필수값

| 필드 | 형식 | 규칙 |
|---|---|---|
| id | String | 영구 고유값, 파일명 기준 slug |
| order | Int | 1부터 시작하는 기본 노출 순서 |
| korean | String | 간판에 보이는 정확한 표기 |
| romanization | String | 설정이 켜졌을 때만 표시 |
| meaningEn | String | 간결하고 자연스러운 영어 의미 |
| category | Enum | LANDMARK, NEIGHBORHOOD, MARKET, TRANSPORT |
| imageAsset | Resource URI | 1개 카드당 고유 이미지 |
| focusX/Y/W/H | Float | 원본 이미지 기준 0~100% 좌표 |
| wordParts | List | 한국어 조각과 영어 풀이의 순서 목록 |
| streetNoteEn | String | 여행자 관점의 짧은 맥락 |
| typecastAudio | Resource URI | MP3 파일 |
| geminiAudio | Resource URI | WAV 파일 |
| contentVersion | Int | 콘텐츠 마이그레이션용 버전 |

### 15.3 이미지 정책

- 1차 콘텐츠는 현재 프로토타입의 생성 이미지 20장을 번들한다.
- 실제 장소를 학습시키는 목적에 맞게 사진형 거리 장면과 읽을 수 있는 간판을 사용한다.
- 로고만 있는 그래픽, 흰 배경의 단어 카드, 맥락 없는 텍스트 합성 이미지는 사용하지 않는다.
- 실제 인물을 식별할 수 있는 얼굴, 차량 번호, 개인 주소는 노출하지 않는다.
- 이미지 생성 또는 촬영 출처와 상업적 사용 권한을 콘텐츠 메타데이터에 보관한다.
- 한국어 철자와 장소 설명은 한국어 검수자가 확인한다.

## 16. 상태 모델

### 16.1 앱 상태

```text
ColdStart
├─ onboardingCompleted = false → Welcome
└─ onboardingCompleted = true  → LearnHome
```

### 16.2 카드 상태

```text
Question
├─ Show answer → Answer
├─ Zoom → ImageViewer(question)
└─ Swipe attempt → Question + 안내 토스트

Answer
├─ Play audio → Answer
├─ Zoom → ImageViewer(answer)
├─ Swipe left → GotIt → NextCard
└─ Swipe right → Review → NextCard
```

### 16.3 세션 상태

```text
Idle → Preparing → Active.Question ↔ Active.Answer
Active → Paused → Active
Active.LastRated → Completed
Active → RecoverableError → Active 또는 Home
```

상태 변경은 Room 트랜잭션으로 저장한다. UI 애니메이션 완료 여부와 데이터 저장 성공 여부를 분리하며, 데이터 저장이 먼저 성공한 뒤 다음 카드를 확정한다.

## 17. Android 기술 요구사항

### 17.1 기술 스택

| 영역 | 기준 |
|---|---|
| 언어 | Kotlin |
| UI | Jetpack Compose, Material 3 기반 커스텀 디자인 시스템 |
| 아키텍처 | 단일 Activity, 단방향 데이터 흐름, ViewModel + StateFlow |
| 내비게이션 | Navigation Compose |
| 구조화 데이터 | Room |
| 사용자 설정 | Preferences DataStore |
| 이미지 | 번들 리소스 + Coil |
| 오디오 | AndroidX Media3 |
| 알림 | WorkManager + NotificationManager |
| 의존성 주입 | Hilt |
| 직렬화 | Kotlinx Serialization |
| 단위 테스트 | JUnit + kotlinx-coroutines-test |
| UI 테스트 | Compose UI Test + Android instrumentation |

### 17.2 지원 범위

- 최소 지원 버전은 Android 8.0(API 26)이다.
- compileSdk와 targetSdk는 개발 시점의 최신 안정 Android SDK와 Google Play 제출 요구사항을 따른다.
- 기준 디자인 폭은 390dp, 기준 높이는 844dp다.
- 360dp 폭에서도 핵심 문구와 컨트롤이 잘리지 않아야 한다.
- 큰 화면에서는 콘텐츠를 중앙에 두고 학습 카드 폭을 480dp 이하로 제한한다.
- 시스템 상태·내비게이션 바 인셋과 디스플레이 컷아웃을 적용한다.
- 앱의 기본 방향은 세로이며, 구성 변경이 발생해도 현재 화면과 세션 상태를 복구한다.

### 17.3 모듈 권장 구조

```text
app
core:model
core:data
core:database
core:designsystem
core:media
feature:onboarding
feature:home
feature:learn
feature:progress
feature:settings
feature:legal
```

### 17.4 저장소 원칙

- `CardRepository`는 번들 콘텐츠와 학습 상태를 결합한다.
- `SessionRepository`는 세션 생성, 판정, Undo, 복구를 트랜잭션으로 처리한다.
- `SettingsRepository`는 DataStore의 설정을 `StateFlow`로 제공한다.
- UI는 리소스 경로나 데이터베이스 구현을 직접 알지 않는다.
- 콘텐츠 번들 버전이 올라가면 카드 ID를 유지하며 Room 상태를 마이그레이션한다.

## 18. 데이터 모델

### 18.1 Card

```kotlin
data class Card(
    val id: String,
    val order: Int,
    val korean: String,
    val romanization: String,
    val meaningEn: String,
    val category: Category,
    val imageAsset: String,
    val focus: FocusRect,
    val wordParts: List<WordPart>,
    val streetNoteEn: String,
    val typecastAudioAsset: String,
    val geminiAudioAsset: String,
    val contentVersion: Int
)
```

### 18.2 ReviewState

```kotlin
data class ReviewState(
    val cardId: String,
    val stage: Int,
    val lastRating: Rating?,
    val lastReviewedAt: Instant?,
    val nextReviewAt: Instant?,
    val reviewCount: Int,
    val gotItCount: Int,
    val mastered: Boolean
)
```

### 18.3 Session

```kotlin
data class Session(
    val id: String,
    val cardIds: List<String>,
    val currentIndex: Int,
    val ratings: List<SessionRating>,
    val status: SessionStatus,
    val startedAt: Instant,
    val updatedAt: Instant
)
```

### 18.4 UserSettings

```kotlin
data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val level: Level = Level.BEGINNER,
    val interests: Set<Interest> = emptySet(),
    val sessionLength: Int = 20,
    val showRomanization: Boolean = false,
    val autoPlayAudio: Boolean = false,
    val audioVoice: AudioVoice = AudioVoice.TYPECAST_EUNKYUNG,
    val reduceMotion: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderLocalTime: LocalTime = LocalTime.of(19, 0)
)
```

## 19. 비기능 요구사항

### 19.1 성능

| ID | 요구사항 |
|---|---|
| NFR-PERF-001 | 일반적인 중급 Android 기기에서 콜드 스타트 후 2초 이내 첫 화면을 표시한다. |
| NFR-PERF-002 | 번들 카드 전환은 입력 후 100ms 이내 시각 반응을 시작한다. |
| NFR-PERF-003 | 스와이프 중 목표 프레임 속도는 60fps이며 저사양 기기에서도 45fps 아래로 연속 하락하지 않는다. |
| NFR-PERF-004 | 다음 두 카드 이미지를 미리 디코딩하되 전체 원본 20장을 동시에 메모리에 올리지 않는다. |
| NFR-PERF-005 | 오디오 재생 요청 후 300ms 이내 시작을 목표로 한다. |

### 19.2 오프라인과 복구

| ID | 요구사항 |
|---|---|
| NFR-OFF-001 | 번들 20장 학습, 음성 재생, 진행 저장, 설정은 네트워크 없이 동작한다. |
| NFR-OFF-002 | 카드 판정 직후 로컬 저장을 완료한다. |
| NFR-OFF-003 | 프로세스 종료 후 마지막 미완료 세션과 현재 인덱스를 복원한다. |
| NFR-OFF-004 | 손상된 파일 하나가 앱 전체 실행을 막지 않는다. |

### 19.3 접근성

| ID | 요구사항 |
|---|---|
| NFR-A11Y-001 | 모든 터치 대상은 최소 48dp다. |
| NFR-A11Y-002 | 일반 텍스트와 배경 명암비는 4.5:1 이상이다. |
| NFR-A11Y-003 | TalkBack 읽기 순서는 상단 상태, 사진 설명, 질문·정답, 액션 순이다. |
| NFR-A11Y-004 | 카드에는 `Got it`과 `Review` 사용자 지정 접근성 액션을 제공한다. |
| NFR-A11Y-005 | 스와이프 방향은 색상만으로 전달하지 않고 텍스트 오버레이와 햅틱을 함께 사용한다. |
| NFR-A11Y-006 | 시스템 `Remove animations`와 앱의 Reduce motion을 모두 존중한다. |
| NFR-A11Y-007 | 글꼴 배율 200%에서 핵심 기능을 사용할 수 있다. |
| NFR-A11Y-008 | 오디오 없이도 모든 학습 정보를 읽을 수 있다. |

### 19.4 보안과 개인정보

| ID | 요구사항 |
|---|---|
| NFR-SEC-001 | TTS API 키와 생성용 비밀값을 저장소, APK, 로그에 포함하지 않는다. |
| NFR-SEC-002 | 1차 출시에서 이메일, 위치, 연락처, 마이크, 카메라 권한을 요청하지 않는다. |
| NFR-SEC-003 | 학습 기록은 앱 전용 저장소에 보관한다. |
| NFR-SEC-004 | 신고 상세는 개인 정보 입력 금지 안내와 500자 제한을 적용한다. |
| NFR-SEC-005 | 백업 허용 여부는 개인정보 처리방침과 일치시킨다. |

### 19.5 품질과 관측 가능성

- 릴리스 빌드는 StrictMode 위반, ANR, 크래시를 출시 차단 기준으로 검토한다.
- 오류 로그에 카드 학습 문맥을 기록할 때 사용자 자유 입력과 식별자를 제외한다.
- 분석 이벤트는 익명 세션 ID와 카드 ID, 상태값만 사용한다.
- 음성 재생 실패는 제공자 ID와 카드 ID를 기록하되 API 키와 파일 전체 경로는 기록하지 않는다.

## 20. 분석 이벤트

| 이벤트 | 필수 속성 |
|---|---|
| app_opened | onboarding_completed |
| onboarding_started | 없음 |
| onboarding_completed | level, interest_count |
| session_started | session_length, due_count, new_count, category |
| answer_revealed | card_id, position |
| audio_played | card_id, voice_id, auto_play |
| card_rated | card_id, rating, position, gesture_or_accessibility |
| rating_undone | card_id, previous_rating |
| session_paused | position, session_length |
| session_resumed | position, session_length |
| session_completed | got_it_count, review_count, duration_ms |
| voice_changed | previous_voice, selected_voice |
| reminder_changed | enabled |
| card_report_saved | card_id, reason |

## 21. 오류 처리

| 상황 | 사용자 처리 | 데이터 처리 |
|---|---|---|
| 이미지 로드 실패 | 중립 배경과 `Photo unavailable` 표시, 정답 학습 허용 | 카드 ID와 오류 기록 |
| 선택 음성 파일 실패 | 비차단 토스트 표시, 재시도 허용 | 제공자·카드 ID 기록 |
| Room 저장 실패 | 다음 카드 진행을 멈추고 Retry 제공 | 트랜잭션 롤백 |
| 중단 세션 데이터 불일치 | 유효한 판정까지만 복구하고 안내 | 손상 레코드 격리 |
| 알림 권한 거부 | 설정값을 Off로 되돌리고 시스템 설정 경로 안내 | 예약 없음 |
| 정책 페이지 연결 실패 | 번들 정책 문서 표시 | 오류 로그만 기록 |

오류 문구는 문제와 다음 행동을 짧게 설명하며 사용자의 잘못으로 표현하지 않는다.

## 22. 수용 기준

| ID | 조건 | 기대 결과 |
|---|---|---|
| AC-001 | 신규 설치 실행 | Welcome을 표시한다. |
| AC-002 | 온보딩 완료 후 재실행 | Learn 홈을 표시한다. |
| AC-003 | 수준을 건너뜀 | Beginner로 저장한다. |
| AC-004 | 관심 분야 미선택 완료 | 홈 진입이 가능하다. |
| AC-005 | 기본 설정 확인 | 세션 길이는 20, 로마자와 자동 재생은 Off, 음성은 Typecast다. |
| AC-006 | 질문 상태에서 크게 스와이프 | 판정하지 않고 중앙 복귀와 안내를 표시한다. |
| AC-007 | 정답 상태에서 왼쪽 88dp 초과 | Got it으로 저장하고 카드가 왼쪽으로 퇴장한다. |
| AC-008 | 정답 상태에서 오른쪽 88dp 초과 | Review로 저장하고 카드가 오른쪽으로 퇴장한다. |
| AC-009 | 임계값 미만에서 손을 놓음 | 카드가 중앙으로 복귀한다. |
| AC-010 | 포인터가 기기 프레임을 벗어남 | 카드가 경계 밖 포인터를 계속 따라가지 않는다. |
| AC-011 | 정답을 열음 | 사진은 유지되고 정답은 사진 아래에 나타난다. |
| AC-012 | 카드 판정 직후 Undo | 직전 카드와 정답 상태, 학습 상태를 복원한다. |
| AC-013 | 20번째 카드 판정 | 완료 화면에 정확한 Got it과 Review 합계를 표시한다. |
| AC-014 | 세션 중 나가기 | 완료 위치를 저장하고 홈에 Resume을 표시한다. |
| AC-015 | Typecast 선택 후 재생 | 대응 MP3를 재생한다. |
| AC-016 | Gemini 선택 후 재생 | 대응 WAV를 재생한다. |
| AC-017 | 음성 선택 후 앱 재실행 | 선택한 음성을 유지한다. |
| AC-018 | 자동 재생 On에서 정답 열기 | 선택 음성을 한 번 재생한다. |
| AC-019 | 로마자 표기 On | 정답 상태에만 로마자를 표시한다. |
| AC-020 | TalkBack으로 카드 접근 | Got it과 Review 사용자 지정 액션을 사용할 수 있다. |
| AC-021 | 오프라인 상태 | 20개 카드의 사진·정답·두 음성·저장이 동작한다. |
| AC-022 | 알림을 켬 | 권한 승인 후 Due 카드가 있을 때 오후 7시 알림을 예약한다. |
| AC-023 | 신고 사유 미선택 | Send report가 비활성 상태다. |
| AC-024 | 이미지 뷰어를 닫음 | 진입 전 질문 또는 정답 상태를 보존한다. |

## 23. QA 범위

### 23.1 필수 자동 테스트

- 세션 구성과 5·10·20장 종료 조건
- 왼쪽 Got it / 오른쪽 Review 매핑
- 정답 전 판정 차단
- Undo 트랜잭션
- Review 간격 계산
- 프로세스 재생성 후 세션 복구
- DataStore 설정 저장과 기본값
- 20개 카드와 40개 음성 파일의 일대일 매핑
- 오디오 제공자 전환
- Room 마이그레이션

### 23.2 필수 기기·UI 테스트

- 360dp, 390dp, 480dp 폭
- API 26과 최신 안정 Android 버전
- 제스처 내비게이션과 3버튼 내비게이션
- 컷아웃, 큰 글자, 굵은 글자, 고대비, TalkBack
- 느린 기기에서 카드 스와이프와 이미지 디코딩
- 비행기 모드에서 전체 핵심 흐름
- 백그라운드·포그라운드 전환 중 오디오와 세션
- 기기 경계 밖 포인터 이동이 가능한 데스크톱 Android 환경

### 23.3 출시 차단 조건

- 좌우 방향이 화면 안내와 다름
- 정답 전 판정이 저장됨
- 정답 정보가 사진을 가림
- 기기 밖 포인터를 카드가 계속 따라감
- 판정 후 학습 기록 유실
- 음성 선택과 실제 재생 제공자가 다름
- API 키 또는 사용자 개인 정보가 로그·APK에 포함됨
- TalkBack으로 핵심 세션을 완료할 수 없음
- 오프라인에서 번들 콘텐츠를 열 수 없음

## 24. 개발 단계와 완료 정의

### 24.1 구현 순서

1. Android 프로젝트·디자인 시스템·내비게이션
2. 번들 콘텐츠 파서와 Room·DataStore
3. 온보딩과 Learn 홈
4. 카드 스택, 정답, 스와이프, Undo
5. 이미지 뷰어와 신고
6. 두 음성 세트와 자동 재생
7. 완료·Progress·복습 알고리즘
8. Settings·알림·정책
9. 접근성, 성능, 오프라인, 복구
10. 분석 동의, QA, 릴리스 준비

### 24.2 완료 정의

Android 1차 출시는 다음 조건을 모두 만족할 때 완료로 본다.

- 이 문서와 기능 명세의 Must 요구사항을 구현했다.
- 프로토타입과 같은 핵심 시각·인터랙션을 360~480dp에서 재현했다.
- 20개 카드, 20개 사진, Typecast 20개, Gemini 20개 파일이 검수됐다.
- 자동·수동 QA에서 출시 차단 결함이 없다.
- 비행기 모드에서 온보딩 이후 전체 학습 흐름을 완료한다.
- TalkBack만으로 정답 확인, 음성 재생, 판정, 완료가 가능하다.
- 개인정보 처리방침과 이용약관이 실제 배포 문서로 연결된다.
- 서명된 릴리스 APK 또는 AAB에 API 키가 없음을 검사했다.
- Android vitals와 오류 로깅 설정을 확인했다.

## 25. 요구사항 추적표

| 영역 | PRD·SRS | 기능 명세 | 프로토타입 |
|---|---|---|---|
| 온보딩 | 9장 | F-ONB-01~04 | welcome, tutorial, level, interests |
| 홈·세션 | 10장 | F-HOME-01, F-SES-01~02 | home, learn, done |
| 카드 학습 | 11장 | F-CARD-01~08 | learn card, image viewer, report |
| 음성 | 12장 | F-AUD-01~02 | playAudio, voiceSetting |
| 진행·복습 | 13장 | F-PROG-01~02 | progress, card detail sheet |
| 설정 | 14장 | F-SET-01~02, F-REM-01, F-ACC-01, F-LEGAL-01 | settings sheets |
| Android 구현 | 17~19장 | 11~14장 | 390×844 기준 반응형 UI |
| 인수·QA | 22~24장 | 15~16장 | 검증 스크립트와 수동 시나리오 |
