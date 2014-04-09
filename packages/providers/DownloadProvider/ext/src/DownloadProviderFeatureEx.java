package com.mediatek.downloadmanager.ext;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.mediatek.xlog.Xlog;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.util.HashSet;

public class DownloadProviderFeatureEx extends ContextWrapper implements IDownloadProviderFeatureEx {
    private static final String TAG = "DownloadProviderPluginEx";
    
    public DownloadProviderFeatureEx(Context context) {
        super(context);
    }

    public boolean shouldSetContinueDownload() {
        Xlog.i(TAG, "Enter: " + "shouldSetContinueDownload" + " --default implement");
        return false;
    }

    public boolean shouldSetDownloadPath() {
        Xlog.i(TAG, "Enter: " + "shouldSetDownloadPath" + " --default implement");
        return false;
    }

    public String getNotificationText(String packageName,
            String mimeType, String fullFileName) {
        Xlog.i(TAG, "Enter: " + "getNotificationText" + " --default implement");
        // TODO Auto-generated method stub
        return null;
    }

    public void addAppReadableColumnsSet(
            HashSet<String> sAppReadableColumnsSet, String column) {
        Xlog.i(TAG, "Enter: " + "addAppReadableColumnsSet" + " --default implement");
        // TODO Auto-generated method stub
        
    }

    public void addCustomerDBColumn(SQLiteDatabase db, String dbTable, String columnName,
             String columnDefinition) {
        Xlog.i(TAG, "Enter: " + "addCustomerDBColumn" + " --default implement");
        db.execSQL("ALTER TABLE " + dbTable + " ADD COLUMN " + columnName + " "
                + columnDefinition);
    }

    public void copyContentValues(String key, ContentValues from,
            ContentValues to) {
        Xlog.i(TAG, "Enter: " + "copyContentValues" + " --default implement");
        /*
        String s = from.getAsString(key);
        if (s != null) {
            to.put(key, s);
        }        
        */
    }

    public void setHttpSocketTimeOut(HttpParams params, int timeout) {
        Xlog.i(TAG, "Enter: " + "setHttpSocketTimeOut" + " --default implement");
        HttpConnectionParams.setSoTimeout(params, 60 * 1000);
    }

    public boolean shouldNotifyFileAlreadyExist() {
        Xlog.i(TAG, "Enter: " + "shouldNotifyFileAlreadyExist" + " --default implement");
        return false;
    }

    public int getShowDialogReasonInt(Intent intent) {
        Xlog.i(TAG, "Enter: " + "getShowDialogReasonInt" + " --default implement");
        // TODO Auto-generated method stub
        return -1;
    }

    public String getStorageDirectory(String mimeType) {
        Xlog.i(TAG, "Enter: " + "getStorageDirectory" + " --default implement");
        // TODO Auto-generated method stub
        return null;
    }

    public boolean shouldSetDownloadPathSelectFileMager() {
        Xlog.i(TAG, "Enter: " + "SetDownloadPathSelectFileMager" + " --default implement");
        return false;
    }

    public boolean shouldFinishThisActivity() {
        Xlog.i(TAG, "Enter: " + "shouldFinishThisActivity" + " --default implement");
        return false;
    }

    public boolean shouldProcessWhenFileExist() {
        Xlog.i(TAG, "Enter: " + "shouldProcessWhenFileExist" + " --default implement");
        return false;
    }

    public void showFileAlreadyExistDialog(AlertDialog.Builder builder, CharSequence appLable,
            CharSequence message, String positiveButtonString, String negativeButtonString, OnClickListener listener) {
        Xlog.i(TAG, "Enter: " + "showFileAlreadyExistDialog" + " --default implement");
    }
}
