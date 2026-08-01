package com.pablomusaber.watson.shared;

public final class JsonUtils {

    private JsonUtils() {}

    public static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl >= 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }
}
