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

package com.android.email;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.UUID;

public class Preferences {

    // Preferences file
    public static final String PREFERENCES_FILE = "AndroidMail.Main";

    // Preferences field names
    private static final String ACCOUNT_UUIDS = "accountUuids";
    private static final String ENABLE_DEBUG_LOGGING = "enableDebugLogging";
    private static final String ENABLE_EXCHANGE_LOGGING = "enableExchangeLogging";
    private static final String ENABLE_EXCHANGE_FILE_LOGGING = "enableExchangeFileLogging";
    private static final String INHIBIT_GRAPHICS_ACCELERATION = "inhibitGraphicsAcceleration";
    private static final String FORCE_ONE_MINUTE_REFRESH = "forceOneMinuteRefresh";
    private static final String ENABLE_STRICT_MODE = "enableStrictMode";
    private static final String DEVICE_UID = "deviceUID";
    private static final String ONE_TIME_INITIALIZATION_PROGRESS = "oneTimeInitializationProgress";
    private static final String AUTO_ADVANCE_DIRECTION = "autoAdvance";
    private static final String TEXT_ZOOM = "textZoom";
    private static final String BACKGROUND_ATTACHMENTS = "backgroundAttachments";
    private static final String TRUSTED_SENDERS = "trustedSenders";
    private static final String LOW_STORAGE = "isLowStorage";
    private static long sStorageOkSize = 0;
    private static final String LAST_ACCOUNT_USED = "lastAccountUsed";
    private static final String REQUIRE_MANUAL_SYNC_DIALOG_SHOWN = "requireManualSyncDialogShown";
    /**M: Support for VIP settings @{*/
    private static final String VIP_NOTIFICATION = "vip_notification";
    private static final String VIP_RINGTONE = "vip_ringtone";
    private static final String VIP_VIBATATE = "vip_vibarate";
    private static final String RINGTONE_DEFAULT = "content://settings/system/notification_sound";
    /** @} */

    /**
     * M: Support for auto-clear cache @{
     */
    private static final String AUTO_CLEAR_CACHE = "auto_clear_cache";
    private static final boolean AUTO_CLEAR_CACHE_DEFAULT = true;
    /** @} */

    public static final int AUTO_ADVANCE_NEWER = 0;
    public static final int AUTO_ADVANCE_OLDER = 1;
    public static final int AUTO_ADVANCE_MESSAGE_LIST = 2;
    // "move to older" was the behavior on older versions.
    private static final int AUTO_ADVANCE_DEFAULT = AUTO_ADVANCE_OLDER;

    // The following constants are used as offsets into R.array.general_preference_text_zoom_size.
    public static final int TEXT_ZOOM_TINY = 0;
    public static final int TEXT_ZOOM_SMALL = 1;
    public static final int TEXT_ZOOM_NORMAL = 2;
    public static final int TEXT_ZOOM_LARGE = 3;
    public static final int TEXT_ZOOM_HUGE = 4;
    // "normal" will be the default
    public static final int TEXT_ZOOM_DEFAULT = TEXT_ZOOM_LARGE;

    // Starting something new here:
    // REPLY_ALL is saved by the framework (CheckBoxPreference's parent, Preference).
    // i.e. android:persistent=true in general_preferences.xml
    public static final String REPLY_ALL = "reply_all";
    // Reply All Default - when changing this, be sure to update general_preferences.xml
    public static final boolean REPLY_ALL_DEFAULT = false;

    public static final String AUTO_DOWNLOAD_REMAINING = "auto_download_remaining";
    public static final boolean AUTO_DOWNLOAD_REMAINING_DEFAULT = false;
    //Always add account address to bcc on Compose a mail.
    public static final String BCC_MYSELF_KEY = "bcc_myself";
    public static final boolean BCC_MYSELF_DEFAULT = false;

    /// M: new feature, ask before deleting. @{
    public static final String ASK_BEFORE_DELETING_KEY = "ask_before_deleting";
    public static final boolean ASK_BEFORE_DELETING_DEFAULT = true;
    /// @}

    private static Preferences sPreferences;

    private final SharedPreferences mSharedPreferences;
    /// M: the maximum low storage threshold value, which defined by MTK's framework team.
    private static final long MAX_LOW_STORAGE_THRESHOLD = 50 * 1024 * 1024;

    /**
     * A set of trusted senders for whom images and external resources should automatically be
     * loaded for.
     * Lazilly created.
     */
    private HashSet<String> mTrustedSenders = null;
    /// M: context @{
    private Context mContext;
    /// @}

    private Preferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        /// M: context @{
        mContext = context;
        /// @}
    }

    /**
     * TODO need to think about what happens if this gets GCed along with the
     * Activity that initialized it. Do we lose ability to read Preferences in
     * further Activities? Maybe this should be stored in the Application
     * context.
     */
    public static synchronized Preferences getPreferences(Context context) {
        if (sPreferences == null) {
            sPreferences = new Preferences(context);
        }
        return sPreferences;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return getPreferences(context).mSharedPreferences;
    }

    public static String getLegacyBackupPreference(Context context) {
        return getPreferences(context).mSharedPreferences.getString(ACCOUNT_UUIDS, null);
    }

    public static void clearLegacyBackupPreference(Context context) {
        getPreferences(context).mSharedPreferences.edit().remove(ACCOUNT_UUIDS).apply();
    }

    public void setEnableDebugLogging(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_DEBUG_LOGGING, value).apply();
    }

    public boolean getEnableDebugLogging() {
        return mSharedPreferences.getBoolean(ENABLE_DEBUG_LOGGING, false);
    }

    public void setEnableExchangeLogging(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_EXCHANGE_LOGGING, value).apply();
    }

    public boolean getEnableExchangeLogging() {
        return mSharedPreferences.getBoolean(ENABLE_EXCHANGE_LOGGING, false);
    }

    public void setEnableExchangeFileLogging(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_EXCHANGE_FILE_LOGGING, value).apply();
    }

    public boolean getEnableExchangeFileLogging() {
        return mSharedPreferences.getBoolean(ENABLE_EXCHANGE_FILE_LOGGING, false);
    }

    public void setInhibitGraphicsAcceleration(boolean value) {
        mSharedPreferences.edit().putBoolean(INHIBIT_GRAPHICS_ACCELERATION, value).apply();
    }

    public boolean getInhibitGraphicsAcceleration() {
        return mSharedPreferences.getBoolean(INHIBIT_GRAPHICS_ACCELERATION, false);
    }

    public void setForceOneMinuteRefresh(boolean value) {
        mSharedPreferences.edit().putBoolean(FORCE_ONE_MINUTE_REFRESH, value).apply();
    }

    public boolean getForceOneMinuteRefresh() {
        return mSharedPreferences.getBoolean(FORCE_ONE_MINUTE_REFRESH, false);
    }

    public void setEnableStrictMode(boolean value) {
        mSharedPreferences.edit().putBoolean(ENABLE_STRICT_MODE, value).apply();
    }

    public boolean getEnableStrictMode() {
        return mSharedPreferences.getBoolean(ENABLE_STRICT_MODE, false);
    }

    public void setLowStorage(boolean value) {
        mSharedPreferences.edit().putBoolean(LOW_STORAGE, value).commit();
    }

    public boolean getLowStorage() {
        return mSharedPreferences.getBoolean(LOW_STORAGE, false);
    }
    /**M: Support for VIP settings @{*/
    public boolean getVipNotification() {
        return mSharedPreferences.getBoolean(VIP_NOTIFICATION, true);
    }

    public String getVipRingtone() {
        return mSharedPreferences.getString(VIP_RINGTONE, RINGTONE_DEFAULT);
    }

    public boolean getVipVebarate() {
        return mSharedPreferences.getBoolean(VIP_VIBATATE, false);
    }
    /** @} */

    /**
     * M: This function is used to avoid Email can not recover from low_storage state.
     * Because we could miss the DEVICE_STORAGE_OK or DEVICE_STORAGE_LOW broadcast in some unknown
     * cases.
     * @NOTE if needRecheck = true and is in low storage, we try to do some clear action to release
     * internal storage while device's in low storage, and check low storage again, if it's still in
     * low storage, we setLowStorage(true)
     * @param needRecheck
     */
    public void checkLowStorage() {
        if (!isLowStorage()) {
            setLowStorage(false);
            return;
        }

        // try to release some space
        // we try to delete attachment files in messages before
        // @NOTE: we don't need to care about attachments in outbox/sentbox/draftbox, cause they'll
        // be cleared automatically after the message has been sent
        Controller.getInstance(mContext).deleteCachedAttachmentsDaysBefore(0);
        // re-check
        if (isLowStorage()) {
            setLowStorage(true);
            return;
        } else {
            setLowStorage(false);
            return;
        }
    }

    /**
     * M: check whether device's in low storage status
     * @return
     */
    private boolean isLowStorage() {
        String storageDirectory = Environment.getDataDirectory().toString();
        StatFs stat = new StatFs(storageDirectory);
        long availableBlocks = stat.getAvailableBlocks();
        long blockSize = stat.getBlockSize();
        long blockCount = stat.getBlockCount();
        long remaining = availableBlocks * blockSize;

        /**
         * M: Because the DEVICE_STORAGE_LOW broadcast will be sent when left
         * storage less 10%, and was limited by 50M, so set sStorageOkSize and
         * the LOW_STORAGE flag follow with the system to avoid conflict with
         * System. @{
         */
        if (0 == sStorageOkSize) {
            long minStorageSize = blockCount * blockSize / 10;
            sStorageOkSize = minStorageSize > MAX_LOW_STORAGE_THRESHOLD ? MAX_LOW_STORAGE_THRESHOLD
                    : minStorageSize;
        }
        if (Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AvailableBlocks: " + availableBlocks
                    + " BlockCount: " + blockCount + " BolckSize: " + blockSize
                    + " Data file system remaining size is " + remaining / 1024 + " KBytes");
        }

        return (remaining <= sStorageOkSize);
        /** @} */
    }

    /**
     * Generate a new "device UID".  This is local to Email app only, to prevent possibility
     * of correlation with any other user activities in any other apps.
     * @return a persistent, unique ID
     */
    public synchronized String getDeviceUID() {
         String result = mSharedPreferences.getString(DEVICE_UID, null);
         if (result == null) {
             result = UUID.randomUUID().toString();
             mSharedPreferences.edit().putString(DEVICE_UID, result).apply();
         }
         return result;
    }

    public int getOneTimeInitializationProgress() {
        return mSharedPreferences.getInt(ONE_TIME_INITIALIZATION_PROGRESS, 0);
    }

    public void setOneTimeInitializationProgress(int progress) {
        mSharedPreferences.edit().putInt(ONE_TIME_INITIALIZATION_PROGRESS, progress).apply();
    }

    public int getAutoAdvanceDirection() {
        return mSharedPreferences.getInt(AUTO_ADVANCE_DIRECTION, AUTO_ADVANCE_DEFAULT);
    }

    public void setAutoAdvanceDirection(int direction) {
        mSharedPreferences.edit().putInt(AUTO_ADVANCE_DIRECTION, direction).apply();
    }

    public int getTextZoom() {
        return mSharedPreferences.getInt(TEXT_ZOOM, TEXT_ZOOM_DEFAULT);
    }

    public void setTextZoom(int zoom) {
        mSharedPreferences.edit().putInt(TEXT_ZOOM, zoom).apply();
    }

    public boolean getAutoDownloadRemaining() {
        return mSharedPreferences.getBoolean(AUTO_DOWNLOAD_REMAINING,
                AUTO_DOWNLOAD_REMAINING_DEFAULT);
    }

    public boolean getBackgroundAttachments() {
        return mSharedPreferences.getBoolean(BACKGROUND_ATTACHMENTS, false);
    }

    public void setBackgroundAttachments(boolean allowed) {
        mSharedPreferences.edit().putBoolean(BACKGROUND_ATTACHMENTS, allowed).apply();
    }

    public boolean isBccMyself() {
        return mSharedPreferences.getBoolean(BCC_MYSELF_KEY, BCC_MYSELF_DEFAULT);
    }

    public void setBccMyself(boolean bccMyself) {
        mSharedPreferences.edit().putBoolean(BCC_MYSELF_KEY, bccMyself).apply();
    }

    /// M: support ask before deleting new feature
    public boolean isAskBeforeDelete() {
        return mSharedPreferences.getBoolean(ASK_BEFORE_DELETING_KEY,
                ASK_BEFORE_DELETING_DEFAULT);
    }

    /// M: support ask before deleting new feature
    public void setAskBeforeDelete(boolean ask) {
        mSharedPreferences.edit().putBoolean(ASK_BEFORE_DELETING_KEY, ask)
                .apply();
    }

    /**
     * M: Support for auto-clear cache
     */
    public boolean getAutoClearCache() {
        return mSharedPreferences.getBoolean(AUTO_CLEAR_CACHE, AUTO_CLEAR_CACHE_DEFAULT);
    }

    /**
     * M: Support for auto-clear cache @{
     */
    public void setAutoClearCache(boolean auto) {
        mSharedPreferences.edit().putBoolean(AUTO_CLEAR_CACHE, auto).apply();
    }

    /**
     * Determines whether or not a sender should be trusted and images should automatically be
     * shown for messages by that sender.
     */
    public boolean shouldShowImagesFor(String email) {
        if (mTrustedSenders == null) {
            try {
                mTrustedSenders = parseEmailSet(mSharedPreferences.getString(TRUSTED_SENDERS, ""));
            } catch (JSONException e) {
                // Something went wrong, and the data is corrupt. Just clear it to be safe.
                Log.w(Logging.LOG_TAG, "Trusted sender set corrupted. Clearing");
                mSharedPreferences.edit().putString(TRUSTED_SENDERS, "").apply();
                mTrustedSenders = new HashSet<String>();
            }
        }
        return mTrustedSenders.contains(email);
    }

    /**
     * Marks a sender as trusted so that images from that sender will automatically be shown.
     */
    public void setSenderAsTrusted(String email) {
        if (!mTrustedSenders.contains(email)) {
            mTrustedSenders.add(email);
            mSharedPreferences
                    .edit()
                    .putString(TRUSTED_SENDERS, packEmailSet(mTrustedSenders))
                    .apply();
        }
    }

    /**
     * Clears all trusted senders asynchronously.
     */
    public void clearTrustedSenders() {
        mTrustedSenders = new HashSet<String>();
        mSharedPreferences
                .edit()
                .putString(TRUSTED_SENDERS, packEmailSet(mTrustedSenders))
                .apply();
    }

    HashSet<String> parseEmailSet(String serialized) throws JSONException {
        HashSet<String> result = new HashSet<String>();
        if (!TextUtils.isEmpty(serialized)) {
            JSONArray arr = new JSONArray(serialized);
            for (int i = 0, len = arr.length(); i < len; i++) {
                result.add((String) arr.get(i));
            }
        }
        return result;
    }

    String packEmailSet(HashSet<String> set) {
        return new JSONArray(set).toString();
    }

    /**
     * Returns the last used account ID as set by {@link #setLastUsedAccountId}.
     * The system makes no attempt to automatically track what is considered a "use" - clients
     * are expected to call {@link #setLastUsedAccountId} manually.
     *
     * Note that the last used account may have been deleted in the background so there is also
     * no guarantee that the account exists.
     */
    public long getLastUsedAccountId() {
        return mSharedPreferences.getLong(LAST_ACCOUNT_USED, Account.NO_ACCOUNT);
    }

    /**
     * Sets the specified ID of the last account used. Treated as an opaque ID and does not
     * validate the value. Value is saved asynchronously.
     */
    public void setLastUsedAccountId(long accountId) {
        mSharedPreferences
                .edit()
                .putLong(LAST_ACCOUNT_USED, accountId)
                .apply();
    }

    /**
     * Gets whether the require manual sync dialog has been shown for the specified account.
     * It should only be shown once per account.
     */
    public boolean getHasShownRequireManualSync(Context context, Account account) {
        return getBoolean(context, account.getEmailAddress(), REQUIRE_MANUAL_SYNC_DIALOG_SHOWN,
                false);
    }

    /**
     * Sets whether the require manual sync dialog has been shown for the specified account.
     * It should only be shown once per account.
     */
    public void setHasShownRequireManualSync(Context context, Account account, boolean value) {
        setBoolean(context, account.getEmailAddress(), REQUIRE_MANUAL_SYNC_DIALOG_SHOWN, value);
    }


    /**
     * Get whether to show the manual sync dialog. This dialog is shown when the user is roaming,
     * connected to a mobile network, the administrator has set the RequireManualSyncWhenRoaming
     * flag to true, and the dialog has not been shown before for the supplied account.
     */
    public boolean shouldShowRequireManualSync(Context context, Account account) {
        return Account.isAutomaticSyncDisabledByRoaming(context, account.mId)
                && !getHasShownRequireManualSync(context, account);
    }

    public void clear() {
        mSharedPreferences.edit().clear().apply();
    }

    public void dump() {
        if (Logging.LOGD) {
            for (String key : mSharedPreferences.getAll().keySet()) {
                Log.v(Logging.LOG_TAG, key + " = " + mSharedPreferences.getAll().get(key));
            }
        }
    }

    /**
     * Utility method for setting a boolean value on a per-account preference.
     */
    private void setBoolean(Context context, String account, String key, Boolean value) {
        mSharedPreferences.edit().putBoolean(makeKey(account, key), value).apply();
    }

    /**
     * Utility method for getting a boolean value from a per-account preference.
     */
    private boolean getBoolean(Context context, String account, String key, boolean def) {
        return mSharedPreferences.getBoolean(makeKey(account, key), def);
    }

    /**
     * Utility method for creating a per account preference key.
     */
    private String makeKey(String account, String key) {
        return account != null ? account + "-" + key : key;
    }
}
