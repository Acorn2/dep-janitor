package com.depjanitor.core.engine.protection

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import org.w3c.dom.Element

class ProjectReferenceProtectionService {

    fun collectProtectedCoordinates(projectPaths: List<String>): Set<String> {
        return projectPaths.asSequence()
            .mapNotNull { path -> path.takeIf { it.isNotBlank() }?.let(Path::of) }
            .filter { it.exists() && it.isDirectory() }
            .flatMap { collectBuildFiles(it).asSequence() }
            .flatMap { file ->
                when (file.name) {
                    "pom.xml" -> parsePomDependencies(file).asSequence()
                    "build.gradle", "build.gradle.kts" -> parseGradleDependencies(file).asSequence()
                    else -> emptySequence()
                }
            }
            .toSet()
    }

    private fun collectBuildFiles(projectRoot: Path): List<Path> = try {
        Files.walk(projectRoot, 4).use { walk ->
            walk.filter { Files.isRegularFile(it) && it.fileName.toString() in BUILD_FILE_NAMES }
                .toList()
        }
    } catch (_: IOException) {
        emptyList()
    }

    private fun parsePomDependencies(path: Path): Set<String> = try {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            isNamespaceAware = false
        }
        val document = factory.newDocumentBuilder().parse(path.toFile())
        val nodes = document.getElementsByTagName("dependency")
        buildSet {
            repeat(nodes.length) { index ->
                val node = nodes.item(index)
                val element = node as? Element ?: return@repeat
                val groupId = element.getChildText("groupId")
                val artifactId = element.getChildText("artifactId")
                if (!groupId.isNullOrBlank() && !artifactId.isNullOrBlank() && !groupId.contains("\${") && !artifactId.contains("\${")) {
                    add("$groupId:$artifactId")
                }
            }
        }
    } catch (_: Exception) {
        emptySet()
    }

    private fun parseGradleDependencies(path: Path): Set<String> = try {
        val content = Files.readString(path)
        COORDINATE_REGEX.findAll(content)
            .map { "${it.groupValues[1]}:${it.groupValues[2]}" }
            .toSet()
    } catch (_: IOException) {
        emptySet()
    }

    private fun Element.getChildText(tagName: String): String? {
        val nodes = getElementsByTagName(tagName)
        if (nodes.length == 0) return null
        return nodes.item(0)?.textContent?.trim()
    }

    companion object {
        private val BUILD_FILE_NAMES = setOf("pom.xml", "build.gradle", "build.gradle.kts")
        private val COORDINATE_REGEX = Regex("""["']([A-Za-z0-9_.\-]+):([A-Za-z0-9_.\-]+):[^"']+["']""")
    }
}
