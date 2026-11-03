package app.crew.mobile

import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// What Crew sends about its sessions (see Crew's remote.js for the protocol).
data class Term(val termId: String, val sessionId: String, val agent: String, val cwd: String, val title: String, val project: String, val busy: Boolean)
data class Session(val id: String, val title: String, val agent: String, val provider: String?, val mtime: Long)
data class Project(val path: String, val name: String, val sessions: List<Session>)
data class Host(val id: String, val name: String, val group: String, val user: String, val host: String, val port: Int, val workspace: String, val connected: Boolean, val sessions: List<Session>) {
    val address get() = (if (user.isNotEmpty()) "$user@" else "") + host + (if (port != 22) ":$port" else "")
}
data class AgentDef(val id: String, val name: String, val cli: String, val access: String)
data class Provider(val id: String, val name: String, val cli: String, val icon: String?)
data class Prompt(val id: Int, val kind: String, val action: String, val command: String, val host: String)
data class Run(val id: String, val agent: String, val task: String, val status: String, val activity: String, val error: String)
data class ChatItem(val id: String, val role: String, val text: String, val name: String, val images: Int, val ref: String = "", val error: Boolean = false)
data class Ask(val question: String, val options: List<Pair<Int, String>>)
// closed: read from the session's saved settings, as it isn't open in Crew (picks need it open).
data class Chips(val model: String, val effort: String, val mode: String, val closed: Boolean = false)
// A run of a terminal line: its text, color (0xRRGGBB, or null for the default) and weight.
data class TermRun(val text: String, val color: Long?, val bold: Boolean)
data class MenuItem(val label: String, val hint: String, val checked: Boolean, val separator: Boolean)

enum class Link { Off, Connecting, On }

// The one connection to Crew, and everything the screens show. Its state is Compose state, changed
// on the main thread only.
class CrewClient(private val computers: Computers) {
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).connectTimeout(4, TimeUnit.SECONDS).build()
    private var ws: WebSocket? = null
    private var wanted = false
    private var retry = 0
    private var hostIndex = 0

    var computer by mutableStateOf<Computer?>(null); private set
    var link by mutableStateOf(Link.Off); private set
    var status by mutableStateOf(""); private set
    var statusError by mutableStateOf(false); private set
    var computerName by mutableStateOf(""); private set
    // Paired with a computer and meaning to stay connected (reconnecting after a drop included).
    var paired by mutableStateOf(false); private set
    // Crew's protocol version: 2 and later send sessions as chats (Crew 2.5.58+).
    var protocol by mutableStateOf(0); private set

    val terminals = mutableStateListOf<Term>()
    val projects = mutableStateListOf<Project>()
    val hosts = mutableStateListOf<Host>()
    val agents = mutableStateListOf<AgentDef>()
    val providers = mutableStateListOf<Provider>()
    val prompts = mutableStateListOf<Prompt>()
    val runs = mutableStateListOf<Run>()
    val chips = mutableStateMapOf<String, Chips>()

    // The open chat: its session, the terminal it runs in (if it's open in Crew), what's been said,
    // and what the agent is doing or asking now.
    var chatSession by mutableStateOf<String?>(null); private set
    var chatTerm by mutableStateOf<String?>(null); private set
    var chatAgent by mutableStateOf("claude"); private set
    var chatTitle by mutableStateOf(""); private set
    val chat = mutableStateListOf<ChatItem>()
    // Crew has sent the transcript (it may be empty: a session with nothing said yet).
    var chatLoaded by mutableStateOf(false); private set
    // Counts the times Crew sent the whole chat (opened, reopened, reconnected): each one starts at the newest message.
    var chatLoads by mutableStateOf(0); private set
    var working by mutableStateOf(""); private set
    // What the agent is writing or running right now, read off its terminal (not in the transcript yet).
    var live by mutableStateOf(""); private set
    var ask by mutableStateOf<Ask?>(null); private set

    // A host's terminal shown on the phone: its screen as Crew sends it.
    var watching by mutableStateOf<String?>(null); private set
    var termLines by mutableStateOf<List<List<TermRun>>>(emptyList()); private set
    var termCursor by mutableStateOf<Pair<Int, Int>?>(null); private set

    var toast by mutableStateOf<Pair<Long, String>?>(null); private set
    fun say(s: String) { toast = System.nanoTime() to s }

    // The service shows notifications for these.
    var onPrompt: ((Prompt) -> Unit)? = null
    var onFinished: ((Term) -> Unit)? = null
    var onLink: ((Link) -> Unit)? = null

    // ---------- the connection ----------

    fun connect(c: Computer) {
        disconnect()
        computer = c
        wanted = true
        retry = 0
        hostIndex = 0
        openNext()
    }

    fun disconnect() {
        wanted = false
        paired = false
        main.removeCallbacksAndMessages(null)
        ws?.close(1000, null)
        ws = null
        updateLink(Link.Off)
    }

    // Tries the computer's addresses in turn until one answers.
    private fun openNext() {
        val c = computer ?: return
        if (hostIndex >= c.hosts.size) {
            updateLink(Link.Off)
            updateStatus(t("Couldn't reach %s. Is Crew running with phone access on, and is this phone on the same Wi-Fi (or Tailscale)?", c.name), true)
            scheduleRetry()
            return
        }
        val host = c.hosts[hostIndex]
        updateLink(Link.Connecting)
        updateStatus(t("Connecting to %s…", "${c.name} ($host)"), false)
        val socket = http.newWebSocket(Request.Builder().url("ws://$host:${c.port}").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                main.post {
                    if (ws !== webSocket) return@post
                    send(JSONObject().put("t", "hello").put("token", c.token).put("device", "Android · ${android.os.Build.MODEL}"))
                }
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                main.post { if (ws === webSocket) try { onMessage(JSONObject(text), host) } catch (e: Exception) { /* a message we don't know */ } }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                main.post {
                    if (ws !== webSocket) return@post
                    ws = null
                    if (link == Link.On) { updateLink(Link.Off); scheduleRetry() } else { hostIndex++; if (wanted) openNext() }
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                main.post { if (ws === webSocket) { ws = null; updateLink(Link.Off); scheduleRetry() } }
            }
        })
        ws = socket
    }

    private fun scheduleRetry() {
        if (!wanted) return
        val delay = minOf(15000L, 1000L shl minOf(retry++, 4))
        main.postDelayed({ if (wanted && link != Link.On) { hostIndex = 0; openNext() } }, delay)
    }

    // Back in the foreground: try at once instead of waiting for the next retry.
    fun wake() {
        if (wanted && link == Link.Off) { main.removeCallbacksAndMessages(null); hostIndex = 0; retry = 0; openNext() }
    }

    private fun updateLink(l: Link) { if (link != l) { link = l; onLink?.invoke(l) } }
    private fun updateStatus(s: String, error: Boolean) { status = s; statusError = error }

    fun send(o: JSONObject) { ws?.send(o.toString()) }
    private fun msg(type: String) = JSONObject().put("t", type)

    // ---------- what Crew sends ----------

    private fun onMessage(m: JSONObject, host: String) {
        when (m.optString("t")) {
            "denied" -> {
                wanted = false
                paired = false
                ws?.close(1000, null)
                ws = null
                updateLink(Link.Off)
                updateStatus(m.optString("reason"), true)
            }
            "ready" -> {
                retry = 0
                protocol = m.optInt("protocol", 1)
                computerName = m.optString("computer", computer?.name ?: "")
                computer?.let { c ->
                    val pc = c.copy(name = computerName, hosts = listOf(host) + c.hosts.filter { it != host })
                    computer = pc
                    computers.remember(pc)
                }
                updateLink(Link.On)
                paired = true
                updateStatus("", false)
                chatSession?.let { send(msg("chat").put("sessionId", it).put("termId", chatTerm)) } // back after a dropped connection
            }
            "state" -> onState(m)
            "msgs" -> if (m.optString("sessionId") == chatSession) {
                val items = m.optJSONArray("items").items { chatItem(it) }
                if (m.optBoolean("reset")) { chat.clear(); chat.addAll(items); chatLoaded = true; chatLoads++ } else chat.addAll(items.filter { n -> chat.none { it.id == n.id } })
            }
            "screen" -> if (m.optString("termId") == chatTerm) {
                working = m.optString("working")
                live = m.optString("live")
                ask = m.optJSONObject("ask")?.let { a ->
                    Ask(a.optString("question"), a.optJSONArray("options").items { Pair(it.optInt("n"), it.optString("label")) })
                }
            }
            "screen-text" -> if (m.optString("termId") == watching) {
                termLines = m.optJSONArray("lines").let { a ->
                    if (a == null) emptyList() else List(a.length()) { y ->
                        val runs = a.getJSONArray(y)
                        List(runs.length()) { i ->
                            val r = runs.getJSONArray(i)
                            TermRun(r.optString(0), r.optString(1).takeIf { it.startsWith("#") }?.removePrefix("#")?.toLongOrNull(16), r.optBoolean(2))
                        }
                    }
                }
                termCursor = m.optJSONArray("cursor")?.let { Pair(it.optInt(0), it.optInt(1)) }
            }
            "activity" -> onActivity(m.optString("termId"), m.optBoolean("busy"), m.optLong("ran"))
            "prompt" -> m.optJSONObject("prompt")?.let { p -> prompt(p).also { pr -> prompts.removeAll { it.id == pr.id }; prompts.add(pr); onPrompt?.invoke(pr) } }
            "prompt-done" -> prompts.removeAll { it.id == m.optInt("id") }
            "run" -> m.optJSONObject("run")?.let { r -> run(r).also { nr -> val i = runs.indexOfFirst { it.id == nr.id }; if (i >= 0) runs[i] = nr else runs.add(nr) } }
            "chips" -> {
                val sid = m.optString("sessionId")
                val c = m.optJSONObject("chips")
                if (c == null) chips.remove(sid) else chips[sid] = Chips(c.optString("model"), c.optString("effort"), c.optString("mode"), c.optBoolean("closed"))
            }
            "chip-menu" -> menus.remove(m.optInt("reqId"))?.invoke(m.optJSONObject("menu"))
            "uploaded" -> uploads.remove(m.optInt("reqId"))?.invoke(m.optString("path").takeIf { it.isNotEmpty() && !m.has("error") }, m.optString("error"))
        }
    }

    private fun onState(m: JSONObject) {
        val terms = m.optJSONArray("terminals").items { o ->
            Term(o.optString("termId"), o.optString("sessionId"), o.optString("agent"), o.optString("cwd"), o.optString("title"), o.optString("project"), o.optBoolean("busy"))
        }
        terminals.replaceWith(terms)
        projects.replaceWith(m.optJSONArray("projects").items { o -> Project(o.optString("path"), o.optString("name"), o.optJSONArray("sessions").items { session(it) }) })
        hosts.replaceWith(m.optJSONArray("hosts").items { o ->
            Host(o.optString("id"), o.optString("name"), o.optString("group"), o.optString("user"), o.optString("host"), o.optInt("port", 22),
                o.optString("workspace"), o.optBoolean("connected"), o.optJSONArray("sessions").items { session(it) })
        })
        agents.replaceWith(m.optJSONArray("agents").items { o -> AgentDef(o.optString("id"), o.optString("name"), o.optString("cli"), o.optString("access")) })
        providers.replaceWith(m.optJSONArray("providers").items { o -> Provider(o.optString("id"), o.optString("name"), o.optString("cli"), o.optString("icon").takeIf { it.isNotEmpty() && it != "null" && it.length <= 8 && !it.startsWith("{") }) })
        prompts.replaceWith(m.optJSONArray("prompts").items { prompt(it) })
        runs.replaceWith(m.optJSONArray("runs").items { run(it) })

        // A new Codex session gets its real id once Codex has written it: follow the new id.
        chatTerm?.let { tid ->
            val t = terms.find { it.termId == tid }
            if (t != null && t.sessionId != chatSession) followChat(t.sessionId, tid)
        }
        // The chat's terminal came (back): opened in Crew after the chat was, or reopened when Crew
        // restarted. Follow it, so its working line, questions and chips show.
        chatSession?.let { sid ->
            val t = terms.find { it.sessionId == sid && it.agent != "ssh" }
            if (t != null && t.termId != chatTerm) {
                chatTerm = t.termId
                send(msg("chat-term").put("termId", t.termId))
                send(msg("chips").put("sessionId", sid))
                afterOpen?.let { run -> afterOpen = null; main.postDelayed(run, 2500) } // the agent needs a moment to start
            } else if (t == null && chatTerm != null && terms.none { it.termId == chatTerm }) chatTerm = null // closed in Crew
            // No chips yet (Crew was busy, or older): ask again.
            if (t != null && chips[sid] == null) send(msg("chips").put("sessionId", sid))
        }
        checkPending()
    }

    private fun onActivity(termId: String, busy: Boolean, ran: Long) {
        val i = terminals.indexOfFirst { it.termId == termId }
        if (i < 0) return
        val t = terminals[i].copy(busy = busy)
        terminals[i] = t
        if (!busy) {
            pendingNew?.let { if (it.termId == termId) flushNew() }
            if (termId == chatTerm) chatSession?.let { send(msg("chips").put("sessionId", it)) } // may have changed
            if (ran >= 8000) onFinished?.invoke(t)
        } else if (termId == chatTerm) ask = null
    }

    // ---------- chats ----------

    // Show a session as a chat: Crew sends its transcript, then what's added, and what its
    // terminal shows (working, a question) if it's open.
    fun openChat(sessionId: String, termId: String?, agent: String, title: String) {
        chatAgent = agent
        chatTitle = title
        followChat(sessionId, termId)
        send(msg("chips").put("sessionId", sessionId)) // open in Crew or not
    }

    private fun followChat(sessionId: String, termId: String?) {
        if (sessionId != chatSession) { chat.clear(); chatLoaded = false }
        chatSession = sessionId
        chatTerm = termId
        working = ""
        live = ""
        ask = null
        send(msg("chat").put("sessionId", sessionId).put("termId", termId))
    }

    fun closeChat() {
        chatSession = null
        chatTerm = null
        chat.clear()
        working = ""
        live = ""
        ask = null
        send(msg("unchat"))
    }

    val chatBusy get() = chatTerm?.let { tid -> terminals.find { it.termId == tid }?.busy } ?: false

    // A message for an agent goes in as a paste (so newlines stay in it), then Enter.
    fun type(termId: String, text: String) {
        send(msg("input").put("termId", termId).put("data", "\u001b[200~" + text.replace(Regex("\r\n?|\n"), "\r") + "\u001b[201~"))
        main.postDelayed({ send(msg("input").put("termId", termId).put("data", "\r")) }, if (text.length > 200) 700 else 200)
    }

    fun key(data: String) { chatTerm?.let { send(msg("input").put("termId", it).put("data", data)) } }

    // A slash command, typed as you would.
    fun command(cmd: String) {
        val tid = chatTerm ?: return
        send(msg("input").put("termId", tid).put("data", cmd))
        main.postDelayed({ send(msg("input").put("termId", tid).put("data", "\r")) }, 250)
    }

    // Esc stops Claude's and Codex's current turn.
    fun stop() = key("\u001b")

    // The agent's numbered question: its number picks the row.
    fun answerAsk(n: Int) { key(n.toString()); ask = null }

    // Sends a message in the open chat; a chat that isn't open in Crew is opened there first.
    private var afterOpen: (() -> Unit)? = null
    fun openInCrew(then: () -> Unit) {
        val sid = chatSession ?: return
        if (chatTerm != null) { then(); return }
        afterOpen = then
        val p = projects.find { pr -> pr.sessions.any { it.id == sid } }
        val h = hosts.find { ho -> ho.sessions.any { it.id == sid } }
        val s = (p?.sessions ?: h?.sessions)?.find { it.id == sid }
        if (h != null) send(msg("open-host").put("hostId", h.id).put("sessionId", sid))
        else send(msg("open").put("projectPath", p?.path ?: "").put("sessionId", sid).put("agent", s?.agent ?: chatAgent).put("provider", s?.provider).put("resume", true))
        say(t("Opening it in Crew…"))
    }

    fun sendMessage(text: String) {
        val sid = chatSession ?: return
        val tid = chatTerm
        if (tid != null) { type(tid, text); return }
        val p = projects.find { pr -> pr.sessions.any { it.id == sid } }
        val h = hosts.find { ho -> ho.sessions.any { it.id == sid } }
        val s = (p?.sessions ?: h?.sessions)?.find { it.id == sid }
        if (h != null) send(msg("open-host").put("hostId", h.id).put("sessionId", sid))
        else send(msg("open").put("projectPath", p?.path ?: "").put("sessionId", sid).put("agent", s?.agent ?: chatAgent).put("provider", s?.provider).put("resume", true))
        pendingResume = PendingResume(sid, text, System.currentTimeMillis() + 30000)
        say(t("Opening it in Crew…"))
    }

    // A new chat: Crew starts the session; once it's up and settled, the first message goes in.
    fun startChat(projectPath: String, agent: String, text: String) {
        pendingNew = PendingNew(projectPath, agent, text, terminals.map { it.termId }.toSet(), System.currentTimeMillis() + 30000)
        send(msg("open").put("projectPath", projectPath).put("agent", agent).put("resume", false))
        say(t("Starting a %s session in Crew…", if (agent == "codex") "Codex" else "Claude"))
    }

    // ---------- a host's terminal ----------

    fun watch(termId: String, cols: Int, rows: Int) {
        if (watching != termId) { termLines = emptyList(); termCursor = null }
        watching = termId
        send(msg("watch").put("termId", termId).put("cols", cols).put("rows", rows))
    }

    fun unwatch() {
        if (watching == null) return
        watching = null
        send(msg("unwatch"))
    }

    // A command for the host: typed, then Enter.
    fun shellLine(text: String) {
        val tid = watching ?: return
        for (line in text.replace("\r\n", "\n").split("\n")) send(msg("input").put("termId", tid).put("data", line + "\r"))
    }

    fun termKey(data: String) { watching?.let { send(msg("input").put("termId", it).put("data", data)) } }

    // A host's terminal: the one open in Crew, or Crew opens it first.
    fun openHostTerminal(h: Host, then: (Term) -> Unit) {
        terminals.find { it.sessionId == "ssh:${h.id}" }?.let { then(it); return }
        send(msg("open-host").put("hostId", h.id).put("terminalOnly", true))
        say(t("Opening it in Crew…"))
        val until = System.currentTimeMillis() + 20000
        fun look() {
            val t = terminals.find { it.sessionId == "ssh:${h.id}" }
            if (t != null) then(t) else if (System.currentTimeMillis() < until) main.postDelayed({ look() }, 500)
        }
        look()
    }

    // A host, as clicking it in Crew does: its terminal beside an agent session, then that session here.
    fun openHost(h: Host) {
        val known = terminals.map { it.termId }.toSet()
        send(msg("open-host").put("hostId", h.id))
        say(t("Opening it in Crew…"))
        pendingHost = PendingHost(h.workspace, known, System.currentTimeMillis() + 20000)
    }

    private data class PendingNew(val project: String, val agent: String, val text: String, val known: Set<String>, val until: Long, var termId: String? = null)
    private data class PendingResume(val sessionId: String, val text: String, val until: Long)
    private data class PendingHost(val workspace: String, val known: Set<String>, val until: Long)
    private var pendingNew: PendingNew? = null
    private var pendingResume: PendingResume? = null
    private var pendingHost: PendingHost? = null

    private fun checkPending() {
        val now = System.currentTimeMillis()
        pendingNew?.let { p ->
            if (now > p.until && p.termId == null) { pendingNew = null; return@let }
            if (p.termId != null) return@let
            val t = terminals.find { it.termId !in p.known && it.agent == p.agent } ?: return@let
            p.termId = t.termId
            openChat(t.sessionId, t.termId, t.agent, t.title)
            // Typed once the agent has drawn its prompt: its first quiet moment, or 10 s at most.
            main.postDelayed({ if (pendingNew === p) flushNew() }, 10000)
        }
        pendingResume?.let { p ->
            if (now > p.until) { pendingResume = null; return@let }
            val t = terminals.find { it.sessionId == p.sessionId } ?: return@let
            pendingResume = null
            if (chatSession == p.sessionId) { chatTerm = t.termId; send(msg("chat-term").put("termId", t.termId)); send(msg("chips").put("sessionId", t.sessionId)) }
            main.postDelayed({ type(t.termId, p.text) }, 2500)
        }
        pendingHost?.let { p ->
            if (now > p.until) { pendingHost = null; return@let }
            val t = terminals.find { it.termId !in p.known && it.agent != "ssh" && sameDir(it.cwd, p.workspace) }
                ?: terminals.find { it.agent != "ssh" && sameDir(it.cwd, p.workspace) } ?: return@let
            pendingHost = null
            openChat(t.sessionId, t.termId, t.agent, t.title)
        }
    }

    private fun flushNew() {
        val p = pendingNew ?: return
        val tid = p.termId ?: return
        pendingNew = null
        if (p.text.isNotBlank()) type(tid, p.text)
    }

    // ---------- the composer's chips (Crew builds their menus and makes the picks) ----------

    private val menus = HashMap<Int, (JSONObject?) -> Unit>()
    private var seq = 0

    fun chipMenu(kind: String, done: (id: Int, items: List<MenuItem>, hint: String) -> Unit) {
        val sid = chatSession ?: return
        val reqId = ++seq
        menus[reqId] = { menu ->
            val items = menu?.optJSONArray("items").items { MenuItem(it.optString("label"), it.optString("hint"), it.optBoolean("checked"), it.optBoolean("separator")) }
            done(menu?.optInt("id") ?: 0, items, menu?.optString("hint") ?: "")
        }
        send(msg("chip-menu").put("sessionId", sid).put("kind", kind).put("reqId", reqId))
    }

    fun chipPick(menuId: Int, index: Int) {
        val sid = chatSession ?: return
        send(msg("chip-pick").put("sessionId", sid).put("menuId", menuId).put("index", index))
    }

    // ---------- the rest ----------

    fun answerPrompt(p: Prompt, value: String?) {
        send(msg("answer").put("id", p.id).put("answer", value ?: JSONObject.NULL))
        prompts.removeAll { it.id == p.id }
    }

    fun stopRun(r: Run) = send(msg("stop-run").put("id", r.id))

    // A photo or file, saved on the computer; done gets its path there.
    private val uploads = HashMap<Int, (String?, String) -> Unit>()
    fun upload(name: String, bytes: ByteArray, done: (path: String?, error: String) -> Unit) {
        if (bytes.size > 20 * 1024 * 1024) { done(null, t("Files up to 20 MB can be sent.")); return }
        val reqId = ++seq
        uploads[reqId] = done
        main.postDelayed({ uploads.remove(reqId)?.invoke(null, t("The computer didn't answer.")) }, 60000)
        send(msg("upload").put("reqId", reqId).put("name", name).put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
    }

    // ---------- parsing ----------

    private fun session(o: JSONObject) = Session(o.optString("id"), o.optString("title"), o.optString("agent", "claude"), o.optString("provider").takeIf { it.isNotEmpty() && it != "null" }, o.optLong("mtime"))
    private fun prompt(o: JSONObject) = Prompt(o.optInt("id"), o.optString("kind"), o.optString("action"), o.optString("command"), o.optString("host"))
    private fun run(o: JSONObject) = Run(o.optString("id"), o.optString("agent"), o.optString("task"), o.optString("status"), o.optString("activity"), o.optString("error"))
    private fun chatItem(o: JSONObject) = ChatItem(o.optString("id"), o.optString("role"), o.optString("text"), o.optString("name"), o.optInt("images"), o.optString("ref"), o.optBoolean("error"))
}

fun <T> JSONArray?.items(f: (JSONObject) -> T): List<T> = if (this == null) emptyList() else List(length()) { f(getJSONObject(it)) }
fun <T> MutableList<T>.replaceWith(list: List<T>) { if (this != list) { clear(); addAll(list) } }
fun sameDir(a: String, b: String) = a.replace('\\', '/').trimEnd('/').equals(b.replace('\\', '/').trimEnd('/'), ignoreCase = true)
