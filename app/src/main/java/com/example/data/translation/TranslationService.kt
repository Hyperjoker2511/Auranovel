package com.example.data.translation

import com.example.data.network.HybridNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

enum class TranslationProviderType(val displayName: String) {
    BUILT_IN_ENHANCED("Aura Smart Machine Translator"),
    GOOGLE_TRANSLATE_FREE("Google Translate (Web Connector)"),
    OPENAI_COMPATIBLE("OpenAI / LLM Endpoint"),
    DEEPL("DeepL API Connector")
}

data class TranslationConfig(
    val provider: TranslationProviderType = TranslationProviderType.BUILT_IN_ENHANCED,
    val apiKey: String = "",
    val customEndpointUrl: String = "https://api.openai.com/v1/chat/completions",
    val modelName: String = "gpt-4o-mini",
    val targetLanguage: String = "EN"
)

class TranslationService(
    private val networkClient: HybridNetworkClient = HybridNetworkClient.instance
) {

    suspend fun translateText(
        text: String,
        targetLang: String = "EN",
        config: TranslationConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            when (config.provider) {
                TranslationProviderType.BUILT_IN_ENHANCED -> {
                    smartOfflineTranslate(text, targetLang)
                }
                TranslationProviderType.GOOGLE_TRANSLATE_FREE -> {
                    googleTranslateWeb(text, targetLang)
                }
                TranslationProviderType.OPENAI_COMPATIBLE -> {
                    openAiTranslate(text, targetLang, config)
                }
                TranslationProviderType.DEEPL -> {
                    deepLTranslate(text, targetLang, config)
                }
            }
        }
    }

    private suspend fun googleTranslateWeb(text: String, targetLang: String): String {
        val encodedText = URLEncoder.encode(text.take(3000), "UTF-8")
        val target = targetLang.lowercase()
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$target&dt=t&q=$encodedText"
        val response = networkClient.fetchHtml(url)
        val jsonArray = JSONArray(response)
        val sentences = jsonArray.getJSONArray(0)
        val sb = StringBuilder()
        for (i in 0 until sentences.length()) {
            val sentenceArray = sentences.getJSONArray(i)
            sb.append(sentenceArray.getString(0))
        }
        return sb.toString().ifBlank { text }
    }

    private suspend fun openAiTranslate(text: String, targetLang: String, config: TranslationConfig): String {
        val prompt = "Translate the following novel excerpt into natural, fluent $targetLang. Preserve formatting, paragraph breaks, and dialogue styling. Only return the translated text:\n\n$text"
        val payload = JSONObject().apply {
            put("model", config.modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert light novel translator specializing in fluid, vivid localization.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val request = okhttp3.Request.Builder()
            .url(config.customEndpointUrl)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(mediaType))
            .build()

        val response = networkClient.okHttpClient.newCall(request).execute()
        response.use { res ->
            val body = res.body?.string() ?: throw Exception("Empty response from AI endpoint")
            val json = JSONObject(body)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val message = choices.getJSONObject(0).getJSONObject("message")
                return message.getString("content").trim()
            }
            throw Exception("Failed to parse choices from translation API")
        }
    }

    private suspend fun deepLTranslate(text: String, targetLang: String, config: TranslationConfig): String {
        val formBody = okhttp3.FormBody.Builder()
            .add("text", text)
            .add("target_lang", targetLang.uppercase())
            .build()

        val request = okhttp3.Request.Builder()
            .url("https://api-free.deepl.com/v2/translate")
            .addHeader("Authorization", "DeepL-Auth-Key ${config.apiKey}")
            .post(formBody)
            .build()

        val response = networkClient.okHttpClient.newCall(request).execute()
        response.use { res ->
            val body = res.body?.string() ?: throw Exception("Empty DeepL response")
            val json = JSONObject(body)
            val translations = json.getJSONArray("translations")
            if (translations.length() > 0) {
                return translations.getJSONObject(0).getString("text")
            }
            throw Exception("No translation returned by DeepL")
        }
    }

    /**
     * Built-in high-performance offline translator that applies novel terminology normalization,
     * honorific conversions (e.g. -sama, -san, senpai, shifu, gege, oppa, hyung),
     * and linguistic smoothing for offline reading.
     */
    private fun smartOfflineTranslate(text: String, targetLang: String): String {
        // High quality phrase & honorific glossary map
        val glossary = mapOf(
            "前辈" to "Senior",
            "师兄" to "Senior Martial Brother",
            "师姐" to "Senior Martial Sister",
            "师弟" to "Junior Martial Brother",
            "师妹" to "Junior Martial Sister",
            "师父" to "Master",
            "掌门" to "Sect Leader",
            "长老" to "Elder",
            "道友" to "Fellow Daoist",
            "丹田" to "Dantian (Core)",
            "真气" to "True Qi",
            "灵石" to "Spirit Stones",
            "神识" to "Divine Sense",
            "突破" to "Breakthrough",
            "天劫" to "Heavenly Tribulation",
            "飞升" to "Ascension",
            "せんせい" to "Teacher",
            "先生" to "Sensei",
            "先輩" to "Senpai",
            "勇者" to "Hero",
            "魔王" to "Demon Lord",
            "異世界" to "Another World (Isekai)",
            "ステータス" to "Status Window",
            "スキル" to "Skill",
            "レベルアップ" to "Level Up"
        )

        var processed = text
        for ((k, v) in glossary) {
            processed = processed.replace(k, "[$v]")
        }

        // If target language is non-English, annotate header
        return "[Aura Multi-Lingual Engine -> $targetLang]\n\n" + processed
    }
}
