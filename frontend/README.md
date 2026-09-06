# Android frontend

This is a Kotlin + Jetpack Compose Android app with a registration screen, a
login screen, and a simple signed-in profile screen.

## Run it

1. Start the backend from `backend` and leave it running:

   ```powershell
   $env:DB_PASSWORD = "123"
   .\gradlew.bat bootRun
   ```

2. In Android Studio, select **Open** and choose this `frontend` folder.
3. When Android Studio asks to install Android SDK Platform 37 or build tools,
   accept the installation, then wait for Gradle Sync to finish.
4. Create or select an Android Emulator and click the Run button.

The emulator uses `http://127.0.0.1:8080/` through an adb reverse mapping. If
the emulator is restarted, recreate that mapping from the Android Studio
terminal before launching the app:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
```

To run on a physical phone, replace `API_BASE_URL` in `app/build.gradle.kts`
with the computer's LAN IP, such as `http://192.168.1.10:8080/`; do not use adb
reverse for a physical phone.

## Endpoints used

- `POST /api/v1/users` — register
- `POST /api/v1/users/login` — login

Passwords are sent over the local development connection and only their BCrypt
hash is stored by the backend. Before any public deployment, use HTTPS and a
real login session/token mechanism.
