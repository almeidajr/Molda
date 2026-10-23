package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.saveable.SaverScope
import androidx.navigation3.runtime.NavKey
import kotlin.test.assertEquals
import org.junit.Test

class EditorStatusHolderTest {
    private data object Editor : NavKey

    private data object OtherEditor : NavKey

    @Test
    fun reports_afterTheEditorChanges_readItsLatestStatus() {
        // Given
        var status = EditorReport()
        val holder = EditorStatusHolder()
        holder.report(Editor) { status }

        // When
        status = EditorReport(isDirty = true)

        // Then
        assertEquals(
            mapOf<NavKey, EditorReport>(Editor to EditorReport(isDirty = true)),
            holder.reports(listOf(Editor)),
        )
    }

    @Test
    fun reports_ofAnEditorThatLeftTheBackStacks_dropItForGood() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor) { EditorReport(isDirty = true) }

        // When
        val withoutEditor = holder.reports(emptyList())

        // Then
        assertEquals(emptyMap(), withoutEditor)
        assertEquals(emptyMap(), holder.reports(listOf(Editor)))
    }

    @Test
    fun editorStatusHolderSaver_keepsOnlyUnsavedChanges() {
        // Given
        val scope = SaverScope { true }
        val holder = EditorStatusHolder()
        holder.report(Editor) { EditorReport(isDirty = true, isSaving = true) }
        holder.report(OtherEditor) { EditorReport(isUnavailable = true) }

        // When
        val saved = with(EditorStatusHolderSaver) { scope.save(holder) }
        val restored = saved?.let(EditorStatusHolderSaver::restore)

        // Then
        assertEquals(
            mapOf<NavKey, EditorReport>(Editor to EditorReport(isDirty = true)),
            restored?.reports(listOf(Editor, OtherEditor)),
        )
    }
}
