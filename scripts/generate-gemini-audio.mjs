#!/usr/bin/env node

import { mkdir, rename, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const API_URL = "https://generativelanguage.googleapis.com/v1beta/interactions";
const MODEL = "gemini-3.8-flash-tts";
const DEFAULT_VOICE = "Kore";
const DEFAULT_STYLE = "clear and natural Korean pronunciation at a calm learning pace";
const MIN_REQUEST_INTERVAL_MS = 21_000;
let lastRequestStartedAt = 0;

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDirectory, "..");
const outputDirectory = path.join(projectRoot, "assets", "audio", "gemini");
const force = process.argv.includes("--force");

try {
  process.loadEnvFile(path.join(projectRoot, ".env.local"));
} catch (error) {
  if (error?.code !== "ENOENT") throw error;
}

const phrases = [
  { file: "01-hongdae-entrance.wav", text: "홍대입구" },
  { file: "02-gyeongbokgung.wav", text: "경복궁" },
  { file: "03-bukchon-hanok-village.wav", text: "북촌한옥마을" },
  { file: "04-namsan-seoul-tower.wav", text: "남산서울타워" },
  { file: "05-myeongdong.wav", text: "명동" },
  { file: "06-gwangjang-market.wav", text: "광장시장" },
  { file: "07-namdaemun-market.wav", text: "남대문시장" },
  { file: "08-itaewon.wav", text: "이태원" },
  { file: "09-hangang-park.wav", text: "한강공원" },
  { file: "10-seongsu-dong.wav", text: "성수동" },
  { file: "11-insadong.wav", text: "인사동" },
  { file: "12-ikseon-dong.wav", text: "익선동" },
  { file: "13-dongdaemun-design-plaza.wav", text: "동대문디자인플라자" },
  { file: "14-haeundae.wav", text: "해운대" },
  { file: "15-gamcheon-culture-village.wav", text: "감천문화마을" },
  { file: "16-jeonju-hanok-village.wav", text: "전주한옥마을" },
  { file: "17-jeju-olle-trail.wav", text: "제주올레길" },
  { file: "18-bulguksa.wav", text: "불국사" },
  { file: "19-busan-station.wav", text: "부산역" },
  { file: "20-seoul-station.wav", text: "서울역" },
];

function configuration() {
  const apiKey = process.env.GEMINI_API_KEY?.trim();
  if (!apiKey) {
    throw new Error(
      "GEMINI_API_KEY is not set. Add a Google AI Studio Gemini API key to .env.local, then run: " +
      "node scripts/generate-gemini-audio.mjs"
    );
  }
  return {
    apiKey,
    voice: process.env.GEMINI_TTS_VOICE?.trim() || DEFAULT_VOICE,
    style: process.env.GEMINI_TTS_STYLE?.trim() || DEFAULT_STYLE,
  };
}

async function checkedFetch(apiKey, body, attempt = 0) {
  const waitMs = Math.max(0, MIN_REQUEST_INTERVAL_MS - (Date.now() - lastRequestStartedAt));
  if (waitMs > 0) {
    console.log(`free-tier pacing; next request in ${(waitMs / 1000).toFixed(1)}s`);
    await new Promise(resolve => setTimeout(resolve, waitMs));
  }
  lastRequestStartedAt = Date.now();
  const response = await fetch(API_URL, {
    method: "POST",
    headers: {
      "x-goog-api-key": apiKey,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (response.ok) return response.json();

  const responseText = (await response.text()).slice(0, 1200);
  if (response.status === 429 && attempt < 12) {
    const messageDelay = Number(responseText.match(/retry in\s+([\d.]+)s/i)?.[1]);
    const headerDelay = Number(response.headers.get("retry-after"));
    const delaySeconds = Number.isFinite(messageDelay) && messageDelay > 0
      ? messageDelay
      : Number.isFinite(headerDelay) && headerDelay > 0
        ? headerDelay
        : Math.min(60, 2 ** attempt * 2);
    if (delaySeconds > 60) {
      const retryMinutes = Math.ceil(delaySeconds / 60);
      throw new Error(
        `Gemini free-tier quota window is exhausted. Retry in about ${retryMinutes} minutes, ` +
        "or enable a higher API tier. Existing WAV files are preserved and will be skipped on the next run."
      );
    }
    const delayMs = Math.ceil(delaySeconds * 1000) + 1000;
    console.log(`rate limit reached; retrying in ${(delayMs / 1000).toFixed(1)}s (attempt ${attempt + 1}/12)`);
    await new Promise(resolve => setTimeout(resolve, delayMs));
    return checkedFetch(apiKey, body, attempt + 1);
  }

  const advice = response.status === 400
    ? "Check the model, voice name, and request format."
    : response.status === 401 || response.status === 403
      ? "Check that the Gemini API key is current and has access to Gemini TTS."
      : response.status === 429
        ? "The Gemini API quota or rate limit was reached. Wait and retry; existing files will be skipped."
        : "Review the Gemini API response and Google AI Studio project status.";
  throw new Error(`Gemini API ${response.status}: ${responseText || response.statusText}\n${advice}`);
}

function extractAudio(payload) {
  const audioBlocks = (payload.steps ?? [])
    .filter(step => step?.type === "model_output")
    .flatMap(step => Array.isArray(step.content) ? step.content : [])
    .filter(content => content?.type === "audio" && content.data);
  const encodedAudio = audioBlocks.at(-1)?.data ?? payload.output_audio?.data;
  if (!encodedAudio) throw new Error("Gemini response did not include an audio block.");
  return Buffer.from(encodedAudio, "base64");
}

function verifyWav(audio, text) {
  const isWav = audio.length >= 44 && audio.subarray(0, 4).toString("ascii") === "RIFF" && audio.subarray(8, 12).toString("ascii") === "WAVE";
  if (!isWav) throw new Error(`Generated audio for ${text} was not a complete WAV file.`);
}

async function synthesize(config, phrase) {
  const outputPath = path.join(outputDirectory, phrase.file);
  const temporaryPath = `${outputPath}.tmp`;
  if (!force) {
    try {
      const existing = await stat(outputPath);
      if (existing.size >= 44) {
        console.log(`skip ${phrase.file} (already exists)`);
        return { ...phrase, bytes: existing.size, skipped: true };
      }
    } catch {
      // File does not exist yet.
    }
  }

  const payload = await checkedFetch(config.apiKey, {
    model: MODEL,
    input: [{
      type: "user_input",
      content: [{
        type: "text",
        text: phrase.text,
        annotations: [{ type: "speech_metadata", style: config.style }],
      }],
    }],
    response_format: { type: "audio", mime_type: "audio/wav", sample_rate: 24000 },
    generation_config: { speech_config: [{ voice: config.voice }] },
  });
  const audio = extractAudio(payload);
  verifyWav(audio, phrase.text);
  await writeFile(temporaryPath, audio);
  await rename(temporaryPath, outputPath);
  console.log(`wrote ${phrase.file} (${audio.length} bytes)`);
  return { ...phrase, bytes: audio.length, skipped: false };
}

async function main() {
  const config = configuration();
  await mkdir(outputDirectory, { recursive: true });
  console.log(`Using ${config.voice} with ${MODEL}`);

  const generated = [];
  for (const phrase of phrases) generated.push(await synthesize(config, phrase));

  const manifest = {
    provider: "Google Gemini",
    model: MODEL,
    voice: config.voice,
    style: config.style,
    language: "Korean (auto-detected)",
    generated_at: new Date().toISOString(),
    output: { audio_format: "wav", mime_type: "audio/wav", sample_rate: 24000, channels: 1, bit_depth: 16 },
    files: generated.map(({ file, text, bytes }) => ({ file, text, bytes })),
  };
  const manifestPath = path.join(outputDirectory, "manifest.json");
  await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
  console.log(`Done. Manifest: ${path.relative(projectRoot, manifestPath)}`);
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
