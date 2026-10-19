package com.aistudio.altermind.voice

import kotlinx.coroutines.channels.ReceiveChannel

/** Run long recordings as overlapping model-sized windows, preserving the full transcript. */
internal suspend fun transcribeSpeechWindows(
    audio: FloatArray,
    decode: suspend (FloatArray) -> String,
    sampleRate: Int = 16000,
    windowSeconds: Int = 20,
    overlapSeconds: Int = 1
): String {
    require(audio.isNotEmpty() && sampleRate > 0 && windowSeconds > overlapSeconds && overlapSeconds >= 0)
    val window = sampleRate * windowSeconds
    val overlap = sampleRate * overlapSeconds
    var offset = 0
    var transcript = ""
    while (offset < audio.size) {
        val end = minOf(offset + window, audio.size)
        val text = decode(audio.copyOfRange(offset, end)).takeUnless { it.contains("[NO_SPEECH_DETECTED]", true) }.orEmpty()
        transcript = joinWhisperOverlap(transcript, text)
        if (end == audio.size) break
        offset = end - overlap
    }
    return transcript.trim()
}

/** Decode short previews while capture continues; commit bounded segments and finish only the tail. */
internal suspend fun transcribeWhisperLive(
    chunks: ReceiveChannel<FloatArray>, decode: suspend (FloatArray) -> String, update: (String) -> Unit,
    sampleRate: Int = 16000
) {
    val window = FloatArray(20 * sampleRate)
    val overlap = sampleRate
    var used = 0
    var carried = 0
    var nextPreview = 4 * sampleRate
    var committed = ""
    var cachedSamples = -1
    var cachedText = ""
    suspend fun recognize(): String {
        if (used != cachedSamples) {
            cachedText = decode(window.copyOf(used)).takeUnless { it.contains("[NO_SPEECH_DETECTED]", true) }.orEmpty().trim()
            cachedSamples = used
        }
        return cachedText
    }
    suspend fun append(chunk: FloatArray) {
        var offset = 0
        while (offset < chunk.size) {
            val count = minOf(chunk.size - offset, window.size - used)
            chunk.copyInto(window, used, offset, offset + count)
            used += count; offset += count
            if (used == window.size) {
                committed = joinWhisperOverlap(committed, recognize())
                update(committed)
                window.copyInto(window, 0, used - overlap, used)
                used = overlap; carried = overlap; cachedSamples = -1; nextPreview = used + 4 * sampleRate
            }
        }
    }
    while (true) {
        val first = chunks.receiveCatching().getOrNull() ?: break
        append(first)
        // A busy decoder skips obsolete previews, never audio. Capture keeps running independently.
        while (true) append(chunks.tryReceive().getOrNull() ?: break)
        if (used >= nextPreview) {
            update(joinWhisperOverlap(committed, recognize()))
            nextPreview = used + 4 * sampleRate
        }
    }
    if (used > carried) update(joinWhisperOverlap(committed, recognize())) else update(committed)
}

/** Only used where consecutive audio segments share their last/first second. */
// ponytail: token agreement handles the one-second overlap; use word timestamps if boundary accuracy needs refinement.
internal fun joinWhisperOverlap(previous: String, next: String): String {
    if (next.isBlank()) return previous
    if (previous.isBlank()) return next
    val before = previous.trim().split(Regex("\\s+"))
    val after = next.trim().split(Regex("\\s+"))
    fun word(text: String) = text.lowercase().trim {
        when (Character.getType(it)) {
            Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt() -> false
            else -> !it.isLetterOrDigit()
        }
    }
    val repeated = (minOf(12, before.size, after.size) downTo 1).firstOrNull { count ->
        before.takeLast(count).map(::word) == after.take(count).map(::word)
    } ?: 0
    return (before + after.drop(repeated)).joinToString(" ")
}
