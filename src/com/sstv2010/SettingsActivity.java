package com.sstv2010;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private static final int REQ_RX_DIR = 2101;
    private static final int REQ_TX_DIR = 2102;

    private CheckBox mAutoSave;
    private EditText mRxDir, mTxDir, mCustomBg;
    private Spinner mBg;
    private TextView mAppDir;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try { getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); } catch (Throwable ignore) {}
        setContentView(R.layout.settings);
        UiTheme.applyGlobalBackground(this);
        try { getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); } catch (Throwable ignore) {}

        mAutoSave = (CheckBox) findViewById(R.id.set_auto_save_rx);
        mRxDir = (EditText) findViewById(R.id.set_rx_dir);
        mTxDir = (EditText) findViewById(R.id.set_tx_dir);
        mBg = (Spinner) findViewById(R.id.set_bg);
        mCustomBg = (EditText) findViewById(R.id.set_custom_bg);
        mAppDir = (TextView) findViewById(R.id.set_app_dir);

        ArrayAdapter ad = new ArrayAdapter(this, android.R.layout.simple_spinner_item, UiTheme.BG_LABELS);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBg.setAdapter(ad);

        Button save = (Button) findViewById(R.id.set_save);
        Button reset = (Button) findViewById(R.id.set_reset);
        Button browseRx = (Button) findViewById(R.id.set_browse_rx);
        Button browseTx = (Button) findViewById(R.id.set_browse_tx);
        Button applyCustom = (Button) findViewById(R.id.set_apply_custom_bg);
        save.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ saveSettings(); }});
        reset.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ resetDefaults(); }});
        if (browseRx != null) browseRx.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ pickDir(REQ_RX_DIR, safeText(mRxDir, StoragePaths.defaultRxImageDir())); }});
        if (browseTx != null) browseTx.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ pickDir(REQ_TX_DIR, safeText(mTxDir, StoragePaths.defaultTxAudioDir())); }});
        if (applyCustom != null) applyCustom.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ applyCustomColor(); }});
        load();

        // keep keyboard hidden if focus lands on edittexts
        hideKeyboard();
    }

    protected void onResume() {
        super.onResume();
        UiTheme.applyGlobalBackground(this);
        hideKeyboard();
    }

    private void hideKeyboard() {
        try { getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); } catch (Throwable ignore) {}
        try {
            View v = getCurrentFocus();
            if (v != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                v.clearFocus();
            }
        } catch (Throwable ignore) {}
    }

    private void pickDir(int req, String initial) {
        try {
            Intent i = new Intent(this, FolderPickerActivity.class);
            i.putExtra(FolderPickerActivity.EXTRA_INITIAL_PATH, initial);
            startActivityForResult(i, req);
        } catch (Throwable t) {
            Toast.makeText(this, "Cannot open folder picker", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int req, int res, Intent data) {
        if (res != RESULT_OK || data == null) return;
        String p = data.getStringExtra(FolderPickerActivity.EXTRA_RESULT_PATH);
        if (p == null || p.length() == 0) return;
        if (req == REQ_RX_DIR && mRxDir != null) mRxDir.setText(p);
        else if (req == REQ_TX_DIR && mTxDir != null) mTxDir.setText(p);
    }

    private void load() {
        try { mAutoSave.setChecked(AppSettings.autoSaveRx(this)); } catch (Throwable ignore) {}
        try { mRxDir.setText(AppSettings.rxImageDir(this)); } catch (Throwable ignore) {}
        try { mTxDir.setText(AppSettings.txAudioDir(this)); } catch (Throwable ignore) {}
        try { mCustomBg.setText(AppSettings.customBackground(this)); } catch (Throwable ignore) {}
        try {
            java.io.File f = StoragePaths.appFilesDir(this);
            mAppDir.setText(getString(R.string.settings_app_folder) + ": " + (f != null ? f.getAbsolutePath() : "(unavailable)"));
        } catch (Throwable ignore) {}
        try {
            mBg.setSelection(UiTheme.indexForKey(AppSettings.globalBackground(this)));
        } catch (Throwable ignore) {}
    }

    private void applyCustomColor() {
        try {
            String hex = mCustomBg.getText() != null ? mCustomBg.getText().toString().trim() : "";
            if (hex.length() == 0) {
                AppSettings.setCustomBackground(this, "");
                Toast.makeText(this, "Custom color cleared", Toast.LENGTH_SHORT).show();
            } else {
                int color = UiTheme.parseColor(hex);
                if (color == 0) {
                    Toast.makeText(this, "Invalid color format (use #RRGGBB or #AARRGGBB)", Toast.LENGTH_SHORT).show();
                    return;
                }
                AppSettings.setCustomBackground(this, hex);
                AppSettings.setGlobalBackground(this, UiTheme.BG_OLED);
                mBg.setSelection(0);
                UiTheme.applyGlobalBackground(this);
                Toast.makeText(this, "Custom color applied", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Toast.makeText(this, "Error: " + t, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSettings() {
        try {
            AppSettings.setAutoSaveRx(this, mAutoSave.isChecked());
            String rx = StoragePaths.resolveDir(safeText(mRxDir, StoragePaths.defaultRxImageDir()), StoragePaths.defaultRxImageDir()).getAbsolutePath();
            String tx = StoragePaths.resolveDir(safeText(mTxDir, StoragePaths.defaultTxAudioDir()), StoragePaths.defaultTxAudioDir()).getAbsolutePath();
            AppSettings.setRxImageDir(this, rx);
            AppSettings.setTxAudioDir(this, tx);
            AppSettings.setGlobalBackground(this, UiTheme.keyForIndex(mBg.getSelectedItemPosition()));
            // reflect normalized paths
            mRxDir.setText(rx); mTxDir.setText(tx);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Throwable t) {
            Toast.makeText(this, "Save failed: " + t, Toast.LENGTH_LONG).show();
        }
    }

    private void resetDefaults() {
        try {
            mAutoSave.setChecked(false);
            mRxDir.setText(StoragePaths.defaultRxImageDir());
            mTxDir.setText(StoragePaths.defaultTxAudioDir());
            mBg.setSelection(UiTheme.indexForKey(UiTheme.BG_OLED));
            Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignore) {}
    }

    private String safeText(EditText e, String def) {
        try {
            String v = e.getText() != null ? e.getText().toString().trim() : "";
            return v.length() == 0 ? def : v;
        } catch (Throwable t) { return def; }
    }
}
