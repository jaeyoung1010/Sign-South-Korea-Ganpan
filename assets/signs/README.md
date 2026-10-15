# Prototype sign image manifest

이 폴더의 사진 10장은 Sign Korean 인터랙티브 프로토타입용으로 OpenAI 내장 이미지 생성 도구를 사용해 제작한 합성 이미지다. 실제 상점·기관·브랜드를 촬영한 자료가 아니며 제품 출시용 콘텐츠로 전환할 때는 별도의 언어·문화·권리 검수를 거쳐야 한다.

## 공통 프롬프트

```text
Use case: photorealistic-natural
Asset type: mobile language-learning flashcard photo
Style/medium: natural editorial or architectural travel photography with authentic Korean street context
Composition/framing: landscape 4:3, the learning sign centered, prominent, and easy to read
Constraints: use the requested Korean text verbatim; no other legible text, brand logos, watermark, recognizable people, phone numbers, or vehicle plates
```

## 자산별 최종 요청

| 파일 | Primary request | Text (verbatim) | 추가 제약 |
|---|---|---|---|
| `bunsik.jpg` | 서울 동네의 작은 분식집 외관과 가로 간판 | `분식` | 밝은 흐린 낮, 캐주얼 식당 맥락 |
| `exit.jpg` | 현대적인 서울 지하철 통로의 출구 방향 간판 | `출구` | 위쪽 화살표, 역명·노선 번호 제외 |
| `pharmacy.jpg` | 서울 동네의 작은 약국 외관과 녹색 간판 | `약국` | 의료 브랜드 제외 |
| `no-parking.jpg` | 좁은 건물 주차장 입구의 적백색 경고 간판 | `주차 금지` | 한 칸 띄어쓰기 유지 |
| `restroom.jpg` | 공공건물 복도의 화장실 방향 간판 | `화장실` | 남녀 픽토그램 포함 |
| `convenience-store.jpg` | 이른 저녁의 작은 한국 편의점 외관 | `편의점` | 특정 체인 색상·로고 제외 |
| `entrance.jpg` | 현대적인 건물 유리문에 부착된 입구 간판 | `입구` | 안쪽 방향 화살표 포함 |
| `open.jpg` | 한국 상점 문에 걸린 나무 영업 안내판 | `영업중` | 영업시간·전화번호 제외 |
| `no-smoking.jpg` | 현대적인 카페 외부의 금연 안내판 | `금연` | 국제 금연 픽토그램 포함 |
| `sale.jpg` | 한국 의류 매장 쇼윈도의 계절 세일 포스터 | `세일` | 가격·브랜드 제외 |

원본 생성 파일은 Codex 이미지 생성 보관 위치에 남겨두고, 프로토타입에는 1280px 이하·JPEG 품질 82로 최적화한 사본을 사용한다.
