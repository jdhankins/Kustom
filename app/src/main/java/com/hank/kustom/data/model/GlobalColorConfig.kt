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
        val LIGHT_PRESETS = listOf("#FFFFFF", "#F4F6F7", "#E2E8F0", "#E0F2FE", "#FEF3C7", "#1E2225")
        val DARK_PRESETS = listOf("#000000", "#17283C", "#1E293B", "#18181B", "#0F172A", "#FFFFFF")
    }
}
