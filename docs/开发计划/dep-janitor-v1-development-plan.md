# Plan: Dep Janitor V1 桌面客户端开发计划

**Generated**: 2026-03-27  
**Estimated Complexity**: High

## Overview

当前仓库仍处于**文档与原型阶段**，尚无实际应用代码。基于 PRD、现有双主题 UI 方案以及桌面端发布目标，建议 V1 采用：

> **Kotlin + Compose Multiplatform Desktop + Gradle 多模块工程**

推荐原因：

1. **更贴近 Java 生态**：产品面向 Maven / Gradle / Java 开发者，扫描与规则逻辑使用 Kotlin/JVM 更自然。
2. **发布形态匹配 PRD**：Compose Desktop 官方支持 macOS `.dmg/.pkg`、Windows `.exe/.msi` 打包。
3. **更适合本地文件系统与桌面能力**：路径发现、符号链接处理、删除/回收站、长路径兼容等本地能力更直接。
4. **更利于扫描引擎与 UI 同仓维护**：V1 可先做单进程桌面应用，降低集成复杂度。
5. **当前仓库尚无前端工程约束**：现有 HTML 原型可作为视觉参考，不构成必须采用 WebView/Tauri 的约束。

### 推荐工程结构

```text
.
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ app-desktop/
│  └─ src/jvmMain/kotlin/com/depjanitor/app/...
├─ core-model/
│  └─ src/main/kotlin/com/depjanitor/core/model/...
├─ core-engine/
│  └─ src/main/kotlin/com/depjanitor/core/engine/...
├─ core-platform/
│  └─ src/main/kotlin/com/depjanitor/core/platform/...
├─ core-testing/
│  └─ src/testFixtures/kotlin/...
└─ docs/
```

### V1 范围边界

本计划仅覆盖 PRD 的 V1：
- 路径发现
- Maven / Gradle 扫描
- 可清理项识别
- 清理建议
- 删除前预演
- 安全删除
- 基础设置
- 双主题桌面 UI
- macOS / Windows 打包

不纳入 V1：
- 云端同步
- 团队协作
- 多语言生态缓存治理
- CLI 独立产品化
- 高级项目引用保护（仅做基础版）

## Prerequisites

- JDK 17+
- Kotlin / Gradle 开发环境
- IntelliJ IDEA
- macOS 与 Windows 双平台验证环境
- 至少 2 组本地样本目录：
  - Maven 仓库样本
  - Gradle caches / wrapper 样本
- 设计稿与原型作为 UI 参考：
  - `docs/设计/客户端UI设计方案.md`
  - `docs/设计/客户端原型图.html`

---

## Sprint 0: 技术定版与工程骨架

**Goal**: 建立可运行、可预览、可扩展的桌面工程骨架。  
**Demo/Validation**:
- 能本地启动桌面窗口
- 能显示占位版首页与双主题切换
- 能执行基础测试与打包命令

### Task 0.1: 锁定技术方案与目录结构
- **Location**: `docs/开发计划/dep-janitor-v1-development-plan.md`, `README.md`
- **Description**: 确认 V1 采用 Kotlin + Compose Desktop，多模块目录结构按本计划建立。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 工程结构说明写入 README
  - 模块边界明确：UI / 引擎 / 平台能力 / 模型
- **Validation**:
  - 评审目录结构与模块说明

### Task 0.2: 初始化 Gradle 多模块工程
- **Location**: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app-desktop/`, `core-model/`, `core-engine/`, `core-platform/`
- **Description**: 创建桌面应用主模块和核心模块，配置 Kotlin、Compose Desktop、测试框架。
- **Dependencies**: Task 0.1
- **Acceptance Criteria**:
  - 项目可导入 IntelliJ
  - 可执行 `run` 启动桌面壳
  - 测试任务可跑通
- **Validation**:
  - 执行 Gradle run/test 成功

### Task 0.3: 建立设计 Token 与主题系统
- **Location**: `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/theme/`
- **Description**: 把双主题设计稿落为代码，包括颜色、排版、间距、圆角、来源语义、风险语义。
- **Dependencies**: Task 0.2
- **Acceptance Criteria**:
  - 支持 `Obsidian` 与 `Ivory` 主题切换
  - Maven / Gradle / 风险标签颜色统一
- **Validation**:
  - 手动切换主题验证视觉一致性

### Task 0.4: 搭建应用壳层 App Shell
- **Location**: `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/shell/`
- **Description**: 实现侧边导航、顶部状态栏、内容区容器、全局 Snackbar/Dialog 框架。
- **Dependencies**: Task 0.3
- **Acceptance Criteria**:
  - 5 个一级页面路由占位可切换
  - 应用壳层符合原型布局
- **Validation**:
  - 本地运行检查页面切换

---

## Sprint 1: 路径发现与扫描基础能力

**Goal**: 做出第一个可用的“发现路径 + 扫描基础信息”的增量。  
**Demo/Validation**:
- 自动识别默认 Maven / Gradle 路径
- 扫描后展示总容量、来源容量、基础热点目录

### Task 1.1: 定义核心领域模型
- **Location**: `core-model/src/main/kotlin/com/depjanitor/core/model/`
- **Description**: 定义 PathConfig、ScanTarget、ArtifactRef、VersionEntry、ScanSummary、CleanupCandidate、RiskLevel、CleanupPlan 等模型。
- **Dependencies**: Sprint 0 完成
- **Acceptance Criteria**:
  - 模型覆盖 PRD V1 核心流程
  - 风险与来源语义可被 UI 直接消费
- **Validation**:
  - 单元测试验证序列化/映射

### Task 1.2: 实现跨平台路径发现
- **Location**: `core-platform/src/main/kotlin/com/depjanitor/core/platform/path/`
- **Description**: 自动识别 macOS / Windows 的 Maven、Gradle caches、Gradle wrapper 默认路径，并支持用户覆写。
- **Dependencies**: Task 1.1
- **Acceptance Criteria**:
  - 能识别默认路径
  - 路径不存在时给出状态说明
  - 支持符号链接与手动录入
- **Validation**:
  - 针对不同 OS 环境变量的单元测试

### Task 1.3: 实现目录体积扫描器
- **Location**: `core-engine/src/main/kotlin/com/depjanitor/core/engine/scan/`
- **Description**: 构建基础扫描器，统计总大小、来源大小、目录大小排行，支持扫描进度事件。
- **Dependencies**: Task 1.2
- **Acceptance Criteria**:
  - 可统计总缓存大小
  - 可按 Maven / Gradle 分类汇总
  - 可输出前 N 大目录
- **Validation**:
  - 使用本地样本目录进行集成测试

### Task 1.4: 实现首页 Observatory 页面
- **Location**: `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/observatory/`
- **Description**: 实现总览页，接入路径发现与基础扫描结果，展示核心容量读数、来源结构、热点仓。
- **Dependencies**: Task 1.3
- **Acceptance Criteria**:
  - 首页可触发扫描
  - 能展示最近扫描时间与扫描状态
  - 双主题下展示正常
- **Validation**:
  - 手动运行验证 UI 与数据联动

---

## Sprint 2: Maven / Gradle 深度扫描与结果页

**Goal**: 输出真正可分析的扫描结果页。  
**Demo/Validation**:
- 能按 artifact 聚合
- 能看到版本层、大小、最近使用时间、来源和风险初判

### Task 2.1: Maven 仓库结构解析
- **Location**: `core-engine/src/main/kotlin/com/depjanitor/core/engine/maven/`
- **Description**: 解析 `.m2/repository` 的 group/artifact/version 目录结构，抽取 artifact 聚合信息。
- **Dependencies**: Sprint 1
- **Acceptance Criteria**:
  - 支持聚合到 artifact 级别
  - 能识别版本目录和文件体积
- **Validation**:
  - 使用样本仓库做解析测试

### Task 2.2: Gradle caches 与 wrapper 结构解析
- **Location**: `core-engine/src/main/kotlin/com/depjanitor/core/engine/gradle/`
- **Description**: 解析 caches、transforms、modules、wrapper distributions，输出统一结果模型。
- **Dependencies**: Sprint 1
- **Acceptance Criteria**:
  - 能区分 caches 与 wrapper
  - 能统计旧 wrapper 及异常缓存项
- **Validation**:
  - Gradle 样本目录集成测试

### Task 2.3: 最后使用时间与排序索引
- **Location**: `core-engine/src/main/kotlin/com/depjanitor/core/engine/index/`
- **Description**: 为 artifact 与 version 建立排序字段：大小、版本数、最后修改/访问时间、来源、优先级。
- **Dependencies**: Task 2.1, Task 2.2
- **Acceptance Criteria**:
  - 支持 UI 多维排序
  - 字段为空时有兜底策略
- **Validation**:
  - 单元测试覆盖排序规则

### Task 2.4: 实现 Artifact Atlas 页面
- **Location**: `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/artifactatlas/`
- **Description**: 实现来源树、结果列表、版本地层、搜索筛选、右侧详情面板。
- **Dependencies**: Task 2.3
- **Acceptance Criteria**:
  - 支持搜索、来源过滤、风险过滤、排序
  - 支持选中某 artifact 查看版本地层
  - 支持“查看路径 / 加入白名单 / 加入方案”入口
- **Validation**:
  - 手动交互验证
  - 关键 UI 状态截图沉淀到 docs

---

## Sprint 3: 规则引擎、建议生成与预演

**Goal**: 让系统从“扫描工具”升级为“治理工具”。  
**Demo/Validation**:
- 能基于规则生成建议包
- 能在执行前预演释放空间和风险

### Task 3.1: 定义规则配置模型
- **Location**: `core-model/src/main/kotlin/com/depjanitor/core/model/rule/`
- **Description**: 建立保留最近 N 个版本、未使用阈值、低风险优先、白名单等规则配置。
- **Dependencies**: Sprint 2
- **Acceptance Criteria**:
  - 配置可持久化
  - 默认规则符合 PRD
- **Validation**:
  - 单元测试验证默认配置与边界值

### Task 3.2: 实现候选项识别器
- **Location**: `core-engine/src/main/kotlin/com/depjanitor/core/engine/candidate/`
- **Description**: 识别旧版本、180 天未使用、`.lastUpdated`、失败下载残留、过期 SNAPSHOT、旧 wrapper 等候选项。
- **Dependencies**: Task 3.1
- **Acceptance Criteria**:
  - 候选类型覆盖 PRD V1
  - 每条候选项有来源、大小、理由、风险级别
- **Validation**:
  - 样本测试覆盖各类候选

### Task 3.3: 实现风险评估与建议打包
- **Location**: `core-engine/src/main/kotlin/com/depjanitor/core/engine/risk/`, `core-engine/src/main/kotlin/com/depjanitor/core/engine/plan/`
- **Description**: 基于规则和来源生成 Cleanup Recipes：低风险、中风险、高风险策略包。
- **Dependencies**: Task 3.2
- **Acceptance Criteria**:
  - 高风险项默认不自动启用
  - 输出预计释放空间与命中数量
- **Validation**:
  - 单元测试验证风险分级

### Task 3.4: 实现 Cleanup Recipes 页面
- **Location**: `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/cleanuprecipes/`
- **Description**: 实现策略卡视图、策略启停、收益统计、加入本次方案。
- **Dependencies**: Task 3.3
- **Acceptance Criteria**:
  - 可查看每个策略包的命中与风险
  - 可调整策略后重新生成
- **Validation**:
  - 手动验证策略变化对统计的影响

### Task 3.5: 实现 Simulation Chamber 页面
- **Location**: `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/simulation/`
- **Description**: 展示清理前后对照、预计释放空间、保留项、白名单、高风险提示。
- **Dependencies**: Task 3.4
- **Acceptance Criteria**:
  - 预演数据准确映射到待执行项
  - 高风险项有额外确认提示
- **Validation**:
  - 样本数据回放验证 before/after 差异

---

## Sprint 4: 安全删除、回收站与设置

**Goal**: 完成真正可执行的清理闭环。  
**Demo/Validation**:
- 用户可执行清理
- 可优先移入回收站
- 删除失败能解释原因

### Task 4.1: 实现白名单与保护规则持久化
- **Location**: `core-platform/src/main/kotlin/com/depjanitor/core/platform/config/`
- **Description**: 保存路径设置、规则配置、白名单、主题偏好到本地配置文件。
- **Dependencies**: Sprint 3
- **Acceptance Criteria**:
  - 重启应用后配置可恢复
  - 白名单能参与候选过滤
- **Validation**:
  - 配置读写测试

### Task 4.2: 实现回收站/废纸篓删除适配
- **Location**: `core-platform/src/main/kotlin/com/depjanitor/core/platform/delete/`
- **Description**: 适配 macOS 废纸篓与 Windows 回收站优先删除；无法回收时可提示回退策略。
- **Dependencies**: Task 4.1
- **Acceptance Criteria**:
  - 优先移入回收站
  - 删除失败给出文件占用/权限等原因
- **Validation**:
  - 在真实临时目录验证删除结果

### Task 4.3: 实现执行器与结果回显
- **Location**: `core-engine/src/main/kotlin/com/depjanitor/core/engine/execute/`
- **Description**: 根据预演方案执行删除，汇总成功项、失败项、释放空间、失败原因。
- **Dependencies**: Task 4.2
- **Acceptance Criteria**:
  - 仅执行已确认项
  - 结果页可展示成功/失败明细
- **Validation**:
  - 集成测试 + 手动样本验证

### Task 4.4: 实现 Rule Forge 设置页
- **Location**: `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/ruleforge/`
- **Description**: 实现路径管理、保留策略、白名单管理、删除方式、主题偏好设置。
- **Dependencies**: Task 4.1
- **Acceptance Criteria**:
  - 配置项与引擎真实联动
  - 支持手动新增/删除白名单规则
- **Validation**:
  - 手动改配置后重新扫描验证

### Task 4.5: 实现执行确认与结果反馈
- **Location**: `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/common/dialog/`, `app-desktop/src/jvmMain/kotlin/com/depjanitor/app/ui/result/`
- **Description**: 实现执行确认弹窗、全局提示、结果页与失败明细。
- **Dependencies**: Task 4.3, Task 4.4
- **Acceptance Criteria**:
  - 删除前必须明确展示数量、空间、风险、回收站策略
  - 删除后展示结果摘要与失败项列表
- **Validation**:
  - 手动流程走通一次完整清理

---

## Sprint 5: 打包、质量与开源交付

**Goal**: 产出可分发的 MVP。  
**Demo/Validation**:
- macOS / Windows 安装包产出
- README、截图、FAQ、Roadmap 基础完成

### Task 5.1: 构建安装包配置
- **Location**: `app-desktop/build.gradle.kts`, `app-desktop/src/jvmMain/resources/`
- **Description**: 配置应用图标、版本、安装包元数据，生成 `.dmg/.pkg/.exe/.msi`。
- **Dependencies**: Sprint 4
- **Acceptance Criteria**:
  - 本地可产出至少一个 macOS 包和一个 Windows 包
  - 图标与元数据完整
- **Validation**:
  - 打包命令执行成功

### Task 5.2: 样本仓与回归测试集
- **Location**: `core-testing/`, `testdata/`
- **Description**: 构造 Maven / Gradle 样本目录，覆盖旧版本、失败下载、过期 SNAPSHOT、wrapper、符号链接等场景。
- **Dependencies**: Sprint 4
- **Acceptance Criteria**:
  - 至少覆盖 10 类关键场景
  - 扫描/建议/执行链路可回归
- **Validation**:
  - 集成测试通过

### Task 5.3: README 与开源资料完善
- **Location**: `README.md`, `docs/FAQ.md`, `docs/ROADMAP.md`, `docs/风险说明.md`, `docs/贡献指南.md`
- **Description**: 完善安装说明、截图、风险说明、路线图、贡献文档。
- **Dependencies**: Sprint 5.1
- **Acceptance Criteria**:
  - 仓库可被外部用户理解和运行
  - 文档与产品行为一致
- **Validation**:
  - README 自检

### Task 5.4: MVP 验收清单
- **Location**: `docs/开发计划/mvp-acceptance-checklist.md`
- **Description**: 输出 V1 验收清单，覆盖功能、UI、性能、安全、打包与文档。
- **Dependencies**: Sprint 5.2, Task 5.3
- **Acceptance Criteria**:
  - 验收项与 PRD V1 范围一致
- **Validation**:
  - 按清单逐项验收

---

## Testing Strategy

### 单元测试
- 领域模型与规则配置
- 路径发现
- Maven / Gradle 目录解析
- 候选项识别
- 风险分级
- 配置持久化

### 集成测试
- 使用样本目录跑完整扫描
- 策略生成与预演
- 删除执行与失败处理

### UI 验证
- 双主题一致性
- 导航与页面状态
- 空状态 / 加载 / 错误态
- 大数据量列表与筛选

### 手工验证重点
- macOS 废纸篓
- Windows 回收站
- 长路径与中文路径
- 符号链接
- 文件占用导致删除失败

---

## Potential Risks & Gotchas

1. **Gradle 缓存结构复杂**  
   - 先做 V1 白名单内的已知结构：modules、transforms、wrapper、常见残留。
2. **最后访问时间不可靠**  
   - V1 使用最后修改时间/元数据近似，UI 明示“估算依据”。
3. **回收站跨平台差异大**  
   - 抽象平台删除适配层，优先回收站，失败时回退并明确提示。
4. **误删仍被项目依赖的版本**  
   - 高风险默认不启用；白名单和预演作为强约束。
5. **仓库当前没有代码**  
   - Sprint 0 必须先做工程基建，不能直接跳进功能开发。
6. **HTML 原型与 Compose 实现存在表现差异**  
   - 保留“语义一致”优先，不强求像素级复刻。

---

## Rollback Plan

- 配置与白名单改动均保存在本地配置文件，可回退到默认。
- 删除默认优先走回收站，降低不可逆风险。
- 每次清理保留执行日志，便于追查与恢复。
- 每个 Sprint 完成后打 Git tag，若后续实现偏离可快速回滚。

---

## Immediate Next Step

建议按以下顺序立刻开始：

1. 建立 Gradle 多模块工程骨架
2. 落地双主题设计 token 与 App Shell
3. 完成路径发现与基础扫描
4. 再进入 Maven / Gradle 深度解析

