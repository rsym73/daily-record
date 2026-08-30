Status: done

# 16 自定义应用图标

## What to build

用用户提供的 1024×1024 方形 PNG 替换默认应用图标。生成自适应图标（整图铺满背景层 + 透明前景）与 legacy PNG（Android 7.x 兜底），并在 manifest 写入 `android:icon` / `android:roundIcon`，替换系统默认绿机器人图标。图标内容居中留白，接受圆形桌面对四角的裁剪。

## Acceptance criteria

- [ ] 桌面显示自定义图标而非系统默认图标
- [ ] 自适应图标在圆形 / 圆角 / 方形桌面形状下正常显示
- [ ] Android 7.x（minSdk 24–25）回退使用 legacy PNG

## Blocked by

None - can start immediately
