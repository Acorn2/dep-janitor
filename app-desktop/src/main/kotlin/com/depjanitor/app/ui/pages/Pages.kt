package com.depjanitor.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.primaryRiskLevel
import com.depjanitor.core.model.versionCount
import com.depjanitor.core.model.RiskLevel

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
    SIZE("按大小"),
    LAST_MODIFIED("按最近修改"),
    VERSION_COUNT("按版本数"),
    NAME("按名称"),
}

@Composable
fun ArtifactAtlasPage(snapshot: DashboardSnapshot) {
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
    var sortMode by remember(entries) { mutableStateOf(ArtifactSortMode.SIZE) }

    val filteredEntries = remember(entries, query, selectedSource, selectedRisk, sortMode) {
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
                        }
                    }
                }
            }
        }
        PanelCard(title = "Version Detail", subtitle = "展示选中结果的版本层与大小信息。", modifier = Modifier.weight(1f)) {
            if (selected == null) {
                Text("暂无可展示的 artifact 结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(selected.coordinate, fontWeight = FontWeight.SemiBold)
                Text(formatBytes(selected.totalSizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("最近修改：${formatTimestamp(selected.lastModifiedMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                selected.versions.forEach { layer ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(layer.label)
                            Text(formatTimestamp(layer.lastModifiedMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatBytes(layer.sizeBytes))
                            layer.riskLevel?.let { Badge(it.label, colorForRisk(it)) } ?: Text(layer.state, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {}) { Text("查看路径") }
                Button(onClick = {}) { Text("加入本次方案") }
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
fun CleanupRecipesPage(snapshot: DashboardSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PanelCard(title = "Rule Snapshot", subtitle = "当前清理建议由默认规则集生成。") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricPill("保留版本", "${snapshot.ruleSet.retainLatestVersions}")
                MetricPill("未使用阈值", "${snapshot.ruleSet.unusedDaysThreshold} 天")
                MetricPill("优先低风险", if (snapshot.ruleSet.prioritizeLowRisk) "是" else "否")
                MetricPill("移动到回收站", if (snapshot.ruleSet.moveToTrash) "是" else "否")
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
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(formatBytes(candidate.sizeBytes))
                            Text(formatTimestamp(candidate.lastModifiedMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (candidate.defaultSelected) "默认加入方案" else "需人工确认",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (candidate.defaultSelected) MaterialTheme.semanticColors.safe else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulationPage(snapshot: DashboardSnapshot) {
    val readyCandidates = snapshot.candidates.filter { it.defaultSelected }
    val reviewCandidates = snapshot.candidates.filterNot { it.defaultSelected }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PanelCard(title = "Before", subtitle = "当前缓存结构", modifier = Modifier.weight(1f)) {
                MetricPill("总缓存", formatBytes(snapshot.totalBytes))
                MetricPill("总候选", snapshot.candidates.size.toString())
                MetricPill("保护项", snapshot.simulation.protectedRuleCount.toString())
            }
            PanelCard(title = "After", subtitle = "默认方案预演结果", modifier = Modifier.weight(1f)) {
                MetricPill("默认释放", formatBytes(snapshot.simulation.releasableBytes))
                MetricPill("已选项目", snapshot.simulation.selectedItemCount.toString())
                MetricPill("高风险复核", snapshot.simulation.highRiskCount.toString())
                OutlinedButton(onClick = {}) { Text("确认预演") }
                Button(onClick = {}) { Text("开始清理") }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PanelCard(title = "Ready Now", subtitle = "默认勾选的低风险候选。", modifier = Modifier.weight(1f)) {
                if (readyCandidates.isEmpty()) {
                    Text("当前没有自动加入方案的低风险项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    readyCandidates.take(8).forEach { candidate ->
                        CandidateRow(candidate)
                    }
                }
            }
            PanelCard(title = "Needs Review", subtitle = "需人工确认的中高风险项。", modifier = Modifier.weight(1f)) {
                if (reviewCandidates.isEmpty()) {
                    Text("当前没有待复核项。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    reviewCandidates.take(8).forEach { candidate ->
                        CandidateRow(candidate)
                    }
                }
            }
        }
    }
}

@Composable
fun RuleForgePage(
    snapshot: DashboardSnapshot,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPathOverrideChange: (PathKind, String) -> Unit,
    onPathOverrideReset: (PathKind) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        PanelCard(title = "Path Discovery", subtitle = "路径管理已支持自定义覆盖并参与下一次扫描。", modifier = Modifier.weight(1.2f)) {
            snapshot.detectedPaths.forEach { detectedPath ->
                EditablePathRow(
                    path = detectedPath,
                    onSave = { onPathOverrideChange(detectedPath.kind, it) },
                    onReset = { onPathOverrideReset(detectedPath.kind) },
                )
            }
        }
        PanelCard(title = "Rule Forge", subtitle = "当前先落地主题、默认规则和路径治理能力。", modifier = Modifier.weight(1f)) {
            MetricPill("保留最近版本数", snapshot.ruleSet.retainLatestVersions.toString())
            MetricPill("未使用阈值", "${snapshot.ruleSet.unusedDaysThreshold} 天")
            MetricPill("默认策略", if (snapshot.ruleSet.prioritizeLowRisk) "低风险优先" else "全部候选")
            MetricPill("回收站策略", if (snapshot.ruleSet.moveToTrash) "优先移入" else "直接删除")
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
private fun CandidateRow(candidate: com.depjanitor.core.model.CleanupCandidate) {
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


private fun safeProgress(value: Long, total: Long): Float = if (total <= 0L) 0f else value.toFloat() / total.toFloat()
