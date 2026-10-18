package com.almeidajr.molda.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class RootPackageImportTest {
    @Test
    fun onlyMainActivityImportsTheRootPackage() {
        Konsist
            .scopeFromProduction()
            .files
            .filterNot { it.name == "MainActivity" }
            .assertFalse(
                additionalMessage = "Only MainActivity may import root package code; R is exempt.",
            ) { file ->
                file.hasImport { import -> ROOT_IMPORT.matches(import.name) }
            }
    }

    private companion object {
        // A declaration directly in com.almeidajr.molda, except the generated R class.
        val ROOT_IMPORT = Regex("""^com\.almeidajr\.molda\.(?!R$)[^.]+$""")
    }
}
