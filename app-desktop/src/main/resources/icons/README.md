# Dep Janitor 图标资源目录

请将产品图标母版文件放到：

- `app-desktop/src/main/resources/icons/source/dep-janitor-1024.png`

要求：
- 正方形 PNG
- 推荐 1024 × 1024
- 建议保留安全边距
- 建议使用最终版主图标，不要带背景展示板或 mockup 外框

---

## 自动生成命令

在项目根目录执行：

```bash
python3 scripts/generate_app_icons.py
```

该命令会自动生成：

### 1. 运行时 PNG 图标
输出目录：`app-desktop/src/main/resources/icons/runtime/`

- `dep-janitor-16.png`
- `dep-janitor-24.png`
- `dep-janitor-32.png`
- `dep-janitor-48.png`
- `dep-janitor-64.png`
- `dep-janitor-128.png`
- `dep-janitor-256.png`
- `dep-janitor-512.png`
- `dep-janitor-1024.png`

### 2. Windows 安装包图标
- `app-desktop/src/main/resources/icons/windows/dep-janitor.ico`

### 3. macOS 安装包图标
- `app-desktop/src/main/resources/icons/macos/dep-janitor.icns`

---

## 打包配置

项目已预留安装包图标配置：

- macOS：读取 `icons/macos/dep-janitor.icns`
- Windows：读取 `icons/windows/dep-janitor.ico`

只要生成完图标，再执行打包即可。

---

## 后续建议

如果你还保留原始母版，也建议一起放到：

- `app-desktop/src/main/resources/icons/source/dep-janitor.svg`
- 或其他高分辨率导出源文件

便于后续重新导出不同尺寸。

---

## 如果原始出图带有外层黑底，先做裁剪与透明化

有些图像模型导出的图标会带一层纯黑背景画布。对于这类图片，建议先执行：

```bash
swift scripts/prepare_icon_master.swift <输入PNG> app-desktop/src/main/resources/icons/source/dep-janitor-1024.png
```

示例：

```bash
swift scripts/prepare_icon_master.swift tmp_icons/new-icon.png app-desktop/src/main/resources/icons/source/dep-janitor-1024.png
```

该脚本会自动完成：

- 识别并移除外层连通黑底
- 将外部背景转为透明
- 按内容区域自动裁成正方形
- 默认追加桌面应用图标圆角蒙版
- 保留适度安全边距
- 导出为 1024 × 1024 PNG 母版

可选参数：

```bash
swift scripts/prepare_icon_master.swift <输入PNG> <输出PNG> --padding 0.06 --threshold 18 --size 1024
```

- `--padding`：额外安全边距比例，默认 `0.06`
- `--threshold`：背景识别阈值，默认 `18`
- `--size`：输出尺寸，默认 `1024`
- `--content-inset`：输出时再向内缩一点内容，默认 `0.00`
- `--shape`：外轮廓形状，默认 `macos`（更接近 mac app icon 的 squircle）
- `--corner-radius`：圆角比例，默认 `0.22`
- `--squircle-exponent`：macOS squircle 指数，默认 `5.0`
- `--no-rounded-mask`：关闭圆角蒙版

如果黑底去除过多或过少，可适当调节 `--threshold`：

- 背景没去干净：适当提高，如 `--threshold 24`
- 抠掉了图标边缘：适当降低，如 `--threshold 12`

如果 Dock 里仍然显得“方”，建议直接增加一点内缩并保持 `macos` 外轮廓：

```bash
swift scripts/prepare_icon_master.swift tmp_icons/new-icon.png app-desktop/src/main/resources/icons/source/dep-janitor-1024.png --shape macos --content-inset 0.04
```
