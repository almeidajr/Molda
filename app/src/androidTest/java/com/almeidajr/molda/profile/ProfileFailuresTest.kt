package com.almeidajr.molda.profile

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class ProfileFailuresTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

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
                            {},
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
        )
    override suspend fun list(): List<MeasurementProfile> {
        if (failLoad) error("Controlled load failure")
        return records
    }
    override suspend fun create(measurements: ProfileMeasurements): MeasurementProfile {
        if (failSave) error("Controlled save failure")
        return MeasurementProfile(2, measurements)
    }
}
