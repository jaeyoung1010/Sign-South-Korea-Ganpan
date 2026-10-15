# Typecast pronunciation audio

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

## Typecast 계정 오류

`UNUSUAL_ACTIVITY_DETECTED`가 반환되면 같은 키로 반복 호출하지 않는다. Typecast API 콘솔에서 계정·API 플랜 상태를 확인하거나 Typecast 지원 채널에 문의한 뒤 다시 실행한다. 웹 편집기 구독과 API 플랜은 별도일 수 있다.
