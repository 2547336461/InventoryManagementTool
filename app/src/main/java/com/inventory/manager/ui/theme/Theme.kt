package com.inventory.manager.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = InStockColor,
    background = SurfaceVariant,
    surface = androidx.compose.ui.graphics.Color.White
)

@Composable
fun InventoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
