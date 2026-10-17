import fs from 'node:fs';

const planPath = 'docs/android-implementation-plan.md';
const sourcePaths = [
  'docs/PRD-SRS.md',
  'docs/functional-specification.md',
];

const plan = fs.readFileSync(planPath, 'utf8');
const sources = sourcePaths.map((path) => fs.readFileSync(path, 'utf8')).join('\n');
const prototype = fs.readFileSync('index.html', 'utf8');

function uniqueMatches(text, pattern) {
  return [...new Set(text.match(pattern) ?? [])].sort();
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

const phaseMatches = [...plan.matchAll(/^### Phase (\d+) — .+ \(~1 man-day\)$/gm)];
const phaseNumbers = phaseMatches.map((match) => Number(match[1]));
assert(phaseNumbers.length === 45, `Expected 45 one-day phases, found ${phaseNumbers.length}`);
assert(
  phaseNumbers.every((number, index) => number === index + 1),
  `Phases are not sequential: ${phaseNumbers.join(', ')}`,
);

const idPattern = /\b(?:FR|NFR|AC|F)-[A-Z0-9]+(?:-[A-Z0-9]+)*\b/g;
const sourceIds = uniqueMatches(sources, idPattern);
const planIds = new Set(uniqueMatches(plan, idPattern));
const missingIds = sourceIds.filter((id) => !planIds.has(id));
assert(missingIds.length === 0, `Requirements missing from plan: ${missingIds.join(', ')}`);

const sourceScreens = uniqueMatches(prototype, /(?<=id=")screen-[a-z-]+/g);
const missingScreens = sourceScreens.filter((screen) => !plan.includes(screen));
assert(missingScreens.length === 0, `Prototype screens missing from plan: ${missingScreens.join(', ')}`);

const requiredStatements = [
  '현재 HTML 프로토타입을 기능 우선순위의 최상위',
  '왼쪽 Got it, 오른쪽 Review',
  'debug` 빌드의 QA 메뉴',
  '경량 헥사고날',
  'Hilt 의존성 주입',
  'Red→Green',
  '영어 직역이 아닌 일본인 여행자 관점',
  '20개 사진, Typecast MP3 20개, Gemini WAV 20개',
  '45 Phase, 약 45 man-day',
];

for (const statement of requiredStatements) {
  assert(plan.includes(statement), `Required planning statement missing: ${statement}`);
}

const placeholders = uniqueMatches(plan, /(?:TODO|TBD|FIXME|미정|추후 결정)/g);
assert(placeholders.length === 0, `Unresolved placeholders found: ${placeholders.join(', ')}`);

console.log('Implementation plan validation passed.');
console.log(`Phases: ${phaseNumbers.length}, sequential and approximately one man-day each.`);
console.log(`Requirement IDs covered: ${sourceIds.length}/${sourceIds.length}.`);
console.log(`Prototype screens covered: ${sourceScreens.length}/${sourceScreens.length}.`);
