# UI 目录与维护指南

三个入口文件保留原有调用接口，只负责连接状态、选择页面和传递事件。修改某个界面时，先找对应目录里的页面文件；地图 SDK、图片读取和资料保存流程有各自的文件。

## Map：地图、寻宝页面与渲染

目录：`app/src/main/kotlin/com/comp90018/app/features/map/`

| 位置 | 负责什么 | 什么时候修改 |
| --- | --- | --- |
| `MapScreen.kt` | 接收应用状态，计算地图输入，连接组队事件与页面 | 调整跨页面协调和地图输入 |
| `route/MapNavigationState.kt` | 各寻宝页面的选择状态及原有 remember key | 调整哪些状态随队伍会话或用户变化而重置 |
| `route/MapHuntContent.kt` | 按原有优先级显示组队、罗盘、答题、挑战、揭晓和详情页 | 调整寻宝页面入口和返回回调 |
| `components/MapExplorerContent.kt` | 普通地图、提示、宝物卡片和开发控件的布局 | 调整地图主页面外观 |
| `components/MapControls.kt` | 地图提示、视角选择、位置过期提示等小组件 | 修改地图控件样式 |
| `components/LocationPermissionPrompt.kt` | 定位权限提示 | 修改定位权限界面 |
| `components/MapTextFormatting.kt` | 地图距离、角度等文字格式 | 修改数值显示方式 |
| `rendering/GoogleMapView.kt` | Compose 与 Google Maps 的连接、地图更新及生命周期 | 修改地图 SDK 集成 |
| `rendering/MapMarkerRenderer.kt` | 宝物、碎片、自己和队友的标记绘制与缓存 | 修改地图标记 |
| `rendering/MapCameraController.kt` | 镜头取景、视角跟随、地图坐标换算 | 修改地图镜头行为 |
| `rendering/GuidingThreadRenderer.kt` | 导航线覆盖层 | 修改导航线绘制 |
| `compass/TreasureCompassGate.kt` | 寻宝前的罗盘、条件灯、星星和动画 | 修改罗盘入口界面 |
| `team/TeamHuntDialogs.kt` | 到达提示及组队等待页面 | 修改组队提示 |
| `team/MemberHuntQuizScreen.kt` | 队员问答界面；题目状态仍由原 ViewModel 管理 | 修改题目呈现 |
| `team/FragmentHuntScreens.kt` | South Lawn 碎片搜索和领取等待页面 | 修改碎片任务界面 |
| `detail/TreasureDetailScreen.kt` | 详情、挖掘、发现、故事的原有阶段切换 | 修改详情阶段协调 |
| `detail/TreasureDetailsContent.kt` | 宝物信息、展开卡片、宝物图片与发现面板 | 修改详情内容布局 |
| `detail/TreasureSearchPanel.kt` | 挖掘前的扫描面板连接 | 修改扫描面板输入 |
| `detail/HuntScannerArtwork.kt` | 扫描仪、宝箱等绘图 | 修改扫描和挖掘视觉 |
| `detail/LegacySensorHuntPanel.kt` | 保留的旧传感器任务与麦克风权限流程 | 维护旧流程；不要仅凭当前入口不可达就删除 |
| `sensors/MapSensorBindings.kt` | 地图现有方向和运动传感器绑定 | 修改地图传感器呈现 |
| `debug/MapDebugControls.kt` | 挑战预览、距离和方向模拟控件 | 修改开发调试界面 |
| `MapDiscoverySave.kt` | 原有宝物保存边界 | 修改保存回调连接 |

`MapHuntContent` 保留原分支顺序。`MapNavigationState` 保留原状态重置边界：会话变化清除对应页面选择，最终组队领取期间的 Atlas 拼合动画只随用户变化重置。动画、权限、触觉、保存及共享位置规则没有在这次拆分中重新设计。

## Rooms：入口、聊天、组队任务与设置

目录：`app/src/main/kotlin/com/comp90018/app/features/rooms/`

| 位置 | 负责什么 |
| --- | --- |
| `RoomsScreen.kt` | 选择加入房间、演示房间或当前房间聊天 |
| `entry/RoomEntryScreen.kt` | 加入、创建和浏览房间入口 |
| `RoomPlazaScreen.kt` | 真实公开房间列表、满员排序及加入确认；已移除本地样例房间和预览开关 |
| `chat/TeamRoomChatScreen.kt` | 当前房间聊天、成员栏、消息呈现及子页面协调 |
| `hunt/RoomHuntControls.kt` | 队伍任务状态、碎片进度和启动按钮 |
| `hunt/DestinationPickerDialog.kt` | 任务目的地选择 |
| `members/RoomMemberProfileScreen.kt` | 队友资料、好友关系和私聊入口 |
| `settings/RoomDetailsDialog.kt` | 房间详情、成员管理和退出/解散动作 |
| `settings/RoomSettingsForm.kt` | 创建和编辑房间的共用表单 |
| `demo/DemoRoomScreen.kt` | 原有 `CAMPUS-2026` 本地演示房间 |

演示房间仍保留原入口条件和原行为。`demo/` 表示它的职责；它不是 Android 的 `src/debug` 构建目录。本次不改变正式版本是否能进入该演示房间。

## Profile：概览、编辑、设置和头像

目录：`app/src/main/kotlin/com/comp90018/app/features/profile/`

| 位置 | 负责什么 |
| --- | --- |
| `ProfileScreen.kt` | 在概览、编辑、设置之间切换 |
| `overview/ProfileOverview.kt` | 头像、资料概览和功能入口 |
| `edit/EditProfileScreen.kt` | 编辑表单与保存状态的呈现 |
| `edit/ProfileFormFields.kt` | 表单输入框和性别选择 |
| `edit/ProfileSaveActions.kt` | 先上传新头像，再保存资料；保留原错误和旧头像处理 |
| `settings/ProfileSettingsScreen.kt` | 设置开关、保存状态和原有失败回滚 |

公共头像组件在 `ui/components/ProfileAvatar.kt`，头像读取在 `ui/images/AvatarImageLoader.kt`。好友、房间、私聊可以共用头像组件，不必依赖 Profile 页面文件。

## 本次旧代码清理的边界

只删除了没有任何源代码、测试或预览调用的三个私有函数：`HuntFactCard`、`RadarAnimation`、`huntMarkerIcon`，以及从未读取的 `navigating` 状态变量。先运行原测试基线，再单独验证清理结果。

旧 `SEARCHING` 分支、计时器、传感器和权限流程保留在 `detail/` 中。现有测试没有完整覆盖它们，因此这次只移动代码，不删除或改变行为。绘图循环、位图裁剪与缓存也保留。

## 本次拆分的验证记录

- 原代码先通过 345 项单元测试基线；删除上述无调用代码后，再次通过 Debug 构建和全部单元测试。
- 拆分后通过 Debug 构建、Release Kotlin 编译、345 项单元测试和 32 项模拟器测试。两个相机权限用例分别恢复未授权状态后运行，验证拒绝、重试和拍照揭晓。
- 对照原代码，99 个直接搬移的声明、地图布局、寻宝分支顺序及回调、11 个导航状态的 remember 表达式保持一致。
- 新增导航状态测试覆盖会话与用户切换时的重置边界；资料保存测试覆盖头像上传顺序、失败处理和上传期间继续编辑性别/简介的读取时机。
- 对照 12 张拍照与揭晓流程的前后截图，尺寸一致，抽查未发现布局变化。系统时钟、实时相机画面和动画帧会产生像素差异。
