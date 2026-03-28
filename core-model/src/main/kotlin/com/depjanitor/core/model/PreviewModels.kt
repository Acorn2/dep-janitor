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

data class DuplicateVersionEntry(
    val coordinate: String,
    val source: ArtifactSource,
    val versionCount: Int,
    val staleVersionCount: Int,
    val totalSizeBytes: Long,
)

data class VersionDistributionEntry(
    val label: String,
    val artifactCount: Int,
    val totalSizeBytes: Long,
)

data class WrapperDistributionEntry(
    val label: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
)

data class DashboardSnapshot(
    val scannedAtMillis: Long,
    val totalBytes: Long,
    val mavenBytes: Long,
    val gradleBytes: Long,
    val reclaimableBytes: Long,
    val lowRiskCount: Int,
    val mediumRiskCount: Int,
    val highRiskCount: Int,
    val hotspots: List<HotspotEntry>,
    val duplicateVersionRankings: List<DuplicateVersionEntry>,
    val oldVersionDistributions: List<VersionDistributionEntry>,
    val wrapperDistributions: List<WrapperDistributionEntry>,
    val strata: List<ArtifactStrataPreview>,
    val recipes: List<RecipePreview>,
    val simulation: SimulationPreview,
    val ruleSet: CleanupRuleSet,
    val whitelistEntries: List<WhitelistEntry>,
    val projectProtectionPaths: List<String>,
    val protectedCoordinates: Set<String>,
    val candidates: List<CleanupCandidate>,
    val detectedPaths: List<DetectedPath>,
    val artifactEntries: List<ArtifactScanEntry>,
)
