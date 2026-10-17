package com.almeidajr.molda.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class DomainIsolationTest {
    @Test
    fun domainFiles_importNothingFromAndroidUiOrData() {
        Konsist
            .scopeFromProduction()
            .files
            .withPackage("..domain..")
            .assertFalse(
                additionalMessage = "Domain code must not depend on Android, UI or data code.",
            ) { file ->
                file.hasImport { import -> FORBIDDEN_IMPORT.matches(import.name) }
            }
    }

    private companion object {
        // Any Android or AndroidX API, the shared design layer, and every feature's ui or data package.
        val FORBIDDEN_IMPORT =
            Regex("""^(androidx?|com\.almeidajr\.molda(\.\w+)*\.(ui|data))\..+""")
    }
}
