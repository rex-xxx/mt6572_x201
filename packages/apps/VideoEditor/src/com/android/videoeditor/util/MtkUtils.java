package com.android.videoeditor.util;

import java.io.File;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.android.videoeditor.R;

import com.mediatek.storage.StorageManagerEx;

public class MtkUtils {
    private static final String TAG = "MtkUtils";
    private static final boolean LOG = true;
    
    public static File getExternalFilesDir(Context context) {
        File ret = null;
        try {
            ensureStorageManager();
            //let framework to create related folder and .nomedia file
            ret = StorageManagerEx.getExternalCacheDir(context.getPackageName());
            if (ret != null) {
                ret = new File(ret.getParent() + "/files");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret = context.getExternalFilesDir(null);//default impl
        }
        if (LOG) MtkLog.v(TAG, "getExternalFilesDir() return " + (ret == null ? "null" : ret.getAbsolutePath())); 
        return ret;
    }

    private static final long LIMITED_SPACE_SIZE = 10 * 1024;//10k 
    public static String appendStorageSpaceInfo(Context context, String text) {
        if (!isLeftEnoughSpace(context)) {
            if (text == null || "".equals(text)) {
                text = context.getResources().getString(getNoEnoughSpaceResId());
            } else {
                text += "\n" + context.getResources().getString(getNoEnoughSpaceResId());
            }
        }
        return text;
    }
    
    public static boolean isLeftEnoughSpace(Context context) {
        boolean enough = true;
        long left = -1;
        try {
            File file = getExternalFilesDir(context);
            if (file != null) {
                String path = file.getPath();
                StatFs stat = new StatFs(path);
                left = stat.getAvailableBlocks() * (long) stat.getBlockSize();
            }
            if (left <= LIMITED_SPACE_SIZE) {
                enough = false;
            }
        } catch (Exception e) {
            MtkLog.w(TAG, "Fail to access external storage", e);
        }
        if (LOG) MtkLog.v(TAG, "isLeftEnoughSpace() left=" + left + ", return " + enough);
        return enough;
    }
    
    public static int getNoEnoughSpaceResId() {
        int resId = 0;
        if (isMultiStorage()) {
            if(isRemoveableStorage()){// EMMC only
                resId = com.mediatek.internal.R.string.storage_sd;
            } else if(haveRemoveableStorage()){
                resId = com.mediatek.internal.R.string.storage_withsd;
            } else {
                resId = com.mediatek.internal.R.string.storage_withoutsd;
            }
        } else {
            resId = R.string.not_enough_space;
        }
        return resId;
    }
    
    private static StorageManager sStorageManager;
    private static void ensureStorageManager() {
        if (sStorageManager == null) {
            try {
                sStorageManager = new StorageManager(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static boolean isRemoveableStorage(){
        ensureStorageManager();
        boolean removeable = false;
        if (sStorageManager != null) {
            String storagePath = StorageManagerEx.getDefaultPath();
            StorageVolume[] volumes = sStorageManager.getVolumeList();
            if (volumes != null) {
                for(int i = 0, len = volumes.length; i < len; i++){
                    StorageVolume volume = volumes[i];
                    if(volume != null && volume.getPath().equals(storagePath)) {
                        removeable = volumes[i].isRemovable();
                        break;
                    }
                }
            }
        }
        if (LOG) MtkLog.v(TAG, "RemoveableStorage() return " + removeable);
        return removeable;
    }

    public static boolean isMultiStorage(){
        ensureStorageManager();
        boolean ismulti = false;
        if (sStorageManager != null) {
            StorageVolume[] volumes = sStorageManager.getVolumeList();
            if (volumes != null) {
                ismulti = volumes.length > 1;
            }
        }
        if (LOG) MtkLog.v(TAG, "isMultiStorage() return " + ismulti);
        return ismulti;
    }

    public static boolean haveRemoveableStorage(){
        ensureStorageManager();
        boolean have = false;
        StorageVolume[] volumes = sStorageManager.getVolumeList();
        if (volumes != null) {
            for(int i = 0, len = volumes.length; i < len; i++){
                StorageVolume volume = volumes[i];
                if(volume.isRemovable()
                        && Environment.MEDIA_MOUNTED.equals(sStorageManager.getVolumeState(volumes[i].getPath()))) {
                    have = true;
                }
            }
        }
        if (LOG) MtkLog.v(TAG, "haveRemoveableStorage() return " + have);
        return have;
    }
    
    /* M: copied from packages/providers/MediaProvider/.../MediaProvider.java
     * MediaProvider will keep its original behavior,
     * here we let VideoEditor's saving folder changed according to current primary storage path.
     * JB: jelly bean just modify video's save path, so here we keep image's behavior.
     * @{
    */
    public static final String IMAGE_PREFERRED_EXTENSION = ".jpg";
    public static final String VIDEO_PREFERRED_EXTENSION =  ".3gp";
    public static final String IMAGE_DIRECTORY_NAME = "DCIM/Camera";
    public static final String VIDEO_DIRECTORY_NAME = "video"; 
    
    public static void ensureFile(final ContentValues initialValues, String preferredExtension, String directoryName) {
        String file = initialValues.getAsString(MediaStore.MediaColumns.DATA);
        if (TextUtils.isEmpty(file)) {
            file = generateFileName(preferredExtension, directoryName);
            initialValues.put(MediaStore.MediaColumns.DATA, file);
        }

        if ((file == null) || !ensureFileExists(file)) {
            throw new IllegalStateException("Unable to create new file: " + file);
        }
    }
    
    private static String generateFileName(String preferredExtension, String directoryName) {
        String filePath = null;
        ensureStorageManager();
        if (sStorageManager != null) {
            // create a random file
            String name = String.valueOf(System.currentTimeMillis());
            String storagePath = StorageManagerEx.getDefaultPath();
            filePath = storagePath + "/" + directoryName + "/" + name + preferredExtension;
        }
        if (LOG) MtkLog.v(TAG, "generateFileName() return " + filePath);
        return filePath;
    }
    
    private static boolean ensureFileExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        } else {
            // we will not attempt to create the first directory in the path
            // (for example, do not create /sdcard if the SD card is not mounted)
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash < 1) return false;
            String directoryPath = path.substring(0, secondSlash);
            File directory = new File(directoryPath);
            if (!directory.exists())
                return false;
            file.getParentFile().mkdirs();
            try {
                return file.createNewFile();
            } catch(IOException ioe) {
                MtkLog.e(TAG, "File creation failed", ioe);
            }
            return false;
        }
    }
    /// @}

    /// M: support switching sdcard for video @{
    private static final String DCIM = 
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    private static final String DIRECTORY = DCIM + "/Camera";
    
    public static String getVideoOutputMediaFilePath() {
        String videopath = null;
        ensureStorageManager();
        if (sStorageManager != null) {
            videopath = StorageManagerEx.getDefaultPath() + "/" + Environment.DIRECTORY_DCIM + "/Camera";
        } else {
            videopath = DIRECTORY + "/" + Environment.DIRECTORY_DCIM + "/Camera";
        }
        if (LOG) MtkLog.v(TAG, "getVideoOutputMediaFilePath() return " + videopath);
        return videopath;
    }
    /// @}

    public static String getMovieExportPath() {
        String exportPath = StorageManagerEx.getDefaultPath() + "/" + Environment.DIRECTORY_MOVIES;
        if (LOG) MtkLog.v(TAG, "getMovieExportPath() return " + exportPath);
        return exportPath;
    }
}
