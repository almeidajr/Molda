package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.saveable.SaverScope
import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
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
            holder.editorOn(stack),
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
            holder.editorOn(withPicker),
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
            holder.editorOn(withGuide),
        )
    }

    @Test
    fun editorOn_withTheEditorOnlyInAnotherTab_answersNoEditor() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Profile, isDirty = true))

        // When/Then
        assertNull(holder.editorOn(listOf(OtherRoot)))
    }

    @Test
    fun editorOn_ofAHiddenTabsStack_answersItsEditor() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Profile, isSaving = true))

        // When/Then
        assertEquals(
            ReportedEditor(Editor, EditorReport(EditorKind.Profile, isSaving = true)),
            holder.editorOn(stack),
        )
    }

    @Test
    fun retainOnly_afterTheEditorLeftTheBackStacks_dropsItsReportForGood() {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Profile, isDirty = true))

        // When
        holder.retainOnly(listOf(Root))

        // Then
        assertNull(holder.editorOn(stack))
    }

    @Test
    fun statusOn_afterTheEditorChanges_followsItsStatus() = runTest {
        // Given
        val report = MutableStateFlow(EditorReport(EditorKind.Profile))
        val holder = EditorStatusHolder()
        holder.report(Editor, report)

        holder.statusOn(stack).test {
            assertEquals(EditorReport(EditorKind.Profile), awaitItem())

            // When
            report.value = EditorReport(EditorKind.Profile, isSaving = true)

            // Then
            assertEquals(EditorReport(EditorKind.Profile, isSaving = true), awaitItem())
        }
    }

    @Test
    fun statusOn_withoutAnEditorOnTheStack_isNoEditor() = runTest {
        // Given
        val holder = EditorStatusHolder()
        holder.report(Editor, reportOf(EditorKind.Profile, isSaving = true))

        // When/Then
        holder.statusOn(listOf(OtherRoot)).test { assertNull(awaitItem()) }
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
            restored?.editorOn(withPicker),
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
            restored?.editorOn(stack),
        )
    }

    @Test
    fun editorStatusHolderSaver_withAnUnknownKindOrACutPair_skipsThem() {
        // Given
        val saved = listOf(Editor.toString(), "Pattern", PickerEditor.toString())
        val withPicker = listOf(Root, Editor, PickerEditor)

        // When
        val restored = editorStatusHolderSaver(EditorStatusHolder()).restore(saved)

        // Then
        assertNull(restored?.editorOn(withPicker))
    }

    @Test
    fun editorStatusHolderSaver_withTheFormerListOfPlainIds_restoresNothing() {
        // Given
        val saved = listOf(Editor.toString(), PickerEditor.toString())
        val withPicker = listOf(Root, Editor, PickerEditor)

        // When
        val restored = editorStatusHolderSaver(EditorStatusHolder()).restore(saved)

        // Then
        assertNull(restored?.editorOn(withPicker))
    }

    private fun reportOf(kind: EditorKind, isDirty: Boolean = false, isSaving: Boolean = false) =
        MutableStateFlow(EditorReport(kind, isDirty, isSaving))
}
