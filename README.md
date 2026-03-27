# Dep Janitor

Dep Janitor 是一个面向 **macOS / Windows** 的桌面客户端，用于**可视化扫描并安全清理 Maven 与 Gradle 本地依赖缓存**。

产品目标不是“暴力删缓存”，而是提供一套偏开发者工具风格的治理流程：

- 先发现路径
- 再分析结构
- 再识别候选项
- 先预演再执行
- 用规则降低误删风险

---

## 当前状态

当前仓库已从文档阶段进入**可运行开发阶段**，已经完成从工程骨架到真实扫描、候选识别与建议预演的首轮闭环。

已落地能力：

- Kotlin + Compose Desktop 多模块工程
- `Obsidian` / `Ivory` 双主题桌面 UI
- 默认 Maven / Gradle / Wrapper 路径发现
- 自定义扫描路径与本地配置持久化
- 基础目录大小扫描与热点仓统计
- Maven `group/artifact/version` 聚合解析
- Gradle caches / wrapper 基础解析
- Artifact Atlas 搜索、筛选、排序、版本详情
- 候选项首轮识别：
  - 旧版本
  - 过期 SNAPSHOT
  - `.lastUpdated`
  - 下载失败残留
  - 长期未使用 Gradle cache
  - 旧版 wrapper
- Cleanup Recipes 与 Simulation 预演数据联动

---

## 技术栈

- **语言**：Kotlin
- **桌面 UI**：Compose Multiplatform Desktop
- **构建工具**：Gradle Wrapper
- **运行时**：JDK 21
- **架构形态**：单仓多模块桌面应用

---

## 工程结构

```text
.
├─ app-desktop/      # Compose Desktop 应用壳、主题、页面与桌面端交互
├─ core-model/       # 核心领域模型
├─ core-engine/      # 扫描、候选识别、建议生成、预演等业务引擎
├─ core-platform/    # 平台能力：路径发现、配置存储，后续接回收站等
├─ docs/
│  ├─ 需求分析/
│  ├─ 设计/
│  └─ 开发计划/
├─ gradle/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ gradlew
└─ gradlew.bat
```

### 模块职责

#### `app-desktop`
- 应用入口
- 页面路由
- 双主题系统
- UI 组件
- 扫描结果展示

#### `core-model`
- 路径、风险、artifact、候选项、规则、预演等核心模型

#### `core-engine`
- 目录扫描
- Maven / Gradle 结构分析
- 候选项识别
- 清理建议生成
- 预演数据构建

#### `core-platform`
- 默认路径发现
- 配置持久化
- 后续回收站 / 平台集成能力

---

## 已实现页面

- **Observatory**：总览页，展示扫描状态、容量、热点仓、依赖地层
- **Artifact Atlas**：依赖图谱页，支持搜索、来源过滤、风险过滤、排序、版本详情
- **Cleanup Recipes**：规则化建议卡与候选清单
- **Simulation Chamber**：默认方案预演、待复核项与保护项展示
- **Rule Forge**：主题切换、路径治理、规则快照

---

## 开发进度

### 已完成
- Sprint 0 / 工程骨架、双主题、桌面壳
- Sprint 1 / 默认路径发现、路径覆盖、目录扫描、首页真实数据接入
- Sprint 2 / Maven / Gradle 结构解析、Artifact Atlas 真实数据接入
- Sprint 3 / 候选识别首轮、清理建议首轮、预演页数据联动

### 下一步
- 持久化规则配置（保留版本数、未使用阈值、回收站策略）
- 提升 Maven / Gradle 深度解析与候选判定精度
- 接入真实删除执行链路
- 接入回收站 / 废纸篓能力
- 增加安装包图标、签名与 CI 打包流程

---

## 环境要求

- JDK **21**
- macOS 或 Windows
- 不需要全局安装 Gradle，项目自带 **Gradle Wrapper**

检查 Java：

```bash
java -version
```

---

## 本地开发

### 运行桌面应用

```bash
./gradlew :app-desktop:run
```

### 执行测试

```bash
./gradlew test
```

### 查看桌面模块任务

```bash
./gradlew :app-desktop:tasks --all
```

---

## 安装包打包

当前项目已经配置 Compose Desktop 原生安装包输出。

### macOS

```bash
./gradlew :app-desktop:packageDmg
./gradlew :app-desktop:packagePkg
```

### Windows

```bat
gradlew.bat :app-desktop:packageExe
gradlew.bat :app-desktop:packageMsi
```

### 当前系统直接打包

```bash
./gradlew :app-desktop:packageDistributionForCurrentOS
```

### 产物目录

通常位于：

```text
app-desktop/build/compose/binaries/main/
```

> 建议在 macOS 上打 mac 安装包，在 Windows 上打 Windows 安装包。

---

## 配置说明

应用当前会在用户目录保存本地配置，用于持久化：

- 主题模式
- 自定义扫描路径

默认配置文件位置：

```text
~/.dep-janitor/config.properties
```

---

## 文档索引

- PRD：`docs/需求分析/Maven与Gradle本地依赖缓存清理工具PRD.md`
- UI 设计方案：`docs/设计/客户端UI设计方案.md`
- 原型图：`docs/设计/客户端原型图.html`
- 开发计划：`docs/开发计划/dep-janitor-v1-development-plan.md`

---

## 项目定位

Dep Janitor 更像一个**安全优先的本地依赖缓存治理工具**，而不是一个简单的“清理缓存按钮”。

核心原则：

- **Safety First**：先分析、先预演、后执行
- **Rule Driven**：规则先行，而不是一次性操作
- **Developer Native**：贴近 Maven / Gradle / Java 开发者习惯
- **Local First**：以本地文件系统能力为核心

---

## 当前注意事项

- 当前删除执行链路还未接入，现阶段重点在**扫描、识别、建议、预演**
- Windows / macOS 安装包虽然已具备基础打包能力，但图标、签名、发布级配置还需继续补齐
- 候选识别目前是 V1 首轮实现，后续还会继续增强准确度
