package com.almeidajr.molda.architecture

import com.lemonappdev.konsist.api.Konsist
import kotlin.test.assertEquals
import org.junit.Test

class PackageCycleTest {
    @Test
    fun productionPackages_throughTheirImports_formNoCycles() {
        // Given
        val files = Konsist.scopeFromProduction().files
        val packages = files.mapNotNull { it.packagee?.name }.toSet()

        // When
        val dependencies = files
            .mapNotNull { file ->
                file.packagee?.name?.let { source ->
                    source to file.imports.mapNotNull { packageOf(it.name, packages) } - source
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, targets) -> targets.flatten().toSet() }

        // Then
        assertEquals(emptyList<List<String>>(), cycles(dependencies), "Package cycles found.")
    }

    private companion object {
        // R is generated into the root package; resources are not a dependency on root code.
        const val RESOURCES = "com.almeidajr.molda.R"

        /** The longest production package holding [import]; null for external or resource imports. */
        fun packageOf(import: String, packages: Set<String>): String? =
            if (import == RESOURCES || import.startsWith("$RESOURCES.")) {
                null
            } else {
                packages.filter { import.startsWith("$it.") }.maxByOrNull { it.length }
            }

        /** For each package on a cycle, one dependency path from it back to itself. */
        fun cycles(dependencies: Map<String, Set<String>>): List<List<String>> =
            dependencies.keys.sorted().mapNotNull { start -> pathBack(start, dependencies) }

        fun pathBack(start: String, dependencies: Map<String, Set<String>>): List<String>? {
            val visited = mutableSetOf<String>()

            fun search(path: List<String>): List<String>? =
                dependencies[path.last()].orEmpty().firstNotNullOfOrNull { next ->
                    when {
                        next == start -> path + next
                        visited.add(next) -> search(path + next)
                        else -> null
                    }
                }
            return search(listOf(start))
        }
    }
}
