package com.depjanitor.core.platform.path

import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class PathPresentationService {

    fun openInFileManager(pathString: String): Boolean {
        return runCatching {
            val path = Path.of(pathString)
            if (!path.exists() || !Desktop.isDesktopSupported()) return false
            val desktop = Desktop.getDesktop()
            val target = if (path.isRegularFile()) path.parent ?: path else path
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(target.toFile())
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
