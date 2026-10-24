package com.hank.kustom.data.model

/**
 * Standardized zone and color formatting options.
 */
enum class ZoneFormat(val displayLabel: String) {
    NONE("NONE"),
    TEXT("Text"),
    BACKGROUND("Background");

    fun toColorMode(): ColorMode {
        return when (this) {
            NONE -> ColorMode.NONE
            TEXT -> ColorMode.TEXT_COLOR
            BACKGROUND -> ColorMode.BACKGROUND_COLOR
        }
    }

    companion object {
        fun fromColorMode(colorMode: ColorMode): ZoneFormat {
            return when (colorMode) {
                ColorMode.NONE -> NONE
                ColorMode.TEXT_COLOR -> TEXT
                ColorMode.BACKGROUND_COLOR -> BACKGROUND
            }
        }
    }
}
