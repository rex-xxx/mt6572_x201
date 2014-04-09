/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.vcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.R;

import java.io.File;

// The following lines are provided and maintained by Mediatek Inc.
import com.mediatek.contacts.activities.ContactImportExportActivity;
// The previous lines are provided and maintained by Mediatek Inc.
/**
 * Shows a dialog confirming the export and asks actual vCard export to {@link VCardService}
 *
 * This Activity first connects to VCardService and ask an available file name and shows it to
 * a user. After the user's confirmation, it send export request with the file name, assuming the
 * file name is not reserved yet.
 */
public class ExportVCardActivity extends Activity implements ServiceConnection,
        DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private static final String LOG_TAG = "VCardExport";
    private static final boolean DEBUG = VCardService.DEBUG;

    /**
     * Handler used when some Message has come from {@link VCardService}.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (DEBUG) Log.d(LOG_TAG, "IncomingHandler received message.");

            if (msg.arg1 != 0) {
                Log.i(LOG_TAG, "Message returned from vCard server contains error code.");
                if (msg.obj != null) {
                    mErrorReason = (String)msg.obj;
                }
                showDialog(msg.arg1);
                return;
            }

            switch (msg.what) {
            case VCardService.MSG_SET_AVAILABLE_EXPORT_DESTINATION:
                if (msg.obj == null) {
                    Log.w(LOG_TAG, "Message returned from vCard server doesn't contain valid path");
                    mErrorReason = getString(R.string.fail_reason_unknown);
                    showDialog(R.id.dialog_fail_to_export_with_reason);
                } else {
                    mTargetFileName = (String)msg.obj;
                    if (TextUtils.isEmpty(mTargetFileName)) {
                        Log.w(LOG_TAG, "Destination file name coming from vCard service is empty.");
                        mErrorReason = getString(R.string.fail_reason_unknown);
                        showDialog(R.id.dialog_fail_to_export_with_reason);
                    } else {
                        if (DEBUG) {
                            Log.d(LOG_TAG,
                                    String.format("Target file name is set (%s). " +
                                            "Show confirmation dialog", mTargetFileName));
                        }
                        showDialog(R.id.dialog_export_confirmation);
                    }
                }
                break;
            default:
                Log.w(LOG_TAG, "Unknown message type: " + msg.what);
                super.handleMessage(msg);
            }
        }
    }

    /**
     * True when this Activity is connected to {@link VCardService}.
     *
     * Should be touched inside synchronized block.
     */
    private boolean mConnected;

    /**
     * True when users need to do something and this Activity should not disconnect from
     * VCardService. False when all necessary procedures are done (including sending export request)
     * or there's some error occured.
     */
    private volatile boolean mProcessOngoing = true;

    private VCardService mService;
    private final Messenger mIncomingMessenger = new Messenger(new IncomingHandler());

    // Used temporarily when asking users to confirm the file name
    private String mTargetFileName;

    // String for storing error reason temporarily.
    private String mErrorReason;

    private class ExportConfirmationListener implements DialogInterface.OnClickListener {
        private final Uri mDestinationUri;

        public ExportConfirmationListener(String path) {
            this(Uri.parse("file://" + path));
        }

        public ExportConfirmationListener(Uri uri) {
            mDestinationUri = uri;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (DEBUG) {
                    Log.d(LOG_TAG,
                            String.format("Try sending export request (uri: %s)", mDestinationUri));
                }
                final ExportRequest request = new ExportRequest(mDestinationUri);
                // The connection object will call finish().
                mService.handleExportRequest(request, new NotificationImportExportListener(
                        ExportVCardActivity.this));
                /*
                 * Bug Fix by Mediatek Begin.
                 *   CR ID: ALPS00110214
                 */
                setResult(ContactImportExportActivity.RESULT_CODE);
                /*
                 * Bug Fix by Mediatek End.
                 */
            }
            unbindAndFinish();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Check directory is available.
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w(LOG_TAG, "External storage is in state " + Environment.getExternalStorageState() +
                    ". Cancelling export");
            showDialog(R.id.dialog_sdcard_not_found);
            return;
        }

        final File targetDirectory = Environment.getExternalStorageDirectory();
        if (!(targetDirectory.exists() &&
                targetDirectory.isDirectory() &&
                targetDirectory.canRead()) &&
                !targetDirectory.mkdirs()) {
            showDialog(R.id.dialog_sdcard_not_found);
            return;
        }

        Intent intent = new Intent(this, VCardService.class);

        if (startService(intent) == null) {
            Log.e(LOG_TAG, "Failed to start vCard service");
            mErrorReason = getString(R.string.fail_reason_unknown);
            showDialog(R.id.dialog_fail_to_export_with_reason);
            return;
        }

        if (!bindService(intent, this, Context.BIND_AUTO_CREATE)) {
            Log.e(LOG_TAG, "Failed to connect to vCard service.");
            mErrorReason = getString(R.string.fail_reason_unknown);
            showDialog(R.id.dialog_fail_to_export_with_reason);
        }
        // Continued to onServiceConnected()
    }

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder binder) {
        if (DEBUG) Log.d(LOG_TAG, "connected to service, requesting a destination file name");
        mConnected = true;
        mService = ((VCardService.MyBinder) binder).getService();
        /**
         * New Feature of Mediatek Inc Begin.
         * Description: The multiple contacts could be selected to export to SD card.
         */
        mService.setQuerySelection(getIntent().getStringExtra("exportselection"));
        String destStoragePath = getIntent().getStringExtra("dest_path");
        if (destStoragePath != null) {
            Log.d(LOG_TAG, "The destination storage path is " + destStoragePath);
            mService.setDestStoragePath(destStoragePath);
        }
        /**
         * New Feature of Mediatek Inc End.
         */
        mService.handleRequestAvailableExportDestination(mIncomingMessenger);
        // Wait until MSG_SET_AVAILABLE_EXPORT_DESTINATION message is available.
    }

    // Use synchronized since we don't want to call unbindAndFinish() just after this call.
    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        if (DEBUG) Log.d(LOG_TAG, "onServiceDisconnected()");
        mService = null;
        mConnected = false;
        if (mProcessOngoing) {
            // Unexpected disconnect event.
            Log.w(LOG_TAG, "Disconnected from service during the process ongoing.");
            mErrorReason = getString(R.string.fail_reason_unknown);
            showDialog(R.id.dialog_fail_to_export_with_reason);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        switch (id) {
            case R.id.dialog_export_confirmation: {
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm_export_title)
                        .setMessage(getString(R.string.confirm_export_message, mTargetFileName))
                        .setPositiveButton(android.R.string.ok,
                                new ExportConfirmationListener(mTargetFileName))
                        .setNegativeButton(android.R.string.cancel, this)
                        .setOnCancelListener(this)
                        .create();
            }
            case R.string.fail_reason_too_many_vcard: {
                mProcessOngoing = false;
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.exporting_contact_failed_title)
                        .setMessage(getString(R.string.exporting_contact_failed_message,
                                getString(R.string.fail_reason_too_many_vcard)))
                        .setPositiveButton(android.R.string.ok, this)
                        .create();
            }
            case R.id.dialog_fail_to_export_with_reason: {
                mProcessOngoing = false;
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.exporting_contact_failed_title)
                        .setMessage(getString(R.string.exporting_contact_failed_message,
                                mErrorReason != null ? mErrorReason :
                                        getString(R.string.fail_reason_unknown)))
                        .setPositiveButton(android.R.string.ok, this)
                        .setOnCancelListener(this)
                        .create();
            }
            case R.id.dialog_sdcard_not_found: {
                mProcessOngoing = false;
                return new AlertDialog.Builder(this)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(R.string.no_sdcard_message)
                        .setPositiveButton(android.R.string.ok, this).create();
            }
        }
        return super.onCreateDialog(id, bundle);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        if (id == R.id.dialog_fail_to_export_with_reason) {
            ((AlertDialog)dialog).setMessage(mErrorReason);
        } else if (id == R.id.dialog_export_confirmation) {
            ((AlertDialog)dialog).setMessage(
                    getString(R.string.confirm_export_message, getSaveFilePathDescription(mTargetFileName)));
        } else {
            super.onPrepareDialog(id, dialog, args);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        /** M: Bug Fix for CR: ALPS00427866 @{ */
        // if (!isFinishing()) {
        //     unbindAndFinish();
        // }
        /** @} */
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (DEBUG) Log.d(LOG_TAG, "ExportVCardActivity#onClick() is called");
        unbindAndFinish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (DEBUG) Log.d(LOG_TAG, "ExportVCardActivity#onCancel() is called");
        mProcessOngoing = false;
        unbindAndFinish();
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        mProcessOngoing = false;
        super.unbindService(conn);
    }

    private synchronized void unbindAndFinish() {
        if (mConnected) {
            unbindService(this);
            mConnected = false;
        }
        finish();
    }

    /** M: The following lines are provided and maintained by Mediatek Inc. @{ */
    /**
     * M: Get the description path which is a description of the file system path.
     * eg: "Phone Storage" is stand for "/storage/sdcard0"
     * CR: ALPS00384104 @{
     */
    private String getSaveFilePathDescription(String path){
        StorageManager mSM = (StorageManager) getApplicationContext().getSystemService(
                STORAGE_SERVICE);
        if (null == mSM) {
            Log.e(LOG_TAG, "Failed to get StorageManager");
            return path;
        }
        StorageVolume volumes[] = mSM.getVolumeList();
        for (StorageVolume storageVolume : volumes) {
            String saveFilePath = storageVolume.getPath();
            String saveFilePathDescription = storageVolume.getDescription(this);
            Log.d(LOG_TAG, "path: " + saveFilePath + " , description: " + saveFilePathDescription);
            if(path.startsWith(saveFilePath)){
                return path.replace(saveFilePath, saveFilePathDescription);
            }
        }
        Log.e(LOG_TAG, "Not found volume for path: " + path);
        return path;
    }
    /**
     * @}
     */

    private static final String FILE_TARGET_NAME = "file_target_name";
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(FILE_TARGET_NAME, mTargetFileName);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTargetFileName = savedInstanceState.getString(FILE_TARGET_NAME);
    }
    
    /** M: The previous lines are provided and maintained by Mediatek Inc. @} */
}