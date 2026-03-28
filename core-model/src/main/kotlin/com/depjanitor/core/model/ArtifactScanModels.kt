package com.depjanitor.core.model

data class VersionScanEntry(
    val label: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val source: ArtifactSource,
    val timeBasis: TimeBasis = TimeBasis.LAST_MODIFIED,
    val timeBasisFallback: Boolean = false,
    val path: String? = null,
    val riskLevel: RiskLevel? = null,
    val state: String = "inspect",
)

data class ArtifactScanEntry(
    val coordinate: String,
    val source: ArtifactSource,
    val group: String? = null,
    val artifact: String? = null,
    val path: String? = null,
    val totalSizeBytes: Long,
    val lastModifiedMillis: Long,
    val versions: List<VersionScanEntry>,
    val timeBasis: TimeBasis = TimeBasis.LAST_MODIFIED,
    val timeBasisFallback: Boolean = false,
)

val ArtifactScanEntry.versionCount: Int
    get() = versions.size

val ArtifactScanEntry.primaryRiskLevel: RiskLevel
    get() = when {
        versions.any { it.riskLevel == RiskLevel.HIGH } -> RiskLevel.HIGH
        versions.any { it.riskLevel == RiskLevel.MEDIUM } -> RiskLevel.MEDIUM
        versions.any { it.riskLevel == RiskLevel.PROTECTED } -> RiskLevel.PROTECTED
        else -> RiskLevel.LOW
    }
