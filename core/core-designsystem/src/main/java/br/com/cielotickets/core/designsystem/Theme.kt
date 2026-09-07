package br.com.cielotickets.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// `lightColorScheme`/`darkColorScheme` têm default pra TODO parâmetro não
// informado — inclusive pros papéis que o `Card` padrão do Material3 usa
// como container (`surfaceContainerLowest`/`surfaceContainerLow`). Sem
// sobrescrever essa escala inteira, esses papéis ficavam no default de
// fábrica do M3 (derivado de uma seed roxa/rosada), então os cards saíam
// meio "rosados" mesmo com o resto da paleta em azul.
private val LightColors = lightColorScheme(
    primary = CieloColors.Blue600,
    onPrimary = Color.White,
    primaryContainer = CieloColors.Blue100,
    onPrimaryContainer = CieloColors.Blue900,
    secondary = CieloColors.Amber500,
    onSecondary = Color.White,
    secondaryContainer = CieloColors.Amber100,
    onSecondaryContainer = CieloColors.Amber700,
    tertiary = CieloColors.Green500,
    onTertiary = Color.White,
    tertiaryContainer = CieloColors.Green300,
    onTertiaryContainer = CieloColors.Green700,
    background = CieloColors.Neutral50,
    onBackground = CieloColors.Neutral900,
    surface = Color.White,
    onSurface = CieloColors.Neutral900,
    surfaceVariant = CieloColors.Neutral100,
    onSurfaceVariant = CieloColors.Neutral500,
    surfaceDim = CieloColors.Neutral200,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = CieloColors.Neutral50,
    surfaceContainer = CieloColors.Blue50,
    surfaceContainerHigh = CieloColors.Neutral100,
    surfaceContainerHighest = CieloColors.Neutral200,
    inverseSurface = CieloColors.Neutral900,
    inverseOnSurface = CieloColors.Neutral50,
    inversePrimary = CieloColors.Blue300,
    outline = CieloColors.Neutral300,
    outlineVariant = CieloColors.Neutral200,
    error = CieloColors.Red500,
    onError = Color.White,
    errorContainer = CieloColors.Red300,
    onErrorContainer = CieloColors.Red700,
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = CieloColors.Blue300,
    onPrimary = CieloColors.Blue800,
    primaryContainer = CieloColors.Blue700,
    onPrimaryContainer = CieloColors.Blue100,
    secondary = CieloColors.Amber300,
    onSecondary = CieloColors.Amber700,
    secondaryContainer = CieloColors.Amber600,
    onSecondaryContainer = CieloColors.Amber100,
    tertiary = CieloColors.Green300,
    onTertiary = CieloColors.Green700,
    tertiaryContainer = CieloColors.Green700,
    onTertiaryContainer = CieloColors.Green300,
    background = CieloColors.Neutral900,
    onBackground = CieloColors.Neutral100,
    surface = CieloColors.Neutral800,
    onSurface = CieloColors.Neutral100,
    surfaceVariant = CieloColors.Neutral700,
    onSurfaceVariant = CieloColors.Neutral300,
    surfaceDim = CieloColors.Neutral900,
    surfaceBright = CieloColors.Neutral600,
    surfaceContainerLowest = CieloColors.Neutral900,
    surfaceContainerLow = CieloColors.Neutral800,
    surfaceContainer = CieloColors.Neutral800,
    surfaceContainerHigh = CieloColors.Neutral700,
    surfaceContainerHighest = CieloColors.Neutral600,
    inverseSurface = CieloColors.Neutral100,
    inverseOnSurface = CieloColors.Neutral900,
    inversePrimary = CieloColors.Blue600,
    outline = CieloColors.Neutral500,
    outlineVariant = CieloColors.Neutral700,
    error = CieloColors.Red300,
    onError = CieloColors.Red700,
    errorContainer = CieloColors.Red700,
    onErrorContainer = CieloColors.Red300,
    scrim = Color.Black,
)

private val CieloShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun CieloTicketsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, shapes = CieloShapes, content = content)
}
