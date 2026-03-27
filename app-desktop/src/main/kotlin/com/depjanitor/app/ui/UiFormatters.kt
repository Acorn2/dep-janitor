package com.depjanitor.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ln
import kotlin.math.pow

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return String.format("%.1f %s", value, units[digitGroups])
}

fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return "未知"
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)
}
