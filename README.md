# 密码助手（PasswordAssistant）

一款 Android 原生密码记录应用，核心特色是**自定义密码存储模板**：每个分组就是一套模板，你可以为每个分组定义完全不同的字段（例如 SSH 分组包含服务器名称、IP、端口、用户名、密码、备注；QQ 分组只有 QQ 号码、昵称、密码、备注）。

技术栈：Kotlin + Jetpack Compose + Material 3 + Room + DataStore，原生 Android（minSdk 26 / targetSdk 35，兼容 Android 16）。

## 已实现功能

- 首页分组卡片（类似快捷指令的网格布局），显示图标、颜色、记录数；顶部左侧标题、右侧设置入口，无底部 Tab
- 分组管理：新建 / 编辑 / 删除，支持自定义图标、颜色、字段模板
- 字段模板：文本、数字、密码、多行文本四种类型；支持默认值、必填、排序、增删改
- **列表展示配置**：新建分组时可指定每条记录卡片上的主标题、副标题显示哪个字段
- 记录管理：按分组模板动态渲染表单，新增 / 编辑 / 删除记录
- 主题：跟随系统 / 浅色 / 深色，Material 3 设计
- 备份 / 恢复：全部数据导出为 zip（分组模板 + 记录 + 设置），支持一键恢复
- 首次启动自动创建 SSH 密码、QQ 密码两个示例分组
- 动效：页面转场、卡片按压缩放、列表项插入动画；release 构建启用 R8 混淆与资源压缩（APK 约 1.5 MB）

## 备份格式

备份为 zip 文件，包含：

```
backup.zip
├─ manifest.json    # 应用名、格式版本、导出时间
├─ settings.json    # 系统设置（如主题模式）
├─ groups.json      # 全部分组及字段模板定义
└─ entries.json     # 全部记录数据
```

该格式未来可直接复用于云同步：整个备份（或其中的密文块）加密后上传到云端，服务器只负责存取，无法解密。

## 构建与运行

环境要求：JDK 17、Android SDK（platform 35）。

命令行构建（debug APK）：

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

也可以直接用 Android Studio 打开本项目目录运行。`local.properties` 中的 `sdk.dir` 指向本机 SDK 路径，该文件已被 git 忽略。

## 项目结构

```
app/src/main/java/com/passwordassistant/app/
├─ PasswordApp.kt              # Application + 依赖容器 + 首次种子数据
├─ MainActivity.kt             # 入口 Activity
├─ data/
│  ├─ FieldType.kt             # 字段类型枚举（文本/数字/密码/多行）
│  ├─ FieldDefinition.kt       # 字段定义（模板的一项）
│  ├─ GroupEntity.kt           # 分组 = 模板
│  ├─ EntryEntity.kt           # 记录 = 按模板填写的值
│  ├─ GroupDao.kt / EntryDao.kt
│  ├─ AppDatabase.kt           # Room 数据库
│  ├─ SettingsRepository.kt    # DataStore 设置
│  └─ BackupManager.kt         # zip 导出 / 导入
└─ ui/
   ├─ MainScreen.kt            # 底部导航 + 路由
   ├─ theme/                   # Material 3 主题、分组图标与配色
   └─ screens/                 # 首页 / 分组详情 / 分组编辑 / 记录编辑 / 设置
```

## 后续路线

1. **安全加固**：主密码解锁（Argon2id 派生密钥 + AES-256-GCM）、Android Keystore 包装密钥、生物识别快速解锁、剪贴板自动清除、任务卡片模糊
2. **备份加密**：备份 zip 支持备份密码加密
3. **云同步（Node.js 后端）**：加密后的备份快照上传/下载。后端计划使用 Node.js + TypeScript，安全措施包括：
   - HTTPS 全链路加密，仅传输密文，服务端无法解密
   - JWT 登录鉴权 + 令牌轮换，接口限流与防暴力破解
   - 参数校验、SQL 注入防护（若用数据库）、CORS 白名单、安全响应头
   - 开源部署，支持官方服务器或自建服务器地址切换
4. **体验完善**：Android 自动填充（Autofill）、搜索、分组排序、密码生成器
