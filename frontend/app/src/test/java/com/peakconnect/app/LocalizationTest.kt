package com.peakconnect.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationTest {

    private fun findResFile(relativePath: String): File {
        val candidates = listOf(
            File("src/main/res", relativePath),
            File("app/src/main/res", relativePath),
            File("../app/src/main/res", relativePath)
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find res file: $relativePath in $candidates")
    }

    private fun parseStringResources(file: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val nodeList = doc.getElementsByTagName("string")

        val result = mutableMapOf<String, String>()
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            if (name.isNotBlank()) {
                result[name] = node.textContent.trim()
            }
        }
        return result
    }

    @Test
    fun everyEnglishStringKeyExistsInHindi() {
        val enFile = findResFile("values/strings.xml")
        val hiFile = findResFile("values-hi/strings.xml")

        assertTrue("English strings.xml file must exist", enFile.exists())
        assertTrue("Hindi strings.xml file must exist", hiFile.exists())

        val enStrings = parseStringResources(enFile)
        val hiStrings = parseStringResources(hiFile)

        assertTrue("English strings map must not be empty", enStrings.isNotEmpty())
        assertTrue("Hindi strings map must not be empty", hiStrings.isNotEmpty())

        val missingInHindi = enStrings.keys - hiStrings.keys

        assertEquals(
            "Every string key in values/strings.xml must exist in values-hi/strings.xml. Missing keys: $missingInHindi",
            emptySet<String>(),
            missingInHindi
        )

        // Ensure none of the Hindi strings are completely blank
        val blankHindiKeys = hiStrings.filter { it.value.isEmpty() }.keys
        assertEquals(
            "Hindi string values must not be blank: $blankHindiKeys",
            emptySet<String>(),
            blankHindiKeys
        )
    }

    @Test
    fun placeholderParityBetweenEnglishAndHindi() {
        val enFile = findResFile("values/strings.xml")
        val hiFile = findResFile("values-hi/strings.xml")

        val enStrings = parseStringResources(enFile)
        val hiStrings = parseStringResources(hiFile)

        // Pattern for format placeholders like %1$s, %2$d, %1$.1f, %s, %d (excluding escaped %%)
        val placeholderPattern = Pattern.compile("%(?:\\d+\\$)?[\\-+#0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]")

        val mismatches = mutableListOf<String>()

        for ((key, enVal) in enStrings) {
            val hiVal = hiStrings[key] ?: continue

            val cleanedEn = enVal.replace("%%", "")
            val cleanedHi = hiVal.replace("%%", "")

            val enMatches = mutableListOf<String>()
            val enMatcher = placeholderPattern.matcher(cleanedEn)
            while (enMatcher.find()) {
                enMatches.add(enMatcher.group())
            }

            val hiMatches = mutableListOf<String>()
            val hiMatcher = placeholderPattern.matcher(cleanedHi)
            while (hiMatcher.find()) {
                hiMatches.add(hiMatcher.group())
            }

            enMatches.sort()
            hiMatches.sort()

            if (enMatches != hiMatches) {
                mismatches.add("Key '$key': EN $enMatches vs HI $hiMatches (EN: \"$enVal\" | HI: \"$hiVal\")")
            }
        }

        assertTrue(
            "Format specifiers in values-hi must match values/. Mismatches:\n${mismatches.joinToString("\n")}",
            mismatches.isEmpty()
        )
    }
}
