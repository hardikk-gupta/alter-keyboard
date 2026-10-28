package com.aistudio.altermind.voice

import org.junit.Assert.*
import org.junit.Test

class IndicXlitTest {
    @Test fun replacesOnlyHindiWordsAndPreservesEnglishIdentifiersAndPunctuation() {
        val outputs = mapOf("मैं" to "main", "घर" to "ghar", "जा" to "jaa", "रहा" to "rahaa", "हूँ" to "huun")
        val seen = mutableListOf<String>()
        val result = romanizeWithIndicXlit("Baby मैं घर जा रहा हूँ, then I'll call you. API_v2 Résumé https://example.com") {
            seen += it
            conversationalXlit(it, outputs.getValue(it))
        }
        assertEquals("Baby main ghar ja raha hun, then I'll call you. API_v2 Résumé https://example.com", result)
        assertEquals(outputs.keys.toList(), seen)
    }
    @Test fun preservesRecognizedExtraWordsAndConvertsNumbersWithoutCallingTheModel() {
        assertEquals("main buss ghar 123.", romanizeWithIndicXlit("मैं बस घर १२३।") {
            mapOf("मैं" to "main", "बस" to "buss", "घर" to "ghar").getValue(it)
        })
        val latin = "main ghar ja raha hun, Notebook_ID = 123 👩‍💻"
        assertEquals(latin, romanizeWithIndicXlit(latin) { error("English must bypass the model") })
        assertEquals("", romanizeWithIndicXlit("") { error("Empty text must bypass the model") })
    }
    @Test fun spellingPreferencesCannotRewriteOtherWordsOrNames() {
        for ((source, pair) in mapOf("बेबी" to ("bebi" to "baby"), "चाहिए" to ("chaahiye" to "chahiye"),
            "देन" to ("daine" to "dena"), "कॉल" to ("coll" to "call"), "यू" to ("yuu" to "you"), "आइल" to ("aail" to "ail"))) {
            assertEquals(pair.second, conversationalXlit(source, pair.first))
        }
        assertEquals("main", conversationalXlit("मैं", "main"))
        assertEquals("rahaa", conversationalXlit("नाम", "rahaa"))
        assertEquals("unexpected", conversationalXlit("रहा", "unexpected"))
    }
}
