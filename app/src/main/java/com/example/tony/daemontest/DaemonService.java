package com.example.tony.daemontest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DaemonService extends Service {
    public DaemonService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("NativeDaemon", "onDaemonServiceCreate");
        startService(new Intent(DaemonService.this, DaemonService.class));
        NativeDaemon.getInstance(DaemonService.this).startDaemon(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("NativeDaemon", "onDaemonServiceDestroy");
    }
}
