package com.example.data.scraper

import com.example.data.model.CommunityRepository
import com.example.data.model.ScraperExtension
import com.example.data.network.HybridNetworkClient
import org.json.JSONArray
import org.json.JSONObject

data class RepositoryManifest(
    val name: String,
    val author: String,
    val description: String,
    val sourceCount: Int,
    val extensions: List<ScraperExtension>
)

class CommunityRepoClient(
    private val networkClient: HybridNetworkClient = HybridNetworkClient.instance
) {

    fun getDefaultRepositories(): List<CommunityRepository> {
        return listOf(
            CommunityRepository(
                id = "repo_novelsourcery",
                name = "NovelSourcery Extensions",
                url = "https://raw.githubusercontent.com/NovelSourcery/extensions/repo/index.min.json",
                description = "Official NovelSourcery / LNReader extension repository with hundreds of multi-language web novel scrapers.",
                sourceCount = 80,
                isDefault = true
            ),
            CommunityRepository(
                id = "repo_official",
                name = "LNReader Community Plugins",
                url = DEFAULT_REPO_URL,
                description = "Primary community repository with verified light novel sources across all languages.",
                sourceCount = 12,
                isDefault = true
            ),
            CommunityRepository(
                id = "repo_asian",
                name = "Asian Web Fiction Index (JP / CN / KR)",
                url = "https://raw.githubusercontent.com/auranovel/index/main/asian_manifest.json",
                description = "Original web fiction providers from Syosetu, Kakuyomu, 69shu, and Biquge with auto-translation support.",
                sourceCount = 8,
                isDefault = false
            )
        )
    }


    fun getCatalogExtensions(): List<ScraperExtension> {
        return listOf(
            // English installed
            ScraperExtension(
                id = "ext_royalroad",
                name = "Royal Road",
                baseUrl = "https://www.royalroad.com",
                lang = "en",
                iconUrl = "https://www.royalroad.com/dist/img/logo/rr-logo-small.png",
                version = "1.2.4",
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
                isEnabled = true,
                isInstalled = true,
                repoId = "repo_official"
            ),
            ScraperExtension(
                id = "ext_freewebnovel",
                name = "FreeWebNovel",
                baseUrl = "https://freewebnovel.com",
                lang = "en",
                version = "1.1.0",
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
                isEnabled = true,
                isInstalled = true,
                repoId = "repo_official"
            ),
            ScraperExtension(
                id = "ext_wuxiabox",
                name = "WuxiaBox",
                baseUrl = "https://wuxiabox.com",
                lang = "en",
                version = "1.0.8",
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
                isEnabled = true,
                isInstalled = true,
                repoId = "repo_official"
            ),
            // English Catalog (Uninstalled / Available to 1-click install)
            ScraperExtension(
                id = "ext_novelfull",
                name = "NovelFull",
                baseUrl = "https://novelfull.com",
                lang = "en",
                version = "2.0.1",
                searchUrlPattern = "https://novelfull.com/search?keyword={query}",
                novelListSelector = ".list-novel .row",
                novelTitleSelector = ".novel-title a",
                novelCoverSelector = "img.cover",
                novelUrlSelector = ".novel-title a",
                chapterListSelector = ".list-chapter li a",
                chapterTitleSelector = "",
                chapterUrlSelector = "a",
                chapterContentSelector = "#chapter-content",
                isBuiltIn = false,
                isEnabled = true,
                isInstalled = false,
                repoId = "repo_official"
            ),
            ScraperExtension(
                id = "ext_scribblehub",
                name = "Scribble Hub",
                baseUrl = "https://www.scribblehub.com",
                lang = "en",
                version = "1.3.2",
                searchUrlPattern = "https://www.scribblehub.com/?s={query}&post_type=fictionposts",
                novelListSelector = ".search_main_box",
                novelTitleSelector = ".search_title a",
                novelCoverSelector = ".search_img img",
                novelUrlSelector = ".search_title a",
                chapterListSelector = ".toc_w .toc_ol li a",
                chapterTitleSelector = "",
                chapterUrlSelector = "a",
                chapterContentSelector = "#chp_raw",
                isBuiltIn = false,
                isEnabled = true,
                isInstalled = false,
                repoId = "repo_official"
            ),
            ScraperExtension(
                id = "ext_lightnovelpub",
                name = "LightNovelPub",
                baseUrl = "https://lightnovelpub.com",
                lang = "en",
                version = "1.4.0",
                searchUrlPattern = "https://lightnovelpub.com/search?kw={query}",
                novelListSelector = ".novel-item",
                novelTitleSelector = ".novel-title a",
                novelCoverSelector = ".novel-cover img",
                novelUrlSelector = ".novel-title a",
                chapterListSelector = ".chapter-list li a",
                chapterTitleSelector = "",
                chapterUrlSelector = "a",
                chapterContentSelector = "#chapter-container",
                isBuiltIn = false,
                isEnabled = true,
                isInstalled = false,
                repoId = "repo_official"
            ),
            // Japanese Catalog
            ScraperExtension(
                id = "ext_syosetu",
                name = "Syosetu (小説家になろう)",
                baseUrl = "https://ncode.syosetu.com",
                lang = "ja",
                version = "2.1.0",
                searchUrlPattern = "https://yousetsu.com/search?word={query}",
                novelListSelector = ".searchkekka_box",
                novelTitleSelector = ".novel_h a",
                novelCoverSelector = "img",
                novelUrlSelector = ".novel_h a",
                chapterListSelector = ".index_box .subtitle a",
                chapterTitleSelector = "",
                chapterUrlSelector = "a",
                chapterContentSelector = "#novel_honbun",
                isBuiltIn = false,
                isEnabled = true,
                isInstalled = false,
                repoId = "repo_asian"
            ),
            ScraperExtension(
                id = "ext_kakuyomu",
                name = "Kakuyomu (カクヨム)",
                baseUrl = "https://kakuyomu.jp",
                lang = "ja",
                version = "1.2.0",
                searchUrlPattern = "https://kakuyomu.jp/search?q={query}",
                novelListSelector = ".widget-work",
                novelTitleSelector = ".widget-work-title a",
                novelCoverSelector = "img",
                novelUrlSelector = ".widget-work-title a",
                chapterListSelector = ".widget-toc-items a",
                chapterTitleSelector = "",
                chapterUrlSelector = "a",
                chapterContentSelector = ".widget-episodeBody",
                isBuiltIn = false,
                isEnabled = true,
                isInstalled = false,
                repoId = "repo_asian"
            ),
            // Chinese Catalog
            ScraperExtension(
                id = "ext_69shu",
                name = "69shu (69书吧)",
                baseUrl = "https://www.69shu.com",
                lang = "zh",
                version = "2.0.0",
                searchUrlPattern = "https://www.69shu.com/modules/article/search.php?searchkey={query}",
                novelListSelector = ".newbox ul li",
                novelTitleSelector = "h3 a",
                novelCoverSelector = ".pic img",
                novelUrlSelector = "h3 a",
                chapterListSelector = ".mulu_list li a",
                chapterTitleSelector = "",
                chapterUrlSelector = "a",
                chapterContentSelector = ".txtnav",
                isBuiltIn = false,
                isEnabled = true,
                isInstalled = false,
                repoId = "repo_asian"
            ),
            ScraperExtension(
                id = "ext_biquge",
                name = "Biquge (笔趣阁)",
                baseUrl = "https://www.biquge.tv",
                lang = "zh",
                version = "1.5.0",
                searchUrlPattern = "https://www.biquge.tv/search.php?q={query}",
                novelListSelector = ".result-item",
                novelTitleSelector = ".result-game-item-title-link",
                novelCoverSelector = ".result-game-item-pic-link img",
                novelUrlSelector = ".result-game-item-title-link",
                chapterListSelector = "#list dd a",
                chapterTitleSelector = "",
                chapterUrlSelector = "a",
                chapterContentSelector = "#content",
                isBuiltIn = false,
                isEnabled = true,
                isInstalled = false,
                repoId = "repo_asian"
            )
        )
    }

    suspend fun fetchRepository(repoUrl: String): Result<RepositoryManifest> {
        return runCatching {
            val jsonStr = networkClient.fetchHtml(repoUrl)
            parseManifest(jsonStr)
        }
    }

    fun parseManifest(jsonStr: String): RepositoryManifest {
        val root = JSONObject(jsonStr)
        val name = root.optString("name", "Community Repository")
        val author = root.optString("author", "AuraCommunity")
        val desc = root.optString("description", "Curated novel sources")

        val list = mutableListOf<ScraperExtension>()
        val sourcesArray = root.optJSONArray("sources") ?: JSONArray()

        for (i in 0 until sourcesArray.length()) {
            val obj = sourcesArray.getJSONObject(i)
            val ext = ScraperExtension(
                id = obj.optString("id", "src_$i"),
                name = obj.optString("name", "Source $i"),
                baseUrl = obj.optString("baseUrl"),
                lang = obj.optString("lang", "en"),
                iconUrl = obj.optString("iconUrl", ""),
                version = obj.optString("version", "1.0.0"),
                searchUrlPattern = obj.optString("searchUrlPattern"),
                novelListSelector = obj.optString("novelListSelector"),
                novelTitleSelector = obj.optString("novelTitleSelector"),
                novelCoverSelector = obj.optString("novelCoverSelector"),
                novelUrlSelector = obj.optString("novelUrlSelector"),
                chapterListSelector = obj.optString("chapterListSelector"),
                chapterTitleSelector = obj.optString("chapterTitleSelector"),
                chapterUrlSelector = obj.optString("chapterUrlSelector"),
                chapterContentSelector = obj.optString("chapterContentSelector"),
                customHeadersJson = obj.optString("customHeadersJson", "{}"),
                isBuiltIn = false,
                isEnabled = true,
                isInstalled = false
            )
            list.add(ext)
        }

        return RepositoryManifest(
            name = name,
            author = author,
            description = desc,
            sourceCount = list.size,
            extensions = list
        )
    }

    companion object {
        const val DEFAULT_REPO_URL = "https://raw.githubusercontent.com/auranovel/index/main/manifest.json"
    }
}
