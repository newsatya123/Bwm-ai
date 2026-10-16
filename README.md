# BWM AI — Personal Phone Assistant 🎙️⚡

**BWM AI** is a modern, native Android personal phone assistant built with **Kotlin** and **Jetpack Compose**. It provides real, voice-controlled smartphone automation, powered by **Gemini 3.5 Flash** structured function calling with an offline **Hybrid NLP Rule Engine** for full device autonomy.

---

## 🌟 Key Features

### 1. 🎨 Modern Dark Navy UI with Dynamic Visualizers
- **Futuristic Orb Visualizer**: Audio-reactive pulsating AI orb with layered ambient glows reflecting 6 assistant states (*Idle, Listening, Processing, Executing, Speaking, Error*).
- **Live Sound Waveform**: Real-time canvas waveform dancing to speech input amplitude.
- **Interim Speech Feedback**: See spoken words transcribed live on-screen before processing.
- **One-Tap Quick Action Chips**: Instant shortcuts for Torch, Battery, Volume, Alarms, Timers, Notes, and Apps.

### 2. 🗣️ Multilingual Voice Engine (Hindi, Hinglish & English)
- **Speech-to-Text**: Native `SpeechRecognizer` integration with multilingual support (`hi-IN`, `en-IN`, `en-US`).
- **Text-to-Speech (TTS)**: Android `TextToSpeech` engine with custom pitch, speech rate sliders, and instant touch-to-interrupt capability.
- **Modular Wake-Word Trigger**: Modular on-device voice trigger interface.

### 3. 📱 Native Device Hardware & App Control
- **App Launcher**: Real `PackageManager` discovery (`<queries>` support) with smart fuzzy-matching for apps like WhatsApp, YouTube, Camera, Calculator, Chrome, Maps, Clock, and Gallery.
- **Flashlight Control**: Toggle torch on/off via `CameraManager`.
- **Volume Controller**: Adjust media volume up/down/mute/max or set percentages via `AudioManager`.
- **Battery Status**: Live battery capacity, charging state (⚡), and health monitoring.
- **Alarms & Timers**: Native `AlarmClock.ACTION_SET_ALARM` and `ACTION_SET_TIMER`.
- **Phone Dialer**: Safe intent-based dialing with explicit user confirmation for security.
- **Settings Shortcuts**: Deep-links to Wi-Fi, Bluetooth, Sound, Display, Accessibility, and App details.

### 4. 🧠 Gemini AI Agent with Function Calling + Offline Hybrid NLP
- Uses **`gemini-3.5-flash`** via direct REST API with typed tool declarations.
- Converts natural language into structured actions: `launch_app`, `toggle_flashlight`, `adjust_volume`, `get_battery_status`, `set_timer`, `set_alarm`, `save_note`, `dial_phone`, etc.
- **Offline NLP Engine**: Understands natural Hindi, Hinglish, and English commands (e.g. *"Flashlight chalu karo"*, *"WhatsApp kholo"*, *"5 minute ka timer lagao"*, *"Battery kitni hai"*, *"Volume badhao"*) even without internet or when the API key is not yet set.

### 5. 🛡️ Background Listening Service & Accessibility
- **Foreground Microphone Service**: Runs `VoiceAssistantService` with type `microphone` (Android 14+ compatible) and persistent notification with Pause/Resume and Stop controls.
- **BwmAccessibilityService**: Optional Android `AccessibilityService` for global phone navigation (*Back*, *Home*, *Notifications*, *Quick Settings*) with explicit user consent.

### 6. 🗄️ Local Persistence & Privacy
- **Room SQLite Database**: Stores conversation transcripts, action audits, and quick notes locally on-device.
- **Privacy First**: Raw voice audio is never stored or exported. Complete **Device Memory Wipe** kill-switch in Settings.

---

## 🛠️ Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Database**: Room Database (`AssistantDatabase`, `AssistantDao`)
- **Networking**: OkHttp 4, Retrofit 2, Moshi
- **AI Model**: Google Gemini (`gemini-3.5-flash`)
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)

---

## 📋 Common Voice Commands

| Category | Example Commands (English / Hinglish / Hindi) |
|---|---|
| **Flashlight** | *"Turn on flashlight"*, *"Torch band karo"*, *"Batti jalao"* |
| **Apps** | *"Open WhatsApp"*, *"YouTube chalao"*, *"Camera kholo"*, *"Calculator open karo"* |
| **Battery** | *"Check battery"*, *"Battery kitni hai?"*, *"Is phone charging?"* |
| **Volume** | *"Increase volume"*, *"Awaz kam karo"*, *"Mute media"*, *"Volume full karo"* |
| **Timers** | *"Set timer for 5 minutes"*, *"10 minute ka timer lagao"* |
| **Alarms** | *"Set alarm for 7:00 AM"*, *"Subah 6 baje ka alarm lagao"* |
| **Notes** | *"Save note: Buy milk"*, *"Yaad rakhna meeting at 4 PM"* |
| **Settings** | *"Open Wi-Fi settings"*, *"Bluetooth settings kholo"* |
| **Dialer** | *"Call 198"*, *"Phone dialer open karo"* |
