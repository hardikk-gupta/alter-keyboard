package com.aistudio.altermind.voice

import org.junit.Assert.*
import org.junit.Test

class HindiRomanizationTest {
    @Test fun convertsHindiRunsWithoutChangingEnglishOrIdentifiers() {
        assertEquals("namaste notebook mujhe API_v2 https://example.com?x=12",
            romanizeHindiText("नमस्ते notebook मुझे API_v2 https://example.com?x=12"))
        val english = "Résumé: naïve café, Notebook_ID = 123; don't change this!"
        assertEquals(english, romanizeHindiText(english))
    }

    @Test fun convertsNativeDigitsAndDandaAndHandlesAnEmptyTranscript() {
        assertEquals("hindi 123.", romanizeHindiText("हिंदी १२३।"))
        assertEquals("", romanizeHindiText(""))
    }

    @Test fun revisionsAreConvertedIndependentlyAndConversionIsIdempotent() {
        assertEquals("mujhe", romanizeHindiText("मुझे"))
        val corrected = romanizeHindiText("मुझे notebook चाहिए")
        assertFalse(corrected.any { it in '\u0900'..'\u097f' })
        assertEquals(corrected, romanizeHindiText(corrected))
        assertTrue(corrected.contains("notebook"))
    }
}
