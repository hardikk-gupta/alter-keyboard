package com.aistudio.altermind.voice

import com.ibm.icu.text.Transliterator

// Transliterator instances are mutable: keep one per inference thread.
private val hindiLatin = ThreadLocal.withInitial {
    Transliterator.getInstance("Devanagari-Latin; Latin-ASCII")
}
private val devanagariRuns = Regex("[\\u0900-\\u097F\\uA8E0-\\uA8FF]+")

/** Script conversion only. Never send existing English, identifiers or punctuation through ICU. */
internal fun romanizeHindiText(text: String): String = devanagariRuns.replace(text) {
    hindiLatin.get()!!.transliterate(it.value)
}
