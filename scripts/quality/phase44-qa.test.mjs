import assert from 'node:assert/strict';
import test from 'node:test';
import { validateQaMatrix } from './phase44-qa.mjs';

const validFixture = {
  manifest: [
    '# case|area|requirement|test reference|evidence|status',
    'QA-01|api-26-gesture|NFR-A11Y-005|tests/qa.kt#gesture|docs/qa.md#QA-01|manual-pending',
    'QA-02|assets|AC-021|tests/assets.kt#assets|docs/qa.md#QA-02|automated',
  ].join('\n'),
  requiredCases: ['QA-01', 'QA-02'],
  blockerEvidence: 'P0/P1 blockers: 0',
  rootDirectory: '/repo',
  fileExists: (filePath) => ['/repo/tests/qa.kt', '/repo/tests/assets.kt', '/repo/docs/qa.md'].includes(filePath),
  fileContents: (filePath) => {
    if (filePath.endsWith('qa.kt')) return 'fun gesture() {}';
    if (filePath.endsWith('assets.kt')) return 'fun assets() {}';
    return 'QA-01\nQA-02\nP0/P1 blockers: 0';
  },
};

test('passes a complete QA matrix with pending manual evidence', () => {
  const result = validateQaMatrix(validFixture);

  assert.deepEqual(result.errors, []);
  assert.equal(result.rowCount, 2);
  assert.equal(result.pendingManualCount, 1);
});

test('rejects missing required cases and duplicate case IDs', () => {
  const result = validateQaMatrix({
    ...validFixture,
    requiredCases: ['QA-01', 'QA-02', 'QA-03'],
    manifest: `${validFixture.manifest}\nQA-01|duplicate|AC-021|tests/assets.kt#assets|docs/qa.md#QA-01|automated`,
  });

  assert.deepEqual(result.errors, ['Duplicate case: QA-01', 'Missing required case: QA-03']);
});

test('rejects missing files, methods, evidence anchors, and invalid statuses', () => {
  const result = validateQaMatrix({
    ...validFixture,
    requiredCases: ['QA-01'],
    manifest: 'QA-01|api-26-gesture|NFR-A11Y-005|missing.kt#gesture|missing.md#QA-01|done',
    fileExists: () => false,
  });

  assert.deepEqual(result.errors, [
    'Missing test file: missing.kt#gesture',
    'Missing evidence file: missing.md#QA-01',
    'Invalid status: done',
  ]);
});

test('rejects a non-zero release blocker count', () => {
  const result = validateQaMatrix({
    ...validFixture,
    blockerEvidence: 'P0/P1 blockers: 1',
    fileContents: (filePath) => {
      if (filePath.endsWith('qa.md')) return 'QA-01\nQA-02\nP0/P1 blockers: 1';
      return validFixture.fileContents(filePath);
    },
  });

  assert.deepEqual(result.errors, ['Release blockers must be zero: 1']);
});

test('rejects an unknown case and a missing release blocker count', () => {
  const result = validateQaMatrix({
    ...validFixture,
    manifest: `${validFixture.manifest}\nQA-99|unexpected|AC-021|tests/assets.kt#assets|docs/qa.md#QA-01|automated`,
    blockerEvidence: 'QA checklist has not recorded the blocker count',
  });

  assert.deepEqual(result.errors, ['Unknown case: QA-99', 'Missing release blocker count']);
});
