# Sign South Korea Ganpan Android 기능 명세서

| 항목 | 내용 |
|---|---|
| 문서 버전 | 2.0 Final |
| 문서 상태 | Android 화면 설계·구현·QA 기준 |
| 최종 갱신일 | 2026-10-17 |
| 기준 제품 문서 | [PRD-SRS.md](PRD-SRS.md) |
| 기준 인터랙션 | [../index.html](../index.html) |
| 대상 | UX·UI 디자이너, Android 개발자, 콘텐츠 편집자, QA |

## 1. 문서 목적

이 문서는 현재 HTML 프로토타입을 Android 앱으로 구현하기 위해 화면별 기능, 입력, 상태 변화, 저장, 오류, 접근성 동작과 인수 기준을 정의한다. 색상·서체·그림자·버튼 조형·이미지 배치·전환의 시각 기준은 프로토타입을 따른다. 기능 의미와 Android 네이티브 동작은 이 문서를 따른다.

프로토타입 바깥의 Interactive design file 패널, 화면 바로가기, Reset prototype은 개발·검수용 도구이며 Android 앱에 포함하지 않는다.

## 2. 고정 기능 기준

다음 사항은 디자인 과정에서 변경하지 않는다.

1. 앱은 Android 전용이다.
2. 기본 UI 언어는 영어다.
3. 간판 이미지는 학습 카드의 가로 폭을 채우며 원본의 핵심 글자를 자르지 않는다.
4. 활성 카드 뒤에 다음 두 사진 카드가 쌓여 보인다.
5. 정답 확인 이후에만 판정이 가능하다.
6. 왼쪽 스와이프는 Got it, 오른쪽 스와이프는 Review다.
7. 정답 상태에 Got it·Review 화면 버튼을 제공하지 않는다.
8. 정답 정보는 사진 아래 독립 영역에 표시하며 사진을 덮지 않는다.
9. 판정 토스트는 사진을 가리지 않는 위치에 표시한다.
10. 카드는 기기 프레임 밖의 포인터를 계속 따라가지 않는다.
11. 보이는 판정 버튼 대신 TalkBack 사용자 지정 액션과 물리 키보드 화살표를 접근성 대체 조작으로 제공한다.
12. 기본 세션 길이는 20장이며 5·10·20장을 선택할 수 있다.
13. Typecast 은경 발음을 재생한다.
14. 두 음성 세트는 앱에 저장된 파일을 재생하며 런타임 TTS API를 호출하지 않는다.
15. 앱은 핵심 학습 흐름을 오프라인에서 완료할 수 있다.

온보딩 튜토리얼의 방향 안내도 실제 학습과 동일하게 왼쪽 Got it, 오른쪽 Review여야 한다. 프로토타입의 일부 예시 컨트롤이 이 순서와 다르게 보이면 본 항목을 기준으로 수정한다.

## 3. 출시 범위

### 3.1 완전히 동작해야 하는 기능

- 첫 실행 판별과 4단계 온보딩
- Learn 홈과 하단 내비게이션
- 5·10·20장 세션 생성
- 중단 세션 저장·재개
- 질문·정답 카드 상태
- 세 장이 보이는 카드 스택
- 왼쪽 Got it / 오른쪽 Review 판정
- 정답 전 판정 차단
- Undo
- 이미지 확대
- Typecast 음성 재생
- 자동 음성 재생과 로마자 표기
- 완료 결과
- 진행 현황과 복습 카드 상세
- 카드 신고의 로컬 저장
- 복습 알림
- 게스트 저장 범위 안내
- 개인정보 처리방침과 이용약관
- TalkBack과 키보드 대체 판정

### 3.2 1차 출시에서 제공하지 않는 기능

- 계정 생성, 소셜 로그인, 클라우드 동기화
- 실시간 카메라와 OCR
- 위치 기반 추천
- 발음 녹음·평가
- 사용자 이미지 업로드
- 결제, 광고, 구독
- 앱 외부 콘텐츠 운영 도구

Connect account는 서버 동기화 출시 전까지 활성 액션으로 노출하지 않는다. Account 화면에는 학습 기록이 현재 기기에만 저장된다는 정보만 제공한다.

## 4. 사용자 상태와 기본값

| 상태 | 기본값 | 저장 |
|---|---|---|
| 온보딩 완료 | false | DataStore |
| 수준 | Beginner | DataStore |
| 관심 분야 | 빈 집합 | DataStore |
| 세션 길이 | 20 | DataStore |
| 로마자 표기 | Off | DataStore |
| 자동 음성 재생 | Off | DataStore |
| 발음 음성 | Typecast · 은경 | DataStore |
| 모션 감소 | 시스템 설정 반영 | DataStore |
| 복습 알림 | Off | DataStore |
| 알림 시각 | 19:00 현지 시각 | DataStore |
| 계정 | Guest | 로컬 고정 |
| 활성 세션 | 없음 | Room |

설정값은 프로세스 종료와 앱 업데이트 후에도 유지한다. 앱 데이터 삭제 또는 앱 제거 시 로컬 학습 기록과 설정은 삭제될 수 있음을 Account 화면에서 안내한다.

## 5. 핵심 용어

| 용어 | 정의 |
|---|---|
| Sign card | 사진, 표현, 의미, 단어 풀이, 맥락, 음성으로 구성된 학습 단위 |
| Question | 정답이 숨겨진 관찰 상태 |
| Answer | 정답을 확인했고 판정할 수 있는 상태 |
| Got it | 현재 표현을 이해했다고 사용자가 판정한 상태 |
| Review | 다시 학습할 필요가 있다고 사용자가 판정한 상태 |
| Due | 다음 복습 시각이 현재보다 이른 카드 |
| Mastered | 90일 단계까지 Got it을 완료한 카드 |
| Session | 연속 학습할 카드 목록과 현재 위치 |
| Undo | 방금 완료한 한 번의 판정을 되돌리는 동작 |
| Voice set | 모든 카드에 대응하는 한 제공자의 사전 생성 음성 파일 묶음 |

Wrong, Fail, Dislike는 사용자 노출 용어로 사용하지 않는다.

## 6. 내비게이션과 화면

### 6.1 라우트

| Route | 화면 |
|---|---|
| launch | 초기 상태 판별 |
| onboarding/welcome | Welcome |
| onboarding/tutorial | How it works |
| onboarding/level | Your Korean |
| onboarding/interests | Your interests |
| main/learn | Learn home |
| main/progress | Progress |
| main/settings | Settings |
| session/{sessionId} | 카드 세션 |
| session/{sessionId}/complete | 세션 완료 |
| legal/privacy | 개인정보 처리방침 |
| legal/terms | 이용약관 |

Bottom sheet와 전체 화면 이미지 뷰어는 상위 라우트의 상태로 관리한다.

### 6.2 전역 이동 규칙

- 온보딩 미완료 사용자는 Welcome로 이동한다.
- 온보딩 완료 사용자는 Learn 홈으로 이동한다.
- Learn, Progress, Settings는 하단 내비게이션으로 이동한다.
- 학습 세션과 완료 화면에서는 하단 내비게이션을 숨긴다.
- Android 뒤로가기는 열린 다이얼로그·bottom sheet·이미지 뷰어를 먼저 닫는다.
- 학습 중 뒤로가기는 세션 종료 확인을 연다.
- 메인 탭의 루트에서 뒤로가기는 Android 기본 앱 종료 동작을 따른다.
- 1차 출시에서는 외부 딥링크를 등록하지 않는다.

### 6.3 화면·상태 목록

| ID | 이름 | 핵심 상태 |
|---|---|---|
| APP-00 | Launch | loading, route-decided, recoverable-error |
| ONB-01 | Welcome | default |
| ONB-02 | Tutorial | question, answer, rated |
| ONB-03 | Level | unselected, selected |
| ONB-04 | Interests | none, selected |
| HOME-01 | Learn home | new, active-session, completed-today |
| LEARN-01 | Card | question, answer, dragging, settling, error |
| LEARN-02 | Image viewer | question-label-hidden, answer-label-visible |
| LEARN-03 | Leave sheet | confirm |
| REPORT-01 | Report | no-reason, reason-selected, saving, saved, error |
| DONE-01 | Complete | no-review, has-review |
| PROG-01 | Progress | empty, populated |
| PROG-02 | Card detail | default |
| SET-01 | Settings | default |
| SET-02 | Session length | 5, 10, 20 |
| SET-04 | Reminder | off, permission-needed, on, denied |
| ACC-01 | Guest info | local-only |
| LEGAL-01 | Policy | loading, content, offline, error |

## 7. 전체 기능 흐름

1. 앱 실행
   - 첫 사용자: Welcome → Tutorial → Level → Interests → Learn home
   - 재방문: Learn home
2. Learn home
   - 새 세션 시작
   - 중단 세션 재개
   - 카테고리 세션 시작
   - Progress 또는 Settings 이동
3. 학습 세션
   - Question → Show answer → Answer
   - Answer → 왼쪽 Got it → 다음 카드
   - Answer → 오른쪽 Review → 다음 카드
   - 판정 토스트 → Undo → 이전 Answer
   - 사진 확대, 음성 재생, 카드 신고
   - 중단 → 홈에서 재개
   - 마지막 판정 → Complete
4. Settings
   - 세션 길이, 로마자, 자동 재생, 음성, 알림, 게스트 안내, 모션, 정책

## 8. 기능 목록

| 기능 ID | 기능명 | 우선순위 | Android 구현 수준 |
|---|---|---|---|
| F-APP-01 | 초기 상태 판별 | Must | 완전 동작 |
| F-ONB-01 | 제품 소개 | Must | 완전 동작 |
| F-ONB-02 | 조작 튜토리얼 | Must | 완전 동작 |
| F-ONB-03 | 수준 선택 | Must | 완전 동작 |
| F-ONB-04 | 관심 분야 선택 | Must | 완전 동작 |
| F-HOME-01 | Learn 홈 | Must | 완전 동작 |
| F-SES-01 | 세션 생성 | Must | 완전 동작 |
| F-SES-02 | 세션 중단·복구 | Must | 완전 동작 |
| F-CARD-01 | 카드 스택과 사진 | Must | 완전 동작 |
| F-CARD-02 | 정답 확인 | Must | 완전 동작 |
| F-CARD-03 | 스와이프 판정 | Must | 완전 동작 |
| F-CARD-04 | 판정 피드백과 Undo | Must | 완전 동작 |
| F-CARD-05 | 이미지 확대 | Must | 완전 동작 |
| F-CARD-06 | 카드 신고 | Must | 로컬 저장 |
| F-CARD-07 | 접근성 판정 | Must | 완전 동작 |
| F-CARD-08 | 기기 경계 처리 | Must | 완전 동작 |
| F-AUD-01 | 발음 재생 | Must | 완전 동작 |
| F-AUD-02 | 발음 음성 선택 | Must | 완전 동작 |
| F-DONE-01 | 세션 완료 | Must | 완전 동작 |
| F-PROG-01 | 진행 현황 | Must | 완전 동작 |
| F-PROG-02 | 복습 카드 상세 | Must | 완전 동작 |
| F-SET-01 | 학습 설정 | Must | 완전 동작 |
| F-SET-02 | 모션 설정 | Must | 완전 동작 |
| F-REM-01 | 복습 알림 | Should | 완전 동작 |
| F-ACC-01 | 게스트 저장 안내 | Must | 정보 제공 |
| F-LEGAL-01 | 정책 문서 | Must | 완전 동작 |

## 9. 기능 상세 명세

### 9.1 F-APP-01 초기 상태 판별

**입력**

- 온보딩 완료 여부
- 활성 세션과 마지막 저장 위치
- 사용자 설정
- 콘텐츠 번들 버전

**처리**

1. DataStore와 Room을 병렬로 읽는다.
2. 콘텐츠 버전이 바뀌었으면 카드 ID를 기준으로 학습 상태를 마이그레이션한다.
3. 온보딩 미완료면 Welcome, 완료면 Learn 홈으로 이동한다.
4. 활성 세션은 자동으로 열지 않고 홈에 재개 카드로 표시한다.

설정 읽기 실패 시 안전한 기본값을 사용한다. Room 열기 실패 시 복구 가능한 오류 화면과 Retry를 제공한다.

### 9.2 F-ONB-01 제품 소개

**필수 콘텐츠**

- Read Korea in the real world
- 거리·상점·역의 한국어를 학습한다는 설명
- 계정 없이 사용할 수 있다는 안내
- 프로토타입과 같은 세 장의 대표 이미지 콜라주

Get started는 Tutorial로 이동한다. 뒤로가기는 앱을 종료한다.

### 9.3 F-ONB-02 조작 튜토리얼

1. 분식 간판과 질문을 표시한다.
2. Show answer 전 판정은 허용하지 않는다.
3. 정답은 분식, Snack bar · casual Korean comfort food다.
4. 방향은 왼쪽 Got it, 오른쪽 Review로 가르친다.
5. 한 번의 예시 판정 후 Continue를 활성화한다.
6. 튜토리얼 결과는 Room 학습 기록에 저장하지 않는다.

화면에 설명용 컨트롤을 사용하더라도 실제 학습 카드의 판정 버튼으로 오해되지 않게 Practice gesture로 구분한다.

### 9.4 F-ONB-03 수준 선택

**선택값**

- Beginner: I can read Hangul slowly.
- Elementary: I can read common words and short phrases.
- Not sure: Start me with easier signs.

한 개만 선택한다. 선택 전 Continue는 비활성이다. Skip은 Beginner를 저장하고 Interests로 이동한다.

### 9.5 F-ONB-04 관심 분야 선택

Food & restaurants, Cafés & drinks, Transport, Shopping, Everyday places, Warnings & notices를 0개 이상 선택할 수 있다. Start learning은 온보딩 완료와 선택을 저장한 뒤 Learn 홈으로 이동한다. 선택이 없으면 모든 카테고리를 균형 있게 사용한다.

### 9.6 F-HOME-01 Learn 홈

**표시 정보**

- 브랜드명과 연속 학습일
- 오늘의 세션 카드 수와 예상 시간
- Review와 New 구성
- 세션 시작 또는 재개 액션
- Landmarks, Neighborhoods, Markets, Transport
- 최근 7일 활동

활성 세션이 있으면 제목은 Pick up where you left off, 메타는 n of m signs complete, 액션은 Resume session으로 바뀐다.

카테고리를 누르면 해당 카테고리 카드가 먼저 오는 새 세션을 만든다. See all은 네 카테고리와 각 카드 수를 보여주는 bottom sheet를 연다.

### 9.7 F-SES-01 세션 생성

**입력**

- 설정된 길이 5, 10, 20
- Due 카드와 미학습 카드
- 선택 카테고리
- 관심 분야와 수준

**규칙**

1. 카테고리 세션이 아니면 Due 카드를 최대 30% 우선 배치한다.
2. Due가 많으면 최대 70%까지 높인다.
3. 나머지는 미학습 카드를 기본 순서와 관심 분야 가중치로 구성한다.
4. 요청 길이보다 사용 가능한 고유 카드가 적으면 같은 세션의 Review 재삽입을 제외하고 중복하지 않는다.
5. 카드 ID 목록을 Room에 저장한 뒤 첫 카드를 연다.
6. 20장 세션에서 기본 번들 전체를 한 번씩 사용할 수 있다.

### 9.8 F-SES-02 세션 중단·복구

닫기 또는 Android 뒤로가기는 다음 정보를 포함한 확인 시트를 연다.

- Leave this session?
- 완료한 카드 수
- 진행이 저장된다는 안내
- Keep learning
- Leave for now

Leave는 현재 인덱스와 판정을 저장하고 홈으로 이동한다. 앱 프로세스가 종료돼도 같은 위치에서 재개한다. 완료한 세션은 재개 대상으로 표시하지 않는다.

### 9.9 F-CARD-01 카드 스택과 사진

다다음 카드, 다음 카드, 활성 카드 순으로 레이어를 구성한다. 활성 카드는 사진과 정보 영역을 하나의 단위로 이동한다. 다음 두 카드는 사진만 보이며 접근성 트리에서는 숨긴다.

사진은 원본 전체가 보이는 Fit 전경, 빈 영역을 채우는 같은 이미지의 블러 배경, 백분율 포커스 프레임, 확대 아이콘으로 구성한다. 질문 상태의 대체 텍스트는 정답을 누설하지 않고, 정답 상태에서는 한국어 표현을 포함한다.

사진 영역은 카드 높이의 62%, 정보 영역은 38%를 기준으로 한다.

### 9.10 F-CARD-02 정답 확인

질문 상태의 하단에는 Show answer 하나만 표시한다. 누르면 같은 카드에서 다음 상태로 바뀐다.

- 질문과 힌트 숨김
- Answer와 ← Got it · Review →
- 한국어와 발음 재생
- 영어 의미와 선택적 로마자
- Words와 Street note
- Show answer 영역 제거

정답 정보는 사진 아래에 머물며 사진을 덮지 않는다. 긴 정보는 하단 정보 영역 안에서만 스크롤한다.

### 9.11 F-CARD-03 스와이프 판정

**방향**

- 왼쪽: Got it
- 오른쪽: Review

**축 판별**

- 최초 12dp 이동까지 축을 정하지 않는다.
- abs(dx)가 abs(dy)의 1.2배보다 크면 가로축으로 잠근다.
- 세로축이면 카드 판정을 시작하지 않고 정보 영역 스크롤을 유지한다.

**정답 전**

- 카드 이동을 ±30dp로 제한한다.
- 28dp를 넘겨 놓으면 중앙 복귀 후 Check the answer first를 표시한다.
- 어떤 ReviewState도 저장하지 않는다.

**정답 후**

- 88dp 초과 시 판정한다.
- 왼쪽 이동 중 카드 오른쪽에 GOT IT을 표시한다.
- 오른쪽 이동 중 카드 왼쪽에 REVIEW를 표시한다.
- 오버레이 투명도는 이동량 90dp까지 비례 증가한다.
- 임계값 미만이면 중앙으로 복귀한다.

Got it 카드는 왼쪽, Review 카드는 오른쪽으로 퇴장한다. 학습 기록 저장 성공 후 다음 카드를 활성화한다.

### 9.12 F-CARD-04 판정 피드백과 Undo

판정 직후 4초 동안 Marked as Got it 또는 Marked as Review와 Undo를 표시한다. 토스트는 사진 하단보다 아래, 세션 조작 영역보다 위에 놓는다.

Undo는 다음을 하나의 Room 트랜잭션으로 처리한다.

1. 마지막 판정 삭제
2. 카드 ReviewState 복원
3. 세션 인덱스 복원
4. 이전 카드를 Answer 상태로 복원
5. Last choice undone 표시

새 판정이 발생하면 이전 Undo 대상은 폐기한다.

### 9.13 F-CARD-05 이미지 확대

- 질문과 정답 상태 모두 확대 뷰어를 열 수 있다.
- 뷰어는 상태·내비게이션 인셋을 포함한 전체 화면이다.
- 핀치 확대와 두 번 탭 확대를 지원한다.
- 질문 진입 시 한국어 라벨을 숨긴다.
- 정답 진입 시 한국어 라벨을 표시한다.
- 닫기와 Android 뒤로가기는 이전 카드 상태로 돌아간다.
- 뷰어 진입과 종료는 카드 판정이나 스크롤 위치를 변경하지 않는다.

### 9.14 F-CARD-06 카드 신고

더보기는 Report this card bottom sheet를 연다.

**필드**

- 사유 단일 선택
- 상세 내용 선택 입력, 최대 500자
- 개인 정보 입력 금지 안내

사유를 선택하기 전 Send report는 비활성이다. 제출하면 로컬 신고 큐에 저장하고 Thanks. We'll review this card.를 표시한다. 네트워크 신고 시스템을 연결한 버전에서는 동일 큐를 재시도 가능한 작업으로 전송한다.

### 9.15 F-CARD-07 접근성 판정

학습 카드의 Compose semantics에 Mark as Got it과 Mark for review 사용자 지정 액션을 등록한다.

정답 전 액션 실행은 판정하지 않고 Check the answer first를 알린다. 정답 후 액션은 스와이프와 같은 저장·토스트·Undo·다음 카드 동작을 실행한다.

물리 키보드는 ArrowLeft = Got it, ArrowRight = Review, Escape = 세션 종료 확인으로 동작한다. 접근성 액션은 화면 판정 버튼을 추가하지 않는다.

### 9.16 F-CARD-08 기기 경계 처리

드래그 좌표는 학습 스테이지의 좌우·상하 경계로 제한한다. 포인터가 경계를 벗어나면 다음 순서로 처리한다.

1. 마지막 좌표를 가장 가까운 경계 값으로 고정한다.
2. 현재까지의 가로 이동량으로 판정 여부를 계산한다.
3. 포인터 캡처를 해제한다.
4. 드래그를 종료한다.
5. 포인터가 바깥에서 움직여도 카드 위치를 갱신하지 않는다.

Android 터치 입력에서는 화면 밖 이동 시 취소 이벤트를 같은 규칙으로 처리한다. 마우스·트랙패드 환경에서도 실제 기기처럼 동작해야 한다.

### 9.17 F-AUD-01 발음 재생

Answer 상태의 스피커 아이콘으로 현재 선택 음성의 현재 카드 파일을 재생한다. 재생 중 다시 누르면 기존 재생을 중단하고 처음부터 재생한다.

Auto-play audio가 On이면 Answer 진입 직후 한 번 재생한다. 화면 재구성만으로 중복 재생하지 않는다.

카드 변경, 세션 종료, 앱 백그라운드 전환 시 재생을 중지한다. 오디오 포커스를 준수하고 재생 실패는 학습을 막지 않는 토스트로 알린다.

### 9.18 F-AUD-02 발음 음성 선택

Settings의 Pronunciation voice는 두 항목을 표시한다.

| 값 | 표시명 | 파일 |
|---|---|---|
| typecast | Typecast · 은경 | assets/audio/eunkyung의 MP3 |

각 세트에는 20개 파일이 있어야 하며 카드 ID와 파일 prefix가 일치해야 한다. 파일이 없으면 제공자 이름이 포함된 오류를 표시한다. 음성 생성용 API 키는 앱에서 사용하지 않는다.

### 9.19 F-DONE-01 세션 완료

마지막 카드 판정 후 다음 정보를 표시한다.

- Session complete
- 연습한 전체 카드 수
- Got it 수와 Review 수
- 다음 복습 안내
- Back home
- Review가 1개 이상일 때 See review cards

전체 수는 Got it과 Review의 합과 같아야 한다. 완료 시 활성 세션을 Completed로 저장하고 홈의 재개 상태를 제거한다.

### 9.20 F-PROG-01 진행 현황

**통계**

- Learned: 한 번 이상 판정한 고유 카드
- Mastered: 최고 복습 단계를 완료한 카드
- Due now: 다음 복습 시각이 지난 카드

최근 7일 일별 학습량, 카테고리별 Learned 수와 비율, Needs review 최근 3개를 표시한다. 표시값은 Room 데이터에서 계산한다. 프로토타입의 24, 8, 3과 그래프 높이는 디자인 예시이며 실제 앱의 고정값이 아니다.

### 9.21 F-PROG-02 복습 카드 상세

Needs review 항목은 간판 사진, 한국어, 영어 의미, Street note, Practice this sign, Close를 포함한 bottom sheet를 연다.

Practice this sign은 선택한 카드로 시작하는 1장 세션을 만든다. 닫으면 Progress의 이전 스크롤 위치로 돌아간다.

### 9.22 F-SET-01 학습 설정

**Session length**

- 5 signs · About 2 minutes
- 10 signs · About 4 minutes
- 20 signs · About 8 minutes

Show romanization이 On이면 Answer에만 로마자를 표시한다. Auto-play audio가 On이면 Answer 진입 시 선택 음성을 한 번 재생한다.

설정 변경은 즉시 DataStore에 저장하고 다음 UI 상태부터 반영한다. Session length 변경은 이미 진행 중인 세션의 길이를 바꾸지 않는다.

### 9.23 F-SET-02 모션 설정

Reduce in-app motion이 On이거나 시스템 애니메이션 제거가 활성화되면 다음을 적용한다.

- 카드 퇴장 20ms 이하 단순 페이드
- 카드 스프링과 회전 제거
- 미리보기 카드의 확대·밝기 보간 최소화
- 화면 전환 단순 페이드 또는 즉시 전환

상태 저장과 판정 타이밍은 일반 모션과 동일하다. 모션 감소가 햅틱·텍스트 피드백을 제거해서는 안 된다.

### 9.24 F-REM-01 복습 알림

기본은 Off다. 사용자가 켜면 Android 버전에 따라 알림 권한을 요청한다.

- 권한 승인: 매일 19:00 기준 작업 예약
- 권한 거부: Off로 복원하고 설정 안내
- Due가 0개: 알림 표시 안 함
- Due가 1개 이상: n signs are ready to review 알림
- 알림 탭: Learn 홈을 열고 복습 시작 액션 강조
- Off 전환: 예약 작업 취소

시간대·재부팅·앱 업데이트 후 WorkManager가 다음 현지 시각 기준으로 다시 계산한다.

### 9.25 F-ACC-01 게스트 저장 안내

Account & sync는 You're learning as a guest, 진행은 현재 기기에 저장됨, 이메일을 저장하지 않음, 앱 데이터 삭제나 앱 제거 시 기록을 잃을 수 있음을 설명한다.

1차 출시에서는 계정 연결 버튼을 비활성 또는 숨김 처리한다. 비활성 상태라면 Cloud sync is not available in this version.을 표시한다.

### 9.26 F-LEGAL-01 정책 문서

Privacy policy와 Terms of use는 각각 실제 문서를 연다.

- 온라인 문서는 Android Custom Tab으로 연다.
- 연결 실패 또는 오프라인에서는 앱에 번들된 마지막 정책 문서를 네이티브 화면으로 표시한다.
- 문서 버전과 적용일을 화면에 표시한다.
- 뒤로가기는 Settings로 복귀한다.

프로토타입의 would open here 토스트를 출시 앱에 사용하지 않는다.

## 10. 상태 모델

### 10.1 카드 상태

| 현재 | 입력 | 조건 | 다음 | 저장 |
|---|---|---|---|---|
| Question | Show answer | 항상 | Answer | answerRevealedAt |
| Question | 가로 스와이프 | 임계값 도달 | Question | 없음 |
| Question | Zoom | 항상 | Viewer.Question | 없음 |
| Answer | Swipe left | 88dp 초과 | Settling.GotIt | Got it |
| Answer | Swipe right | 88dp 초과 | Settling.Review | Review |
| Answer | Swipe | 88dp 이하 | Answer | 없음 |
| Answer | Play audio | 파일 존재 | Answer.Playing | 없음 |
| Answer | Zoom | 항상 | Viewer.Answer | 없음 |
| Settling | 애니메이션 완료 | 다음 카드 있음 | Next.Question | 인덱스 |
| Settling | 애니메이션 완료 | 마지막 카드 | Complete | 세션 완료 |
| Next.Question | Undo | 토스트 유효 | Previous.Answer | 직전 판정 롤백 |

### 10.2 세션 상태

| 상태 | 진입 | 이탈 |
|---|---|---|
| Preparing | 세션 시작 | Active 또는 Error |
| Active | 첫 카드 준비 | Paused, Completed, Error |
| Paused | Leave 확정 또는 프로세스 중단 | Active 또는 Abandoned |
| Completed | 마지막 판정 저장 | Home 또는 Progress |
| Error | 저장·콘텐츠 복구 필요 | Retry 성공 또는 Home |

### 10.3 복습 단계

| 현재 판정 | 결과 | 다음 단계 | 다음 복습 |
|---|---|---:|---|
| 미학습 | Review | 0 | 다음 날 |
| 미학습 | Got it | 1 | 3일 뒤 |
| stage 0~3 | Review | 0 | 다음 날 |
| stage 1 | Got it | 2 | 7일 뒤 |
| stage 2 | Got it | 3 | 21일 뒤 |
| stage 3 | Got it | 4 | 60일 뒤 |
| stage 4 | Got it | 5 | 90일 뒤 |
| stage 5 | Got it | Mastered | 90일 뒤 유지 점검 |

## 11. 카드 데이터 계약

| 필드 | 예시 | 규칙 |
|---|---|---|
| id | 01-hongdae-entrance | 고유, 변경 금지 |
| order | 1 | 1~20 연속 |
| korean | 홍대입구 | 사진과 음성 텍스트 일치 |
| romanization | hongdae ipgu | Answer에서 선택 표시 |
| meaningEn | Hongdae Entrance | 영어 |
| category | NEIGHBORHOOD | 정의된 enum |
| imageAsset | 01-hongdae-entrance.jpg | 고유 사진 |
| focus | 40,18,28,12 | 원본 기준 백분율 |
| wordParts | 홍대 / 입구 | 한 개 이상 |
| streetNoteEn | Hongdae 설명 | 영어 |
| typecastAudio | 01-hongdae-entrance.mp3 | 카드 prefix 일치 |
| contentVersion | 1 | 양의 정수 |

카드 수는 20개다. id, order, 이미지, 두 음성 경로는 중복할 수 없다. focus 사각형은 원본 범위를 넘지 않는다. 모든 카드는 두 음성 파일을 가진다.

## 12. Android 구현 규칙

### 12.1 Compose 상태

- 화면은 immutable UiState를 관찰한다.
- 이벤트는 UiAction으로 ViewModel에 전달한다.
- 판정은 Repository 트랜잭션 성공 후 NextCard effect를 발행한다.
- 화면 회전과 프로세스 재생성은 SavedStateHandle과 Room으로 복구한다.
- 토스트·Snackbar·bottom sheet는 일회성 UI effect로 취급한다.

### 12.2 카드 제스처

- anchoredDraggable 또는 동등한 pointer input으로 가로축을 처리한다.
- 정보 영역의 세로 스크롤과 카드 가로 드래그의 축 경쟁을 12dp·1.2배 규칙으로 해결한다.
- 터치가 버튼에서 시작되면 카드 드래그를 시작하지 않는다.
- 실제 화면 bounds를 기준으로 pointer cancellation을 처리한다.
- 판정 저장 중 중복 입력을 잠근다.

### 12.3 리소스

- 사진과 오디오는 1차 출시 APK/AAB에 번들한다.
- 이미지는 메모리 크기에 맞게 샘플링한다.
- 다음 두 이미지만 적극적으로 prefetch한다.
- Media3 Player 인스턴스는 학습 화면 수명주기에 맞춰 하나만 유지한다.
- API 키, 생성 스크립트, 비밀 환경 파일은 Android source set에 포함하지 않는다.

## 13. 공통 피드백과 오류

| 상황 | 문구 또는 동작 |
|---|---|
| 정답 전 스와이프 | Check the answer first |
| Got it 판정 | Marked as Got it + Undo |
| Review 판정 | Marked as Review + Undo |
| Undo | Last choice undone |
| Typecast 선택 | Typecast voice selected |
| 재생 시작 차단 | Tap the audio button again to play. |
| Typecast 파일 없음 | Typecast audio is unavailable for this sign. |
| 이미지 없음 | Photo unavailable + 정답 학습 유지 |
| 저장 실패 | We couldn't save that choice. Try again. + Retry |
| 신고 저장 | Thanks. We'll review this card. |
| 알림 권한 거부 | Notifications are off. You can enable them in Android settings. |

오류 토스트는 사진을 덮지 않는다. 오류 때문에 정답 읽기나 세션 나가기까지 막아서는 안 된다.

## 14. 접근성 명세

- 터치 대상 최소 48×48dp
- 본문 명암비 4.5:1 이상, 큰 텍스트 3:1 이상
- 한국어 텍스트에 ko 로케일 semantics 적용
- 영어 UI에 en 로케일 적용
- 사진 배경은 접근성 트리에서 제외하고 전경 사진 하나만 설명
- 뒤의 두 카드 미리보기는 접근성 트리에서 제외
- 진행 막대는 current / total을 읽을 수 있어야 함
- 스위치는 이름, 현재값, 변경 가능 상태를 제공
- Radio option은 선택 상태를 제공
- Got it과 Review는 사용자 지정 접근성 액션으로 제공
- 결과는 TalkBack live region으로 알림
- 햅틱은 보조 피드백이며 유일한 정보 전달 수단으로 사용하지 않음
- 200% 글꼴에서 정답 정보 영역 스크롤로 모든 텍스트 접근
- Reduce motion과 시스템 애니메이션 설정을 함께 존중

## 15. 프로토타입·Android 검증 시나리오

### P-01 첫 사용자 핵심 경로

Welcome부터 Interests까지 완료하고 홈에서 5장 세션을 시작한다. 모든 카드의 정답을 열고 판정해 완료 화면에 도달한다.

### P-02 정답 전 판정 차단

Question 상태에서 좌우로 임계값 이상 움직인다. 카드는 중앙으로 돌아오고 안내만 표시되며 진행 수와 데이터가 변하지 않는다.

### P-03 좌우 방향

Answer 상태에서 왼쪽으로 넘긴 카드는 Got it, 오른쪽으로 넘긴 카드는 Review로 저장된다. 오버레이, 퇴장 방향, 토스트가 모두 같은 의미여야 한다.

### P-04 Undo

Got it과 Review를 각각 판정한 직후 Undo한다. 이전 카드가 Answer로 돌아오고 통계와 복습 일정이 원상 복구된다.

### P-05 세션 중단과 프로세스 복구

세 장을 판정하고 세션을 나간다. 앱 프로세스를 종료한 뒤 다시 열어 홈의 Resume으로 네 번째 카드부터 이어간다.

### P-06 이미지와 정답 비가림

Question과 Answer에서 사진을 비교한다. Answer의 정보와 판정 토스트가 사진을 덮지 않으며 확대 뷰어에서 질문 상태는 라벨을 숨긴다.

### P-07 기기 경계

마우스 또는 트랙패드로 카드를 드래그하다 기기 프레임 밖으로 이동한다. 카드는 경계 좌표까지만 이동하고 포인터를 계속 따라가지 않는다.

### P-08 두 음성 선택

카드 1의 Typecast 발음을 재생하고 앱 재실행 후에도 학습 설정이 유지되는지 확인한다.

### P-09 TalkBack 세션

화면 스와이프 없이 Show answer, Play audio, 사용자 지정 Got it·Review 액션을 사용해 세션을 완료한다.

### P-10 오프라인

비행기 모드에서 온보딩 완료 후 20장 사진, 두 음성, 판정, Undo, 완료, Progress, 설정 저장을 확인한다.

## 16. 기능 수용 기준

| AC ID | 수용 조건 |
|---|---|
| AC-01 | 신규 사용자는 Welcome, 완료 사용자는 Learn 홈으로 진입한다. |
| AC-02 | 온보딩은 Welcome, Tutorial, Level, Interests 네 단계다. |
| AC-03 | Level Skip은 Beginner를 저장한다. |
| AC-04 | 관심 분야를 고르지 않아도 홈에 진입한다. |
| AC-05 | 기본 세션 길이는 20장이다. |
| AC-06 | 설정에서 5·10·20장을 선택하고 새 세션부터 반영한다. |
| AC-07 | 활성 카드 뒤에 다음 두 카드가 보인다. |
| AC-08 | 사진은 핵심 간판을 자르지 않고 블러 배경으로 빈 영역을 채운다. |
| AC-09 | Show answer 전 판정 결과가 저장되지 않는다. |
| AC-10 | Show answer 후 사진은 그대로이고 정보는 사진 아래에 표시된다. |
| AC-11 | 왼쪽 스와이프는 Got it이다. |
| AC-12 | 오른쪽 스와이프는 Review다. |
| AC-13 | 정답 상태에 판정 화면 버튼이 없다. |
| AC-14 | 88dp 미만 이동은 판정되지 않는다. |
| AC-15 | 포인터가 기기 밖으로 나가면 카드가 계속 따라가지 않는다. |
| AC-16 | 판정 토스트는 사진을 가리지 않는다. |
| AC-17 | Undo는 직전 판정과 복습 상태를 원상 복구한다. |
| AC-18 | 세션 중단 후 프로세스 재생성에도 위치를 복구한다. |
| AC-19 | 완료 화면의 Got it + Review가 전체 수와 같다. |
| AC-20 | 로마자 표기는 Answer에서만 보인다. |
| AC-21 | Typecast 선택 시 20개 MP3 중 현재 카드 파일을 재생한다. |
| AC-23 | 음성 선택은 앱 재실행 후 유지된다. |
| AC-24 | 자동 재생은 Answer 진입당 한 번만 발생한다. |
| AC-25 | 재생 실패가 카드 판정과 다음 카드 진행을 막지 않는다. |
| AC-26 | Progress 통계는 저장 데이터에서 계산한다. |
| AC-27 | TalkBack 사용자 지정 액션으로 Got it과 Review를 실행한다. |
| AC-28 | ArrowLeft는 Got it, ArrowRight는 Review다. |
| AC-29 | 비행기 모드에서 핵심 학습과 두 음성이 동작한다. |
| AC-30 | TTS API 키가 Android 소스와 배포 파일에 포함되지 않는다. |

## 17. 설계·개발 전달 체크리스트

### 17.1 디자이너

- [ ] 기준 390×844와 최소 360dp 화면을 설계했다.
- [ ] Question과 Answer의 사진 크기가 일관된다.
- [ ] Answer 정보가 사진 아래에 있고 내부 스크롤 상태가 있다.
- [ ] 활성 카드 뒤 두 카드가 분명히 보인다.
- [ ] 왼쪽 Got it, 오른쪽 Review 안내가 모든 화면에서 일치한다.
- [ ] 판정 버튼 없이도 제스처 의미를 이해할 수 있다.
- [ ] 사진을 가리지 않는 토스트 위치를 정의했다.
- [ ] bottom sheet, 키보드, 큰 글자, TalkBack 포커스 상태를 정의했다.
- [ ] Typecast 발음 재생·오류 상태를 정의했다.
- [ ] Reduce motion 버전을 정의했다.

### 17.2 Android 개발자

- [ ] Compose UI가 프로토타입의 구조와 카피를 재현한다.
- [ ] Room과 DataStore의 migration test가 있다.
- [ ] 세션 판정과 Undo가 트랜잭션이다.
- [ ] 터치·마우스·키보드·TalkBack 입력 결과가 동일하다.
- [ ] 다음 두 이미지만 우선 prefetch한다.
- [ ] Media3 Player가 화면 수명주기를 따른다.
- [ ] 20개 카드와 40개 음성 파일을 빌드 시 검증한다.
- [ ] 오프라인 계측 테스트가 있다.
- [ ] API 키 스캔을 CI에 포함한다.

### 17.3 QA

- [ ] P-01~P-10을 실제 Android 기기에서 통과했다.
- [ ] AC-01~AC-30의 증거를 남겼다.
- [ ] API 26과 최신 안정 버전을 확인했다.
- [ ] 360dp·390dp·480dp, 큰 글자, TalkBack을 확인했다.
- [ ] 제스처 내비게이션·3버튼 내비게이션을 확인했다.
- [ ] 비행기 모드와 프로세스 종료 복구를 확인했다.
- [ ] 두 음성의 20개 전체 매핑을 자동 검사로 확인했다.

## 18. 최종 산출물 구조

디자인 파일과 Android 구현은 다음 순서로 추적할 수 있어야 한다.

1. Foundations
2. Components
3. Onboarding
4. Learn home
5. Card session
6. Completion
7. Progress
8. Settings
9. Sheets and dialogs
10. Accessibility and reduced motion
11. Error and offline states
12. Android implementation map
13. Prototype flows

각 화면은 기본 상태만이 아니라 로딩, 비어 있음, 오류, 큰 글자, TalkBack 포커스, Reduce motion 상태를 포함한다. 모든 기능 ID는 디자인 프레임, Compose route 또는 test tag, QA 케이스와 연결한다.
