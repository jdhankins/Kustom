package com.hank.kustom.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.GlobalColorConfig
import com.hank.kustom.data.model.TypographyConfig
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.models.ViewConfig

/**
 * Offscreen bitmap renderer that draws label and value text onto an ARGB_8888 canvas.
 * The top 26% zone is reserved for the label and the bottom 74% zone for the value,
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
        globalColors: GlobalColorConfig = GlobalColorConfig(),
    ): Bitmap {
        return runCatching {
            val width = config.viewSize.first.coerceAtLeast(10)
            val height = config.viewSize.second.coerceAtLeast(10)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val boundaries = config.boundariesEnabled

            // Sensor Searching / No Sensor status text detection for live ride UX matching native Karoo OS
            val isSensorStatusText = valueText == "Searching..." || valueText == "No Sensor" || valueText == "Searching"
            val sensorStatusColor = if (isDarkTheme) 0xFFFF5252.toInt() else 0xFFD32F2F.toInt()

            // Default colors based on theme and custom background preferences
            val defaultBgColor = if (isDarkTheme) {
                globalColors.getDarkColorInt()
            } else {
                globalColors.getLightColorInt()
            }

            val defaultTextColor = KustomColorGradeMapper.getContrastingTextColor(defaultBgColor)
            val mutedLabelColor = defaultTextColor

            val backgroundColor: Int
            val labelColor: Int
            val valueColor: Int

            when (colorMode) {
                ColorMode.NONE -> {
                    backgroundColor = defaultBgColor
                    labelColor = mutedLabelColor
                    valueColor = if (isSensorStatusText) sensorStatusColor else defaultTextColor
                }
                ColorMode.TEXT_COLOR -> {
                    backgroundColor = defaultBgColor
                    labelColor = mutedLabelColor
                    valueColor = if (isSensorStatusText) sensorStatusColor else (mappedColor ?: defaultTextColor)
                }
                ColorMode.BACKGROUND_COLOR -> {
                    backgroundColor = if (isSensorStatusText) defaultBgColor else (mappedColor ?: defaultBgColor)
                    val contrastColor = if (mappedColor != null && !isSensorStatusText) {
                        KustomColorGradeMapper.getContrastingTextColor(mappedColor)
                    } else {
                        defaultTextColor
                    }
                    labelColor = contrastColor
                    valueColor = if (isSensorStatusText) sensorStatusColor else contrastColor
                }
            }

            // Draw Background (100% edge-to-edge fill with zero margins or corner rounding)
            if (boundaries) {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (backgroundColor != 0x00000000) backgroundColor else defaultBgColor
                    style = Paint.Style.FILL
                }
                canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), bgPaint)
            } else {
                if (backgroundColor != 0x00000000) {
                    canvas.drawColor(backgroundColor)
                }
                // When Data Boundaries is OFF: Draw 100% width bottom border line, 5.5px thick, dark shade of gray
                val darkGreyBorder = if (isDarkTheme) 0xFF475569.toInt() else 0xFF64748B.toInt()
                val bottomLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = darkGreyBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 5.5f
                }
                val lineY = height.toFloat() - 2.75f
                canvas.drawLine(0f, lineY, width.toFloat(), lineY, bottomLinePaint)
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

            // --- ZONE 1: TOP 26% ZONE FOR LABEL (0 .. 0.26 * height) ---
            val labelZoneHeight = height * 0.26f
            val labelPx = (height * (labelTypography.fontSizeSp / 68f)).coerceAtLeast(10f)
            val labelPaint = TextPaint().apply {
                color = labelColor
                textAlign = align
                textScaleX = 1.0f // True 1:1 aspect ratio
            }
            KustomTypefaceResolver.applyTypography(
                paint = labelPaint,
                font = labelTypography.font,
                weight = labelTypography.fontWeight,
                textSizePx = labelPx,
            )

            // Auto-scale label font size only if text width exceeds canvas width
            val upperLabel = label.uppercase()
            val minLabelTextSize = 10f
            val maxLabelWidth = width * 0.92f
            while (labelPaint.textSize > minLabelTextSize && labelPaint.measureText(upperLabel) > maxLabelWidth) {
                labelPaint.textSize -= 1f
            }

            val labelMetrics = labelPaint.fontMetrics
            val topPadding = height * 0.04f
            val labelCenterY = (labelZoneHeight / 2f) + topPadding
            val labelY = labelCenterY - ((labelMetrics.ascent + labelMetrics.descent) / 2f)

            // --- ZONE 2: BOTTOM 74% ZONE FOR VALUE (0.26 * height .. height) ---
            val valueZoneTop = height * 0.26f
            val valueZoneHeight = height * 0.74f
            val rawValuePx = (height * (valueTypography.fontSizeSp / 86f)).coerceAtLeast(18f)
            val valuePx = if (isSensorStatusText) rawValuePx * 0.52f else rawValuePx

            val valuePaint = TextPaint().apply {
                color = valueColor
                textAlign = align
                textScaleX = 1.0f // True 1:1 aspect ratio
            }
            KustomTypefaceResolver.applyTypography(
                paint = valuePaint,
                font = valueTypography.font,
                weight = if (isSensorStatusText) 700 else valueTypography.fontWeight,
                textSizePx = valuePx,
            )

            // Auto-scale value font size only if text width exceeds canvas width
            val (primaryPart, secondaryPart) = if (isSensorStatusText) Pair(valueText, "") else parsePrimaryAndSecondary(valueText)
            val minValueTextSize = 10f

            fun getMeasuredValueWidth(): Float {
                return if (secondaryPart.isNotEmpty()) {
                    val secPaint = TextPaint(valuePaint).apply { textSize = valuePaint.textSize * 0.58f }
                    valuePaint.measureText(primaryPart) + secPaint.measureText(secondaryPart)
                } else {
                    valuePaint.measureText(valueText)
                }
            }

            val maxAllowedWidth = width * 0.92f
            while (valuePaint.textSize > minValueTextSize && getMeasuredValueWidth() > maxAllowedWidth) {
                valuePaint.textSize -= 1f
            }

            val valueMetrics = valuePaint.fontMetrics
            val valueCenterY = valueZoneTop + (valueZoneHeight / 2f)
            val valueY = valueCenterY - ((valueMetrics.ascent + valueMetrics.descent) / 2f)

            // Draw Label & Value Text (Wahoo Superscript Style for Decimals/Time)
            canvas.drawText(upperLabel, textX, labelY, labelPaint)
            drawWahooValueText(canvas, valueText, textX, valueY, valuePaint, align, isSensorStatusText)

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
        isSensorStatusText: Boolean = false,
    ) {
        if (isSensorStatusText) {
            canvas.drawText(valueText, primaryX, baseY, primaryPaint)
            return
        }

        val (primaryPart, secondaryPart) = parsePrimaryAndSecondary(valueText)

        if (secondaryPart.isEmpty()) {
            canvas.drawText(primaryPart, primaryX, baseY, primaryPaint)
            return
        }

        val secondaryPaint = TextPaint(primaryPaint).apply {
            textSize = primaryPaint.textSize * 0.58f
            textScaleX = 1.0f
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
}
