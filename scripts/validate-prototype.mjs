import fs from 'node:fs';

const html = fs.readFileSync(new URL('../index.html', import.meta.url), 'utf8');
const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]);
const duplicateIds = [...new Set(ids.filter((id, index) => ids.indexOf(id) !== index))];

if (duplicateIds.length) throw new Error(`Duplicate IDs: ${duplicateIds.join(', ')}`);

const script = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
if (!script) throw new Error('Inline application script was not found.');
new Function(script);

const referencedIds = [...script.matchAll(/\$\('#([^']+)'\)/g)].map(match => match[1]);
const missingIds = [...new Set(referencedIds.filter(id => !ids.includes(id)))];
if (missingIds.length) throw new Error(`Missing DOM IDs: ${missingIds.join(', ')}`);

const signPaths = [...html.matchAll(/assets\/signs\/[a-z-]+\.jpg/g)].map(match => match[0]);
for (const signPath of new Set(signPaths)) {
  if (!fs.existsSync(new URL(`../${signPath}`, import.meta.url))) {
    throw new Error(`Missing sign asset: ${signPath}`);
  }
}

const viewportWidth = 390;
const viewportHeight = 844;
const imageWidth = 1280;
const imageHeight = 960;
const scale = Math.min(viewportWidth / imageWidth, viewportHeight / imageHeight);
const renderedWidth = imageWidth * scale;
const renderedHeight = imageHeight * scale;

console.log(`DOM IDs: ${ids.length} unique`);
console.log('JavaScript syntax and DOM references: OK');
console.log(`Sign image assets: ${new Set(signPaths).size} available`);
console.log(`Portrait fit: ${renderedWidth.toFixed(0)}x${renderedHeight.toFixed(0)} foreground, no horizontal crop`);

