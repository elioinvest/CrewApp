package app.crew.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Icons as SVG path data on a 24×24 grid, stroked like the web version's (and Crew's) icons.
object Ico {
    const val SIDEBAR = "M6 4h12a2.5 2.5 0 0 1 2.5 2.5v11a2.5 2.5 0 0 1-2.5 2.5H6a2.5 2.5 0 0 1-2.5-2.5v-11A2.5 2.5 0 0 1 6 4zM9.5 4v16"
    const val NEW = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17zM12 8.5v7M8.5 12h7"
    const val PLUS = "M12 5v14M5 12h14"
    const val UP = "M12 19V5M6 11l6-6 6 6"
    const val STOP = "M7.5 5h9a2.5 2.5 0 0 1 2.5 2.5v9a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 5 16.5v-9A2.5 2.5 0 0 1 7.5 5z"
    const val CARET = "M6 9l6 6 6-6"
    const val CHECK = "M5 12.5l4.5 4.5L19 7.5"
    const val CLOCK = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17zM12 7.5V12l3 2"
    const val MONITOR = "M5.5 4.5h13a2 2 0 0 1 2 2V14a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2V6.5a2 2 0 0 1 2-2zM8.5 20h7M12 16v4"
    const val SEARCH = "M11 4.5a6.5 6.5 0 1 1 0 13a6.5 6.5 0 1 1 0-13zM16 16l4 4"
    const val FOLDER = "M5 5h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"
    const val SERVER = "M4.5 4h15A1.5 1.5 0 0 1 21 5.5v4a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 9.5v-4A1.5 1.5 0 0 1 4.5 4zM4.5 13h15a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5v-4A1.5 1.5 0 0 1 4.5 13zM7 7.5h.01M7 16.5h.01"
    const val SHELL = "M5 8l4 4-4 4M12 17h7"
    const val SLASH = "M15 4L9 20"
    const val KEYS = "M5 6h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2zM7 10h.01M11 10h.01M15 10h.01M17 10h.01M7.5 14h9"
    const val COPY = "M9 9h9a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2zM5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"
    const val TOOL = "M5 8l4 4-4 4M12 17h7"
    // Claude's mark and the OpenAI mark (filled), as Crew's sidebar shows them.
    const val CLAUDE = "M12 3v18M3 12h18M5.6 5.6l12.8 12.8M18.4 5.6 5.6 18.4"
    const val OPENAI = "M22.28 9.82a5.98 5.98 0 0 0-.52-4.91 6.05 6.05 0 0 0-6.51-2.9A6.07 6.07 0 0 0 4.98 4.18a5.98 5.98 0 0 0-4 2.9 6.05 6.05 0 0 0 .75 7.1 5.98 5.98 0 0 0 .51 4.91 6.05 6.05 0 0 0 6.51 2.9A5.98 5.98 0 0 0 13.26 24a6.06 6.06 0 0 0 5.77-4.21 5.99 5.99 0 0 0 4-2.9 6.06 6.06 0 0 0-.75-7.07zm-9.02 12.61a4.48 4.48 0 0 1-2.88-1.04l.14-.08 4.78-2.76a.79.79 0 0 0 .39-.68v-6.74l2.02 1.17a.07.07 0 0 1 .04.05v5.58a4.5 4.5 0 0 1-4.49 4.5zm-9.66-4.13a4.47 4.47 0 0 1-.53-3.01l.14.09 4.78 2.76a.77.77 0 0 0 .78 0l5.84-3.37v2.33a.08.08 0 0 1-.03.06L9.74 19.95a4.5 4.5 0 0 1-6.14-1.65zM2.34 7.9a4.49 4.49 0 0 1 2.37-1.97v5.67a.77.77 0 0 0 .39.68l5.81 3.35-2.02 1.17a.08.08 0 0 1-.07 0l-4.83-2.79A4.5 4.5 0 0 1 2.34 7.87zm16.6 3.86-5.84-3.39 2.02-1.16a.08.08 0 0 1 .07 0l4.83 2.79a4.49 4.49 0 0 1-.68 8.1v-5.68a.79.79 0 0 0-.4-.67zm2.01-3.02-.14-.09-4.77-2.78a.78.78 0 0 0-.79 0L9.41 9.23V6.9a.07.07 0 0 1 .03-.06l4.83-2.79a4.5 4.5 0 0 1 6.68 4.66zM8.31 12.86l-2.02-1.16a.08.08 0 0 1-.04-.06V6.07a4.5 4.5 0 0 1 7.38-3.45l-.14.08L8.7 5.46a.79.79 0 0 0-.39.68zm1.1-2.37 2.6-1.5 2.61 1.5v3l-2.6 1.5-2.61-1.5z"
}

@Composable
fun Icon(path: String, color: Color, size: Dp = 24.dp, stroke: Float = 1.6f, filled: Boolean = false, modifier: Modifier = Modifier) {
    val p = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val scaled = androidx.compose.ui.graphics.Path().apply { addPath(p); transform(Matrix().apply { scale(s, s) }) }
        if (filled) drawPath(scaled, color, style = Fill)
        else drawPath(scaled, color, style = Stroke(width = stroke * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// The agent's mark: Claude's orange star, the OpenAI mark, a provider's emoji or first letter.
@Composable
fun AgentMark(agent: String, provider: app.crew.mobile.Provider?, size: Dp = 14.dp) {
    val p = LocalPalette.current
    when {
        provider != null -> if (provider.icon != null) Text(provider.icon, fontSize = (size.value * 0.9f).sp)
        else Box(Modifier.size(size).clip(RoundedCornerShape(4.dp)).background(letterColor(provider.id.ifEmpty { provider.name })), contentAlignment = Alignment.Center) {
            Text(provider.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = androidx.compose.ui.text.TextStyle(
                fontSize = (size.value * 0.62f).sp, lineHeight = (size.value * 0.62f).sp,
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.Both)))
        }
        agent == "codex" -> Icon(Ico.OPENAI, p.text, size, filled = true)
        agent == "ssh" -> Icon(Ico.SHELL, p.green, size, stroke = 2f)
        else -> Icon(Ico.CLAUDE, p.accent, size, stroke = 2.2f)
    }
}

// A provider's letter tile, colored from its id as Crew colors it (hsl(h 55% 48%)).
fun letterColor(key: String): Color {
    var h = 0
    for (c in key) h = (h * 31 + c.code) % 360
    return Color.hsl(h.toFloat(), 0.55f, 0.48f)
}

// A bottom sheet with a list of choices, the current one ticked.
data class Choice(val label: String, val sub: String = "", val checked: Boolean = false, val divider: Boolean = false, val icon: String? = null, val run: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceSheet(title: String, choices: List<Choice>, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = p.bg, dragHandle = {
        Box(Modifier.padding(top = 10.dp, bottom = 6.dp).width(36.dp).heightIn(4.dp).clip(CircleShape).background(p.line))
    }) {
        // A third of the screen at most; longer lists scroll.
        Column(Modifier.fillMaxWidth().heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp / 3).dp).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            if (title.isNotEmpty()) Text(title, color = p.muted, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
            for (c in choices) {
                if (c.divider) HorizontalDivider(color = p.line, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onDismiss(); c.run() }.heightIn(min = 50.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    c.icon?.let { Icon(it, p.muted, 22.dp) }
                    Column(Modifier.weight(1f)) {
                        Text(c.label, color = p.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (c.sub.isNotEmpty()) Text(c.sub, color = p.muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (c.checked) Icon(Ico.CHECK, p.blue, 22.dp, stroke = 2f)
                }
            }
            Spacer(Modifier.size(4.dp))
        }
    }
}

// Crew's short times: now, 5m, 3h, 2d.
fun relTime(ms: Long): String {
    if (ms <= 0) return ""
    val m = if (ms < 1_000_000_000_000L) ms * 1000 else ms
    val sec = (System.currentTimeMillis() - m) / 1000
    return when {
        sec < 60 -> app.crew.mobile.t("now")
        sec < 3600 -> app.crew.mobile.t("%dm", (sec / 60).toInt())
        sec < 86400 -> app.crew.mobile.t("%dh", (sec / 3600).toInt())
        else -> app.crew.mobile.t("%dd", (sec / 86400).toInt())
    }
}
