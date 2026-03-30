package com.depjanitor.core.engine

import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.PathKind
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkspaceMetricsScannerTest {

    private val scanner = WorkspaceMetricsScanner()

    @Test
    fun `should aggregate bytes by source and rank hotspots`() {
        val root = Files.createTempDirectory("depjanitor-scan")
        val maven = root.resolve(".m2/repository").createDirectories()
        val gradleCaches = root.resolve(".gradle/caches").createDirectories()
        val wrapper = root.resolve(".gradle/wrapper").createDirectories()

        maven.resolve("com/example/a.jar").apply {
            parent.createDirectories()
            writeBytes(ByteArray(100))
        }
        maven.resolve("org/demo/b.jar").apply {
            parent.createDirectories()
            writeBytes(ByteArray(40))
        }
        gradleCaches.resolve("modules-2/files.bin").apply {
            parent.createDirectories()
            writeBytes(ByteArray(70))
        }
        wrapper.resolve("dists/gradle-8.7.zip").apply {
            parent.createDirectories()
            writeBytes(ByteArray(30))
        }

        val result = scanner.scan(
            listOf(
                DetectedPath(PathKind.MAVEN_REPOSITORY, maven.toString(), exists = true),
                DetectedPath(PathKind.GRADLE_CACHES, gradleCaches.toString(), exists = true),
                DetectedPath(PathKind.GRADLE_WRAPPER, wrapper.toString(), exists = true),
            ),
        )

        assertEquals(140L, result.mavenBytes)
        assertEquals(70L, result.gradleBytes)
        assertEquals(30L, result.wrapperBytes)
        assertEquals(240L, result.totalBytes)
        assertTrue(result.hotspots.isNotEmpty())
        assertTrue(result.hotspots.first().sizeBytes >= result.hotspots.last().sizeBytes)
        assertTrue(result.hotspots.any { it.name == "${maven.toString().replace('\\', '/')}/com" })
        assertTrue(result.hotspots.any { it.name == "${gradleCaches.toString().replace('\\', '/')}/modules-2" })
        assertTrue(result.hotspots.any { it.name == "${wrapper.toString().replace('\\', '/')}/dists" })
    }
}
