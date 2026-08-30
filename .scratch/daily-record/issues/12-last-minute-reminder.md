Status: done

# 12 凌晨前最后提醒通知

## What to build

用 WorkManager 在凌晨 1 点边界前（默认 0:30）推送「最后提醒」系统通知。

## Acceptance criteria

- [ ] 在默认 0:30 收到提醒通知
- [ ] 只在当天未完成时提醒（已完成的当天不提醒）
- [ ] 测试覆盖调度与「是否提醒」判定

## Blocked by

- #05 完成今天 + 连续天数计算
