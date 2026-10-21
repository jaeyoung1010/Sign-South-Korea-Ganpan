import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const allowedStatuses = new Set(['automated', 'ci', 'manual-pass', 'manual-pending']);
const blockerPattern = /P0\/P1 blockers:\s*(\d+)/u;

export function parseQaManifest(manifest) {
  return manifest
    .split(/\r?\n/u)
    .map((line) => line.trim())
    .filter((line) => line.length > 0 && !line.startsWith('#'))
    .map((line) => {
      const [caseId, area, requirement, testReference, evidence, status] = line
        .split('|')
        .map((value) => value.trim());
      return { caseId, area, requirement, testReference, evidence, status };
    });
}

function splitReference(reference) {
  const separator = reference.indexOf('#');
  return separator === -1
    ? { filePath: reference, anchor: null }
    : { filePath: reference.slice(0, separator), anchor: reference.slice(separator + 1) };
}

function referenceFile(reference, rootDirectory) {
  return path.resolve(rootDirectory, splitReference(reference).filePath);
}

export function validateQaMatrix({
  manifest,
  requiredCases,
  rootDirectory = process.cwd(),
  fileExists = fs.existsSync,
  fileContents = (filePath) => fs.readFileSync(filePath, 'utf8'),
  blockerEvidence = '',
}) {
  const errors = [];
  const rows = parseQaManifest(manifest);
  const seenCases = new Set();

  for (const row of rows) {
    if (!row.caseId) {
      errors.push('Missing case ID');
      continue;
    }
    if (seenCases.has(row.caseId)) errors.push(`Duplicate case: ${row.caseId}`);
    seenCases.add(row.caseId);

    for (const field of ['area', 'requirement', 'testReference', 'evidence', 'status']) {
      if (!row[field]) errors.push(`Missing ${field}: ${row.caseId}`);
    }

    const test = splitReference(row.testReference ?? '');
    const testFile = referenceFile(row.testReference ?? '', rootDirectory);
    if (!test.filePath || !fileExists(testFile)) {
      errors.push(`Missing test file: ${row.testReference ?? ''}`);
    } else if (test.anchor && !fileContents(testFile).includes(test.anchor)) {
      errors.push(`Missing test reference: ${row.testReference}`);
    }

    const evidence = splitReference(row.evidence ?? '');
    const evidenceFile = referenceFile(row.evidence ?? '', rootDirectory);
    if (!evidence.filePath || !fileExists(evidenceFile)) {
      errors.push(`Missing evidence file: ${row.evidence ?? ''}`);
    } else if (evidence.anchor && !fileContents(evidenceFile).includes(evidence.anchor)) {
      errors.push(`Missing evidence anchor: ${row.evidence}`);
    }

    if (row.status && !allowedStatuses.has(row.status)) errors.push(`Invalid status: ${row.status}`);
  }

  for (const requiredCase of requiredCases) {
    if (!seenCases.has(requiredCase)) errors.push(`Missing required case: ${requiredCase}`);
  }

  const blockerMatch = blockerPattern.exec(blockerEvidence);
  if (blockerMatch && Number(blockerMatch[1]) !== 0) {
    errors.push(`Release blockers must be zero: ${blockerMatch[1]}`);
  }

  return {
    errors,
    rowCount: rows.length,
    pendingManualCount: rows.filter((row) => row.status === 'manual-pending').length,
  };
}

function readRepositoryFile(rootDirectory, relativePath) {
  return fs.readFileSync(path.join(rootDirectory, relativePath), 'utf8');
}

function run() {
  const rootDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
  const result = validateQaMatrix({
    manifest: readRepositoryFile(rootDirectory, 'docs/phase44-qa-matrix.tsv'),
    requiredCases: [
      'QA-01',
      'QA-02',
      'QA-03',
      'QA-04',
      'QA-05',
      'QA-06',
      'QA-07',
      'QA-08',
      'QA-09',
      'QA-10',
      'QA-11',
      'QA-12',
      'QA-13',
      'QA-14',
    ],
    blockerEvidence: readRepositoryFile(rootDirectory, 'docs/phase44-device-qa.md'),
    rootDirectory,
  });

  if (result.errors.length > 0) {
    console.error('Phase 44 QA matrix validation failed:\n' + result.errors.map((error) => `- ${error}`).join('\n'));
    process.exitCode = 1;
    return;
  }

  console.log('Phase 44 QA matrix validation passed.');
  console.log(`Cases: ${result.rowCount}, manual evidence pending: ${result.pendingManualCount}.`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) run();
