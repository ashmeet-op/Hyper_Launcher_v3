package com.ashmeet.hyperlauncher.utils.installer

private val RELEASE_REGEX = Regex("""^1\.\d+(\.\d+)*$""")
private val SNAPSHOT_REGEX = Regex("""\d+w\d+[a-z]""")
private val BASE_VERSION_REGEX = Regex("""1\.\d+(\.\d+)*""")

private val LOADER_PREFIX_REGEX = Regex("""(fabric|quilt|neoforge|forge)-loader-[0-9.]+-""", RegexOption.IGNORE_CASE)
private val LOADER_DASH_REGEX = Regex("""(fabric|quilt|neoforge|forge)-loader-""", RegexOption.IGNORE_CASE)
private val LOADER_NO_DASH_REGEX = Regex("""(fabric|quilt|neoforge|forge)loader""", RegexOption.IGNORE_CASE)
private val FORGE_SUFFIX_REGEX = Regex("""-forge-.*""", RegexOption.IGNORE_CASE)
private val FABRIC_SUFFIX_REGEX = Regex("""-fabric-.*""", RegexOption.IGNORE_CASE)
private val QUILT_SUFFIX_REGEX = Regex("""-quilt-.*""", RegexOption.IGNORE_CASE)
private val NEOFORGE_SUFFIX_REGEX = Regex("""-neoforge-.*""", RegexOption.IGNORE_CASE)
private val OPTIFINE_UNDERSCORE_REGEX = Regex("""-OptiFine_.*""", RegexOption.IGNORE_CASE)
private val OPTIFINE_SPACE_REGEX = Regex(""" OptiFine .*""", RegexOption.IGNORE_CASE)
private val PARENTHESES_REGEX = Regex(""" \(.*\)""")

fun isMcVersionCompatible(v1: String, v2: String): Boolean {
    val cv1 = cleanMcVersion(v1)
    val cv2 = cleanMcVersion(v2)
    if (cv1 == cv2) return true
    val isR1 = cv1.matches(RELEASE_REGEX)
    val isR2 = cv2.matches(RELEASE_REGEX)
    val isNonRelease1 = cv1.contains("-rc", ignoreCase = true) || cv1.contains("-pre", ignoreCase = true) || cv1.contains(SNAPSHOT_REGEX)
    val isNonRelease2 = cv2.contains("-rc", ignoreCase = true) || cv2.contains("-pre", ignoreCase = true) || cv2.contains(SNAPSHOT_REGEX)
    if ((isR1 && isNonRelease2) || (isR2 && isNonRelease1)) return false
    if (isR1 != isR2) return false

    if (isR1) {
        val parts1 = cv1.split(".")
        val parts2 = cv2.split(".")
        if (parts1.size >= 2 && parts2.size >= 2 && parts1[1] == parts2[1]) {
            if (parts1.size >= 3 && parts2.size >= 3) {
                return parts1[2] == parts2[2]
            }
            return true
        }
    }
    return false
}

fun cleanMcVersion(version: String?): String {
    if (version == null) return ""
    var cleaned = version
        .replace(LOADER_PREFIX_REGEX, "")
        .replace(LOADER_DASH_REGEX, "")
        .replace(LOADER_NO_DASH_REGEX, "")
        .replace(FORGE_SUFFIX_REGEX, "")
        .replace(FABRIC_SUFFIX_REGEX, "")
        .replace(QUILT_SUFFIX_REGEX, "")
        .replace(NEOFORGE_SUFFIX_REGEX, "")
        .replace(OPTIFINE_UNDERSCORE_REGEX, "")
        .replace(OPTIFINE_SPACE_REGEX, "")
        .replace(PARENTHESES_REGEX, "")
        .trim()

    val match = BASE_VERSION_REGEX.find(cleaned)
    if (match != null) {
        cleaned = match.value
    } else {
        val snapshotMatch = SNAPSHOT_REGEX.find(cleaned)
        if (snapshotMatch != null) {
            cleaned = snapshotMatch.value
        }
    }

    return cleaned
}

fun cleanLoaderName(loader: String): String {
    return when (loader.lowercase()) {
        "fabric", "fabricloader" -> "Fabric"
        "forge" -> "Forge"
        "quilt" -> "Quilt"
        "neoforge" -> "NeoForge"
        "optifine" -> "OptiFine"
        "iris" -> "Iris"
        "canvas" -> "Canvas"
        else -> loader.replaceFirstChar { it.uppercase() }
    }
}

fun cleanLoadersList(loaders: List<String>): String {
    return loaders.map { cleanLoaderName(it) }
        .distinct()
        .joinToString(", ")
}

fun isLoaderCompatible(targetLoader: String?, versionLoaders: List<String>): Boolean {
    if (targetLoader == null || targetLoader.equals("any", ignoreCase = true)) return true
    val cleanTarget = cleanLoaderName(targetLoader).lowercase()
    return versionLoaders.any { cleanLoaderName(it).lowercase() == cleanTarget }
}
