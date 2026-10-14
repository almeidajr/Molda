package com.almeidajr.molda.profile

import android.text.format.DateFormat
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

internal fun ComposeTestRule.openProfileDateInput(value: String) {
    onNodeWithText("Data da medida (dd/mm/aaaa)").performScrollTo().performClick()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    onNodeWithContentDescription(
        context.getString(androidx.compose.material3.R.string.m3c_date_picker_switch_to_input_mode),
    ).performClick()
    val (day, month, year) = value.split('/')
    val parts = mapOf('d' to day, 'M' to month, 'y' to year)
    val input = DateFormat.getDateFormatOrder(context).joinToString("") { parts.getValue(it) }
    onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement(input)
}

internal fun ComposeTestRule.enterProfileDate(value: String) {
    openProfileDateInput(value)
    onNodeWithText("Confirmar data").performClick()
}
