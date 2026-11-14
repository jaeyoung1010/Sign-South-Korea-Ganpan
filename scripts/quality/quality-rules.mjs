import fs from 'node:fs';
import path from 'node:path';

const ignoredDirectoryNames = new Set([
  '.git',
  '.gradle',
  '.idea',
  'build',
  'node_modules',
]);

const textExtensions = new Set([
  '',
  '.env',
  '.html',
  '.java',
  '.js',
  '.json',
  '.kts',
  '.kt',
  '.md',
  '.mjs',
  '.properties',
  '.toml',
  '.txt',
  '.xml',
  '.yaml',
  '.yml',
]);

const placeholderFragments = [
  'change_me',
  'changeme',
  'example',
  'placeholder',
  'replace_me',
  'replace_with',
  'your_',
];

export function collectTextFiles(rootDirectory) {
  const files = [];

  function visit(directory) {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      if (entry.isDirectory() && ignoredDirectoryNames.has(entry.name)) continue;

      const absolutePath = path.join(directory, entry.name);
      if (entry.isDirectory()) {
        visit(absolutePath);
      } else if (
        entry.isFile() &&
        (entry.name === '.env' ||
          entry.name.startsWith('.env.') ||
          textExtensions.has(path.extname(entry.name)))
      ) {
        files.push(absolutePath);
      }
    }
  }

  visit(rootDirectory);
  return files.sort();
}

export function findDynamicVersions(text) {
  const findings = [];
  const lines = text.split(/\r?\n/u);
  const quotedPattern = /(["'])([^"'\r\n]*)\1/gu;
  const dynamicPattern = /(?:\+|latest\.(?:release|integration)|-SNAPSHOT)/iu;

  lines.forEach((line, index) => {
    if ([...line.matchAll(quotedPattern)].some((match) => dynamicPattern.test(match[2]))) {
      findings.push({ line: index + 1, value: line.trim() });
    }
  });
  return findings;
}
function isPlaceholder(value) {
  const normalized = value.toLowerCase();
  return placeholderFragments.some((fragment) => normalized.includes(fragment));
}

export function findLikelySecrets(text) {
  const findings = [];
  const lines = text.split(/\r?\n/u);
  const tokenPatterns = [
    { kind: 'OpenAI-style token', pattern: /\bsk-[A-Za-z0-9_-]{20,}\b/u },
    { kind: 'Google API key', pattern: /\bAIza[A-Za-z0-9_-]{30,}\b/u },
    { kind: 'Typecast-style token', pattern: /\b__[A-Za-z0-9_-]{40,}\b/u },
  ];
  const assignmentPattern =
    /(?:api[_-]?key|access[_-]?token|auth[_-]?token|secret|password)\s*[:=]\s*(?:["']([^"']{12,})["']|([A-Za-z0-9_+/=-]{16,}))/iu;

  lines.forEach((line, index) => {
    for (const { kind, pattern } of tokenPatterns) {
      if (pattern.test(line)) findings.push({ kind, line: index + 1 });
    }

    const assignment = assignmentPattern.exec(line);
    const assignedValue = assignment?.[1] ?? assignment?.[2];
    if (assignedValue && !isPlaceholder(assignedValue)) {
      findings.push({ kind: 'credential assignment', line: index + 1 });
    }
  });

  return findings;
}
