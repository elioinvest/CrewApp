# Crew 手机端

**简体中文** | [English](README.en.md)

在手机上使用电脑里的 [Crew](https://github.com/elioinvest/Crew)：随时随地查看、回复 Claude Code 和 Codex 会话，并处理审批请求。

| 客户端 | 平台 | 会话展示方式 |
| --- | --- | --- |
| [`android/`](android) | 原生 Android 应用（Kotlin、Jetpack Compose），Android 8.0+ | 聊天 |
| [`www/`](www) | 由 Crew 提供的网页版，支持任意手机浏览器（含 iPhone） | 终端 |

## 功能（Android 应用）

- **聊天视图**：你的消息显示为气泡，智能体的回复渲染为 Markdown，执行过的工具折叠为“Ran 3 steps”（点击展开）。
- **实时状态**：随时看到智能体在做什么（“Thinking… (12s)”）。权限确认、选项等编号问题会变成按钮。
- **输入栏**：与 Crew 相同的模型、推理强度和模式选择。**＋** 向电脑发送照片和文件，**/** 执行智能体命令；智能体运行时发送按钮变为 **■** 停止。
- **会话**：在任意项目中用 Claude 或 Codex 新建对话，也可以直接阅读历史会话而无需在 Crew 中打开。
- **侧边栏**：布局与 Crew 一致，包含项目、主机、@ 智能体、智能体运行记录和已配对的电脑。
- **审批**：`crew-ssh` 请求以卡片形式出现（允许、允许 15 分钟、拒绝）。
- **通知**：应用在后台保持连接，Crew 需要确认或任务完成时通知你。

## 快速开始

### 配对

1. 在电脑上的 Crew 中打开 **Settings → Phone access**，勾选 **Let paired phones connect**。
2. 在应用中点击 **Scan QR code** 扫描二维码，或手动输入地址（如 `192.168.1.20:7788`）和 Crew 中显示的配对码。

手机和电脑需要连接同一 Wi-Fi；如果两端都安装了 [Tailscale](https://tailscale.com)，则可在任何网络下使用。应用会记住配对过的电脑。

iPhone 或其他手机：用相机扫描 Crew 的二维码，即可在浏览器中打开网页版。

### 构建 Android 应用

环境要求：JDK 17+ 和 Android SDK（platform 35、build tools 35），并在 `android/local.properties` 中设置 `sdk.dir`，或设置 `ANDROID_HOME`。

```sh
cd android
gradlew assembleRelease     # 或：npm run apk
```

APK 输出到 `android/app/build/outputs/apk/release/app-release.apk`。它使用调试密钥签名，可直接安装；正式发布请换成自己的密钥。`gradlew assembleDebug` 可构建调试版。

### 开发网页版

```sh
npm install
npm run vendor   # 将 xterm.js 复制到 www/vendor
npm run serve    # 本地预览：http://localhost:8090
npm run crew     # 将 www 复制到 Crew（../Crew/phone）
```

## 工作原理

应用通过 WebSocket 连接 Crew 的手机服务（`remote.js`），并使用配对码认证。Crew 读取每个会话的记录文件（由 Claude Code 或 Codex 写入），转换为聊天内容发送给手机；同时跟踪每个智能体的终端画面，报告它正在做什么或在询问什么。你发送的消息会被输入到 Crew 中对应会话的终端，就像在电脑上亲手输入一样。

## 项目结构

```
android/   原生 Android 应用
www/       网页版（无需打包工具，www/ 即完整应用）
scripts/   vendor.js 复制 xterm.js；to-crew.js 将 www 复制到 Crew
```

## 致谢

- [OkHttp](https://square.github.io/okhttp/)：WebSocket 客户端
- [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded)：二维码扫描，无需 Google Play 服务
- [xterm.js](https://xtermjs.org/)：网页版终端渲染

## 许可证

[MIT](LICENSE)
