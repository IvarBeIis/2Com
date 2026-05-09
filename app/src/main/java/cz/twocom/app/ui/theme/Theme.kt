package cz.twocom.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4CAF82),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005233),
    onPrimaryContainer = Color(0xFF72FBAE),
    secondary = Color(0xFF4DB6AC),
    background = Color(0xFF0E1512),
    surface = Color(0xFF0E1512),
    surfaceVariant = Color(0xFF1C2B22),
    onBackground = Color(0xFFE2E9E3),
    onSurface = Color(0xFFE2E9E3),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C46),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF72FBAE),
    onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFF006B5F),
    background = Color(0xFFF4FBF4),
    surface = Color(0xFFF4FBF4),
    onBackground = Color(0xFF161D18),
    onSurface = Color(0xFF161D18),
)

@Composable
fun TwoComTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
