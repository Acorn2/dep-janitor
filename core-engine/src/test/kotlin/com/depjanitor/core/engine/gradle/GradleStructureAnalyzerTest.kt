package com.depjanitor.core.engine.gradle

import java.nio.file.Files
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

        val entries = analyzer.analyzeCaches(root)

        assertEquals(2, entries.size)
        assertEquals("gradle-cache:modules-2", entries.first().coordinate)
        assertTrue(entries.first().totalSizeBytes >= entries.last().totalSizeBytes)
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
}
