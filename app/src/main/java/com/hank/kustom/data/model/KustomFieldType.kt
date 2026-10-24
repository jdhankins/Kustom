package com.hank.kustom.data.model

/**
 * Supported 5 core cycling fields for Kustom extension.
 */
enum class KustomFieldType(
    val typeId: String,
    val displayName: String,
    val defaultUnit: String,
) {
    POWER("kustom-power", "Power", "W"),
    HEART_RATE("kustom-hr", "Heart Rate", "bpm"),
    SPEED("kustom-speed", "Speed", "km/h"),
    CADENCE("kustom-cadence", "Cadence", "rpm"),
    GRADE("kustom-grade", "Grade %", "%"),
}
