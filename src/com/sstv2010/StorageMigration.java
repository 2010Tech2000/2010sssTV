package com.sstv2010;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class StorageMigration {
    private StorageMigration() {}

    public static void migrateIfNeeded(final Context c) {
        if (c == null) return;
        try {
            if (AppSettings.isMigrated(c)) return;
            final Context app = c.getApplicationContext();
            new Thread(new Runnable() {
                public void run() {
                    try { doMigrate(app); } catch (Throwable ignore) {}
                    try { AppSettings.setMigrated(app, true); } catch (Throwable ignore) {}
                }
            }, "sstv-migrate").start();
        } catch (Throwable ignore) {}
    }

    private static void doMigrate(Context c) {
        File base = Environment.getExternalStorageDirectory();
        if (base == null) return;
        File legacy = new File(base, "SSTV");
        if (!legacy.exists() || !legacy.isDirectory()) return;
        File[] files = legacy.listFiles();
        if (files == null) return;
        File rxDir = StoragePaths.resolveDir(AppSettings.rxImageDir(c), StoragePaths.defaultRxImageDir());
        File txDir = StoragePaths.resolveDir(AppSettings.txAudioDir(c), StoragePaths.defaultTxAudioDir());
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f == null || !f.isFile()) continue;
            String name = f.getName();
            String lower = name.toLowerCase();
            File destDir = null;
            String mime = null;
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".bmp")) {
                destDir = rxDir; mime = "image/*";
            } else if (lower.endsWith(".wav")) {
                destDir = txDir; mime = "audio/wav";
            }
            if (destDir == null) continue;
            File dest = uniqueDest(destDir, name);
            if (copyFile(f, dest)) {
                try { StoragePaths.scanFile(c, dest, mime); } catch (Throwable ignore) {}
            }
        }
    }

    private static File uniqueDest(File dir, String name) {
        File dest = new File(dir, name);
        if (!dest.exists()) return dest;
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) { base = name.substring(0, dot); ext = name.substring(dot); }
        for (int i = 1; i < 1000; i++) {
            File cand = new File(dir, base + "_migrated" + i + ext);
            if (!cand.exists()) return cand;
        }
        return new File(dir, base + "_" + System.currentTimeMillis() + ext);
    }

    private static boolean copyFile(File src, File dst) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            return true;
        } catch (Throwable t) {
            try { if (dst.exists()) dst.delete(); } catch (Throwable ignore) {}
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignore) {}
            try { if (out != null) out.close(); } catch (Throwable ignore) {}
        }
    }
}
