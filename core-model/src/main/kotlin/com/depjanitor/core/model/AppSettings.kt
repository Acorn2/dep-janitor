package com.depjanitor.core.model

data class AppSettings(
    val themeModeName: String = "Obsidian",
    val pathOverrides: Map<PathKind, String> = emptyMap(),
)
