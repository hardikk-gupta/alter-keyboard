package com.aistudio.altermind.voice

import org.junit.Assert.*
import org.junit.Test

class HindiRomanizationTest {
    @Test fun convertsHindiRunsWithoutChangingEnglishOrIdentifiers() {
        assertEquals("namaste notebook mujhe API_v2 https://example.com?x=12",
            romanizeNemotronHindi("नमस्ते notebook मुझे API_v2 https://example.com?x=12"))
        val english = "Résumé: naïve café, Notebook_ID = 123; don't change this!"
        assertEquals(english, romanizeNemotronHindi(english))
    }

    @Test fun convertsNativeDigitsAndDandaAndHandlesAnEmptyTranscript() {
        assertEquals("hindi 123.", romanizeNemotronHindi("हिंदी १२३।"))
        assertEquals("", romanizeNemotronHindi(""))
    }

    @Test fun revisionsAreConvertedIndependentlyAndConversionIsIdempotent() {
        assertEquals("mujhe", romanizeNemotronHindi("मुझे"))
        val corrected = romanizeNemotronHindi("मुझे notebook चाहिए")
        assertFalse(corrected.any { it in '\u0900'..'\u097f' })
        assertEquals(corrected, romanizeNemotronHindi(corrected))
        assertTrue(corrected.contains("notebook"))
    }
}
