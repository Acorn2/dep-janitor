package com.depjanitor.core.engine

import com.depjanitor.core.engine.analysis.WorkspaceArtifactAnalysisService
import com.depjanitor.core.engine.cleanup.WorkspaceCleanupAdvisorService
import com.depjanitor.core.engine.protection.ProjectReferenceProtectionService
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.ScanProgress
import com.depjanitor.core.model.WhitelistEntry

class WorkspaceDashboardService(
    private val metricsScanner: WorkspaceMetricsScanner = WorkspaceMetricsScanner(),
    private val artifactAnalysisService: WorkspaceArtifactAnalysisService = WorkspaceArtifactAnalysisService(),
    private val cleanupAdvisorService: WorkspaceCleanupAdvisorService = WorkspaceCleanupAdvisorService(),
    private val projectReferenceProtectionService: ProjectReferenceProtectionService = ProjectReferenceProtectionService(),
    private val previewWorkspaceService: PreviewWorkspaceService = PreviewWorkspaceService(),
) {

    fun scanDashboard(
        detectedPaths: List<DetectedPath>,
        ruleSet: CleanupRuleSet = cleanupAdvisorService.defaultRuleSet(),
        whitelistEntries: List<WhitelistEntry> = emptyList(),
        projectProtectionPaths: List<String> = emptyList(),
        onProgress: (ScanProgress) -> Unit = {},
    ): DashboardSnapshot {
        val metrics = metricsScanner.scan(detectedPaths, onProgress)
        onProgress(ScanProgress(phase = "解析 Maven / Gradle 结构"))
        val artifactEntries = artifactAnalysisService.analyze(detectedPaths)
        onProgress(ScanProgress(phase = "分析项目引用保护"))
        val protectedCoordinates = projectReferenceProtectionService.collectProtectedCoordinates(projectProtectionPaths)
        onProgress(ScanProgress(phase = "生成清理建议"))
        val candidates = cleanupAdvisorService.analyzeCandidates(
            detectedPaths = detectedPaths,
            artifactEntries = artifactEntries,
            ruleSet = ruleSet,
            whitelistEntries = whitelistEntries,
            protectedCoordinates = protectedCoordinates,
        )
        val recipes = cleanupAdvisorService.buildRecipes(candidates)
        val simulation = cleanupAdvisorService.buildSimulation(candidates, artifactEntries, ruleSet)
        return previewWorkspaceService.buildDashboard(
            detectedPaths = detectedPaths,
            metrics = metrics,
            artifactEntries = artifactEntries,
            ruleSet = ruleSet,
            whitelistEntries = whitelistEntries,
            projectProtectionPaths = projectProtectionPaths,
            protectedCoordinates = protectedCoordinates,
            candidates = candidates,
            recipes = recipes,
            simulation = simulation,
            scannedAtMillis = System.currentTimeMillis(),
        )
    }
}
