# Crew on your phone

Use [Crew](https://github.com/elioinvest/Crew) on your computer from your phone. This project has two clients:

- **`android/`: the Android app.** A native app (Kotlin, Jetpack Compose) that shows each session as a chat.
- **`www/`: the web version.** Crew serves it to any phone browser, iPhone included, at the address in Settings → Phone access. It shows sessions as their terminal.

## The Android app

- **Chats:** each session reads as a conversation. What you said is in a bubble, the agent's replies are formatted Markdown, and the tools it ran are folded into "Ran 3 steps" (tap to see them).
- **Live status:** while the agent works you see what it's doing ("Thinking… (12s)"). When it asks a numbered question, such as a permission prompt or a choice, the choices are buttons.
- **Composer:** the same chips as in Crew show the model, reasoning effort and mode. Tapping one opens Crew's own list for it. **＋** sends photos and files to the computer, **/** runs the agent's commands, and the send button turns into **■** stop while the agent works.
- **New chats:** start one from the home screen in any project, with Claude or Codex.
- **Old sessions:** read them without opening them in Crew. Sending a message opens the session there.
- **Drawer:** laid out like Crew's sidebar. It has projects with their sessions grouped by agent (Claude, GPT, each custom provider), hosts by group with their sessions, and @-agents (tap one to mention it). Agent runs and your paired computers are in the drawer too.
- **Approvals:** `crew-ssh` requests appear as cards (Allow, Allow 15 min, Deny).
- **Notifications:** while connected, the app stays connected in the background. It notifies you when Crew needs an OK or when a task finishes.

### Pair

1. In Crew on the computer, open **Settings → Phone access** and check **Let paired phones connect**.
2. In the app, tap **Scan QR code** and point it at the code. Or type the address (`192.168.1.20:7788`) and the pairing code shown in Crew.

The phone and the computer need to be on the same Wi-Fi, or anywhere if both have [Tailscale](https://tailscale.com). The app remembers the computers it has paired with.

### Build

You need JDK 17 or later and the Android SDK (platform 35, build tools 35), with `sdk.dir` in `android/local.properties` or `ANDROID_HOME` set.

```
cd android
gradlew assembleRelease
```

The APK is written to `android/app/build/outputs/apk/release/app-release.apk`. It's signed with the debug key so it installs directly; use your own key to publish it. `gradlew assembleDebug` builds a debug version.

### How it works

The app connects to Crew's phone server (Crew's `remote.js`) over a WebSocket, authenticated with the pairing code. Crew reads each session's transcript, the file Claude Code or Codex writes, and sends it as chat items. It also follows each agent's terminal screen to report what the agent is doing or asking. Messages are typed into the session's terminal in Crew, as if you had typed them on the computer.

Libraries: [OkHttp](https://square.github.io/okhttp/) for the WebSocket and [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded) for the QR scanner. ZXing doesn't need Google Play services, so scanning works on phones without them. Both are Apache 2.0.

## The web version

Crew serves `www` itself. Scanning Crew's QR code with the phone's camera opens it in the browser. After changing `www`, run `npm run crew` to copy it into Crew (`../Crew/phone`).
