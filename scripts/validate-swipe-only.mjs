import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = path.resolve(import.meta.dirname, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');

const failures = [];
const requireMatch = (label, pattern) => {
  if (!pattern.test(html)) failures.push(`- ${label}`);
};
const requireAbsent = (label, pattern) => {
  if (pattern.test(html)) failures.push(`- ${label}`);
};

requireAbsent('rating action bar is removed', /id="ratingActions"/);
requireAbsent('Review rating button is removed', /id="reviewButton"/);
requireAbsent('Got it rating button is removed', /id="gotButton"/);
requireAbsent('removed rating buttons have no event listeners', /\$\(['"]#(?:reviewButton|gotButton|ratingActions)['"]\)/);
requireAbsent('prototype tip does not promise removed buttons', /buttons offer the same actions/i);
requireMatch('answer gives both swipe directions', /<span>←\s*Got it\s*·\s*Review\s*→<\/span>/i);
requireMatch('prototype tip teaches both swipe outcomes', /Tip:[^<]*left[^<]*Got it[^<]*right[^<]*Review/i);
requireMatch('tutorial teaches Got it on the left and Review on the right', /tutorial-directions[\s\S]*?←\s*Got it[\s\S]*?Review\s*→/i);
requireMatch('left swipe rates Got it and right swipe rates Review', /rateCard\(dx\s*<\s*0\s*\?\s*['"]got['"]\s*:\s*['"]review['"]\)/);
requireMatch('left keyboard arrow rates Got it', /event\.key\s*===\s*['"]ArrowLeft['"]\)\s*rateCard\(['"]got['"]\)/);
requireMatch('right keyboard arrow rates Review', /event\.key\s*===\s*['"]ArrowRight['"]\)\s*rateCard\(['"]review['"]\)/);
requireMatch('left drag shows Got it cue', /gotOverlay['"]\)\.style\.opacity\s*=\s*limited\s*<\s*0\s*\?\s*strength\s*:\s*0/);
requireMatch('right drag shows Review cue', /reviewOverlay['"]\)\.style\.opacity\s*=\s*limited\s*>\s*0\s*\?\s*strength\s*:\s*0/);
requireMatch('Got it cue stays visible on a left drag', /\.swipe-overlay\.got\s*\{[^}]*right:\s*22px/i);
requireMatch('Review cue stays visible on a right drag', /\.swipe-overlay\.review\s*\{[^}]*left:\s*22px/i);
requireMatch('Got it exits left and Review exits right', /rating\s*===\s*['"]got['"]\s*\?\s*-125\s*:\s*125/);
requireMatch('answer stays in its own lower card region', /\.learn-card\.answer-open \.card-scroll\s*\{[^}]*top:\s*62%\s*;[^}]*bottom:\s*0\s*;/i);

if (failures.length) {
  console.error(`Swipe-only validation failed:\n${failures.join('\n')}`);
  process.exit(1);
}

console.log('Swipe-only validation passed: buttons removed; left=Got it; right=Review.');
