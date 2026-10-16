package com.almeidajr.molda.profile

import android.text.format.DateFormat
import androidx.compose.material3.R
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry

// Material 3 exposes the localized mode toggle label only as a private resource.
@Suppress("PrivateResource")
internal fun ComposeTestRule.openProfileDateInput(value: String) {
    onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo()
    onNodeWithContentDescription("Escolher data no calendário").performClick()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onNodeWithContentDescription(
        context.getString(R.string.m3c_date_picker_switch_to_input_mode),
    ).performClick()
    val (day, month, year) = value.split('/')
    val parts = mapOf('d' to day, 'M' to month, 'y' to year)
    val input = DateFormat.getDateFormatOrder(context).joinToString("") { parts.getValue(it) }
    onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement(input)
}

internal fun ComposeTestRule.enterProfileDate(value: String) {
    onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo().performTextReplacement(value)
}
