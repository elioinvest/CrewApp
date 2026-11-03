package app.crew.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.crew.mobile.AgentDef
import app.crew.mobile.CrewApplication
import app.crew.mobile.CrewClient
import app.crew.mobile.Host
import app.crew.mobile.Link
import app.crew.mobile.Session
import app.crew.mobile.Term
import app.crew.mobile.t
import kotlinx.coroutines.launch

enum class View { Welcome, Chat, Runs, Terminal }

// What the screen shows, kept across the drawer and the sheets.
class MainState {
    var view by mutableStateOf(View.Welcome)
    var project by mutableStateOf<String?>(null)
    var agent by mutableStateOf("claude")
    var sheet by mutableStateOf<(@Composable () -> Unit)?>(null)
    var draft by mutableStateOf("")
    // The host terminal shown (View.Terminal): its terminal and the host's name.
    var term by mutableStateOf<String?>(null)
    var termTitle by mutableStateOf("")
}

@Composable
fun MainScreen(client: CrewClient) {
    val p = LocalPalette.current
    val st = remember { MainState() }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    if (st.project == null || client.projects.none { it.path == st.project }) st.project = client.projects.firstOrNull()?.path

    fun openChat(sessionId: String, termId: String?, agent: String, title: String) {
        client.openChat(sessionId, termId, agent, title)
        st.view = View.Chat
        scope.launch { drawer.close() }
    }
    fun newChat(project: String? = null, agent: String? = null) {
        client.closeChat()
        project?.let { st.project = it }
        agent?.let { st.agent = it }
        st.view = View.Welcome
        scope.launch { drawer.close() }
    }
    // A chat the phone started shows up once Crew has it.
    if (client.chatSession != null && st.view == View.Welcome) st.view = View.Chat

    BackHandler(drawer.isOpen || st.view != View.Welcome) {
        if (drawer.isOpen) scope.launch { drawer.close() } else newChat()
    }

    ModalNavigationDrawer(drawerState = drawer, scrimColor = Color.Black.copy(alpha = 0.28f), drawerContent = {
        ModalDrawerSheet(drawerContainerColor = p.side, drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp), modifier = Modifier.width(320.dp)) {
            Drawer(client, st,
                onNew = { newChat() },
                onRuns = { st.view = View.Runs; client.closeChat(); scope.launch { drawer.close() } },
                onComputers = { scope.launch { drawer.close() }; st.sheet = { ComputersSheet(client) { st.sheet = null } } },
                onClose = { scope.launch { drawer.close() } },
                onOpen = ::openChat, onNewIn = { proj, ag -> newChat(proj, ag) },
                onHost = { h -> client.openHost(h); scope.launch { drawer.close() } },
                onTerminal = { h ->
                    scope.launch { drawer.close() }
                    client.openHostTerminal(h) { t -> client.closeChat(); st.term = t.termId; st.termTitle = h.name; st.view = View.Terminal }
                },
                onMention = { a -> st.draft = (if (st.draft.isEmpty() || st.draft.endsWith(" ")) st.draft else "${st.draft} ") + "@${a.name} "; scope.launch { drawer.close() } },
            )
        }
    }) {
        Column(Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
            // Header: the drawer, the chat's title, keys, a new chat.
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(Ico.SIDEBAR) { scope.launch { drawer.open() } }
                Text(
                    when (st.view) { View.Chat -> client.chatTitle.ifEmpty { t("Untitled") }; View.Runs -> t("Agent runs"); View.Terminal -> st.termTitle; else -> "" },
                    color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                )
                if (st.view == View.Chat && client.chatTerm != null) IconButton(Ico.KEYS) { st.sheet = { KeysSheet(client) { st.sheet = null } } }
                IconButton(Ico.NEW) { newChat() }
            }
            Box(Modifier.weight(1f)) {
                when (st.view) {
                    View.Welcome -> Welcome(client)
                    View.Chat -> ChatView(client)
                    View.Runs -> RunsView(client)
                    View.Terminal -> st.term?.let { TerminalView(client, it) }
                }
            }
            Prompts(client)
            if (st.view == View.Terminal) TerminalComposer(client)
            else if (st.view != View.Runs) Composer(client, st)
        }
    }
    st.sheet?.invoke()
}

@Composable
fun IconButton(path: String, tint: Color? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(path, tint ?: p.text, 24.dp)
    }
}

@Composable
private fun Welcome(client: CrewClient) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().padding(bottom = 28.dp), contentAlignment = Alignment.BottomCenter) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CrewLogo(40)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(client.computerName, color = p.blue, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(p.blueBg).padding(horizontal = 9.dp, vertical = 2.dp))
                Text(t("What are we doing today?"), color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------- the drawer, laid out like Crew's sidebar ----------

private const val SHOWN = 8

@Composable
private fun Drawer(
    client: CrewClient, st: MainState, onNew: () -> Unit, onRuns: () -> Unit, onComputers: () -> Unit, onClose: () -> Unit,
    onOpen: (String, String?, String, String) -> Unit, onNewIn: (String, String?) -> Unit, onHost: (Host) -> Unit, onTerminal: (Host) -> Unit, onMention: (AgentDef) -> Unit,
) {
    val p = LocalPalette.current
    val prefs = remember { CrewApplication.instance.getSharedPreferences("crew", 0) }
    val folded = remember { mutableStateMapOf<String, Boolean>().apply { prefs.getStringSet("folded", emptySet())!!.forEach { put(it, true) } } }
    val showAll = remember { mutableStateMapOf<String, Boolean>() }
    var searching by rememberSaveable { mutableStateOf(false) }
    var q by rememberSaveable { mutableStateOf("") }
    fun fold(key: String) {
        if (folded[key] == true) folded.remove(key) else folded[key] = true
        prefs.edit().putStringSet("folded", folded.keys.toSet()).apply()
    }
    val query = q.trim().lowercase()
    fun has(vararg s: String) = query.isEmpty() || s.any { it.lowercase().contains(query) }
    val live = client.terminals.filter { it.agent != "ssh" }.associateBy { it.sessionId }

    Column(Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp)) {
        Row(Modifier.padding(start = 6.dp, top = 10.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            CrewLogo(30)
            Text("Crew", color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            Text("REMOTE", color = p.muted, fontSize = 13.sp, letterSpacing = 2.5.sp, modifier = Modifier.padding(start = 8.dp, top = 6.dp))
            Spacer(Modifier.weight(1f))
            IconButton(Ico.SIDEBAR, onClick = onClose)
        }
        Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(p.sideHover).clickable(onClick = onNew),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Ico.NEW, p.text, 22.dp)
            Text(t("New chat"), color = p.text, fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        val running = client.runs.count { it.status == "running" }
        DrawerItem(Ico.CLOCK, t("Agent runs"), if (running > 0) "$running" else null, onRuns)
        DrawerItem(Ico.MONITOR, t("Computers"), null, onComputers)
        Row(Modifier.padding(start = 8.dp, end = 6.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t("Projects"), color = p.muted, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { searching = !searching; if (!searching) q = "" }, contentAlignment = Alignment.Center) { Icon(Ico.SEARCH, p.muted, 20.dp) }
        }
        if (searching) Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(p.sideHover).padding(horizontal = 14.dp, vertical = 9.dp)) {
            if (q.isEmpty()) Text(t("Search projects and sessions"), color = p.faint, fontSize = 15.sp)
            BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(color = p.text, fontSize = 15.sp), cursorBrush = SolidColor(p.blue), modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(Modifier.weight(1f).padding(horizontal = 0.dp)) {
            // Projects: each folds open to its sessions, grouped by agent, as in Crew.
            for (proj in client.projects) {
                val nameHit = has(proj.name, proj.path)
                val fresh = live.values.filter { sameDirOf(it.cwd, proj.path) && proj.sessions.none { s -> s.id == it.sessionId } }
                    .map { Session(it.sessionId, it.title, it.agent, null, System.currentTimeMillis()) }
                var sessions = fresh + proj.sessions
                if (!nameHit) sessions = sessions.filter { has(it.title) }
                if (!nameHit && sessions.isEmpty()) continue
                val pf = query.isEmpty() && folded[proj.path] == true
                item(key = "p:${proj.path}") {
                    TreeRow(Ico.FOLDER, proj.name, collapsed = pf, onPlus = { onNewIn(proj.path, null) }) { fold(proj.path) }
                }
                if (pf) continue
                val groups = buildList {
                    add(Triple("claude", "Claude") { s: Session -> s.agent != "codex" && s.provider == null })
                    add(Triple("codex", "GPT") { s: Session -> s.agent == "codex" && s.provider == null })
                    for (pv in client.providers) add(Triple("provider:${pv.id}", pv.name) { s: Session -> s.provider == pv.id })
                }
                for ((key, label, test) in groups) {
                    val group = sessions.filter(test)
                    if (group.isEmpty()) continue
                    val gk = "${proj.path}::$key"
                    val gf = query.isEmpty() && folded[gk] == true
                    val agentOf = if (key.startsWith("provider:")) "" else key
                    item(key = "g:$gk") {
                        GroupRow(key, label, group.size, collapsed = gf, client = client, onPlus = if (agentOf.isNotEmpty()) ({ onNewIn(proj.path, agentOf) }) else null) { fold(gk) }
                    }
                    if (gf) continue
                    val limit = if (query.isNotEmpty() || showAll[gk] == true) Int.MAX_VALUE else SHOWN
                    items(group.take(limit), key = { "s:${proj.path}:${it.id}" }) { s ->
                        val term = live[s.id]
                        SessionRow(s.title.ifEmpty { t("Untitled") }, relTime(s.mtime), term, active = client.chatSession == s.id) { onOpen(s.id, term?.termId, s.agent, s.title) }
                    }
                    if (group.size > limit) item(key = "more:$gk") {
                        Text(t("Show %d more", group.size - limit), color = p.sideMuted, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(start = 21.dp).clip(RoundedCornerShape(8.dp)).clickable { showAll[gk] = true }.padding(start = 32.dp, top = 10.dp, bottom = 10.dp))
                    }
                }
            }
            if (client.projects.isEmpty()) item { Note(t("No projects yet")) }

            // Hosts: by group, each folding open to its agent sessions.
            item(key = "hosts-head") { SectionHead(t("Hosts"), query.isEmpty() && folded["§hosts"] == true) { fold("§hosts") } }
            if (!(query.isEmpty() && folded["§hosts"] == true)) {
                if (client.hosts.isEmpty()) item { Note(t("Add hosts in Crew on your computer.")) }
                val hosts = client.hosts.filter { h -> has(h.name, h.host) || h.sessions.any { has(it.title) } }.sortedBy { it.name.lowercase() }
                for ((g, list) in hosts.groupBy { it.group }.toSortedMap(compareBy({ it.isNotEmpty() }, { it }))) {
                    if (g.isNotEmpty()) item(key = "hg:$g") { Text(g.uppercase(), color = p.sideMuted, fontSize = 11.sp, letterSpacing = 0.4.sp, modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp)) }
                    for (h in list) {
                        val hk = "§host:${h.id}"
                        val open = query.isNotEmpty() || folded[hk] != true
                        item(key = "h:${h.id}") { HostRow(h, collapsed = !open, onFold = { fold(hk) }) { onHost(h) } }
                        // Its terminal first, as Crew lists it.
                        if (open) item(key = "ht:${h.id}") {
                            val shell = client.terminals.find { it.sessionId == "ssh:${h.id}" }
                            SessionRow(t("Terminal"), "", shell, active = st.view == View.Terminal && st.term == shell?.termId && shell != null, mark = { AgentMark("ssh", null) }) { onTerminal(h) }
                        }
                        if (open) items(h.sessions, key = { "hs:${h.id}:${it.id}" }) { s ->
                            val term = live[s.id]
                            SessionRow(s.title.ifEmpty { t("Untitled") }, relTime(s.mtime), term, active = client.chatSession == s.id, mark = { AgentMark(s.agent, client.providers.find { it.id == s.provider }) }) {
                                onOpen(s.id, term?.termId, s.agent, s.title)
                            }
                        }
                    }
                }
            }

            // The @-agents: tap one to mention it in the message.
            item(key = "agents-head") { SectionHead(t("Agents"), query.isEmpty() && folded["§agents"] == true) { fold("§agents") } }
            if (!(query.isEmpty() && folded["§agents"] == true)) {
                if (client.agents.isEmpty()) item { Note(t("Add agents in Crew on your computer, then @-mention them in a chat.")) }
                items(client.agents.filter { has(it.name) }.sortedBy { it.name.lowercase() }, key = { "a:${it.id}" }) { a ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onMention(a) }.heightIn(min = 40.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val pv = if (a.cli.startsWith("provider:")) client.providers.find { it.id == a.cli.removePrefix("provider:") } else null
                        AgentMark(if (a.cli.startsWith("provider:")) "" else a.cli, pv)
                        Text(a.name, color = p.text, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t(mapOf("read" to "reads", "edit" to "edits", "full" to "full access")[a.access] ?: a.access), color = p.sideMuted, fontSize = 12.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        // The connection.
        Row(Modifier.padding(start = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(when (client.link) { Link.On -> p.green; Link.Connecting -> p.orange; else -> p.faint }))
            Text(if (client.link == Link.On) client.computerName else t("Not connected"), color = p.text, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun sameDirOf(a: String, b: String) = app.crew.mobile.sameDir(a, b)

@Composable
private fun DrawerItem(icon: String, label: String, badge: String?, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, p.muted, 24.dp)
        Text(label, color = p.text, fontSize = 17.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
        if (badge != null) Text(badge, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(p.blue).padding(horizontal = 7.dp, vertical = 2.dp))
    }
}

@Composable
private fun Chevron(collapsed: Boolean, size: Int = 14) {
    val p = LocalPalette.current
    Box(Modifier.size(size.dp).then(if (collapsed) Modifier.graphicsRotate(-90f) else Modifier)) { Icon(Ico.CARET, p.sideMuted, size.dp, stroke = 2f) }
}
private fun Modifier.graphicsRotate(deg: Float) = this.rotate(deg)

@Composable
private fun TreeRow(icon: String, label: String, collapsed: Boolean, onPlus: () -> Unit, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).heightIn(min = 40.dp).padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, p.text, 18.dp)
        Text(label, color = p.text, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onPlus), contentAlignment = Alignment.Center) { Icon(Ico.PLUS, p.sideMuted, 15.dp) }
        Chevron(collapsed)
    }
}

@Composable
private fun GroupRow(key: String, label: String, count: Int, collapsed: Boolean, client: CrewClient, onPlus: (() -> Unit)?, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(start = 21.dp).drawLeftLine(p.sideBorder).padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
        .heightIn(min = 34.dp).padding(start = 10.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val pv = if (key.startsWith("provider:")) client.providers.find { it.id == key.removePrefix("provider:") } else null
        AgentMark(if (pv != null) "" else key, pv, 12.dp)
        Text(label, color = p.sideMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp, modifier = Modifier.weight(1f))
        Text("$count", color = p.sideMuted, fontSize = 12.sp)
        if (onPlus != null) Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onPlus), contentAlignment = Alignment.Center) { Icon(Ico.PLUS, p.sideMuted, 15.dp) }
        Chevron(collapsed, 13)
    }
}

@Composable
private fun SessionRow(title: String, meta: String, term: Term?, active: Boolean, mark: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(start = 21.dp).drawLeftLine(p.sideBorder).padding(start = 8.dp).clip(RoundedCornerShape(8.dp))
        .background(if (active) p.sideActive else Color.Transparent).clickable(onClick = onClick).heightIn(min = 40.dp).padding(start = if (mark == null) 22.dp else 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (term != null) StatusDot(term.busy)
        mark?.invoke()
        Text(title, color = p.text, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(meta, color = p.sideMuted, fontSize = 12.sp)
    }
}

@Composable
fun StatusDot(busy: Boolean) {
    val p = LocalPalette.current
    if (busy) androidx.compose.material3.CircularProgressIndicator(Modifier.size(11.dp), color = p.accent, strokeWidth = 2.dp, trackColor = Color.Transparent)
    else Box(Modifier.size(7.dp).clip(CircleShape).background(p.green))
}

@Composable
private fun HostRow(h: Host, collapsed: Boolean, onFold: () -> Unit, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).heightIn(min = 40.dp).padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Ico.SERVER, if (h.connected) p.green else p.sideMuted, 16.dp)
        Text(h.name, color = p.text, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(h.address, color = p.sideMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onFold).padding(4.dp)) { Chevron(collapsed) }
    }
}

@Composable
private fun SectionHead(label: String, collapsed: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.padding(top = 6.dp)) {
        HorizontalDivider(color = p.sideBorder)
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 8.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = p.sideMuted, fontSize = 15.sp)
            Spacer(Modifier.width(4.dp))
            Chevron(collapsed, 12)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = LocalPalette.current.sideMuted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
}

// The thin line down the left of a project's sessions, as in Crew's sidebar.
private fun Modifier.drawLeftLine(color: Color) = this.drawBehind { drawLine(color, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx()) }
