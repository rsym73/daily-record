# 领域引擎（domain）

每日记录应用的**纯 Kotlin 领域引擎**——承载「严格锁」的全部规则。无 Android 依赖、不可变数据、纯函数，可独立测试，可被未来 Android 应用作为 `:domain` 模块直接引入。

## 定位

- 对应 issue #02、#05–#09 的核心逻辑。
- 只含纯函数与不可变数据，不依赖 Room / Compose / 任何 Android 框架。
- 规则语义先由 Python 原型 `prototype/engine.py` 验证，再逐条移植，用 13 个测试锁死。

## 公开接口

### 时间 / 边界（`Time.kt`）

| 函数 | 说明 |
|------|------|
| `currentPageDate(now, zone)` | now 时刻对应的「记录页日期」；凌晨 1 点前属于前一天 |
| `isFrozen(day, now, zone)` | day 的记录页是否已跨过其凌晨 1 点边界（变为只读） |

### 链状态（`Chain.kt`）

```kotlin
data class Chain(
    val completed: Set<LocalDate>,  // 已完成过的所有记录页（含重置前的只读历史）
    val epoch: LocalDate,           // 当前链起点
)
```

- `isBroken(today)` / `isUnlocked(today)`
- `streak(today)` —— 连续天数；断链时返回断链前达到的值
- `complete(target, today)` / `undo(target, today)` → `CompleteResult.Ok` | `Rejected`
- `reset(today)` —— 归零天数、保留只读历史

`RejectReason`：`FROZEN`（补写过去）、`FUTURE`（提前写未来）、`LOCKED`（断链锁死）、`NOT_COMPLETED`（撤销未完成）。

## 核心规则（与 ADR-0001 一致）

1. 一天边界 = 凌晨 1 点（设备本地时区）。
2. 严格锁：不能补写过去、不能提前写未来、漏一天断链、断链只能重置。
3. 完成当天内可撤销；跨天冻结后不可撤销。
4. 断链时连续天数保留断链前值（界面标红显示）。

## 运行测试

```bash
./gradlew test          # 或：pwsh ./run-tests.ps1
```

- 需要 JDK 17+（`JAVA_HOME`）。本仓库 `.toolchain/jdk` 有现成 JDK，`run-tests.ps1` 会自动使用。
- Gradle 由 wrapper 自动下载（已指向腾讯云镜像）。

## 引入到 Android 应用

本模块是纯 Kotlin/JVM，未来在 Android 工程根 `settings.gradle.kts` 里 `include(":domain")` 即可；`Time.kt` 的 `ZoneId` 参数对应 app 的设备本地时区。`build.gradle.kts` 里 `repositories` 的阿里云镜像仅为国内网络便利，可移除。
