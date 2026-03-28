# Backlog: Dep Janitor PRD 缺口开发待办

**Generated**: 2026-03-27  
**Source**: `docs/需求分析/Maven与Gradle本地依赖缓存清理工具PRD.md` + 当前仓库实现对照  
**Status Baseline**: 当前项目已完成扫描、基础分析、候选识别首轮、建议与预演首轮，但尚未完成安全删除闭环。  
**Recommended Priority Model**: P0（V1 必做） / P1（V1 强化） / P2（后续演进）

---

## 1. 文档目标

本待办文档用于把 **PRD 要求** 与 **当前实现缺口** 整理成可执行开发清单，作为后续 Sprint 排期和提交拆分依据。

重点回答三个问题：

1. 当前项目距离 PRD 的 V1 还缺什么
2. 哪些能力必须优先补齐才能形成完整闭环
3. 每项缺口应拆成哪些可提交、可验证的任务

---

## 2. 当前实现概览

### 已基本完成

- 默认路径发现：Maven / Gradle caches / Gradle wrapper
- 自定义路径覆盖与本地持久化
- 目录大小扫描与热点目录统计
- Maven `group/artifact/version` 聚合解析
- Gradle caches / wrapper 基础解析
- Artifact Atlas 搜索、筛选、排序、版本详情
- 候选项首轮识别：
  - 旧版本
  - 过期 SNAPSHOT
  - `.lastUpdated`
  - 部分失败下载残留
  - 长期未更新 Gradle cache
  - 旧版 wrapper
- Cleanup Recipes 首轮
- Simulation 预演首轮
- 主题切换与应用壳层
- macOS / Windows 安装包基础任务配置

### 已有但仅为首轮 / 占位实现

- 规则快照展示
- 候选风险分级
- 预演结果展示
- “查看路径 / 加入方案 / 开始清理”等交互入口

### 尚未形成闭环

- 删除执行
- 回收站 / 废纸篓
- 白名单
- 项目引用保护
- 删除前确认弹窗
- 删除结果反馈
- 可编辑并持久化的规则设置

---

## 3. 缺口总览

| 能力域 | PRD 要求 | 当前状态 | 缺口判断 |
|---|---|---|---|
| 路径发现 | 自动发现 + 手动修改或新增路径 | 已支持默认路径和覆盖 | 缺“新增多扫描路径” |
| 缓存扫描 | 总量、来源、最大目录、重复版本排行、旧版本分布、wrapper 分布 | 已有总量、来源、热点目录 | 缺专门的排行/分布分析 |
| 候选识别 | 旧版本、180 天未使用、`.lastUpdated`、失败残留、SNAPSHOT、Gradle 无效缓存、旧 wrapper | 已有首轮 | 缺精度与完整覆盖 |
| 清理建议 | 基于规则给出建议 | 已有首轮 recipes | 缺可配置规则驱动 |
| 清理预演 | 展示将删内容、空间、风险、保护规则 | 已有默认预演 | 缺“按用户勾选动态预演” |
| 执行删除 | 勾选删除、一键按建议删除、结果反馈 | 未完成 | 核心缺口 |
| 安全保护 | 确认、白名单、项目引用保护、回收站、失败回显 | 未完成 | 核心缺口 |
| 设置页 | 路径、规则、白名单、回收站策略 | 仅路径和主题 | 缺大部分 |
| 平台要求 | macOS/Windows 回收站、路径兼容、软链接 | 部分满足 | 缺系统级能力与验证 |
| 开源交付 | 可分发安装包、可维护仓库 | 初步具备 | 缺 CI、LICENSE、Release 规范 |

---

## 4. P0（V1 必做）待办

这些任务完成后，项目才算真正达到 PRD 中“扫描 → 建议 → 预演 → 安全删除”的闭环。

---

## Sprint P0-1：删除执行闭环

**Goal**: 接入真实删除执行能力，形成可演示的 V1 核心闭环。  
**Demo/Validation**:
- 能从候选列表勾选项目
- 能执行删除前确认
- 能完成真实删除或移动到回收站
- 能看到删除结果反馈

### Task P0-1.1：定义删除计划与执行结果模型
- **Location**:
  - `core-model/src/main/kotlin/com/depjanitor/core/model/`
- **Description**:
  - 新增删除计划、删除项、执行结果、失败原因、执行模式（直接删除 / 移入回收站）等模型。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 可表达“用户选中了哪些候选项”
  - 可表达每个删除项的执行结果
  - 支持失败原因与风险汇总
- **Validation**:
  - 模型单元测试

### Task P0-1.2：实现删除执行服务
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/delete/`
  - `core-platform/src/main/kotlin/com/depjanitor/core/platform/trash/`
- **Description**:
  - 提供统一删除入口，支持：
    - 直接删除
    - 优先移入回收站 / 废纸篓
    - 删除失败回传原因
- **Dependencies**: Task P0-1.1
- **Acceptance Criteria**:
  - 能针对候选项逐项执行
  - 支持部分成功、部分失败
  - 不因单项失败中断整个批次
- **Validation**:
  - 临时目录集成测试

### Task P0-1.3：实现 macOS / Windows 回收站适配层
- **Location**:
  - `core-platform/src/main/kotlin/com/depjanitor/core/platform/trash/`
- **Description**:
  - 封装平台能力，优先移入废纸篓/回收站；无可用能力时再回退为直接删除。
- **Dependencies**: Task P0-1.2
- **Acceptance Criteria**:
  - 接口统一
  - 平台不可用时有明确回退策略
- **Validation**:
  - 单元测试 + 平台手动验证

### Task P0-1.4：接入删除确认弹窗
- **Location**:
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/`
- **Description**:
  - 删除前展示：
    - 删除项数量
    - 总释放空间
    - 风险提示
    - 是否移入回收站
    - 是否保留白名单项
- **Dependencies**: Task P0-1.1
- **Acceptance Criteria**:
  - 用户二次确认后才允许执行
  - 高风险项有显著提示
- **Validation**:
  - 手动交互验证

### Task P0-1.5：接入删除结果反馈页/结果面板
- **Location**:
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/pages/`
- **Description**:
  - 展示成功数、失败数、释放空间、失败原因列表。
- **Dependencies**: Task P0-1.2, Task P0-1.4
- **Acceptance Criteria**:
  - 删除后用户能明确看到结果
  - 失败项可重新审阅
- **Validation**:
  - 手动演示 + 临时目录测试

---

## Sprint P0-2：白名单与保护能力

**Goal**: 把“安全优先”从文案变成真实能力。  
**Demo/Validation**:
- 能将候选项加入白名单
- 白名单项默认不进入执行计划
- 能进行基础项目引用保护

### Task P0-2.1：定义白名单模型与持久化结构
- **Location**:
  - `core-model/src/main/kotlin/com/depjanitor/core/model/`
  - `core-platform/src/main/kotlin/com/depjanitor/core/platform/config/`
- **Description**:
  - 支持按坐标、版本、路径或来源维度保存白名单。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 白名单可持久化
  - 可用于候选过滤
- **Validation**:
  - 配置读写测试

### Task P0-2.2：在扫描/建议阶段应用白名单过滤
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/cleanup/`
- **Description**:
  - 被白名单命中的候选项标记为保护或直接过滤出默认方案。
- **Dependencies**: Task P0-2.1
- **Acceptance Criteria**:
  - 白名单项不会被默认勾选
  - UI 能看到“已保护”状态
- **Validation**:
  - 单元测试 + UI 手动验证

### Task P0-2.3：扫描结果页支持“加入白名单”
- **Location**:
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/pages/Pages.kt`
- **Description**:
  - 在 Artifact Atlas / Candidate Ledger 中提供加入白名单操作。
- **Dependencies**: Task P0-2.1
- **Acceptance Criteria**:
  - 可对 artifact / version / candidate 执行白名单操作
- **Validation**:
  - 手动交互验证

### Task P0-2.4：实现项目引用保护（V1 基础版）
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/protection/`
- **Description**:
  - 从用户指定项目目录中解析基础依赖坐标，命中后提升风险或设为保护。
- **Dependencies**: Task P0-2.1
- **Acceptance Criteria**:
  - 能识别基础 Maven / Gradle 项目依赖引用
  - 命中项默认不自动进入清理计划
- **Validation**:
  - 示例项目集成测试

---

## Sprint P0-3：规则可编辑并持久化

**Goal**: 让建议与预演真正由规则驱动，而非固定逻辑。  
**Demo/Validation**:
- 用户修改保留版本数/未使用阈值后重新扫描
- 建议列表和预演结果发生变化
- 重启应用后配置仍然生效

### Task P0-3.1：扩展设置模型
- **Location**:
  - `core-model/src/main/kotlin/com/depjanitor/core/model/AppSettings.kt`
- **Description**:
  - 新增：
    - 保留最近 N 个版本
    - 未使用时间阈值
    - 是否优先移入回收站
    - 是否启用自定义路径扫描
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 设置字段完整可持久化
- **Validation**:
  - 配置测试

### Task P0-3.2：扩展配置存储
- **Location**:
  - `core-platform/src/main/kotlin/com/depjanitor/core/platform/config/AppConfigStore.kt`
- **Description**:
  - 读写新增规则设置与开关项。
- **Dependencies**: Task P0-3.1
- **Acceptance Criteria**:
  - 重启应用后规则仍保留
- **Validation**:
  - 读写测试

### Task P0-3.3：Rule Forge 接入真实可编辑表单
- **Location**:
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/pages/Pages.kt`
  - `app-desktop/src/main/kotlin/com/depjanitor/app/Main.kt`
- **Description**:
  - 增加数值输入、开关、保存与重扫流程。
- **Dependencies**: Task P0-3.2
- **Acceptance Criteria**:
  - UI 可修改规则
  - 保存后立即生效或重扫生效
- **Validation**:
  - 手动验证

### Task P0-3.4：清理建议与预演改为完全读取规则配置
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/cleanup/WorkspaceCleanupAdvisorService.kt`
- **Description**:
  - 将当前默认规则改为读取用户配置。
- **Dependencies**: Task P0-3.3
- **Acceptance Criteria**:
  - 修改规则后结果变化可见
- **Validation**:
  - 单元测试 + 手动验证

---

## 5. P1（V1 强化）待办

这些任务不是阻塞 V1 闭环，但对 PRD 体验完整度影响很大。

---

## Sprint P1-1：扫描分析增强

**Goal**: 补足 PRD 要求的分析维度。  
**Demo/Validation**:
- 首页可展示更多分析指标
- 扫描结果支持更符合 PRD 的排序与排行

### Task P1-1.1：新增重复版本排行
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/analysis/`
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/pages/`
- **Description**:
  - 按 artifact 的版本数量输出排行。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 能展示前 N 个历史版本最多的 artifact
- **Validation**:
  - 单元测试 + UI 校验

### Task P1-1.2：新增旧版本分布与 wrapper 分布视图
- **Location**:
  - `core-model/src/main/kotlin/com/depjanitor/core/model/`
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/pages/`
- **Description**:
  - 增加 PRD 指定的“旧版本分布”“Gradle wrapper 分布”可视化数据。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 首页或结果页可展示分布信息
- **Validation**:
  - 手动验证

### Task P1-1.3：补充最近一次扫描时间
- **Location**:
  - `core-model/src/main/kotlin/com/depjanitor/core/model/`
  - `app-desktop/src/main/kotlin/com/depjanitor/app/Main.kt`
- **Description**:
  - 保存并展示最近一次扫描时间。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 首页可见最后扫描时间
- **Validation**:
  - 手动验证

### Task P1-1.4：支持按可清理优先级排序
- **Location**:
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/pages/Pages.kt`
- **Description**:
  - 在 Artifact Atlas / Candidate 列表中增加“按清理优先级排序”。
- **Dependencies**: 候选模型稳定
- **Acceptance Criteria**:
  - 排序结果体现风险和释放空间优先级
- **Validation**:
  - 手动验证

---

## Sprint P1-2：候选识别精度提升

**Goal**: 让识别逻辑更贴近 PRD 的“低风险优先”。  
**Demo/Validation**:
- 对 Maven / Gradle 候选项的分类更细
- 风险等级更准确

### Task P1-2.1：增强 Gradle 无效缓存识别
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/gradle/`
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/cleanup/`
- **Description**:
  - 细化 modules/transforms/jars 等结构识别，区分“可疑残留”和“正常缓存”。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - Gradle 候选分类更加准确
- **Validation**:
  - 样本目录测试

### Task P1-2.2：细化失败下载残留识别
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/cleanup/`
- **Description**:
  - 扩展 Maven / Gradle 的临时文件与异常文件识别规则。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 低风险候选覆盖更完整
- **Validation**:
  - 测试样本验证

### Task P1-2.3：引入更准确的时间语义
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/index/` 或现有 cleanup/analysis 模块
- **Description**:
  - 区分最后修改时间与近似使用时间；为空时采用回退策略。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - UI 明确使用的是哪种时间字段
- **Validation**:
  - 单元测试

### Task P1-2.4：细化风险等级模型
- **Location**:
  - `core-model/src/main/kotlin/com/depjanitor/core/model/RiskLevel.kt`
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/cleanup/`
- **Description**:
  - 在保留 PRD 低/中/高基础上，细化内部打分规则。
- **Dependencies**: 无
- **Acceptance Criteria**:
  - 同类候选风险分层更合理
- **Validation**:
  - 单元测试 + 人工评审

---

## Sprint P1-3：预演与交互增强

**Goal**: 把当前默认预演提升为用户可控预演。  
**Demo/Validation**:
- 用户勾选候选项后可动态预演
- 预演结果与确认弹窗一致

### Task P1-3.1：候选项选择状态管理
- **Location**:
  - `app-desktop/src/main/kotlin/com/depjanitor/app/`
  - `core-model/src/main/kotlin/com/depjanitor/core/model/`
- **Description**:
  - 为候选项增加选择态、批量选择、风险筛选。
- **Dependencies**: P0 删除闭环基础完成
- **Acceptance Criteria**:
  - 用户能控制哪些项进入执行计划
- **Validation**:
  - 手动验证

### Task P1-3.2：预演结果实时重算
- **Location**:
  - `core-engine/src/main/kotlin/com/depjanitor/core/engine/cleanup/`
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/pages/`
- **Description**:
  - 根据用户当前选择实时重算释放空间、风险汇总、保护项数量。
- **Dependencies**: Task P1-3.1
- **Acceptance Criteria**:
  - UI 上的预演数值与选择同步变化
- **Validation**:
  - 手动验证

### Task P1-3.3：查看路径与详情抽屉/弹层
- **Location**:
  - `app-desktop/src/main/kotlin/com/depjanitor/app/ui/pages/`
- **Description**:
  - 将“查看路径”接入真实路径，并可复制路径或在系统文件管理器中打开。
- **Dependencies**: 候选项与 artifact 记录真实路径
- **Acceptance Criteria**:
  - 路径信息真实可见
- **Validation**:
  - 手动验证

---

## 6. P2（后续演进）待办

这些能力更偏 V1.1 / V1.2，可在 V1 可用版发布后继续推进。

### Task P2-1：支持新增多个自定义扫描路径
- **Reason**: PRD 提到“手动修改或新增扫描路径”，当前仅覆盖默认路径

### Task P2-2：规则模板
- **Reason**: 对应 PRD V1.2

### Task P2-3：保存清理配置
- **Reason**: 对应 PRD V1.2

### Task P2-4：定时提醒
- **Reason**: 对应 PRD V1.2

### Task P2-5：CLI 补充版
- **Reason**: 对应 PRD V1.2

### Task P2-6：扫描报告导出
- **Reason**: 对应中期目标“更完整分析报告”

### Task P2-7：开源工程化完善
- **Location**:
  - 根目录
  - `.github/workflows/`
- **Description**:
  - 增加 LICENSE、CHANGELOG、GitHub Actions、发布脚本、Issue 模板。

---

## 7. 推荐开发顺序

建议严格按以下顺序推进：

1. **P0-3 规则持久化**
   - 因为它会影响建议、预演、执行逻辑的输入
2. **P0-2 白名单与保护能力**
   - 先把“不要删什么”确定好
3. **P0-1 删除执行闭环**
   - 在保护边界清晰后接入真实删除风险更低
4. **P1-3 用户可控预演**
   - 让执行前体验更完整
5. **P1-1 / P1-2 分析与识别增强**
   - 提升结果质量
6. **P2 后续演进**

如果希望更偏“尽快可演示”，也可采用另一顺序：

1. 删除确认弹窗
2. 直接删除（临时）
3. 回收站
4. 白名单
5. 项目引用保护

但这条路径风险更高，不推荐。

---

## 8. 建议里程碑

### Milestone A：V1 安全闭环 Alpha
包含：
- 规则持久化
- 白名单
- 删除确认弹窗
- 删除执行
- 删除结果反馈

### Milestone B：V1 可发布 Beta
包含：
- 回收站/废纸篓
- 项目引用保护基础版
- 动态预演
- 更完整设置页

### Milestone C：V1 正式版
包含：
- 分析维度补齐
- 候选识别精度增强
- 安装包图标/签名/CI

---

## 9. 测试策略

### 单元测试
- 模型序列化与映射
- 配置持久化
- 候选识别规则
- 白名单过滤
- 删除计划与执行结果

### 集成测试
- 临时目录模拟 Maven / Gradle 仓库
- 删除执行 / 回收站回退
- 项目引用保护

### UI 手动验证
- 路径配置与规则保存
- 候选项选择与预演
- 删除确认弹窗
- 删除结果回显
- 主题切换下的完整流程

### 跨平台验证
- macOS：废纸篓、软链接路径
- Windows：长路径、中文用户名、空格路径、回收站

---

## 10. 潜在风险与注意事项

### 风险 1：真实删除能力接入过早
- **问题**：在白名单和保护机制不完善时接入删除，误删风险高
- **建议**：优先完成规则 + 白名单 + 确认弹窗，再开删除

### 风险 2：时间字段语义不准确
- **问题**：当前更接近最后修改时间，不等于真正“未使用”
- **建议**：UI 上明确字段含义，后续再增强

### 风险 3：回收站跨平台行为不一致
- **问题**：不同平台 API 差异大
- **建议**：抽象统一接口，回退策略明确

### 风险 4：项目引用保护容易复杂化
- **问题**：完整引用解析范围很大
- **建议**：V1 只做基础版，先支持常见 Maven / Gradle 项目

### 风险 5：Gradle 结构复杂、误判成本高
- **问题**：无效缓存识别若过度激进，可能影响构建
- **建议**：Gradle 相关候选默认偏保守

---

## 11. 回滚策略

- 删除执行能力应在接入初期保留“仅预演模式”开关
- 高风险删除默认禁止批量一键执行
- 回收站不可用时必须二次确认是否直接删除
- 每次新增候选规则前先补样本测试，避免扩大误判面

---

## 12. 最终结论

当前项目已经具备 **“扫描 + 分析 + 候选识别 + 建议预演首轮”** 的基础，但距离 PRD 定义的 **V1 可用桌面客户端** 还差一个关键闭环：

> **安全删除能力 + 安全保护能力 + 可编辑规则能力**

因此后续开发建议聚焦三条主线：

1. **规则可编辑并持久化**
2. **白名单 / 项目引用保护 / 回收站等安全能力**
3. **真实删除执行与结果反馈**

这三条完成后，再继续补强分析精度与平台发布能力，项目就能更接近 PRD 的 V1 正式目标。
