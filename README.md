# 🔊 BLE Speaker

**Turn your phone into a wireless Bluetooth microphone & speaker relay.**

BLE Speaker is a native Android app that captures audio from your phone's microphone and streams it in real-time to any paired Bluetooth audio device — speakers, headphones, or hearing aids — using BLE scanning, A2DP, and ASHA audio profiles.

---

## ✨ Features

| Feature | Description |
|---|---|
| **BLE Device Scanning** | Discovers nearby Bluetooth Low Energy devices in real-time |
| **One-Tap Streaming** | Select a device and tap the circular Stream button to begin |
| **ASHA + A2DP Support** | Connects via ASHA (hearing aids) with automatic A2DP fallback |
| **Live Microphone Relay** | Captures mic audio and plays it through the connected BT device |
| **Volume Control** | Vertical slider to adjust system media volume on-the-fly |
| **Mute / Unmute** | Toggle mic mute from the persistent notification |
| **Foreground Service** | Audio streaming continues reliably in the background |
| **Notification Controls** | Mute and Stop actions directly from the notification shade |
| **Dark UI** | Sleek dark theme with accent-colored controls |

---

## 📱 Screenshots

> _Add screenshots of the app here._

---

## 🏗️ Architecture

```
com.example.ble_speaker/
├── MainActivity.kt          # UI — device list, stream button, volume slider
├── BluetoothHelper.kt       # BLE scanning, pairing, ASHA / A2DP connection
├── AudioStreamService.kt    # Foreground service — mic capture & audio playback
└── NotificationHelper.kt    # Notification channel, mute/stop action buttons
```

### Data Flow

```
Microphone ──► AudioRecord ──► PCM buffer ──► AudioTrack ──► Bluetooth Device
                                   │
                              (gain / mute)
```

1. **`MainActivity`** handles permissions, builds the programmatic UI, scans for BLE devices, and lets the user select one.
2. **`BluetoothHelper`** manages the BLE scan lifecycle, device pairing, and connects the audio profile (ASHA first, A2DP fallback).
3. **`AudioStreamService`** runs as a foreground service, reads PCM audio from the mic at 16 kHz mono, applies gain/mute, and writes it to an `AudioTrack` routed to the Bluetooth device.
4. **`NotificationHelper`** creates a persistent notification with **Mute** and **Stop** action buttons.

---

## 🔧 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Min SDK | 31 (Android 12) |
| Target SDK | 36 |
| UI | Programmatic Views (no XML layouts) |
| Audio | `AudioRecord` / `AudioTrack` (PCM 16-bit, 16 kHz, Mono) |
| Bluetooth | BLE Scanner, A2DP Profile, ASHA Profile |
| Background | Foreground Service (`microphone \| connectedDevice`) |
| Build | Gradle (Kotlin DSL) |

---

## 📋 Permissions

| Permission | Purpose |
|---|---|
| `BLUETOOTH_SCAN` | Discover nearby BLE devices |
| `BLUETOOTH_CONNECT` | Pair and connect to Bluetooth devices |
| `RECORD_AUDIO` | Capture microphone input |
| `MODIFY_AUDIO_SETTINGS` | Control audio routing and volume |
| `FOREGROUND_SERVICE` | Keep the streaming service alive |
| `FOREGROUND_SERVICE_MICROPHONE` | Foreground service type declaration |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Foreground service type declaration |
| `POST_NOTIFICATIONS` | Show the streaming status notification |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug or newer
- **JDK 11+**
- A physical Android device running **Android 12 (API 31)** or higher  
  _(BLE and audio APIs are not available on the emulator)_

### Build & Run

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/Ble_speaker.git

# 2. Open the project in Android Studio

# 3. Connect a physical device via USB or Wi-Fi debugging

# 4. Build & run
./gradlew installDebug
```

> **Note:** On first launch, the app will request Bluetooth, Microphone, and Notification permissions — all must be granted for full functionality.

---

## 📖 Usage

1. **Launch** the app — it immediately begins scanning for nearby BLE devices.
2. **Tap** a device from the list to select it (the row highlights).
3. **Press** the circular **▶ Stream** button to pair, connect, and start streaming.
4. **Adjust** volume using the vertical slider on the right.
5. **Mute / Stop** from the notification shade or tap the button again to stop.

---

## 📁 Project Structure

```
Ble_speaker/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/ble_speaker/
│   │   │   ├── MainActivity.kt
│   │   │   ├── AudioStreamService.kt
│   │   │   ├── BluetoothHelper.kt
│   │   │   └── NotificationHelper.kt
│   │   ├── res/
│   │   │   ├── drawable/         # App logo & assets
│   │   │   └── mipmap-*/         # Launcher icons
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts              # Root build file
├── settings.gradle.kts
└── gradle/                       # Gradle wrapper
```

---

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">
  Made with ❤️ by <strong>Krushna</strong>
</p>
