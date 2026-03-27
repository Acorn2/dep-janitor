package com.depjanitor.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val ObsidianColors = darkColorScheme(
    primary = Color(0xFF7A6FF0),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4FA59A),
    tertiary = Color(0xFFC97A44),
    background = Color(0xFF121317),
    surface = Color(0xFF1B1F26),
    surfaceVariant = Color(0xFF232833),
    onBackground = Color(0xFFEEF1F6),
    onSurface = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFFA7AFBD),
    outline = Color(0xFF2E3542),
)

private val IvoryColors = lightColorScheme(
    primary = Color(0xFF6E63DD),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF468F87),
    tertiary = Color(0xFFB96F3F),
    background = Color(0xFFF4F1EA),
    surface = Color(0xFFFFFCF7),
    surfaceVariant = Color(0xFFF1ECE4),
    onBackground = Color(0xFF1F2430),
    onSurface = Color(0xFF1F2430),
    onSurfaceVariant = Color(0xFF5E6573),
    outline = Color(0xFFD8D0C4),
)

@Immutable
data class SemanticColors(
    val maven: Color,
    val gradle: Color,
    val wrapper: Color,
    val safe: Color,
    val warn: Color,
    val danger: Color,
    val protect: Color,
    val canvasAlt: Color,
)

private val ObsidianSemantics = SemanticColors(
    maven = Color(0xFFC97A44),
    gradle = Color(0xFF4FA59A),
    wrapper = Color(0xFF9B92F7),
    safe = Color(0xFF7BAE84),
    warn = Color(0xFFC79A56),
    danger = Color(0xFFB76579),
    protect = Color(0xFF7F8DA6),
    canvasAlt = Color(0xFF20242C),
)

private val IvorySemantics = SemanticColors(
    maven = Color(0xFFB96F3F),
    gradle = Color(0xFF468F87),
    wrapper = Color(0xFF8C7FF0),
    safe = Color(0xFF6F9E77),
    warn = Color(0xFFB78949),
    danger = Color(0xFFA55C6F),
    protect = Color(0xFF728099),
    canvasAlt = Color(0xFFF7F2EA),
)

val LocalSemanticColors = staticCompositionLocalOf { ObsidianSemantics }

@Composable
fun DepJanitorTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDark = when (mode) {
        ThemeMode.Obsidian -> true
        ThemeMode.Ivory -> false
    }
    val colorScheme = if (useDark || isSystemInDarkTheme() && mode == ThemeMode.Obsidian) ObsidianColors else IvoryColors
    val semanticColors = if (colorScheme == ObsidianColors) ObsidianSemantics else IvorySemantics

    androidx.compose.runtime.CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

val MaterialTheme.semanticColors: SemanticColors
    @Composable
    get() = LocalSemanticColors.current

val ColorScheme.panelBorder: Color
    get() = outline.copy(alpha = 0.7f)
