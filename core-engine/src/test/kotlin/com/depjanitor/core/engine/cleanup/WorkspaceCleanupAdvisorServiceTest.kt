package com.depjanitor.core.engine.cleanup

import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.CleanupCandidateKind
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.VersionScanEntry
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkspaceCleanupAdvisorServiceTest {

    @Test
    fun `should identify residue old versions and legacy wrappers`() {
        val nowMillis = 1_800_000_000_000L
        val service = WorkspaceCleanupAdvisorService(nowMillis = nowMillis)
        val mavenRoot = Files.createTempDirectory("depjanitor-maven")
        val residueFile = mavenRoot.resolve("org/example/demo/1.0.0/demo-1.0.0.jar.lastUpdated")
        residueFile.parent.createDirectories()
        residueFile.writeBytes(ByteArray(12))
        Files.setLastModifiedTime(residueFile, FileTime.fromMillis(nowMillis - 10_000L))

        val staleMillis = nowMillis - 220L * 24L * 60L * 60L * 1_000L
        val artifactEntries = listOf(
            ArtifactScanEntry(
                coordinate = "org.example:demo",
                source = ArtifactSource.MAVEN,
                group = "org.example",
                artifact = "demo",
                totalSizeBytes = 370L,
                lastModifiedMillis = nowMillis - 5_000L,
                versions = listOf(
                    VersionScanEntry("3.0.0", 150L, nowMillis - 1_000L, ArtifactSource.MAVEN),
                    VersionScanEntry("2.0.0", 120L, nowMillis - 2_000L, ArtifactSource.MAVEN),
                    VersionScanEntry("1.0.0", 100L, staleMillis, ArtifactSource.MAVEN, RiskLevel.MEDIUM, "candidate"),
                ),
            ),
            ArtifactScanEntry(
                coordinate = "gradle-wrapper:gradle-8.7-bin",
                source = ArtifactSource.WRAPPER,
                totalSizeBytes = 200L,
                lastModifiedMillis = nowMillis - 1_000L,
                versions = listOf(
                    VersionScanEntry("gradle-8.7-bin", 200L, nowMillis - 1_000L, ArtifactSource.WRAPPER, RiskLevel.HIGH, "caution"),
                ),
            ),
            ArtifactScanEntry(
                coordinate = "gradle-wrapper:gradle-7.6-bin",
                source = ArtifactSource.WRAPPER,
                totalSizeBytes = 180L,
                lastModifiedMillis = staleMillis,
                versions = listOf(
                    VersionScanEntry("gradle-7.6-bin", 180L, staleMillis, ArtifactSource.WRAPPER, RiskLevel.HIGH, "caution"),
                ),
            ),
        )

        val candidates = service.analyzeCandidates(
            detectedPaths = listOf(
                DetectedPath(PathKind.MAVEN_REPOSITORY, mavenRoot.toString(), exists = true),
            ),
            artifactEntries = artifactEntries,
            ruleSet = CleanupRuleSet(),
        )

        assertTrue(candidates.any { it.kind == CleanupCandidateKind.LAST_UPDATED_RESIDUE && it.defaultSelected })
        assertTrue(candidates.any { it.kind == CleanupCandidateKind.OLD_VERSION && it.coordinate == "org.example:demo" })
        assertTrue(candidates.any { it.kind == CleanupCandidateKind.LEGACY_WRAPPER && it.riskLevel == RiskLevel.HIGH })

        val recipes = service.buildRecipes(candidates)
        assertTrue(recipes.any { it.title == "Residue Sweep" && it.defaultEnabled })

        val simulation = service.buildSimulation(candidates, artifactEntries, CleanupRuleSet())
        assertEquals(1, simulation.selectedItemCount)
        assertTrue(simulation.releasableBytes >= 12L)
        assertEquals(1, simulation.highRiskCount)
        assertEquals(3, simulation.protectedRuleCount)
    }
}
