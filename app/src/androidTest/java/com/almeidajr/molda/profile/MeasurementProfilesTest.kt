package com.almeidajr.molda.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.awaitDialogFocus
import com.almeidajr.molda.awaitKeyboardVisible
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.testing.FakeProfileRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class MeasurementProfilesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun deleteConfirmation_cancel_keepsProfile() {
        // Given
        val storage = FakeProfileRepository(listOf(profile(1, "Ana")))
        showProfiles(storage)
        composeRule.click("Ações: Ana")
        composeRule.click("Excluir perfil")

        // When
        composeRule.click("Cancelar")

        // Then
        composeRule.onNodeWithText("Excluir perfil Ana?").assertDoesNotExist()
        assertEquals(1, storage.current.size)
    }

    @Test
    fun deleteConfirmation_back_keepsProfile() {
        // Given
        val storage = FakeProfileRepository(listOf(profile(1, "Ana")))
        showProfiles(storage)
        composeRule.click("Ações: Ana")
        composeRule.click("Excluir perfil")
        composeRule.awaitDialogFocus()

        // When
        pressBack()

        // Then
        composeRule.onNodeWithText("Excluir perfil Ana?").assertDoesNotExist()
        composeRule.onNodeWithText("Ana").assertIsDisplayed()
        assertEquals(1, storage.current.size)
    }

    @Test
    fun confirmDelete_withDuplicateNames_removesOnlyTheChosenProfile() {
        // Given
        val storage = FakeProfileRepository(listOf(profile(1, "Ana"), profile(2, "Ana")))
        showProfiles(storage)

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
        showCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
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
        showCalculator(FakeProfileRepository())
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
    fun calendar_withTypedDate_canConfirmIt() {
        // Given
        showNewProfileEditor()
        composeRule.edit("Data da medida (dd/mm/aaaa)", "12/10/2026")

        // When
        composeRule.openCalendar()

        // Then
        composeRule.onNodeWithText("Confirmar data").assertIsEnabled()
    }

    @Test
    fun calendar_tomorrow_isNotSelectable() {
        // Given
        showNewProfileEditor()
        composeRule.edit("Data da medida (dd/mm/aaaa)", "13/10/2026")

        // When
        composeRule.openCalendar()

        // Then
        composeRule.onNodeWithText(
            calendarDay(LocalDate.of(2026, 10, 14)),
            substring = true,
            ignoreCase = true,
        ).assertIsNotEnabled()
    }

    @Test
    fun calendar_pickDay_fillsDateField() {
        // Given
        showNewProfileEditor()
        composeRule.edit("Data da medida (dd/mm/aaaa)", "13/10/2026")
        composeRule.openCalendar()

        // When
        composeRule.onNodeWithText(calendarDay(LocalDate.of(2026, 10, 12)), ignoreCase = true)
            .performClick()
        composeRule.click("Confirmar data")

        // Then
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertTextContains("12/10/2026")
    }

    @Test
    fun calendarInput_impossibleDate_cannotBeConfirmed() {
        // Given
        showNewProfileEditor()

        // When
        composeRule.typeInCalendar("32/02/2026")

        // Then
        composeRule.onNodeWithText("Confirmar data").assertIsNotEnabled()
    }

    @Test
    fun calendarBack_inDirtyEditor_closesOnlyTheCalendar() {
        // Given
        showNewProfileEditor()
        composeRule.edit("Nome ou apelido", "Rascunho")
        // Settle native IME dismissal before testing the calendar's own Back handling.
        closeSoftKeyboard()
        composeRule.openCalendar()
        composeRule.awaitDialogFocus()

        // When
        pressBack()

        // Then
        composeRule.onNodeWithText("Cancelar calendário").assertDoesNotExist()
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Rascunho")
    }

    @Test
    fun openSavedProfile_cancelWithoutChanges_closesWithoutAsking() {
        // Given
        showProfiles(FakeProfileRepository(listOf(profile(1, "Ana"))))
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
        // A keyboard open under the dialog reopens once it closes, resizing the editor.
        closeSoftKeyboard()

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
        closeSoftKeyboard()
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
        showProfiles(storage)
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Rascunho")
        closeSoftKeyboard()
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
        composeRule.edit("Data da medida (dd/mm/aaaa)", "32/02/2026")

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
    fun saveEdit_withInvalidFields_keepsStoredProfile() {
        // Given
        val ana = profile(1, "Ana")
        val storage = FakeProfileRepository(listOf(ana))
        showProfiles(storage)
        composeRule.click("Ana")
        composeRule.edit("Nome ou apelido", "")
        composeRule.edit("Cintura do perfil (cm)", "Infinity")

        // When
        composeRule.click("Salvar alterações")

        // Then
        composeRule.onNodeWithText("Informe um nome ou apelido.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Informe uma cintura positiva e finita, como 80,5.")
            .performScrollTo().assertIsDisplayed()
        assertEquals(listOf(ana), storage.current)
    }

    @Test
    fun newProfileEditor_afterRecreation_keepsEveryField() {
        // Given
        val storage = FakeProfileRepository()
        showNewProfileEditor(storage)
        composeRule.fillProfile("Ana", waist = "80,5")
        composeRule.edit("Observações (opcional)", "Conferir antes de cortar")

        // When
        composeRule.activityRule.scenario.recreate()
        composeRule.showProfileRoute(storage)

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
        showCalculator(FakeProfileRepository())
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
    fun pickerNewProfile_afterRecreation_keepsDraft() {
        // Given
        val storage = FakeProfileRepository()
        showCalculator(storage)
        composeRule.click("Usar perfil")
        composeRule.click("Novo perfil")
        composeRule.edit("Nome ou apelido", "Nova no seletor")

        // When
        composeRule.activityRule.scenario.recreate()
        composeRule.showProfileRoute(storage)

        // Then
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Nova no seletor")
    }

    @Test
    fun pickProfile_withManyDecimals_copiesExactWaist() {
        // Given
        showCalculator(FakeProfileRepository(listOf(profile(1, "Ana", waist = 80.123456789))))

        // When
        pick("Usar perfil: Ana — 80,123456789 cm — 12/10/2026")

        // Then
        composeRule.onNodeWithText("Cintura copiada do perfil: Ana").assertIsDisplayed()
        waistField().assertTextContains("80,123456789")
    }

    @Test
    fun pickProfile_afterCalculating_clearsResults() {
        // Given
        showCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
        composeRule.edit(WAIST_LABEL, "90")
        fillCalculatorExceptWaist()
        composeRule.click("Calcular")
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()

        // When
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")

        // Then
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }

    @Test
    fun pickProfile_keepsOtherCalculatorFields() {
        // Given
        showCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
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
        showCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
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
    fun copiedSource_afterRecreation_staysLabelled() {
        // Given
        val storage = FakeProfileRepository(listOf(profile(1, "Ana")))
        showCalculator(storage)
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")

        // When
        composeRule.activityRule.scenario.recreate()
        composeRule.showProfileRoute(storage)

        // Then
        composeRule.onNodeWithText("Cintura copiada do perfil: Ana")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pickerCancel_keepsManualDraft() {
        // Given
        showCalculator(FakeProfileRepository(listOf(profile(1, "Ana"))))
        composeRule.edit(WAIST_LABEL, "91,5")
        composeRule.click("Usar perfil")

        // When
        composeRule.click("Cancelar seleção")

        // Then
        waistField().assertTextContains("91,5")
        composeRule.onNodeWithText("Medida manual").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun largeText_withKeyboardOpen_saveStillWorks() {
        // Given
        val storage = FakeProfileRepository()
        composeRule.showProfileRoute(storage, fontScale = 2f)
        composeRule.showProfiles()
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Ana")
        composeRule.onNodeWithText("Observações (opcional)").performScrollTo().performClick()
        composeRule.awaitKeyboardVisible()

        // When
        saveProfile()

        // Then
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()
        assertEquals(1, storage.current.size)
    }

    @Test
    fun wideWindow_editor_showsBesideList() {
        // Given
        composeRule.showProfileRoute(FakeProfileRepository(), width = WIDE_WIDTH)
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
        composeRule.showProfileRoute(FakeProfileRepository(), width = WIDE_WIDTH)
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
        val storage = FakeProfileRepository(listOf(profile(1, "Ana"), profile(2, "Bia")))
        composeRule.showProfileRoute(storage, width = WIDE_WIDTH)
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
        val storage = FakeProfileRepository()
        composeRule.showProfileRoute(storage, width = WIDE_WIDTH)
        composeRule.showProfiles()
        composeRule.click("Novo perfil")
        composeRule.edit("Nome ou apelido", "Rascunho")

        // When
        composeRule.showProfileRoute(storage, fontScale = 2f, width = 1000)

        // Then
        composeRule.screenTitle("Perfis de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo()
            .assertTextContains("Rascunho")
        composeRule.onNodeWithText("Salvar perfil").performScrollTo().assertIsDisplayed()
    }

    private fun showCalculator(storage: FakeProfileRepository) {
        composeRule.showProfileRoute(storage)
        composeRule.showSkirtCalculator()
    }

    private fun showProfiles(storage: FakeProfileRepository) {
        composeRule.showProfileRoute(storage)
        composeRule.showProfiles()
    }

    private fun showNewProfileEditor(storage: FakeProfileRepository = FakeProfileRepository()) {
        showProfiles(storage)
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

    private fun calendarDay(date: LocalDate): String = date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(composeRule.activity.resources.configuration.locales[0]),
    )

    private companion object {
        const val WAIST_LABEL = "Circunferência da cintura (cm)"
        const val WIDE_WIDTH = 1200
    }
}
