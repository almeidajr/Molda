package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// Delay-then-hold: quick loads never flash the indicator, and a shown one never just blinks.
private val IndicatorDelay = 400.milliseconds
private val IndicatorMinDuration = 500.milliseconds

/**
 * The drawing frame's loading indicator: it shows once [isLoading] has lasted [IndicatorDelay],
 * then stays at least [IndicatorMinDuration], covering a drawing that loads meanwhile so it appears
 * once. A frame loads only once, so [isLoading] only ever turns false.
 */
// LoadingIndicator is Material 3's expressive indicator for short waits; still experimental in 1.5.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GuideLoadingIndicator(isLoading: Boolean, modifier: Modifier = Modifier) {
    var isShown by remember { mutableStateOf(false) }
    val isStillLoading by rememberUpdatedState(isLoading)
    LaunchedEffect(Unit) {
        delay(IndicatorDelay)
        if (!isStillLoading) return@LaunchedEffect
        isShown = true
        delay(IndicatorMinDuration)
        snapshotFlow { isStillLoading }.first { !it }
        isShown = false
    }
    if (isShown) {
        Box(
            modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { LoadingIndicator() }
    }
}
