package com.almeidajr.molda.profile

import android.view.KeyEvent
import android.view.WindowInsets
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.assertDrawnHeightIsAtLeast
import com.almeidajr.molda.assertReadableTextContrast
import com.almeidajr.molda.assertSurfaceStandsOut
import com.almeidajr.molda.data.room.openMoldaDatabase
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfilePresentationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val databaseName = "profile-presentation-test.db"
    private lateinit var storage: LocalProfileRepository

    @Before
    fun showIsolatedProfiles() {
        composeRule.activity.deleteDatabase(databaseName)
        storage =
            LocalProfileRepository(openMoldaDatabase(composeRule.activity, databaseName).profiles())
        showContent()
        composeRule.showSkirtCalculator()
    }

    private fun showContent(
        fontScale: Float = 1f,
        width: Int = 400,
        dark: Boolean = false,
        dynamic: Boolean = true,
    ) {
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
                        MoldaTheme(darkTheme = dark, dynamicColor = dynamic) {
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
    fun managementRows_showNotesToDistinguishOtherwiseIdenticalProfiles() {
        manage()
        listOf("Medida com roupa leve", "Conferida antes do corte").forEach { note ->
            click("Novo perfil")
            edit("Nome ou apelido", "Ana")
            edit("Cintura do perfil (cm)", "80")
            edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
            edit("Observações (opcional)", note)
            saveProfile()
        }
        composeRule.onNodeWithText("Medida com roupa leve").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Conferida antes do corte").performScrollTo().assertIsDisplayed()
        composeRule.showSkirtCalculator()
        click("Usar perfil")
        composeRule.onNodeWithText("Conferida antes do corte").performScrollTo().performClick()
        composeRule.onNodeWithText("Cintura copiada do perfil: Ana").assertIsDisplayed()
    }

    @Test
    fun newProfile_explainsExplicitDateAndOptionalNotes_beforeSaving() {
        manage()
        click("Novo perfil")
        composeRule.onNodeWithText("Identificação e medidas").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Escolha a data em que a medida foi tomada, sem usar uma data futura.",
        )
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Use observações para distinguir perfis com o mesmo nome.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun profileGuidanceErrorsAndRowNames_useReadableAgreedTypography() {
        manage()
        click("Novo perfil")
        listOf(
            "Nomes repetidos são permitidos.",
            "Escolha a data em que a medida foi tomada, sem usar uma data futura.",
        ).forEach { assertTypography(it, 14, 20) }
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80")
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)")
            .performScrollTo().performTextReplacement("32/02/2026")
        click("Salvar perfil")
        assertTypography(
            "Informe uma data válida no formato dd/mm/aaaa.",
            14,
            20,
        )
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        saveProfile()
        assertTypography("Ana", 16, 24, FontWeight.Medium)
        composeRule.showSkirtCalculator()
        click("Usar perfil")
        assertTypography("Usar perfil: Ana — 80 cm — 12/10/2026", 16, 24, FontWeight.Medium)
    }

    @Test
    fun pickerRows_standOutFromTheSheet_inEveryScheme() {
        manage()
        listOf("Ana" to "80", "Bia" to "72").forEach { (name, waist) ->
            click("Novo perfil")
            edit("Nome ou apelido", name)
            edit("Cintura do perfil (cm)", waist)
            edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
            saveProfile()
        }
        composeRule.showSkirtCalculator()
        listOf(
            false to false,
            true to false,
            false to true,
            true to true,
        ).forEach { (dark, dynamic) ->
            showContent(dark = dark, dynamic = dynamic)
            click("Usar perfil")
            listOf("Ana — 80", "Bia — 72").forEach {
                val label = "Usar perfil: $it cm — 12/10/2026"
                val context = "$label dark=$dark dynamic=$dynamic"
                composeRule.onNodeWithText(label).performScrollTo()
                    .assertSurfaceStandsOut("$context row")
                composeRule.onNodeWithText(label, useUnmergedTree = true)
                    .assertReadableTextContrast(context)
            }
            click("Cancelar seleção")
        }
    }

    @Test
    fun profileButtons_drawAtLeast48dpTall_atDefaultTextSize() {
        manage()
        assertDrawnHeight("Perfis")
        click("Novo perfil")
        assertDrawnHeight("Salvar perfil", "Cancelar")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        saveProfile()
        assertDrawnHeight("Ações: Ana")
        click("Ações: Ana")
        click("Excluir perfil")
        assertDrawnHeight("Confirmar exclusão", "Cancelar")
        click("Cancelar")
        composeRule.showSkirtCalculator()
        click("Usar perfil")
        assertDrawnHeight("Cancelar seleção", "Novo perfil")
    }

    @Test
    fun keyboard_editFromProfileRow_doesNotFocusAField() {
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        saveProfile()
        Espresso.closeSoftKeyboard()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // A real key event leaves touch mode, so the row joins keyboard focus traversal.
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        composeRule.onNodeWithText("Ana").requestFocus().assertIsFocused()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        composeRule.onNodeWithText("Editar perfil de medidas").assertIsDisplayed()
        // The list leaves after the editor arrives; losing its focused row must not refocus a field.
        composeRule.onNodeWithText("Nome ou apelido").assertIsNotFocused()
        composeRule.runOnUiThread {
            assertEquals(
                false,
                composeRule.activity.window.decorView.rootWindowInsets
                    .isVisible(WindowInsets.Type.ime()),
            )
        }
    }

    private fun assertDrawnHeight(vararg labels: String) = labels.forEach {
        val node = composeRule.onNodeWithText(it)
        if (!node.isDisplayed()) node.performScrollTo()
        node.assertDrawnHeightIsAtLeast(48.dp)
    }

    private fun assertTypography(
        label: String,
        size: Int,
        height: Int,
        weight: FontWeight? = null,
    ) {
        val text = composeRule.onNodeWithText(label, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            assertTrue(it(layouts))
        }
        val style = layouts.single().layoutInput.style
        assertEquals("$label size", size.sp, style.fontSize)
        assertEquals("$label line height", height.sp, style.lineHeight)
        if (weight != null) assertEquals("$label weight", weight, style.fontWeight)
    }

    @Test
    fun enlargedText_themeVariants_keepProfileRowsAndSeparateActionsReachable() {
        manage()
        click("Novo perfil")
        val name = "Ana Maria da Silva — medidas conferidas para a saia"
        edit("Nome ou apelido", name)
        edit("Cintura do perfil (cm)", "80")
        edit("Data da medida (dd/mm/aaaa)", "12/10/2026")
        edit("Observações (opcional)", "Conferida antes do corte")
        saveProfile()
        listOf(
            false to false,
            true to false,
            false to true,
            true to true,
        ).forEach { (dark, dynamic) ->
            showContent(fontScale = 2f, dark = dark, dynamic = dynamic)
            composeRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
            composeRule.onNodeWithText("Conferida antes do corte").assertIsDisplayed()
            listOf(name, "Conferida antes do corte").forEach { label ->
                val text = composeRule.onNodeWithText(label, useUnmergedTree = true)
                text.assertReadableTextContrast("$label dark=$dark dynamic=$dynamic")
            }
            composeRule.onNodeWithText("Ações: $name").performScrollTo().assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            composeRule.onNodeWithContentDescription("Novo perfil").assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            click("Ações: $name")
            click("Editar perfil")
            composeRule.onNodeWithText("Salvar alterações").performScrollTo().assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            click("Cancelar")
        }
    }

    @Test
    fun directDateEntry_usesDayMonthYear_andFailedEditsPreserveSavedDate() {
        manage()
        click("Novo perfil")
        edit("Nome ou apelido", "Ana")
        edit("Cintura do perfil (cm)", "80")
        val date = composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)")
        date.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
        date.performScrollTo().performTextReplacement("32/02/2026")
        click("Salvar perfil")
        composeRule.onNodeWithText("Informe uma data válida no formato dd/mm/aaaa.")
            .performScrollTo().assertIsDisplayed()
        date.assertTextContains("32/02/2026")
        date.performTextReplacement("12/10/0000")
        click("Salvar perfil")
        composeRule.onNodeWithText("Informe uma data válida no formato dd/mm/aaaa.")
            .performScrollTo().assertIsDisplayed()
        date.assertTextContains("12/10/0000")
        date.performTextReplacement("14/10/2026")
        click("Salvar perfil")
        composeRule.onNodeWithText("A medida já deve ter sido tomada. Corrija a data futura.")
            .performScrollTo().assertIsDisplayed()
        date.assertTextContains("14/10/2026")
        date.performTextReplacement("12/10/2026")
        saveProfile()
        composeRule.onNodeWithText("80 cm — 12/10/2026").performScrollTo().assertIsDisplayed()
        click("Ana")
        date.performScrollTo().performTextReplacement("14/10/2026")
        click("Salvar alterações")
        composeRule.onNodeWithText("A medida já deve ter sido tomada. Corrija a data futura.")
            .performScrollTo().assertIsDisplayed()
        click("Cancelar")
        click("Descartar")
        click("Ana")
        date.performScrollTo().assertTextContains("12/10/2026")
    }

    private fun edit(label: String, value: String) {
        if (label == "Data da medida (dd/mm/aaaa)") {
            composeRule.enterProfileDate(value)
        } else {
            composeRule.onNodeWithText(label).performScrollTo().performTextReplacement(value)
        }
    }

    private fun manage() = composeRule.showProfiles()

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
                hasText(label) or hasContentDescription(label) and isEnabled() and
                    !hasAnyAncestor(!isEnabled()),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val node = composeRule.onNode(hasText(label) or hasContentDescription(label))
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}
