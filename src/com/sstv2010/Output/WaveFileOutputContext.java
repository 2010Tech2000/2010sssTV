package com.sstv2010.Output;

import android.content.Context;

import com.sstv2010.AppSettings;
import com.sstv2010.StoragePaths;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WaveFileOutputContext {
    private final String mFileName;
    private final File mFile;

    public WaveFileOutputContext(android.content.ContentResolver contentResolver, String fileName) {
        this((Context) null, fileName);
    }

    public WaveFileOutputContext(Context c, String fileName) {
        mFileName = fileName;
        File dir = null;
        try {
            if (c != null) dir = StoragePaths.resolveDir(AppSettings.txAudioDir(c), StoragePaths.defaultTxAudioDir());
        } catch (Throwable ignore) {}
        if (dir == null) dir = StoragePaths.resolveDir(null, StoragePaths.defaultTxAudioDir());
        mFile = new File(dir, fileName);
    }

    public String getFileName() { return mFileName; }

    public File getFile() { return mFile; }

    public OutputStream getOutputStream() {
        try {
            if (mFile != null && mFile.getParentFile() != null) mFile.getParentFile().mkdirs();
            return new FileOutputStream(mFile);
        } catch (IOException e) {
            return null;
        }
    }

    public void update() { }

    public void deleteFile() {
        try { if (mFile != null) mFile.delete(); } catch (Exception ignore) { }
    }
}
