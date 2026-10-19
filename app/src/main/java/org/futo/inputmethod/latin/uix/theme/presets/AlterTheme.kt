package org.futo.inputmethod.latin.uix.theme.presets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.extendedDarkColorScheme
import org.futo.inputmethod.latin.uix.theme.ThemeOption
import org.futo.inputmethod.latin.uix.theme.selector.ThemePreview

// ALTER Keyboard: ALTER's design tokens (ui/theme/Color.kt in the ALTER app). PitchBlack background, SlateGrayCard
// keys, RaisedSurface for pressed and function keys, LightText labels, MutedText hints, AlterYellow as the one accent.
private val alterScheme = extendedDarkColorScheme(
    primary = Color(0xFFFFBD00),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFECECEC),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF18181B),
    onSecondaryContainer = Color(0xFFA1A1AA),
    tertiary = Color(0xFFFFBD00),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF262626),
    onTertiaryContainer = Color(0xFFECECEC),
    error = Color(0xFFEF4444),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF5C1717),
    onErrorContainer = Color(0xFFFFD6D6),
    outline = Color(0xFF71717A),
    outlineVariant = Color(0xFF262626),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFECECEC),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerHighest = Color(0xFF262626),
    shadow = Color(0xFF000000).copy(alpha = 0.7f),
    keyboardSurface = Color(0xFF000000),
    keyboardSurfaceDim = Color(0xFF000000),
    keyboardContainer = Color(0xFF18181B),
    keyboardContainerVariant = Color(0xFF262626),
    onKeyboardContainer = Color(0xFFECECEC),
    keyboardPress = Color(0xFF262626),
    keyboardFade0 = Color(0xFF000000),
    keyboardFade1 = Color(0xFF000000),
    primaryTransparent = Color(0xFFFFBD00).copy(alpha = 0.3f),
    onSurfaceTransparent = Color(0xFFECECEC).copy(alpha = 0.1f),
    hintColor = Color(0xFFA1A1AA),
)

val AlterTheme = ThemeOption(
    dynamic = false,
    key = "AlterTheme",
    name = R.string.theme_alter,
    available = { true }
) {
    alterScheme
}

@Composable
@Preview
private fun PreviewTheme() {
    ThemePreview(AlterTheme)
}
