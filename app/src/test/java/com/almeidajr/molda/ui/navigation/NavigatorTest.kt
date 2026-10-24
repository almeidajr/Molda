package com.almeidajr.molda.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.assertEquals
import org.junit.Test

class NavigatorTest {
    private data object Start : NavKey

    private data object Other : NavKey

    private data object Screen : NavKey

    private data object Dialog : NavKey

    private var switches = 0

    private fun navigator(
        start: List<NavKey> = listOf(Start),
        other: List<NavKey> = listOf(Other),
        selected: NavKey = Start,
    ) = navigatorOf(listOf(start, other), selected, onSwitch = { switches++ })

    @Test
    fun push_withoutARoot_addsToTheSelectedStack() {
        // Given
        val navigator = navigator(selected = Other)

        // When
        navigator.push(Screen)

        // Then
        assertEquals(listOf(listOf(Start), listOf(Other, Screen)), navigator.stacks)
    }

    @Test
    fun push_onAnotherRoot_addsToThatStackAndKeepsTheSelection() {
        // Given
        val navigator = navigator()

        // When
        navigator.push(Screen, root = Other)

        // Then
        assertEquals(listOf(listOf(Start), listOf(Other, Screen)), navigator.stacks)
        assertEquals(Start, navigator.state.selected)
    }

    @Test
    fun removeAll_matchingKeysOnEveryStack_removesThemAndKeepsTheRoots() {
        // Given
        val navigator = navigator(
            start = listOf(Start, Screen, Dialog),
            other = listOf(Other, Screen),
        )

        // When
        navigator.removeAll { it == Screen || it == Other }

        // Then
        assertEquals(listOf(listOf(Start, Dialog), listOf(Other)), navigator.stacks)
    }

    @Test
    fun remove_aKeyBelowTheTop_removesOnlyIt() {
        // Given
        val navigator = navigator(start = listOf(Start, Screen, Dialog))

        // When
        navigator.remove(Screen)

        // Then
        assertEquals(listOf(listOf(Start, Dialog), listOf(Other)), navigator.stacks)
    }

    @Test
    fun popTo_aKeyOnTheSelectedStack_popsWhatIsAboveIt() {
        // Given
        val navigator = navigator(other = listOf(Other, Screen, Dialog), selected = Other)

        // When
        navigator.popTo(Screen)

        // Then
        assertEquals(listOf(listOf(Start), listOf(Other, Screen)), navigator.stacks)
    }

    @Test
    fun popTo_aKeyOnlyOnAnotherStack_pushesItOntoTheSelectedOne() {
        // Given
        val navigator = navigator(start = listOf(Start, Screen), other = listOf(Other, Dialog))

        // When
        navigator.popTo(Dialog)

        // Then
        assertEquals(
            listOf(listOf(Start, Screen, Dialog), listOf(Other, Dialog)),
            navigator.stacks,
        )
    }

    @Test
    fun select_anotherDestination_switchesAndKeepsItsStack() {
        // Given
        val navigator = navigator(other = listOf(Other, Screen))

        // When
        navigator.select(Other)

        // Then
        assertEquals(listOf(Other, Screen), navigator.state.current.toList())
        assertEquals(1, switches)
    }

    @Test
    fun select_theSelectedDestination_returnsItToItsRootWithoutSwitching() {
        // Given
        val navigator = navigator(other = listOf(Other, Screen, Dialog), selected = Other)

        // When
        navigator.select(Other)

        // Then
        assertEquals(listOf(listOf(Start), listOf(Other)), navigator.stacks)
        assertEquals(0, switches)
    }

    @Test
    fun back_aboveARoot_popsTheSelectedStack() {
        // Given
        val navigator = navigator(start = listOf(Start, Screen), other = listOf(Other, Screen))

        // When
        navigator.back()

        // Then
        assertEquals(listOf(listOf(Start), listOf(Other, Screen)), navigator.stacks)
    }

    @Test
    fun back_atAnotherDestinationsRoot_returnsToTheStartOne() {
        // Given
        val navigator = navigator(start = listOf(Start, Screen), selected = Other)

        // When
        navigator.back()

        // Then
        assertEquals(Start, navigator.state.selected)
        assertEquals(listOf(listOf(Start, Screen), listOf(Other)), navigator.stacks)
        assertEquals(1, switches)
    }
}
