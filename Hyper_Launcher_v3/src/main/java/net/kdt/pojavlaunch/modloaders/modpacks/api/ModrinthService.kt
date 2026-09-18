package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import com.ashmeet.hyperlauncher.utils.installer.ContentInstallerType
import com.ashmeet.hyperlauncher.utils.installer.ModrinthProject
import com.ashmeet.hyperlauncher.utils.installer.ModrinthVersion
import com.ashmeet.hyperlauncher.utils.installer.ModDependency
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.lifecycle.ContextExecutor

object ModrinthService {
    private val apiHandler = ApiHandler("https://api.modrinth.com/v2")

    suspend fun search(
        query: String,
        type: ContentInstallerType,
        mcVersion: String? = null,
        loader: String? = null
    ): List<ModrinthProject> = withContext(Dispatchers.IO) {
        val params = hashMapOf<String, Any>()
        val facets = mutableListOf<String>()
        
        val typeStr = when(type) {
            ContentInstallerType.MODS -> "mod"
            ContentInstallerType.MODPACKS -> "modpack"
            ContentInstallerType.RESOURCEPACKS -> "resourcepack"
            ContentInstallerType.SHADERS -> "shader"
            ContentInstallerType.WORLDS -> "world"
        }
        facets.add("[\"project_type:$typeStr\"]")
        
        if (!mcVersion.isNullOrEmpty()) {
            facets.add("[\"versions:$mcVersion\"]")
        }
        
        val shouldApplyLoader = type == ContentInstallerType.MODS || type == ContentInstallerType.MODPACKS
        if (shouldApplyLoader && !loader.isNullOrEmpty()) {
            facets.add("[\"categories:$loader\"]")
        }
        
        if (facets.isNotEmpty()) {
            params["facets"] = "[" + facets.joinToString(",") + "]"
        }
        
        params["query"] = query
        params["limit"] = 50
        params["index"] = "relevance"

        val response = apiHandler.get("search", params, JsonObject::class.java) ?: return@withContext emptyList()
        val hits = response.getAsJsonArray("hits") ?: return@withContext emptyList()

        hits.map {
            val hit = it.asJsonObject
            ModrinthProject(
                id = hit.get("project_id").asString,
                title = hit.get("title").asString,
                description = hit.get("description").asString,
                iconUrl = hit.get("icon_url")
                    ?.let { url -> if (url.isJsonNull) null else url.asString },
                gallery = hit.getAsJsonArray("gallery")?.map { g -> g.asString } ?: emptyList()
            )
        }
    }

    suspend fun getProjectVersions(projectId: String): List<ModrinthVersion> = withContext(Dispatchers.IO) {
        val response = apiHandler.get("project/$projectId/version", JsonArray::class.java) ?: return@withContext emptyList()
        response.map {
            val v = it.asJsonObject
            val files = v.getAsJsonArray("files")
            val primaryFile = (0 until files.size())
                .map { i -> files.get(i).asJsonObject }
                .firstOrNull { f -> f.has("primary") && f.get("primary").asBoolean } 
                ?: files.get(0).asJsonObject

            ModrinthVersion(
                id = v.get("id").asString,
                name = v.get("name").asString,
                gameVersions = v.getAsJsonArray("game_versions").map { gv -> gv.asString },
                loaders = v.getAsJsonArray("loaders").map { l -> l.asString },
                downloadUrl = primaryFile.get("url").asString,
                versionType = v.get("version_type").asString,
                dependencies = v.getAsJsonArray("dependencies")?.map { dep ->
                    val d = dep.asJsonObject
                    ModDependency(
                        projectId = if (d.has("project_id") && !d.get("project_id").isJsonNull) d.get("project_id").asString else null,
                        versionId = if (d.has("version_id") && !d.get("version_id").isJsonNull) d.get("version_id").asString else null,
                        fileName = if (d.has("file_name") && !d.get("file_name").isJsonNull) d.get("file_name").asString else null,
                        dependencyType = d.get("dependency_type").asString
                    )
                } ?: emptyList()
            )
        }
    }

    suspend fun getProjectDetails(projectId: String): ModrinthProject? = withContext(Dispatchers.IO) {
        val response = apiHandler.get("project/$projectId", JsonObject::class.java) ?: return@withContext null
        ModrinthProject(
            id = response.get("id").asString,
            title = response.get("title").asString,
            description = response.get("description").asString,
            iconUrl = response.get("icon_url")?.let { if (it.isJsonNull) null else it.asString },
            fullDescription = response.get("body")?.asString,
            gallery = response.getAsJsonArray("gallery")?.map { it.asJsonObject.get("url").asString } ?: emptyList(),
            sourceUrl = response.get("source_url")?.let { if (it.isJsonNull) null else it.asString },
            discordUrl = response.get("discord_url")?.let { if (it.isJsonNull) null else it.asString },
            wikiUrl = response.get("wiki_url")?.let { if (it.isJsonNull) null else it.asString },
            issuesUrl = response.get("issues_url")?.let { if (it.isJsonNull) null else it.asString },
            websiteUrl = response.get("website_url")?.let { if (it.isJsonNull) null else it.asString }
        )
    }

    suspend fun getProjects(projectIds: List<String>): List<ModrinthProject> = withContext(Dispatchers.IO) {
        if (projectIds.isEmpty()) return@withContext emptyList()
        val params = hashMapOf<String, Any>()
        params["ids"] = "[\"" + projectIds.joinToString("\",\"") + "\"]"
        val response = apiHandler.get("projects", params, JsonArray::class.java) ?: return@withContext emptyList()
        response.map {
            val p = it.asJsonObject
            ModrinthProject(
                id = p.get("id").asString,
                title = p.get("title").asString,
                description = p.get("description").asString,
                iconUrl = p.get("icon_url")?.let { if (it.isJsonNull) null else it.asString }
            )
        }
    }

    suspend fun loadIcon(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val context = ContextExecutor.getContext() ?: return@withContext null
        try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            null
        }
    }
}
