package com.hank.kustom.data.model

/**
 * Color modes for zone and grade color coding across data fields.
 */
enum class ColorMode {
    /** No zone/grade color formatting applied; uses default theme colors. */
    NONE,

    /** Fills the entire data field card background with the corresponding zone or grade color. */
    BACKGROUND_COLOR,

    /** Applies the corresponding zone or grade color directly to the value text. */
    TEXT_COLOR,
}
