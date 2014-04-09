/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.contacts;

import android.app.Application;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

/// The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.test.InjectedServices;
import com.android.contacts.util.Constants;
import com.android.contacts.vcard.VCardService;
import com.google.common.annotations.VisibleForTesting;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.calloption.SimAssociateHandler;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.list.ContactsGroupMultiPickerFragment;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.phone.HyphonManager;
import com.mediatek.phone.SIMInfoWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/// The previous lines are provided and maintained by Mediatek Inc.

public final class ContactsApplication extends Application {
    private static final boolean ENABLE_LOADER_LOG = false; // Don't submit with true
    private static final boolean ENABLE_FRAGMENT_LOG = false; // Don't submit with true

    private static InjectedServices sInjectedServices;
    private AccountTypeManager mAccountTypeManager;
    private ContactPhotoManager mContactPhotoManager;
    private ContactListFilterController mContactListFilterController;

    private static ContactsApplication sMe;

    public static final boolean sDialerSearchSupport = FeatureOption.MTK_DIALER_SEARCH_SUPPORT;
    public static final boolean sSpeedDial = true;
    private static final String TAG = ContactsApplication.class.getSimpleName();

    /**
     * Overrides the system services with mocks for testing.
     */
    @VisibleForTesting
    public static void injectServices(InjectedServices services) {
        sInjectedServices = services;
    }

    public static InjectedServices getInjectedServices() {
        return sInjectedServices;
    }

    @Override
    public ContentResolver getContentResolver() {
        if (sInjectedServices != null) {
            ContentResolver resolver = sInjectedServices.getContentResolver();
            if (resolver != null) {
                return resolver;
            }
        }
        return super.getContentResolver();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (sInjectedServices != null) {
            SharedPreferences prefs = sInjectedServices.getSharedPreferences();
            if (prefs != null) {
                return prefs;
            }
        }

        return super.getSharedPreferences(name, mode);
    }

    @Override
    public Object getSystemService(String name) {
        if (sInjectedServices != null) {
            Object service = sInjectedServices.getSystemService(name);
            if (service != null) {
                return service;
            }
        }

        if (AccountTypeManager.ACCOUNT_TYPE_SERVICE.equals(name)) {
            if (mAccountTypeManager == null) {
                mAccountTypeManager = AccountTypeManager.createAccountTypeManager(this);
            }
            return mAccountTypeManager;
        }

        if (ContactPhotoManager.CONTACT_PHOTO_SERVICE.equals(name)) {
            if (mContactPhotoManager == null) {
                mContactPhotoManager = ContactPhotoManager.createContactPhotoManager(this);
                registerComponentCallbacks(mContactPhotoManager);
                mContactPhotoManager.preloadPhotosInBackground();
            }
            return mContactPhotoManager;
        }

        if (ContactListFilterController.CONTACT_LIST_FILTER_SERVICE.equals(name)) {
            if (mContactListFilterController == null) {
                mContactListFilterController = ContactListFilterController
                        .createContactListFilterController(this);
            }
            return mContactListFilterController;
        }

        return super.getSystemService(name);
    }

    public static ContactsApplication getInstance() {
        return sMe;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "ContactsApplication.onCreate start");
        }

        if (ENABLE_FRAGMENT_LOG) FragmentManager.enableDebugLogging(true);
        if (ENABLE_LOADER_LOG) LoaderManager.enableDebugLogging(true);

        if (Log.isLoggable(Constants.STRICT_MODE_TAG, Log.DEBUG)) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        }

        // Perform the initialization that doesn't have to finish immediately.
        // We use an async task here just to avoid creating a new thread.
        (new DelayedInitializer()).execute();

        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "ContactsApplication.onCreate finish");
        }

        sMe = this;

        /**
         * description : initialize CellConnService and SIM associate handler
         */
        SimAssociateHandler.getInstance(this).prepair();
        SimAssociateHandler.getInstance(this).load();

        cellConnMgr = new CellConnMgr();
        cellConnMgr.register(getApplicationContext());
        SIMInfoWrapper.getDefault().init(this);

        /// M: Add for ALPS00540397 @{
        //     Init the SimInfo when appication is being created.
        new Thread() {
            public void run() {
                SIMInfoWrapper.getDefault();
                Log.d("ContactsApplication" , "onCreate : SIMInfoWrapper.getInsertedSimCount()"
                        + SIMInfoWrapper.getDefault().getInsertedSimCount());
            }
        }.start();
        /// @}

        HyphonManager.getInstance().init(this);
        new Thread(new Runnable() {
            public void run() {
                long lStart = System.currentTimeMillis();
                HyphonManager.getInstance().formatNumber(TEST_NUMBER);
                Log.i(Constants.PERFORMANCE_TAG, " Thread HyphonManager formatNumber() use time :"
                        + (System.currentTimeMillis() - lStart));
            }
        }).start();

        /**
         * Bug Fix by Mediatek Begin. CR ID: ALPS00286964 Descriptions: Remove
         * all of notifications which owns to Contacts application.
         */
        final NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        /**
         * Bug Fix by Mediatek End.
         */
    }

    private class DelayedInitializer extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            final Context context = ContactsApplication.this;

            // Warm up the preferences, the account type manager and the
            // contacts provider.
            PreferenceManager.getDefaultSharedPreferences(context);
            AccountTypeManager.getInstance(context);
            getContentResolver().getType(ContentUris.withAppendedId(Contacts.CONTENT_URI, 1));
            return null;
        }

        public void execute() {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
    }

    // M: The following lines are provided and maintained by Mediatek Inc.
    public CellConnMgr cellConnMgr;

    protected String TEST_NUMBER = "10086";

    /**
     * Should be single thread, as we don't want to simultaneously handle
     * contacts copy-delete-import-export request.
     */
    public final ExecutorService singleTaskService = Executors.newSingleThreadExecutor();

    /**
     * M: when Contacts app is busy, some operations should be forbidden
     * 
     * @return true if there are time consuming operation running, such as:
     *         Multi-delete, import/export, SIMService running, group batch
     *         operation
     */
    public static boolean isContactsApplicationBusy() {
        boolean isMultiDeleting = MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE);
        boolean isMultiCopying = MultiChoiceService.isProcessing(MultiChoiceService.TYPE_COPY);
        boolean isVcardProcessing = VCardService.isProcessing(VCardService.TYPE_IMPORT);
        boolean isGroupMoving = ContactsGroupMultiPickerFragment.isMoveContactsInProcessing();
        boolean isGroupSavingInTransaction = ContactSaveService.isGroupTransactionProcessing();
        Log.i(TAG, "[isContactsApplicationBusy] multi-del: " + isMultiDeleting + ", multi-copy: " + isMultiCopying
                + ", vcard: " + isVcardProcessing + ", group-move: " + isGroupMoving + ", group-trans: "
                + isGroupSavingInTransaction);
        return (isMultiDeleting || isMultiCopying || isVcardProcessing || isGroupMoving || isGroupSavingInTransaction);
    }
    // M: The previous lines are provided and maintained by Mediatek Inc.
}
