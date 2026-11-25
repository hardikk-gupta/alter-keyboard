package com.aistudio.altermind.voice.ui

/** One second of display levels, sampled against the UI frame clock. Each pill owns its history. */
internal class VoiceTape {
    private val levels = FloatArray(50)
    private var latest = -1L

    private var lastSound = Long.MIN_VALUE

    fun record(frameNanos: Long, level: Float): Boolean {
        val tick = frameNanos / 20_000_000L
        val value = if (level.isFinite()) level.coerceIn(0f, 1f) else 0f
        val slot = (tick % levels.size).toInt()
        val wasVisible = lastSound >= 0 && latest - lastSound < levels.size
        if (tick == latest) {
            if (value <= levels[slot]) return false
        } else {
            // A delayed frame leaves a gap in the trace.
            val elapsed = (tick - latest).coerceAtMost(levels.size.toLong()).toInt()
            for (age in 0 until elapsed) levels[index(tick - age)] = 0f
            latest = tick
        }
        levels[slot] = value
        if (value > 0f) lastSound = tick
        return wasVisible || value > 0f
    }

    private fun index(tick: Long) = Math.floorMod(tick, levels.size.toLong()).toInt()

    fun tickAt(column: Int, columns: Int): Long =
        latest - (columns - 1 - column) * (levels.size - 1) / (columns - 1).coerceAtLeast(1)

    fun levelAt(tick: Long) = if (tick < 0 || latest - tick >= levels.size) 0f else levels[index(tick)]
}
