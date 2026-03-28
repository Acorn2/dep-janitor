package com.depjanitor.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.depjanitor.app.ui.shell.AppShell
import com.depjanitor.app.ui.theme.DepJanitorTheme
import com.depjanitor.app.ui.theme.ThemeMode
import com.depjanitor.core.engine.PreviewWorkspaceService
import com.depjanitor.core.engine.WorkspaceDashboardService
import com.depjanitor.core.engine.delete.WorkspaceCleanupExecutionService
import com.depjanitor.core.model.AppSettings
import com.depjanitor.core.model.CleanupExecutionItem
import com.depjanitor.core.model.CleanupExecutionMode
import com.depjanitor.core.model.CleanupExecutionPlan
import com.depjanitor.core.model.CleanupExecutionResult
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.CustomScanPath
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.WhitelistEntry
import com.depjanitor.core.model.releasedBytes
import com.depjanitor.core.model.successCount
import com.depjanitor.core.platform.config.AppConfigStore
import com.depjanitor.core.platform.path.DefaultPathDiscoveryService
import com.depjanitor.core.platform.path.PathPresentationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dep Janitor",
        state = WindowState(size = DpSize(1480.dp, 980.dp)),
    ) {
        DepJanitorApp()
    }
}

private data class WorkspaceUiState(
    val snapshot: DashboardSnapshot,
    val isScanning: Boolean,
    val statusText: String,
)

@Composable
private fun DepJanitorApp() {
    val configStore = remember { AppConfigStore() }
    val storedSettings = remember { configStore.load() }
    var themeMode by remember {
        mutableStateOf(
            ThemeMode.entries.firstOrNull { it.name == storedSettings.themeModeName } ?: ThemeMode.Obsidian,
        )
    }
    var pathOverrides by remember { mutableStateOf(storedSettings.pathOverrides) }
    var customScanPaths by remember { mutableStateOf(storedSettings.customScanPaths) }
    var cleanupRuleSet by remember { mutableStateOf(storedSettings.cleanupRuleSet) }
    var scanCustomPaths by remember { mutableStateOf(storedSettings.scanCustomPaths) }
    var whitelistEntries by remember { mutableStateOf(storedSettings.whitelistEntries) }
    var projectProtectionPaths by remember { mutableStateOf(storedSettings.projectProtectionPaths) }

    val pathDiscoveryService = remember { DefaultPathDiscoveryService() }
    val pathPresentationService = remember { PathPresentationService() }
    val dashboardService = remember { WorkspaceDashboardService() }
    val cleanupExecutionService = remember { WorkspaceCleanupExecutionService() }
    val previewWorkspaceService = remember { PreviewWorkspaceService() }
    fun resolveDetectedPaths(): List<com.depjanitor.core.model.DetectedPath> {
        return pathDiscoveryService.discover(
            pathOverrides = if (scanCustomPaths) pathOverrides else emptyMap(),
            customScanPaths = if (scanCustomPaths) customScanPaths else emptyList(),
        )
    }
    val detectedPaths = remember(pathOverrides, customScanPaths, scanCustomPaths) { resolveDetectedPaths() }
    var uiState by remember(detectedPaths) {
        mutableStateOf(
            WorkspaceUiState(
                snapshot = previewWorkspaceService.buildDashboard(
                    detectedPaths = detectedPaths,
                    ruleSet = cleanupRuleSet,
                    whitelistEntries = whitelistEntries,
                    projectProtectionPaths = projectProtectionPaths,
                ),
                isScanning = false,
                statusText = "已发现 ${detectedPaths.count { it.exists }} 个本地缓存路径，等待扫描。",
            ),
        )
    }
    val scope = rememberCoroutineScope()
    var isCleaning by remember { mutableStateOf(false) }
    var lastCleanupResult by remember { mutableStateOf<CleanupExecutionResult?>(null) }
    var selectedCandidateIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasInitializedExecutionPlan by remember { mutableStateOf(false) }

    fun persistSettings(
        nextThemeMode: ThemeMode = themeMode,
        nextOverrides: Map<PathKind, String> = pathOverrides,
        nextCustomScanPaths: List<CustomScanPath> = customScanPaths,
        nextRuleSet: CleanupRuleSet = cleanupRuleSet,
        nextScanCustomPaths: Boolean = scanCustomPaths,
        nextWhitelistEntries: List<WhitelistEntry> = whitelistEntries,
        nextProjectProtectionPaths: List<String> = projectProtectionPaths,
    ) {
        configStore.save(
            AppSettings(
                themeModeName = nextThemeMode.name,
                pathOverrides = nextOverrides,
                customScanPaths = nextCustomScanPaths,
                cleanupRuleSet = nextRuleSet,
                scanCustomPaths = nextScanCustomPaths,
                whitelistEntries = nextWhitelistEntries,
                projectProtectionPaths = nextProjectProtectionPaths,
            ),
        )
    }

    fun triggerScan() {
        scope.launch {
            uiState = uiState.copy(isScanning = true, statusText = "正在初始化扫描…")
            val currentPaths = resolveDetectedPaths()
            val snapshot = withContext(Dispatchers.IO) {
                dashboardService.scanDashboard(
                    detectedPaths = currentPaths,
                    ruleSet = cleanupRuleSet,
                    whitelistEntries = whitelistEntries,
                    projectProtectionPaths = projectProtectionPaths,
                ) { progress ->
                    val status = buildString {
                        append(progress.phase)
                        progress.currentTarget?.let { append(" · ").append(it) }
                        if (progress.totalTargets > 0) {
                            append(" (").append(progress.completedTargets).append('/').append(progress.totalTargets).append(')')
                        }
                    }
                    scope.launch {
                        uiState = uiState.copy(isScanning = true, statusText = status)
                    }
                }
            }
            uiState = uiState.copy(
                snapshot = snapshot,
                isScanning = false,
                statusText = "扫描完成：共发现 ${snapshot.detectedPaths.count { it.exists }} 个有效路径，总缓存 ${com.depjanitor.app.ui.formatBytes(snapshot.totalBytes)}。",
            )
        }
    }

    fun executeCleanup(selectedCandidateIds: Set<String>, mode: CleanupExecutionMode) {
        scope.launch {
            val selectedItems = uiState.snapshot.candidates
                .filter { it.id in selectedCandidateIds }
                .mapNotNull { candidate ->
                    val path = candidate.path ?: return@mapNotNull null
                    CleanupExecutionItem(
                        candidateId = candidate.id,
                        coordinate = candidate.coordinate,
                        path = path,
                        source = candidate.source,
                        riskLevel = candidate.riskLevel,
                        sizeBytes = candidate.sizeBytes,
                    )
                }
            if (selectedItems.isEmpty()) {
                uiState = uiState.copy(statusText = "没有可执行的清理项。")
                return@launch
            }

            isCleaning = true
            uiState = uiState.copy(statusText = "正在执行清理…")
            val result = withContext(Dispatchers.IO) {
                cleanupExecutionService.execute(
                    CleanupExecutionPlan(
                        items = selectedItems,
                        mode = mode,
                    ),
                )
            }
            lastCleanupResult = result
            isCleaning = false
            uiState = uiState.copy(
                statusText = "清理完成：成功 ${result.successCount} 项，释放 ${com.depjanitor.app.ui.formatBytes(result.releasedBytes)}。",
            )
            triggerScan()
        }
    }

    fun addCandidatesToPlan(candidateIds: Set<String>) {
        val actionableIds = uiState.snapshot.candidates
            .filter { it.path != null && it.riskLevel != RiskLevel.PROTECTED }
            .map { it.id }
            .toSet()
        selectedCandidateIds = (selectedCandidateIds + candidateIds).intersect(actionableIds)
        hasInitializedExecutionPlan = true
    }

    fun removeCandidatesFromPlan(candidateIds: Set<String>) {
        selectedCandidateIds = selectedCandidateIds - candidateIds
        hasInitializedExecutionPlan = true
    }

    fun toggleCandidateInPlan(candidateId: String) {
        selectedCandidateIds = if (candidateId in selectedCandidateIds) {
            selectedCandidateIds - candidateId
        } else {
            val candidate = uiState.snapshot.candidates.firstOrNull { it.id == candidateId }
            if (candidate?.path != null && candidate.riskLevel != RiskLevel.PROTECTED) {
                selectedCandidateIds + candidateId
            } else {
                selectedCandidateIds
            }
        }
        hasInitializedExecutionPlan = true
    }

    fun resetPlanSelection() {
        selectedCandidateIds = uiState.snapshot.candidates
            .filter { it.defaultSelected && it.path != null && it.riskLevel != RiskLevel.PROTECTED }
            .map { it.id }
            .toSet()
        hasInitializedExecutionPlan = true
    }

    fun clearPlanSelection() {
        selectedCandidateIds = emptySet()
        hasInitializedExecutionPlan = true
    }

    LaunchedEffect(pathOverrides, customScanPaths, cleanupRuleSet, scanCustomPaths, whitelistEntries, projectProtectionPaths) {
        triggerScan()
    }

    LaunchedEffect(uiState.snapshot.candidates) {
        val actionableIds = uiState.snapshot.candidates
            .filter { it.path != null && it.riskLevel != RiskLevel.PROTECTED }
            .map { it.id }
            .toSet()
        val defaultIds = uiState.snapshot.candidates
            .filter { it.defaultSelected && it.path != null && it.riskLevel != RiskLevel.PROTECTED }
            .map { it.id }
            .toSet()

        when {
            actionableIds.isEmpty() -> {
                selectedCandidateIds = emptySet()
                hasInitializedExecutionPlan = false
            }

            !hasInitializedExecutionPlan -> {
                selectedCandidateIds = defaultIds
                hasInitializedExecutionPlan = true
            }

            else -> {
                selectedCandidateIds = selectedCandidateIds.intersect(actionableIds)
            }
        }
    }

    DepJanitorTheme(mode = themeMode) {
        AppShell(
            themeMode = themeMode,
            onThemeModeChange = {
                themeMode = it
                persistSettings(nextThemeMode = it)
            },
            snapshot = uiState.snapshot,
            isScanning = uiState.isScanning,
            statusText = uiState.statusText,
            onScan = ::triggerScan,
            isCleaning = isCleaning,
            lastCleanupResult = lastCleanupResult,
            onExecuteCleanup = ::executeCleanup,
            onOpenPath = { path ->
                val opened = pathPresentationService.openInFileManager(path)
                if (!opened) {
                    uiState = uiState.copy(statusText = "无法打开路径：$path")
                }
            },
            selectedCandidateIds = selectedCandidateIds,
            onAddCandidatesToPlan = ::addCandidatesToPlan,
            onRemoveCandidatesFromPlan = ::removeCandidatesFromPlan,
            onToggleCandidateInPlan = ::toggleCandidateInPlan,
            onResetPlanSelection = ::resetPlanSelection,
            onClearPlanSelection = ::clearPlanSelection,
            scanCustomPaths = scanCustomPaths,
            onScanCustomPathsChange = {
                scanCustomPaths = it
                persistSettings(nextScanCustomPaths = it)
            },
            onRuleSetChange = {
                cleanupRuleSet = it
                persistSettings(nextRuleSet = it)
            },
            onAddWhitelistEntry = { entry ->
                val nextEntries = (whitelistEntries + entry).distinct()
                whitelistEntries = nextEntries
                persistSettings(nextWhitelistEntries = nextEntries)
            },
            onRemoveWhitelistEntry = { entry ->
                val nextEntries = whitelistEntries.filterNot { it == entry }
                whitelistEntries = nextEntries
                persistSettings(nextWhitelistEntries = nextEntries)
            },
            projectProtectionPaths = projectProtectionPaths,
            onAddProjectProtectionPath = { path ->
                val normalized = path.trim()
                if (normalized.isNotBlank()) {
                    val nextPaths = (projectProtectionPaths + normalized).distinct()
                    projectProtectionPaths = nextPaths
                    persistSettings(nextProjectProtectionPaths = nextPaths)
                }
            },
            onRemoveProjectProtectionPath = { path ->
                val nextPaths = projectProtectionPaths.filterNot { it == path }
                projectProtectionPaths = nextPaths
                persistSettings(nextProjectProtectionPaths = nextPaths)
            },
            customScanPaths = customScanPaths,
            onAddCustomScanPath = { entry ->
                val normalized = entry.path.trim()
                if (normalized.isNotBlank()) {
                    val nextEntries = (customScanPaths + entry.copy(path = normalized))
                        .distinctBy { "${it.kind.name}:${it.path.lowercase()}" }
                    customScanPaths = nextEntries
                    persistSettings(nextCustomScanPaths = nextEntries)
                }
            },
            onToggleCustomScanPathEnabled = { entry ->
                val nextEntries = customScanPaths.map {
                    if (it == entry) it.copy(enabled = !it.enabled) else it
                }
                customScanPaths = nextEntries
                persistSettings(nextCustomScanPaths = nextEntries)
            },
            onRemoveCustomScanPath = { entry ->
                val nextEntries = customScanPaths.filterNot { it == entry }
                customScanPaths = nextEntries
                persistSettings(nextCustomScanPaths = nextEntries)
            },
            onPathOverrideChange = { kind, path ->
                val nextOverrides = pathOverrides.toMutableMap().apply {
                    if (path.isBlank()) remove(kind) else put(kind, path)
                }.toMap()
                pathOverrides = nextOverrides
                persistSettings(nextOverrides = nextOverrides)
            },
            onPathOverrideReset = { kind ->
                val nextOverrides = pathOverrides.toMutableMap().apply { remove(kind) }.toMap()
                pathOverrides = nextOverrides
                persistSettings(nextOverrides = nextOverrides)
            },
        )
    }
}
