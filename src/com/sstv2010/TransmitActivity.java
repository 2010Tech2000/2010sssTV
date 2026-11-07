package com.sstv2010;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

import com.sstv2010.ModeInterfaces.IMode;
import com.sstv2010.ModeInterfaces.IModeInfo;
import com.sstv2010.Modes.ModeFactory;
import com.sstv2010.Output.AudioOutput;
import com.sstv2010.Output.IOutput;
import com.sstv2010.Output.WaveFileOutput;
import com.sstv2010.Output.WaveFileOutputContext;

public class TransmitActivity extends Activity {
    private static final int REQ_PICK = 101;
    private static final int REQ_EDIT = 102;
    private static final double SAMPLE_RATE = 44100.0;
    private static final long TICK_MS = 50;

    private ImageView mImage;
    private View mUnsent, mImageContainer;
    private Button mInsert, mEdit, mReplace, mPlay, mSaveWav, mStopBtn;
    private View mIdleActions;
    private Spinner mMode;
    private TextView mStatus, mProgressText;
    private ProgressBar mProgress;
    private IModeInfo[] mModes;
    private Bitmap mSource, mBitmap;
    private final Handler mHandler = new Handler();
    private volatile boolean mStop, mBusy, mPaused, mSavingWav;
    private volatile long mSamplesWritten, mTotalSamples;
    private volatile int mCurrentLine, mTotalLines;
    private volatile IMode mCurrentMode;
    private Runnable mTicker;
    private AudioOutput mAudioOut;
    private volatile boolean mAudioMode;
    private Thread mTxThread;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.transmit);
            mImage = (ImageView) findViewById(R.id.tx_image);
            mUnsent = findViewById(R.id.tx_unsent_mask);
            mImageContainer = findViewById(R.id.tx_image_container);
            mInsert = (Button) findViewById(R.id.btn_insert);
            mEdit = (Button) findViewById(R.id.btn_edit);
            mReplace = (Button) findViewById(R.id.btn_replace);
            mPlay = (Button) findViewById(R.id.btn_play);
            mSaveWav = (Button) findViewById(R.id.btn_save_wav);
            mStopBtn = (Button) findViewById(R.id.btn_stop);
            mIdleActions = findViewById(R.id.tx_actions_idle);
            mMode = (Spinner) findViewById(R.id.tx_mode);
            mStatus = (TextView) findViewById(R.id.tx_status);
            mProgressText = (TextView) findViewById(R.id.tx_progress_text);
            mProgress = (ProgressBar) findViewById(R.id.tx_progress);
            if (mProgress != null) { mProgress.setMax(1000); mProgress.setProgress(0); }
            if (mProgressText != null) mProgressText.setText("0%");

            mModes = ModeFactory.getModeInfoList();
            String[] names = new String[mModes.length];
            int def = 0;
            for (int i = 0; i < mModes.length; i++) {
                names[i] = String.valueOf(mModes[i].getModeName());
                if (names[i].indexOf("Robot 36") >= 0 || "Robot36".equals(names[i])) def = i;
            }
            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mMode.setAdapter(adapter);
            mMode.setSelection(def);
            mMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onItemSelected(AdapterView p, View v, int pos, long id){ rescaleForMode(); resetProgressUi(); }
                public void onNothingSelected(AdapterView p){}
            });

            View.OnClickListener pick = new View.OnClickListener(){ public void onClick(View v){ if (!mBusy) pick(); } };
            mInsert.setOnClickListener(pick);
            mReplace.setOnClickListener(pick);
            mEdit.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ openEditor(); } });
            mPlay.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if (!mBusy) startTx(false); } });
            mSaveWav.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ if (!mBusy) startTx(true); } });
            mStopBtn.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ stopTx(); } });
            refreshButtons();
            setStatus("Insert an image to begin.");
        } catch (Throwable e) {
            android.util.Log.e("SSTVAndroid2", "TX onCreate", e);
            TextView t = new TextView(this); t.setText("TX init error: " + e); setContentView(t);
        }
    }

    private void pick() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "Pick image"), REQ_PICK);
        } catch (Throwable t){ toast("No image picker"); }
    }

    private void openEditor() {
        if (mSource == null) { toast("Pick an image first"); return; }
        if (mBusy) { toast("Stop playback first"); return; }
        int[] wh = modeWH();
        TxImageStore.put(mSource);
        Intent i = new Intent(this, EditImageActivity.class);
        i.putExtra("mode_w", wh[0]); i.putExtra("mode_h", wh[1]);
        startActivityForResult(i, REQ_EDIT);
    }

    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_PICK && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                InputStream in = getContentResolver().openInputStream(uri);
                Bitmap raw = BitmapFactory.decodeStream(in);
                if (in != null) in.close();
                if (raw != null) {
                    TxImageStore.clearEdit(); // new image => drop old editable state
                    mSource = raw; rescaleForMode(); refreshButtons(); resetProgressUi();
                }
                else toast("Image load failed");
            } catch (Exception e) { toast("Image load failed"); }
        } else if (req == REQ_EDIT && res == RESULT_OK) {
            Bitmap b = TxImageStore.get();
            if (b != null) { mSource = b; rescaleForMode(); refreshButtons(); resetProgressUi(); setStatus("Image edited. Ready."); }
        }
    }

    private Class modeClassAt(int idx) {
        try { return Class.forName(mModes[idx].getModeClassName()); } catch (Exception e) { return null; }
    }

    private int[] modeWH() {
        int idx = (mMode!=null)?mMode.getSelectedItemPosition():0; if (idx<0) idx=0;
        Class cls = modeClassAt(idx);
        com.sstv2010.ModeInterfaces.ModeSize sz = cls==null?null:(com.sstv2010.ModeInterfaces.ModeSize) cls.getAnnotation(com.sstv2010.ModeInterfaces.ModeSize.class);
        return new int[]{ sz!=null?sz.width():320, sz!=null?sz.height():256 };
    }

    private void rescaleForMode() {
        // Encoder needs exact mode resolution, but we do NOT stretch/distort the source.
        // Preview shows the source; makeMode fits (letterbox) for transmission.
        if (mSource == null || mMode == null || mModes == null) return;
        int[] wh = modeWH();
        try {
            mBitmap = mSource; // keep original aspect; fit happens in makeMode
            if (mImage != null) mImage.setImageBitmap(mBitmap);
            if (!mBusy) setStatus("Ready (" + wh[0] + "x" + wh[1] + ")");
        } catch(Throwable t){ setStatus("Scale error: "+t); }
    }

    private void refreshButtons() {
        boolean has = (mSource != null);
        if (mInsert != null) mInsert.setVisibility(has ? View.GONE : View.VISIBLE);
        if (mImage != null) mImage.setVisibility(has ? View.VISIBLE : View.INVISIBLE);
        if (mMode != null) mMode.setEnabled(!mBusy);
        if (mIdleActions != null) mIdleActions.setVisibility(mBusy ? View.GONE : View.VISIBLE);
        if (mStopBtn != null) mStopBtn.setVisibility(mBusy ? View.VISIBLE : View.GONE);
        setEnabledSafe(mEdit, has && !mBusy);
        setEnabledSafe(mReplace, has && !mBusy);
        setEnabledSafe(mPlay, has && !mBusy);
        setEnabledSafe(mSaveWav, has && !mBusy);
        if (mStopBtn != null) mStopBtn.setEnabled(mBusy);
    }
    private void setEnabledSafe(Button b, boolean en){ if (b!=null) b.setEnabled(en); }

    private void resetProgressUi() {
        mCurrentLine = 0; mSamplesWritten = 0;
        if (mProgress != null) mProgress.setProgress(0);
        if (mProgressText != null) mProgressText.setText("0%");
        applyMask(0f, false);
    }

    private void applyMask(float frac, boolean active) {
        if (mUnsent == null || mImageContainer == null) return;
        final float f = frac < 0f ? 0f : (frac > 1f ? 1f : frac);
        final boolean show = active && f < 1f;
        mHandler.post(new Runnable(){ public void run(){
            try {
                int ch = mImageContainer.getHeight();
                if (ch <= 0) { mUnsent.setVisibility(show ? View.VISIBLE : View.INVISIBLE); return; }
                int sentPx = Math.round(ch * f);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mUnsent.getLayoutParams();
                lp.gravity = android.view.Gravity.TOP;
                lp.topMargin = sentPx;
                lp.height = Math.max(0, ch - sentPx);
                lp.width = FrameLayout.LayoutParams.FILL_PARENT;
                mUnsent.setLayoutParams(lp);
                mUnsent.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            } catch(Throwable ignore){}
        }});
    }

    private void stopTx() {
        if (!mBusy) return;
        mStop = true;
        try { stopTicker(); } catch (Throwable ignore) { }
        try {
            AudioOutput ao = mAudioOut;
            if (ao != null) ao.finish(true);
        } catch (Throwable ignore) { }
        try { if (mProgress != null) mProgress.setProgress(0); } catch (Throwable ignore) { }
        try { if (mProgressText != null) mProgressText.setText("0%"); } catch (Throwable ignore) { }
        try { applyMask(0f, false); } catch (Throwable ignore) { }
        try { setStatus("Stopped  Elapsed 0:00 | Remaining --:--"); } catch (Throwable ignore) { }
        final Thread t = mTxThread;
        if (t != null && t != Thread.currentThread()) {
            try { t.interrupt(); } catch (Throwable ignore) { }
        }
        // UI reset immediately; worker thread finalizes state if still running
        mBusy = false; mPaused = false; mSavingWav = false; mCurrentMode = null; mAudioOut = null; mAudioMode = false;
        try { refreshButtons(); } catch (Throwable ignore) { }
    }

    private IMode makeMode(IOutput out) {
        if (mBitmap == null) { toast("Pick an image first"); return null; }
        int idx = mMode.getSelectedItemPosition(); if (idx<0) idx=0;
        Class cls = modeClassAt(idx);
        com.sstv2010.ModeInterfaces.ModeSize sz = cls==null?null:(com.sstv2010.ModeInterfaces.ModeSize) cls.getAnnotation(com.sstv2010.ModeInterfaces.ModeSize.class);
        Bitmap bmp = mBitmap;
        if (sz != null && (bmp.getWidth() != sz.width() || bmp.getHeight() != sz.height())) {
            // Fit (preserve aspect) into mode resolution, centered on black — no stretching.
            int tw = sz.width(), th = sz.height();
            float scale = Math.min(((float)tw)/bmp.getWidth(), ((float)th)/bmp.getHeight());
            int dw = Math.max(1, Math.round(bmp.getWidth() * scale));
            int dh = Math.max(1, Math.round(bmp.getHeight() * scale));
            Bitmap outBmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(outBmp);
            c.drawColor(android.graphics.Color.BLACK);
            c.drawBitmap(bmp, new android.graphics.Rect(0,0,bmp.getWidth(),bmp.getHeight()),
                    new android.graphics.Rect((tw-dw)/2, (th-dh)/2, (tw-dw)/2+dw, (th-dh)/2+dh),
                    new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
            bmp = outBmp;
        }
        return ModeFactory.CreateMode(cls, bmp, out);
    }

    private void startTx(final boolean saveWav) {
        if (mBusy) return;
        if (mBitmap == null && mSource != null) rescaleForMode();
        if (mBitmap == null) { toast("Pick an image first"); return; }
        final IOutput realOut;
        final String label;
        try {
            if (saveWav) {
                WaveFileOutputContext ctx = new WaveFileOutputContext(getContentResolver(), "sstv_" + System.currentTimeMillis() + ".wav");
                realOut = new WaveFileOutput(ctx, SAMPLE_RATE);
                mAudioOut = null; mAudioMode = false;
                label = "Saving WAV: " + ctx.getFileName();
            } else {
                AudioOutput ao = new AudioOutput(SAMPLE_RATE);
                realOut = ao; mAudioOut = ao; mAudioMode = true;
                label = "Transmitting";
            }
        } catch (Throwable t){ setStatus("Output error: "+t); return; }

        IOutput wrapped = new IOutput() {
            public double getSampleRate(){ return realOut.getSampleRate(); }
            public void init(int samples){ realOut.init(samples); }
            public void write(double value){
                if (mStop) return;
                realOut.write(value);
                mSamplesWritten++;
            }
            public void finish(boolean cancel){ realOut.finish(cancel); }
        };
        final IMode mode = makeMode(wrapped);
        if (mode == null) { mAudioOut = null; mAudioMode = false; return; }
        runMode(mode, label, saveWav);
    }

    private long playedSamples() {
        try {
            AudioOutput ao = mAudioOut;
            if (mAudioMode && ao != null) return ao.getPlaybackHeadPosition();
        } catch (Throwable ignore) { }
        return mSamplesWritten;
    }

    private void startTicker(final IMode mode, final String label) {
        stopTicker();
        mTicker = new Runnable(){ public void run(){
            if (!mBusy) return;
            try {
                int line = mCurrentLine; try { line = mode.getCurrentLine(); } catch(Throwable ignore){}
                mCurrentLine = line;
                long total = mTotalSamples; if (total <= 0) try { total = mode.getTotalSamples(); mTotalSamples = total; } catch(Throwable ignore){}
                long done = Math.min(playedSamples(), mSamplesWritten);
                float frac = 0f;
                if (total > 0) frac = ((float)Math.min(total, done)) / ((float)total);
                else if (mTotalLines > 0) frac = ((float)Math.max(0, Math.min(line, mTotalLines))) / ((float)mTotalLines);
                int p10 = Math.max(0, Math.min(1000, Math.round(frac * 1000f)));
                final int pct = Math.max(0, Math.min(100, Math.round(frac * 100f)));
                if (mProgress != null) mProgress.setProgress(p10);
                if (mProgressText != null) mProgressText.setText(pct + "%");
                applyMask(frac, true);
                setStatus(formatStatus(label, frac, total, done));
            } catch(Throwable ignore){}
            if (mBusy) mHandler.postDelayed(this, TICK_MS);
        }};
        mHandler.post(mTicker);
    }
    private void stopTicker(){ if (mTicker!=null){ try{ mHandler.removeCallbacks(mTicker);}catch(Throwable ignore){} mTicker=null; } }

    private void runMode(final IMode mode, final String label, final boolean saveWav) {
        mStop = false; mBusy = true; mPaused = false; mSavingWav = saveWav;
        mSamplesWritten = 0; mTotalSamples = 0; mCurrentLine = 0; mTotalLines = mBitmap!=null?mBitmap.getHeight():0;
        mCurrentMode = mode;
        refreshButtons();
        if (mProgress != null) mProgress.setProgress(0);
        if (mProgressText != null) mProgressText.setText("0%");
        setStatus(saveWav ? "Preparing WAV..." : "Starting transmission...");
        applyMask(0f, true);
        startTicker(mode, label);

        mTxThread = new Thread(new Runnable() {
            public void run() {
                String err = null; boolean stopped = false;
                try {
                    mode.init();
                    try { mTotalSamples = mode.getTotalSamples(); } catch(Throwable ignore){}
                    while (!mStop) {
                        if (!mode.process()) break;
                        try { mCurrentLine = mode.getCurrentLine(); } catch(Throwable ignore){}
                    }
                    stopped = mStop;
                    mode.finish(stopped);
                    if (!stopped && mAudioMode) {
                        long target = mTotalSamples > 0 ? mTotalSamples : mSamplesWritten;
                        long start = System.currentTimeMillis();
                        while (!mStop && System.currentTimeMillis() - start < 12000) {
                            long p = playedSamples();
                            if (p >= target - 200) break;
                            try { Thread.sleep(20); } catch(Exception ignore){}
                        }
                        if (mStop) {
                            try { AudioOutput ao = mAudioOut; if (ao!=null) ao.finish(true); } catch(Throwable ignore){}
                        }
                    }
                } catch (final Throwable e) { err = String.valueOf(e); }
                final String ferr = err; final boolean fstop = stopped;
                mHandler.post(new Runnable(){ public void run(){
                    try{
                        stopTicker();
                        mBusy=false; mPaused=false; mCurrentMode=null; mSavingWav=false;
                        if (mStop) {
                            // already reset by stopTx; avoid overwriting with stale/delayed values
                        } else {
                            long total = mTotalSamples>0?mTotalSamples:mSamplesWritten;
                            if (ferr==null && !fstop) {
                                mSamplesWritten = total; mCurrentLine = mTotalLines;
                                if (mProgress!=null) mProgress.setProgress(1000);
                                if (mProgressText!=null) mProgressText.setText("100%");
                                applyMask(1f, false);
                                setStatus("Done  " + fmtSec(((double)total)/SAMPLE_RATE));
                            } else if (fstop) {
                                if (mProgress!=null) mProgress.setProgress(0);
                                if (mProgressText!=null) mProgressText.setText("0%");
                                applyMask(0f, false);
                                setStatus("Stopped  Elapsed 0:00 | Remaining --:--");
                            } else {
                                applyMask(0f, false);
                                setStatus("Error: " + ferr);
                            }
                        }
                        mAudioOut = null; mAudioMode = false; mTxThread = null;
                        refreshButtons();
                    }catch(Throwable ignore){}
                }});
            }
        }, "SSTV-TX");
        mTxThread.start();
    }

    private float progressFrac(long done, long total) {
        if (total > 0) return ((float)Math.min(total,Math.max(0,done)))/((float)total);
        if (mTotalLines > 0) return ((float)Math.max(0,Math.min(mCurrentLine,mTotalLines)))/((float)mTotalLines);
        return 0f;
    }

    private String formatStatus(String label, float frac, long totalSamples, long doneSamples) {
        long total = totalSamples>0?totalSamples:mSamplesWritten;
        long done = Math.min(total>0?total:doneSamples, Math.max(0, doneSamples));
        long rem = Math.max(0, total - done);
        return label + "  Elapsed " + fmtSec(((double)done)/SAMPLE_RATE) + " | Left " + fmtSec(((double)rem)/SAMPLE_RATE) + " / " + fmtSec(((double)total)/SAMPLE_RATE);
    }
    private String fmtSec(double s) {
        int sec = (int)Math.round(s);
        int m = sec/60, r = sec%60;
        return m + ":" + (r<10?"0":"") + r;
    }
    private String modeName(){ try{ int i=mMode.getSelectedItemPosition(); return (i>=0&&i<mModes.length)?String.valueOf(mModes[i].getModeName()):""; }catch(Throwable t){ return ""; } }

    private void setStatus(final String s){ if (mStatus!=null) mStatus.setText(s); }
    private void toast(final String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    protected void onDestroy(){ mStop = true; stopTicker(); super.onDestroy(); }
}