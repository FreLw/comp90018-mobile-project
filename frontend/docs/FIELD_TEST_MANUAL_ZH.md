# Task 1–4 与 Sensor 真机测试手册

日期：2026-10-06。检查基线：main / ac5ce36，加本次工作区修正。
本手册暂按四个寻宝挑战定义 Task 1–4；如果课程中的任务编号不同，需要重新映射。
代码检查和单元测试不等于现场通过。本文的现场用例均需团队实际执行。

## 1. 今天先做什么

建议两台不同型号 Android 手机、两个独立测试账号，一人操作、一人记录；先室内准备约 30 分钟，再现场校准与测试约 60–90 分钟，最后组队和异常测试约 30 分钟。

出发前检查：

- [ ] Android Studio 打开仓库中的 `frontend` 目录；不要用仓库根目录遗留的 `app/build` 作为本次 APK 来源。
- [ ] 手机 Android 8.0 / API 26 以上，有可用的 Google Play services、网络、GPS、相机、加速度计、陀螺仪和旋转向量传感器。
- [ ] 配好 Maps API Key，实际看到地图底图和标记。
- [ ] 确认 Firebase 登录、宝物列表读取、发现记录保存可用。`google-services.json` 与 Maps Key 是两项配置。
- [ ] 使用独立测试账号，避免模拟完成记录影响正式账号。重新安装不会清除 Firestore 中的发现记录。
- [ ] 记录 APK 构建来源、Git 提交及未提交修改、手机型号、Android 版本、账号、测试时间。
- [ ] 现场测试前退出所有模拟界面，地图距离按钮为 `Test distance`（OFF），从地图正常入口进入挑战。

## 2. Debug 到底是什么，会不会影响真机测试

| 名称 | 用途 | 对测试的影响 |
|---|---|---|
| Debug APK / `BuildConfig.DEBUG` | 开发安装包，允许调试并显示开发工具 | 可以在真机测试硬件；是否使用假数据由应用代码决定 |
| Android Studio Debug / 断点 | 暂停程序并检查变量 | 断点会干扰计时和连续传感器采样；计时验收用普通 Run，不暂停程序 |
| 手机 USB debugging | 允许电脑安装、运行和调试应用 | 本身不会把 GPS 或传感器换成模拟数据 |
| 本项目 Debug challenge launcher | 在室内直接进入挑战、模拟位置和姿态 | 只能验证流程和判定逻辑，不能证明硬件可用；模拟完成也可能写入发现记录 |
| 地图 `Test: 100 m / 50 m / 10 m` | 模拟接近宝物的距离 | 影响地图入口与位置流程，现场必须切回 `Test distance` |
| `Show calibration diagnostics` | 查看坐标、精度、方向、姿态及各条件 | 只查看数值可用于现场校准；不要点击 `DEBUG: simulate sustained noise` |

**本次发现并修正：** 原代码在挑战入口直接传入 `BuildConfig.DEBUG`，导致任何 Debug APK 的挑战都使用模拟引擎。现在普通入口使用真实传感器，只有显式 Debug launcher 启用模拟。修改后必须重新构建并安装；手机上旧 APK 不会自动变化。

Debug 功能存在的原因是：不用每次走到校园，也能重复检查“位置不对、方向不对、保持时间不足、拍照失败”等分支。现场验收必须使用真实输入，之后再用签名 Release 包做最终回归。当前项目未配置 Release 签名，不能把生成 unsigned APK 等同于可安装发布包。

## 3. Maps API Key 配置

本次检查：`frontend/local.properties` 中的 `MAPS_API_KEY` 缺失或为空；`frontend/app/google-services.json` 存在。尚未验证云端 Key、账单状态或授权。

1. 打开 Google Cloud Console，选择团队使用的项目，完成账单配置，启用 **Maps SDK for Android**。步骤依据 [Google Maps 官方配置说明](https://developers.google.com/maps/documentation/android-sdk/get-api-key)。
2. 在 APIs & Services → Credentials 创建或选用 API Key。
3. Application restrictions 选择 **Android apps**，添加包名 **`com.comp90018.app`** 及构建该 APK 所用证书的 SHA-1。API restrictions 限制为 **Maps SDK for Android**。依据 [Google Key 限制说明](https://developers.google.com/maps/api-security-best-practices)。
4. 在 `frontend` 目录执行下列命令，读取 `Variant: debug` 对应的 SHA1：

   ```bash
   ./gradlew :app:signingReport
   ```

   Windows 使用 `gradlew.bat :app:signingReport`。SHA-1 属于签名证书，不属于手机；同一 APK 安装到多台手机不需要每台手机添加 SHA-1。每个同学自行构建时，debug keystore 可能不同，需要分别授权；Release 签名也需对应授权。

   本机本次 `signingReport` 的 Debug SHA-1（仅适用于此签名证书构建的 APK）：

   ```text
   7A:EC:0B:26:BC:3D:BD:DE:09:18:E1:91:E8:2D:DC:EF:3F:B6:BF:DC
   ```

5. 编辑 `frontend/local.properties`，保留原有 SDK 路径，只添加或更新：

   ```properties
   MAPS_API_KEY=替换为实际的Key
   ```

   不加引号。文件已被 Git 忽略，不要把 Key 写进已跟踪的示例文件或提交到仓库。Key 不需要发到聊天中。

6. Sync Gradle，重新构建并安装。项目已经把此配置注入 Manifest 的 `com.google.android.geo.API_KEY`，无需再改 Manifest，也无需为本项目另装 Secrets Gradle 插件。注意 Gradle 属性 `MAPS_API_KEY` 优先于 local.properties；如团队在全局 Gradle 配置或命令行传值，需核对覆盖关系。
7. 手机上确认能看到地图瓦片，Logcat 没有 Maps authorization failure。灰色底图排查顺序：最终构建是否带 Key → SDK 是否启用 → 账单 → 包名 → 本 APK 证书 SHA-1 → 手机网络与 Google Play services。

## 4. 构建与安装

在 `frontend` 目录：

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:signingReport
```

APK：`frontend/app/build/outputs/apk/debug/app-debug.apk`。
项目编译 SDK 为 37、最低运行 SDK 为 26，源码目标 Java 21；Gradle daemon 配置请求 JDK 25。首次构建可能下载 Gradle、JDK 或依赖，出发前完成。

手机开启开发者选项和 USB debugging，连接电脑并接受授权；Android Studio 选择该真机后点击普通 Run。官方步骤见 [在硬件设备上运行](https://developer.android.com/studio/run/device)。也可用 Android SDK platform-tools：

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

以上 adb 命令在 `frontend` 目录运行；多设备时用 `adb -s <设备序列号> ...`。安装成功后可断开电脑继续测试。若提示签名不一致，不要直接删除有用数据：先确认原 APK 的构建来源及测试账号数据，再决定处理方式。

## 5. 当前任务规则与现场校准

以下值来自仓库 `database/treasures.json`；运行时宝物来自 Firestore，**改 JSON 不会自动更新云端**。先在校准面板核对运行时数值。如果不同，记录差异后按团队确认的数据测试。本次没有修改云端数据。

| 任务 | 目标坐标（纬度，经度） | 挑战半径 | 方向要求（磁北） | 连续保持与动作 |
|---|---|---:|---|---|
| 1 Union Lawn Lost Lake | -37.797149, 144.961958 | 25 m | 288° ±12° | 对齐 0.8 秒后真实拍照；仅计时完成不算成功 |
| 2 Wilson Hall | -37.797895, 144.961417 | 20 m | 197° ±10° | 静止、稳定、无明显旋转，保持 3 秒 |
| 3 Old Quad | -37.797590, 144.960970 | 18 m | 无固定朝向 | 手机水平、静止、稳定、无明显旋转，保持 3 秒 |
| 4 South Lawn 单人 | -37.798680, 144.959900 | 15 m | 185° ±8° | 静止、稳定、无明显旋转，保持 1.2 秒 |

上述四项均标记 `calibrationStatus=pending`，不能当作已现场验证的位置与朝向。地图 `huntReadyRadiusMeters` 与挑战半径是不同概念；这四项数据中的地图准备距离为 10 m。进入挑战后还要独立满足挑战位置条件。

校准方法：

1. 从正常入口进入；打开 `Show calibration diagnostics`。确认移动/转动手机时 Current、Heading、Pitch/Roll 实际变化，并且没有模拟滑块或模拟场景按钮。
2. 户外等定位稳定，记录坐标、accuracy、目标距离和目标朝向；位置需要两个不同时间戳的合格读数。定位请求间隔约 5 秒，因此到点后等待不只等于 0.8/3 秒挑战时间。
3. 位置精度还须不差于该任务半径。人在点位但 accuracy=40 m 时，这四项挑战都不应算到达。
4. 检查安全可站立位置与实际观测对象，记录手机读出的磁北方向。不要直接拿真北方向替换磁北字段。
5. 用两台手机分别重复三次，记录可稳定完成的站位和角度；若偏差明显，记录建议值，由团队确认后更新实际 Firestore 数据，再回测。

## 6. Task 1–4 逐项操作

每项用新测试账号或未收集宝物开始。完成后检查成功反馈、历史/艺术图、Collection 中记录，以及退出重进后仍保留。模拟通过与真机通过分开记录。

| 编号 | 操作 | 预期结果 |
|---|---|---|
| T1-01 | 到 Union Lawn，方向先偏离目标，再转到 ±12° 范围 | 偏离时不能拍照完成；对齐连续 0.8 秒后开放相机 |
| T1-02 | 首次拒绝相机权限，再授权并重试 | 不崩溃、不误收集；授权后有真实预览、拍照和保存结果 |
| T1-03 | 保持对齐但不拍照；然后点击拍照 | 未拍照不完成，成功捕获后才进入保存流程 |
| T2-01 | 到 Wilson Hall，方向正确且静止持机 ≥3 秒 | 条件全满足后完成；不要只看地图距离 |
| T2-02 | 计时约 1–2 秒时走动、抖动、转动或偏离角度，分别重复 | 对应条件变为 wait，保持进度重置，不能累加多次短暂停留 |
| T3-01 | 到 Old Quad，竖着或倾斜手机，再水平放稳 | 倾斜时阻止完成；俯仰和横滚均在 ±12° 内且稳定 3 秒可完成 |
| T3-02 | 水平持机途中抖动或旋转；恢复稳定 | 计时重置，恢复后重新保持完整 3 秒 |
| T4-01 | South Lawn 单人模式下先偏离方向，再转到 185° ±8° | 朝向错误不能完成；静止稳定保持 1.2 秒后完成 |
| T4-02 | 保持过程中转动或走出有效范围 | 进度重置；返回后重新满足条件 |

## 7. 组队流程，尤其 Task 4

Task 1 的特殊行为：达到 TAKE_PHOTO 后，ViewModel 会保留拍照资格并暂不处理后续传感器快照，以允许按快门时移动。因此 T1 的方向/GPS 失效阻断用例应在相机解锁前执行；解锁后转动手机仍可拍照属于当前设计。若产品要求拍摄瞬间仍在范围且方向正确，需要另改规则。

两台手机分别使用账号 A、B，加好友并进入同一个 Room，确认双方看到相同目标。

1. 对非 South Lawn 任务，检查房主挑战和队友问答各自完成；只有一人完成时仍应等待队友，双方完成后才能挖掘。验证最终两端记录和重进后的状态。
2. 对 South Lawn，先执行新增的**四碎片收集阶段**，不能仅用单人方向挑战代表 Task 4 组队测试。点击碎片标记，走入 8 m 后收集，观察另一台手机是否同步。
3. 四个碎片坐标：NW (-37.798490, 144.959940)、NE (-37.798500, 144.960570)、SW (-37.798850, 144.959940)、SE (-37.798910, 144.960530)。
4. 8 m 外不可收集；两人同时点击同一碎片不应重复计数；离线/重连后进度不丢失。
5. 全部碎片后继续检查后续任务、领取和等待队友状态；一人领取后另一人应仍能完成自己的领取。不要把“4/4 碎片”直接记为整条组队流程通过。

当前碎片距离门槛只检查有效定位和距离，没有像挑战规则那样要求 accuracy ≤8 m 和连续两次定位。弱 GPS 下可能出现误收集，现场务必记录；此处尚未修改。

## 8. Sensor 与异常测试矩阵

| 编号 | 操作 | 期望/检查点 |
|---|---|---|
| S01 GPS 权限 | 拒绝定位、仅粗略定位、允许精确定位各测一次 | 不崩溃；不因缺权限误判到达；精确定位后可恢复 |
| S02 GPS 中断 | 挑战开始后关闭定位或使读数失效，再恢复 | 过期/无效位置不能继续完成；恢复后重新取得合格定位 |
| S03 方位 | 顺/逆时针旋转，跨越 359°→0°，换横竖屏 | 转向提示和角度连续合理；不会把北向边界当大角度误差 |
| S04 水平姿态 | 分别改变 pitch 和 roll | 任一明显超过 ±12°，水平条件应失败 |
| S05 加速度与稳定 | 桌上放稳、手持、走动、轻抖各测 | 静止/运动与稳定/不稳定区分合理，恢复有预热时间 |
| S06 陀螺仪 | 原地转动手机，再停稳 | 旋转期间不应满足 ROTATION_STILL；停下后恢复 |
| S07 生命周期 | 计时中返回地图、锁屏、切后台，再回来 | 不在后台继续完成；回来重新采样和计时；已完成但保存中的状态需单独检查 |
| S08 相机 | 拒绝/允许权限、实际拍摄、相机被其他应用占用 | 错误可见、可重试，没有照片不能误完成 T1 |
| S09 麦克风 | Grainger 任务拒绝再授权；安静和持续发声 | 默认要求 ≥-30 dBFS 持续约 1 秒；短拍手不保证成功，不要按环境声压 dB 理解 |
| S10 保存失败 | 网络正常完成一次；再测断网完成、恢复网络与重试 | 成功必须保存后显示收藏；失败不能伪装成功，不能重复奖励 |
| S11 设备差异 | 两台手机对同一点位做三次 | 记录 GPS、朝向、稳定判定差异；缺少陀螺仪等硬件时可能一直无法满足条件 |
| S12 音频释放 | 先授麦克风权限，再进入非声音任务，退出/锁屏 | 观察系统麦克风指示；当前引擎会为所有挑战启动音频传感器，已授权时非声音任务也可能采音 |

额外 sensor 挑战：System Garden 与 Old Quad 类似（水平稳定 3 秒）；Grainger 是声音挑战。本轮 Task 1–4 不直接验证声音，故需单独执行 S09。

## 9. 代码复查发现及剩余风险

| 优先级 | 发现 | 处理/现场影响 |
|---|---|---|
| P1 | Debug 包所有挑战被强制替换成模拟引擎 | 本次修正为仅显式模拟入口使用假数据；重新安装 APK |
| P1 | 已有两次有效位置后，相同时间戳的过期读数可能保留“已到达” | 已复现并修正：无效位置立即清除到达计数，恢复后需两个新定位；验证见文末 |
| P1 准备项 | 本机 local.properties 没有 Maps Key | 按第 3 节配置；地图真机验收尚未完成 |
| P2 | 四项挑战标为 pending，坐标/朝向未现场校准 | 现场记录并核对 Firestore 实际值 |
| P2 | 旋转向量/陀螺仪不存在时静默跳过注册；onAccuracyChanged 未处理 | 相关挑战可能卡住；磁干扰下方向可靠性不足，不应只用高端单机验收 |
| P2 | 非声音挑战也启动 AndroidSoundLevelSensor | 已授权时会用麦克风；需后续按任务需求启停 |
| P2 | 8 m 碎片门槛没有同等级的 GPS 精度/连续样本约束 | 弱信号下检查是否能远处误收集 |

本次检查覆盖 MapScreen 的挑战/组队入口、ChallengeRuleEvaluator、TreasureChallengeViewModel/Screen、Debug/Release 模拟工厂、定位/方向/姿态/运动/声音实现，以及相关单元测试。相机保存、云端同步和多设备表现仍需上述真机用例；未声称完成 Firebase 线上验证。

## 10. 现场记录模板与通过标准

每个异常至少留一张包含校准面板的截图，必要时补录屏和 Logcat；避免记录 Key、密码和他人资料。

| 时间 | 用例编号 | 手机/系统 | 账号/单人或组队 | 真机/模拟 | 坐标/精度 | 朝向/误差 | 预期 | 实际 | 通过/失败 | 截图/日志 |
|---|---|---|---|---|---|---|---|---|---|---|
| 待填写 | | | | | | | | | | |

通过标准：两台设备各完成 Task 1–4 正向用例；错误条件均能阻止完成；中断可恢复；收藏能保存并重进后读取；Task 4 组队碎片和双方领取流程通过。模拟测试结果不得填入“真机通过”。待校准、地图未授权、无真机数据的项目标为 Blocked 或 Not run。

## 11. 本次自动验证记录

第一轮：Debug APK 构建成功；单元测试共 161 项，其中新增的过期定位回归用例失败，其余 160 项通过，证实原有缺陷。

修复后第二轮：`:app:testDebugUnitTest :app:assembleDebug` 成功，161 项测试全部通过，Debug APK 已重新生成。`git diff --check` 通过。两处逻辑修改、回归测试及手册尚未提交或推送。

已执行 `signingReport`，Debug SHA-1 见第 3 节；Release 未配置签名。已检查最终合并的 Debug Manifest，Maps Key 为空，因此 APK 构建成功不代表地图可用。Key 配好后必须重新构建安装。

现场、实际相机/GPS/麦克风和双机 Firebase 测试未执行。
