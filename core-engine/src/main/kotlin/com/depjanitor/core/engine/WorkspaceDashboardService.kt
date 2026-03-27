package com.depjanitor.core.engine

import com.depjanitor.core.engine.analysis.WorkspaceArtifactAnalysisService
import com.depjanitor.core.engine.cleanup.WorkspaceCleanupAdvisorService
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.ScanProgress

class WorkspaceDashboardService(
    private val metricsScanner: WorkspaceMetricsScanner = WorkspaceMetricsScanner(),
    private val artifactAnalysisService: WorkspaceArtifactAnalysisService = WorkspaceArtifactAnalysisService(),
    private val cleanupAdvisorService: WorkspaceCleanupAdvisorService = WorkspaceCleanupAdvisorService(),
    private val previewWorkspaceService: PreviewWorkspaceService = PreviewWorkspaceService(),
) {

    fun scanDashboard(
        detectedPaths: List<DetectedPath>,
        onProgress: (ScanProgress) -> Unit = {},
    ): DashboardSnapshot {
        val metrics = metricsScanner.scan(detectedPaths, onProgress)
        onProgress(ScanProgress(phase = "解析 Maven / Gradle 结构"))
        val artifactEntries = artifactAnalysisService.analyze(detectedPaths)
        onProgress(ScanProgress(phase = "生成清理建议"))
        val ruleSet = cleanupAdvisorService.defaultRuleSet()
        val candidates = cleanupAdvisorService.analyzeCandidates(detectedPaths, artifactEntries, ruleSet)
        val recipes = cleanupAdvisorService.buildRecipes(candidates)
        val simulation = cleanupAdvisorService.buildSimulation(candidates, artifactEntries, ruleSet)
        return previewWorkspaceService.buildDashboard(
            detectedPaths = detectedPaths,
            metrics = metrics,
            artifactEntries = artifactEntries,
            ruleSet = ruleSet,
            candidates = candidates,
            recipes = recipes,
            simulation = simulation,
        )
    }
}
