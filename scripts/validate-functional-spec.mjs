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
  '간판 이미지는 학습 화면 전체를 덮는 방식으로 표시한다.',
  '왼쪽은 `Review`, 오른쪽은 `Got it`으로 고정한다.',
  '정답 확인 이후에만 판정이 가능하다.',
  '카드는 기기 프레임 밖의 포인터를 계속 따라가지 않는다.',
  '스와이프가 필요한 모든 기능에는 직접 실행 가능한 대체 조작이 있어야 한다.'
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
