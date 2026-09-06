This is the project for COMP90018.

环境安装、MySQL 初始化与启动说明见 [REQUIREMENTS.md](REQUIREMENTS.md)。

## Backend

`backend` is a Kotlin + Spring Boot REST API. It uses Java 21 as its target
version and includes a Gradle Wrapper, so a separate Gradle installation is not
needed.

From the repository root, start it with:

```powershell
cd backend
.\gradlew.bat bootRun
```

The server then listens at `http://localhost:8080`.

Try either endpoint in a browser or terminal:

```text
http://localhost:8080/api/v1/health
http://localhost:8080/actuator/health
```

## User API and database

The backend uses MySQL and Flyway. On startup, Flyway applies SQL migrations
from `backend/src/main/resources/db/migration`; `V1__create_users_table.sql`
creates the `users` table.

Create a user after MySQL has been initialized as described in
[REQUIREMENTS.md](REQUIREMENTS.md):

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/users" -ContentType "application/json" -Body '{"account":"alice","password":"change-me-123","gender":"FEMALE","avatarUrl":"https://example.com/avatar.png","bio":"Hello"}'
```

Read user ID 1:

```text
http://localhost:8080/api/v1/users/1
```

## Android frontend

Open `frontend` in Android Studio and let it install the requested Android SDK
and Gradle dependencies. Start the backend first, then launch an Android
emulator. The app is configured to use `http://127.0.0.1:8080/` through adb
reverse. For a physical phone, replace `API_BASE_URL` in
`frontend/app/build.gradle.kts` with the computer's LAN IP.
