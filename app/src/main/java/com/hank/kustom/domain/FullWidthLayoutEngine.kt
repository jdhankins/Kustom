package com.hank.kustom.domain

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import com.hank.kustom.data.model.TypographyConfig

/**
 * Vertical alignment positioning modes for bounding box text drawing.
 */
enum class VerticalAlign {
    TOP,
    CENTER
}

/**
 * Slot content data models for full-width composite fields.
 */
sealed class HeaderSlot {
    /**
     * Standard 2-row metric stack (Row 1 = Label, Row 2 = Value).
     */
    data class MetricStack(
        val label: String,
        val value: String,
        val isZoneFilled: Boolean = false,
        val zoneColor: Int? = null,
    ) : HeaderSlot()

    /**
     * Status pill badge (e.g. "ON TARGET", "LOW -15W", "HIGH +20W").
     */
    data class StatusPill(
        val text: String,
        val backgroundColor: Int,
        val textColor: Int = 0xFFFFFFFF.toInt(),
    ) : HeaderSlot()

    /**
     * Hero primary metric digits in the center, with optional header label drawn directly above.
     */
    data class HeroValue(
        val value: String,
        val label: String? = null,
        val hasBottomBar: Boolean = false,
    ) : HeaderSlot()
}

/**
 * Shared layout engine for all full-width multi-slot fields in Kustom.
 * Manages strict 1:1 Bounding-Box Grid, baselines, font sizing, auto-scaling, and drawing.
 */
object FullWidthLayoutEngine {

    /**
     * Renders labels at a strictly fixed pixel size with ZERO auto-scaling,
     * top-anchoring glyphs so all labels across Left, Center, and Right share the exact same top cap-line.
     */
    private fun drawFixedLabel(
        canvas: Canvas,
        text: String,
        anchorX: Float,
        topY: Float,
        paint: Paint,
        align: Paint.Align,
        fixedTextSizePx: Float,
    ) {
        if (text.isBlank()) return
        val strokeRatio = if (paint.style == Paint.Style.FILL_AND_STROKE && paint.textSize > 0f) {
            (paint.strokeWidth / paint.textSize).coerceAtMost(0.028f)
        } else 0f

        paint.textAlign = Paint.Align.LEFT
        paint.textScaleX = 1.0f
        paint.textSize = fixedTextSizePx
        if (strokeRatio > 0f) {
            paint.strokeWidth = fixedTextSizePx * strokeRatio
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val halfStroke = paint.strokeWidth / 2f

        val drawX = when (align) {
            Paint.Align.LEFT -> anchorX - bounds.left + halfStroke
            Paint.Align.RIGHT -> anchorX - bounds.right - halfStroke
            else -> anchorX - bounds.exactCenterX()
        }
        val drawY = topY - bounds.top + halfStroke
        canvas.drawText(text, drawX, drawY, paint)
    }

    /**
     * Splits value text into primary main digits and secondary superscript digits (for decimals or time colons).
     */
    internal fun parsePrimaryAndSecondary(valueText: String): Pair<String, String> {
        if (valueText.contains(":")) {
            val idx = valueText.lastIndexOf(":")
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

    /**
     * Fits and positions dynamic values strictly inside [boxLeft, boxTop, boxRight, boxBottom]
     * using true visible glyph ink bounds (Paint.getTextBounds), with support for superscript decimals and colons.
     */
    internal fun drawTextInBox(
        canvas: Canvas,
        text: String,
        boxLeft: Float,
        boxTop: Float,
        boxRight: Float,
        boxBottom: Float,
        paint: Paint,
        align: Paint.Align,
        verticalAlign: VerticalAlign = VerticalAlign.CENTER,
        maxStartTextSizePx: Float,
        minTextSizePx: Float = 10f,
    ): Rect {
        val renderText = text.ifEmpty { "--" }
        val boxW = (boxRight - boxLeft).coerceAtLeast(1f)
        val boxH = (boxBottom - boxTop).coerceAtLeast(1f)
        paint.textAlign = Paint.Align.LEFT // Always draw using exact LEFT offset from getTextBounds
        paint.textScaleX = 1.0f

        var strokeRatio = if (maxStartTextSizePx > 0f && paint.strokeWidth > 0f) (paint.strokeWidth / maxStartTextSizePx).coerceAtMost(0.028f) else 0f

        if (renderText == "--") {
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
            strokeRatio = 0f
        }

        // Check if text has decimal point or time colon for superscript rendering (e.g. "18.1" -> 18¹ or "02:50" -> 02:⁵⁰)
        val isSuperscriptEligible = (renderText.contains(".") || renderText.contains(":")) && 
            !renderText.drop(1).contains("-") && !renderText.contains(" ")

        if (isSuperscriptEligible) {
            val (primary, secondary) = parsePrimaryAndSecondary(renderText)
            if (secondary.isNotEmpty()) {
                var size = maxStartTextSizePx.coerceAtLeast(minTextSizePx)
                paint.textSize = size
                if (strokeRatio > 0f) {
                    paint.strokeWidth = size * strokeRatio
                }

                val secPaint = TextPaint(paint).apply {
                    textSize = size * 0.58f
                    if (strokeRatio > 0f) {
                        strokeWidth = (size * 0.58f) * strokeRatio
                    }
                }

                val priWidthBounds = Rect()
                paint.getTextBounds(primary, 0, primary.length, priWidthBounds)

                val priHeightBounds = Rect()
                paint.getTextBounds("88", 0, 2, priHeightBounds)

                val secWidthBounds = Rect()
                secPaint.getTextBounds(secondary, 0, secondary.length, secWidthBounds)

                var totalW = priWidthBounds.width() + secWidthBounds.width()
                var totalH = priHeightBounds.height()

                while (size > minTextSizePx && (totalW + paint.strokeWidth > boxW || totalH + paint.strokeWidth > boxH)) {
                    size -= 1f
                    paint.textSize = size
                    secPaint.textSize = size * 0.58f
                    if (strokeRatio > 0f) {
                        paint.strokeWidth = size * strokeRatio
                        secPaint.strokeWidth = (size * 0.58f) * strokeRatio
                    }
                    paint.getTextBounds(primary, 0, primary.length, priWidthBounds)
                    paint.getTextBounds("88", 0, 2, priHeightBounds)
                    secPaint.getTextBounds(secondary, 0, secondary.length, secWidthBounds)
                    totalW = priWidthBounds.width() + secWidthBounds.width()
                    totalH = priHeightBounds.height()
                }

                val secGap = (paint.textSize * 0.035f).coerceIn(1.2f, 2.2f)
                val primaryW = paint.measureText(primary)
                val secondaryW = secPaint.measureText(secondary)
                val totalAdvanceWidth = primaryW + secGap + secondaryW

                val priX = when (align) {
                    Paint.Align.LEFT -> boxLeft - priWidthBounds.left + (paint.strokeWidth / 2f)
                    Paint.Align.RIGHT -> boxRight - totalAdvanceWidth - priWidthBounds.left - (paint.strokeWidth / 2f)
                    else -> ((boxLeft + boxRight) / 2f) - (totalAdvanceWidth / 2f) - priWidthBounds.left
                }

                val priY = when (verticalAlign) {
                    VerticalAlign.TOP -> boxTop - priHeightBounds.top + (paint.strokeWidth / 2f)
                    VerticalAlign.CENTER -> ((boxTop + boxBottom) / 2f) - priHeightBounds.exactCenterY()
                }

                canvas.drawText(primary, priX, priY, paint)

                val secX = priX + primaryW + secGap
                val secY = priY + priHeightBounds.top - secWidthBounds.top

                canvas.drawText(secondary, secX, secY, secPaint)

                return Rect(
                    priWidthBounds.left,
                    priHeightBounds.top,
                    totalAdvanceWidth.toInt(),
                    priHeightBounds.bottom
                )
            }
        }

        // Standard Single-Paint Text Drawing (No Superscript)
        var size = maxStartTextSizePx.coerceAtLeast(minTextSizePx)
        paint.textSize = size
        if (strokeRatio > 0f) {
            paint.strokeWidth = size * strokeRatio
        }

        val widthBounds = Rect()
        paint.getTextBounds(renderText, 0, renderText.length, widthBounds)

        val heightBounds = Rect()
        paint.getTextBounds("88", 0, 2, heightBounds)

        while (size > minTextSizePx && (widthBounds.width() + paint.strokeWidth > boxW || heightBounds.height() + paint.strokeWidth > boxH)) {
            size -= 1f
            paint.textSize = size
            if (strokeRatio > 0f) {
                paint.strokeWidth = size * strokeRatio
            }
            paint.getTextBounds(renderText, 0, renderText.length, widthBounds)
            paint.getTextBounds("88", 0, 2, heightBounds)
        }

        val halfStroke = paint.strokeWidth / 2f
        val drawX = when (align) {
            Paint.Align.LEFT -> boxLeft - widthBounds.left + halfStroke
            Paint.Align.RIGHT -> boxRight - widthBounds.right - halfStroke
            else -> ((boxLeft + boxRight) / 2f) - widthBounds.exactCenterX()
        }

        val drawY = when (verticalAlign) {
            VerticalAlign.TOP -> boxTop - heightBounds.top + halfStroke
            VerticalAlign.CENTER -> ((boxTop + boxBottom) / 2f) - heightBounds.exactCenterY()
        }

        canvas.drawText(renderText, drawX, drawY, paint)
        return widthBounds
    }

    private fun isSensorStatusText(text: String): Boolean {
        return text == "Searching..." || text == "No Sensor" || text == "Searching"
    }

    /**
     * Draws the standardized 3-slot header (Left, Center Hero, Right) on the provided Canvas
     * using strict non-overlapping bounding-box grid geometry and zero-scaling labels.
     */
    fun drawHeaderSlots(
        canvas: Canvas,
        leftSlot: HeaderSlot?,
        centerHero: HeaderSlot.HeroValue?,
        rightSlot: HeaderSlot?,
        width: Float,
        height: Float,
        defaultTextColor: Int,
        mutedTextColor: Int,
        labelTypography: TypographyConfig,
        valueTypography: TypographyConfig,
        isDarkTheme: Boolean = false,
        globalZonePalette: com.hank.kustom.data.model.ZonePalette = com.hank.kustom.data.model.ZonePalette.KAROO,
    ) {
        val sensorRedColor = if (isDarkTheme) 0xFFFF5252.toInt() else 0xFFD32F2F.toInt()
        val hasLeft = leftSlot is HeaderSlot.MetricStack && leftSlot.label.isNotEmpty()
        val hasRight = rightSlot != null
        val sideSlotsPresent = hasLeft || hasRight

        // Strict Horizontal Column Coordinates (Sum = 100%)
        val leftColLeft = width * 0.030f
        val leftColRight = width * 0.305f

        val rightColLeft = width * 0.695f
        val rightColRight = width * 0.970f

        val heroColLeft = if (sideSlotsPresent) width * 0.320f else width * 0.080f
        val heroColRight = if (sideSlotsPresent) width * 0.680f else width * 0.920f

        // Top Cap-Line for All Labels across the card (6% height)
        val labelTopY = height * 0.06f

        // Value Box for Left/Right Corner Values
        val valueBoxTop = height * 0.26f
        val valueBoxBottom = if (centerHero?.hasBottomBar == true) height * 0.72f else height * 0.82f

        // Unified fixed label font size across ALL 3 labels on the card (16.5%..22.5% height)
        val unifiedLabelPx = height * (0.165f + ((labelTypography.fontSizeSp - 10f).coerceIn(0f, 10f) * 0.006f))
        val startValuePx = (height * (valueTypography.fontSizeSp / 74f)).coerceAtLeast(20f)

        // 1. Draw Left Slot
        if (hasLeft && leftSlot is HeaderSlot.MetricStack) {
            val leftValColor = when {
                isSensorStatusText(leftSlot.value) -> sensorRedColor
                leftSlot.isZoneFilled && leftSlot.zoneColor != null -> KustomColorGradeMapper.getContrastingTextColor(leftSlot.zoneColor)
                else -> defaultTextColor
            }
            val leftLabelColor = if (leftSlot.isZoneFilled && leftSlot.zoneColor != null) leftValColor else defaultTextColor

            val labelPaint = TextPaint().apply {
                color = leftLabelColor
            }
            KustomTypefaceResolver.applyTypography(labelPaint, labelTypography.font, labelTypography.fontWeight, unifiedLabelPx)

            val valPaint = TextPaint().apply {
                color = leftValColor
            }
            KustomTypefaceResolver.applyTypography(valPaint, valueTypography.font, valueTypography.fontWeight, startValuePx)

            drawFixedLabel(
                canvas = canvas,
                text = leftSlot.label,
                anchorX = leftColLeft,
                topY = labelTopY,
                paint = labelPaint,
                align = Paint.Align.LEFT,
                fixedTextSizePx = unifiedLabelPx
            )

            val vBounds = drawTextInBox(
                canvas = canvas,
                text = leftSlot.value,
                boxLeft = leftColLeft,
                boxTop = valueBoxTop,
                boxRight = leftColRight,
                boxBottom = valueBoxBottom,
                paint = valPaint,
                align = Paint.Align.LEFT,
                verticalAlign = VerticalAlign.TOP,
                maxStartTextSizePx = startValuePx,
                minTextSizePx = 10f
            )

            if (leftSlot.isZoneFilled && leftSlot.zoneColor != null) {
                val lPillRect = RectF(
                    leftColLeft - width * 0.012f,
                    height * 0.05f,
                    leftColLeft + vBounds.width().toFloat() + width * 0.020f,
                    valueBoxBottom + height * 0.02f
                )
                if (globalZonePalette == com.hank.kustom.data.model.ZonePalette.NEON_GLASS) {
                    KustomFieldRenderer.drawNeonGlassRoundRect(canvas, lPillRect, 8f, leftSlot.zoneColor, isActiveHighlight = false, palette = globalZonePalette)
                } else {
                    val lPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = leftSlot.zoneColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(lPillRect, 8f, 8f, lPillPaint)
                }
            }
        }

        // 2. Draw Right Slot
        if (rightSlot != null) {
            when (rightSlot) {
                is HeaderSlot.StatusPill -> {
                    val pillBoxLeft = width * 0.690f
                    val pillBoxRight = width * 0.965f
                    val pillBoxTop = height * 0.06f
                    val pillBoxBottom = height * 0.30f
                    val pillH = pillBoxBottom - pillBoxTop

                    val badgeTextPx = (pillH * 0.50f).coerceIn(11f, 14f)
                    val badgeTextPaint = TextPaint().apply {
                        color = rightSlot.textColor
                    }
                    KustomTypefaceResolver.applyTypography(badgeTextPaint, labelTypography.font, 900, badgeTextPx)

                    val badgeW = badgeTextPaint.measureText(rightSlot.text)
                    val pillW = badgeW + (width * 0.05f)
                    val pRight = pillBoxRight
                    val pLeft = (pRight - pillW).coerceAtLeast(pillBoxLeft)

                    val badgePillRect = RectF(pLeft, pillBoxTop, pRight, pillBoxBottom)
                    if (globalZonePalette == com.hank.kustom.data.model.ZonePalette.NEON_GLASS) {
                        KustomFieldRenderer.drawNeonGlassRoundRect(canvas, badgePillRect, pillH / 2f, rightSlot.backgroundColor, isActiveHighlight = false, palette = globalZonePalette)
                    } else {
                        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = rightSlot.backgroundColor
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(badgePillRect, pillH / 2f, pillH / 2f, badgePaint)
                    }

                    drawTextInBox(
                        canvas = canvas,
                        text = rightSlot.text,
                        boxLeft = pLeft,
                        boxTop = pillBoxTop,
                        boxRight = pRight,
                        boxBottom = pillBoxBottom,
                        paint = badgeTextPaint,
                        align = Paint.Align.CENTER,
                        verticalAlign = VerticalAlign.CENTER,
                        maxStartTextSizePx = badgeTextPx,
                        minTextSizePx = 9f
                    )
                }

                is HeaderSlot.MetricStack -> {
                    if (rightSlot.label.isNotEmpty()) {
                        val isZoneFilled = rightSlot.isZoneFilled && rightSlot.zoneColor != null
                        val rightValColor = when {
                            isSensorStatusText(rightSlot.value) -> sensorRedColor
                            isZoneFilled -> 0xFFFFFFFF.toInt()
                            else -> defaultTextColor
                        }
                        val rightLabelColor = if (isZoneFilled) 0xFFFFFFFF.toInt() else defaultTextColor

                        val labelPaint = TextPaint().apply {
                            color = rightLabelColor
                        }
                        KustomTypefaceResolver.applyTypography(labelPaint, labelTypography.font, labelTypography.fontWeight, unifiedLabelPx)

                        val valPaint = TextPaint().apply {
                            color = rightValColor
                        }
                        KustomTypefaceResolver.applyTypography(valPaint, valueTypography.font, valueTypography.fontWeight, startValuePx)

                        if (isZoneFilled) {
                            labelPaint.letterSpacing = 0.04f
                            valPaint.letterSpacing = 0.04f

                            val pBoxL = rightColLeft - width * 0.010f
                            val pBoxR = rightColRight + width * 0.010f
                            val pTop = height * 0.04f
                            val pBottom = height * 0.58f
                            val pH = pBottom - pTop

                            val rPillRect = RectF(pBoxL, pTop, pBoxR, pBottom)
                            if (globalZonePalette == com.hank.kustom.data.model.ZonePalette.NEON_GLASS) {
                                KustomFieldRenderer.drawNeonGlassRoundRect(canvas, rPillRect, 12f, rightSlot.zoneColor, isActiveHighlight = false, palette = globalZonePalette)
                            } else {
                                val rPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = rightSlot.zoneColor
                                    style = Paint.Style.FILL
                                }
                                val rPillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = 0x4DFFFFFF.toInt() // 30% semi-transparent white outline
                                    style = Paint.Style.STROKE
                                    strokeWidth = 1.5f
                                }
                                canvas.drawRoundRect(rPillRect, 12f, 12f, rPillPaint)
                                canvas.drawRoundRect(rPillRect, 12f, 12f, rPillBorderPaint)
                            }

                            val line1Top = pTop + (pH * 0.08f)
                            val line1Bottom = pTop + (pH * 0.50f)

                            val line2Top = pTop + (pH * 0.48f)
                            val line2Bottom = pBottom - (pH * 0.08f)

                            drawTextInBox(
                                canvas = canvas,
                                text = rightSlot.label,
                                boxLeft = pBoxL,
                                boxTop = line1Top,
                                boxRight = pBoxR,
                                boxBottom = line1Bottom,
                                paint = labelPaint,
                                align = Paint.Align.CENTER,
                                verticalAlign = VerticalAlign.CENTER,
                                maxStartTextSizePx = unifiedLabelPx * 1.1f,
                                minTextSizePx = 9f
                            )

                            drawTextInBox(
                                canvas = canvas,
                                text = rightSlot.value,
                                boxLeft = pBoxL,
                                boxTop = line2Top,
                                boxRight = pBoxR,
                                boxBottom = line2Bottom,
                                paint = valPaint,
                                align = Paint.Align.CENTER,
                                verticalAlign = VerticalAlign.CENTER,
                                maxStartTextSizePx = startValuePx * 0.85f,
                                minTextSizePx = 10f
                            )
                        } else {
                            drawFixedLabel(
                                canvas = canvas,
                                text = rightSlot.label,
                                anchorX = rightColRight,
                                topY = labelTopY,
                                paint = labelPaint,
                                align = Paint.Align.RIGHT,
                                fixedTextSizePx = unifiedLabelPx
                            )

                            drawTextInBox(
                                canvas = canvas,
                                text = rightSlot.value,
                                boxLeft = rightColLeft,
                                boxTop = valueBoxTop,
                                boxRight = rightColRight,
                                boxBottom = valueBoxBottom,
                                paint = valPaint,
                                align = Paint.Align.RIGHT,
                                verticalAlign = VerticalAlign.TOP,
                                maxStartTextSizePx = startValuePx,
                                minTextSizePx = 10f
                            )
                        }
                    }
                }
                is HeaderSlot.HeroValue -> {}
            }
        }

        // 3. Draw Center Hero Slot
        if (centerHero != null) {
            val headerLabelText = centerHero.label
            val hasHeaderLabel = !headerLabelText.isNullOrEmpty()
            val hasBottomBar = centerHero.hasBottomBar

            if (hasHeaderLabel) {
                val heroLabelPaint = TextPaint().apply {
                    color = defaultTextColor
                }
                KustomTypefaceResolver.applyTypography(heroLabelPaint, labelTypography.font, labelTypography.fontWeight, unifiedLabelPx)

                drawFixedLabel(
                    canvas = canvas,
                    text = headerLabelText,
                    anchorX = width / 2f,
                    topY = labelTopY,
                    paint = heroLabelPaint,
                    align = Paint.Align.CENTER,
                    fixedTextSizePx = unifiedLabelPx
                )
            }

            val heroValueBoxTop = if (hasHeaderLabel) height * 0.26f else height * 0.10f
            val heroValueBoxBottom = if (hasBottomBar) height * 0.74f else height * 0.90f

            val heroValColor = if (isSensorStatusText(centerHero.value)) sensorRedColor else defaultTextColor
            val heroValPaint = TextPaint().apply {
                color = heroValColor
            }
            KustomTypefaceResolver.applyTypography(heroValPaint, valueTypography.font, valueTypography.fontWeight, startValuePx)

            drawTextInBox(
                canvas = canvas,
                text = centerHero.value,
                boxLeft = heroColLeft,
                boxTop = heroValueBoxTop,
                boxRight = heroColRight,
                boxBottom = heroValueBoxBottom,
                paint = heroValPaint,
                align = Paint.Align.CENTER,
                verticalAlign = VerticalAlign.CENTER,
                maxStartTextSizePx = startValuePx,
                minTextSizePx = 14f
            )
        }
    }

    private fun drawRotatedEdgeLabel(
        canvas: Canvas,
        text: String,
        x: Float,
        yCenter: Float,
        maxHeight: Float,
        paint: TextPaint,
        rotationDegrees: Float,
    ) {
        val upperText = text.uppercase()
        val minTextSize = 10f
        while (paint.textSize > minTextSize && paint.measureText(upperText) > maxHeight) {
            paint.textSize -= 1f
        }

        canvas.save()
        canvas.translate(x, yCenter)
        canvas.rotate(rotationDegrees)
        
        val bounds = Rect()
        paint.getTextBounds(upperText, 0, upperText.length, bounds)
        
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(upperText, 0f, bounds.height() / 2f, paint)
        canvas.restore()
    }

    fun drawDualSplitCard(
        canvas: Canvas,
        leftLabel: String,
        leftValueText: String,
        leftZoneFormat: com.hank.kustom.data.model.ZoneFormat,
        leftZoneColor: Int?,
        rightLabel: String,
        rightValueText: String,
        rightZoneFormat: com.hank.kustom.data.model.ZoneFormat,
        rightZoneColor: Int?,
        width: Float,
        height: Float,
        defaultBgColor: Int,
        defaultTextColor: Int,
        labelTypography: TypographyConfig,
        valueTypography: TypographyConfig,
        isDarkTheme: Boolean = false,
        globalZonePalette: com.hank.kustom.data.model.ZonePalette = com.hank.kustom.data.model.ZonePalette.KAROO,
    ) {
        val cornerRadius = height * 0.12f
        val clipPath = android.graphics.Path().apply {
            addRoundRect(RectF(1.5f, 1.5f, width - 1.5f, height - 1.5f), cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        val halfW = width / 2f

        // Draw Backgrounds
        val leftRect = RectF(0f, 0f, halfW, height)
        val rightRect = RectF(halfW, 0f, width, height)

        val isLeftBg = leftZoneFormat == com.hank.kustom.data.model.ZoneFormat.BACKGROUND && leftZoneColor != null
        val isRightBg = rightZoneFormat == com.hank.kustom.data.model.ZoneFormat.BACKGROUND && rightZoneColor != null

        if (isLeftBg) {
            if (globalZonePalette == com.hank.kustom.data.model.ZonePalette.NEON_GLASS) {
                KustomFieldRenderer.drawNeonGlassRoundRect(canvas, leftRect, 0f, leftZoneColor!!, isActiveHighlight = false, palette = globalZonePalette)
            } else {
                canvas.drawRect(leftRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = leftZoneColor!! })
            }
        } else {
            canvas.drawRect(leftRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = defaultBgColor })
        }

        if (isRightBg) {
            if (globalZonePalette == com.hank.kustom.data.model.ZonePalette.NEON_GLASS) {
                KustomFieldRenderer.drawNeonGlassRoundRect(canvas, rightRect, 0f, rightZoneColor!!, isActiveHighlight = false, palette = globalZonePalette)
            } else {
                canvas.drawRect(rightRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rightZoneColor!! })
            }
        } else {
            canvas.drawRect(rightRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = defaultBgColor })
        }

        // Draw Center Divider
        val dividerColor = when {
            isLeftBg || isRightBg -> KustomColorGradeMapper.getContrastingTextColor(if (isLeftBg) leftZoneColor!! else rightZoneColor!!)
            isDarkTheme -> 0xFF334155.toInt()
            else -> 0xFFCBD5E1.toInt()
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
        }
        canvas.drawLine(halfW, 0f, halfW, height, dividerPaint)

        // Text Colors
        val leftTextColor = if (isLeftBg) KustomColorGradeMapper.getContrastingTextColor(leftZoneColor!!) else (if (leftZoneFormat == com.hank.kustom.data.model.ZoneFormat.TEXT) leftZoneColor ?: defaultTextColor else defaultTextColor)
        val rightTextColor = if (isRightBg) KustomColorGradeMapper.getContrastingTextColor(rightZoneColor!!) else (if (rightZoneFormat == com.hank.kustom.data.model.ZoneFormat.TEXT) rightZoneColor ?: defaultTextColor else defaultTextColor)
        
        val leftLabelColor = if (isLeftBg) leftTextColor else defaultTextColor
        val rightLabelColor = if (isRightBg) rightTextColor else defaultTextColor

        // Typography
        val unifiedLabelPx = height * (0.165f + ((labelTypography.fontSizeSp - 10f).coerceIn(0f, 10f) * 0.006f))
        val startValuePx = (height * (valueTypography.fontSizeSp / 74f)).coerceAtLeast(20f)

        val leftLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = leftLabelColor }
        KustomTypefaceResolver.applyTypography(leftLabelPaint, labelTypography.font, labelTypography.fontWeight, unifiedLabelPx)
        
        val rightLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = rightLabelColor }
        KustomTypefaceResolver.applyTypography(rightLabelPaint, labelTypography.font, labelTypography.fontWeight, unifiedLabelPx)

        val leftValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = leftTextColor }
        KustomTypefaceResolver.applyTypography(leftValPaint, valueTypography.font, valueTypography.fontWeight, startValuePx)
        
        val rightValPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = rightTextColor }
        KustomTypefaceResolver.applyTypography(rightValPaint, valueTypography.font, valueTypography.fontWeight, startValuePx)

        // Draw Rotated Edge Labels
        val railWidth = height * 0.16f
        val railPadY = height * 0.06f
        drawRotatedEdgeLabel(canvas, leftLabel, railWidth / 2f, height / 2f, height - (railPadY * 2), leftLabelPaint, -90f)
        drawRotatedEdgeLabel(canvas, rightLabel, width - (railWidth / 2f), height / 2f, height - (railPadY * 2), rightLabelPaint, 90f)

        // Bottom Bars
        val isLeftBar = leftZoneFormat == com.hank.kustom.data.model.ZoneFormat.BAR && leftZoneColor != null
        val isRightBar = rightZoneFormat == com.hank.kustom.data.model.ZoneFormat.BAR && rightZoneColor != null
        
        val barH = height * 0.08f
        val barBottom = height * 0.95f
        val barTop = barBottom - barH
        val barPadX = width * 0.04f

        if (isLeftBar) {
            val barRect = RectF(barPadX, barTop, halfW - barPadX, barBottom)
            if (globalZonePalette == com.hank.kustom.data.model.ZonePalette.NEON_GLASS) {
                KustomFieldRenderer.drawNeonGlassRoundRect(canvas, barRect, barH / 2f, leftZoneColor!!, isActiveHighlight = true, palette = globalZonePalette)
            } else {
                canvas.drawRoundRect(barRect, barH / 2f, barH / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = leftZoneColor!!; style = Paint.Style.FILL })
            }
        }
        
        if (isRightBar) {
            val barRect = RectF(halfW + barPadX, barTop, width - barPadX, barBottom)
            if (globalZonePalette == com.hank.kustom.data.model.ZonePalette.NEON_GLASS) {
                KustomFieldRenderer.drawNeonGlassRoundRect(canvas, barRect, barH / 2f, rightZoneColor!!, isActiveHighlight = true, palette = globalZonePalette)
            } else {
                canvas.drawRoundRect(barRect, barH / 2f, barH / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rightZoneColor!!; style = Paint.Style.FILL })
            }
        }

        // Draw Values
        val vTop = height * 0.15f
        val vBottomLeft = if (isLeftBar) barTop - (height * 0.04f) else height * 0.85f
        val vBottomRight = if (isRightBar) barTop - (height * 0.04f) else height * 0.85f
        
        val centerPad = width * 0.02f
        val lBoxL = railWidth + centerPad
        val lBoxR = halfW - centerPad
        
        val rBoxL = halfW + centerPad
        val rBoxR = width - railWidth - centerPad

        drawTextInBox(canvas, leftValueText, lBoxL, vTop, lBoxR, vBottomLeft, leftValPaint, Paint.Align.CENTER, VerticalAlign.CENTER, startValuePx, 14f)
        drawTextInBox(canvas, rightValueText, rBoxL, vTop, rBoxR, vBottomRight, rightValPaint, Paint.Align.CENTER, VerticalAlign.CENTER, startValuePx, 14f)

        canvas.restore()
    }
}
