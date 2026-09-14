#!/usr/bin/env node
/**
 * Capacitor 9 removed-API guard for plugin native sources.
 *
 * Fails when Cap 9-removed APIs appear in Android/iOS plugin code.
 * Does not enforce Cordova SPM policy (Cordova product deps in Package.swift are allowed).
 *
 * Usage:
 *   node scripts/check-cap9-deprecated.mjs
 *   node scripts/check-cap9-deprecated.mjs --dir path
 */

import fs from "node:fs";
import path from "node:path";

const SKIP_DIRS = new Set([
  "node_modules",
  "dist",
  "build",
  ".build",
  ".gradle",
  "Pods",
  "DerivedData",
  ".swiftpm",
  ".git",
]);

/** @type {{ id: string, hint: string, exts: string[], re: RegExp }[]} */
const RULES = [
  {
    id: "android-native-plugin-annotation",
    hint: "Use @CapacitorPlugin instead of @NativePlugin",
    exts: [".java", ".kt"],
    re: /@NativePlugin\b/,
  },
  {
    id: "android-plugin-call-hasOption",
    hint: "Use typed PluginCall accessors (getString, getBoolean, etc.)",
    exts: [".java", ".kt"],
    re: /\.hasOption\s*\(/,
  },
  {
    id: "android-plugin-call-save",
    hint: "Use PluginCall.setKeepAlive(true)",
    exts: [".java", ".kt"],
    re: /\bcall\.save\s*\(\s*\)/,
  },
  {
    id: "android-plugin-call-isSaved",
    hint: "Use PluginCall.isKeptAlive()",
    exts: [".java", ".kt"],
    re: /\bcall\.isSaved\s*\(\s*\)/,
  },
  {
    id: "android-plugin-call-isReleased",
    hint: "Released calls are managed by the bridge; do not call isReleased()",
    exts: [".java", ".kt"],
    re: /\bcall\.isReleased\s*\(\s*\)/,
  },
  {
    id: "android-plugin-getConfigValue",
    hint: "Use getConfig() and PluginConfig typed accessors",
    exts: [".java", ".kt"],
    re: /\bgetConfigValue\s*\(/,
  },
  {
    id: "android-plugin-saveCall",
    hint: "Use Bridge.saveCall(PluginCall) or PluginCall.setKeepAlive(true)",
    exts: [".java", ".kt"],
    re: /\bsaveCall\s*\(\s*call\s*\)/,
  },
  {
    id: "android-plugin-getSavedCall-no-arg",
    hint: "Use Bridge.getSavedCall(String)",
    exts: [".java", ".kt"],
    re: /\bgetSavedCall\s*\(\s*\)/,
  },
  {
    id: "android-plugin-freeSavedCall",
    hint: "Use PluginCall.release(Bridge)",
    exts: [".java", ".kt"],
    re: /\bfreeSavedCall\s*\(/,
  },
  {
    id: "android-bridge-https-interceptor",
    hint: "Use CAPACITOR_HTTP_INTERCEPTOR_START",
    exts: [".java", ".kt"],
    re: /\bCAPACITOR_HTTPS_INTERCEPTOR_START\b/,
  },
  {
    id: "ios-plugin-call-hasOption",
    hint: "Use typed CAPPluginCall accessors (getString, getBool, etc.)",
    exts: [".swift"],
    re: /\.hasOption\s*\(/,
  },
  {
    id: "ios-cap-notifications-enum",
    hint: "Use Notification.Name.capacitor* constants",
    exts: [".swift"],
    re: /\bCAPNotifications\b/,
  },
  {
    id: "ios-plugin-getConfigValue",
    hint: "Use getConfig() and PluginConfig typed accessors",
    exts: [".swift"],
    re: /\bgetConfigValue\s*\(/,
  },
  {
    id: "ios-instance-getPluginConfigValue",
    hint: "Use getPluginConfig(_:)",
    exts: [".swift"],
    re: /\bgetPluginConfigValue\s*\(/,
  },
  {
    id: "ios-cap-bridge-shim",
    hint: "CAPBridge compatibility shim was removed; use ApplicationDelegateProxy / Notification.Name.capacitor*",
    exts: [".swift"],
    re: /\bCAPBridge\./,
  },
  {
    id: "ios-getPortablePath",
    hint: "Use bridge.portablePath(fromLocalURL:)",
    exts: [".swift"],
    re: /\bgetPortablePath\s*\(/,
  },
  {
    id: "ios-https-interceptor",
    hint: "Use httpInterceptorStartIdentifier",
    exts: [".swift"],
    re: /\bhttpsInterceptorStartIdentifier\b/,
  },
];

function readText(p) {
  try {
    return fs.readFileSync(p, "utf8");
  } catch {
    return "";
  }
}

function exists(p) {
  try {
    fs.accessSync(p);
    return true;
  } catch {
    return false;
  }
}

function walkFiles(rootDir, exts) {
  const out = [];
  const stack = [rootDir];
  while (stack.length) {
    const dir = stack.pop();
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const e of entries) {
      if (e.isDirectory()) {
        if (SKIP_DIRS.has(e.name)) continue;
        stack.push(path.join(dir, e.name));
        continue;
      }
      if (!e.isFile()) continue;
      for (const ext of exts) {
        if (e.name.endsWith(ext)) {
          out.push(path.join(dir, e.name));
          break;
        }
      }
    }
  }
  out.sort();
  return out;
}

function parseArgs(argv) {
  const out = { dir: process.cwd() };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--dir" || a === "--pluginDir") {
      out.dir = path.resolve(argv[++i] || ".");
      continue;
    }
  }
  return out;
}

function isIgnorableLine(filePath, line) {
  const trimmed = line.trim();
  if (!trimmed) return true;
  if (trimmed.startsWith("//")) return true;
  if (trimmed.startsWith("*")) return true;
  if (trimmed.startsWith("/*") || trimmed.startsWith("*/")) return true;

  const base = path.basename(filePath);
  if (base === "check-cap9-deprecated.mjs") return true;

  if (base === "Package.swift") {
    if (/product\s*\(\s*name:\s*"Cordova"/.test(line)) return true;
    if (/\.product\s*\(\s*name:\s*"Cordova"/.test(line)) return true;
  }

  return false;
}

function scanFile(filePath, rulesForFile) {
  const hits = [];
  const lines = readText(filePath).split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (isIgnorableLine(filePath, line)) continue;
    for (const rule of rulesForFile) {
      if (rule.re.test(line)) {
        hits.push({ rule, line: i + 1, text: line.trim() });
      }
    }
  }
  return hits;
}

const args = parseArgs(process.argv);
const pluginDir = args.dir;

const scanRoots = [
  path.join(pluginDir, "android", "src", "main"),
  path.join(pluginDir, "ios", "Sources"),
  path.join(pluginDir, "ios"),
];

const allExts = [...new Set(RULES.flatMap((r) => r.exts))];
const files = [];
for (const root of scanRoots) {
  if (!exists(root)) continue;
  for (const f of walkFiles(root, allExts)) {
    if (f.includes(`${path.sep}Tests${path.sep}`) || f.includes(`${path.sep}Test${path.sep}`)) continue;
    if (f.endsWith("check-cap9-deprecated.mjs")) continue;
    files.push(f);
  }
}

/** @type {{ file: string, rule: typeof RULES[number], line: number, text: string }[]} */
const violations = [];

for (const file of files) {
  const ext = path.extname(file);
  const rulesForFile = RULES.filter((r) => r.exts.includes(ext));
  if (!rulesForFile.length) continue;
  for (const hit of scanFile(file, rulesForFile)) {
    violations.push({ file, ...hit });
  }
}

if (violations.length) {
  console.error(`[cap9-deprecated] FAIL in ${pluginDir}`);
  for (const v of violations) {
    const rel = path.relative(pluginDir, v.file);
    console.error(`- ${rel}:${v.line} [${v.rule.id}] ${v.rule.hint}`);
    console.error(`  ${v.text}`);
  }
  process.exit(1);
}

console.log(`[cap9-deprecated] OK (${files.length} native source file(s) scanned)`);
process.exit(0);
