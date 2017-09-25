package com.example.tony.daemontest;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NativeDaemon {
    private final String BINARY_DEST_DIR_NAME = "bin";
    private final String BINARY_FILE_NAME = "daemon";
    private final String BINARY_FILE_NAME_X = "daemonx";

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
                installBinary(mContext, true);
                File daemonDir = mContext.getDir(BINARY_DEST_DIR_NAME, Context.MODE_PRIVATE);
                doDaemon(
                        new File(daemonDir, BINARY_FILE_NAME).getAbsolutePath(),
                        BINARY_FILE_NAME,
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
                        true);
            }
        };
        t.start();
    }

    public void runAssist() {
        Thread t = new Thread(){
            public void run() {
                installBinary(mContext, false);
                File daemonDir = mContext.getDir(BINARY_DEST_DIR_NAME, Context.MODE_PRIVATE);
                doDaemon(
                        new File(daemonDir, BINARY_FILE_NAME_X).getAbsolutePath(),
                        BINARY_FILE_NAME_X,
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
            return install(context, BINARY_DEST_DIR_NAME, binaryDirName, BINARY_FILE_NAME);
        } else {
            return install(context, BINARY_DEST_DIR_NAME, binaryDirName, BINARY_FILE_NAME_X);
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
    public void onDaemonDead() {
        //关闭两个service进程
        //启动两个service进程
        Log.e("NativeDaemon", "onDaemonDead");
    }

}
