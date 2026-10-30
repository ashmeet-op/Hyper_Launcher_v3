package com.ashmeet.hyperlauncher.utils.translation

import android.content.Context
import android.content.res.Configuration
import android.util.AtomicFile
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.ashmeet.hyperbackend.progresskeeper.ProgressKeeper
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.tracker.TrackerLayout
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import net.ashmeet.hyperlauncher.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class TranslationResponse(
    val result: JsonElement? = null,
    val text: List<String>? = null,
    val info: String? = null
)

@Serializable
data class PersistentCache(
    val translations: Map<String, Map<String, String>> = emptyMap()
)

object Translator {

    private val TRANSLATION_SERVICES = listOf(
        "https://api.translate.zvo.cn/translate.json"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }
    }

    private val languageMap = mapOf(
        "en" to "english",
        "ru" to "russian",
        "es" to "spanish",
        "fr" to "french",
        "zh" to "chinese_simplified",
        "ja" to "japanese",
        "ko" to "korean",
        "de" to "german",
        "it" to "italian",
        "pt" to "portuguese",
        "tr" to "turkish",
        "vi" to "vietnamese",
        "hi" to "hindi",
        "ar" to "arabic",
        "uk" to "ukrainian"
    )

    private val cache = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()
    private val _targetLanguage = MutableStateFlow(resolveTargetLanguage())
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var prefetchJob: Job? = null
    private var debounceSaveJob: Job? = null
    private val cacheFileMutex = Mutex()
    private val cacheLoaded = CompletableDeferred<Unit>()

    private var appContext: Context? = null
    private const val PROGRESS_KEY = TrackerLayout.DOWNLOAD_TRANSLATIONS
    private val _refreshTrigger = mutableIntStateOf(0)
    val refreshTrigger: State<Int> = _refreshTrigger

    fun init(context: Context) {
        if (appContext != null) return
        val appCtx = context.applicationContext
        appContext = appCtx

        loadCache(appCtx)
        if (!cacheLoaded.isCompleted) {
            cacheLoaded.complete(Unit)
        }

        scope.launch {
            refreshLanguage()
        }
    }

    private suspend fun ensureCacheLoaded() {
        if (!cacheLoaded.isCompleted) {
            cacheLoaded.await()
        }
    }

    private fun resolveTargetLanguage(): String {
        if (LauncherPreferences.PREF_FORCE_ENGLISH) return "english"
        var lang = LauncherPreferences.PREF_LANGUAGE
        if (lang == "system") {
            lang = Locale.getDefault().language
        }
        val cleanLang = lang.split("-")[0].split("_")[0].lowercase()
        return languageMap[cleanLang] ?: languageMap[lang] ?: "english"
    }


    fun refreshLanguage() {
        val newTarget = resolveTargetLanguage()
        _targetLanguage.value = newTarget
        val ctx = appContext ?: return
        prefetchTranslations(ctx)
    }


    private fun getCacheFile(context: Context): File =
        File(context.filesDir, "translations_cache.json")

    private fun loadCache(context: Context) {
        try {
            val file = getCacheFile(context)
            val bakFile = File(file.parentFile, "${file.name}.bak")
            if (file.exists() || bakFile.exists()) {
                val atomicFile = AtomicFile(file)
                val text = atomicFile.openRead().use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (text.isNotBlank()) {
                    val persistentCache = json.decodeFromString<PersistentCache>(text)
                    persistentCache.translations.forEach { (lang, map) ->
                        val langMap = cache.getOrPut(lang) { ConcurrentHashMap() }
                        langMap.putAll(map)
                    }
                    Log.i("Translator", "Loaded cache: ${persistentCache.translations.size} languages")
                }
            }
        } catch (e: Exception) {
            Log.e("Translator", "Failed to load cache, resetting corrupt file", e)
            try {
                val file = getCacheFile(context)
                if (file.exists()) file.delete()
                val bakFile = File(file.parentFile, "${file.name}.bak")
                if (bakFile.exists()) bakFile.delete()
            } catch (_: Exception) {}
        }
    }

    private suspend fun saveCacheSync(context: Context) {
        ensureCacheLoaded()
        cacheFileMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val file = getCacheFile(context)
                    val snapshot = PersistentCache(cache.mapValues { it.value.toMap() })
                    val jsonBytes = json.encodeToString(snapshot).toByteArray(Charsets.UTF_8)
                    val atomicFile = AtomicFile(file)
                    var fos: FileOutputStream? = null
                    try {
                        fos = atomicFile.startWrite()
                        fos.write(jsonBytes)
                        atomicFile.finishWrite(fos)
                    } catch (e: Exception) {
                        if (fos != null) {
                            atomicFile.failWrite(fos)
                        }
                        throw e
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("Translator", "Failed to save cache", e)
                }
            }
        }
    }

    private fun scheduleSaveCache(context: Context) {
        debounceSaveJob?.cancel()
        debounceSaveJob = scope.launch {
            delay(500.milliseconds)
            saveCacheSync(context)
        }
    }

    private fun extractResults(response: TranslationResponse): List<String>? {
        response.text?.let { return it }
        val res = response.result ?: return null
        return when (res) {
            is JsonArray -> res.map { it.jsonPrimitive.content }
            is JsonPrimitive -> listOf(res.content)
            else -> null
        }
    }

    fun getCached(text: String, targetLanguage: String? = null): String? {
        if (text.isBlank()) return text
        val target = targetLanguage ?: _targetLanguage.value
        if (target == "english") return text
        return cache[target]?.get(text)
    }

    suspend fun translate(text: String, targetLanguage: String? = null): String {
        if (text.isBlank()) return text
        val target = targetLanguage ?: _targetLanguage.value
        if (target == "english") return text

        ensureCacheLoaded()
        cache[target]?.get(text)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val jsonArray = json.encodeToString(listOf(text))
                var translated: String? = null
                var lastException: Exception? = null

                for (serviceUrl in TRANSLATION_SERVICES) {
                    try {
                        val httpResponse: HttpResponse = client.post(serviceUrl) {
                            setBody(FormDataContent(Parameters.build {
                                append("to", target)
                                append("text", jsonArray)
                            }))
                        }
                        if (httpResponse.status.isSuccess() &&
                            httpResponse.contentType()?.match(ContentType.Application.Json) == true
                        ) {
                            val response: TranslationResponse = httpResponse.body()
                            translated = extractResults(response)?.firstOrNull()
                            if (translated != null) break
                        } else {
                            Log.w("Translator", "Service $serviceUrl returned ${httpResponse.status}")
                        }
                    } catch (e: Exception) {
                        lastException = e
                        Log.w("Translator", "Service $serviceUrl failed, trying next…")
                    }
                }

                if (translated == null && lastException != null) throw lastException

                val finalResult = translated?.ifBlank { text } ?: text
                cache.getOrPut(target) { ConcurrentHashMap() }[text] = finalResult
                appContext?.let { scheduleSaveCache(it) }
                finalResult
            } catch (e: Exception) {
                Log.e("Translator", "Translation failed for: $text", e)
                text
            }
        }
    }

    fun prefetchTranslations(context: Context) {
        val target = _targetLanguage.value
        if (target == "english") {
            ProgressKeeper.submitProgress(PROGRESS_KEY, -1, -1)
            return
        }

        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            val myJob = coroutineContext[Job]
            try {
                ensureCacheLoaded()
                val strings = extractStrings(context)
                if (strings.isEmpty()) return@launch

                val langCache = cache.getOrPut(target) { ConcurrentHashMap() }
                val toTranslate = strings.filter { !langCache.containsKey(it) }
                if (toTranslate.isEmpty()) {
                    _refreshTrigger.intValue++
                    return@launch
                }

                val totalCount = toTranslate.size
                val capitalizedTarget = target.replaceFirstChar { it.uppercase() }
                ProgressKeeper.submitProgress(
                    PROGRESS_KEY, 0,
                    R.string.translation_prefetching, capitalizedTarget, 0, totalCount
                )

                val batchSize = 20
                var completed = 0

                for (batch in toTranslate.chunked(batchSize)) {
                    ensureActive()
                    var retryCount = 0
                    var success = false
                    while (retryCount < 3 && !success) {
                        ensureActive()
                        try {
                            val jsonArray = json.encodeToString(batch)
                            var batchResults: List<String>? = null
                            var lastException: Exception? = null

                            for (serviceUrl in TRANSLATION_SERVICES) {
                                try {
                                    val httpResponse: HttpResponse = withContext(Dispatchers.IO) {
                                        client.post(serviceUrl) {
                                            setBody(FormDataContent(Parameters.build {
                                                append("to", target)
                                                append("text", jsonArray)
                                            }))
                                        }
                                    }
                                    if (httpResponse.status.isSuccess() &&
                                        httpResponse.contentType()?.match(ContentType.Application.Json) == true
                                    ) {
                                        val response: TranslationResponse = httpResponse.body()
                                        batchResults = extractResults(response)
                                        if (batchResults != null) break
                                    } else {
                                        Log.w("Translator", "Service $serviceUrl returned ${httpResponse.status} for batch")
                                    }
                                } catch (e: Exception) {
                                    lastException = e
                                    Log.w("Translator", "Service $serviceUrl failed for batch, trying next…")
                                }
                            }

                            if (batchResults == null && lastException != null) throw lastException

                            batchResults?.let { results ->
                                if (results.size == batch.size) {
                                    batch.zip(results).forEach { (original, translated) ->
                                        langCache[original] = translated.ifBlank { original }
                                    }
                                } else {
                                    batch.forEachIndexed { index, original ->
                                        val translated = results.getOrNull(index)?.ifBlank { original } ?: original
                                        langCache[original] = translated
                                    }
                                }
                            } ?: run {
                                batch.forEach { original ->
                                    langCache.putIfAbsent(original, original)
                                }
                            }
                            success = true
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            retryCount++
                            Log.e("Translator", "Batch translation failed (retry $retryCount/3)", e)
                            if (retryCount < 3) {
                                delay((2000L * retryCount).milliseconds)
                            } else {
                                batch.forEach { original ->
                                    langCache.putIfAbsent(original, original)
                                }
                                success = true
                            }
                        }
                    }

                    saveCacheSync(context)

                    completed += batch.size
                    val progress = (completed.toFloat() / totalCount * 100).toInt()
                    ProgressKeeper.submitProgress(
                        PROGRESS_KEY, progress,
                        R.string.translation_prefetching, capitalizedTarget, completed, totalCount
                    )

                    delay(1100L.milliseconds)
                }

                _refreshTrigger.intValue++
            } catch (e: Exception) {
                if (e !is CancellationException) Log.e("Translator", "Prefetch failed", e)
            } finally {
                if (prefetchJob == myJob) {
                    ProgressKeeper.submitProgress(PROGRESS_KEY, -1, -1)
                }
            }
        }
    }

    private fun extractStrings(context: Context): List<String> {
        val strings = mutableListOf<String>()
        try {
            val englishContext = try {
                val config = Configuration(context.resources.configuration)
                config.setLocale(Locale.ENGLISH)
                context.createConfigurationContext(config)
            } catch (_: Exception) {
                context
            }
            for (field in R.string::class.java.fields) {
                try {
                    val id = field.getInt(null)
                    val s = englishContext.getString(id)
                    if (s.isNotBlank() && !s.startsWith("http") && s.length > 1) {
                        strings.add(s)
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("Translator", "Failed to extract strings", e)
        }
        return strings.distinct()
    }
}

@Composable
fun translatedText(text: String): String {
    val target by Translator.targetLanguage.collectAsState()

    if (target == "english") return text

    val trigger by Translator.refreshTrigger
    val cached = remember(text, target) { Translator.getCached(text, target) }
    val translated by produceState(initialValue = cached ?: text, text, target, trigger) {
        value = Translator.translate(text, target)
    }
    return translated
}

@Composable
private fun <T> StateFlow<T>.collectAsState(): State<T> {
    val state = remember(this) { mutableStateOf(value) }
    LaunchedEffect(this) {
        collect { state.value = it }
    }
    return state
}
