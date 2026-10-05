package com.ashmeet.hyperlauncher.screens.auth.skin

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ashmeet.hyperlauncher.components.dialog.DialogTextInput
import com.ashmeet.hyperlauncher.components.dialog.SimpleAlertDialog
import com.ashmeet.hyperlauncher.skin.SkinManager
import com.ashmeet.hyperlauncher.skin.SkinPreview
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.authenticator.accounts.Account
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class TextureType {
    SKIN, CAPE
}

data class TextureItem(
    val id: String,
    val name: String,
    val url: String,
    val localFile: File? = null,
    val isCustom: Boolean = false
)

private val skinSources = listOf(
    "https://crafty.gg/skins",
    "https://crafty.gg/skins?discover=newest",
    "https://crafty.gg/skins?discover=top_viewed",
    "https://crafty.gg/skins?discover=most_worn",
    "https://crafty.gg/skins/color/black",
    "https://crafty.gg/skins/color/white",
    "https://crafty.gg/skins/color/red",
    "https://crafty.gg/skins/color/blue",
    "https://crafty.gg/skins/color/green",
    "https://crafty.gg/skins/color/purple",
    "https://crafty.gg/skins/color/pink",
    "https://crafty.gg/skins/color/cyan",
    "https://crafty.gg/skins/color/yellow",
    "https://crafty.gg/skins/color/orange",
    "https://crafty.gg/skins/color/grey",
    "https://crafty.gg/skins/color/brown"
)

private val capeSources = listOf(
    "https://crafty.gg/capes",
    "https://crafty.gg/capes?discover=newest",
    "https://crafty.gg/capes?discover=fewest_players",
    "https://crafty.gg/capes?discover=most_players"
)

private val curatedCapes = listOf(
    Triple("Migrator Cape", "2340c0e03dd24a11b15a8b33c2a7e9e32abb2051b2481d0ba7defd635ca7a933", "cape_migrator"),
    Triple("15th Anniversary", "cd9d82ab17fd92022dbd4a86cde4c382a7540e117fae7b9a2853658505a80625", "cape_15th"),
    Triple("Cherry Blossom", "afd553b39358a24edfe3b8a9a939fa5fa4faa4d9a9c3d6af8eafb377fa05c2bb", "cape_cherry"),
    Triple("Vanilla Cape", "f9a76537647989f9a0b6d001e320dac591c359e9e61a31f4ce11c88f207f0ad4", "cape_vanilla"),
    Triple("Follower's Cape", "569b7f2a1d00d26f30efe3f9ab9ac817b1e6d35f4f3cfb0324ef2d328223d350", "cape_follower"),
    Triple("Purple Heart", "cb40a92e32b57fd732a00fc325e7afb00a7ca74936ad50d8e860152e482cfbde", "cape_purple"),
    Triple("Pan Cape", "28de4a81688ad18b49e735a273e086c18f1e3966956123ccb574034c06f5d336", "cape_pan"),
    Triple("Common Cape", "5ec930cdd2629c8771655c60eebeb867b4b6559b0e6d3bc71c40c96347fa03f0", "cape_common"),
    Triple("Copper Cape", "5e6f3193e74cd16cdd6637d9bae5484e3a37ff2a14c2d157c659a07810b1bdca", "cape_copper"),
    Triple("Menace Cape", "dbc21e222528e30dc88445314f7be6ff12d3aeebc3c192054fba7e3b3f8c77b1", "cape_menace"),
    Triple("Mojang Classic", "10f135ef7010f36f6f9661e479a9539d91f4a43b2f279f1311b5e39d5b035133", "cape_mojang_classic"),
    Triple("Mojang Studios", "8f9024f2e51f893114d24a0d93dd1c0ba0a0f828a2a86ef7842c0c7a523a676a", "cape_mojang_studios"),
    Triple("Founder's Cape", "b30a133d1e1f13b1f1f917bb957e84a2114d54d9a9394625b1f9c058097a13d", "cape_founders"),
    Triple("Crafter Cape", "1e72fb4d01b97b0a8f71bb572012d2281a17fa2b64d0d023f03b87bb900f6f41", "cape_crafter"),
    Triple("MCC 15th Year", "7f2e106df231c50e29b13c8f1352f20a108a7b9ef182a450e12f6d2146430e71", "cape_mcc"),
    Triple("MineCon 2011", "1335b3644a428e21ba17b8f047cb50eb6d859e900a6e0380fbc5527845f3c928", "cape_minecon_2011"),
    Triple("MineCon 2012", "a21764cb5d438edc6d1f0578e9f50e9734df2fa5b7b960c1d6ffccb1e225e3d7", "cape_minecon_2012"),
    Triple("MineCon 2013", "9876e5dcf49c48b1114d59a80436d4001994b2a473a21396eb1525a818317d7a", "cape_minecon_2013"),
    Triple("MineCon 2015", "179127d606881c62f2a74c206d2c448d56b0d918a221f1e3895e54d3d9d30043", "cape_minecon_2015"),
    Triple("MineCon 2016", "bdf4821b0a88390f058097d4715f3e9c56801f945395662f5616ed3dcf8b73d", "cape_minecon_2016")
)

private val featuredPlayers = listOf(
    "Technoblade", "Dream", "Dinnerbone", "jeb_", "MumboJumbo", "Grian", "DanTDM",
    "Ph1LzA", "Skeppy", "TommyInnit", "Notch", "Steve", "Alex", "CaptainSparklez",
    "Stampy", "LDShadowLady", "Smallishbeans", "Etho", "Docm77", "ImpulseSV",
    "TangoTek", "Keralis", "BDoubleO100", "Rendog", "Xisuma", "ZombieCleo",
    "Cubfan135", "FalseSymmetry", "Stressmonster101", "Iskall85"
)

private fun fetchMineSkinFallback(page: Int): List<TextureItem> {
    val items = mutableListOf<TextureItem>()
    try {
        val url = URL("https://api.mineskin.org/get/list/${page - 1}?size=24")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connectTimeout = 8000
            readTimeout = 10000
        }
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)
            val array = json.optJSONArray("skins")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val texUrl = obj.optString("url", "")
                    val hashVal = texUrl.substringAfterLast("/")
                    if (hashVal.length == 64) {
                        val skinId = obj.optString("id", hashVal.take(12))
                        val name = obj.optString("name", "Skin ${i + 1}").ifEmpty { "Skin ${i + 1}" }
                        items.add(
                            TextureItem(
                                id = "mineskin_$skinId",
                                name = name,
                                url = "https://textures.minecraft.net/texture/$hashVal"
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("TextureSelection", "MineSkin fallback error for page $page", e)
    }
    return items
}

@Composable
fun Cape2DPreview(
    capeUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmapState by remember(capeUrl) { mutableStateOf<Bitmap?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(capeUrl) {
        withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(capeUrl)
                    .allowHardware(false)
                    .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        bitmapState = drawable.bitmap
                    } else {
                        isError = true
                    }
                } else {
                    isError = true
                }
            } catch (e: Exception) {
                Log.e("Cape2DPreview", "Failed to load cape image: $capeUrl", e)
                isError = true
            }
        }
    }

    val bmp = bitmapState
    if (bmp != null) {
        Canvas(modifier = modifier) {
            val bw = bmp.width.toFloat()
            val bh = bmp.height.toFloat()

            val (srcLeft, srcTop, srcWidth, srcHeight) = if (bw <= 32) {
                val scaleX = bw / 22f
                val scaleY = bh / 17f
                listOf(
                    (1 * scaleX).toInt().coerceIn(0, bmp.width - 1),
                    (1 * scaleY).toInt().coerceIn(0, bmp.height - 1),
                    (10 * scaleX).toInt().coerceAtLeast(1),
                    (16 * scaleY).toInt().coerceAtLeast(1)
                )
            } else {
                val scaleX = bw / 64f
                val scaleY = bh / 32f
                listOf(
                    (12 * scaleX).toInt().coerceIn(0, bmp.width - 1),
                    (1 * scaleY).toInt().coerceIn(0, bmp.height - 1),
                    (10 * scaleX).toInt().coerceAtLeast(1),
                    (16 * scaleY).toInt().coerceAtLeast(1)
                )
            }

            drawImage(
                image = bmp.asImageBitmap(),
                srcOffset = IntOffset(srcLeft, srcTop),
                srcSize = IntSize(srcWidth, srcHeight),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    } else if (isError) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = translatedText("Cape"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextureSelectionScreen(
    type: TextureType,
    account: Account?,
    onApplied: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Browse, 1: Saved
    var browseItems by remember { mutableStateOf<List<TextureItem>>(emptyList()) }
    var isLoadingBrowse by remember { mutableStateOf(true) }

    var currentPage by remember { mutableIntStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMorePages by remember { mutableStateOf(true) }

    var savedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isApplying by remember { mutableStateOf(false) }

    var pendingCustomUri by remember { mutableStateOf<Uri?>(null) }
    var customNameDialogVisible by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    fun refreshSavedFiles() {
        val folder = File(context.filesDir, if (type == TextureType.CAPE) "capes" else "skins")
        folder.mkdirs()
        savedFiles = folder.listFiles { _, name -> name.endsWith(".png", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun fetchPage(page: Int): List<TextureItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<TextureItem>()
        val seenHashes = mutableSetOf<String>()

        if (type == TextureType.CAPE) {
            if (page == 1) {
                for ((cName, cHash, cId) in curatedCapes) {
                    if (seenHashes.add(cHash)) {
                        items.add(
                            TextureItem(
                                id = cId,
                                name = cName,
                                url = "https://textures.minecraft.net/texture/$cHash"
                            )
                        )
                    }
                }
            }

            val sources = capeSources
            if (page <= sources.size) {
                val sourceUrl = sources[page - 1]
                try {
                    val url = URL(sourceUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        connectTimeout = 8000
                        readTimeout = 10000
                    }

                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val html = conn.inputStream.bufferedReader().use { it.readText() }
                        val hashRegex = Regex("""hash:\s*["']([a-f0-9]{64})["']""", RegexOption.IGNORE_CASE)
                        val nameRegex = Regex("""name:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

                        val matches = hashRegex.findAll(html).toList()
                        for (match in matches) {
                            val hashVal = match.groupValues[1]
                            if (!seenHashes.add(hashVal)) continue

                            val startIndex = (match.range.first - 150).coerceAtLeast(0)
                            val snippet = html.substring(startIndex, match.range.first)
                            val capeName = nameRegex.findAll(snippet).lastOrNull()?.groupValues?.get(1) ?: "Cape ${items.size + 1}"

                            items.add(
                                TextureItem(
                                    id = "crafty_cape_${hashVal.take(12)}",
                                    name = capeName,
                                    url = "https://textures.minecraft.net/texture/$hashVal"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TextureSelection", "Error fetching capes page $page from crafty.gg", e)
                }
            }
        } else {
            // SKIN
            val sources = skinSources
            if (page <= sources.size) {
                val sourceUrl = sources[page - 1]
                try {
                    val url = URL(sourceUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        connectTimeout = 8000
                        readTimeout = 10000
                    }

                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val html = conn.inputStream.bufferedReader().use { it.readText() }
                        val hashRegex = Regex("""hash:\s*["']([a-f0-9]{64})["']""", RegexOption.IGNORE_CASE)
                        val userRegex = Regex("""username:\s*["']([A-Za-z0-9_]+)["']""", RegexOption.IGNORE_CASE)
                        val altRegex = Regex("""first worn by ([A-Za-z0-9_]+)""", RegexOption.IGNORE_CASE)

                        val matches = hashRegex.findAll(html).toList()
                        for (match in matches) {
                            val hashVal = match.groupValues[1]
                            if (!seenHashes.add(hashVal)) continue

                            val subAfter = html.substring(match.range.last, (match.range.last + 300).coerceAtMost(html.length))
                            val subBefore = html.substring((match.range.first - 300).coerceAtLeast(0), match.range.first)

                            val username = userRegex.find(subAfter)?.groupValues?.get(1)
                                ?: userRegex.find(subBefore)?.groupValues?.get(1)
                                ?: altRegex.find(subAfter)?.groupValues?.get(1)
                                ?: altRegex.find(subBefore)?.groupValues?.get(1)

                            val skinName = username ?: "Skin ${items.size + 1}"
                            items.add(
                                TextureItem(
                                    id = "crafty_skin_${hashVal.take(12)}",
                                    name = skinName,
                                    url = "https://textures.minecraft.net/texture/$hashVal"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TextureSelection", "Error fetching skins page $page from crafty.gg", e)
                }
            }

            if (items.isEmpty()) {
                val mineSkinItems = fetchMineSkinFallback(page)
                for (ms in mineSkinItems) {
                    val hashVal = ms.url.substringAfterLast("/")
                    if (seenHashes.add(hashVal)) {
                        items.add(ms)
                    }
                }
            }

            if (page == 1) {
                val playersToLoad = if (items.size < 10) featuredPlayers else featuredPlayers.take(10)
                for (player in playersToLoad) {
                    items.add(
                        TextureItem(
                            id = "player_skin_$player",
                            name = player,
                            url = "https://mc-heads.net/skin/$player"
                        )
                    )
                }
            }
        }

        items
    }

    fun loadNextPage() {
        val sources = if (type == TextureType.CAPE) capeSources else skinSources
        if (isLoadingMore || !hasMorePages || currentPage >= sources.size) {
            hasMorePages = false
            return
        }
        isLoadingMore = true
        val nextPage = currentPage + 1
        scope.launch(Dispatchers.IO) {
            try {
                val newItems = fetchPage(nextPage)
                withContext(Dispatchers.Main) {
                    if (newItems.isEmpty()) {
                        if (nextPage >= sources.size) {
                            hasMorePages = false
                        }
                    } else {
                        val existingIds = browseItems.map { it.id }.toSet()
                        val filteredNew = newItems.filter { it.id !in existingIds }
                        if (filteredNew.isNotEmpty()) {
                            browseItems = browseItems + filteredNew
                        }
                        currentPage = nextPage
                        if (currentPage >= sources.size) {
                            hasMorePages = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TextureSelection", "Failed to load page $nextPage", e)
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingMore = false
                }
            }
        }
    }

    LaunchedEffect(type) {
        refreshSavedFiles()
        isLoadingBrowse = true
        currentPage = 1
        val sources = if (type == TextureType.CAPE) capeSources else skinSources
        hasMorePages = sources.size > 1
        scope.launch(Dispatchers.IO) {
            try {
                val initialItems = fetchPage(1)
                withContext(Dispatchers.Main) {
                    browseItems = initialItems
                }
            } catch (e: Exception) {
                Log.e("TextureSelection", "Failed to fetch initial page from crafty.gg", e)
                withContext(Dispatchers.Main) {
                    browseItems = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingBrowse = false
                }
            }
        }
    }

    val customPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentWithExtension("image/png")
    ) { uri ->
        uri?.let {
            pendingCustomUri = it
            customNameDialogVisible = true
        }
    }

    fun applyTexture(item: TextureItem) {
        if (account == null) {
            Toast.makeText(context, "No active account selected", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isApplying = true
            try {
                val folder = File(context.filesDir, if (type == TextureType.CAPE) "capes" else "skins")
                folder.mkdirs()

                val savedFile: File? = if (item.localFile != null && item.localFile.exists()) {
                    item.localFile
                } else {
                    val sanitizedName = item.name.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
                    val targetFile = File(folder, "${type.name.lowercase()}_${sanitizedName}_${System.currentTimeMillis()}.png")

                    withContext(Dispatchers.IO) {
                        if (item.url.startsWith("file://")) {
                            val srcFile = File(item.url.substring(7))
                            if (srcFile.exists()) srcFile.copyTo(targetFile, overwrite = true)
                        } else {
                            val conn = (URL(item.url).openConnection() as HttpURLConnection).apply {
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                                connectTimeout = 10000
                                readTimeout = 15000
                            }
                            conn.inputStream.use { input ->
                                FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                            }
                        }
                    }
                    if (targetFile.exists() && targetFile.length() > 0) targetFile else null
                }

                if (savedFile != null) {
                    val path = savedFile.absolutePath
                    if (type == TextureType.CAPE) {
                        account.capePath = path
                    } else {
                        account.skinPath = path
                    }
                    account.save()
                    SkinManager.instance.registerAndStartServer(account)

                    Toast.makeText(context, "${type.name.lowercase().replaceFirstChar { it.uppercase() }} applied!", Toast.LENGTH_SHORT).show()
                    refreshSavedFiles()
                    onApplied()
                } else {
                    Toast.makeText(context, "Failed to download texture", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("TextureSelection", "Error applying texture", e)
                Toast.makeText(context, "Error applying texture: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isApplying = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    modifier = Modifier.fillMaxWidth(),
                    indicator = @Composable {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(selectedTab)
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(translatedText("Browse"), fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(translatedText("Saved"), fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(0.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedTab == 0) {
                        if (isLoadingBrowse) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator()
                            }
                        } else if (browseItems.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translatedText("No ${type.name.lowercase()}s found."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val browseGridState = rememberLazyGridState()

                            val shouldLoadMore by remember {
                                derivedStateOf {
                                    val layoutInfo = browseGridState.layoutInfo
                                    val totalItems = layoutInfo.totalItemsCount
                                    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    totalItems > 0 && lastVisibleIndex >= totalItems - 6
                                }
                            }

                            LaunchedEffect(shouldLoadMore) {
                                if (shouldLoadMore && !isLoadingBrowse && !isLoadingMore && hasMorePages) {
                                    loadNextPage()
                                }
                            }

                            LazyVerticalGrid(
                                state = browseGridState,
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(browseItems, key = { it.id }) { item ->
                                    val isCurrent = if (type == TextureType.CAPE) {
                                        account?.capePath != null && item.localFile?.absolutePath == account.capePath
                                    } else {
                                        account?.skinPath != null && item.localFile?.absolutePath == account.skinPath
                                    }

                                    TextureCard(
                                        type = type,
                                        item = item,
                                        isApplied = isCurrent,
                                        onApply = { applyTexture(item) }
                                    )
                                }

                                if (isLoadingMore) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LoadingIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (savedFiles.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translatedText("No saved ${type.name.lowercase()}s yet.\nTap the '+' button at the bottom right to add one!"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(savedFiles, key = { it.absolutePath }) { file ->
                                    val currentPath = if (type == TextureType.CAPE) account?.capePath else account?.skinPath
                                    val isCurrent = currentPath == file.absolutePath

                                    val nameClean = file.name
                                        .removePrefix("cape_")
                                        .removePrefix("skin_")
                                        .substringBeforeLast("_")
                                        .ifEmpty { file.nameWithoutExtension }

                                    val item = TextureItem(
                                        id = file.absolutePath,
                                        name = nameClean,
                                        url = "file://${file.absolutePath}",
                                        localFile = file,
                                        isCustom = true
                                    )

                                    TextureCard(
                                        type = type,
                                        item = item,
                                        isApplied = isCurrent,
                                        onApply = { applyTexture(item) },
                                        onDelete = { fileToDelete = file }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { customPickerLauncher.launch(null) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = translatedText("Add Custom"))
                }
            }

            if (isApplying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) {},
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            LoadingIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = translatedText("Applying..."),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (customNameDialogVisible && pendingCustomUri != null) {
                DialogTextInput(
                    title = "Name your custom ${type.name.lowercase()}",
                    initialValue = "${type.name.lowercase()}_${System.currentTimeMillis()}",
                    onConfirm = { name ->
                        customNameDialogVisible = false
                        val uri = pendingCustomUri
                        pendingCustomUri = null
                        if (uri != null) {
                            scope.launch {
                                isApplying = true
                                val folder = File(context.filesDir, if (type == TextureType.CAPE) "capes" else "skins")
                                folder.mkdirs()
                                val sanitizedName = name.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
                                val destFile = File(folder, "${type.name.lowercase()}_${sanitizedName}.png")

                                try {
                                    withContext(Dispatchers.IO) {
                                        context.contentResolver.openInputStream(uri)?.use { input ->
                                            FileOutputStream(destFile).use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                    }

                                    if (destFile.exists() && account != null) {
                                        val path = destFile.absolutePath
                                        if (type == TextureType.CAPE) {
                                            account.capePath = path
                                        } else {
                                            account.skinPath = path
                                        }
                                        account.save()
                                        SkinManager.instance.registerAndStartServer(account)

                                        Toast.makeText(context, "${type.name.lowercase().replaceFirstChar { it.uppercase() }} saved & applied!", Toast.LENGTH_SHORT).show()
                                        refreshSavedFiles()
                                        onApplied()
                                    }
                                } catch (e: Exception) {
                                    Log.e("TextureSelection", "Failed to save custom file", e)
                                } finally {
                                    isApplying = false
                                }
                            }
                        }
                    },
                    onDismiss = {
                        customNameDialogVisible = false
                        pendingCustomUri = null
                    }
                )
            }

            if (fileToDelete != null) {
                SimpleAlertDialog(
                    title = translatedText("Delete ${type.name.lowercase()}"),
                    text = translatedText("Are you sure you want to delete this saved ${type.name.lowercase()}?"),
                    confirmText = translatedText("Delete"),
                    onConfirm = {
                        fileToDelete?.delete()
                        fileToDelete = null
                        refreshSavedFiles()
                    },
                    dismissText = translatedText("Cancel"),
                    onDismiss = { fileToDelete = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextureCard(
    type: TextureType,
    item: TextureItem,
    isApplied: Boolean,
    onApply: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onDelete?.invoke() }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (type == TextureType.CAPE) {
                    Cape2DPreview(
                        capeUrl = item.url,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(10f / 16f)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else if (item.localFile != null && item.localFile.exists()) {
                    SkinPreview(
                        modifier = Modifier.fillMaxSize(),
                        skinUrl = item.url,
                        capeUrl = null,
                        showSkin = true,
                        backEquipment = "cape",
                        animation = "NewIdle",
                        azimuth = 0f
                    )
                } else {
                    val previewUrl = remember(item.url, item.name) {
                        val hash = item.url.substringAfterLast("/")
                        if (hash.length == 64) {
                            "https://render.crafty.gg/3d/bust/$hash"
                        } else if (item.name.isNotEmpty()) {
                            "https://render.crafty.gg/3d/bust/${item.name}"
                        } else {
                            "https://render.crafty.gg/3d/bust/MHF_Alex"
                        }
                    }

                    val imageRequest = remember(previewUrl, context) {
                        ImageRequest.Builder(context)
                            .data(previewUrl)
                            .crossfade(true)
                            .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .build()
                    }

                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator()
                            }
                        },
                        error = {
                            val fallbackRequest = remember(context) {
                                ImageRequest.Builder(context)
                                    .data("https://render.crafty.gg/3d/bust/MHF_Alex")
                                    .crossfade(true)
                                    .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    .build()
                            }

                            SubcomposeAsyncImage(
                                model = fallbackRequest,
                                contentDescription = item.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isApplied) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (isApplied) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isApplied) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(translatedText("Active"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(translatedText("Apply"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
