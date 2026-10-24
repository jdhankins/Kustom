package com.hank.kustom.domain

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import com.hank.kustom.data.model.KustomFont

/**
 * Typeface loader for top 5 cycling computer fonts (Karoo, Wahoo, Garmin, Athletic Display, Clean).
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
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f

        // Distinct character scale & letter spacing per cycling font family
        when (font) {
            KustomFont.KAROO_RBNO2 -> {
                paint.textScaleX = 0.90f
                paint.letterSpacing = -0.01f
            }
            KustomFont.WAHOO_BARLOW -> {
                paint.textScaleX = 0.96f
                paint.letterSpacing = 0.01f
            }
            KustomFont.GARMIN_DIN -> {
                paint.textScaleX = 0.88f
                paint.letterSpacing = -0.02f
            }
            KustomFont.ATHLETIC_DISPLAY -> {
                paint.textScaleX = 0.80f
                paint.letterSpacing = -0.03f
            }
            KustomFont.ROBOTO_CLEAN -> {
                paint.textScaleX = 1.0f
                paint.letterSpacing = 0.0f
            }
        }

        val effectiveWeight = if (font == KustomFont.KAROO_RBNO2 || font == KustomFont.GARMIN_DIN || font == KustomFont.ATHLETIC_DISPLAY) {
            weight.coerceAtLeast(700)
        } else {
            weight
        }

        val baseTypeface = resolveFamilyTypeface(font, effectiveWeight)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val created = runCatching {
                Typeface.create(baseTypeface, effectiveWeight.coerceIn(100, 900), false)
            }.getOrNull()
            paint.typeface = created ?: baseTypeface
        } else {
            paint.typeface = baseTypeface
        }

        paint.isFakeBoldText = (effectiveWeight >= 700)
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
