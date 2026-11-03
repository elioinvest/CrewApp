package app.crew.mobile.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.crew.mobile.CrewClient
import app.crew.mobile.t

// The agents' own slash commands, typed in as you would.
private val COMMANDS = mapOf(
    "claude" to listOf("/model" to "Switch model", "/compact" to "Summarize to free context", "/clear" to "Start over", "/context" to "Context used",
        "/usage" to "Plan usage", "/resume" to "Resume a session", "/help" to "All commands"),
    "codex" to listOf("/model" to "Model and reasoning", "/permissions" to "Approval mode", "/compact" to "Summarize to free context",
        "/new" to "New conversation", "/diff" to "Changes so far", "/review" to "Review changes", "/status" to "Session status"),
)

@Composable
fun Composer(client: CrewClient, st: MainState) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val welcome = st.view == View.Welcome
    val busy = client.chatBusy
    val typed = st.draft.isNotBlank()

    // Photos and files: uploaded to the computer, their paths put into the message.
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        client.say(t("Sending…"))
        for (u in uris) {
            val name = context.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "photo.jpg"
            val bytes = try { context.contentResolver.openInputStream(u)?.use { it.readBytes() } } catch (e: Exception) { null } ?: continue
            client.upload(name, bytes) { path, error ->
                if (path == null) client.say(error)
                else { st.draft = (if (st.draft.isEmpty() || st.draft.endsWith(" ")) st.draft else "${st.draft} ") + "\"$path\" "; client.say(t("Added. Say what to do with it, then send.")) }
            }
        }
    }

    fun submit() {
        val text = st.draft
        if (text.isBlank()) { if (busy) client.stop(); return }
        if (welcome) {
            val proj = st.project ?: run { st.sheet = { ProjectSheet(client, st) { st.sheet = null } }; return }
            client.startChat(proj, st.agent, text)
        } else client.sendMessage(text)
        st.draft = ""
    }

    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth().shadow(6.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x10000000), spotColor = Color(0x10000000))
        .clip(RoundedCornerShape(28.dp)).background(p.bg).border(1.dp, p.line, RoundedCornerShape(28.dp)).padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 10.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp, max = 150.dp).padding(end = 4.dp, bottom = 6.dp)) {
            if (st.draft.isEmpty()) Text(if (welcome) t("Describe the task to start a new chat") else t("Message, or / for commands"), color = p.faint, fontSize = 16.sp)
            BasicTextField(st.draft, { st.draft = it }, textStyle = TextStyle(color = p.text, fontSize = 16.sp, lineHeight = 24.sp), cursorBrush = SolidColor(p.blue), modifier = Modifier.fillMaxWidth())
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(p.fill).clickable { pick.launch("*/*") }, contentAlignment = Alignment.Center) { Icon(Ico.PLUS, p.text, 22.dp) }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                if (welcome) {
                    val proj = client.projects.find { it.path == st.project }
                    Chip(proj?.name ?: t("Choose a project"), icon = Ico.FOLDER) { st.sheet = { ProjectSheet(client, st) { st.sheet = null } } }
                    Chip(if (st.agent == "codex") "Codex" else "Claude", strong = true) {
                        st.sheet = { ChoiceSheet(t("Agent"), listOf("claude" to "Claude", "codex" to "Codex").map { (a, l) -> Choice(l, checked = st.agent == a) { st.agent = a } }) { st.sheet = null } }
                    }
                } else {
                    val sid = client.chatSession
                    val chips = sid?.let { client.chips[it] }
                    if (chips != null) {
                        Chip(t(chips.model), strong = true) { openChip(client, st, "model") }
                        if (chips.effort.isNotEmpty()) Chip(t(chips.effort)) { openChip(client, st, "effort") }
                        Chip(t(chips.mode)) { openChip(client, st, "mode") }
                    } else Chip(if (client.chatAgent == "codex") "Codex" else "Claude", strong = true, caret = false) {}
                    if (client.chatTerm != null) COMMANDS[client.chatAgent]?.let { list ->
                        Chip("", icon = Ico.SLASH, caret = false) {
                            st.sheet = { ChoiceSheet(t("Commands"), list.map { (cmd, what) -> Choice(cmd, t(what)) { client.command(cmd) } }) { st.sheet = null } }
                        }
                    }
                }
            }
            val stop = !typed && busy && !welcome
            Box(Modifier.size(44.dp).clip(CircleShape).background(if (stop) p.text else if (typed) p.blue else p.blueSoft).clickable { submit() }, contentAlignment = Alignment.Center) {
                if (stop) Icon(Ico.STOP, p.bg, 16.dp, filled = true) else Icon(Ico.UP, Color.White, 22.dp, stroke = 2f)
            }
        }
    }
}

@Composable
private fun Chip(label: String, icon: String? = null, strong: Boolean = false, caret: Boolean = true, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(it, p.muted, 18.dp) }
        if (label.isNotEmpty()) Text(label, color = if (strong) p.text else p.muted, fontSize = 15.sp, fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1, modifier = Modifier.padding(start = if (icon != null) 4.dp else 0.dp))
        if (caret) Box(Modifier.padding(start = 3.dp)) { Icon(Ico.CARET, p.faint, 14.dp, stroke = 2f) }
    }
}

@Composable
fun ProjectSheet(client: CrewClient, st: MainState, onDismiss: () -> Unit) {
    ChoiceSheet(t("Choose a project"), client.projects.map { pr -> Choice(pr.name, pr.path, checked = pr.path == st.project, icon = Ico.FOLDER) { st.project = pr.path } }, onDismiss)
}

// A chip's menu, built by Crew for this session; the pick is made there.
private fun openChip(client: CrewClient, st: MainState, kind: String) {
    // Not open in Crew: open it there first; the menu comes once it runs.
    if (client.chatTerm == null) { client.openInCrew { openChip(client, st, kind) }; return }
    val title = t(when (kind) { "model" -> "Model"; "effort" -> "Effort"; else -> "Mode" })
    st.sheet = { ChoiceSheet(title, listOf(Choice(t("Loading…")) {})) { st.sheet = null } }
    client.chipMenu(kind) { id, items, hint ->
        if (items.isEmpty()) { st.sheet = null; client.say(hint.ifEmpty { t("Try again when the agent is idle.") }); return@chipMenu }
        st.sheet = {
            ChoiceSheet(title, items.mapIndexed { i, it ->
                Choice(t(it.label), if (it.hint.isNotEmpty()) t(it.hint) else "", checked = it.checked, divider = it.separator) { client.chipPick(id, i); client.say(t("Switching…")) }
            }) { st.sheet = null }
        }
    }
}
