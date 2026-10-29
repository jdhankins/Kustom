package com.hank.kustom.domain

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import com.hank.kustom.data.model.TypographyConfig

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
 * Manages 1:1 Canvas geometry, baselines, font sizing, auto-scaling, and drawing.
 */
object FullWidthLayoutEngine {

    /**
     * Draws the standardized 3-slot header (Left, Center Hero, Right) on the provided Canvas.
     *
     * @return RectF bounding box of the left slot pill if filled, or null.
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
        val marginX = width * 0.025f
        val topY = height * 0.06f

        // Calculate standard center/top label baseline Y
        val cornerLabelSize = height * 0.16f
        val cornerValueSize = height * 0.26f

        val labelZoneHeight = height * 0.26f
        val centerLabelPx = (height * (labelTypography.fontSizeSp / 68f)).coerceAtLeast(10f)
        val tempPaint = TextPaint().apply {
            textSize = centerLabelPx
        }
        KustomTypefaceResolver.applyTypography(tempPaint, labelTypography.font, labelTypography.fontWeight, centerLabelPx)

        val labelMetrics = tempPaint.fontMetrics
        val topPadding = height * 0.04f
        val labelCenterY = (labelZoneHeight / 2f) + topPadding
        val labelY = labelCenterY - ((labelMetrics.ascent + labelMetrics.descent) / 2f)

        // 1. Draw Left Slot (if present)
        if (leftSlot is HeaderSlot.MetricStack && leftSlot.label.isNotEmpty()) {
            val leftValColor = when {
                leftSlot.isZoneFilled && leftSlot.zoneColor != null -> KustomColorGradeMapper.getContrastingTextColor(leftSlot.zoneColor)
                else -> defaultTextColor
            }
            val leftLabelColor = if (leftSlot.isZoneFilled && leftSlot.zoneColor != null) leftValColor else defaultTextColor

            val maxCornerW = width * 0.30f
            val labelPx = cornerLabelSize // NEVER SCALED!
            var curValuePx = cornerValueSize

            val targetLabelPaint = TextPaint().apply {
                color = leftLabelColor
                textAlign = Paint.Align.LEFT
                textSize = labelPx
            }
            KustomTypefaceResolver.applyTypography(targetLabelPaint, labelTypography.font, 700, labelPx)

            val targetValuePaint = TextPaint().apply {
                color = leftValColor
                textAlign = Paint.Align.LEFT
                textSize = curValuePx
            }
            KustomTypefaceResolver.applyTypography(targetValuePaint, valueTypography.font, valueTypography.fontWeight, curValuePx)

            // Scale ONLY the value text size if value width > maxCornerW
            while (targetValuePaint.measureText(leftSlot.value) > maxCornerW && curValuePx > 10f) {
                curValuePx -= 0.5f
                targetValuePaint.textSize = curValuePx
            }

            val lWidth = maxOf(targetLabelPaint.measureText(leftSlot.label), targetValuePaint.measureText(leftSlot.value))

            // Draw Zone Background Pill behind left slot if enabled
            if (leftSlot.isZoneFilled && leftSlot.zoneColor != null) {
                val lPillRect = RectF(
                    marginX - width * 0.015f,
                    topY + height * 0.02f,
                    marginX + lWidth + width * 0.025f,
                    labelY + height * 0.30f
                )
                val lPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = leftSlot.zoneColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(lPillRect, 8f, 8f, lPillPaint)
            }

            canvas.drawText(leftSlot.label, marginX, labelY, targetLabelPaint)
            canvas.drawText(leftSlot.value, marginX, labelY + height * 0.28f, targetValuePaint)
        }

        // 2. Draw Right Slot (if present)
        if (rightSlot != null) {
            when (rightSlot) {
                is HeaderSlot.StatusPill -> {
                    val pillHeight = height * 0.22f
                    val pillTop = topY + (height * 0.02f)
                    val pillBottom = pillTop + pillHeight
                    val pillRight = width - marginX

                    val badgeTextPx = (height * 0.11f).coerceIn(11f, 14f)
                    val badgeTextPaint = TextPaint().apply {
                        color = rightSlot.textColor
                        textSize = badgeTextPx
                        textAlign = Paint.Align.CENTER
                    }
                    KustomTypefaceResolver.applyTypography(badgeTextPaint, labelTypography.font, 900, badgeTextPx)

                    val badgeTextW = badgeTextPaint.measureText(rightSlot.text)
                    val pillWidth = badgeTextW + (width * 0.05f)
                    val pillLeftX = pillRight - pillWidth

                    val badgePillRect = RectF(pillLeftX, pillTop, pillRight, pillBottom)
                    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rightSlot.backgroundColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(badgePillRect, pillHeight / 2f, pillHeight / 2f, badgePaint)

                    val badgeTextY = badgePillRect.centerY() - ((badgeTextPaint.fontMetrics.ascent + badgeTextPaint.fontMetrics.descent) / 2f)
                    canvas.drawText(rightSlot.text, badgePillRect.centerX(), badgeTextY, badgeTextPaint)
                }

                is HeaderSlot.MetricStack -> {
                    if (rightSlot.label.isNotEmpty()) {
                        val rightValColor = when {
                            rightSlot.isZoneFilled && rightSlot.zoneColor != null -> KustomColorGradeMapper.getContrastingTextColor(rightSlot.zoneColor)
                            else -> defaultTextColor
                        }
                        val rightLabelColor = if (rightSlot.isZoneFilled && rightSlot.zoneColor != null) rightValColor else defaultTextColor

                        val maxCornerW = width * 0.30f
                        val labelPx = cornerLabelSize // NEVER SCALED!
                        var curValuePx = cornerValueSize

                        val rLabelPaint = TextPaint().apply {
                            color = rightLabelColor
                            textAlign = Paint.Align.RIGHT
                            textSize = labelPx
                        }
                        KustomTypefaceResolver.applyTypography(rLabelPaint, labelTypography.font, 700, labelPx)

                        val rValuePaint = TextPaint().apply {
                            color = rightValColor
                            textAlign = Paint.Align.RIGHT
                            textSize = curValuePx
                        }
                        KustomTypefaceResolver.applyTypography(rValuePaint, valueTypography.font, valueTypography.fontWeight, curValuePx)

                        // Scale ONLY the value text size if value width > maxCornerW
                        while (rValuePaint.measureText(rightSlot.value) > maxCornerW && curValuePx > 10f) {
                            curValuePx -= 0.5f
                            rValuePaint.textSize = curValuePx
                        }

                        val rWidth = maxOf(rLabelPaint.measureText(rightSlot.label), rValuePaint.measureText(rightSlot.value))
                        val rightX = width - marginX

                        if (rightSlot.isZoneFilled && rightSlot.zoneColor != null) {
                            val rPillRect = RectF(
                                rightX - rWidth - width * 0.025f,
                                topY + height * 0.02f,
                                rightX + width * 0.015f,
                                labelY + height * 0.30f
                            )
                            val rPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = rightSlot.zoneColor
                                style = Paint.Style.FILL
                            }
                            canvas.drawRoundRect(rPillRect, 8f, 8f, rPillPaint)
                        }

                        canvas.drawText(rightSlot.label, rightX, labelY, rLabelPaint)
                        canvas.drawText(rightSlot.value, rightX, labelY + height * 0.28f, rValuePaint)
                    }
                }

                else -> {}
            }
        }

        // 3. Draw Center Hero Slot (if present)
        if (centerHero != null) {
            val headerLabelText = centerHero.label
            val hasHeaderLabel = !headerLabelText.isNullOrEmpty()
            val hasBottomBar = centerHero.hasBottomBar

            // Draw optional label directly above hero digits on labelY baseline
            if (hasHeaderLabel) {
                val heroLabelPaint = TextPaint().apply {
                    color = mutedTextColor
                    textAlign = Paint.Align.CENTER
                    textSize = centerLabelPx
                }
                KustomTypefaceResolver.applyTypography(heroLabelPaint, labelTypography.font, labelTypography.fontWeight, centerLabelPx)

                val maxLabelW = width * 0.44f
                while (heroLabelPaint.textSize > 10f && heroLabelPaint.measureText(headerLabelText) > maxLabelW) {
                    heroLabelPaint.textSize -= 1f
                }

                canvas.drawText(headerLabelText, width / 2f, labelY, heroLabelPaint)
            }

            // Calculate precise vertical bounds for hero digits based on presence of top header label & bottom bar
            val topLimit = if (hasHeaderLabel) labelY + (height * 0.12f) else topY + (height * 0.04f)
            val bottomLimit = if (hasBottomBar) height * 0.74f else height * 0.94f
            val maxAllowedHeroHeight = (bottomLimit - topLimit).coerceAtLeast(height * 0.25f)
            val heroCenterY = topLimit + (maxAllowedHeroHeight / 2f)

            val baseHeroPx = (height * (valueTypography.fontSizeSp / 86f)).coerceAtLeast(20f)
            val heroPaint = TextPaint().apply {
                color = defaultTextColor
                textSize = baseHeroPx
                textAlign = Paint.Align.CENTER
            }
            KustomTypefaceResolver.applyTypography(heroPaint, valueTypography.font, valueTypography.fontWeight, baseHeroPx)

            val maxAllowedHeroWidth = width * 0.44f
            val maxHeroHeightPx = maxAllowedHeroHeight * 0.95f

            var heroMetrics = heroPaint.fontMetrics
            var heroTextHeight = heroMetrics.descent - heroMetrics.ascent

            // Auto-scale hero font size so it takes maximum available area without overlapping top label or bottom bar
            while (heroPaint.textSize > 14f && (heroPaint.measureText(centerHero.value) > maxAllowedHeroWidth || heroTextHeight > maxHeroHeightPx)) {
                heroPaint.textSize -= 1f
                heroMetrics = heroPaint.fontMetrics
                heroTextHeight = heroMetrics.descent - heroMetrics.ascent
            }

            val finalHeroY = heroCenterY - ((heroMetrics.ascent + heroMetrics.descent) / 2f)
            canvas.drawText(centerHero.value, width / 2f, finalHeroY, heroPaint)
        }
    }
}
