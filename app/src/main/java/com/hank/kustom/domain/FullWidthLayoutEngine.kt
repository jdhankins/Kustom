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
     * Fits and positions dynamic values strictly inside [boxLeft, boxTop, boxRight, boxBottom]
     * using true visible glyph ink bounds (Paint.getTextBounds).
     */
    private fun drawTextInBox(
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

        val initialStroke = paint.strokeWidth
        val strokeRatio = if (maxStartTextSizePx > 0f && initialStroke > 0f) (initialStroke / maxStartTextSizePx).coerceAtMost(0.028f) else 0f

        var size = maxStartTextSizePx.coerceAtLeast(minTextSizePx)
        paint.textSize = size
        if (strokeRatio > 0f) {
            paint.strokeWidth = size * strokeRatio
        }

        val bounds = Rect()
        paint.getTextBounds(renderText, 0, renderText.length, bounds)

        while (size > minTextSizePx && (bounds.width() > boxW || bounds.height() > boxH)) {
            size -= 1f
            paint.textSize = size
            if (strokeRatio > 0f) {
                paint.strokeWidth = size * strokeRatio
            }
            paint.getTextBounds(renderText, 0, renderText.length, bounds)
        }

        val drawX = when (align) {
            Paint.Align.LEFT -> boxLeft - bounds.left + (paint.strokeWidth / 2f)
            Paint.Align.RIGHT -> boxRight - bounds.right - (paint.strokeWidth / 2f)
            else -> ((boxLeft + boxRight) / 2f) - bounds.exactCenterX()
        }

        val drawY = when (verticalAlign) {
            VerticalAlign.TOP -> boxTop - bounds.top + (paint.strokeWidth / 2f)
            VerticalAlign.CENTER -> ((boxTop + boxBottom) / 2f) - bounds.exactCenterY()
        }

        canvas.drawText(renderText, drawX, drawY, paint)
        return bounds
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
    ) {
        val hasLeft = leftSlot is HeaderSlot.MetricStack && leftSlot.label.isNotEmpty()
        val hasRight = rightSlot != null
        val sideSlotsPresent = hasLeft || hasRight

        // Strict Horizontal Column Coordinates (Sum = 100%)
        val leftColLeft = width * 0.035f
        val leftColRight = width * 0.265f

        val rightColLeft = width * 0.735f
        val rightColRight = width * 0.965f

        val heroColLeft = if (sideSlotsPresent) width * 0.290f else width * 0.080f
        val heroColRight = if (sideSlotsPresent) width * 0.710f else width * 0.920f

        // Top Cap-Line for All Labels across the card (7% height)
        val labelTopY = height * 0.07f

        // Value Box for Left/Right Corner Values (25%..55% height)
        val valueBoxTop = height * 0.25f
        val valueBoxBottom = height * 0.55f

        // Uniform fixed label font size across ALL labels on the card (11.5%..15.0% height)
        val fixedLabelPx = height * (0.115f + ((labelTypography.fontSizeSp - 10f).coerceIn(0f, 10f) * 0.0035f))
        val startValuePx = (height * (valueTypography.fontSizeSp / 86f)).coerceAtLeast(18f)

        // 1. Draw Left Slot
        if (hasLeft && leftSlot is HeaderSlot.MetricStack) {
            val leftValColor = when {
                leftSlot.isZoneFilled && leftSlot.zoneColor != null -> KustomColorGradeMapper.getContrastingTextColor(leftSlot.zoneColor)
                else -> defaultTextColor
            }
            val leftLabelColor = if (leftSlot.isZoneFilled && leftSlot.zoneColor != null) leftValColor else defaultTextColor

            val labelPaint = TextPaint().apply {
                color = leftLabelColor
            }
            KustomTypefaceResolver.applyTypography(labelPaint, labelTypography.font, 700, fixedLabelPx)

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
                fixedTextSizePx = fixedLabelPx
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
                val lPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = leftSlot.zoneColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(lPillRect, 8f, 8f, lPillPaint)
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
                    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rightSlot.backgroundColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(badgePillRect, pillH / 2f, pillH / 2f, badgePaint)

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
                        val rightValColor = if (isZoneFilled) {
                            0xFFFFFFFF.toInt()
                        } else {
                            defaultTextColor
                        }
                        val rightLabelColor = if (isZoneFilled) 0xFFFFFFFF.toInt() else defaultTextColor

                        if (isZoneFilled) {
                            val rPillRect = RectF(
                                rightColLeft - width * 0.010f,
                                height * 0.04f,
                                rightColRight + width * 0.010f,
                                height * 0.58f
                            )
                            val rPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = rightSlot.zoneColor
                                style = Paint.Style.FILL
                            }
                            canvas.drawRoundRect(rPillRect, 12f, 12f, rPillPaint)
                        }

                        val labelPaint = TextPaint().apply {
                            color = rightLabelColor
                        }
                        KustomTypefaceResolver.applyTypography(labelPaint, labelTypography.font, 700, fixedLabelPx)

                        val valPaint = TextPaint().apply {
                            color = rightValColor
                        }
                        KustomTypefaceResolver.applyTypography(valPaint, valueTypography.font, valueTypography.fontWeight, startValuePx)

                        val align = if (isZoneFilled) Paint.Align.CENTER else Paint.Align.RIGHT
                        val labelX = if (isZoneFilled) (rightColLeft + rightColRight) / 2f else rightColRight

                        drawFixedLabel(
                            canvas = canvas,
                            text = rightSlot.label,
                            anchorX = labelX,
                            topY = labelTopY,
                            paint = labelPaint,
                            align = align,
                            fixedTextSizePx = fixedLabelPx
                        )

                        val boxL = if (isZoneFilled) rightColLeft + width * 0.010f else rightColLeft
                        val boxR = if (isZoneFilled) rightColRight - width * 0.010f else rightColRight

                        drawTextInBox(
                            canvas = canvas,
                            text = rightSlot.value,
                            boxLeft = boxL,
                            boxTop = valueBoxTop,
                            boxRight = boxR,
                            boxBottom = valueBoxBottom,
                            paint = valPaint,
                            align = align,
                            verticalAlign = VerticalAlign.TOP,
                            maxStartTextSizePx = startValuePx,
                            minTextSizePx = 10f
                        )
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

            // Draw header label if present at fixedLabelPx with ZERO auto-scaling
            if (hasHeaderLabel) {
                val heroLabelPaint = TextPaint().apply {
                    color = mutedTextColor
                }
                KustomTypefaceResolver.applyTypography(heroLabelPaint, labelTypography.font, labelTypography.fontWeight, fixedLabelPx)

                drawFixedLabel(
                    canvas = canvas,
                    text = headerLabelText,
                    anchorX = width / 2f,
                    topY = labelTopY,
                    paint = heroLabelPaint,
                    align = Paint.Align.CENTER,
                    fixedTextSizePx = fixedLabelPx
                )
            }

            // Calculate exact bounding box for Center Hero Value
            val heroValueBoxTop = if (hasHeaderLabel) height * 0.26f else height * 0.10f
            val heroValueBoxBottom = if (hasBottomBar) height * 0.71f else height * 0.89f

            val heroValPaint = TextPaint().apply {
                color = defaultTextColor
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
}
