package com.almeidajr.molda.profile

import android.text.format.DateFormat
import android.view.KeyEvent
import androidx.compose.material3.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.showRoute
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** "Today" in every profile screen test: 13/10/2026, so 14/10/2026 is a future date. */
internal val ProfileTestClock: Clock =
    Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)

/** A stored profile measured on 12/10/2026 unless [date] says otherwise. */
internal fun profile(
    id: Long,
    name: String,
    waist: Double = 80.0,
    date: LocalDate = LocalDate.of(2026, 10, 12),
    notes: String = "",
) = MeasurementProfile(
    ProfileId(id),
    ProfileMeasurements(ProfileName(name), Length(waist), date, notes),
)

/**
 * Shows the app on [profiles] at [ProfileTestClock], with [fontScale] and the theme given, forced
 * to [width] dp when given. Recreation restores the app's own content, so call it again after
 * recreating the activity.
 */
internal fun AndroidComposeTestRule<*, MainActivity>.showProfileRoute(
    profiles: ProfileRepository,
    fontScale: Float = 1f,
    width: Int? = null,
    dark: Boolean = false,
    dynamic: Boolean = true,
) = showRoute(profiles, FakeProjectRepository(), ProfileTestClock) { route ->
    val theme = @Composable {
        MoldaTheme(darkTheme = dark, dynamicColor = dynamic, content = route)
    }
    val window = LocalWindowInfo.current.containerSize
    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
        if (width == null) {
            theme()
        } else {
            // Forced density does not scale native IME insets; keep the physical proportions.
            val height = maxOf(900f, width * window.height.toFloat() / window.width)
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, height.dp)),
                theme,
            )
        }
    }
}

/** Replaces the text of the field labelled [label]. */
internal fun SemanticsNodeInteractionsProvider.edit(label: String, value: String) {
    onNodeWithText(label).performScrollTo().performTextReplacement(value)
}

/** Clicks the node whose text or content description is [label], scrolling to it when hidden. */
internal fun SemanticsNodeInteractionsProvider.click(
    label: String,
    useUnmergedTree: Boolean = false,
) {
    val node = onNode(hasText(label) or hasContentDescription(label), useUnmergedTree)
    if (!node.isDisplayed()) node.performScrollTo()
    node.performClick()
}

/** Fills the open profile editor's name, waist and date. */
internal fun SemanticsNodeInteractionsProvider.fillProfile(
    name: String,
    waist: String = "80",
    date: String = "12/10/2026",
) {
    edit("Nome ou apelido", name)
    edit("Cintura do perfil (cm)", waist)
    edit("Data da medida (dd/mm/aaaa)", date)
}

/** Opens the calendar from the date field. */
internal fun SemanticsNodeInteractionsProvider.openCalendar() {
    onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo()
    onNodeWithContentDescription("Escolher data no calendário").performClick()
}

/** Opens the calendar in its text input mode and types [value] (dd/mm/yyyy) in the device's order. */
// Material 3 exposes the localized mode toggle label only as a private resource.
@Suppress("PrivateResource")
internal fun SemanticsNodeInteractionsProvider.typeInCalendar(value: String) {
    openCalendar()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onNodeWithContentDescription(context.getString(R.string.m3c_date_picker_switch_to_input_mode))
        .performClick()
    val (day, month, year) = value.split('/')
    val parts = mapOf('d' to day, 'M' to month, 'y' to year)
    val input = DateFormat.getDateFormatOrder(context).joinToString("") { parts.getValue(it) }
    onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement(input)
}

/** Sends a real Back key to the focused window. */
internal fun pressBack() =
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
