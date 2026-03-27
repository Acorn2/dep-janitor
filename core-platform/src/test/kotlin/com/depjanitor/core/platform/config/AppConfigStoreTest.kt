package com.depjanitor.core.platform.config

import com.depjanitor.core.model.AppSettings
import com.depjanitor.core.model.PathKind
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
        )

        store.save(settings)
        val loaded = store.load()

        assertEquals("Ivory", loaded.themeModeName)
        assertEquals("/tmp/.m2/repository", loaded.pathOverrides[PathKind.MAVEN_REPOSITORY])
        assertEquals("/tmp/.gradle/caches", loaded.pathOverrides[PathKind.GRADLE_CACHES])
    }
}
