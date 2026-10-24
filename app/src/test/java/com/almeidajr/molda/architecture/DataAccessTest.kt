package com.almeidajr.molda.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withoutPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class DataAccessTest {
    @Test
    fun productionFiles_outsideDependencyInjectionAndData_importNoDataLayer() {
        // Given
        val files = Konsist
            .scopeFromProduction()
            .files
            .withoutPackage("com.almeidajr.molda.di..", "..data..")

        // When/Then
        files.assertFalse(
            additionalMessage = "Only dependency injection may import data-layer code, " +
                "except the load-result convention and the no-longer-exists failure.",
        ) { file ->
            file.hasImport { import ->
                DATA_IMPORT.matches(import.name) && import.name !in LOAD_RESULT_IMPORTS
            }
        }
    }

    private companion object {
        val DATA_IMPORT = Regex("""^com\.almeidajr\.molda(\.\w+)*\.data\..+""")

        // Every list ViewModel follows its stored list through the shared load-result convention,
        // and editors and dialogs close when their row turns out to be deleted.
        val LOAD_RESULT_IMPORTS = setOf(
            "com.almeidajr.molda.data.LoadResult",
            "com.almeidajr.molda.data.asLoadResult",
            "com.almeidajr.molda.data.NoLongerExistsException",
        )
    }
}
