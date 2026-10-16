package com.almeidajr.molda.profile

import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.espresso.Espresso
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MeasurementProfilesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val databaseName = "profile-ui-test.db"
    private lateinit var storage: LocalProfileRepository

    @Before
    fun showIsolatedProfiles() {
        composeRule.activity.deleteDatabase(databaseName)
        storage = LocalProfileRepository(composeRule.activity, databaseName)
        showContent()
    }

    private fun showContent(fontScale: Float = 1f, width: Int = 400) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                val window = LocalWindowInfo.current.containerSize
                // Forced density does not scale native IME insets; retain the physical viewport height.
                val height = maxOf(900f, width * window.height.toFloat() / window.width)
                val profiles: ProfilesViewModel = viewModel(
                    key = "isolatedProfiles",
                    factory = viewModelFactory {
                        initializer {
                            ProfilesViewModel(
                                storage,
                                createSavedStateHandle(),
                                Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC),
                            )
                        }
                    },
                )
                val calculator: SkirtCalculatorViewModel = viewModel()
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, height.dp)),
                    ) {
                        MoldaTheme {
                            MoldaRoute(
                                calculator = calculator,
                                profiles = profiles,
                            )
                        }
                    }
                }
            }
        }
    }

    @After
    fun removeIsolatedProfiles() {
        composeRule.activityRule.scenario.close()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.deleteDatabase(databaseName)
    }

    @Test
    fun deleteConfirmation_cancelAndBackPreserveSavedProfile() {
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        saveProfile()
        click("Ações: Ana")
        click("Excluir perfil")
        composeRule.onNodeWithText("Excluir perfil Ana?").assertIsDisplayed()
        runBlocking { assertEquals(1, storage.list().size) }
        click("Cancelar")
        composeRule.onNodeWithText("Excluir perfil Ana?").assertDoesNotExist()
        click("Ações: Ana")
        click("Excluir perfil")
        waitForDialogFocus()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Excluir perfil Ana?")).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Excluir perfil Ana?").assertDoesNotExist()
        composeRule.onNodeWithText("Ana").assertIsDisplayed()
        runBlocking { assertEquals(1, storage.list().size) }
    }

    @Test
    fun confirmedDelete_matchingNamesKeepsOtherIdentityAndCopiedCalculation() {
        repeat(2) {
            manage()
            click("Novo perfil")
            edit("Nome ou apelido", "Ana")
            edit("Cintura do perfil (cm)", "80")
            edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
            saveProfile()
        }
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")
        edit("Folga de cintura (cm)", "0")
        edit("Comprimento acabado (cm)", "60")
        edit("Margem de costura da cintura (cm)", "0")
        edit("Margem de barra (cm)", "0")
        click("Calcular")
        manage()
        composeRule.onAllNodes(hasText("Ações: Ana"))[0]
            .performScrollTo().performClick()
        click("Excluir perfil")
        composeRule.onNodeWithText("Excluir perfil Ana?").assertIsDisplayed()
        click("Confirmar exclusão")
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertIsDisplayed()
        click("Voltar ao cálculo")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("80")
        composeRule.onNodeWithText("Cintura copiada do perfil: Ana").assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
        runBlocking { assertEquals(2L, storage.list().single().id) }
    }

    @Test
    fun saveFeedback_keepsCalculateTouchableWhenReturningToDraft() {
        edit("Circunferência da cintura (cm)", "80")
        edit("Folga de cintura (cm)", "0")
        edit("Comprimento acabado (cm)", "60")
        edit("Margem de costura da cintura (cm)", "0")
        edit("Margem de barra (cm)", "0")
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        saveProfile()
        click("Voltar ao cálculo")
        composeRule.onNodeWithText("Calcular").performScrollTo()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()
        composeRule.onNodeWithText("Calcular").performClick()
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dateEntryAndCalendarIcon_openPickerAndCancelPreservesDate() {
        manage()
        click("Novo perfil")
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo()
        composeRule.onNodeWithContentDescription("Escolher data no calendário").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Escolher data no calendário").performClick()
        waitForDialogFocus()
        composeRule.onNodeWithText("Confirmar data").assertIsDisplayed().assertIsNotEnabled()
        click("Cancelar calendário")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        composeRule.onNodeWithContentDescription("Escolher data no calendário").performClick()
        waitForDialogFocus()
        composeRule.onNodeWithText("Confirmar data").assertIsDisplayed().assertIsEnabled()
        click("Cancelar calendário")
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertTextContains("12/10/2026")
    }

    @Test
    fun back_dismissesCalendarThenConfirmsDirtyEditor() {
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Rascunho")
        // Settle native IME dismissal before testing the calendar's own Back handling.
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo()
        composeRule.onNodeWithContentDescription("Escolher data no calendário").performClick()
        waitForDialogFocus()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Cancelar calendário")).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Rascunho")
        composeRule.waitUntil(5_000) { composeRule.activity.hasWindowFocus() }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        click("Descartar")
        composeRule.onNodeWithText("Perfis de medidas (opcional)").assertIsDisplayed()
    }

    @Test
    fun dateBoundary_futureRejectedTodaySavedAndCalendarMatchesInput() {
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Data limite")
        edit("Cintura do perfil (cm)", "80")
        Espresso.closeSoftKeyboard()
        // Native IME dismissal can move the button after Compose has become idle.
        composeRule.waitUntil(5_000) {
            !composeRule.activity.window.decorView.rootWindowInsets
                .isVisible(WindowInsets.Type.ime())
        }
        click("Salvar perfil")
        composeRule.onNodeWithText("Informe uma data válida no formato dd/mm/aaaa.")
            .performScrollTo().assertIsDisplayed()
        composeRule.openProfileDateInput("14/10/2026")
        composeRule.onNodeWithText("Confirmar data").assertIsNotEnabled()
        click("Cancelar calendário")
        edit("Data da medida (dd/mm/aaaa)", "13/10/2026")
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo()
        composeRule.onNodeWithContentDescription("Escolher data no calendário").performClick()
        val calendarFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(composeRule.activity.resources.configuration.locales[0])
        composeRule.onNodeWithText(
            LocalDate.of(2026, 10, 14).format(calendarFormat),
            substring = true,
            ignoreCase = true,
        )
            .assertIsNotEnabled()
        composeRule.onNodeWithText(
            LocalDate.of(2026, 10, 12).format(calendarFormat),
            ignoreCase = true,
        ).performClick()
        click("Confirmar data")
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertTextContains("12/10/2026")
        edit("Data da medida (dd/mm/aaaa)", "13/10/2026")
        saveProfile()
        composeRule.onNodeWithText("Data limite").performScrollTo().assertIsDisplayed()
        click("Data limite")
        click("Cancelar")
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
    }

    @Test
    fun pickerCreation_returnsWithoutSelectingAndRestoresDirtyEditor() {
        edit("Circunferência da cintura (cm)", "95")
        click("Usar perfil")
        click("Novo perfil")
        edit("Nome ou apelido", "Nova no seletor")
        edit("Cintura do perfil (cm)", "80")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        composeRule.activityRule.scenario.recreate()
        showContent()
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Nova no seletor")
        click("Cancelar")
        click("Continuar editando")
        saveProfile()
        val profile = composeRule.onNodeWithText(
            "Usar perfil: Nova no seletor — 80 cm — 12/10/2026",
        )
        composeRule.waitUntil(5_000) { profile.isDisplayed() }
        profile.assertIsDisplayed()
        click("Cancelar seleção")
        composeRule.onNodeWithText("Circunferência da cintura (cm)").assertTextContains("95")
    }

    @Test
    fun cancelDirtyCreation_confirmsDiscardAndLeavesCalculatorUntouched() {
        edit("Circunferência da cintura (cm)", "95")
        manage()
        click("Novo perfil")
        composeRule.onNodeWithText("Nenhum perfil salvo.", substring = true).assertDoesNotExist()
        edit("Nome ou apelido", "Rascunho")
        click("Cancelar")
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        click("Continuar editando")
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Rascunho")
        click("Cancelar")
        click("Descartar")
        click("Voltar ao cálculo")
        composeRule.onNodeWithText("Circunferência da cintura (cm)").assertTextContains("95")
    }

    @Test
    fun explicitEdit_invalidFieldsPreserveSavedMeasurements() {
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        saveProfile()
        click("Ana")
        edit("Nome ou apelido", "")
        edit("Cintura do perfil (cm)", "Infinity")
        composeRule.openProfileDateInput("32/02/2026")
        composeRule.onNodeWithText("Confirmar data").assertIsNotEnabled()
        click("Cancelar calendário")
        click("Salvar alterações")
        composeRule.onNodeWithText("Informe um nome ou apelido.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Informe uma cintura positiva e finita, como 80,5.")
            .performScrollTo().assertIsDisplayed()
        runBlocking {
            val saved = storage.list().single().measurements
            assertEquals("Ana", saved.name)
            assertEquals(80.0, saved.waist, 0.0)
            assertEquals(LocalDate.of(2026, 10, 12), saved.measurementDate)
        }
    }

    @Test
    fun explicitEditAndDelete_preserveCalculationUntilNextSelectionAndOtherProfile() {
        repeat(2) {
            manage()
            click("Novo perfil")
            edit("Nome ou apelido", "Ana")
            edit("Cintura do perfil (cm)", "80,123456789")
            edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
            saveProfile()
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodes(hasText("Perfil salvo neste aparelho."))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()
        }
        pick("Usar perfil: Ana — 80,123456789 cm — 12/10/2026")
        edit("Circunferência da cintura (cm)", "90")
        manage()
        composeRule.onAllNodes(
            hasText("Ana"),
        )[0].performScrollTo().performClick()
        composeRule.onNodeWithText("Cintura do perfil (cm)")
            .performScrollTo().assertTextContains("80,123456789")
        edit("Nome ou apelido", "Ana atualizada")
        edit("Cintura do perfil (cm)", "82,25")
        edit("Data da medida (dd/mm/aaaa)", "13/10/2026")
        edit("Observações (opcional)", "Nova medida")
        composeRule.activityRule.scenario.recreate()
        showContent()
        manage()
        composeRule.onNodeWithText("Nome ou apelido")
            .performScrollTo().assertTextContains("Ana atualizada")
        saveProfile("Salvar alterações")
        click("Voltar ao cálculo")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("90")
        pick("Usar perfil: Ana atualizada — 82,25 cm — 13/10/2026")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("82,25")
        manage()
        click("Ações: Ana atualizada")
        click("Excluir perfil")
        click("Confirmar exclusão")
        composeRule.onNodeWithText(
            "Usar perfil: Ana atualizada — 82,25 cm — 13/10/2026",
        ).assertDoesNotExist()
        click("Voltar ao cálculo")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("82,25")
        pick("Usar perfil: Ana — 80,123456789 cm — 12/10/2026")
        runBlocking { assertEquals(2L, storage.list().single().id) }
    }

    @Test
    fun create_selectAndOverride_keepsSavedWaistAndCalculationChoices() {
        edit("Comprimento acabado (cm)", "60")
        edit("Folga de cintura (cm)", "2")
        edit("Margem de costura da cintura (cm)", "1")
        edit("Margem de barra (cm)", "2")
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Teste reutilização")
        edit("Cintura do perfil (cm)", "80,5")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        edit("Observações (opcional)", "Medida no cós")
        saveProfile()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                hasText(
                    "Teste reutilização",
                ),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        pick("Usar perfil: Teste reutilização — 80,5 cm — 12/10/2026")
        composeRule.onNodeWithText(
            "Cintura copiada do perfil: Teste reutilização",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Circunferência da cintura (cm)",
        ).performScrollTo().assertTextContains("80,5")
        composeRule.activityRule.scenario.recreate()
        showContent()
        composeRule.onNodeWithText("Cintura copiada do perfil: Teste reutilização")
            .performScrollTo().assertIsDisplayed()
        click("Calcular")
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
        pick("Usar perfil: Teste reutilização — 80,5 cm — 12/10/2026")
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        edit("Circunferência da cintura (cm)", "90")
        composeRule.onNodeWithText("Medida manual").assertIsDisplayed()
        pick("Usar perfil: Teste reutilização — 80,5 cm — 12/10/2026")
        composeRule.onNodeWithText(
            "Circunferência da cintura (cm)",
        ).performScrollTo().assertTextContains("80,5")
        composeRule.onNodeWithText(
            "Comprimento acabado (cm)",
        ).performScrollTo().assertTextContains("60")
        composeRule.onNodeWithText(
            "Folga de cintura (cm)",
        ).performScrollTo().assertTextContains("2")
        composeRule.onNodeWithText(
            "Margem de costura da cintura (cm)",
        ).performScrollTo().assertTextContains("1")
        composeRule.onNodeWithText("Margem de barra (cm)").performScrollTo().assertTextContains("2")
        runBlocking {
            val saved = storage.list().single().measurements
            assertEquals("Teste reutilização", saved.name)
            assertEquals(80.5, saved.waist, 0.0)
            assertEquals("Medida no cós", saved.notes)
        }
    }

    @Test
    fun pickerDismissalAndActivityRecreation_keepManualDraft() {
        edit("Circunferência da cintura (cm)", "91,5")
        edit("Comprimento acabado (cm)", "65")
        click("Usar perfil")
        composeRule.onNodeWithText("Cancelar seleção").performClick()
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("91,5")
        manage()
        click("Voltar ao cálculo")
        composeRule.activityRule.scenario.recreate()
        showContent()
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("91,5")
        composeRule.onNodeWithText("Comprimento acabado (cm)")
            .performScrollTo().assertTextContains("65")
        composeRule.onNodeWithText("Medida manual").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun create_invalidFieldsShowsErrorsAndRecreationKeepsForm() {
        manage()
        click("Novo perfil")
        click("Salvar perfil")
        composeRule.onNodeWithText(
            "Informe um nome ou apelido.",
        ).performScrollTo().assertIsDisplayed()
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "NaN")
        composeRule.openProfileDateInput("32/02/2026")
        composeRule.onNodeWithText("Confirmar data").assertIsNotEnabled()
        click("Cancelar calendário")
        click("Salvar perfil")
        composeRule.onNodeWithText("Informe uma cintura positiva e finita, como 80,5.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Informe uma data válida no formato dd/mm/aaaa.")
            .performScrollTo().assertIsDisplayed()
        edit("Cintura do perfil (cm)", "80,5")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        edit("Observações (opcional)", "Conferir antes de cortar")
        composeRule.activityRule.scenario.recreate()
        showContent()
        manage()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo().assertTextContains("Ana")
        composeRule.onNodeWithText(
            "Cintura do perfil (cm)",
        ).performScrollTo().assertTextContains("80,5")
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)")
            .performScrollTo().assertTextContains("12/10/2026")
        composeRule.onNodeWithText("Observações (opcional)")
            .performScrollTo().assertTextContains("Conferir antes de cortar")
    }

    @Test
    fun create_identicalNamesAndMeasurementsRemainSelectableAsSeparateProfiles() {
        repeat(2) {
            manage()
            click("Novo perfil")
            edit("Nome ou apelido", "Ana")
            edit("Cintura do perfil (cm)", "80")
            edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
            saveProfile()
        }
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("80")
    }

    @Test
    fun enlargedText_keyboardKeepsProfileFormAndSaveReachable() {
        showContent(fontScale = 2f)
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80,5")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        composeRule.onNodeWithText("Observações (opcional)")
            .performScrollTo().performClick().assertIsDisplayed()
        saveProfile()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Perfil salvo neste aparelho."))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Ana")
            .performScrollTo().assertIsDisplayed()
        click("Ana")
        edit("Cintura do perfil (cm)", "82,5")
        composeRule.onNodeWithText("Observações (opcional)")
            .performScrollTo().performClick().assertIsDisplayed()
        saveProfile("Salvar alterações")
        composeRule.onNodeWithText("Ana")
            .performScrollTo().assertIsDisplayed()
        click("Ações: Ana")
        click("Excluir perfil")
        click("Confirmar exclusão")
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertIsDisplayed()
    }

    @Test
    fun wideEditor_keepsListVisible_andNewProfileConfirmsBeforeReplacingDirtyDraft() {
        showContent(width = 1200)
        manage()
        click("Novo perfil")
        composeRule.onNodeWithText("Perfis de medidas (opcional)").assertIsDisplayed()
        edit("Nome ou apelido", "Rascunho")
        click("Novo perfil")
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        click("Continuar editando")
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Rascunho")
        click("Novo perfil")
        click("Descartar")
        composeRule.onNodeWithText("Rascunho").assertDoesNotExist()
        composeRule.onNodeWithText("Perfis de medidas (opcional)").assertIsDisplayed()
    }

    @Test
    fun resizingAndLargeText_preserveDirtyEditor_andBackReturnsToVisibleList() {
        showContent(width = 1200)
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Rascunho")
        showContent(fontScale = 2f, width = 1000)
        composeRule.onNodeWithText("Perfis de medidas (opcional)").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo()
            .assertTextContains("Rascunho")
        composeRule.onNodeWithText("Salvar perfil").performScrollTo().assertIsDisplayed()
        showContent(width = 1200)
        composeRule.onNodeWithText("Perfis de medidas (opcional)").assertIsDisplayed()
        click("Cancelar")
        click("Continuar editando")
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Rascunho")
        Espresso.closeSoftKeyboard()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        click("Descartar")
        composeRule.onNodeWithText("Perfis de medidas (opcional)").assertIsDisplayed()
        composeRule.onNodeWithText("Nome ou apelido").assertDoesNotExist()
        composeRule.onNodeWithText("Voltar ao cálculo").assertIsDisplayed()
    }

    @Test
    fun wideRows_confirmDiscardBeforeSwitching_andPreserveCopiedCalculatorSourceOnResize() {
        showContent(width = 1200)
        manage()
        listOf("Ana", "Bia").forEach { name ->
            click("Novo perfil")
            edit("Nome ou apelido", name)
            edit("Cintura do perfil (cm)", "80")
            edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
            saveProfile()
        }
        click("Ana")
        edit("Nome ou apelido", "Rascunho")
        click("Bia")
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        click("Continuar editando")
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Rascunho")
        click("Bia")
        click("Descartar")
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Bia")
        click("Cancelar")
        pick("Usar perfil: Ana — 80 cm — 12/10/2026")
        showContent(width = 400)
        composeRule.onNodeWithText(
            "Cintura copiada do perfil: Ana",
        ).performScrollTo().assertIsDisplayed()
        showContent(width = 1200)
        composeRule.onNodeWithText("Circunferência da cintura (cm)").assertTextContains("80")
        composeRule.onNodeWithText("Cintura copiada do perfil: Ana").assertIsDisplayed()
    }
    private fun waitForDialogFocus() {
        // Wait for native dialog focus and keyboard dismissal before delivering Back.
        composeRule.waitUntil(5_000) {
            android.view.inspector.WindowInspector.getGlobalWindowViews().any {
                it.hasWindowFocus() &&
                    !it.rootWindowInsets.isVisible(WindowInsets.Type.ime()) &&
                    (it.layoutParams as WindowManager.LayoutParams).type ==
                    WindowManager.LayoutParams.TYPE_APPLICATION
            }
        }
    }
    private fun edit(label: String, value: String) {
        if (label == "Data da medida (dd/mm/aaaa)") {
            composeRule.enterProfileDate(value)
        } else {
            composeRule.onNodeWithText(label).performScrollTo().performTextReplacement(value)
        }
    }

    private fun manage() {
        if (composeRule.onAllNodes(
                hasText("Perfis de medidas"),
            ).fetchSemanticsNodes().isNotEmpty()
        ) {
            click("Perfis de medidas")
        }
    }

    private fun pick(label: String) {
        if (composeRule.onAllNodes(
                hasText("Voltar ao cálculo"),
            ).fetchSemanticsNodes().isNotEmpty()
        ) {
            click("Voltar ao cálculo")
        }
        click("Usar perfil")
        composeRule.onAllNodes(hasText(label))[0].performClick()
    }

    private fun saveProfile(label: String = "Salvar perfil") {
        click(label)
        // A profile name also matches its draft field while storage is still pending.
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Nome ou apelido")).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun click(label: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                hasText(label) and isEnabled() and !hasAnyAncestor(!isEnabled()),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val node = composeRule.onNodeWithText(label)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}
