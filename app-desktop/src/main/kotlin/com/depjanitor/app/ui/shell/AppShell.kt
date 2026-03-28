package com.depjanitor.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.depjanitor.app.ui.formatBytes
import com.depjanitor.app.ui.pages.ArtifactAtlasPage
import com.depjanitor.app.ui.pages.CleanupRecipesPage
import com.depjanitor.app.ui.pages.ObservatoryPage
import com.depjanitor.app.ui.pages.RuleForgePage
import com.depjanitor.app.ui.pages.SimulationPage
import com.depjanitor.app.ui.theme.ThemeMode
import com.depjanitor.app.ui.theme.panelBorder
import com.depjanitor.app.ui.theme.semanticColors
import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.CleanupExecutionMode
import com.depjanitor.core.model.CleanupExecutionResult
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.CustomScanPath
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.WhitelistEntry
import com.depjanitor.core.model.releasedBytes
import com.depjanitor.core.model.primaryRiskLevel
import com.depjanitor.core.model.versionCount

private enum class AppDestination(
    val label: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
) {
    Observatory(
        label = "总览",
        title = "本机依赖结构总览",
        description = "主分析舱 / 来源结构 / 依赖地层 / 风险热区",
        icon = Icons.Outlined.AutoGraph,
    ),
    ArtifactAtlas(
        label = "依赖图谱",
        title = "依赖图谱",
        description = "统一列表视图 + 右侧检视器",
        icon = Icons.Outlined.Hub,
    ),
    CleanupRecipes(
        label = "清理建议",
        title = "清理建议",
        description = "按策略包组织建议，而不是散点候选列表",
        icon = Icons.Outlined.CleaningServices,
    ),
    Simulation(
        label = "预演",
        title = "预演舱",
        description = "执行前对照、保护规则和删除方式统一预演",
        icon = Icons.Outlined.PlayCircleOutline,
    ),
    RuleForge(
        label = "规则工坊",
        title = "规则工坊",
        description = "路径、保留规则、白名单与恢复策略",
        icon = Icons.Outlined.SettingsEthernet,
    ),
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
    var destination by remember { mutableStateOf(AppDestination.Observatory) }
    val snackbarHostState = remember { SnackbarHostState() }
    val desktopPlatform = remember { detectDesktopPlatform() }
    val selectedPlanCandidates = remember(snapshot.candidates, selectedCandidateIds) {
        snapshot.candidates.filter { it.id in selectedCandidateIds }
    }
    val selectedPlanBytes = remember(selectedPlanCandidates) {
        selectedPlanCandidates.sumOf { it.sizeBytes }
    }

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
            val showInspector = maxWidth > 1340.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .border(1.dp, MaterialTheme.colorScheme.panelBorder, RoundedCornerShape(18.dp)),
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    CompactSidebar(
                        destination = destination,
                        onSelect = { destination = it },
                        selectedPlanCount = selectedPlanCandidates.size,
                        selectedPlanBytes = selectedPlanBytes,
                    )
                    MainWorkspace(
                        modifier = Modifier.weight(1f),
                        destination = destination,
                        snapshot = snapshot,
                        themeMode = themeMode,
                        isScanning = isScanning,
                        statusText = statusText,
                        showInspector = showInspector,
                        selectedPlanCount = selectedPlanCandidates.size,
                        selectedPlanBytes = selectedPlanBytes,
                        isCleaning = isCleaning,
                        lastCleanupResult = lastCleanupResult,
                        onThemeModeChange = onThemeModeChange,
                        onScan = onScan,
                        selectedCandidateIds = selectedCandidateIds,
                        onAddCandidatesToPlan = onAddCandidatesToPlan,
                        onRemoveCandidatesFromPlan = onRemoveCandidatesFromPlan,
                        onToggleCandidateInPlan = onToggleCandidateInPlan,
                        onResetPlanSelection = onResetPlanSelection,
                        onClearPlanSelection = onClearPlanSelection,
                        onExecuteCleanup = onExecuteCleanup,
                        onOpenPath = onOpenPath,
                        scanCustomPaths = scanCustomPaths,
                        onScanCustomPathsChange = onScanCustomPathsChange,
                        onRuleSetChange = onRuleSetChange,
                        onAddWhitelistEntry = onAddWhitelistEntry,
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
                    if (showInspector) {
                        InspectorPane(
                            destination = destination,
                            snapshot = snapshot,
                            selectedPlanCount = selectedPlanCandidates.size,
                            selectedPlanBytes = selectedPlanBytes,
                            projectProtectionPaths = projectProtectionPaths,
                            customScanPaths = customScanPaths,
                            lastCleanupResult = lastCleanupResult,
                        )
                    }
                }
                BottomStatusBar(
                    platform = desktopPlatform,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    isScanning = isScanning,
                    statusText = statusText,
                    selectedPlanCount = selectedPlanCandidates.size,
                    selectedPlanBytes = selectedPlanBytes,
                )
            }
        }
    }
}

@Composable
private fun CompactSidebar(
    destination: AppDestination,
    onSelect: (AppDestination) -> Unit,
    selectedPlanCount: Int,
    selectedPlanBytes: Long,
) {
    Column(
        modifier = Modifier
            .width(92.dp)
            .fillMaxHeight()
            .background(MaterialTheme.semanticColors.canvasAlt.copy(alpha = 0.26f))
            .border(1.dp, MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.45f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                .border(1.dp, MaterialTheme.colorScheme.panelBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.semanticColors.gradle),
                        ),
                        RoundedCornerShape(10.dp),
                    ),
            )
            Text("依赖管家", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, lineHeight = 12.sp)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppDestination.entries.forEach { item ->
                CompactSidebarItem(
                    item = item,
                    active = item == destination,
                    onClick = { onSelect(item) },
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
                .border(1.dp, MaterialTheme.colorScheme.panelBorder, RoundedCornerShape(14.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiniSidebarStat(value = selectedPlanCount.toString(), label = "方案")
            MiniSidebarStat(value = compactBytes(selectedPlanBytes), label = "释放")
        }
    }
}

@Composable
private fun CompactSidebarItem(
    item: AppDestination,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f) else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .border(
                width = 1.dp,
                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                    RoundedCornerShape(12.dp),
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
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2,
        )
    }
}

@Composable
private fun MainWorkspace(
    modifier: Modifier = Modifier,
    destination: AppDestination,
    snapshot: DashboardSnapshot,
    themeMode: ThemeMode,
    isScanning: Boolean,
    statusText: String,
    showInspector: Boolean,
    selectedPlanCount: Int,
    selectedPlanBytes: Long,
    isCleaning: Boolean,
    lastCleanupResult: CleanupExecutionResult?,
    onThemeModeChange: (ThemeMode) -> Unit,
    onScan: () -> Unit,
    selectedCandidateIds: Set<String>,
    onAddCandidatesToPlan: (Set<String>) -> Unit,
    onRemoveCandidatesFromPlan: (Set<String>) -> Unit,
    onToggleCandidateInPlan: (String) -> Unit,
    onResetPlanSelection: () -> Unit,
    onClearPlanSelection: () -> Unit,
    onExecuteCleanup: (Set<String>, CleanupExecutionMode) -> Unit,
    onOpenPath: (String) -> Unit,
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
    Column(
        modifier = modifier
            .fillMaxHeight()
            .border(
                width = if (showInspector) 1.dp else 0.dp,
                color = MaterialTheme.colorScheme.panelBorder.copy(alpha = 0.6f),
            ),
    ) {
        PageHeader(
            destination = destination,
            snapshot = snapshot,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            isScanning = isScanning,
            onScan = onScan,
            selectedPlanCount = selectedPlanCount,
            selectedPlanBytes = selectedPlanBytes,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (destination) {
                AppDestination.Observatory -> ObservatoryPage(snapshot, isScanning, statusText)
                AppDestination.ArtifactAtlas -> ArtifactAtlasPage(
                    snapshot = snapshot,
                    selectedCandidateIds = selectedCandidateIds,
                    onAddCandidatesToPlan = onAddCandidatesToPlan,
                    onRemoveCandidatesFromPlan = onRemoveCandidatesFromPlan,
                    onAddWhitelistEntry = onAddWhitelistEntry,
                    onOpenPath = onOpenPath,
                )
                AppDestination.CleanupRecipes -> CleanupRecipesPage(
                    snapshot = snapshot,
                    selectedCandidateIds = selectedCandidateIds,
                    onToggleCandidateInPlan = onToggleCandidateInPlan,
                    onAddWhitelistEntry = onAddWhitelistEntry,
                    onOpenPath = onOpenPath,
                )
                AppDestination.Simulation -> SimulationPage(
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
                AppDestination.RuleForge -> RuleForgePage(
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

@Composable
private fun PageHeader(
    destination: AppDestination,
    snapshot: DashboardSnapshot,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    isScanning: Boolean,
    onScan: () -> Unit,
    selectedPlanCount: Int,
    selectedPlanBytes: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                destination.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
            )
            Text(destination.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(destination.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderMetricPill("候选项", snapshot.candidates.size.toString())
            HeaderMetricPill("保护项", snapshot.protectedCoordinates.size.toString())
            HeaderMetricPill("可释放", formatBytes(selectedPlanBytes.takeIf { it > 0 } ?: snapshot.simulation.releasableBytes))
            if (selectedPlanCount > 0) {
                HeaderMetricPill("方案", "$selectedPlanCount 项")
            }
            CompactActionButton(
                icon = if (themeMode == ThemeMode.Obsidian) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                label = null,
                onClick = {
                    onThemeModeChange(
                        if (themeMode == ThemeMode.Obsidian) ThemeMode.Ivory else ThemeMode.Obsidian
                    )
                },
            )
            CompactActionButton(
                icon = Icons.Outlined.Refresh,
                label = if (isScanning) "扫描中" else "重新扫描",
                enabled = !isScanning,
                primary = true,
                onClick = onScan,
            )
        }
    }
}

@Composable
private fun InspectorPane(
    destination: AppDestination,
    snapshot: DashboardSnapshot,
    selectedPlanCount: Int,
    selectedPlanBytes: Long,
    projectProtectionPaths: List<String>,
    customScanPaths: List<CustomScanPath>,
    lastCleanupResult: CleanupExecutionResult?,
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (destination) {
            AppDestination.Observatory -> {
                InspectorHero("检视器", "当前会话快照", "右侧检视器只承载工作上下文，不再承担顶部过多控制项。")
                InspectorSection("方案摘要", "当前执行方案") {
                    InspectorKV("已选候选", "$selectedPlanCount 项")
                    InspectorKV("预计释放", formatBytes(selectedPlanBytes))
                    InspectorKV("保护规则排除", "${snapshot.protectedCoordinates.size} 项")
                    InspectorKV("高风险待复核", "${snapshot.highRiskCount} 组")
                }
                InspectorSection("上下文", "扫描范围") {
                    snapshot.detectedPaths.filter { it.exists }.take(3).forEach {
                        InspectorKV(it.kind.label, it.path)
                    }
                }
            }
            AppDestination.ArtifactAtlas -> {
                val selected = snapshot.artifactEntries.firstOrNull() ?: sampleArtifactFromStrata(snapshot)
                InspectorHero("当前依赖", selected.coordinate, "版本层、规则命中和白名单状态固定放在检视器，符合桌面客户端习惯。")
                InspectorSection("版本地层", "版本层详情") {
                    selected.versions.take(5).forEach { version ->
                        val color = riskColor(version.riskLevel ?: selected.primaryRiskLevel)
                        TinyTag(version.label, color)
                    }
                }
                InspectorSection("规则说明", "命中说明") {
                    InspectorKV("来源", selected.source.label)
                    InspectorKV("版本数", selected.versionCount.toString())
                    InspectorKV("占用体积", formatBytes(selected.totalSizeBytes))
                    InspectorKV("风险等级", selected.primaryRiskLevel.label)
                }
            }
            AppDestination.CleanupRecipes -> {
                val recipe = snapshot.recipes.firstOrNull()
                InspectorHero(
                    "当前策略",
                    recipe?.title ?: "残留清理",
                    "聚焦收益、默认开关和保护规则影响，不再把策略说明放到左侧导航里。",
                )
                InspectorSection("影响范围", "命中结果") {
                    InspectorKV("命中数量", (recipe?.hitCount ?: snapshot.candidates.size).toString())
                    InspectorKV("预计释放", formatBytes(recipe?.releasableBytes ?: snapshot.simulation.releasableBytes))
                    InspectorKV("白名单影响", snapshot.protectedCoordinates.size.toString())
                    InspectorKV("默认状态", if (recipe?.defaultEnabled == false) "未启用" else "已启用")
                }
            }
            AppDestination.Simulation -> {
                InspectorHero("执行安全", "删除检查清单", "执行前确认项统一留在检视器，主视图只保留前后对照和队列。")
                InspectorSection("检查清单", "执行前检查") {
                    InspectorKV("白名单排除", "已完成")
                    InspectorKV("高风险二次确认", if (snapshot.highRiskCount > 0) "待处理" else "无")
                    InspectorKV("删除方式", "移入回收站")
                    InspectorKV(
                        "最近执行",
                        lastCleanupResult?.let { "${it.mode.label} · ${formatBytes(it.releasedBytes)}" } ?: "尚未执行",
                    )
                }
            }
            AppDestination.RuleForge -> {
                InspectorHero("治理摘要", "规则总览", "设置页检视器负责汇总治理规模，不再让左侧出现过多配置块。")
                InspectorSection("摘要", "规则概况") {
                    InspectorKV("扫描路径", (snapshot.detectedPaths.size + customScanPaths.size).toString())
                    InspectorKV("白名单规则", snapshot.whitelistEntries.size.toString())
                    InspectorKV("工程保护路径", projectProtectionPaths.size.toString())
                    InspectorKV("回收站优先", "启用")
                }
            }
        }
    }
}

@Composable
private fun BottomStatusBar(
    platform: DesktopPlatform,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    isScanning: Boolean,
    statusText: String,
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isScanning) MaterialTheme.semanticColors.warn else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(platform.displayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("方案 $selectedPlanCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("释放 ${formatBytes(selectedPlanBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (themeMode == ThemeMode.Obsidian) "深色" else "浅色",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeaderMetricPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .border(1.dp, MaterialTheme.colorScheme.panelBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CompactActionButton(
    icon: ImageVector,
    label: String?,
    enabled: Boolean = true,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (primary) MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.96f else 0.20f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.24f else 0.14f),
            )
            .border(
                1.dp,
                if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.panelBorder,
                RoundedCornerShape(10.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (label == null) 8.dp else 10.dp, vertical = 8.dp),
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

@Composable
private fun MiniSidebarStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InspectorHero(kicker: String, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(kicker, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InspectorSection(kicker: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f))
            .border(1.dp, MaterialTheme.colorScheme.panelBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(kicker, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            content()
        },
    )
}

@Composable
private fun InspectorKV(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
    }
}

@Composable
private fun TinyTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
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

private fun compactBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L * 1024L)}G"
    bytes >= 1024L * 1024L -> "${bytes / (1024L * 1024L)}M"
    bytes > 0 -> "${bytes / 1024L}K"
    else -> "0"
}

private fun sampleArtifactFromStrata(snapshot: DashboardSnapshot): ArtifactScanEntry {
    val strata = snapshot.artifactEntries.firstOrNull()
    return strata ?: ArtifactScanEntry(
        coordinate = snapshot.strata.firstOrNull()?.coordinate ?: "org.example:artifact",
        source = snapshot.strata.firstOrNull()?.layers?.firstOrNull()?.source ?: ArtifactSource.MAVEN,
        totalSizeBytes = snapshot.strata.firstOrNull()?.totalSizeBytes ?: 0L,
        lastModifiedMillis = snapshot.scannedAtMillis,
        versions = emptyList(),
    )
}

@Composable
private fun riskColor(riskLevel: RiskLevel?): Color = when (riskLevel) {
    RiskLevel.LOW -> MaterialTheme.semanticColors.safe
    RiskLevel.MEDIUM -> MaterialTheme.semanticColors.warn
    RiskLevel.HIGH -> MaterialTheme.semanticColors.danger
    RiskLevel.PROTECTED -> MaterialTheme.semanticColors.protect
    null -> MaterialTheme.colorScheme.primary
}
