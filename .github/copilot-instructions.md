# 泊车记录 App — 协作与构建约定

> 本文件面向在本仓库工作的开发者与 AI 助手，说明本项目的构建要求和历史上踩过的坑。

## 一、最重要的一条：push 前必须编译通过

本仓库曾**多次**出现「提交推上去但编译不过」的情况，导致负责构建/装机的一侧反复返工修补。

因此：**任何 push 之前，必须先本地编译通过。**

```bash
./gradlew assembleDebug
```

仓库已提供 `.githooks/pre-push` 钩子自动做这件事。**每台机器启用一次即可**：

```bash
git config core.hooksPath .githooks
```

启用后，编译不过的提交无法被推送；确需跳过时用 `git push --no-verify`。

### 协作分工（为什么这条很重要）

| 角色 | 职责 |
|------|------|
| 开发机 | 写代码 → 编译验证 → push |
| 构建机 | `git pull` → 编译 → 安装到真机 → 回归测试 |

因为模拟器在本项目环境下不可用（需要管理员权限开启 WHPX/Hyper-V），**运行时验证只能靠真机**，
而真机安装由构建机负责。所以一旦推上来的是编译不过的代码，整条链路会直接停住。

---

## 二、项目事实

| 项 | 值 |
|----|----|
| 语言 / UI | Kotlin 1.9.20，Jetpack Compose（BOM `2024.02.00`），Material 3 |
| 构建 | AGP 8.2.0，Gradle 8.5（Wrapper 已随仓库提供），JDK 17 |
| SDK | compileSdk / targetSdk 34，minSdk 26 |
| 持久化 | Room 2.6.1（KSP 处理器） |
| 序列化 | Gson 2.10.1 |

`gradle.properties` **必须存在**且包含 `android.useAndroidX=true`，否则 AGP 会在
`:app:checkDebugAarMetadata` 阶段直接失败。该文件已提交到仓库，请勿删除。

---

## 三、历史踩坑（改代码前请对照）

这几类错误都曾真实导致过「推送后构建失败」：

| 编译报错 | 根因 | 正确写法 |
|----------|------|----------|
| `Unresolved reference: KeyboardOptions` | import 到了不存在的包 | `androidx.compose.foundation.text.KeyboardOptions`，**不是** `androidx.compose.ui.text.input.KeyboardOptions`（后者不包含该类） |
| `Unresolved reference: runCatching` / `valueOf` | 把顶层函数当成员函数调用 | `runCatching { ParkingType.valueOf(x).label }`，**不是** `ParkingType.runCatching { valueOf(x) … }` |
| `Unresolved reference: <局部函数名>` | Kotlin **局部函数不支持前向引用** | 被调用的局部函数必须定义在调用点**之前** |
| `Unresolved reference: rememberCoroutineScope` | 漏 import | `androidx.compose.runtime.rememberCoroutineScope` |
| `contains AndroidX dependencies, but android.useAndroidX property is not enabled` | 缺 `gradle.properties` | 确认该文件存在且含 `android.useAndroidX=true` |

> 经验：同类 import 直接参考同仓库中**已能编译**的文件（如 `HomeScreen.kt`、`PullOutScreen.kt`），比凭记忆写更可靠。

---

## 四、Room 数据库与迁移

自 version 4 起，`AppDatabase` 已改为**显式 `Migration`**（`MIGRATION_2_4`、`MIGRATION_3_4`），
不再使用 `fallbackToDestructiveMigration()`——**升级不再清空真机数据**。

> 历史教训：version 1→2、2→3 曾两次因破坏性迁移清空真机数据。
> 如果以后的改动确实不介意清空，请在提交信息里**明确写出「本版本会清空真机数据」**。

### ⚠️ 最容易踩的坑：迁移后表结构校验失败（App 启动即闪退）

```
java.lang.IllegalStateException: Migration didn't properly handle: parking_records(...)
    at androidx.room.RoomOpenHelper.onUpgrade(RoomOpenHelper.kt:93)
```

Room 在迁移完成后，会把**实际表结构**与**由实体推导出的期望结构**逐项比对——
字段、类型、NOT NULL、默认值、索引、**外键**，任何一处不一致都会抛异常并让 App 启动即崩溃。

手写迁移最常见的疏漏是**重建表时漏掉外键约束**（真实发生过一次）：

```sql
-- 重建 parking_records 时必须带上外键，且 ON UPDATE / ON DELETE 都要写全
FOREIGN KEY(sessionId) REFERENCES test_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
```

**获取 Room 生成的「规范 DDL」的两种办法：**

1. 从已构建的 APK 里搜 dex 中的字符串 `CREATE TABLE IF NOT EXISTS`；
2. 把 `exportSchema` 设为 `true` 并配置 `room.schemaLocation`，构建后读生成的 schema JSON。

> 建议：迁移代码写完后，**在带有旧版本数据的真机上实测一次升级路径**，
> 不要只测全新安装（全新安装不会触发迁移，问题会被掩盖）。

---

## 五、网络环境（中国大陆）

`repo.maven.apache.org`（Maven Central）在部分网络下**不可达**（TLS 握手被重置），
表现为依赖解析失败。推荐用 Gradle 初始化脚本挂镜像，**不要去改仓库里的 `settings.gradle.kts`**：

`~/.gradle/init.d/mirrors.gradle`

```groovy
def PUBLIC = ['https://maven.aliyun.com/repository/public',
              'https://mirrors.cloud.tencent.com/nexus/repository/maven-public']
def GOOGLE = ['https://maven.aliyun.com/repository/google',
              'https://mirrors.cloud.tencent.com/nexus/repository/google']

settingsEvaluated { settings ->
    settings.pluginManagement.repositories {
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        PUBLIC.each { u -> maven { url u } }
        GOOGLE.each { u -> maven { url u } }
    }
    settings.dependencyResolutionManagement.repositories {
        GOOGLE.each { u -> maven { url u } }
        PUBLIC.each { u -> maven { url u } }
    }
}
```

（写成 `init.d` 脚本的好处：属于本机配置，不进入仓库，因此不会与 `git pull` 冲突。）

---

## 六、语音录入（SpeechRecognizer）：真机实测结论

### 6.1 必须声明 `<queries>`，否则功能完全不可用

`targetSdk >= 30` 时，若 manifest 不声明下面的 `<queries>`，`SpeechRecognizer.isRecognitionAvailable()`
**恒为 `false`**——即使设备已装识别服务也查不到。表现为「语音录入」按钮**置灰**、无法点击。

```xml
<queries>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
</queries>
```

已修复（见 `AndroidManifest.xml`）。真机验证：修复前按钮置灰，修复后可点击。

> 排查提示：`adb shell pm query-services -a android.speech.RecognitionService` 不受包可见性限制，
> 可用来区分「设备真的没有识别服务」与「被包可见性挡住」。

### 6.2 真机（小米 23116PN5BC / Android 14）上云端识别连不通 —— 尚未解决

实测日志（`adb logcat`，小米 `com.xiaomi.mibrain.speech/.asr.AsrService`）：

```
AsrService: onStartListening: isCTAAllow=true isRecordPermGranted=true useLocal=false
AsrService: startEngine → startAudioInput → onPreSpeakBegin       ← 已就绪
    ~5 秒后
EngineCapabilityUtils: onError code=50010004 msg=timeout at stage:DIALOG_START
AivsSDK-[OkHttp http://111.13.40.86:8080/] LiteCryptWsClient:
    onFailure: java.net.SocketException: Socket closed
```

**结论：App 侧无问题**（权限已授予、识别服务已绑定、引擎已启动）。
失败发生在小米识别服务**建立云端会话**阶段（WebSocket 到 `111.13.40.86:8080` 建连失败）。
关闭手机 VPN 后仍失败，说明不是代理拦截。

可能原因（未最终确认）：
1. 该云端端点在当前网络下不可达；
2. 或 MIUI 的云端 ASR 对**第三方应用**不开放（系统应用可能持有预认证会话）。

**可尝试的方向**（按性价比排序）：
1. 让识别走本地引擎，绕开云端：`RecognizerIntent.EXTRA_PREFER_OFFLINE = true`
   （服务日志中的 `useLocal=false` 可能正是由该 extra 决定的）
2. 换网络（Wi-Fi / 蜂窝）验证是否为网络可达性问题
3. 对照测试：MIUI 键盘的语音输入、小爱同学是否可用
4. 兜底：语音不可用时，距离字段仍可**手动输入**（注意不要因此让按钮处于不可用状态而无提示）

### 6.3 已修复的相关缺陷

| 缺陷 | 影响 |
|------|------|
| 所有 `onError` 分支静默重试、无任何提示 | 云端持续失败时界面永远停在「正在监听…」，用户零反馈，故障无法察觉（改为：提示错误 + 连续失败 3 次后停止） |
| `onResults` 在 `matches` 为空时提前 `return` | 一次空结果会让连续监听**永久停住**（改为：无论是否有结果都继续监听） |

### 6.4 语音文本解析（`VoiceDistanceParser`）已修复的 3 个错误

| 输入 | 修复前 | 修复后 |
|------|--------|--------|
| 「右后 2.5」 | 填到 **Back**（`Back` 的单字别名「后」抢先命中） | BR |
| 「左前 2.5」 | 填到 **Front**（`Front` 的单字别名「前」抢先命中） | FL |
| 「第1个 2.5」 | 值取成 **1.0**（正则先抓到编号里的「1」） | 2.5 |
| 「十五点五」 | **105.5**（`十` 被按位拼接） | 15.5 |

要点：标签匹配必须**长别名优先**（`LABEL_MATCHERS` 按长度倒序）；抽数字前要先剔除编号词与标签词；
中文整数需按「十」作位标记解析，而小数部分要按位拼接。

