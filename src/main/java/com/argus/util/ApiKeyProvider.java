package com.argus.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Utility per risolvere la STEAM_API_KEY.
 * Priorità di risoluzione:
 * 1. Variabile d'ambiente STEAM_API_KEY (utile per dev locale o override manuale)
 * 2. Proprietà di sistema -DSTEAM_API_KEY
 * 3. File incorporato /steam_api.properties (iniettato e offuscato a tempo di build)
 */
public final class ApiKeyProvider {

    private static final byte XOR_MASK = 0x5A;

    private ApiKeyProvider() {}

    public static String getSteamApiKey() {
        // 1. Variabile d'ambiente
        String envKey = System.getenv("STEAM_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }

        // 2. Proprietà di sistema
        String sysPropKey = System.getProperty("STEAM_API_KEY");
        if (sysPropKey != null && !sysPropKey.trim().isEmpty()) {
            return sysPropKey.trim();
        }

        // 3. Risorsa inclusa cifrata a compile/build time
        try (InputStream in = ApiKeyProvider.class.getResourceAsStream("/steam_api.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String obfuscated = props.getProperty("steam.api.payload");
                if (obfuscated != null && !obfuscated.isBlank()) {
                    return deobfuscate(obfuscated.trim());
                }
            }
        } catch (Exception ignored) {
            // Se fallisce la lettura della risorsa opzionale
        }

        return null;
    }

    /**
     * Deoffusca la stringa codificata in hex + XOR.
     */
    public static String deobfuscate(String hex) {
        try {
            int len = hex.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                int high = Character.digit(hex.charAt(i), 16);
                int low = Character.digit(hex.charAt(i + 1), 16);
                data[i / 2] = (byte) (((high << 4) + low) ^ XOR_MASK);
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Utility per offuscare una stringa (usata nello script di build su GitHub Actions).
     */
    public static String obfuscate(String raw) {
        if (raw == null) return "";
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int masked = (b ^ XOR_MASK) & 0xFF;
            sb.append(String.format("%02x", masked));
        }
        return sb.toString();
    }
}
