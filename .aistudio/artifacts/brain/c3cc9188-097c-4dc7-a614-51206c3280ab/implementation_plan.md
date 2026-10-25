# AuraNovel - Architectural Redesign & Feature Expansion Plan

## Summary
Transform AuraNovel into an industry-grade, customizable light novel and universal document reader combining the best features of Mihon, LNReader, QuickNovel, and the user's reference prototype (`AetherNovel` from `VID_f55928cf.zip`). This includes a Cyber-Aura visual overhaul, a Discover/Explore section, a comprehensive Reading History timeline, NovelSourcery repository support (`index.pb` / `index.min.json`), and a universal local document engine (EPUB, PDF, CBZ/CBR, TXT, MD, HTML).

---

## User Review & Critical Decisions

### 1. NovelSourcery Protobuf (`index.pb`) & Repository Handling
- **Decision**: NovelSourcery publishes `index.pb` alongside canonical JSON (`index.json` and `index.min.json`) on GitHub. When users paste or add URLs ending in `index.pb` (e.g. `https://github.com/NovelSourcery/extensions/raw/repo/index.pb`), the repository manager will automatically resolve and parse `index.min.json` / `index.json`, with fallback binary protobuf field extraction. This ensures 100% compatibility with NovelSourcery and standard Mihon/LNReader repo catalogs without adding bloated binary dependencies.

### 2. Universal Document Architecture
- **Decision**: Build an extensible `DocumentReaderEngine` with modular parsers:
  - **EPUB**: Streaming ZIP decompression extracting OPF manifest, spine navigation, CSS styling, and XHTML chapter rendering.
  - **PDF**: Hardware-accelerated `android.graphics.pdf.PdfRenderer` with pinch-to-zoom Compose canvas and page caching.
  - **CBZ / CBR**: Archive extraction with optimized bitmap decoding and vertical webtoon or horizontal comic paged mode.
  - **TXT / Markdown / HTML**: Streaming text reader with typography settings, auto-bookmarking, and paragraph spacing.

### 3. Visual Aesthetic & Theme ("Cyber-Aura")
- **Decision**: Incorporate the dark glassmorphic styling, neon cyan/violet gradients, glowing card borders, and smooth micro-interactions from the user's reference codebase (`AetherNovel`), strictly using Material 3 design tokens.

---

## 1. Overview & Core Concept

AuraNovel serves two primary functions:
1. **Online Novel Discovery & Extension Ecosystem**: Browse, search, install, and read from community scraper extensions across multiple online repositories (NovelSourcery, LNReader, Mihon-style).
2. **Offline Universal E-Reader**: Open any light novel, book, comic, or document from local device storage with custom fonts, themes, reading modes, and statistics.

---

## 2. User Experience & Visual Design

### Design Language & Colors
- **Surface & Background**: Pitch OLED Black (`#0B0E14`) and Deep Twilight (`#121824`) with subtle translucent glassmorphic surfaces (`#1A2234` at 85% alpha).
- **Primary & Accent Gradients**: Electric Cyan (`#00F2FE`) to Neon Purple (`#4FACFE`), with Coral Flame (`#FF5E62`) for hot/trending tags.
- **Typography**: Clean, high-legibility sans-serif with configurable serif/monospace modes in reader view.

### Screen Layouts & Navigation
- **Navigation Bar (5 Core Hubs)**:
  1. **Library**: Grid/List of tracked novels and local books with unread badges, reading progress, and category tabs.
  2. **Discover**: Hero featured banner carousel, Popular/Trending horizontal shelves, genre chips, and Unified Search across installed extensions.
  3. **History**: Chronological timeline (Today, Yesterday, Last 7 Days, Older) with quick "Continue Chapter" buttons and swipe-to-remove.
  4. **Extensions / Repos**: Repository manager (NovelSourcery, LNReader, custom URLs), install/update buttons, permission toggles, and source filters.
  5. **Settings & Stats**: Reader customization (themes, margins, font family, line height), total reading time, streaks, and storage management.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale |
| :--- | :--- | :--- |
| **Repo URL Resolution** | Auto-translate `index.pb` to `index.min.json` + `index.json` fallback | Guarantees instant compatibility with NovelSourcery without needing heavy external protoc plugins. |
| **Local File Storage** | Android `Storage Access Framework` (SAF) URI resolution with Room metadata caching | Enables reading books directly from internal storage or SD cards with zero unnecessary file duplication. |
| **Reader Pagination** | Dual mode: Continuous Vertical Scroll (webtoon/novel) & Paged Flip | Accommodates both light novel readers and comic/manga/PDF document users. |
| **History Tracking** | Room `ReadingHistory` entity tied to both online chapters and local document page offsets | Unified history tracking for both streamed web novels and local EPUB/PDF files. |

---

## 4. Technical Architecture & System Diagram

```
+-----------------------------------------------------------------------------------+
|                                  AuraNovel UI                                     |
|  [Library]     [Discover]     [History]     [Extensions/Repos]     [Settings]     |
+----------------------------------------+------------------------------------------+
                                         |
+----------------------------------------v------------------------------------------+
|                                ViewModels & State                                 |
|  DiscoverViewModel   HistoryViewModel   RepoViewModel   ReaderViewModel           |
+--------------------+-------------------+---------------+--------------------------+
                     |                   |               |
+--------------------v----+     +--------v---------+     +--------------------------+
|  RepositoryManager      |     |  DocumentReader  |     |  ScraperRuntime          |
|  - NovelSourcery URL    |     |  - EpubParser    |     |  - JS/Regex Scrapers     |
|    translators          |     |  - PdfRenderer   |     |  - Rate limiter & Cache  |
|  - Community Repos      |     |  - CBZ Extractor |     |  - Chapter Crawler       |
|  - Validation & Sync    |     |  - TXT/MD/HTML   |     |                          |
+--------------------+----+     +--------+---------+     +-------------+------------+
                     |                   |                             |
+--------------------v-------------------v-----------------------------v------------+
|                               Room Database                                       |
|  Repositories  |  Extensions  |  Novels  |  Chapters  |  History  |  Bookmarks    |
+-----------------------------------------------------------------------------------+
```

---

## 5. Phased Implementation Roadmap

### Phase 1: Repository Engine & NovelSourcery Integration
- Enhance `RepositoryManager` to handle `index.pb` -> `index.min.json` / `index.json` URL mapping.
- Update manifest parser to support NovelSourcery extension metadata (id, name, lang, version, icon, pkg).
- Add pre-configured default repositories (NovelSourcery and popular LNReader repos).

### Phase 2: Universal Document Reader Engine
- Implement `UniversalDocumentParser`:
  - `EpubParser`: Read container.xml, content.opf, NCX/NAV spine, and chapter content.
  - `PdfDocumentRenderer`: Native `android.graphics.pdf.PdfRenderer` rendering with bitmap caching.
  - `CbzParser`: ZIP extraction of comic images for paged view.
  - `TextDocumentParser`: TXT, Markdown, and HTML string parsing.
- Implement unified `UniversalReaderScreen` with customizable reader controls.

### Phase 3: Discover & Explore Hub
- Build `DiscoverScreen` with:
  - Hero featured carousel for trending novels.
  - Quick genre filtering pills (Action, Fantasy, Romance, Sci-Fi, LitRPG, Wuxia).
  - Cross-source global search to query multiple installed extensions concurrently.

### Phase 4: Reading History View
- Build `ReadingHistoryScreen`:
  - Group history items into chronological sections (Today, Yesterday, This Week, Older).
  - Card view showing novel cover, chapter title, percentage progress, and last read timestamp.
  - "Continue" floating action button on each item.
  - History management (clear all, delete single item).

### Phase 5: Cyber-Aura Visual Polish & Verification
- Apply glassmorphism, glowing borders, modern cards, and dark neon color tokens.
- Add app icon and launcher resources.
- Run `compile_applet` and test suite to ensure clean build.
