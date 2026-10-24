package com.hank.kustom.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.TypographyConfig
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.models.ViewConfig

/**
 * Offscreen bitmap renderer that draws label and value text onto an ARGB_8888 canvas.
 * The top 25% zone is reserved for the label and the bottom 75% zone for the value,
 * with label and value always centered vertically in their respective spaces.
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

            val boundaries = config.boundariesEnabled

            // Default colors based on theme and boundaries
            val defaultBgColor = if (boundaries) {
                if (isDarkTheme) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            } else {
                0x00000000 // Transparent when boundaries OFF
            }

            val defaultTextColor = if (isDarkTheme) {
                0xFFFFFFFF.toInt() // White text in Dark Mode
            } else {
                0xFF1E2225.toInt() // Charcoal text in Light Mode
            }

            val mutedLabelColor = if (isDarkTheme) {
                0xFFFFFFFF.toInt() // White label in Dark Mode
            } else {
                0xFF4A5258.toInt() // Muted slate label in Light Mode
            }

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

            // Draw Background (Karoo OS draws the outer bounding box/border)
            if (boundaries) {
                val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                val cornerRadius = height * 0.12f

                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (backgroundColor != 0x00000000) backgroundColor else defaultBgColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
            } else if (backgroundColor != 0x00000000) {
                canvas.drawColor(backgroundColor)
            }

            // Text Alignment & Horizontal Position
            val marginX = width * 0.08f
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

            // --- ZONE 1: TOP 25% ZONE FOR LABEL (0 .. 0.25 * height) ---
            val labelZoneHeight = height * 0.25f
            val labelPx = spToPx(context, labelTypography.fontSizeSp, labelZoneHeight.toInt(), maxRatio = 0.98f)
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

            // Auto-scale label font size only if text width exceeds canvas width
            val maxLabelWidth = width * 0.92f
            val measuredLabelWidth = labelPaint.measureText(label.uppercase())
            if (measuredLabelWidth > maxLabelWidth && measuredLabelWidth > 0f) {
                val scaledLabelPx = labelPx * (maxLabelWidth / measuredLabelWidth)
                labelPaint.textSize = scaledLabelPx
            }

            val labelMetrics = labelPaint.fontMetrics
            val topPadding = height * 0.04f
            val labelCenterY = (labelZoneHeight / 2f) + topPadding
            val labelY = labelCenterY - ((labelMetrics.ascent + labelMetrics.descent) / 2f)

            // --- ZONE 2: BOTTOM 75% ZONE FOR VALUE (0.25 * height .. height) ---
            val valueZoneTop = height * 0.25f
            val valueZoneHeight = height * 0.75f
            val requestedValuePx = spToPx(context, valueTypography.fontSizeSp, valueZoneHeight.toInt(), maxRatio = 0.95f)
            var valuePx = requestedValuePx

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

            // Auto-scale font size if text width exceeds canvas width
            val maxTextWidth = width * 0.88f
            val measuredWidth = valuePaint.measureText(valueText)
            if (measuredWidth > maxTextWidth && measuredWidth > 0f) {
                valuePx *= (maxTextWidth / measuredWidth)
                valuePaint.textSize = valuePx
            }

            val valueMetrics = valuePaint.fontMetrics
            val valueCenterY = valueZoneTop + (valueZoneHeight / 2f)
            val valueY = valueCenterY - ((valueMetrics.ascent + valueMetrics.descent) / 2f)

            // Draw Label & Value Text
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
