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
            additionalMessage = "Only dependency injection may import data-layer code.",
        ) { file ->
            file.hasImport { import -> DATA_IMPORT.matches(import.name) }
        }
    }

    private companion object {
        val DATA_IMPORT = Regex("""^com\.almeidajr\.molda(\.\w+)*\.data\..+""")
    }
}
