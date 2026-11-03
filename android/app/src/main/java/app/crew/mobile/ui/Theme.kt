package app.crew.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The web version's colors: a white page, near-black text, a blue accent; the drawer in Crew's
// own warm greys with its orange accent.
data class Palette(
    val bg: Color, val text: Color, val muted: Color, val faint: Color, val line: Color, val hover: Color, val fill: Color,
    val blue: Color, val blueSoft: Color, val blueBg: Color, val orange: Color, val orangeBg: Color, val green: Color, val red: Color,
    val side: Color, val sideHover: Color, val sideActive: Color, val sideBorder: Color, val accent: Color, val sideMuted: Color,
    val bubble: Color, val code: Color,
)

val Light = Palette(
    bg = Color(0xFFFFFFFF), text = Color(0xFF1B1C1F), muted = Color(0xFF8E9198), faint = Color(0xFFB6B9BF), line = Color(0xFFEBECEF),
    hover = Color(0xFFECEEF2), fill = Color(0xFFF2F3F5), blue = Color(0xFF4D6BFE), blueSoft = Color(0xFFA9BBFC), blueBg = Color(0xFFEAEFFE),
    orange = Color(0xFFE2861B), orangeBg = Color(0xFFFFF3E2), green = Color(0xFF2FA45B), red = Color(0xFFE5484D),
    side = Color(0xFFF9F8F6), sideHover = Color(0xFFEFEDE9), sideActive = Color(0xFFE7E4DF), sideBorder = Color(0xFFEBE8E3),
    accent = Color(0xFFD97757), sideMuted = Color(0xFF8A8580), bubble = Color(0xFFEDF2FE), code = Color(0xFFF4F5F7),
)

val Dark = Palette(
    bg = Color(0xFF161718), text = Color(0xFFE9EAEC), muted = Color(0xFF8B8E94), faint = Color(0xFF5D6066), line = Color(0xFF2A2C2F),
    hover = Color(0xFF2A2C30), fill = Color(0xFF26282B), blue = Color(0xFF6C86FF), blueSoft = Color(0xFF3B4A85), blueBg = Color(0xFF22294A),
    orange = Color(0xFFF0A142), orangeBg = Color(0xFF3A2C18), green = Color(0xFF3FA45B), red = Color(0xFFE5484D),
    side = Color(0xFF1F1F1F), sideHover = Color(0xFF2A2A2A), sideActive = Color(0xFF333333), sideBorder = Color(0xFF2C2C2C),
    accent = Color(0xFFD97757), sideMuted = Color(0xFF8D8984), bubble = Color(0xFF22294A), code = Color(0xFF202124),
)

val LocalPalette = staticCompositionLocalOf { Light }

@Composable
fun CrewTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) Dark else Light
    val scheme = if (dark) darkColorScheme(primary = p.blue, background = p.bg, surface = p.bg, onSurface = p.text, onBackground = p.text)
    else lightColorScheme(primary = p.blue, background = p.bg, surface = p.bg, onSurface = p.text, onBackground = p.text)
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
