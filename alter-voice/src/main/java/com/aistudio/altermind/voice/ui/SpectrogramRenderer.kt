package com.aistudio.altermind.voice.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.toArgb
import com.aistudio.altermind.voice.SpeechSpectrum

internal class SpectrogramRenderer {
    private val width = SpeechSpectrum.COLUMNS
    private val height = SpeechSpectrum.BANDS
    private val pixels = IntArray(width * height)
    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val bounds = RectF()
    private val colors = IntArray(256) {
        val energy = it / 255f
        val grey = AlterColors.DisabledText.toArgb()
        if (energy < .3f) ColorUtils.blendARGB(AlterColors.SlateGrayCard.toArgb(), grey, energy / .3f)
        else ColorUtils.blendARGB(grey, AlterColors.AlterYellow.toArgb(), (energy - .3f) / .7f)
    }

    fun draw(canvas: Canvas, w: Float, h: Float, frame: SpeechSpectrum, scroll: Boolean) {
        for (band in 0 until height) for (column in 0 until width) {
            val energy = frame.energy(if (scroll) column else width - 1, band)
            pixels[(height - 1 - band) * width + column] = colors[energy]
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bounds.set(0f, 0f, w, h)
        canvas.drawBitmap(bitmap, null, bounds, paint)
    }
}
