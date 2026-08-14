# 密码助手云同步后端

极简开源后端：只负责存取客户端上传的**加密快照**，服务器不保存任何明文，也无法解密。

## 启动

```bash
npm install
npm start
```

环境变量：

- `PORT`：监听端口，默认 `3000`
- `DATA_DIR`：数据目录，默认 `server/data`
- `DEFAULT_TOKEN`：预置一个固定同步令牌（可选，常用于官方服务器）

## 接口

- `POST /api/register`：注册，返回一个新同步令牌（`{ "token": "..." }`）
- `POST /api/vault`（`Authorization: Bearer <token>`）：上传快照，body 为 `{ "data": "<加密后的密文 base64 字符串>", "updatedAt": 毫秒时间戳 }`
- `GET /api/vault`（`Authorization: Bearer <token>`）：下载快照，返回 `{ "data": "...", "updatedAt": ... }`；无备份时返回 404
- `GET /health`：健康检查

## 安全说明

- 令牌使用 `crypto.timingSafeEqual` 常量时间比较，按 IP 限流（默认每分钟 120 次）
- 上传体限制 20 MB，响应头禁缓存
- 数据以 JSON 文件存储（`data/vaults.json`），生产环境建议改为数据库并启用自动备份
- **生产环境必须放在 HTTPS 反向代理（Caddy / Nginx）后面**，客户端默认地址 `https://backup.06xy.cn` 即走 TLS

## 与客户端配合

客户端「设置 → 云同步」填写服务器地址（默认 `https://backup.06xy.cn`）与令牌，即可上传/下载加密快照。
