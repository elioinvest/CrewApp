package app.crew.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.core.content.ContextCompat

class CrewApplication : Application() {
    lateinit var computers: Computers
    lateinit var client: CrewClient

    override fun onCreate() {
        super.onCreate()
        instance = this
        computers = Computers(this)
        client = CrewClient(computers)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_LINK, t("Crew connection"), NotificationManager.IMPORTANCE_MIN))
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ALERTS, t("Approvals and finished tasks"), NotificationManager.IMPORTANCE_HIGH))
        // While connected, a foreground service keeps the connection (and its notifications) alive.
        client.onLink = { l ->
            val i = Intent(this, CrewService::class.java)
            if (l == Link.On) ContextCompat.startForegroundService(this, i) else if (l == Link.Off) stopService(i)
        }
    }

    companion object {
        const val CHANNEL_LINK = "link"
        const val CHANNEL_ALERTS = "alerts"
        lateinit var instance: CrewApplication
    }
}
