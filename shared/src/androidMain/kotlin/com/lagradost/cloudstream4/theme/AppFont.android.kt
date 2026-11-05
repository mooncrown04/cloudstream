package com.lagradost.cloudstream4.theme

import androidx.compose.ui.text.font.FontFamily
import com.lagradost.cloudstream3.utils.getAppFontFamily

/**
 * Android tarafında font anahtarını `res/font/` içindeki dosyalara çözer.
 *
 * Eşleme tek yerde tutulur: `com.lagradost.cloudstream3.utils.getAppFontFamily`
 * (app modülünde, `R.font` erişimi gerektiği için). `shared` modülü app'e bağımlı
 * olmadığından bu referans yalnızca Android hedefinde çözümlenir.
 */
actual fun resolveAppFontFamily(fontKey: String?): FontFamily? {
    return runCatching { getAppFontFamily(fontKey) }.getOrNull()
}
