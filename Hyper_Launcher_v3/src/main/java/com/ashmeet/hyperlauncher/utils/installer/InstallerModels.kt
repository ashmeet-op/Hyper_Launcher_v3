package com.ashmeet.hyperlauncher.utils.installer

import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import net.ashmeet.hyperlauncher.R

enum class ContentInstallerType(val labelRes: Int, val iconRes: ImageVector) {
    MODS(R.string.global_mods, Icons.Default.Extension),
    MODPACKS(R.string.global_modpacks, Icons.Default.Inventory2),
    RESOURCEPACKS(R.string.global_resourcepacks, Icons.Default.Image),
    SHADERS(R.string.global_shaders, Icons.Default.WbSunny),
    WORLDS(R.string.global_worlds, Icons.Default.Public)
}

enum class ContentSource(val displayName: String) {
    MODRINTH("Modrinth"),
    CURSEFORGE("CurseForge")
}

data class ModrinthProject(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    var iconBitmap: Bitmap? = null,
    var isIconLoading: Boolean = false,
    val fullDescription: String? = null,
    val gallery: List<String> = emptyList(),
    val sourceUrl: String? = null,
    val discordUrl: String? = null,
    val wikiUrl: String? = null,
    val issuesUrl: String? = null,
    val websiteUrl: String? = null
)

data class ModrinthVersion(
    val id: String,
    val name: String,
    val gameVersions: List<String>,
    val loaders: List<String>,
    val downloadUrl: String,
    val versionType: String = "release",
    val dependencies: List<ModDependency> = emptyList()
)

data class ModDependency(
    val projectId: String?,
    val versionId: String?,
    val fileName: String?,
    val dependencyType: String
)
