package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class TelaJogoVisualTokens(
    val oledPowerSaver: Boolean = true,
    val hairlineBorder: Color = OledHairlineBorder,
    val mutedText: Color = OledMutedText,
    val accentCyan: Color = CyanPulse,
    val accentEmerald: Color = EmeraldSignal,
    val accentAmber: Color = AmberAlert,
    val accentCrimson: Color = CrimsonGuard,
    val accentViolet: Color = VioletOrbital,
)

val LocalTelaJogoTokens = staticCompositionLocalOf { TelaJogoVisualTokens() }

private val OledPureBlackColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = OledPureBlack,
    primaryContainer = Color(0xFF002429),
    onPrimaryContainer = CyanPulse,
    secondary = EmeraldSignal,
    onSecondary = OledPureBlack,
    secondaryContainer = Color(0xFF002615),
    onSecondaryContainer = EmeraldSignal,
    tertiary = AmberAlert,
    onTertiary = OledPureBlack,
    background = OledPureBlack,
    onBackground = PureWhite,
    surface = OledPureBlack,
    onSurface = PureWhite,
    surfaceVariant = OledElevatedBlack,
    onSurfaceVariant = OledMutedText,
    outline = OledHairlineBorder,
    error = CrimsonGuard,
    onError = PureWhite,
)

private val StandardDarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = OledPureBlack,
    primaryContainer = Color(0xFF0D3540),
    onPrimaryContainer = CyanPulse,
    secondary = EmeraldSignal,
    onSecondary = OledPureBlack,
    secondaryContainer = Color(0xFF0D3322),
    onSecondaryContainer = EmeraldSignal,
    tertiary = AmberAlert,
    onTertiary = OledPureBlack,
    background = StandardDarkBg,
    onBackground = SoftWhite,
    surface = StandardDarkSurface,
    onSurface = SoftWhite,
    surfaceVariant = StandardDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA0AEC0),
    outline = StandardHairlineBorder,
    error = CrimsonGuard,
    onError = PureWhite,
)

@Composable
fun TelaJogoTheme(
    oledPowerSaver: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (oledPowerSaver) OledPureBlackColorScheme else StandardDarkColorScheme
    val tokens = TelaJogoVisualTokens(
        oledPowerSaver = oledPowerSaver,
        hairlineBorder = if (oledPowerSaver) OledHairlineBorder else StandardHairlineBorder,
        mutedText = if (oledPowerSaver) OledMutedText else Color(0xFFA0AEC0),
    )

    CompositionLocalProvider(LocalTelaJogoTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TelaJogoTypography,
            content = content,
        )
    }
}
