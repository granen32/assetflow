import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const history = process.argv.includes("--history");
const git = (...args) => execFileSync("git", args, { cwd: root, maxBuffer: 64 * 1024 * 1024 });
const findings = [];
let checked = 0;
let binarySkipped = 0;

const patterns = [
  ["GitHub token", /\b(?:gh[pousr]_[A-Za-z0-9_]{25,}|github_pat_[A-Za-z0-9_]{30,})\b/],
  ["JWT value", /\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b/],
  ["private key", /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/],
  ["AWS access key", /\b(?:AKIA|ASIA)[A-Z0-9]{16}\b/],
  ["URL credentials", /https?:\/\/[^\s/:@]+:[^\s/@]+@/],
  ["personal Windows path", /[A-Za-z]:[\\/]+Users[\\/]+(?!Public\b|Default\b)[^\\/\s`]+/i],
  ["personal library link", /https:\/\/chatgpt\.com\/library/],
];

function inspect(file, bytes, revision = "worktree") {
  checked++;
  if (/(?:^|\/)\.env(?:\..*)?$/.test(file) && !file.endsWith(".env.example")) {
    findings.push({ revision, file, rule: "environment file" });
  }
  if (/^backend\/bin\//.test(file) || /\.(?:class|pem|key|p12|pfx|jks|keystore|log|db|sqlite3?|dump|backup)$/i.test(file)) {
    findings.push({ revision, file, rule: "generated or sensitive file" });
  }
  if (bytes.includes(0)) { binarySkipped++; return; }
  bytes.toString("utf8").split(/\r?\n/).forEach((line, index) => {
    for (const [rule, pattern] of patterns) {
      if (pattern.test(line)) findings.push({ revision, file, line: index + 1, rule });
    }
    const assignment = line.match(/(?:JWT_SECRET|jwt-secret)\s*[:=]\s*["']?([^\s"',}]+)/i);
    const generatedPowerShellKey = /^\s*\$env:JWT_SECRET\s*=\s*\[Convert\]::ToBase64String\(\$[A-Za-z_]\w*\)\s*$/.test(line);
    const knownTestKey = file === "backend/src/test/java/com/assetflow/auth/AuthIntegrationTest.java"
      && assignment?.[1] === "integration-test-secret-at-least-32-bytes";
    if (assignment && !assignment[1].startsWith("${") && !generatedPowerShellKey && !knownTestKey) {
      findings.push({ revision, file, line: index + 1, rule: "literal JWT signing secret" });
    }
  });
}

if (history) {
  const revisions = git("rev-list", "--all").toString("utf8").trim().split(/\r?\n/).filter(Boolean);
  const seen = new Set();
  for (const revision of revisions) {
    for (const entry of git("ls-tree", "-rz", "--full-tree", revision).toString("utf8").split("\0")) {
      const match = entry.match(/^\d+ blob ([a-f0-9]+)\t(.+)$/s);
      if (!match || seen.has(`${match[1]}:${match[2]}`)) continue;
      seen.add(`${match[1]}:${match[2]}`);
      inspect(match[2], git("cat-file", "blob", match[1]), revision.slice(0, 7));
    }
    const emails = git("show", "-s", "--format=%ae%n%ce", revision).toString("utf8").trim().split(/\r?\n/);
    if (emails.some(email => !email.endsWith("@users.noreply.github.com"))) {
      findings.push({ revision: revision.slice(0, 7), rule: "commit author/committer email is not GitHub noreply" });
    }
  }
} else {
  const files = new Set(git("ls-files", "-z", "--cached", "--others", "--exclude-standard").toString("utf8").split("\0").filter(Boolean));
  for (const file of files) {
    try { inspect(file, readFileSync(path.join(root, file))); }
    catch (error) { if (error.code !== "ENOENT") throw error; }
  }
}

// 민감한 값은 출력하지 않고 위치와 규칙만 보고한다.
console.log(JSON.stringify({ mode: history ? "history" : "worktree", checked, binarySkipped, findings }, null, 2));
process.exitCode = findings.length ? 1 : 0;
