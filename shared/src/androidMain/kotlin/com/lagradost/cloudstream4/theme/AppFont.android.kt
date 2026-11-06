package com.lagradost.cloudstream4.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext

/**
 * Android tarafında font anahtarını `res/font/` içindeki dosyalara çözer.
 *
 * `shared` modülü app modülüne bağımlı OLMADIĞI için app'teki `R.font`
 * kullanılamaz; bu yüzden font dosyaları kaynak adıyla (dosya adı) yüklenir.
 * Anahtar -> dosya adı eşlemesi `appFontFileNames` içinde tek yerde tutulur.
 *
 * `LocalContext` yalnızca @Composable fonksiyonlarda okunabildiği için çözümleme
 * composable sarmalayıcı üzerinden yapılır.
 */
@androidx.compose.runtime.Composable
actual fun resolveAppFontFamily(fontKey: String?): FontFamily? {
    val fileName = appFontFileNames[fontKey] ?: return null
    val context = LocalContext.current

    return try {
        val resId = context.resources.getIdentifier(fileName, "font", context.packageName)
        if (resId == 0) null else FontFamily(Font(resId))
    } catch (t: Throwable) {
        null
    }
}

/** Anahtar -> `app/src/main/res/font/` altındaki dosya adı (uzantısız). */
val appFontFileNames: Map<String, String> = mapOf(
    "TimesNewRoman" to "times_new_roman",
    "StixGeneral" to "stix_general",
    "ComicSans" to "comic_sans",
    "Maybach" to "maybach",
    "Perfume" to "perfume",
    "Naxmos" to "naxmos",
    "Consola" to "consola",
    "Futura" to "futura",
    "GoogleSans" to "google_sans",
    "Gotham" to "gotham",
)
