package app.crew.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The agents write Markdown: headings, lists, quotes, code blocks, tables, and inline bold,
// italic, code and links. This draws that much natively.
private sealed class Block {
    data class Para(val text: String) : Block()
    data class Heading(val level: Int, val text: String) : Block()
    data class Item(val marker: String, val text: String, val indent: Int) : Block()
    data class Quote(val text: String) : Block()
    data class Code(val text: String) : Block()
    data class Table(val rows: List<List<String>>) : Block()
    object Rule : Block()
}

private fun blocks(md: String): List<Block> {
    val out = mutableListOf<Block>()
    val lines = md.replace("\r\n", "\n").split("\n")
    var i = 0
    val para = StringBuilder()
    fun flush() { if (para.isNotBlank()) out.add(Block.Para(para.toString().trim().replace(Regex("\\s*\\n\\s*"), " "))); para.clear() }
    while (i < lines.size) {
        val l = lines[i]
        val trimmed = l.trim()
        when {
            trimmed.startsWith("```") -> {
                flush()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) { code.append(lines[i]).append('\n'); i++ }
                out.add(Block.Code(code.toString().trimEnd()))
            }
            trimmed.isEmpty() -> flush()
            Regex("^#{1,6}\\s").containsMatchIn(trimmed) -> { flush(); val h = trimmed.takeWhile { it == '#' }.length; out.add(Block.Heading(h, trimmed.drop(h).trim())) }
            Regex("^(-{3,}|\\*{3,}|_{3,})$").matches(trimmed) -> { flush(); out.add(Block.Rule) }
            Regex("^\\s*([-*+]|\\d+[.)])\\s+").containsMatchIn(l) -> {
                flush()
                val m = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+(.*)$").find(l)!!
                val marker = m.groupValues[2].let { if (it in listOf("-", "*", "+")) "•" else it }
                out.add(Block.Item(marker, m.groupValues[3], m.groupValues[1].length / 2))
            }
            trimmed.startsWith(">") -> { flush(); out.add(Block.Quote(trimmed.trimStart('>').trim())) }
            trimmed.startsWith("|") && i + 1 < lines.size && Regex("^\\|?\\s*:?-{2,}").containsMatchIn(lines[i + 1].trim()) -> {
                flush()
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val cells = lines[i].trim().trim('|').split("|").map { it.trim() }
                    if (!cells.all { Regex("^:?-{2,}:?$").matches(it) }) rows.add(cells)
                    i++
                }
                out.add(Block.Table(rows))
                continue
            }
            else -> para.append(l).append('\n')
        }
        i++
    }
    flush()
    return out
}

// **bold**, *italic*, `code`, [text](url).
private fun inline(s: String, codeBg: androidx.compose.ui.graphics.Color, link: androidx.compose.ui.graphics.Color): AnnotatedString = buildAnnotatedString {
    val re = Regex("(\\*\\*|__)(.+?)\\1|`([^`]+)`|\\[([^\\]]+)]\\(([^)\\s]+)\\)|(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?!\\*)")
    var pos = 0
    for (m in re.findAll(s)) {
        append(s.substring(pos, m.range.first))
        when {
            m.groups[2] != null -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(m.groupValues[2]) }
            m.groups[3] != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, fontSize = 14.sp)) { append(" ${m.groupValues[3]} ") }
            m.groups[4] != null -> withLink(LinkAnnotation.Url(m.groupValues[5], TextLinkStyles(SpanStyle(color = link)))) { append(m.groupValues[4]) }
            m.groups[6] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[6]) }
        }
        pos = m.range.last + 1
    }
    append(s.substring(pos))
}

@Composable
// mono: drawn in the terminal's font (the chat drawn like the TUI); size: the body text size.
fun Markdown(text: String, modifier: Modifier = Modifier, mono: Boolean = false, size: Float = 16f) {
    val p = LocalPalette.current
    val parsed = remember(text) { blocks(text) }
    val base = androidx.compose.material3.LocalTextStyle.current
    androidx.compose.material3.ProvideTextStyle(if (mono) base.copy(fontFamily = FontFamily.Monospace) else base) {
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (b in parsed) when (b) {
                is Block.Para -> Text(inline(b.text, p.code, p.blue), color = p.text, fontSize = size.sp, lineHeight = (size * 1.55f).sp)
                is Block.Heading -> Text(inline(b.text, p.code, p.blue), color = p.text, fontWeight = FontWeight.SemiBold,
                    fontSize = when (b.level) { 1 -> (size + 5).sp; 2 -> (size + 3).sp; else -> (size + 1).sp }, modifier = Modifier.padding(top = 4.dp))
                is Block.Item -> Row(Modifier.padding(start = (b.indent * 16).dp)) {
                    Text(b.marker, color = p.muted, fontSize = size.sp, lineHeight = (size * 1.55f).sp, modifier = Modifier.width(if (b.marker == "•") 16.dp else 24.dp))
                    Text(inline(b.text, p.code, p.blue), color = p.text, fontSize = size.sp, lineHeight = (size * 1.55f).sp)
                }
                is Block.Quote -> Row {
                    Box(Modifier.width(3.dp).padding(vertical = 2.dp).background(p.line))
                    Text(inline(b.text, p.code, p.blue), color = p.muted, fontSize = (size - 1).sp, lineHeight = ((size - 1) * 1.5f).sp, modifier = Modifier.padding(start = 10.dp))
                }
                is Block.Code -> Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.code).horizontalScroll(rememberScrollState()).padding(12.dp)) {
                    Text(b.text, color = p.text, fontFamily = FontFamily.Monospace, fontSize = (size - 3).sp, lineHeight = ((size - 3) * 1.45f).sp, softWrap = false)
                }
                is Block.Table -> Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.code).horizontalScroll(rememberScrollState())) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        b.rows.forEachIndexed { r, cells ->
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                for (c in cells) Text(inline(c, p.code, p.blue), color = p.text, fontSize = (size - 2).sp,
                                    fontWeight = if (r == 0) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.width(140.dp).padding(end = 12.dp))
                            }
                        }
                    }
                }
                Block.Rule -> Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(p.line).padding(top = 1.dp))
            }
        }
    }
}
}
