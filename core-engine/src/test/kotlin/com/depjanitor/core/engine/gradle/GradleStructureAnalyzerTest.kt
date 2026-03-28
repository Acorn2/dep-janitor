package com.depjanitor.core.engine.gradle

import com.depjanitor.core.model.TimeBasis
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GradleStructureAnalyzerTest {

    private val analyzer = GradleStructureAnalyzer()

    @Test
    fun `should parse gradle cache roots as entries`() {
        val root = Files.createTempDirectory("gradle-caches")
        root.resolve("modules-2").createDirectories().resolve("a.bin").writeBytes(ByteArray(70))
        root.resolve("transforms-3").createDirectories().resolve("b.bin").writeBytes(ByteArray(30))
        root.resolve("notifications-1").createDirectories().resolve("notice.bin").writeBytes(ByteArray(10))

        val entries = analyzer.analyzeCaches(root)

        assertEquals(3, entries.size)
        assertEquals("gradle-cache:modules-2", entries.first().coordinate)
        assertTrue(entries.first().totalSizeBytes >= entries.last().totalSizeBytes)
        assertEquals("inspect", entries.first().versions.single().state)
        assertEquals("residue", entries.first { it.coordinate == "gradle-cache:transforms-3" }.versions.single().state)
        assertEquals("residue", entries.first { it.coordinate == "gradle-cache:notifications-1" }.versions.single().state)
    }

    @Test
    fun `should parse wrapper dists as high risk entries`() {
        val root = Files.createTempDirectory("gradle-wrapper")
        root.resolve("dists/gradle-8.7-bin").createDirectories().resolve("gradle.zip").writeBytes(ByteArray(50))

        val entries = analyzer.analyzeWrapper(root)

        assertEquals(1, entries.size)
        assertEquals("gradle-wrapper:gradle-8.7-bin", entries.single().coordinate)
        assertEquals("caution", entries.single().versions.single().state)
    }

    @Test
    fun `should mark fallback when access time is unavailable for gradle cache`() {
        val root = Files.createTempDirectory("gradle-cache-time")
        val file = root.resolve("modules-2").createDirectories().resolve("module.bin")
        file.writeBytes(ByteArray(24))
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_750_000_000_000L))
        Files.setAttribute(file, "basic:lastAccessTime", FileTime.fromMillis(1_750_000_000_000L))

        val entry = analyzer.analyzeCaches(root).single()

        assertEquals(TimeBasis.LAST_MODIFIED, entry.timeBasis)
        assertEquals(true, entry.timeBasisFallback)
    }
}
