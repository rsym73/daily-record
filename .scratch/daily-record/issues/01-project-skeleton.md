Status: done

# 01 项目骨架 + 可启动空应用

## What to build

搭建 Android 项目骨架：Kotlin + Jetpack Compose，引入 Room 与 WorkManager 依赖，建立「纯 Kotlin 领域引擎（无 Android 依赖）→ 数据层 → UI 层」的分层结构，产出一个可安装启动的空应用。完全本地，无网络依赖。

## Acceptance criteria

- [ ] 应用能在 Android 设备/模拟器上安装并启动
- [ ] 使用 Kotlin + Jetpack Compose，依赖含 Room 与 WorkManager
- [ ] 领域逻辑位于不依赖 Android 框架的纯 Kotlin 代码中（可纯单元测试）
- [ ] 无任何云端/网络依赖

## Blocked by

None - can start immediately
