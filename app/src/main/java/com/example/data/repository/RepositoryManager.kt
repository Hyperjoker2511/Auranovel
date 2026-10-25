package com.example.data.repository

import com.example.data.db.ExtensionDao
import com.example.data.db.RepositoryDao
import com.example.data.model.CommunityRepository
import com.example.data.model.ScraperExtension
import com.example.data.network.HybridNetworkClient
import com.example.data.scraper.CommunityRepoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject

sealed class RepoValidationResult {
    data class Valid(val normalizedUrl: String) : RepoValidationResult()
    data class Invalid(val reason: String) : RepoValidationResult()
}

data class SyncResult(
    val isSuccess: Boolean,
    val repositoryName: String,
    val totalSourcesFound: Int,
    val newSourcesCount: Int,
    val errorMessage: String? = null
)

class RepositoryManager(
    private val repositoryDao: RepositoryDao,
    private val extensionDao: ExtensionDao,
    private val networkClient: HybridNetworkClient = HybridNetworkClient.instance
) {

    val allRepositories: Flow<List<CommunityRepository>> = repositoryDao.getAllRepositories()

    /**
     * Normalizes and validates repository and GitHub manifest URLs.
     * Automatically converts github.com URLs (blob/raw) to raw.githubusercontent.com,
     * and maps index.pb / index.json to index.min.json for NovelSourcery & LNReader repos.
     */
    fun validateAndNormalizeUrl(rawUrl: String): RepoValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return RepoValidationResult.Invalid("URL cannot be empty")
        }

        var candidate = trimmed
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
            candidate = "https://$candidate"
        }

        // NovelSourcery .pb mapping -> index.min.json
        if (candidate.endsWith("index.pb")) {
            candidate = candidate.replace("index.pb", "index.min.json")
        }

        // Convert GitHub web viewer and raw URLs to raw.githubusercontent.com
        if (candidate.contains("github.com")) {
            if (candidate.contains("/raw/")) {
                candidate = candidate.replace("github.com", "raw.githubusercontent.com")
                    .replace("/raw/", "/")
            } else if (candidate.contains("/blob/")) {
                candidate = candidate.replace("github.com", "raw.githubusercontent.com")
                    .replace("/blob/", "/")
            }
        }

        val httpUrl = candidate.toHttpUrlOrNull()
            ?: return RepoValidationResult.Invalid("Malformed URL syntax: '$rawUrl'")

        if (httpUrl.scheme != "http" && httpUrl.scheme != "https") {
            return RepoValidationResult.Invalid("Only HTTP and HTTPS protocols are supported")
        }

        if (httpUrl.host.isBlank()) {
            return RepoValidationResult.Invalid("URL must contain a valid hostname")
        }

        return RepoValidationResult.Valid(httpUrl.toString())
    }

    /**
     * Validates whether a scraped website base URL or extension URL is safe.
     */
    fun isValidExtensionUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val candidate = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        val parsed = candidate.toHttpUrlOrNull() ?: return false
        return (parsed.scheme == "http" || parsed.scheme == "https") && parsed.host.isNotBlank()
    }

    /**
     * Seeds initial repositories into Room DB if empty.
     */
    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        val defaultClient = CommunityRepoClient()
        val defaultRepos = defaultClient.getDefaultRepositories()
        for (repo in defaultRepos) {
            val existing = repositoryDao.getRepositoryById(repo.id)
            if (existing == null) {
                repositoryDao.insertOrUpdateRepository(repo)
            }
        }
    }

    /**
     * Fetches, validates, and stores a repository manifest from GitHub or remote URL into Room DB.
     * Seamlessly supports both NovelSourcery / LNReader JSON array formats and object manifests.
     */
    suspend fun fetchAndStoreManifest(
        name: String,
        rawUrl: String,
        isDefault: Boolean = false
    ): SyncResult = withContext(Dispatchers.IO) {
        val validation = validateAndNormalizeUrl(rawUrl)
        if (validation is RepoValidationResult.Invalid) {
            return@withContext SyncResult(
                isSuccess = false,
                repositoryName = name,
                totalSourcesFound = 0,
                newSourcesCount = 0,
                errorMessage = validation.reason
            )
        }

        val normalizedUrl = (validation as RepoValidationResult.Valid).normalizedUrl

        try {
            var jsonString = networkClient.fetchHtml(normalizedUrl)
            // Fallback attempt with index.json if index.min.json was 404 or empty
            if (jsonString.isBlank() && normalizedUrl.endsWith("index.min.json")) {
                val fallbackUrl = normalizedUrl.replace("index.min.json", "index.json")
                jsonString = networkClient.fetchHtml(fallbackUrl)
            }

            val trimmedJson = jsonString.trim()
            if (trimmedJson.isBlank()) {
                return@withContext SyncResult(
                    isSuccess = false,
                    repositoryName = name,
                    totalSourcesFound = 0,
                    newSourcesCount = 0,
                    errorMessage = "Server returned empty response from '$normalizedUrl'"
                )
            }

            val repoId = "repo_${normalizedUrl.hashCode()}"
            var repoTitle = name
            var repoDesc = "Community extension repository"
            val parsedExtensions = mutableListOf<ScraperExtension>()

            if (trimmedJson.startsWith("[")) {
                // LNReader / NovelSourcery root array format: [ { "id": "...", "name": "...", "site": "..." }, ... ]
                val jsonArray = JSONArray(trimmedJson)
                if (repoTitle.isBlank() || repoTitle.equals("Custom Repository", ignoreCase = true)) {
                    repoTitle = if (rawUrl.contains("NovelSourcery", ignoreCase = true)) "NovelSourcery Extensions" else "Community Repository"
                }
                repoDesc = "Community sources index (${jsonArray.length()} sources available)"

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "${repoId}_src_$i")
                    val srcName = obj.optString("name", "Source $i")
                    val rawBaseUrl = obj.optString("site", obj.optString("baseUrl", obj.optString("url", "")))
                    val baseUrl = if (rawBaseUrl.isNotBlank() && !rawBaseUrl.startsWith("http://") && !rawBaseUrl.startsWith("https://")) {
                        "https://$rawBaseUrl"
                    } else rawBaseUrl

                    val existingLocal = extensionDao.getExtensionById(id)
                    val isInstalled = existingLocal?.isInstalled ?: false

                    val ext = ScraperExtension(
                        id = id,
                        name = srcName,
                        baseUrl = if (baseUrl.isNotBlank()) baseUrl else "https://auranovel.internal/$id",
                        lang = obj.optString("lang", "en"),
                        iconUrl = obj.optString("icon", obj.optString("iconUrl", "")),
                        version = obj.optString("version", "1.0.0"),
                        searchUrlPattern = obj.optString("searchUrlPattern", if (baseUrl.isNotBlank()) "$baseUrl/search?q={query}" else ""),
                        novelListSelector = obj.optString("novelListSelector", ".novel-item, .fiction-list-item, .searchkekka_box"),
                        novelTitleSelector = obj.optString("novelTitleSelector", ".novel-title, .fiction-title a, h3 a"),
                        novelCoverSelector = obj.optString("novelCoverSelector", "img"),
                        novelUrlSelector = obj.optString("novelUrlSelector", "a"),
                        chapterListSelector = obj.optString("chapterListSelector", ".chapter-list a, #chapters a, .mulu_list a"),
                        chapterTitleSelector = obj.optString("chapterTitleSelector", ""),
                        chapterUrlSelector = obj.optString("chapterUrlSelector", "a"),
                        chapterContentSelector = obj.optString("chapterContentSelector", ".chapter-content, #content, .txtnav, #novel_honbun"),
                        customHeadersJson = obj.optString("customHeadersJson", "{}"),
                        isBuiltIn = false,
                        isEnabled = true,
                        isInstalled = isInstalled,
                        repoId = repoId
                    )
                    parsedExtensions.add(ext)
                }
            } else {
                // Object format: { "name": "...", "sources": [ ... ] } or { "plugins": [ ... ] }
                val rootJson = JSONObject(trimmedJson)
                repoTitle = rootJson.optString("name", name).ifBlank { name }
                repoDesc = rootJson.optString("description", "Community extension repository")
                val sourcesArray = rootJson.optJSONArray("sources")
                    ?: rootJson.optJSONArray("plugins")
                    ?: rootJson.optJSONArray("extensions")
                    ?: JSONArray()

                for (i in 0 until sourcesArray.length()) {
                    val sourceObj = sourcesArray.optJSONObject(i) ?: continue
                    val id = sourceObj.optString("id", "${repoId}_src_$i")
                    val srcName = sourceObj.optString("name", "Unknown Source")
                    val rawBaseUrl = sourceObj.optString("baseUrl", sourceObj.optString("site", sourceObj.optString("url", "")))
                    val baseUrl = if (rawBaseUrl.isNotBlank() && !rawBaseUrl.startsWith("http://") && !rawBaseUrl.startsWith("https://")) {
                        "https://$rawBaseUrl"
                    } else rawBaseUrl

                    val existingLocal = extensionDao.getExtensionById(id)
                    val isInstalled = existingLocal?.isInstalled ?: false

                    val ext = ScraperExtension(
                        id = id,
                        name = srcName,
                        baseUrl = if (baseUrl.isNotBlank()) baseUrl else "https://auranovel.internal/$id",
                        lang = sourceObj.optString("lang", "en"),
                        iconUrl = sourceObj.optString("iconUrl", sourceObj.optString("icon", "")),
                        version = sourceObj.optString("version", "1.0.0"),
                        searchUrlPattern = sourceObj.optString("searchUrlPattern", "$baseUrl/search?q={query}"),
                        novelListSelector = sourceObj.optString("novelListSelector", ".novel-item"),
                        novelTitleSelector = sourceObj.optString("novelTitleSelector", ".novel-title"),
                        novelCoverSelector = sourceObj.optString("novelCoverSelector", "img"),
                        novelUrlSelector = sourceObj.optString("novelUrlSelector", "a"),
                        chapterListSelector = sourceObj.optString("chapterListSelector", ".chapter-list a"),
                        chapterTitleSelector = sourceObj.optString("chapterTitleSelector", ""),
                        chapterUrlSelector = sourceObj.optString("chapterUrlSelector", "a"),
                        chapterContentSelector = sourceObj.optString("chapterContentSelector", ".chapter-content"),
                        customHeadersJson = sourceObj.optString("customHeadersJson", "{}"),
                        isBuiltIn = false,
                        isEnabled = true,
                        isInstalled = isInstalled,
                        repoId = repoId
                    )
                    parsedExtensions.add(ext)
                }
            }

            // Save Repository Entry in Room
            val repository = CommunityRepository(
                id = repoId,
                name = repoTitle,
                url = normalizedUrl,
                description = repoDesc,
                sourceCount = parsedExtensions.size,
                isDefault = isDefault,
                lastSyncTime = System.currentTimeMillis()
            )
            repositoryDao.insertOrUpdateRepository(repository)

            // Save all Extension items into Room DB
            if (parsedExtensions.isNotEmpty()) {
                extensionDao.insertExtensions(parsedExtensions)
            }

            SyncResult(
                isSuccess = true,
                repositoryName = repoTitle,
                totalSourcesFound = parsedExtensions.size,
                newSourcesCount = parsedExtensions.size,
                errorMessage = null
            )
        } catch (e: Exception) {
            SyncResult(
                isSuccess = false,
                repositoryName = name,
                totalSourcesFound = 0,
                newSourcesCount = 0,
                errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }


    /**
     * Removes a repository and optionally its uninstalled extensions from Room DB.
     */
    suspend fun removeRepository(repoId: String) = withContext(Dispatchers.IO) {
        repositoryDao.deleteRepositoryById(repoId)
    }
}
