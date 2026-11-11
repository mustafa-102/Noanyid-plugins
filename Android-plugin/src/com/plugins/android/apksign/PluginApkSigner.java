package com.plugins.android.apksign;

import com.android.apksig.*;
import com.android.apksig.ApkSigner.SignerConfig;

import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.io.*;

import com.plugins.android.*;

public class PluginApkSigner {
    // private final Main main;
    public File keystore, unsignedApkFile;
    public String keystorePath, alias, storePass, keyPass;

    public PluginApkSigner(Main main) {
        // this.main = main;
    }

    public String sign() throws Exception {
        File back ;
        if (keystorePath == null)
            back =_sign(safe(this.unsignedApkFile.getPath()));
        else
           back = _sign(safe(this.unsignedApkFile.getPath()), safe(keystorePath), safe(storePass), safe(alias), safe(keyPass));

        System.out.println("SIGNED");
         return back.toString();
    }

    private static File _sign(String apkPath) throws Exception {
        File ksFile = new File(System.getProperty("java.io.tmpdir"), "defaultDebug.jks");
        
        if (!ksFile.exists()) {
            createDebugKeystore(ksFile.getPath(), "123456", "key0", "123456");
        }
        return _sign(apkPath, ksFile.getPath(), "123456", "key0", "123456");
    }

    private static File _sign(String apkPath, String keystorePath, String storePass, String keyName, String keyPass)
            throws Exception {
        File input = new File(apkPath);
        File output = new File(input.getParent(), input.getName().replace(".apk", "-signed.apk"));

        // Load keystore
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, storePass.toCharArray());
        }

        PrivateKey privateKey = (PrivateKey) ks.getKey(keyName, keyPass.toCharArray());
        List<X509Certificate> certs = new ArrayList<>();
        for (java.security.cert.Certificate c : ks.getCertificateChain(keyName)) {
            certs.add((X509Certificate) c);
        }

        SignerConfig config = new SignerConfig.Builder(keyName, privateKey, certs).build();

        new ApkSigner.Builder(List.of(config))
                .setInputApk(input)
                .setOutputApk(output)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign();
        System.out.println("OK");
        return output;
    }

    private static void createDebugKeystore(String path, String storePass, String keyName, String keyPass)
            throws Exception {
        File f = new File(path);
        f.getParentFile().mkdirs();
        if (f.exists()) f.delete();
        
        String keytool = System.getProperty("java.home") + "/bin/keytool";

        String cmd = String.format(
                keytool + " -genkeypair -keystore \"%s\" -storepass %s -keypass %s -alias %s -keyalg RSA -keysize 2048 -validity 10000 -dname \"CN=Debug, OU=HH, O=Debug, L=Khartoum, S=Khartoum, C=SD\"",
                path, storePass, keyPass, keyName
        );
        Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
        p.waitFor();
        if (!f.exists()) throw new RuntimeException("keytool failed");
    }

    public String safe(String val) {
        return val == null ? "" : val;
    }
    
}
