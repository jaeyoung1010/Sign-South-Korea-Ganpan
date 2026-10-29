package com.signkorean.app.localization

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizedResourcesCompletenessTest {
    @Test
    fun `every user-facing English resource has a translation for each supported language`() {
        resourceDirectories().forEach { directory ->
            val english = parseResources(directory.resolve("values/strings.xml"))
            assertFalse("No English resources found in $directory", english.isEmpty())
            listOf("ja", "zh").forEach { languageTag ->
                val localized = parseResources(directory.resolve("values-$languageTag/strings.xml"))
                assertEquals("Missing or extra $languageTag keys in $directory", english.keys, localized.keys)
                english.forEach { (key, value) ->
                    assertTrue("Blank English value for $key in $directory", value.isNotBlank())
                    assertTrue("Blank $languageTag value for $key in $directory", localized.getValue(key).isNotBlank())
                    assertEquals(
                        "Placeholder mismatch for $key in $directory",
                        placeholders(value),
                        placeholders(localized.getValue(key)),
                    )
                }
            }
        }
    }

    private fun resourceDirectories(): List<File> {
        val start = File(requireNotNull(System.getProperty("user.dir")))
        val root = generateSequence(start) { it.parentFile }
            .first { it.resolve("settings.gradle.kts").isFile }
        return listOf("app", "feature-learn", "feature-onboarding", "feature-progress", "feature-settings")
            .map { root.resolve("$it/src/main/res") }
            .filter { it.resolve("values/strings.xml").isFile }
    }

    private fun parseResources(file: File): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val result = linkedMapOf<String, String>()
        val resources = document.documentElement.childNodes
        for (index in 0 until resources.length) {
            val node = resources.item(index)
            if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
            val name = node.attributes.getNamedItem("name")?.nodeValue ?: continue
            val value = when (node.nodeName) {
                "string" -> node.textContent
                "plurals" -> (0 until node.childNodes.length)
                    .mapNotNull { childIndex ->
                        val child = node.childNodes.item(childIndex)
                        if (child.nodeName == "item") child.textContent else null
                    }
                    .joinToString(" ")
                else -> continue
            }
            result[name] = value
        }
        return result
    }

    private fun placeholders(value: String): Set<String> =
        Regex("%\\d+\\$[a-zA-Z]").findAll(value).map { it.value }.toSet()
}
