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
2. 打开仓库 **Actions** → **Build Android APK**（推送 `android/` 会自动触发）  
3. **推荐**：到仓库 **Releases** 页直接下载 `tugulu-scanner.apk`  
4. 或者在 Actions 成功运行页下载 Artifact `tugulu-scanner-apk`：  
   - GitHub 给的是一个 **zip**，必须先解压  
   - 解压后里面才是 `tugulu-scanner.apk`  
   - **不要**把 zip 改后缀成 `.apk` 去安装（手机会提示“安装包已损坏”）  
5. 传到手机安装；若提示未知来源，在系统设置里允许该来源安装  

## 本地有 JDK 时也可命令行打包

```bash
cd android
./gradlew assembleRelease   # Linux / macOS
gradlew.bat assembleRelease # Windows
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`（推荐侧载安装）

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
