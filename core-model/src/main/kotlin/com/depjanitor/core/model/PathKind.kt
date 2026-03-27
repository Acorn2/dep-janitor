package com.depjanitor.core.model

enum class PathKind(val label: String, val source: ArtifactSource) {
    MAVEN_REPOSITORY("Maven Repository", ArtifactSource.MAVEN),
    GRADLE_CACHES("Gradle Caches", ArtifactSource.GRADLE),
    GRADLE_WRAPPER("Gradle Wrapper", ArtifactSource.WRAPPER),
}
