package app.crew.mobile

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

// A computer running Crew: its name, addresses (the Wi-Fi one first, then Tailscale's), port and
// pairing code.
data class Computer(val name: String, val hosts: List<String>, val port: Int, val token: String) {
    fun json() = JSONObject().put("name", name).put("hosts", JSONArray(hosts)).put("port", port).put("token", token)

    companion object {
        fun from(o: JSONObject) = Computer(
            o.optString("name", "Computer"),
            o.optJSONArray("hosts")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
            o.optInt("port", 7788),
            o.optString("token"),
        )

        // Crew's QR code: http://<computer>:7788/?d=<base64url JSON { v, name, port, token, hosts }>
        // (or crew://pair?d=…).
        fun parse(text: String): Computer? {
            val d = Regex("[?&]d=([A-Za-z0-9_-]+)").find(text)?.groupValues?.get(1) ?: return null
            return try {
                val o = JSONObject(String(Base64.decode(d, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)))
                val c = from(o)
                if (c.token.isBlank() || c.hosts.isEmpty()) null else c
            } catch (e: Exception) {
                null
            }
        }
    }
}

// The paired computers, the last used first.
class Computers(context: Context) {
    private val prefs = context.getSharedPreferences("crew", Context.MODE_PRIVATE)

    fun all(): List<Computer> = try {
        val a = JSONArray(prefs.getString("computers", "[]"))
        List(a.length()) { Computer.from(a.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    fun remember(c: Computer) = save(listOf(c) + all().filter { it.token != c.token })
    fun forget(c: Computer) = save(all().filter { it.token != c.token })

    private fun save(list: List<Computer>) {
        prefs.edit().putString("computers", JSONArray(list.take(6).map { it.json() }).toString()).apply()
    }
}
