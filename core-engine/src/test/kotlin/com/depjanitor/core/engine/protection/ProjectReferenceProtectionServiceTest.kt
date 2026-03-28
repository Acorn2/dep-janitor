package com.depjanitor.core.engine.protection

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class ProjectReferenceProtectionServiceTest {

    @Test
    fun `should collect coordinates from pom and gradle builds`() {
        val root = Files.createTempDirectory("depjanitor-project-protection")
        root.resolve("project-a").createDirectories().resolve("pom.xml").writeText(
            """
            <project>
              <dependencies>
                <dependency>
                  <groupId>org.slf4j</groupId>
                  <artifactId>slf4j-api</artifactId>
                  <version>2.0.13</version>
                </dependency>
              </dependencies>
            </project>
            """.trimIndent(),
        )
        root.resolve("project-b").createDirectories().resolve("build.gradle.kts").writeText(
            """
            dependencies {
                implementation("com.squareup.okio:okio:3.9.0")
                testImplementation("junit:junit:4.13.2")
            }
            """.trimIndent(),
        )

        val service = ProjectReferenceProtectionService()
        val coordinates = service.collectProtectedCoordinates(
            listOf(
                root.resolve("project-a").toString(),
                root.resolve("project-b").toString(),
            ),
        )

        assertTrue("org.slf4j:slf4j-api" in coordinates)
        assertTrue("com.squareup.okio:okio" in coordinates)
        assertTrue("junit:junit" in coordinates)
    }
}
