package com.lagradost.cloudstream3.utils // Uygulamanızın paket adı

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.cloudstream4.theme.CloudStreamFontTheme
import com.mihon.presentation.settings.collectAsState

/**
 * Ayarlar > Arayüz > App Font seçeneğinin font ailesi karşılığı.
 *
 * TEK KAYNAK: Yeni bir font eklemek için `app/src/main/res/font/` altına dosyayı
 * koyup aşağıdaki `when` bloğuna, `array.xml` içindeki `app_font_values` /
 * `app_font_names` listelerine ve `styles.xml` içindeki ilgili `...FontOverlay`
 * stiline eklemek yeterli.
 *
 * `null` dönerse Compose varsayılan (sistem) fontu kullanılır.
 */
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
        else -> null // "Default" ve bilinmeyen anahtarlar → sistem fontu
    }
}

/**
 * Ayarda seçili font anahtarını reaktif olarak okur.
 * Ayar değiştiğinde yeniden compose edilir; `activity.recreate()` gerekmez.
 */
@Composable
fun rememberAppFontKey(): String {
    val settings = rememberAppSettings()
    val fontKey by settings.ui.appFont.collectAsState()
    return fontKey
}

/** Ayarda seçili fontu reaktif olarak döndürür. */
@Composable
fun rememberAppFontFamily(): FontFamily? = getAppFontFamily(rememberAppFontKey())

/**
 * Tüm Compose ağacına seçili fontu uygulayan sarmalayıcı.
 *
 * Kök ComposeView'lar (`createComposeView`) fontu zaten uygular; bu fonksiyon
 * yeni bir pencere açan Compose içerikleri (diyaloglar, bağımsız ComposeView'lar)
 * için kullanılır: renk şemasına dokunmaz, sadece tipografiyi değiştirir.
 */
@Composable
fun AppFontTheme(
    fontFamily: FontFamily? = rememberAppFontFamily(),
    content: @Composable () -> Unit,
) {
    CloudStreamFontTheme(fontFamily = fontFamily, content = content)
}
