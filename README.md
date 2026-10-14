# 泊车数据记录 App (Parking Logger)

一个用于测试人员记录泊车数据的 Android 应用。支持多角色选择、多泊车类型、结果评估、拍照与录音，数据本地存储，未来可批量导出 Excel。

---

## 项目定位

面向**泊车测试工程师 / 测试人员**的现场数据采集工具。用于在实地泊车测试中快速、规范化地记录每一次泊车操作的类型、条件、边界、操作把数、是否 IO、异常问题、现场照片和语音备注。

**核心目标**：
- 测试现场快速录入，减少纸质记录
- 结构化保存每次测试（session）下的多条泊车记录（分支测试）
- 字段与最终导出 Excel 的规范格式对应（Excel 模板待接入）
- 拍照 / 录音文件随记录保留，导出时可定位到对应文件

---

## 技术栈

| 层 | 技术 |
|----|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM (ViewModel + Repository) |
| 导航 | AndroidX Navigation Compose |
| 存储 | Room (SQLite) |
| 拍照 | CameraX / Activity Result + FileProvider |
| 录音 | MediaRecorder |
| 权限 | CAMERA、RECORD_AUDIO |

---

## 如何启动

### 环境要求
- Android Studio（Hedgehog 或更新版本，支持 AGP 8.2）
- JDK 17
- Android SDK 34（compileSdk）

### 导入运行
1. 用 Android Studio 打开本目录
2. 等待 Gradle 同步完成（首次会自动下载依赖）
3. 连接 Android 真机（≥ Android 8.0 / minSdk 26），开启 USB 调试
4. 点击 **Run ▶** 安装运行

### 命令行构建 APK
```bash
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

> 注意：本项目的照片 / 录音文件保存在 App 私有外部目录
> `Android/data/com.parking.logger/files/ParkingLogger/` 下，方便导出时定位。

### 协作约定（重要）

**push 之前必须本地编译通过**（本仓库曾多次推上编译不过的代码）：

```bash
./gradlew assembleDebug
```

仓库提供 `.githooks/pre-push` 钩子自动拦截编译不过的推送，**每台机器启用一次**：

```bash
git config core.hooksPath .githooks
```

临时跳过：`git push --no-verify`

> 本项目**模拟器不可用**（需管理员权限开启 WHPX/Hyper-V），运行时验证只能靠真机，
> 而真机安装由专门的构建机负责。推上编译不过的代码会让整条链路直接停住。

**依赖下载失败时**（Maven Central 在部分网络下不可达）：不要改 `settings.gradle.kts`，
改用本机 `~/.gradle/init.d/mirrors.gradle` 挂镜像，详见
[.github/copilot-instructions.md](.github/copilot-instructions.md)。

**⚠️ 提升 Room 数据库版本会清空真机数据**：`AppDatabase` 使用了
`fallbackToDestructiveMigration()`，升 `version` 会清空已有的测试人员／会话／泊车记录。
需要保留数据请写 `Migration`。

---

## 项目简介 / 核心流程

### 数据模型关系

```
TestSession（一次测试）           # 人员 + 地点 + 时间戳
  └── ParkingRecord（分支测试）    # 每次泊车操作
        ├── parkingType            # 泊车类型
        ├── condition              # 泊车条件
        ├── boundary               # 边界类型
        ├── totalCount             # 单一总把数（计数器统计）
        ├── isIO + ioIssues        # IO 结果 + 问题勾选
        ├── note                   # 整体备注
        ├── photoPath / audioPath  # 照片 / 录音
        └── pullOut*               # 泊出记录（正入/侧方时）
```

### 用户流程

```
首页
 ├─ 选择测试人员 + 输入地点 → 开始记录
 │      ↓
 │  Session 页（本次测试的分支列表）
 │      ↓ 新建泊车
 │  选择泊车类型（正入/反入/侧方/手动泊入后泊出）
 │  （此处显示每种类型已记录次数，可点"结束测试"）
 │      ↓
 │  选择泊车条件（按类型动态展示）
 │      ↓
 │  选择边界（线/石头/混合/马路牙子/无线只有车）
 │      ↓
 │  把数统计页（向前向后箭头，每点一次记一把，自动累加总把数）
 │      ↓
 │  结果评估页
 │   ├─ 显示本次泊车类型 + 总把数
 │   ├─ IO / 不IO 切换
 │   ├─ 不IO时勾选问题 + 备注
 │   ├─ 拍照（可选）
 │   └─ 录音（可选）
 │      ↓
 │  若为正入或侧方 → 泊出记录页（泊出类型/把数/结果/拍照）
 │      ↓
 │  保存 → 回到选择泊车类型页，继续下一次泊车
 │      ↓
 │  （选择类型页点"结束测试"→ 回到 Session 页）
 │
 └─ 人员管理页（增/删/改测试人员）
```

### 导航结构
记录主流程：`Home → Session → ParkingType → Condition → Boundary → Count → Evaluation → [PullOut]`
辅助页：`Home → 人员管理`、`Home → 历史记录&备份`

- Count 页：泊车把数计数器（前进 / 后退箭头**均 +1**，辅助统计总把数）
- 泊车记录保存后**返回选择泊车类型页**，可继续记录多次小记录
- 评估页顶部有"返回上一步"回到把数页，可修正计数
- 首页可进入 `人员管理` 与 `历史记录&备份`
- 历史记录页可查看所有测试大记录与明细、**导出 JSON 备份**、**清除所有缓存**
- Session 页右上角可删除**整个测试**（含所有子记录）

---

## 已实现功能（MVP）

- [x] 人员管理（添加 / 编辑 / 删除）
- [x] 场地测试（session）+ 时间戳
- [x] 4 种泊车类型选择 + 显示各类型已记录次数
- [x] 按类型动态展示泊车条件（正入/反入 6 种、侧方 4 种、泊出 2 种）
- [x] 5 种边界类型
- [x] 把数统计页：前进/后退箭头均加一把，取单一总把数；评估页可返回上一步修正
- [x] 结果评估：总把数、IO/不IO、问题勾选（7 项）、整体备注
- [x] 拍照（结果评估页 + 泊出页）
- [x] 录音（MediaRecorder，关联到记录）
- [x] **仅**正入 / 侧方自动进入泊出记录页（其他类型不弹泊出）
- [x] 泊出结果选项（OK/不OK/把数太多/无Offer/功能中断/碰撞风险/纵向控制）
- [x] 保存后返回类型选择页继续记录，支持"结束测试"
- [x] 历史记录页：查看所有测试大记录及明细（永久保存，退出不丢失）
- [x] 导出 JSON 备份 + 清除所有缓存
- [x] 删除单条记录、删除整个测试

---

## 待实现 / 计划中

> 用户已明确：**数据导出到 Excel 由后续接入规范 Excel 模板**，因此本次先不做导出。

- [x] 数据本地永久保存（Room），历史记录页可回查
- [x] 导出 JSON 备份（设备 App 目录）

### 近期
- [ ] **Excel 导出**：接入用户提供的规范 Excel 模板，批量导出所有记录（含把数、IO、勾选问题、照片/录音文件定位）
- [ ] 手动泊入后的泊出：完善泊出前是否需要条件/边界选择
- [ ] 结果评估页：把数与 IO 是否需要在保存时校验必填

### 后续
- [ ] **自动计算**：中断/总数百分比统计
- [ ] **Good PEP 判断**：侧方满足 `skew angle < 2.5°` 自动判定（需夹角计算）
- [ ] **云端同步预留**：数据后续可能同步到云端以便网页导出 Excel
- [ ] CSV / JSON 本地导出（过渡方案）
- [ ] 录音播放功能（在记录详情中播放）
- [ ] 深色主题支持

---

## 目录结构

```
parking-logger/
├── app/
│   ├── src/main/
│   │   ├── java/com/parking/logger/
│   │   │   ├── ParkingApp.kt          # Application，提供 repository
│   │   │   ├── MainActivity.kt        # 入口
│   │   │   ├── data/                  # 数据层
│   │   │   │   ├── model/             # 实体 & 枚举
│   │   │   │   ├── dao/               # Room DAO
│   │   │   │   ├── database/          # Room 数据库
│   │   │   │   └── repository/        # 仓库层
│   │   │   ├── ui/
│   │   │   │   ├── navigation/        # 导航图
│   │   │   │   ├── screens/           # 各页面
│   │   │   │   └── theme/
│   │   │   └── util/                  # 工具类（AudioRecorder）
│   │   └── res/                       # 资源（manifest/theme/file_paths）
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 主要数据表

| 表 | 说明 | 关键字段 |
|----|------|----------|
| `testers` | 测试人员 | id, name |
| `test_sessions` | 测试任务 | id, testerId, location, startTime |
| `parking_records` | 泊车记录 | id, sessionId(FK), timestamp, parkingType, condition, boundary, 6×count, isIO, ioIssues, note, photoPath, audioPath, pullOut* |

照片 / 录音文件路径保存在 `photoPath` / `audioPath` 字段（存放于 `ParkingLogger/photos/`、`ParkingLogger/audio/`）。
