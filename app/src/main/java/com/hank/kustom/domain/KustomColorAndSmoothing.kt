package com.hank.kustom.domain

import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.ZonePalette
import io.hammerhead.karooext.models.UserProfile
import java.util.ArrayDeque
import kotlin.math.pow

/**
 * Pure Kotlin color mappers for Power zones, Heart Rate zones, and Elevation Grade % with WCAG contrast helper.
 */
object KustomColorGradeMapper {

    /**
     * Maps power output in watts to a high-contrast zone color.
     *
     * @param watts Current power output in watts.
     * @param userProfile The rider's [UserProfile] containing custom power zones or FTP.
     * @param colorMode The active [ColorMode] (NONE, BACKGROUND_COLOR, or TEXT_COLOR).
     * @param isDarkTheme Whether dark theme is active.
     * @param palette Selected manufacturer [ZonePalette].
     * @return Color Int for the zone, or null if [ColorMode.NONE].
     */
    fun getPowerZoneColor(
        watts: Double,
        userProfile: UserProfile?,
        colorMode: ColorMode,
        isDarkTheme: Boolean = false,
        palette: ZonePalette = ZonePalette.KAROO,
    ): Int? {
        if (colorMode == ColorMode.NONE) return null

        val ftp = (userProfile?.ftp ?: 250).toDouble().coerceAtLeast(50.0)
        val pctFtp = watts / ftp

        val powerZones = userProfile?.powerZones
        val zoneIndex = if (!powerZones.isNullOrEmpty()) {
            val idx = powerZones.indexOfFirst { (min, max) ->
                val minD = min.toDouble()
                val maxD = max.toDouble()
                if (maxD <= 5.0) {
                    pctFtp in minD..maxD
                } else {
                    watts in minD..maxD
                }
            }
            if (idx != -1) {
                idx
            } else if (pctFtp < (powerZones.firstOrNull()?.min?.toDouble() ?: 0.55)) {
                0
            } else {
                powerZones.lastIndex
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

        val colors7 = palette.get7ZoneColors()
        return colors7.getOrElse(zoneIndex) { colors7.last() }
    }

    /**
     * Maps heart rate in bpm to a high-contrast zone color.
     *
     * @param bpm Current heart rate in beats per minute.
     * @param userProfile The rider's [UserProfile] containing custom HR zones or Max HR.
     * @param colorMode The active [ColorMode] (NONE, BACKGROUND_COLOR, or TEXT_COLOR).
     * @param isDarkTheme Whether dark theme is active.
     * @param palette Selected manufacturer [ZonePalette].
     * @return Color Int for the zone, or null if [ColorMode.NONE].
     */
    fun getHeartRateZoneColor(
        bpm: Double,
        userProfile: UserProfile?,
        colorMode: ColorMode,
        isDarkTheme: Boolean = false,
        palette: ZonePalette = ZonePalette.KAROO,
    ): Int? {
        if (colorMode == ColorMode.NONE) return null

        val hrZones = userProfile?.heartRateZones
        val zoneIndex = if (!hrZones.isNullOrEmpty()) {
            val idx = hrZones.indexOfFirst { (min, max) -> bpm in min.toDouble()..max.toDouble() }
            if (idx != -1) {
                idx
            } else if (bpm < (hrZones.firstOrNull()?.min ?: 0)) {
                0
            } else {
                hrZones.lastIndex
            }
        } else {
            val maxHr = userProfile?.maxHr ?: 185
            when {
                bpm < 0.60 * maxHr -> 0
                bpm < 0.70 * maxHr -> 1
                bpm < 0.80 * maxHr -> 2
                bpm < 0.90 * maxHr -> 3
                bpm < 0.95 * maxHr -> 4
                else -> 5
            }
        }

        val colors5 = palette.get5ZoneColors()
        return colors5.getOrElse(zoneIndex) { colors5.last() }
    }

    /**
     * Maps elevation grade percentage to a color.
     *
     * @param gradePercent Current road gradient in percent (e.g. 5.5%).
     * @param colorMode The active [ColorMode] (NONE, BACKGROUND_COLOR, or TEXT_COLOR).
     * @param isDarkTheme Whether dark theme is active.
     * @return Color Int for the gradient steepness, or null if [ColorMode.NONE].
     */
    fun getGradeColor(
        gradePercent: Double,
        colorMode: ColorMode,
        isDarkTheme: Boolean = false,
    ): Int? {
        if (colorMode == ColorMode.NONE) return null

        return when {
            gradePercent < -2.0 -> 0xFF29B6F6.toInt() // Cool Sky Blue (descent)
            gradePercent in -2.0..2.0 -> null // Neutral / Default
            gradePercent in 2.0..4.0 -> if (colorMode == ColorMode.BACKGROUND_COLOR) 0xFF5AE0A0.toInt() else 0xFF66BB6A.toInt() // Mint Green (2-4%)
            gradePercent in 4.0..7.0 -> 0xFFFBC02D.toInt() // Gold/Amber (4-7%)
            gradePercent in 7.0..10.0 -> 0xFFFFA726.toInt() // Deep Orange (7-10%)
            gradePercent in 10.0..14.0 -> 0xFFEF5350.toInt() // Coral Red (10-14%)
            else -> 0xFFAB47BC.toInt() // Purple (>14%)
        }
    }

    /**
     * Calculates relative luminance and returns charcoal (#1E2225) or white (#FFFFFF) text color
     * for maximum outdoor readability on top of [backgroundColor].
     *
     * @param backgroundColor Background color Int.
     * @return High-contrast text color Int (charcoal or white).
     */
    fun getContrastingTextColor(backgroundColor: Int): Int {
        val red = (backgroundColor shr 16) and 0xFF
        val green = (backgroundColor shr 8) and 0xFF
        val blue = backgroundColor and 0xFF

        val r = red / 255.0
        val g = green / 255.0
        val b = blue / 255.0

        val rL = if (r <= 0.04045) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gL = if (g <= 0.04045) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bL = if (b <= 0.04045) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        val luminance = 0.2126 * rL + 0.7152 * gL + 0.0722 * bL

        return if (luminance > 0.30) 0xFF1E2225.toInt() else 0xFFFFFFFF.toInt()
    }
}

/**
 * Lightweight rolling time-window smoother for metrics without SDK built-in smoothed streams.
 *
 * @property windowSeconds Smoothing window duration in seconds.
 */
class RollingTimeSmoother(private val windowSeconds: Int) {

    private data class Sample(val timestampMs: Long, val value: Double)

    private val samples = ArrayDeque<Sample>()

    /**
     * Adds a new sample and returns the smoothed rolling average over [windowSeconds].
     *
     * @param value Sample raw value.
     * @param timestampMs Sample timestamp in epoch ms.
     * @return Rolling average value over the time window.
     */
    @Synchronized
    fun addSample(value: Double, timestampMs: Long = System.currentTimeMillis()): Double {
        if (windowSeconds <= 0) return value

        samples.addLast(Sample(timestampMs, value))
        val cutoff = timestampMs - (windowSeconds * 1000L)
        while (samples.isNotEmpty() && samples.first.timestampMs < cutoff) {
            samples.removeFirst()
        }

        return if (samples.isEmpty()) value else samples.sumOf { it.value } / samples.size
    }

    /** Clears all rolling samples. */
    @Synchronized
    fun clear() {
        samples.clear()
    }
}
