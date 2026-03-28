package com.depjanitor.core.platform.path

import com.depjanitor.core.model.CustomScanPath
import com.depjanitor.core.model.DetectedPath
import com.depjanitor.core.model.PathKind
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DefaultPathDiscoveryService {

    fun discover(
        pathOverrides: Map<PathKind, String> = emptyMap(),
        customScanPaths: List<CustomScanPath> = emptyList(),
    ): List<DetectedPath> {
        val userHome = System.getProperty("user.home").orEmpty()
        val osName = System.getProperty("os.name").orEmpty()
        val env = System.getenv()
        return discover(
            osName = osName,
            userHome = userHome,
            env = env,
            pathOverrides = pathOverrides,
            customScanPaths = customScanPaths,
        )
    }

    fun discover(
        osName: String,
        userHome: String,
        env: Map<String, String>,
        pathOverrides: Map<PathKind, String> = emptyMap(),
        customScanPaths: List<CustomScanPath> = emptyList(),
    ): List<DetectedPath> {
        val normalizedOs = osName.lowercase()
        val defaults = if (normalizedOs.contains("win")) {
            discoverForWindows(env["USERPROFILE"].orEmpty().ifBlank { userHome })
        } else {
            discoverForUnix(userHome)
        }
        val resolvedDefaults = defaults.map { defaultPath ->
            val override = pathOverrides[defaultPath.kind]?.takeIf { it.isNotBlank() }
            if (override == null) defaultPath else path(defaultPath.kind, Paths.get(override), autoDetected = false)
        }
        val resolvedCustomPaths = customScanPaths.asSequence()
            .filter { it.enabled }
            .mapNotNull { entry ->
                entry.path.takeIf { it.isNotBlank() }?.let { path(entry.kind, Paths.get(it), autoDetected = false) }
            }
            .toList()
        return (resolvedDefaults + resolvedCustomPaths)
            .distinctBy { "${it.kind.name}:${it.path.lowercase()}" }
    }

    private fun discoverForUnix(userHome: String): List<DetectedPath> {
        return listOf(
            path(PathKind.MAVEN_REPOSITORY, Paths.get(userHome, ".m2", "repository")),
            path(PathKind.GRADLE_CACHES, Paths.get(userHome, ".gradle", "caches")),
            path(PathKind.GRADLE_WRAPPER, Paths.get(userHome, ".gradle", "wrapper")),
        )
    }

    private fun discoverForWindows(userProfile: String): List<DetectedPath> {
        return listOf(
            path(PathKind.MAVEN_REPOSITORY, Paths.get(userProfile, ".m2", "repository")),
            path(PathKind.GRADLE_CACHES, Paths.get(userProfile, ".gradle", "caches")),
            path(PathKind.GRADLE_WRAPPER, Paths.get(userProfile, ".gradle", "wrapper")),
        )
    }

    private fun path(kind: PathKind, path: Path, autoDetected: Boolean = true): DetectedPath {
        return DetectedPath(
            kind = kind,
            path = path.toString(),
            exists = Files.exists(path),
            autoDetected = autoDetected,
        )
    }
}
