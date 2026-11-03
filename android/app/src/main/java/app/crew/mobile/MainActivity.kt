package app.crew.mobile

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import app.crew.mobile.ui.CrewRoot

class MainActivity : ComponentActivity() {
    private val app get() = application as CrewApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        handleLink(intent)
        // The last computer, unless a pairing link opened the app.
        if (app.client.computer == null) app.computers.all().firstOrNull()?.let { app.client.connect(it) }
        setContent { CrewRoot(app.client, app.computers) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    // crew://pair?d=… (Crew's QR code opened from the camera).
    private fun handleLink(intent: Intent?) {
        val data = intent?.data?.toString() ?: return
        Computer.parse(data)?.let { app.client.connect(it) }
    }

    override fun onStart() { super.onStart(); visible = true; app.client.wake() }
    override fun onStop() { visible = false; super.onStop() }

    companion object { @Volatile var visible = false }
}
