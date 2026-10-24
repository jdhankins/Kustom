package com.hank.kustom.data.model

/**
 * Top 5 cycling computer typography options matching Karoo, Wahoo, Garmin, and athletic race displays.
 */
enum class KustomFont(
    val displayName: String,
    val systemPaths: List<String>,
    val fallbackFamily: String,
    val defaultMinWeight: Int,
) {
    KAROO_RBNO2(
        displayName = "RBNo2.1a (Karoo)",
        systemPaths = listOf("/system/fonts/RBNo2.1a-Bold.otf", "/system/fonts/RBNo2.1a-Medium.otf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),
    WAHOO_BARLOW(
        displayName = "Barlow (Wahoo)",
        systemPaths = listOf("/system/fonts/Barlow-Regular.ttf", "/system/fonts/Barlow-Bold.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 500,
    ),
    GARMIN_DIN(
        displayName = "DIN (Garmin)",
        systemPaths = listOf("/system/fonts/RobotoCondensed-Bold.ttf", "/system/fonts/RobotoCondensed-Regular.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),
    ATHLETIC_DISPLAY(
        displayName = "Teko (Athletic)",
        systemPaths = listOf("/system/fonts/Oswald-Bold.ttf", "/system/fonts/Teko-Bold.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),
    ROBOTO_CLEAN(
        displayName = "Roboto (Clean)",
        systemPaths = listOf("/system/fonts/Roboto-Regular.ttf", "/system/fonts/Roboto-Bold.ttf"),
        fallbackFamily = "sans-serif",
        defaultMinWeight = 400,
    ),
}
