package com.hank.kustom.domain

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import com.hank.kustom.data.model.KustomFont
import java.io.File

/**
 * Typeface loader with distinct font-family styling and native weight rendering.
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

        // Character scale & letter spacing per font family
        when (font) {
            KustomFont.RBNO2_BOLD -> {
                paint.textScaleX = 0.90f
                paint.letterSpacing = -0.01f
            }
            KustomFont.RBNO2_MEDIUM -> {
                paint.textScaleX = 0.94f
                paint.letterSpacing = 0.0f
            }
            KustomFont.BARLOW -> {
                paint.textScaleX = 0.98f
                paint.letterSpacing = 0.0f
            }
            KustomFont.ROBOTO -> {
                paint.textScaleX = 1.0f
                paint.letterSpacing = 0.0f
            }
            KustomFont.NOTO_SANS -> {
                paint.textScaleX = 1.02f
                paint.letterSpacing = 0.01f
            }
        }

        val effectiveWeight = if (font == KustomFont.RBNO2_BOLD) weight.coerceAtLeast(700) else weight
        val baseTypeface = resolveBaseTypeface(font, effectiveWeight)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val created = runCatching {
                Typeface.create(baseTypeface, effectiveWeight.coerceIn(100, 900), false)
            }.getOrNull()
            paint.typeface = created ?: baseTypeface
        } else {
            paint.typeface = baseTypeface
            paint.isFakeBoldText = effectiveWeight >= 700
        }
    }

    private fun resolveBaseTypeface(font: KustomFont, weight: Int): Typeface {
        for (path in font.systemPaths) {
            val file = File(path)
            if (file.exists()) {
                val tf = runCatching { Typeface.createFromFile(file) }.getOrNull()
                if (tf != null) return tf
            }
        }

        val familyName = when (font) {
            KustomFont.RBNO2_BOLD, KustomFont.RBNO2_MEDIUM, KustomFont.BARLOW -> "sans-serif-condensed"
            KustomFont.ROBOTO -> "Roboto"
            KustomFont.NOTO_SANS -> "sans-serif"
        }

        val style = if (weight >= 700 || font == KustomFont.RBNO2_BOLD) Typeface.BOLD else Typeface.NORMAL
        return Typeface.create(familyName, style)
    }
}
