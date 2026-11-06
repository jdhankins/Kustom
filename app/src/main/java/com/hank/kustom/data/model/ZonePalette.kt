package com.hank.kustom.data.model

data class NeonGlassSpec(
    val topColor: Int,
    val coreColor: Int,
    val rimColor: Int,
)

/**
 * Manufacturer zone color palette options (Karoo, Garmin, Wahoo, Zwift, Neon Glass).
 *
 * @property displayName User-visible palette name string.
 */
enum class ZonePalette(val displayName: String) {
    KAROO("Karoo"),
    GARMIN("Garmin"),
    WAHOO("Wahoo"),
    ZWIFT("Zwift"),
    NEON_GLASS("Neon Glass");

    fun get7ZoneColors(): List<Int> {
        return when (this) {
            KAROO -> listOf(0xFF718096.toInt(), 0xFF2B6CB0.toInt(), 0xFF2F855A.toInt(), 0xFFD69E2E.toInt(), 0xFFDD6B20.toInt(), 0xFFE53E3E.toInt(), 0xFF805AD5.toInt())
            GARMIN -> listOf(0xFF707070.toInt(), 0xFF0055FF.toInt(), 0xFF00CC44.toInt(), 0xFFFF8000.toInt(), 0xFFFF0000.toInt(), 0xFFA00000.toInt(), 0xFF800080.toInt())
            WAHOO -> listOf(0xFF757575.toInt(), 0xFF00B0FF.toInt(), 0xFF00E676.toInt(), 0xFFFFD600.toInt(), 0xFFFF6D00.toInt(), 0xFFFF1744.toInt(), 0xFFAA00FF.toInt())
            ZWIFT -> listOf(0xFF808080.toInt(), 0xFF3399FF.toInt(), 0xFF33CC33.toInt(), 0xFFFFCC00.toInt(), 0xFFFF6600.toInt(), 0xFFFF0000.toInt(), 0xFFCC0066.toInt())
            NEON_GLASS -> listOf(0xFF38BDF8.toInt(), 0xFF0284C7.toInt(), 0xFF059669.toInt(), 0xFFD97706.toInt(), 0xFFEA580C.toInt(), 0xFFE11D48.toInt(), 0xFF9333EA.toInt())
        }
    }

    fun get5ZoneColors(): List<Int> {
        return when (this) {
            KAROO -> listOf(0xFF718096.toInt(), 0xFF2B6CB0.toInt(), 0xFF2F855A.toInt(), 0xFFDD6B20.toInt(), 0xFFE53E3E.toInt())
            GARMIN -> listOf(0xFF707070.toInt(), 0xFF0055FF.toInt(), 0xFF00CC44.toInt(), 0xFFFF8000.toInt(), 0xFFFF0000.toInt())
            WAHOO -> listOf(0xFF757575.toInt(), 0xFF00B0FF.toInt(), 0xFF00E676.toInt(), 0xFFFFD600.toInt(), 0xFFFF1744.toInt())
            ZWIFT -> listOf(0xFF808080.toInt(), 0xFF3399FF.toInt(), 0xFF33CC33.toInt(), 0xFFFFCC00.toInt(), 0xFFFF0000.toInt())
            NEON_GLASS -> listOf(0xFF38BDF8.toInt(), 0xFF0284C7.toInt(), 0xFF059669.toInt(), 0xFFEA580C.toInt(), 0xFFE11D48.toInt())
        }
    }

    fun getNeonGlassSpec(zoneIndex: Int): NeonGlassSpec {
        val idx = zoneIndex.coerceIn(0, 6)
        return when (idx) {
            0 -> NeonGlassSpec(0xFF94A3B8.toInt(), 0xFF38BDF8.toInt(), 0xFF7DD3FC.toInt())
            1 -> NeonGlassSpec(0xFF00F0FF.toInt(), 0xFF0284C7.toInt(), 0xFF38BDF8.toInt())
            2 -> NeonGlassSpec(0xFF00FF87.toInt(), 0xFF059669.toInt(), 0xFF34D399.toInt())
            3 -> NeonGlassSpec(0xFFFFE600.toInt(), 0xFFD97706.toInt(), 0xFFFDE047.toInt())
            4 -> NeonGlassSpec(0xFFFF6B00.toInt(), 0xFFEA580C.toInt(), 0xFFFB923C.toInt())
            5 -> NeonGlassSpec(0xFFFF0055.toInt(), 0xFFE11D48.toInt(), 0xFFFB7185.toInt())
            else -> NeonGlassSpec(0xFFD946EF.toInt(), 0xFF9333EA.toInt(), 0xFFE879F9.toInt())
        }
    }

    fun getNeonGlassSpecForColor(baseColor: Int): NeonGlassSpec {
        val colors7 = get7ZoneColors()
        val idx = colors7.indexOf(baseColor)
        if (idx >= 0) return getNeonGlassSpec(idx)

        val top = 0xFFFFFFFF.toInt()
        val core = baseColor
        val rim = 0xFFE2E8F0.toInt()
        return NeonGlassSpec(top, core, rim)
    }
}
