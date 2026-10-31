#!/usr/bin/env node

import { mkdir, readFile, rename, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const API_BASE_URL = "https://api.typecast.ai";
const MODEL = "ssfm-v30";
const LANGUAGE = "kor";
const VOICE_SEARCH_NAME = "은경";
const INTEGRATION_SOURCE = "api-page";
const GENERATED_BY = "codex";
const USER_AGENT = `typecast-direct/1 undici typecast-integration/1 (source=${INTEGRATION_SOURCE}; generated_by=${GENERATED_BY})`;

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDirectory, "..");
const outputDirectory = path.join(projectRoot, "assets", "audio", "eunkyung");
const force = process.argv.includes("--force");

try {
  process.loadEnvFile(path.join(projectRoot, ".env.local"));
} catch (error) {
  if (error?.code !== "ENOENT") throw error;
}

async function loadPhrases() {
  const contentPath = path.join(projectRoot, "assets", "content", "cards.base.json");
  const cards = JSON.parse(await readFile(contentPath, "utf8"));
  const files = new Set();

  return cards.map((card) => {
    if (!card.id?.trim() || !card.korean?.trim()) {
      throw new Error("Every learning card must provide a non-empty id and Korean text for Typecast synthesis.");
    }
    const file = `${card.id}.mp3`;
    if (files.has(file)) throw new Error(`Duplicate Typecast output filename: ${file}`);
    files.add(file);
    return { file, text: card.korean };
  });
}

function requireApiKey() {
  const apiKey = process.env.TYPECAST_API_KEY?.trim();
  if (!apiKey) {
    throw new Error(
      "TYPECAST_API_KEY is not set. Create a current Typecast API key, then run: " +
      "node scripts/generate-typecast-audio.mjs after saving the key in .env.local."
    );
  }
  return apiKey;
}

function requestHeaders(apiKey, includeJson = false) {
  return {
    "X-API-KEY": apiKey,
    "User-Agent": USER_AGENT,
    ...(includeJson ? { "Content-Type": "application/json" } : {}),
  };
}

async function checkedFetch(url, options) {
  const response = await fetch(url, options);
  if (response.ok) return response;

  const responseText = (await response.text()).slice(0, 800);
  const unusualActivity = response.status === 403 && responseText.includes("UNUSUAL_ACTIVITY_DETECTED");
  const advice = unusualActivity
    ? "Typecast blocked synthesis with its unusual-activity protection. Review the API account or plan in the Typecast API console before retrying."
    : response.status === 401
    ? "Check that the API key is current and copied without whitespace."
    : response.status === 402
      ? "The Typecast API account does not have enough credits."
      : response.status === 403
        ? "Use a current SSFM API key and confirm that the API account is active."
        : response.status === 429
          ? "The API rate limit was reached. Wait briefly and retry."
          : "Review the Typecast API response and request payload.";

  throw new Error(`Typecast API ${response.status}: ${responseText || response.statusText}\n${advice}`);
}

function voiceCollection(payload) {
  if (Array.isArray(payload)) return payload;
  for (const key of ["voices", "data", "items", "results"]) {
    if (Array.isArray(payload?.[key])) return payload[key];
  }
  return [];
}

function collectStrings(value, bucket = []) {
  if (typeof value === "string") bucket.push(value);
  else if (Array.isArray(value)) value.forEach(item => collectStrings(item, bucket));
  else if (value && typeof value === "object") Object.values(value).forEach(item => collectStrings(item, bucket));
  return bucket;
}

function voiceNames(voice) {
  const likelyNameFields = {
    voice_name: voice?.voice_name,
    name: voice?.name,
    display_name: voice?.display_name,
    localized_names: voice?.localized_names,
    names: voice?.names,
  };
  return [...new Set(collectStrings(likelyNameFields).map(value => value.trim()).filter(Boolean))];
}

async function findEunkyungVoice(apiKey) {
  if (process.env.TYPECAST_VOICE_ID?.trim()) {
    return {
      voiceId: process.env.TYPECAST_VOICE_ID.trim(),
      voiceName: VOICE_SEARCH_NAME,
      selection: "TYPECAST_VOICE_ID override",
    };
  }

  const url = new URL("/v3/voices", API_BASE_URL);
  url.searchParams.set("model", MODEL);
  url.searchParams.set("gender", "female");
  const response = await checkedFetch(url, { headers: requestHeaders(apiKey) });
  const voices = voiceCollection(await response.json());
  const matches = voices.filter(voice => {
    const names = voiceNames(voice);
    return names.some(name => name === VOICE_SEARCH_NAME || name.toLowerCase() === "eunkyung" || name.toLowerCase() === "eun-kyung") ||
      JSON.stringify(voice).includes(VOICE_SEARCH_NAME);
  });

  if (matches.length === 0) {
    const similar = voices
      .map(voice => ({ voice_id: voice.voice_id ?? voice.id, names: voiceNames(voice) }))
      .filter(voice => voice.names.some(name => /eun|은경/i.test(name)))
      .slice(0, 10);
    throw new Error(
      `Could not find an exact Typecast voice named ${VOICE_SEARCH_NAME}. ` +
      `Set TYPECAST_VOICE_ID after checking the API voice library.` +
      (similar.length ? ` Similar API results: ${JSON.stringify(similar)}` : "")
    );
  }

  if (matches.length > 1) {
    const choices = matches.map(voice => ({ voice_id: voice.voice_id ?? voice.id, names: voiceNames(voice) }));
    throw new Error(
      `More than one voice matched ${VOICE_SEARCH_NAME}. Set TYPECAST_VOICE_ID to the intended voice. ` +
      `Matches: ${JSON.stringify(choices)}`
    );
  }

  const voice = matches[0];
  const voiceId = voice.voice_id ?? voice.id;
  if (!voiceId) throw new Error(`The matching ${VOICE_SEARCH_NAME} voice did not include a voice_id.`);

  const detailResponse = await checkedFetch(
    new URL(`/v3/voices/${encodeURIComponent(voiceId)}`, API_BASE_URL),
    { headers: requestHeaders(apiKey) },
  );
  const detail = await detailResponse.json();
  const detailText = JSON.stringify(detail);
  if (!detailText.includes(MODEL)) {
    throw new Error(`${VOICE_SEARCH_NAME} (${voiceId}) does not advertise support for ${MODEL}.`);
  }

  return {
    voiceId,
    voiceName: voiceNames(voice).find(name => name === VOICE_SEARCH_NAME) ?? voiceNames(voice)[0] ?? VOICE_SEARCH_NAME,
    selection: "GET /v3/voices exact-name match",
  };
}

async function synthesize(apiKey, voiceId, phrase) {
  const outputPath = path.join(outputDirectory, phrase.file);
  const temporaryPath = `${outputPath}.tmp`;
  if (!force) {
    try {
      const existing = await stat(outputPath);
      if (existing.size > 0) {
        console.log(`skip ${phrase.file} (already exists)`);
        return { ...phrase, bytes: existing.size, skipped: true };
      }
    } catch {
      // File does not exist yet.
    }
  }

  const response = await checkedFetch(new URL("/v1/text-to-speech", API_BASE_URL), {
    method: "POST",
    headers: requestHeaders(apiKey, true),
    body: JSON.stringify({
      model: MODEL,
      voice_id: voiceId,
      text: phrase.text,
      language: LANGUAGE,
      prompt: {
        emotion_type: "preset",
        emotion_preset: "normal",
        emotion_intensity: 1.0,
      },
      output: {
        audio_format: "mp3",
        audio_tempo: 0.9,
        target_lufs: -14,
        remove_silence_ms: 120,
      },
    }),
  });

  const audio = Buffer.from(await response.arrayBuffer());
  if (audio.length < 1000) throw new Error(`Generated audio for ${phrase.text} was unexpectedly small.`);
  await writeFile(temporaryPath, audio);
  await rename(temporaryPath, outputPath);
  console.log(`wrote ${phrase.file} (${audio.length} bytes)`);
  return { ...phrase, bytes: audio.length, skipped: false };
}

async function main() {
  const apiKey = requireApiKey();
  const phrases = await loadPhrases();
  await mkdir(outputDirectory, { recursive: true });
  const voice = await findEunkyungVoice(apiKey);
  console.log(`Using ${voice.voiceName} (${voice.voiceId}) with ${MODEL}`);

  const generated = [];
  for (const phrase of phrases) {
    generated.push(await synthesize(apiKey, voice.voiceId, phrase));
  }

  const manifest = {
    provider: "Typecast",
    voice_id: voice.voiceId,
    voice_name: voice.voiceName,
    voice_selection: voice.selection,
    model: MODEL,
    language: LANGUAGE,
    source: INTEGRATION_SOURCE,
    generated_by: GENERATED_BY,
    generated_at: new Date().toISOString(),
    output: {
      audio_format: "mp3",
      audio_tempo: 0.9,
      target_lufs: -14,
      remove_silence_ms: 120,
    },
    files: generated.map(({ file, text, bytes }) => ({ file, text, bytes })),
  };
  await writeFile(path.join(outputDirectory, "manifest.json"), `${JSON.stringify(manifest, null, 2)}\n`);
  console.log(`Done. Manifest: ${path.relative(projectRoot, path.join(outputDirectory, "manifest.json"))}`);
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
