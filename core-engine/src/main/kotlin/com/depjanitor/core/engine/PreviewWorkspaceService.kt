package com.depjanitor.core.engine

import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.ArtifactStrataLayer
import com.depjanitor.core.model.ArtifactStrataPreview
import com.depjanitor.core.model.CleanupCandidate
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.DashboardSnapshot
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.DuplicateVersionEntry
import com.depjanitor.core.model.HotspotEntry
import com.depjanitor.core.model.RecipePreview
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.SimulationPreview
import com.depjanitor.core.model.VersionDistributionEntry
import com.depjanitor.core.model.WhitelistEntry
import com.depjanitor.core.model.WorkspaceMetrics
import com.depjanitor.core.model.WrapperDistributionEntry

class PreviewWorkspaceService {

    fun buildDashboard(
        detectedPaths: List<DetectedPath>,
        metrics: WorkspaceMetrics? = null,
        artifactEntries: List<ArtifactScanEntry> = emptyList(),
        ruleSet: CleanupRuleSet = CleanupRuleSet(),
        whitelistEntries: List<WhitelistEntry> = emptyList(),
        projectProtectionPaths: List<String> = emptyList(),
        protectedCoordinates: Set<String> = emptySet(),
        candidates: List<CleanupCandidate> = emptyList(),
        recipes: List<RecipePreview> = emptyList(),
        simulation: SimulationPreview? = null,
        scannedAtMillis: Long = System.currentTimeMillis(),
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
        val duplicateVersionRankings = artifactEntries.takeIf { it.isNotEmpty() }
            ?.map { entry ->
                DuplicateVersionEntry(
                    coordinate = entry.coordinate,
                    source = entry.source,
                    versionCount = entry.versions.size,
                    staleVersionCount = entry.versions.count { it.riskLevel == RiskLevel.MEDIUM || it.riskLevel == RiskLevel.HIGH },
                    totalSizeBytes = entry.totalSizeBytes,
                )
            }
            ?.sortedWith(compareByDescending<DuplicateVersionEntry> { it.versionCount }.thenByDescending { it.totalSizeBytes })
            ?.take(6)
            ?: previewDuplicateVersionRankings()
        val oldVersionDistributions = artifactEntries.takeIf { it.isNotEmpty() }
            ?.let(::buildOldVersionDistributions)
            ?.takeIf { it.isNotEmpty() }
            ?: previewOldVersionDistributions()
        val wrapperDistributions = artifactEntries.filter { it.source == ArtifactSource.WRAPPER }
            .takeIf { it.isNotEmpty() }
            ?.map { entry ->
                WrapperDistributionEntry(
                    label = entry.versions.firstOrNull()?.label ?: entry.coordinate,
                    sizeBytes = entry.totalSizeBytes,
                    lastModifiedMillis = entry.lastModifiedMillis,
                )
            }
            ?.sortedByDescending { it.sizeBytes }
            ?: previewWrapperDistributions()
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
            scannedAtMillis = scannedAtMillis,
            totalBytes = totalBytes,
            mavenBytes = mavenBytes,
            gradleBytes = gradleBytes + wrapperBytes,
            reclaimableBytes = if (previewMode) 0L else candidates.sumOf { it.sizeBytes },
            lowRiskCount = lowRiskCount,
            mediumRiskCount = mediumRiskCount,
            highRiskCount = highRiskCount,
            hotspots = hotspots,
            duplicateVersionRankings = duplicateVersionRankings,
            oldVersionDistributions = oldVersionDistributions,
            wrapperDistributions = wrapperDistributions,
            strata = strata,
            recipes = recipePreviews,
            simulation = simulationPreview,
            ruleSet = ruleSet,
            whitelistEntries = whitelistEntries,
            projectProtectionPaths = projectProtectionPaths,
            protectedCoordinates = protectedCoordinates,
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

    private fun previewDuplicateVersionRankings(): List<DuplicateVersionEntry> = listOf(
        DuplicateVersionEntry("org.slf4j:slf4j-api", ArtifactSource.MAVEN, 4, 2, gb(1.4)),
        DuplicateVersionEntry("ch.qos.logback:logback-classic", ArtifactSource.MAVEN, 3, 1, gb(1.1)),
        DuplicateVersionEntry("gradle-wrapper:gradle-7.6-bin", ArtifactSource.WRAPPER, 3, 2, gb(1.8)),
    )

    private fun previewOldVersionDistributions(): List<VersionDistributionEntry> = listOf(
        VersionDistributionEntry("1 个旧版本", 34, gb(6.8)),
        VersionDistributionEntry("2-3 个旧版本", 19, gb(9.4)),
        VersionDistributionEntry("4+ 个旧版本", 7, gb(12.2)),
    )

    private fun previewWrapperDistributions(): List<WrapperDistributionEntry> = listOf(
        WrapperDistributionEntry("gradle-8.7-bin", gb(1.2), System.currentTimeMillis()),
        WrapperDistributionEntry("gradle-7.6-bin", gb(0.9), System.currentTimeMillis() - 86_400_000L),
        WrapperDistributionEntry("gradle-6.9-bin", gb(0.6), System.currentTimeMillis() - 172_800_000L),
    )

    private fun buildOldVersionDistributions(artifactEntries: List<ArtifactScanEntry>): List<VersionDistributionEntry> {
        val buckets = linkedMapOf(
            "1 个旧版本" to mutableListOf<ArtifactScanEntry>(),
            "2-3 个旧版本" to mutableListOf(),
            "4+ 个旧版本" to mutableListOf(),
        )
        artifactEntries.filter { it.source == ArtifactSource.MAVEN }.forEach { entry ->
            val staleVersionCount = entry.versions.count { it.riskLevel == RiskLevel.MEDIUM || it.riskLevel == RiskLevel.HIGH }
            when {
                staleVersionCount == 1 -> buckets.getValue("1 个旧版本") += entry
                staleVersionCount in 2..3 -> buckets.getValue("2-3 个旧版本") += entry
                staleVersionCount >= 4 -> buckets.getValue("4+ 个旧版本") += entry
            }
        }
        return buckets.mapNotNull { (label, entries) ->
            entries.takeIf { it.isNotEmpty() }?.let {
                VersionDistributionEntry(
                    label = label,
                    artifactCount = it.size,
                    totalSizeBytes = it.sumOf { item -> item.totalSizeBytes },
                )
            }
        }
    }

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
