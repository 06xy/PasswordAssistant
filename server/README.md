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
- `DISABLE_REGISTER=1`：关闭公开注册接口（生产环境建议开启，令牌由管理员分配）

## pm2 部署

```bash
# 服务器首次安装（进入 server 目录）
npm install --omit=dev
npm install -g pm2

# 设置预置令牌（生产环境由管理员分配，不要写进代码仓库）
export DEFAULT_TOKEN="你的同步令牌"

# 启动（使用仓库内生态文件，已配置生产参数与自动重启；
# 注意必须是 .cjs 后缀，因为 package.json 声明了 "type": "module"）
pm2 start ecosystem.config.cjs

# 保存进程列表并配置开机自启（会输出一条 systemd 命令，复制执行即可）
pm2 save
pm2 startup

# 常用管理命令
pm2 status
pm2 logs password-assistant-server
pm2 restart password-assistant-server
```

> 注意：`DEFAULT_TOKEN` 通过环境变量注入；如果在 `.bashrc` 或 shell 配置文件里设置，`pm2 startup` 重启后仍然生效。生产环境必须在 pm2 之前放置 HTTPS 反向代理（Caddy / Nginx），客户端默认地址 `https://backup.06xy.cn` 走 TLS。

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
