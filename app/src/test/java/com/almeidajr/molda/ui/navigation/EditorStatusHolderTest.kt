package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.saveable.SaverScope
import androidx.navigation3.runtime.NavKey
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class EditorStatusHolderTest {
    private data object Root : NavKey

    private data object OtherRoot : NavKey

    private data object Editor : NavKey

    private data object PickerEditor : NavKey

    private data object Guide : NavKey

    private val stack = listOf(Root, Editor)

    @Test
    fun editorOn_afterTheEditorChanges_readsItsLatestStatus() {
        // Given
        val report = MutableStateFlow(EditorReport(EditorKind.Project))
        val holder = EditorStatusHolder()
        holder.report(Editor, report)

        // When
        report.value = EditorReport(EditorKind.Project, isDirty = true)

        // Then
        assertEquals(
            ReportedEditor(Editor, EditorReport(EditorKind.Project, isDirty = true)),
            holder.editorOn(stack, keys = stack),
        )
    }

    @Test
    fun editorOn_withTwoEditorsOnTheStack_answersTheTopmost() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Project, isDirty = true))
        holder.report(PickerEditor, reportOf(EditorKind.Profile))
        val withPicker = listOf(Root, Editor, PickerEditor)

        // When/Then
        assertEquals(
            ReportedEditor(PickerEditor, EditorReport(EditorKind.Profile)),
            holder.editorOn(withPicker, keys = withPicker),
        )
    }

    @Test
    fun editorOn_withTheEditorHiddenUnderAGuide_answersTheEditor() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Project, isDirty = true))
        val withGuide = listOf(Root, Editor, Guide)

        // When/Then
        assertEquals(
            ReportedEditor(Editor, EditorReport(EditorKind.Project, isDirty = true)),
            holder.editorOn(withGuide, keys = withGuide),
        )
    }

    @Test
    fun editorOn_withTheEditorOnlyInAnotherTab_answersNoEditor() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Profile, isDirty = true))

        // When/Then
        assertNull(holder.editorOn(listOf(OtherRoot), keys = stack + OtherRoot))
    }

    @Test
    fun editorOn_ofAHiddenTabsStack_answersItsEditor() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Profile, isSaving = true))

        // When/Then
        assertEquals(
            ReportedEditor(Editor, EditorReport(EditorKind.Profile, isSaving = true)),
            holder.editorOn(stack, keys = stack + OtherRoot),
        )
    }

    @Test
    fun editorOn_afterTheEditorLeftTheBackStacks_dropsItsReportForGood() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Profile, isDirty = true))

        // When
        holder.editorOn(listOf(Root), keys = listOf(Root))

        // Then
        assertNull(holder.editorOn(stack, keys = stack))
    }

    @Test
    fun editorStatusHolderSaver_afterProcessDeath_restoresOnlyUnsavedChanges() {
        // Given
        val scope = SaverScope { true }
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Project, isDirty = true, isSaving = true))
        holder.report(PickerEditor, reportOf(EditorKind.Profile, isSaving = true))
        val saved = with(editorStatusHolderSaver(holder)) { scope.save(holder) }
        val withPicker = listOf(Root, Editor, PickerEditor)

        // When
        val restored = saved?.let(editorStatusHolderSaver(EditorStatusHolder())::restore)

        // Then
        assertEquals(
            ReportedEditor(Editor, EditorReport(EditorKind.Project, isDirty = true)),
            restored?.editorOn(withPicker, keys = withPicker),
        )
    }

    @Test
    fun editorStatusHolderSaver_afterConfigurationChange_keepsTheLiveReports() {
        // Given
        val scope = SaverScope { true }
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Profile, isDirty = true, isSaving = true))
        val saver = editorStatusHolderSaver(holder)
        val saved = with(saver) { scope.save(holder) }

        // When
        val restored = saved?.let(saver::restore)

        // Then
        assertEquals(
            ReportedEditor(
                Editor,
                EditorReport(EditorKind.Profile, isDirty = true, isSaving = true),
            ),
            restored?.editorOn(stack, keys = stack),
        )
    }

    private fun reportOf(kind: EditorKind, isDirty: Boolean = false, isSaving: Boolean = false) =
        MutableStateFlow(EditorReport(kind, isDirty, isSaving))
}
