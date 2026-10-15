import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const manifest = JSON.parse(fs.readFileSync(path.join(root, 'assets/audio/eunkyung/manifest.json'), 'utf8'));
const failures = [];

const cardBlock = html.match(/const cards = \[([\s\S]*?)\n    \];/)?.[1] ?? '';
const imagePaths = [...cardBlock.matchAll(/image:'([^']+)'/g)].map(match => match[1]);
const audioPaths = [...cardBlock.matchAll(/audio:'([^']+)'/g)].map(match => match[1]);
const words = [...cardBlock.matchAll(/\{ word:'([^']+)'/g)].map(match => match[1]);

if (words.length !== 20) failures.push(`- expected 20 learning cards, found ${words.length}`);
if (new Set(imagePaths).size !== 20) failures.push('- each learning card must use a unique generated image');
if (new Set(audioPaths).size !== 20) failures.push('- each learning card must use a unique Typecast audio file');

for (const assetPath of [...imagePaths, ...audioPaths]) {
  const absolutePath = path.join(root, assetPath);
  if (!fs.existsSync(absolutePath)) failures.push(`- missing asset: ${assetPath}`);
  else if (fs.statSync(absolutePath).size < 1000) failures.push(`- asset is unexpectedly small: ${assetPath}`);
}

if (!/id="thirdCardPreview"/.test(html) || !/id="nextCardPreview"/.test(html)) {
  failures.push('- two visible preview cards are required behind the active card');
}
if (!/\.third-card-preview\s*\{[\s\S]*?transform:\s*translateY\(20px\)\s*scale\(\.91\)/.test(html)) {
  failures.push('- third preview card is not visibly offset in the stack');
}
if (!/\.learn-card \.photo-wrap\s*\{[\s\S]*?height:\s*62%/.test(html)) {
  failures.push('- photo does not have a dedicated fixed card region');
}
if (!/\.learn-card\.answer-open \.card-scroll\s*\{[\s\S]*?top:\s*62%/.test(html)) {
  failures.push('- answer region is not kept below the photo');
}

if (manifest.voice_name !== '은경') failures.push('- Typecast manifest voice must be 은경');
if (manifest.source !== 'api-page') failures.push('- Typecast manifest source must be api-page');
if (manifest.generated_by !== 'codex') failures.push('- Typecast manifest generated_by must be codex');
if (manifest.files?.length !== 20) failures.push(`- expected 20 manifest entries, found ${manifest.files?.length ?? 0}`);

if (failures.length) {
  console.error(`Card-stack validation failed:\n${failures.join('\n')}`);
  process.exit(1);
}

console.log('Card-stack validation passed: 20 generated photo cards, two-card preview stack, non-covering answer region, and 20 Typecast 은경 clips.');
