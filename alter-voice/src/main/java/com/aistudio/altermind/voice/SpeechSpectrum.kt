package com.aistudio.altermind.voice

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

/** Immutable display snapshot. Low frequencies occupy band zero, newest time is at the right. */
class SpeechSpectrum internal constructor(private val cells: ByteArray, private val oldest: Int) {
    fun energy(column: Int, band: Int): Int =
        cells[((oldest + column) % COLUMNS) * BANDS + band].toInt() and 255

    companion object {
        const val COLUMNS = 64
        const val BANDS = 16
    }
}

/** Hann-windowed 256-point FFT on 16 kHz mono PCM. Each column represents 16 ms. */
internal class SpectrumAnalyzer {
    private val length = 256
    private val window = FloatArray(length) { (.5 - .5 * cos(2 * PI * it / length)).toFloat() }
    private val cosine = FloatArray(length / 2) { cos(-2 * PI * it / length).toFloat() }
    private val sine = FloatArray(length / 2) { sin(-2 * PI * it / length).toFloat() }
    private val reversed = IntArray(length) { Integer.reverse(it) ushr 24 }
    private val real = FloatArray(length)
    private val imaginary = FloatArray(length)
    // Exclusive upper FFT-bin limits, with more resolution in the lower speech frequencies.
    private val limits = intArrayOf(2, 3, 4, 5, 7, 9, 12, 16, 21, 27, 35, 45, 59, 76, 99, 129)
    private val history = ByteArray(SpeechSpectrum.COLUMNS * SpeechSpectrum.BANDS)
    private var used = 0
    private var nextColumn = 0

    fun accept(samples: FloatArray): SpeechSpectrum? {
        var updated = false
        for (sample in samples) {
            real[reversed[used]] = sample * window[used]
            if (++used == length) {
                analyzeColumn()
                used = 0
                updated = true
            }
        }
        return if (updated) SpeechSpectrum(history.copyOf(), nextColumn) else null
    }

    private fun analyzeColumn() {
        imaginary.fill(0f)
        var width = 2
        while (width <= length) {
            val half = width / 2
            for (start in 0 until length step width) for (offset in 0 until half) {
                val a = start + offset
                val b = a + half
                val phase = offset * (length / width)
                val re = cosine[phase] * real[b] - sine[phase] * imaginary[b]
                val im = sine[phase] * real[b] + cosine[phase] * imaginary[b]
                real[b] = real[a] - re
                imaginary[b] = imaginary[a] - im
                real[a] += re
                imaginary[a] += im
            }
            width *= 2
        }
        var bin = 1 // Exclude microphone DC offset.
        for (band in limits.indices) {
            var peakPower = 0f
            while (bin < limits[band]) {
                peakPower = maxOf(peakPower, real[bin] * real[bin] + imaginary[bin] * imaginary[bin])
                bin++
            }
            val amplitude = sqrt(peakPower) * 4f / length // Hann coherent gain and one-sided spectrum.
            history[nextColumn * limits.size + band] = (255 * microphoneLevel(amplitude)).roundToInt().toByte()
        }
        nextColumn = (nextColumn + 1) % SpeechSpectrum.COLUMNS
    }
}
