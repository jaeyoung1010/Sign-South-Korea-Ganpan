import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');

const checks = [
  ['warm diary theme color', /<meta name="theme-color" content="#fff7fd">/i],
  ['blush-pink primary token', /--primary:\s*#ff5e8e\s*;/i],
  ['pastel mint token', /--got:\s*#2dd4bf\s*;/i],
  ['butter-yellow token', /--y2k-yellow:\s*#ffed85\s*;/i],
  ['deep-plum outline token', /--outline:\s*#3b2c43\s*;/i],
  ['offset game-shadow token', /--game-shadow:\s*4px 5px 0 var\(--outline\)\s*;/i],
  ['diary grid background token', /--diary-grid\s*:/i],
  ['screen uses diary background', /\.screen\s*\{[\s\S]*?background-image:\s*var\(--diary-grid\)/i],
  ['pill buttons have outlined game styling', /\.primary-btn[\s\S]*?border:\s*2px solid var\(--outline\)[\s\S]*?border-radius:\s*999px/i],
  ['buttons visibly depress', /\.primary-btn:active[\s\S]*?transform:\s*translate\(2px,\s*3px\)/i],
  ['review swipe cue is pink', /\.swipe-overlay\.review\s*\{[^}]*background:\s*#fff0f6/i],
  ['got-it swipe cue is mint', /\.swipe-overlay\.got\s*\{[^}]*background:\s*#e6fff8/i],
  ['bottom nav uses deep outline', /\.bottom-nav\s*\{[\s\S]*?border-top:\s*2px solid var\(--outline\)/i],
  ['learning image remains full-screen', /\.learn-card \.photo-wrap\s*\{[\s\S]*?position:\s*absolute[\s\S]*?inset:\s*0/i],
  ['learning backdrop still fills the device', /\.learn-card \.photo-wrap \.photo-backdrop\s*\{[\s\S]*?object-fit:\s*cover/i],
  ['learning foreground preserves full sign', /\.learn-card \.photo-wrap \.photo-main\s*\{[\s\S]*?object-fit:\s*contain/i],
];

const failures = checks
  .filter(([, pattern]) => !pattern.test(html))
  .map(([label]) => `- ${label}`);

if (failures.length) {
  console.error(`Theme validation failed:\n${failures.join('\n')}`);
  process.exit(1);
}

console.log(`Theme validation passed (${checks.length} checks).`);
