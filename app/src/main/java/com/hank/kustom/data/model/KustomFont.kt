package com.hank.kustom.data.model

/**
 * Supported 5 font options with system font path fallbacks for Karoo devices.
 */
enum class KustomFont(
    val displayName: String,
    val systemPaths: List<String>,
    val fallbackFamily: String,
    val defaultMinWeight: Int,
) {
    RBNO2_BOLD(
        displayName = "RBNo2.1a-Bold",
        systemPaths = listOf("/system/fonts/RBNo2.1a-Bold.otf", "/system/fonts/RBNo2.1a-Bold.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 700,
    ),
    RBNO2_MEDIUM(
        displayName = "RBNo2.1a-Medium",
        systemPaths = listOf("/system/fonts/RBNo2.1a-Medium.otf", "/system/fonts/RBNo2.1a-Medium.ttf"),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 500,
    ),
    BARLOW(
        displayName = "Barlow",
        systemPaths = listOf(
            "/system/fonts/Barlow-Regular.ttf",
            "/system/fonts/Barlow-Medium.ttf",
            "/system/fonts/Barlow-Bold.ttf"
        ),
        fallbackFamily = "sans-serif-condensed",
        defaultMinWeight = 400,
    ),
    ROBOTO(
        displayName = "Roboto",
        systemPaths = listOf("/system/fonts/Roboto-Regular.ttf", "/system/fonts/Roboto-Bold.ttf"),
        fallbackFamily = "Roboto",
        defaultMinWeight = 400,
    ),
    NOTO_SANS(
        displayName = "Noto Sans",
        systemPaths = listOf("/system/fonts/NotoSans-Regular.ttf", "/system/fonts/NotoSans-Bold.ttf"),
        fallbackFamily = "noto-sans",
        defaultMinWeight = 400,
    ),
}
