package com.hank.kustom.data.model

/**
 * Manufacturer zone color palette options (Karoo, Garmin, Wahoo, Zwift).
 *
 * @property displayName User-visible palette name string.
 */
enum class ZonePalette(val displayName: String) {
    KAROO("Karoo"),
    GARMIN("Garmin"),
    WAHOO("Wahoo"),
    ZWIFT("Zwift");

    fun get7ZoneColors(): List<Int> {
        return when (this) {
            KAROO -> listOf(0xFF718096.toInt(), 0xFF2B6CB0.toInt(), 0xFF2F855A.toInt(), 0xFFD69E2E.toInt(), 0xFFDD6B20.toInt(), 0xFFE53E3E.toInt(), 0xFF805AD5.toInt())
            GARMIN -> listOf(0xFF707070.toInt(), 0xFF0055FF.toInt(), 0xFF00CC44.toInt(), 0xFFFF8000.toInt(), 0xFFFF0000.toInt(), 0xFFA00000.toInt(), 0xFF800080.toInt())
            WAHOO -> listOf(0xFF757575.toInt(), 0xFF00B0FF.toInt(), 0xFF00E676.toInt(), 0xFFFFD600.toInt(), 0xFFFF6D00.toInt(), 0xFFFF1744.toInt(), 0xFFAA00FF.toInt())
            ZWIFT -> listOf(0xFF808080.toInt(), 0xFF3399FF.toInt(), 0xFF33CC33.toInt(), 0xFFFFCC00.toInt(), 0xFFFF6600.toInt(), 0xFFFF0000.toInt(), 0xFFCC0066.toInt())
        }
    }

    fun get5ZoneColors(): List<Int> {
        return when (this) {
            KAROO -> listOf(0xFF718096.toInt(), 0xFF2B6CB0.toInt(), 0xFF2F855A.toInt(), 0xFFDD6B20.toInt(), 0xFFE53E3E.toInt())
            GARMIN -> listOf(0xFF707070.toInt(), 0xFF0055FF.toInt(), 0xFF00CC44.toInt(), 0xFFFF8000.toInt(), 0xFFFF0000.toInt())
            WAHOO -> listOf(0xFF757575.toInt(), 0xFF00B0FF.toInt(), 0xFF00E676.toInt(), 0xFFFFD600.toInt(), 0xFFFF1744.toInt())
            ZWIFT -> listOf(0xFF808080.toInt(), 0xFF3399FF.toInt(), 0xFF33CC33.toInt(), 0xFFFFCC00.toInt(), 0xFFFF0000.toInt())
        }
    }
}
