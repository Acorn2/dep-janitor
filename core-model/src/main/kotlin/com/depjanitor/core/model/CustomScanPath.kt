package com.depjanitor.core.model

data class CustomScanPath(
    val kind: PathKind,
    val path: String,
    val enabled: Boolean = true,
)
