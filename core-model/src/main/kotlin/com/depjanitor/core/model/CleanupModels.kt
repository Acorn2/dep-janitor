package com.depjanitor.core.model

data class CleanupRuleSet(
    val retainLatestVersions: Int = 2,
    val unusedDaysThreshold: Int = 180,
    val deleteLastUpdatedFiles: Boolean = true,
    val deleteFailedDownloads: Boolean = true,
    val deleteStaleSnapshots: Boolean = true,
    val prioritizeLowRisk: Boolean = true,
    val moveToTrash: Boolean = true,
)

enum class CleanupCandidateKind(val label: String, val description: String) {
    OLD_VERSION("旧版本", "超出保留版本数的历史版本"),
    STALE_SNAPSHOT("过期 SNAPSHOT", "长期未更新的 SNAPSHOT 版本"),
    LAST_UPDATED_RESIDUE(".lastUpdated", "Maven 下载状态残留文件"),
    FAILED_DOWNLOAD_RESIDUE("失败下载残留", "part / tmp / lock 等失败下载残留"),
    STALE_GRADLE_CACHE("长期未使用缓存", "长期未更新的 Gradle cache 目录"),
    LEGACY_WRAPPER("旧版 Wrapper", "旧的 Gradle wrapper distribution 或压缩包"),
}

data class CleanupCandidate(
    val id: String,
    val kind: CleanupCandidateKind,
    val coordinate: String,
    val versionLabel: String? = null,
    val source: ArtifactSource,
    val riskLevel: RiskLevel,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val reason: String,
    val path: String? = null,
    val defaultSelected: Boolean = false,
)
