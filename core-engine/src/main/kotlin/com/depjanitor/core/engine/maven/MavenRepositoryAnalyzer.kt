package com.depjanitor.core.engine.maven

import com.depjanitor.core.engine.time.UsageTimeResolver
import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.VersionScanEntry
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class MavenRepositoryAnalyzer {

    fun analyze(root: Path): List<ArtifactScanEntry> {
        if (!root.exists() || !root.isDirectory()) return emptyList()
        return listDirectories(root).flatMap { child -> scanGroup(child, emptyList()) }
            .sortedByDescending { it.totalSizeBytes }
    }

    private fun scanGroup(current: Path, groupSegments: List<String>): List<ArtifactScanEntry> {
        val children = listDirectories(current)
        if (children.isEmpty()) return emptyList()

        val versionDirectories = children.filter { child -> looksLikeVersionDirectory(child.name) }
        return if (versionDirectories.isNotEmpty()) {
            val artifact = current.fileName.toString()
            val group = groupSegments.joinToString(".")
            val coordinate = if (group.isBlank()) artifact else "$group:$artifact"
            val sortedVersions = versionDirectories.sortedByDescending { versionScore(it.name) }
            val layers = sortedVersions.mapIndexed { index, versionDir ->
                val keep = index < 2
                val usageTime = UsageTimeResolver.resolve(versionDir)
                VersionScanEntry(
                    label = versionDir.name,
                    sizeBytes = directorySize(versionDir),
                    lastModifiedMillis = usageTime.millis,
                    timeBasis = usageTime.basis,
                    timeBasisFallback = usageTime.isFallback,
                    source = ArtifactSource.MAVEN,
                    path = versionDir.toString(),
                    riskLevel = if (keep) null else RiskLevel.MEDIUM,
                    state = if (keep) "keep" else "candidate",
                )
            }
            val latestLayer = layers.maxByOrNull { it.lastModifiedMillis }
            listOf(
                ArtifactScanEntry(
                    coordinate = coordinate,
                    source = ArtifactSource.MAVEN,
                    group = group.ifBlank { null },
                    artifact = artifact,
                    path = current.toString(),
                    totalSizeBytes = layers.sumOf { it.sizeBytes },
                    lastModifiedMillis = latestLayer?.lastModifiedMillis ?: 0L,
                    timeBasis = latestLayer?.timeBasis ?: com.depjanitor.core.model.TimeBasis.UNKNOWN,
                    timeBasisFallback = latestLayer?.timeBasisFallback ?: true,
                    versions = layers,
                ),
            )
        } else {
            children.flatMap { child -> scanGroup(child, groupSegments + current.name) }
        }
    }

    private fun listDirectories(path: Path): List<Path> = try {
        Files.list(path).use { stream -> stream.filter { Files.isDirectory(it) }.toList() }
    } catch (_: IOException) {
        emptyList()
    }

    private fun looksLikeVersionDirectory(name: String): Boolean {
        return name.firstOrNull()?.isDigit() == true || name.contains("SNAPSHOT", ignoreCase = true)
    }

    private fun versionScore(name: String): Long {
        val numericParts = Regex("""\d+""").findAll(name).map { it.value.toLong() }.toList()
        var score = 0L
        numericParts.take(4).forEach { part -> score = score * 1_000L + part.coerceAtMost(999) }
        if (name.contains("SNAPSHOT", ignoreCase = true)) score -= 1
        return score
    }

    private fun directorySize(path: Path): Long = try {
        Files.walk(path).use { walk ->
            walk.filter { Files.isRegularFile(it) }.mapToLong { Files.size(it) }.sum()
        }
    } catch (_: IOException) {
        0L
    }
}
