package com.depjanitor.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.depjanitor.app.ui.pages.ArtifactAtlasPage
import com.depjanitor.app.ui.pages.CleanupRecipesPage
import com.depjanitor.app.ui.pages.ObservatoryPage
import com.depjanitor.app.ui.pages.RuleForgePage
import com.depjanitor.app.ui.pages.SimulationPage
import com.depjanitor.app.ui.theme.ThemeMode
import com.depjanitor.app.ui.theme.semanticColors
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.PathKind

private enum class AppDestination(
    val label: String,
    val title: String,
    val description: String,
) {
    Observatory(
        label = "Observatory",
        title = "本机依赖结构总览",
        description = "先分析，再清理。当前阶段已接入真实路径发现与基础扫描。",
    ),
    ArtifactAtlas(
        label = "Artifact Atlas",
        title = "扫描结果 · 依赖图谱",
        description = "展示 artifact 聚合、来源过滤与版本地层视图的基础骨架。",
    ),
    CleanupRecipes(
        label = "Cleanup Recipes",
        title = "清理建议 · 策略卡",
        description = "把零散候选项转成规则化治理策略包。",
    ),
    Simulation(
        label = "Simulation Chamber",
        title = "预演舱 · Before / After",
        description = "用预演替代盲删，是 V1 的核心安全体验。",
    ),
    RuleForge(
        label = "Rule Forge",
        title = "设置 · 规则工坊",
        description = "路径、规则、白名单和双主题偏好都沉淀在这里。",
    ),
}

@Composable
fun AppShell(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    snapshot: DashboardSnapshot,
    isScanning: Boolean,
    statusText: String,
    onScan: () -> Unit,
    onPathOverrideChange: (PathKind, String) -> Unit,
    onPathOverrideReset: (PathKind) -> Unit,
) {
    var destination by remember { mutableStateOf(AppDestination.Observatory) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
        ) {
            Sidebar(destination = destination, onSelect = { destination = it })
            Spacer(Modifier.width(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TopBar(
                    destination = destination,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    isScanning = isScanning,
                    statusText = statusText,
                    onScan = onScan,
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (destination) {
                        AppDestination.Observatory -> ObservatoryPage(snapshot, isScanning, statusText)
                        AppDestination.ArtifactAtlas -> ArtifactAtlasPage(snapshot)
                        AppDestination.CleanupRecipes -> CleanupRecipesPage(snapshot)
                        AppDestination.Simulation -> SimulationPage(snapshot)
                        AppDestination.RuleForge -> RuleForgePage(
                            snapshot = snapshot,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            onPathOverrideChange = onPathOverrideChange,
                            onPathOverrideReset = onPathOverrideReset,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    destination: AppDestination,
    onSelect: (AppDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f), RoundedCornerShape(22.dp))
                .padding(16.dp),
        ) {
            Text("Dep Janitor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Obsidian / Ivory dual-theme desktop workspace",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AppDestination.entries.forEach { item ->
            val active = item == destination
            val backgroundColor = if (active) MaterialTheme.semanticColors.protect.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(18.dp))
                    .clickable { onSelect(item) }
                    .padding(14.dp),
            ) {
                Text(item.label, fontWeight = FontWeight.Medium)
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TopBar(
    destination: AppDestination,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    isScanning: Boolean,
    statusText: String,
    onScan: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(destination.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(destination.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(destination.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(statusText, style = MaterialTheme.typography.bodySmall, color = if (isScanning) MaterialTheme.semanticColors.warn else MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onThemeModeChange(ThemeMode.Obsidian) }) {
                    Text(ThemeMode.Obsidian.label)
                }
                Button(onClick = { onThemeModeChange(ThemeMode.Ivory) }) {
                    Text(ThemeMode.Ivory.label)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onScan, enabled = !isScanning) {
                    Text(if (isScanning) "扫描中…" else "重新扫描")
                }
                Text("当前主题：${themeMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
