package com.hank.kustom.data.model

/**
 * Normalized workout interval step model representing a single workout phase.
 *
 * @property stepIndex Zero-based step position index.
 * @property name Step title (e.g. "Warmup", "Interval 1", "Rest").
 * @property durationSeconds Duration of the step in seconds.
 * @property targetWattsMin Lower power target boundary in Watts.
 * @property targetWattsMax Upper power target boundary in Watts.
 * @property targetType Target intensity type ("POWER", "HR", "CADENCE").
 * @property zoneColorInt ARGB Color Int corresponding to target power/HR zone.
 * @property heightRatio Normalized height ratio (0.1f to 1.0f) for graph canvas rendering.
 */
data class WorkoutStepInterval(
    val stepIndex: Int,
    val name: String = "",
    val durationSeconds: Int,
    val targetWattsMin: Double,
    val targetWattsMax: Double,
    val targetType: String = "POWER",
    val zoneColorInt: Int,
    val heightRatio: Float,
)

/**
 * Complete structured workout data model parsed from local FIT or ZWO files.
 *
 * @property title Workout title (e.g. "Endurance ride 2h 30m").
 * @property source Workout platform source (e.g. "TrainingPeaks", "Zwift", "TrainerRoad").
 * @property totalDurationSeconds Total duration of the workout in seconds.
 * @property steps Ordered list of [WorkoutStepInterval] steps composing the workout.
 */
data class ParsedWorkout(
    val title: String = "Workout",
    val source: String = "TrainingPeaks",
    val totalDurationSeconds: Int = 5400,
    val steps: List<WorkoutStepInterval> = emptyList(),
)
