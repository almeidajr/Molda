package com.almeidajr.molda.profile

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesViewModel
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
        click("Perfis de medidas")
        click("Existente")
        edit("Nome ou apelido", "Alterado")
        edit("Cintura do perfil (cm)", "82")
        click("Salvar alterações")
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido")
            .performScrollTo().assertTextContains("Alterado")
        click("Ações: Existente")
        click("Excluir perfil")
        composeRule.onNodeWithText("Não foi possível excluir o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Existente")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cintura do perfil (cm)")
            .performScrollTo().assertTextContains("82")
        click("Voltar ao cálculo")
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
        click("Perfis de medidas")
        composeRule.onNodeWithText("Não foi possível carregar os perfis.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum perfil salvo.", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.runOnUiThread { storage.failLoad = false }
        composeRule.onNodeWithText("Tentar carregar novamente").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasText(
                    "Existente",
                ),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Existente")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingProfiles_manualCalculationRemainsAvailable() {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        showProfiles(FailingProfiles(loadGate = gate))
        click("Usar perfil")
        composeRule.onNodeWithText("Carregando perfis…").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar seleção").performClick()
        edit("Circunferência da cintura (cm)", "80")
        edit("Folga de cintura (cm)", "0")
        edit("Comprimento acabado (cm)", "60")
        edit("Margem de costura da cintura (cm)", "0")
        edit("Margem de barra (cm)", "0")
        click("Calcular")
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
        gate.complete(Unit)
        click("Usar perfil")
        composeRule.onNodeWithText("Usar perfil: Existente — 80 cm — 12/10/2026")
            .assertIsDisplayed()
    }

    @Test
    fun saveFailure_keepsFormAndExistingRecordsWithoutSuccess() {
        showProfiles(FailingProfiles(failSave = true))
        click("Perfis de medidas")
        click("Novo perfil")
        edit("Nome ou apelido", "Nova")
        edit("Cintura do perfil (cm)", "90")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        click("Salvar perfil")
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo()
        composeRule.onNodeWithText("Existente")
            .performScrollTo().assertIsDisplayed()
    }

    private fun showProfiles(storage: ProfileRepository) {
        composeRule.runOnUiThread {
            val profiles = ProfilesViewModel(storage, SavedStateHandle())
            val calculator = SkirtCalculatorViewModel(androidx.lifecycle.SavedStateHandle())
            composeRule.activity.setContent {
                MoldaTheme {
                    com.almeidajr.molda.MoldaRoute(calculator = calculator, profiles = profiles)
                }
            }
        }
    }

    private fun edit(label: String, value: String) {
        composeRule.onNodeWithText(label).performScrollTo().performTextReplacement(value)
    }

    private fun click(label: String) {
        val node = composeRule.onNodeWithText(label)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}

private class FailingProfiles(
    var failLoad: Boolean = false,
    val failSave: Boolean = false,
    val loadGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null,
) : ProfileRepository {
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
        loadGate?.await()
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
