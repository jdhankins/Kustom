package com.hank.kustom.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.TypographyConfig
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.models.ViewConfig

/**
 * Offscreen bitmap renderer that draws label and value text onto an ARGB_8888 canvas.
 * The top 25% zone is reserved for the label and the bottom 75% zone for the value,
 * with Wahoo ELEMNT style superscript formatting for decimals and time colons.
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
            val (primaryPart, secondaryPart) = parsePrimaryAndSecondary(valueText)
            val measuredWidth = if (secondaryPart.isNotEmpty()) {
                val secPaint = TextPaint(valuePaint).apply { textSize = valuePaint.textSize * 0.58f }
                valuePaint.measureText(primaryPart) + secPaint.measureText(secondaryPart)
            } else {
                valuePaint.measureText(valueText)
            }

            if (measuredWidth > maxTextWidth && measuredWidth > 0f) {
                valuePx *= (maxTextWidth / measuredWidth)
                valuePaint.textSize = valuePx
            }

            val valueMetrics = valuePaint.fontMetrics
            val valueCenterY = valueZoneTop + (valueZoneHeight / 2f)
            val valueY = valueCenterY - ((valueMetrics.ascent + valueMetrics.descent) / 2f)

            // Draw Label & Value Text (Wahoo Superscript Style for Decimals/Time)
            canvas.drawText(label.uppercase(), textX, labelY, labelPaint)
            drawWahooValueText(canvas, valueText, textX, valueY, valuePaint, align)

            bitmap
        }.getOrElse { throwable ->
            KustomLogger.e("Error rendering field bitmap", throwable)
            createFallbackBitmap(
                config.viewSize.first.coerceAtLeast(10),
                config.viewSize.second.coerceAtLeast(10),
            )
        }
    }

    /**
     * Draws value text with Wahoo ELEMNT style superscript formatting for decimals and time colons.
     * The top of the superscript digits is aligned exactly even with the top of the main value digits.
     */
    private fun drawWahooValueText(
        canvas: Canvas,
        valueText: String,
        primaryX: Float,
        baseY: Float,
        primaryPaint: TextPaint,
        align: Paint.Align,
    ) {
        val (primaryPart, secondaryPart) = parsePrimaryAndSecondary(valueText)

        if (secondaryPart.isEmpty()) {
            canvas.drawText(primaryPart, primaryX, baseY, primaryPaint)
            return
        }

        val secondaryPaint = TextPaint(primaryPaint).apply {
            textSize = primaryPaint.textSize * 0.58f
        }

        val primaryWidth = primaryPaint.measureText(primaryPart)
        val secondaryWidth = secondaryPaint.measureText(secondaryPart)
        val totalWidth = primaryWidth + secondaryWidth

        // Exact cap-height top bounds for primary digits
        val primaryBounds = Rect()
        primaryPaint.getTextBounds("0", 0, 1, primaryBounds)

        // Exact cap-height top bounds for secondary digits
        val secondaryBounds = Rect()
        val sampleChar = if (secondaryPart.startsWith(":")) "0" else secondaryPart.first().toString()
        secondaryPaint.getTextBounds(sampleChar, 0, 1, secondaryBounds)

        // Calculate secondary baseline Y so top of secondary digits is EXACTLY EVEN with top of primary digits
        val secondaryY = baseY + primaryBounds.top - secondaryBounds.top

        val (primaryStartX, secondaryStartX) = when (align) {
            Paint.Align.LEFT -> Pair(primaryX, primaryX + primaryWidth)
            Paint.Align.CENTER -> {
                val startX = primaryX - (totalWidth / 2f)
                Pair(startX, startX + primaryWidth)
            }
            Paint.Align.RIGHT -> {
                val startX = primaryX - totalWidth
                Pair(startX, startX + primaryWidth)
            }
        }

        val drawPrimaryPaint = TextPaint(primaryPaint).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText(primaryPart, primaryStartX, baseY, drawPrimaryPaint)

        val drawSecondaryPaint = TextPaint(secondaryPaint).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText(secondaryPart, secondaryStartX, secondaryY, drawSecondaryPaint)
    }

    /**
     * Splits value text into primary main digits and secondary superscript digits (for decimals or time colons).
     */
    private fun parsePrimaryAndSecondary(valueText: String): Pair<String, String> {
        if (valueText.contains(":")) {
            val idx = valueText.indexOf(":")
            val primary = valueText.substring(0, idx)
            val secondary = valueText.substring(idx)
            return Pair(primary, secondary)
        } else if (valueText.contains(".")) {
            val idx = valueText.indexOf(".")
            val primary = valueText.substring(0, idx)
            val secondary = valueText.substring(idx + 1)
            return Pair(primary, secondary)
        }
        return Pair(valueText, "")
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
