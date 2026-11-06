package om.sstvandroid2;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import java.io.InputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public class ReceiveActivity extends Activity implements AudioSource.AudioDataListener {
    private static final int RATE = 16000;
    private TextView mStatus, mMode;
    private Button mToggle;
    private ImageView mScopeView, mImageView;
    private final Handler mHandler = new Handler();
    private AudioSource mSource;
    private volatile boolean mRunning;
    private volatile int mWavCh = 0;

    private final Object mLock = new Object();
    private Decoder mDecoder;
    private PixelBuffer mScope, mImage;
    private Bitmap mScopeBmp, mImageBmp;
    private String mModeName = "";
    private long mFrames, mImages;
    private boolean mNewImage, mNewScope;
    private volatile boolean mUiPosted;
    private volatile long mLastUiMs, mLastScopeMs;
    private static final long UI_MIN_MS = 200;
    private static final long SCOPE_MIN_MS = 1000;
    private int mLastScopeLine = -1, mLastImageLine = -1, mLastImageW = -1, mLastImageH = -1;
    private int[] mScopeScratch;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receive);
        mStatus = (TextView) findViewById(R.id.rx_status);
        mMode = (TextView) findViewById(R.id.rx_mode);
        mScopeView = (ImageView) findViewById(R.id.rx_scope);
        mImageView = (ImageView) findViewById(R.id.rx_image);
        mToggle = (Button) findViewById(R.id.btn_toggle_rx);
        mToggle.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            if (mRunning) stopRx(); else startRx();
        }});
        View save = findViewById(getResources().getIdentifier("btn_save_rx","id",getPackageName())); if (save!=null) save.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ saveImage(); }});
        View wav = findViewById(getResources().getIdentifier("btn_wav_rx","id",getPackageName())); if (wav!=null) wav.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ pickWav(); }});
    }

    private void ensureDecoder(int sampleRate) {
        if (mDecoder != null && sampleRate == RATE) return;
        mDecoder = null;
        mScope = new PixelBuffer(640, 2 * 640); java.util.Arrays.fill(mScope.pixels, 0xff000000);
        mImage = new PixelBuffer(800, 616);
        Arrays.fill(mImage.pixels, 0xff101010);
        mDecoder = new Decoder(mScope, mImage, "Raw", sampleRate);
    }

    private void resetImageState() {
        if (mImage != null) Arrays.fill(mImage.pixels, 0xff101010);
        mLastImageLine = -1; mLastImageW = -1; mLastImageH = -1;
        mImageBmp = null;
        mNewImage = true;
    }

    private void startRx() {
        if (mRunning) return;
        try { ensureDecoder(RATE); } catch(Throwable t){ mStatus.setText("Decoder init: "+t); return; }
        resetImageState();
        mWavCh = 0; mRunning = true;
        mFrames = 0; mImages = 0; mLastScopeLine = -1; mModeName = "";
        mStatus.setText("Starting..."); if (mToggle!=null) mToggle.setText("Pause");

        try {
            mSource = new MicrophoneAudioSource(RATE, 1, MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT);
            mSource.setAudioDataListener(this);
            mSource.prepare();
            mSource.start();
        } catch(final Throwable t) {
            mRunning=false; mSource = null;
            mStatus.setText("Start error: "+t);

        }
    }

    private void stopRx() {
        mWavCh = 0; mRunning = false;
        AudioSource s = mSource; mSource = null;
        if (s != null) { try { s.stop(); s.release(); } catch (Exception ignore) {} }
        mStatus.setText(getString(R.string.status_idle)); if (mToggle!=null) mToggle.setText("Start");
    }

    private void pickWav() {
        stopRx();
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.setType("audio/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(i, "Open WAV"), 901);
        } catch(Throwable t){ Toast.makeText(this, "No file picker: "+t, Toast.LENGTH_LONG).show(); }
    }

    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req,res,data);
        if (req!=901 || res!=RESULT_OK || data==null || data.getData()==null) return;
        final Uri uri = data.getData();
        mRunning = true; mFrames = 0; mImages = 0; mLastScopeLine = -1; mModeName = "";
        mStatus.setText("Opening WAV..."); if (mToggle!=null) mToggle.setText("Pause");
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            final PcmFileReader reader = WavFileReader.open(in);
            int wavRate = reader.getFormat().getSampleRate();
            try { ensureDecoder(wavRate); resetImageState(); } catch(Throwable t){ try{reader.close();}catch(Exception ignore){} mStatus.setText("Decoder init: "+t); mRunning=false; return; }
            mWavCh = Math.max(1, reader.getFormat().getChannels());
            FileAudioSource src = new FileAudioSource(new FileAudioSource.ReaderFactory(){ public PcmFileReader open(){ return reader; } }, true);
            mSource = src;
            src.setAudioDataListener(this);
            src.prepare();
            src.start();

        } catch(final Throwable t) {
            mRunning=false; mSource=null;
            mStatus.setText("WAV error: "+t); if (mToggle!=null) mToggle.setText("Start");

        }
    }

    public void onAudioDataAvailable(float[] pcm, int frameCount, int sampleRate, int channels) {
        if (!mRunning || mDecoder == null) return;
        long now = android.os.SystemClock.uptimeMillis();
        synchronized (mLock) {
            boolean lines = mDecoder.process(pcm, channels == 1 ? 0 : 3);
            mFrames += frameCount;
            if (mScope != null && mScope.line != mLastScopeLine && (now - mLastScopeMs) >= SCOPE_MIN_MS) {
                mLastScopeLine = mScope.line; mNewScope = true;
            }
            Mode cm = mDecoder.currentMode;
            String mn = cm != null ? cm.getName() : "";
            if (mn.length() > 0 && !mn.equals(mModeName)) { mModeName = mn; mNewImage = true; }
            if (mImage != null && (mImage.width != mLastImageW || mImage.height != mLastImageH)) {
                mLastImageW = mImage.width; mLastImageH = mImage.height; mImageBmp = null; mNewImage = true;
            }
            if (lines && mImage != null) {
                if (mImage.line >= mImage.height) { mImage.line = -1; mNewImage = true; mImages++; }
                else if (mImage.line != mLastImageLine) { mLastImageLine = mImage.line; mNewImage = true; }
            }
        }
        if ((mNewImage || mNewScope) && (now - mLastUiMs) >= UI_MIN_MS) postUi();
    }

    public void onStatusChanged(final AudioSource.AudioSourceState state, final String message) {
        if (state == AudioSource.AudioSourceState.COMPLETED || state == AudioSource.AudioSourceState.STOPPED) {
            int ch = mWavCh; mWavCh = 0;
            if (ch > 0) {
                try { synchronized (mLock){ if (mDecoder!=null) mDecoder.finish(ch==1?0:3); mNewImage=true; mNewScope=true; } postUi(); } catch(Throwable ignore){}
            }
        }
        mHandler.post(new Runnable(){ public void run(){
            if (state == AudioSource.AudioSourceState.RUNNING) { mStatus.setText("Listening"); if (mToggle!=null) mToggle.setText("Pause"); }
            else if (state == AudioSource.AudioSourceState.ERROR) { mStatus.setText("Error: " + String.valueOf(message)); if (mToggle!=null) mToggle.setText("Start"); }
            else if (state == AudioSource.AudioSourceState.COMPLETED) { mStatus.setText("Completed"); if (mToggle!=null) mToggle.setText("Start"); }
            else if (state == AudioSource.AudioSourceState.STOPPED) { if (!mRunning) { mStatus.setText(getString(R.string.status_idle)); if (mToggle!=null) mToggle.setText("Start"); } }
        }});
    }

    private void postUi() {
        if (mUiPosted) return; mUiPosted = true;
        mHandler.post(new Runnable(){ public void run(){
            mUiPosted = false;
            Bitmap sb=null, ib=null; String mode=""; long frames=0, images=0; int iw=0, ih=0;
            synchronized (mLock) {
                mode = mModeName; frames = mFrames; images = mImages; iw = mImage!=null?mImage.width:0; ih = mImage!=null?mImage.height:0;
                if (mNewScope && mScope != null) {
                    mNewScope = false;
                    int w = mScope.width, h = mScope.height/2;
                    if (w > 0 && h > 0) {
                        if (mScopeBmp == null || mScopeBmp.getWidth()!=w || mScopeBmp.getHeight()!=h) mScopeBmp = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                        int off = w * (mScope.line % h);
                        if (off < 0) off = 0;
                        if (off + w*h <= mScope.pixels.length) {
                            mScopeBmp.setPixels(mScope.pixels, off, w, 0, 0, w, h); sb = mScopeBmp;
                        } else {
                            int len = w*h;
                            if (mScopeScratch == null || mScopeScratch.length != len) mScopeScratch = new int[len];
                            int first = Math.min(len, Math.max(0, mScope.pixels.length - off));
                            if (first > 0) System.arraycopy(mScope.pixels, off, mScopeScratch, 0, first);
                            if (first < len) Arrays.fill(mScopeScratch, first, len, 0xff000000);
                            mScopeBmp.setPixels(mScopeScratch, 0, w, 0, 0, w, h); sb = mScopeBmp;
                        }
                    }
                }
                if (mNewImage && mImage != null && iw > 0 && ih > 0 && iw*ih <= mImage.pixels.length) {
                    mNewImage = false;
                    if (mImageBmp == null || mImageBmp.getWidth()!=iw || mImageBmp.getHeight()!=ih) mImageBmp = Bitmap.createBitmap(iw,ih,Bitmap.Config.ARGB_8888);
                    mImageBmp.setPixels(mImage.pixels, 0, iw, 0, 0, iw, ih); ib = mImageBmp;
                }
            }
            if (mode != null && mode.length() > 0) mMode.setText("Mode: " + mode + (iw>0?" "+iw+"x"+ih:"") + "  img:" + images + "  sync:" + Decoder.dbgSyncPulses + " hdr:" + Decoder.dbgHeaders + " fb:" + Decoder.dbgFallback + " nl:" + Decoder.dbgNewLines);
            if (sb != null) mScopeView.setImageBitmap(sb);
            if (ib != null) mImageView.setImageBitmap(ib);
        }});
    }

    private void saveImage() {
        Bitmap bmp = null;
        synchronized (mLock) {
            if (mImage != null && mImage.width > 0 && mImage.height > 0 && mImage.width*mImage.height <= mImage.pixels.length)
                bmp = Bitmap.createBitmap(mImage.pixels, 0, mImage.width, mImage.width, mImage.height, Bitmap.Config.ARGB_8888);
        }
        if (bmp == null) { Toast.makeText(this, "No image", Toast.LENGTH_SHORT).show(); return; }
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "SSTV");
            if (!dir.exists()) dir.mkdirs();
            String name = "sstv_rx_" + System.currentTimeMillis() + ".png";
            File f = new File(dir, name);
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); out.flush(); out.close();
            Toast.makeText(this, "Saved: " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Toast.makeText(this, "Save failed: " + t, Toast.LENGTH_LONG).show();
        }
    }

    protected void onResume(){ super.onResume(); if (!mRunning && mToggle!=null) { mHandler.post(new Runnable(){ public void run(){ if (!mRunning) startRx(); }}); } }
    protected void onPause(){ stopRx(); super.onPause(); }
}
