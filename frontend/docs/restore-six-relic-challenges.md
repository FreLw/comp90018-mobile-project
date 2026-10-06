# 六个宝物挑战恢复：中文交接文档

交接日期：2026 年 10 月 6 日。开发分支：`jiayi/fix/restore-six-relic-challenges`。基于更新并确认干净的 `main`（`41d52ba`）开发，保留原有提交历史。实现与测试已分为 12 次提交；未推送、合并、rebase 或创建 PR。本文是给队友的代码审查、数据库配置和联调说明。

## 1. 问题原因

此前地图按钮、宝物卡片和多人房主到达后的 Start Hunting 入口，都把配置了挑战的宝物送到 `TreasureCompassGate`，所以六个宝物看到相同的 Trail / Bearing / Balance 条件。项目已有专属挑战类型、配置、Context Engine、evaluator 和挑战页面，但这些入口绕过了它们。详情页也存在旧的本地挑战分流和定时解锁代码。

另外，System Garden 的配置沿用了 Old Quad 的水平、静止、稳定挖掘条件；拍照面板只支持 Union Lawn。专属挑战入口还曾在所有 Debug 构建中强制启用传感器模拟，导致普通运行不能正确验证真实传感器流程。

## 2. 六个挑战现在分别做什么

下表是单人挑战的行为。GPS 必须经过已有有效性、精度及连续位置读数校验；各宝物原有半径没有调整。

| 宝物与任务 | 必须满足的条件 | GPS 半径 | 持续时间／完成动作 |
| --- | --- | --- | --- |
| Union Lawn：Lost Lake Photograph | 有效 GPS、配置的指南针方向对准 | 25m | 保留 800ms 对准等待，随后 CameraX 成功拍照 |
| Wilson Hall：Stone Rosette | 有效 GPS、正确方向、静止、稳定、无旋转 | 20m | 连续保持 3 秒，中断后重新计时 |
| Old Quad：Fossil Excavation | 有效 GPS、手机水平、静止、稳定、无旋转；无需指南针方向 | 18m | 连续保持 3 秒，中断后重新计时 |
| South Lawn：Atlas Sculpture | 有效 GPS、配置的观看方向、静止、稳定、无旋转 | 15m | 保留配置的 1.2 秒持续时间 |
| System Garden：Lost Glasshouse | 有效 GPS；无需方向、水平、静止或稳定 | 22m | CameraX 成功拍照，无额外传感器保持时间 |
| Grainger Museum：Tone-Tool | 有效 GPS、麦克风权限、声音达到配置阈值；无需方向或稳定 | 18m | 当前阈值为 -30dBFS，连续持续 1 秒 |

South Lawn 多人模式仍优先进入原有四碎片协作寻宝流程，不替换成单人的观看角度挑战。原有最终领取、房间状态和成员领取等待流程保留。

## 3. 代码具体改了什么

### 挑战入口与配置校验

- 地图 Start Hunting 按钮、宝物卡片、详情页及房主到达入口，统一进入 `TreasureChallengeRoute`，使用原有 `TreasureChallengeViewModel` 和 `ChallengeRuleEvaluator`。
- 新增 `TreasureChallengeRouting.kt`：检查宝物 ID 与 `RelicChallengeType` 是否对应，保留“房主做挑战、待完成的成员做问答”的入口选择。
- 新增 `ChallengeConfigValidation.kt`：检查各任务必需的配置规则；Firestore parser 和地图路由都使用这类校验。
- 缺失、禁用、规则不完整或类型不匹配的挑战配置显示“Challenge unavailable”，不能直接跳过任务进入揭示页面。System Garden 的旧挖掘配置也会被拒绝。
- 普通 Debug 运行使用真实传感器；只有显式进入调试挑战时才启用模拟控制。

### 拍照与揭示流程

- 在 `RelicChallengeConfigs.kt` 和 `database/treasures.json` 中把 System Garden 改为 GPS＋拍照任务，移除挖掘条件。
- 复用 `UnionPhotoPanel` 和 `CameraXCapture`，让 Union Lawn 与 System Garden 共用拍照 UI。
- ViewModel 仅在已有拍照机会、处于 CAPTURING 状态且收到有效非空本地 `content://` URI 时接受成功结果。UI 仅在 CameraX 返回 URI 且没有错误时提交成功回调。
- 开相机、授权、点快门、空 URI、无效 URI、失败回调或未开始拍摄就收到的回调，都不能完成任务；失败后可以重试。
- 保留原有拍照就绪状态锁存机制，避免按快门时的手机移动使已取得的拍照机会丢失。
- 照片保存在设备 MediaStore，没有上传照片，也没有新增 Firebase collection。没有增加图片识别、内容验证、AI、AR 或玻璃屋覆盖图。
- 保存成功后直接进入原有宝物揭示页面。历史图片过渡组件保留，但不再是简化拍照流程的必经步骤。

### 专属 UI 与原有功能

- 各任务显示自己的说明和 evaluator 实际要求的传感器条件，不再统一显示三个固定条件。
- 相机控件只出现在拍照任务；麦克风权限控件只出现在声音任务。
- “挑战完成”和“宝物保存成功”仍是两个不同状态。保存失败可重试，并阻止宝物揭示。
- 成功震动仍由原有保存确认／多人领取确认逻辑触发。独立的 20m 接近震动阈值未修改。
- 原有 GPS／传感器处理、Context Engine、收藏、多人问答、South Lawn 碎片、最终领取和导航揭示逻辑继续复用。

## 4. Firestore：需要队友手动更新

**本次没有读取或修改 live Firestore。仅修改本地配置与 JSON 文件，不会自动更新线上数据库。**

请负责数据库的队友在有权限并确认后，更新 `treasures` collection 中 `system_garden_glasshouse` 文档的 `challenge` map 内以下字段。下面是字段更新清单，不是替换整个文档或整个 challenge map 的内容：

```json
{
  "photoActionRequired": true,
  "requiresStationary": false,
  "requiresStability": false,
  "requiresRotationStill": false,
  "requiresHorizontal": false,
  "requiredHeadingDegrees": null,
  "holdDurationMs": 0,
  "requiresSound": false
}
```

操作建议：

1. 记录该文档更新前的配置，便于核对和回退。
2. 只更新上述嵌套字段。保持 `type = SYSTEM_GARDEN_GLASSHOUSE`、challengeId、enabled、坐标及 22m GPS 半径等其他字段不变。
3. 检查 Union Lawn、Wilson Hall、South Lawn 的 requiredHeadingDegrees 非空，六个文档的类型和规则与本地配置一致；不要调整其他宝物半径或 20m 震动阈值。
4. 在使用线上 catalogue 的客户端确认配置已刷新，再测试 System Garden。更新前，该客户端会阻断旧挖掘配置，这是防止绕过拍照要求的预期行为。

不要为本次功能新增 Firebase collection，也不需要上传照片或历史图片资产。

## 5. 已验证的内容

这些结果来自上一轮实现的最终代码验证；本次中文交接文档更新不涉及功能改动。

| 验证 | 结果 |
| --- | --- |
| `./gradlew :app:testDebugUnitTest` | 212 项通过，0 failure／error |
| `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` | 构建通过 |
| 相关 `:app:connectedDebugAndroidTest` | Medium_Phone 模拟器，Android 17／API 37，7 项全部通过 |
| `git diff --check` | 上一轮检查通过 |

模拟器测试类：`TreasureChallengeUiTest`、`HapticSessionBindingTest`、`RoomClaimHapticUiTest`。测试覆盖专属 UI、Room claim 确认时机与震动绑定。测试时启动的无窗口模拟器已关闭。

新增单元测试覆盖六类配置与路由、宝物 ID／类型不匹配、缺失方向、旧 Garden 配置拒绝、Garden 无需水平稳定但必须拍照、Union 方向门禁、拍照失败／无效 URI／重试、麦克风不可用及声音中断、成员问答兼容、保存重试与成功震动时机。完整测试还包含已有 GPS、传感器、碎片、多人领取和震动回归。

中间一次 AndroidTest 构建发现 Compose assertion 的错误 import，已在 `a37778c` 修复，后续构建及测试通过。

**尚未实测：真实设备相机拍摄、校园 GPS／指南针校准、真实声音阈值、物理震动，以及 live Firestore 双设备联调。** 模拟器测试中的保存回调使用测试实现，不能当作线上保存或多人事务已验证的证明。

在 `frontend` 目录复现：

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
```

启动可用模拟器后运行相关 instrumentation：

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.comp90018.app.features.treasurechallenge.TreasureChallengeUiTest,com.comp90018.app.features.haptics.HapticSessionBindingTest,com.comp90018.app.features.rooms.RoomClaimHapticUiTest
```

## 6. 队友接手后的检查顺序

1. **代码审查**：查看 MapScreen 入口统一、配置校验、CameraX 成功回调判定和保存后揭示流程，确认六个任务及多人 South Lawn 特例符合团队设计。
2. **数据库配置**：人工完成第 4 节 Garden 字段更新，并确认其他文档没有缺失必需规则。现在的校验更严格，旧的缺失方向或弱化规则配置会显示不可用。
3. **实机权限与拍照**：验证两个拍照任务的拒绝授权、重新授权、捕获失败、重试、成功保存、照片本地展示；Union 未对准时应无法拍照。
4. **校园传感器验证**：校准 GPS、磁北方向、稳定判断与声音阈值。坐标和方向仍沿用已有配置，不能视为已完成实地校准。
5. **保存与震动**：模拟网络失败后重试；确认挑战完成时不会提前成功震动，只有保存／最终领取确认后才震动与揭示，重复回调不重复震动。
6. **双设备多人联调**：非 South Lawn 宝物验证房主挑战、成员问答和最终领取；South Lawn 验证四碎片、共享状态、最终领取和已领取成员等待页。

需要推送、创建 PR 或合并时，再取得批准；当前分支只在本地。

## 7. 修改文件清单

- `database/treasures.json`
- `frontend/app/src/androidTest/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeUiTest.kt`
- `frontend/app/src/debug/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeSimulationFactory.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/contextengine/challenge/ChallengeConfigValidation.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/contextengine/challenge/RelicChallengeConfigs.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/data/treasure/FirebaseTreasureRepository.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/MapScreen.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/PostChallengeRevealSession.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/map/TreasureChallengeRouting.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeScreen.kt`
- `frontend/app/src/main/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeViewModel.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/contextengine/challenge/ChallengeRuleEvaluatorTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/data/treasure/ChallengeCalibrationParsingTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/map/PostChallengeRevealSessionTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/map/TreasureChallengeRoutingTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/treasurechallenge/ChallengeHapticSaveRegressionTest.kt`
- `frontend/app/src/test/kotlin/com/comp90018/app/features/treasurechallenge/TreasureChallengeViewModelTest.kt`
- `frontend/docs/restore-six-relic-challenges.md` （本交接文档）

## 8. 实现提交记录

| 提交 | 内容 |
| --- | --- |
| `9b3a7ae` | System Garden 改为 GPS 门禁拍照任务 |
| `1e865a0` | 地图入口进入宝物专属挑战 |
| `b03565f` | 拒绝不完整挑战配置 |
| `6b2bdff` | 复用相机面板并要求成功捕获 |
| `463b5df` | 专属任务说明与传感器条件 UI |
| `9e7be4f` | Garden 拍照规则与缺失方向测试 |
| `1b25144` | 宝物身份校验与成员问答路由 |
| `5b32766` | 六类路由、相机重试与麦克风测试 |
| `8bc7481` | 挑战 UI 和保存确认震动集成测试 |
| `a37778c` | 修复 Compose 测试 import |
| `49b5aae` | 拍照保存后直接打开宝物揭示 |
| `3c4f857` | Garden parser 迁移测试与交接报告 |

以上 12 次提交为功能实现与测试里程碑。本次中文文档更新在此基础上完成，未重写上述提交。
