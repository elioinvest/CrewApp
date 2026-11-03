# Crew on your phone

[简体中文](README.md) | **English**

Use [Crew](https://github.com/elioinvest/Crew) on your computer from your phone: follow your Claude Code and Codex sessions, reply to them, and approve requests from anywhere.

| Client | Platform | Shows sessions as |
| --- | --- | --- |
| [`android/`](android) | Native Android app (Kotlin, Jetpack Compose), Android 8.0+ | Chats |
| [`www/`](www) | Web version served by Crew, works in any phone browser, iPhone included | Terminals |

## Features (Android app)

- **Chats:** your messages appear as bubbles, the agent's replies as formatted Markdown, and the tools it ran are folded into "Ran 3 steps" (tap to expand).
- **Live status:** see what the agent is doing ("Thinking… (12s)"). Numbered questions, such as permission prompts or choices, become buttons.
- **Composer:** the same model, reasoning effort and mode chips as in Crew. **＋** sends photos and files to the computer, **/** runs the agent's commands, and the send button turns into **■** stop while the agent works.
- **Sessions:** start a new chat with Claude or Codex in any project, or read old sessions without opening them in Crew.
- **Drawer:** laid out like Crew's sidebar, with projects, hosts, @-agents, agent runs and paired computers.
- **Approvals:** `crew-ssh` requests appear as cards (Allow, Allow 15 min, Deny).
- **Notifications:** the app stays connected in the background and notifies you when Crew needs an OK or a task finishes.

## Getting started

### Pair

1. In Crew on the computer, open **Settings → Phone access** and check **Let paired phones connect**.
2. In the app, tap **Scan QR code** and point it at the code, or type the address (such as `192.168.1.20:7788`) and the pairing code shown in Crew.

The phone and computer need to be on the same Wi-Fi, or anywhere if both run [Tailscale](https://tailscale.com). The app remembers the computers it has paired with.

On iPhone or other phones, scan Crew's QR code with the camera to open the web version in the browser.

### Build the Android app

Requirements: JDK 17+ and the Android SDK (platform 35, build tools 35), with `sdk.dir` set in `android/local.properties` or `ANDROID_HOME` set.

```sh
cd android
gradlew assembleRelease     # or: npm run apk
```

The APK is written to `android/app/build/outputs/apk/release/app-release.apk`. It's signed with the debug key so it installs directly; sign it with your own key to publish it. `gradlew assembleDebug` builds a debug version.

### Develop the web version

```sh
npm install
npm run vendor   # copy xterm.js into www/vendor
npm run serve    # preview locally at http://localhost:8090
npm run crew     # copy www into Crew (../Crew/phone)
```

## How it works

The app connects to Crew's phone server (`remote.js`) over a WebSocket, authenticated with the pairing code. Crew reads each session's transcript (the file Claude Code or Codex writes) and sends it as chat items. It also follows each agent's terminal screen to report what the agent is doing or asking. Messages you send are typed into the session's terminal in Crew, as if you had typed them on the computer.

## Project structure

```
android/   Native Android app
www/       Web version (no bundler; www/ is the whole app)
scripts/   vendor.js copies xterm.js; to-crew.js copies www into Crew
```

## Acknowledgements

- [OkHttp](https://square.github.io/okhttp/): WebSocket client
- [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded): QR scanning, no Google Play services needed
- [xterm.js](https://xtermjs.org/): terminal rendering for the web version

## License

[MIT](LICENSE)
