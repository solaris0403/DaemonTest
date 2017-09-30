package com.example.tony.daemontest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SuperService extends Service {
    public SuperService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("NativeDaemon", "SuperService onCreate:");
//        stopService(new Intent(this, DaemonService.class));
//        stopService(new Intent(this, OtherService.class));
//        startService(new Intent(this, DaemonService.class));
//        startService(new Intent(this, OtherService.class));
//        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
