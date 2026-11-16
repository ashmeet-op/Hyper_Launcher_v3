package com.ashmeet.hyperbackend.modloaders.modpacks.api

import com.ashmeet.hyperlauncher.utils.installer.ContentInstallerType
import com.ashmeet.hyperlauncher.utils.installer.ModrinthProject
import com.ashmeet.hyperlauncher.utils.installer.ModrinthVersion
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HorizonService {
    private const val INDEX_URL = "https://playhorizon.dev/mods/index.json"
    private const val FALLBACK_INDEX_URL = "https://horizon-rust.github.io/horizon-mods/index.json"

    private var cachedIndex: JsonObject? = null
    private var lastFetchTime = 0L

    private suspend fun fetchIndex(): JsonObject? = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (cachedIndex != null && currentTime - lastFetchTime < 600_000) {
            return@withContext cachedIndex
        }
        try {
            var raw = ApiHandler.getRaw(INDEX_URL)
            if (raw == null) {
                raw = ApiHandler.getRaw(FALLBACK_INDEX_URL)
            }
            if (raw != null) {
                val json = JsonParser.parseString(raw).asJsonObject
                cachedIndex = json
                lastFetchTime = currentTime
                return@withContext json
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext cachedIndex
    }

    private fun matchesKind(projectKind: String?, type: ContentInstallerType): Boolean {
        return projectKind != null && when (type) {
            ContentInstallerType.MODS -> projectKind == "mod" || projectKind == "native-mod"
            ContentInstallerType.RESOURCEPACKS -> projectKind == "resourcepack"
            ContentInstallerType.SHADERS -> projectKind == "shader"
            ContentInstallerType.MODPACKS, ContentInstallerType.WORLDS -> false
        }
    }

    suspend fun search(
        query: String,
        type: ContentInstallerType,
        mcVersion: String? = null,
        loader: String? = null
    ): List<ModrinthProject> = withContext(Dispatchers.IO) {
        val index = fetchIndex() ?: return@withContext emptyList()
        val projectsArray = index.getAsJsonArray("projects") ?: return@withContext emptyList()

        val results = mutableListOf<ModrinthProject>()
        for (el in projectsArray) {
            val p = el.asJsonObject
            val kind = p.get("kind")?.asString
            if (!matchesKind(kind, type)) continue

            val name = p.get("name")?.asString ?: ""
            val summary = p.get("summary")?.asString ?: ""
            val id = p.get("id")?.asString ?: ""

            if (query.isNotBlank()) {
                val q = query.lowercase()
                if (!name.lowercase().contains(q) && !summary.lowercase().contains(q) && !id.lowercase().contains(q)) {
                    continue
                }
            }

            if (!mcVersion.isNullOrEmpty() && p.has("versions")) {
                val versions = p.getAsJsonArray("versions")
                val matchesGame = versions.any { v ->
                    val game = v.asJsonObject.get("game")?.asString
                    game == null || game == mcVersion || game.contains(mcVersion)
                }
                if (!matchesGame) continue
            }

            results.add(
                ModrinthProject(
                    id = id,
                    title = name,
                    description = summary,
                    iconUrl = p.get("icon")?.let { if (it.isJsonNull) null else it.asString },
                    sourceUrl = p.get("source")?.let { if (it.isJsonNull) null else it.asString }
                )
            )
        }
        results
    }

    suspend fun getProjectDetails(projectId: String): ModrinthProject? = withContext(Dispatchers.IO) {
        val index = fetchIndex() ?: return@withContext null
        val projectsArray = index.getAsJsonArray("projects") ?: return@withContext null

        for (el in projectsArray) {
            val p = el.asJsonObject
            if (p.get("id")?.asString == projectId) {
                return@withContext ModrinthProject(
                    id = projectId,
                    title = p.get("name")?.asString ?: "",
                    description = p.get("summary")?.asString ?: "",
                    iconUrl = p.get("icon")?.let { if (it.isJsonNull) null else it.asString },
                    fullDescription = p.get("description")?.asString ?: p.get("summary")?.asString,
                    sourceUrl = p.get("source")?.let { if (it.isJsonNull) null else it.asString }
                )
            }
        }
        null
    }

    suspend fun getProjectVersions(projectId: String): List<ModrinthVersion> = withContext(Dispatchers.IO) {
        val index = fetchIndex() ?: return@withContext emptyList()
        val projectsArray = index.getAsJsonArray("projects") ?: return@withContext emptyList()

        for (el in projectsArray) {
            val p = el.asJsonObject
            if (p.get("id")?.asString == projectId) {
                val versionsArr = p.getAsJsonArray("versions") ?: return@withContext emptyList()
                return@withContext versionsArr.map { vEl ->
                    val vObj = vEl.asJsonObject
                    val versionStr = vObj.get("version")?.asString ?: "1.0.0"
                    val game = vObj.get("game")?.asString ?: "26.3"
                    val fileObj = vObj.getAsJsonObject("file")
                    val downloadUrl = fileObj?.get("url")?.asString ?: ""

                    ModrinthVersion(
                        id = versionStr,
                        name = versionStr,
                        gameVersions = listOf(game),
                        loaders = listOf("horizon"),
                        downloadUrl = downloadUrl,
                        versionType = "release"
                    )
                }
            }
        }
        emptyList()
    }
}
