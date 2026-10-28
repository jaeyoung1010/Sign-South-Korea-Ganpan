import fs from 'node:fs';

const specUrl = new URL('../docs/functional-specification.md', import.meta.url);
const spec = fs.readFileSync(specUrl, 'utf8');

const unresolved = spec.match(/\b(?:TBD|TODO|FIXME)\b|미정|추후 결정/gi) ?? [];
if (unresolved.length) throw new Error(`Unresolved terms: ${unresolved.join(', ')}`);

const listedIds = [...spec.matchAll(/^\| (F-[A-Z]+-\d+) \|/gm)].map(match => match[1]);
const detailIds = [...spec.matchAll(/^### 9\.\d+ (F-[A-Z]+-\d+) /gm)].map(match => match[1]);
const missingDetails = listedIds.filter(id => !detailIds.includes(id));
const unlistedDetails = detailIds.filter(id => !listedIds.includes(id));

if (new Set(listedIds).size !== listedIds.length) throw new Error('Duplicate function IDs in the function list.');
if (new Set(detailIds).size !== detailIds.length) throw new Error('Duplicate function detail sections.');
if (missingDetails.length) throw new Error(`Missing detail sections: ${missingDetails.join(', ')}`);
if (unlistedDetails.length) throw new Error(`Unlisted detail sections: ${unlistedDetails.join(', ')}`);

const requiredStatements = [
  '간판 이미지는 학습 카드의 가로 폭을 채우며 원본의 핵심 글자를 자르지 않는다.',
  '왼쪽 스와이프는 Got it, 오른쪽 스와이프는 Review다.',
  '정답 확인 이후에만 판정이 가능하다.',
  '카드는 기기 프레임 밖의 포인터를 계속 따라가지 않는다.',
  '정답 상태에 Got it·Review 화면 버튼을 제공하지 않는다.',
  '보이는 판정 버튼 대신 TalkBack 사용자 지정 액션과 물리 키보드 화살표를 접근성 대체 조작으로 제공한다.',
  'Typecast 은경 발음을 재생한다.'
];

for (const statement of requiredStatements) {
  if (!spec.includes(statement)) throw new Error(`Missing required behavior: ${statement}`);
}

const scenarioCount = [...spec.matchAll(/^### P-\d+/gm)].length;
const acceptanceCount = [...spec.matchAll(/^\| AC-\d+ \|/gm)].length;
if (scenarioCount < 7) throw new Error(`Expected at least 7 prototype scenarios, found ${scenarioCount}.`);
if (acceptanceCount < 15) throw new Error(`Expected at least 15 acceptance criteria, found ${acceptanceCount}.`);

console.log(`Function IDs: ${listedIds.length}/${listedIds.length} have detail sections`);
console.log(`Prototype scenarios: ${scenarioCount}`);
console.log(`Acceptance criteria: ${acceptanceCount}`);
console.log('Required full-screen swipe behaviors: present');
console.log('Unresolved placeholders: none');
