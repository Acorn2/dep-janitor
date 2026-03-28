package com.depjanitor.core.engine.delete

import com.depjanitor.core.model.CleanupExecutionEntry
import com.depjanitor.core.model.CleanupExecutionMode
import com.depjanitor.core.model.CleanupExecutionPlan
import com.depjanitor.core.model.CleanupExecutionResult
import com.depjanitor.core.model.CleanupExecutionStatus
import com.depjanitor.core.platform.trash.DesktopTrashSupport
import com.depjanitor.core.platform.trash.TrashSupport
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class WorkspaceCleanupExecutionService(
    private val trashSupport: TrashSupport = DesktopTrashSupport(),
) {

    fun execute(plan: CleanupExecutionPlan): CleanupExecutionResult {
        val entries = plan.items.map { item ->
            val path = Path.of(item.path)
            when {
                !path.exists() -> CleanupExecutionEntry(
                    item = item,
                    status = CleanupExecutionStatus.SKIPPED,
                    message = "目标不存在，可能已被清理",
                )

                plan.mode == CleanupExecutionMode.MOVE_TO_TRASH && trashSupport.moveToTrash(path) -> CleanupExecutionEntry(
                    item = item,
                    status = CleanupExecutionStatus.TRASHED,
                )

                deletePath(path) -> CleanupExecutionEntry(
                    item = item,
                    status = CleanupExecutionStatus.DELETED,
                    message = if (plan.mode == CleanupExecutionMode.MOVE_TO_TRASH && !trashSupport.isSupported()) {
                        "当前平台不可用回收站，已回退为直接删除"
                    } else if (plan.mode == CleanupExecutionMode.MOVE_TO_TRASH) {
                        "移入回收站失败，已回退为直接删除"
                    } else {
                        null
                    },
                )

                else -> CleanupExecutionEntry(
                    item = item,
                    status = CleanupExecutionStatus.FAILED,
                    message = "删除失败，请检查文件权限或占用状态",
                )
            }
        }
        return CleanupExecutionResult(mode = plan.mode, entries = entries)
    }

    private fun deletePath(path: Path): Boolean = try {
        if (path.isDirectory()) {
            Files.walk(path).use { walk ->
                walk.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        } else {
            Files.deleteIfExists(path)
        }
        true
    } catch (_: IOException) {
        false
    }
}
