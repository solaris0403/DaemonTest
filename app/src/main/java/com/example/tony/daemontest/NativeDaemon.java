package com.example.tony.daemontest;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;

public class NativeDaemon {
    private final static String INDICATOR_DIR_NAME = "indicators";
    private final static String INDICATOR_PERSISTENT_FILENAME = "indicator_p";
    private final static String INDICATOR_DAEMON_ASSISTANT_FILENAME = "indicator_d";
    private final static String OBSERVER_PERSISTENT_FILENAME = "observer_p";
    private final static String OBSERVER_DAEMON_ASSISTANT_FILENAME = "observer_d";
    private static NativeDaemon sInstance = new NativeDaemon();
    private static Context mContext;

    public static NativeDaemon getInstance(Context context) {
        mContext = context;
        return sInstance;
    }

    static {
        try {
            System.loadLibrary("NativeDaemon");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startDaemon(final boolean isCore) {
        if (mContext == null) {
            return;
        }
        final File indicatorDir = mContext.getDir(INDICATOR_DIR_NAME, Context.MODE_PRIVATE);
        final String a1 = new File(indicatorDir, INDICATOR_PERSISTENT_FILENAME).getAbsolutePath();
        final String b1 = new File(indicatorDir, INDICATOR_DAEMON_ASSISTANT_FILENAME).getAbsolutePath();
        final String a2 = new File(indicatorDir, OBSERVER_PERSISTENT_FILENAME).getAbsolutePath();
        final String b2 = new File(indicatorDir, OBSERVER_DAEMON_ASSISTANT_FILENAME).getAbsolutePath();
        final String pkg = mContext.getPackageName();
        final String svc = DaemonService.class.getCanonicalName();
        final int version = Build.VERSION.SDK_INT;
        Thread t = new Thread() {
            public void run() {
                if (isCore){
                    doDaemon(a1, b1, a2, b2, pkg, svc, version);
                }else {
                    doDaemon(b1, a1, b2, a2, pkg, svc, version);
                }
            }
        };
        t.start();
    }

    /**
     * native回调
     */
    public void onDaemonDead() {
        Log.e("NativeDaemon", "onDaemonDead");
    }

    public native void doDaemon(String indicatorSelfPath,
                                String indicatorDaemonPath,
                                String observerSelfPath,
                                String observerDaemonPath,
                                String pkgName,
                                String svcName,
                                int sdkVersion);
}
