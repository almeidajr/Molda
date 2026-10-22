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
import com.almeidajr.molda.R
import com.almeidajr.molda.guide.navigation.ExampleFabricFoldingGuideKey
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.skirt.navigation.FabricFoldingGuideKey
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar
import com.almeidajr.molda.ui.navigation.Navigator

/**
 * The skirt calculator on Moldes with its folding guide, and the example guide Guias opens;
 * [onSaveProject] saves the draft as a project named after the pattern, and [onCalculateOwn] leaves
 * the example guide for the calculator.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.skirtSection(
    navigator: Navigator,
    calculator: SkirtCalculatorViewModel,
    onSaveProject: (patternName: String) -> Unit,
    onCalculateOwn: () -> Unit,
) {
    entry<SkirtCalculatorKey> {
        CalculatorDestination(
            calculator,
            title = stringResource(R.string.skirt_title),
            onUp = navigator::back,
            onUseProfile = { navigator.push(ProfilePickerKey) },
            onOpenGuide = { navigator.push(FabricFoldingGuideKey) },
            onSaveProject = onSaveProject,
        )
    }
    entry<FabricFoldingGuideKey> {
        val state by calculator.state.collectAsStateWithLifecycle()
        CalculatedFoldingGuide(state.radii) { navigator.remove(FabricFoldingGuideKey) }
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

/** The skirt calculator; with [onSaveAsNewProject] it edits a saved project. */
@Composable
internal fun CalculatorDestination(
    calculator: SkirtCalculatorViewModel,
    title: String,
    onUp: () -> Unit,
    onUseProfile: () -> Unit,
    onOpenGuide: () -> Unit,
    onSaveProject: (patternName: String) -> Unit,
    onSaveAsNewProject: ((patternName: String) -> Unit)? = null,
) {
    val state by calculator.state.collectAsStateWithLifecycle()
    val patternName = stringResource(R.string.project_pattern_skirt)
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
            onSaveProject = { onSaveProject(patternName) },
            onSaveAsNewProject = onSaveAsNewProject?.let { { it(patternName) } },
        )
    }
}
