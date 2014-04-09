/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.deskclock;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.io.File;

/**
 * The RingtonePreference does not have a way to get/set the current ringtone so
 * we override onSaveRingtone and onRestoreRingtone to get the same behavior.
 */
public class AlarmPreference extends RingtonePreference {
    protected static final Uri DEFAULT_RINGTONE_URI = Uri.parse("content://media/internal/audio/media/5");
    private Uri mAlert;
    private boolean mChangeDefault;
    private AsyncTask mRingtoneTask;

    public AlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        Log.v("ringtoneUri: " + ringtoneUri);
        setAlert(ringtoneUri);
        if (mChangeDefault) {
            // Update the default alert in the system.
            saveRingtone(ringtoneUri);
        }
    }

    @Override
    protected Uri onRestoreRingtone() {
        Log.v("mAlert: " + mAlert);
        if (mAlert != null
                && (RingtoneManager.isDefault(mAlert) || !isRingtoneExisted(getContext(), mAlert))) {
            return RingtoneManager.getActualDefaultRingtoneUri(getContext(),
                    RingtoneManager.TYPE_ALARM);
        }
        return mAlert;
    }
    public void setAlert(Uri alert) {
        mAlert = alert;
        if (alert != null) {
            setSummary(R.string.loading_ringtone);
            if (mRingtoneTask != null) {
                mRingtoneTask.cancel(true);
            }
            mRingtoneTask = new AsyncTask<Uri, Void, String>() {
                @Override
                protected String doInBackground(Uri... params) {
                    Log.v("params[0]=" + params[0]);
                    Ringtone r = null;
                    if (params[0] == null)
                        return null;
                    String title = "";
                    // use default ringtone, deal with default external ringtone file is removed issue.
                    // if removed, then return default ringtone: "Cesium" 
                    if (params[0].toString().contains("system/alarm_alert")) {
                        // use user set default ringtone
                        title = usingDefaultRingtone();
                    } else {
                        //user specified ringtone
                        if (isRingtoneExisted(getContext(), params[0])) {
                            r = RingtoneManager.getRingtone(getContext(), params[0]);
                        }
                        if (r != null) {
                            title = r.getTitle(getContext());
                        } else {
                            title = usingDefaultRingtone();
                        }
                    }
                    return title;
                }

                @Override
                protected void onPostExecute(String title) {
                    if (!isCancelled()) {
                        Log.v("AlarmPreference title = " + title);
                        if (TextUtils.isEmpty(title)) {
                            setSummary(R.string.silent_alarm_summary);
                        } else {
                            setSummary(title);
                        }
                        mRingtoneTask = null;
                    }
                }
            }.execute(alert);
        } else {
            setSummary(R.string.silent_alarm_summary);
        }
    }

    public Uri getAlert() {
        if (mAlert != null
                && "content://settings/system/alarm_alert".equals(mAlert
                        .toString())) {
            mAlert = RingtoneManager.getActualDefaultRingtoneUri(getContext(),
                    RingtoneManager.TYPE_ALARM);
        }
        return mAlert;
    }

    public void setChangeDefault() {
        mChangeDefault = true;
    }
    
    private String usingDefaultRingtone() {
        String title = null;
        // user set default alarm ringtone
        Ringtone r = null;
        Uri defaultUri = RingtoneManager.getActualDefaultRingtoneUri(
                getContext(), RingtoneManager.TYPE_ALARM);
        if (isRingtoneExisted(getContext(), defaultUri)) {
            r = RingtoneManager.getRingtone(getContext(), defaultUri);
        }
        if (r != null) {
            title = r.getTitle(getContext());
        } else if (defaultUri != null) {
            // using system default alarm ringtone
            saveRingtone(DEFAULT_RINGTONE_URI);
            r = RingtoneManager.getRingtone(getContext(), DEFAULT_RINGTONE_URI);
            title = r.getTitle(getContext());
        }
        return title;
    }

   /**
     *M: to check if the ringtone media file is removed from SD-card or not.
     * 
     * @param ringtoneUri
     * @return
     */
    protected static boolean isRingtoneExisted(Context ctx, Uri ringtoneUri) {
        boolean result = false;
        Cursor c = null;
        ContentResolver cr = ctx.getContentResolver();
        if (ringtoneUri != null) {
            try {
                if (ringtoneUri.toString().contains("internal")) {
                    return true;
                }
                c = cr.query(ringtoneUri, null, null, null, null);
                if (c != null && c.getCount() > 0 && c.moveToFirst()) {
                    String path = c.getString(c.getColumnIndex("_data"));
                    Log.v("default uri: " + path);
                    result = new File(path).exists();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return result;
    }

    private void saveRingtone(Uri ringtoneUri) {
        Settings.System.putString(getContext().getContentResolver(),
                Settings.System.ALARM_ALERT, ringtoneUri == null ? null
                        : ringtoneUri.toString());
    }
}
