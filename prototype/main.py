"""TUI 壳 —— 一次性，验证完即删。真正的逻辑在 engine.py。"""
import os
from datetime import datetime, timedelta

import engine


def clear():
    os.system("cls" if os.name == "nt" else "clear")


def render(s, msg=""):
    clear()
    B = "\x1b[1m"
    D = "\x1b[2m"
    R = "\x1b[0m"
    t = engine.today(s)
    broken = engine.is_broken(s)
    unlocked = engine.unlocked_today(s)
    st = engine.streak(s)
    c_today = t in s.completed

    print(f"{B}每日记录 · 严格锁状态机{R}  {D}(一次性原型，验证语义用){R}")
    print()
    if msg:
        print(f"  {msg}")
        print()
    print(f"{B}模拟时钟{R}      {s.now:%Y-%m-%d %H:%M}")
    print(f"{B}当前记录页{R}    {t}  {D}(今天；凌晨1点前属于前一天){R}")
    RED = "\x1b[31m"
    if broken:
        streak_line = f"{B}连续天数{R}      {RED}{st}  [已断链]{R}"
    else:
        streak_line = f"{B}连续天数{R}      {st}"
    print(streak_line)
    print(f"{B}链起点{R}        {s.epoch}")
    if c_today:
        today_state = "已完成 ✓"
    elif not unlocked:
        today_state = "锁死 🔒（断链）"
    else:
        today_state = "进行中（未完成）"
    print(f"{B}今天状态{R}      {today_state}")
    y = t - timedelta(days=1)
    print(f"{B}昨天{R}          {y}   " + ("已完成 ✓" if y in s.completed else "未完成 ✗"))
    print()
    print(f"{B}最近 11 天{R}   {D}(✓完成 ·旧历史 ✗链内未完成 ›今天进行中 🔒今天锁死){R}")
    for i in range(10, -1, -1):
        d = t - timedelta(days=i)
        if d == t:
            mark = "✓" if c_today else ("🔒" if not unlocked else "›")
        elif d in s.completed:
            mark = "✓"
        elif s.epoch is not None and d < s.epoch:
            mark = "·"
        else:
            mark = "✗"
        epoch_mark = "  ◄起点" if d == s.epoch else ""
        print(f"    {d}  {mark}{epoch_mark}")
    print()
    print(f"{B}命令{R}")
    print(f"  {B}c{R} 完成今天      {B}c 2024-01-02{R} 尝试补/提前完成某天")
    print(f"  {B}u{R} 撤销完成      {B}r{R} 重置(清天数)")
    print(f"  {B}+1h{R} 前进1小时   {B}+1d{R} 前进1天")
    print(f"  {B}set 2024-01-01 00:30{R} 设时钟     {B}q{R} 退出")


def main():
    s = engine.init()
    msg = "试试：set 到某天 00:30，再 +1h 跨过 1 点边界。"
    while True:
        render(s, msg)
        cmd = input("> ").strip()
        if not cmd:
            msg = ""
            continue
        parts = cmd.split()
        head = parts[0].lower()
        if head in ("q", "quit", "exit"):
            break
        elif head in ("c", "complete"):
            if len(parts) >= 2:
                try:
                    d = datetime.strptime(parts[1], "%Y-%m-%d").date()
                    msg = engine.complete(s, d)
                except ValueError:
                    msg = "✗ 日期格式：YYYY-MM-DD"
            else:
                msg = engine.complete(s)
        elif head in ("u", "undo"):
            msg = engine.undo_today(s)
        elif head in ("r", "reset"):
            msg = engine.reset(s)
        elif head in ("+1h", "1h"):
            msg = engine.advance(s, hours=1)
        elif head in ("+1d", "1d"):
            msg = engine.advance(s, days=1)
        elif head == "set":
            try:
                dt = datetime.strptime(" ".join(parts[1:]), "%Y-%m-%d %H:%M")
                msg = engine.set_clock(s, dt)
            except ValueError:
                msg = "✗ 用法：set 2024-01-01 00:30"
        elif head in ("h", "help"):
            msg = "命令：c / u / r / +1h / +1d / set <时间> / q"
        else:
            msg = f"✗ 未知命令：{cmd}"
    print("再见。")


if __name__ == "__main__":
    main()
