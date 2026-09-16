package com.ashmeet.hyperlauncher.utils.installer

fun isMcVersionCompatible(v1: String, v2: String): Boolean {
    val cv1 = cleanMcVersion(v1)
    val cv2 = cleanMcVersion(v2)
    if (cv1 == cv2) return true
    val releaseRegex = Regex("""^1\.\d+(\.\d+)*$""")
    val isR1 = cv1.matches(releaseRegex)
    val isR2 = cv2.matches(releaseRegex)
    val isNonRelease1 = cv1.contains("-rc", ignoreCase = true) || cv1.contains("-pre", ignoreCase = true) || cv1.contains(Regex("""\d+w\d+[a-z]"""))
    val isNonRelease2 = cv2.contains("-rc", ignoreCase = true) || cv2.contains("-pre", ignoreCase = true) || cv2.contains(Regex("""\d+w\d+[a-z]"""))
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
        .replace(Regex("""(fabric|quilt|neoforge|forge)-loader-[0-9.]+-""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""(fabric|quilt|neoforge|forge)-loader-""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""(fabric|quilt|neoforge|forge)loader""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("-forge-.*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("-fabric-.*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("-quilt-.*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("-neoforge-.*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("-OptiFine_.*", RegexOption.IGNORE_CASE), "")
        .replace(Regex(" OptiFine .*", RegexOption.IGNORE_CASE), "")
        .replace(Regex(" \\(.*\\)"), "") // Remove anything in parentheses
        .trim()
    
    // Extract base version if it's still dirty (e.g. 1.20.1-somethingelse)
    val baseVersionRegex = Regex("""1\.\d+(\.\d+)*""")
    val match = baseVersionRegex.find(cleaned)
    if (match != null) {
        cleaned = match.value
    } else {
        // Handle snapshots like 23w14a
        val snapshotRegex = Regex("""\d+w\d+[a-z]""")
        val snapshotMatch = snapshotRegex.find(cleaned)
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
