# TuGuLu（兔咕噜）仓储物流系统

全栈项目：Spring Boot API + Vue3 英文前台 + Vue3 管理后台。

## 环境要求

- JDK 17+（当前环境为 JDK 21）
- Maven 3.9+（本机已放在 `E:\develop\tools\apache-maven-3.9.9`）
- MySQL 8（`localhost:3306`，库名 `tugulu_warehouse`）
- Node.js 18+

## 数据库

```bash
E:\develop\MySQL\bin\mysql.exe -u root -p < sql/schema.sql
```

默认账号由后端启动时自动初始化：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | admin |
| employee | admin123 | employee |

## 启动后端

```bash
cd backend
mvn -DskipTests package
java -jar target/tugulu-backend-1.0.0.jar
```

API 地址：`http://localhost:8080/api`

## 启动前端

管理后台（端口 5173）：

```bash
cd frontend-admin
npm install
npm run dev
```

英文前台（端口 5174）：

```bash
cd frontend-public
npm install
npm run dev
```

## 功能概览

- 员工 JWT 登录
- CMS：company / service / product
- 快递入库扫描（幂等）与前台查询
- 工单全生命周期（乐观锁 + 流转日志）
- 入库/打包统计与定时清理任务

## 配置

数据库连接见 `backend/src/main/resources/application.yml`。

## Android 扫码 App（无需 Android Studio）

原生扫码工程在 `android/`，使用 CameraX + ML Kit，识别效果优于网页版。

**通过 GitHub Actions 打包 APK：**

1. 推送仓库到 GitHub  
2. 打开 Actions → `Build Android APK` → Run workflow  
3. 构建完成后下载 Artifact `tugulu-scanner-apk`，安装到手机  

详细说明见 [`android/README.md`](android/README.md)。
