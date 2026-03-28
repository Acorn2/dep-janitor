package com.depjanitor.core.model

data class AppSettings(
    val themeModeName: String = "Obsidian",
    val pathOverrides: Map<PathKind, String> = emptyMap(),
    val customScanPaths: List<CustomScanPath> = emptyList(),
    val cleanupRuleSet: CleanupRuleSet = CleanupRuleSet(),
    val scanCustomPaths: Boolean = true,
    val whitelistEntries: List<WhitelistEntry> = emptyList(),
    val projectProtectionPaths: List<String> = emptyList(),
)
