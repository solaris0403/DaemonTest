package com.example.tony.daemontest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by tony on 9/27/17.
 */

public class Launcher {
    private Context mContext;
    private IBinder mRemote;
    private Parcel mServiceData;
    private Parcel mBroadcastData;

    public Launcher(Context context) {
        this.mContext = context;
    }

    public void buildBroadcastLauncher(String broadcastName) {
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
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName(mContext.getPackageName(), broadcastName);
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        mBroadcastData = Parcel.obtain();
        mBroadcastData.writeInterfaceToken("android.app.IActivityManager");
        mBroadcastData.writeStrongBinder(null);
        intent.writeToParcel(mBroadcastData, 0);
        mBroadcastData.writeString(intent.resolveTypeIfNeeded(mContext.getContentResolver()));
        mBroadcastData.writeStrongBinder(null);
        mBroadcastData.writeInt(Activity.RESULT_OK);
        mBroadcastData.writeString(null);
        mBroadcastData.writeBundle(null);
        mBroadcastData.writeString(null);
        mBroadcastData.writeInt(-1);
        mBroadcastData.writeInt(0);
        mBroadcastData.writeInt(0);
        mBroadcastData.writeInt(0);


    }

    public boolean sendBroadcastByAmsBinder(){
        try {
            if(mRemote == null || mBroadcastData == null){
                Log.e("Daemon", "REMOTE IS NULL or PARCEL IS NULL !!!");
                return false;
            }
            mRemote.transact(14, mBroadcastData, null, 0);//BROADCAST_INTENT_TRANSACTION = 0x00000001 + 13
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }
}
