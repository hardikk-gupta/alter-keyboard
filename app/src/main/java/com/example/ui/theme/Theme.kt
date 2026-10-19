package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AlterColorScheme = darkColorScheme(
  primary = AlterYellow,
  onPrimary = PitchBlack,
  primaryContainer = SlateGrayCard,
  onPrimaryContainer = LightText,
  secondary = MutedText,
  onSecondary = PitchBlack,
  background = PitchBlack,
  onBackground = LightText,
  surface = SlateGrayCard,
  onSurface = LightText,
  surfaceVariant = RaisedSurface,
  onSurfaceVariant = MutedText,
  error = ErrorRed,
  onError = LightText
)

@Composable
fun AlterKeyboardTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = AlterColorScheme,
    typography = Typography,
    content = content
  )
}
