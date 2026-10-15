import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');

const learnToastRules = [...html.matchAll(/\.toast\.above-actions\s*\{([^}]*)\}/gi)];
const learnToastRule = learnToastRules.at(-1)?.[1] ?? '';
const checks = [
  ['learning toast is no longer anchored over the photo', /top:\s*auto\s*;/i],
  ['learning toast sits above the action area', /bottom:\s*calc\(96px\s*\+\s*var\(--safe-bottom\)\)\s*;/i],
];

const failures = checks
  .filter(([, pattern]) => !pattern.test(learnToastRule))
  .map(([label]) => `- ${label}`);

if (failures.length) {
  console.error(`Learning toast validation failed:\n${failures.join('\n')}`);
  process.exit(1);
}

console.log('Learning toast validation passed: feedback stays below the sign photo and above the action area.');
