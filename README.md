# 分贝检测仪 · Decibel Meter

轻量、离线、无广告的 Android 环境噪音检测工具。无需登录，不保存或上传录音。

**当前源码版本：V1.0.2**（Android 8.0 / API 26 及以上）。

## 功能

- 打开即测；首次使用申请麦克风权限。
- 实时分贝仪表盘、六级噪音状态、最低/最高/等效声级、最近约 60 秒趋势。
- 自定义超阈值颜色提示、环境参考限值、手动校准、暂停和重置。
- 深色/浅色/跟随系统主题，检测时屏幕常亮可选。
- 原生 AudioRecord + 本地 WebView，所有运算均在手机完成；退出前台即停止采集。

| 声级参考 (dB(A)) | 状态 |
|---|---|
| <35 | 非常安静 |
| 35–<50 | 安静 |
| 50–<65 | 正常 |
| 65–<80 | 吵闹 |
| 80–<95 | 非常吵闹 |
| ≥95 | 极度吵闹 |

以上为应用自定义分级，不是国家标准的正式分级。未经专业校准的数值为估算值，不能替代声级计或用于执法、职业暴露及医疗判断。

## 安装和构建

在 [Actions](https://github.com/gaojunwei/decibel-meter-android/actions) 中选择成功的 Android APK 构建记录，登录 GitHub 后下载 Artifacts 中的 DecibelMeter-debug，解压并安装 APK。尚未确认构建成功前，不表示已有可用安装包。调试 APK 不用于正式上架。

本地需要 JDK 17、Gradle 8.9、Android SDK 35；在 Android Studio 打开项目或执行 gradle :app:assembleDebug。Windows 也可执行 build-apk.ps1。

## 文档

- [功能设计](docs/DESIGN.md)
- [构建、部署与签名](docs/BUILD.md)
- [使用及测试清单](docs/USAGE.md)
- [隐私政策](PRIVACY.md)
- [版本记录](CHANGELOG.md)

## 版本说明

V1.0.2 统一为此前 V1.0.1 APK 的轻量架构，沿用本地 HTML 界面并重新实现原生采集桥接、适配启动图标和标准 Gradle 构建。它不是旧 Compose 原型，也不是对旧 APK 的逐字节复现。签名不同可能需要卸载旧版再安装，卸载会清除应用设置。

当前尚需 CI 构建及真机验证。正式发布前应完成测试并确认隐私政策主体、联系方式及适用商店要求。仓库公开不代表已授予开源许可证；许可证待维护者确认。

反馈：[Issues](https://github.com/gaojunwei/decibel-meter-android/issues)（勿提交个人隐私信息）。
