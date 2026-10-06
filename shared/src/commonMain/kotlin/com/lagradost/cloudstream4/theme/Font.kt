package com.lagradost.cloudstream4.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.lagradost.cloudstream3.R

object AppFont {
    // Android R.font kaynakları üzerinden Google Sans tanımı
    val googleSans = FontFamily(
        Font(R.font.google_sans, weight = FontWeight.Normal, style = FontStyle.Normal)
    )

    // res/font/ klasöründeki .ttf / .xml kaynakları ile birebir eşleşen Font Aileleri
    val fontFamilies = mapOf(
        "Default" to FontFamily.Default,
        "TimesNewRoman" to FontFamily(Font(R.font.times_new_roman)),
        "StixGeneral" to FontFamily(Font(R.font.stix_general)),
        "ComicSans" to FontFamily(Font(R.font.comic_sans)),
        "Maybach" to FontFamily(Font(R.font.maybach)),
        "Perfume" to FontFamily(Font(R.font.perfume)),
        "Naxmos" to FontFamily(Font(R.font.naxmos)),
        "Consola" to FontFamily(Font(R.font.consola)),
        "Futura" to FontFamily(Font(R.font.futura)),
        "GoogleSans" to googleSans,
        "Gotham" to FontFamily(Font(R.font.gotham))
    )

    private val defaultTypography = Typography()
    
    // Temanın dinamik olarak seçilen fontu veya varsayılan fontu almasını sağlayan fonksiyon
    fun getTypography(fontFamily: FontFamily = googleSans): Typography {
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

    val typography @Composable get() = getTypography(googleSans)
}
