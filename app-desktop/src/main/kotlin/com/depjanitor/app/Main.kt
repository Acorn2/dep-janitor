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
import com.depjanitor.core.model.AppSettings
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.platform.config.AppConfigStore
import com.depjanitor.core.platform.path.DefaultPathDiscoveryService
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

    val pathDiscoveryService = remember { DefaultPathDiscoveryService() }
    val dashboardService = remember { WorkspaceDashboardService() }
    val previewWorkspaceService = remember { PreviewWorkspaceService() }
    val detectedPaths = remember(pathOverrides) { pathDiscoveryService.discover(pathOverrides) }
    var uiState by remember(detectedPaths) {
        mutableStateOf(
            WorkspaceUiState(
                snapshot = previewWorkspaceService.buildDashboard(detectedPaths),
                isScanning = false,
                statusText = "已发现 ${detectedPaths.count { it.exists }} 个本地缓存路径，等待扫描。",
            ),
        )
    }
    val scope = rememberCoroutineScope()

    fun persistSettings(nextThemeMode: ThemeMode = themeMode, nextOverrides: Map<PathKind, String> = pathOverrides) {
        configStore.save(
            AppSettings(
                themeModeName = nextThemeMode.name,
                pathOverrides = nextOverrides,
            ),
        )
    }

    fun triggerScan() {
        scope.launch {
            uiState = uiState.copy(isScanning = true, statusText = "正在初始化扫描…")
            val currentPaths = pathDiscoveryService.discover(pathOverrides)
            val snapshot = withContext(Dispatchers.IO) {
                dashboardService.scanDashboard(currentPaths) { progress ->
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

    LaunchedEffect(pathOverrides) {
        triggerScan()
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
