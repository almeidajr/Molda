package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEffect
import com.almeidajr.molda.R
import com.almeidajr.molda.guide.navigation.ExampleFabricFoldingGuideKey
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.skirt.navigation.FabricFoldingGuideKey
import com.almeidajr.molda.skirt.navigation.MOLDES_PROFILE_RESULT_KEY
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar
import com.almeidajr.molda.ui.navigation.Navigator

/**
 * The skirt calculator on Moldes with its folding guide, and the example guide Guias opens;
 * [onSaveProject] saves the draft as a project, and [onCalculateOwn] leaves the example guide
 * for the calculator.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.skirtSection(
    navigator: Navigator,
    calculator: SkirtCalculatorViewModel,
    onSaveProject: () -> Unit,
    onCalculateOwn: () -> Unit,
) {
    entry<SkirtCalculatorKey> {
        ResultEffect<MeasurementProfile>(MOLDES_PROFILE_RESULT_KEY) { profile ->
            calculator.useProfile(profile)
        }
        CalculatorDestination(
            calculator,
            title = stringResource(R.string.skirt_title),
            onUp = navigator::back,
            onUseProfile = { navigator.push(ProfilePickerKey(MOLDES_PROFILE_RESULT_KEY)) },
            onOpenGuide = { navigator.push(FabricFoldingGuideKey) },
            onSaveProject = onSaveProject,
        )
    }
    entry<FabricFoldingGuideKey> {
        val state by calculator.state.collectAsStateWithLifecycle()
        CalculatedFoldingGuide(state.result?.radii) { navigator.remove(FabricFoldingGuideKey) }
    }
    entry<ExampleFabricFoldingGuideKey>(metadata = ListDetailSceneStrategy.detailPane()) {
        FabricFoldingPlayer(
            ExampleSkirtRadii,
            onClose = { navigator.remove(ExampleFabricFoldingGuideKey) },
            // Beside the list the guide is a pane, closed by choosing from the list or Back.
            isCloseShown = !LocalIsTwoPane.current,
            onCalculateOwn = onCalculateOwn,
        )
    }
}

/** Fills the waist with [profile]'s, copied from it. */
internal fun SkirtCalculatorViewModel.useProfile(profile: MeasurementProfile) =
    selectProfile(profile.measurements.waist, profile.measurements.source)

/** The skirt calculator; with [onSaveAsNewProject] it edits a saved project. */
@Composable
internal fun CalculatorDestination(
    calculator: SkirtCalculatorViewModel,
    title: String,
    onUp: () -> Unit,
    onUseProfile: () -> Unit,
    onOpenGuide: () -> Unit,
    onSaveProject: () -> Unit,
    onSaveAsNewProject: (() -> Unit)? = null,
) {
    val state by calculator.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        MoldaTopAppBar(title, onUp = onUp)
        SkirtCalculatorScreen(
            state,
            calculator::edit,
            calculator::calculate,
            Modifier.weight(1f),
            onBlur = calculator::blur,
            onUseProfile = onUseProfile,
            onOpenGuide = onOpenGuide,
            onSaveProject = onSaveProject,
            onSaveAsNewProject = onSaveAsNewProject,
        )
    }
}
