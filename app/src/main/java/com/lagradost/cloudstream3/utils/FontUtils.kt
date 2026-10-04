package com.lagradost.cloudstream3.utils // Uygulamanızın paket adı

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.lagradost.cloudstream3.R

fun getAppFontFamily(fontKey: String?): FontFamily? {
    return when (fontKey) {
        "TimesNewRoman" -> FontFamily(Font(R.font.times_new_roman))
        "StixGeneral" -> FontFamily(Font(R.font.stix_general))
        "ComicSans" -> FontFamily(Font(R.font.comic_sans))
        "Maybach" -> FontFamily(Font(R.font.maybach))
        "Perfume" -> FontFamily(Font(R.font.perfume))
        "Naxmos" -> FontFamily(Font(R.font.naxmos))
        "Consola" -> FontFamily(Font(R.font.consola))
        "Futura" -> FontFamily(Font(R.font.futura))
        "GoogleSans" -> FontFamily(Font(R.font.google_sans))
        "Gotham" -> FontFamily(Font(R.font.gotham))
        else -> null
    }
}
