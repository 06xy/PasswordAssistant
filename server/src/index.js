import express from 'express';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, '..', 'data');
const PORT = Number(process.env.PORT || 3000);
const MAX_BODY_BYTES = 20 * 1024 * 1024; // 20 MB
const RATE_LIMIT_WINDOW_MS = 60_000;
const RATE_LIMIT_MAX = 120;

fs.mkdirSync(DATA_DIR, { recursive: true });

const vaultsFile = path.join(DATA_DIR, 'vaults.json');
const tokensFile = path.join(DATA_DIR, 'tokens.json');

function readJson(file, fallback) {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch {
    return fallback;
  }
}

function writeJson(file, data) {
  const tmp = `${file}.tmp`;
  fs.writeFileSync(tmp, JSON.stringify(data, null, 2));
  fs.renameSync(tmp, file);
}

function loadTokens() {
  const tokens = readJson(tokensFile, []);
  const envToken = process.env.DEFAULT_TOKEN;
  if (envToken && !tokens.includes(envToken)) {
    tokens.push(envToken);
    writeJson(tokensFile, tokens);
  }
  return tokens;
}

function tokenValid(token) {
  if (!token) return false;
  const candidates = loadTokens();
  const a = Buffer.from(token);
  return candidates.some((candidate) => {
    const b = Buffer.from(candidate);
    return a.length === b.length && crypto.timingSafeEqual(a, b);
  });
}

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: `${MAX_BODY_BYTES}b` }));
app.use((req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Cache-Control', 'no-store');
  next();
});

// 简易内存限流：按 IP 每分钟最多 RATE_LIMIT_MAX 次
const rateBuckets = new Map();
app.use((req, res, next) => {
  const ip = req.ip || 'unknown';
  const now = Date.now();
  const bucket = rateBuckets.get(ip) || { count: 0, resetAt: now + RATE_LIMIT_WINDOW_MS };
  if (now > bucket.resetAt) {
    bucket.count = 0;
    bucket.resetAt = now + RATE_LIMIT_WINDOW_MS;
  }
  bucket.count += 1;
  rateBuckets.set(ip, bucket);
  if (bucket.count > RATE_LIMIT_MAX) {
    return res.status(429).json({ error: '请求过于频繁，请稍后再试' });
  }
  next();
});

app.get('/health', (req, res) => {
  res.json({ ok: true, service: 'password-assistant-server' });
});

// 注册：生成一个新的同步令牌（一次性展示）
app.post('/api/register', (req, res) => {
  if (process.env.DISABLE_REGISTER === '1') {
    return res.status(403).json({ error: '注册已关闭，请使用服务器管理员分配的令牌' });
  }
  const token = crypto.randomBytes(24).toString('hex');
  const tokens = loadTokens();
  tokens.push(token);
  writeJson(tokensFile, tokens);
  res.status(201).json({ token });
});

function bearerToken(req) {
  const header = req.headers.authorization || '';
  const match = header.match(/^Bearer\s+(.+)$/i);
  return match ? match[1] : null;
}

function requireAuth(req, res, next) {
  if (!tokenValid(bearerToken(req))) {
    return res.status(401).json({ error: '无效的同步令牌' });
  }
  next();
}

// 上传加密快照（覆盖式，last-write-wins）
app.post('/api/vault', requireAuth, (req, res) => {
  const token = bearerToken(req);
  const { data, updatedAt } = req.body || {};
  if (typeof data !== 'string' || data.length === 0) {
    return res.status(400).json({ error: '缺少 data 字段' });
  }
  if (Buffer.byteLength(data, 'utf8') > MAX_BODY_BYTES) {
    return res.status(413).json({ error: '快照过大' });
  }
  const vaults = readJson(vaultsFile, {});
  vaults[token] = {
    data,
    updatedAt: Number(updatedAt) || Date.now(),
  };
  writeJson(vaultsFile, vaults);
  res.json({ ok: true });
});

// 下载加密快照
app.get('/api/vault', requireAuth, (req, res) => {
  const token = bearerToken(req);
  const vaults = readJson(vaultsFile, {});
  const entry = vaults[token];
  if (!entry) {
    return res.status(404).json({ error: '云端暂无备份' });
  }
  res.json(entry);
});

app.listen(PORT, () => {
  console.log(`password-assistant-server listening on http://0.0.0.0:${PORT}`);
  console.log(`data dir: ${DATA_DIR}`);
});
