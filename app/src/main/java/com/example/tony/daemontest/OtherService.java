package com.example.tony.daemontest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class OtherService extends Service {
    public OtherService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("NativeDaemon", "onOtherServiceCreate");
        new NativeDaemon(this).runAssist();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("NativeDaemon", "OtherServiceDestroy");
    }
}
