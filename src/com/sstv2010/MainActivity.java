package com.sstv2010;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StorageMigration.migrateIfNeeded(this);
        setContentView(R.layout.main);
        UiTheme.applyGlobalBackground(this);

        String version = "0.6.0";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException ignore) {}
        TextView sub = (TextView) findViewById(R.id.main_subtext);
        if (sub != null) sub.setText("From 2010Tech | 2026 | " + version);

        Button rx = (Button) findViewById(R.id.btn_receive);
        Button tx = (Button) findViewById(R.id.btn_transmit);
        Button st = (Button) findViewById(R.id.btn_settings);
        rx.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, ReceiveActivity.class)); }
        });
        tx.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, TransmitActivity.class)); }
        });
        if (st != null) st.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        });
    }

    protected void onResume() {
        super.onResume();
        UiTheme.applyGlobalBackground(this);
    }
}
