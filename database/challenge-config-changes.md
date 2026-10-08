# 宝藏任务数据库配置修复

记录日期：2026-10-06。

**执行状态：更新脚本已准备，线上只读预览已通过；本次记录不代表已执行线上更新。**

## 目标数据库

| 项目 | 值 |
|---|---|
| Firebase Project ID | `mobile-melbourne` |
| Firestore Database | `(default)` |
| Collection | `treasures` |
| 配置位置 | 各宝藏文档的 `challenge` map |

Android App 的 `frontend/app/google-services.json` 指向 `mobile-melbourne`。仓库 `.firebaserc` 中的 `STAGING` 别名已于 2026-10-09 更新为 `mobile-melbourne`，与 Android App 配置一致。执行数据库更新脚本时仍应显式指定 `mobile-melbourne`。

## 修复原因

当前 App 会校验每个专属 Challenge 是否具备该任务必需的条件。Union Lawn、Wilson Hall 和 South Lawn 的单人挑战需要固定朝向，但线上 `requiredHeadingDegrees` 为 `null`；System Garden 的线上配置仍采用旧的静止与姿态考核条件，与当前拍照任务不符。这些配置未通过校验时，App 会显示 `Challenge unavailable`。

Firestore rules 控制读写权限。部署 rules 不会补充或修改这里的任务配置。

## 字段改动

以下“更新前”取自本次运行更新脚本时的线上只读预览。

### Union Lawn / Lake

文档路径：`treasures/union_lawn_lost_lake`。

| 字段 | 更新前 | 更新后 | Firestore 类型 |
|---|---|---|---|
| `challenge.requiredHeadingDegrees` | `null` | `288` | number |

### Wilson Hall

文档路径：`treasures/wilson_hall_rosette`。

| 字段 | 更新前 | 更新后 | Firestore 类型 |
|---|---|---|---|
| `challenge.requiredHeadingDegrees` | `null` | `197` | number |

### South Lawn

文档路径：`treasures/south_lawn_atlas`。

| 字段 | 更新前 | 更新后 | Firestore 类型 |
|---|---|---|---|
| `challenge.requiredHeadingDegrees` | `null` | `185` | number |

这项方向配置用于单人观看角度挑战。Room 中的 South Lawn 仍采用共享碎片收集流程，本次数据库改动不改变该流程。

### System Garden

文档路径：`treasures/system_garden_glasshouse`。

| 字段 | 更新前 | 更新后 | Firestore 类型 |
|---|---|---|---|
| `challenge.photoActionRequired` | `false` | `true` | boolean |
| `challenge.requiresStationary` | `true` | `false` | boolean |
| `challenge.requiresStability` | `true` | `false` | boolean |
| `challenge.requiresRotationStill` | `true` | `false` | boolean |
| `challenge.requiresHorizontal` | `true` | `false` | boolean |
| `challenge.holdDurationMs` | `3000` | `0` | number |

`challenge.requiredHeadingDegrees` 保持 `null`。Garden 的专属任务要求到达目标区域并拍照，不要求固定方向，也不要求静止保持三秒。

## 方向字段说明

`requiredHeadingDegrees` 表示专属 Challenge 要求玩家面向的目标方向，单位为度：北为 0°、东为 90°、南为 180°、西为 270°。当前配置的 `headingReference` 为 `magnetic`，按磁北作为参考。

App 将手机测得的朝向与该字段比较，并使用 `headingToleranceDegrees` 判断误差是否合格。`null` 表示没有设置固定目标方向；是否允许为空取决于任务类型。

进入任务前的指南针页面使用当前位置和宝藏坐标计算朝向；它不会自动填充专属 Challenge 的固定方向字段。

288°、197°、185°来自本地 `database/treasures.json` 的开发估算配置，尚未完成现场校准。更新时保留现有 `calibrationStatus`，当前值为 `pending`。

## 执行步骤

在项目根目录运行，需要 Python 3.8+、Node.js，以及全局安装并已登录的 Firebase CLI。脚本会复用 Firebase CLI 登录，无需安装额外 Python 包或创建服务账号密钥。

先预览，确认目标项目和字段变化：

```powershell
python database/update-challenge-configs.py --project mobile-melbourne
```

执行线上更新：

```powershell
python database/update-challenge-configs.py --project mobile-melbourne --apply
```

如果提示登录失败，先重新登录再运行：

```powershell
firebase.cmd login --reauth
```

## 更新范围与备份

- 脚本只处理上述四个宝藏文档，按字段更新，不覆盖完整文档。
- Old Quad、Grainger Museum、用户、房间、收藏和 Firestore rules 不在更新范围内。
- 更新前将四个文档备份到 `tmp/challenge-config-backups/`，文件名包含项目 ID 和时间戳。此目录被 Git 忽略，不包含登录凭据。
- 写入使用读取时的文档更新时间作为前提；期间若有其他修改，整个批次会中止，需要重新预览并执行。
- 更新后回读四个文档，核对目标字段。再次执行时，已符合配置的字段会跳过。

现有 `import-treasures.mjs` 会替换六个完整宝藏文档；本次修复使用 `update-challenge-configs.py`。

## 验证结果与后续检查

已完成：脚本离线验证，以及对 `mobile-melbourne` 的线上只读预览。离线验证覆盖预览不写入、字段更新范围、备份、更新时间前提、回读验证、重复执行和其他字段保留。

待执行：线上 `--apply` 写入，以及更新后的 App 验证。

执行成功后，联网重新打开 App，依次检查 Union Lawn、Wilson Hall、South Lawn 单人挑战和 System Garden 能否进入专属 Challenge。正常流程为：`Start Hunting → 指南针考核 → Hunt → 专属 Challenge`。这些字段修复应解除已诊断的配置校验失败；真实任务能否完成仍需要现场传感器与方向校准验证。
