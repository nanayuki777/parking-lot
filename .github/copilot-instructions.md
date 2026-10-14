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

## 四、Room 数据库：升版本会清空用户数据

`AppDatabase` 目前使用 `fallbackToDestructiveMigration()`。这意味着：

> **只要提升 `AppDatabase` 的 `version`，用户设备上已有的全部数据
> （测试人员、测试会话、泊车记录、以及记录关联的照片/录音索引）都会被清空。**

历史上已因此清空过真机数据两次（version 1→2、2→3）。

如果某次改动需要保留数据，请写正式的 `Migration`，而不是单纯把版本号 +1。
如果不介意清空，也请在提交信息里**明确写出「本版本会清空真机数据」**，方便构建机提前告知测试人员。

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
