package com.lagradost.cloudstream4.theme

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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

@Composable
@ReadOnlyComposable
fun MaterialTheme.infiniteSharedTransition() = LocalSharedInfiniteTransition.current

@Composable
fun CloudStreamPreviewTheme(content: @Composable () -> Unit) {
    CloudStreamTheme(content = content)
}

@Composable
private fun getDynamicTypography(fontFamily: FontFamily?): Typography {
    val base = AppFont.typography
    if (fontFamily == null) return base

    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun CloudStreamTheme(
    mode: CloudStreamThemeMode = CloudStreamThemeMode.FollowSystem,
    primaryColor: CloudStreamPrimaryColor = CloudStreamPrimaryColor.NORMAL,
    // Ayarlarda seçili font ismini alıyoruz (Örn: "ComicSans", "Gotham")
    fontStyleKey: String = remember { settings.ui.appFont.get() },
    fontFamily: FontFamily? = fontFamilies[fontStyleKey], // Map'ten ilgili FontFamily'i çeker
    content: @Composable () -> Unit,
) {
    val csColors = modeToTheme(mode, primaryColor)
    val globalTransition = rememberInfiniteTransition(label = "GlobalSharedTransition")
    
    // Artık seçili fontFamily geçerli olur
    val typography = getDynamicTypography(fontFamily)

    CompositionLocalProvider(LocalSharedInfiniteTransition provides globalTransition) {
        MaterialTheme(
            colorScheme = csColors.toMaterial3ColorScheme(),
            typography = typography,
            content = content
        )
    }
}
