package com.depjanitor.core.engine.time

import com.depjanitor.core.model.TimeBasis
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

data class UsageTimeEstimate(
    val millis: Long,
    val basis: TimeBasis,
    val isFallback: Boolean,
)

object UsageTimeResolver {

    fun resolve(path: Path): UsageTimeEstimate {
        return try {
            if (Files.isDirectory(path)) resolveDirectory(path) else resolveFile(path)
        } catch (_: IOException) {
            UsageTimeEstimate(0L, TimeBasis.UNKNOWN, true)
        }
    }

    private fun resolveDirectory(path: Path): UsageTimeEstimate {
        val fileEstimates = try {
            Files.walk(path).use { walk ->
                walk.filter { Files.isRegularFile(it) }
                    .map(::resolveFile)
                    .toList()
            }
        } catch (_: IOException) {
            emptyList()
        }

        return fileEstimates.maxByOrNull { it.millis }
            ?: runCatching { resolveAttributes(Files.readAttributes(path, BasicFileAttributes::class.java)) }
                .getOrElse { UsageTimeEstimate(0L, TimeBasis.UNKNOWN, true) }
    }

    private fun resolveFile(path: Path): UsageTimeEstimate {
        return runCatching {
            resolveAttributes(Files.readAttributes(path, BasicFileAttributes::class.java))
        }.getOrElse {
            UsageTimeEstimate(0L, TimeBasis.UNKNOWN, true)
        }
    }

    private fun resolveAttributes(attributes: BasicFileAttributes): UsageTimeEstimate {
        val accessMillis = attributes.lastAccessTime()?.toMillis() ?: 0L
        val modifiedMillis = attributes.lastModifiedTime()?.toMillis() ?: 0L
        return when {
            accessMillis > modifiedMillis + ACCESS_TIME_EPSILON_MILLIS -> {
                UsageTimeEstimate(accessMillis, TimeBasis.LAST_ACCESSED, false)
            }

            modifiedMillis > 0L -> {
                UsageTimeEstimate(modifiedMillis, TimeBasis.LAST_MODIFIED, accessMillis <= 0L || kotlin.math.abs(accessMillis - modifiedMillis) <= ACCESS_TIME_EPSILON_MILLIS)
            }

            accessMillis > 0L -> {
                UsageTimeEstimate(accessMillis, TimeBasis.DERIVED_USAGE, true)
            }

            else -> UsageTimeEstimate(0L, TimeBasis.UNKNOWN, true)
        }
    }

    private const val ACCESS_TIME_EPSILON_MILLIS = 1_000L
}
