package com.almeidajr.molda.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class SkirtIndependenceTest {
    @Test
    fun skirtFiles_inAnyLayer_importNoProjectCode() {
        // Given
        val files = Konsist.scopeFromProduction().files.withPackage("com.almeidajr.molda.skirt..")

        // When/Then
        files.assertFalse(
            additionalMessage = "The skirt calculator must not know about projects; " +
                "the root package joins them.",
        ) { file ->
            file.hasImport { import -> import.name.startsWith("com.almeidajr.molda.project.") }
        }
    }
}
