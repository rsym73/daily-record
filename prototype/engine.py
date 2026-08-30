"""每日记录 · 严格锁状态机 —— 纯领域引擎（一次性原型，验证语义用）。

验证的问题：
  给定「模拟时钟 + 已完成天数 + 当前链起点(epoch)」，
  系统如何判定：当前记录页日期、冻结、解锁、断链、连续天数、重置。

一天边界 = 凌晨 1 点（设备本地时间）。
日期 D 的记录页窗口 = [D 01:00, D+1 01:00)，跨过即冻结。

本模块无 I/O、无终端代码，可被 TUI 调用，也可日后译回 Kotlin。
"""
from __future__ import annotations
from dataclasses import dataclass, field
from datetime import datetime, date, time, timedelta

BOUNDARY_HOUR = 1


# ---------- 纯函数：时间 / 边界 ----------

def current_page_date(now: datetime) -> date:
    """now 时刻对应的「记录页日期」。凌晨 1 点之前算前一天。"""
    return (now - timedelta(hours=BOUNDARY_HOUR)).date()


def freeze_time(day: date) -> datetime:
    """day 的记录页在 day+1 的凌晨 1 点冻结。"""
    return datetime.combine(day + timedelta(days=1), time(BOUNDARY_HOUR, 0))


def is_frozen(day: date, now: datetime) -> bool:
    """day 是否已跨过它自己的 1 点边界（已冻结）。"""
    return now >= freeze_time(day)


# ---------- 状态 ----------

@dataclass
class State:
    now: datetime = datetime(2024, 1, 1, 10, 0)
    completed: set = field(default_factory=set)   # 已完成的天（含历史，只读保留）
    epoch: date | None = None                      # 当前链起点；None = 尚未开始


def init() -> State:
    s = State()
    s.epoch = current_page_date(s.now)
    return s


# ---------- 派生状态 ----------

def today(s: State) -> date:
    return current_page_date(s.now)


def is_broken(s: State) -> bool:
    """当前链是否已断：从 epoch 到昨天之间是否存在未完成的天。"""
    if s.epoch is None:
        return False
    t = today(s)
    d = s.epoch
    while d < t:
        if d not in s.completed:
            return True
        d += timedelta(days=1)
    return False


def unlocked_today(s: State) -> bool:
    """今天能否写入：链未断即解锁。"""
    if s.epoch is None:
        return True
    return not is_broken(s)


def streak(s: State) -> int:
    """连续天数。链未断时是实时 streak；断链时返回断链前达到的值（配合 is_broken 标红显示）。"""
    if s.epoch is None:
        return 0
    t = today(s)
    n = 0
    d = s.epoch
    while d < t:
        if d not in s.completed:
            break
        n += 1
        d += timedelta(days=1)
    if not is_broken(s) and t in s.completed:
        n += 1
    return n


# ---------- 动作 ----------

def complete(s: State, day: date | None = None) -> str:
    """完成某天（默认今天）。可显式传一个日期来验证「不能补写/不能提前写」。"""
    t = today(s)
    target = day if day is not None else t
    if target < t:
        return f"✗ {target} 已冻结，不能补写/补完成"
    if target > t:
        return f"✗ {target} 是未来天，不能提前写"
    # target == 今天
    if not unlocked_today(s):
        return "✗ 断链中，今天未解锁，不能完成"
    s.completed.add(target)
    return f"✓ 已完成 {target}"


def undo_today(s: State) -> str:
    t = today(s)
    if t not in s.completed:
        return "✗ 今天未完成，无需撤销"
    if is_frozen(t, s.now):
        return "✗ 今天已冻结，不能撤销"
    s.completed.discard(t)
    return f"✓ 已撤销 {t} 的完成"


def reset(s: State) -> str:
    t = today(s)
    s.epoch = t
    return f"✓ 已重置：连续天数归零，从 {t} 重新起链（历史保留只读）"


def advance(s: State, *, hours: int = 0, days: int = 0) -> str:
    s.now += timedelta(hours=hours, days=days)
    return f"→ 时钟前进到 {s.now:%Y-%m-%d %H:%M}"


def set_clock(s: State, dt: datetime) -> str:
    s.now = dt
    s.epoch = current_page_date(dt)   # 跳转即从这一刻重新锚定链起点，避免「今天早于起点」的怪状态
    return f"→ 时钟设为 {s.now:%Y-%m-%d %H:%M}（链起点重新锚定为 {s.epoch}）"
