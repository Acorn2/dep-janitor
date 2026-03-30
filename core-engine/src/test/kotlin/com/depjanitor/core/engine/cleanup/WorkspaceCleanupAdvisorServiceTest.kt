package com.depjanitor.core.engine.cleanup

import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.CleanupCandidateKind
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.TimeBasis
import com.depjanitor.core.model.VersionScanEntry
import com.depjanitor.core.model.WhitelistEntry
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
                    VersionScanEntry("1.0.0", 100L, staleMillis, ArtifactSource.MAVEN, riskLevel = RiskLevel.MEDIUM, state = "candidate"),
                ),
            ),
            ArtifactScanEntry(
                coordinate = "gradle-wrapper:gradle-8.7-bin",
                source = ArtifactSource.WRAPPER,
                totalSizeBytes = 200L,
                lastModifiedMillis = nowMillis - 1_000L,
                versions = listOf(
                    VersionScanEntry("gradle-8.7-bin", 200L, nowMillis - 1_000L, ArtifactSource.WRAPPER, riskLevel = RiskLevel.HIGH, state = "caution"),
                ),
            ),
            ArtifactScanEntry(
                coordinate = "gradle-wrapper:gradle-7.6-bin",
                source = ArtifactSource.WRAPPER,
                totalSizeBytes = 180L,
                lastModifiedMillis = staleMillis,
                versions = listOf(
                    VersionScanEntry("gradle-7.6-bin", 180L, staleMillis, ArtifactSource.WRAPPER, riskLevel = RiskLevel.HIGH, state = "caution"),
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
        val oldVersion = candidates.first { it.kind == CleanupCandidateKind.OLD_VERSION && it.coordinate == "org.example:demo" }
        assertEquals(RiskLevel.LOW, oldVersion.riskLevel)
        assertEquals(true, oldVersion.defaultSelected)
        assertTrue(candidates.any { it.kind == CleanupCandidateKind.LEGACY_WRAPPER && it.riskLevel == RiskLevel.HIGH })

        val recipes = service.buildRecipes(candidates)
        assertTrue(recipes.any { it.title == "Residue Sweep" && it.defaultEnabled })

        val simulation = service.buildSimulation(candidates, artifactEntries, CleanupRuleSet())
        assertEquals(2, simulation.selectedItemCount)
        assertTrue(simulation.releasableBytes >= 112L)
        assertEquals(1, simulation.highRiskCount)
        assertEquals(3, simulation.protectedRuleCount)
    }

    @Test
    fun `should mark whitelisted candidates as protected`() {
        val nowMillis = 1_800_000_000_000L
        val service = WorkspaceCleanupAdvisorService(nowMillis = nowMillis)
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
                    VersionScanEntry("1.0.0", 100L, staleMillis, ArtifactSource.MAVEN, riskLevel = RiskLevel.MEDIUM, state = "candidate"),
                ),
            ),
        )

        val candidates = service.analyzeCandidates(
            detectedPaths = emptyList(),
            artifactEntries = artifactEntries,
            ruleSet = CleanupRuleSet(),
            whitelistEntries = listOf(WhitelistEntry.coordinate("org.example:demo")),
        )

        assertEquals(1, candidates.size)
        assertEquals(RiskLevel.PROTECTED, candidates.single().riskLevel)
        assertEquals(false, candidates.single().defaultSelected)
        assertTrue(service.buildRecipes(candidates).isEmpty())
    }

    @Test
    fun `should mark project protected coordinates as protected`() {
        val service = WorkspaceCleanupAdvisorService(nowMillis = 1_800_000_000_000L)
        val artifactEntries = listOf(
            ArtifactScanEntry(
                coordinate = "org.example:demo",
                source = ArtifactSource.MAVEN,
                group = "org.example",
                artifact = "demo",
                totalSizeBytes = 370L,
                lastModifiedMillis = 1_799_999_995_000L,
                versions = listOf(
                    VersionScanEntry("3.0.0", 150L, 1_799_999_999_000L, ArtifactSource.MAVEN),
                    VersionScanEntry("2.0.0", 120L, 1_799_999_998_000L, ArtifactSource.MAVEN),
                    VersionScanEntry("1.0.0", 100L, 1_780_000_000_000L, ArtifactSource.MAVEN, riskLevel = RiskLevel.MEDIUM, state = "candidate"),
                ),
            ),
        )

        val candidates = service.analyzeCandidates(
            detectedPaths = emptyList(),
            artifactEntries = artifactEntries,
            ruleSet = CleanupRuleSet(),
            protectedCoordinates = setOf("org.example:demo"),
        )

        assertEquals(RiskLevel.PROTECTED, candidates.single().riskLevel)
        assertTrue(candidates.single().reason.contains("项目引用保护"))
    }

    @Test
    fun `should classify stale gradle residues as low risk and default selected`() {
        val nowMillis = 1_800_000_000_000L
        val service = WorkspaceCleanupAdvisorService(nowMillis = nowMillis)
        val staleMillis = nowMillis - 220L * 24L * 60L * 60L * 1_000L
        val artifactEntries = listOf(
            ArtifactScanEntry(
                coordinate = "gradle-cache:transforms-3",
                source = ArtifactSource.GRADLE,
                artifact = "transforms-3",
                totalSizeBytes = 120L,
                lastModifiedMillis = staleMillis,
                versions = listOf(
                    VersionScanEntry("transforms-3", 120L, staleMillis, ArtifactSource.GRADLE, riskLevel = RiskLevel.LOW, state = "residue"),
                ),
            ),
            ArtifactScanEntry(
                coordinate = "gradle-cache:fileHashes",
                source = ArtifactSource.GRADLE,
                artifact = "fileHashes",
                totalSizeBytes = 90L,
                lastModifiedMillis = staleMillis,
                versions = listOf(
                    VersionScanEntry("fileHashes", 90L, staleMillis, ArtifactSource.GRADLE, riskLevel = RiskLevel.HIGH, state = "caution"),
                ),
            ),
        )

        val candidates = service.analyzeCandidates(
            detectedPaths = emptyList(),
            artifactEntries = artifactEntries,
            ruleSet = CleanupRuleSet(),
        )

        val residue = candidates.first { it.coordinate == "gradle-cache:transforms-3" }
        val fileHashes = candidates.first { it.coordinate == "gradle-cache:fileHashes" }

        assertEquals(RiskLevel.LOW, residue.riskLevel)
        assertEquals(true, residue.defaultSelected)
        assertTrue(residue.reason.contains("Gradle transforms 残留"))
        assertTrue(residue.reason.contains("最近修改时间"))

        assertEquals(RiskLevel.HIGH, fileHashes.riskLevel)
        assertEquals(false, fileHashes.defaultSelected)
        assertTrue(fileHashes.reason.contains("fileHashes"))
    }

    @Test
    fun `should keep fallback semantic in candidate reason`() {
        val nowMillis = 1_800_000_000_000L
        val service = WorkspaceCleanupAdvisorService(nowMillis = nowMillis)
        val staleMillis = nowMillis - 220L * 24L * 60L * 60L * 1_000L
        val artifactEntries = listOf(
            ArtifactScanEntry(
                coordinate = "org.example:demo",
                source = ArtifactSource.MAVEN,
                group = "org.example",
                artifact = "demo",
                totalSizeBytes = 100L,
                lastModifiedMillis = staleMillis,
                timeBasis = TimeBasis.LAST_MODIFIED,
                timeBasisFallback = true,
                versions = listOf(
                    VersionScanEntry(
                        label = "1.0.0",
                        sizeBytes = 100L,
                        lastModifiedMillis = staleMillis,
                        source = ArtifactSource.MAVEN,
                        timeBasis = TimeBasis.LAST_MODIFIED,
                        timeBasisFallback = true,
                        riskLevel = RiskLevel.MEDIUM,
                        state = "candidate",
                    ),
                ),
            ),
        )

        val candidate = service.analyzeCandidates(
            detectedPaths = emptyList(),
            artifactEntries = artifactEntries,
            ruleSet = CleanupRuleSet(retainLatestVersions = 0),
        ).single()

        assertTrue(candidate.reason.contains("回退推断"))
        assertEquals(true, candidate.timeBasisFallback)
    }

    @Test
    fun `should only default select stale maven old versions`() {
        val nowMillis = 1_800_000_000_000L
        val service = WorkspaceCleanupAdvisorService(nowMillis = nowMillis)
        val staleMillis = nowMillis - 220L * 24L * 60L * 60L * 1_000L
        val freshMillis = nowMillis - 7L * 24L * 60L * 60L * 1_000L

        val artifactEntries = listOf(
            ArtifactScanEntry(
                coordinate = "org.example:demo",
                source = ArtifactSource.MAVEN,
                group = "org.example",
                artifact = "demo",
                totalSizeBytes = 460L,
                lastModifiedMillis = nowMillis - 1_000L,
                versions = listOf(
                    VersionScanEntry("4.0.0", 160L, nowMillis - 1_000L, ArtifactSource.MAVEN),
                    VersionScanEntry("3.0.0", 140L, nowMillis - 2_000L, ArtifactSource.MAVEN),
                    VersionScanEntry("2.0.0", 90L, staleMillis, ArtifactSource.MAVEN, riskLevel = RiskLevel.MEDIUM, state = "candidate"),
                    VersionScanEntry("1.0.0", 70L, freshMillis, ArtifactSource.MAVEN, riskLevel = RiskLevel.MEDIUM, state = "candidate"),
                ),
            ),
        )

        val candidates = service.analyzeCandidates(
            detectedPaths = emptyList(),
            artifactEntries = artifactEntries,
            ruleSet = CleanupRuleSet(retainLatestVersions = 2, unusedDaysThreshold = 180, prioritizeLowRisk = true),
        )

        val staleCandidate = candidates.first { it.versionLabel == "2.0.0" }
        val freshCandidate = candidates.first { it.versionLabel == "1.0.0" }

        assertEquals(RiskLevel.LOW, staleCandidate.riskLevel)
        assertEquals(true, staleCandidate.defaultSelected)
        assertTrue(staleCandidate.reason.contains("可优先纳入批量清理"))

        assertEquals(RiskLevel.MEDIUM, freshCandidate.riskLevel)
        assertEquals(false, freshCandidate.defaultSelected)
        assertTrue(freshCandidate.reason.contains("建议人工复核"))
    }

    @Test
    fun `should detect stale gradle lock residues as low risk candidates`() {
        val nowMillis = 1_800_000_000_000L
        val service = WorkspaceCleanupAdvisorService(nowMillis = nowMillis)
        val gradleRoot = Files.createTempDirectory("depjanitor-gradle")
        val staleLock = gradleRoot.resolve("modules-2/metadata.bin.lock")
        staleLock.parent.createDirectories()
        staleLock.writeBytes(ByteArray(8))
        Files.setLastModifiedTime(staleLock, FileTime.fromMillis(nowMillis - 30L * 24L * 60L * 60L * 1_000L))

        val candidates = service.analyzeCandidates(
            detectedPaths = listOf(
                DetectedPath(PathKind.GRADLE_CACHES, gradleRoot.toString(), exists = true),
            ),
            artifactEntries = emptyList(),
            ruleSet = CleanupRuleSet(),
        )

        val residue = candidates.single()
        assertEquals(CleanupCandidateKind.FAILED_DOWNLOAD_RESIDUE, residue.kind)
        assertEquals(ArtifactSource.GRADLE, residue.source)
        assertEquals(RiskLevel.LOW, residue.riskLevel)
        assertEquals(true, residue.defaultSelected)
        assertTrue(residue.reason.contains("Gradle 临时/锁文件残留"))
    }
}
