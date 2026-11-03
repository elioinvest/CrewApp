package app.crew.mobile

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

// Stays in the foreground while the app is connected to Crew, so the connection survives the
// app being in the background, and tells you when Crew needs an OK or a task has finished.
class CrewService : Service() {
    private val client get() = CrewApplication.instance.client

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = NotificationCompat.Builder(this, CrewApplication.CHANNEL_LINK)
            .setSmallIcon(R.drawable.ic_stat_crew)
            .setContentTitle(t("Connected to %s", client.computerName.ifEmpty { "Crew" }))
            .setContentIntent(openApp())
            .setOngoing(true)
            .setSilent(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID_LINK, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID_LINK, n)

        client.onPrompt = { p ->
            val body = if (p.kind == "approve") "${p.host}: ${p.command}" else t("%s is waiting for you on the computer", p.host)
            alert(1000 + p.id % 1000, t("Needs your OK"), body)
        }
        client.onFinished = { term ->
            if (!MainActivity.visible || client.chatTerm != term.termId) alert(term.termId.hashCode(), t("Finished"), term.title)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        client.onPrompt = null
        client.onFinished = null
        super.onDestroy()
    }

    private fun alert(id: Int, title: String, body: String) {
        val n: Notification = NotificationCompat.Builder(this, CrewApplication.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_crew)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try { getSystemService(NotificationManager::class.java).notify(id, n) } catch (e: SecurityException) { /* notifications off */ }
    }

    private fun openApp() = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)

    companion object { const val ID_LINK = 1 }
}
