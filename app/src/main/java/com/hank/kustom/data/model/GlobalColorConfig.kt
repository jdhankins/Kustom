package com.hank.kustom.data.model

/**
 * Global background color configuration for Light and Dark modes.
 *
 * @property lightBgColorHex Hex string for Light Mode field background color (default #FFFFFF).
 * @property darkBgColorHex Hex string for Dark Mode field background color (default #000000).
 * @property useCustomBg Whether custom background colors are active.
 */
data class GlobalColorConfig(
    val lightBgColorHex: String = "#FFFFFF",
    val darkBgColorHex: String = "#000000",
    val useCustomBg: Boolean = false,
) {
    fun getLightColorInt(): Int {
        return runCatching { android.graphics.Color.parseColor(lightBgColorHex) }.getOrDefault(0xFFFFFFFF.toInt())
    }

    fun getDarkColorInt(): Int {
        return runCatching { android.graphics.Color.parseColor(darkBgColorHex) }.getOrDefault(0xFF000000.toInt())
    }

    companion object {
        val LIGHT_PRESETS = listOf(
            "#FFFFFF", "#FCFCFC", "#F8F8F8", "#F4F4F4", "#F1F1F1", "#EDEDED",
            "#EAEAEA", "#E6E6E6", "#E2E2E2", "#DEDEDE", "#DADADA", "#D7D7D7",
            "#D3D3D3", "#CFCFCF", "#CCCCCC", "#C8C8C8", "#C4C4C4", "#BFBFBF"
        )
        val DARK_PRESETS = listOf(
            "#8C8C8C", "#848484", "#7C7C7C", "#747474", "#6C6C6C", "#646464",
            "#5C5C5C", "#545454", "#4C4C4C", "#444444", "#3C3C3C", "#343434",
            "#2C2C2C", "#242424", "#1C1C1C", "#141414", "#0C0C0C", "#000000"
        )
    }
}
