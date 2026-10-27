package com.almeidajr.molda.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.recreate
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.showRoute
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.testing.FakeProfileRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MeasurementProfilesTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun deleteConfirmation_cancel_keepsProfile() {
        // Given
        val storage = FakeProfileRepository(listOf(profile(1, "Ana")))
        composeRule.launchProfiles(storage)
        composeRule.click("Ações: Ana")
        composeRule.click("Excluir perfil")

        // When
        composeRule.click("Cancelar")

        // Then
        composeRule.onNodeWithText("Excluir perfil Ana?").assertDoesNotExist()
        assertEquals(1, storage.current.size)
    }

    @Test
    fun confirmDelete_withDuplicateNames_removesOnlyTheChosenProfile() {
        // Given
        val storage = FakeProfileRepository(listOf(profile(1, "Ana"), profile(2, "Ana")))
        composeRule.launchProfiles(storage)

        // When
        composeRule.onAllNodes(hasText("Ações: Ana"))[0].performScrollTo().performClick()
        composeRule.click("Excluir perfil")
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertIsDisplayed()
        assertEquals(ProfileId(2), storage.current.single().id)
    }

    @Test
    fun deleteSource_afterCalculating_keepsCopiedWaistAndResults() {
        // Given
        composeRule.launchCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")
        fillCalculatorExceptWaist()
        composeRule.click("Calcular")

        // When
        composeRule.showProfiles()
        composeRule.click("Ações: Ana")
        composeRule.click("Excluir perfil")
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.showSkirtCalculator()
        waistField().assertTextContains("80")
        composeRule.onNodeWithText("Cintura copiada do perfil: Ana")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveFeedback_onCalculator_leavesCalculateTouchable() {
        // Given
        composeRule.launchCalculator()
        composeRule.edit(WAIST_LABEL, "80")
        fillCalculatorExceptWaist()
        composeRule.showProfiles()
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Ana")
        saveProfile()
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText("Calcular").performScrollTo()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()

        // When
        composeRule.onNodeWithText("Calcular").performClick()

        // Then
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calendar_withoutDate_cannotConfirm() {
        // Given
        showNewProfileEditor()

        // When
        composeRule.openCalendar()

        // Then
        composeRule.onNodeWithText("Confirmar data").assertIsNotEnabled()
    }

    @Test
    fun dateField_tap_opensCalendar() {
        // Given
        showNewProfileEditor()

        // When
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo().performClick()

        // Then
        composeRule.onNodeWithText("Cancelar calendário").assertIsDisplayed()
    }

    @Test
    fun calendar_withStoredDate_canConfirmIt() {
        // Given
        composeRule.launchProfiles(FakeProfileRepository(listOf(profile(1, "Ana"))))
        composeRule.click("Ana")

        // When
        composeRule.openCalendar()

        // Then
        composeRule.onNodeWithText("Confirmar data").assertIsEnabled()
    }

    @Test
    fun calendar_tomorrow_isNotSelectable() {
        // Given
        showNewProfileEditor()

        // When
        composeRule.openCalendar()

        // Then
        composeRule.onNodeWithText(
            composeRule.calendarDay(LocalDate.of(2026, 10, 14)),
            substring = true,
            ignoreCase = true,
        ).assertIsNotEnabled()
    }

    @Test
    fun calendar_pickDay_fillsDateField() {
        // Given
        showNewProfileEditor()
        composeRule.openCalendar()

        // When
        composeRule.onNodeWithText(
            composeRule.calendarDay(LocalDate.of(2026, 10, 12)),
            ignoreCase = true,
        )
            .performClick()
        composeRule.click("Confirmar data")

        // Then
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertTextContains("12/10/2026")
    }

    @Test
    fun calendarBack_inDirtyEditor_closesOnlyTheCalendar() {
        // Given
        showNewProfileEditor()
        composeRule.openCalendar()
        composeRule.onNodeWithText(
            composeRule.calendarDay(LocalDate.of(2026, 10, 12)),
            ignoreCase = true,
        )
            .performClick()
        composeRule.click("Confirmar data")
        composeRule.openCalendar()

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Cancelar calendário").assertDoesNotExist()
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertTextContains("12/10/2026")
    }

    @Test
    fun openSavedProfile_cancelWithoutChanges_closesWithoutAsking() {
        // Given
        composeRule.launchProfiles(FakeProfileRepository(listOf(profile(1, "Ana"))))
        composeRule.click("Ana")

        // When
        composeRule.click("Cancelar")

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").assertDoesNotExist()
    }

    @Test
    fun editorCancel_whenDirty_asksToDiscard() {
        // Given
        showNewProfileEditor()
        composeRule.edit("Nome ou apelido", "Rascunho")

        // When
        composeRule.click("Cancelar")

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
    }

    @Test
    fun discardDialog_continueEditing_keepsDraft() {
        // Given
        showNewProfileEditor()
        composeRule.edit("Nome ou apelido", "Rascunho")
        composeRule.click("Cancelar")

        // When
        composeRule.click("Continuar editando")

        // Then
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Rascunho")
    }

    @Test
    fun discardDialog_discard_closesEditorWithoutSaving() {
        // Given
        val storage = FakeProfileRepository()
        composeRule.launchProfiles(storage)
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Rascunho")
        composeRule.click("Cancelar")

        // When
        composeRule.click("Descartar")

        // Then
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
        composeRule.onNodeWithText("Nome ou apelido").assertDoesNotExist()
        assertTrue(storage.current.isEmpty())
    }

    @Test
    fun saveNew_withValidFields_storesThem() {
        // Given
        val storage = FakeProfileRepository()
        showNewProfileEditor(storage)
        composeRule.fillProfile("Ana", waist = "80,5")
        composeRule.edit("Observações (opcional)", "Medida no cós")

        // When
        saveProfile()

        // Then
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()
        assertEquals(
            ProfileMeasurements(
                ProfileName("Ana"),
                Length(80.5),
                LocalDate.of(2026, 10, 12),
                "Medida no cós",
            ),
            storage.current.single().measurements,
        )
    }

    @Test
    fun saveNew_withInvalidFields_showsErrorOnEachField() {
        // Given
        val storage = FakeProfileRepository()
        showNewProfileEditor(storage)
        composeRule.edit("Cintura do perfil (cm)", "NaN")

        // When
        composeRule.click("Salvar perfil")

        // Then
        listOf(
            "Informe um nome ou apelido.",
            "Informe uma cintura positiva e finita, como 80,5.",
            "Informe uma data válida no formato dd/mm/aaaa.",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        assertTrue(storage.current.isEmpty())
    }

    @Test
    fun newProfileEditor_afterRecreation_keepsEveryField() {
        // Given
        showNewProfileEditor()
        composeRule.fillProfile("Ana", waist = "80,5")
        composeRule.edit("Observações (opcional)", "Conferir antes de cortar")

        // When
        composeRule.recreate()

        // Then
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo().assertTextContains("Ana")
        composeRule.onNodeWithText("Cintura do perfil (cm)")
            .performScrollTo().assertTextContains("80,5")
        composeRule.onNodeWithText(
            "Data da medida (dd/mm/aaaa)",
        ).performScrollTo().assertTextContains("12/10/2026")
        composeRule.onNodeWithText("Observações (opcional)")
            .performScrollTo().assertTextContains("Conferir antes de cortar")
    }

    @Test
    fun pickerNewProfile_save_selectsItAndLeavesNoEditorInPerfis() {
        // Given
        composeRule.launchCalculator()
        composeRule.edit(WAIST_LABEL, "95")
        composeRule.click("Usar perfil")
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova no seletor")

        // When
        saveProfile()

        // Then
        composeRule.onNodeWithText("Cintura copiada do perfil: Nova no seletor").assertIsDisplayed()
        waistField().assertTextContains("80")
        composeRule.onNodeWithText("Cancelar seleção").assertDoesNotExist()
        composeRule.showProfiles()
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Nova no seletor").assertIsDisplayed()
    }

    @Test
    fun pickerNewProfile_whilePerfisEditOpen_leavesThatEditIntact() {
        // Given
        composeRule.launchProfiles(FakeProfileRepository(listOf(profile(1, "Ana"))))
        composeRule.click("Ana", useUnmergedTree = true)
        composeRule.edit("Nome ou apelido", "Ana Maria")
        composeRule.showSkirtCalculator()
        composeRule.click("Usar perfil")
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova no seletor")

        // When
        saveProfile()

        // Then
        composeRule.showProfiles()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo()
            .assertTextContains("Ana Maria")
    }

    @Test
    fun pickProfile_keepsOtherCalculatorFields() {
        // Given
        composeRule.launchCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
        val otherFields = mapOf(
            "Folga de cintura (cm)" to "2",
            "Comprimento acabado (cm)" to "60",
            "Margem de costura da cintura (cm)" to "1",
            "Margem de barra (cm)" to "3",
        )
        otherFields.forEach { (label, value) -> composeRule.edit(label, value) }

        // When
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")

        // Then
        otherFields.forEach { (label, value) ->
            composeRule.onNodeWithText(label).performScrollTo().assertTextContains(value)
        }
    }

    @Test
    fun editSource_afterCopy_keepsCalculatorWaist() {
        // Given
        composeRule.launchCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")

        // When
        composeRule.showProfiles()
        composeRule.click("Ana")
        composeRule.edit("Cintura do perfil (cm)", "82,25")
        saveProfile("Salvar alterações")

        // Then
        composeRule.showSkirtCalculator()
        waistField().assertTextContains("80")
    }

    @Test
    fun pickerCancel_keepsManualDraft() {
        // Given
        composeRule.launchCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
        composeRule.edit(WAIST_LABEL, "91,5")
        composeRule.click("Usar perfil")

        // When
        composeRule.click("Cancelar seleção")

        // Then
        waistField().assertTextContains("91,5")
        composeRule.onNodeWithText("Medida manual").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun wideWindow_editor_showsBesideList() {
        // Given
        composeRule.launchApp(frame = profileFrame(width = WIDE_WIDTH))
        composeRule.showProfiles()

        // When
        composeRule.click("Novo perfil")

        // Then
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
        composeRule.onNodeWithText("Nome ou apelido").assertIsDisplayed()
    }

    @Test
    fun wideWindow_newProfileOverDirtyDraft_asksBeforeReplacingIt() {
        // Given
        composeRule.launchApp(frame = profileFrame(width = WIDE_WIDTH))
        composeRule.showProfiles()
        composeRule.click("Novo perfil")
        composeRule.edit("Nome ou apelido", "Rascunho")
        composeRule.click("Novo perfil")
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()

        // When
        composeRule.click("Descartar")

        // Then
        composeRule.onNodeWithText("Rascunho").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").assertIsDisplayed()
    }

    @Test
    fun wideWindow_switchRowOverDirtyDraft_asksBeforeOpeningIt() {
        // Given
        composeRule.launchApp(
            profiles = FakeProfileRepository(listOf(profile(1, "Ana"), profile(2, "Bia"))),
            frame = profileFrame(width = WIDE_WIDTH),
        )
        composeRule.showProfiles()
        composeRule.click("Ana")
        composeRule.edit("Nome ou apelido", "Rascunho")
        composeRule.click("Bia")
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()

        // When
        composeRule.click("Descartar")

        // Then
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Bia")
    }

    @Test
    fun resizeToNarrow_showsDirtyEditorAlone() {
        // Given
        composeRule.launchApp(frame = profileFrame(width = WIDE_WIDTH))
        composeRule.showProfiles()
        composeRule.click("Novo perfil")
        composeRule.edit("Nome ou apelido", "Rascunho")

        // When
        composeRule.showRoute(frame = profileFrame(fontScale = 2f, width = 1000))

        // Then
        composeRule.screenTitle("Perfis de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo()
            .assertTextContains("Rascunho")
        composeRule.onNodeWithText("Salvar perfil").performScrollTo().assertIsDisplayed()
    }

    private fun showNewProfileEditor(storage: FakeProfileRepository = FakeProfileRepository()) {
        composeRule.launchProfiles(storage)
        composeRule.click("Novo perfil")
    }

    private fun fillCalculatorExceptWaist() {
        composeRule.edit("Folga de cintura (cm)", "0")
        composeRule.edit("Comprimento acabado (cm)", "60")
        composeRule.edit("Margem de costura da cintura (cm)", "0")
        composeRule.edit("Margem de barra (cm)", "0")
    }

    private fun waistField() = composeRule.onNodeWithText(WAIST_LABEL).performScrollTo()

    private fun pick(label: String) {
        composeRule.showSkirtCalculator()
        composeRule.click("Usar perfil")
        composeRule.onAllNodes(hasText(label))[0].performClick()
    }

    private fun saveProfile(label: String = "Salvar perfil") {
        composeRule.click(label)
        composeRule.onNodeWithText("Nome ou apelido").assertDoesNotExist()
    }

    private companion object {
        const val WAIST_LABEL = "Circunferência da cintura (cm)"
        const val WIDE_WIDTH = 1200
    }
}
