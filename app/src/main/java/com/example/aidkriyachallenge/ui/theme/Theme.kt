package com.example.aidkriyachallenge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable


private val LightColorScheme = lightColorScheme(
    primary = AidTeal,
    onPrimary = OnAidTeal,
    primaryContainer = AidTealLight,
    onPrimaryContainer = AidTealDark,

    secondary = KriyaGold,
    onSecondary = OnKriyaGold,
    secondaryContainer = KriyaGoldLight,
    onSecondaryContainer = KriyaGoldDark,

    tertiary = AidAmber,
    onTertiary = OnAidAmber,
    tertiaryContainer = AidAmberLight,
    onTertiaryContainer = AidAmberDark,

    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,

    inverseSurface = DarkBackground,
    inverseOnSurface = DarkOnBackground,
    inversePrimary = AidTealLight,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = AidTealLight,
    onPrimary = AidTealDark,
    primaryContainer = AidTeal,
    onPrimaryContainer = OnAidTeal,

    secondary = KriyaGoldLight,
    onSecondary = KriyaGoldDark,
    secondaryContainer = KriyaGold,
    onSecondaryContainer = OnKriyaGold,

    tertiary = AidAmberLight,
    onTertiary = AidAmberDark,
    tertiaryContainer = AidAmber,
    onTertiaryContainer = OnAidAmber,

    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,

    inverseSurface = LightBackground,
    inverseOnSurface = LightOnBackground,
    inversePrimary = AidTeal,

    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)


@Composable
fun AidKRIYAChallengeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme =if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}