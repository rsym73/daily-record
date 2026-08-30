Status: done

# 17 自定义壁纸

## What to build

在设置页提供「更换壁纸」与「恢复默认」两个入口。更换：通过系统 Photo Picker 从相册选图，复制到应用私有目录（filesDir）并覆盖旧图；今天 / 历史 / 设置三个页面在最底层以 CenterCrop 裁剪铺满渲染该图，并叠一层半透明白色遮罩保证文字清晰。恢复默认：删除私有目录里的壁纸文件，回到默认白底（仅已设置壁纸时显示）。壁纸不参与 JSON 导出 / 导入。

## Acceptance criteria

- [ ] 能从相册选图，并立即在三个页面看到背景变化
- [ ] 壁纸以 CenterCrop 铺满，半透明白遮罩保证文字可读
- [ ] 「恢复默认」后回到默认白底
- [ ] 相册中删除原图后壁纸仍保留（副本在私有目录）
- [ ] 壁纸不参与 JSON 导出 / 导入

## Blocked by

None - can start immediately
