package com.mediatek.nfc.handover;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.webkit.MimeTypeMap;
import android.net.Uri;
import java.io.File;
import android.util.Log;


import android.provider.MediaStore;
import android.database.Cursor;

public class FilePushRecord {
    private static final String TAG = "FilePushRecord";
    private static final String AUTHORITY = "com.android.settings.provider.beam.share";
    private static final String TABLE_NAME = "share_tasks";
    private Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
    private static final String TASK_TYPE = "type";
    private static final String TASK_STATE = "state";
    private static final String TASK_OBJECT_FILE = "data";
    private static final String TASK_MIMETYPE = "mime";
    private static final String TASK_TOTAL_BYTES = "total";
    private static final String TASK_DONE_BYTES = "done";
    private static final String TASK_MODIFIED_DATE = "modified";
    private ContentResolver mContentResolver;

    private Context mContext;

    public int TYPE_BT_INCOMING = 0;
    public int TYPE_BT_OUTGOING = 1;
    public int TYPE_WIFI_INCOMING = 2;
    public int TYPE_WIFI_OUTGOING = 3;
    public int STATE_FAILURE = 0;
    public int STATE_SUCCESS = 1;


    public static final String ACTION_BT_INCOMING   = "com.mediatek.nfc.handover.intent.action.BT_INCOMING";
    public static final String ACTION_BT_OUTGOING   = "com.mediatek.nfc.handover.intent.action.BT_OUTGOING";

    public static final String EXTRA_URL            = "com.mediatek.nfc.handover.FilePushRecord.intent.extra.URL";
    public static final String EXTRA_TYPE           = "com.mediatek.nfc.handover.FilePushRecord.intent.extra.TYPE";
    public static final String EXTRA_RESULT         = "com.mediatek.nfc.handover.FilePushRecord.intent.extra.RESULT";
    public static final String EXTRA_DONE_BYTES     = "com.mediatek.nfc.handover.FilePushRecord.intent.extra.DONE_BYTES";
    public static final String EXTRA_TOTAL_BYTES    = "com.mediatek.nfc.handover.FilePushRecord.intent.extra.TOTAL_BYTES";



    private FilePushRecord(Context context) {
        mContentResolver = context.getContentResolver(); 
        mContext = context;       

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BT_INCOMING);
        filter.addAction(ACTION_BT_OUTGOING);
        mContext.registerReceiver(mReceiver, filter);             
        
    }
    
    private static FilePushRecord mStaticInstance;

    public static void createSingleton(Context context) {
        if (mStaticInstance == null) {
            mStaticInstance = new FilePushRecord(context); 
        }
    }

    public static FilePushRecord getInstance() {
        return mStaticInstance;
    }

    public boolean insertBtIncomingRecord(String url, String mimeType, boolean result, long doneBytes, long totalBytes) {
        try {
            insertRecord(url, mimeType, TYPE_BT_INCOMING, result ? STATE_SUCCESS : STATE_FAILURE, doneBytes, totalBytes); 
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean insertBtOutgoingRecord(String url, String mimeType, boolean result, long doneBytes, long totalBytes) {
        try {
            insertRecord(url, mimeType, TYPE_BT_OUTGOING, result ? STATE_SUCCESS : STATE_FAILURE, doneBytes, totalBytes); 
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean insertWifiIncomingRecord(String url, boolean result, long doneBytes, long totalBytes) {
        try {
            insertRecord(url, TYPE_WIFI_INCOMING, result ? STATE_SUCCESS : STATE_FAILURE, doneBytes, totalBytes); 
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean insertWifiOutgoingRecord(Uri uri, boolean result, long doneBytes, long totalBytes) {
        try {
            insertRecord(uri, TYPE_WIFI_OUTGOING, result ? STATE_SUCCESS : STATE_FAILURE, doneBytes, totalBytes); 
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void insertRecord(Uri uri, int type, int state, long doneBytes, long totalBytes) {
        insertRecord(uri.getPath(), getMimeType(uri.getPath()), type, state, doneBytes, totalBytes);
    }

    private void insertRecord(String url, int type, int state, long doneBytes, long totalBytes) {
        insertRecord(url, getMimeType(url), type, state, doneBytes, totalBytes);
    }

    private void insertRecord(String url, String mimeType, int type, int state, long doneBytes, long totalBytes) {
        
        Log.d(TAG, "insertRecord  url:" + url+" type:" +type+ " state:"+state+ " doneBytes:"+doneBytes+" totalBytes"+totalBytes);
        ContentValues values = new ContentValues();

        values.put(TASK_TYPE, type);
        values.put(TASK_STATE, state);
        values.put(TASK_OBJECT_FILE, url);
        values.put(TASK_MIMETYPE, mimeType);
        values.put(TASK_DONE_BYTES, (long)doneBytes);
        values.put(TASK_TOTAL_BYTES, (long)totalBytes);
        values.put(TASK_MODIFIED_DATE, System.currentTimeMillis());
        
        mContentResolver.insert(CONTENT_URI, values);
    }

    private String getMimeType(String url) {
        String type = "";
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }
    
    /**
     * Del the MediaStore image file(Gallery) on external storage
     * @param filePath The path (on external storage) of the file to delete
     * @return void
     */
    public void deleteRecord(File filePath, String mimeType) {
        try {
            deleteRecordImpl(filePath, mimeType);
        } catch (Exception e) {
            Log.d(TAG, "not able to delete record due to exception");
            e.printStackTrace();
        }
    }
     
    private void deleteRecordImpl(File filePath, String mimeType) {
        String absFilePath = filePath.getAbsolutePath();
        String[] projection = new String[1];
        String selection = null;
        Uri queryUri = null;
        Log.d(TAG,"mimeType:"+mimeType+"  filePath.getAbsolutePath(): "+absFilePath);
        
        if (mimeType.startsWith("image/")){
            projection[0]   =   MediaStore.Images.Media._ID;
            selection       =   MediaStore.Images.Media.DATA;
            queryUri        =   MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        else if(mimeType.startsWith("video/")){
            projection[0]   =   MediaStore.Video.Media._ID;
            selection       =   MediaStore.Video.Media.DATA;
            queryUri        =   MediaStore.Video.Media.EXTERNAL_CONTENT_URI;            
        }
        else if(mimeType.startsWith("audio/")){
            projection[0]   =   MediaStore.Audio.Media._ID;
            selection       =   MediaStore.Audio.Media.DATA;
            queryUri        =   MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;            
        }
        else{
            Log.d(TAG,"not support mimeType:"+mimeType+"  not deleteRecord()   Return;");
            return;
        }
        
        Log.d(TAG,"queryUri = " + queryUri.toString());

        //(the other possibility is "internal")
        //Uri imageUri = MediaStore.Images.Media.getContentUri("external");//MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        //Log.d(TAG,"imageUri = " + imageUri.toString());

        //String[] projection = {MediaStore.Images.Media._ID};
        Cursor cursor = mContentResolver.query(queryUri, projection, selection + " LIKE ?", new String[] { absFilePath }, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
        //Log.d(TAG,"cursor.getColumnName():" + cursor.getColumnName(columnIndex2));
        
        String idString = cursor.getString(columnIndex);
        Log.d(TAG, "cursor.getCount(): "+ cursor.getCount()+"  idString : " + idString);

        //Uri contentPathUri = Uri.parse(cursor.getString(columnIndex));
        //Log.d(TAG, "contentPathUri.getPath() : " + contentPathUri.getPath());
        //Log.d(TAG, "contentPathUri.toString() : " + contentPathUri.toString());

        Uri dstContentUri = Uri.withAppendedPath(queryUri,idString);  

        Log.d(TAG,"CR.delete dstContentUri.toString() " + dstContentUri.toString());
        cursor.close();

        mContentResolver.delete(dstContentUri, "", null);  
     }
        

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d(TAG, "mReceiver.onReceive(): action:"+ action);
            
            if (action.equals(ACTION_BT_INCOMING)){

                String url          = intent.getStringExtra(EXTRA_URL);
                String mime_type    = intent.getStringExtra(EXTRA_TYPE);
                boolean result      = intent.getBooleanExtra(EXTRA_RESULT,true);
                long doneBytes      = intent.getLongExtra(EXTRA_DONE_BYTES,0);
                long totalBytes     = intent.getLongExtra(EXTRA_TOTAL_BYTES,0);

                Log.d(TAG, "INCOMING:  url:"+url+" result:"+result+" doneBytes:"+doneBytes+" totalBytes:"+totalBytes );

                insertBtIncomingRecord(url,mime_type,result,doneBytes,totalBytes);

            

            }
            else if(action.equals(ACTION_BT_OUTGOING)){

                String url          = intent.getStringExtra(EXTRA_URL);
                String mime_type    = intent.getStringExtra(EXTRA_TYPE);
                boolean result      = intent.getBooleanExtra(EXTRA_RESULT,true);
                long doneBytes      = intent.getLongExtra(EXTRA_DONE_BYTES,0);
                long totalBytes     = intent.getLongExtra(EXTRA_TOTAL_BYTES,0);

                Log.d(TAG, "OUTGOING:  url:"+url+" result:"+result+" doneBytes:"+doneBytes+" totalBytes:"+totalBytes ); 
                
                insertBtOutgoingRecord(url,mime_type,result,doneBytes,totalBytes);

            }

        }
    };


             
}
