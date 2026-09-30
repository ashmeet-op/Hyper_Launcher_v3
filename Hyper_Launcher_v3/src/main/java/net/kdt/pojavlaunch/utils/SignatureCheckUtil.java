package net.kdt.pojavlaunch.utils;

import android.content.res.AssetManager;
import android.util.ArrayMap;
import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Map;

public class SignatureCheckUtil {
    private final PublicKey mPublicKey;

    public SignatureCheckUtil(PublicKey mPublicKey) {
        this.mPublicKey = mPublicKey;
    }


    public static Map<String,byte[]> decodeSignatureBundle(String bundle) {
        String[] signatureLines = bundle.split("\n");
        ArrayMap<String, byte[]> signatures = new ArrayMap<>(signatureLines.length);
        for(String signatureLine : signatureLines) {
            String[] splitSignLine = signatureLine.split(":");
            if(splitSignLine.length != 2) continue;
            try {
                byte[] signatureBytes = decodeRsa4096FromBase64(splitSignLine[1]);
                if(signatureBytes == null) continue;
                signatures.put(splitSignLine[0], signatureBytes);
            }catch (IllegalArgumentException ignored) {}
        }
        return signatures;
    }


    public static byte[] decodeRsa4096FromBase64(String base64) {
        byte[] rsaBytes = Base64.decode(base64, Base64.DEFAULT);
        if(rsaBytes.length != 512) return null;
        return rsaBytes;
    }


    public boolean verify(InputStream inputStream, byte[] signatureBytes) throws IOException {
        byte[] ingestionBuffer = new byte[65535];
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(mPublicKey);
            for (int i = 0; i != -1; i = inputStream.read(ingestionBuffer)) {
                signature.update(ingestionBuffer, 0, i);
            }
            return signature.verify(signatureBytes);
        }catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }


    public static SignatureCheckUtil create(AssetManager assetManager) throws IOException {
        try (InputStream certificateStream = assetManager.open("cert.pem")) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate certificate = certificateFactory.generateCertificate(certificateStream);
            return new SignatureCheckUtil(certificate.getPublicKey());
        }catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }
}
