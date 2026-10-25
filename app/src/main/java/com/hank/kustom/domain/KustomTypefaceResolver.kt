package com.hank.kustom.domain

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import com.hank.kustom.data.model.KustomFont

/**
 * Typeface loader for top 5 cycling computer fonts (Karoo, Wahoo, Garmin, Athletic Display, Clean).
 * Guarantees 1:1 true aspect ratio with zero textScaleX stretching.
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
        paint.textScaleX = 1.0f // True 1:1 typographic aspect ratio (never stretch)
        paint.letterSpacing = 0.0f

        val effectiveWeight = if (font == KustomFont.KAROO_RBNO2 || font == KustomFont.GARMIN_DIN || font == KustomFont.ATHLETIC_DISPLAY) {
            weight.coerceAtLeast(700)
        } else {
            weight
        }

        val baseTypeface = resolveFamilyTypeface(font, effectiveWeight)

        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                Typeface.create(baseTypeface, effectiveWeight.coerceIn(100, 900), false)
            }.getOrNull()
        } else null

        if (created != null) {
            paint.typeface = created
            paint.isFakeBoldText = false
        } else {
            paint.typeface = baseTypeface
            paint.isFakeBoldText = (effectiveWeight >= 700)
        }

        // Supplemental proportional stroke for clean weight thickening on custom fonts without altering width/height ratio
        if (effectiveWeight >= 700) {
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeWidth = textSizePx * ((effectiveWeight - 400) / 12000f).coerceAtLeast(0f)
        } else {
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
        }
    }

    private fun resolveFamilyTypeface(font: KustomFont, weight: Int): Typeface {
        val familyName = when (font) {
            KustomFont.KAROO_RBNO2, KustomFont.WAHOO_BARLOW, KustomFont.GARMIN_DIN, KustomFont.ATHLETIC_DISPLAY -> "sans-serif-condensed"
            KustomFont.ROBOTO_CLEAN -> "sans-serif"
        }

        val style = if (weight >= 700 || font == KustomFont.KAROO_RBNO2 || font == KustomFont.GARMIN_DIN) Typeface.BOLD else Typeface.NORMAL
        return Typeface.create(familyName, style)
    }
}
