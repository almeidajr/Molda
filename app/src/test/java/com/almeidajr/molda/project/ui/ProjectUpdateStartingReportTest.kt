package com.almeidajr.molda.project.ui

import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorReport
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

/** The update's report before its flows first emit, apart from [ProjectUpdateViewModelTest]. */
class ProjectUpdateStartingReportTest {
    // Nothing runs until the test advances, so the report holds its starting value.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun report_beforeItsFlowsEmit_isTheEditorsCurrentStatus() {
        // When
        val update = ProjectUpdateViewModel(
            FakeProjectRepository(listOf(saved)),
            October18Clock,
            ProjectEditorKey(saved.id, saved.snapshot),
            isEditorDirty = MutableStateFlow(true),
        )

        // Then
        assertEquals(EditorReport(EditorKind.Project, isDirty = true), update.report.value)
    }
}
