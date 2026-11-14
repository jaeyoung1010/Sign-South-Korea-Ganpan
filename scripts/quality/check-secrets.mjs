import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { collectTextFiles, findLikelySecrets } from './quality-rules.mjs';

const root = path.resolve(process.argv[2] ?? '.');
const included = new Set(execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z'], { cwd: root, encoding: 'utf8' }).split('\0').filter(Boolean));
const candidates = collectTextFiles(root).filter((file) => included.has(path.relative(root, file)));
const findings = [];

for (const file of candidates) {
  const matches = findLikelySecrets(fs.readFileSync(file, 'utf8'));
  for (const match of matches) {
    findings.push(`${path.relative(root, file)}:${match.line} ${match.kind}`);
  }
}

if (findings.length > 0) {
  console.error('Likely committed credentials detected:');
  findings.forEach((finding) => console.error(`- ${finding}`));
  process.exit(1);
}

console.log(`Secret scan passed (${candidates.length} text files).`);
