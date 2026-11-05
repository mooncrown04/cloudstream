package com.lagradost.cloudstream4.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.getValue
import com.lagradost.cloudstream4.rememberAppSettings
import com.mihon.presentation.settings.collectAsState

/** Ayarda seçili font anahtarını reaktif olarak okur. */
@Composable
fun rememberAppFontKey(): String {
    val settings = rememberAppSettings()
    val fontKey by settings.ui.appFont.collectAsState()
    return fontKey
}

/** Ayarda seçili fontu reaktif olarak döndürür (`null` = sistem fontu). */
@Composable
fun rememberAppFontFamily(fontKey: String? = rememberAppFontKey()): FontFamily? {
    return resolveAppFontFamily(fontKey)
}

/**
 * Font anahtarını platformun sağladığı `FontFamily` çözücüsüne devreder.
 *
 * Eşleme Android tarafında (`com.lagradost.cloudstream3.utils.getAppFontFamily`)
 * tek yerde tutulur; burası ortak (common) kod olduğu için `R.font` erişimi yoktur.
 */
expect fun resolveAppFontFamily(fontKey: String?): FontFamily?

/**
 * Yalnızca tipografiyi (fontu) uygulayan hafif sarmalayıcı.
 *
 * Yeni bir pencere açan Compose içerikleri (diyaloglar, bağımsız ComposeView'lar)
 * üstteki `MaterialTheme`'i miras almadığı için bunu kullanır: renk şemasına
 * dokunmaz, sadece `MaterialTheme.typography`'yi seçili fonta çevirir.
 */
@Composable
fun CloudStreamFontTheme(
    fontFamily: FontFamily? = LocalAppFontFamily.current,
    content: @Composable () -> Unit,
) {
    val typography = rememberAppTypography(fontFamily)
    CompositionLocalProvider(LocalAppFontFamily provides fontFamily) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = typography,
            content = content
        )
    }
}

/**
 * Yeni bir pencere açan Compose içerikleri için font sarmalayıcısı.
 * Sadece ayarlarda seçili fontu uygular; tema/renk tarafına dokunmaz.
 */
@Composable
fun AppFontTheme(
    fontFamily: FontFamily? = rememberAppFontFamily(),
    content: @Composable () -> Unit,
) {
    CloudStreamFontTheme(fontFamily = fontFamily, content = content)
}
