# 构建、部署和签名

## 环境

JDK 17，Gradle 8.9，Android Gradle Plugin 8.7.3，Android SDK Platform 35，Build Tools 35.0.0。Windows 可使用 Android Studio SDK Manager 安装 SDK，设置 JAVA_HOME、ANDROID_HOME 与 PATH。若需要 local.properties，写入本机 sdk.dir，禁止提交此文件。

项目不携带未经验证的 Gradle Wrapper 二进制；使用已安装的 Gradle 8.9，或者在本地执行 gradle wrapper --gradle-version 8.9 后自行维护 Wrapper。

## 调试版

在项目根目录执行：

    gradle --no-daemon :app:assembleDebug

Windows PowerShell：powershell -ExecutionPolicy Bypass -File .\build-apk.ps1

输出：app/build/outputs/apk/debug/app-debug.apk。开启手机允许来自对应来源的安装权限，或使用 adb install -r app/build/outputs/apk/debug/app-debug.apk。

调试证书由构建机生成；不同机器的调试证书可能不一致。如果提示签名不匹配，先备份需要的数据后卸载旧版本，再安装新版。

## 正式签名

在受信任的本地计算机用 Android Studio 的 Generate Signed Bundle / APK 功能创建并保管自己的发布密钥，或执行：

    keytool -genkeypair -v -keystore release.jks -alias decibel -keyalg RSA -keysize 2048 -validity 10000
    gradle :app:assembleRelease

使用 SDK build-tools 下 zipalign 和 apksigner，将未签名 release APK 对齐、签名并验证（Windows 使用对应 .exe/.bat）：

    zipalign -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk aligned.apk
    apksigner sign --ks release.jks --ks-key-alias decibel --out DecibelMeter-release.apk aligned.apk
    apksigner verify --verbose --print-certs DecibelMeter-release.apk

交互输入密码，不写入命令历史或源码。签名密钥必须长期安全保存；绝不提交私钥、密码、签名配置、local.properties。正式上架需要确认应用 ID 的归属、目标 SDK、隐私政策、商店审核要求。当前沿用旧包名 com.example.decibelmeter.lite；正式发行前建议确认专属 applicationId，修改后将成为不同应用。

## GitHub Actions

.github/workflows/android.yml 在 main 推送、PR 及手动触发时构建 debug APK，成功后上传为 Actions Artifact。工作流只读权限，不使用发布密钥，不自动公开发布 Release。正式签名及 Release 由维护者确认后独立执行。
