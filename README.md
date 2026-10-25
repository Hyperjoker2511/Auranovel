# AuraNovel 📖✨

> A production-ready, fully native Android light novel and web fiction reader built 100% with Kotlin, Jetpack Compose, Room Database, and OkHttp. Designed to deliver an edge-to-edge Material 3 experience with LNReader / Mihon-style extension ecosystems, QuickNovel-style batch EPUB generation, and on-the-fly AI translation.

---

## 🌟 Key Features

### 1. 📚 Library & Category Management (Mihon & LNReader Style)
- **Categorized Reading:** Organize novels into *Reading*, *Favorites*, *Completed*, and *Local Files*.
- **EPUB & TXT Parsing:** Native zero-dependency importer that unpacks `.epub` (OPF, manifest, spine) and `.txt` documents.
- **Sorting & Quick Filters:** Sort by *Last Read*, *Title*, and *Chapter Count*.
- **Continue Reading Hero Banner:** 1-tap resumption with progress tracking.

### 2. ⚡ Batch Downloader & EPUB Compiler (QuickNovel Style)
- **Batch Downloads:** Download next 5, 10, 25, or all unread chapters in the background for offline reading.
- **Standalone EPUB Generator:** Compile and export entire online or imported novels into standards-compliant `.epub` files for external archiving.

### 3. 🌐 Visual Extension Wizard & Community Repositories
- **Visual Scraper Wizard:** Create custom novel scrapers without writing code using 1-click template presets (*WordPress/Madara*, *Wuxia Grid*, *Classic Archive*, or *Pro CSS Selectors*).
- **Isolated Sandbox Testing:** Live simulation showing items found, sample titles, cover previews, and DOM response time.
- **Community Repositories Hub:** Browse, install, update, and manage extensions grouped by language (🇬🇧 English, 🇯🇵 Japanese, 🇨🇳 Chinese, 🇰🇷 Korean). Pre-configured with working community sources (*Royal Road, FreeWebNovel, WuxiaBox, NovelFull, Scribble Hub, Syosetu, 69shu, etc.*).

### 4. 🧠 AI Translation & Multilingual Suite
- **On-the-Fly Localization:** Instant 1-tap toggle in the reader between original foreign source text and translated text.
- **Connectors:** Built-in Smart Terminology Translator, Google Translate Web, DeepL, and OpenAI/LLM endpoints.
- **Offline Cache:** Translations are stored locally in Room DB for offline reading.

### 5. 🛡️ Cloudflare & Anti-Bot Protection Layer
- **Hybrid OkHttp Engine:** Cookie synchronization with Android's system `CookieManager`.
- **Interactive WebView Solver:** Automatically intercepts 403/503 Cloudflare/Turnstile challenges and captures `cf_clearance` tokens into the network jar.

### 6. 🎮 Gamification Engine & "Me" Cultivation
- **Anti-AFK Protection:** Automatically pauses reading XP accrual when idle for over 60 seconds.
- **Leveling & Unlockable Titles:** Advance through levels to unlock arcane titles (*Novice Reader, Arcane Scholar, Domain Master, Grand Sovereign, Omniscient Reader*).
- **8 Achievement Badges:** *First Words, Page Storm, Night Owl, Marathoner, Polyglot, Extension Architect, E-Ink Master, Imperial Librarian*.
- **External Trackers:** Sync progress to **AniList**, **MyAnimeList**, **Kitsu**, and **NovelUpdates**.

### 7. 📖 Highly Customizable Reader Engine
- **Reading Modes:** Continuous Vertical Scroll and Paged Flip Mode.
- **Color Palettes:** Midnight Purple, Pure AMOLED Black, Warm Parchment/Sepia, Forest Jade Emerald, Paper Light, and **E-Ink High Contrast Mode** (0-animation, tailored for Onyx Boox / e-readers).
- **Typography:** Custom font size, line-height spacing (1.2x–2.4x), margins, typefaces (Serif, Sans, Monospace), and text alignment (Left / Justified).
- **Text-to-Speech (TTS):** Real-time sentence highlighting, playback speed (0.75x–2.0x), pitch, and floating player.
- **In-Chapter Search:** Fast text search with live highlight navigation.
- **Status Overlay:** Clean battery percentage and time clock.

---

## 🛠️ Tech Stack & Architecture

- **Language:** 100% Kotlin
- **UI Toolkit:** Jetpack Compose & Material 3 Expressive
- **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Local Persistence:** Room Database 2.7
- **Networking:** OkHttp 4.10 with custom CookieJar & Jsoup 1.18 for DOM extraction
- **Image Loading:** Coil 3
- **Audio:** Native Android `TextToSpeech`
- **Target SDK:** Android 14+ (minSdk 24, compatible down to Android 7.0+)

---

## 🚀 How to Publish to GitHub from Google AI Studio

1. Open the top-right menu in **Google AI Studio**.
2. Select **Settings** > **Export to GitHub** (or click **Download as ZIP**).
3. Connect your GitHub account and target repository.
4. All source code, Gradle configurations, adaptive icons, and documentation will be pushed directly!
