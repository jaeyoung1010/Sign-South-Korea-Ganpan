import fs from 'node:fs';
import process from 'node:process';

const design = fs.readFileSync(new URL('../docs/android-implementation-design.md', import.meta.url), 'utf8');
const prototype = fs.readFileSync(new URL('../index.html', import.meta.url), 'utf8');
const failures = [];

const requireText = (label, text) => {
  if (!design.includes(text)) failures.push('- ' + label);
};

const requiredSections = [
  '## 3. 설계 원칙',
  '## 5. 모듈 구조',
  '## 6. 헥사고날 구성',
  '## 7. 의존성 주입 설계',
  '## 14. 다국어·현지화 설계',
  '## 17. TDD 실행 규칙',
  '## 26. CI 품질 게이트',
  '## 32. 구현 완료 정의'
];

for (const section of requiredSections) requireText('missing section: ' + section, section);

const requiredRules = [
  'domain은 Android, Compose, Room, Hilt, Media3, WorkManager를 참조하지 않는다.',
  '모든 기능 변경은 Red → Green → Refactor 순서를 따른다.',
  '왼쪽 스와이프는 Got it이다.',
  '오른쪽 스와이프는 Review다.',
  '영어와 일본어를 처음부터 동등한 출시 언어로 다룬다.',
  '기계 번역 결과를 그대로 출시 콘텐츠로 사용하지 않는다.',
  'UI string key 100% 존재',
  '20개 카드 LocalizedCardCopy 100% 존재',
  'Typecast MP3 20개',
  'Gemini WAV 20개'
];

for (const rule of requiredRules) requireText('missing implementation rule: ' + rule, rule);

const outputPorts = [
  'CardCatalog',
  'LearningRepository',
  'SessionRepository',
  'UserSettingsRepository',
  'AudioPlayerPort',
  'ReminderSchedulerPort',
  'AppLocalePort',
  'CardReportRepository',
  'AnalyticsSink',
  'ClockPort'
];

for (const port of outputPorts) requireText('missing output port: ' + port, '| ' + port + ' |');

const gestureTests = [...design.matchAll(/^\| UT-GES-\d+ \|/gm)].length;
const e2eTests = [...design.matchAll(/^### E2E-\d+/gm)].length;
if (gestureTests < 12) failures.push('- expected at least 12 gesture boundary tests, found ' + gestureTests);
if (e2eTests < 12) failures.push('- expected at least 12 end-to-end scenarios, found ' + e2eTests);

const unresolved = design.match(/\b(?:TBD|TODO|FIXME)\b|미정|추후 결정/gi) ?? [];
if (unresolved.length) failures.push('- unresolved terms: ' + unresolved.join(', '));

const defaultSessionPattern = /sessionLength:\s*20/;
const resetSessionPattern = /Object\.assign\(state,[\s\S]*?sessionLength:20/;
if (!defaultSessionPattern.test(prototype)) failures.push('- prototype initial session length is not 20');
if (!resetSessionPattern.test(prototype)) failures.push('- prototype reset session length is not 20');

if (failures.length) {
  console.error('Implementation design validation failed:\n' + failures.join('\n'));
  process.exit(1);
}

console.log('Implementation design validation passed.');
console.log('Architecture: lightweight hexagonal + Hilt injection + TDD gates.');
console.log('Localization: English and Japanese with native transcreation workflow.');
console.log('Tests: ' + gestureTests + ' gesture cases and ' + e2eTests + ' E2E scenarios.');
