import assert from 'node:assert/strict';
import test from 'node:test';
import {
  parsePermissions,
  scanArtifactEntries,
  validateBackupPolicy,
  validatePolicyGate,
  validateReleaseManifest,
} from './phase45-release.mjs';

const manifest = `
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="com.signkorean.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
  <application android:allowBackup="false" android:fullBackupContent="false" android:dataExtractionRules="@xml/data_extraction_rules" />
</manifest>`;

test('parses and accepts the release permission allowlist', () => {
  assert.deepEqual(parsePermissions(manifest), [
    'android.permission.POST_NOTIFICATIONS',
    'android.permission.ACCESS_NETWORK_STATE',
    'android.permission.WAKE_LOCK',
    'android.permission.RECEIVE_BOOT_COMPLETED',
    'android.permission.FOREGROUND_SERVICE',
    'com.signkorean.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION',
  ]);
  assert.deepEqual(validateReleaseManifest(manifest).errors, []);
});

test('rejects a permission outside the release allowlist', () => {
  const result = validateReleaseManifest(manifest.replace(
    '</manifest>',
    '<uses-permission android:name="android.permission.CAMERA" /></manifest>',
  ));

  assert.deepEqual(result.errors, ['Disallowed release permission: android.permission.CAMERA']);
});

test('requires backup disabled and complete data extraction exclusions', () => {
  assert.deepEqual(
    validateBackupPolicy(manifest, '<data-extraction-rules><cloud-backup><exclude domain="root" path="." /></cloud-backup><device-transfer><exclude domain="root" path="." /></device-transfer></data-extraction-rules>').errors,
    [],
  );
  assert.deepEqual(
    validateBackupPolicy(manifest.replace('allowBackup="false"', 'allowBackup="true"'), '<data-extraction-rules />').errors,
    ['android:allowBackup must be false', 'cloud-backup must exclude root data', 'device-transfer must exclude root data'],
  );
});

test('keeps policy versions and effective dates aligned', () => {
  const result = validatePolicyGate({
    privacyText: '# Privacy policy\nVersion 1.0 · Effective 2026-10-21',
    termsText: '# Terms of use\nVersion 1.0 · Effective 2026-10-21',
    resourceText: '<string name="policy_privacy_version">1.0</string><string name="policy_terms_version">1.0</string><string name="policy_effective_date">2026–10–21</string>',
  });

  assert.deepEqual(result.errors, []);
});

test('rejects secrets and debug-only content in a release artifact', () => {
  const result = scanArtifactEntries({
    'base/dex/classes.dex': 'DebugQaActivity sk-xxxxxxxxxxxxxxxxxxxxxxxx',
    'base/assets/content/manifest.json': '{}',
  });

  assert.deepEqual(result, [
    'Release artifact contains debug-only marker: DebugQaActivity',
    'Release artifact contains a credential-shaped token',
  ]);
});
