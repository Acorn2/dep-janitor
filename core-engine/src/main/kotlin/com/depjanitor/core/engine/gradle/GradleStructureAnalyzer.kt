package com.depjanitor.core.engine.gradle

import com.depjanitor.core.engine.time.UsageTimeResolver
import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.TimeBasis
import com.depjanitor.core.model.VersionScanEntry
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class GradleStructureAnalyzer {

    fun analyzeCaches(root: Path): List<ArtifactScanEntry> {
        if (!root.exists() || !root.isDirectory()) return emptyList()
        return immediateChildren(root).map { child ->
            val size = directorySize(child)
            val classification = classifyCache(child.name)
            val usageTime = UsageTimeResolver.resolve(child)
            ArtifactScanEntry(
                coordinate = "gradle-cache:${child.name}",
                source = ArtifactSource.GRADLE,
                artifact = child.name,
                path = child.toString(),
                totalSizeBytes = size,
                lastModifiedMillis = usageTime.millis,
                timeBasis = usageTime.basis,
                timeBasisFallback = usageTime.isFallback,
                versions = listOf(
                    VersionScanEntry(
                        label = child.name,
                        sizeBytes = size,
                        lastModifiedMillis = usageTime.millis,
                        timeBasis = usageTime.basis,
                        timeBasisFallback = usageTime.isFallback,
                        source = ArtifactSource.GRADLE,
                        path = child.toString(),
                        riskLevel = classification.riskLevel,
                        state = classification.state,
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
            val usageTime = UsageTimeResolver.resolve(child)
            ArtifactScanEntry(
                coordinate = "gradle-wrapper:${child.name}",
                source = ArtifactSource.WRAPPER,
                artifact = child.name,
                path = child.toString(),
                totalSizeBytes = size,
                lastModifiedMillis = usageTime.millis,
                timeBasis = usageTime.basis,
                timeBasisFallback = usageTime.isFallback,
                versions = listOf(
                    VersionScanEntry(
                        label = child.name,
                        sizeBytes = size,
                        lastModifiedMillis = usageTime.millis,
                        timeBasis = usageTime.basis,
                        timeBasisFallback = usageTime.isFallback,
                        source = ArtifactSource.WRAPPER,
                        path = child.toString(),
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
    private fun classifyCache(name: String): CacheClassification = when {
        name.startsWith("transforms-") -> CacheClassification(RiskLevel.LOW, "residue")
        name.startsWith("build-cache-") -> CacheClassification(RiskLevel.LOW, "residue")
        name.startsWith("notifications") -> CacheClassification(RiskLevel.LOW, "residue")
        name.startsWith("journal-") -> CacheClassification(RiskLevel.LOW, "residue")
        name.startsWith("workers") -> CacheClassification(RiskLevel.LOW, "review")
        name.startsWith("expanded") -> CacheClassification(RiskLevel.LOW, "review")
        name.startsWith("jars-") -> CacheClassification(RiskLevel.MEDIUM, "review")
        name.startsWith("modules-") -> CacheClassification(RiskLevel.MEDIUM, "inspect")
        name.startsWith("fileHashes") -> CacheClassification(RiskLevel.HIGH, "caution")
        name.startsWith("kotlin-dsl") -> CacheClassification(RiskLevel.HIGH, "caution")
        else -> CacheClassification(RiskLevel.MEDIUM, "inspect")
    }

    private data class CacheClassification(
        val riskLevel: RiskLevel,
        val state: String,
    )
}
