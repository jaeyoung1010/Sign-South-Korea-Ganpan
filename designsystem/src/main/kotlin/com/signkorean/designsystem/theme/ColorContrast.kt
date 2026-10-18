package com.signkorean.designsystem.theme

import kotlin.math.pow

object ColorContrast {
    fun ratio(
        foreground: Long,
        background: Long,
    ): Double {
        val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
        val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))

        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Long): Double {
        val red = (color shr 16 and 0xFF).toDouble() / 255
        val green = (color shr 8 and 0xFF).toDouble() / 255
        val blue = (color and 0xFF).toDouble() / 255

        return 0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
    }

    private fun linearize(channel: Double): Double = if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
