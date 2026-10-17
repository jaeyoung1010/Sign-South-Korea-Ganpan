import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const generator = fs.readFileSync(path.join(root, 'scripts/generate-gemini-audio.mjs'), 'utf8');
const typecastManifest = JSON.parse(fs.readFileSync(path.join(root, 'assets/audio/eunkyung/manifest.json'), 'utf8'));
const geminiManifest = JSON.parse(fs.readFileSync(path.join(root, 'assets/audio/gemini/manifest.json'), 'utf8'));
const failures = [];

const requireMatch = (label, pattern, source = html) => {
  if (!pattern.test(source)) failures.push(`- ${label}`);
};

requireMatch('settings includes a pronunciation voice selector', /id="voiceSetting"/);
requireMatch('settings displays the selected voice', /id="voiceValue"/);
requireMatch('state tracks typecast or gemini selection', /audioVoice:/);
requireMatch('Gemini selection maps MP3 cards to downloaded WAV files', /assets\/audio\/gemini\/[\s\S]*?\.replace\(\/\\\.mp3\$\/, '\.wav'\)/);
requireMatch('selected voice is persisted locally', /localStorage\.setItem\('signKoreanAudioVoice'/);
requireMatch('Gemini generator uses the non-streaming interactions endpoint', /v1beta\/interactions/, generator);
requireMatch('Gemini generator requests complete WAV output', /mime_type: "audio\/wav"/, generator);
requireMatch('Gemini generator uses Flash TTS', /gemini-3\.8-flash-tts/, generator);
requireMatch('Gemini API key stays in the local generation process', /process\.env\.GEMINI_API_KEY/, generator);

const audioSets = [
  { label:'Typecast', directory:'assets/audio/eunkyung', extension:'.mp3', manifest:typecastManifest },
  { label:'Gemini', directory:'assets/audio/gemini', extension:'.wav', manifest:geminiManifest },
];

for (const set of audioSets) {
  const directory = path.join(root, set.directory);
  const files = fs.readdirSync(directory).filter(file => file.endsWith(set.extension));
  if (files.length !== 20) failures.push(`- ${set.label} must contain 20 ${set.extension} files, found ${files.length}`);
  if (set.manifest.files?.length !== 20) failures.push(`- ${set.label} manifest must list 20 files`);
  for (const entry of set.manifest.files ?? []) {
    const audioPath = path.join(directory, entry.file);
    if (!fs.existsSync(audioPath) || fs.statSync(audioPath).size < 1000) failures.push(`- missing or empty ${set.label} audio: ${entry.file}`);
  }
}

if (failures.length) {
  console.error(`Audio-choice validation failed:\n${failures.join('\n')}`);
  process.exit(1);
}

console.log('Audio-choice validation passed: Typecast and downloaded Gemini Flash TTS voice sets are selectable.');
