/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.MediatekDM;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.conn.DmDataConnection;
import com.mediatek.MediatekDM.data.IDmPersistentValues;
import com.mediatek.MediatekDM.data.PersistentContext;
import com.mediatek.MediatekDM.ext.MTKPhone;
import com.mediatek.MediatekDM.mdm.DownloadDescriptor;
import com.mediatek.MediatekDM.mdm.DownloadDescriptor.Field;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.PLStorage.ItemType;
import com.mediatek.MediatekDM.mdm.fumo.MdmFumoUpdateResult;
import com.mediatek.MediatekDM.option.Options;
import com.mediatek.MediatekDM.util.DialogFactory;
import com.mediatek.MediatekDM.util.DmThreadPool;

import junit.framework.Assert;

import java.util.concurrent.ExecutorService;

public class DmClient extends Activity {

    /**
     * Get the reference of dm client instance.
     * 
     * @return The reference of current dm client instance
     */
    public static DmClient getMdmClientInstance() {
        return sDmClientInstance;
    }

    /**
     * Override function of android.app.Activity, bind dm service when create.
     */
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG.Client, "DmClient->onCreate()");
        super.onCreate(savedInstanceState);
        sDmClientInstance = this;
        if (mProgressBarDlDD == null) {
            Log.d(TAG.Client, "onCreate creat mProgressBarDlDD");
            mProgressBarDlDD = new ProgressDialog(this);
        }

        // start and bind the dm controller service.
        Intent serviceIntent = new Intent(this, DmService.class);
        serviceIntent.setAction(DmConst.IntentAction.ACTION_DM_SERVE);
        // startService(serviceIntent);
        SessionStateControlThread thread = new SessionStateControlThread();
        sExecutorService = DmThreadPool.getInstance();
        if (sExecutorService != null) {
            sExecutorService.execute(thread);
        }

        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Options.UseDirectInternet) {
            Log.i(TAG.Client, "onNewIntent, [option]=internet, execute update...");
            executeUpdate();
        } else {
            Log.i(TAG.Client, "onNewIntent, [option]=DM-WAP, check network...");
            checkNetwork();
        }
    }

    /**
     * Override function of android.app.Activity, unbind service when destroy.
     */
    public void onDestroy() {
        Log.i(TAG.Client, "DmClient->onDestroy()");

        sDmClientInstance = null;
        cancleDialog();
        cancelResumingDialog();
        if (DmService.getServiceInstance() != null) {
            Log.i(TAG.Client, "DmClient unregister listener");
            DmService.getServiceInstance().unregisterListener(mFumoListener);
        }
        unbindService(mConnection);
        super.onDestroy();
    }

    /**
     * Override function of android.app.Activity, called when change the
     * orientation of screen.
     */
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG.Client, "DmClient->onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Show query new version UI and invoke query function of dm service.
     */
    private void onQueryNewVersion() {
        Log.i(TAG.Client, "DmClient==>onQueryNewVersion()");
        DmPLStorage storage = new DmPLStorage(this);
        if (storage != null) {
            storage.delete(ItemType.DLRESUME);
        }
        PersistentContext.getInstance(this).deleteDeltaPackage();
        setDmStatus(IDmPersistentValues.STATE_QUERYNEWVERSION);
        setContentView(R.layout.main);
        if (mProgressBarDlDD == null) {
            mProgressBarDlDD = new ProgressDialog(this);
        }
        mProgressBarDlDD.setCancelable(false);
        mProgressBarDlDD.setIndeterminate(true);
        mProgressBarDlDD.setProgressStyle(android.R.attr.progressBarStyleSmall);
        mProgressBarDlDD.setMessage(getString(R.string.wait));
        mProgressBarDlDD.show();

        mService.setSessionInitor(IDmPersistentValues.CLIENT_PULL);
        if (mHandler != null) {
            mHandler.sendEmptyMessage(IDmPersistentValues.STATE_QUERYNEWVERSION);
        } else {
            Log.w(TAG.Client, "[onQueryNewVersion] mHandler is null");
        }
    }

    /**
     * Show network error UI and will query again if click retry button.
     */
    public void onNetworkError() {
        Log.w(TAG.Client, "DmClient==>onNetworkError()");
        cancleDialog();
        setContentView(R.layout.networkerror);
        mRetryButton = (Button) findViewById(R.id.buttonRetry);
        if (mRetryButton == null) {
            return;
        }

        mRetryButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (mStatus == IDmPersistentValues.STATE_DOWNLOADING) {
                    onDownloadingPkg(IDmPersistentValues.STATE_RESUMEDOWNLOAD);
                } else if (mStatus == IDmPersistentValues.STATE_DETECT_WAP
                        || mStatus == IDmPersistentValues.STATE_WAPCONNECT_TIMEOUT) {
                    checkNetwork();
                } else {
                    onQueryNewVersion();
                }
            }
        });
    }

    /**
     * Show other error (error message except network error) UI and will query
     * again if click retry button.
     */
    public void onOtherError(String errorMsg) {
        Log.w(TAG.Client, "DmClient==>onOtherError(" + errorMsg + ")");
        cancleDialog();
        // if(mDmPersistent!=null)
        // mDmPersistent.deleteDlInfo();
        setContentView(R.layout.networkerror);

        mAuthentication = (TextView) findViewById(R.id.errorView);
        mRetryButton = (Button) findViewById(R.id.buttonRetry);
        if ((mAuthentication == null) || (mRetryButton == null)) {
            return;
        }

        mAuthentication.setText(errorMsg);
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onQueryNewVersion();
            }
        });
    }

    /**
     * Show no new version UI.
     */
    public void onNoNewVersionDetected() {
        Log.i(TAG.Client, "DmClient==>onNoNewVersionDetected()");
        setDmStatus(IDmPersistentValues.STATE_NOTDOWNLOAD);
        cancleDialog();
        // mDmPersistent.deleteDlInfo();
        setContentView(R.layout.nonewversion);
    }

    /**
     * Show new version detected UI and will start download if click download
     * button.
     * 
     * @param DownloadDescriptor dd - Download descriptor of delta package to
     *            download.
     */
    public void onNewVersionDetected(DownloadDescriptor dd) {
        Log.i(TAG.Client, "DmClient==>onNewVersionDetected(),dd=" + dd);
        if (dd == null) {
            Log.w(TAG.Client, "onNewVersionDetected dd is null");
            return;
        }

        if (dd != null) {
            mDd = dd;
        }
        setDmStatus(IDmPersistentValues.STATE_NEWVERSIONDETECTED);
        cancleDialog();

        setContentView(R.layout.releasenotes);
        mDlDescriptions = (TextView) findViewById(R.id.dscrpContent);
        mDlNewFeatureNotes = (TextView) findViewById(R.id.featureNotes);
        mDownloadButton = (Button) findViewById(R.id.buttonDl);
        if ((mDlDescriptions == null) || (mDlNewFeatureNotes == null) || (mDownloadButton == null)) {
            return;
        }

        String description = mDlDescriptions.getText() + " " + mDd.getField(Field.VERSION) + " ("
                + mDd.size
                + " Bytes)";
        String releasenotes = mDd.getField(Field.DESCRIPTION);
        mDlDescriptions.setText(description);
        mDlNewFeatureNotes.setText(releasenotes);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // On download button clicked
                boolean hasEnoughSpace = PersistentContext.getInstance(DmClient.this).getMaxSize() > mDd.size;
                if (!hasEnoughSpace) {
                    showDialog(DIALOG_NOENOUGHSPACE);
                    return;
                }

                if (!Options.UseDirectInternet) {
                    // for CMCC case (CMWAP connection)
                    onDownloadingPkg(IDmPersistentValues.STATE_DOWNLOADING);
                } else {
                    // check network type before downloading.
                    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connMgr != null) {
                        NetworkInfo net = connMgr.getActiveNetworkInfo();
                        if (net != null) {
                            int netType = net.getType();
                            if (netType == ConnectivityManager.TYPE_MOBILE) {
                                if (net.isRoaming()) {
                                    Log.d(TAG.Client, "[WARNING] downloading in roaming network.");
                                    showDialog(DIALOG_ROAMING_DOWNLOAD);
                                } else {
                                    Log.d(TAG.Client, "[WARNING] downloading in mobile network.");
                                    showDialog(DIALOG_GPRSDOWNLOAD);
                                }
                            } else if (netType == ConnectivityManager.TYPE_WIFI) {
                                Log.i(TAG.Client, "[INFO] downloading via wifi.");
                                onDownloadingPkg(IDmPersistentValues.STATE_DOWNLOADING);
                            } else {
                                Log.e(TAG.Client, "[ERROR] invalid network type: " + netType + "."
                                        + net.getTypeName());
                            }
                        } else {
                            Log.e(TAG.Client,
                                    "[ERROR] no active network for downloading FOTA pack.");
                            showDialog(DIALOG_NETWORKERROR);
                        }
                    } else {
                        Log.e(TAG.Client, "[ERROR] no active network for downloading FOTA pack.");
                        showDialog(DIALOG_NETWORKERROR);
                    }
                }

            }
        });
    }

    /**
     * Show downloading UI and will: 1. Pause download if click pause button; 2.
     * Show confirm cancel download dialog if click cancel button; 3.
     * Downloading back ground if click back key.
     * 
     * @param int status - downloading status
     */
    private void onDownloadingPkg(int status) {
        Log.i(TAG.Client, "DmClient==>onDownloadingPkg(" + status + ")");
        setDmStatus(IDmPersistentValues.STATE_DOWNLOADING);
        String description;
        String releasenotes;

        mDd = PersistentContext.getInstance(this).getDownloadDescriptor();
        long downloadedSize = PersistentContext.getInstance(this).getDownloadedSize();

        Log.i(TAG.Client, "download size is " + downloadedSize);
        Log.i(TAG.Client, "total size is " + mDd.size);

        setContentView(R.layout.downloading);
        mProgressBarDlPkg = (ProgressBar) findViewById(R.id.progressbarDownload);
        mDlRatial = (TextView) findViewById(R.id.rate);
        mDlDescriptions = (TextView) findViewById(R.id.dscrpContentDl);
        mDlNewFeatureNotes = (TextView) findViewById(R.id.featureNotesDl);
        mCancelButton = (Button) findViewById(R.id.cancellbutton);
        mPausedButton = (Button) findViewById(R.id.buttonSuspend);
        if ((mProgressBarDlPkg == null) || (mDlRatial == null)
                || (mDlDescriptions == null) || (mDlNewFeatureNotes == null)
                || (mCancelButton == null) || (mPausedButton == null)) {
            return;
        }

        description = mDlDescriptions.getText() + " " + mDd.getField(Field.VERSION) + " ("
                + mDd.size + " Bytes)";
        releasenotes = mDd.getField(Field.DESCRIPTION);
        mDlDescriptions.setText(description);
        mDlNewFeatureNotes.setText(releasenotes);
        mCancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                onDlPkgCancelled();
            }
        });

        mPausedButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                onDlPkgPaused();
            }
        });

        Log.i(TAG.Client, "mDownloadedSize is " + downloadedSize + " ,mDd size is " + mDd.size);
        onDlPkgUpgrade(downloadedSize, mDd.size);
        if (mHandler != null) {
            if (status == IDmPersistentValues.STATE_RESUMEDOWNLOAD) {
                Log.v(TAG.Client, "FOTA DL is Resuming, waiting...");
                showResumingDialog();
            }
            mHandler.sendEmptyMessage(status);
        } else {
            Log.w(TAG.Client, "[onDownloadingPkg] mHandler is null");
        }
    }

    private void showResumingDialog() {
        Log.v(TAG.Client, "DmClient==>showResumingDialog");
        if (mResumingDialog == null) {
            mResumingDialog = new ProgressDialog(this);
        }
        mResumingDialog.setCancelable(false);
        mResumingDialog.setIndeterminate(true);
        mResumingDialog.setProgressStyle(android.R.attr.progressBarStyleSmall);
        mResumingDialog.setMessage(getString(R.string.resuming_download));
        mResumingDialog.show();
    }

    private void cancelResumingDialog() {
        Log.v(TAG.Client, "DmClient==>cancelResumingDialog");
        if (mResumingDialog != null) {
            mResumingDialog.dismiss();
            mResumingDialog = null;
        }
    }

    /**
     * Update the download progress of downloading UI.
     * 
     * @param float dlSize - the downloaded size of the delta package
     * @param float totalSize - the total size of the delta package
     */
    public void onDlPkgUpgrade(long dlSize, long totalSize) {
        Log.i(TAG.Client, "DmClient==>onDlPkgUpgrade(" + dlSize + "," + totalSize + ")");
        int ratial = (int) (((float) dlSize / (float) totalSize) * 100);
        if (mProgressBarDlPkg != null) {
            mProgressBarDlPkg.setProgress(ratial);
        }
        CharSequence text = Integer.toString(ratial) + "%    " + Integer.toString((int) dlSize)
                + " Bytes / "
                + Integer.toString((int) totalSize) + " Bytes";
        if (mDlRatial != null) {
            mDlRatial.setText(text);
        }
    }

    /**
     * The response function of click pause button of downloading UI.
     */
    private void onDlPkgPaused() {
        Log.i(TAG.Client, "DmClient==>onDlPkgPaused()");
        DmApplication.getInstance().cancelAllPendingJobs();

        setDmStatus(IDmPersistentValues.STATE_PAUSEDOWNLOAD);
        if (mHandler != null) {
            mHandler.sendEmptyMessage(IDmPersistentValues.STATE_PAUSEDOWNLOAD);
        } else {
            Log.w(TAG.Client, "[onDlPkgPaused] mHandler is null");
        }
        finish();
    }

    /**
     * The response function of click cancel button of downloading UI.
     */
    private void onDlPkgCancelled() {
        Log.i(TAG.Client, "DmClient==>onDlPkgCancelled()");
        DmApplication.getInstance().cancelAllPendingJobs();

        // pause the download first before show the cancel dialog
        Log.i(TAG.Client, "onDlPkgCancelled set the session state is cancled");
        setDmStatus(IDmPersistentValues.STATE_PAUSEDOWNLOAD);
        if (mHandler != null) {
            mHandler.sendEmptyMessage(IDmPersistentValues.STATE_PAUSEDOWNLOAD);
        } else {
            Log.w(TAG.Client, "[onDlPkgCancelled] mHandler is null");
        }
        showDialog(DIALOG_CANCELDOWNLOAD);
    }

    /**
     * Show download complete UI after download delta package finished.
     */
    public void onDlPkgComplete(String[] updateTypes) {
        Log.i(TAG.Client, "DmClient==>onDlPkgComplete():" + updateTypes);
        setDmStatus(IDmPersistentValues.STATE_DLPKGCOMPLETE);
        mDd = null;
        setContentView(R.layout.updateenquire);
        onShowUpdateList(updateTypes);
        mUpdateButton = (Button) findViewById(R.id.update);
        if (mUpdateButton == null) {
            return;
        }

        mUpdateButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // onUpdateTypeSelected(mSingleChoiceList.getSelectedItemId());
                onUpdateTypeSelected();
            }
        });
    }

    /**
     * The response function of click OK button of download complete UI.
     * 
     * @param long typeId - the id of the list view item user selected.
     */
    private void onUpdateTypeSelected() {
        Log.i(TAG.Client, "DmClient==>onUpdateTypeSelected()");
        if (mHandler != null) {
            mHandler.sendEmptyMessage(IDmPersistentValues.STATE_DLPKGCOMPLETE);
        } else {
            Log.w(TAG.Client, "[onUpdateTypeSelected] mHandler is null");
        }
        finish();
    }

    /**
     * Show update type list view.
     * 
     * @param String [] updateTypes - string array which contains the text
     *            content of update types.
     */
    private void onShowUpdateList(String[] updateTypes) {
        Log.i(TAG.Client, "DmClient==>onShowUpdateList():" + updateTypes);
        mSingleChoiceList = (ListView) findViewById(R.id.updatetypelist);
        if (mSingleChoiceList == null) {
            return;
        }

        mSingleChoiceList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice,
                updateTypes));
        mSingleChoiceList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mSingleChoiceList.setClickable(true);
        mSingleChoiceList.setItemsCanFocus(false);
        mSingleChoiceList.setItemChecked(0, true);
        mSingleChoiceList.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mSingleChoiceList.setItemChecked(arg2, true);
                mUpdateType = arg2;
                Log.d(TAG.Client, "onItemClick select is " + arg2);
            }

        });
    }

    @Override
    /**
     * Override function of com.android.Activity
     * @param ind id - The dialog type to create.
     */
    protected Dialog onCreateDialog(int id) {
        Log.i(TAG.Client, "DmClient->onCreateDialog(" + id + ")");
        switch (id) {
            case DIALOG_NETWORKERROR:
                return DialogFactory.newAlert(this).setTitle(R.string.networkerror)
                        .setNeutralButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // On informed the network error
                                        finish();
                                    }
                                }).create();

            case DIALOG_NONEWVERSION:
                return DialogFactory.newAlert(this).setTitle(R.string.nonewversion)
                        .setNeutralButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // On informed the current version is
                                        // the
                                        // latest
                                        finish();
                                    }
                                }).create();

            case DIALOG_NOENOUGHSPACE:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.noenoughspace)
                        .setPositiveButton(
                                R.string.appmanager, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // On user go to application manager to
                                        // release
                                        // space
                                        Intent intent = new Intent();
                                        intent.setAction("com.android.settings.ManageApplications");
                                        intent.putExtra("DmClient", true);
                                        sendBroadcast(intent);
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // On user abort to down load
                                finish();
                            }
                        }).create();

            case DIALOG_CANCELDOWNLOAD:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.cancel)
                        .setMessage(R.string.canceldownload)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // On user confirm cancel download
                                setDmStatus(IDmPersistentValues.STATE_CANCELDOWNLOAD);
                                if (mHandler != null) {
                                    mHandler.sendEmptyMessage(IDmPersistentValues.STATE_CANCELDOWNLOAD);
                                } else {
                                    Log.w(TAG.Client,
                                            "[DIALOG_CANCELDOWNLOAD]positive button, mHandler is null");
                                }
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // On user abort cancel download
                                onDownloadingPkg(IDmPersistentValues.STATE_RESUMEDOWNLOAD);
                            }
                        }).create();
            case DIALOG_GPRSDOWNLOAD:
                return DialogFactory.newAlert(this).setTitle(R.string.gprs_download_title)
                        .setMessage(
                                R.string.gprs_download)
                        .setPositiveButton(R.string.start_new_session,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        onDownloadingPkg(IDmPersistentValues.STATE_DOWNLOADING);
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mHandler != null) {
                                    mHandler.sendEmptyMessage(IDmPersistentValues.STATE_NOTDOWNLOAD);
                                } else {
                                    Log.w(TAG.Client,
                                            "[DIALOG_GPRSDOWNLOAD] negative button, mHandler is null");
                                }
                                finish();
                            }
                        }).create();

            case DIALOG_ROAMING_DOWNLOAD:
                return DialogFactory.newAlert(this).setTitle(R.string.gprs_download_title)
                        .setMessage(
                                R.string.roaming_download)
                        .setPositiveButton(R.string.start_new_session,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        onDownloadingPkg(IDmPersistentValues.STATE_DOWNLOADING);
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mHandler != null) {
                                    mHandler.sendEmptyMessage(IDmPersistentValues.STATE_NOTDOWNLOAD);
                                } else {
                                    Log.w(TAG.Client,
                                            "[DIALOG_ROAMING_DOWNLOAD] negative button, mHandler is null");
                                }
                                finish();
                            }
                        }).create();
            default:
                break;

        }
        return null;
    }

    // Handler mHandler = new Handler(){
    // @Override
    // public void handleMessage(Message msg){
    // switch(msg.what){
    // case IDmPersistentValues.MSG_NETWORKERROR:
    // case IDmPersistentValues.MSG_CONNECTTIMEOUT:
    // onNetworkError();
    // break;
    // case IDmPersistentValues.MSG_NEWVERSIONDETECTED:
    // onNewVersionDetected((DownloadDescriptor)msg.obj);
    // break;
    // case IDmPersistentValues.MSG_NONEWVERSIONDETECTED:
    // onNoNewVersionDetected();
    // break;
    // case IDmPersistentValues.MSG_DLPKGUPGRADE:
    // onDlPkgUpgrade(msg.arg1, msg.arg2);
    // break;
    // case IDmPersistentValues.MSG_DLPKGCOMPLETE:
    // onDlPkgComplete((String[])msg.obj);
    // break;
    // case IDmPersistentValues.MSG_DLPKGRESUME:
    // break;
    // case IDmPersistentValues.MSG_OTHERERROR:
    // onOtherError((String)msg.obj);
    // break;
    // default:
    // super.handleMessage(msg);
    // }
    // }
    // };

    /**
     * Thread of calling dm service functions. Start a new thread to call
     * functions of service to avoid ANR. Use handler to process in order.
     */
    public class SessionStateControlThread extends Thread {

        public void run() {
            Log.i(TAG.Client, "In DmClient's new thread to invoke service's function: "
                    + mStateType);
            Looper.prepare();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case IDmPersistentValues.STATE_QUERYNEWVERSION:
                            mService.queryNewVersion();
                            break;
                        case IDmPersistentValues.STATE_DOWNLOADING:
                            mService.startDlPkg();
                            break;
                        case IDmPersistentValues.STATE_CANCELDOWNLOAD:
                            mService.cancelDlPkg();
                            break;
                        case IDmPersistentValues.STATE_PAUSEDOWNLOAD:
                            Log.i(TAG.Client, "SessionStateControlThread state is pausedownload");
                            mService.pauseDlPkg();
                            break;
                        case IDmPersistentValues.STATE_RESUMEDOWNLOAD:
                            mService.resumeDlPkg();
                            break;
                        case IDmPersistentValues.STATE_DLPKGCOMPLETE:
                            mService.setUpdateType(mUpdateType);
                            break;
                        case IDmPersistentValues.STATE_NOTDOWNLOAD:
                            mService.reportresult(MdmFumoUpdateResult.ResultCode.USER_CANCELED);
                            break;
                        default:
                            break;
                    }
                }
            };
            Looper.loop();
        }

        private int mStateType;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG.Client, "DmClient->onServiceConnected(), got service reference");
            mService = ((DmService.DmBinder) service).getService();
            if (mService == null) {
                Log.w(TAG.Client, "onServiceConnected mService is null");
                return;
            }
            if (DmService.getServiceInstance() != null && mFumoListener != null) {
                Log.w(TAG.Client, "onServiceConnected DmClient register listener");
                DmService.getServiceInstance().registListener(mFumoListener);
            }

            if (Options.UseDirectInternet) {
                Log.i(TAG.Client, "[option]=internet, execute update...");
                executeUpdate();
            } else {
                Log.i(TAG.Client, "[option]=DM-WAP, check network...");
                checkNetwork();
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private void executeUpdate() {
        Log.i(TAG.Client, "DmClient==>executeUpdate()");
        if (DmService.getServiceInstance() == null) {
            Log.w(TAG.Client, "executeUpdate service is not available");
            return;
        }

        try {
            if (!DmService.getServiceInstance().IsInitDmController()) {
                DmService.getServiceInstance().initDmController();
            }
        } catch (Exception e) {
            Log.e(TAG.Client, e.getMessage(), e);
            return;
        }

        mStatus = PersistentContext.getInstance(this).getDLSessionStatus();
        Log.i(TAG.Client, "executeUpdate state is " + mStatus);

        switch (mStatus) {
            case IDmPersistentValues.STATE_PAUSEDOWNLOAD:
            case IDmPersistentValues.STATE_RESUMEDOWNLOAD:
                onDownloadingPkg(IDmPersistentValues.STATE_RESUMEDOWNLOAD);
                break;
            case IDmPersistentValues.STATE_DOWNLOADING:
                onDownloadingPkg(IDmPersistentValues.STATE_DOWNLOADING);
                break;
            case IDmPersistentValues.STATE_DLPKGCOMPLETE:
                String[] updateTypes = mService.getUpdateTypes();
                if (updateTypes == null) {
                    Log.i(TAG.Client, "updateTypes is null.");
                    break;
                }
                onDlPkgComplete(updateTypes);
                break;
            case IDmPersistentValues.STATE_NEWVERSIONDETECTED:
                mDd = PersistentContext.getInstance(this).getDownloadDescriptor();
                onNewVersionDetected(mDd);
                break;
            default:
                onQueryNewVersion();
        }
    }

    private synchronized void setDmStatus(int status) {
        Log.d(TAG.Client, "DmClient->setDmStatus(" + status + ")");
        if (status < 0) {
            return;
        }
        mStatus = status;
        if (DmService.getServiceInstance() != null) {
            DmService.getServiceInstance().syncStateWithClient(status);
        }
        PersistentContext.getInstance(this).setDLSessionStatus(status);
    }

    private synchronized void interrupt() {
        Log.w(TAG.Client, "DmClient->interrupt()");
    }

    private synchronized void cancleDialog() {
        Log.d(TAG.Client, "DmClient->cancleDialog()");
        if (mProgressBarDlDD != null) {
            try {
                Log.i(TAG.Client, "cancleDialog cancle mProgressBarDlDD");
                mProgressBarDlDD.cancel();
            } catch (Exception e) {
                Log.e(TAG.Client, e.getMessage(), e);
            }
            mProgressBarDlDD = null;
        }
    }

    private class FumoListener implements FumoUpdateListener {

        public void onUpdate(Message msg) {
            Log.d(TAG.Client, "DmClient.FumoListener->onupdate():" + msg);
            if (msg == null) {
                return;
            }
            switch (msg.what) {
                case IDmPersistentValues.MSG_NEWVERSIONDETECTED:
                    Log.d(TAG.Client, "msg>>new version detected.");
                    setDmStatus(IDmPersistentValues.STATE_NEWVERSIONDETECTED);

                    int sessionInitor = DmService.getServiceInstance().getSessionInitor();
                    if (sessionInitor == IDmPersistentValues.CLIENT_PULL
                            || sessionInitor == IDmPersistentValues.CLIENT_POLLING) {
                        onNewVersionDetected((DownloadDescriptor) msg.obj);
                    }
                    break;
                case IDmPersistentValues.MSG_DLPKGUPGRADE:
                    Log.d(TAG.Client, "msg>>dl pkg ongoing.");
                    if (mStatus == IDmPersistentValues.STATE_DOWNLOADING) {
                        onDlPkgUpgrade(msg.arg1, msg.arg2);
                    }
                    break;
                case IDmPersistentValues.MSG_DLPKGSTARTED:
                    Log.d(TAG.Client, "msg>>dl pkg started.");
                    cancelResumingDialog();
                    break;
                case IDmPersistentValues.MSG_DLPKGCOMPLETE:
                    Log.d(TAG.Client, "msg>>dl pkg complete.");
                    // if(mStatus==IDmPersistentValues.STATE_DOWNLOADING)
                    // {
                    if (DmService.getServiceInstance() != null) {
                        if (DmService.getServiceInstance().getUpdateTypes() != null) {
                            onDlPkgComplete(DmService.getServiceInstance().getUpdateTypes());
                        }
                    }
                    // }
                    break;
                case IDmPersistentValues.MSG_DMSESSIONCOMPLETED:
                    Log.d(TAG.Client, "msg>>dm session complete. status=" + mStatus + ",initor="
                            + DmService.getServiceInstance().getSessionInitor());
                    if (DmService.getServiceInstance() != null) {
                        if (DmService.getServiceInstance().getSessionInitor() == IDmPersistentValues.CLIENT_PULL
                                && mStatus == IDmPersistentValues.STATE_NOTDOWNLOAD) {
                            onNoNewVersionDetected();
                        }
                    }

                    break;
                case IDmPersistentValues.MSG_DLSESSIONABORTED:
                case IDmPersistentValues.MSG_DMSESSIONABORTED:
                    Log.w(TAG.Client, "msg>>dl/dm session aborted.");
                    interrupt();
                    int lasterror = msg.arg1;
                    if ((lasterror == MdmException.MdmError.COMMS_FATAL.val)
                            || (lasterror == MdmException.MdmError.COMMS_NON_FATAL.val)
                            || (lasterror == MdmException.MdmError.COMMS_SOCKET_ERROR.val)
                            || lasterror == MdmException.MdmError.COMMS_HTTP_ERROR.val
                            || lasterror == MdmException.MdmError.COMMS_SOCKET_TIMEOUT.val) {
                        onNetworkError();
                        Log.w(TAG.Client, "Get network error message.");
                    } else if (lasterror == MdmException.MdmError.COMMS_SOCKET_TIMEOUT.val) {
                        Log.w(TAG.Client, "Get time out error message.");
                        onNetworkError();
                    } else if (mStatus != IDmPersistentValues.STATE_PAUSEDOWNLOAD
                            && lasterror != MdmException.MdmError.CANCEL.val) {
                        Log.i(TAG.Client, "Get other erro");
                        String errorMsg = "Error happens. Last error is " + lasterror;
                        onOtherError(errorMsg);
                    }
                    break;
                default:
                    break;
            }
        }

        public void syncStatus(int status) {
            Log.d(TAG.Client, "DmClient.FumoListener->syncstatus(" + status + ")");
            if (status == -1) {
                return;
            }
            mStatus = status;
        }

        public void syncDmSession(int status) {

        }

    }

    // ******************************** DM APN begins
    // **********************************//
    public Handler apnConnHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG.Client, "DmClient->apnConnHandler->handleMessage()");
            if (mStatus != IDmPersistentValues.STATE_DETECT_WAP) {
                Log.w(TAG.Client, "apnConnHandler state is not STATE_DETECT_WAP, the status = "
                        + mStatus);
                return;
            }

            Log.i(TAG.Client, "apnConnHandler message is " + msg.what);
            switch (msg.what) {
                case IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS:
                    Log.i(TAG.Client,
                            "apnConnHandler handleMessage message is connect sucesss");
                    // modify for CR ALPS002821226
                    // setDmStatus(IDmPersistentValues.STATE_WAPCONNECT_SUCCESS);
                    mStatus = IDmPersistentValues.STATE_WAPCONNECT_SUCCESS;
                    // end modify
                    if (mNetworkDetect != null) {
                        mNetworkDetect.cancel();
                        mNetworkDetect = null;
                    }
                    cancleNetworkTimeoutAlarm();
                    executeUpdate();
                    break;
                case IDmPersistentValues.MSG_WAP_CONNECTION_TIMEOUT:
                    Log.i(TAG.Client,
                            "apnConnHandler handleMessage message is connect timeout");
                    // setDmStatus(IDmPersistentValues.STATE_WAPCONNECT_TIMEOUT);
                    mStatus = IDmPersistentValues.STATE_WAPCONNECT_TIMEOUT;
                    if (mNetworkDetect != null) {
                        mNetworkDetect.cancel();
                        mNetworkDetect = null;
                    }
                    onNetworkError();
                    break;
                default:
                    break;
            }
        }
    };

    private void checkNetwork() {
        Assert.assertTrue("check network should only be used in DM WAP connection.",
                !Options.UseDirectInternet);
        Log.i(TAG.Client, "checkNetwork begin");
        try {
            if (mNetworkDetect == null) {
                mNetworkDetect = new ProgressDialog(this);
            }
            mNetworkDetect.setCancelable(false);
            mNetworkDetect.setIndeterminate(true);
            mNetworkDetect.setProgressStyle(android.R.attr.progressBarStyleSmall);
            mNetworkDetect.setMessage(getString(R.string.network_detect));
            mNetworkDetect.show();

            Log.d(TAG.Client, "checkNetwork begin check ");
            int result = DmDataConnection.getInstance(this).startDmDataConnectivity();
            Log.d(TAG.Client, "checkNetwork result is " + result);
            if (result == MTKPhone.APN_ALREADY_ACTIVE) {
                Log.i(TAG.Client, "checkNetwork network is ok, continue");
                mNetworkDetect.cancel();
                executeUpdate();
            } else {
                Log.i(TAG.Client, "checkNetwork network is not ok, request network establish");
                // setDmStatus(IDmPersistentValues.STATE_DETECT_WAP);
                mStatus = IDmPersistentValues.STATE_DETECT_WAP;
                setNetworkTimeoutAlarm();
            }
        } catch (Exception e) {
            Log.e(TAG.Client, e.getMessage(), e);
        }
    }

    private void setNetworkTimeoutAlarm() {
        Log.d(TAG.Client, "setAlarm alarm");
        Intent intent = new Intent();
        intent.setAction(DmConst.IntentAction.NET_DETECT_TIMEOUT);
        sAlarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (sAlarmManager == null) {
            Log.w(TAG.Client, "setAlarm sAlarmManager is null");
            return;
        }
        if (sAlarmManager != null) {
            sNetworkTimeoutIntent = PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            sAlarmManager.cancel(sNetworkTimeoutIntent);
            sAlarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + TIME_OUT_VALUE
                    * ONESEVOND),
                    sNetworkTimeoutIntent);
        }
    }

    private void cancleNetworkTimeoutAlarm() {
        if (sAlarmManager != null && sNetworkTimeoutIntent != null) {
            sAlarmManager.cancel(sNetworkTimeoutIntent);
            sAlarmManager = null;
            sNetworkTimeoutIntent = null;
        }
    }

    private static final int ONESEVOND = 1000;
    private static final int TIME_OUT_VALUE = 30;
    private static AlarmManager sAlarmManager = null;
    private static PendingIntent sNetworkTimeoutIntent = null;
    private ProgressDialog mNetworkDetect = null;
    // ******************************** DM APN end
    // **********************************//

    private Button mDownloadButton;
    private Button mPausedButton;
    private Button mCancelButton;
    private Button mUpdateButton;
    private Button mRetryButton;
    private TextView mDlRatial;
    private TextView mDlDescriptions;
    private TextView mDlNewFeatureNotes;
    private TextView mAuthentication;
    private ListView mSingleChoiceList;
    private ProgressBar mProgressBarDlPkg;
    private ProgressDialog mProgressBarDlDD = null;
    private ProgressDialog mResumingDialog = null;
    Handler mHandler = null;// a handler in SessionStateControlThread.

    private DmService mService;
    private DownloadDescriptor mDd = null;
    private long mUpdateType = 0;
    private int mStatus = -1;

    private static DmClient sDmClientInstance = null;

    static final int DIALOG_NETWORKERROR = 0;
    static final int DIALOG_NONEWVERSION = 1;
    static final int DIALOG_CANCELDOWNLOAD = 2;
    static final int DIALOG_NOENOUGHSPACE = 3;
    static final int DIALOG_GPRSDOWNLOAD = 4;
    static final int DIALOG_ROAMING_DOWNLOAD = 5;

    private static ExecutorService sExecutorService = null;
    private FumoListener mFumoListener = new FumoListener();
}
