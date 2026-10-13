package com.almeidajr.molda.profile

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesContent
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.skirt.ui.MeasurementField
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileFailuresTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun updateAndDeleteFailures_keepSavedRecordsDraftAndCalculationWithoutSuccess() {
        val storage = FailingProfiles()
        val original = runBlocking { storage.list() }
        showProfiles(storage)
        edit("Circunferência da cintura (cm)", "95")
        edit("Comprimento acabado (cm)", "60")
        click("Editar perfil: Existente (perfil 1)")
        edit("Nome ou apelido", "Alterado")
        edit("Cintura do perfil (cm)", "82")
        click("Salvar alterações")
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido")
            .performScrollTo().assertTextContains("Alterado")
        click("Excluir perfil: Existente (perfil 1)")
        composeRule.onNodeWithText("Não foi possível excluir o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Usar perfil: Existente — 80 cm — 12/10/2026 (perfil 1)")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cintura do perfil (cm)")
            .performScrollTo().assertTextContains("82")
        composeRule.onNodeWithText("Circunferência da cintura (cm)")
            .performScrollTo().assertTextContains("95")
        composeRule.onNodeWithText("Comprimento acabado (cm)")
            .performScrollTo().assertTextContains("60")
        runBlocking { assertEquals(original, storage.list()) }
    }

    @Test
    fun loadFailure_blocksSavingAndRetryShowsExistingRecords() {
        val storage = FailingProfiles(failLoad = true)
        showProfiles(storage)
        composeRule.onNodeWithText("Não foi possível carregar os perfis.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Novo perfil").assertIsNotEnabled()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.runOnUiThread { storage.failLoad = false }
        composeRule.onNodeWithText("Tentar carregar novamente").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText(
                    "Usar perfil: Existente — 80 cm — 12/10/2026 (perfil 1)",
                ),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Usar perfil: Existente — 80 cm — 12/10/2026 (perfil 1)")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveFailure_keepsFormAndExistingRecordsWithoutSuccess() {
        showProfiles(FailingProfiles(failSave = true))
        click("Novo perfil")
        edit("Nome ou apelido", "Nova")
        edit("Cintura do perfil (cm)", "90")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        click("Salvar perfil")
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo()
        composeRule.onNodeWithText("Usar perfil: Existente — 80 cm — 12/10/2026 (perfil 1)")
            .performScrollTo().assertIsDisplayed()
    }

    private fun showProfiles(storage: ProfileRepository) {
        composeRule.runOnUiThread {
            val profiles = ProfilesViewModel(storage, SavedStateHandle())
            val calculator = SkirtCalculatorViewModel()
            composeRule.activity.setContent {
                val state by profiles.state.collectAsStateWithLifecycle()
                val calculatorState by calculator.state.collectAsStateWithLifecycle()
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

    private fun edit(label: String, value: String) {
        composeRule.onNodeWithText(label).performScrollTo().performTextReplacement(value)
    }

    private fun click(label: String) {
        composeRule.onNodeWithText(label).performScrollTo().performClick()
    }
}

private class FailingProfiles(var failLoad: Boolean = false, val failSave: Boolean = false) :
    ProfileRepository {
    private val records =
        listOf(
            MeasurementProfile(
                1,
                ProfileMeasurements("Existente", 80.0, LocalDate.of(2026, 10, 12)),
            ),
            MeasurementProfile(
                2,
                ProfileMeasurements("Outra pessoa", 95.0, LocalDate.of(2026, 10, 11)),
            ),
        )
    override suspend fun list(): List<MeasurementProfile> {
        if (failLoad) error("Controlled load failure")
        return records
    }
    override suspend fun create(measurements: ProfileMeasurements): MeasurementProfile {
        if (failSave) error("Controlled save failure")
        return MeasurementProfile(3, measurements)
    }
    override suspend fun update(profile: MeasurementProfile) = error("Controlled update failure")
    override suspend fun delete(id: Long) = error("Controlled delete failure")
}
