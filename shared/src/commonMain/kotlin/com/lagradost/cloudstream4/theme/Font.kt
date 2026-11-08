package com.lagradost.cloudstream4.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.em

object AppFont {
    private val defaultTypography = Typography()

    fun getTypography(fontFamily: FontFamily? = null): Typography {
        val targetFontFamily = fontFamily ?: FontFamily.Default
        val lineHeight = 1.3.em
        return Typography(
            displayLarge = defaultTypography.displayLarge.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            displayMedium = defaultTypography.displayMedium.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            displaySmall = defaultTypography.displaySmall.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),

            headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),

            titleLarge = defaultTypography.titleLarge.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            titleMedium = defaultTypography.titleMedium.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            titleSmall = defaultTypography.titleSmall.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),

            bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            bodySmall = defaultTypography.bodySmall.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),

            labelLarge = defaultTypography.labelLarge.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            labelMedium = defaultTypography.labelMedium.copy(fontFamily = targetFontFamily, lineHeight = lineHeight),
            labelSmall = defaultTypography.labelSmall.copy(fontFamily = targetFontFamily, lineHeight = lineHeight)
        )
    }

    val typography @Composable get() = getTypography()
}


@Composable
internal fun rememberAppTypography(fontFamily: FontFamily?): Typography {
    return if (fontFamily == null) {
        Typography()
    } else {
        AppFont.getTypography(fontFamily)
    }
}

/**
 * `MaterialTheme.typography`'deki HER stile seçili fontu uygular.
 *
 * Neden gerekli: ekranlarda `MaterialTheme.typography.bodyMedium.copy(fontSize = …)`
 * gibi `.copy()` çağrıları yaygın; `.copy()` çağrılırken `fontFamily` parametresi
 * açıkça verilmezse stil kendi alanını korur, ancak Material3'te bu alan çoğu
 * yerde `FontFamily.Default` olarak saklandığı için üstten gelen tema fontu
 * beklenmedik biçimde kaybolabiliyor. Burada her stil tek tek kopyalanıp
 * `fontFamily` açıkça set edilir.
 */
@Composable
internal fun Typography.withFontFamily(fontFamily: FontFamily?): Typography {
    if (fontFamily == null) return this

    fun TextStyle.withFont() = copy(fontFamily = fontFamily)

    return Typography(
        displayLarge = displayLarge.withFont(),
        displayMedium = displayMedium.withFont(),
        displaySmall = displaySmall.withFont(),
        headlineLarge = headlineLarge.withFont(),
        headlineMedium = headlineMedium.withFont(),
        headlineSmall = headlineSmall.withFont(),
        titleLarge = titleLarge.withFont(),
        titleMedium = titleMedium.withFont(),
        titleSmall = titleSmall.withFont(),
        bodyLarge = bodyLarge.withFont(),
        bodyMedium = bodyMedium.withFont(),
        bodySmall = bodySmall.withFont(),
        labelLarge = labelLarge.withFont(),
        labelMedium = labelMedium.withFont(),
        labelSmall = labelSmall.withFont(),
    )
}
