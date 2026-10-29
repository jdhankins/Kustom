package com.hank.kustom.domain

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import com.hank.kustom.data.model.KustomFont
import java.io.File

/**
 * Premium cycling computer typeface loader supporting distinct device typography
 * (Karoo, Wahoo 1/2, Garmin 1/2, Coros Dura, Athletic, Eurostile, Oswald, Teko, Roboto).
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

        // 1. Try loading system OTF/TTF font files from device system fonts
        val fileTypeface = font.systemPaths
            .map { File(it) }
            .firstOrNull { it.exists() && it.canRead() }
            ?.let { runCatching { Typeface.createFromFile(it) }.getOrNull() }

        if (fileTypeface != null) {
            val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { Typeface.create(fileTypeface, weight.coerceIn(100, 900), false) }.getOrNull()
            } else null

            paint.typeface = created ?: fileTypeface
            paint.isFakeBoldText = (weight >= 700)
        } else {
            // 2. Distinct fallback system font family & style mapping
            val familyName = when (font) {
                KustomFont.KAROO -> "sans-serif-condensed"
                KustomFont.WAHOO_1 -> "sans-serif-condensed-light"
                KustomFont.WAHOO_2 -> "sans-serif-condensed"
                KustomFont.GARMIN_1 -> "sans-serif-condensed"
                KustomFont.GARMIN_2 -> "sans-serif-medium"
                KustomFont.COROS_DURA -> "sans-serif-medium"
                KustomFont.ATHLETIC_DISPLAY -> "sans-serif-black"
                KustomFont.EUROSTILE -> "sans-serif-smallcaps"
                KustomFont.OSWALD -> "sans-serif-condensed"
                KustomFont.TEKO -> "sans-serif-condensed-light"
                KustomFont.ROBOTO -> "sans-serif"
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
        }

        // 3. Font-specific distinct typographic aspect ratio & letter spacing
        val (scaleX, spacing) = when (font) {
            KustomFont.KAROO -> Pair(0.98f, -0.02f)
            KustomFont.WAHOO_1 -> Pair(0.95f, 0.02f)
            KustomFont.WAHOO_2 -> Pair(0.90f, -0.03f)
            KustomFont.GARMIN_1 -> Pair(0.88f, 0.0f)
            KustomFont.GARMIN_2 -> Pair(1.02f, 0.01f)
            KustomFont.COROS_DURA -> Pair(0.94f, 0.04f)
            KustomFont.ATHLETIC_DISPLAY -> Pair(1.08f, -0.04f)
            KustomFont.EUROSTILE -> Pair(1.12f, 0.05f)
            KustomFont.OSWALD -> Pair(0.86f, -0.02f)
            KustomFont.TEKO -> Pair(0.76f, -0.05f)
            KustomFont.ROBOTO -> Pair(1.0f, 0.0f)
        }

        paint.textScaleX = scaleX
        paint.letterSpacing = spacing

        // 4. Progressive stroke for clear weight differentiation across steps (300, 400, 500, 700, 900)
        val fontMultiplier = when (font) {
            KustomFont.WAHOO_1 -> 0.8f
            KustomFont.WAHOO_2 -> 1.2f
            KustomFont.GARMIN_1 -> 0.9f
            KustomFont.GARMIN_2 -> 1.1f
            KustomFont.COROS_DURA -> 1.05f
            KustomFont.ATHLETIC_DISPLAY -> 1.25f
            KustomFont.TEKO -> 1.3f
            KustomFont.EUROSTILE -> 1.15f
            else -> 1.0f
        }

        val baseFactor = when (weight) {
            300 -> 0.0f
            400 -> 0.0f
            500 -> 0.008f
            700 -> 0.018f
            900 -> 0.028f
            else -> ((weight - 400) / 18000f).coerceIn(0f, 0.028f)
        }

        val strokeFactor = baseFactor * fontMultiplier

        if (strokeFactor > 0f) {
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeWidth = textSizePx * strokeFactor
        } else {
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
        }
    }
}
