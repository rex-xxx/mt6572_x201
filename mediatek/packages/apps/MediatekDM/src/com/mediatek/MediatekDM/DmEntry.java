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

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.mediatek.MediatekDM.data.DownloadInfo;
import com.mediatek.MediatekDM.data.IDmPersistentValues;
import com.mediatek.MediatekDM.data.PersistentContext;
import com.mediatek.MediatekDM.mdm.PLStorage.ItemType;
import com.mediatek.MediatekDM.option.Options;
import com.mediatek.MediatekDM.util.DialogFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DmEntry extends PreferenceActivity {
    private static final String TAG = "DM/Entry";

    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.i(TAG, "DmEntry onDestroy");
        unbindService(mConnection);
        if (DmService.getServiceInstance() != null) {
            Log.i(TAG, "DmEntry unregister listener");
            DmService.getServiceInstance().unregisterListener(mEntryListener);
        }
        super.onDestroy();
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        startDmService();
        mContext = this;
        addPreferencesFromResource(R.xml.system_update);

        String vtime = getFormattedKernelVersion();
        String buildDate = "";
        try {
            if (!vtime.equals("Unavailable")) {
                Log.i(TAG, "DmEntry: version time = " + vtime);
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", new Locale("US"));
                Date versionTime = new Date(Date.parse(vtime));
                buildDate = df.format(versionTime);
            } else {
                Log.e(TAG, "get build date error");
                buildDate = "2011-01-01";
            }

            Log.i(TAG, " DmEntry: Date = " + buildDate);
        } catch (Exception e) {
            Log.e(TAG, "DmEntry: There is some exception accured parse date string!");
            e.printStackTrace();
        }

        // Current version info
        Preference currentVersionPreference = findPreference(VERSION_PREFERENCE);
        currentVersionPreference.setTitle(
                getResources().getString(R.string.current_version) + " " + Build.DISPLAY);
        currentVersionPreference.setSummary(
                getResources().getString(R.string.release_date) + " " + buildDate);

        mUpdatePreference = findPreference(UPDATE_PREFERENCE);
        mInstallPreference = findPreference(INSTALL_PREFERENCE);
    }

    protected void onStart() {
        super.onStart();
        sStatus = getSWUpdateStatus();
        Log.i(TAG, "onStart the status is " + sStatus);
        updateUI();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        // if sim card is not register to dm server
        if (Options.UseSmsRegister && DmCommonFunction.getRegisteredSimId(mContext) == -1) {
            showDialog(DIALOG_SIMNOTREGISTER);
        } else if (key.equals(UPDATE_PREFERENCE)) {
            Log.i(TAG, "System update key clicked! status is " + sStatus);
            if (sStatus == IDmPersistentValues.STATE_PAUSEDOWNLOAD) {
                showDialog(DIALOG_COVEROLDPKG);
            } else if (sStatus == IDmPersistentValues.STATE_DLPKGCOMPLETE) {
                showDialog(DIALOG_DLGCOMPLETE);
            } else {
                startDmActivity();
            }
        } else if (key.equals(INSTALL_PREFERENCE)) {
            Log.i(TAG, "Update intall key clicked!");
            startDmActivity();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_COVEROLDPKG:
                return DialogFactory.newAlert(this).setTitle(R.string.system_update)
                        .setMessage(R.string.old_pkg_exists)
                        .setPositiveButton(R.string.start_new_session,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        startDmActivity();
                                    }
                                })
                        .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                PersistentContext.getInstance(DmEntry.this).deleteDeltaPackage();
                                PersistentContext.getInstance(DmEntry.this).setDLSessionStatus(
                                        IDmPersistentValues.STATE_QUERYNEWVERSION);
                                DmPLStorage storage = new DmPLStorage(mContext);
                                if (storage != null) {
                                    storage.delete(ItemType.DLRESUME);
                                }

                                startDmActivity();
                            }
                        })
                        .create();
            case DIALOG_PROGRAMCRASH:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.system_update)
                        .setMessage(R.string.program_crash)
                        .setPositiveButton(R.string.start_new_session,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        PersistentContext.getInstance(DmEntry.this)
                                                .deleteDeltaPackage();
                                        PersistentContext.getInstance(DmEntry.this)
                                                .setDLSessionStatus(
                                                        IDmPersistentValues.STATE_QUERYNEWVERSION);
                                        DmPLStorage storage = new DmPLStorage(mContext);
                                        if (storage != null) {
                                            storage.delete(ItemType.DLRESUME);
                                        }
                                        startDmActivity();
                                    }
                                })
                        .create();
            case DIALOG_DLGCOMPLETE:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.system_update)
                        .setMessage(R.string.download_restart)
                        .setPositiveButton(R.string.start_new_session,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        PersistentContext.getInstance(DmEntry.this)
                                                .deleteDeltaPackage();
                                        PersistentContext.getInstance(DmEntry.this)
                                                .setDLSessionStatus(
                                                        IDmPersistentValues.STATE_QUERYNEWVERSION);
                                        DmPLStorage storage = new DmPLStorage(mContext);
                                        if (storage != null) {
                                            storage.delete(ItemType.DLRESUME);
                                        }
                                        startDmActivity();
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finish();
                            }
                        })
                        .create();
            case DIALOG_SIMNOTREGISTER:
                return DialogFactory
                        .newAlert(this)
                        .setTitle(R.string.system_update)
                        .setMessage(R.string.sim_not_register)
                        .setPositiveButton(R.string.start_new_session,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // do nothing
                                    }
                                })
                        .create();
            default:
                return null;
        }
    }

    private int getSWUpdateStatus() {
        return PersistentContext.getInstance(this).getDLSessionStatus();
    }

    private void startDmActivity() {
        Intent activityIntent = new Intent(mContext, DmClient.class);
        activityIntent.setAction("com.mediatek.MediatekDM.DMCLIENT");
        startActivity(activityIntent);
    }

    private void updateUI() {
        Log.i(TAG, "System update stauts is : " + sStatus + " dm status is " + sDmStatus);
        // Set system update status
        DownloadInfo dlInfo = PersistentContext.getInstance(this).getDownloadInfo();
        String dlVersion = (dlInfo != null ? dlInfo.version : "");

        if (sStatus == IDmPersistentValues.STATE_PAUSEDOWNLOAD) {
            mUpdatePreference.setSummary(getResources().getString(R.string.download_status) + " "
                    + dlVersion);
        } else if (sStatus == IDmPersistentValues.STATE_DOWNLOADING) {
            long currentSize = PersistentContext.getInstance(this).getDownloadedSize();
            long totalSize = PersistentContext.getInstance(this).getSize();
            Log.d(TAG, "current size " + currentSize + ", and total size " + totalSize);
            setpartial((int) currentSize, (int) totalSize);
        } else {
            mUpdatePreference.setSummary("");
        }

        if (sDmStatus == IDmPersistentValues.STATE_NIASESSION_START) {
            mUpdatePreference.setSummary("The nia DM session in going");
            mUpdatePreference.setEnabled(false);
        } else if (sDmStatus == IDmPersistentValues.STATE_NIASESSION_CANCLE
                || sDmStatus == IDmPersistentValues.STATE_NIASESSION_COMPLETE) {
            mUpdatePreference.setEnabled(true);
        }

        // Set install status
        if (sStatus == IDmPersistentValues.STATE_DLPKGCOMPLETE) {
            mInstallPreference.setEnabled(true);
            mInstallPreference.setSummary(getResources().getString(R.string.install_version) + " "
                    + dlVersion);
        } else {
            mInstallPreference.setEnabled(false);
            mInstallPreference.setSummary("");
        }
    }

    private void setpartial(int download, int total) {
        int mCurrentProgress = 0;
        try {
            mCurrentProgress = (int) ((double) download / (double) total * 100);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        mUpdatePreference.setSummary(getResources().getString(R.string.download_status) + " "
                + mCurrentProgress + "%");
    }

    private void startDmService() {
        Intent serviceIntent = new Intent(this, DmService.class);
        serviceIntent.setAction("com.mediatek.MediatekDM.DMCLIENT");
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "DmClient gets service reference");
            mService = ((DmService.DmBinder) service).getService();
            if (mService == null) {
                Log.w(TAG, "onServiceConnected mService is null");
                return;
            }
            if (DmService.getServiceInstance() != null && mEntryListener != null) {
                DmService.getServiceInstance().registListener(mEntryListener);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    class EntryListener implements FumoUpdateListener {

        public void onUpdate(Message msg) {
            // TODO Auto-generated method stub
            if (msg == null) {
                return;
            }
            if (msg.what == IDmPersistentValues.MSG_DLPKGUPGRADE) {
                if (sStatus == IDmPersistentValues.STATE_DOWNLOADING) {
                    setpartial(msg.arg1, msg.arg2);
                }
            }
        }

        public void syncStatus(int status) {
            // TODO Auto-generated method stub
            if (status < 0) {
                return;
            }
            sStatus = status;
            updateUI();
        }

        public void syncDmSession(int status) {
            // TODO Auto-generated method stub
            if (status < 0) {
                return;
            }
            sDmStatus = status;
            if (sDmStatus == IDmPersistentValues.STATE_NIASESSION_START
                    || sDmStatus == IDmPersistentValues.STATE_NIASESSION_CANCLE) {
                updateUI();
            }
        }
    }

    private String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }
            if (procVersionStr == null) {
                return "Unavailable";
            }
            final String PROC_VERSION_REGEX =
                    "\\w+\\s+" + /* ignore: Linux */
                            "\\w+\\s+" + /* ignore: version */
                            "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                            "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /*
                                                                         * group
                                                                         * 2:
                                                                         * (xxxxxx
                                                                         * @
                                                                         * xxxxx
                                                                         * .
                                                                         * constant
                                                                         * )
                                                                         */
                            "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /*
                                                                      * ignore:
                                                                      * (gcc ..)
                                                                      */
                            "([^\\s]+)\\s+" + /* group 3: #26 */
                            "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                            "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            if (p == null) {
                return "Unavailable";
            }
            Matcher m = p.matcher(procVersionStr);
            if (m == null) {
                return "Unavailable";
            }
            if (!m.matches()) {
                Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
                        + " groups");
                return "Unavailable";
            } else {
                StringBuilder buildVersion = new StringBuilder(m.group(4));
                return buildVersion.toString();
                // return (new StringBuilder(m.group(1)).append("\n").append(
                // m.group(2)).append(" ").append(m.group(3)).append("\n")
                // .append(m.group(4))).toString();

            }
        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting kernel version for Device Info screen", e);

            return "Unavailable";
        }
    }

    private Context mContext;
    private DmService mService;
    private EntryListener mEntryListener = new EntryListener();

    private static int sDmStatus = -1;
    private static int sStatus = -1;
    private Preference mUpdatePreference;
    private Preference mInstallPreference;
    private static final String VERSION_PREFERENCE = "current_version";
    private static final String UPDATE_PREFERENCE = "system_update";
    private static final String INSTALL_PREFERENCE = "update_install";
    private static final int DIALOG_COVEROLDPKG = 0;
    private static final int DIALOG_PROGRAMCRASH = 1;
    private static final int DIALOG_DLGCOMPLETE = 2;
    private static final int DIALOG_SIMNOTREGISTER = 3;
}
