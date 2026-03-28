package com.depjanitor.core.platform.config

import com.depjanitor.core.model.AppSettings
import com.depjanitor.core.model.CleanupRuleSet
import com.depjanitor.core.model.CustomScanPath
import com.depjanitor.core.model.PathKind
import com.depjanitor.core.model.WhitelistEntry
import com.depjanitor.core.model.WhitelistEntryType
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
        val customScanPaths = loadCustomScanPaths(properties)
        val whitelistEntries = properties.stringPropertyNames()
            .filter { it.startsWith(WHITELIST_PREFIX) }
            .sorted()
            .mapNotNull { key -> properties.getProperty(key)?.let(::parseWhitelistEntry) }
        val projectProtectionPaths = properties.stringPropertyNames()
            .filter { it.startsWith(PROJECT_PATH_PREFIX) }
            .sorted()
            .mapNotNull { key -> properties.getProperty(key)?.takeIf { it.isNotBlank() } }
        return AppSettings(
            themeModeName = properties.getProperty(THEME_KEY, "Obsidian"),
            pathOverrides = overrides,
            customScanPaths = customScanPaths,
            cleanupRuleSet = CleanupRuleSet(
                retainLatestVersions = properties.getProperty(RETAIN_LATEST_VERSIONS_KEY)?.toIntOrNull() ?: 2,
                unusedDaysThreshold = properties.getProperty(UNUSED_DAYS_THRESHOLD_KEY)?.toIntOrNull() ?: 180,
                deleteLastUpdatedFiles = properties.getProperty(DELETE_LAST_UPDATED_KEY)?.toBooleanStrictOrNull() ?: true,
                deleteFailedDownloads = properties.getProperty(DELETE_FAILED_DOWNLOADS_KEY)?.toBooleanStrictOrNull() ?: true,
                deleteStaleSnapshots = properties.getProperty(DELETE_STALE_SNAPSHOTS_KEY)?.toBooleanStrictOrNull() ?: true,
                prioritizeLowRisk = properties.getProperty(PRIORITIZE_LOW_RISK_KEY)?.toBooleanStrictOrNull() ?: true,
                moveToTrash = properties.getProperty(MOVE_TO_TRASH_KEY)?.toBooleanStrictOrNull() ?: true,
            ),
            scanCustomPaths = properties.getProperty(SCAN_CUSTOM_PATHS_KEY)?.toBooleanStrictOrNull() ?: true,
            whitelistEntries = whitelistEntries,
            projectProtectionPaths = projectProtectionPaths,
        )
    }

    fun save(settings: AppSettings) {
        configPath.parent?.createDirectories()
        val properties = Properties().apply {
            setProperty(THEME_KEY, settings.themeModeName)
            setProperty(RETAIN_LATEST_VERSIONS_KEY, settings.cleanupRuleSet.retainLatestVersions.toString())
            setProperty(UNUSED_DAYS_THRESHOLD_KEY, settings.cleanupRuleSet.unusedDaysThreshold.toString())
            setProperty(DELETE_LAST_UPDATED_KEY, settings.cleanupRuleSet.deleteLastUpdatedFiles.toString())
            setProperty(DELETE_FAILED_DOWNLOADS_KEY, settings.cleanupRuleSet.deleteFailedDownloads.toString())
            setProperty(DELETE_STALE_SNAPSHOTS_KEY, settings.cleanupRuleSet.deleteStaleSnapshots.toString())
            setProperty(PRIORITIZE_LOW_RISK_KEY, settings.cleanupRuleSet.prioritizeLowRisk.toString())
            setProperty(MOVE_TO_TRASH_KEY, settings.cleanupRuleSet.moveToTrash.toString())
            setProperty(SCAN_CUSTOM_PATHS_KEY, settings.scanCustomPaths.toString())
            settings.customScanPaths.forEachIndexed { index, entry ->
                setProperty(customPathKey(index, "kind"), entry.kind.name)
                setProperty(customPathKey(index, "enabled"), entry.enabled.toString())
                setProperty(customPathKey(index, "path"), entry.path)
            }
            settings.whitelistEntries.forEachIndexed { index, entry ->
                setProperty("$WHITELIST_PREFIX$index", "${entry.type.name}|${entry.value}")
            }
            settings.projectProtectionPaths.forEachIndexed { index, path ->
                if (path.isNotBlank()) setProperty("$PROJECT_PATH_PREFIX$index", path)
            }
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

    private fun customPathKey(index: Int, field: String): String = "$CUSTOM_PATH_PREFIX$index.$field"

    private fun loadCustomScanPaths(properties: Properties): List<CustomScanPath> {
        val indices = properties.stringPropertyNames()
            .mapNotNull { key ->
                CUSTOM_PATH_INDEX_REGEX.matchEntire(key)?.groupValues?.get(1)?.toIntOrNull()
            }
            .distinct()
            .sorted()

        return indices.mapNotNull { index ->
            val kind = properties.getProperty(customPathKey(index, "kind"))
                ?.let { runCatching { PathKind.valueOf(it) }.getOrNull() }
                ?: return@mapNotNull null
            val path = properties.getProperty(customPathKey(index, "path"))?.trim().orEmpty()
            if (path.isBlank()) return@mapNotNull null
            val enabled = properties.getProperty(customPathKey(index, "enabled"))?.toBooleanStrictOrNull() ?: true
            CustomScanPath(kind = kind, path = path, enabled = enabled)
        }
    }

    private fun parseWhitelistEntry(raw: String): WhitelistEntry? {
        val separatorIndex = raw.indexOf('|')
        if (separatorIndex <= 0) return null
        val typeName = raw.substring(0, separatorIndex)
        val value = raw.substring(separatorIndex + 1)
        val type = runCatching { WhitelistEntryType.valueOf(typeName) }.getOrNull() ?: return null
        if (value.isBlank()) return null
        return WhitelistEntry(type = type, value = value)
    }

    companion object {
        private const val THEME_KEY = "theme.mode"
        private const val RETAIN_LATEST_VERSIONS_KEY = "rules.retainLatestVersions"
        private const val UNUSED_DAYS_THRESHOLD_KEY = "rules.unusedDaysThreshold"
        private const val DELETE_LAST_UPDATED_KEY = "rules.deleteLastUpdatedFiles"
        private const val DELETE_FAILED_DOWNLOADS_KEY = "rules.deleteFailedDownloads"
        private const val DELETE_STALE_SNAPSHOTS_KEY = "rules.deleteStaleSnapshots"
        private const val PRIORITIZE_LOW_RISK_KEY = "rules.prioritizeLowRisk"
        private const val MOVE_TO_TRASH_KEY = "rules.moveToTrash"
        private const val SCAN_CUSTOM_PATHS_KEY = "scan.customPaths.enabled"
        private const val CUSTOM_PATH_PREFIX = "scan.customPath."
        private const val WHITELIST_PREFIX = "whitelist."
        private const val PROJECT_PATH_PREFIX = "protection.projectPath."
        private val CUSTOM_PATH_INDEX_REGEX = Regex("""scan\.customPath\.(\d+)\.(kind|enabled|path)""")

        private fun defaultConfigPath(): Path {
            return Paths.get(System.getProperty("user.home"), ".dep-janitor", "config.properties")
        }
    }
}
