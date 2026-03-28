package com.depjanitor.core.model

enum class PathKind(val label: String, val source: ArtifactSource) {
    MAVEN_REPOSITORY("Maven 本地仓库", ArtifactSource.MAVEN),
    GRADLE_CACHES("Gradle 缓存目录", ArtifactSource.GRADLE),
    GRADLE_WRAPPER("Gradle Wrapper", ArtifactSource.WRAPPER),
}
