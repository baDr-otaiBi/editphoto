package com.editphoto.producteditor.processing

import android.graphics.Bitmap

/**
 * Manages undo/redo history for image editing operations.
 * Stores bitmap snapshots with configurable maximum history size.
 */
class EditHistory(private val maxSize: Int = 15) {

    private val undoStack = ArrayDeque<HistoryEntry>()
    private val redoStack = ArrayDeque<HistoryEntry>()

    data class HistoryEntry(
        val bitmap: Bitmap,
        val actionName: String
    )

    /**
     * Pushes a new state to the history. Clears redo stack.
     */
    fun push(bitmap: Bitmap, actionName: String) {
        // Clear redo when new action is performed
        redoStack.forEach { it.bitmap.recycle() }
        redoStack.clear()

        // Add to undo stack
        undoStack.addLast(
            HistoryEntry(bitmap.copy(Bitmap.Config.ARGB_8888, false), actionName)
        )

        // Trim if exceeds max
        while (undoStack.size > maxSize) {
            undoStack.removeFirst().bitmap.recycle()
        }
    }

    /**
     * Undoes the last action. Returns the previous bitmap state.
     */
    fun undo(): Bitmap? {
        if (undoStack.size <= 1) return null // Keep at least the original

        val current = undoStack.removeLast()
        redoStack.addLast(current)

        return undoStack.lastOrNull()?.bitmap?.copy(Bitmap.Config.ARGB_8888, false)
    }

    /**
     * Redoes the last undone action. Returns the restored bitmap state.
     */
    fun redo(): Bitmap? {
        val entry = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(entry)
        return entry.bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    fun canUndo(): Boolean = undoStack.size > 1
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun getUndoActionName(): String? = undoStack.lastOrNull()?.actionName
    fun getRedoActionName(): String? = redoStack.lastOrNull()?.actionName

    fun getHistoryList(): List<String> = undoStack.map { it.actionName }

    fun clear() {
        undoStack.forEach { it.bitmap.recycle() }
        redoStack.forEach { it.bitmap.recycle() }
        undoStack.clear()
        redoStack.clear()
    }
}
