package com.sstv2010;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.File;

public final class StoragePaths {
    private StoragePaths() {}

    public static File appFilesDir(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= 8) {
                File f = c.getExternalFilesDir(null);
                if (f != null) { ensureDir(f); return f; }
            }
        } catch (Throwable ignore) {}
        try {
            File base = Environment.getExternalStorageDirectory();
            if (base != null) {
                File f = new File(base, "Android/data/" + c.getPackageName() + "/files");
                ensureDir(f);
                return f;
            }
        } catch (Throwable ignore) {}
        try {
            File f = c.getFilesDir();
            if (f != null) ensureDir(f);
            return f;
        } catch (Throwable ignore) {}
        return null;
    }

    public static File appCacheDir(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= 8) {
                File f = c.getExternalCacheDir();
                if (f != null) { ensureDir(f); return f; }
            }
        } catch (Throwable ignore) {}
        try {
            File base = Environment.getExternalStorageDirectory();
            if (base != null) {
                File f = new File(base, "Android/data/" + c.getPackageName() + "/cache");
                ensureDir(f);
                return f;
            }
        } catch (Throwable ignore) {}
        try { return c.getCacheDir(); } catch (Throwable ignore) {}
        return null;
    }

    public static String defaultRxImageDir() {
        File base = Environment.getExternalStorageDirectory();
        if (base == null) return "/sdcard/Pictures/SSTV";
        return new File(new File(base, "Pictures"), "SSTV").getAbsolutePath();
    }

    public static String defaultTxAudioDir() {
        File base = Environment.getExternalStorageDirectory();
        if (base == null) return "/sdcard/Music/SSTV";
        return new File(new File(base, "Music"), "SSTV").getAbsolutePath();
    }

    public static File resolveDir(String path, String fallback) {
        String p = (path == null || path.trim().length() == 0) ? fallback : path.trim();
        File f = new File(p);
        if (!ensureDir(f)) {
            f = new File(fallback);
            ensureDir(f);
        }
        return f;
    }

    public static boolean ensureDir(File f) {
        if (f == null) return false;
        try {
            if (f.exists()) return f.isDirectory() || f.mkdirs();
            return f.mkdirs() || f.isDirectory();
        } catch (Throwable t) { return false; }
    }

    public static void scanFile(Context c, File f, String mime) {
        if (c == null || f == null) return;
        try {
            MediaScannerConnection.scanFile(c.getApplicationContext(),
                    new String[]{ f.getAbsolutePath() },
                    (mime == null ? null : new String[]{ mime }),
                    null);
        } catch (Throwable t) {
            try {
                Uri u = Uri.fromFile(f);
                Intent i = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, u);
                c.sendBroadcast(i);
            } catch (Throwable ignore) {}
        }
    }
}
