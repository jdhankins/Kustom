package com.hank.kustom.data.model

/**
 * Global background color and zone configuration for Light and Dark modes.
 *
 * @property lightBgColorHex Hex string for Light Mode field background color (default #FFFFFF).
 * @property darkBgColorHex Hex string for Dark Mode field background color (default #000000).
 * @property useCustomBg Whether custom background colors are active.
 * @property globalPowerZoneCount Global default power zone count (7 or 5 zones).
 */
data class GlobalColorConfig(
    val lightBgColorHex: String = "#FFFFFF",
    val darkBgColorHex: String = "#000000",
    val useCustomBg: Boolean = false,
    val globalPowerZoneCount: Int = 7,
) {
    fun getLightColorInt(): Int {
        return runCatching { android.graphics.Color.parseColor(lightBgColorHex) }.getOrDefault(0xFFFFFFFF.toInt())
    }

    fun getDarkColorInt(): Int {
        return runCatching { android.graphics.Color.parseColor(darkBgColorHex) }.getOrDefault(0xFF000000.toInt())
    }

    companion object {
        val LIGHT_PRESETS = listOf(
            // 9 Lightest Whites / Greys
            "#FFFFFF", "#FCFCFC", "#F8F8F8", "#F4F4F4", "#F1F1F1", "#EDEDED",
            "#EAEAEA", "#E6E6E6", "#E2E2E2",
            // 6 Red / Pink
            "#FFF5F6", "#FFEBEF", "#FFE0E5", "#FFD6DC", "#FFCCD2", "#FFC2C8",
            // 6 Yellow / Cream
            "#FFFFFA", "#FFFFF0", "#FFFFE3", "#FFFFD6", "#FFFFCC", "#FFFFBD",
            // 6 Green / Mint
            "#F5FFFA", "#EBFDF4", "#E1FAED", "#D7F7E6", "#CDF4DF", "#C3F1D8",
            // 6 Blue / Ice
            "#FCFDFF", "#F2F7FC", "#E8F1F9", "#DEEAF7", "#D4E4F4", "#CBDEF2"
        )
        val DARK_PRESETS = listOf(
            "#8C8C8C", "#848484", "#7C7C7C", "#747474", "#6C6C6C", "#646464",
            "#5C5C5C", "#545454", "#4C4C4C", "#444444", "#3C3C3C", "#343434",
            "#2C2C2C", "#242424", "#1C1C1C", "#141414", "#0C0C0C", "#000000"
        )
    }
}
