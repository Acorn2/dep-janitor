package com.depjanitor.core.engine.maven

import com.depjanitor.core.model.TimeBasis
import java.nio.file.Files
import java.nio.file.attribute.FileTime
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

    @Test
    fun `should prefer last access time when it is newer than last modified`() {
        val root = Files.createTempDirectory("maven-repo-access")
        val versionDir = root.resolve("org/example/demo/1.0.0")
        val jar = versionDir.createDirectories().resolve("demo-1.0.0.jar")
        jar.writeBytes(ByteArray(100))
        Files.setLastModifiedTime(jar, FileTime.fromMillis(1_700_000_000_000L))
        Files.setAttribute(jar, "basic:lastAccessTime", FileTime.fromMillis(1_800_000_000_000L))

        val entry = analyzer.analyze(root).single()
        val version = entry.versions.single()

        assertEquals(TimeBasis.LAST_ACCESSED, version.timeBasis)
        assertEquals(false, version.timeBasisFallback)
        assertEquals(1_800_000_000_000L, version.lastModifiedMillis)
    }
}
