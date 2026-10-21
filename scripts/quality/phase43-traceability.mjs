import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const requirementPattern = /\b(?:FR|NFR|AC|F)-[A-Z0-9]+(?:-[A-Z0-9]+)*\b/g;
const e2ePattern = /\bE2E-\d{2}\b/g;

export function collectRequirementIds(...sources) {
  return [...new Set(sources.flatMap((source) => source.match(requirementPattern) ?? []))].sort();
}

export function collectE2eIds(...sources) {
  return [...new Set(sources.flatMap((source) => source.match(e2ePattern) ?? []))].sort();
}

export function parseTraceManifest(manifest) {
  return manifest
    .split(/\r?\n/u)
    .map((line) => line.trim())
    .filter((line) => line.length > 0 && !line.startsWith('#'))
    .map((line) => {
      const [idPattern, phase, evidencePath, testReference] = line.split('|').map((value) => value.trim());
      return { idPattern, phase, evidencePath, testReference };
    });
}

function matches(pattern, id) {
  return pattern === id || (pattern?.endsWith('*') && id.startsWith(pattern.slice(0, -1)));
}

function containsToken(text, token) {
  const escaped = token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`(?:^|[^A-Za-z0-9-])${escaped}(?:$|[^A-Za-z0-9-])`, 'u').test(text);
}

function splitTestReference(reference) {
  const separator = reference.indexOf('#');
  return separator === -1
    ? { filePath: reference, method: null }
    : { filePath: reference.slice(0, separator), method: reference.slice(separator + 1) };
}

export function validateTraceability({
  requirementSources,
  design,
  manifest,
  codeTags,
  rootDirectory = process.cwd(),
  fileExists = fs.existsSync,
  fileContents = (filePath) => fs.readFileSync(filePath, 'utf8'),
}) {
  const errors = [];
  const requirementIds = collectRequirementIds(...requirementSources);
  const e2eIds = collectE2eIds(design);
  const rows = parseTraceManifest(manifest);

  for (const id of requirementIds) {
    if (!containsToken(codeTags, id)) errors.push(`Missing code tag: ${id}`);
  }

  for (const id of [...requirementIds, ...e2eIds]) {
    if (!rows.some((row) => matches(row.idPattern, id))) {
      errors.push(`Missing trace row: ${id}`);
    }
  }

  for (const row of rows) {
    const evidenceFile = path.resolve(rootDirectory, row.evidencePath ?? '');
    if (!row.evidencePath || !fileExists(evidenceFile)) {
      errors.push(`Missing evidence file: ${row.evidencePath}`);
    }

    const { filePath, method } = splitTestReference(row.testReference ?? '');
    const testFile = path.resolve(rootDirectory, filePath);
    if (row.idPattern?.startsWith('E2E-') && filePath.endsWith('Phase43E2ETraceTest.kt')) {
      errors.push(`E2E trace must reference a flow test: ${row.idPattern}`);
    }
    if (!filePath || !fileExists(testFile)) {
      errors.push(`Missing test file: ${row.testReference}`);
    } else if (method && !fileContents(testFile).includes(method)) {
      errors.push(`Missing test method: ${row.testReference}`);
    }
  }

  return {
    errors,
    requirementCount: requirementIds.length,
    e2eCount: e2eIds.length,
    traceCount: rows.length,
  };
}

function readRepositoryFile(rootDirectory, relativePath) {
  return fs.readFileSync(path.join(rootDirectory, relativePath), 'utf8');
}

function run() {
  const rootDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
  const result = validateTraceability({
    requirementSources: [
      readRepositoryFile(rootDirectory, 'docs/android-implementation-plan.md'),
      readRepositoryFile(rootDirectory, 'docs/functional-specification.md'),
      readRepositoryFile(rootDirectory, 'docs/PRD-SRS.md'),
    ],
    design: readRepositoryFile(rootDirectory, 'docs/android-implementation-design.md'),
    manifest: readRepositoryFile(rootDirectory, 'docs/phase43-traceability.tsv'),
    codeTags: readRepositoryFile(rootDirectory, 'app/src/androidTest/java/com/signkorean/app/Phase43RequirementTags.kt'),
    rootDirectory,
  });

  if (result.errors.length > 0) {
    console.error('Phase 43 traceability validation failed:\n' + result.errors.map((error) => `- ${error}`).join('\n'));
    process.exitCode = 1;
    return;
  }

  console.log('Phase 43 traceability validation passed.');
  console.log(`Requirements: ${result.requirementCount}, E2E scenarios: ${result.e2eCount}, trace rows: ${result.traceCount}.`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) run();
