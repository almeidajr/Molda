package com.almeidajr.molda

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.project.navigation.profileResultKey
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.navigation.MOLDES_PROFILE_RESULT_KEY
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.ui.navigation.navigatorOf
import com.almeidajr.molda.ui.navigation.stacks
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SharedEntriesTest {
    private val editor = ProjectEditorKey(
        ProjectId(1),
        SkirtSnapshot(
            SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
            SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
            source = null,
        ),
    )

    @Test
    fun closeDeletedProjectEditor_withItsPickerAndDiscardOpen_closesThemWithTheProject() {
        // Given
        val moldes = listOf(
            PatternCatalogKey,
            SkirtCalculatorKey,
            ProfilePickerKey(MOLDES_PROFILE_RESULT_KEY),
        )
        val navigator = navigatorOf(
            listOf(
                moldes,
                listOf(
                    ProjectsKey,
                    ProjectKey(editor.id),
                    editor,
                    ProfilePickerKey(editor.profileResultKey),
                    DiscardChangesKey(AfterDiscard.CloseProjectEditor),
                ),
            ),
            selected = ProjectsKey,
        )

        // When
        val closed = navigator.closeDeletedProjectEditor(editor)

        // Then: Moldes' own picker stays.
        assertTrue(closed)
        assertEquals(listOf(moldes, listOf(ProjectsKey)), navigator.stacks)
    }
}
