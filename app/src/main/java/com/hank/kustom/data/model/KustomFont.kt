package com.hank.kustom.data.model

/**
 * Premium cycling computer typography options featuring Karoo, Wahoo, Garmin, Coros, and display fonts.
 *
 * @property displayName Clean user-visible font name in UI dropdown menus.
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
    KAROO(
        displayName = "Karoo",
        systemPaths = listOf("/system/fonts/RBNo2.1a-Bold.otf", "/system/fonts/RBNo2.1a-Medium.otf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Wahoo 1: ELEMNT Bolt v1 / Roam v1 Barlow Medium typeface. */
    WAHOO_1(
        displayName = "Wahoo 1",
        systemPaths = listOf("/system/fonts/Barlow-Regular.ttf", "/system/fonts/Barlow-Medium.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 500,
    ),

    /** Wahoo 2: ELEMNT Bolt v2 / Roam v2 Barlow Bold Extra Condensed typeface. */
    WAHOO_2(
        displayName = "Wahoo 2",
        systemPaths = listOf("/system/fonts/Barlow-Bold.ttf", "/system/fonts/Barlow-ExtraBold.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Garmin 1: Legacy Edge 500 / 800 DIN Condensed technical typeface. */
    GARMIN_1(
        displayName = "Garmin 1",
        systemPaths = listOf("/system/fonts/RobotoCondensed-Bold.ttf", "/system/fonts/RobotoCondensed-Regular.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Garmin 2: Modern Edge 530 / 830 / 1040 Highway DIN Alternate typeface. */
    GARMIN_2(
        displayName = "Garmin 2",
        systemPaths = listOf("/system/fonts/RobotoCondensed-Bold.ttf", "/system/fonts/RobotoCondensed-Medium.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Coros Dura: Sharp industrial condensed display typeface. */
    COROS_DURA(
        displayName = "Coros Dura",
        systemPaths = listOf("/system/fonts/RobotoCondensed-Bold.ttf", "/system/fonts/Roboto-Bold.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** High-impact athletic racing display font. */
    ATHLETIC_DISPLAY(
        displayName = "Athletic Display",
        systemPaths = listOf("/system/fonts/Oswald-Bold.ttf", "/system/fonts/Teko-Bold.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Square-tech digital display font. */
    EUROSTILE(
        displayName = "Eurostile",
        systemPaths = listOf("/system/fonts/Roboto-Bold.ttf"),
        fallbackFamily = "sans-serif-smallcaps",
        defaultMinWeight = 700,
    ),

    /** Narrow tall high-density display font. */
    OSWALD(
        displayName = "Oswald",
        systemPaths = listOf("/system/fonts/Oswald-Bold.ttf", "/system/fonts/Oswald-Regular.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Ultra-condensed maximum digit height font. */
    TEKO(
        displayName = "Teko",
        systemPaths = listOf("/system/fonts/Teko-Bold.ttf", "/system/fonts/Teko-Medium.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),

    /** Universal clean neutral sans-serif. */
    ROBOTO(
        displayName = "Roboto",
        systemPaths = listOf("/system/fonts/Roboto-Regular.ttf", "/system/fonts/Roboto-Bold.ttf"),
        fallbackFamily = "sans-serif",
        defaultMinWeight = 400,
    ),
}
