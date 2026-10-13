package com.almeidajr.molda.profile

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesContent
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.skirt.ui.MeasurementField
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
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

    private fun showContent(fontScale: Float = 1f) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                val profiles: ProfilesViewModel = viewModel(
                    key = "isolatedProfiles",
                    factory = viewModelFactory {
                        initializer {
                            ProfilesViewModel(
                                storage,
                                createSavedStateHandle(),
                            )
                        }
                    },
                )
                val calculator: SkirtCalculatorViewModel = viewModel()
                val state by profiles.state.collectAsStateWithLifecycle()
                val calculatorState by calculator.state.collectAsStateWithLifecycle()
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    MoldaTheme {
                        SkirtCalculatorScreen(
                            calculatorState,
                            calculator::edit,
                            calculator::calculate,
                        ) {
                            ProfilesContent(
                                state,
                                profiles::startCreating,
                                profiles::edit,
                                profiles::save,
                                profiles::load,
                                { calculator.edit(MeasurementField.Waist, profileWaistText(it)) },
                                profiles::startEditing,
                                profiles::delete,
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
    fun explicitEdit_invalidFieldsPreserveSavedMeasurements() {
        click("Novo perfil")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        click("Salvar perfil")
        click("Editar perfil: Ana (perfil 1)")
        edit("Nome ou apelido", "")
        edit("Cintura do perfil (cm)", "Infinity")
        edit("Data da medida (dd/mm/aaaa)", "31/02/2026")
        click("Salvar alterações")
        composeRule.onNodeWithText("Informe um nome ou apelido.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Informe uma cintura positiva e finita, como 80,5.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Informe uma data válida no formato dd/mm/aaaa.")
            .performScrollTo().assertIsDisplayed()
        runBlocking {
            val saved = storage.list().single().measurements
            assertEquals("Ana", saved.name)
            assertEquals(80.0, saved.waist, 0.0)
        }
    }

    @Test
    fun explicitEditAndDelete_preserveCalculationUntilNextSelectionAndOtherProfile() {
        repeat(2) {
            click("Novo perfil")
            edit("Nome ou apelido", "Ana")
            edit("Cintura do perfil (cm)", "80,123456789")
            edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
            click("Salvar perfil")
        }
        click("Usar perfil: Ana — 80,123456789 cm — 12/10/2026 (perfil 1)")
        edit("Circunferência da cintura (cm)", "90")
        click("Editar perfil: Ana (perfil 1)")
        composeRule.onNodeWithText("Cintura do perfil (cm)")
            .performScrollTo().assertTextContains("80,123456789")
        edit("Nome ou apelido", "Ana atualizada")
        edit("Cintura do perfil (cm)", "82,25")
        edit("Data da medida (dd/mm/aaaa)", "13/10/2026")
        edit("Observações (opcional)", "Nova medida")
        composeRule.activityRule.scenario.recreate()
        showContent()
        composeRule.onNodeWithText("Nome ou apelido")
            .performScrollTo().assertTextContains("Ana atualizada")
        click("Salvar alterações")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("90")
        click("Usar perfil: Ana atualizada — 82,25 cm — 13/10/2026 (perfil 1)")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("82,25")
        click("Excluir perfil: Ana atualizada (perfil 1)")
        composeRule.onNodeWithText(
            "Usar perfil: Ana atualizada — 82,25 cm — 13/10/2026 (perfil 1)",
        ).assertDoesNotExist()
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("82,25")
        click("Usar perfil: Ana — 80,123456789 cm — 12/10/2026 (perfil 2)")
        runBlocking { assertEquals(2L, storage.list().single().id) }
    }

    @Test
    fun create_selectAndOverride_keepsSavedWaistAndCalculationChoices() {
        edit("Comprimento acabado (cm)", "60")
        edit("Folga de cintura (cm)", "2")
        edit("Margem de costura da cintura (cm)", "1")
        edit("Margem de barra (cm)", "2")
        click("Novo perfil")
        edit("Nome ou apelido", "Teste reutilização")
        edit("Cintura do perfil (cm)", "80,5")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        edit("Observações (opcional)", "Medida no cós")
        click("Salvar perfil")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText(
                    "Usar perfil: Teste reutilização — 80,5 cm — 12/10/2026 (perfil 1)",
                ),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        click("Usar perfil: Teste reutilização — 80,5 cm — 12/10/2026 (perfil 1)")
        composeRule.onNodeWithText(
            "Circunferência da cintura (cm)",
        ).performScrollTo().assertTextContains("80,5")
        edit("Circunferência da cintura (cm)", "90")
        click("Usar perfil: Teste reutilização — 80,5 cm — 12/10/2026 (perfil 1)")
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
    fun create_invalidFieldsShowsErrorsAndRecreationKeepsForm() {
        click("Novo perfil")
        click("Salvar perfil")
        composeRule.onNodeWithText(
            "Informe um nome ou apelido.",
        ).performScrollTo().assertIsDisplayed()
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "NaN")
        edit("Data da medida (dd/mm/aaaa)", "31/02/2026")
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
            click("Novo perfil")
            edit("Nome ou apelido", "Ana")
            edit("Cintura do perfil (cm)", "80")
            edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
            click("Salvar perfil")
        }
        click("Usar perfil: Ana — 80 cm — 12/10/2026 (perfil 1)")
        click("Usar perfil: Ana — 80 cm — 12/10/2026 (perfil 2)")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("80")
    }

    @Test
    fun enlargedText_keyboardKeepsProfileFormAndSaveReachable() {
        showContent(fontScale = 2f)
        click("Novo perfil")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80,5")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        composeRule.onNodeWithText("Observações (opcional)")
            .performScrollTo().performClick().assertIsDisplayed()
        click("Salvar perfil")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Perfil salvo neste aparelho."))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Usar perfil: Ana — 80,5 cm — 12/10/2026 (perfil 1)")
            .performScrollTo().assertIsDisplayed()
        click("Editar perfil: Ana (perfil 1)")
        edit("Cintura do perfil (cm)", "82,5")
        composeRule.onNodeWithText("Observações (opcional)")
            .performScrollTo().performClick().assertIsDisplayed()
        click("Salvar alterações")
        composeRule.onNodeWithText("Usar perfil: Ana — 82,5 cm — 12/10/2026 (perfil 1)")
            .performScrollTo().assertIsDisplayed()
        click("Excluir perfil: Ana (perfil 1)")
        composeRule.onNodeWithText("Perfil excluído deste aparelho.")
            .performScrollTo().assertIsDisplayed()
    }
    private fun edit(label: String, value: String) {
        composeRule.onNodeWithText(label).performScrollTo().performTextReplacement(value)
    }

    private fun click(label: String) {
        composeRule.onNodeWithText(label).performScrollTo().performClick()
    }
}
