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
