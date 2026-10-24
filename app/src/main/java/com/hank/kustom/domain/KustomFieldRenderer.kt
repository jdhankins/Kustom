package com.hank.kustom.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.TypographyConfig
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.models.ViewConfig

/**
 * Offscreen bitmap renderer that draws label and value text onto an ARGB_8888 canvas.
 */
object KustomFieldRenderer {

    fun render(
        context: Context,
        config: ViewConfig,
        label: String,
        valueText: String,
        labelTypography: TypographyConfig,
        valueTypography: TypographyConfig,
        colorMode: ColorMode,
        mappedColor: Int?,
        isDarkTheme: Boolean = false,
    ): Bitmap {
        return runCatching {
            val width = config.viewSize.first.coerceAtLeast(10)
            val height = config.viewSize.second.coerceAtLeast(10)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val defaultBgColor = if (isDarkTheme) 0xFF14171A.toInt() else 0xFFFFFFFF.toInt()
            val defaultTextColor = if (isDarkTheme) 0xFFF4F6F7.toInt() else 0xFF1E2225.toInt()
            val mutedLabelColor = if (isDarkTheme) 0xFF8A96A0.toInt() else 0xFF4A5258.toInt()

            val backgroundColor: Int
            val labelColor: Int
            val valueColor: Int

            when (colorMode) {
                ColorMode.NONE -> {
                    backgroundColor = defaultBgColor
                    labelColor = mutedLabelColor
                    valueColor = defaultTextColor
                }
                ColorMode.TEXT_COLOR -> {
                    backgroundColor = defaultBgColor
                    labelColor = mutedLabelColor
                    valueColor = mappedColor ?: defaultTextColor
                }
                ColorMode.BACKGROUND_COLOR -> {
                    backgroundColor = mappedColor ?: defaultBgColor
                    val contrastColor = if (mappedColor != null) {
                        KustomColorGradeMapper.getContrastingTextColor(mappedColor)
                    } else {
                        defaultTextColor
                    }
                    labelColor = contrastColor
                    valueColor = contrastColor
                }
            }

            canvas.drawColor(backgroundColor)

            val marginX = width * 0.06f
            val align = when (config.alignment) {
                ViewConfig.Alignment.LEFT -> Paint.Align.LEFT
                ViewConfig.Alignment.CENTER -> Paint.Align.CENTER
                ViewConfig.Alignment.RIGHT -> Paint.Align.RIGHT
            }

            val textX = when (config.alignment) {
                ViewConfig.Alignment.LEFT -> marginX
                ViewConfig.Alignment.CENTER -> width / 2f
                ViewConfig.Alignment.RIGHT -> width - marginX
            }

            // 1. Prepare Label Paint
            val labelPx = spToPx(context, labelTypography.fontSizeSp, height, maxRatio = 0.22f)
            val labelPaint = TextPaint().apply {
                color = labelColor
                textAlign = align
            }
            KustomTypefaceResolver.applyTypography(
                paint = labelPaint,
                font = labelTypography.font,
                weight = labelTypography.fontWeight,
                textSizePx = labelPx,
            )

            // 2. Prepare Value Paint
            val maxAvailableValuePx = (height * 0.52f).coerceAtLeast(10f)
            val requestedValuePx = spToPx(context, valueTypography.fontSizeSp, height, maxRatio = 0.70f)
            val valuePx = requestedValuePx.coerceAtMost(maxAvailableValuePx)

            val valuePaint = TextPaint().apply {
                color = valueColor
                textAlign = align
            }
            KustomTypefaceResolver.applyTypography(
                paint = valuePaint,
                font = valueTypography.font,
                weight = valueTypography.fontWeight,
                textSizePx = valuePx,
            )

            // 3. Compute Metrics with Zero-Overlap Baseline Equation
            val labelMetrics = labelPaint.fontMetrics
            val labelAscent = -labelMetrics.ascent
            val labelDescent = labelMetrics.descent
            val labelHeight = labelAscent + labelDescent

            val valueMetrics = valuePaint.fontMetrics
            val valueAscent = -valueMetrics.ascent
            val valueDescent = valueMetrics.descent
            val valueHeight = valueAscent + valueDescent

            val gap = height * 0.04f
            val totalBlockHeight = labelHeight + gap + valueHeight

            val rawBlockTop = (height - totalBlockHeight) / 2f
            val blockTop = rawBlockTop.coerceAtLeast(height * 0.04f)

            val labelY = blockTop + labelAscent
            val valueY = labelY + labelDescent + gap + valueAscent

            // 4. Draw Label & Value
            canvas.drawText(label.uppercase(), textX, labelY, labelPaint)
            canvas.drawText(valueText, textX, valueY, valuePaint)

            bitmap
        }.getOrElse { throwable ->
            KustomLogger.e("Error rendering field bitmap", throwable)
            createFallbackBitmap(
                config.viewSize.first.coerceAtLeast(10),
                config.viewSize.second.coerceAtLeast(10),
            )
        }
    }

    private fun createFallbackBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFF14171A.toInt())
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF4F6F7.toInt()
            textSize = height * 0.3f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ERR", width / 2f, height / 2f, paint)
        return bitmap
    }

    private fun spToPx(context: Context, sp: Float, containerHeightPx: Int, maxRatio: Float): Float {
        val density = context.resources.displayMetrics.scaledDensity
        val calculatedPx = sp * density
        return calculatedPx.coerceAtMost(containerHeightPx * maxRatio)
    }
}
