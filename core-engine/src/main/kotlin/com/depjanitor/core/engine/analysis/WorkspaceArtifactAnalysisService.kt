package com.depjanitor.core.engine.analysis

import com.depjanitor.core.engine.gradle.GradleStructureAnalyzer
import com.depjanitor.core.engine.maven.MavenRepositoryAnalyzer
import com.depjanitor.core.model.ArtifactScanEntry
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.PathKind
import java.nio.file.Path

class WorkspaceArtifactAnalysisService(
    private val mavenAnalyzer: MavenRepositoryAnalyzer = MavenRepositoryAnalyzer(),
    private val gradleAnalyzer: GradleStructureAnalyzer = GradleStructureAnalyzer(),
) {

    fun analyze(detectedPaths: List<DetectedPath>): List<ArtifactScanEntry> {
        return detectedPaths.filter { it.exists }.flatMap { path ->
            when (path.kind) {
                PathKind.MAVEN_REPOSITORY -> mavenAnalyzer.analyze(Path.of(path.path))
                PathKind.GRADLE_CACHES -> gradleAnalyzer.analyzeCaches(Path.of(path.path))
                PathKind.GRADLE_WRAPPER -> gradleAnalyzer.analyzeWrapper(Path.of(path.path))
            }
        }.sortedByDescending { it.totalSizeBytes }
    }
}
