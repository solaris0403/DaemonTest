package com.example.tony.daemontest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class NativeDaemon {
    private final String BINARY_DEST_DIR_NAME = "bin";
    private final String BINARY_FILE_NAME_1 = "daemon1";
    private final String BINARY_FILE_NAME_2 = "daemon2";
    private final String BINARY_FILE_PROCESS_NAME_1 = "daemon1_d";
    private final String BINARY_FILE_PROCESS_NAME_2 = "daemon2_d";

    private static final String SELF_SERVICE_FILE = "s1f";
    private static final String OTHER_SERVICE_FILE = "s2f";
    private static final String SELF_SERVICE_FILE_TMP = "s1ft";
    private static final String OTHER_SERVICE_FILE_TMP = "s2ft";
    private static final String SELF_NATIVE_FILE = "s1nf";
    private static final String OTHER_NATIVE_FILE = "s2nf";
    private static final String SELF_NATIVE_FILE_TMP = "s1nft";
    private static final String OTHER_NATIVE_FILE_TMP = "s2nft";

    static{
        try {
            System.loadLibrary("NativeDaemon");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Context mContext;
    public NativeDaemon(Context context) {
        this.mContext = context;
        installBinary(mContext, true);
        installBinary(mContext, false);
//        initAmsBinder();
//        initServiceParcel();
//        startServiceByAmsBinder();
    }

    private Parcel					mServiceData;
    private void initServiceParcel() {
        Intent intent = new Intent();
        ComponentName component = new ComponentName(mContext.getPackageName(), SuperService.class.getCanonicalName());
        intent.setComponent(component);
        //write pacel
        mServiceData = Parcel.obtain();
        mServiceData.writeInterfaceToken("android.app.IActivityManager");
        mServiceData.writeStrongBinder(null);
        intent.writeToParcel(mServiceData, 0);
        mServiceData.writeString(null);
        mServiceData.writeInt(0);
    }

    private boolean startServiceByAmsBinder(){
        try {
            if(mRemote == null || mServiceData == null){
                Log.e("Daemon", "REMOTE IS NULL or PARCEL IS NULL !!!");
                return false;
            }
            mRemote.transact(34, mServiceData, null, 0);//START_SERVICE_TRANSACTION = 34
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }


    private IBinder 				mRemote;
    private void initAmsBinder() {
        Class<?> activityManagerNative;
        try {
            activityManagerNative = Class.forName("android.app.ActivityManagerNative");
            Object amn = activityManagerNative.getMethod("getDefault").invoke(activityManagerNative);
            Field mRemoteField = amn.getClass().getDeclaredField("mRemote");
            mRemoteField.setAccessible(true);
            mRemote = (IBinder) mRemoteField.get(amn);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param selfDaemonPath      当前service进程需要启动的二进制文件
     * @param selfDaemonName      当前service进程需要启动的二进制文件name
     * @param selfServiceFile     当前service进程文件
     * @param otherServiceFile    辅助service进程文件
     * @param selfServiceFileTmp  当前service进程临时文件
     * @param otherServiceFileTmp 辅助service进程临时文件
     * @param selfNativeFile      当前native进程文件
     * @param otherNativeFile     辅助native进程文件
     * @param selfNativeFileTmp   当前native进程临时文件
     * @param otherNativeFileTmp  辅助native进程临时文件
     * @param selfPkgName         当前服务包名
     * @param otherPkgName        辅助服务包名
     * @param selfSvcName         当前服务名称
     * @param otherSvcName        辅助服务名称
     * @param sdkVersion          sdk版本
     * @param isCoreService       是否为需要优先启动的service
     */
    public native void doDaemon(String selfDaemonPath, String selfDaemonName,
                                String selfServiceFile, String otherServiceFile,
                                String selfServiceFileTmp, String otherServiceFileTmp,
                                String selfNativeFile, String otherNativeFile,
                                String selfNativeFileTmp, String otherNativeFileTmp,
                                String selfPkgName, String otherPkgName,
                                String selfSvcName, String otherSvcName,
                                int sdkVersion, boolean isCoreService);

    public void runCore() {
        Thread t = new Thread(){
            public void run() {
                File daemonDir = mContext.getDir(BINARY_DEST_DIR_NAME, Context.MODE_WORLD_WRITEABLE);
                doDaemon(
                        new File(daemonDir, BINARY_FILE_NAME_1).getAbsolutePath(),
                        BINARY_FILE_PROCESS_NAME_1,
                        new File(daemonDir, SELF_SERVICE_FILE).getAbsolutePath(),
                        new File(daemonDir, OTHER_SERVICE_FILE).getAbsolutePath(),
                        new File(daemonDir, SELF_SERVICE_FILE_TMP).getAbsolutePath(),
                        new File(daemonDir, OTHER_SERVICE_FILE_TMP).getAbsolutePath(),
                        new File(daemonDir, SELF_NATIVE_FILE).getAbsolutePath(),
                        new File(daemonDir, OTHER_NATIVE_FILE).getAbsolutePath(),
                        new File(daemonDir, SELF_NATIVE_FILE_TMP).getAbsolutePath(),
                        new File(daemonDir, OTHER_NATIVE_FILE_TMP).getAbsolutePath(),
                        mContext.getPackageName(),
                        mContext.getPackageName(),
                        DaemonService.class.getCanonicalName(),
                        OtherService.class.getCanonicalName(),
                        Build.VERSION.SDK_INT,
                        false);
            }
        };
        t.start();
    }

    public void runAssist() {
        Thread t = new Thread(){
            public void run() {
                File daemonDir = mContext.getDir(BINARY_DEST_DIR_NAME, Context.MODE_PRIVATE);
                doDaemon(
                        new File(daemonDir, BINARY_FILE_NAME_2).getAbsolutePath(),
                        BINARY_FILE_PROCESS_NAME_2,

                        new File(daemonDir, OTHER_SERVICE_FILE).getAbsolutePath(),
                        new File(daemonDir, SELF_SERVICE_FILE).getAbsolutePath(),

                        new File(daemonDir, OTHER_SERVICE_FILE_TMP).getAbsolutePath(),
                        new File(daemonDir, SELF_SERVICE_FILE_TMP).getAbsolutePath(),

                        new File(daemonDir, OTHER_NATIVE_FILE).getAbsolutePath(),
                        new File(daemonDir, SELF_NATIVE_FILE).getAbsolutePath(),

                        new File(daemonDir, OTHER_NATIVE_FILE_TMP).getAbsolutePath(),
                        new File(daemonDir, SELF_NATIVE_FILE_TMP).getAbsolutePath(),

                        mContext.getPackageName(),
                        mContext.getPackageName(),

                        OtherService.class.getCanonicalName(),
                        DaemonService.class.getCanonicalName(),

                        Build.VERSION.SDK_INT,
                        false);
            }
        };
        t.start();
    }

    private boolean installBinary(Context context, boolean isCore) {
        String binaryDirName = null;
        String abi = Build.CPU_ABI;
        if (abi.startsWith("armeabi-v7a")) {
            binaryDirName = "armeabi-v7a";
        } else if (abi.startsWith("x86")) {
            binaryDirName = "x86";
        } else {
            binaryDirName = "armeabi";
        }
        if (isCore) {
            return install(context, BINARY_DEST_DIR_NAME, binaryDirName, BINARY_FILE_NAME_1);
        } else {
            return install(context, BINARY_DEST_DIR_NAME, binaryDirName, BINARY_FILE_NAME_2);
        }
    }


    private boolean install(Context context, String destDirName, String assetsDirName, String filename) {
        File file = new File(context.getDir(destDirName, Context.MODE_PRIVATE), filename);
        if (file.exists()) {
            return true;
        }
        try {
            copyAssets(context, (TextUtils.isEmpty(assetsDirName) ? "" : (assetsDirName + File.separator)) + filename, file, "700");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void copyAssets(Context context, String assetsFilename, File file, String mode) throws IOException, InterruptedException {
        AssetManager manager = context.getAssets();
        final InputStream is = manager.open(assetsFilename);
        copyFile(file, is, mode);
    }

    private void copyFile(File file, InputStream is, String mode) throws IOException, InterruptedException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        final String abspath = file.getAbsolutePath();
        final FileOutputStream out = new FileOutputStream(file);
        byte buf[] = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        is.close();
        Runtime.getRuntime().exec("chmod " + mode + " " + abspath).waitFor();
    }

    /**
     * native回调
     */
    public void onDaemonDead(String service) {
        Log.e("NativeDaemon", "onDaemonDead start");
    }
    /**
     * native回调
     */
    public void onDaemonDead() {
        Log.e("NativeDaemon", "onDaemonDead start");
        if (mContext != null) {
            mContext.startService(new Intent(mContext, SuperService.class));
        }
    }
}
