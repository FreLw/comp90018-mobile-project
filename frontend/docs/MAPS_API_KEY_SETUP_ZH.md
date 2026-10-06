# Google Maps API Key 创建与配置指南

适用项目：COMP90018 Lost Treasures Android 客户端。
用途：让应用中的 Google 地图底图正常显示。

这是一份操作指南，尚未替你在 Google Cloud 中创建真实 Key。创建时需要登录有项目管理权限的 Google 账号，并使用团队确认的账单账户。

## 1. 准备要填写的信息

| 项目 | 填写内容 |
|---|---|
| API | Maps SDK for Android |
| 建议 Key 名称 | Lost Treasures Android Debug |
| Application restrictions | Android apps |
| Package name | `com.comp90018.app` |
| SHA-1 | 见下方本机 Debug 签名指纹 |
| API restrictions | Restrict key → Maps SDK for Android |

本机此前通过 `./gradlew :app:signingReport` 获取的 Debug SHA-1：

```text
7A:EC:0B:26:BC:3D:BD:DE:09:18:E1:91:E8:2D:DC:EF:3F:B6:BF:DC
```

此值适用于本机当前 Debug 证书构建的 APK，不是所有团队成员通用的值。更换电脑或重新生成证书后，请重新检查。

## 2. 选择 Google Cloud 项目

1. 打开 [Google Cloud Console](https://console.cloud.google.com/)，登录 Google 账号。
2. 点击页面顶部的项目选择器。
3. 优先选择团队现有 Firebase 对应的 Google Cloud 项目，方便统一管理。Maps 也可以使用另一个 Cloud 项目，但要在那个项目中完成 API、Key 和账单配置。
4. 若团队没有现成项目，选择 **New Project / 新建项目**，填写例如 `Lost Treasures`，创建后切换到该项目。
5. 确认页面顶部当前项目正确，后续步骤都在同一项目中操作。

## 3. 配置账单并启用 SDK

1. 打开侧边菜单中的 **Billing / 结算**。
2. 确认项目已关联有效的账单账户；如果没有，使用团队确认的账户关联，或按控制台流程建立账户。
3. 打开 **APIs & Services → Library / API 和服务 → 库**。
4. 搜索 **Maps SDK for Android**，进入详情页。
5. 点击 **Enable / 启用**；如果显示 **Manage / 管理**，通常说明已经启用。

本项目使用 Android 原生地图 SDK，应选择 Maps SDK for Android。实际计费及额度以当前控制台为准，不把启用账单理解为保证免费。官方入口见 [Maps SDK 配置说明](https://developers.google.com/maps/documentation/android-sdk/get-api-key)。

## 4. 创建并限制 API Key

1. 打开 **APIs & Services → Credentials / API 和服务 → 凭据**。
2. 点击 **Create credentials → API key / 创建凭据 → API 密钥**。
3. 创建后进入该 Key 的编辑页面；根据控制台界面，也可能直接出现 **Restrict key / 限制密钥**。
4. 名称填写 `Lost Treasures Android Debug`。
5. 在 **Application restrictions / 应用限制** 中选择 **Android apps / Android 应用**。
6. 添加一个 Android 应用条目：

   ```text
   Package name:
   com.comp90018.app

   SHA-1 certificate fingerprint:
   7A:EC:0B:26:BC:3D:BD:DE:09:18:E1:91:E8:2D:DC:EF:3F:B6:BF:DC
   ```

7. 在 **API restrictions / API 限制** 中选择 **Restrict key / 限制密钥**。
8. 勾选 **Maps SDK for Android**，保存。
9. 复制生成的 Key，下一步直接粘贴到本机配置文件，不需要发到聊天中。

Android 应用限制使用“包名 + 签名证书 SHA-1”。具体要求见 [Google Maps Key 限制说明](https://developers.google.com/maps/api-security-best-practices)。

## 5. 写入本项目

打开项目内的 `frontend/local.properties`。保留现有 `sdk.dir`，新增或更新一行：

```properties
MAPS_API_KEY=在这里粘贴刚创建的实际Key
```

填写时注意：

- 等号右侧不加引号，文件中只保留一条 `MAPS_API_KEY`。
- 不要把示例中文字当作实际 Key。
- 不要覆盖 SDK 路径，不要修改 `local.properties.sample` 来保存真实 Key。
- `local.properties` 已被 Git 忽略，不要强行提交。

项目现有构建代码会读取该值，并写入 Manifest 的 `com.google.android.geo.API_KEY`。无需再手动修改 Manifest。

如果 `~/.gradle/gradle.properties` 或构建命令已设置同名 Gradle 属性，它会优先于 `frontend/local.properties`。出现“明明改了 Key 但没有生效”时，检查这项覆盖关系。

## 6. 重新构建并安装真机

使用 Android Studio：

1. 打开 `frontend` 项目。
2. 执行 Gradle Sync。
3. 连接手机并接受 USB 调试授权。
4. 选择目标手机，点击普通 **Run**，重新安装应用。
5. 登录并进入地图页。

或者在 `frontend` 目录的终端运行：

```bash
./gradlew :app:assembleDebug
```

生成的 APK 位于：

```text
frontend/app/build/outputs/apk/debug/app-debug.apk
```

Windows 使用：

```powershell
.\gradlew.bat :app:assembleDebug
```

仅修改 local.properties、重启手机上的旧应用不会更新 Key，必须重新构建安装。

## 7. 验证配置成功

- [ ] 地图能显示道路、建筑等底图，而不是只有空白背景。
- [ ] 缩放和拖动后仍能加载地图内容。
- [ ] 应用宝物标记正常出现；如果底图正常但无宝物，继续排查 Firebase 数据。
- [ ] 授予精确定位后可观察实际位置；如果底图正常但没有定位，继续排查定位权限和 GPS。
- [ ] Android Studio Logcat 中没有 Maps 的 `Authorization failure`。

Maps Key 控制地图服务授权。它不会自动授予 GPS 权限，也不会开启或关闭本项目的 Debug 模拟功能。

## 8. 常见问题

| 现象 | 检查方法 |
|---|---|
| 找不到 Maps SDK for Android 可选项 | 确认当前项目正确，并先在 API Library 中启用 SDK |
| 地图灰白，但有 Google 标志或标记 | 检查 Key 是否进入新 APK、账单是否有效、SDK 是否启用、包名与 SHA-1 是否匹配；结合 Logcat 排查 |
| 自己电脑构建能用，同学构建不能用 | 同学可能使用另一张 Debug 签名证书，需添加对应 SHA-1 |
| 同一 APK 装另一部手机 | 签名没有变，不需要为每台手机添加 SHA-1；仍需正常网络及 Google Play services |
| Debug 可以，Release 不可以 | 检查 Release APK 的实际签名 SHA-1；当前项目尚未配置 Release 签名 |
| 有 google-services.json，地图仍不显示 | Firebase 配置文件不能替代本项目要求的 MAPS_API_KEY 配置 |
| 改完 Key 仍报旧授权错误 | 确认重建安装、没有同名 Gradle 属性覆盖，并给云端限制变更留出传播时间 |

同学获取自己 Debug SHA-1 的方法：在 `frontend` 目录执行 `./gradlew :app:signingReport`，找到 `Variant: debug` 下的 `SHA1`。Windows 用 `.\gradlew.bat :app:signingReport`。

## 9. 完成记录

```text
Google Cloud 项目 ID：
Key 名称（不填完整 Key）：
账单已关联：是 / 否
Maps SDK for Android 已启用：是 / 否
包名：com.comp90018.app
已授权的构建证书 SHA-1：
重新构建及安装时间：
真机型号：
底图显示正常：是 / 否
Logcat 授权错误：无 / 有（记录错误内容，不记录 Key）
```
