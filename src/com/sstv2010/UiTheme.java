package com.sstv2010;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

public final class UiTheme {
    public static final String BG_OLED = "Oled";
    public static final String BG_BLUE = "Blue";
    public static final String BG_GREEN = "Green";
    public static final String BG_PURPLE = "Lpurple";
    public static final String BG_LIGHT = "Tight";
    public static final String BG_TEAL = "Teal";
    public static final String BG_HELL = "Hell";
    public static final String BG_DISCORD = "That blue chatting app";

    public static final String[] BG_KEYS = new String[] {
        BG_OLED, BG_BLUE, BG_GREEN, BG_PURPLE, BG_LIGHT, BG_TEAL, BG_HELL, BG_DISCORD
    };
    public static final String[] BG_LABELS = new String[] {
        "Oled", "Blue", "Green", "Purple", "Light", "Teal", "Hell", "Discord"
    };
    public static final int[] BG_COLORS = new int[] {
        0xFF000000, 0xFF304860, 0xFF3C504A, 0xFF62526D, 0xFF565557, 0xFF507470, 0xFF16000A, 0xFF36393E
    };

    private UiTheme() {}

    public static int indexForKey(String key) {
        if (key == null) return 0;
        for (int i = 0; i < BG_KEYS.length; i++) if (BG_KEYS[i].equals(key)) return i;
        // legacy values
        if ("default".equals(key)) return 0;
        if ("dark".equals(key)) return 0;
        if ("light".equals(key)) return 4;
        if ("black".equals(key)) return 0;
        if ("charcoal".equals(key)) return 0;
        if ("dark_blue".equals(key)) return 1;
        if ("dark_green".equals(key)) return 2;
        if ("dark_purple".equals(key)) return 3;
        if ("dark_red".equals(key)) return 6;
        if ("dark_brown".equals(key)) return 5;
        if ("dark_slate".equals(key)) return 5;
        return 0;
    }

    public static String keyForIndex(int idx) {
        if (idx < 0 || idx >= BG_KEYS.length) return BG_OLED;
        return BG_KEYS[idx];
    }

    public static void applyGlobalBackground(Activity a) {
        if (a == null) return;
        try {
            String custom = AppSettings.customBackground(a);
            if (custom != null && custom.length() > 0) {
                int color = parseColor(custom);
                if (color != 0) {
                    View root = a.getWindow() != null ? a.getWindow().getDecorView() : null;
                    if (root != null) root.setBackgroundColor(color);
                    try { View v = a.findViewById(android.R.id.content); if (v != null) v.setBackgroundColor(color); } catch (Throwable ignore) {}
                    return;
                }
            }
            String key = AppSettings.globalBackground(a);
            int idx = indexForKey(key);
            int color = BG_COLORS[idx];
            View root = a.getWindow() != null ? a.getWindow().getDecorView() : null;
            if (root != null) root.setBackgroundColor(color);
            try { View v = a.findViewById(android.R.id.content); if (v != null) v.setBackgroundColor(color); } catch (Throwable ignore) {}
        } catch (Throwable ignore) {}
    }

    public static int parseColor(String hex) {
        try {
            String h = hex.trim();
            if (!h.startsWith("#")) return 0;
            h = h.substring(1);
            if (h.length() == 6) return android.graphics.Color.parseColor("#FF" + h);
            if (h.length() == 8) return android.graphics.Color.parseColor("#" + h);
        } catch (Throwable ignore) {}
        return 0;
    }
}
