package com.lagradost.cloudstream4.theme

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class CloudStreamThemeMode {
    Dark,
    Amoled,
    AmoledLight,
    Light,
    Dracula,
    Lavender,
    SilentBlue,
    Galatasaray,
    CimBom,
    GS,
    Rengarenk,
    FollowSystem,
    Dynamic,
}

@Composable
fun modeToTheme(mode : CloudStreamThemeMode, primaryColor: CloudStreamPrimaryColor) : CloudStreamColorScheme {
    val dynamicTheme = DeviceTheme.resolveDynamicTheme()
    val dynamicPrimary = DeviceTheme.resolveDynamicPrimaryColor()
    val dynamicSecondary = DeviceTheme.resolveDynamicSecondaryColor()
    val systemDark = isSystemInDarkTheme()
    val color = remember(mode, primaryColor, systemDark, dynamicTheme, dynamicPrimary, dynamicSecondary) {
        val base = when (mode) {
            CloudStreamThemeMode.Dark -> darkScheme()
            CloudStreamThemeMode.Amoled -> amoledScheme()
            CloudStreamThemeMode.AmoledLight -> amoledLightScheme()
            CloudStreamThemeMode.Light -> lightScheme()
            CloudStreamThemeMode.Galatasaray -> galatasarayScheme()
            CloudStreamThemeMode.CimBom -> cimBomScheme()
            CloudStreamThemeMode.GS -> gsScheme()
            CloudStreamThemeMode.Rengarenk -> rengarenkScheme()
            CloudStreamThemeMode.Dracula -> draculaScheme()
            CloudStreamThemeMode.Lavender -> lavenderScheme()
            CloudStreamThemeMode.SilentBlue -> silentBlueScheme()
            CloudStreamThemeMode.FollowSystem -> if (systemDark) darkScheme() else lightScheme()
            CloudStreamThemeMode.Dynamic -> dynamicTheme
        }
        when {
            mode == CloudStreamThemeMode.Dynamic -> base
            primaryColor == CloudStreamPrimaryColor.DYNAMIC -> base.copy(primary = dynamicPrimary)
            primaryColor == CloudStreamPrimaryColor.DYNAMIC_TWO -> base.copy(primary = dynamicSecondary)
            else -> base.copy(primary = primaryColor.color)
        }
    }
    return color
}

private fun CloudStreamColorScheme.toMaterial3ColorScheme() = if (isLight) {
    lightColorScheme(
        primary = primary,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        surfaceContainer = surfaceContainer,
        onBackground = onBackground,
        onSurface = onBackground,
        onSurfaceVariant = onSurfaceVariant,
        onPrimary = Color.White,
    )
} else {
    darkColorScheme(
        primary = primary,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        surfaceContainer = surfaceContainer,
        onBackground = onBackground,
        onSurface = onBackground,
        onSurfaceVariant = onSurfaceVariant,
        onPrimary = Color.White,
    )
}

internal val LocalSharedInfiniteTransition = staticCompositionLocalOf<InfiniteTransition> { throw NotImplementedError() }

/**
 * Kullanıcının Ayarlar > Arayüz > App Font seçiminden çözülen font ailesi.
 *
 * Kök Compose ağacına [CloudStreamTheme] ile verilir; yeni bir pencere açan
 * bileşenler (ör. diyaloglar) üstteki MaterialTheme'i miras almadığı için bu
 * composition local'den okuyup kendilerine uygular.
 *
 * `null` = seçili font yok / "Default" → Compose varsayılan fontu kullanılır.
 */
val LocalAppFontFamily = staticCompositionLocalOf<FontFamily?> { null }

@Composable
@ReadOnlyComposable
fun MaterialTheme.infiniteSharedTransition() = LocalSharedInfiniteTransition.current

@Composable
fun CloudStreamPreviewTheme(content: @Composable () -> Unit) {
    CloudStreamTheme(content = content)
}

/**
 * Seçili fontu Material3 tipografisine çevirir (asıl tanım AppFont.kt içindedir).
 */
@Composable
private fun getDynamicTypography(fontFamily: FontFamily?): Typography {
    return rememberAppTypography(fontFamily)
}

/**
 * Uygulamanın kök teması.
 *
 * [fontFamily] verilmezse (veya `null` ise) Ayarlar > Arayüz > App Font tercihi
 * reaktif olarak okunur; böylece tema çağrısını değiştirmeye gerek kalmadan
 * tüm Compose ağacı seçili fontu alır.
 */
@Composable
fun CloudStreamTheme(
    mode: CloudStreamThemeMode = CloudStreamThemeMode.FollowSystem,
    primaryColor: CloudStreamPrimaryColor = CloudStreamPrimaryColor.NORMAL,
    fontFamily: FontFamily? = rememberAppFontFamily(),
    content: @Composable () -> Unit,
) {
    val csColors = modeToTheme(mode, primaryColor)
    val globalTransition = rememberInfiniteTransition(label = "GlobalSharedTransition")
    val typography = getDynamicTypography(fontFamily)

    CompositionLocalProvider(
        LocalSharedInfiniteTransition provides globalTransition,
        LocalAppFontFamily provides fontFamily
    ) {
        MaterialTheme(
            colorScheme = csColors.toMaterial3ColorScheme(),
            typography = typography,
        ) {
            // Stil belirtmeyen Text() çağrıları için de varsayılanı ayarla.
            ProvideTextStyle(typography.bodyMedium, content = content)
        }
    }
}
