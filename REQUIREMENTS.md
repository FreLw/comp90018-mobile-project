# 开发环境要求（Kotlin + Android + Spring Boot + MySQL）

## 技术栈

| 部分 | 技术 |
| --- | --- |
| Android 前端 | Kotlin、Jetpack Compose、Android Studio |
| 后端 REST API | Kotlin、Spring Boot、Gradle Wrapper |
| 数据库 | MySQL Community Server 8.4 或更高版本 |
| API 数据格式 | HTTP + JSON |

## 必装软件（Windows）

1. **Git**：用于下载和提交项目代码。
2. **JDK 21（LTS）**：后端项目的 Java toolchain 固定为 21。请安装 JDK，不能只安装 JRE。
3. **Android Studio**：安装时保留 Android SDK、Android SDK Platform、Android SDK Build-Tools 和 Android Emulator。
4. **MySQL Community Server**：安装 MySQL Server 和 MySQL Workbench；记录安装时设置的 `root` 密码。

可选：安装 Docker Desktop。团队若使用 Docker，可用它统一启动 MySQL；否则直接使用本机 MySQL 即可。

不需要单独安装 Kotlin 或 Gradle：Android Studio 会提供 Android 所需的 Kotlin 支持，后端使用仓库内的 Gradle Wrapper。

## 安装后的检查

在 PowerShell 中执行：

```powershell
git --version
java -version
```

`java -version` 应显示 JDK 21。若电脑同时安装了多个 Java 版本，请将 `JAVA_HOME` 指向 JDK 21 的安装目录，例如：

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21", "User")
```

设置后请重新打开终端和 Android Studio。实际 JDK 安装目录可能不同，请按你的电脑修改该路径。

## 初始化 MySQL

用 MySQL Workbench 或 MySQL 命令行，以 `root` 用户执行：

```sql
CREATE DATABASE comp90018
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'comp90018_app'@'localhost' IDENTIFIED BY 'change-this-local-password';
GRANT ALL PRIVILEGES ON comp90018.* TO 'comp90018_app'@'localhost';
FLUSH PRIVILEGES;
```

请将示例密码替换为自己的本机密码，不要把真实密码提交到 Git。

建议未来后端数据库配置使用环境变量：

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=comp90018
DB_USERNAME=comp90018_app
DB_PASSWORD=<你的本机密码>
```

后端现在使用 Flyway 管理 schema。启动时会自动执行
`backend/src/main/resources/db/migration` 下尚未执行的迁移；第一份
`V1__create_users_table.sql` 会创建 `users` 表。不要手动修改已提交的
迁移文件；以后改表请新增 `V2__...sql`、`V3__...sql`。

## 启动后端

从项目根目录执行：

```powershell
cd backend
$env:DB_PASSWORD = "<你的 comp90018_app 密码>"
.\gradlew.bat bootRun
```

MySQL 服务必须已经启动，且 `comp90018` 数据库和 `comp90018_app` 用户已按上方 SQL 建好。
第一次启动会下载 Gradle 与 Maven 依赖，并由 Flyway 自动创建 `users` 表。
成功后访问：

```text
http://localhost:8080/api/v1/health
```

预期结果：

```json
{"status":"ok","service":"comp90018-backend","timestamp":"..."}
```

## Android 连接本机后端

Android Emulator 中的 `localhost` 指向模拟器本身，而不是开发电脑。因此模拟器请求后端时使用：

```text
http://10.0.2.2:8080/api/v1/health
```

真机测试时使用开发电脑局域网 IP，例如 `http://192.168.1.10:8080`，并确认手机与电脑处于同一 Wi-Fi、Windows 防火墙允许端口 8080。
