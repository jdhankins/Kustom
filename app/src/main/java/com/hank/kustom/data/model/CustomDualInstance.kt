package com.hank.kustom.data.model

import java.util.UUID

/**
 * User-created dynamic 2-in-1 dual split field instance model.
 */
data class CustomDualInstance(
    val id: String = "dual_${UUID.randomUUID().toString().take(8)}",
    val name: String = "2-in-1 Block",
    val leftMetric: String = "power",
    val rightMetric: String = "grade",
    val leftZoneFormat: ZoneFormat = ZoneFormat.BACKGROUND,
    val rightZoneFormat: ZoneFormat = ZoneFormat.TEXT,
    val leftSmoothing: SmoothingOption = SmoothingOption.THREE_SEC,
    val rightSmoothing: SmoothingOption = SmoothingOption.NONE,
)