package com.lagradost.cloudstream4.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.em

object AppFont {
    private val defaultTypography = Typography()

    /**
     * Dışarıdan verilen FontFamily'e göre Typography nesnesi üretir.
     */
    fun getTypography(fontFamily: FontFamily = FontFamily.Default): Typography {
        val lineHeight = 1.3.em
        return Typography(
            displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily, lineHeight = lineHeight),

            headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily, lineHeight = lineHeight),

            titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily, lineHeight = lineHeight),

            bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily, lineHeight = lineHeight),

            labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily, lineHeight = lineHeight),
            labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily, lineHeight = lineHeight)
        )
    }

    val typography @Composable get() = getTypography(FontFamily.Default)
}
