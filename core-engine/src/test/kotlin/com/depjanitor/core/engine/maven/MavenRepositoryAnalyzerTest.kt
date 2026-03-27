package com.depjanitor.core.engine.maven

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MavenRepositoryAnalyzerTest {

    private val analyzer = MavenRepositoryAnalyzer()

    @Test
    fun `should aggregate artifact versions from maven repository`() {
        val root = Files.createTempDirectory("maven-repo")
        val artifactRoot = root.resolve("org/slf4j/slf4j-api")
        artifactRoot.resolve("2.0.13").createDirectories().resolve("slf4j-api-2.0.13.jar").writeBytes(ByteArray(100))
        artifactRoot.resolve("2.0.9").createDirectories().resolve("slf4j-api-2.0.9.jar").writeBytes(ByteArray(80))
        artifactRoot.resolve("1.7.36").createDirectories().resolve("slf4j-api-1.7.36.jar").writeBytes(ByteArray(60))

        val entries = analyzer.analyze(root)
        val target = entries.single()

        assertEquals("org.slf4j:slf4j-api", target.coordinate)
        assertEquals(3, target.versions.size)
        assertEquals(240L, target.totalSizeBytes)
        assertEquals("2.0.13", target.versions.first().label)
        assertEquals("candidate", target.versions.last().state)
        assertTrue(target.versions.last().sizeBytes > 0)
    }
}
