package app.crew.mobile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.crew.mobile.Computer
import app.crew.mobile.Computers
import app.crew.mobile.CrewClient
import app.crew.mobile.Link
import app.crew.mobile.R
import app.crew.mobile.t
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun CrewRoot(client: CrewClient, computers: Computers) {
    CrewTheme {
        val p = LocalPalette.current
        Box(Modifier.fillMaxSize().background(p.bg)) {
            // Paired and connected (or reconnecting after a drop): the app; otherwise pairing.
            if (client.paired) MainScreen(client)
            else ConnectScreen(client, computers)
            Toast(client)
        }
    }
}

@Composable
fun CrewLogo(size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size / 4.5).dp))) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}

@Composable
fun ConnectScreen(client: CrewClient, computers: Computers) {
    val p = LocalPalette.current
    var host by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val saved = remember { mutableStateListOf<Computer>().apply { addAll(computers.all()) } }
    val scan = androidx.activity.compose.rememberLauncherForActivityResult(ScanContract()) { r ->
        val text = r.contents ?: return@rememberLauncherForActivityResult
        val c = Computer.parse(text)
        if (c == null) error = t("That isn't Crew's pairing code.") else { error = ""; client.connect(c) }
    }
    LaunchedEffect(client.link) { saved.clear(); saved.addAll(computers.all()) }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 90.dp, bottom = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CrewLogo(40)
            Spacer(Modifier.size(12.dp))
            Text(t("Use Crew from your phone"), color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(t("In Crew on your computer, open Settings → Phone access, turn it on, then scan its QR code."), color = p.muted, fontSize = 15.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 24.dp, start = 8.dp, end = 8.dp))
        PillButton(t("Scan QR code"), primary = true) {
            scan.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false).setOrientationLocked(false).setPrompt(""))
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().border(1.dp, p.line, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp).padding(top = 6.dp, bottom = 14.dp)) {
            Field(host, t("Address, like 192.168.1.20:7788")) { host = it }
            HorizontalDivider(color = p.line)
            Field(token, t("Pairing code")) { token = it }
            HorizontalDivider(color = p.line)
            Spacer(Modifier.height(14.dp))
            PillButton(t("Connect")) {
                val parts = host.trim().split(":")
                val code = token.trim().substringAfterLast("·").trim()
                if (parts[0].isEmpty() || code.isEmpty()) error = t("Enter the address and the pairing code shown in Crew.")
                else { error = ""; client.connect(Computer(parts[0], listOf(parts[0]), parts.getOrNull(1)?.toIntOrNull() ?: 7788, code)) }
            }
        }
        val status = error.ifEmpty { client.status }
        if (status.isNotEmpty()) Text(status, color = if (error.isNotEmpty() || client.statusError) p.red else p.muted, fontSize = 14.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp))
        if (saved.isNotEmpty()) {
            Text(t("Computers"), color = p.muted, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp))
            for (c in saved) Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { client.connect(c) }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Ico.MONITOR, p.muted, 22.dp)
                Column(Modifier.weight(1f)) {
                    Text(c.name, color = p.text, fontSize = 16.sp)
                    Text(c.hosts.firstOrNull() ?: "", color = p.muted, fontSize = 13.sp)
                }
                Text(t("Forget"), color = p.muted, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { computers.forget(c); saved.remove(c) }.padding(8.dp))
            }
        }
    }
}

@Composable
fun PillButton(label: String, primary: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(if (primary) p.blue else p.fill).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = if (primary) androidx.compose.ui.graphics.Color.White else p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Field(value: String, hint: String, onChange: (String) -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, color = p.faint, fontSize = 16.sp)
        BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = p.text, fontSize = 16.sp), cursorBrush = SolidColor(p.blue), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun Toast(client: CrewClient) {
    val p = LocalPalette.current
    var shown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(client.toast) {
        val m = client.toast ?: return@LaunchedEffect
        shown = m.second
        kotlinx.coroutines.delay(2600)
        shown = null
    }
    shown?.let { s ->
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(top = 64.dp), contentAlignment = Alignment.TopCenter) {
            Text(s, color = p.bg, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(p.text).padding(horizontal = 16.dp, vertical = 9.dp))
        }
    }
}
