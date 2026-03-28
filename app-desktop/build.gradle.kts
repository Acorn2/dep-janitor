plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-engine"))
    implementation(project(":core-platform"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
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
