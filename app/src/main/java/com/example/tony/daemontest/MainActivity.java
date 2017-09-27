package com.example.tony.daemontest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, DaemonService.class));
        startService(new Intent(this, OtherService.class));
        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Launcher launcher = new Launcher(MainActivity.this);
                launcher.buildBroadcastLauncher(SuperService.class.getCanonicalName());
                final long start = System.currentTimeMillis();
                Log.e("NativeDaemon", String.valueOf(start));
//                sendBroadcast(new Intent(DaemonReceiver.ACTION));
//                startService(new Intent(MainActivity.this, SuperService.class));
                launcher.sendBroadcastByAmsBinder();
            }
        });
    }
}
