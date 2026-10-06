# Android Studio 第一阶段测试：Task 1–4 与 Sensors

本阶段使用 Android Emulator，先验证逻辑、页面与保存，再验证 Android 传感器接口。现场 GPS 精度、真实磁场/陀螺仪、真实相机与麦克风效果留到真机阶段。这里的 Task 1–4 分别指 Union Lawn、Wilson Hall、Old Quad、South Lawn。

## 1. 测试顺序与结果分类

按顺序执行：环境准备 → 自动单元测试 → 登录和地图 → Debug 模拟的反向用例 → 手动计时 → 成功与保存 → 正常入口/Android 接口 → 双模拟器组队。

结果使用四类：Pass、Fail、Blocked（依赖未准备好）、Not run。不要把模拟逻辑通过写成硬件通过。每条用例记录设备、账号、任务、输入、预期、实际及截图。

## 2. Android Studio 准备

1. 打开仓库的 `frontend` 目录，等待 Gradle Sync 完成。项目 compileSdk=37、minSdk=26；按项目工具链配置完成 JDK/SDK 安装。
2. 确认 `app/google-services.json` 存在，并按 [Key 指南](MAPS_API_KEY_SETUP_ZH.md) 配好 Maps Key。本轮此前检查时 Key 为空，不应假设已经配好。
3. 在 Build Variants 工具窗口将 app 的 Active Build Variant 设为 `debug`。
4. 在 Device Manager 创建手机 AVD，选择带 Google Play 的系统镜像、API ≥26；Apple Silicon 选择可用的 ARM64 镜像。启动后确认网络正常。
5. 工具栏选择 app 和这台模拟器，点击普通绿色 Run。计时测试不要使用断点暂停。
6. 使用独立测试账号 A，组队时再准备账号 B。模拟完成会走实际保存流程，可能写入 Firebase。

AVD 官方说明：[创建和管理虚拟设备](https://developer.android.com/studio/run/managing-avds)。Google Play 镜像包含 Google Play services。不要把编译 SDK 37 理解为只能使用 API 37 的模拟器。

## 3. 先跑自动单元测试

Android Studio Terminal 的当前目录为 `frontend` 时执行：

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Windows：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

预期 BUILD SUCCESSFUL；上一轮修复后的基线是 161 项测试全部通过。测试报告为 `app/build/reports/tests/testDebugUnitTest/index.html`。也可在源码 `app/src/test/kotlin` 中打开测试类，点击类旁运行图标单独执行。

重点已有测试：ChallengeRuleEvaluatorTest（位置/方向/保持/照片）、TreasureChallengeViewModelTest（状态和生命周期）、DebugChallengeSimulationTest（模拟场景）、Attitude/Direction/Rotation/MotionStabilityProcessorTest、LocationReadingFilterTest、SoundLevelCalculatorTest。单元测试不依赖你在模拟器里走动，也不能证明硬件已验证。

## 4. 登录、地图、测试入口

| 编号 | 操作 | 预期 |
|---|---|---|
| E01 | 登录账号 A，进入 Map | 无崩溃，可读取宝物列表 |
| E02 | 检查底图、拖动和缩放 | 地图道路等内容可显示；否则先排查 Key/网络 |
| E03 | 关闭已打开的宝物详情，确保没有选中宝物 | 地图右上角可见 DEBUG tasks |
| E04 | 展开 DEBUG tasks，选择对应地点 | 挑战页面出现 DEBUG · Fake sensor context |
| E05 | 打开 Show calibration diagnostics | 可查看实际任务目标、半径、heading 和每项 pass/wait |

入口列表来自加载到的宝物数据，没有有效挑战数据时不会显示。找不到入口先检查 debug 构建、是否选中了宝物、Firebase 数据是否正常。启动按钮显示地点名，进入后的任务标题用于确认身份。

DEBUG tasks 不含 Grainger 声音任务；当前列表包含四个任务和 System Garden，不能在此列表寻找 Grainger。

## 5. 理解模拟控件，避免假阳性

| 控件 | 用途与注意事项 |
|---|---|
| Location valid | 直接改变模拟 GPS 有效性；它不受模拟器系统权限控制 |
| Distance | 模拟与目标距离，超过挑战半径应阻止到达 |
| GPS accuracy | 模拟定位误差，数值越小越精确；应 ≤任务半径 |
| Heading | 模拟磁北朝向，需符合任务 required heading ± tolerance |
| Pitch / Roll | 模拟显示数值；本模拟器不会由这两个值重新计算 Horizontal |
| Horizontal | 直接决定模拟水平状态；Old Quad 用此开关测规则 |
| Stationary | 直接决定模拟是否静止 |
| Stable | 直接决定模拟是否稳定 |
| Gyroscope still | 直接决定模拟是否停止旋转 |
| Simulate PHOTO_CAPTURED | 注入拍照成功事件，用内置图片 URI；不会调用相机 |
| All valid… | 对非拍照任务直接推进规则时间并尝试完成；仅用于成功/保存流程，不能检验等待时间 |

模拟快照每约 250 ms 更新一次，两个不同位置读数后才会信任到达。场景按钮会整体替换所有模拟输入，不能把它理解成只修改按钮名字对应的单个字段。比如 Inside target、Heading aligned 可能同时让全部条件合格，开始计时。

每项任务先测失败分支，最后才完成。任务完成后界面可能自动跳转，无法在同一会话继续测失败分支。需要重测时返回地图重新进入；保存持久性用例优先换尚未收集该宝物的测试账号，重装应用不清除云端收藏。

## 6. 四项任务共有的 GPS 反向用例

每个任务初始模拟位置无效。打开校准面板，按下表逐项操作，执行时保留一个其他条件为失败可避免意外完成。

| 编号 | 输入 | 预期 |
|---|---|---|
| G01 | Location valid=OFF | LOCATION_INSIDE=wait，保持进度为 0，不完成 |
| G02 | Location valid=ON，Distance=100 m，accuracy=5 m | 超出四项任务范围，不完成 |
| G03 | Distance=0 m，accuracy=100 m | 精度太差，不完成 |
| G04 | Distance=0 m，accuracy=5 m，Location valid=ON | 等待新快照后位置条件 pass；其他条件仍独立判断 |
| G05 | 在未完成的保持期间把 Location valid 关掉 | 位置变 wait、保持归零；恢复后重新计时 |

任务半径以诊断面板为准。仓库参考值为 T1 25 m、T2 20 m、T3 18 m、T4 15 m。滑块不能准确输入边界值，严格边界检查交给单元测试；不要用滑块整数标签声称验证了精确边界。

## 7. Task 1：Union Lawn，方向 + 拍照

1. 从 DEBUG tasks 进入 Union Lawn，确认标题 Union Lawn Lost Lake。
2. 点击 Outside target：预期要求靠近，不允许拍照完成。
3. 点击 Heading misaligned：位置合格后仍应提示转向，保持进度为 0。
4. 点击 Photo ready (hold)：等待两个定位快照及约 0.8 秒连续对齐。这里没有 All valid 的快捷完成行为。
5. 检查 Simulate PHOTO_CAPTURED 从不可用变为可用，但任务还未完成；等几秒不点击，仍不应自动收集。
6. 点击 Simulate PHOTO_CAPTURED：应进入保存，保存成功后进入宝物展示并可在收藏中看到。

计时中断另开会话测试：先 Heading misaligned，再手动将 Heading 调到诊断面板目标附近，0.8 秒内调出范围，确认进度重置。此时间较短，手工测试可用录屏辅助，严格时序以单元测试为准。

达到 TAKE_PHOTO 后代码会锁定拍照资格，后续移动不再立即取消资格，这是当前设计。模拟拍照通过只能证明事件与保存流程；相机权限、预览、实际保存需第 11 节正常入口测试。

## 8. Task 2：Wilson Hall，方向 + 稳定 3 秒

先逐一点击失败场景，每次停留约 4 秒：

| 场景按钮 | 预期 |
|---|---|
| Inside but moving | STATIONARY=wait，不能完成 |
| Stationary but unstable | STABLE=wait，不能完成 |
| Stable but rotating | ROTATION_STILL=wait，不能完成 |
| Heading misaligned | HEADING_ALIGNED=wait，不能完成 |

验证真实计时，不点击 All valid (3s hold)：

1. 点击 Stable but rotating，确认位置和方向等条件满足，Gyroscope still=OFF。
2. 手动开启 Gyroscope still，观察计时开始。
3. 约 1 秒后关闭，预期进度归零，而不是暂停保留。
4. 再次开启，完整等待约 3 秒，才应完成。
5. 另一次会话可分别用 Stationary、Stable 或 Location valid 中断，检查同样重置。

最后单独测试 All valid (3s hold) 快捷完成：预期快速进入正常保存流程，记录为“快捷路径”，不要记录为“3 秒计时通过”。

## 9. Task 3：Old Quad，水平 + 稳定 3 秒

1. 点击 Not horizontal：PHONE_HORIZONTAL=wait，等 4 秒不完成。
2. 点击 Horizontal but moving：水平通过，STATIONARY=wait，等 4 秒不完成。
3. 点击 Not horizontal 重新建立其他条件有效、Horizontal=OFF 的状态。
4. 手动打开 Horizontal，约 1 秒后关闭，预期进度归零。
5. 再开启 Horizontal，保持全部条件有效约 3 秒，完成并保存。
6. 另一次会话在全部条件满足但 Horizontal=OFF 时改变 Heading：这个任务不应出现固定朝向要求。

不要只拖 Pitch 到 45° 就期待模拟 Horizontal 自动失败。倾角到水平状态的转换由 AttitudeProcessor 单元测试及第 11 节 Android 接口验证。

## 10. Task 4：South Lawn 单人，方向 + 稳定 1.2 秒

1. 点击 Heading misaligned：方向条件失败，不能完成。
2. 点击 Aligned but rotating：方向正确，但旋转条件失败，不能完成。
3. 在该场景手动开启 Gyroscope still，计时开始；约 0.5 秒后关闭，应归零。
4. 再开启，保持约 1.2 秒后完成并保存。
5. 单独开一轮点击 All valid (hold)，只验证快捷完成与保存。

参考目标是 185° ±8°，但当前运行的 Firestore 配置可能不同，以校准面板为准。Heading aligned 会使其他条件也有效并可能快速完成，因此失败分支要先测。

此入口覆盖单人方向挑战，Task 4 组队四碎片另见第 13 节。

## 11. 正常入口：验证 Android 接口接线

这一轮退出 Fake sensor context，地图距离按钮切回 Test distance（OFF），通过正常地图流程进入。即使在 Emulator 中，普通入口也会走 AndroidDeviceContextEngine；它读取的是模拟器提供的 Android 传感器数据，不是物理手机的现场数据。

1. 打开 Emulator 的 Extended controls（通常为 …）。Location 中设定测试点，并点击 Set location。Union Lawn 参考纬度 -37.797149、经度 144.961958；使用诊断面板核对应用实际目标。
2. 等应用收到定位，必要时再发送一次位置，确认诊断中的 Current、距离变化。地图入场还有方向/稳定等门槛，不能仅凭设点就断言一定进入挑战。
3. 在 Virtual sensors → Device Pose 改变模拟设备姿态，观察应用方位、俯仰和横滚是否变化；实际支持程度依 AVD 而定。如果缺传感器或输入一直 UNKNOWN，记录 Blocked，不使用 Debug 完成替代硬件链路验证。
4. 正常挑战页面不应有 DEBUG · Fake sensor context 或模拟场景滑块；Show calibration diagnostics 可以存在。
5. 用系统设置拒绝/重新授权定位，检查是否正确阻止和恢复；这不能在 Fake 模式下测试，因为 Fake 模式绕过真实权限输入。
6. Task 1 正常达到拍照资格后，拒绝相机权限，确认不崩溃、不保存；再授权并实际拍摄。AVD 相机使用 VirtualScene 或 Webcam 等可用配置。成功需要真实 CameraX 回调，不能按 Simulate PHOTO_CAPTURED 代替。
7. Grainger 通过正常入口进入。授予麦克风权限，并按 AVD 支持情况启用主机音频输入，先安静再持续发声；检查 ≥-30 dBFS、持续约 1 秒的默认规则。不同主机输入强度不同，记录读数；列表没有 Grainger Debug 启动按钮。不要点击 DEBUG: simulate sustained noise 来证明录音成功。

模拟器位置、姿态、音频功能见 [Extended controls 官方说明](https://developer.android.com/studio/run/emulator-extended-controls)。某些功能需要将模拟器放在独立窗口运行。

## 12. 生命周期、保存与回归

| 编号 | 操作 | 预期 |
|---|---|---|
| X01 | T2 开始计时约 1 秒时按 Home，停留 ≥5 秒再返回 | 未完成任务重新计时；如果模拟输入仍全有效，回来会再次自动开始，不应误判成后台累计 |
| X02 | T3 计时中返回地图，再进入 T2 | 进度和规则不串任务 |
| X03 | 在 DEBUG 入口退出后从正常入口进入 | 无 Fake 控件，实际走 Android 引擎 |
| X04 | 成功收集后退出登录，再登录同账号 | 收藏仍存在；检查重复收集是否产生重复奖励 |
| X05 | 完成前关闭模拟器网络，完成后观察，再恢复 | 不应在服务端未确认保存时直接显示成功；可能等待，或失败后显示 Retry Save；恢复后应可保存 |
| X06 | 保存失败时点击 Retry Save | 正常重试，无重复记录；如果无失败回调而一直等待，记录原样，不要求断网立即显示重试按钮 |

Logcat 选择目标模拟器和应用进程。崩溃保存 FATAL EXCEPTION 及调用栈；地图问题保存授权错误；保存问题记录界面错误、网络状态和时间。不要只记录“按钮没反应”。

## 13. 双模拟器组队补充

启动两个 AVD，分别登录 A、B，加入同一个 Room，确认共同目标和角色一致。该流程使用正常地图/组队入口，Debug 单人挑战不能替代双机同步测试。

South Lawn 四碎片坐标（纬度，经度）：

| 碎片 | 纬度 | 经度 |
|---|---:|---:|
| NW | -37.798490 | 144.959940 |
| NE | -37.798500 | 144.960570 |
| SW | -37.798850 | 144.959940 |
| SE | -37.798910 | 144.960530 |

用 AVD Location 输入各碎片点，等待有效定位。点击标记后 8 m 内应允许 Collect fragment，远离后不允许。A 收集时 B 的计数应同步；同时操作同一碎片不能重复计数。四片完成后继续验证后续任务、两人领取和等待状态，不能在 4/4 时就结束整条测试。

非 South Lawn 任务检查房主挑战和队友问答分别完成，一方完成应等待另一方。若 Emulator 无法提供所需姿态传感器，把对应正常挑战标 Blocked，记录到真机阶段继续。

## 14. 本阶段完成标准与记录表

第一阶段通过要求：自动测试通过；四个任务所有失败分支有效；手动计时和中断重置通过；T1 必须有照片事件才能完成；保存和重进读取通过；普通/模拟入口选择正确。Android 接口和双机项目逐条记录，不因逻辑通过自动勾选。

| 用例 | 模式（Fake/Android接口/双机） | 输入/操作 | 预期 | 实际 | Pass/Fail/Blocked | 截图/日志 |
|---|---|---|---|---|---|---|
| 待填写 | | | | | | |

现场真机测试继续使用 [现场测试手册](FIELD_TEST_MANUAL_ZH.md)。本指南为操作步骤，未声称已经代替团队在 Android Studio 图形界面执行了这些用例。
