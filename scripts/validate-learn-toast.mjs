import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');

const learnToastRule = html.match(/\.toast\.above-actions\s*\{([^}]*)\}/i)?.[1] ?? '';
const checks = [
  ['learning toast is anchored near the top safe area', /top:\s*calc\(var\(--safe-top\)\s*\+\s*148px\)\s*;/i],
  ['learning toast is no longer anchored to the bottom', /bottom:\s*auto\s*;/i],
];

const failures = checks
  .filter(([, pattern]) => !pattern.test(learnToastRule))
  .map(([label]) => `- ${label}`);

if (failures.length) {
  console.error(`Learning toast validation failed:\n${failures.join('\n')}`);
  process.exit(1);
}

console.log('Learning toast validation passed: feedback sits above the sign photo.');
