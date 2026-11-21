package app.clearsms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.clearsms.domain.model.ThemeMode

/**
 * Sift's own palette: a mailroom of warm paper and ink.
 *
 * Every role is defined. Material's `lightColorScheme`/`darkColorScheme`
 * fill unspecified roles with the baseline M3 purple, which is how the
 * navigation bar, menus and sheets used to pick up a lavender tint under a
 * teal fallback palette. One warm neutral ramp carries the surfaces, ink
 * carries the text, and a single blue-black accent marks the one live thing
 * on a screen. Wallpaper color remains available as an opt-in (Dynamic
 * color in Settings), never as the default.
 */
private val LightScheme =
    lightColorScheme(
        primary = Color(0xFF2E4A6E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCE6F2),
        onPrimaryContainer = Color(0xFF16283F),
        inversePrimary = Color(0xFFA9C4E8),
        secondary = Color(0xFF6C6355),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE9E0D1),
        onSecondaryContainer = Color(0xFF3A332A),
        tertiary = Color(0xFF7A5A2E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF3E3C9),
        onTertiaryContainer = Color(0xFF46320F),
        background = Color(0xFFF7F4ED),
        onBackground = Color(0xFF1F1B15),
        surface = Color(0xFFFBF8F2),
        onSurface = Color(0xFF1F1B15),
        surfaceVariant = Color(0xFFE8E1D5),
        onSurfaceVariant = Color(0xFF5A5348),
        surfaceTint = Color(0xFF2E4A6E),
        inverseSurface = Color(0xFF33302A),
        inverseOnSurface = Color(0xFFF5F0E6),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        outline = Color(0xFF8C8374),
        outlineVariant = Color(0xFFD9D1C2),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFBF8F2),
        surfaceDim = Color(0xFFDFD9CD),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5F1E9),
        surfaceContainer = Color(0xFFF0EBE1),
        surfaceContainerHigh = Color(0xFFEAE4D8),
        surfaceContainerHighest = Color(0xFFE3DCCD),
    )

private val DarkScheme =
    darkColorScheme(
        primary = Color(0xFFA9C4E8),
        onPrimary = Color(0xFF10263F),
        primaryContainer = Color(0xFF294361),
        onPrimaryContainer = Color(0xFFD7E6F8),
        inversePrimary = Color(0xFF2E4A6E),
        secondary = Color(0xFFCDC3B2),
        onSecondary = Color(0xFF332E26),
        secondaryContainer = Color(0xFF3B352B),
        onSecondaryContainer = Color(0xFFE8DED0),
        tertiary = Color(0xFFDDBB80),
        onTertiary = Color(0xFF422F10),
        tertiaryContainer = Color(0xFF5A4523),
        onTertiaryContainer = Color(0xFFF6E0B5),
        background = Color(0xFF141210),
        onBackground = Color(0xFFEAE3D6),
        surface = Color(0xFF17140F),
        onSurface = Color(0xFFEAE3D6),
        surfaceVariant = Color(0xFF4A443A),
        onSurfaceVariant = Color(0xFFC4BAA9),
        surfaceTint = Color(0xFFA9C4E8),
        inverseSurface = Color(0xFFEAE3D6),
        inverseOnSurface = Color(0xFF33302A),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF968B7B),
        outlineVariant = Color(0xFF4A443A),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF332E26),
        surfaceDim = Color(0xFF141210),
        surfaceContainerLowest = Color(0xFF0F0E0B),
        surfaceContainerLow = Color(0xFF1B1812),
        surfaceContainer = Color(0xFF211D17),
        surfaceContainerHigh = Color(0xFF29241D),
        surfaceContainerHighest = Color(0xFF322C24),
    )

/**
 * App theme: Sift's paper-and-ink palette by default, Material You dynamic
 * color only when the user asks for it (Settings -> Appearance -> Dynamic
 * color, Android 12+).
 */
@Composable
fun ClearSmsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                DarkScheme
            }

            else -> {
                LightScheme
            }
        }
    val semanticAmountColors = if (darkTheme) SemanticAmountColors.Dark else SemanticAmountColors.Light
    CompositionLocalProvider(LocalSemanticAmountColors provides semanticAmountColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ClearSmsTypography,
            shapes = ClearSmsShapes,
            content = content,
        )
    }
}
