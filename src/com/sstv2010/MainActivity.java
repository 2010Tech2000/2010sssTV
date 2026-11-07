package com.sstv2010;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button rx = (Button) findViewById(R.id.btn_receive);
        Button tx = (Button) findViewById(R.id.btn_transmit);
        rx.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, ReceiveActivity.class)); }
        });
        tx.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, TransmitActivity.class)); }
        });
    }
}
