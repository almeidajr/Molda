package com.almeidajr.molda.project

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.unit.DpSize
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.project.domain.PatternDraftKind
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.ui.ProjectsViewModel
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * "Saia da formatura", saved at [savedAt]: an 80 cm waist from Ana's profile measured on
 * 12/10/2026, with radii that differ from a recalculation so a test can tell stored from
 * recalculated.
 */
internal fun formaturaProject(savedAt: Instant) = Project(
    ProjectId(1),
    ProjectName("Saia da formatura"),
    PatternDraftKind.FullCircleSkirt,
    SkirtSnapshot(
        SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
        SkirtRadii(Length(11.11), Length(71.11), Length(10.11), Length(73.11)),
        MeasurementSource("Ana", LocalDate.of(2026, 10, 12)),
    ),
    createdAt = savedAt,
    updatedAt = savedAt,
)

/** The name field of the open save or rename dialog. */
internal fun SemanticsNodeInteractionsProvider.nameField(): SemanticsNodeInteraction =
    onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))

/**
 * Shows the app on [profiles] and [projects], at [size] when given, inside [frame] (the theme by
 * default). Recreation restores the app's own content, so call it again after recreating the
 * activity.
 */
internal fun AndroidComposeTestRule<*, MainActivity>.showRoute(
    profiles: ProfileRepository,
    projects: ProjectRepository,
    clock: Clock,
    size: DpSize? = null,
    frame: @Composable (route: @Composable () -> Unit) -> Unit = { MoldaTheme(content = it) },
) = runOnUiThread {
    activity.setContent {
        val profilesViewModel: ProfilesViewModel = viewModel(
            key = "isolatedProfiles",
            factory = viewModelFactory {
                initializer { ProfilesViewModel(profiles, createSavedStateHandle(), clock) }
            },
        )
        val projectsViewModel: ProjectsViewModel = viewModel(
            key = "isolatedProjects",
            factory = viewModelFactory {
                initializer { ProjectsViewModel(projects, clock, createSavedStateHandle()) }
            },
        )
        val calculator: SkirtCalculatorViewModel = viewModel()
        val route = @Composable {
            frame {
                MoldaRoute(
                    calculator = calculator,
                    profiles = profilesViewModel,
                    projects = projectsViewModel,
                )
            }
        }
        if (size == null) {
            route()
        } else {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size), route)
        }
    }
}
