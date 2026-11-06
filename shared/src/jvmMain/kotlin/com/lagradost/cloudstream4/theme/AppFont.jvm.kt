package com.lagradost.cloudstream4.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Masaüstü (JVM) hedefinde uygulamaya özel fontlar paketlenmediği için
 * her zaman sistem fontuna (`null`) düşer.
 */
@Composable
actual fun resolveAppFontFamily(fontKey: String?): FontFamily? = null
