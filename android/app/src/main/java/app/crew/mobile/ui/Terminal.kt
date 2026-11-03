package app.crew.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.crew.mobile.CrewClient
import app.crew.mobile.TermRun
import app.crew.mobile.t

// A host's terminal, as on the computer: Crew sends its screen (text and colors) as it changes,
// with the terminal sized to the phone while it's shown here. Commands are typed below, a line
// at a time; the keys row sends what a command line can't.
private val TERM_SIZE = 13.sp
private val TERM_LINE = 17.sp

@Composable
fun TerminalView(client: CrewClient, termId: String) {
    val p = LocalPalette.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = TERM_SIZE, lineHeight = TERM_LINE)
    // One character's width, to size the terminal to the screen.
    val charW = remember { measurer.measure("M".repeat(20), style).size.width / 20f }
    val list = rememberLazyListState()
    DisposableEffect(termId) { onDispose { client.unwatch() } }

    BoxWithConstraints(Modifier.fillMaxSize().background(p.bg).padding(horizontal = 8.dp)) {
        val cols = with(density) { (maxWidth.toPx() / charW).toInt() }.coerceAtLeast(20)
        val rows = with(density) { (maxHeight.toPx() / TERM_LINE.toPx()).toInt() }.coerceAtLeast(8)
        LaunchedEffect(termId, cols, rows) { client.watch(termId, cols, rows) }
        // Follow the output, as a terminal does.
        LaunchedEffect(client.termLines.size, client.termCursor) { if (client.termLines.isNotEmpty()) list.scrollToItem(client.termLines.size - 1) }
        LazyColumn(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()), state = list) {
            itemsIndexed(client.termLines) { y, runs ->
                val cursor = client.termCursor
                Text(buildAnnotatedString {
                    var x = 0
                    for (r in runs) {
                        withStyle(SpanStyle(color = r.color?.let { termColor(it, p) } ?: p.text, fontWeight = if (r.bold) FontWeight.Bold else FontWeight.Normal)) { append(r.text) }
                        x += r.text.length
                    }
                    // The cursor, as a block on its line.
                    if (cursor != null && cursor.second == y) {
                        if (cursor.first > x) append(" ".repeat(cursor.first - x))
                        withStyle(SpanStyle(background = p.muted.copy(alpha = 0.5f))) { append(" ") }
                    }
                }, style = style, softWrap = false)
            }
        }
    }
}

// Terminal colors are made for a dark background; keep them readable on a light one.
private fun termColor(c: Long, p: Palette): Color {
    val col = Color(c or 0xFF000000)
    val light = p.bg.red > 0.5f
    if (!light) return col
    val lum = 0.299f * col.red + 0.587f * col.green + 0.114f * col.blue
    return if (lum > 0.75f) p.text else col
}

// The command line under a host's terminal: a line runs when sent; the keys send the rest.
@Composable
fun TerminalComposer(client: CrewClient) {
    val p = LocalPalette.current
    var line by remember { mutableStateOf("") }
    fun run() { client.shellLine(line); line = "" }
    val keys = listOf("Esc" to "\u001b", "Tab" to "\t", "↑" to "\u001b[A", "↓" to "\u001b[B", "←" to "\u001b[D", "→" to "\u001b[C", "Ctrl+C" to "\u0003", "Ctrl+D" to "\u0004", "Ctrl+L" to "\u000c", "⏎" to "\r")
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((label, data) in keys) Text(label, color = p.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(p.fill).clickable { client.termKey(data) }.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.bg).border(1.dp, p.line, RoundedCornerShape(24.dp)).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                if (line.isEmpty()) Text(t("Command"), color = p.faint, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                BasicTextField(line, { line = it }, singleLine = true, textStyle = TextStyle(color = p.text, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(p.blue), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { run() }), modifier = Modifier.fillMaxWidth())
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (line.isNotEmpty()) p.blue else p.blueSoft).clickable { run() }, contentAlignment = Alignment.Center) {
                Icon(Ico.UP, Color.White, 20.dp, stroke = 2f)
            }
        }
    }
}
