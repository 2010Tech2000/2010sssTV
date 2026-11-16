package com.sstv2010;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {
    private static final String PREFS = "sstv_prefs";
    public static final String KEY_AUTO_SAVE_RX = "auto_save_rx";
    public static final String KEY_RX_IMAGE_DIR = "rx_image_dir";
    public static final String KEY_TX_AUDIO_DIR = "tx_audio_dir";
    public static final String KEY_GLOBAL_BACKGROUND = "global_background";
    public static final String KEY_CUSTOM_BG = "custom_bg";
    public static final String KEY_MIGRATED = "migrated_v1";

    private AppSettings() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean autoSaveRx(Context c) {
        return prefs(c).getBoolean(KEY_AUTO_SAVE_RX, true);
    }

    public static void setAutoSaveRx(Context c, boolean v) {
        prefs(c).edit().putBoolean(KEY_AUTO_SAVE_RX, v).commit();
    }

    public static String rxImageDir(Context c) {
        String v = prefs(c).getString(KEY_RX_IMAGE_DIR, null);
        return (v == null || v.length() == 0) ? StoragePaths.defaultRxImageDir() : v;
    }

    public static void setRxImageDir(Context c, String v) {
        prefs(c).edit().putString(KEY_RX_IMAGE_DIR, v).commit();
    }

    public static String txAudioDir(Context c) {
        String v = prefs(c).getString(KEY_TX_AUDIO_DIR, null);
        return (v == null || v.length() == 0) ? StoragePaths.defaultTxAudioDir() : v;
    }

    public static void setTxAudioDir(Context c, String v) {
        prefs(c).edit().putString(KEY_TX_AUDIO_DIR, v).commit();
    }

    public static String globalBackground(Context c) {
        String v = prefs(c).getString(KEY_GLOBAL_BACKGROUND, null);
        return v == null ? "" : v;
    }

    public static void setGlobalBackground(Context c, String v) {
        prefs(c).edit().putString(KEY_GLOBAL_BACKGROUND, v == null ? "" : v).commit();
    }

    public static String customBackground(Context c) {
        String v = prefs(c).getString(KEY_CUSTOM_BG, null);
        return v == null ? "" : v;
    }

    public static void setCustomBackground(Context c, String v) {
        prefs(c).edit().putString(KEY_CUSTOM_BG, v == null ? "" : v).commit();
    }

    public static boolean isMigrated(Context c) {
        return prefs(c).getBoolean(KEY_MIGRATED, false);
    }

    public static void setMigrated(Context c, boolean v) {
        prefs(c).edit().putBoolean(KEY_MIGRATED, v).commit();
    }
}
