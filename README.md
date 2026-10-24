# BWM AI — Personal Phone Assistant

> A modern, native Android AI personal assistant powered by Google Gemini and native Android system APIs.

---

## 🚀 Overview

**BWM AI** is a voice-controlled native Android personal assistant built from the ground up using **Kotlin** and **Jetpack Compose**. It understands natural commands in **Hindi, Hinglish, and English**, executes native device actions, launches installed applications, manages notes, and supports hands-free on-device wake-word activation (**"Hey BWM"**).

---

## ✨ Features

- **🎙️ Voice Engine & Speech Recognition**:
  - Live waveform visualizer with real-time audio RMS dB feedback.
  - Multi-language recognition: Hindi (`hi-IN`), Indian English / Hinglish (`en-IN`), and English (`en-US`).
  - Native Text-to-Speech (TTS) with interruption, pitch, and speed controls.
  - On-device wake-word detection for **"Hey BWM"** / **"Suno BWM"**.

- **🤖 Intelligent Reasoning**:
  - Direct integration with **Google Gemini API** for structured intent analysis and multi-step tasks.
  - Robust offline NLP parser fallback for continuous device control without internet.

- **📱 Deep Native Phone Control**:
  - **App Launcher**: Instant package discovery and launching with fuzzy matching.
  - **Flashlight**: CameraManager-backed toggle.
  - **Volume & Audio**: Step adjustments and percentage volume controls.
  - **Battery & Charging**: Real-time battery percentage, charging state, and power status.
  - **Timers & Alarms**: System AlarmClock integration.
  - **Phone Dialer**: One-tap phone dialing with confirmation.
  - **Settings Shortcuts**: Direct access to Wi-Fi, Bluetooth, Accessibility, and App details.

- **🛡️ Screen-Aware Navigation & Automation**:
  - Global navigation actions via `AccessibilityService` (Back, Home, Notifications, Quick Settings, Recents).
  - Explicit user authorization and privacy controls.

- **💾 Local Persistence**:
  - Built-in Room database for conversation history, quick voice notes, and executed action audit logs.

- **🎧 Background Listening**:
  - Foreground service with persistent notification and pause/resume controls.

---

## 📦 Download APK

Direct download from GitHub Releases:
- Download the latest APK from the **[Releases](https://github.com/newsatya123/Bwm-ai/releases)** section.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3 Dark Navy & Neon Cyan palette)
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Database**: Android Jetpack Room
- **AI / Agent**: Google Gemini API via OkHttp REST + Offline NLP Parser
- **Voice**: Android SpeechRecognizer & TextToSpeech
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## ⚙️ Configuration

To enable online Gemini AI reasoning:
1. Obtain an API key from [Google AI Studio](https://aistudio.google.com).
2. Add your key to `.env` or the Secrets panel:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
*(Even without an API key, BWM AI functions fully using its built-in offline NLP engine!)*

---

## 📄 License

MIT License.
