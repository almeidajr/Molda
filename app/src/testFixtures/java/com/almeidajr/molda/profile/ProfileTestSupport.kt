package com.almeidajr.molda.profile

import androidx.activity.ComponentActivity
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
import com.almeidajr.molda.AppLauncher
import com.almeidajr.molda.RouteFrame
import com.almeidajr.molda.TestBindings
import com.almeidajr.molda.click
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** A stored profile measured on 12/10/2026 unless [date] says otherwise. */
fun profile(
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
fun profileFrame(
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
fun <A : ComponentActivity> AndroidComposeTestRule<AppLauncher<A>, A>.launchCalculator(
    profiles: ProfileRepository = TestBindings.profiles,
) {
    launchApp(profiles = profiles, frame = profileFrame())
    showSkirtCalculator()
}

/** Launches the app on [profiles] in [profileFrame] and selects Perfis. */
fun <A : ComponentActivity> AndroidComposeTestRule<AppLauncher<A>, A>.launchProfiles(
    profiles: ProfileRepository = TestBindings.profiles,
) {
    launchApp(profiles = profiles, frame = profileFrame())
    showProfiles()
}

/** Replaces the text of the field labeled [label]. */
fun SemanticsNodeInteractionsProvider.edit(label: String, value: String) {
    onNodeWithText(label).performScrollTo().performTextReplacement(value)
}

/**
 * Fills the open profile editor's name, waist and, through the calendar, date. Device tests use
 * `fillProfileOnDevice`, which keeps the keyboard out of the calendar's way.
 */
fun AndroidComposeTestRule<*, *>.fillProfile(
    name: String,
    waist: String = "80",
    date: LocalDate = LocalDate.of(2026, 10, 12),
) {
    edit("Nome ou apelido", name)
    edit("Cintura do perfil (cm)", waist)
    pickDate(date)
}

/** Picks [date] in the calendar, which must open on its month, and confirms it. */
fun AndroidComposeTestRule<*, *>.pickDate(date: LocalDate) {
    openCalendar()
    onNodeWithText(calendarDay(date), ignoreCase = true).performClick()
    click("Confirmar data")
}

/** The calendar's label for [date]'s day. */
fun AndroidComposeTestRule<*, *>.calendarDay(date: LocalDate): String = date.format(
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withLocale(activity.resources.configuration.locales[0]),
)

/** Opens the calendar from the date field. */
fun SemanticsNodeInteractionsProvider.openCalendar() {
    onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo()
    onNodeWithContentDescription("Escolher data no calendário").performClick()
}
