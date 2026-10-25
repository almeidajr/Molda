package com.almeidajr.molda.update.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class ChooseUpdateTest {
    @Test
    fun chooseUpdate_onABetaBuild_offersTheNewestBetaBuild() {
        // When
        val update = chooseUpdate("0.3.0-beta.1", listOf(beta("v0.3.0-beta.2"), beta("v0.2.0")))

        // Then
        assertEquals("0.3.0-beta.2", update?.version)
    }

    @Test
    fun chooseUpdate_onABetaBuild_offersTheReleaseAfterItsBetaBuilds() {
        // When
        val update = chooseUpdate(
            "0.3.0-beta.9",
            listOf(beta("v0.3.0-beta.10"), release("v0.3.0"), beta("v0.3.0-beta.11")),
        )

        // Then
        assertEquals("0.3.0", update?.version)
    }

    @Test
    fun chooseUpdate_byPrecedence_putsBeta10AfterBeta9() {
        // When
        val update = chooseUpdate(
            "0.3.0-beta.8",
            listOf(beta("v0.3.0-beta.10"), beta("v0.3.0-beta.9")),
        )

        // Then
        assertEquals("0.3.0-beta.10", update?.version)
    }

    @Test
    fun chooseUpdate_onARelease_offersOnlyNewerReleases() {
        // When
        val update = chooseUpdate(
            "0.3.0",
            listOf(beta("v0.5.0-beta.1"), release("v0.4.0"), release("v0.2.0")),
        )

        // Then
        assertEquals("0.4.0", update?.version)
    }

    @Test
    fun chooseUpdate_onARelease_withOnlyNewerBetaBuilds_offersNothing() {
        // When/Then
        assertNull(chooseUpdate("0.3.0", listOf(beta("v0.4.0-beta.1"))))
    }

    @Test
    fun chooseUpdate_whenInstalledIsNewest_offersNothing() {
        // When/Then
        assertNull(chooseUpdate("0.3.0", listOf(release("v0.3.0"), release("v0.2.0"))))
    }

    @Test
    fun chooseUpdate_ignoresDraftsAndReleasesWithoutAnApk() {
        // When
        val update = chooseUpdate(
            "0.1.0",
            listOf(
                release("v0.4.0").copy(isDraft = true),
                release("v0.3.0").copy(assets = listOf(asset("molda-v0.3.0.aab"))),
                release("v0.2.0").copy(assets = emptyList()),
                release("v0.1.1"),
            ),
        )

        // Then
        assertEquals("0.1.1", update?.version)
    }

    @Test
    fun chooseUpdate_ignoresTagsThatAreNotSemVer() {
        // When/Then
        assertNull(chooseUpdate("0.1.0", listOf(release("nightly"))))
    }

    @Test
    fun chooseUpdate_offersTheReleasePageAndApk() {
        // When
        val update = chooseUpdate("0.1.0", listOf(release("v0.2.0")))

        // Then
        assertEquals(
            Update(
                version = "0.2.0",
                page = "https://github.com/almeidajr/Molda/releases/tag/v0.2.0",
                apk = asset("molda-v0.2.0.apk"),
            ),
            update,
        )
    }

    @Test
    fun checksForUpdates_onLocalAndNonSemVerBuilds_isFalse() {
        // When/Then
        assertEquals(
            listOf(false, false, false, true, true),
            listOf("0.0.0-dev", "", "1.0", "0.3.0-beta.1", "1.2.3").map(::checksForUpdates),
        )
    }

    private fun release(tag: String) = Release(
        tag = tag,
        isDraft = false,
        isPrerelease = false,
        page = "https://github.com/almeidajr/Molda/releases/tag/$tag",
        assets = listOf(asset("molda-$tag.apk")),
    )

    private fun beta(tag: String) = release(tag).copy(isPrerelease = true)

    private fun asset(name: String) = ReleaseAsset(
        name = name,
        downloadUrl = "https://github.com/almeidajr/Molda/releases/download/$name",
        digest = "sha256:0123",
    )
}
