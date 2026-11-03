package app.crew.mobile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.crew.mobile.ChatItem
import app.crew.mobile.CrewClient
import app.crew.mobile.t

// The chat, drawn the way Claude Code's terminal draws a session (Codex's, for a Codex session):
// monospace, a bullet before what the agent says and before each tool it runs, the tool's output
// under ⎿, what you said after "> " on a grey band, and the working line in the agent's color.
private val Mono = FontFamily.Monospace
private const val BODY = 14f

private sealed class Row_ { abstract val key: String }
private data class Msg(val item: ChatItem) : Row_() { override val key get() = item.id }
private data class Tool(val item: ChatItem, val result: ChatItem?) : Row_() { override val key get() = item.id }

private fun rows(items: List<ChatItem>): List<Row_> {
    val results = items.filter { it.role == "result" && it.ref.isNotEmpty() }.associateBy { it.ref }
    return items.mapNotNull { i ->
        when {
            i.role == "tool" -> Tool(i, results[i.ref])
            i.role == "result" && i.ref.isNotEmpty() -> null // under its call
            else -> Msg(i)
        }
    }
}

// The glyphs each agent's terminal uses.
private class Glyphs(val bullet: String, val under: String)
// ● rather than ⏺: Android draws ⏺ as an emoji. Claude Code shows ● on Windows too.
private val ClaudeGlyphs = Glyphs("●", "⎿")
private val CodexGlyphs = Glyphs("•", "└")

@Composable
fun ChatView(client: CrewClient) {
    val p = LocalPalette.current
    val list = rememberLazyListState()
    val shown = remember(client.chat.size, client.chat.lastOrNull()?.id) { rows(client.chat.toList()) }
    val busy = client.chatBusy
    val total = shown.size + 2
    val g = if (client.chatAgent == "codex") CodexGlyphs else ClaudeGlyphs
    // Follow new messages while you're at the bottom; scrolled up, stay where you are.
    val atBottom by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.lastOrNull()?.let { it.index >= list.layoutInfo.totalItemsCount - 3 } ?: true } }
    LaunchedEffect(shown.size, busy, client.ask, client.working.isNotEmpty(), client.live.length / 80) { if (atBottom || shown.size <= 2) list.scrollToItem(total - 1) }
    // Opening a chat starts at its newest messages, once they have arrived.
    LaunchedEffect(client.chatLoads) { if (client.chatLoaded) list.scrollToItem(maxOf(0, total - 1)) }

    if (client.protocol in 1..1) {
        Text(t("This Crew is too old to show chats. Update Crew on your computer to 2.5.58 or later."), color = p.muted, fontSize = 15.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize().padding(32.dp))
        return
    }
    if (shown.isEmpty() && !busy && client.ask == null) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (!client.chatLoaded) CircularProgressIndicator(Modifier.size(22.dp), color = p.blue, strokeWidth = 2.dp, trackColor = p.line)
            Text(if (!client.chatLoaded) t("Loading…") else t("No messages in this chat yet."), color = p.muted, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (client.chatTerm == null && shown.isNotEmpty()) item(key = "closed") {
            Text(t("This chat isn't open in Crew. Sending a message opens it there."), color = p.muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        items(shown, key = { it.key }) { r ->
            when (r) {
                is Msg -> Message(r.item, g)
                is Tool -> ToolCall(r.item, r.result, g, client.chatAgent == "codex")
            }
        }
        item(key = "working") {
            if (busy) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val live = client.live
                // Shown until the transcript has the same text (then it's an ordinary message above).
                val last = client.chat.lastOrNull { it.role == "assistant" }?.text.orEmpty()
                if (live.isNotBlank() && !last.replace(Regex("\\s+"), "").startsWith(live.take(60).replace(Regex("\\s+"), ""))) LiveBlock(live, g)
                Working(client.working, client.chatAgent == "codex")
            } else Spacer(Modifier.height(1.dp))
        }
        item(key = "ask") { client.ask?.let { AskCard(it, client) } ?: Spacer(Modifier.height(1.dp)) }
    }
}

// A bullet and what follows it, the text lined up under itself as in the terminal.
@Composable
private fun Bulleted(bullet: String, color: Color, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(bullet, color = color, fontFamily = Mono, fontSize = BODY.sp, lineHeight = (BODY * 1.5f).sp, modifier = Modifier.width(20.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

// Output under ⎿ (└ for Codex), kept short as the terminal keeps it: the first lines, one row
// each, then "… +N lines". Tap to see all of it, wrapped.
@Composable
private fun Under(text: String, g: Glyphs, color: Color, shownLines: Int = 4) {
    val p = LocalPalette.current
    var all by remember(text) { mutableStateOf(false) }
    // Without the indentation the lines share (the first line arrives trimmed, so it doesn't count).
    val raw = text.trimEnd().lines()
    val indent = raw.drop(1).filter { it.isNotBlank() }.minOfOrNull { l -> l.takeWhile { it == ' ' || it == '\t' }.length } ?: 0
    val lines = raw.mapIndexed { i, l -> if (i == 0) l.trimStart() else l.drop(minOf(indent, l.length)) }
    val folds = lines.size > shownLines + 1
    val visible = if (all || !folds) lines else lines.take(shownLines)
    val style = Modifier
    Row(Modifier.fillMaxWidth().padding(start = 20.dp).clickable { all = !all }) {
        Text("${g.under}  ", color = p.muted, fontFamily = Mono, fontSize = (BODY - 1).sp, lineHeight = (BODY * 1.45f).sp)
        Column(Modifier.weight(1f)) {
            if (all) Text(visible.joinToString("\n"), color = color, fontFamily = Mono, fontSize = (BODY - 1).sp, lineHeight = (BODY * 1.45f).sp, modifier = style)
            else for (l in visible) Text(l, color = color, fontFamily = Mono, fontSize = (BODY - 1).sp, lineHeight = (BODY * 1.45f).sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            if (visible.size < lines.size) Text("… +${lines.size - visible.size} lines", color = p.muted, fontFamily = Mono, fontSize = (BODY - 1).sp, lineHeight = (BODY * 1.45f).sp)
        }
    }
}

@Composable
private fun Message(m: ChatItem, g: Glyphs) {
    val p = LocalPalette.current
    when (m.role) {
        // What you said: "> " on a grey band, as the terminal shows a sent prompt.
        "user", "command" -> SelectionContainer {
            Row(Modifier.fillMaxWidth().background(p.fill).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(">", color = p.muted, fontFamily = Mono, fontSize = BODY.sp, lineHeight = (BODY * 1.5f).sp, modifier = Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    if (m.images > 0) Text((1..m.images).joinToString(" ") { "[Image #$it]" }, color = p.muted, fontFamily = Mono, fontSize = BODY.sp, lineHeight = (BODY * 1.5f).sp)
                    if (m.text.isNotEmpty()) Text(m.text, color = p.text, fontFamily = Mono, fontSize = BODY.sp, lineHeight = (BODY * 1.5f).sp)
                }
            }
        }
        "assistant" -> Bulleted(g.bullet, p.text) { Markdown(m.text, Modifier.fillMaxWidth(), mono = true, size = BODY) }
        "thinking" -> Thinking(m.text, codex = g === CodexGlyphs)
        "result" -> Under(m.text, g, if (m.error) p.red else p.muted)
        // A note: a command's output, an interruption, a compaction.
        else -> Under(m.text, g, if (m.text.startsWith("[Request interrupted")) p.red else p.muted, shownLines = 8)
    }
}

// How the terminal names a tool: Claude's "Update" for an edit, "Search" for a grep, and so on;
// Codex's "Ran <command>" for a shell command.
private val CLAUDE_TOOL_NAMES = mapOf("Edit" to "Update", "MultiEdit" to "Update", "Grep" to "Search", "Glob" to "Search", "WebFetch" to "Fetch", "WebSearch" to "Web Search", "TodoWrite" to "Update Todos", "NotebookEdit" to "Edit Notebook")

@Composable
private fun ToolCall(call: ChatItem, result: ChatItem?, g: Glyphs, codex: Boolean) {
    val p = LocalPalette.current
    val shell = codex && call.name in setOf("exec_command", "shell", "exec", "local_shell", "container.exec")
    val header = buildAnnotatedString {
        if (shell) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Ran") }; append(" ${call.text}") }
        else {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(CLAUDE_TOOL_NAMES[call.name] ?: call.name) }
            if (call.text.isNotEmpty()) append("(${call.text})")
        }
    }
    // The bullet: green once it worked, red if it failed, grey while it runs.
    val color = when { result == null -> p.muted; result.error -> p.red; else -> p.green }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Bulleted(g.bullet, color) { Text(header, color = p.text, fontFamily = Mono, fontSize = BODY.sp, lineHeight = (BODY * 1.5f).sp, maxLines = 3, overflow = TextOverflow.Ellipsis) }
        if (result != null && result.text.isNotBlank()) Under(result.text, g, if (result.error) p.red else p.muted)
    }
}

// The agent's thinking, shown in full where its terminal shows it: Claude's indented under the
// bullets' text, with no bullet of its own; Codex's reasoning summaries as dim italic lines.
@Composable
private fun Thinking(text: String, codex: Boolean) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().padding(start = 20.dp)) {
        if (codex) Text(text.replace("**", ""), color = p.muted, fontFamily = Mono, fontSize = BODY.sp, lineHeight = (BODY * 1.5f).sp, fontStyle = FontStyle.Italic)
        else Markdown(text, Modifier.fillMaxWidth(), mono = true, size = BODY)
    }
}

// What the agent is writing or running right now, as its terminal shows it.
@Composable
private fun LiveBlock(text: String, g: Glyphs) {
    val p = LocalPalette.current
    val tool = Regex("^[A-Za-z_][\\w.:-]*\\(").containsMatchIn(text) || text.startsWith("Ran ") || text.startsWith("Running")
    if (tool) {
        val lines = text.lines()
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Bulleted(g.bullet, p.muted) { Text(lines.first(), color = p.text, fontFamily = Mono, fontSize = BODY.sp, lineHeight = (BODY * 1.5f).sp) }
            val rest = lines.drop(1).joinToString("\n") { it.trimStart().removePrefix("⎿").removePrefix("└").trimStart() }.trim()
            if (rest.isNotEmpty()) Under(rest, g, p.muted, shownLines = 8)
        }
    } else Bulleted(g.bullet, p.text) { Markdown(text, Modifier.fillMaxWidth(), mono = true, size = BODY) }
}

// The working line, as the agent shows it: Claude's turning star in its orange, Codex's bullet.
@Composable
private fun Working(text: String, codex: Boolean) {
    val p = LocalPalette.current
    val frames = if (codex) listOf("•", "◦") else listOf("·", "✢", "✳", "✶", "✻", "✽")
    var frame by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(160); frame = (frame + 1) % frames.size } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(frames[frame], color = p.accent, fontFamily = Mono, fontSize = BODY.sp, modifier = Modifier.width(20.dp))
        Text(text.ifEmpty { t("Working") + "…" }, color = p.accent, fontFamily = Mono, fontSize = BODY.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// The agent's numbered question, as buttons.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskCard(ask: app.crew.mobile.Ask, client: CrewClient) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).border(1.dp, p.line, RoundedCornerShape(22.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t("The agent is asking"), color = p.orange, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(p.orangeBg).padding(horizontal = 10.dp, vertical = 1.dp))
        }
        if (ask.question.isNotEmpty()) Text(ask.question, color = p.text, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ask.options.forEachIndexed { i, (n, label) ->
                Text("$n. $label", color = if (i == 0) Color.White else p.text, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (i == 0) p.blue else p.fill).clickable { client.answerAsk(n) }.padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }
    }
}

// crew-ssh's approvals: Allow / Allow 15 min / Deny. Passwords and host keys stay on the computer.
@Composable
fun Prompts(client: CrewClient) {
    val p = LocalPalette.current
    Column(Modifier.padding(horizontal = 12.dp)) {
        for (pr in client.prompts) Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).border(1.dp, p.line, RoundedCornerShape(22.dp)).background(p.bg).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("Needs your OK"), color = p.orange, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(p.orangeBg).padding(horizontal = 10.dp, vertical = 1.dp))
                val verb = mapOf("upload" to "upload a file to", "download" to "download a file from", "input" to "type into your terminal on")[pr.action] ?: "run on"
                Text(if (pr.kind == "approve") t("An agent wants to %s %s", t(verb), pr.host) else t("%s is waiting for you on the computer", pr.host),
                    color = p.muted, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
            }
            if (pr.kind == "approve") {
                Text(pr.command, color = p.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.fill).padding(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallPill(t("Deny")) { client.answerPrompt(pr, null) }
                    Spacer(Modifier.weight(1f))
                    SmallPill(t("Allow 15 min")) { client.answerPrompt(pr, "host") }
                    SmallPill(t("Allow"), primary = true) { client.answerPrompt(pr, "once") }
                }
            }
        }
    }
}

@Composable
fun SmallPill(label: String, primary: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Text(label, color = if (primary) Color.White else p.text, fontSize = 14.sp,
        modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (primary) p.blue else p.fill).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 7.dp))
}

// Agent runs, like a trajectory: a colored bar, tags, the task, what it's doing.
@Composable
fun RunsView(client: CrewClient) {
    val p = LocalPalette.current
    val runs = client.runs.reversed()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Text(t("%d runs · %d running", runs.size, runs.count { it.status == "running" }), color = p.muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp)) }
        if (runs.isEmpty()) item { Text(t("No agent runs yet."), color = p.muted, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 60.dp)) }
        items(runs, key = { it.id }) { r ->
            Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(vertical = 10.dp)) {
                Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(when (r.status) { "running" -> p.blue; "failed" -> p.red; else -> p.faint }))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("@${r.agent}", color = p.text, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(p.fill).padding(horizontal = 10.dp, vertical = 1.dp))
                        Text(t(when (r.status) { "running" -> "Running"; "done" -> "Done"; else -> "Failed" }), fontSize = 13.sp,
                            color = when (r.status) { "running" -> p.blue; "failed" -> p.red; else -> p.text },
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(when (r.status) { "running" -> p.blueBg; else -> p.fill }).padding(horizontal = 10.dp, vertical = 1.dp))
                    }
                    Text(r.task, color = p.text, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    val what = if (r.status == "running") r.activity else r.error
                    if (what.isNotEmpty()) Text(what, color = p.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (r.status == "running") Box(Modifier.padding(start = 8.dp)) { SmallPill(t("Stop")) { client.stopRun(r) } }
            }
        }
    }
}

// Keys for what a chat can't say: pickers, interrupts, a yes or a no.
@Composable
fun KeysSheet(client: CrewClient, onDismiss: () -> Unit) {
    val keys = listOf("Esc" to "\u001b", "↑" to "\u001b[A", "↓" to "\u001b[B", "⏎ Enter" to "\r", "Tab" to "\t", "⇧Tab" to "\u001b[Z", "Ctrl+C" to "\u0003", "y" to "y", "n" to "n")
    ChoiceSheet(t("Keys"), keys.map { (label, data) -> Choice(label) { client.key(data) } }, onDismiss)
}

@Composable
fun ComputersSheet(client: CrewClient, onDismiss: () -> Unit) {
    val computers = app.crew.mobile.CrewApplication.instance.computers
    val current = client.computer
    ChoiceSheet(t("Computers"), computers.all().map { c ->
        Choice(c.name, c.hosts.joinToString(" · "), checked = c.token == current?.token, icon = Ico.MONITOR) { if (c.token != current?.token) client.connect(c) }
    } + listOf(
        Choice(t("Pair another computer"), icon = Ico.PLUS) { client.disconnect() },
        Choice(t("Disconnect"), icon = Ico.STOP) { client.disconnect() },
    ), onDismiss)
}
