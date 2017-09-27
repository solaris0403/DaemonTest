package com.example.tony.daemontest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DaemonReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.example.tony.daemontest.DaemonReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("NativeDaemon", intent.getAction());
    }
}
