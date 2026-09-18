package net.kdt.pojavlaunch.modloaders.modpacks.api

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import com.ashmeet.hyperlauncher.utils.installer.ContentInstallerType
import com.ashmeet.hyperlauncher.utils.installer.ModrinthProject
import com.ashmeet.hyperlauncher.utils.installer.ModrinthVersion
import com.ashmeet.hyperlauncher.utils.installer.ModDependency
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.lifecycle.ContextExecutor
import java.util.regex.Pattern

object CurseForgeService {
    private const val CURSEFORGE_API = "https://api.curseforge.com/v1"
    private const val MCIM_CURSEFORGE_API = "https://mod.mcimirror.top/curseforge/v1"
    
    private const val CURSEFORGE_MC_GAME_ID = 432
    private const val CURSEFORGE_MODPACK_CLASS_ID = 4471
    private const val CURSEFORGE_MOD_CLASS_ID = 6
    private const val CURSEFORGE_RESOURCEPACK_CLASS_ID = 12
    private const val CURSEFORGE_WORLDS_CLASS_ID = 17
    private const val CURSEFORGE_CUSTOMIZATION_CLASS_ID = 4546
    private const val SORT_RELEVANCE = 1
    private val sMcVersionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.?([0-9]+)?")
    
    private var apiHandler: ApiHandler? = null
    private var useMirror = false

    /**
     * CurseForge Mod Loader Types
     * 1 = Forge
     * 2 = Cauldron
     * 3 = LiteLoader
     * 4 = Fabric
     * 5 = Quilt
     * 6 = NeoForge
     */
    private fun getModLoaderTypeCode(loader: String?): Int? {
        if (loader == null) return null
        return when (loader.lowercase()) {
            "forge" -> 1
            "fabric" -> 4
            "quilt" -> 5
            "neoforge" -> 6
            else -> null
        }
    }

    fun init(apiKey: String) {
        if (apiKey == "DUMMY" || apiKey.isBlank()) {
            apiHandler = ApiHandler(MCIM_CURSEFORGE_API)
            useMirror = true
        } else {
            apiHandler = ApiHandler(CURSEFORGE_API, apiKey)
            useMirror = false
        }
    }

    private fun getHandler(): ApiHandler {
        if (apiHandler == null) {
            init("DUMMY")
        }
        return apiHandler!!
    }

    suspend fun search(
        query: String,
        type: ContentInstallerType,
        mcVersion: String? = null,
        loader: String? = null,
        index: Int = 0
    ): List<ModrinthProject> = withContext(Dispatchers.IO) {
        val handler = getHandler()
        
        val params = hashMapOf<String, Any>()
        params["gameId"] = CURSEFORGE_MC_GAME_ID
        params["classId"] = when (type) {
            ContentInstallerType.MODPACKS -> CURSEFORGE_MODPACK_CLASS_ID
            ContentInstallerType.RESOURCEPACKS -> CURSEFORGE_RESOURCEPACK_CLASS_ID
            ContentInstallerType.SHADERS -> CURSEFORGE_CUSTOMIZATION_CLASS_ID
            ContentInstallerType.WORLDS -> CURSEFORGE_WORLDS_CLASS_ID
            else -> CURSEFORGE_MOD_CLASS_ID
        }
        if (query.isNotBlank()) {
            params["searchFilter"] = query
        }
        params["sortField"] = SORT_RELEVANCE
        params["sortOrder"] = "desc"
        params["index"] = index
        params["pageSize"] = 50
        
        if (!mcVersion.isNullOrEmpty()) {
            params["gameVersion"] = mcVersion
        }
        
        getModLoaderTypeCode(loader)?.let {
            params["modLoaderType"] = it
        }
        
        val response = handler.get("mods/search", params, JsonObject::class.java) ?: return@withContext emptyList()
        val data = response.getAsJsonArray("data") ?: return@withContext emptyList()

        data.mapNotNull {
            val item = it.asJsonObject
            if (item.has("status") && !item.get("status").isJsonNull) {
                if (item.get("status").asInt != 4) {
                }
            }
            
            ModrinthProject(
                id = item.get("id").asString,
                title = item.get("name").asString,
                description = item.get("summary").asString,
                iconUrl = item.getAsJsonObject("logo")?.get("thumbnailUrl")?.asString,
                gallery = emptyList()
            )
        }
    }

    suspend fun getProjectDetails(projectId: String): ModrinthProject? = withContext(Dispatchers.IO) {
        val handler = getHandler()
        val response = handler.get("mods/$projectId", JsonObject::class.java) ?: return@withContext null
        val data = response.getAsJsonObject("data") ?: return@withContext null
        
        val screenshots = data.getAsJsonArray("screenshots") ?: null
        val gallery = screenshots?.map { 
            it.asJsonObject.get("url").asString 
        } ?: emptyList()

        val linksObj = data.getAsJsonObject("links")
        val websiteUrl = linksObj?.get("websiteUrl")?.let { if (it.isJsonNull) null else it.asString }
        val wikiUrl = linksObj?.get("wikiUrl")?.let { if (it.isJsonNull) null else it.asString }
        val issuesUrl = linksObj?.get("issuesUrl")?.let { if (it.isJsonNull) null else it.asString }
        val sourceUrl = linksObj?.get("sourceUrl")?.let { if (it.isJsonNull) null else it.asString }

        ModrinthProject(
            id = data.get("id").asString,
            title = data.get("name").asString,
            description = data.get("summary").asString,
            iconUrl = data.getAsJsonObject("logo")?.get("thumbnailUrl")?.asString,
            fullDescription = data.get("summary").asString,
            gallery = gallery,
            websiteUrl = websiteUrl,
            wikiUrl = wikiUrl,
            issuesUrl = issuesUrl,
            sourceUrl = sourceUrl
        )
    }

    suspend fun getMods(modIds: List<Int>): List<ModrinthProject> = withContext(Dispatchers.IO) {
        val handler = getHandler()
        val body = hashMapOf<String, Any>()
        body["modIds"] = modIds
        
        val response = handler.post("mods", body as Any, JsonObject::class.java) ?: return@withContext emptyList()
        val data = response.getAsJsonArray("data") ?: return@withContext emptyList()
        
        data.map {
            val item = it.asJsonObject
            ModrinthProject(
                id = item.get("id").asString,
                title = item.get("name").asString,
                description = item.get("summary").asString,
                iconUrl = item.getAsJsonObject("logo")?.get("thumbnailUrl")?.asString,
                gallery = emptyList()
            )
        }
    }

    suspend fun getProjectVersions(projectId: String): List<ModrinthVersion> = withContext(Dispatchers.IO) {
        val handler = getHandler()
        
        val params = hashMapOf<String, Any>()
        params["pageSize"] = 50

        val response = handler.get("mods/$projectId/files", params, JsonObject::class.java) ?: return@withContext emptyList()
        val data = response.getAsJsonArray("data") ?: return@withContext emptyList()

        data.mapNotNull {
            val file = it.asJsonObject
            if (file.has("isServerPack") && !file.get("isServerPack").isJsonNull && file.get("isServerPack").asBoolean) return@mapNotNull null
            
            val gameVersions = file.getAsJsonArray("gameVersions")?.map { gv -> gv.asString } ?: emptyList()
            val mcVersions = gameVersions.filter { gv -> sMcVersionPattern.matcher(gv).matches() }
            val loaders = gameVersions.filter { gv -> !sMcVersionPattern.matcher(gv).matches() && 
                (gv.equals("fabric", true) || gv.equals("forge", true) || gv.equals("quilt", true) || gv.equals("neoforge", true)) }

            val downloadUrl = getDownloadUrl(projectId, file) ?: return@mapNotNull null

            ModrinthVersion(
                id = file.get("id").asString,
                name = file.get("displayName").asString,
                gameVersions = mcVersions,
                loaders = loaders,
                downloadUrl = downloadUrl,
                versionType = when(file.get("releaseType").asInt) {
                    1 -> "release"
                    2 -> "beta"
                    3 -> "alpha"
                    else -> "release"
                },
                dependencies = file.getAsJsonArray("dependencies")?.mapNotNull { dep ->
                    val d = dep.asJsonObject
                    val relationType = d.get("relationType").asInt
                    if (relationType == 5) return@mapNotNull null // Incompatible
                    
                    ModDependency(
                        projectId = d.get("modId").asString,
                        versionId = null,
                        fileName = null,
                        dependencyType = when(relationType) {
                            1 -> "embedded"
                            2 -> "optional"
                            3 -> "required"
                            4 -> "tool"
                            6 -> "include"
                            else -> "optional"
                        }
                    )
                } ?: emptyList()
            )
        }
    }

    private suspend fun getDownloadUrl(projectId: String, fileMetadata: JsonObject): String? = withContext(Dispatchers.IO) {
        val handler = getHandler()
        val fileId = fileMetadata.get("id").asString
        
        // If downloadUrl is already in the metadata, use it
        if (fileMetadata.has("downloadUrl") && !fileMetadata.get("downloadUrl").isJsonNull) {
            val url = fileMetadata.get("downloadUrl").asString
            if (url.isNotBlank()) return@withContext url
        }

        // Try to get download URL from API
        val response = handler.get("mods/$projectId/files/$fileId/download-url", JsonObject::class.java)
        val data = response?.get("data")
        if (data != null && !data.isJsonNull) {
            return@withContext data.asString
        }
        
        // Fallback to edge link if possible
        val fileName = fileMetadata.get("fileName").asString
        val fileIdLong = fileId.toLong()
        String.format("https://edge.forgecdn.net/files/%s/%s/%s", fileIdLong / 1000, fileIdLong % 1000, fileName)
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
