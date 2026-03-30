# Dep Janitor

Dep Janitor 是一个面向 **macOS / Windows** 的开源桌面客户端，用于 **扫描、分析并清理 Maven / Gradle 本地依赖缓存**。

它的目标不是“一键暴力删缓存”，而是给 Java / Kotlin / Android / Gradle / Maven 开发者提供一个 **可视化、可选择、可回溯、风险可控** 的本地缓存治理工具。

---

## 1. 项目解决什么问题

在长期开发过程中，本机通常会积累大量依赖缓存，例如：

- Maven 本地仓库中的旧版本依赖
- 过期 SNAPSHOT
- `.lastUpdated` 残留文件
- 下载失败残留文件
- Gradle caches 中长期未使用的缓存目录
- 旧版 Gradle Wrapper distribution

这些内容会带来几个典型问题：

- 本地磁盘空间被持续占用
- Maven / Gradle 目录越来越臃肿
- 手动删除风险高，不知道哪些能删、哪些不能删
- 想清理时只能靠经验或 shell 命令，容易误删

Dep Janitor 通过桌面客户端的方式，把这件事做成一套完整流程：

1. 自动发现 Maven / Gradle 本地路径
2. 扫描缓存体积与依赖结构
3. 识别候选清理项
4. 让用户自行选择项目
5. 真实执行删除（移入回收站 / 直接删除）
6. 删除后重新扫描并刷新结果

---

## 2. 当前功能

当前仓库已经具备可运行的桌面客户端，并支持真实扫描与真实删除流程。

### 已实现能力

- Kotlin + Compose Multiplatform Desktop 多模块工程
- macOS / Windows 桌面端 UI
- 深色 / 浅色双主题
- 默认 Maven / Gradle / Wrapper 路径发现
- 自定义扫描路径
- 本地配置持久化
- Maven / Gradle / Wrapper 缓存扫描
- 热点目录统计
- 重复版本与旧版本分析
- 候选清理项识别
- 风险等级划分（低 / 中 / 高 / 已保护）
- 白名单
- 项目保护路径
- 候选项手动加入 / 移出删除方案
- 真实删除执行：
  - 移入回收站
  - 直接删除
- 删除完成后自动重新扫描

### 当前候选项类型

- 旧版本依赖
- 过期 SNAPSHOT
- `.lastUpdated` 文件
- 下载失败残留（part / tmp / lock 等）
- 长期未使用的 Gradle cache
- 旧版 Gradle Wrapper

---

## 3. 技术栈

- **语言**：Kotlin
- **桌面 UI**：Compose Multiplatform Desktop
- **构建工具**：Gradle Wrapper
- **运行时**：JDK 21
- **打包方式**：Compose Desktop Native Distributions（DMG / PKG / EXE / MSI）

---

## 4. 工程结构

```text
.
├─ app-desktop/      # 桌面应用入口、页面、主题、桌面端交互
├─ core-model/       # 领域模型：路径、风险、候选项、规则、结果模型
├─ core-engine/      # 扫描、分析、候选识别、删除执行等核心逻辑
├─ core-platform/    # 平台能力：路径发现、配置存储、回收站、文件管理器打开
├─ docs/
│  ├─ 需求分析/
│  ├─ 设计/
│  └─ 开发计划/
├─ gradle/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradlew
└─ gradlew.bat
```

### 模块职责

#### `app-desktop`
负责：

- 应用入口
- 页面导航
- 主题系统
- 用户交互
- 扫描结果展示
- 删除确认与执行页面

#### `core-model`
负责：

- 路径模型
- 风险等级模型
- 候选项模型
- 删除计划 / 删除结果模型
- 预览快照模型

#### `core-engine`
负责：

- 扫描目录
- 解析 Maven / Gradle 结构
- 识别候选项
- 生成建议
- 执行删除

#### `core-platform`
负责：

- 默认路径发现
- 配置读写
- 打开文件管理器
- 回收站支持

---

## 5. 环境要求

### 必需环境

- **JDK 21**
- macOS 或 Windows
- 不需要全局安装 Gradle，项目自带 **Gradle Wrapper**

### 检查 Java 版本

```bash
java -version
```

输出建议包含 `21`。

例如：

```bash
openjdk version "21.x.x"
```

---

## 6. 如何运行项目

### 6.1 克隆仓库

```bash
git clone <your-repo-url>
cd dep-janitor
```

### 6.2 启动桌面应用

macOS / Linux：

```bash
./gradlew :app-desktop:run
```

Windows：

```bat
gradlew.bat :app-desktop:run
```

---

## 7. 如何使用这个项目

下面按真实使用流程说明。

---

### 7.1 第一次打开后做什么

应用启动后，建议按下面步骤操作：

1. 进入 **首页**
2. 查看应用自动识别到的本地缓存路径
3. 如果默认路径不对，进入 **设置** 调整
4. 回到首页点击 **开始扫描**

---

### 7.2 路径发现规则

默认会自动识别：

#### macOS / Linux

- Maven：`~/.m2/repository`
- Gradle Caches：`~/.gradle/caches`
- Gradle Wrapper：`~/.gradle/wrapper`

#### Windows

- Maven：`%USERPROFILE%\.m2\repository`
- Gradle Caches：`%USERPROFILE%\.gradle\caches`
- Gradle Wrapper：`%USERPROFILE%\.gradle\wrapper`

如果你的缓存路径不是这些默认值，可以在 **设置** 页面中：

- 覆盖默认路径
- 添加自定义扫描路径
- 启用 / 禁用某个自定义路径

---

### 7.3 扫描后可以看什么

扫描完成后，你可以在 **扫描结果** 下看到 4 个标签页：

#### 1）总览
用于查看：

- 当前总缓存体积
- 预计可释放空间
- 候选项数量
- 保护项数量
- 热点目录
- 重复版本热点
- 来源分布（Maven / Gradle / Wrapper）

#### 2）依赖图谱
用于查看：

- 依赖坐标列表
- 版本数量
- 风险等级
- 清理优先级
- 依赖详情
- 保留版本 / 候选删除版本

#### 3）清理建议
用于：

- 看默认推荐项
- 看待人工复核项
- 搜索候选项
- 手动把候选项加入删除方案
- 批量加入 / 移出方案

#### 4）执行删除
用于：

- 查看当前已选项目
- 选择删除方式
- 发起真实删除
- 查看最近一次执行结果

---

### 7.4 风险等级是什么意思

#### 低风险
通常更适合优先清理，例如：

- 过期旧版本
- `.lastUpdated`
- 下载失败残留
- 长期未使用的部分 Gradle 缓存

#### 中风险
可以删除，但建议再复核，例如：

- 近期仍可能被项目使用的旧版本
- 一些不确定影响范围的缓存目录

#### 高风险
不建议直接批量删，例如：

- 旧 Wrapper 中可能仍需保留的内容
- 风险较高的 Gradle 内部缓存

#### 已保护
命中白名单或项目保护规则，不会进入最终删除计划。

---

### 7.5 如何真正删除

当前版本已经支持真实删除，不是模拟。

#### 删除步骤

1. 扫描完成后进入 **清理建议**
2. 选择你要处理的项目，加入删除方案
3. 切换到 **执行删除**
4. 确认“本次将删除的项目”
5. 选择删除方式：
   - **移入回收站**（推荐）
   - **直接删除**
6. 点击 **执行删除**
7. 在确认弹窗中再次确认
8. 应用会真实执行删除并自动重新扫描

#### 推荐测试方式

如果你是第一次测试，建议优先使用：

- **移入回收站**

这样既能验证真实删除链路，又相对安全。

#### 删除完成后会发生什么

- 已成功删除的项目会立即从当前 UI 中移除
- 应用会后台重新扫描，刷新全量结果
- 最近一次执行结果会显示成功 / 跳过 / 失败数量

---

### 7.6 白名单与保护路径

如果某些依赖或目录你不希望应用删除，可以使用：

#### 白名单
支持把以下内容加入白名单：

- 某个依赖坐标
- 某个具体路径

#### 项目保护路径
你可以把项目目录加入保护路径，降低误删正在使用依赖的风险。

---

## 8. 本地配置文件

应用会把配置保存到用户目录：

```text
~/.dep-janitor/config.properties
```

当前会持久化的内容包括：

- 主题模式
- 默认路径覆盖
- 自定义扫描路径
- 清理规则
- 是否扫描自定义路径
- 白名单
- 项目保护路径

如果你想重置本地配置，可以先关闭应用，然后删除这个文件。

---

## 9. 开发命令

### 9.1 运行应用

```bash
./gradlew :app-desktop:run
```

### 9.2 执行测试

```bash
./gradlew test --console=plain
```

### 9.3 查看桌面模块任务

```bash
./gradlew :app-desktop:tasks --all
```

### 9.4 查看依赖树

```bash
./gradlew :app-desktop:dependencies --configuration runtimeClasspath --console=plain
```

---

## 10. 如何打包

项目使用 Compose Desktop 原生打包能力。

### 10.1 先决条件

- JDK 21
- 使用项目自带 `gradlew` / `gradlew.bat`
- **建议在目标系统上打对应平台安装包**

也就是说：

- 在 **macOS** 上打 `.dmg` / `.pkg`
- 在 **Windows** 上打 `.exe` / `.msi`

> 一般不建议跨系统直接产出另一平台的安装包。

---

### 10.2 macOS 打包

#### 生成 `.dmg`

```bash
./gradlew :app-desktop:packageDmg --console=plain
```

#### 生成 `.pkg`

```bash
./gradlew :app-desktop:packagePkg --console=plain
```

#### 只生成 `.app` 分发目录

```bash
./gradlew :app-desktop:createDistributable --console=plain
```

产物通常位于：

```text
app-desktop/build/compose/binaries/main/app/
app-desktop/build/compose/binaries/main/dmg/
app-desktop/build/compose/binaries/main/pkg/
```

---

### 10.3 Windows 打包

在 Windows 上执行：

#### 生成 `.exe`

```bat
gradlew.bat :app-desktop:packageExe --console=plain
```

#### 生成 `.msi`

```bat
gradlew.bat :app-desktop:packageMsi --console=plain
```

产物通常位于：

```text
app-desktop\build\compose\binaries\main\exe\
app-desktop\build\compose\binaries\main\msi\
```

---

### 10.4 打包前建议先执行测试

```bash
./gradlew test --console=plain
```

---

### 10.5 安装包体积说明

桌面端安装包会内置运行时，因此体积不会像普通脚本项目那么小。

当前项目已经做过一轮瘦身优化，但仍会包含：

- Compose Desktop 运行依赖
- Skiko 原生绘制库
- 打包运行时（JRE / Runtime Image）

因此：

- `.app` 体积通常会明显大于源码体积
- `.dmg` / `.msi` 是压缩后的分发形式

这是 Java / Compose Desktop 桌面应用的常见现象。

---

## 11. 如何在 macOS 上做真实测试

推荐测试流程：

1. 执行打包：

```bash
./gradlew :app-desktop:packageDmg --console=plain
```

2. 安装 DMG 中的应用
3. 启动应用并扫描本机缓存
4. 在 **清理建议** 中选择少量低风险项目
5. 切换到 **执行删除**
6. 先使用 **移入回收站** 测试真实删除流程
7. 检查：
   - UI 是否正确刷新
   - Finder 是否能看到目标已移入废纸篓
   - 重新扫描结果是否同步变化

如果你确认逻辑正确，再测试 **直接删除**。

---

## 12. 项目文档

- PRD：`docs/需求分析/Maven与Gradle本地依赖缓存清理工具PRD.md`
- UI 设计方案：`docs/设计/客户端UI设计方案.md`
- V1 极简交互重构方案：`docs/设计/客户端V1极简交互重构方案.md`
- 开发计划：`docs/开发计划/dep-janitor-v1-development-plan.md`
- 功能缺口与优先级计划：`docs/开发计划/dep-janitor-prd-gap-backlog.md`

---

## 13. 当前状态与注意事项

### 当前已可用

- 真实扫描
- 真实候选识别
- 真实删除执行
- 本地配置持久化
- macOS DMG 打包

### 当前仍在持续完善

- 候选识别准确度持续提升
- Windows 安装包的完整测试与验证
- 安装包签名 / 发布流程
- CI 自动构建与发布
- 更细粒度的删除后增量刷新能力

---

## 14. 常见问题

### Q1：需要全局安装 Gradle 吗？
不需要。

直接使用项目自带：

- macOS / Linux：`./gradlew`
- Windows：`gradlew.bat`

### Q2：为什么安装包体积比较大？
因为桌面端分发包会包含运行时、Skiko 原生库和 Compose Desktop 依赖，这是正常现象。

### Q3：为什么建议优先用“移入回收站”？
因为它是真实删除流程，但更安全，更适合测试阶段。

### Q4：删除后为什么应用还会重新扫描？
因为应用需要重新读取磁盘上的真实状态，确保页面结果与本地缓存目录一致。

---

## 15. 参与贡献

欢迎通过以下方式参与：

- 提交 Issue
- 提交 Pull Request
- 补充规则识别与测试用例
- 完善文档、打包与发布流程

如果你准备提交代码，建议先执行：

```bash
./gradlew test --console=plain
```

---

## 16. 开源说明

本项目定位为开源开发者工具项目。

如果你计划正式对外发布，建议补充以下文件：

- `LICENSE`
- `CHANGELOG.md`
- `CONTRIBUTING.md`
- GitHub Actions / CI 发布流程

