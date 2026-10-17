# Pronunciation audio

프로토타입은 각 간판의 발음을 다음 경로에서 재생한다.

```text
assets/audio/eunkyung/*.mp3
```

오디오는 Typecast `ssfm-v30` 모델과 `은경` 캐릭터로 생성한다. API 키는 HTML이나 저장소에 넣지 않고 로컬 생성 프로세스의 `TYPECAST_API_KEY` 환경 변수로만 전달한다.

`.env.example`을 `.env.local`로 복사하고 현재 Typecast API 키를 입력한 뒤 실행한다. `.env.local`은 Git에서 제외된다.

```bash
cp .env.example .env.local
node scripts/generate-typecast-audio.mjs
```

같은 파일을 다시 생성하려면 `--force`를 추가한다. 은경 이름이 API에서 둘 이상 발견되는 경우 Typecast API 음성 라이브러리에서 정확한 ID를 확인한 뒤 `TYPECAST_VOICE_ID`도 함께 지정한다.

이 경우 `.env.local`의 `TYPECAST_VOICE_ID`에 선택한 ID를 추가하고 다음 명령을 실행한다.

```bash
node scripts/generate-typecast-audio.mjs --force
```

생성 스크립트와 결과 manifest에는 Typecast API 페이지 요청에 따라 다음 attribution을 영구 기록한다.

```text
source=api-page
generated_by=codex
```

## Gemini Flash TTS 음성

Gemini 음성은 실시간 스트리밍하지 않는다. 로컬 생성 스크립트가 20개 발음을 완전한 WAV 파일로 다운로드하고 앱은 다음 경로에서 정적 파일로 재생한다.

```text
assets/audio/gemini/*.wav
```

Google AI Studio에서 Gemini API 키를 만든 뒤 `.env.local`에 다음 값을 추가한다.

```text
GEMINI_API_KEY=your_key_here
GEMINI_TTS_VOICE=Kore
GEMINI_TTS_STYLE=clear and natural Korean pronunciation at a calm learning pace
```

그다음 프로젝트 루트에서 실행한다.

```bash
node scripts/generate-gemini-audio.mjs
```

이미 생성된 파일은 자동으로 건너뛴다. 전부 다시 생성하려면 `--force`를 사용한다.

```bash
node scripts/generate-gemini-audio.mjs --force
```

스크립트는 무료 요금제의 분당 3회 호출 제한에 맞춰 요청 사이를 최소 21초로 조절한다. 짧은 제한 응답은 자동으로 대기하고, 재시도 시간이 1분을 넘는 장기 할당량 제한이면 즉시 종료한다. 제한이 초기화된 뒤 같은 명령을 다시 실행하면 이미 생성된 파일을 건너뛰고 남은 음원부터 계속한다.

스크립트는 `gemini-3.8-flash-tts`의 비스트리밍 Interactions API를 사용하며, `Kore` 음성의 24kHz mono 16-bit WAV 파일과 `manifest.json`을 저장한다. `GEMINI_TTS_VOICE`에는 기본 제공 음성 이름 또는 Voice Design으로 만든 `voice_...` ID를 지정할 수 있다. API 키는 브라우저와 `index.html`에 포함되지 않는다.

## Typecast 계정 오류

`UNUSUAL_ACTIVITY_DETECTED`가 반환되면 같은 키로 반복 호출하지 않는다. Typecast API 콘솔에서 계정·API 플랜 상태를 확인하거나 Typecast 지원 채널에 문의한 뒤 다시 실행한다. 웹 편집기 구독과 API 플랜은 별도일 수 있다.
