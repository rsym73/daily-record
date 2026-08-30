# 每日记录（Daily Record）

一个记录「每天做了什么」并追踪**连续完成天数**的 Android 应用。核心是一条严格的规则：**必须每天完成记录**，没完成当天的记录就不能进入下一天；一旦漏掉某天，唯一出路是清除连续天数、重新开始。

## 核心规则

| 概念 | 规则 |
|------|------|
| **一天边界** | 凌晨 1 点（设备本地时区）。0:00–1:00 仍属于「前一天」。 |
| **完成（Complete）** | 点「完成今天」即完成（不要求有条目）。完成后当天内锁定条目的添加/编辑/删除，撤销后恢复。 |
| **冻结（Freeze）** | 跨过凌晨 1 点后，当天变为只读历史，不可再编辑或完成。 |
| **严格锁** | 不能补写过去、不能提前写未来、没有宽限期。 |
| **断链（Broken Chain）** | 漏掉一天 → 后续记录页锁死，只能「清除连续天数、重新开始」。 |
| **连续天数（Streak）** | 从最近一天起连续完成的天数；断链时保留断链前达到的值并标红。 |
| **重置（Reset）** | 断链后的唯一出路：连续天数归零、从今天重新起链，历史保留为只读。 |

> 完整术语定义见 [`CONTEXT.md`](CONTEXT.md)，严格锁的架构决策见 [`docs/adr/0001-strict-daily-lock.md`](docs/adr/0001-strict-daily-lock.md)。

## 功能

- **今天页**：streak 显示、条目列表、添加/编辑/删除、完成/撤销、断链重置入口
- **历史日历**：回看过去每天的状态（完成 ✓ / 断链 ✗），点某天看只读详情
- **提醒**：每天可配置时间（默认 0:30）在边界前提醒「今天还没记录」（当天已完成则不提醒）
- **备份**：JSON 导出/导入，完整保留所有记录页、条目、完成时间

## 技术栈

- **Android 原生**：Kotlin + Jetpack Compose + Material 3
- **架构**：分层 —— 纯领域引擎 / 数据层（Room）/ 用例层（Repository）/ UI（Compose + ViewModel）
- **持久化**：Room（条目、完成天、键值对）
- **通知**：WorkManager
- **测试**：JUnit 5（领域）+ Robolectric + 内存 Room（数据层），本地 JVM 即可跑，无需模拟器

## 项目结构

```
daily-record/
├── domain/                # 纯 Kotlin 领域引擎（零 Android 依赖）
│   ├── Time.kt            #   currentPageDate / isFrozen（1 点边界）
│   └── Chain.kt           #   Chain 链状态机（streak / 断链 / 完成 / 重置）
├── app/                   # Android 应用
│   ├── data/              #   Room 实体/DAO + RecordRepository（用例层）+ BackupService
│   ├── MainActivity.kt    #   导航 + 今天页
│   ├── HistoryScreen.kt   #   历史日历 + 只读详情
│   ├── SettingsScreen.kt  #   提醒时间 + 导出/导入
│   └── ReminderWorker.kt  #   WorkManager 提醒
├── CONTEXT.md             # 领域词汇表
├── docs/adr/              # 架构决策记录
└── .scratch/daily-record/ # PRD + 15 个 issue（已全部 done）
```

## 构建与测试

```bash
# 跑全部测试（domain + app，共 50 个）
./gradlew test

# 出 debug APK（app/build/outputs/apk/debug/app-debug.apk）
./gradlew :app:assembleDebug

# 一键测试（自动使用 .toolchain 里的 JDK）
pwsh ./run-tests.ps1
```

要求 JDK 17+。Gradle 由 wrapper 自动下载（已指向国内镜像）。

## 运行

用 Android Studio 打开项目，或直接安装 debug APK 到设备。首次启动会请求**通知权限**（Android 13+），请允许，否则提醒会静默失败。

## 文档

- [CONTEXT.md](CONTEXT.md) —— 领域词汇表（条目、完成、连续天数、记录页、冻结、断链、重置）
- [docs/adr/0001-strict-daily-lock.md](docs/adr/0001-strict-daily-lock.md) —— 严格锁架构决策
- [.scratch/daily-record/PRD.md](.scratch/daily-record/PRD.md) —— 产品需求文档
- [.scratch/daily-record/issues/](.scratch/daily-record/issues/) —— 15 个实现 issue（验收标准）
- [domain/README.md](domain/README.md) —— 领域引擎模块接口

## 国内网络说明

- **Maven 依赖**：`maven.google.com` 被墙，AGP/AndroidX/Compose 走阿里云镜像（已在 `settings.gradle.kts` 配好）。
- **GitHub 推送**：本机直连 `github.com` HTTPS 被墙，改用 **SSH-over-443**（`ssh.github.com:443`），配置在 `~/.ssh/config`。
- **临时工具链**：`.toolchain/`（JDK + Gradle + Android SDK）仅供本机开发，已 gitignore，项目落地后统一删除；届时改用 Android Studio 自带的 JDK 和 SDK（改 `local.properties` 的 `sdk.dir` 即可）。
