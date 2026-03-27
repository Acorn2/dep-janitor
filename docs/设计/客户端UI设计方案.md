# Dep Janitor 客户端 UI 设计方案（双主题版）

> 基于 `docs/需求分析/Maven与Gradle本地依赖缓存清理工具PRD.md` 重设计并扩展为双主题  
> 产出日期：2026-03-27  
> 目标：建立一套既有产品辨识度、又支持浅色/深色切换的桌面端设计系统。

---

## 1. 结论

当前产品应采用：

> **Obsidian Artifact Lab（深色默认） + Ivory Lab（浅色辅助）**

不是简单“反色”，而是两套共享同一语义系统的主题：

- 深色主题：强调沉浸、分析、实验室控制台气质
- 浅色主题：强调日间使用、截图传播、信息清晰度

两者共享：

- 相同信息架构
- 相同品牌语义
- 相同来源色语义（Maven / Gradle）
- 相同风险语义（Safe / Warn / Danger / Protect）
- 相同核心组件（依赖地层、策略卡、预演舱、规则工坊）

---

## 2. 基于 PRD 的设计判断

PRD 的重点不是“做个桌面 dashboard”，而是：

- 面向 Java 开发者
- 处理 Maven / Gradle 本地依赖缓存
- 强调安全优先
- 强调先分析再删除
- 强调规则化治理与预演

因此 UI 的任务是：

1. 让用户看懂本地依赖结构
2. 让用户理解风险来源
3. 让用户通过策略包而不是散点列表做决策
4. 让用户在执行前完成可视化预演

所以视觉上必须避免：

- 通用 SaaS 模板感
- 深蓝 + 亮蓝 + 薄荷绿的互联网后台感
- 纯白后台 + 蓝按钮的通用企业工具感

---

## 3. 设计概念

## 3.1 主题命名

### 深色主题：**Obsidian Artifact Lab**
关键词：
- 黑曜石
- 仪器面板
- 依赖地层
- 风险取证
- 安全控制台

### 浅色主题：**Ivory Lab**
关键词：
- 象牙纸面
- 工程档案
- 实验记录卡
- 日间分析台
- 文档级清晰度

### 两者关系

- 深色是默认主主题
- 浅色是完整辅助主题
- 浅色不是深色的简单反相，而是“实验室纸面版”

---

## 4. 信息架构

```text
Dep Janitor
├─ Observatory（总览）
│  ├─ 核心容量读数
│  ├─ Maven / Gradle 来源结构
│  ├─ 风险光谱
│  ├─ 依赖地层图
│  └─ 热点仓与空间分布
├─ Artifact Atlas（扫描结果）
│  ├─ 来源树
│  ├─ Artifact 列表
│  ├─ Version Strata 版本地层
│  ├─ 风险说明
│  └─ 白名单操作
├─ Cleanup Recipes（清理建议）
│  ├─ 策略卡
│  ├─ 命中范围
│  ├─ 预计释放空间
│  ├─ 风险等级
│  └─ 是否加入本次方案
├─ Simulation Chamber（预演）
│  ├─ Before / After 对照
│  ├─ 风险变化
│  ├─ 保留项
│  └─ 执行确认
└─ Rule Forge（设置）
   ├─ 路径管理
   ├─ 保留策略
   ├─ 删除策略
   ├─ 白名单
   └─ 恢复与日志
```

---

## 5. 双主题配色系统

这是本次方案的重点。

## 5.1 配色原则

1. 品牌色与风险色严格分离
2. Maven / Gradle 需要独立来源色
3. 浅色和深色只改变承载背景，不改变语义逻辑
4. 高风险不用刺眼纯红，保持专业克制
5. 深色偏“实验室”，浅色偏“档案纸面”

---

## 5.2 深色主题：Obsidian Artifact Lab

### 基础底色

| Token | 色值 |
|---|---|
| `bg.canvas` | `#121317` |
| `bg.sidebar` | `#16181D` |
| `bg.surface` | `#1B1F26` |
| `bg.surface-2` | `#232833` |
| `bg.surface-3` | `#20242C` |
| `border.default` | `#2E3542` |
| `border.soft` | `#262C37` |

### 文本色

| Token | 色值 |
|---|---|
| `text.primary` | `#EEF1F6` |
| `text.secondary` | `#A7AFBD` |
| `text.muted` | `#7F8797` |
| `text.disabled` | `#5F6674` |

### 品牌与语义色

| Token | 色值 | 说明 |
|---|---|---|
| `accent.primary` | `#7A6FF0` | Iris 紫 |
| `source.maven` | `#C97A44` | Copper 铜棕 |
| `source.gradle` | `#4FA59A` | Verdigris 铜绿 |
| `risk.safe` | `#7BAE84` | 鼠尾草绿 |
| `risk.warn` | `#C79A56` | 黄铜色 |
| `risk.danger` | `#B76579` | 酒红色 |
| `risk.protect` | `#7F8DA6` | 冷钢灰 |

---

## 5.3 浅色主题：Ivory Lab

### 基础底色

| Token | 色值 |
|---|---|
| `bg.canvas` | `#F4F1EA` |
| `bg.sidebar` | `#ECE7DD` |
| `bg.surface` | `#FFFCF7` |
| `bg.surface-2` | `#F7F2EA` |
| `bg.surface-3` | `#F1ECE4` |
| `border.default` | `#D8D0C4` |
| `border.soft` | `#E5DDD1` |

### 文本色

| Token | 色值 |
|---|---|
| `text.primary` | `#1F2430` |
| `text.secondary` | `#5E6573` |
| `text.muted` | `#7B8190` |
| `text.disabled` | `#A5ABB8` |

### 品牌与语义色

| Token | 色值 | 说明 |
|---|---|---|
| `accent.primary` | `#6E63DD` | 更柔和的 Iris 紫 |
| `source.maven` | `#B96F3F` | 浅底下更稳的铜棕 |
| `source.gradle` | `#468F87` | 浅底下更稳的铜绿 |
| `risk.safe` | `#6F9E77` | 低风险 |
| `risk.warn` | `#B78949` | 中风险 |
| `risk.danger` | `#A55C6F` | 高风险 |
| `risk.protect` | `#728099` | 保护语义 |

### 设计意图

浅色主题不是“办公软件白底版”，而是：

- 有纸面与档案感
- 更适合白天使用
- 更适合文档截图与 README 演示
- 保留专业感，不变成普通企业管理后台

---

## 5.4 双主题共享语义

无论浅色还是深色，以下语义不变：

- 主品牌操作：Iris 紫
- Maven 来源：Copper 铜棕
- Gradle 来源：Verdigris 铜绿
- 低风险：Safe
- 中风险：Warn
- 高风险：Danger
- 白名单/保护：Protect

这样用户在切换主题后，不会丢失产品认知。

---

## 6. 核心组件

## 6.1 Artifact Strata（依赖地层视图）

用于展示一个 artifact 的多版本沉积关系：

- 最新版在前层
- 越旧越后退
- Maven / Gradle 用来源色边线区分
- 高风险版本加酒红/玫瑰边提示

双主题要求：
- 深色主题更强调层叠阴影与深浅差
- 浅色主题更强调边框与纸面层次

## 6.2 Cleanup Recipe Card（清理策略卡）

不是推荐删除项列表，而是策略包：

- 删除失败残留
- 保留最近 2 个版本
- 清理 180 天未使用版本
- 清理旧 wrapper

双主题要求：
- 深色更偏控制台卡片
- 浅色更偏实验记录卡

## 6.3 Simulation Chamber（预演舱）

用于清理前后对照：

- Before / After
- 风险变化
- 保留项
- 白名单项
- 删除方式

双主题要求：
- 深色更突出“安全演练舱”
- 浅色更突出“清理方案对照页”

## 6.4 Rule Forge（规则工坊）

设置页强调治理规则而非简单配置项：

- 路径
- 保留版本数
- 未使用阈值
- 回收站策略
- 白名单
- 日志与恢复

---

## 7. 页面风格要求

## 7.1 总览页 Observatory

不要做成：
- 标准四卡片 + donut

要做成：
- 主分析舱
- 来源结构舱
- 依赖地层
- 热区矩阵

## 7.2 扫描结果页 Artifact Atlas

要突出：
- 来源树
- Artifact 列表
- 右侧版本地层
- 风险说明

## 7.3 清理建议页 Cleanup Recipes

要突出：
- 策略卡
- 命中范围
- 风险等级
- 预计释放空间

## 7.4 预演页 Simulation Chamber

要突出：
- Before / After
- 风险变化
- 白名单保护
- 最终确认

## 7.5 设置页 Rule Forge

要突出：
- 路径管理
- 策略治理
- 安全策略
- 恢复与日志

---

## 8. 主题切换策略

## 8.1 默认行为

- 默认主题：深色 `Obsidian Artifact Lab`
- 支持用户手动切换为浅色 `Ivory Lab`
- 应记住用户最近选择

## 8.2 切换原则

主题切换时不改变：
- 页面布局
- 信息层级
- 组件结构
- 来源/风险语义

只改变：
- 背景层级
- 文本对比
- 边框强度
- 阴影与高光表现

## 8.3 不建议做法

- 简单整体反色
- 只换背景，不重算边框与文本对比
- 浅色主题沿用深色主题的高亮强度

---

## 9. 排版与字体

推荐：

- 标题 / 大数字：`Iowan Old Style`, `Source Han Serif SC`, `Georgia`, `serif`
- 正文 / UI：`IBM Plex Sans`, `PingFang SC`, `Segoe UI`, `sans-serif`
- 路径 / 版本 / 数字：`JetBrains Mono`, `SF Mono`, `Consolas`, `monospace`

主题要求：
- 深色强调大数字的仪器读数感
- 浅色强调标题的档案与记录感

---

## 10. 最终建议

Dep Janitor 最适合的不是“只有一个深色主题”，也不是“简单做个浅色皮肤”，而是：

> **一套有统一产品语义的双主题设计系统。**

最终建议：

- 默认使用深色主题承载产品主气质
- 提供浅色主题满足办公、阅读、截图与演示需求
- 所有视觉切换都围绕同一套品牌语义和工程语义展开

---

## 11. 当前交付物

已同步更新：

1. `docs/设计/客户端UI设计方案.md`
2. `docs/设计/客户端原型图.html`

其中静态原型应支持：
- 浅色 / 深色切换
- 同一套页面结构下的双主题展示

