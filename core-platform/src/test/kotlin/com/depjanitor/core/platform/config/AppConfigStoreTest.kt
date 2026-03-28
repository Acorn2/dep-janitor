package com.depjanitor.core.platform.config

import com.depjanitor.core.model.AppSettings
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.CustomScanPath
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.WhitelistEntry
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AppConfigStoreTest {

    @Test
    fun `should persist and load theme and path overrides`() {
        val tempFile = Files.createTempDirectory("depjanitor-config").resolve("config.properties")
        val store = AppConfigStore(tempFile)
        val settings = AppSettings(
            themeModeName = "Ivory",
            pathOverrides = mapOf(
                PathKind.MAVEN_REPOSITORY to "/tmp/.m2/repository",
                PathKind.GRADLE_CACHES to "/tmp/.gradle/caches",
            ),
            customScanPaths = listOf(
                CustomScanPath(PathKind.MAVEN_REPOSITORY, "/Volumes/cache-a/m2", enabled = true),
                CustomScanPath(PathKind.GRADLE_CACHES, "/Volumes/cache-b/gradle", enabled = false),
            ),
            cleanupRuleSet = CleanupRuleSet(
                retainLatestVersions = 4,
                unusedDaysThreshold = 365,
                deleteLastUpdatedFiles = false,
                deleteFailedDownloads = true,
                deleteStaleSnapshots = false,
                prioritizeLowRisk = false,
                moveToTrash = false,
            ),
            scanCustomPaths = false,
            whitelistEntries = listOf(
                WhitelistEntry.coordinate("org.example:demo"),
                WhitelistEntry.path("/tmp/.m2/repository/org/example/demo"),
            ),
            projectProtectionPaths = listOf(
                "/workspace/project-a",
                "/workspace/project-b",
            ),
        )

        store.save(settings)
        val loaded = store.load()

        assertEquals("Ivory", loaded.themeModeName)
        assertEquals("/tmp/.m2/repository", loaded.pathOverrides[PathKind.MAVEN_REPOSITORY])
        assertEquals("/tmp/.gradle/caches", loaded.pathOverrides[PathKind.GRADLE_CACHES])
        assertEquals(2, loaded.customScanPaths.size)
        assertEquals("/Volumes/cache-a/m2", loaded.customScanPaths[0].path)
        assertEquals(true, loaded.customScanPaths[0].enabled)
        assertEquals("/Volumes/cache-b/gradle", loaded.customScanPaths[1].path)
        assertEquals(false, loaded.customScanPaths[1].enabled)
        assertEquals(4, loaded.cleanupRuleSet.retainLatestVersions)
        assertEquals(365, loaded.cleanupRuleSet.unusedDaysThreshold)
        assertEquals(false, loaded.cleanupRuleSet.deleteLastUpdatedFiles)
        assertEquals(true, loaded.cleanupRuleSet.deleteFailedDownloads)
        assertEquals(false, loaded.cleanupRuleSet.deleteStaleSnapshots)
        assertEquals(false, loaded.cleanupRuleSet.prioritizeLowRisk)
        assertEquals(false, loaded.cleanupRuleSet.moveToTrash)
        assertEquals(false, loaded.scanCustomPaths)
        assertEquals(2, loaded.whitelistEntries.size)
        assertEquals("org.example:demo", loaded.whitelistEntries[0].value)
        assertEquals("/tmp/.m2/repository/org/example/demo", loaded.whitelistEntries[1].value)
        assertEquals(2, loaded.projectProtectionPaths.size)
        assertEquals("/workspace/project-a", loaded.projectProtectionPaths[0])
        assertEquals("/workspace/project-b", loaded.projectProtectionPaths[1])
    }
}
