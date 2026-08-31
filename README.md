# 每日记录（Daily Record）

一个记录「每天做了什么」并追踪**连续完成天数**的 Android 应用。核心是一条严格的规则：**必须每天完成记录**，没完成当天的记录就不能进入下一天；一旦漏掉某天，唯一出路是清除连续天数、重新开始。

## 下载（Release）

- **最新版本**：`v1.0.0`
- **下载 APK**：<https://github.com/rsym73/daily-record/releases/download/v1.0.0/app-release.apk>
- 历史版本见 [Releases](https://github.com/rsym73/daily-record/releases)

> 正式包用专用签名密钥（CN=DailyRecord）签名，release 包不可调试。签名密钥与密码仅本地持有，未提交进仓库。

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
