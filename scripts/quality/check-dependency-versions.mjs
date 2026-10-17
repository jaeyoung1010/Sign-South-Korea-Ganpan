import fs from 'node:fs';
import path from 'node:path';
import { collectTextFiles, findDynamicVersions } from './quality-rules.mjs';

const root = path.resolve(process.argv[2] ?? '.');
const candidates = collectTextFiles(root).filter((file) =>
  file.endsWith('.gradle.kts') || file.endsWith('libs.versions.toml'),
);
const findings = [];

for (const file of candidates) {
  const matches = findDynamicVersions(fs.readFileSync(file, 'utf8'));
  for (const match of matches) {
    findings.push(`${path.relative(root, file)}:${match.line} ${match.value}`);
  }
}

if (findings.length > 0) {
  console.error('Dynamic dependency versions are not allowed:');
  findings.forEach((finding) => console.error(`- ${finding}`));
  process.exit(1);
}

console.log(`Dependency version validation passed (${candidates.length} files).`);
