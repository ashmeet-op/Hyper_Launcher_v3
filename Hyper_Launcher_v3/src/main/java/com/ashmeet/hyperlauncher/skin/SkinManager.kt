package com.ashmeet.hyperlauncher.skin

import com.ashmeet.hyperlauncher.skin.model.SkinModelType
import com.ashmeet.hyperlauncher.skin.server.OfflineYggdrasilServer
import com.ashmeet.hyperlauncher.utils.LocalUuidUtils
import com.ashmeet.hyperlauncher.utils.LocalUuidUtils.toFormattedUuid
import com.ashmeet.hyperlauncher.utils.Tools
import java.io.File


class SkinManager(private val analyzer: SkinAnalyzerFacade) {


    interface SkinAnalyzerFacade {

        fun prepareSkin(bytes: ByteArray): PlayerSkin?
        fun prepareCape(bytes: ByteArray): PlayerCape
    }

    val server = OfflineYggdrasilServer()
    private var port: Int = 0

    @Throws(InvalidSkinException::class)
    fun prepareAccount(
        username: String,
        skinFile: File? = null,
        capeFile: File? = null,
        modelOverride: SkinModelType? = null
    ): PreparedAccount {
        val skinBytes = skinFile?.takeIf { it.exists() }?.readBytes()
        val capeBytes = capeFile?.takeIf { it.exists() }?.readBytes()

        val skin: PlayerSkin? = skinBytes?.let {
            val base = analyzer.prepareSkin(it)
                ?: throw InvalidSkinException("${skinFile.name ?: "Skin file"} must be 64×64 or 64×32 pixels")

            if (modelOverride != null && modelOverride != base.model)
                base.copy(model = modelOverride)
            else base
        }

        val cape: PlayerCape? = capeBytes?.let { analyzer.prepareCape(it) }

        val model = skin?.model ?: SkinModelType.NONE
        val profileId = LocalUuidUtils.generateProfileId(username, model)

        server.addCharacter(
            username  = username,
            profileId = profileId,
            skin      = skin,
            cape      = cape
        )

        val permanentSkin = skinFile?.takeIf { it.exists() }?.let {
            val file = File(Tools.DIR_CACHE, "skin-$profileId.png")
            if (it.absolutePath != file.absolutePath) {
                it.copyTo(file, overwrite = true)
            }
            file.absolutePath
        }

        val permanentCape = capeFile?.takeIf { it.exists() }?.let {
            val file = File(Tools.DIR_CACHE, "cape-$profileId.png")
            if (it.absolutePath != file.absolutePath) {
                it.copyTo(file, overwrite = true)
            }
            file.absolutePath
        }

        return PreparedAccount(
            username      = username,
            profileId     = profileId,
            formattedUuid = profileId.toFormattedUuid(),
            skinModel     = model,
            skinPath      = permanentSkin,
            capePath      = permanentCape
        )
    }


    fun startServer(): Int {
        port = server.start()
        return port
    }


    val authlibUrl: String get() = "http://127.0.0.1:$port"
}


data class PreparedAccount(
    val username: String,
    val profileId: String,
    val formattedUuid: String,
    val skinModel: SkinModelType,
    val skinPath: String?,
    val capePath: String?
)

class InvalidSkinException(message: String) : Exception(message)

@JvmField
val androidSkinAnalyzerFacade = object : SkinManager.SkinAnalyzerFacade {
    override fun prepareSkin(bytes: ByteArray) = AndroidSkinAnalyzer.prepareSkin(bytes)
    override fun prepareCape(bytes: ByteArray) = AndroidSkinAnalyzer.prepareCape(bytes)
}
