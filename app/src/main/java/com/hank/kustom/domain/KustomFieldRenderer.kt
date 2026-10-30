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
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.ViewConfig
import java.util.Locale

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

            val activeZoneColor = mappedColor ?: if (powerValueWatts != null) {
                KustomColorGradeMapper.getPowerZoneColor(
                    watts = powerValueWatts,
                    userProfile = null,
                    colorMode = ColorMode.BACKGROUND_COLOR,
                    isDarkTheme = isDarkTheme,
                    palette = globalColors.globalZonePalette,
                )
            } else null

            val isBgZoneFilled = zoneFormat == ZoneFormat.BACKGROUND && activeZoneColor != null && !isSensorStatusText

            // Calculate background color (stay default background when searching/no sensor)
            val backgroundColor: Int = if (isBgZoneFilled) {
                activeZoneColor!!
            } else {
                defaultBgColor
            }

            // Corner default values color
            val defaultCornerValueColor: Int = if (isBgZoneFilled) {
                KustomColorGradeMapper.getContrastingTextColor(activeZoneColor!!)
            } else {
                defaultTextColor
            }

            val mutedTextColor = if (isBgZoneFilled) defaultCornerValueColor else defaultTextColor

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

            val leftSlot = if (cleanLeft.isNotEmpty()) {
                HeaderSlot.MetricStack(
                    label = cleanLeft,
                    value = leftValueText,
                    isZoneFilled = leftZoneFormat == ZoneFormat.BACKGROUND && leftMappedColor != null,
                    zoneColor = leftMappedColor,
                )
            } else null

            val rightSlot = if (cleanRight.isNotEmpty()) {
                HeaderSlot.MetricStack(
                    label = cleanRight,
                    value = rightValueText,
                    isZoneFilled = rightZoneFormat == ZoneFormat.BACKGROUND && rightMappedColor != null,
                    zoneColor = rightMappedColor,
                )
            } else null

            val isBarMode = zoneFormat == ZoneFormat.BAR
            val centerHero = HeaderSlot.HeroValue(
                value = primaryValueText,
                label = label.uppercase(),
                hasBottomBar = isBarMode,
            )

            // Delegate header slot layout and drawing to global FullWidthLayoutEngine
            FullWidthLayoutEngine.drawHeaderSlots(
                canvas = canvas,
                leftSlot = leftSlot,
                centerHero = centerHero,
                rightSlot = rightSlot,
                width = width.toFloat(),
                height = height.toFloat(),
                defaultTextColor = defaultCornerValueColor,
                mutedTextColor = mutedTextColor,
                labelTypography = labelTypography,
                valueTypography = valueTypography,
                isDarkTheme = isDarkTheme,
            )

            // Bottom 5-Zone or 7-Zone Gauge Bar (DRAWN ONLY WHEN zoneFormat == ZoneFormat.BAR)
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
     * Offscreen ARGB_8888 canvas renderer for Workout Profile Graph HUD with target zone blocks,
     * current-time divider line, and live Power (White), Heart Rate (Red), and Cadence (Cyan) trace lines.
     */
    fun renderWorkoutGraph(
        context: Context,
        config: ViewConfig,
        parsedWorkout: com.hank.kustom.data.model.ParsedWorkout? = null,
        currentElapsedRatio: Float = 0.40f,
        powerHistory: List<Float> = emptyList(),
        hrHistory: List<Float> = emptyList(),
        cadenceHistory: List<Float> = emptyList(),
        showPowerTrace: Boolean = true,
        showHrTrace: Boolean = true,
        showCadenceTrace: Boolean = true,
        isDarkTheme: Boolean = false,
        globalColors: GlobalColorConfig = GlobalColorConfig(),
    ): Bitmap {
        return runCatching {
            val width = config.viewSize.first.coerceAtLeast(10)
            val height = config.viewSize.second.coerceAtLeast(10)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val defaultBgColor = if (isDarkTheme) globalColors.getDarkColorInt() else globalColors.getLightColorInt()

            // Draw Background Frame
            if (config.boundariesEnabled) {
                val insetRect = RectF(1.5f, 1.5f, width.toFloat() - 1.5f, height.toFloat() - 1.5f)
                val cornerRadius = height * 0.12f
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = defaultBgColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(insetRect, cornerRadius, cornerRadius, bgPaint)
            } else {
                if (defaultBgColor != 0x00000000) canvas.drawColor(defaultBgColor)
            }

            // Define Graph Area (insets keep cursor pin comfortably inside top/bottom rounded bounds)
            val graphTop = height * 0.18f
            val graphBottom = height * 0.90f
            val graphHeight = graphBottom - graphTop
            val marginX = width * 0.035f
            val graphWidth = width - (marginX * 2)

            // Current-time divider line position
            val activeRatio = currentElapsedRatio.coerceIn(0.0f, 1.0f)
            val dividerX = marginX + (graphWidth * activeRatio)

            // Active workout model or default sample fallback
            val activeWorkout = parsedWorkout ?: WorkoutFileParser.getDefaultSampleWorkout(250.0, globalColors.globalZonePalette)
            val totalSec = activeWorkout.totalDurationSeconds.coerceAtLeast(1)

            var currentBarX = marginX
            val cornerRadii = floatArrayOf(6f, 6f, 6f, 6f, 0f, 0f, 0f, 0f) // Top corners rounded 6px, bottom corners 0px flat

            activeWorkout.steps.forEach { step ->
                val durRatio = step.durationSeconds.toFloat() / totalSec
                val barW = graphWidth * durRatio
                val barH = graphHeight * step.heightRatio
                val barTop = graphBottom - barH
                val barRect = RectF(currentBarX, barTop, currentBarX + barW, graphBottom)

                val isPast = activeRatio > 0.01f && (currentBarX + barW) <= dividerX

                val barPath = android.graphics.Path().apply {
                    addRoundRect(barRect, cornerRadii, android.graphics.Path.Direction.CW)
                }

                // 100% solid fill for all interval blocks with flat bottom
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = step.zoneColorInt
                    style = Paint.Style.FILL
                    if (isPast) alpha = 180
                }
                canvas.drawPath(barPath, barPaint)

                currentBarX += barW
            }

            // Draw Vertical Current-Time Divider Line (stops flush at graphBottom)
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF1E2225.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
            }
            canvas.drawLine(dividerX, graphTop, dividerX, graphBottom, dividerPaint)

            // Draw Top Progress Cursor Pin Dot marking exact current position
            val cursorCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF1E2225.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(dividerX, graphTop - 5.0f, 5.0f, cursorCirclePaint)

            // Draw Live Telemetry Trace Lines with subtle high-contrast drop-shadows
            val traceShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTheme) 0xAA000000.toInt() else 0x55000000.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 4.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            // 1. Power Trace Line (White with black border in Dark Mode; Black with white border in Light Mode)
            if (showPowerTrace) {
                val steps = if (powerHistory.isNotEmpty()) powerHistory.size else 30
                val stepW = (dividerX - marginX) / (steps - 1).coerceAtLeast(1)
                val points = (0 until steps).map { idx ->
                    val px = marginX + (idx * stepW)
                    val rawVal = if (powerHistory.isNotEmpty()) {
                        (powerHistory[idx] / 350f).coerceIn(0.1f, 1.0f)
                    } else {
                        (0.35f + (idx * 0.015f) + (Math.sin(idx * 0.4).toFloat() * 0.06f)).coerceIn(0.1f, 1.0f)
                    }
                    val py = graphBottom - (graphHeight * rawVal)
                    Pair(px, py)
                }

                val powerPath = buildSmoothTracePath(points)
                val powerBorderColor = if (isDarkTheme) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                val powerLineColor = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF1E2225.toInt()

                val powerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = powerBorderColor
                    style = Paint.Style.STROKE
                    strokeWidth = 5.0f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                val powerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = powerLineColor
                    style = Paint.Style.STROKE
                    strokeWidth = 3.0f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                canvas.drawPath(powerPath, powerBorderPaint) // High contrast outer border
                canvas.drawPath(powerPath, powerPaint)       // Main line
            }

            // 2. Heart Rate Trace Line (Red with subtle dark under-stroke)
            if (showHrTrace) {
                val steps = if (hrHistory.isNotEmpty()) hrHistory.size else 30
                val stepW = (dividerX - marginX) / (steps - 1).coerceAtLeast(1)
                val points = (0 until steps).map { idx ->
                    val px = marginX + (idx * stepW)
                    val rawVal = if (hrHistory.isNotEmpty()) {
                        (hrHistory[idx] / 190f).coerceIn(0.1f, 1.0f)
                    } else {
                        (0.30f + (idx * 0.018f) + (Math.sin(idx * 0.2).toFloat() * 0.04f)).coerceIn(0.1f, 1.0f)
                    }
                    val py = graphBottom - (graphHeight * rawVal)
                    Pair(px, py)
                }

                val hrPath = buildSmoothTracePath(points)
                val hrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFF5252.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 2.8f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(hrPath, traceShadowPaint)
                canvas.drawPath(hrPath, hrPaint)
            }

            // 3. Cadence Trace Line (Cyan with subtle dark under-stroke)
            if (showCadenceTrace) {
                val steps = if (cadenceHistory.isNotEmpty()) cadenceHistory.size else 30
                val stepW = (dividerX - marginX) / (steps - 1).coerceAtLeast(1)
                val points = (0 until steps).map { idx ->
                    val px = marginX + (idx * stepW)
                    val rawVal = if (cadenceHistory.isNotEmpty()) {
                        (cadenceHistory[idx] / 120f).coerceIn(0.1f, 1.0f)
                    } else {
                        (0.72f + (Math.sin(idx * 0.6).toFloat() * 0.08f)).coerceIn(0.1f, 1.0f)
                    }
                    val py = graphBottom - (graphHeight * rawVal)
                    Pair(px, py)
                }

                val cadencePath = buildSmoothTracePath(points)
                val cadencePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF00E5FF.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 2.8f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(cadencePath, traceShadowPaint) // High-contrast under-stroke
                canvas.drawPath(cadencePath, cadencePaint)      // Main line
            }

            bitmap
        }.getOrElse { throwable ->
            KustomLogger.e("Error rendering workout graph bitmap", throwable)
            createFallbackBitmap(
                config.viewSize.first.coerceAtLeast(10),
                config.viewSize.second.coerceAtLeast(10),
            )
        }
    }

    /**
     * Offscreen ARGB_8888 canvas renderer for Target Power Corridor field.
     * Displays target power range label, status pill badge (ON TARGET, LOW -XW, HIGH +XW),
     * hero power digits, and horizontal corridor track with state-colored cursor needle.
     */
    fun renderTargetPowerCorridor(
        context: Context,
        config: ViewConfig,
        currentPower: Float?,
        minTarget: Float?,
        maxTarget: Float?,
        targetWatts: Float?,
        labelTypography: TypographyConfig,
        valueTypography: TypographyConfig,
        isDarkTheme: Boolean = false,
        globalColors: GlobalColorConfig = GlobalColorConfig(),
        isMock: Boolean = false,
    ): Bitmap {
        return runCatching {
            val width = config.viewSize.first.coerceAtLeast(10)
            val height = config.viewSize.second.coerceAtLeast(10)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val defaultBgColor = if (isDarkTheme) globalColors.getDarkColorInt() else globalColors.getLightColorInt()
            val defaultTextColor = KustomColorGradeMapper.getContrastingTextColor(defaultBgColor)

            // Draw Background Frame
            if (config.boundariesEnabled) {
                val insetRect = RectF(1.5f, 1.5f, width.toFloat() - 1.5f, height.toFloat() - 1.5f)
                val cornerRadius = height * 0.12f
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = defaultBgColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(insetRect, cornerRadius, cornerRadius, bgPaint)
            } else {
                if (defaultBgColor != 0x00000000) canvas.drawColor(defaultBgColor)
            }

            val barLeft = width * 0.08f
            val barRight = width * 0.92f
            val barWidth = barRight - barLeft

            val minT = minTarget ?: targetWatts
            val maxT = maxTarget ?: targetWatts

            val cPower = currentPower ?: (if (isMock) 142f else 0f)
            val lowBound = minT ?: (targetWatts ?: 130f)
            val highBound = maxT ?: (targetWatts ?: 160f)

            val leftSlot = HeaderSlot.MetricStack(
                label = "TARGET",
                value = when {
                    minT != null && maxT != null && minT != maxT -> "${minT.toInt()}-${maxT.toInt()}"
                    targetWatts != null -> "${targetWatts.toInt()}W"
                    else -> "--"
                },
            )

            // Full-Height 2-Line Zone-Filled Corner Block for Right Slot (BELOW/ON/ABOVE TARGET)
            val rightSlot = when {
                currentPower == null && !isMock -> HeaderSlot.MetricStack(
                    label = "NO",
                    value = "SENSOR",
                    isZoneFilled = true,
                    zoneColor = android.graphics.Color.parseColor("#475569")
                )
                cPower < lowBound -> HeaderSlot.MetricStack(
                    label = "BELOW",
                    value = "TARGET",
                    isZoneFilled = true,
                    zoneColor = android.graphics.Color.parseColor("#2563EB") // Matte Blue
                )
                cPower > highBound -> HeaderSlot.MetricStack(
                    label = "ABOVE",
                    value = "TARGET",
                    isZoneFilled = true,
                    zoneColor = android.graphics.Color.parseColor("#C62828") // Matte Crimson Red
                )
                else -> HeaderSlot.MetricStack(
                    label = "ON",
                    value = "TARGET",
                    isZoneFilled = true,
                    zoneColor = android.graphics.Color.parseColor("#2E7D32") // Matte Forest Green
                )
            }

            val centerHero = HeaderSlot.HeroValue(
                value = if (currentPower != null || isMock) cPower.toInt().toString() else "No Sensor",
                hasBottomBar = true,
            )

            // Delegate header slot layout and drawing to global FullWidthLayoutEngine
            FullWidthLayoutEngine.drawHeaderSlots(
                canvas = canvas,
                leftSlot = leftSlot,
                centerHero = centerHero,
                rightSlot = rightSlot,
                width = width.toFloat(),
                height = height.toFloat(),
                defaultTextColor = defaultTextColor,
                mutedTextColor = defaultTextColor,
                labelTypography = labelTypography,
                valueTypography = valueTypography,
                isDarkTheme = isDarkTheme,
            )

            // Unified Footer Band (78%..92% Height)
            val trackY = height * 0.78f
            val trackHeight = height * 0.14f

            val spanBuffer = if (minT != null && maxT != null && minT != maxT) ((maxT - minT) * 0.45f).coerceAtLeast(30f) else 40f
            val scaleMin = ((lowBound - spanBuffer)).coerceAtLeast(0f)
            val scaleMax = highBound + spanBuffer
            val scaleSpan = (scaleMax - scaleMin).coerceAtLeast(1f)

            val tLeftFrac = ((lowBound - scaleMin) / scaleSpan).coerceIn(0f, 1f)
            val tRightFrac = ((highBound - scaleMin) / scaleSpan).coerceIn(0f, 1f)
            val tLeftX = barLeft + (tLeftFrac * barWidth)
            val tRightX = barLeft + (tRightFrac * barWidth)

            // Base Track Capsule (Flat Solid Slate #1E262C with thin border)
            val trackBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTheme) 0xFF1E262C.toInt() else 0xFFE2E8F0.toInt()
                style = Paint.Style.FILL
            }
            val trackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTheme) 0xFF475569.toInt() else 0xFF94A3B8.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
            }
            val trackRect = RectF(barLeft, trackY, barRight, trackY + trackHeight)
            canvas.drawRoundRect(trackRect, trackHeight / 2f, trackHeight / 2f, trackBasePaint)
            canvas.drawRoundRect(trackRect, trackHeight / 2f, trackHeight / 2f, trackBorderPaint)

            // Target Corridor Highlight Box (Matte Forest Green #388E3C)
            val targetBoxRect = RectF(tLeftX, trackY, tRightX, trackY + trackHeight)
            val targetFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF388E3C.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(targetBoxRect, 6f, 6f, targetFillPaint)

            // Crisp Vertical Boundary Gate Tick Lines at tLeftX and tRightX
            val gateTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF0F172A.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
            }
            canvas.drawLine(tLeftX, trackY - 4f, tLeftX, trackY + trackHeight + 4f, gateTickPaint)
            canvas.drawLine(tRightX, trackY - 4f, tRightX, trackY + trackHeight + 4f, gateTickPaint)

            // Solid WHITE Cursor Needle (extending 5px above and below track with 2.5f border)
            if (currentPower != null || isMock) {
                val cursorFrac = ((cPower - scaleMin) / scaleSpan).coerceIn(0f, 1f)
                val cursorX = barLeft + (cursorFrac * barWidth)

                val needleWidth = 7f
                val needleExt = 5f
                val needleRect = RectF(
                    cursorX - (needleWidth / 2f),
                    trackY - needleExt,
                    cursorX + (needleWidth / 2f),
                    trackY + trackHeight + needleExt
                )

                val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                val needleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF0F172A.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                }
                canvas.drawRoundRect(needleRect, needleWidth / 2f, needleWidth / 2f, needlePaint)
                canvas.drawRoundRect(needleRect, needleWidth / 2f, needleWidth / 2f, needleBorderPaint)
            }

            bitmap
        }.getOrElse { throwable ->
            KustomLogger.e("Error rendering target power corridor bitmap", throwable)
            createFallbackBitmap(
                config.viewSize.first.coerceAtLeast(10),
                config.viewSize.second.coerceAtLeast(10),
            )
        }
    }



    /**
     * Constructs a smooth cubic Bézier curve path from an array of 2D data points (X, Y).
     */
    private fun buildSmoothTracePath(points: List<Pair<Float, Float>>): android.graphics.Path {
        val path = android.graphics.Path()
        if (points.isEmpty()) return path

        path.moveTo(points[0].first, points[0].second)
        if (points.size == 1) return path

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val controlX = (p0.first + p1.first) / 2f
            path.quadTo(p0.first, p0.second, controlX, (p0.second + p1.second) / 2f)
        }
        path.lineTo(points.last().first, points.last().second)
        return path
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
     * Renders an unformatted plain-text diagnostic list for raw debugging.
     * Line 1: STREAM_NAME
     * Line 2: key1:val1 | key2:val2 | key3:val3...
     * Single line return between sets.
     */
    fun renderDebugCategoryField(
        context: Context,
        config: ViewConfig,
        categoryTitle: String,
        debugDps: Map<String, DataPoint>?,
        isDarkTheme: Boolean = false,
    ): Bitmap {
        val width = config.viewSize.first.coerceAtLeast(10)
        val height = config.viewSize.second.coerceAtLeast(10)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = if (isDarkTheme) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        val textColor = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val keyColor = if (isDarkTheme) 0xFF00E5FF.toInt() else 0xFF0055FF.toInt()

        canvas.drawColor(bgColor)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = keyColor
            textSize = (height * 0.075f).coerceIn(12f, 18f)
            isFakeBoldText = true
        }
        canvas.drawText(categoryTitle.uppercase(), width * 0.03f, height * 0.08f, titlePaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF444444.toInt() else 0xFFCCCCCC.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawLine(width * 0.03f, height * 0.095f, width * 0.97f, height * 0.095f, linePaint)

        val streamKeyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = keyColor
            textSize = (height * 0.052f).coerceIn(11f, 16f)
            isFakeBoldText = true
        }

        val valTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = (height * 0.046f).coerceIn(10f, 14f)
        }

        var startY = height * 0.15f
        val entries = debugDps?.entries?.toList() ?: emptyList()

        if (entries.isEmpty()) {
            canvas.drawText("No active stream DataPoints received from Karoo", width * 0.03f, startY, valTextPaint)
            return bitmap
        }

        entries.forEach { (streamName, dp) ->
            val cleanName = streamName.replace("WORKOUT_", "").replace("_REMAINING", "")
            // Line 1: Stream Name
            canvas.drawText(cleanName, width * 0.03f, startY, streamKeyPaint)
            startY += height * 0.050f

            // Line 2: Values (Unformatted plain text)
            if (dp.values.isEmpty()) {
                canvas.drawText("No Signal / Empty", width * 0.03f, startY, valTextPaint)
                startY += height * 0.055f
            } else {
                val entriesStr = dp.values.entries.joinToString(" | ") { (k, v) ->
                    val kClean = k.replace("FIELD_WORKOUT_", "").replace("FIELD_", "").replace("_ID", "").replace("_VALUE", "")
                    val vFormatted = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
                    "$kClean:$vFormatted"
                }

                KustomLogger.d("[DEBUG HUD TRACE] $cleanName -> $entriesStr")

                // Handle multi-line wrapping if text is long
                if (entriesStr.length > 36) {
                    val part1 = entriesStr.substring(0, 36)
                    val part2 = if (entriesStr.length > 72) entriesStr.substring(36, 72) else entriesStr.substring(36)
                    canvas.drawText(part1, width * 0.03f, startY, valTextPaint)
                    startY += height * 0.045f
                    canvas.drawText(part2, width * 0.03f, startY, valTextPaint)
                    startY += height * 0.050f
                } else {
                    canvas.drawText(entriesStr, width * 0.03f, startY, valTextPaint)
                    startY += height * 0.050f
                }
            }

            // Single line return / small vertical space between sets
            startY += height * 0.025f
        }

        return bitmap
    }
    fun renderDebugAllFields(
        context: Context,
        config: ViewConfig,
        debugDps: Map<String, DataPoint>?,
        isDarkTheme: Boolean = false,
    ): Bitmap {
        val width = config.viewSize.first.coerceAtLeast(10)
        val height = config.viewSize.second.coerceAtLeast(10)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val pageIndex = ((System.currentTimeMillis() / 3000L) % 3).toInt()

        val bgColor = if (isDarkTheme) 0xFF0F172A.toInt() else 0xFFF8FAFC.toInt()
        val cardBgColor = if (isDarkTheme) 0xFF1E293B.toInt() else 0xFFFFFFFF.toInt()
        val textColor = if (isDarkTheme) 0xFFF8FAFC.toInt() else 0xFF0F172A.toInt()
        val titleColor = if (isDarkTheme) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt()
        val keyColor = if (isDarkTheme) 0xFF94A3B8.toInt() else 0xFF334155.toInt()

        canvas.drawColor(bgColor)

        // Draw Outer Boundary Card Inset
        val insetRect = RectF(2f, 2f, width.toFloat() - 2f, height.toFloat() - 2f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardBgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(insetRect, 12f, 12f, cardPaint)

        // 1. Header Banner
        val headerHeight = height * 0.12f
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF334155.toInt() else 0xFFE2E8F0.toInt()
            style = Paint.Style.FILL
        }
        val headerRect = RectF(2f, 2f, width.toFloat() - 2f, headerHeight)
        canvas.drawRoundRect(headerRect, 12f, 12f, headerBgPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColor
            textSize = (height * 0.065f).coerceIn(12f, 18f)
            isFakeBoldText = true
        }
        canvas.drawText("DEBUG HUD", width * 0.04f, headerHeight * 0.68f, titlePaint)

        val pageTitle = when (pageIndex) {
            0 -> "PG 1/3: WORKOUT"
            1 -> "PG 2/3: POWER & HR"
            else -> "PG 3/3: SENSORS"
        }

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColor
            style = Paint.Style.FILL
        }
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = (height * 0.052f).coerceIn(11f, 15f)
            isFakeBoldText = true
        }

        val badgeWidth = badgeTextPaint.measureText(pageTitle) + (width * 0.04f)
        val badgeRect = RectF(
            width * 0.94f - badgeWidth,
            headerHeight * 0.15f,
            width * 0.94f,
            headerHeight * 0.85f
        )
        canvas.drawRoundRect(badgeRect, 8f, 8f, badgePaint)
        canvas.drawText(
            pageTitle,
            badgeRect.left + (width * 0.02f),
            badgeRect.top + (badgeRect.height() * 0.72f),
            badgeTextPaint
        )

        // Define Page Groups
        val pageGroup0Keys = listOf(
            "WORKOUT_POWER_TARGET",
            "WORKOUT_PRIMARY_TARGET",
            "WORKOUT_SECONDARY_TARGET",
            "WORKOUT_REMAINING_TOTAL_DURATION",
            "WORKOUT_REMAINING_INTERVAL_DURATION",
            "WORKOUT_INTERVAL_COUNT",
        )

        val pageGroup1Keys = listOf(
            "POWER",
            "HEART_RATE",
            "AVERAGE_POWER",
            "MAX_POWER",
            "NORMALIZED_POWER",
            "AVERAGE_HR",
            "MAX_HR",
        )

        val pageGroup2Keys = listOf(
            "SPEED",
            "CADENCE",
            "DISTANCE",
            "ELAPSED_TIME",
            "CLOCK_TIME",
            "BATTERY_PERCENT",
            "TEMPERATURE",
            "ELEVATION_GRADE",
        )

        val activeKeys = when (pageIndex) {
            0 -> pageGroup0Keys
            1 -> pageGroup1Keys
            else -> pageGroup2Keys
        }

        var startY = headerHeight + (height * 0.02f)
        val rowHeight = (height - headerHeight - (height * 0.03f)) / activeKeys.size.coerceAtLeast(1)

        val keyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = keyColor
            textSize = (rowHeight * 0.48f).coerceIn(13f, 22f)
            isFakeBoldText = true
        }

        val valTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = (rowHeight * 0.48f).coerceIn(13f, 22f)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF334155.toInt() else 0xFFE2E8F0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        activeKeys.forEachIndexed { idx, streamName ->
            val dp = debugDps?.get(streamName)
            val shortName = streamName.replace("WORKOUT_", "").replace("_REMAINING", "")

            canvas.drawText(shortName, width * 0.04f, startY + (rowHeight * 0.65f), keyPaint)

            val valBoxLeft = width * 0.46f
            val valBoxRight = width * 0.96f
            val valBoxTop = startY + (rowHeight * 0.08f)
            val valBoxBottom = startY + (rowHeight * 0.92f)

            if (dp == null || dp.values.isEmpty()) {
                val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDarkTheme) 0xFF334155.toInt() else 0xFFF1F5F9.toInt()
                    style = Paint.Style.FILL
                }
                val pillRect = RectF(valBoxLeft, valBoxTop, valBoxRight, valBoxBottom)
                canvas.drawRoundRect(pillRect, 6f, 6f, inactivePaint)

                val nullPaint = TextPaint(valTextPaint).apply {
                    color = if (isDarkTheme) 0xFF64748B.toInt() else 0xFF94A3B8.toInt()
                }
                canvas.drawText("No Signal", valBoxLeft + (width * 0.03f), valBoxTop + (pillRect.height() * 0.70f), nullPaint)
            } else {
                val entriesStr = dp.values.entries.joinToString(" ") { (k, v) ->
                    val kClean = k.replace("FIELD_WORKOUT_", "").replace("FIELD_", "").replace("_ID", "").replace("_VALUE", "")
                    val vFormatted = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
                    "$kClean:$vFormatted"
                }

                val hasNonZeroVal = dp.values.values.any { it > 0.0 }
                val pillBgColor = if (hasNonZeroVal) {
                    if (isDarkTheme) 0xFF0284C7.toInt() else 0xFFE0F2FE.toInt()
                } else {
                    if (isDarkTheme) 0xFF334155.toInt() else 0xFFF1F5F9.toInt()
                }

                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pillBgColor
                    style = Paint.Style.FILL
                }
                val pillRect = RectF(valBoxLeft, valBoxTop, valBoxRight, valBoxBottom)
                canvas.drawRoundRect(pillRect, 6f, 6f, pillPaint)

                val activeValTextPaint = TextPaint(valTextPaint).apply {
                    color = if (hasNonZeroVal) {
                        if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF0369A1.toInt()
                    } else {
                        if (isDarkTheme) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
                    }
                    isFakeBoldText = hasNonZeroVal
                }

                val cleanLine = if (entriesStr.length > 22) entriesStr.substring(0, 22) + "…" else entriesStr
                canvas.drawText(cleanLine, valBoxLeft + (width * 0.02f), valBoxTop + (pillRect.height() * 0.70f), activeValTextPaint)
            }

            if (idx < activeKeys.size - 1) {
                val lineY = startY + rowHeight
                canvas.drawLine(width * 0.04f, lineY, width * 0.96f, lineY, linePaint)
            }

            startY += rowHeight
        }

        return bitmap
    }

    /**
     * Splits value text into primary main digits and secondary superscript digits (for decimals or time colons).
     */
    private fun parsePrimaryAndSecondary(valueText: String): Pair<String, String> {
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
     * Abbreviates long corner metric labels to save space and prevent text crowding.
     *
     * @param rawLabel The original un-abbreviated metric title (e.g. "Average Cadence").
     * @return Shortened uppercase label string (e.g. "AVG CAD").
     */
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
