package com.aistudio.altermind.voice

private val correctionWord = Regex("[\\p{L}\\p{M}]{1,60}")
private val protectedCorrections = setOf("no", "not", "never", "without", "cannot", "nahi", "nahin", "mat")
fun validVoiceCorrection(from: String, to: String): Boolean =
    correctionWord.matches(from) && correctionWord.matches(to) && !from.equals(to, true) &&
        from.lowercase(java.util.Locale.ROOT) !in protectedCorrections && to.lowercase(java.util.Locale.ROOT) !in protectedCorrections

/** One pass over whole words: never cascade replacements or modify words inside URLs/identifiers. */
fun applyVoiceCorrections(text: String, entries: Map<String, String>): String =
    Regex("[\\p{L}\\p{M}\\p{N}_@./:'’-]+").replace(text) { match ->
        val word = match.value.trimEnd('.', ':')
        val replacement = entries[word.lowercase(java.util.Locale.ROOT)]
        if (replacement != null && validVoiceCorrection(word, replacement)) replacement + match.value.drop(word.length) else match.value
    }
