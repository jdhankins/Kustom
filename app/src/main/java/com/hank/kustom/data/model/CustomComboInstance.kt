package com.hank.kustom.data.model

import java.util.UUID

/**
 * User-created dynamic 3-in-1 combo block instance model.
 *
 * @property id Unique instance identifier (e.g. "combo_1698234").
 * @property name User-customized display name (e.g. "Climbing Block").
 * @property centerMetric Metric typeId assigned to center primary position.
 * @property leftMetric Metric typeId assigned to top-left corner position.
 * @property rightMetric Metric typeId assigned to top-right corner position.
 * @property centerZoneFormat Zone color format for center metric.
 * @property leftZoneFormat Zone color format for left metric.
 * @property rightZoneFormat Zone color format for right metric.
 * @property centerSmoothing Rolling smoothing duration for center metric.
 * @property leftSmoothing Rolling smoothing duration for left metric.
 * @property rightSmoothing Rolling smoothing duration for right metric.
 */
data class CustomComboInstance(
    val id: String = "combo_${UUID.randomUUID().toString().take(8)}",
    val name: String = "Custom Block",
    val centerMetric: String = "power",
    val leftMetric: String = "np",
    val rightMetric: String = "max-power",
    val centerZoneFormat: ZoneFormat = ZoneFormat.BAR,
    val leftZoneFormat: ZoneFormat = ZoneFormat.NONE,
    val rightZoneFormat: ZoneFormat = ZoneFormat.NONE,
    val centerSmoothing: SmoothingOption = SmoothingOption.THREE_SEC,
    val leftSmoothing: SmoothingOption = SmoothingOption.NONE,
    val rightSmoothing: SmoothingOption = SmoothingOption.NONE,
)
