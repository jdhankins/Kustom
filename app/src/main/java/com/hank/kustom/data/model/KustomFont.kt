package com.hank.kustom.data.model

/**
 * Top 5 cycling computer typography options matching Karoo, Wahoo, Garmin, Teko, and Roboto displays.
 *
 * @property displayName User-visible font name in UI dropdown menus.
 * @property systemPaths Candidate file system paths on Android devices.
 * @property fallbackFamily Fallback Android system font family name.
 * @property defaultMinWeight Minimum default weight for bold display fonts.
 */
enum class KustomFont(
    /** User-visible font name. */
    val displayName: String,

    /** Candidate system font paths. */
    val systemPaths: List<String>,

    /** Fallback Android system font family. */
    val fallbackFamily: String,

    /** Minimum default weight. */
    val defaultMinWeight: Int,
) {
    /** Native Hammerhead Karoo / Suunto geometric condensed typeface. */
    KAROO_RBNO2(
        displayName = "RBNo2.1a (Karoo)",
        systemPaths = listOf("/system/fonts/RBNo2.1a-Bold.otf", "/system/fonts/RBNo2.1a-Medium.otf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Wahoo ELEMNT low-contrast rounded condensed typeface. */
    WAHOO_BARLOW(
        displayName = "Barlow (Wahoo)",
        systemPaths = listOf("/system/fonts/Barlow-Regular.ttf", "/system/fonts/Barlow-Bold.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 500,
    ),

    /** Technical industrial condensed typeface used on Garmin Edge units. */
    GARMIN_DIN(
        displayName = "DIN (Garmin)",
        systemPaths = listOf("/system/fonts/RobotoCondensed-Bold.ttf", "/system/fonts/RobotoCondensed-Regular.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** High-impact ultra-condensed athletic race typeface. */
    ATHLETIC_DISPLAY(
        displayName = "Teko (Athletic)",
        systemPaths = listOf("/system/fonts/Oswald-Bold.ttf", "/system/fonts/Teko-Bold.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Universal clean neutral neo-grotesque sans-serif. */
    ROBOTO_CLEAN(
        displayName = "Roboto (Clean)",
        systemPaths = listOf("/system/fonts/Roboto-Regular.ttf", "/system/fonts/Roboto-Bold.ttf"),
        fallbackFamily = "sans-serif",
        defaultMinWeight = 400,
    ),
}
