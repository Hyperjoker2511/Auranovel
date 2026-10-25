package com.example.data.scraper

import com.example.data.model.Chapter
import com.example.data.model.Novel
import com.example.data.model.ScraperExtension
import com.example.data.network.HybridNetworkClient
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URI

data class ScrapeTestResult(
    val success: Boolean,
    val itemsFound: Int = 0,
    val sampleTitles: List<String> = emptyList(),
    val sampleImages: List<String> = emptyList(),
    val sampleChapterCount: Int = 0,
    val sampleContentPreview: String = "",
    val errorDetails: String? = null,
    val executionTimeMs: Long = 0L
)

class JsoupScraperEngine(
    private val networkClient: HybridNetworkClient = HybridNetworkClient.instance
) {

    private fun parseHeaders(json: String): Map<String, String> {
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.optString(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun resolveUrl(base: String, relative: String): String {
        return try {
            if (relative.startsWith("http://") || relative.startsWith("https://")) {
                relative
            } else if (relative.startsWith("//")) {
                "https:$relative"
            } else {
                val baseUri = URI(base)
                baseUri.resolve(relative).toString()
            }
        } catch (_: Exception) {
            relative
        }
    }

    suspend fun searchNovels(extension: ScraperExtension, query: String): Result<List<Novel>> {
        return runCatching {
            val searchUrl = if (query.isBlank()) {
                extension.baseUrl
            } else {
                extension.searchUrlPattern.replace("{query}", query).replace("%s", query)
            }
            val headers = parseHeaders(extension.customHeadersJson)
            val html = networkClient.fetchHtml(searchUrl, headers)
            val doc: Document = Jsoup.parse(html, searchUrl)

            val items: Elements = doc.select(extension.novelListSelector)
            val novels = mutableListOf<Novel>()

            for (i in 0 until items.size) {
                val element: Element = items[i]
                val titleEl: Element? = if (extension.novelTitleSelector.isNotBlank()) {
                    element.selectFirst(extension.novelTitleSelector)
                } else null
                val title = titleEl?.text()?.trim() ?: element.text().trim()
                if (title.isBlank()) continue

                val urlEl: Element = if (extension.novelUrlSelector.isNotBlank()) {
                    element.selectFirst(extension.novelUrlSelector) ?: element
                } else element
                val href = urlEl.attr("href")
                val novelUrl = resolveUrl(extension.baseUrl, href)

                val coverEl: Element? = if (extension.novelCoverSelector.isNotBlank()) {
                    element.selectFirst(extension.novelCoverSelector)
                } else null
                val src = coverEl?.attr("src") ?: ""
                val dataSrc = coverEl?.attr("data-src") ?: ""
                val coverRaw = if (src.isNotBlank()) src else dataSrc
                val coverUrl = resolveUrl(extension.baseUrl, coverRaw)

                novels.add(
                    Novel(
                        id = "${extension.id}_${title.hashCode()}",
                        title = title,
                        author = "Unknown",
                        coverUrl = coverUrl,
                        description = "Discovered via ${extension.name}",
                        genres = "Web Novel",
                        status = "Ongoing",
                        sourceId = extension.id,
                        sourceUrl = novelUrl,
                        inLibrary = false
                    )
                )
            }
            novels
        }
    }

    suspend fun fetchNovelDetails(extension: ScraperExtension, novelUrl: String): Result<Pair<Novel, List<Chapter>>> {
        return runCatching {
            val headers = parseHeaders(extension.customHeadersJson)
            val html = networkClient.fetchHtml(novelUrl, headers)
            val doc: Document = Jsoup.parse(html, novelUrl)

            val title = doc.selectFirst("h1, .novel-title, .title")?.text()?.trim() ?: "Unknown Title"
            val author = doc.selectFirst(".author, span:contains(Author), a[href*='/author/']")?.text()?.trim() ?: "Author Unknown"
            val descEl = doc.selectFirst(".description, .synopsis, #description, .summary")
            val description = descEl?.text()?.trim() ?: "No description available."
            val coverEl = doc.selectFirst(".novel-cover img, .cover img, img[alt*='cover']")
            val src = coverEl?.attr("src") ?: ""
            val dataSrc = coverEl?.attr("data-src") ?: ""
            val coverRaw = if (src.isNotBlank()) src else dataSrc
            val coverUrl = resolveUrl(novelUrl, coverRaw)

            val genreElements = doc.select(".genre, .genres a, .tag")
            val genreList = mutableListOf<String>()
            for (i in 0 until genreElements.size) {
                val g = genreElements[i].text().trim()
                if (g.isNotBlank()) genreList.add(g)
            }
            val genres = if (genreList.isNotEmpty()) genreList.joinToString(", ") else "Fantasy, Adventure"

            val novelId = "${extension.id}_${title.hashCode()}"
            val novel = Novel(
                id = novelId,
                title = title,
                author = author,
                coverUrl = coverUrl,
                description = description,
                genres = genres,
                status = "Ongoing",
                sourceId = extension.id,
                sourceUrl = novelUrl,
                inLibrary = false
            )

            val chapterElements: Elements = doc.select(extension.chapterListSelector)
            val chapters = mutableListOf<Chapter>()
            var chapterIndex = 1f

            for (i in 0 until chapterElements.size) {
                val chEl: Element = chapterElements[i]
                val titleEl = if (extension.chapterTitleSelector.isNotBlank()) {
                    chEl.selectFirst(extension.chapterTitleSelector)
                } else chEl
                val chTitle = titleEl?.text()?.trim() ?: "Chapter $chapterIndex"

                val directHref = chEl.attr("href")
                val linkHref = chEl.selectFirst("a")?.attr("href") ?: ""
                val href = if (directHref.isNotBlank()) directHref else linkHref
                val chUrl = resolveUrl(novelUrl, href)

                if (chUrl.isNotBlank()) {
                    chapters.add(
                        Chapter(
                            id = "${novelId}_ch_${chapterIndex.toInt()}",
                            novelId = novelId,
                            chapterNumber = chapterIndex,
                            title = chTitle,
                            url = chUrl,
                            content = ""
                        )
                    )
                    chapterIndex += 1f
                }
            }

            Pair(novel.copy(totalChapters = chapters.size), chapters)
        }
    }

    suspend fun fetchChapterContent(extension: ScraperExtension, chapterUrl: String): Result<String> {
        return runCatching {
            val headers = parseHeaders(extension.customHeadersJson)
            val html = networkClient.fetchHtml(chapterUrl, headers)
            val doc: Document = Jsoup.parse(html, chapterUrl)

            doc.select("script, style, iframe, nav, header, footer, .ads, .advertisement, button, form").remove()

            val contentEl: Element = doc.selectFirst(extension.chapterContentSelector)
                ?: doc.selectFirst(".chapter-content, #chapter-content, .text-left, article, .entry-content")
                ?: doc.body()
                ?: throw Exception("No body element found in HTML")

            val paragraphs = contentEl.select("p")
            val pList = mutableListOf<String>()
            for (i in 0 until paragraphs.size) {
                val t = paragraphs[i].text().trim()
                if (t.isNotBlank()) pList.add(t)
            }

            if (pList.isNotEmpty()) {
                pList.joinToString("\n\n")
            } else {
                contentEl.wholeText().trim()
            }
        }
    }

    suspend fun testSandboxScraper(
        testUrl: String,
        headersJson: String,
        itemSelector: String,
        titleSelector: String,
        coverSelector: String,
        urlSelector: String,
        chapterListSelector: String,
        chapterContentSelector: String
    ): ScrapeTestResult {
        val start = System.currentTimeMillis()
        return try {
            val headers = parseHeaders(headersJson)
            val html = networkClient.fetchHtml(testUrl, headers)
            val doc: Document = Jsoup.parse(html, testUrl)

            var itemsCount = 0
            val titles = mutableListOf<String>()
            val covers = mutableListOf<String>()

            if (itemSelector.isNotBlank()) {
                val elements: Elements = doc.select(itemSelector)
                itemsCount = elements.size
                val limit = if (itemsCount > 5) 5 else itemsCount
                for (i in 0 until limit) {
                    val el: Element = elements[i]
                    val t = if (titleSelector.isNotBlank()) el.selectFirst(titleSelector)?.text() ?: "" else el.text()
                    if (t.isNotBlank()) titles.add(t.trim())

                    if (coverSelector.isNotBlank()) {
                        val cEl = el.selectFirst(coverSelector)
                        val src = cEl?.attr("src") ?: ""
                        val dataSrc = cEl?.attr("data-src") ?: ""
                        val raw = if (src.isNotBlank()) src else dataSrc
                        if (raw.isNotBlank()) covers.add(resolveUrl(testUrl, raw))
                    }
                }
            }

            var chapterCount = 0
            if (chapterListSelector.isNotBlank()) {
                val chElements = doc.select(chapterListSelector)
                chapterCount = chElements.size
            }

            var previewContent = ""
            if (chapterContentSelector.isNotBlank()) {
                val contentEl = doc.selectFirst(chapterContentSelector)
                val fullTxt = contentEl?.text() ?: ""
                previewContent = if (fullTxt.length > 400) fullTxt.substring(0, 400) else fullTxt
            }

            val elapsed = System.currentTimeMillis() - start
            ScrapeTestResult(
                success = true,
                itemsFound = itemsCount,
                sampleTitles = titles,
                sampleImages = covers,
                sampleChapterCount = chapterCount,
                sampleContentPreview = previewContent,
                executionTimeMs = elapsed
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            ScrapeTestResult(
                success = false,
                errorDetails = "${e.javaClass.simpleName}: ${e.message}",
                executionTimeMs = elapsed
            )
        }
    }

    companion object {
        fun getDefaultExtensions(): List<ScraperExtension> {
            return listOf(
                ScraperExtension(
                    id = "ext_royalroad",
                    name = "Royal Road (Fiction Hub)",
                    baseUrl = "https://www.royalroad.com",
                    lang = "en",
                    iconUrl = "https://www.royalroad.com/dist/img/logo/rr-logo-small.png",
                    version = "1.2.0",
                    searchUrlPattern = "https://www.royalroad.com/fictions/search?title={query}",
                    novelListSelector = ".fiction-list-item",
                    novelTitleSelector = ".fiction-title a",
                    novelCoverSelector = "img",
                    novelUrlSelector = ".fiction-title a",
                    chapterListSelector = "#chapters tbody tr a",
                    chapterTitleSelector = "",
                    chapterUrlSelector = "a",
                    chapterContentSelector = ".chapter-content",
                    isBuiltIn = true,
                    isEnabled = true
                ),
                ScraperExtension(
                    id = "ext_freewebnovel",
                    name = "FreeWebNovel Explorer",
                    baseUrl = "https://freewebnovel.com",
                    lang = "en",
                    version = "1.0.4",
                    searchUrlPattern = "https://freewebnovel.com/search?keyword={query}",
                    novelListSelector = ".ul-list1 .li-row",
                    novelTitleSelector = ".tit a",
                    novelCoverSelector = ".pic img",
                    novelUrlSelector = ".tit a",
                    chapterListSelector = "#chapterlist li a",
                    chapterTitleSelector = "a",
                    chapterUrlSelector = "a",
                    chapterContentSelector = "#article, .txt",
                    isBuiltIn = true,
                    isEnabled = true
                ),
                ScraperExtension(
                    id = "ext_wuxiabox",
                    name = "WuxiaBox Archives",
                    baseUrl = "https://wuxiabox.com",
                    lang = "en",
                    version = "1.1.0",
                    searchUrlPattern = "https://wuxiabox.com/search?q={query}",
                    novelListSelector = ".novel-item",
                    novelTitleSelector = ".novel-title",
                    novelCoverSelector = "img",
                    novelUrlSelector = "a",
                    chapterListSelector = ".chapter-list a",
                    chapterTitleSelector = "",
                    chapterUrlSelector = "a",
                    chapterContentSelector = ".chapter-text",
                    isBuiltIn = true,
                    isEnabled = true
                )
            )
        }
    }
}
