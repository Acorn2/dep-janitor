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
import androidx.compose.ui.draw.clip
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
fun ObservatoryPage(
    snapshot: DashboardSnapshot,
    isScanning: Boolean,
    statusText: String,
    onOpenPath: (String) -> Unit,
) {
    val wrapperBytes = remember(snapshot.wrapperDistributions) { snapshot.wrapperDistributions.sumOf { it.sizeBytes } }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OverviewStatCard(
                title = "总缓存",
                value = formatBytes(snapshot.totalBytes),
                note = "当前扫描到的本地依赖缓存总量",
                modifier = Modifier.weight(1f),
            )
            OverviewStatCard(
                title = "可释放空间",
                value = formatBytes(snapshot.simulation.releasableBytes),
                note = "基于当前规则推导的预计可释放空间",
                modifier = Modifier.weight(1f),
            )
            OverviewStatCard(
                title = "候选项",
                value = snapshot.candidates.size.toString(),
                note = "进入建议或执行删除页后可进一步确认",
                modifier = Modifier.weight(1f),
            )
            OverviewStatCard(
                title = "保护项",
                value = snapshot.protectedCoordinates.size.toString(),
                note = "命中白名单或项目保护规则的依赖",
                modifier = Modifier.weight(1f),
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(
                title = "扫描状态",
                subtitle = "让用户先看懂现在处于哪个阶段。",
                modifier = Modifier.weight(0.95f),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Badge(
                        if (isScanning) "正在扫描" else "扫描完成",
                        if (isScanning) MaterialTheme.semanticColors.warn else MaterialTheme.semanticColors.safe,
                    )
                    Badge(
                        "有效路径 ${snapshot.detectedPaths.count { it.exists }}",
                        MaterialTheme.semanticColors.protect,
                    )
                }
                Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (snapshot.scannedAtMillis > 0L) {
                    Text(
                        "最近扫描：${formatTimestamp(snapshot.scannedAtMillis)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        "当前展示的是预览态，请先点击“开始扫描”获取真实结果。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("Maven", formatBytes(snapshot.mavenBytes))
                    MetricPill("Gradle", formatBytes(snapshot.gradleBytes))
                    MetricPill("Wrapper", formatBytes(wrapperBytes))
                }
            }
            PanelCard(
                title = "已发现路径",
                subtitle = "这些目录会纳入当前扫描范围。",
                modifier = Modifier.weight(1.05f),
            ) {
                snapshot.detectedPaths.forEach { path ->
                    PathRow(path)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(
                title = "清理摘要",
                subtitle = "把复杂分析结果收敛成用户更容易理解的结论。",
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (snapshot.simulation.releasableBytes > 0L) {
                        "当前规则下预计最多可释放 ${formatBytes(snapshot.simulation.releasableBytes)}，建议先进入“清理建议”或“执行删除”继续确认。"
                    } else {
                        "当前还没有足够的候选项可释放，建议检查路径配置或调整清理规则。"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("低风险", snapshot.lowRiskCount.toString())
                    MetricPill("中风险", snapshot.mediumRiskCount.toString())
                    MetricPill("高风险", snapshot.highRiskCount.toString())
                }
                val selectedSummary = listOf(
                    "默认方案项 ${snapshot.simulation.selectedItemCount}",
                    "保护规则 ${snapshot.simulation.protectedRuleCount}",
                    "热点目录 ${snapshot.hotspots.size}",
                )
                selectedSummary.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PanelCard(
                title = "来源分布",
                subtitle = "帮助区分空间主要来自 Maven 还是 Gradle。",
                modifier = Modifier.weight(1f),
            ) {
                GradientBar(
                    "Maven 缓存",
                    formatBytes(snapshot.mavenBytes),
                    safeProgress(snapshot.mavenBytes, snapshot.totalBytes),
                    listOf(MaterialTheme.semanticColors.maven.copy(alpha = 0.35f), MaterialTheme.semanticColors.maven),
                )
                GradientBar(
                    "Gradle 缓存",
                    formatBytes(snapshot.gradleBytes),
                    safeProgress(snapshot.gradleBytes, snapshot.totalBytes),
                    listOf(MaterialTheme.semanticColors.gradle.copy(alpha = 0.35f), MaterialTheme.semanticColors.gradle),
                )
                GradientBar(
                    "Wrapper 缓存",
                    formatBytes(wrapperBytes),
                    safeProgress(wrapperBytes, snapshot.totalBytes),
                    listOf(MaterialTheme.semanticColors.wrapper.copy(alpha = 0.35f), MaterialTheme.semanticColors.wrapper),
                )
            }
        }

        PanelCard(
            title = "风险等级说明",
            subtitle = "看到低 / 中 / 高 / 已保护时，能立刻理解代表什么。",
        ) {
            RiskLegendRow(
                level = RiskLevel.LOW,
                title = "低风险",
                description = "通常是过期 SNAPSHOT、下载残留、长期未更新的旧版本或低风险 Gradle 残留，适合优先纳入批量清理。",
            )
            RiskLegendRow(
                level = RiskLevel.MEDIUM,
                title = "中风险",
                description = "系统认为可以清理，但近期可能仍有用途，建议结合项目情况人工复核后再删除。",
            )
            RiskLegendRow(
                level = RiskLevel.HIGH,
                title = "高风险",
                description = "通常是旧 Wrapper、fileHashes、kotlin-dsl 等影响较大的缓存，不会默认加入方案，需谨慎处理。",
            )
            RiskLegendRow(
                level = RiskLevel.PROTECTED,
                title = "已保护",
                description = "命中白名单或项目保护规则，不会进入最终删除计划。",
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(
                title = "热点目录",
                subtitle = "优先关注体积最大的缓存热点。",
                modifier = Modifier.weight(1f),
            ) {
                val maxHotspot = snapshot.hotspots.maxOfOrNull { it.sizeBytes }?.takeIf { it > 0 } ?: 1L
                if (snapshot.hotspots.isEmpty()) {
                    Text("当前没有可展示的热点目录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    snapshot.hotspots.take(6).forEach { hotspot ->
                        val colors = when (hotspot.source) {
                            ArtifactSource.MAVEN -> listOf(MaterialTheme.semanticColors.maven.copy(alpha = 0.35f), MaterialTheme.semanticColors.maven)
                            ArtifactSource.GRADLE -> listOf(MaterialTheme.semanticColors.gradle.copy(alpha = 0.35f), MaterialTheme.semanticColors.gradle)
                            ArtifactSource.WRAPPER -> listOf(MaterialTheme.semanticColors.wrapper.copy(alpha = 0.35f), MaterialTheme.semanticColors.wrapper)
                        }
                        HotspotRow(
                            hotspotName = hotspot.name,
                            sizeText = formatBytes(hotspot.sizeBytes),
                            progress = hotspot.sizeBytes.toFloat() / maxHotspot,
                            colors = colors,
                            clickable = !hotspot.path.isNullOrBlank(),
                            onClick = { hotspot.path?.let(onOpenPath) },
                        )
                    }
                }
            }
            PanelCard(
                title = "重复版本",
                subtitle = "版本越多，越值得进入清理建议继续看。",
                modifier = Modifier.weight(1f),
            ) {
                if (snapshot.duplicateVersionRankings.isEmpty()) {
                    Text("当前没有发现明显的重复版本热点。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    snapshot.duplicateVersionRankings.take(6).forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.coordinate, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${item.versionCount} 个版本 · ${item.staleVersionCount} 个旧版本",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(formatBytes(item.totalSizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStatCard(
    title: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier,
) {
    PanelCard(title = title, subtitle = note, modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HotspotRow(
    hotspotName: String,
    sizeText: String,
    progress: Float,
    colors: List<Color>,
    clickable: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (clickable) 0.10f else 0f))
            .clickable(enabled = clickable, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(hotspotName)
                Text(
                    if (clickable) "点击可打开对应目录" else "当前目录不可直接打开",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(sizeText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f), RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(Brush.horizontalGradient(colors), RoundedCornerShape(999.dp))
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun RiskLegendRow(
    level: RiskLevel,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Badge(title, colorForRisk(level))
        Text(
            description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    val candidatesByCoordinate = remember(snapshot.candidates) { snapshot.candidates.groupBy { it.coordinate } }
    val displayRiskByCoordinate = remember(entries, candidatesByCoordinate) {
        entries.associate { entry ->
            entry.coordinate to artifactDisplayRisk(entry.coordinate, candidatesByCoordinate)
        }
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
            .filter { entry -> selectedRisk == null || displayRiskByCoordinate[entry.coordinate] == selectedRisk }
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PanelCard(
            title = "依赖图谱使用说明",
            subtitle = "这里不再做复杂图形展示，而是用“列表 + 详情”帮助你快速定位值得处理的依赖。",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricPill("结果总数", filteredEntries.size.toString())
                MetricPill("Maven", entries.count { it.source == ArtifactSource.MAVEN }.toString())
                MetricPill("Gradle", entries.count { it.source == ArtifactSource.GRADLE }.toString())
                MetricPill("Wrapper", entries.count { it.source == ArtifactSource.WRAPPER }.toString())
            }
            Text(
                "建议先用搜索和筛选缩小范围，再查看右侧详情。确认某个依赖确实适合处理后，再把相关候选加入本次方案。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PanelCard(
            title = "当前删除规则",
            subtitle = "告诉你批量删除时，系统到底会删什么、保留什么。",
        ) {
            Text(
                "Maven 依赖默认保留最近 ${snapshot.ruleSet.retainLatestVersions} 个版本；超出部分才会进入候选。若旧版本同时超过 ${snapshot.ruleSet.unusedDaysThreshold} 天未更新，会优先列为低风险建议。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Gradle 缓存会按目录类型划分风险；低风险残留可能默认加入方案，高风险缓存需要人工确认。Wrapper 默认只保留最新的一份，旧版不会自动勾选。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(
                title = "筛选与结果列表",
                subtitle = "先找目标，再看详情。",
                modifier = Modifier.weight(1.15f),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索坐标 / group / artifact") },
                    singleLine = true,
                )
                Text("来源", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterBadge("全部", selectedSource == null) { selectedSource = null }
                    FilterBadge("Maven", selectedSource == ArtifactSource.MAVEN) { selectedSource = ArtifactSource.MAVEN }
                    FilterBadge("Gradle", selectedSource == ArtifactSource.GRADLE) { selectedSource = ArtifactSource.GRADLE }
                    FilterBadge("Wrapper", selectedSource == ArtifactSource.WRAPPER) { selectedSource = ArtifactSource.WRAPPER }
                }
                Text("风险", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterBadge("全部", selectedRisk == null) { selectedRisk = null }
                    FilterBadge("低", selectedRisk == RiskLevel.LOW) { selectedRisk = RiskLevel.LOW }
                    FilterBadge("中", selectedRisk == RiskLevel.MEDIUM) { selectedRisk = RiskLevel.MEDIUM }
                    FilterBadge("高", selectedRisk == RiskLevel.HIGH) { selectedRisk = RiskLevel.HIGH }
                }
                Text("排序方式", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ArtifactSortMode.entries.forEach { mode ->
                        FilterBadge(mode.label, sortMode == mode) { sortMode = mode }
                    }
                }
                Text("共 ${filteredEntries.size} 条结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(460.dp)) {
                    items(filteredEntries) { entry ->
                        val metrics = priorityByCoordinate[entry.coordinate] ?: cleanupPriorityMetrics(entry, candidatesByCoordinate)
                        val displayRisk = displayRiskByCoordinate[entry.coordinate]
                        val selectedRow = entry.coordinate == selectedCoordinate
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selectedRow) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable { selectedCoordinate = entry.coordinate }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.coordinate, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(formatBytes(entry.totalSizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Badge(entry.source.label, colorForSource(entry.source))
                                if (displayRisk != null) {
                                    Badge(displayRisk.label, colorForRisk(displayRisk))
                                } else {
                                    Badge("无候选", MaterialTheme.semanticColors.protect)
                                }
                                if (metrics.hasActionableCandidate) {
                                    Badge(metrics.label.substringBefore('：'), MaterialTheme.semanticColors.protect)
                                }
                            }
                            Text(
                                "${entry.versionCount} 个版本 · ${formatTimestamp(entry.lastModifiedMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            PanelCard(
                title = "依赖详情",
                subtitle = "选中一条结果后，在这里查看版本与可操作项。",
                modifier = Modifier.weight(0.85f),
            ) {
                if (selected == null) {
                    Text("当前没有可展示的依赖结果。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val priorityMetrics = priorityByCoordinate[selected.coordinate] ?: cleanupPriorityMetrics(selected, candidatesByCoordinate)
                    val displayRisk = displayRiskByCoordinate[selected.coordinate]
                    val keptVersions = selected.versions.take(snapshot.ruleSet.retainLatestVersions).map { it.label }
                    val candidateVersionLabels = relatedCandidates.mapNotNull { it.versionLabel }.distinct()
                    Text(selected.coordinate, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge(selected.source.label, colorForSource(selected.source))
                        if (displayRisk != null) {
                            Badge(displayRisk.label, colorForRisk(displayRisk))
                        } else {
                            Badge("无候选", MaterialTheme.semanticColors.protect)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill("总大小", formatBytes(selected.totalSizeBytes))
                        MetricPill("版本数", selected.versionCount.toString())
                        MetricPill("候选项", relatedCandidateIds.size.toString())
                    }
                    Text("最近参考时间：${formatTimestamp(selected.lastModifiedMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (priorityMetrics.hasActionableCandidate) priorityMetrics.label else "当前没有可直接执行的候选项。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "已加入方案 ${selectedRelatedIds.size} 项 · 预计释放 ${formatBytes(relatedReclaimableBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when (selected.source) {
                        ArtifactSource.MAVEN -> {
                            Text(
                                "保留版本：${if (keptVersions.isEmpty()) "无" else keptVersions.joinToString("、")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "候选删除版本：${if (candidateVersionLabels.isEmpty()) "当前没有" else candidateVersionLabels.joinToString("、")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ArtifactSource.WRAPPER -> {
                            Text(
                                "Wrapper 规则：默认保留最新的一份 distribution，旧版仅列为高风险候选，不会自动加入方案。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ArtifactSource.GRADLE -> {
                            Text(
                                "Gradle 规则：按目录类型区分风险。transforms / build-cache 更偏低风险，modules / jars 需复核，fileHashes / kotlin-dsl 不建议直接批量清理。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    selected.versions.take(8).forEach { layer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(layer.label)
                                Text(
                                    buildTimeReferenceText(layer.lastModifiedMillis, layer.timeBasis.displayLabel(layer.timeBasisFallback)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(formatBytes(layer.sizeBytes))
                                layer.riskLevel?.let { Badge(it.label, colorForRisk(it)) }
                            }
                        }
                    }
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
                            if (allRelatedSelected) onRemoveCandidatesFromPlan(relatedCandidateIds)
                            else onAddCandidatesToPlan(relatedCandidateIds)
                        },
                        enabled = relatedCandidateIds.isNotEmpty(),
                    ) {
                        Text(
                            when {
                                relatedCandidateIds.isEmpty() -> "没有可加入项"
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
    onAddCandidatesToPlan: (Set<String>) -> Unit,
    onRemoveCandidatesFromPlan: (Set<String>) -> Unit,
    onClearPlanSelection: () -> Unit,
    onAddWhitelistEntry: (WhitelistEntry) -> Unit,
    onOpenPath: (String) -> Unit,
) {
    val actionableCandidates = snapshot.candidates.filter { it.path != null && it.riskLevel != RiskLevel.PROTECTED }
    var query by remember(actionableCandidates) { mutableStateOf("") }
    val filteredCandidates = remember(actionableCandidates, query) {
        actionableCandidates.filter { candidate ->
            query.isBlank() ||
                candidate.coordinate.contains(query, ignoreCase = true) ||
                candidate.versionLabel.orEmpty().contains(query, ignoreCase = true) ||
                candidate.reason.contains(query, ignoreCase = true)
        }
    }
    val recommendedCandidates = filteredCandidates.filter { it.defaultSelected }
    val reviewCandidates = filteredCandidates.filterNot { it.defaultSelected }
    val allRecommendedIds = remember(filteredCandidates) { filteredCandidates.filter { it.defaultSelected }.map { it.id }.toSet() }
    val allReviewIds = remember(filteredCandidates) { filteredCandidates.filterNot { it.defaultSelected }.map { it.id }.toSet() }
    val selectedRecommendedIds = remember(selectedCandidateIds, allRecommendedIds) { selectedCandidateIds.intersect(allRecommendedIds) }
    val selectedReviewIds = remember(selectedCandidateIds, allReviewIds) { selectedCandidateIds.intersect(allReviewIds) }
    val selectedCandidates = remember(filteredCandidates, selectedCandidateIds) { filteredCandidates.filter { it.id in selectedCandidateIds } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PanelCard(
            title = "清理建议使用说明",
            subtitle = "先看系统建议，再决定哪些项目加入本次方案。",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricPill("建议总数", snapshot.candidates.size.toString())
                MetricPill("默认建议", recommendedCandidates.size.toString())
                MetricPill("待复核", reviewCandidates.size.toString())
                MetricPill("已加入方案", selectedCandidateIds.size.toString())
            }
            Text(
                "默认建议通常是低风险、收益更明确的项目；待复核项需要你结合项目情况再决定是否加入。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索坐标 / 版本 / 原因") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onAddCandidatesToPlan(allRecommendedIds) },
                    enabled = allRecommendedIds.isNotEmpty() && selectedRecommendedIds.size < allRecommendedIds.size,
                ) { Text("加入全部默认建议") }
                OutlinedButton(
                    onClick = { onRemoveCandidatesFromPlan(allRecommendedIds) },
                    enabled = selectedRecommendedIds.isNotEmpty(),
                ) { Text("移出默认建议") }
                OutlinedButton(
                    onClick = onClearPlanSelection,
                    enabled = selectedCandidateIds.isNotEmpty(),
                ) { Text("清空已选方案") }
            }
        }

        PanelCard(
            title = "批量删除如何划分",
            subtitle = "这部分说明真正的批量删除规则，不用靠猜。",
        ) {
            Text(
                "1. Maven：默认保留最近 ${snapshot.ruleSet.retainLatestVersions} 个版本；超出部分才会进入候选。超出且超过 ${snapshot.ruleSet.unusedDaysThreshold} 天未更新的旧版本，会优先作为低风险默认建议。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "2. SNAPSHOT：过期 SNAPSHOT 会优先列为低风险建议。3. .lastUpdated、失败下载残留、Gradle transforms/build-cache 残留通常可批量清理。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "3. Gradle modules/jars 以及旧 Wrapper 更偏复核型候选；fileHashes、kotlin-dsl 这类高风险缓存不会自动加入方案。4. 白名单和保护路径命中的内容不会执行删除。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PanelCard(
            title = "风险等级说明",
            subtitle = "清理建议页里的风险标签，对应的是这套解释。",
        ) {
            RiskLegendRow(
                level = RiskLevel.LOW,
                title = "低风险",
                description = "更适合批量处理，很多会直接进入默认推荐方案。",
            )
            RiskLegendRow(
                level = RiskLevel.MEDIUM,
                title = "中风险",
                description = "可删但不建议直接批量执行，最好先看依赖详情和项目实际使用情况。",
            )
            RiskLegendRow(
                level = RiskLevel.HIGH,
                title = "高风险",
                description = "不会自动加入方案，删除前应重点确认影响。",
            )
            RiskLegendRow(
                level = RiskLevel.PROTECTED,
                title = "已保护",
                description = "命中白名单或保护规则，系统不会执行删除。",
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(title = "当前规则", subtitle = "这些规则决定了本页建议如何生成。", modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("保留版本", snapshot.ruleSet.retainLatestVersions.toString())
                    MetricPill("未使用阈值", "${snapshot.ruleSet.unusedDaysThreshold} 天")
                    MetricPill("低风险优先", if (snapshot.ruleSet.prioritizeLowRisk) "是" else "否")
                    MetricPill("回收站", if (snapshot.ruleSet.moveToTrash) "是" else "否")
                }
                Text("白名单 ${snapshot.whitelistEntries.size} 项，命中保护坐标 ${snapshot.protectedCoordinates.size} 项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PanelCard(title = "规则命中摘要", subtitle = "用于快速理解为什么会出现这些建议。", modifier = Modifier.weight(1f)) {
                if (snapshot.recipes.isEmpty()) {
                    Text("当前没有可展示的建议分类。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    snapshot.recipes.forEach { recipe ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(recipe.title, fontWeight = FontWeight.SemiBold)
                                Text(recipe.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Badge(recipe.riskLevel.label, colorForRisk(recipe.riskLevel))
                                Text("${recipe.hitCount} 项 · ${formatBytes(recipe.releasableBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(title = "默认推荐", subtitle = "优先处理这里的项目。", modifier = Modifier.weight(1f)) {
                if (recommendedCandidates.isEmpty()) {
                    Text("当前没有默认推荐项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill("当前结果", recommendedCandidates.size.toString())
                        MetricPill("已选", selectedRecommendedIds.size.toString())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onAddCandidatesToPlan(allRecommendedIds) },
                            enabled = allRecommendedIds.isNotEmpty() && selectedRecommendedIds.size < allRecommendedIds.size,
                        ) { Text("全部加入") }
                        OutlinedButton(
                            onClick = { onRemoveCandidatesFromPlan(allRecommendedIds) },
                            enabled = selectedRecommendedIds.isNotEmpty(),
                        ) { Text("全部移出") }
                    }
                    LazyColumn(
                        modifier = Modifier.height(640.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(recommendedCandidates, key = { it.id }) { candidate ->
                            CompactCandidateActionRow(
                                candidate = candidate,
                                selected = candidate.id in selectedCandidateIds,
                                onToggleCandidateInPlan = onToggleCandidateInPlan,
                                onAddWhitelistEntry = onAddWhitelistEntry,
                                onOpenPath = onOpenPath,
                            )
                        }
                    }
                }
            }
            PanelCard(title = "待人工确认", subtitle = "这些项目建议你再看一眼。", modifier = Modifier.weight(1f)) {
                if (reviewCandidates.isEmpty()) {
                    Text("当前没有待人工确认项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill("当前结果", reviewCandidates.size.toString())
                        MetricPill("已选", selectedReviewIds.size.toString())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onAddCandidatesToPlan(allReviewIds) },
                            enabled = allReviewIds.isNotEmpty() && selectedReviewIds.size < allReviewIds.size,
                        ) { Text("全部加入") }
                        OutlinedButton(
                            onClick = { onRemoveCandidatesFromPlan(allReviewIds) },
                            enabled = selectedReviewIds.isNotEmpty(),
                        ) { Text("全部移出") }
                    }
                    LazyColumn(
                        modifier = Modifier.height(640.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(reviewCandidates, key = { it.id }) { candidate ->
                            CompactCandidateActionRow(
                                candidate = candidate,
                                selected = candidate.id in selectedCandidateIds,
                                onToggleCandidateInPlan = onToggleCandidateInPlan,
                                onAddWhitelistEntry = onAddWhitelistEntry,
                                onOpenPath = onOpenPath,
                            )
                        }
                    }
                }
            }
        }

        if (selectedCandidates.isNotEmpty()) {
            PanelCard(
                title = "本次已选方案",
                subtitle = "这里展示你已经选中的项目，便于马上进入“执行删除”发起真实删除。",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("已选数量", selectedCandidates.size.toString())
                    MetricPill("预计释放", formatBytes(selectedCandidates.sumOf { it.sizeBytes }))
                }
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(selectedCandidates, key = { it.id }) { candidate ->
                        CompactCandidateActionRow(
                            candidate = candidate,
                            selected = true,
                            onToggleCandidateInPlan = onToggleCandidateInPlan,
                            onAddWhitelistEntry = onAddWhitelistEntry,
                            onOpenPath = onOpenPath,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCandidateActionRow(
    candidate: CleanupCandidate,
    selected: Boolean,
    onToggleCandidateInPlan: (String) -> Unit,
    onAddWhitelistEntry: (WhitelistEntry) -> Unit,
    onOpenPath: (String) -> Unit,
) {
    val actionable = candidate.path != null && candidate.riskLevel != RiskLevel.PROTECTED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(candidate.coordinate, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(formatBytes(candidate.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        candidate.versionLabel?.let {
            Text("版本：$it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Badge(candidate.kind.label, colorForSource(candidate.source))
            Badge(candidate.riskLevel.label, colorForRisk(candidate.riskLevel))
            Badge(if (candidate.defaultSelected) "默认建议" else "待复核", if (candidate.defaultSelected) MaterialTheme.semanticColors.safe else MaterialTheme.semanticColors.warn)
        }
        Text(candidate.reason, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(
            buildTimeReferenceText(candidate.lastModifiedMillis, candidate.timeBasis.displayLabel(candidate.timeBasisFallback)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    onAddWhitelistEntry(
                        candidate.path?.let(WhitelistEntry::path) ?: WhitelistEntry.coordinate(candidate.coordinate),
                    )
                },
            ) { Text("白名单") }
            OutlinedButton(onClick = { candidate.path?.let(onOpenPath) }, enabled = candidate.path != null) { Text("查看路径") }
            Button(onClick = { onToggleCandidateInPlan(candidate.id) }, enabled = actionable) {
                Text(
                    when {
                        !actionable -> "不可加入"
                        selected -> "移出方案"
                        else -> "加入方案"
                    },
                )
            }
        }
    }
}

@Composable
fun CleanupExecutionPage(
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
    var executionMode by remember(snapshot.ruleSet.moveToTrash) {
        mutableStateOf(if (snapshot.ruleSet.moveToTrash) CleanupExecutionMode.MOVE_TO_TRASH else CleanupExecutionMode.DELETE_DIRECTLY)
    }
    var highRiskConfirmed by remember(selectedCandidateIds) { mutableStateOf(false) }
    var directDeleteConfirmed by remember(selectedCandidateIds, executionMode) { mutableStateOf(false) }
    val selectedCandidates = actionableCandidates.filter { it.id in selectedCandidateIds }
    val readyCandidates = actionableCandidates.filter { it.defaultSelected }
    val reviewCandidates = actionableCandidates.filterNot { it.defaultSelected }
    val selectedHighRiskCount = selectedCandidates.count { it.riskLevel == RiskLevel.HIGH }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PanelCard(
            title = "执行删除",
            subtitle = "这里不是预演，而是真正执行删除。测试阶段你可以自行勾选项目并发起删除。",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricPill("已选项目", selectedCandidateIds.size.toString())
                MetricPill("预计释放", formatBytes(selectedCandidates.sumOf { it.sizeBytes }))
                MetricPill("高风险", selectedHighRiskCount.toString())
                MetricPill("执行方式", executionMode.label)
            }
            Text(
                "你可以在本页直接勾选、取消勾选，并选择“移入回收站”或“直接删除”。点击按钮后会真的执行删除。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterBadge("移入回收站", executionMode == CleanupExecutionMode.MOVE_TO_TRASH) {
                    executionMode = CleanupExecutionMode.MOVE_TO_TRASH
                }
                FilterBadge("直接删除", executionMode == CleanupExecutionMode.DELETE_DIRECTLY) {
                    executionMode = CleanupExecutionMode.DELETE_DIRECTLY
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(title = "当前扫描情况", subtitle = "删除前先看一下整体范围。", modifier = Modifier.weight(1f)) {
                MetricPill("总缓存", formatBytes(snapshot.totalBytes))
                MetricPill("总候选", snapshot.candidates.size.toString())
                MetricPill("保护项", snapshot.simulation.protectedRuleCount.toString())
                Text("白名单与项目保护命中的依赖不会进入最终执行计划。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PanelCard(title = "本次删除结果预估", subtitle = "这里显示当前已选项目会带来的影响。", modifier = Modifier.weight(1f)) {
                MetricPill("预计释放", formatBytes(selectedCandidates.sumOf { it.sizeBytes }))
                MetricPill("默认推荐", readyCandidates.size.toString())
                MetricPill("待复核", reviewCandidates.size.toString())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onResetPlanSelection) { Text("恢复默认选择") }
                    OutlinedButton(onClick = onClearPlanSelection, enabled = selectedCandidateIds.isNotEmpty()) { Text("清空已选") }
                }
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = selectedCandidateIds.isNotEmpty() && !isCleaning,
                ) { Text(if (isCleaning) "删除中…" else "执行删除") }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(title = "建议直接执行", subtitle = "默认勾选的低风险项。", modifier = Modifier.weight(1f)) {
                if (readyCandidates.isEmpty()) {
                    Text("当前没有默认可执行项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("共 ${readyCandidates.size} 项，可滚动查看。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(
                        modifier = Modifier.height(520.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(readyCandidates, key = { it.id }) { candidate ->
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
            PanelCard(title = "建议人工复核", subtitle = "中高风险项请再次确认。", modifier = Modifier.weight(1f)) {
                if (reviewCandidates.isEmpty()) {
                    Text("当前没有待复核项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("共 ${reviewCandidates.size} 项，可滚动查看。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(
                        modifier = Modifier.height(520.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(reviewCandidates, key = { it.id }) { candidate ->
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
        }

        if (selectedCandidates.isNotEmpty()) {
            PanelCard(title = "本次将删除的项目", subtitle = "真正删除前，先在这里确认最终名单。") {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(selectedCandidates, key = { it.id }) { candidate ->
                        CandidateSelectionRow(
                            candidate = candidate,
                            checked = true,
                            onCheckedChange = { onToggleCandidateInPlan(candidate.id) },
                            onOpenPath = onOpenPath,
                        )
                    }
                }
            }
        }

        lastCleanupResult?.let { result ->
            PanelCard(title = "最近一次执行结果", subtitle = "如果你刚执行过清理，结果会显示在这里。") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("成功", result.successCount.toString())
                    MetricPill("失败", result.failureCount.toString())
                    MetricPill("跳过", result.skippedCount.toString())
                    MetricPill("释放空间", formatBytes(result.releasedBytes))
                }
                result.entries.take(10).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            title = { Text("确认执行删除") },
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
                        onExecuteCleanup(selectedCandidateIds, executionMode)
                    },
                    enabled = (selectedHighRiskCount == 0 || highRiskConfirmed) &&
                        (executionMode != CleanupExecutionMode.DELETE_DIRECTLY || directDeleteConfirmed),
                ) { Text("确认删除") }
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

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PanelCard(
            title = "设置说明",
            subtitle = "设置页只处理路径、规则和保护策略，不打断主流程。",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricPill("当前主题", themeMode.label)
                MetricPill("附加路径", customScanPaths.size.toString())
                MetricPill("白名单", snapshot.whitelistEntries.size.toString())
                MetricPill("保护项目", projectProtectionPaths.size.toString())
            }
            Text(
                "如果你只是想完成一次扫描与查看结果，通常不需要频繁进入设置页。只有在路径识别不准确、规则需要调整或需要保护某些依赖时，再来这里修改。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(
                title = "扫描路径",
                subtitle = "修改默认路径，或补充额外的扫描目录。",
                modifier = Modifier.weight(1.05f),
            ) {
                defaultPathRows.forEach { detectedPath ->
                    EditablePathRow(
                        path = detectedPath,
                        onSave = { onPathOverrideChange(detectedPath.kind, it) },
                        onReset = { onPathOverrideReset(detectedPath.kind) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("新增自定义扫描路径", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PathKind.entries.forEach { kind ->
                        FilterBadge(kind.label, newCustomPathKind == kind) { newCustomPathKind = kind }
                    }
                }
                OutlinedTextField(
                    value = newCustomPath,
                    onValueChange = { newCustomPath = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("输入要补充的扫描目录") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            onAddCustomScanPath(CustomScanPath(kind = newCustomPathKind, path = newCustomPath.trim()))
                            newCustomPath = ""
                        },
                        enabled = newCustomPath.isNotBlank(),
                    ) { Text("添加路径") }
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
                ToggleRow("扫描时包含自定义路径", scanCustomPaths) { onScanCustomPathsChange(it) }
            }

            PanelCard(
                title = "清理规则",
                subtitle = "控制保留版本数、未使用阈值和删除策略。",
                modifier = Modifier.weight(0.95f),
            ) {
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
                ToggleRow("低风险优先", prioritizeLowRisk) { prioritizeLowRisk = it }
                ToggleRow("优先移入回收站", moveToTrash) { moveToTrash = it }
                ToggleRow("清理 .lastUpdated", deleteLastUpdated) { deleteLastUpdated = it }
                ToggleRow("清理失败下载残留", deleteFailedDownloads) { deleteFailedDownloads = it }
                ToggleRow("清理过期 SNAPSHOT", deleteStaleSnapshots) { deleteStaleSnapshots = it }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onRuleSetChange(nextRuleSet) }) { Text("保存规则") }
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
                    ) { Text("恢复默认") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricPill("当前保留数", snapshot.ruleSet.retainLatestVersions.toString())
                    MetricPill("当前阈值", "${snapshot.ruleSet.unusedDaysThreshold} 天")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PanelCard(
                title = "白名单与项目保护",
                subtitle = "这些项不会进入清理计划，适合保护常用依赖或项目引用。",
                modifier = Modifier.weight(1f),
            ) {
                Text("白名单", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(
                    "为常用项目目录建立保护后，命中的依赖会优先标记为保护项。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    MetricPill("命中坐标", snapshot.protectedCoordinates.size.toString())
                }
                if (projectProtectionPaths.isEmpty()) {
                    Text("当前没有配置项目保护路径。", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }

            PanelCard(
                title = "显示与外观",
                subtitle = "支持浅色与深色两种主题。",
                modifier = Modifier.weight(0.8f),
            ) {
                Text("当前主题：${themeMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterBadge("深色", themeMode == ThemeMode.Obsidian) { onThemeModeChange(ThemeMode.Obsidian) }
                    FilterBadge("浅色", themeMode == ThemeMode.Ivory) { onThemeModeChange(ThemeMode.Ivory) }
                }
                Text(
                    "深色更适合长时间查看扫描结果；浅色更适合白天办公环境。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                candidate.versionLabel?.let {
                    Text("版本：$it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text(candidate.reason, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    buildTimeReferenceText(candidate.lastModifiedMillis, candidate.timeBasis.displayLabel(candidate.timeBasisFallback)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
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

private fun artifactDisplayRisk(
    coordinate: String,
    candidatesByCoordinate: Map<String, List<CleanupCandidate>>,
): RiskLevel? {
    val actionableCandidates = candidatesByCoordinate[coordinate].orEmpty().filter(::isActionableCandidate)
    return when {
        actionableCandidates.any { it.riskLevel == RiskLevel.HIGH } -> RiskLevel.HIGH
        actionableCandidates.any { it.riskLevel == RiskLevel.MEDIUM } -> RiskLevel.MEDIUM
        actionableCandidates.any { it.riskLevel == RiskLevel.LOW } -> RiskLevel.LOW
        else -> null
    }
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

private fun buildTimeReferenceText(millis: Long, basisLabel: String): String {
    val formatted = formatTimestamp(millis)
    return if (formatted == "未知") basisLabel else "$basisLabel：$formatted"
}

private fun safeProgress(value: Long, total: Long): Float = if (total <= 0L) 0f else value.toFloat() / total.toFloat()
