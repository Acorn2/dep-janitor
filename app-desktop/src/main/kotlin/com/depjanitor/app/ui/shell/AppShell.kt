package com.depjanitor.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.depjanitor.app.ui.components.Badge
import com.depjanitor.app.ui.components.MetricPill
import com.depjanitor.app.ui.components.PanelCard
import com.depjanitor.app.ui.icons.AppIcons
import com.depjanitor.app.ui.formatBytes
import com.depjanitor.app.ui.formatTimestamp
import com.depjanitor.app.ui.pages.ArtifactAtlasPage
import com.depjanitor.app.ui.pages.CleanupRecipesPage
import com.depjanitor.app.ui.pages.ObservatoryPage
import com.depjanitor.app.ui.pages.RuleForgePage
import com.depjanitor.app.ui.pages.CleanupExecutionPage
import com.depjanitor.app.ui.theme.ThemeMode
import com.depjanitor.app.ui.theme.panelBorder
import com.depjanitor.app.ui.theme.semanticColors
import com.depjanitor.core.model.CleanupExecutionMode
import com.depjanitor.core.model.CleanupExecutionResult
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.CustomScanPath
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.WhitelistEntry

private enum class MainSection(
    val label: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
) {
    Home(
        label = "首页",
        title = "开始扫描本机依赖缓存",
        description = "先发现路径，再开始扫描，再查看清理建议。",
        icon = AppIcons.Home,
    ),
    Results(
        label = "扫描结果",
        title = "扫描结果与清理建议",
        description = "统一查看总览、依赖图谱、清理建议和执行删除。",
        icon = AppIcons.Insights,
    ),
    Settings(
        label = "设置",
        title = "路径与清理规则设置",
        description = "管理扫描路径、白名单和清理规则。",
        icon = AppIcons.Settings,
    ),
}

private enum class ResultsTab(val label: String) {
    Overview("总览"),
    Atlas("依赖图谱"),
    Recipes("清理建议"),
    Execute("执行删除"),
}

private enum class DesktopPlatform {
    Mac,
    Windows,
    Other,
}

@Composable
fun AppShell(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    snapshot: DashboardSnapshot,
    isScanning: Boolean,
    statusText: String,
    onScan: () -> Unit,
    isCleaning: Boolean,
    lastCleanupResult: CleanupExecutionResult?,
    onExecuteCleanup: (Set<String>, CleanupExecutionMode) -> Unit,
    onOpenPath: (String) -> Unit,
    selectedCandidateIds: Set<String>,
    onAddCandidatesToPlan: (Set<String>) -> Unit,
    onRemoveCandidatesFromPlan: (Set<String>) -> Unit,
    onToggleCandidateInPlan: (String) -> Unit,
    onResetPlanSelection: () -> Unit,
    onClearPlanSelection: () -> Unit,
    scanCustomPaths: Boolean,
    onScanCustomPathsChange: (Boolean) -> Unit,
    onRuleSetChange: (CleanupRuleSet) -> Unit,
    onAddWhitelistEntry: (WhitelistEntry) -> Unit,
    onRemoveWhitelistEntry: (WhitelistEntry) -> Unit,
    projectProtectionPaths: List<String>,
    onAddProjectProtectionPath: (String) -> Unit,
    onRemoveProjectProtectionPath: (String) -> Unit,
    customScanPaths: List<CustomScanPath>,
    onAddCustomScanPath: (CustomScanPath) -> Unit,
    onToggleCustomScanPathEnabled: (CustomScanPath) -> Unit,
    onRemoveCustomScanPath: (CustomScanPath) -> Unit,
    onPathOverrideChange: (PathKind, String) -> Unit,
    onPathOverrideReset: (PathKind) -> Unit,
) {
    var section by remember { mutableStateOf(MainSection.Home) }
    var resultsTab by remember { mutableStateOf(ResultsTab.Overview) }
    val snackbarHostState = remember { SnackbarHostState() }
    val desktopPlatform = remember { detectDesktopPlatform() }
    val selectedPlanCandidates = remember(snapshot.candidates, selectedCandidateIds) {
        snapshot.candidates.filter { it.id in selectedCandidateIds }
    }
    val selectedPlanBytes = remember(selectedPlanCandidates) { selectedPlanCandidates.sumOf { it.sizeBytes } }
    val hasScanned = snapshot.scannedAtMillis > 0L

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.semanticColors.canvasAlt,
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.semanticColors.canvasAlt,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .border(1.dp, MaterialTheme.colorScheme.panelBorder, RoundedCornerShape(14.dp)),
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    SimplifiedSidebar(
                        section = section,
                        onSelect = { section = it },
                        platform = desktopPlatform,
                        isScanning = isScanning,
                        statusText = statusText,
                        hasScanned = hasScanned,
                        detectedPathCount = snapshot.detectedPaths.count { it.exists },
                        selectedPlanCount = selectedPlanCandidates.size,
                        selectedPlanBytes = selectedPlanBytes,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        MainHeader(
                            section = section,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            isScanning = isScanning,
                            onScan = onScan,
                            snapshot = snapshot,
                            selectedPlanCount = selectedPlanCandidates.size,
                            selectedPlanBytes = selectedPlanBytes,
                        )
                        if (section == MainSection.Results && hasScanned) {
                            ResultsTabs(
                                current = resultsTab,
                                onSelect = { resultsTab = it },
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            when (section) {
                                MainSection.Home -> HomePage(
                                    snapshot = snapshot,
                                    isScanning = isScanning,
                                    statusText = statusText,
                                    hasScanned = hasScanned,
                                    onScan = onScan,
                                    onGoResults = {
                                        section = MainSection.Results
                                        resultsTab = ResultsTab.Overview
                                    },
                                )

                                MainSection.Results -> {
                                    if (!hasScanned) {
                                        EmptyResultsState(
                                            onScan = onScan,
                                            isScanning = isScanning,
                                        )
                                    } else {
                                        when (resultsTab) {
                                            ResultsTab.Overview -> ObservatoryPage(
                                                snapshot = snapshot,
                                                isScanning = isScanning,
                                                statusText = statusText,
                                                onOpenPath = onOpenPath,
                                            )
                                            ResultsTab.Atlas -> ArtifactAtlasPage(
                                                snapshot = snapshot,
                                                selectedCandidateIds = selectedCandidateIds,
                                                onAddCandidatesToPlan = onAddCandidatesToPlan,
                                                onRemoveCandidatesFromPlan = onRemoveCandidatesFromPlan,
                                                onAddWhitelistEntry = onAddWhitelistEntry,
                                                onOpenPath = onOpenPath,
                                            )
                                            ResultsTab.Recipes -> CleanupRecipesPage(
                                                snapshot = snapshot,
                                                selectedCandidateIds = selectedCandidateIds,
                                                onToggleCandidateInPlan = onToggleCandidateInPlan,
                                                onAddCandidatesToPlan = onAddCandidatesToPlan,
                                                onRemoveCandidatesFromPlan = onRemoveCandidatesFromPlan,
                                                onClearPlanSelection = onClearPlanSelection,
                                                onAddWhitelistEntry = onAddWhitelistEntry,
                                                onOpenPath = onOpenPath,
                                            )
                                            ResultsTab.Execute -> CleanupExecutionPage(
                                                snapshot = snapshot,
                                                isCleaning = isCleaning,
                                                lastCleanupResult = lastCleanupResult,
                                                selectedCandidateIds = selectedCandidateIds,
                                                onToggleCandidateInPlan = onToggleCandidateInPlan,
                                                onResetPlanSelection = onResetPlanSelection,
                                                onClearPlanSelection = onClearPlanSelection,
                                                onExecuteCleanup = onExecuteCleanup,
                                                onOpenPath = onOpenPath,
                                            )
                                        }
                                    }
                                }

                                MainSection.Settings -> RuleForgePage(
                                    snapshot = snapshot,
                                    themeMode = themeMode,
                                    onThemeModeChange = onThemeModeChange,
                                    scanCustomPaths = scanCustomPaths,
                                    onScanCustomPathsChange = onScanCustomPathsChange,
                                    onRuleSetChange = onRuleSetChange,
                                    onRemoveWhitelistEntry = onRemoveWhitelistEntry,
                                    projectProtectionPaths = projectProtectionPaths,
                                    onAddProjectProtectionPath = onAddProjectProtectionPath,
                                    onRemoveProjectProtectionPath = onRemoveProjectProtectionPath,
                                    customScanPaths = customScanPaths,
                                    onAddCustomScanPath = onAddCustomScanPath,
                                    onToggleCustomScanPathEnabled = onToggleCustomScanPathEnabled,
                                    onRemoveCustomScanPath = onRemoveCustomScanPath,
                                    onPathOverrideChange = onPathOverrideChange,
                                    onPathOverrideReset = onPathOverrideReset,
                                )
                            }
                        }
                    }
                }
                BottomStatusBar(
                    platform = desktopPlatform,
                    themeMode = themeMode,
                    statusText = statusText,
                    isScanning = isScanning,
                    selectedPlanCount = selectedPlanCandidates.size,
                    selectedPlanBytes = selectedPlanBytes,
                )
            }
        }
    }
}

@Composable
private fun SimplifiedSidebar(
    section: MainSection,
    onSelect: (MainSection) -> Unit,
    platform: DesktopPlatform,
    isScanning: Boolean,
    statusText: String,
    hasScanned: Boolean,
    detectedPathCount: Int,
    selectedPlanCount: Int,
    selectedPlanBytes: Long,
) {
    val statusSummary = remember(isScanning, hasScanned, statusText) {
        sidebarStatusSummary(
            isScanning = isScanning,
            hasScanned = hasScanned,
            statusText = statusText,
        )
    }

    Column(
        modifier = Modifier
            .width(178.dp)
            .fillMaxHeight()
            .background(MaterialTheme.semanticColors.canvasAlt.copy(alpha = 0.20f))
            .border(1.dp, MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource("icons/runtime/dep-janitor-64.png"),
                    contentDescription = "Dep Janitor 图标",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("依赖管家", fontWeight = FontWeight.SemiBold)
                    Text("Dep Janitor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                statusSummary.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = statusSummary.color(),
            )
            Text(
                statusSummary.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "导航",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MainSection.entries.forEach { item ->
                SidebarSectionItem(
                    item = item,
                    active = item == section,
                    onClick = { onSelect(item) },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                .border(1.dp, MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.36f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("当前状态", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SidebarMetaRow("系统", platform.displayLabel)
            SidebarMetaRow("路径", "$detectedPathCount 个")
            SidebarMetaRow("方案", "$selectedPlanCount 项")
            SidebarMetaRow("可释放", formatBytes(selectedPlanBytes))
        }
    }
}

private data class SidebarStatusSummary(
    val title: String,
    val detail: String,
    val tone: SidebarStatusTone,
)

private enum class SidebarStatusTone {
    Active,
    Success,
    Idle,
}

@Composable
private fun SidebarStatusSummary.color() = when (tone) {
    SidebarStatusTone.Active -> MaterialTheme.semanticColors.wrapper
    SidebarStatusTone.Success -> MaterialTheme.semanticColors.safe
    SidebarStatusTone.Idle -> MaterialTheme.colorScheme.onSurface
}

private fun sidebarStatusSummary(
    isScanning: Boolean,
    hasScanned: Boolean,
    statusText: String,
): SidebarStatusSummary {
    val normalizedDetail = statusText.trim().ifBlank {
        if (isScanning) "正在扫描本机依赖缓存。"
        else if (hasScanned) "最近一次扫描已完成。"
        else "请先点击开始扫描。"
    }

    return when {
        isScanning -> SidebarStatusSummary(
            title = "正在扫描",
            detail = normalizedDetail,
            tone = SidebarStatusTone.Active,
        )

        hasScanned -> SidebarStatusSummary(
            title = "扫描已完成",
            detail = normalizedDetail,
            tone = SidebarStatusTone.Success,
        )

        else -> SidebarStatusSummary(
            title = "等待扫描",
            detail = normalizedDetail,
            tone = SidebarStatusTone.Idle,
        )
    }
}

@Composable
private fun SidebarMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SidebarSectionItem(
    item: MainSection,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
            )
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.18f),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                    RoundedCornerShape(999.dp),
                ),
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (active) {
                Text(
                    item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun MainHeader(
    section: MainSection,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    isScanning: Boolean,
    onScan: () -> Unit,
    snapshot: DashboardSnapshot,
    selectedPlanCount: Int,
    selectedPlanBytes: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .border(1.dp, MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.28f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dep Janitor · 桌面客户端", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(section.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(section.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderActionButton(
                    icon = if (themeMode == ThemeMode.Obsidian) AppIcons.Moon else AppIcons.Sun,
                    label = if (themeMode == ThemeMode.Obsidian) "深色" else "浅色",
                    onClick = {
                        onThemeModeChange(
                            if (themeMode == ThemeMode.Obsidian) ThemeMode.Ivory else ThemeMode.Obsidian,
                        )
                    },
                )
                HeaderActionButton(
                    icon = AppIcons.Refresh,
                    label = if (isScanning) "扫描中" else "开始扫描",
                    primary = true,
                    enabled = !isScanning,
                    onClick = onScan,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            when (section) {
                MainSection.Home -> {
                    HeaderMetricPill("已发现路径", snapshot.detectedPaths.count { it.exists }.toString())
                    HeaderMetricPill("待分析", snapshot.detectedPaths.size.toString())
                    HeaderMetricPill("当前状态", if (snapshot.scannedAtMillis > 0L) "已扫描" else "等待开始")
                }
                MainSection.Results -> {
                    HeaderMetricPill("候选项", snapshot.candidates.size.toString())
                    HeaderMetricPill("保护项", snapshot.protectedCoordinates.size.toString())
                    HeaderMetricPill("可释放", formatBytes(selectedPlanBytes.takeIf { it > 0 } ?: snapshot.simulation.releasableBytes))
                    if (selectedPlanCount > 0) {
                        HeaderMetricPill("方案", "$selectedPlanCount 项")
                    }
                }
                MainSection.Settings -> {
                    HeaderMetricPill("白名单", snapshot.whitelistEntries.size.toString())
                    HeaderMetricPill("保护路径", snapshot.projectProtectionPaths.size.toString())
                    HeaderMetricPill("当前主题", if (themeMode == ThemeMode.Obsidian) "深色" else "浅色")
                }
            }
        }
    }
}

@Composable
private fun ResultsTabs(
    current: ResultsTab,
    onSelect: (ResultsTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResultsTab.entries.forEach { tab ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (tab == current) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
                    )
                    .border(
                        1.dp,
                        if (tab == current) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.42f),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (tab == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomePage(
    snapshot: DashboardSnapshot,
    isScanning: Boolean,
    statusText: String,
    hasScanned: Boolean,
    onScan: () -> Unit,
    onGoResults: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PanelCard(
            title = "第一次使用，只需要按这 3 步操作",
            subtitle = "把首页做成操作引导页，而不是分析工作台。",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickStartStepCard(
                    index = "01",
                    icon = AppIcons.Home,
                    title = "确认路径",
                    description = "应用会自动发现 Maven 本地仓库、Gradle 缓存与 Wrapper 目录。",
                )
                QuickStartStepCard(
                    index = "02",
                    icon = AppIcons.Insights,
                    title = "开始扫描",
                    description = "点击一次开始扫描，生成缓存体积、候选项与风险判断。",
                )
                QuickStartStepCard(
                    index = "03",
                    icon = AppIcons.PlayCircle,
                    title = "选择后直接删除",
                    description = "进入扫描结果，勾选要处理的项目，再到执行删除页发起真实删除。",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                MetricPill("扫描范围", "Maven / Gradle")
                MetricPill("执行方式", "回收站 / 直接删除")
                MetricPill("删除策略", "手动选择")
                Badge("测试阶段可自行勾选项目删除", MaterialTheme.semanticColors.protect)
            }
            Text(
                "当前首页只负责告诉你下一步做什么：先点“开始扫描”，扫描完成后再去看“扫描结果”。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onScan, enabled = !isScanning) {
                    Text(if (isScanning) "扫描中…" else "开始扫描")
                }
                if (hasScanned) {
                    OutlinedButton(onClick = onGoResults) {
                        Text("查看扫描结果")
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            PanelCard(
                title = "已发现路径",
                subtitle = "这些是应用自动识别到的本地缓存目录。",
                modifier = Modifier.weight(1.1f),
            ) {
                if (snapshot.detectedPaths.isEmpty()) {
                    GuidedEmptyState(
                        icon = AppIcons.Home,
                        title = "还没有发现可扫描路径",
                        description = "请先到“设置”里检查默认路径或补充自定义扫描目录，然后再返回首页开始扫描。",
                    )
                } else {
                    snapshot.detectedPaths.forEach { path ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(path.kind.label, fontWeight = FontWeight.SemiBold)
                                Text(path.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Badge(
                                if (path.exists) "已发现" else "未发现",
                                if (path.exists) MaterialTheme.semanticColors.safe else MaterialTheme.semanticColors.warn,
                            )
                        }
                    }
                }
            }
            PanelCard(
                title = if (hasScanned) "扫描摘要" else "当前状态",
                subtitle = if (hasScanned) "扫描完成后，下一步去“扫描结果”继续查看。" else "现在还没有执行真实扫描。",
                modifier = Modifier.weight(0.9f),
            ) {
                if (hasScanned) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill("总缓存", formatBytes(snapshot.totalBytes))
                        MetricPill("候选项", snapshot.candidates.size.toString())
                        MetricPill("可释放", formatBytes(snapshot.simulation.releasableBytes))
                    }
                    Text("最近扫描：${formatTimestamp(snapshot.scannedAtMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onGoResults) {
                        Text("进入扫描结果")
                    }
                } else {
                    GuidedEmptyState(
                        icon = AppIcons.Insights,
                        title = if (isScanning) "正在建立扫描结果" else "还没有生成扫描结果",
                        description = if (snapshot.detectedPaths.any { it.exists }) {
                            "已发现 ${snapshot.detectedPaths.count { it.exists }} 个本地缓存路径。点击“开始扫描”后，应用会执行真实分析。"
                        } else {
                            "当前没有找到有效缓存路径。建议先去“设置”页确认路径，再返回首页开始扫描。"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.QuickStartStepCard(
    index: String,
    icon: ImageVector,
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f))
            .border(1.dp, MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Badge(index, MaterialTheme.colorScheme.primary)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyResultsState(
    onScan: () -> Unit,
    isScanning: Boolean,
) {
    PanelCard(
            title = "还没有扫描结果",
            subtitle = "请先在首页点击“开始扫描”，完成后再查看结果与清理建议。",
        ) {
        GuidedEmptyState(
                icon = AppIcons.Insights,
                title = "扫描结果页暂时为空",
                description = "当前还没有执行真实扫描，所以这里不会显示总览、依赖图谱、清理建议和执行删除内容。",
            )
        Button(onClick = onScan, enabled = !isScanning) {
            Text(if (isScanning) "扫描中…" else "立即开始扫描")
        }
    }
}

@Composable
private fun GuidedEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f))
            .border(1.dp, MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.34f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BottomStatusBar(
    platform: DesktopPlatform,
    themeMode: ThemeMode,
    statusText: String,
    isScanning: Boolean,
    selectedPlanCount: Int,
    selectedPlanBytes: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            statusText,
            style = MaterialTheme.typography.labelSmall,
            color = if (isScanning) MaterialTheme.semanticColors.warn else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(platform.displayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("方案 $selectedPlanCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("释放 ${formatBytes(selectedPlanBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (themeMode == ThemeMode.Obsidian) "深色" else "浅色", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HeaderMetricPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f))
            .border(1.dp, MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HeaderActionButton(
    icon: ImageVector,
    label: String?,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (primary) MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.94f else 0.20f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.16f else 0.10f),
            )
            .border(
                1.dp,
                if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.42f),
                RoundedCornerShape(10.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (label == null) 8.dp else 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label ?: "操作",
            tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        label?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun detectDesktopPlatform(): DesktopPlatform {
    val name = System.getProperty("os.name")?.lowercase().orEmpty()
    return when {
        "mac" in name -> DesktopPlatform.Mac
        "win" in name -> DesktopPlatform.Windows
        else -> DesktopPlatform.Other
    }
}

private val DesktopPlatform.displayLabel: String
    get() = when (this) {
        DesktopPlatform.Mac -> "苹果系统"
        DesktopPlatform.Windows -> "微软系统"
        DesktopPlatform.Other -> "桌面端"
    }
