package com.hank.kustom.domain

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import com.hank.kustom.data.model.KustomFont

/**
 * Typeface loader supporting distinct font weight rendering (300, 400, 500, 700, 900)
 * across all cycling computer fonts with 1:1 true aspect ratio.
 */
object KustomTypefaceResolver {

    fun applyTypography(
        paint: TextPaint,
        font: KustomFont,
        weight: Int,
        textSizePx: Float,
    ) {
        paint.isAntiAlias = true
        paint.textSize = textSizePx
        paint.textScaleX = 1.0f // True 1:1 typographic aspect ratio
        paint.letterSpacing = 0.0f

        val familyName = when (font) {
            KustomFont.KAROO_RBNO2, KustomFont.WAHOO_BARLOW, KustomFont.GARMIN_DIN, KustomFont.ATHLETIC_DISPLAY -> "sans-serif-condensed"
            KustomFont.ROBOTO_CLEAN -> "sans-serif"
        }

        val baseTypeface = Typeface.create(familyName, Typeface.NORMAL)

        // API 28+ Typeface.create with exact weight (100 to 900)
        val typefaceWithWeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                Typeface.create(baseTypeface, weight.coerceIn(100, 900), false)
            }.getOrNull()
        } else null

        if (typefaceWithWeight != null) {
            paint.typeface = typefaceWithWeight
            paint.isFakeBoldText = false
        } else {
            val style = if (weight >= 700) Typeface.BOLD else Typeface.NORMAL
            paint.typeface = Typeface.create(baseTypeface, style)
            paint.isFakeBoldText = (weight >= 700)
        }

        // Progressive stroke for clear, distinct weight differentiation across ALL steps (300, 400, 500, 700, 900)
        val strokeFactor = when (weight) {
            300 -> 0.0f          // Light: crisp thin
            400 -> 0.008f        // Regular
            500 -> 0.018f        // Medium
            700 -> 0.032f        // Bold
            900 -> 0.050f        // Ultra Bold / Heavy
            else -> ((weight - 300) / 12000f).coerceAtLeast(0f)
        }

        if (strokeFactor > 0f) {
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeWidth = textSizePx * strokeFactor
        } else {
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
        }
    }
}
