package com.almeidajr.molda.ui.navigation

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** A label that never wraps or ellipsizes: at large text it shrinks to fit, down to 14dp. */
@Composable
fun NavigationLabel(text: String, modifier: Modifier = Modifier) {
    val maximum = LocalTextStyle.current.fontSize
    val minimum = with(LocalDensity.current) { 14.dp.toSp() }
    Text(
        text,
        modifier,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(
            minFontSize = if (minimum < maximum) minimum else maximum,
            maxFontSize = maximum,
        ),
    )
}
