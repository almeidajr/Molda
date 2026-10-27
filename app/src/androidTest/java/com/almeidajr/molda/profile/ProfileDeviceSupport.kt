package com.almeidajr.molda.profile

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.almeidajr.molda.awaitActivityFocus
import com.almeidajr.molda.closeKeyboard
import java.time.LocalDate

/**
 * [fillProfile] on a device: closes the keyboard before the calendar, which a keyboard still
 * opening could take the tap from, then waits for the editor's focus to return.
 */
internal fun AndroidComposeTestRule<*, *>.fillProfileOnDevice(name: String, waist: String = "80") {
    edit("Nome ou apelido", name)
    edit("Cintura do perfil (cm)", waist)
    closeKeyboard()
    pickDate(LocalDate.of(2026, 10, 12))
    // The closing calendar keeps input focus for a moment, and a text field tapped meanwhile can
    // miss its keyboard.
    awaitActivityFocus()
}
