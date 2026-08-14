module.exports = {
  apps: [
    {
      name: 'password-assistant-server',
      script: 'src/index.js',
      cwd: __dirname,
      instances: 1,
      exec_mode: 'fork',
      autorestart: true,
      max_memory_restart: '200M',
      env: {
        NODE_ENV: 'production',
        PORT: 3000,
        // 生产环境建议：
        // 1. 关闭公开注册，改为管理员预置令牌
        // 2. 通过环境变量注入 DEFAULT_TOKEN（不要写死在仓库里）
        DISABLE_REGISTER: '1',
      },
    },
  ],
};
