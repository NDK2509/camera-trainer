package com.example.cameratrainer.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Orange400,
    background = Slate900,
    surface = Slate800,
    onPrimary = Slate900,
    onSecondary = Slate900,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun CameraTrainerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
