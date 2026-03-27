package com.depjanitor.core.engine.cleanup

import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.CleanupCandidate
import com.depjanitor.core.model.CleanupCandidateKind
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.RecipePreview
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.SimulationPreview
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class WorkspaceCleanupAdvisorService(
    private val nowMillis: Long = System.currentTimeMillis(),
) {

    fun defaultRuleSet(): CleanupRuleSet = CleanupRuleSet()

    fun analyzeCandidates(
        detectedPaths: List<DetectedPath>,
        artifactEntries: List<ArtifactScanEntry>,
        ruleSet: CleanupRuleSet = defaultRuleSet(),
    ): List<CleanupCandidate> {
        val candidates = buildList {
            addAll(analyzeMavenVersionCandidates(artifactEntries, ruleSet))
            addAll(analyzeGradleCacheCandidates(artifactEntries, ruleSet))
            addAll(analyzeWrapperCandidates(artifactEntries))
            addAll(scanResidueFiles(detectedPaths, ruleSet))
        }
        return candidates.distinctBy { it.id }
            .sortedWith(
                compareByDescending<CleanupCandidate> { riskPriority(it.riskLevel) }
                    .thenByDescending { it.defaultSelected }
                    .thenByDescending { it.sizeBytes }
                    .thenBy { it.coordinate.lowercase() },
            )
    }

    fun buildRecipes(candidates: List<CleanupCandidate>): List<RecipePreview> {
        if (candidates.isEmpty()) return emptyList()
        return candidates.groupBy { it.kind }
            .map { (kind, items) ->
                val riskLevel = items.maxByOrNull { riskPriority(it.riskLevel) }?.riskLevel ?: RiskLevel.LOW
                RecipePreview(
                    title = recipeTitle(kind),
                    description = kind.description,
                    riskLevel = riskLevel,
                    source = dominantSource(items),
                    hitCount = items.size,
                    releasableBytes = items.sumOf { it.sizeBytes },
                    defaultEnabled = items.any { it.defaultSelected },
                )
            }
            .sortedWith(
                compareByDescending<RecipePreview> { riskPriority(it.riskLevel) }
                    .thenByDescending { it.releasableBytes },
            )
    }

    fun buildSimulation(
        candidates: List<CleanupCandidate>,
        artifactEntries: List<ArtifactScanEntry>,
        ruleSet: CleanupRuleSet = defaultRuleSet(),
    ): SimulationPreview {
        val defaultSelection = candidates.filter { it.defaultSelected }
        val protectedMavenCount = artifactEntries.filter { it.source == ArtifactSource.MAVEN }
            .sumOf { entry -> entry.versions.take(ruleSet.retainLatestVersions).size }
        val protectedWrapperCount = artifactEntries.count { it.source == ArtifactSource.WRAPPER }
            .coerceAtMost(1)
        val protectedCount = protectedMavenCount + protectedWrapperCount
        return SimulationPreview(
            releasableBytes = defaultSelection.sumOf { it.sizeBytes },
            selectedItemCount = defaultSelection.size,
            highRiskCount = candidates.count { it.riskLevel == RiskLevel.HIGH },
            protectedRuleCount = protectedCount,
        )
    }

    private fun analyzeMavenVersionCandidates(
        artifactEntries: List<ArtifactScanEntry>,
        ruleSet: CleanupRuleSet,
    ): List<CleanupCandidate> {
        return artifactEntries.asSequence()
            .filter { it.source == ArtifactSource.MAVEN }
            .flatMap { entry ->
                entry.versions.drop(ruleSet.retainLatestVersions).asSequence().map { version ->
                    val stale = isOlderThanThreshold(version.lastModifiedMillis, ruleSet.unusedDaysThreshold)
                    val isSnapshot = version.label.contains("SNAPSHOT", ignoreCase = true)
                    val kind = if (isSnapshot && ruleSet.deleteStaleSnapshots && stale) {
                        CleanupCandidateKind.STALE_SNAPSHOT
                    } else {
                        CleanupCandidateKind.OLD_VERSION
                    }
                    CleanupCandidate(
                        id = "${kind.name}:${entry.coordinate}:${version.label}",
                        kind = kind,
                        coordinate = entry.coordinate,
                        versionLabel = version.label,
                        source = ArtifactSource.MAVEN,
                        riskLevel = when {
                            kind == CleanupCandidateKind.STALE_SNAPSHOT -> RiskLevel.LOW
                            stale -> RiskLevel.MEDIUM
                            else -> RiskLevel.MEDIUM
                        },
                        sizeBytes = version.sizeBytes,
                        lastModifiedMillis = version.lastModifiedMillis,
                        reason = buildString {
                            append("超出保留最近 ${ruleSet.retainLatestVersions} 个版本")
                            if (stale) append("，且已超过 ${ruleSet.unusedDaysThreshold} 天未更新")
                        },
                        defaultSelected = kind == CleanupCandidateKind.STALE_SNAPSHOT,
                    )
                }
            }
            .toList()
    }

    private fun analyzeGradleCacheCandidates(
        artifactEntries: List<ArtifactScanEntry>,
        ruleSet: CleanupRuleSet,
    ): List<CleanupCandidate> {
        return artifactEntries.asSequence()
            .filter { it.source == ArtifactSource.GRADLE }
            .filter { isOlderThanThreshold(it.lastModifiedMillis, ruleSet.unusedDaysThreshold) }
            .map { entry ->
                CleanupCandidate(
                    id = "${CleanupCandidateKind.STALE_GRADLE_CACHE.name}:${entry.coordinate}",
                    kind = CleanupCandidateKind.STALE_GRADLE_CACHE,
                    coordinate = entry.coordinate,
                    versionLabel = entry.versions.firstOrNull()?.label,
                    source = ArtifactSource.GRADLE,
                    riskLevel = RiskLevel.MEDIUM,
                    sizeBytes = entry.totalSizeBytes,
                    lastModifiedMillis = entry.lastModifiedMillis,
                    reason = "Gradle cache 超过 ${ruleSet.unusedDaysThreshold} 天未更新，建议人工确认后清理",
                    defaultSelected = false,
                )
            }
            .toList()
    }

    private fun analyzeWrapperCandidates(artifactEntries: List<ArtifactScanEntry>): List<CleanupCandidate> {
        val wrappers = artifactEntries.filter { it.source == ArtifactSource.WRAPPER }
            .sortedByDescending { wrapperScore(it.versions.firstOrNull()?.label ?: it.coordinate) }

        return wrappers.drop(1).map { entry ->
            val version = entry.versions.firstOrNull()
            CleanupCandidate(
                id = "${CleanupCandidateKind.LEGACY_WRAPPER.name}:${entry.coordinate}",
                kind = CleanupCandidateKind.LEGACY_WRAPPER,
                coordinate = entry.coordinate,
                versionLabel = version?.label,
                source = ArtifactSource.WRAPPER,
                riskLevel = RiskLevel.HIGH,
                sizeBytes = entry.totalSizeBytes,
                lastModifiedMillis = entry.lastModifiedMillis,
                reason = "检测到更新的 wrapper distribution，旧版本默认不自动勾选",
                defaultSelected = false,
            )
        }
    }

    private fun scanResidueFiles(
        detectedPaths: List<DetectedPath>,
        ruleSet: CleanupRuleSet,
    ): List<CleanupCandidate> {
        val mavenRoots = detectedPaths.filter { it.kind == PathKind.MAVEN_REPOSITORY && it.exists }.map { Path.of(it.path) }
        return mavenRoots.flatMap { root ->
            val lastUpdated = if (ruleSet.deleteLastUpdatedFiles) {
                scanFiles(root) { file -> file.name.endsWith(".lastUpdated") }.map { file ->
                    fileCandidate(
                        kind = CleanupCandidateKind.LAST_UPDATED_RESIDUE,
                        file = file,
                        root = root,
                        reason = "Maven 下载状态文件，可安全清理",
                    )
                }
            } else {
                emptyList()
            }

            val failedDownloadResidues = if (ruleSet.deleteFailedDownloads) {
                scanFiles(root) { file ->
                    val lowerName = file.name.lowercase()
                    lowerName.endsWith(".part") || lowerName.endsWith(".tmp") || lowerName.endsWith(".part.lock")
                }.map { file ->
                    fileCandidate(
                        kind = CleanupCandidateKind.FAILED_DOWNLOAD_RESIDUE,
                        file = file,
                        root = root,
                        reason = "检测到失败下载残留文件",
                    )
                }
            } else {
                emptyList()
            }

            lastUpdated + failedDownloadResidues
        }
    }

    private fun fileCandidate(
        kind: CleanupCandidateKind,
        file: Path,
        root: Path,
        reason: String,
    ): CleanupCandidate {
        val relativePath = runCatching { root.relativize(file).toString() }.getOrDefault(file.fileName.toString())
        return CleanupCandidate(
            id = "${kind.name}:${file.normalize()}",
            kind = kind,
            coordinate = relativePath,
            versionLabel = file.fileName.toString(),
            source = ArtifactSource.MAVEN,
            riskLevel = RiskLevel.LOW,
            sizeBytes = fileSize(file),
            lastModifiedMillis = lastModified(file),
            reason = reason,
            path = file.toString(),
            defaultSelected = true,
        )
    }

    private fun scanFiles(root: Path, predicate: (Path) -> Boolean): List<Path> = try {
        Files.walk(root).use { walk ->
            walk.filter { it.isRegularFile() && predicate(it) }
                .limit(2_000)
                .toList()
        }
    } catch (_: IOException) {
        emptyList()
    }

    private fun fileSize(path: Path): Long = try {
        Files.size(path)
    } catch (_: IOException) {
        0L
    }

    private fun lastModified(path: Path): Long = try {
        Files.getLastModifiedTime(path).toMillis()
    } catch (_: IOException) {
        0L
    }

    private fun isOlderThanThreshold(lastModifiedMillis: Long, thresholdDays: Int): Boolean {
        if (lastModifiedMillis <= 0L) return false
        val ageMillis = nowMillis - lastModifiedMillis
        return ageMillis >= thresholdDays * MILLIS_PER_DAY
    }

    private fun recipeTitle(kind: CleanupCandidateKind): String = when (kind) {
        CleanupCandidateKind.OLD_VERSION -> "Version Retain"
        CleanupCandidateKind.STALE_SNAPSHOT -> "Snapshot Sweep"
        CleanupCandidateKind.LAST_UPDATED_RESIDUE -> "Residue Sweep"
        CleanupCandidateKind.FAILED_DOWNLOAD_RESIDUE -> "Download Debris Sweep"
        CleanupCandidateKind.STALE_GRADLE_CACHE -> "Gradle Cache Review"
        CleanupCandidateKind.LEGACY_WRAPPER -> "Wrapper Caution"
    }

    private fun dominantSource(items: List<CleanupCandidate>): ArtifactSource {
        return items.groupingBy { it.source }.eachCount().maxByOrNull { it.value }?.key ?: ArtifactSource.MAVEN
    }

    private fun wrapperScore(label: String): Long {
        val numericParts = Regex("""\d+""").findAll(label).map { it.value.toLong() }.toList()
        var score = 0L
        numericParts.take(4).forEach { part -> score = score * 1_000L + part.coerceAtMost(999) }
        return score
    }

    private fun riskPriority(level: RiskLevel): Int = when (level) {
        RiskLevel.HIGH -> 4
        RiskLevel.MEDIUM -> 3
        RiskLevel.LOW -> 2
        RiskLevel.PROTECTED -> 1
    }

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
    }
}
