plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-platform"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
