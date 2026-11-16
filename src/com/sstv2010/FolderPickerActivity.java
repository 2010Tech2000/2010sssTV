package com.sstv2010;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FolderPickerActivity extends Activity {
    public static final String EXTRA_INITIAL_PATH = "initial_path";
    public static final String EXTRA_RESULT_PATH = "result_path";
    public static final String EXTRA_TITLE = "title";

    private File mCurrent;
    private ArrayList mItems = new ArrayList(); // display names
    private ArrayList mFiles = new ArrayList(); // File
    private ArrayAdapter mAdapter;
    private TextView mPathView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.folder_picker);
        UiTheme.applyGlobalBackground(this);

        mPathView = (TextView) findViewById(R.id.fp_path);
        ListView list = (ListView) findViewById(R.id.fp_list);
        ImageButton up = (ImageButton) findViewById(R.id.fp_up);
        Button select = (Button) findViewById(R.id.fp_select);
        Button cancel = (Button) findViewById(R.id.fp_cancel);
        ImageButton newFolder = (ImageButton) findViewById(R.id.fp_new_folder);

        mAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mItems);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                if (position < 0 || position >= mFiles.size()) return;
                Object o = mFiles.get(position);
                if (o instanceof File) {
                    File f = (File) o;
                    if (f.isDirectory()) openDir(f);
                }
            }
        });

        up.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ goUp(); }});
        cancel.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ setResult(RESULT_CANCELED); finish(); }});
        select.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ selectCurrent(); }});
        newFolder.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ promptNewFolder(); }});

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null) { try { setTitle(title); } catch (Throwable ignore) {} }

        String init = getIntent().getStringExtra(EXTRA_INITIAL_PATH);
        File start = null;
        if (init != null && init.length() > 0) {
            try { File f = new File(init); if (f.exists() && f.isDirectory()) start = f; } catch (Throwable ignore) {}
        }
        if (start == null) start = Environment.getExternalStorageDirectory();
        if (start == null || !start.exists()) start = new File("/");
        openDir(start);
    }

    protected void onResume() { super.onResume(); UiTheme.applyGlobalBackground(this); }

    private void openDir(File dir) {
        if (dir == null) return;
        try {
            File[] list = dir.listFiles();
            if (list == null && dir.exists()) {
                // might not be readable
                Toast.makeText(this, R.string.folder_cannot_open, Toast.LENGTH_SHORT).show();
                return;
            }
            mCurrent = dir;
            mItems.clear(); mFiles.clear();
            if (list != null) {
                Arrays.sort(list, new Comparator() {
                    public int compare(Object a, Object b) {
                        File fa = (File) a, fb = (File) b;
                        if (fa.isDirectory() && !fb.isDirectory()) return -1;
                        if (!fa.isDirectory() && fb.isDirectory()) return 1;
                        return fa.getName().compareToIgnoreCase(fb.getName());
                    }
                });
                for (int i = 0; i < list.length; i++) {
                    File f = list[i];
                    if (f == null) continue;
                    if (f.isDirectory()) {
                        String n = f.getName();
                        if (n == null || n.startsWith(".")) continue;
                        mItems.add(n + "/");
                        mFiles.add(f);
                    }
                }
            }
            mAdapter.notifyDataSetChanged();
            mPathView.setText(mCurrent.getAbsolutePath());
        } catch (Throwable t) {
            Toast.makeText(this, R.string.folder_cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private void goUp() {
        if (mCurrent == null) return;
        File p = mCurrent.getParentFile();
        if (p != null) openDir(p);
    }

    private void selectCurrent() {
        if (mCurrent == null) return;
        Intent i = new Intent();
        i.putExtra(EXTRA_RESULT_PATH, mCurrent.getAbsolutePath());
        setResult(RESULT_OK, i);
        finish();
    }

    private void promptNewFolder() {
        if (mCurrent == null) return;
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
            .setTitle(R.string.folder_new)
            .setView(input)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    try {
                        String name = input.getText() != null ? input.getText().toString().trim() : "";
                        if (name.length() == 0) return;
                        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) { Toast.makeText(FolderPickerActivity.this, R.string.folder_invalid_name, Toast.LENGTH_SHORT).show(); return; }
                        File nf = new File(mCurrent, name);
                        if (nf.exists()) { Toast.makeText(FolderPickerActivity.this, R.string.folder_exists, Toast.LENGTH_SHORT).show(); return; }
                        if (nf.mkdirs()) { openDir(mCurrent); }
                        else { Toast.makeText(FolderPickerActivity.this, R.string.folder_create_failed, Toast.LENGTH_SHORT).show(); }
                    } catch (Throwable t) { Toast.makeText(FolderPickerActivity.this, R.string.folder_create_failed, Toast.LENGTH_SHORT).show(); }
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }
}
