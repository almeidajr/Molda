package com.almeidajr.molda.update.data

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A downloaded APK checked against the SHA-256 digest GitHub publishes for it. */
class VerifyApkTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun verifyApk_withTheDigestOfItsContents_passesAndKeepsTheFile() {
        // Given
        val apk = apk("abc")

        // When
        val isVerified = verifyApk(apk, ABC_DIGEST)

        // Then
        assertTrue(isVerified)
        assertTrue(apk.exists())
    }

    @Test
    fun verifyApk_withAnotherDigest_isRejectedAndDeletesTheFile() {
        // Given
        val apk = apk("abd")

        // When
        val isVerified = verifyApk(apk, ABC_DIGEST)

        // Then
        assertFalse(isVerified)
        assertFalse(apk.exists())
    }

    @Test
    fun verifyApk_withoutADigest_isRejectedAndDeletesTheFile() {
        // Given
        val apk = apk("abc")

        // When
        val isVerified = verifyApk(apk, digest = null)

        // Then
        assertFalse(isVerified)
        assertFalse(apk.exists())
    }

    @Test
    fun verifyApk_withAMissingFile_isRejected() {
        // When/Then
        assertFalse(verifyApk(File(folder.root, "molda-v1.0.0.apk"), ABC_DIGEST))
    }

    private fun apk(contents: String) =
        folder.newFile("molda-v1.0.0.apk").apply { writeText(contents) }

    private companion object {
        // The SHA-256 of "abc", from FIPS 180-2's worked example.
        const val ABC_DIGEST =
            "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }
}
