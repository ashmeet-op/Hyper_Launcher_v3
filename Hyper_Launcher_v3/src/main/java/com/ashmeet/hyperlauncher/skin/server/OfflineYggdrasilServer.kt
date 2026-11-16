package com.ashmeet.hyperlauncher.skin.server

import android.util.Base64
import android.util.Log
import com.ashmeet.hyperlauncher.skin.PlayerCape
import com.ashmeet.hyperlauncher.skin.PlayerSkin
import com.ashmeet.hyperlauncher.skin.model.SkinModelType
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class OfflineYggdrasilServer(
    private val serverName: String = "HyperLauncher",
    private val implName: String   = "drasl",
    private val implVersion: String = "1.4"
) {
    @Suppress("PrivatePropertyName")
    private val TAG = "OfflineSkinServer"
    private val byUuid = ConcurrentHashMap<String, Character>()
    private val byName = ConcurrentHashMap<String, Character>()
    private val textureStore = ConcurrentHashMap<String, ByteArray>()

    private val keyPair by lazy {
        KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }.genKeyPair()
    }

    private var server: EmbeddedServer<*, *>? = null
    private val startLatch = CountDownLatch(1)
    private var running = false

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun addCharacter(
        username: String,
        profileId: String,
        skin: PlayerSkin? = null,
        cape: PlayerCape? = null
    ) {
        val uuid = profileId.replace("-", "").lowercase()
        val char = Character(uuid, username, skin, cape)
        byUuid[uuid] = char
        byName[username.lowercase()] = char
        skin?.let { textureStore[it.hash] = it.bytes }
        cape?.let { textureStore[it.hash] = it.bytes }
        Log.d(TAG, "Added character: $username ($uuid)")
    }

    fun start(timeoutSec: Long = 10L): Int {
        val s = embeddedServer(CIO, host = "127.0.0.1", port = 0) {
            monitor.subscribe(ApplicationStarted) { startLatch.countDown() }

            install(ContentNegotiation) {
                json(json)
            }
            routing { registerRoutes() }
        }
        server = s
        s.start(wait = false)

        check(startLatch.await(timeoutSec, TimeUnit.SECONDS)) {
            "OfflineYggdrasilServer failed to start within ${timeoutSec}s"
        }
        running = true
        val boundPort = port() ?: 0
        Log.i(TAG, "Server started on 127.0.0.1:$boundPort")
        return boundPort
    }

    fun stop() {
        running = false
        server?.stop(1000, 5000)
    }

    fun port(): Int? {
        if (!running) return null
        return runBlocking {
            server?.engine?.resolvedConnectors()?.firstOrNull()?.port
        }
    }

    private fun Routing.registerRoutes() {

        get("/") {
            Log.i(TAG, "GET / - Metadata requested")
            call.respondText(buildRoot(), ContentType.Application.Json)
        }

        get("/api") {
            Log.i(TAG, "GET /api requested")
            call.respondText(buildRoot(), ContentType.Application.Json)
        }

        post("/api/profiles/minecraft") {
            Log.i(TAG, "POST /api/profiles/minecraft requested")
            val names = runCatching { call.receive<List<String>>() }.getOrDefault(emptyList())
            val result = buildJsonArray {
                names.distinct().mapNotNull { byName[it.lowercase()] }.forEach { c ->
                    add(buildJsonObject {
                        put("id", JsonPrimitive(c.uuid))
                        put("name", JsonPrimitive(c.name))
                    })
                }
            }
            call.respondText(result.toString(), ContentType.Application.Json)
        }

        get("/sessionserver/session/minecraft/hasJoined") {
            val username = call.request.queryParameters["username"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            Log.i(TAG, "INJECTOR: GET /hasJoined for $username")
            val char = byName[username.lowercase()]
                ?: run {
                    Log.w(TAG, "INJECTOR: Character not found for $username")
                    return@get call.respond(HttpStatusCode.NoContent)
                }

            Log.i(TAG, "INJECTOR: Serving profile for $username")
            call.respondText(
                char.toProfileResponse(localBase(), ::signRsa, json),
                ContentType.Application.Json.withCharset(Charsets.UTF_8)
            )
        }

        post("/sessionserver/session/minecraft/join") {
            call.respond(HttpStatusCode.NoContent)
        }

        get("/sessionserver/session/minecraft/profile/{uuid}") {
            val rawUuid = call.parameters["uuid"] ?: ""
            val uuid = rawUuid.replace("-", "").lowercase()
            Log.i(TAG, "INJECTOR: GET /profile/$rawUuid requested")

            val char = byUuid[uuid]
                ?: run {
                    Log.w(TAG, "INJECTOR: Profile not found for UUID: $uuid")
                    return@get call.respond(HttpStatusCode.NoContent)
                }

            Log.i(TAG, "INJECTOR: Serving profile for UUID: $uuid")
            call.respondText(
                char.toProfileResponse(localBase(), ::signRsa, json),
                ContentType.Application.Json.withCharset(Charsets.UTF_8)
            )
        }

        get("/textures/{hash}") {
            val hash = call.parameters["hash"]
                ?: return@get call.respond(HttpStatusCode.NotFound)

            Log.d(TAG, "GET /textures/$hash - Download requested")
            val bytes = textureStore[hash]
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.response.header(HttpHeaders.CacheControl, "max-age=2592000, public")
            call.response.header(HttpHeaders.ETag, "\"$hash\"")
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respondBytes(bytes, ContentType.Image.PNG)
        }
    }

    private fun localBase() = "http://127.0.0.1:${port()}"

    private fun buildRoot(): String = buildJsonObject {
        put("skinDomains", buildJsonArray {
            add("127.0.0.1")
            add("localhost")
        })
        put("meta", buildJsonObject {
            put("serverName", JsonPrimitive(serverName))
            put("implementationName", JsonPrimitive(implName))
            put("implementationVersion", JsonPrimitive(implVersion))
            put("feature.non_email_login", JsonPrimitive(true))
            put("feature.legacy_skin_api", JsonPrimitive(true))
            put("links", buildJsonObject {
                put("homepage", JsonPrimitive("https://github.com/unmojang/drasl"))
                put("register", JsonPrimitive("https://github.com/unmojang/drasl"))
            })
        })

        val publicKeyBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT).trim()
        put(
            "signaturePublickey", JsonPrimitive(
                "-----BEGIN PUBLIC KEY-----\n$publicKeyBase64\n-----END PUBLIC KEY-----"
            )
        )
    }.toString()

    private fun signRsa(data: String): String {
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initSign(keyPair.private)
        sig.update(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
    }

    private data class Character(
        val uuid: String,
        val name: String,
        val skin: PlayerSkin? = null,
        val cape: PlayerCape? = null
    ) {
        fun toProfileResponse(baseUrl: String, signer: (String) -> String, json: Json): String {
            val formattedUuid = if (uuid.length == 32) {
                "${uuid.take(8)}-${uuid.substring(8, 12)}-${uuid.substring(12, 16)}-${uuid.substring(16, 20)}-${uuid.substring(20)}"
            } else uuid

            val texturesObj = buildJsonObject {
                put("timestamp", JsonPrimitive(System.currentTimeMillis()))
                put("profileId", JsonPrimitive(formattedUuid))
                put("profileName", JsonPrimitive(name))
                put("textures", buildJsonObject {
                    skin?.let { s ->
                        put("SKIN", buildJsonObject {
                            put("url", JsonPrimitive("$baseUrl/textures/${s.hash}"))
                            if (s.model == SkinModelType.ALEX) {
                                put("metadata", buildJsonObject {
                                    put("model", JsonPrimitive("slim"))
                                })
                            }
                        })
                    }
                    cape?.let { c ->
                        put("CAPE", buildJsonObject {
                            put("url", JsonPrimitive("$baseUrl/textures/${c.hash}"))
                        })
                    }
                })
            }

            val texturesJson = json.encodeToString(JsonObject.serializer(), texturesObj)
            val encoded = Base64.encodeToString(
                texturesJson.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val signature = signer(encoded)
            val response = buildJsonObject {
                put("id", JsonPrimitive(uuid))
                put("name", JsonPrimitive(name))
                put("properties", buildJsonArray {
                    add(buildJsonObject {
                        put("name", JsonPrimitive("textures"))
                        put("value", JsonPrimitive(encoded))
                        put("signature", JsonPrimitive(signature))
                    })
                })
            }
            return json.encodeToString(JsonObject.serializer(), response)
        }
    }
}