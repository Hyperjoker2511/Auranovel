package com.example.data.network

import android.webkit.CookieManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class CloudflareChallengeEvent(
    val url: String,
    val userAgent: String
)

class HybridNetworkClient private constructor() {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val _challengeFlow = MutableSharedFlow<CloudflareChallengeEvent>(extraBufferCapacity = 5)
    val challengeFlow: SharedFlow<CloudflareChallengeEvent> = _challengeFlow.asSharedFlow()

    var currentUserAgent: String =
        "Mozilla/5.0 (Linux; Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0"

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val host = url.host
                val list = cookieStore.getOrPut(host) { mutableListOf() }
                synchronized(list) {
                    list.removeAll { old -> cookies.any { it.name == old.name } }
                    list.addAll(cookies)
                }
                // Sync to Android CookieManager as well
                try {
                    val androidCookieManager = CookieManager.getInstance()
                    cookies.forEach { cookie ->
                        androidCookieManager.setCookie(url.toString(), cookie.toString())
                    }
                    androidCookieManager.flush()
                } catch (_: Exception) {
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val host = url.host
                val result = mutableListOf<Cookie>()
                cookieStore[host]?.let {
                    synchronized(it) { result.addAll(it) }
                }

                // Also check Android CookieManager for tokens (e.g., cf_clearance, cf_bm)
                try {
                    val androidCookieManager = CookieManager.getInstance()
                    val cookieStr = androidCookieManager.getCookie(url.toString())
                    if (!cookieStr.isNullOrBlank()) {
                        val pairs = cookieStr.split(";")
                        for (pair in pairs) {
                            val trimmed = pair.trim()
                            val parts = trimmed.split("=", limit = 2)
                            if (parts.size == 2) {
                                val name = parts[0].trim()
                                val value = parts[1].trim()
                                if (result.none { it.name == name }) {
                                    val cookie = Cookie.Builder()
                                        .domain(host)
                                        .path("/")
                                        .name(name)
                                        .value(value)
                                        .build()
                                    result.add(cookie)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                return result
            }
        })
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", currentUserAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")

            val response = chain.proceed(requestBuilder.build())

            // Inspect response for Cloudflare or anti-bot challenge
            val isChallenge = response.code in listOf(403, 503, 429)
            if (isChallenge) {
                val peekBody = response.peekBody(4096).string()
                if (peekBody.contains("Just a moment", ignoreCase = true) ||
                    peekBody.contains("cf-turnstile", ignoreCase = true) ||
                    peekBody.contains("challenges.cloudflare.com", ignoreCase = true) ||
                    peekBody.contains("DDoS-Guard", ignoreCase = true)
                ) {
                    _challengeFlow.tryEmit(
                        CloudflareChallengeEvent(
                            url = original.url.toString(),
                            userAgent = currentUserAgent
                        )
                    )
                }
            }
            response
        }
        .build()

    fun injectCookiesFromWebView(url: String, cookieHeader: String?) {
        if (cookieHeader.isNullOrBlank()) return
        val httpUrl = url.toHttpUrlOrNull() ?: return
        val host = httpUrl.host
        val pairs = cookieHeader.split(";")
        val cookiesToAdd = mutableListOf<Cookie>()
        for (pair in pairs) {
            val parts = pair.trim().split("=", limit = 2)
            if (parts.size == 2) {
                try {
                    val cookie = Cookie.Builder()
                        .domain(host)
                        .path("/")
                        .name(parts[0].trim())
                        .value(parts[1].trim())
                        .build()
                    cookiesToAdd.add(cookie)
                } catch (_: Exception) {
                }
            }
        }
        val list = cookieStore.getOrPut(host) { mutableListOf() }
        synchronized(list) {
            list.removeAll { old -> cookiesToAdd.any { it.name == old.name } }
            list.addAll(cookiesToAdd)
        }
    }

    suspend fun fetchHtml(url: String, headers: Map<String, String> = emptyMap()): String {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        val call = okHttpClient.newCall(requestBuilder.build())
        return kotlin.coroutines.suspendCoroutine { continuation ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            continuation.resumeWith(
                                Result.failure(IOException("HTTP ${it.code}: ${it.message}"))
                            )
                        } else {
                            val body = it.body?.string() ?: ""
                            continuation.resumeWith(Result.success(body))
                        }
                    }
                }
            })
        }
    }

    companion object {
        val instance: HybridNetworkClient by lazy { HybridNetworkClient() }
    }
}
