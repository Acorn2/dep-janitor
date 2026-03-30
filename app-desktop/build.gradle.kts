plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

sourceSets {
    named("main") {
        resources {
            exclude(
                "**/.DS_Store",
                "icons/source/**",
                "icons/runtime/dep-janitor-16.png",
                "icons/runtime/dep-janitor-24.png",
                "icons/runtime/dep-janitor-32.png",
                "icons/runtime/dep-janitor-48.png",
                "icons/runtime/dep-janitor-128.png",
                "icons/runtime/dep-janitor-256.png",
                "icons/runtime/dep-janitor-1024.png",
            )
        }
    }
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-engine"))
    implementation(project(":core-platform"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

compose.desktop {
    application {
        mainClass = "com.depjanitor.app.MainKt"

        nativeDistributions {
            packageName = "Dep Janitor"
            packageVersion = "1.0.0"
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
            )
            modules("java.instrument", "jdk.unsupported")

            val macIcon = project.file("src/main/resources/icons/macos/dep-janitor.icns")
            if (macIcon.exists()) {
                macOS {
                    iconFile.set(macIcon)
                }
            }

            val windowsIcon = project.file("src/main/resources/icons/windows/dep-janitor.ico")
            if (windowsIcon.exists()) {
                windows {
                    iconFile.set(windowsIcon)
                }
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}
