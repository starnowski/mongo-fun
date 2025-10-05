package com.github.starnowski.mongo.fun.mongodb.container.odata;

public class NormalizeHelper {

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replaceAll("\\s+", " ");

        // Normalize Unicode (e.g., é → e + ´)
        normalized = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFC);

        // Lowercase for consistency
        return normalized.toLowerCase();
    }
}
