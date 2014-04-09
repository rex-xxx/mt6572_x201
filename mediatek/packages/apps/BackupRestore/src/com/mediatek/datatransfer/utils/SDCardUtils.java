package com.mediatek.datatransfer.utils;



import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import com.mediatek.storage.StorageManagerEx;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.datatransfer.R;
import com.mediatek.datatransfer.utils.Constants.LogTag;
import com.mediatek.datatransfer.utils.Constants.ModulePath;

import java.io.File;
import java.io.IOException;


public class SDCardUtils {

	public final static int MINIMUM_SIZE = 512;
	
	public static String getExternalStoragePath(){
		String storagePath = null;
        StorageManager storageManager = null;

        try {
            storageManager = new StorageManager(Looper.getMainLooper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
        
        storagePath = StorageManagerEx.getExternalStoragePath();
        if(storagePath==null || storagePath.isEmpty()){
        	MyLogger.logE("SDCardUtils", "storagePath is null");
        	return null;
        }
		if(!Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(storagePath))){
			return null;
		}
		return storagePath;
	}
	
	public static String getSDStatueMessage(Context context){
		String message= context.getString(R.string.nosdcard_notice);
		String status = Environment.getExternalStorageState();
		if (status.equals(Environment.MEDIA_SHARED) ||
            status.equals(Environment.MEDIA_UNMOUNTED)) {
			message = context.getString(R.string.sdcard_busy_message);
        }
		return message;
	}
	
	public static String getStoragePath(){
        String storagePath = getExternalStoragePath();
        if(storagePath == null){
        	return null;
        }
        storagePath = storagePath + File.separator + "backup";
        Log.d(LogTag.LOG_TAG,
                "getStoragePath: path is " + storagePath);
        File file = new File(storagePath);
        if (file != null) {

            if (file.exists() && file.isDirectory()) {
                File temp = new File(storagePath + File.separator
                        + ".BackupRestoretemp");
                boolean ret;
                if (temp.exists()) {
                    ret = temp.delete();
                } else {
                    try {
                        ret = temp.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LogTag.LOG_TAG,
                                "getStoragePath: " + e.getMessage());
                        ret = false;
                    } finally {
                        temp.delete();
                    }
                }
                if (ret) {
                    return storagePath;
                } else {
                    return null;
                }

            } else if (file.mkdir()) {
                return storagePath;
            }
        }else {
        	MyLogger.logE(LogTag.LOG_TAG,
                "getStoragePath: path is not ok" );
			return null;
		}
        return null;
    }
	
	public static String getPersonalDataBackupPath(){
	    String path = getStoragePath();
	    if(path != null){
	        return path + File.separator + ModulePath.FOLDER_DATA;
	    }

	    return path;
	}
	
	public static String getAppsBackupPath(){
        String path = getStoragePath();
        if(path != null){
            return path + File.separator + ModulePath.FOLDER_APP;
        }
        return path;
    }

    public static boolean isSdCardAvailable() {
        return (getStoragePath() != null);
    }

    public static long getAvailableSize(String file) {
        android.os.StatFs stat = new android.os.StatFs(file);
        long count = stat.getAvailableBlocks();
        long size = stat.getBlockSize();
        long totalSize = count * size;
        Log.v(LogTag.LOG_TAG, "file remain size = " + totalSize);
        return totalSize;
    }
}

