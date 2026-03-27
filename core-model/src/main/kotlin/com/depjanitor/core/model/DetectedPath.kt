package com.depjanitor.core.model

data class DetectedPath(
    val kind: PathKind,
    val path: String,
    val exists: Boolean,
    val autoDetected: Boolean = true,
)
