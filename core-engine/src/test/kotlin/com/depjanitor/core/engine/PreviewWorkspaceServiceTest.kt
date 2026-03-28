package com.depjanitor.core.engine

import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.VersionScanEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreviewWorkspaceServiceTest {

    @Test
    fun `should build duplicate ranking old version distribution and wrapper distribution from artifacts`() {
        val service = PreviewWorkspaceService()
        val artifactEntries = listOf(
            ArtifactScanEntry(
                coordinate = "org.example:alpha",
                source = ArtifactSource.MAVEN,
                totalSizeBytes = 300L,
                lastModifiedMillis = 1_000L,
                versions = listOf(
                    VersionScanEntry("3.0.0", 120L, 1_000L, ArtifactSource.MAVEN),
                    VersionScanEntry("2.0.0", 100L, 900L, ArtifactSource.MAVEN, riskLevel = RiskLevel.MEDIUM),
                    VersionScanEntry("1.0.0", 80L, 800L, ArtifactSource.MAVEN, riskLevel = RiskLevel.MEDIUM),
                ),
            ),
            ArtifactScanEntry(
                coordinate = "org.example:beta",
                source = ArtifactSource.MAVEN,
                totalSizeBytes = 160L,
                lastModifiedMillis = 2_000L,
                versions = listOf(
                    VersionScanEntry("2.0.0", 90L, 2_000L, ArtifactSource.MAVEN),
                    VersionScanEntry("1.0.0", 70L, 1_500L, ArtifactSource.MAVEN, riskLevel = RiskLevel.MEDIUM),
                ),
            ),
            ArtifactScanEntry(
                coordinate = "gradle-wrapper:gradle-8.7-bin",
                source = ArtifactSource.WRAPPER,
                totalSizeBytes = 500L,
                lastModifiedMillis = 3_000L,
                versions = listOf(
                    VersionScanEntry("gradle-8.7-bin", 500L, 3_000L, ArtifactSource.WRAPPER, riskLevel = RiskLevel.HIGH),
                ),
            ),
        )

        val snapshot = service.buildDashboard(
            detectedPaths = emptyList(),
            artifactEntries = artifactEntries,
            scannedAtMillis = 9_999L,
        )

        assertEquals(9_999L, snapshot.scannedAtMillis)
        assertEquals("org.example:alpha", snapshot.duplicateVersionRankings.first().coordinate)
        assertEquals(3, snapshot.duplicateVersionRankings.first().versionCount)
        assertTrue(snapshot.oldVersionDistributions.any { it.label == "1 个旧版本" && it.artifactCount == 1 })
        assertTrue(snapshot.oldVersionDistributions.any { it.label == "2-3 个旧版本" && it.artifactCount == 1 })
        assertEquals("gradle-8.7-bin", snapshot.wrapperDistributions.first().label)
    }
}
