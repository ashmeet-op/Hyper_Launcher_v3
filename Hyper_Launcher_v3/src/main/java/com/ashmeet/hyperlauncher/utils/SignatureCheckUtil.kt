package com.ashmeet.hyperlauncher.utils

import android.content.res.AssetManager
import android.util.ArrayMap
import android.util.Base64
import java.io.IOException
import java.io.InputStream
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory

class SignatureCheckUtil(private val mPublicKey: PublicKey) {

    @Throws(IOException::class)
    fun verify(inputStream: InputStream, signatureBytes: ByteArray): Boolean {
        val ingestionBuffer = ByteArray(65535)
        return try {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(mPublicKey)
            var i: Int
            while (inputStream.read(ingestionBuffer).also { i = it } != -1) {
                signature.update(ingestionBuffer, 0, i)
            }
            signature.verify(signatureBytes)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: SignatureException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        @JvmStatic
        fun decodeSignatureBundle(bundle: String): Map<String, ByteArray> {
            val signatureLines = bundle.split("\n")
            val signatures = ArrayMap<String, ByteArray>(signatureLines.size)
            for (signatureLine in signatureLines) {
                val splitSignLine = signatureLine.split(":")
                if (splitSignLine.size != 2) continue
                try {
                    val signatureBytes = decodeRsa4096FromBase64(splitSignLine[1]) ?: continue
                    signatures[splitSignLine[0]] = signatureBytes
                } catch (_: IllegalArgumentException) {}
            }
            return signatures
        }

        @JvmStatic
        fun decodeRsa4096FromBase64(base64: String): ByteArray? {
            val rsaBytes = Base64.decode(base64, Base64.DEFAULT)
            if (rsaBytes.size != 512) return null
            return rsaBytes
        }

        @JvmStatic
        @Throws(IOException::class)
        fun create(assetManager: AssetManager): SignatureCheckUtil {
            try {
                assetManager.open("cert.pem").use { certificateStream ->
                    val certificateFactory = CertificateFactory.getInstance("X.509")
                    val certificate = certificateFactory.generateCertificate(certificateStream)
                    return SignatureCheckUtil(certificate.publicKey)
                }
            } catch (e: CertificateException) {
                throw RuntimeException(e)
            }
        }
    }
}
