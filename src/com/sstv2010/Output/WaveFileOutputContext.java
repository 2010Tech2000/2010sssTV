package com.sstv2010.Output;

import android.content.ContentResolver;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WaveFileOutputContext {
    private final String mFileName;
    private final File mFile;

    public WaveFileOutputContext(ContentResolver contentResolver, String fileName) {
        mFileName = fileName;
        File dir = new File(Environment.getExternalStorageDirectory(), "SSTV");
        dir.mkdirs();
        mFile = new File(dir, fileName);
    }

    public String getFileName() { return mFileName; }

    public OutputStream getOutputStream() {
        try {
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
