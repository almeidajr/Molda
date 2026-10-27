package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import com.almeidajr.molda.AnimatorDurationScaleRule
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October18Clock
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/** A saved project's folding guide. On a device: the guide draws native Rive frames. */
@HiltAndroidTest
class OpenProjectTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    @Test
    fun guide_fromProject_showsItsStoredRadii() {
        // Given
        composeRule.launchApp(
            projects = FakeProjectRepository(listOf(formaturaProject(October18Clock.instant()))),
            clock = October18Clock,
        )
        composeRule.selectDestination("Projetos")
        composeRule.clickProject("Saia da formatura")

        // When: the guide's drawings hold still, so Compose goes idle.
        animatorScale.set(0f)
        composeRule.click("Guia de dobra do tecido")
        repeat(3) { composeRule.click("Próximo") }

        // Then
        // The guide's own numbered line, not the project's radii behind it.
        composeRule.onNode(hasText("1. Costura da cintura: 11,11", substring = true))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
