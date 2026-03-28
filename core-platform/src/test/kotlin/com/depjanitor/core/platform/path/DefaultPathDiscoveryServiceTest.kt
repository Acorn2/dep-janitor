package com.depjanitor.core.platform.path

import com.depjanitor.core.model.CustomScanPath
import com.depjanitor.core.model.PathKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DefaultPathDiscoveryServiceTest {

    private val service = DefaultPathDiscoveryService()

    @Test
    fun `should resolve unix style default paths`() {
        val paths = service.discover(
            osName = "Mac OS X",
            userHome = "/Users/ankanghao",
            env = emptyMap(),
        )

        assertEquals("/Users/ankanghao/.m2/repository", paths.first { it.kind == PathKind.MAVEN_REPOSITORY }.path)
        assertEquals("/Users/ankanghao/.gradle/caches", paths.first { it.kind == PathKind.GRADLE_CACHES }.path)
        assertEquals("/Users/ankanghao/.gradle/wrapper", paths.first { it.kind == PathKind.GRADLE_WRAPPER }.path)
    }

    @Test
    fun `should resolve windows style default paths`() {
        val paths = service.discover(
            osName = "Windows 11",
            userHome = "C:/Users/fallback",
            env = mapOf("USERPROFILE" to "C:/Users/dev"),
        )

        assertEquals("C:/Users/dev/.m2/repository", paths.first { it.kind == PathKind.MAVEN_REPOSITORY }.path.replace('\\', '/'))
        assertEquals("C:/Users/dev/.gradle/caches", paths.first { it.kind == PathKind.GRADLE_CACHES }.path.replace('\\', '/'))
        assertEquals("C:/Users/dev/.gradle/wrapper", paths.first { it.kind == PathKind.GRADLE_WRAPPER }.path.replace('\\', '/'))
    }

    @Test
    fun `should prefer custom override when provided`() {
        val paths = service.discover(
            osName = "Mac OS X",
            userHome = "/Users/ankanghao",
            env = emptyMap(),
            pathOverrides = mapOf(PathKind.MAVEN_REPOSITORY to "/custom/m2/repository"),
        )

        val overridden = paths.first { it.kind == PathKind.MAVEN_REPOSITORY }
        assertEquals("/custom/m2/repository", overridden.path)
        assertFalse(overridden.autoDetected)
    }

    @Test
    fun `should include enabled custom scan paths and skip disabled ones`() {
        val paths = service.discover(
            osName = "Mac OS X",
            userHome = "/Users/ankanghao",
            env = emptyMap(),
            customScanPaths = listOf(
                CustomScanPath(PathKind.MAVEN_REPOSITORY, "/Volumes/extra/m2", enabled = true),
                CustomScanPath(PathKind.GRADLE_CACHES, "/Volumes/extra/gradle", enabled = false),
            ),
        )

        assertEquals(true, paths.any { it.kind == PathKind.MAVEN_REPOSITORY && it.path == "/Volumes/extra/m2" })
        assertEquals(false, paths.any { it.kind == PathKind.GRADLE_CACHES && it.path == "/Volumes/extra/gradle" })
    }
}
