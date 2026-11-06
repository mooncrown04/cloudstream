package com.lagradost.cloudstream4.theme

import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import com.lagradost.cloudstream3.utils.getAppFontFamily

/**
 * Android tarafında font anahtarını `res/font/` içindeki dosyalara çözer.
 *
 * Eşleme tek yerde tutulur: `com.lagradost.cloudstream3.utils.getAppFontFamily`
 * (app modülünde, `R.font` erişimi gerektiği için).
 *
 * Hata sessizce yutulmaz: font çözümlenemezse logcat'te görünür, aksi halde
 * "font değişmiyor" sorununun kaynağı bulunamaz.
 */
actual fun resolveAppFontFamily(fontKey: String?): FontFamily? {
    return try {
        getAppFontFamily(fontKey)
    } catch (t: Throwable) {
        Log.e("AppFont", "Font çözümlenemedi: key=$fontKey", t)
        null
    }
}
