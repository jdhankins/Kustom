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
import com.hank.kustom.data.model.ZoneFormat
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.models.ViewConfig

/**
 * Offscreen bitmap renderer that draws label and value text onto an ARGB_8888 canvas.
 * Supports standard single-metric layout and multi-metric Power Combo Block layouts.
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

            // Draw Background (Rounded cell fill sitting cleanly inside Karoo cell border)
            if (boundaries) {
                val insetRect = RectF(1.5f, 1.5f, width.toFloat() - 1.5f, height.toFloat() - 1.5f)
                val cornerRadius = height * 0.12f
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (backgroundColor != 0x00000000) backgroundColor else defaultBgColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(insetRect, cornerRadius, cornerRadius, bgPaint)
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
     * Renders multi-metric Power Combo Block with clean un-cluttered corner labels, small smoothing badge, and zone bar.
     */
    fun renderComboBlock(
        context: Context,
        config: ViewConfig,
        label: String,
        primaryValueText: String,
        smoothingLabel: String = "",
        leftLabel: String,
        leftValueText: String,
        rightLabel: String,
        rightValueText: String,
        powerValueWatts: Double?,
        ftpWatts: Double = 250.0,
        zoneCount: Int = 7,
        zoneFormat: ZoneFormat = ZoneFormat.BAR,
        mappedColor: Int? = null,
        leftZoneFormat: ZoneFormat = ZoneFormat.NONE,
        leftMappedColor: Int? = null,
        rightZoneFormat: ZoneFormat = ZoneFormat.NONE,
        rightMappedColor: Int? = null,
        labelTypography: TypographyConfig,
        valueTypography: TypographyConfig,
        isDarkTheme: Boolean = false,
        globalColors: GlobalColorConfig = GlobalColorConfig(),
    ): Bitmap {
        return runCatching {
            val width = config.viewSize.first.coerceAtLeast(10)
            val height = config.viewSize.second.coerceAtLeast(10)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val isSensorStatusText = primaryValueText == "Searching..." || primaryValueText == "No Sensor" || primaryValueText == "Searching"
            val sensorStatusColor = if (isDarkTheme) 0xFFFF5252.toInt() else 0xFFD32F2F.toInt()

            val defaultBgColor = if (isDarkTheme) globalColors.getDarkColorInt() else globalColors.getLightColorInt()
            val defaultTextColor = KustomColorGradeMapper.getContrastingTextColor(defaultBgColor)

            val activeZoneColor = mappedColor ?: KustomColorGradeMapper.getPowerZoneColor(powerValueWatts ?: 214.0, null, ColorMode.BACKGROUND_COLOR)

            // Calculate background color (stay default background when searching/no sensor)
            val backgroundColor: Int = if (zoneFormat == ZoneFormat.BACKGROUND && activeZoneColor != null && !isSensorStatusText) {
                activeZoneColor
            } else {
                defaultBgColor
            }

            // Primary center value color: red when searching/no sensor, colored in TEXT mode
            val primaryValueColor: Int = when {
                isSensorStatusText -> sensorStatusColor
                zoneFormat == ZoneFormat.TEXT && activeZoneColor != null -> activeZoneColor
                zoneFormat == ZoneFormat.BACKGROUND && activeZoneColor != null -> KustomColorGradeMapper.getContrastingTextColor(activeZoneColor)
                else -> defaultTextColor
            }

            // Corner default values color
            val defaultCornerValueColor: Int = when {
                zoneFormat == ZoneFormat.BACKGROUND && activeZoneColor != null -> KustomColorGradeMapper.getContrastingTextColor(activeZoneColor)
                else -> defaultTextColor
            }

            val mutedTextColor = if (zoneFormat == ZoneFormat.BACKGROUND && activeZoneColor != null) defaultCornerValueColor else defaultTextColor

            // Draw Background (Identical fill logic to standard fields with 1.5px inset)
            if (config.boundariesEnabled) {
                val insetRect = RectF(1.5f, 1.5f, width.toFloat() - 1.5f, height.toFloat() - 1.5f)
                val cornerRadius = height * 0.12f
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = backgroundColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(insetRect, cornerRadius, cornerRadius, bgPaint)
            } else {
                if (backgroundColor != 0x00000000) canvas.drawColor(backgroundColor)
                val darkGreyBorder = if (isDarkTheme) 0xFF475569.toInt() else 0xFF64748B.toInt()
                val bottomLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = darkGreyBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 5.5f
                }
                val lineY = height.toFloat() - 2.75f
                canvas.drawLine(0f, lineY, width.toFloat(), lineY, bottomLinePaint)
            }

            val marginX = width * 0.025f
            val topY = height * 0.06f

            // Abbreviate corner metric labels (e.g. Cadence -> CAD, Torque -> TRQ, Speed -> SPD)
            val cleanLeft = abbreviateCornerLabel(leftLabel)
            val cleanRight = abbreviateCornerLabel(rightLabel)

            val cornerLabelSize = height * 0.18f
            val cornerValueSize = height * 0.32f

            // Calculate exact same label baseline Y as standard fields
            val labelZoneHeight = height * 0.26f
            val centerLabelPx = (height * (labelTypography.fontSizeSp / 68f)).coerceAtLeast(10f)
            val centerLabelPaint = TextPaint().apply {
                color = mutedTextColor
                textAlign = Paint.Align.CENTER
                textSize = centerLabelPx
            }
            KustomTypefaceResolver.applyTypography(centerLabelPaint, labelTypography.font, labelTypography.fontWeight, centerLabelPx)

            val labelMetrics = centerLabelPaint.fontMetrics
            val topPadding = height * 0.04f
            val labelCenterY = (labelZoneHeight / 2f) + topPadding
            val labelY = labelCenterY - ((labelMetrics.ascent + labelMetrics.descent) / 2f)

            // Auto-scale center label if it exceeds center space
            val primaryLabelText = label.uppercase()
            while (centerLabelPaint.textSize > 10f && centerLabelPaint.measureText(primaryLabelText) > width * 0.45f) {
                centerLabelPaint.textSize -= 1f
            }

            // 0. Top-Center Primary Metric Label (e.g. "3S POWER" - exact same vertical baseline Y as all other fields)
            canvas.drawText(primaryLabelText, width / 2f, labelY, centerLabelPaint)

            // 1. Top-Left Corner Metric
            var leftMaxX = marginX
            if (cleanLeft.isNotEmpty()) {
                val leftValColor = when {
                    leftZoneFormat == ZoneFormat.TEXT && leftMappedColor != null -> leftMappedColor
                    leftZoneFormat == ZoneFormat.BACKGROUND && leftMappedColor != null -> KustomColorGradeMapper.getContrastingTextColor(leftMappedColor)
                    else -> defaultCornerValueColor
                }

                val leftLabelColor = if (leftZoneFormat == ZoneFormat.BACKGROUND && leftMappedColor != null) {
                    leftValColor
                } else {
                    mutedTextColor
                }

                val leftCornerLabelPaint = TextPaint().apply {
                    color = leftLabelColor
                    textAlign = Paint.Align.LEFT
                    textSize = cornerLabelSize
                }
                KustomTypefaceResolver.applyTypography(leftCornerLabelPaint, labelTypography.font, 700, cornerLabelSize)

                val leftCornerValuePaint = TextPaint().apply {
                    color = leftValColor
                    textAlign = Paint.Align.LEFT
                    textSize = cornerValueSize
                }
                KustomTypefaceResolver.applyTypography(leftCornerValuePaint, valueTypography.font, valueTypography.fontWeight, cornerValueSize)

                val w1 = leftCornerLabelPaint.measureText(cleanLeft)
                val w2 = leftCornerValuePaint.measureText(leftValueText)
                val lWidth = maxOf(w1, w2)

                // Fill mode: draw compact rounded pill behind corner metric
                if (leftZoneFormat == ZoneFormat.BACKGROUND && leftMappedColor != null) {
                    val lPillRect = RectF(
                        marginX - width * 0.015f,
                        topY + height * 0.02f,
                        marginX + lWidth + width * 0.025f,
                        labelY + height * 0.30f
                    )
                    val lPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = leftMappedColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(lPillRect, 8f, 8f, lPillPaint)
                }

                canvas.drawText(cleanLeft, marginX, labelY, leftCornerLabelPaint)
                canvas.drawText(leftValueText, marginX, labelY + height * 0.28f, leftCornerValuePaint)
                leftMaxX = marginX + lWidth
            }

            // 2. Top-Right Corner Metric
            var rightMinX = width - marginX
            if (cleanRight.isNotEmpty()) {
                val rightValColor = when {
                    rightZoneFormat == ZoneFormat.TEXT && rightMappedColor != null -> rightMappedColor
                    rightZoneFormat == ZoneFormat.BACKGROUND && rightMappedColor != null -> KustomColorGradeMapper.getContrastingTextColor(rightMappedColor)
                    else -> defaultCornerValueColor
                }

                val rightLabelColor = if (rightZoneFormat == ZoneFormat.BACKGROUND && rightMappedColor != null) {
                    rightValColor
                } else {
                    mutedTextColor
                }

                val rightCornerLabelPaint = TextPaint().apply {
                    color = rightLabelColor
                    textAlign = Paint.Align.RIGHT
                    textSize = cornerLabelSize
                }
                KustomTypefaceResolver.applyTypography(rightCornerLabelPaint, labelTypography.font, 700, cornerLabelSize)

                val rightCornerValuePaint = TextPaint().apply {
                    color = rightValColor
                    textAlign = Paint.Align.RIGHT
                    textSize = cornerValueSize
                }
                KustomTypefaceResolver.applyTypography(rightCornerValuePaint, valueTypography.font, valueTypography.fontWeight, cornerValueSize)

                val w1 = rightCornerLabelPaint.measureText(cleanRight)
                val w2 = rightCornerValuePaint.measureText(rightValueText)
                val rWidth = maxOf(w1, w2)

                // Fill mode: draw compact rounded pill behind corner metric
                if (rightZoneFormat == ZoneFormat.BACKGROUND && rightMappedColor != null) {
                    val rPillRect = RectF(
                        width - marginX - rWidth - width * 0.025f,
                        topY + height * 0.02f,
                        width - marginX + width * 0.015f,
                        labelY + height * 0.30f
                    )
                    val rPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rightMappedColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(rPillRect, 8f, 8f, rPillPaint)
                }

                canvas.drawText(cleanRight, width - marginX, labelY, rightCornerLabelPaint)
                canvas.drawText(rightValueText, width - marginX, labelY + height * 0.28f, rightCornerValuePaint)
                rightMinX = width - marginX - rWidth
            }

            // 3. Center Primary Power Metric
            val isBarMode = zoneFormat == ZoneFormat.BAR
            val centerValuePaint = TextPaint().apply {
                color = primaryValueColor
                textAlign = Paint.Align.CENTER
            }

            val primaryY: Float
            if (isBarMode) {
                val rawPrimaryPx = (height * (valueTypography.fontSizeSp / 88f)).coerceAtLeast(18f)
                val basePrimaryPx = if (isSensorStatusText) rawPrimaryPx * 0.52f else rawPrimaryPx
                KustomTypefaceResolver.applyTypography(
                    centerValuePaint,
                    valueTypography.font,
                    if (isSensorStatusText) 700 else valueTypography.fontWeight,
                    basePrimaryPx
                )

                val leftGap = (width / 2f) - leftMaxX
                val rightGap = rightMinX - (width / 2f)
                val baseClearanceRadius = (minOf(leftGap, rightGap) - width * 0.03f).coerceAtLeast(width * 0.12f)
                val maxCenterWidth = baseClearanceRadius * 2f

                primaryY = height * 0.75f

                fun isPrimaryTooLarge(): Boolean {
                    val bounds = Rect()
                    centerValuePaint.getTextBounds(primaryValueText, 0, primaryValueText.length, bounds)
                    val topEdgeY = primaryY + bounds.top
                    return topEdgeY < (topY + height * 0.22f) || centerValuePaint.measureText(primaryValueText) > maxCenterWidth
                }

                while (centerValuePaint.textSize > 10f && isPrimaryTooLarge()) {
                    centerValuePaint.textSize -= 1f
                }
            } else {
                // When zoneFormat != ZoneFormat.BAR (None, Text, Fill): Middle label & value follow EXACT same size, weight & Y position as all standard fields
                val rawPrimaryPx = (height * (valueTypography.fontSizeSp / 86f)).coerceAtLeast(18f)
                val primaryPx = if (isSensorStatusText) rawPrimaryPx * 0.52f else rawPrimaryPx
                KustomTypefaceResolver.applyTypography(
                    centerValuePaint,
                    valueTypography.font,
                    if (isSensorStatusText) 700 else valueTypography.fontWeight,
                    primaryPx
                )

                val valueZoneTop = height * 0.26f
                val valueZoneHeight = height * 0.74f
                val valueMetrics = centerValuePaint.fontMetrics
                val valueCenterY = valueZoneTop + (valueZoneHeight / 2f)
                primaryY = valueCenterY - ((valueMetrics.ascent + valueMetrics.descent) / 2f)

                val maxAllowedWidth = width * 0.92f
                while (centerValuePaint.textSize > 10f && centerValuePaint.measureText(primaryValueText) > maxAllowedWidth) {
                    centerValuePaint.textSize -= 1f
                }
            }

            canvas.drawText(primaryValueText, width / 2f, primaryY, centerValuePaint)

            // 4. Bottom 5-Zone or 7-Zone Gauge Bar (DRAWN ONLY WHEN zoneFormat == ZoneFormat.BAR)
            if (zoneFormat == ZoneFormat.BAR) {
                val barMarginX = width * 0.05f
                val barWidth = width - (barMarginX * 2)
                val barBaseY = height * 0.82f
                val baseBarHeight = height * 0.08f

                val palette = globalColors.globalZonePalette
                val zones = if (zoneCount == 5) palette.get5ZoneColors() else palette.get7ZoneColors()

                val pWatts = powerValueWatts ?: 0.0
                val pctFtp = pWatts / ftpWatts

                val activeZoneIdx = if (zoneCount == 5) {
                    when {
                        pctFtp < 0.60 -> 0
                        pctFtp < 0.70 -> 1
                        pctFtp < 0.80 -> 2
                        pctFtp < 0.90 -> 3
                        else -> 4
                    }
                } else {
                    when {
                        pctFtp < 0.55 -> 0
                        pctFtp < 0.75 -> 1
                        pctFtp < 0.90 -> 2
                        pctFtp < 1.05 -> 3
                        pctFtp < 1.20 -> 4
                        pctFtp < 1.50 -> 5
                        else -> 6
                    }
                }

                val segGap = width * 0.01f
                val totalGaps = (zones.size - 1) * segGap
                val segWidth = (barWidth - totalGaps) / zones.size

                zones.forEachIndexed { idx, zoneColor ->
                    val isActive = idx == activeZoneIdx
                    val segLeft = barMarginX + idx * (segWidth + segGap)
                    val segRight = segLeft + segWidth

                    val currentHeight = if (isActive) baseBarHeight * 1.8f else baseBarHeight
                    val currentTop = barBaseY - (currentHeight - baseBarHeight) / 2f
                    val currentBottom = currentTop + currentHeight

                    val rect = RectF(segLeft, currentTop, segRight, currentBottom)
                    val cornerRadius = baseBarHeight * 0.35f

                    val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = zoneColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, segPaint)

                    // Crisp high-contrast border outline over active zone segment
                    if (isActive) {
                        val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF1E2225.toInt()
                            style = Paint.Style.STROKE
                            strokeWidth = 2.5f
                        }
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, activeBorderPaint)
                    }
                }
            }

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

    private fun abbreviateCornerLabel(rawLabel: String): String {
        val trimmed = rawLabel.replace("power", "", ignoreCase = true).trim().uppercase()
        return when {
            trimmed.contains("CADENCE") -> trimmed.replace("CADENCE", "CAD")
            trimmed.contains("TORQUE") -> trimmed.replace("TORQUE", "TRQ")
            trimmed.contains("HEART RATE") -> trimmed.replace("HEART RATE", "HR")
            trimmed.contains("SPEED") -> trimmed.replace("SPEED", "SPD")
            trimmed.contains("DISTANCE") -> trimmed.replace("DISTANCE", "DIST")
            trimmed.contains("ASCENT") -> trimmed.replace("ASCENT", "ASC")
            trimmed.contains("DESCENT") -> trimmed.replace("DESCENT", "DSC")
            else -> trimmed
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
}
