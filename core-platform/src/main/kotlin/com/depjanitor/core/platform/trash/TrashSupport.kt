package com.depjanitor.core.platform.trash

import java.awt.Desktop
import java.io.File
import java.nio.file.Path

interface TrashSupport {
    fun isSupported(): Boolean
    fun moveToTrash(path: Path): Boolean
}

class DesktopTrashSupport : TrashSupport {
    override fun isSupported(): Boolean {
        return runCatching {
            Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)
        }.getOrDefault(false)
    }

    override fun moveToTrash(path: Path): Boolean {
        return runCatching {
            if (!isSupported()) {
                false
            } else {
                Desktop.getDesktop().moveToTrash(path.toFile())
            }
        }.getOrDefault(false)
    }
}
