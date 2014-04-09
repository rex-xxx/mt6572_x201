package com.mediatek.downloadmanager.ext;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import org.apache.http.params.HttpParams;

import java.util.HashSet;

public interface IDownloadProviderFeatureEx {

    /**
     * OP02 Feature
     * Usage location: DownloadInfo.java
     * Check whether need set download path to the download info.
     */
    boolean shouldSetContinueDownload();

    /**
     * OP01 Feature
     * Usage location: DownloadInfo.java
     * Check whether need set download path to the download info.
     */
    boolean shouldSetDownloadPath();

    /**
     * 
     * @param context context of app
     * @param packageName completed download will call package 
     * @param mimeType download file mimetype
     * @param fullFileName 
     * @return
     */
    String getNotificationText(String packageName,
            String mimeType, String fullFileName);

    /**
     * 
     * @param sAppReadableColumnsSet hashset object to maintain colums
     * @param column which column name  add to hashset
     */
    void addAppReadableColumnsSet(HashSet<String> sAppReadableColumnsSet, String column);

     /**
     * Add a column to a table using ALTER TABLE.
     * @param dbTable name of the table
     * @param columnName name of the column to add
     * @param columnDefinition SQL for the column definition
     */
    void addCustomerDBColumn(SQLiteDatabase db, String dbTable, String columnName,
                           String columnDefinition);

    /**
     * copy the given key and relevant value from one contentvalues to another
     * @param key  
     * @param from
     * @param to
     */
    void copyContentValues(String key, ContentValues from, ContentValues to);

    /**
     * set timeout in Http Socket params
     * @param params
     * @param timeout
     */
    void setHttpSocketTimeOut(HttpParams params, int timeout);

    /**
     * M: Add this function to support CU customization
     * If user click "Yes", continue download.
     * If click "Cancel", abort download.
     */
    boolean shouldNotifyFileAlreadyExist();

    int getShowDialogReasonInt(Intent intent);

    String getStorageDirectory(String mimeType);

    boolean shouldSetDownloadPathSelectFileMager();

    boolean shouldFinishThisActivity();

    boolean shouldProcessWhenFileExist();

    void showFileAlreadyExistDialog(AlertDialog.Builder builder, CharSequence appLable,
            CharSequence message, String positiveButtonString, String negativeButtonString, OnClickListener listener);
}
