package com.almeidajr.molda

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry

/** The navigation suite item labelled [label]: Moldes, Projetos, Guias or Perfis. */
internal fun SemanticsNodeInteractionsProvider.destination(
    label: String,
): SemanticsNodeInteraction = onNode(hasText(label) and isSelectable())

/** Selects the top-level destination labelled [label]. */
internal fun SemanticsNodeInteractionsProvider.selectDestination(label: String) {
    destination(label).performClick()
}

/** The top app bar heading reading [title]. */
internal fun SemanticsNodeInteractionsProvider.screenTitle(
    title: String,
): SemanticsNodeInteraction = onNode(hasText(title) and isHeading())

/** Opens the full-circle skirt calculator from the Moldes catalog the app starts on. */
internal fun SemanticsNodeInteractionsProvider.openSkirtCalculator() {
    onNode(hasText(SkirtTitle) and !isHeading()).performClick()
    screenTitle(SkirtTitle).assertExists()
}

/** Shows the skirt calculator: selects Moldes when needed, then opens the skirt from the catalog. */
internal fun SemanticsNodeInteractionsProvider.showSkirtCalculator() {
    showDestination("Moldes")
    if (onAllNodes(hasText(SkirtTitle) and isHeading()).fetchSemanticsNodes().isEmpty()) {
        openSkirtCalculator()
    }
}

/** Shows Perfis, keeping any open editor: re-selecting it would return to its root. */
internal fun SemanticsNodeInteractionsProvider.showProfiles() = showDestination("Perfis")

private fun SemanticsNodeInteractionsProvider.showDestination(label: String) {
    if (onAllNodes(hasText(label) and isSelectable() and isSelected()).fetchSemanticsNodes()
            .isEmpty()
    ) {
        selectDestination(label)
    }
}

/** The skirt's catalog entry and calculator title as the app renders it. */
internal val SkirtTitle: String
    get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(
        R.string.skirt_title,
    )
