package com.depjanitor.core.engine.delete

import com.depjanitor.core.model.ArtifactSource
import com.depjanitor.core.model.CleanupExecutionItem
import com.depjanitor.core.model.CleanupExecutionMode
import com.depjanitor.core.model.CleanupExecutionPlan
import com.depjanitor.core.model.CleanupExecutionStatus
import com.depjanitor.core.model.RiskLevel
import com.depjanitor.core.model.releasedBytes
import com.depjanitor.core.platform.trash.TrashSupport
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkspaceCleanupExecutionServiceTest {

    @Test
    fun `should delete file directly when using direct mode`() {
        val file = Files.createTempDirectory("depjanitor-delete").resolve("old.jar")
        file.parent.createDirectories()
        file.writeBytes(ByteArray(16))

        val service = WorkspaceCleanupExecutionService(trashSupport = FakeTrashSupport(supported = false))
        val result = service.execute(
            CleanupExecutionPlan(
                items = listOf(
                    CleanupExecutionItem(
                        candidateId = "1",
                        coordinate = "org.example:demo",
                        path = file.toString(),
                        source = ArtifactSource.MAVEN,
                        riskLevel = RiskLevel.LOW,
                        sizeBytes = 16L,
                    ),
                ),
                mode = CleanupExecutionMode.DELETE_DIRECTLY,
            ),
        )

        assertFalse(file.exists())
        assertEquals(CleanupExecutionStatus.DELETED, result.entries.single().status)
        assertEquals(16L, result.releasedBytes)
    }

    @Test
    fun `should move to trash when supported`() {
        val file = Files.createTempDirectory("depjanitor-trash").resolve("old.jar")
        file.writeBytes(ByteArray(8))
        val trash = FakeTrashSupport(supported = true)
        val service = WorkspaceCleanupExecutionService(trashSupport = trash)

        val result = service.execute(
            CleanupExecutionPlan(
                items = listOf(
                    CleanupExecutionItem(
                        candidateId = "1",
                        coordinate = "org.example:demo",
                        path = file.toString(),
                        source = ArtifactSource.MAVEN,
                        riskLevel = RiskLevel.LOW,
                        sizeBytes = 8L,
                    ),
                ),
                mode = CleanupExecutionMode.MOVE_TO_TRASH,
            ),
        )

        assertTrue(trash.movedPaths.contains(file.toString()))
        assertEquals(CleanupExecutionStatus.TRASHED, result.entries.single().status)
    }

    @Test
    fun `should fall back to direct delete when trash move fails`() {
        val file = Files.createTempDirectory("depjanitor-trash-fallback").resolve("old.jar")
        file.writeBytes(ByteArray(8))
        val trash = FakeTrashSupport(supported = true, moveResult = false)
        val service = WorkspaceCleanupExecutionService(trashSupport = trash)

        val result = service.execute(
            CleanupExecutionPlan(
                items = listOf(
                    CleanupExecutionItem(
                        candidateId = "1",
                        coordinate = "org.example:demo",
                        path = file.toString(),
                        source = ArtifactSource.MAVEN,
                        riskLevel = RiskLevel.LOW,
                        sizeBytes = 8L,
                    ),
                ),
                mode = CleanupExecutionMode.MOVE_TO_TRASH,
            ),
        )

        assertFalse(file.exists())
        assertEquals(CleanupExecutionStatus.DELETED, result.entries.single().status)
        assertTrue(result.entries.single().message?.contains("回退") == true)
    }
}

private class FakeTrashSupport(
    private val supported: Boolean,
    private val moveResult: Boolean = true,
) : TrashSupport {
    val movedPaths = mutableListOf<String>()

    override fun isSupported(): Boolean = supported

    override fun moveToTrash(path: java.nio.file.Path): Boolean {
        if (!supported) return false
        movedPaths += path.toString()
        if (moveResult) {
            Files.deleteIfExists(path)
        }
        return moveResult
    }
}
