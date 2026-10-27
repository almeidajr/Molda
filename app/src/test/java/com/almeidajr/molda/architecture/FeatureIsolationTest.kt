package com.almeidajr.molda.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class FeatureIsolationTest {
    @Test
    fun featureUiAndNavigationFiles_inAnyFeature_importNoOtherFeaturesLayer() {
        // Given
        val files = Konsist.scopeFromProduction().files

        LAYERS.forEach { (layer, message) ->
            // When/Then
            files.assertFalse(additionalMessage = message) { file ->
                val feature = layer.featureOf("${file.packagee?.name}.")
                feature != null &&
                    file.imports.any { import ->
                        layer.featureOf(import.name)?.let { it != feature } == true
                    }
            }
        }
    }

    private fun Regex.featureOf(name: String): String? = find(name)?.groupValues?.get(1)

    private companion object {
        val LAYERS = listOf(
            // A feature's ui package; never the shared design layer (molda.ui) or measurement.
            Regex("""^com\.almeidajr\.molda\.(\w+)\.ui\.""") to
                "One feature's UI must not import another feature's UI.",
            // A feature's navigation package; the shared molda.ui.navigation is excluded.
            Regex("""^com\.almeidajr\.molda\.(?!ui\.)(\w+)\.navigation\.""") to
                "A feature's navigation rules know only its own keys; " +
                "rules across features belong in the root executor.",
        )
    }
}
