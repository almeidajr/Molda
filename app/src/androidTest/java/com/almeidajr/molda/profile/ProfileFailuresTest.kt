package com.almeidajr.molda.profile

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileFailuresTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun saving_pendingStorageKeepsEditorAndDisablesDuplicateMutation() {
        val gate = CompletableDeferred<Unit>()
        showProfiles(FailingProfiles(saveGate = gate))
        click("Perfis de medidas")
        click("Novo perfil")
        edit("Nome ou apelido", "Nova")
        edit("Cintura do perfil (cm)", "90")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        click("Salvar perfil")
        composeRule.onNodeWithText("Salvando…").assertIsNotEnabled()
        composeRule.onNodeWithText("Cancelar").assertIsNotEnabled()
        composeRule.onNodeWithText("Nome ou apelido").assertIsNotEnabled()
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Escolher data no calendário").assertIsNotEnabled()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithText("Novo perfil de medidas").performScrollTo().assertIsDisplayed()
        gate.complete(Unit)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Nome ou apelido")).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Nova").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()
    }

    @Test
    fun deleting_pendingStorageDisablesMutationsAndFailureKeepsRecords() {
        val gate = CompletableDeferred<Unit>()
        val storage = FailingProfiles(deleteGate = gate)
        showProfiles(storage)
        click("Perfis de medidas")
        click("Ações: Existente")
        click("Excluir perfil")
        click("Confirmar exclusão")
        composeRule.onNodeWithText("Excluindo Existente…").assertIsDisplayed()
        composeRule.onNodeWithText("Ações: Existente").assertIsNotEnabled()
        composeRule.onNodeWithText("Ações: Outra pessoa").assertIsNotEnabled()
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertDoesNotExist()
        gate.complete(Unit)
        composeRule.onNodeWithText("Não foi possível excluir o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Existente").assertIsDisplayed()
        runBlocking { assertEquals(2, storage.list().size) }
    }

    @Test
    fun legacyFutureDate_staysReadableAndRequiresCorrectionOnExplicitSave() {
        val storage = FailingProfiles(measurementDate = LocalDate.of(2026, 10, 14))
        showProfiles(storage)
        click("Usar perfil")
        composeRule.onNodeWithText(
            "Usar perfil: Existente — 80 cm — 14/10/2026",
        ).assertIsDisplayed()
        click("Cancelar seleção")
        click("Perfis de medidas")
        click("Existente", useUnmergedTree = true)
        click("Salvar alterações")
        composeRule.onNodeWithText("A medida já deve ter sido tomada. Corrija a data futura.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertTextContains("14/10/2026")
        click("Cancelar")
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Existente").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun updateAndDeleteFailures_keepSavedRecordsDraftAndCalculationWithoutSuccess() {
        val storage = FailingProfiles()
        val original = runBlocking { storage.list() }
        showProfiles(storage)
        edit("Circunferência da cintura (cm)", "95")
        edit("Comprimento acabado (cm)", "60")
        click("Perfis de medidas")
        click("Existente", useUnmergedTree = true)
        edit("Nome ou apelido", "Alterado")
        edit("Cintura do perfil (cm)", "82")
        click("Salvar alterações")
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido")
            .performScrollTo().assertTextContains("Alterado")
        composeRule.onNodeWithText("Cintura do perfil (cm)")
            .performScrollTo().assertTextContains("82")
        click("Cancelar")
        click("Descartar")
        click("Ações: Existente")
        click("Excluir perfil")
        click("Confirmar exclusão")
        composeRule.onNodeWithText("Não foi possível excluir o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Existente")
            .performScrollTo().assertIsDisplayed()
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
                hasText(
                    "Existente",
                ),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Existente")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingProfiles_manualCalculationRemainsAvailable() {
        val gate = CompletableDeferred<Unit>()
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
            .assertTextContains("Nova")
        click("Cancelar")
        click("Descartar")
        composeRule.onNodeWithText("Existente")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun wideDeleteFailure_announcesOnce_andKeepsOtherEditorDraft() {
        showProfiles(FailingProfiles(), wide = true)
        click("Perfis de medidas")
        click("Existente", useUnmergedTree = true)
        edit("Nome ou apelido", "Rascunho")
        Espresso.closeSoftKeyboard()
        click("Ações: Outra pessoa")
        click("Excluir perfil")
        click("Confirmar exclusão")
        composeRule.onAllNodesWithText("Não foi possível excluir o perfil.", substring = true)
            .assertCountEquals(1)
        composeRule.onNodeWithText("Não foi possível excluir o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            )
        composeRule.onNodeWithText(
            "Nome ou apelido",
        ).performScrollTo().assertTextContains("Rascunho")
        composeRule.onNodeWithText("Outra pessoa").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertDoesNotExist()
    }

    private fun showProfiles(storage: ProfileRepository, wide: Boolean = false) {
        composeRule.runOnUiThread {
            val profiles = ProfilesViewModel(
                storage,
                SavedStateHandle(),
                Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC),
            )
            val calculator = SkirtCalculatorViewModel(SavedStateHandle())
            composeRule.activity.setContent {
                MoldaTheme {
                    if (wide) {
                        DeviceConfigurationOverride(
                            DeviceConfigurationOverride.ForcedSize(DpSize(1200.dp, 2700.dp)),
                        ) {
                            MoldaRoute(
                                calculator = calculator,
                                profiles = profiles,
                            )
                        }
                    } else {
                        MoldaRoute(calculator = calculator, profiles = profiles)
                    }
                }
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

    private fun click(label: String, useUnmergedTree: Boolean = false) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                hasText(label) and isEnabled() and
                    !hasAnyAncestor(!isEnabled()),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val node = composeRule.onNodeWithText(label, useUnmergedTree = useUnmergedTree)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}

private class FailingProfiles(
    var failLoad: Boolean = false,
    val failSave: Boolean = false,
    val loadGate: CompletableDeferred<Unit>? = null,
    val deleteGate: CompletableDeferred<Unit>? = null,
    val saveGate: CompletableDeferred<Unit>? = null,
    val measurementDate: LocalDate = LocalDate.of(2026, 10, 12),
) : ProfileRepository {
    private val records =
        listOf(
            MeasurementProfile(
                1,
                ProfileMeasurements("Existente", 80.0, measurementDate),
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
        saveGate?.await()
        if (failSave) error("Controlled save failure")
        return MeasurementProfile(3, measurements)
    }
    override suspend fun update(profile: MeasurementProfile) = error("Controlled update failure")
    override suspend fun delete(id: Long) {
        deleteGate?.await()
        error("Controlled delete failure")
    }
}
