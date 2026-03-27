package com.depjanitor.core.model

data class WorkspaceMetrics(
    val totalBytes: Long,
    val mavenBytes: Long,
    val gradleBytes: Long,
    val wrapperBytes: Long,
    val hotspots: List<HotspotEntry>,
)

data class ScanProgress(
    val phase: String,
    val currentTarget: String? = null,
    val completedTargets: Int = 0,
    val totalTargets: Int = 0,
)
