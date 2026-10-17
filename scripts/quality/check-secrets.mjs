import fs from 'node:fs';
import path from 'node:path';
import { collectTextFiles, findLikelySecrets } from './quality-rules.mjs';

const root = path.resolve(process.argv[2] ?? '.');
const candidates = collectTextFiles(root);
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
