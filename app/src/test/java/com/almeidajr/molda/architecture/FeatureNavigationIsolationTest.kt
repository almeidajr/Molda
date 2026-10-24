package com.almeidajr.molda.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class FeatureNavigationIsolationTest {
    @Test
    fun featureNavigationFiles_inAnyFeature_importNoOtherFeatureNavigation() {
        // Given
        val files = Konsist.scopeFromProduction().files

        // When/Then
        files.assertFalse(
            additionalMessage = "A feature's navigation rules know only its own keys; " +
                "rules across features belong in the root executor.",
        ) { file ->
            val feature = featureOf("${file.packagee?.name}.")
            feature != null &&
                file.imports.any { import ->
                    featureOf(import.name)?.let { it != feature } == true
                }
        }
    }

    private fun featureOf(name: String): String? =
        FEATURE_NAVIGATION.find(name)?.groupValues?.get(1)

    private companion object {
        // A feature's navigation package; the shared molda.ui.navigation is excluded.
        val FEATURE_NAVIGATION = Regex("""^com\.almeidajr\.molda\.(?!ui\.)(\w+)\.navigation\.""")
    }
}
