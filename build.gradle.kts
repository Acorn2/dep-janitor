plugins {
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.compose") version "2.2.20" apply false
    id("org.jetbrains.compose") version "1.9.1" apply false
}

allprojects {
    group = "com.depjanitor"
    version = "1.0.0"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }
}
