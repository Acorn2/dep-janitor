package com.depjanitor.core.engine

import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.HotspotEntry
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.ScanProgress
import com.depjanitor.core.model.WorkspaceMetrics
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class WorkspaceMetricsScanner {

    fun scan(
        detectedPaths: List<DetectedPath>,
        onProgress: (ScanProgress) -> Unit = {},
    ): WorkspaceMetrics {
        val existingTargets = detectedPaths.filter { it.exists }
        if (existingTargets.isEmpty()) {
            return WorkspaceMetrics(
                totalBytes = 0L,
                mavenBytes = 0L,
                gradleBytes = 0L,
                wrapperBytes = 0L,
                hotspots = emptyList(),
            )
        }

        val hotspots = mutableListOf<HotspotEntry>()
        var mavenBytes = 0L
        var gradleBytes = 0L
        var wrapperBytes = 0L

        existingTargets.forEachIndexed { index, target ->
            onProgress(
                ScanProgress(
                    phase = "扫描目录",
                    currentTarget = target.path,
                    completedTargets = index,
                    totalTargets = existingTargets.size,
                ),
            )

            val root = Path.of(target.path)
            val rootSize = calculateSize(root)
            when (target.kind) {
                PathKind.MAVEN_REPOSITORY -> mavenBytes += rootSize
                PathKind.GRADLE_CACHES -> gradleBytes += rootSize
                PathKind.GRADLE_WRAPPER -> wrapperBytes += rootSize
            }
            hotspots += calculateHotspots(root, target.kind)
        }

        onProgress(
            ScanProgress(
                phase = "扫描完成",
                completedTargets = existingTargets.size,
                totalTargets = existingTargets.size,
            ),
        )

        return WorkspaceMetrics(
            totalBytes = mavenBytes + gradleBytes + wrapperBytes,
            mavenBytes = mavenBytes,
            gradleBytes = gradleBytes,
            wrapperBytes = wrapperBytes,
            hotspots = hotspots.sortedByDescending { it.sizeBytes }.take(8),
        )
    }

    private fun calculateHotspots(root: Path, kind: PathKind): List<HotspotEntry> {
        if (!root.exists() || !root.isDirectory()) return emptyList()
        return Files.list(root).use { children ->
            children
                .filter { child -> child.name.isNotBlank() }
                .map { child ->
                    HotspotEntry(
                        name = buildHotspotName(root, child),
                        source = kind.source,
                        sizeBytes = calculateSize(child),
                        path = child.toString(),
                    )
                }
                .toList()
        }
    }

    private fun buildHotspotName(root: Path, child: Path): String {
        val normalizedRoot = root.normalize().toString().replace('\\', '/')
        val userHome = System.getProperty("user.home")?.replace('\\', '/').orEmpty().trimEnd('/')
        val displayRoot = if (userHome.isNotBlank() && normalizedRoot == userHome) {
            "~"
        } else if (userHome.isNotBlank() && normalizedRoot.startsWith("$userHome/")) {
            "~/${normalizedRoot.removePrefix("$userHome/")}"
        } else {
            normalizedRoot
        }
        return "$displayRoot/${child.fileName}"
    }

    private fun calculateSize(path: Path): Long {
        if (!path.exists()) return 0L
        return try {
            if (!path.isDirectory()) {
                Files.size(path)
            } else {
                Files.walk(path).use { walk ->
                    walk
                        .filter { Files.isRegularFile(it) }
                        .mapToLong { file -> safeFileSize(file) }
                        .sum()
                }
            }
        } catch (_: IOException) {
            0L
        }
    }

    private fun safeFileSize(path: Path): Long {
        return try {
            Files.size(path)
        } catch (_: IOException) {
            0L
        }
    }
}
