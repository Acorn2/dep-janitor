package com.depjanitor.core.engine.gradle

import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.VersionScanEntry
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class GradleStructureAnalyzer {

    fun analyzeCaches(root: Path): List<ArtifactScanEntry> {
        if (!root.exists() || !root.isDirectory()) return emptyList()
        return immediateChildren(root).map { child ->
            val size = directorySize(child)
            ArtifactScanEntry(
                coordinate = "gradle-cache:${child.name}",
                source = ArtifactSource.GRADLE,
                totalSizeBytes = size,
                lastModifiedMillis = directoryLastModified(child),
                versions = listOf(
                    VersionScanEntry(
                        label = child.name,
                        sizeBytes = size,
                        lastModifiedMillis = directoryLastModified(child),
                        source = ArtifactSource.GRADLE,
                        riskLevel = RiskLevel.MEDIUM,
                        state = "inspect",
                    ),
                ),
            )
        }.sortedByDescending { it.totalSizeBytes }
    }

    fun analyzeWrapper(root: Path): List<ArtifactScanEntry> {
        if (!root.exists() || !root.isDirectory()) return emptyList()
        val dists = root.resolve("dists")
        val targets = if (dists.exists() && dists.isDirectory()) immediateChildren(dists) else immediateChildren(root)
        return targets.map { child ->
            val size = directorySize(child)
            ArtifactScanEntry(
                coordinate = "gradle-wrapper:${child.name}",
                source = ArtifactSource.WRAPPER,
                totalSizeBytes = size,
                lastModifiedMillis = directoryLastModified(child),
                versions = listOf(
                    VersionScanEntry(
                        label = child.name,
                        sizeBytes = size,
                        lastModifiedMillis = directoryLastModified(child),
                        source = ArtifactSource.WRAPPER,
                        riskLevel = RiskLevel.HIGH,
                        state = "caution",
                    ),
                ),
            )
        }.sortedByDescending { it.totalSizeBytes }
    }

    private fun immediateChildren(path: Path): List<Path> = try {
        Files.list(path).use { stream ->
            stream.filter { Files.isDirectory(it) || Files.isRegularFile(it) }.toList()
        }
    } catch (_: IOException) {
        emptyList()
    }

    private fun directorySize(path: Path): Long = try {
        if (Files.isRegularFile(path)) Files.size(path) else Files.walk(path).use { walk -> walk.filter { Files.isRegularFile(it) }.mapToLong { Files.size(it) }.sum() }
    } catch (_: IOException) {
        0L
    }

    private fun directoryLastModified(path: Path): Long = try {
        if (Files.isRegularFile(path)) Files.getLastModifiedTime(path).toMillis() else Files.walk(path).use { walk ->
            walk.filter { Files.isRegularFile(it) }
                .map { Files.getLastModifiedTime(it) }
                .max(compareBy<FileTime> { it.toMillis() })
                .orElse(null)?.toMillis() ?: 0L
        }
    } catch (_: IOException) {
        0L
    }
}
