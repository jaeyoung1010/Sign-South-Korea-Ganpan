import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { collectTextFiles, findDynamicVersions, findLikelySecrets } from './quality-rules.mjs';

test('dotenv variants are included in credential scans', (context) => {
  const fixtureDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'sign-korean-quality-'));
  context.after(() => fs.rmSync(fixtureDirectory, { recursive: true, force: true }));

  const dotenvVariant = path.join(fixtureDirectory, '.env.production');
  fs.writeFileSync(dotenvVariant, 'API_KEY=replace_with_your_key\n');

  assert.deepEqual(collectTextFiles(fixtureDirectory), [dotenvVariant]);
});

test('fixed dependency versions pass', () => {
  assert.deepEqual(findDynamicVersions('agp = "9.4.0"'), []);
});

test('dynamic dependency versions fail', () => {
  assert.equal(findDynamicVersions('implementation("sample:library:1.+")').length, 1);
  assert.equal(findDynamicVersions('version = "latest.release"').length, 1);
});

test('known placeholder credentials pass', () => {
  assert.deepEqual(findLikelySecrets('TYPECAST_API_KEY=replace_with_your_key'), []);
});

test('credential-shaped assignments fail without storing a real credential fixture', () => {
  const keyName = ['api', 'key'].join('_');
  const syntheticCredential = ['synthetic', 'credential', 'value'].join('_');
  assert.equal(findLikelySecrets(`${keyName} = "${syntheticCredential}"`).length, 1);
});

test('provider-shaped tokens fail without embedding a usable token', () => {
  const syntheticToken = `sk-${'x'.repeat(24)}`;
  assert.equal(findLikelySecrets(syntheticToken).length, 1);
});
