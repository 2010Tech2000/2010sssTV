package com.sstv2010;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class EditImageActivity extends Activity {
    private static final int DEF_SIZE = 36;
    private static final int DEF_STROKE = 2;
    private static final int DEF_COLOR = 0; // White
    private static final int DEF_FONT = 0;  // Default

    private EditorCanvasView mCanvas;
    private View mPanelText, mPanelCrop;
    private EditText mTextValue;
    private SeekBar mSize, mStroke;
    private Spinner mColor, mFont;
    private TextView mSizeLabel, mStrokeLabel;
    private CheckBox mStretch;
    private int mTargetW, mTargetH;
    private boolean mSyncing;
    private static final String[] COLORS = new String[]{"White","Black","Red","Green","Blue","Yellow","Cyan","Magenta"};
    private static final int[] COLOR_VALS = new int[]{Color.WHITE,Color.BLACK,Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW,Color.CYAN,Color.MAGENTA};
    private static final String[] FONTS = new String[]{"Default","Sans","Serif","Mono","Bold"};
    private Typeface[] mFaces;

    private Bitmap mOriginal;
    private java.util.ArrayList mLoadedItems;
    private boolean mFresh;
    private static final int MAX_EDIT_PX = 1280; // cap to avoid OOM on S3 mini while editing

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.edit_image);
            mTargetW = getIntent().getIntExtra("mode_w", 320);
            mTargetH = getIntent().getIntExtra("mode_h", 256);
            mCanvas = (EditorCanvasView) findViewById(R.id.ed_canvas);
            mPanelText = findViewById(R.id.panel_text);
            mPanelCrop = findViewById(R.id.panel_crop);
            mTextValue = (EditText) findViewById(R.id.ed_text_value);
            mSize = (SeekBar) findViewById(R.id.ed_text_size);
            mStroke = (SeekBar) findViewById(R.id.ed_text_stroke);
            mColor = (Spinner) findViewById(R.id.ed_text_color);
            mFont = (Spinner) findViewById(R.id.ed_text_font);
            mSizeLabel = (TextView) findViewById(R.id.ed_size_label);
            mStrokeLabel = (TextView) findViewById(R.id.ed_stroke_label);
            mStretch = (CheckBox) findViewById(R.id.ed_stretch_mode);
            Bitmap b = null;
            java.util.ArrayList items = null;
            boolean stretch = false;
            Bitmap orig = null;
            if (TxImageStore.hasEdit()) {
                orig = TxImageStore.getEditOriginal();
                b = TxImageStore.getEditBase();
                items = TxImageStore.getEditItems();
                stretch = TxImageStore.getEditStretch();
                mFresh = false;
            } else {
                b = TxImageStore.get();
                mFresh = true;
            }
            if (b == null) b = orig;
            if (b == null) { Toast.makeText(this, "No image", Toast.LENGTH_SHORT).show(); finish(); return; }
            Bitmap origSrc = orig != null ? orig : b;
            mOriginal = limit(origSrc);
            mLoadedItems = (items != null) ? copyItemsList(items) : new java.util.ArrayList();
            // scale text positions/sizes if we downscaled relative to the base we loaded
            float sx = 1f, sy = 1f;
            if (b.getWidth() > 0 && b.getHeight() > 0) { Bitmap lb = limit(b); sx = ((float)lb.getWidth())/b.getWidth(); sy = ((float)lb.getHeight())/b.getHeight(); }
            if (Math.abs(sx-1f)>0.001f || Math.abs(sy-1f)>0.001f) scaleItems(mLoadedItems, sx, sy);
            Bitmap base = limit(b);
            mCanvas.setBase(base);
            if (!mLoadedItems.isEmpty()) mCanvas.setItems(mLoadedItems);
            if (mStretch != null) mStretch.setChecked(stretch);
            mCanvas.saveSnapshot();
            mCanvas.setPreviewMode(mTargetW, mTargetH, stretch);

            mColor.setAdapter(simple(COLORS));
            mFaces = new Typeface[]{Typeface.DEFAULT, Typeface.SANS_SERIF, Typeface.SERIF, Typeface.MONOSPACE, Typeface.DEFAULT_BOLD};
            mFont.setAdapter(simple(FONTS));

            mCanvas.setOnSelectionChanged(new EditorCanvasView.OnSelectionChanged(){
                public void onSelectionChanged(EditorCanvasView.TextItem item){ syncFromSelection(item); }
            });

            bind(R.id.ed_mode_text, new View.OnClickListener(){ public void onClick(View v){ hideIme(); showText(); }});
            bind(R.id.ed_mode_crop, new View.OnClickListener(){ public void onClick(View v){ hideIme(); showCrop(); }});
            bind(R.id.ed_done, new View.OnClickListener(){ public void onClick(View v){ hideIme(); done(); }});
            bind(R.id.ed_cancel, new View.OnClickListener(){ public void onClick(View v){ setResult(RESULT_CANCELED); finish(); }});
            bind(R.id.ed_add_text, new View.OnClickListener(){ public void onClick(View v){ addNewText(); }});
            bind(R.id.ed_del_text, new View.OnClickListener(){ public void onClick(View v){ hideIme(); if (!mCanvas.removeActiveItem()) setDefaults(true); }});
            bind(R.id.ed_apply_crop, new View.OnClickListener(){ public void onClick(View v){ hideIme(); try{ mCanvas.applyCrop(); } catch(Throwable t){ Toast.makeText(EditImageActivity.this,"Apply failed: "+t,Toast.LENGTH_SHORT).show(); } }});
            bind(R.id.ed_reset_crop, new View.OnClickListener(){ public void onClick(View v){ hideIme(); resetImageKeepText(); }});
            bind(R.id.ed_cancel_crop, new View.OnClickListener(){ public void onClick(View v){ hideIme(); cancelCropEdits(); }});
            bind(R.id.ed_rot90, new View.OnClickListener(){ public void onClick(View v){ mCanvas.rotate90(); }});
bind(R.id.ed_rot270, new View.OnClickListener(){ public void onClick(View v){ mCanvas.rotate270(); }});
bind(R.id.ed_flip_h, new View.OnClickListener(){ public void onClick(View v){ mCanvas.flipH(); }});
bind(R.id.ed_flip_v, new View.OnClickListener(){ public void onClick(View v){ mCanvas.flipV(); }});


            if (mStretch != null) mStretch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener(){
                public void onCheckedChanged(android.widget.CompoundButton b, boolean checked){
                    mCanvas.setPreviewMode(mTargetW, mTargetH, checked);
                    mCanvas.invalidate();
                }
            });

            mTextValue.addTextChangedListener(new TextWatcher(){
                public void beforeTextChanged(CharSequence s,int a,int b,int c){}
                public void onTextChanged(CharSequence s,int a,int b,int c){}
                public void afterTextChanged(Editable s){
                    if (mSyncing) return;
                    String txt = String.valueOf(s);
                    EditorCanvasView.TextItem it = mCanvas.getActiveItem();
                    if (it != null) {
                        it.text = txt;
                        mCanvas.invalidate();
                    } else if (txt != null && txt.length() > 0) {
                        addTextWith(txt);
                    }
                }
            });
            SeekBar.OnSeekBarChangeListener sl = new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar sb,int p,boolean u){ updateLabels(); if (!mSyncing) applyToActive(); }
                public void onStartTrackingTouch(SeekBar sb){}
                public void onStopTrackingTouch(SeekBar sb){}
            };
            mSize.setOnSeekBarChangeListener(sl); mStroke.setOnSeekBarChangeListener(sl);
            android.widget.AdapterView.OnItemSelectedListener isl = new android.widget.AdapterView.OnItemSelectedListener(){
                public void onItemSelected(android.widget.AdapterView p,View v,int pos,long id){ if (!mSyncing) applyToActive(); }
                public void onNothingSelected(android.widget.AdapterView p){}
            };
            mColor.setOnItemSelectedListener(isl); mFont.setOnItemSelectedListener(isl);

            mCanvas.setPreviewMode(mTargetW, mTargetH, mStretch!=null && mStretch.isChecked());
            // keep existing text/controls when resuming a saved edit; default only for fresh open
            if (mFresh) setDefaults(true);
            else { mCanvas.clearActive(); updateLabels(); }
            showText();
            hideIme();
        } catch (Throwable t) {
            Toast.makeText(this, "Editor error: "+t, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private ArrayAdapter simple(String[] a){ ArrayAdapter ad=new ArrayAdapter(this, android.R.layout.simple_spinner_item, a); ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); return ad; }

    private void bind(int id, View.OnClickListener l){ View v=findViewById(id); if (v!=null) v.setOnClickListener(l); }
    private Bitmap copy(Bitmap b){ try { return b.copy(b.getConfig()!=null?b.getConfig():Bitmap.Config.ARGB_8888, true);} catch(Throwable t){ return Bitmap.createBitmap(b); } }

    private void showText(){
        hideIme();
        if (mPanelCrop != null && mPanelCrop.getVisibility() == View.VISIBLE) mCanvas.saveSnapshot();
        mCanvas.setMode(EditorCanvasView.MODE_TEXT);
        mPanelText.setVisibility(View.VISIBLE); mPanelCrop.setVisibility(View.GONE);
    }
    private void showCrop(){
        hideIme();
        mCanvas.saveSnapshot(); // baseline for Cancel while in crop mode
        mCanvas.setMode(EditorCanvasView.MODE_CROP);
        mPanelText.setVisibility(View.GONE); mPanelCrop.setVisibility(View.VISIBLE);
    }

    private void setDefaults(boolean clearText) {
        mSyncing = true;
        try {
            if (clearText && mTextValue != null) mTextValue.setText("");
            if (mSize != null) mSize.setProgress(DEF_SIZE);
            if (mStroke != null) mStroke.setProgress(DEF_STROKE);
            if (mColor != null) mColor.setSelection(DEF_COLOR);
            if (mFont != null) mFont.setSelection(DEF_FONT);
            updateLabels();
        } finally { mSyncing = false; }
    }

    private void syncFromSelection(EditorCanvasView.TextItem it) {
        mSyncing = true;
        try {
            if (it == null) {
                if (mTextValue != null) mTextValue.setText("");
                if (mSize != null) mSize.setProgress(DEF_SIZE);
                if (mStroke != null) mStroke.setProgress(DEF_STROKE);
                if (mColor != null) mColor.setSelection(DEF_COLOR);
                if (mFont != null) mFont.setSelection(DEF_FONT);
            } else {
                if (mTextValue != null) {
                    String cur = String.valueOf(mTextValue.getText());
                    String nxt = it.text != null ? it.text : "";
                    if (!nxt.equals(cur)) mTextValue.setText(nxt);
                }
                if (mSize != null) mSize.setProgress(Math.max(0, Math.min(120, Math.round(it.sizePx))));
                if (mStroke != null) mStroke.setProgress(Math.max(0, Math.min(20, Math.round(it.strokePx))));
                if (mColor != null) mColor.setSelection(indexOfColor(it.color));
                if (mFont != null) mFont.setSelection(indexOfFace(it.face));
            }
            updateLabels();
        } finally { mSyncing = false; }
    }

    private int indexOfColor(int c){ for(int i=0;i<COLOR_VALS.length;i++) if (COLOR_VALS[i]==c) return i; return DEF_COLOR; }
    private int indexOfFace(Typeface f){
        if (f==null) return DEF_FONT;
        for(int i=0;i<mFaces.length;i++) if (mFaces[i]==f) return i;
        return DEF_FONT;
    }

    private void scaleItems(java.util.ArrayList items, float sx, float sy){
        if (items == null) return;
        for (int i=0;i<items.size();i++){
            EditorCanvasView.TextItem it = (EditorCanvasView.TextItem) items.get(i);
            it.x *= sx; it.y *= sy; it.sizePx *= sy; it.strokePx *= sy;
        }
    }
    private Bitmap limit(Bitmap b){
        if (b == null) return null;
        int w=b.getWidth(), h=b.getHeight();
        if (w<=0||h<=0) return b;
        float s = Math.min(1f, ((float)MAX_EDIT_PX)/Math.max(w,h));
        if (s >= 0.999f) return copy(b);
        try { return Bitmap.createScaledBitmap(b, Math.max(1,Math.round(w*s)), Math.max(1,Math.round(h*s)), true); }
        catch(Throwable t){ return copy(b); }
    }

    private java.util.ArrayList copyItemsList(java.util.ArrayList src){
        java.util.ArrayList a = new java.util.ArrayList();
        if (src != null) for (int i=0;i<src.size();i++) a.add(((EditorCanvasView.TextItem)src.get(i)).copy());
        return a;
    }

    private void resetImageKeepText(){
        try {
            Bitmap ob = mOriginal != null ? mOriginal : TxImageStore.get();
            if (ob != null) {
                Bitmap cur = mCanvas.getBase();
                int cw = cur!=null?cur.getWidth():0, ch = cur!=null?cur.getHeight():0;
                java.util.ArrayList keep = mCanvas.copyItems(); // keep text as currently placed
                Bitmap base = limit(ob);
                if (cw>0 && ch>0 && (base.getWidth()!=cw || base.getHeight()!=ch)) {
                    scaleItems(keep, ((float)base.getWidth())/cw, ((float)base.getHeight())/ch);
                }
                mCanvas.setBase(base); // reset image
                mCanvas.setItems(keep);
            }
            mCanvas.setPreviewMode(mTargetW, mTargetH, mStretch!=null && mStretch.isChecked());
            mCanvas.saveSnapshot();
        } catch (Throwable t) { Toast.makeText(this, "Reset error: "+t, Toast.LENGTH_SHORT).show(); }
    }
    private void cancelCropEdits(){
        if (!mCanvas.restoreSnapshot()) resetToOriginal();
    }

    private void resetToOriginal() {
        hideIme();
        Bitmap ob = mOriginal != null ? mOriginal : TxImageStore.get();
        if (ob != null) mCanvas.setBase(copy(ob)); // clears text items, resets crop to full image
        mCanvas.setPreviewMode(mTargetW, mTargetH, mStretch!=null && mStretch.isChecked());
        mCanvas.saveSnapshot();
        setDefaults(true);
    }

    private void addNewText() {
        hideIme();
        String t = mTextValue!=null ? String.valueOf(mTextValue.getText()) : "";
        if (t == null || t.trim().length() == 0) t = "Text";
        addTextWith(t);
    }

    private void addTextWith(String text) {
        EditorCanvasView.TextItem it = new EditorCanvasView.TextItem();
        it.text = text;
        it.sizePx = Math.max(8, mSize!=null?mSize.getProgress():DEF_SIZE);
        it.strokePx = mStroke!=null?mStroke.getProgress():DEF_STROKE;
        int ci = mColor!=null?mColor.getSelectedItemPosition():DEF_COLOR;
        it.color = COLOR_VALS[Math.max(0, Math.min(COLOR_VALS.length-1, ci))];
        int fi = mFont!=null?mFont.getSelectedItemPosition():DEF_FONT;
        it.face = (fi>=0 && fi<mFaces.length)?mFaces[fi]:Typeface.DEFAULT;
        Bitmap b = mCanvas.getBase();
        if (b != null) {
            int n = mCanvas.getItems().size();
            it.x = b.getWidth()/10f + (n%5)*12f;
            it.y = b.getHeight()/5f + (n%5)*18f;
        }
        mCanvas.addTextItem(it); // selects + notifies, syncFromSelection keeps controls in sync
    }

    private void applyToActive() {
        EditorCanvasView.TextItem it = mCanvas.getActiveItem();
        if (it==null) return;
        it.sizePx = Math.max(8, mSize.getProgress());
        it.strokePx = mStroke.getProgress();
        it.color = COLOR_VALS[Math.max(0, Math.min(COLOR_VALS.length-1, mColor.getSelectedItemPosition()))];
        int fi = Math.max(0, Math.min(mFaces.length-1, mFont.getSelectedItemPosition())); it.face = mFaces[fi];
        // text is driven by TextWatcher to avoid cursor/IME fights
        mCanvas.invalidate();
    }

    private void updateLabels(){
        if (mSizeLabel!=null && mSize!=null) mSizeLabel.setText("Size: "+mSize.getProgress());
        if (mStrokeLabel!=null && mStroke!=null) mStrokeLabel.setText("Stroke: "+mStroke.getProgress());
    }

    private void hideIme() {
        try {
            if (mTextValue != null) mTextValue.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View f = getCurrentFocus();
            if (imm != null) imm.hideSoftInputFromWindow((f!=null?f:getWindow().getDecorView()).getWindowToken(), 0);
            View root = findViewById(android.R.id.content);
            if (root != null) root.requestFocus();
        } catch (Throwable ignore) {}
    }

    private void done() {
        try {
            hideIme();
            boolean stretch = mStretch!=null && mStretch.isChecked();
            Bitmap original = mOriginal != null ? copy(mOriginal) : mCanvas.copyBase();
            Bitmap base = mCanvas.copyBase();
            java.util.ArrayList items = mCanvas.copyItems();
            // persist editable state in RAM for re-edit (keeps true original for Reset)
            TxImageStore.putEdit(original, base, items, stretch);
            // render for TX/preview (fit/letterbox by default, no baked bars beyond mode canvas)
            Bitmap out = mCanvas.flatten(mTargetW, mTargetH, stretch);
            if (out == null) { Toast.makeText(this, "No image", Toast.LENGTH_SHORT).show(); return; }
            TxImageStore.put(out);
            setResult(RESULT_OK);
            finish();
        } catch (Throwable t){ Toast.makeText(this, "Save edit error: "+t, Toast.LENGTH_LONG).show(); }
    }
}