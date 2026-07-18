# 兔咕噜扫码入库 · Android 版

原生 Android 扫码入库 App，使用 **CameraX + ML Kit**，识别效果优于网页版。

## 功能

- 员工登录（JWT，对接 `/api/auth/login`）
- 摄像头连续扫码（Code128 / EAN / QR / DataMatrix 等）
- 手动输入单号
- 扫码成功「滴」声 + 震动
- 记录列表、删除、清空
- 一键入库：先上传最近一张包裹照片到 `/api/common/upload`，再调用 `/api/inbound/scan`
- 记住服务器地址与登录用户名

## 不装 Android Studio，用 Git 打包

1. 把本仓库推送到 GitHub
2. 打开仓库 **Actions** → **Build Android APK**
3. 点击 **Run workflow**（或推送 `android/` 目录下的改动自动触发）
4. 构建完成后在该次运行页面的 **Artifacts** 下载 `tugulu-scanner-apk`
5. 解压得到 `app-release-unsigned.apk`，传到手机安装  
   （正式签名未配置时为 unsigned；多数安卓机可直接安装 debug 包，需要更稳妥可下载 `tugulu-scanner-debug-apk`）

## 本地有 JDK 时也可命令行打包

```bash
cd android
./gradlew assembleRelease   # Linux / macOS
gradlew.bat assembleRelease # Windows
```

产物：

- `app/build/outputs/apk/release/app-release-unsigned.apk`
- `app/build/outputs/apk/debug/app-debug.apk`

## 登录说明

| 字段 | 说明 |
|------|------|
| 服务器地址 | 后端根地址，例如 `https://你的域名` 或 `http://内网IP:8080` |
| 用户名 / 密码 | 与管理后台相同（如 `employee` / `admin123`） |

> 若站点 Nginx 只托管了静态页，需要把 `/api/` 反代到 Spring Boot，否则 App 登录会失败。

## 技术栈

- Kotlin + ViewBinding
- CameraX + ML Kit Barcode Scanning
- OkHttp
- minSdk 26 / targetSdk 34
