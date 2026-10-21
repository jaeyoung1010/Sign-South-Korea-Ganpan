import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const RELEASE_PERMISSIONS = [
  'android.permission.POST_NOTIFICATIONS',
  'android.permission.ACCESS_NETWORK_STATE',
  'android.permission.WAKE_LOCK',
  'android.permission.RECEIVE_BOOT_COMPLETED',
  'android.permission.FOREGROUND_SERVICE',
  'com.signkorean.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION',
];

const RELEASE_DEBUG_MARKERS = ['DebugQaActivity', 'DebugResetReceiver', 'Interactive QA menu'];
const secretPatterns = [
  /\bsk-[A-Za-z0-9_-]{20,}\b/u,
  /\bAIza[A-Za-z0-9_-]{30,}\b/u,
  /\b__[A-Za-z0-9_-]{40,}\b/u,
];

export function parsePermissions(manifestText) {
  return [...manifestText.matchAll(/<uses-permission\b[^>]*\bandroid:name="([^"]+)"/gu)].map((match) => match[1]);
}

export function validateReleaseManifest(manifestText) {
  const errors = [];
  const permissions = parsePermissions(manifestText);
  for (const permission of permissions) {
    if (!RELEASE_PERMISSIONS.includes(permission)) errors.push(`Disallowed release permission: ${permission}`);
  }
  for (const marker of RELEASE_DEBUG_MARKERS) {
    if (manifestText.includes(marker)) errors.push(`Release manifest contains debug-only marker: ${marker}`);
  }
  return { errors, permissions };
}

export function validateBackupPolicy(manifestText, extractionRulesText) {
  const errors = [];
  const application = manifestText.match(/<application\b[^>]*>/u)?.[0] ?? '';
  if (!/android:allowBackup="false"/u.test(application)) errors.push('android:allowBackup must be false');
  if (!/android:fullBackupContent="false"/u.test(application)) errors.push('android:fullBackupContent must be false');
  if (!/android:dataExtractionRules="@xml\/data_extraction_rules"/u.test(application)) {
    errors.push('android:dataExtractionRules must reference data_extraction_rules');
  }
  if (!/<cloud-backup>[\s\S]*?<exclude\s+domain="root"\s+path="\."\s*\/>[\s\S]*?<\/cloud-backup>/u.test(extractionRulesText)) {
    errors.push('cloud-backup must exclude root data');
  }
  if (!/<device-transfer>[\s\S]*?<exclude\s+domain="root"\s+path="\."\s*\/>[\s\S]*?<\/device-transfer>/u.test(extractionRulesText)) {
    errors.push('device-transfer must exclude root data');
  }
  return { errors };
}

function normalizeDate(value) {
  return value.replace(/[–—]/gu, '-');
}

function parsePolicyMetadata(text) {
  const match = text.match(/Version\s+([^·\s]+)\s+·\s+Effective\s+(\d{4}[-–—]\d{2}[-–—]\d{2})/u);
  return match ? { version: match[1], effectiveDate: normalizeDate(match[2]) } : null;
}

export function validatePolicyGate({ privacyText, termsText, resourceText }) {
  const errors = [];
  const privacy = parsePolicyMetadata(privacyText);
  const terms = parsePolicyMetadata(termsText);
  const privacyVersion = resourceText.match(/name="policy_privacy_version">([^<]+)</u)?.[1];
  const termsVersion = resourceText.match(/name="policy_terms_version">([^<]+)</u)?.[1];
  const effectiveDate = resourceText.match(/name="policy_effective_date">([^<]+)</u)?.[1];

  if (!privacy) errors.push('Privacy policy version metadata is missing');
  if (!terms) errors.push('Terms of use version metadata is missing');
  if (!privacyVersion || !termsVersion || !effectiveDate) errors.push('Policy resource metadata is incomplete');
  if (privacy && privacy.version !== privacyVersion) errors.push('Privacy policy version does not match resources');
  if (terms && terms.version !== termsVersion) errors.push('Terms of use version does not match resources');
  if (privacy && normalizeDate(effectiveDate ?? '') !== privacy.effectiveDate) {
    errors.push('Privacy policy effective date does not match resources');
  }
  if (terms && normalizeDate(effectiveDate ?? '') !== terms.effectiveDate) {
    errors.push('Terms of use effective date does not match resources');
  }
  return { errors };
}

function unique(values) {
  return [...new Set(values)];
}

export function validateContentGate({ rootDirectory, readText = (filePath) => fs.readFileSync(filePath, 'utf8'), fileExists = fs.existsSync }) {
  const errors = [];
  const warnings = [];
  const contentRoot = path.join(rootDirectory, 'assets', 'content');
  const manifest = JSON.parse(readText(path.join(contentRoot, 'manifest.json')));
  const baseCards = JSON.parse(readText(path.join(contentRoot, 'cards.base.json')));
  const languageCards = Object.fromEntries(
    manifest.languages.map((language) => [language, JSON.parse(readText(path.join(rootDirectory, 'assets', manifest.localizedContent[language])))])
  );

  if (manifest.cardCount !== 20 || baseCards.length !== 20) errors.push('Release content must contain exactly 20 base cards');
  if (JSON.stringify(manifest.languages.slice().sort()) !== JSON.stringify(['en', 'ja'])) errors.push('Release content must include en and ja');
  const baseIds = baseCards.map((card) => card.id);
  if (unique(baseIds).length !== 20) errors.push('Release content contains duplicate base card IDs');

  for (const language of ['en', 'ja']) {
    const cards = languageCards[language] ?? [];
    if (cards.length !== 20) errors.push(`Release content must contain 20 ${language} cards`);
    if (JSON.stringify(cards.map((card) => card.cardId).sort()) !== JSON.stringify(baseIds.slice().sort())) {
      errors.push(`${language} card IDs do not match the base card IDs`);
    }
  }
  if (manifest.reviewStatus?.en !== 'native-english-reviewed' || manifest.reviewStatus?.ja !== 'native-japanese-reviewed') {
    errors.push('Release content native review statuses are incomplete');
  }
  if (Object.values(manifest.factReviewStatus ?? {}).some((status) => status !== 'korean-fact-reviewed')) {
    errors.push('Release content Korean fact review statuses are incomplete');
  }

  for (const card of baseCards) {
    const assetPaths = [card.imageAsset, card.typecastAudioAsset, card.geminiAudioAsset];
    for (const assetPath of assetPaths) {
      const absolutePath = path.join(rootDirectory, assetPath);
      if (!fileExists(absolutePath)) errors.push(`Missing release asset: ${assetPath}`);
    }
    if (card.image?.reviewStatus !== 'approved') warnings.push(`Image review is not approved: ${card.id}`);
  }

  for (const [provider, expectedExtension] of [['eunkyung', '.mp3'], ['gemini', '.wav']]) {
    const audioManifest = JSON.parse(readText(path.join(rootDirectory, 'assets', 'audio', provider, 'manifest.json')));
    if (audioManifest.files?.length !== 20) errors.push(`Release ${provider} audio manifest must contain 20 files`);
    for (const file of audioManifest.files ?? []) {
      if (!file.file.endsWith(expectedExtension)) errors.push(`Invalid ${provider} audio extension: ${file.file}`);
      if (!fileExists(path.join(rootDirectory, 'assets', 'audio', provider, file.file))) {
        errors.push(`Missing ${provider} audio asset: ${file.file}`);
      }
    }
  }
  return { errors, warnings, cardCount: baseCards.length, languageCount: manifest.languages.length, audioCount: 40 };
}

export function scanArtifactEntries(entries) {
  const findings = [];
  const seen = new Set();
  const add = (finding) => {
    if (!seen.has(finding)) {
      seen.add(finding);
      findings.push(finding);
    }
  };
  for (const content of Object.values(entries)) {
    const text = Buffer.isBuffer(content) ? content.toString('utf8') : String(content);
    for (const marker of RELEASE_DEBUG_MARKERS) {
      if (text.includes(marker)) add(`Release artifact contains debug-only marker: ${marker}`);
    }
    if (secretPatterns.some((pattern) => pattern.test(text))) add('Release artifact contains a credential-shaped token');
  }
  return findings;
}

function listAabEntries(aabPath) {
  return execFileSync('unzip', ['-Z1', aabPath], { encoding: 'utf8' }).split(/\r?\n/u).filter(Boolean);
}

function readAabTextEntries(aabPath) {
  const entries = {};
  for (const entry of listAabEntries(aabPath)) {
    if (!/\.(?:dex|json|xml|properties|txt|md|bin)$/iu.test(entry)) continue;
    entries[entry] = execFileSync('unzip', ['-p', aabPath, entry], {
      maxBuffer: 32 * 1024 * 1024,
    });
  }
  return entries;
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex');
}

function readArg(name, fallback) {
  const index = process.argv.indexOf(name);
  return index === -1 ? fallback : process.argv[index + 1];
}

function run() {
  const rootDirectory = path.resolve(readArg('--root', path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')));
  const aabPath = path.resolve(readArg('--aab', path.join(rootDirectory, 'app/build/outputs/bundle/release/app-release.aab')));
  const manifestPath = path.resolve(readArg('--manifest', path.join(rootDirectory, 'app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml')));
  const mappingPath = path.resolve(readArg('--mapping', path.join(rootDirectory, 'app/build/outputs/mapping/release/mapping.txt')));
  const reportPath = path.resolve(readArg('--report', path.join(rootDirectory, 'build/reports/phase45/release-report.json')));
  const requireSigned = process.argv.includes('--require-signed');
  const errors = [];
  const warnings = [];

  const manifestText = fs.readFileSync(manifestPath, 'utf8');
  errors.push(...validateReleaseManifest(manifestText).errors);
  errors.push(...validateBackupPolicy(manifestText, fs.readFileSync(path.join(rootDirectory, 'app/src/main/res/xml/data_extraction_rules.xml'), 'utf8')).errors);
  errors.push(...validatePolicyGate({
    privacyText: fs.readFileSync(path.join(rootDirectory, 'assets/legal/privacy-policy.md'), 'utf8'),
    termsText: fs.readFileSync(path.join(rootDirectory, 'assets/legal/terms-of-use.md'), 'utf8'),
    resourceText: fs.readFileSync(path.join(rootDirectory, 'app/src/main/res/values/strings.xml'), 'utf8'),
  }).errors);
  const content = validateContentGate({ rootDirectory });
  errors.push(...content.errors);
  warnings.push(...content.warnings);

  if (!fs.existsSync(mappingPath) || fs.statSync(mappingPath).size === 0) errors.push('R8 mapping.txt is missing or empty');
  const releaseNotesPath = path.join(rootDirectory, 'docs/release-notes.md');
  const releaseNotes = fs.existsSync(releaseNotesPath) ? fs.readFileSync(releaseNotesPath, 'utf8') : '';
  if (!/0\.1\.0/u.test(releaseNotes)) errors.push('Release notes do not declare version 0.1.0');
  if (!/Known issues/u.test(releaseNotes)) errors.push('Release notes must include a Known issues section');

  let signed = false;
  let artifactSha256 = null;
  if (!fs.existsSync(aabPath)) {
    errors.push(`AAB is missing: ${aabPath}`);
  } else {
    const entries = readAabTextEntries(aabPath);
    const allEntries = listAabEntries(aabPath);
    signed = allEntries.some((entry) => /^META-INF\/[^/]+\.(?:RSA|DSA|EC)$/iu.test(entry));
    errors.push(...scanArtifactEntries(entries));
    artifactSha256 = sha256File(aabPath);
    if (requireSigned && !signed) errors.push('Release AAB is not signed');
    if (!signed) warnings.push('Release AAB is unsigned; provide signing credentials before distribution');
  }

  const report = {
    versionName: '0.1.0',
    aab: aabPath,
    aabSha256: artifactSha256,
    signed,
    mapping: mappingPath,
    content: { cardCount: content.cardCount, languageCount: content.languageCount, audioCount: content.audioCount },
    warnings,
    errors,
  };
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);

  if (errors.length > 0) {
    console.error(`Phase 45 release gate failed. Report: ${reportPath}\n${errors.map((error) => `- ${error}`).join('\n')}`);
    process.exitCode = 1;
    return;
  }
  console.log(`Phase 45 release gate passed. AAB SHA-256: ${artifactSha256}`);
  if (warnings.length > 0) console.warn(warnings.map((warning) => `- ${warning}`).join('\n'));
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) run();
