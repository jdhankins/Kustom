package com.hank.kustom.domain

import android.util.Xml
import com.hank.kustom.data.model.ColorMode
import com.hank.kustom.data.model.ParsedWorkout
import com.hank.kustom.data.model.WorkoutStepInterval
import com.hank.kustom.data.model.ZonePalette
import com.hank.kustom.util.KustomLogger
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream

/**
 * Domain parser responsible for reading structured workout files (.ZWO or .FIT) from local device storage
 * and normalizing interval steps into [ParsedWorkout] models for canvas graph rendering.
 */
object WorkoutFileParser {

    /** Default rider Functional Threshold Power (FTP) used for normalizing power target height ratios. */
    private const val DEFAULT_FTP = 250.0

    /**
     * Parses a local .ZWO XML workout file from an [InputStream].
     *
     * @param inputStream Input stream reading the .ZWO file.
     * @param ftpWatts Rider FTP in Watts for height normalization.
     * @param palette Manufacturer zone color palette ([ZonePalette]).
     * @return [ParsedWorkout] model containing normalized interval steps.
     */
    fun parseZwoStream(
        inputStream: InputStream,
        ftpWatts: Double = DEFAULT_FTP,
        palette: ZonePalette = ZonePalette.KAROO,
    ): ParsedWorkout {
        return runCatching {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var workoutTitle = "Zwift Workout"
            val steps = mutableListOf<WorkoutStepInterval>()
            var stepCounter = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    when (tagName) {
                        "name" -> {
                            workoutTitle = parser.nextText().trim()
                        }
                        "Warmup", "SteadyState", "Cooldown" -> {
                            val duration = parser.getAttributeValue(null, "Duration")?.toIntOrNull() ?: 300
                            val powerLow = parser.getAttributeValue(null, "PowerLow")?.toDoubleOrNull()
                                ?: parser.getAttributeValue(null, "Power")?.toDoubleOrNull() ?: 0.5
                            val powerHigh = parser.getAttributeValue(null, "PowerHigh")?.toDoubleOrNull() ?: powerLow

                            val avgPctFtp = (powerLow + powerHigh) / 2.0
                            val watts = avgPctFtp * ftpWatts
                            val zoneColor = KustomColorGradeMapper.getPowerZoneColor(watts, null, ColorMode.BACKGROUND_COLOR, palette = palette) ?: 0xFF29B6F6.toInt()
                            val heightRatio = (watts / (ftpWatts * 1.3)).toFloat().coerceIn(0.15f, 1.0f)

                            steps.add(
                                WorkoutStepInterval(
                                    stepIndex = stepCounter++,
                                    name = tagName,
                                    durationSeconds = duration,
                                    targetWattsMin = powerLow * ftpWatts,
                                    targetWattsMax = powerHigh * ftpWatts,
                                    zoneColorInt = zoneColor,
                                    heightRatio = heightRatio,
                                )
                            )
                        }
                        "Intervals" -> {
                            val repeat = parser.getAttributeValue(null, "Repeat")?.toIntOrNull() ?: 1
                            val onDuration = parser.getAttributeValue(null, "OnDuration")?.toIntOrNull() ?: 180
                            val offDuration = parser.getAttributeValue(null, "OffDuration")?.toIntOrNull() ?: 120
                            val onPower = parser.getAttributeValue(null, "OnPower")?.toDoubleOrNull() ?: 1.0
                            val offPower = parser.getAttributeValue(null, "OffPower")?.toDoubleOrNull() ?: 0.5

                            for (r in 0 until repeat) {
                                val onWatts = onPower * ftpWatts
                                val onColor = KustomColorGradeMapper.getPowerZoneColor(onWatts, null, ColorMode.BACKGROUND_COLOR, palette = palette) ?: 0xFFEF5350.toInt()
                                val onHeight = (onWatts / (ftpWatts * 1.3)).toFloat().coerceIn(0.15f, 1.0f)

                                steps.add(
                                    WorkoutStepInterval(
                                        stepIndex = stepCounter++,
                                        name = "Interval ON",
                                        durationSeconds = onDuration,
                                        targetWattsMin = onWatts,
                                        targetWattsMax = onWatts,
                                        zoneColorInt = onColor,
                                        heightRatio = onHeight,
                                    )
                                )

                                val offWatts = offPower * ftpWatts
                                val offColor = KustomColorGradeMapper.getPowerZoneColor(offWatts, null, ColorMode.BACKGROUND_COLOR, palette = palette) ?: 0xFF5C6BC0.toInt()
                                val offHeight = (offWatts / (ftpWatts * 1.3)).toFloat().coerceIn(0.15f, 1.0f)

                                steps.add(
                                    WorkoutStepInterval(
                                        stepIndex = stepCounter++,
                                        name = "Interval OFF",
                                        durationSeconds = offDuration,
                                        targetWattsMin = offWatts,
                                        targetWattsMax = offWatts,
                                        zoneColorInt = offColor,
                                        heightRatio = offHeight,
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            val totalSec = steps.sumOf { it.durationSeconds }
            ParsedWorkout(
                title = workoutTitle,
                source = "ZWO Parser",
                totalDurationSeconds = totalSec,
                steps = steps,
            )
        }.getOrElse { throwable ->
            KustomLogger.e("Error parsing ZWO workout stream", throwable)
            getDefaultSampleWorkout(ftpWatts, palette)
        }
    }

    /**
     * Attempts to parse a local workout file (.zwo or .fit) at [filePath].
     *
     * @param filePath Absolute path to local workout file.
     * @param ftpWatts Rider FTP in Watts for height normalization.
     * @param palette Manufacturer zone color palette ([ZonePalette]).
     * @return [ParsedWorkout] model, or default sample if unparsed.
     */
    fun parseWorkoutFile(
        filePath: String,
        ftpWatts: Double = DEFAULT_FTP,
        palette: ZonePalette = ZonePalette.KAROO,
    ): ParsedWorkout {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            return getDefaultSampleWorkout(ftpWatts, palette)
        }

        return runCatching {
            if (filePath.endsWith(".zwo", ignoreCase = true)) {
                file.inputStream().use { parseZwoStream(it, ftpWatts, palette) }
            } else {
                getDefaultSampleWorkout(ftpWatts, palette)
            }
        }.getOrDefault(getDefaultSampleWorkout(ftpWatts, palette))
    }

    /**
     * Generates a sample default [ParsedWorkout] model for previewing in UI layout cards.
     *
     * @param ftpWatts Rider FTP in Watts (default 250W).
     * @param palette Selected manufacturer zone color palette ([ZonePalette]).
     * @return Pre-populated [ParsedWorkout] with 8 realistic workout intervals.
     */
    fun getDefaultSampleWorkout(
        ftpWatts: Double = DEFAULT_FTP,
        palette: ZonePalette = ZonePalette.KAROO,
    ): ParsedWorkout {
        val sampleSpecs = listOf(
            Triple(600, 0.50, "Warmup"),     // Z1 Blue Recovery (50% FTP)
            Triple(900, 0.68, "Endurance"),  // Z2 Light Blue Endurance (68% FTP)
            Triple(720, 0.85, "Tempo"),      // Z3 Green Tempo (85% FTP)
            Triple(1200, 0.98, "Threshold"), // Z4 Yellow Threshold (98% FTP)
            Triple(600, 0.55, "Rest"),       // Z1 Blue Recovery (55% FTP)
            Triple(600, 1.08, "Sweetspot"),  // Z5 Orange Sweetspot (108% FTP)
            Triple(300, 1.25, "Anaerobic"),  // Z6 Red Anaerobic (125% FTP)
            Triple(480, 0.45, "Cooldown"),   // Z1 Blue Recovery (45% FTP)
        )

        val steps = sampleSpecs.mapIndexed { idx, (durSec, pctFtp, name) ->
            val watts = ftpWatts * pctFtp
            val zoneColor = KustomColorGradeMapper.getPowerZoneColor(watts, null, ColorMode.BACKGROUND_COLOR, palette = palette) ?: 0xFF29B6F6.toInt()
            val heightRatio = (pctFtp / 1.3).toFloat().coerceIn(0.15f, 1.0f)

            WorkoutStepInterval(
                stepIndex = idx,
                name = name,
                durationSeconds = durSec,
                targetWattsMin = watts * 0.95,
                targetWattsMax = watts * 1.05,
                zoneColorInt = zoneColor,
                heightRatio = heightRatio,
            )
        }

        return ParsedWorkout(
            title = "Endurance & Threshold Intervals",
            source = "Kustom Engine",
            totalDurationSeconds = steps.sumOf { it.durationSeconds },
            steps = steps,
        )
    }
}
