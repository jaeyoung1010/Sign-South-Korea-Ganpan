package com.signkorean.domain.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DomainArchitectureTest {
    @Test
    fun `domain source tree has no framework imports`() {
        val sourceRoot =
            sequenceOf(File("src/main/kotlin"), File("domain/src/main/kotlin"))
                .first { it.exists() }

        val violations = findForbiddenReferences(sourceRoot)

        assertTrue("Domain framework references: $violations", violations.isEmpty())
    }

    @Test
    fun `forbidden Android Compose Room and Hilt imports are reported`() {
        val forbiddenSource = "import android.content.Context\nimport dagger.hilt.InstallIn"

        assertEquals(
            listOf("android", "hilt"),
            findForbiddenReferences(forbiddenSource),
        )
    }

    private fun findForbiddenReferences(sourceRoot: File): List<String> =
        sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> findForbiddenReferences(file.readText()).map { "$file:$it" }.asSequence() }
            .toList()

    private fun findForbiddenReferences(source: String): List<String> =
        forbiddenReferences
            .filter { (_, forbiddenReference) ->
                source.lineSequence().any { line ->
                    val code = line.trimStart()
                    !code.startsWith("//") && forbiddenReference in code
                }
            }.map { (name, _) -> name }

    private companion object {
        val forbiddenReferences =
            listOf(
                "android" to "android.",
                "compose" to "androidx.compose.",
                "room" to "androidx.room.",
                "workmanager" to "androidx.work.",
                "media3" to "androidx.media3.",
                "hilt" to "dagger.hilt.",
            )
    }
}
