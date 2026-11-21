package com.writeright.ai.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WriteRightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = CanvasWhite,
    secondary = SuccessGreen,
    background = PageBackground,
    surface = CanvasWhite,
    onBackground = Ink,
    onSurface = Ink
)

@Composable
fun WriteRightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WriteRightColors,
        content = content
    )
}
