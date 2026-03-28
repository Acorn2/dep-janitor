package com.depjanitor.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.depjanitor.app.ui.formatBytes
import com.depjanitor.app.ui.formatTimestamp
import com.depjanitor.app.ui.components.Badge
import com.depjanitor.app.ui.components.GradientBar
import com.depjanitor.app.ui.components.MetricPill
import com.depjanitor.app.ui.components.PanelCard
import com.depjanitor.app.ui.theme.ThemeMode
import com.depjanitor.app.ui.theme.semanticColors
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.CleanupExecutionMode
import com.depjanitor.core.model.CleanupExecutionResult
import com.depjanitor.core.model.CleanupExecutionStatus
import com.depjanitor.core.model.CleanupCandidate
import com.depjanitor.core.model.CustomScanPath
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.primaryRiskLevel
import com.depjanitor.core.model.versionCount
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.WhitelistEntry
import com.depjanitor.core.model.displayLabel
import com.depjanitor.core.model.failureCount
import com.depjanitor.core.model.releasedBytes
import com.depjanitor.core.model.skippedCount
import com.depjanitor.core.model.successCount

@Composable
fun ObservatoryPage(snapshot: DashboardSnapshot, isScanning: Boolean, statusText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PanelCard(
                title = "Analysis Chamber",
                subtitle = "用主分析舱取代普通 KPI 四卡片。",
                modifier = Modifier.weight(1.4f),
            ) {
                Text(
                    formatBytes(snapshot.totalBytes),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (snapshot.reclaimableBytes > 0L) "当前已完成真实扫描，预计最多可释放 ${formatBytes(snapshot.reclaimableBytes)}。" else "当前已完成真实路径扫描；策略估算将在后续 Sprint 接入。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("总缓存", formatBytes(snapshot.totalBytes))
                    MetricPill("热点仓", snapshot.hotspots.size.toString())
                    MetricPill("有效路径", snapshot.detectedPaths.count { it.exists }.toString())
                }
            }
            PanelCard(
                title = "Simulation Core",
                subtitle = "先预演再清理。",
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), MaterialTheme.colorScheme.surfaceVariant),
                            ),
                            RoundedCornerShape(24.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Safe-first Simulation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatBytes(snapshot.simulation.releasableBytes), fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
                        Text("已选 ${snapshot.simulation.selectedItemCount} 项，保护规则 ${snapshot.simulation.protectedRuleCount} 条")
                    }
                }
            }
        }

        PanelCard(title = "扫描状态", subtitle = "Sprint 1 已接入真实目录扫描。") {
            Badge(if (isScanning) "正在扫描" else "扫描完成", if (isScanning) MaterialTheme.semanticColors.warn else MaterialTheme.semanticColors.safe)
            Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("最近扫描：${formatTimestamp(snapshot.scannedAtMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PanelCard(title = "来源结构", subtitle = "Maven / Gradle / 风险语义保持统一", modifier = Modifier.weight(1f)) {
                GradientBar("Maven", formatBytes(snapshot.mavenBytes), safeProgress(snapshot.mavenBytes, snapshot.totalBytes), listOf(MaterialTheme.semanticColors.maven.copy(alpha = 0.35f), MaterialTheme.semanticColors.maven))
                GradientBar("Gradle", formatBytes(snapshot.gradleBytes), safeProgress(snapshot.gradleBytes, snapshot.totalBytes), listOf(MaterialTheme.semanticColors.gradle.copy(alpha = 0.35f), MaterialTheme.semanticColors.gradle))
                GradientBar("低风险项", snapshot.lowRiskCount.toString(), safeProgress(snapshot.lowRiskCount.toLong(), 128L), listOf(MaterialTheme.semanticColors.safe.copy(alpha = 0.35f), MaterialTheme.semanticColors.safe))
                GradientBar("中风险项", snapshot.mediumRiskCount.toString(), safeProgress(snapshot.mediumRiskCount.toLong(), 128L), listOf(MaterialTheme.semanticColors.warn.copy(alpha = 0.35f), MaterialTheme.semanticColors.warn))
            }
            PanelCard(title = "自动发现路径", subtitle = "Sprint 1 会把这些路径接入真实扫描。", modifier = Modifier.weight(1f)) {
                snapshot.detectedPaths.forEach { path ->
                    PathRow(path)
                }
            }
        }

        PanelCard(title = "热点仓排行", subtitle = "当前来自真实扫描结果。") {
            val maxHotspot = snapshot.hotspots.maxOfOrNull { it.sizeBytes }?.takeIf { it > 0 } ?: 1L
            snapshot.hotspots.forEach { hotspot ->
                val colors = when (hotspot.source) {
                    ArtifactSource.MAVEN -> listOf(MaterialTheme.semanticColors.maven.copy(alpha = 0.35f), MaterialTheme.semanticColors.maven)
                    ArtifactSource.GRADLE -> listOf(MaterialTheme.semanticColors.gradle.copy(alpha = 0.35f), MaterialTheme.semanticColors.gradle)
                    ArtifactSource.WRAPPER -> listOf(MaterialTheme.semanticColors.wrapper.copy(alpha = 0.35f), MaterialTheme.semanticColors.wrapper)
                }
                GradientBar(hotspot.name, formatBytes(hotspot.sizeBytes), hotspot.sizeBytes.toFloat() / maxHotspot, colors)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PanelCard(title = "重复版本排行", subtitle = "按版本数量和体积识别重复依赖热点。", modifier = Modifier.weight(1f)) {
                snapshot.duplicateVersionRankings.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.coordinate, fontWeight = FontWeight.SemiBold)
                            Text("${item.versionCount} 个版本 · ${item.staleVersionCount} 个旧版本", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(formatBytes(item.totalSizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            PanelCard(title = "旧版本分布", subtitle = "观察历史版本密度与潜在清理空间。", modifier = Modifier.weight(1f)) {
                val maxDistribution = snapshot.oldVersionDistributions.maxOfOrNull { it.totalSizeBytes }?.takeIf { it > 0 } ?: 1L
                snapshot.oldVersionDistributions.forEach { distribution ->
                    GradientBar(
                        distribution.label,
                        "${distribution.artifactCount} 个 artifact · ${formatBytes(distribution.totalSizeBytes)}",
                        distribution.totalSizeBytes.toFloat() / maxDistribution,
                        listOf(MaterialTheme.semanticColors.warn.copy(alpha = 0.35f), MaterialTheme.semanticColors.warn),
                    )
                }
            }
        }

        PanelCard(title = "Wrapper 分布", subtitle = "帮助识别多个 Gradle wrapper distribution 的占用结构。") {
            val maxWrapper = snapshot.wrapperDistributions.maxOfOrNull { it.sizeBytes }?.takeIf { it > 0 } ?: 1L
            snapshot.wrapperDistributions.forEach { wrapper ->
                GradientBar(
                    wrapper.label,
                    "${formatBytes(wrapper.sizeBytes)} · ${formatTimestamp(wrapper.lastModifiedMillis)}",
                    wrapper.sizeBytes.toFloat() / maxWrapper,
                    listOf(MaterialTheme.semanticColors.wrapper.copy(alpha = 0.35f), MaterialTheme.semanticColors.wrapper),
                )
            }
        }

        PanelCard(title = "Artifact Strata", subtitle = "依赖地层视图已经进入桌面壳。") {
            snapshot.strata.forEach { strata ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strata.coordinate, fontWeight = FontWeight.SemiBold)
                        Text(formatBytes(strata.totalSizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    strata.layers.forEachIndexed { index, layer ->
                        val baseColor = when (layer.source) {
                            ArtifactSource.MAVEN -> MaterialTheme.semanticColors.maven
                            ArtifactSource.GRADLE -> MaterialTheme.semanticColors.gradle
                            ArtifactSource.WRAPPER -> MaterialTheme.semanticColors.wrapper
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (index * 14).dp)
                                .background(baseColor.copy(alpha = 0.10f + (index * 0.03f)), RoundedCornerShape(999.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(layer.label)
                                Text(layer.state, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ArtifactSortMode(val label: String) {
    PRIORITY("按清理优先级"),
    SIZE("按大小"),
    LAST_MODIFIED("按时间参考"),
    VERSION_COUNT("按版本数"),
    NAME("按名称"),
}

@Composable
fun ArtifactAtlasPage(
    snapshot: DashboardSnapshot,
    selectedCandidateIds: Set<String>,
    onAddCandidatesToPlan: (Set<String>) -> Unit,
    onRemoveCandidatesFromPlan: (Set<String>) -> Unit,
    onAddWhitelistEntry: (WhitelistEntry) -> Unit,
    onOpenPath: (String) -> Unit,
) {
    val entries = snapshot.artifactEntries.ifEmpty {
        snapshot.strata.map { strata ->
            ArtifactScanEntry(
                coordinate = strata.coordinate,
                source = strata.layers.firstOrNull()?.source ?: ArtifactSource.MAVEN,
                totalSizeBytes = strata.totalSizeBytes,
                lastModifiedMillis = 0L,
                versions = strata.layers.map { layer ->
                    com.depjanitor.core.model.VersionScanEntry(
                        label = layer.label,
                        sizeBytes = 0L,
                        lastModifiedMillis = 0L,
                        source = layer.source,
                        riskLevel = layer.riskLevel,
                        state = layer.state,
                    )
                },
            )
        }
    }
    var query by remember(entries) { mutableStateOf("") }
    var selectedSource by remember(entries) { mutableStateOf<ArtifactSource?>(null) }
    var selectedRisk by remember(entries) { mutableStateOf<RiskLevel?>(null) }
    var sortMode by remember(entries) { mutableStateOf(ArtifactSortMode.PRIORITY) }
    val candidatesByCoordinate = remember(snapshot.candidates) {
        snapshot.candidates.groupBy { it.coordinate }
    }
    val priorityByCoordinate = remember(entries, candidatesByCoordinate) {
        entries.associate { entry -> entry.coordinate to cleanupPriorityMetrics(entry, candidatesByCoordinate) }
    }

    val filteredEntries = remember(entries, query, selectedSource, selectedRisk, sortMode, priorityByCoordinate) {
        entries
            .asSequence()
            .filter { entry ->
                query.isBlank() || entry.coordinate.contains(query, ignoreCase = true) ||
                    entry.group.orEmpty().contains(query, ignoreCase = true) ||
                    entry.artifact.orEmpty().contains(query, ignoreCase = true)
            }
            .filter { entry -> selectedSource == null || entry.source == selectedSource }
            .filter { entry -> selectedRisk == null || entry.primaryRiskLevel == selectedRisk }
            .sortedWith(
                when (sortMode) {
                    ArtifactSortMode.PRIORITY -> compareByDescending<ArtifactScanEntry> {
                        priorityByCoordinate[it.coordinate]?.priorityScore ?: Int.MIN_VALUE
                    }
                        .thenByDescending { priorityByCoordinate[it.coordinate]?.reclaimableBytes ?: 0L }
                        .thenByDescending { priorityByCoordinate[it.coordinate]?.staleCandidateCount ?: 0 }
                        .thenByDescending { it.totalSizeBytes }
                        .thenBy { it.coordinate.lowercase() }
                    ArtifactSortMode.SIZE -> compareByDescending<ArtifactScanEntry> { it.totalSizeBytes }
                    ArtifactSortMode.LAST_MODIFIED -> compareByDescending<ArtifactScanEntry> { it.lastModifiedMillis }
                    ArtifactSortMode.VERSION_COUNT -> compareByDescending<ArtifactScanEntry> { it.versionCount }
                    ArtifactSortMode.NAME -> compareBy<ArtifactScanEntry> { it.coordinate.lowercase() }
                },
            )
            .toList()
    }

    var selectedCoordinate by remember(filteredEntries) { mutableStateOf(filteredEntries.firstOrNull()?.coordinate.orEmpty()) }
    LaunchedEffect(filteredEntries) {
        if (filteredEntries.none { it.coordinate == selectedCoordinate }) {
            selectedCoordinate = filteredEntries.firstOrNull()?.coordinate.orEmpty()
        }
    }
    val selected = filteredEntries.firstOrNull { it.coordinate == selectedCoordinate } ?: filteredEntries.firstOrNull()
    val relatedCandidates = remember(selected, snapshot.candidates) {
        selected?.let { entry ->
            snapshot.candidates.filter { candidate ->
                candidate.coordinate == entry.coordinate && candidate.path != null && candidate.riskLevel != RiskLevel.PROTECTED
            }
        }.orEmpty()
    }
    val relatedCandidateIds = remember(relatedCandidates) { relatedCandidates.map { it.id }.toSet() }
    val selectedRelatedIds = remember(selectedCandidateIds, relatedCandidateIds) { selectedCandidateIds.intersect(relatedCandidateIds) }
    val relatedReclaimableBytes = remember(relatedCandidates) { relatedCandidates.sumOf { it.sizeBytes } }
    val allRelatedSelected = relatedCandidateIds.isNotEmpty() && selectedRelatedIds.size == relatedCandidateIds.size
    val pendingRelatedCount = relatedCandidateIds.size - selectedRelatedIds.size

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        PanelCard(title = "Sources", subtitle = "已接入真实 Maven / Gradle 解析结果。", modifier = Modifier.weight(0.9f)) {
            snapshot.detectedPaths.forEach {
                Badge(it.kind.label, colorForSource(it.kind))
            }
            Spacer(Modifier.height(8.dp))
            Badge("Maven ${entries.count { it.source == ArtifactSource.MAVEN }}", MaterialTheme.semanticColors.maven)
            Badge("Gradle ${entries.count { it.source == ArtifactSource.GRADLE }}", MaterialTheme.semanticColors.gradle)
            Badge("Wrapper ${entries.count { it.source == ArtifactSource.WRAPPER }}", MaterialTheme.semanticColors.wrapper)
            Spacer(Modifier.height(8.dp))
            Text("来源过滤", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterBadge("全部", selectedSource == null) { selectedSource = null }
                FilterBadge("Maven", selectedSource == ArtifactSource.MAVEN) { selectedSource = ArtifactSource.MAVEN }
                FilterBadge("Gradle", selectedSource == ArtifactSource.GRADLE) { selectedSource = ArtifactSource.GRADLE }
                FilterBadge("Wrapper", selectedSource == ArtifactSource.WRAPPER) { selectedSource = ArtifactSource.WRAPPER }
            }
            Spacer(Modifier.height(8.dp))
            Text("风险过滤", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterBadge("全部", selectedRisk == null) { selectedRisk = null }
                FilterBadge("低", selectedRisk == RiskLevel.LOW) { selectedRisk = RiskLevel.LOW }
                FilterBadge("中", selectedRisk == RiskLevel.MEDIUM) { selectedRisk = RiskLevel.MEDIUM }
                FilterBadge("高", selectedRisk == RiskLevel.HIGH) { selectedRisk = RiskLevel.HIGH }
            }
            Spacer(Modifier.height(10.dp))
            Text("最近扫描：${formatTimestamp(snapshot.scannedAtMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("重复版本排行", color = MaterialTheme.colorScheme.onSurfaceVariant)
            snapshot.duplicateVersionRankings.take(3).forEach { item ->
                Text("• ${item.coordinate} · ${item.versionCount} 版本", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text("Wrapper 分布", color = MaterialTheme.colorScheme.onSurfaceVariant)
            snapshot.wrapperDistributions.take(3).forEach { item ->
                Text("• ${item.label} · ${formatBytes(item.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        PanelCard(title = "Artifact Rows", subtitle = "支持搜索、筛选与排序。", modifier = Modifier.weight(1.35f)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索 artifact / group / 坐标") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ArtifactSortMode.entries.forEach { mode ->
                    FilterBadge(mode.label, sortMode == mode) { sortMode = mode }
                }
            }
            Text("共 ${filteredEntries.size} 条结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(420.dp)) {
                items(filteredEntries) { entry ->
                    val selectedRow = entry.coordinate == selectedCoordinate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedRow) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                                RoundedCornerShape(18.dp),
                            )
                            .padding(14.dp)
                            .clickable { selectedCoordinate = entry.coordinate },
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.coordinate, fontWeight = FontWeight.SemiBold)
                            Text("${entry.versionCount} versions · ${entry.source.label} · ${entry.primaryRiskLevel.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatBytes(entry.totalSizeBytes))
                            Text(formatTimestamp(entry.lastModifiedMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(entry.timeBasis.displayLabel(entry.timeBasisFallback), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        PanelCard(title = "Version Detail", subtitle = "展示选中结果的版本层与大小信息。", modifier = Modifier.weight(1f)) {
            if (selected == null) {
                Text("暂无可展示的 artifact 结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val priorityMetrics = priorityByCoordinate[selected.coordinate] ?: cleanupPriorityMetrics(selected, candidatesByCoordinate)
                Text(selected.coordinate, fontWeight = FontWeight.SemiBold)
                Text(formatBytes(selected.totalSizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("时间参考：${formatTimestamp(selected.lastModifiedMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "可清理候选 ${relatedCandidateIds.size} 项 · 已加入方案 ${selectedRelatedIds.size} 项 · 可释放 ${formatBytes(relatedReclaimableBytes)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (priorityMetrics.hasActionableCandidate) {
                    Text(
                        "清理优先级：${priorityMetrics.label}",
                        color = MaterialTheme.semanticColors.safe,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                selected.versions.forEach { layer ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(layer.label)
                            Text(formatTimestamp(layer.lastModifiedMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(layer.timeBasis.displayLabel(layer.timeBasisFallback), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatBytes(layer.sizeBytes))
                            layer.riskLevel?.let { Badge(it.label, colorForRisk(it)) } ?: Text(layer.state, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            selected.path?.let(onOpenPath)
                                ?: selected.versions.firstOrNull { !it.path.isNullOrBlank() }?.path?.let(onOpenPath)
                        },
                        enabled = selected.path != null || selected.versions.any { it.path != null },
                    ) { Text("查看路径") }
                    OutlinedButton(onClick = { onAddWhitelistEntry(WhitelistEntry.coordinate(selected.coordinate)) }) { Text("加入白名单") }
                }
                Button(
                    onClick = {
                        if (allRelatedSelected) {
                            onRemoveCandidatesFromPlan(relatedCandidateIds)
                        } else {
                            onAddCandidatesToPlan(relatedCandidateIds)
                        }
                    },
                    enabled = relatedCandidateIds.isNotEmpty(),
                ) {
                    Text(
                        when {
                            relatedCandidateIds.isEmpty() -> "无可加入候选"
                            allRelatedSelected -> "移出本次方案"
                            pendingRelatedCount < relatedCandidateIds.size -> "补充剩余 $pendingRelatedCount 项"
                            else -> "加入本次方案"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterBadge(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
    ) {
        Text(label, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CleanupRecipesPage(
    snapshot: DashboardSnapshot,
    selectedCandidateIds: Set<String>,
    onToggleCandidateInPlan: (String) -> Unit,
    onAddWhitelistEntry: (WhitelistEntry) -> Unit,
    onOpenPath: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PanelCard(title = "Rule Snapshot", subtitle = "当前清理建议由默认规则集生成。") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricPill("保留版本", "${snapshot.ruleSet.retainLatestVersions}")
                MetricPill("未使用阈值", "${snapshot.ruleSet.unusedDaysThreshold} 天")
                MetricPill("优先低风险", if (snapshot.ruleSet.prioritizeLowRisk) "是" else "否")
                MetricPill("移动到回收站", if (snapshot.ruleSet.moveToTrash) "是" else "否")
                MetricPill("白名单", snapshot.whitelistEntries.size.toString())
                MetricPill("当前方案", selectedCandidateIds.size.toString())
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            snapshot.recipes.forEach { recipe ->
                PanelCard(title = recipe.title, subtitle = recipe.description, modifier = Modifier.weight(1f)) {
                    Badge(recipe.riskLevel.label, colorForRisk(recipe.riskLevel))
                    Badge(recipe.source.label, colorForSource(recipe.source))
                    MetricPill("命中数量", recipe.hitCount.toString())
                    MetricPill("预计释放", formatBytes(recipe.releasableBytes))
                    Text(
                        if (recipe.defaultEnabled) "默认启用" else "需人工确认",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PanelCard(title = "Candidate Ledger", subtitle = "按风险与大小排序的候选清单。") {
            if (snapshot.candidates.isEmpty()) {
                Text("当前没有识别到可清理候选项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                snapshot.candidates.take(12).forEach { candidate ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(candidate.coordinate, fontWeight = FontWeight.SemiBold)
                                Badge(candidate.kind.label, colorForSource(candidate.source))
                                Badge(candidate.riskLevel.label, colorForRisk(candidate.riskLevel))
                            }
                            Text(candidate.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            candidate.versionLabel?.let {
                                Text("版本 / 文件：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("时间依据：${candidate.timeBasis.displayLabel(candidate.timeBasisFallback)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val actionable = candidate.path != null && candidate.riskLevel != RiskLevel.PROTECTED
                            Text(formatBytes(candidate.sizeBytes))
                            Text(formatTimestamp(candidate.lastModifiedMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (candidate.defaultSelected) "默认加入方案" else "需人工确认",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (candidate.defaultSelected) MaterialTheme.semanticColors.safe else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(
                                onClick = {
                                    onAddWhitelistEntry(
                                        candidate.path?.let(WhitelistEntry::path)
                                            ?: WhitelistEntry.coordinate(candidate.coordinate),
                                    )
                                },
                            ) { Text("加入白名单") }
                            OutlinedButton(
                                onClick = { candidate.path?.let(onOpenPath) },
                                enabled = candidate.path != null,
                            ) { Text("查看路径") }
                            Button(
                                onClick = { onToggleCandidateInPlan(candidate.id) },
                                enabled = actionable,
                            ) {
                                Text(
                                    when {
                                        !actionable -> "不可加入"
                                        candidate.id in selectedCandidateIds -> "移出方案"
                                        else -> "加入方案"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulationPage(
    snapshot: DashboardSnapshot,
    isCleaning: Boolean,
    lastCleanupResult: CleanupExecutionResult?,
    selectedCandidateIds: Set<String>,
    onToggleCandidateInPlan: (String) -> Unit,
    onResetPlanSelection: () -> Unit,
    onClearPlanSelection: () -> Unit,
    onExecuteCleanup: (Set<String>, CleanupExecutionMode) -> Unit,
    onOpenPath: (String) -> Unit,
) {
    val actionableCandidates = snapshot.candidates.filter { it.path != null && it.riskLevel != RiskLevel.PROTECTED }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var highRiskConfirmed by remember(selectedCandidateIds) { mutableStateOf(false) }
    var directDeleteConfirmed by remember(selectedCandidateIds, snapshot.ruleSet.moveToTrash) { mutableStateOf(false) }
    val selectedCandidates = actionableCandidates.filter { it.id in selectedCandidateIds }
    val readyCandidates = actionableCandidates.filter { it.defaultSelected }
    val reviewCandidates = actionableCandidates.filterNot { it.defaultSelected }
    val selectedHighRiskCount = selectedCandidates.count { it.riskLevel == RiskLevel.HIGH }
    val executionMode = if (snapshot.ruleSet.moveToTrash) CleanupExecutionMode.MOVE_TO_TRASH else CleanupExecutionMode.DELETE_DIRECTLY

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PanelCard(title = "Before", subtitle = "当前缓存结构", modifier = Modifier.weight(1f)) {
                MetricPill("总缓存", formatBytes(snapshot.totalBytes))
                MetricPill("总候选", snapshot.candidates.size.toString())
                MetricPill("保护项", snapshot.simulation.protectedRuleCount.toString())
            }
            PanelCard(title = "After", subtitle = "默认方案预演结果", modifier = Modifier.weight(1f)) {
                MetricPill("默认释放", formatBytes(selectedCandidates.sumOf { it.sizeBytes }))
                MetricPill("已选项目", selectedCandidateIds.size.toString())
                MetricPill("高风险复核", selectedHighRiskCount.toString())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onResetPlanSelection) { Text("恢复默认选择") }
                    OutlinedButton(onClick = onClearPlanSelection, enabled = selectedCandidateIds.isNotEmpty()) { Text("清空方案") }
                }
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = selectedCandidateIds.isNotEmpty() && !isCleaning,
                ) { Text(if (isCleaning) "清理中…" else "开始清理") }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PanelCard(title = "Ready Now", subtitle = "默认勾选的低风险候选。", modifier = Modifier.weight(1f)) {
                if (readyCandidates.isEmpty()) {
                    Text("当前没有自动加入方案的低风险项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    readyCandidates.take(8).forEach { candidate ->
                        CandidateSelectionRow(
                            candidate = candidate,
                            checked = candidate.id in selectedCandidateIds,
                            onCheckedChange = { onToggleCandidateInPlan(candidate.id) },
                            onOpenPath = onOpenPath,
                        )
                    }
                }
            }
            PanelCard(title = "Needs Review", subtitle = "需人工确认的中高风险项。", modifier = Modifier.weight(1f)) {
                if (reviewCandidates.isEmpty()) {
                    Text("当前没有待复核项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    reviewCandidates.take(8).forEach { candidate ->
                        CandidateSelectionRow(
                            candidate = candidate,
                            checked = candidate.id in selectedCandidateIds,
                            onCheckedChange = { onToggleCandidateInPlan(candidate.id) },
                            onOpenPath = onOpenPath,
                        )
                    }
                }
            }
        }

        lastCleanupResult?.let { result ->
            PanelCard(title = "Execution Result", subtitle = "展示最近一次清理执行结果。") {
                MetricPill("成功", result.successCount.toString())
                MetricPill("失败", result.failureCount.toString())
                MetricPill("跳过", result.skippedCount.toString())
                MetricPill("释放空间", formatBytes(result.releasedBytes))
                result.entries.take(10).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.item.coordinate, fontWeight = FontWeight.SemiBold)
                            Text("执行方式：${result.mode.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            entry.message?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Badge(
                            entry.status.label,
                            when (entry.status) {
                                CleanupExecutionStatus.TRASHED, CleanupExecutionStatus.DELETED -> MaterialTheme.semanticColors.safe
                                CleanupExecutionStatus.SKIPPED -> MaterialTheme.semanticColors.warn
                                CleanupExecutionStatus.FAILED -> MaterialTheme.semanticColors.danger
                            },
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认执行清理") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("本次将处理 ${selectedCandidates.size} 项候选，预计释放 ${formatBytes(selectedCandidates.sumOf { it.sizeBytes })}。")
                    Text("执行方式：${executionMode.label}")
                    Text("高风险项：$selectedHighRiskCount 项")
                    Text("白名单项不会进入执行计划。")
                    if (selectedHighRiskCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = highRiskConfirmed, onCheckedChange = { highRiskConfirmed = it })
                            Text("我已确认处理高风险项")
                        }
                    }
                    if (executionMode == CleanupExecutionMode.DELETE_DIRECTLY) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = directDeleteConfirmed, onCheckedChange = { directDeleteConfirmed = it })
                            Text("我确认直接删除而非移入回收站")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onExecuteCleanup(
                            selectedCandidateIds,
                            executionMode,
                        )
                    },
                    enabled = (selectedHighRiskCount == 0 || highRiskConfirmed) &&
                        (executionMode != CleanupExecutionMode.DELETE_DIRECTLY || directDeleteConfirmed),
                ) { Text("确认清理") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
fun RuleForgePage(
    snapshot: DashboardSnapshot,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    scanCustomPaths: Boolean,
    onScanCustomPathsChange: (Boolean) -> Unit,
    onRuleSetChange: (CleanupRuleSet) -> Unit,
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
    var retainVersionsInput by remember(snapshot.ruleSet.retainLatestVersions) { mutableStateOf(snapshot.ruleSet.retainLatestVersions.toString()) }
    var unusedDaysInput by remember(snapshot.ruleSet.unusedDaysThreshold) { mutableStateOf(snapshot.ruleSet.unusedDaysThreshold.toString()) }
    var prioritizeLowRisk by remember(snapshot.ruleSet.prioritizeLowRisk) { mutableStateOf(snapshot.ruleSet.prioritizeLowRisk) }
    var moveToTrash by remember(snapshot.ruleSet.moveToTrash) { mutableStateOf(snapshot.ruleSet.moveToTrash) }
    var deleteLastUpdated by remember(snapshot.ruleSet.deleteLastUpdatedFiles) { mutableStateOf(snapshot.ruleSet.deleteLastUpdatedFiles) }
    var deleteFailedDownloads by remember(snapshot.ruleSet.deleteFailedDownloads) { mutableStateOf(snapshot.ruleSet.deleteFailedDownloads) }
    var deleteStaleSnapshots by remember(snapshot.ruleSet.deleteStaleSnapshots) { mutableStateOf(snapshot.ruleSet.deleteStaleSnapshots) }
    var newProjectPath by remember(projectProtectionPaths) { mutableStateOf("") }
    var newCustomPath by remember { mutableStateOf("") }
    var newCustomPathKind by remember { mutableStateOf(PathKind.MAVEN_REPOSITORY) }
    val defaultPathRows = remember(snapshot.detectedPaths) {
        PathKind.entries.mapNotNull { kind ->
            snapshot.detectedPaths.firstOrNull { it.kind == kind }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        PanelCard(title = "Path Discovery", subtitle = "路径管理已支持自定义覆盖并参与下一次扫描。", modifier = Modifier.weight(1.2f)) {
            defaultPathRows.forEach { detectedPath ->
                EditablePathRow(
                    path = detectedPath,
                    onSave = { onPathOverrideChange(detectedPath.kind, it) },
                    onReset = { onPathOverrideReset(detectedPath.kind) },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("新增扫描路径", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PathKind.entries.forEach { kind ->
                    FilterBadge(kind.label, newCustomPathKind == kind) { newCustomPathKind = kind }
                }
            }
            OutlinedTextField(
                value = newCustomPath,
                onValueChange = { newCustomPath = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("新增自定义扫描路径") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        onAddCustomScanPath(CustomScanPath(kind = newCustomPathKind, path = newCustomPath.trim()))
                        newCustomPath = ""
                    },
                    enabled = newCustomPath.isNotBlank(),
                ) { Text("添加扫描路径") }
                MetricPill("附加路径", customScanPaths.size.toString())
                MetricPill("启用中", customScanPaths.count { it.enabled }.toString())
            }
            if (customScanPaths.isEmpty()) {
                Text("当前没有新增扫描路径。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                customScanPaths.forEach { entry ->
                    CustomScanPathRow(
                        entry = entry,
                        onToggleEnabled = { onToggleCustomScanPathEnabled(entry) },
                        onRemove = { onRemoveCustomScanPath(entry) },
                    )
                }
            }
        }
        PanelCard(title = "Rule Forge", subtitle = "把规则从展示态升级为真实可编辑配置。", modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = retainVersionsInput,
                onValueChange = { retainVersionsInput = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("保留最近 N 个版本") },
                singleLine = true,
            )
            OutlinedTextField(
                value = unusedDaysInput,
                onValueChange = { unusedDaysInput = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("未使用时间阈值（天）") },
                singleLine = true,
            )
            ToggleRow("启用自定义路径扫描", scanCustomPaths) { onScanCustomPathsChange(it) }
            ToggleRow("低风险优先", prioritizeLowRisk) { prioritizeLowRisk = it }
            ToggleRow("优先移入回收站", moveToTrash) { moveToTrash = it }
            ToggleRow("清理 .lastUpdated", deleteLastUpdated) { deleteLastUpdated = it }
            ToggleRow("清理失败下载残留", deleteFailedDownloads) { deleteFailedDownloads = it }
            ToggleRow("清理过期 SNAPSHOT", deleteStaleSnapshots) { deleteStaleSnapshots = it }

            val parsedRetainVersions = retainVersionsInput.toIntOrNull()?.coerceIn(1, 20) ?: snapshot.ruleSet.retainLatestVersions
            val parsedUnusedDays = unusedDaysInput.toIntOrNull()?.coerceIn(1, 3650) ?: snapshot.ruleSet.unusedDaysThreshold
            val nextRuleSet = CleanupRuleSet(
                retainLatestVersions = parsedRetainVersions,
                unusedDaysThreshold = parsedUnusedDays,
                deleteLastUpdatedFiles = deleteLastUpdated,
                deleteFailedDownloads = deleteFailedDownloads,
                deleteStaleSnapshots = deleteStaleSnapshots,
                prioritizeLowRisk = prioritizeLowRisk,
                moveToTrash = moveToTrash,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onRuleSetChange(nextRuleSet) }) { Text("保存规则并重扫") }
                OutlinedButton(
                    onClick = {
                        val defaults = CleanupRuleSet()
                        retainVersionsInput = defaults.retainLatestVersions.toString()
                        unusedDaysInput = defaults.unusedDaysThreshold.toString()
                        prioritizeLowRisk = defaults.prioritizeLowRisk
                        moveToTrash = defaults.moveToTrash
                        deleteLastUpdated = defaults.deleteLastUpdatedFiles
                        deleteFailedDownloads = defaults.deleteFailedDownloads
                        deleteStaleSnapshots = defaults.deleteStaleSnapshots
                        onRuleSetChange(defaults)
                    },
                ) { Text("恢复默认规则") }
            }

            MetricPill("当前生效保留数", snapshot.ruleSet.retainLatestVersions.toString())
            MetricPill("当前阈值", "${snapshot.ruleSet.unusedDaysThreshold} 天")
            MetricPill("默认策略", if (snapshot.ruleSet.prioritizeLowRisk) "低风险优先" else "全部候选")
            MetricPill("回收站策略", if (snapshot.ruleSet.moveToTrash) "优先移入" else "直接删除")
            Spacer(Modifier.height(8.dp))
            Text("白名单管理", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (snapshot.whitelistEntries.isEmpty()) {
                Text("当前还没有白名单项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                snapshot.whitelistEntries.take(8).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.value, fontWeight = FontWeight.Medium)
                            Text(entry.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { onRemoveWhitelistEntry(entry) }) { Text("移除") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("项目引用保护", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("为项目目录建立基础引用保护，命中的依赖将自动标记为保护。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = newProjectPath,
                onValueChange = { newProjectPath = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("添加项目目录路径") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        onAddProjectProtectionPath(newProjectPath)
                        newProjectPath = ""
                    },
                    enabled = newProjectPath.isNotBlank(),
                ) { Text("添加保护项目") }
                MetricPill("受保护项目", projectProtectionPaths.size.toString())
                MetricPill("命中坐标", snapshot.protectedCoordinates.size.toString())
            }
            if (projectProtectionPaths.isEmpty()) {
                Text("当前没有配置项目引用保护路径。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                projectProtectionPaths.take(8).forEach { projectPath ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(projectPath, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { onRemoveProjectProtectionPath(projectPath) }) { Text("移除") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("当前主题：${themeMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onThemeModeChange(ThemeMode.Obsidian) }) { Text("切换 Obsidian") }
                Button(onClick = { onThemeModeChange(ThemeMode.Ivory) }) { Text("切换 Ivory") }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CandidateRow(candidate: CleanupCandidate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(candidate.coordinate, fontWeight = FontWeight.SemiBold)
            Text(candidate.reason, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Badge(candidate.riskLevel.label, colorForRisk(candidate.riskLevel))
            Text(formatBytes(candidate.sizeBytes))
        }
    }
}

@Composable
private fun CandidateSelectionRow(
    candidate: CleanupCandidate,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenPath: ((String) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(candidate.coordinate, fontWeight = FontWeight.SemiBold)
                Text(candidate.reason, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(candidate.timeBasis.displayLabel(candidate.timeBasisFallback), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Badge(candidate.riskLevel.label, colorForRisk(candidate.riskLevel))
            Text(formatBytes(candidate.sizeBytes))
            if (onOpenPath != null) {
                OutlinedButton(
                    onClick = { candidate.path?.let(onOpenPath) },
                    enabled = candidate.path != null,
                ) { Text("查看路径") }
            }
        }
    }
}

@Composable
private fun EditablePathRow(
    path: DetectedPath,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
) {
    var value by remember(path.kind, path.path) { mutableStateOf(path.path) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(path.kind.label, fontWeight = FontWeight.SemiBold)
                Text(
                    if (path.autoDetected) "自动发现路径" else "自定义覆盖路径",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Badge(if (path.exists) "已存在" else "未找到", if (path.exists) MaterialTheme.semanticColors.safe else MaterialTheme.semanticColors.warn)
        }
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("扫描路径") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onSave(value.trim()) }) { Text("保存并重扫") }
            OutlinedButton(onClick = onReset, enabled = !path.autoDetected) { Text("恢复默认") }
        }
    }
}

@Composable
private fun CustomScanPathRow(
    entry: CustomScanPath,
    onToggleEnabled: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(entry.kind.label, fontWeight = FontWeight.SemiBold)
                Badge(if (entry.enabled) "已启用" else "已停用", if (entry.enabled) MaterialTheme.semanticColors.safe else MaterialTheme.semanticColors.warn)
            }
            Text(entry.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = entry.enabled, onCheckedChange = { onToggleEnabled() })
            OutlinedButton(onClick = onRemove) { Text("移除") }
        }
    }
}

@Composable
private fun PathRow(path: DetectedPath) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(path.kind.label, fontWeight = FontWeight.SemiBold)
            Text(path.path, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Badge(if (path.exists) "已存在" else "待扫描", colorForSource(path.kind))
    }
}

@Composable
private fun colorForSource(source: ArtifactSource): Color = when (source) {
    ArtifactSource.MAVEN -> MaterialTheme.semanticColors.maven
    ArtifactSource.GRADLE -> MaterialTheme.semanticColors.gradle
    ArtifactSource.WRAPPER -> MaterialTheme.semanticColors.wrapper
}

@Composable
private fun colorForSource(kind: PathKind): Color = colorForSource(kind.source)

@Composable
private fun colorForRisk(level: RiskLevel): Color = when (level) {
    RiskLevel.LOW -> MaterialTheme.semanticColors.safe
    RiskLevel.MEDIUM -> MaterialTheme.semanticColors.warn
    RiskLevel.HIGH -> MaterialTheme.semanticColors.danger
    RiskLevel.PROTECTED -> MaterialTheme.semanticColors.protect
}

private data class CleanupPriorityMetrics(
    val priorityScore: Int,
    val reclaimableBytes: Long,
    val staleCandidateCount: Int,
    val hasActionableCandidate: Boolean,
    val label: String,
)

private fun cleanupPriorityMetrics(
    entry: ArtifactScanEntry,
    candidatesByCoordinate: Map<String, List<CleanupCandidate>>,
): CleanupPriorityMetrics {
    val actionableCandidates = candidatesByCoordinate[entry.coordinate].orEmpty().filter(::isActionableCandidate)
    if (actionableCandidates.isEmpty()) {
        return CleanupPriorityMetrics(
            priorityScore = Int.MIN_VALUE,
            reclaimableBytes = 0L,
            staleCandidateCount = 0,
            hasActionableCandidate = false,
            label = "暂无可执行候选",
        )
    }

    val bestRiskRank = actionableCandidates.maxOf(::cleanabilityRank)
    val defaultSelectedCount = actionableCandidates.count { it.defaultSelected }
    val reclaimableBytes = actionableCandidates.sumOf { it.sizeBytes }
    val staleCandidateCount = actionableCandidates.size
    val priorityScore = defaultSelectedCount * 10_000 +
        bestRiskRank * 1_000 +
        staleCandidateCount * 100 +
        actionableCandidates.maxOf { (it.sizeBytes / (32L * 1024L * 1024L)).coerceAtMost(99L) }.toInt()

    val label = when {
        defaultSelectedCount > 0 && bestRiskRank >= cleanabilityRank(RiskLevel.LOW) -> "高：低风险高收益，适合优先处理"
        bestRiskRank >= cleanabilityRank(RiskLevel.MEDIUM) -> "中：建议复核后处理"
        else -> "低：高风险或收益有限"
    }

    return CleanupPriorityMetrics(
        priorityScore = priorityScore,
        reclaimableBytes = reclaimableBytes,
        staleCandidateCount = staleCandidateCount,
        hasActionableCandidate = true,
        label = label,
    )
}

private fun isActionableCandidate(candidate: CleanupCandidate): Boolean {
    return candidate.path != null && candidate.riskLevel != RiskLevel.PROTECTED
}

private fun cleanabilityRank(candidate: CleanupCandidate): Int = cleanabilityRank(candidate.riskLevel)

private fun cleanabilityRank(level: RiskLevel): Int = when (level) {
    RiskLevel.LOW -> 4
    RiskLevel.MEDIUM -> 3
    RiskLevel.HIGH -> 2
    RiskLevel.PROTECTED -> 1
}

private fun safeProgress(value: Long, total: Long): Float = if (total <= 0L) 0f else value.toFloat() / total.toFloat()
