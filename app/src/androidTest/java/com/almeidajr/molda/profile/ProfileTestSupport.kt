package com.almeidajr.molda.profile

import android.text.format.DateFormat
import androidx.compose.material3.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
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
import com.almeidajr.molda.MainActivityLauncher
import com.almeidajr.molda.RouteFrame
import com.almeidajr.molda.TestBindings
import com.almeidajr.molda.awaitActivityFocus
import com.almeidajr.molda.click
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.dialogTextField
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
 * The profile tests' frame: the theme given, with [fontScale], forced to [width] dp when given.
 */
internal fun profileFrame(
    fontScale: Float = 1f,
    width: Int? = null,
    isDarkTheme: Boolean = false,
): RouteFrame = { route ->
    val theme = @Composable {
        MoldaTheme(isDarkTheme = isDarkTheme, content = route)
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

/** Launches the app on [profiles] in [profileFrame] and opens the skirt calculator. */
internal fun AndroidComposeTestRule<MainActivityLauncher, MainActivity>.launchCalculator(
    profiles: ProfileRepository = TestBindings.profiles,
) {
    launchApp(profiles = profiles, frame = profileFrame())
    showSkirtCalculator()
}

/** Launches the app on [profiles] in [profileFrame] and selects Perfis. */
internal fun AndroidComposeTestRule<MainActivityLauncher, MainActivity>.launchProfiles(
    profiles: ProfileRepository = TestBindings.profiles,
) {
    launchApp(profiles = profiles, frame = profileFrame())
    showProfiles()
}

/** Replaces the text of the field labeled [label]. */
internal fun SemanticsNodeInteractionsProvider.edit(label: String, value: String) {
    onNodeWithText(label).performScrollTo().performTextReplacement(value)
}

/** Leaves a new profile's editor open in Perfis, unsaved, with "Rascunho" typed as its name. */
internal fun AndroidComposeTestRule<*, *>.leaveUnsavedNewProfile() {
    selectDestination("Perfis")
    click("Novo perfil")
    edit("Nome ou apelido", "Rascunho")
    closeKeyboard()
}

/** Fills the open profile editor's name, waist and, through the calendar, date. */
internal fun AndroidComposeTestRule<*, *>.fillProfile(
    name: String,
    waist: String = "80",
    date: LocalDate = LocalDate.of(2026, 10, 12),
) {
    edit("Nome ou apelido", name)
    edit("Cintura do perfil (cm)", waist)
    pickDate(date)
}

/** Picks [date] in the calendar, which must open on its month, and confirms it. */
internal fun AndroidComposeTestRule<*, *>.pickDate(date: LocalDate) {
    openCalendar()
    onNodeWithText(calendarDay(date), ignoreCase = true).performClick()
    click("Confirmar data")
    // The closing calendar keeps input focus for a moment, and a text field tapped meanwhile can
    // miss its keyboard.
    awaitActivityFocus()
}

/** The calendar's label for [date]'s day. */
internal fun AndroidComposeTestRule<*, *>.calendarDay(date: LocalDate): String = date.format(
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withLocale(activity.resources.configuration.locales[0]),
)

/**
 * Opens the calendar from the date field, with the keyboard closed: a keyboard still opening from
 * typing in the editor can take the tap on the calendar button.
 */
internal fun AndroidComposeTestRule<*, *>.openCalendar() {
    closeKeyboard()
    onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo()
    onNodeWithContentDescription("Escolher data no calendário").performClick()
}

/**
 * Opens the calendar in its text input mode and types [value] (dd/mm/yyyy) in the device's order.
 */
// Material 3 exposes the localized mode toggle label only as a private resource.
@Suppress("PrivateResource")
internal fun AndroidComposeTestRule<*, *>.typeInCalendar(value: String) {
    openCalendar()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onNodeWithContentDescription(context.getString(R.string.m3c_date_picker_switch_to_input_mode))
        .performClick()
    val (day, month, year) = value.split('/')
    val parts = mapOf('d' to day, 'M' to month, 'y' to year)
    val input = DateFormat.getDateFormatOrder(context).joinToString("") { parts.getValue(it) }
    dialogTextField().performTextReplacement(input)
}
