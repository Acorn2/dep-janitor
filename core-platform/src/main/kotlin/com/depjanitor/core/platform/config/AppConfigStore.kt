package com.depjanitor.core.platform.config

import com.depjanitor.core.model.AppSettings
import com.depjanitor.core.model.PathKind
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class AppConfigStore(
    private val configPath: Path = defaultConfigPath(),
) {

    fun load(): AppSettings {
        if (!configPath.exists()) return AppSettings()
        val properties = Properties()
        inputStream(configPath).use { properties.load(it) }
        val overrides = PathKind.entries.mapNotNull { kind ->
            properties.getProperty(pathKey(kind))?.takeIf { it.isNotBlank() }?.let { kind to it }
        }.toMap()
        return AppSettings(
            themeModeName = properties.getProperty(THEME_KEY, "Obsidian"),
            pathOverrides = overrides,
        )
    }

    fun save(settings: AppSettings) {
        configPath.parent?.createDirectories()
        val properties = Properties().apply {
            setProperty(THEME_KEY, settings.themeModeName)
            PathKind.entries.forEach { kind ->
                val value = settings.pathOverrides[kind]
                if (value.isNullOrBlank()) {
                    remove(pathKey(kind))
                } else {
                    setProperty(pathKey(kind), value)
                }
            }
        }
        outputStream(configPath).use { properties.store(it, "Dep Janitor configuration") }
    }

    private fun inputStream(path: Path): InputStream = Files.newInputStream(path)

    private fun outputStream(path: Path): OutputStream = Files.newOutputStream(path)

    private fun pathKey(kind: PathKind): String = "paths.${kind.name}"

    companion object {
        private const val THEME_KEY = "theme.mode"

        private fun defaultConfigPath(): Path {
            return Paths.get(System.getProperty("user.home"), ".dep-janitor", "config.properties")
        }
    }
}
