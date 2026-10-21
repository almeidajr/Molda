package com.almeidajr.molda.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class FeatureUiIsolationTest {
    @Test
    fun featureUiFiles_inAnyFeature_importNoOtherFeatureUi() {
        // Given
        val files = Konsist.scopeFromProduction().files

        // When/Then
        files.assertFalse(
            additionalMessage = "One feature's UI must not import another feature's UI.",
        ) { file ->
            val feature = featureOf("${file.packagee?.name}.")
            feature != null &&
                file.imports.any { import ->
                    featureOf(import.name)?.let { it != feature } == true
                }
        }
    }

    private fun featureOf(name: String): String? = FEATURE_UI.find(name)?.groupValues?.get(1)

    private companion object {
        // A feature's ui package; the shared design layer (molda.ui) and measurement never match.
        val FEATURE_UI = Regex("""^com\.almeidajr\.molda\.(\w+)\.ui\.""")
    }
}
