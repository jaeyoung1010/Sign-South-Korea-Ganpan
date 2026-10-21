import assert from 'node:assert/strict';
import test from 'node:test';
import { collectRequirementIds, validateTraceability } from './phase43-traceability.mjs';

const validFixture = {
  requirementSources: ['F-APP-01 AC-001'],
  design: '### E2E-01 신규 사용자',
  manifest: [
    'F-*|43|docs/trace.md|tests/E2E.kt#e2e01',
    'AC-*|43|docs/trace.md|tests/E2E.kt#e2e01',
    'E2E-01|43|tests/E2E.kt|tests/E2E.kt#e2e01',
  ].join('\n'),
  codeTags: 'F-APP-01 AC-001 E2E-01',
  rootDirectory: '/repo',
  fileExists: (path) => ['/repo/docs/trace.md', '/repo/tests/E2E.kt'].includes(path),
  fileContents: (path) => (path.endsWith('E2E.kt') ? 'fun e2e01() {}' : ''),
};

test('collects unique requirement IDs from multiple sources', () => {
  assert.deepEqual(
    collectRequirementIds('F-APP-01 AC-001', 'F-APP-01 NFR-OFF-001'),
    ['AC-001', 'F-APP-01', 'NFR-OFF-001'],
  );
});

test('passes when every requirement and E2E scenario has trace and code evidence', () => {
  const result = validateTraceability(validFixture);

  assert.equal(result.errors.length, 0);
  assert.equal(result.requirementCount, 2);
  assert.equal(result.e2eCount, 1);
});

test('reports missing requirement tags, trace rows, and test methods', () => {
  const result = validateTraceability({
    ...validFixture,
    requirementSources: ['F-APP-01 AC-001 NFR-OFF-001'],
    codeTags: 'F-APP-01 AC-001 E2E-01',
    manifest: 'F-*|43|docs/trace.md|tests/E2E.kt#missing',
  });

  assert.deepEqual(result.errors, [
    'Missing code tag: NFR-OFF-001',
    'Missing trace row: AC-001',
    'Missing trace row: NFR-OFF-001',
    'Missing trace row: E2E-01',
    'Missing test method: tests/E2E.kt#missing',
  ]);
});

test('does not treat a longer requirement ID as an exact code tag', () => {
  const result = validateTraceability({
    ...validFixture,
    requirementSources: ['AC-01'],
    codeTags: 'AC-010 E2E-01',
  });

  assert.deepEqual(result.errors, ['Missing code tag: AC-01']);
});

test('rejects a self-referential E2E trace row', () => {
  const result = validateTraceability({
    ...validFixture,
    manifest: 'E2E-01|43|tests/E2E.kt|tests/Phase43E2ETraceTest.kt#e2e01',
    fileExists: () => true,
    fileContents: () => 'fun e2e01() {}',
  });

  assert.deepEqual(result.errors, ['Missing trace row: AC-001', 'Missing trace row: F-APP-01', 'E2E trace must reference a flow test: E2E-01']);
});
