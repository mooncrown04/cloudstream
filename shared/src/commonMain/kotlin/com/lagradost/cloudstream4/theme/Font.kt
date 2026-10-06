package com.lagradost.cloudstream4.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.lagradost.cloudstream4.generated.resources.Res
import com.lagradost.cloudstream4.generated.resources.comic_sans
import com.lagradost.cloudstream4.generated.resources.consola
import com.lagradost.cloudstream4.generated.resources.futura
import com.lagradost.cloudstream4.generated.resources.gotham
import com.lagradost.cloudstream4.generated.resources.maybach
import com.lagradost.cloudstream4.generated.resources.naxmos
import com.lagradost.cloudstream4.generated.resources.perfume
import com.lagradost.cloudstream4.generated.resources.productsans_black
import com.lagradost.cloudstream4.generated.resources.productsans_blackitalic
import com.lagradost.cloudstream4.generated.resources.productsans_bold
import com.lagradost.cloudstream4.generated.resources.productsans_bolditalic
import com.lagradost.cloudstream4.generated.resources.productsans_italic
import com.lagradost.cloudstream4.generated.resources.productsans_light
import com.lagradost.cloudstream4.generated.resources.productsans_lightitalic
import com.lagradost.cloudstream4.generated.resources.productsans_medium
import com.lagradost.cloudstream4.generated.resources.productsans_mediumitalic
import com.lagradost.cloudstream4.generated.resources.productsans_regular
import com.lagradost.cloudstream4.generated.resources.productsans_thin
import com.lagradost.cloudstream4.generated.resources.productsans_thinitalic
import com.lagradost.cloudstream4.generated.resources.stix_general
import com.lagradost.cloudstream4.generated.resources.times_new_roman
import org.jetbrains.compose.resources.Font

object AppFont {
    val googleSans @Composable get() = FontFamily(
        Font(Res.font.productsans_thin, weight = FontWeight.W100, style = FontStyle.Normal),
        Font(Res.font.productsans_thinitalic, weight = FontWeight.W100, style = FontStyle.Italic),

        Font(Res.font.productsans_light, weight = FontWeight.W300, style = FontStyle.Normal),
        Font(Res.font.productsans_lightitalic, weight = FontWeight.W300, style = FontStyle.Italic),

        Font(Res.font.productsans_regular, weight = FontWeight.W400, style = FontStyle.Normal),
        Font(Res.font.productsans_italic, weight = FontWeight.W400, style = FontStyle.Italic),

        Font(Res.font.productsans_medium, weight = FontWeight.W500, style = FontStyle.Normal),
        Font(Res.font.productsans_mediumitalic, weight = FontWeight.W500, style = FontStyle.Italic),

        Font(Res.font.productsans_bold, weight = FontWeight.W700, style = FontStyle.Normal),
        Font(Res.font.productsans_bolditalic, weight = FontWeight.W700, style = FontStyle.Italic),

        Font(Res.font.productsans_black, weight = FontWeight.W900, style = FontStyle.Normal),
        Font(Res.font.productsans_blackitalic, weight = FontWeight.W900, style = FontStyle.Italic)
    )

    // İstediğin tüm font ailelerinin tek tek tanımları:
    val timesNewRoman @Composable get() = FontFamily(Font(Res.font.times_new_roman))
    val stixGeneral @Composable get() = FontFamily(Font(Res.font.stix_general))
    val comicSans @Composable get() = FontFamily(Font(Res.font.comic_sans))
    val maybach @Composable get() = FontFamily(Font(Res.font.maybach))
    val perfume @Composable get() = FontFamily(Font(Res.font.perfume))
    val naxmos @Composable get() = FontFamily(Font(Res.font.naxmos))
    val consola @Composable get() = FontFamily(Font(Res.font.consola))
    val futura @Composable get() = FontFamily(Font(Res.font.futura))
    val gotham @Composable get() = FontFamily(Font(Res.font.gotham))

    // İstediğin isimlerle birebir eşleştirilmiş fontFamilies Map'i:
    val fontFamilies @Composable get() = mapOf(
        "Default" to FontFamily.Default,
        "TimesNewRoman" to timesNewRoman,
        "StixGeneral" to stixGeneral,
        "ComicSans" to comicSans,
        "Maybach" to maybach,
        "Perfume" to perfume,
        "Naxmos" to naxmos,
        "Consola" to consola,
        "Futura" to futura,
        "GoogleSans" to googleSans,
        "Gotham" to gotham
    )

    private val defaultTypography = Typography()
    val typography @Composable get() =
        googleSans.let { fontFamily ->
            val lineHeight = 1.3.em
            Typography(
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
}
