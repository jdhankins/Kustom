package com.hank.kustom.data.model

/**
 * Standardized zone color formatting options (None, Text, Fill/Background, Bar).
 *
 * @property displayLabel User-visible segment label string.
 */
enum class ZoneFormat(
    /** User-visible segment label. */
    val displayLabel: String,
) {
    /** No zone color formatting applied. */
    NONE("None"),

    /** Applies zone color to value text. */
    TEXT("Text"),

    /** Fills field card background with zone color. */
    BACKGROUND("Fill"),

    /** Renders bottom multi-zone gauge bar. */
    BAR("Bar");

    /**
     * Converts this [ZoneFormat] to its corresponding [ColorMode].
     *
     * @return The matching [ColorMode] (NONE, TEXT_COLOR, or BACKGROUND_COLOR).
     */
    fun toColorMode(): ColorMode {
        return when (this) {
            NONE -> ColorMode.NONE
            TEXT -> ColorMode.TEXT_COLOR
            BACKGROUND -> ColorMode.BACKGROUND_COLOR
            BAR -> ColorMode.NONE
        }
    }

    companion object {
        /**
         * Resolves a [ZoneFormat] from an existing [ColorMode].
         *
         * @param colorMode The [ColorMode] to convert.
         * @return The corresponding [ZoneFormat].
         */
        fun fromColorMode(colorMode: ColorMode): ZoneFormat {
            return when (colorMode) {
                ColorMode.NONE -> NONE
                ColorMode.TEXT_COLOR -> TEXT
                ColorMode.BACKGROUND_COLOR -> BACKGROUND
            }
        }
    }
}
