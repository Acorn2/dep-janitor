package com.depjanitor.core.engine

import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.ArtifactStrataLayer
import com.depjanitor.core.model.ArtifactStrataPreview
import com.depjanitor.core.model.CleanupCandidate
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.HotspotEntry
import com.depjanitor.core.model.RecipePreview
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.SimulationPreview
import com.depjanitor.core.model.WorkspaceMetrics

class PreviewWorkspaceService {

    fun buildDashboard(
        detectedPaths: List<DetectedPath>,
        metrics: WorkspaceMetrics? = null,
        artifactEntries: List<ArtifactScanEntry> = emptyList(),
        ruleSet: CleanupRuleSet = CleanupRuleSet(),
        candidates: List<CleanupCandidate> = emptyList(),
        recipes: List<RecipePreview> = emptyList(),
        simulation: SimulationPreview? = null,
    ): DashboardSnapshot {
        val previewMode = metrics == null && artifactEntries.isEmpty() && candidates.isEmpty()
        val previewMavenBytes = gb(34.1)
        val previewGradleBytes = gb(48.3)
        val previewWrapperBytes = gb(9.7)
        val totalBytes = metrics?.totalBytes ?: previewMavenBytes + previewGradleBytes + previewWrapperBytes
        val mavenBytes = metrics?.mavenBytes ?: previewMavenBytes
        val gradleBytes = metrics?.gradleBytes ?: previewGradleBytes
        val wrapperBytes = metrics?.wrapperBytes ?: previewWrapperBytes
        val hotspots = metrics?.hotspots?.takeIf { it.isNotEmpty() } ?: previewHotspots()
        val strata = artifactEntries.takeIf { it.isNotEmpty() }?.take(6)?.map { it.toStrataPreview() } ?: previewStrata()
        val lowRiskCount = if (candidates.isNotEmpty()) candidates.count { it.riskLevel == RiskLevel.LOW } else artifactEntries.sumOf { entry -> entry.versions.count { it.riskLevel == null || it.riskLevel == RiskLevel.LOW } }.takeIf { it > 0 } ?: 128
        val mediumRiskCount = if (candidates.isNotEmpty()) candidates.count { it.riskLevel == RiskLevel.MEDIUM } else artifactEntries.sumOf { entry -> entry.versions.count { it.riskLevel == RiskLevel.MEDIUM } }.takeIf { it > 0 } ?: 46
        val highRiskCount = if (candidates.isNotEmpty()) candidates.count { it.riskLevel == RiskLevel.HIGH } else artifactEntries.sumOf { entry -> entry.versions.count { it.riskLevel == RiskLevel.HIGH } }.takeIf { it > 0 } ?: 7
        val recipePreviews = if (previewMode) previewRecipes() else recipes
        val simulationPreview = if (previewMode) {
            SimulationPreview(
                releasableBytes = 0L,
                selectedItemCount = 0,
                highRiskCount = highRiskCount,
                protectedRuleCount = 17,
            )
        } else {
            simulation ?: SimulationPreview(
                releasableBytes = candidates.filter { it.defaultSelected }.sumOf { it.sizeBytes },
                selectedItemCount = candidates.count { it.defaultSelected },
                highRiskCount = highRiskCount,
                protectedRuleCount = 0,
            )
        }

        return DashboardSnapshot(
            totalBytes = totalBytes,
            mavenBytes = mavenBytes,
            gradleBytes = gradleBytes + wrapperBytes,
            reclaimableBytes = if (previewMode) 0L else candidates.sumOf { it.sizeBytes },
            lowRiskCount = lowRiskCount,
            mediumRiskCount = mediumRiskCount,
            highRiskCount = highRiskCount,
            hotspots = hotspots,
            strata = strata,
            recipes = recipePreviews,
            simulation = simulationPreview,
            ruleSet = ruleSet,
            candidates = candidates,
            detectedPaths = detectedPaths,
            artifactEntries = artifactEntries,
        )
    }

    private fun ArtifactScanEntry.toStrataPreview(): ArtifactStrataPreview {
        return ArtifactStrataPreview(
            coordinate = coordinate,
            totalSizeBytes = totalSizeBytes,
            layers = versions.map {
                ArtifactStrataLayer(
                    label = it.label,
                    source = it.source,
                    state = it.state,
                    riskLevel = it.riskLevel,
                )
            },
        )
    }

    private fun previewHotspots(): List<HotspotEntry> = listOf(
        HotspotEntry("gradle/caches/modules", ArtifactSource.GRADLE, gb(18.2)),
        HotspotEntry(".m2/repository/com", ArtifactSource.MAVEN, gb(14.8)),
        HotspotEntry("gradle/wrapper/dists", ArtifactSource.WRAPPER, gb(9.7)),
        HotspotEntry(".m2/repository/org", ArtifactSource.MAVEN, gb(7.1)),
    )

    private fun previewStrata(): List<ArtifactStrataPreview> = listOf(
        ArtifactStrataPreview(
            coordinate = "org.slf4j:slf4j-api",
            totalSizeBytes = gb(1.4),
            layers = listOf(
                ArtifactStrataLayer("2.0.13", ArtifactSource.MAVEN, "keep"),
                ArtifactStrataLayer("2.0.9", ArtifactSource.MAVEN, "keep"),
                ArtifactStrataLayer("1.7.36", ArtifactSource.MAVEN, "aging", RiskLevel.MEDIUM),
                ArtifactStrataLayer("1.7.21", ArtifactSource.MAVEN, "candidate", RiskLevel.MEDIUM),
            ),
        ),
        ArtifactStrataPreview(
            coordinate = "gradle-7.6-bin",
            totalSizeBytes = gb(1.8),
            layers = listOf(
                ArtifactStrataLayer("8.7-bin", ArtifactSource.WRAPPER, "keep"),
                ArtifactStrataLayer("7.6-bin", ArtifactSource.WRAPPER, "inspect", RiskLevel.MEDIUM),
                ArtifactStrataLayer("6.9-bin", ArtifactSource.WRAPPER, "caution", RiskLevel.HIGH),
            ),
        ),
    )

    private fun previewRecipes(): List<RecipePreview> = listOf(
        RecipePreview(
            title = "Residue Sweep",
            description = "优先清理 .lastUpdated、失败下载残留与过期 SNAPSHOT。",
            riskLevel = RiskLevel.LOW,
            source = ArtifactSource.MAVEN,
            hitCount = 84,
            releasableBytes = gb(8.7),
            defaultEnabled = true,
        ),
        RecipePreview(
            title = "Version Retain-2",
            description = "每个 artifact 默认保留最近 2 个版本。",
            riskLevel = RiskLevel.MEDIUM,
            source = ArtifactSource.MAVEN,
            hitCount = 61,
            releasableBytes = gb(12.1),
            defaultEnabled = false,
        ),
        RecipePreview(
            title = "Wrapper Caution",
            description = "Gradle wrapper distributions 关联项默认不自动启用。",
            riskLevel = RiskLevel.HIGH,
            source = ArtifactSource.WRAPPER,
            hitCount = 7,
            releasableBytes = gb(5.3),
            defaultEnabled = false,
        ),
    )

    private fun gb(value: Double): Long = (value * 1024 * 1024 * 1024).toLong()
}
