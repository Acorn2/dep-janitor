package com.depjanitor.core.model

data class HotspotEntry(
    val name: String,
    val source: ArtifactSource,
    val sizeBytes: Long,
)

data class ArtifactStrataLayer(
    val label: String,
    val source: ArtifactSource,
    val state: String,
    val riskLevel: RiskLevel? = null,
)

data class ArtifactStrataPreview(
    val coordinate: String,
    val totalSizeBytes: Long,
    val layers: List<ArtifactStrataLayer>,
)

data class RecipePreview(
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
    val source: ArtifactSource,
    val hitCount: Int,
    val releasableBytes: Long,
    val defaultEnabled: Boolean,
)

data class SimulationPreview(
    val releasableBytes: Long,
    val selectedItemCount: Int,
    val highRiskCount: Int,
    val protectedRuleCount: Int,
)

data class DashboardSnapshot(
    val totalBytes: Long,
    val mavenBytes: Long,
    val gradleBytes: Long,
    val reclaimableBytes: Long,
    val lowRiskCount: Int,
    val mediumRiskCount: Int,
    val highRiskCount: Int,
    val hotspots: List<HotspotEntry>,
    val strata: List<ArtifactStrataPreview>,
    val recipes: List<RecipePreview>,
    val simulation: SimulationPreview,
    val ruleSet: CleanupRuleSet,
    val candidates: List<CleanupCandidate>,
    val detectedPaths: List<DetectedPath>,
    val artifactEntries: List<ArtifactScanEntry>,
)
