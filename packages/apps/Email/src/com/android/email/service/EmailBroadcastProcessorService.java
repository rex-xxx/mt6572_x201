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

package com.android.email.service;

import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.android.email.Controller;
import com.android.email.Email;
import com.android.email.NotificationController;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.SecurityPolicy;
import com.android.email.VendorPolicyLoader;
import com.android.email.activity.MessageListItem;
import com.android.email.activity.setup.AccountSettings;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.HostAuth;
/**
 * The service that really handles broadcast intents on a worker thread.
 *
 * We make it a service, because:
 * <ul>
 *   <li>So that it's less likely for the process to get killed.
 *   <li>Even if it does, the Intent that have started it will be re-delivered by the system,
 *   and we can start the process again.  (Using {@link #setIntentRedelivery}).
 * </ul>
 *
 * This also handles the DeviceAdminReceiver in SecurityPolicy, because it is also
 * a BroadcastReceiver and requires the same processing semantics.
 */
public class EmailBroadcastProcessorService extends IntentService {
    // Action used for BroadcastReceiver entry point
    private static final String ACTION_BROADCAST = "broadcast_receiver";

    // Dialing "*#*#36245#*#*" to open the debug screen.   "36245" = "email"
    private static final String ACTION_SECRET_CODE = "android.provider.Telephony.SECRET_CODE";
    private static final String SECRET_CODE_HOST_DEBUG_SCREEN = "36245";

    // This is a helper used to process DeviceAdminReceiver messages
    private static final String ACTION_DEVICE_POLICY_ADMIN = "com.android.email.devicepolicy";
    private static final String EXTRA_DEVICE_POLICY_ADMIN = "message_code";
    private static final String EMIAL_PACKAGE_NAME = "com.android.email";
    
    private static final int NO_UNREAD_MAIL = 0;
    private final Preferences mPref = Preferences.getPreferences(this);
    private final Handler mHandler = new Handler();

    /** M: use for testing  @{ */
    public static boolean mEmailBroadcastServiceFinished = false;
    
    public EmailBroadcastProcessorService() {
        // Class name will be the thread name.
        super(EmailBroadcastProcessorService.class.getName());

        // Intent should be redelivered if the process gets killed before completing the job.
        setIntentRedelivery(true);
    }

    /**
     * Entry point for {@link EmailBroadcastReceiver}.
     */
    public static void processBroadcastIntent(Context context, Intent broadcastIntent) {
        Intent i = new Intent(context, EmailBroadcastProcessorService.class);
        i.setAction(ACTION_BROADCAST);
        i.putExtra(Intent.EXTRA_INTENT, broadcastIntent);
        context.startService(i);
    }

    /**
     * Entry point for {@link com.android.email.SecurityPolicy.PolicyAdmin}.  These will
     * simply callback to {@link
     * com.android.email.SecurityPolicy#onDeviceAdminReceiverMessage(Context, int)}.
     */
    public static void processDevicePolicyMessage(Context context, int message) {
        Intent i = new Intent(context, EmailBroadcastProcessorService.class);
        i.setAction(ACTION_DEVICE_POLICY_ADMIN);
        i.putExtra(EXTRA_DEVICE_POLICY_ADMIN, message);
        context.startService(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // This method is called on a worker thread.
        Cursor c = null;
        // Dispatch from entry point
        final String action = intent.getAction();
        try {
            if (ACTION_BROADCAST.equals(action)) {
                final Intent broadcastIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                final String broadcastAction = broadcastIntent.getAction();
                final String broadcastPackageName = broadcastIntent.getStringExtra("packageName");

                if (Intent.ACTION_BOOT_COMPLETED.equals(broadcastAction)) {
                    onBootCompleted();
                    // Force policies to be set in DPM
                    SecurityPolicy.getInstance(this);
                // TODO: Do a better job when we get ACTION_DEVICE_STORAGE_LOW.
                //       The code below came from very old code....
                } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(broadcastAction)) {
                    /**
                     * M: try to release some space to prevent from stopping @{
                     */
                    Log.w(Logging.LOG_TAG,
                            "Receive STORAGE_LOW broadcast , and Email try to release space");
                    mPref.checkLowStorage();
                    // it indeed go into low storage
                    if (mPref.getLowStorage()) {
                        Log.w(Logging.LOG_TAG,
                                "Still STORAGE_LOW , and Email will stop work");
                        c = getContentResolver().query(Account.CONTENT_URI,
                                Account.ID_PROJECTION, null, null, null);
                        if (c.getCount() > 0) {
                            mHandler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(EmailBroadcastProcessorService.this,
                                            R.string.low_storage_service_stop,
                                            Toast.LENGTH_LONG).show();
                                    Toast.makeText(EmailBroadcastProcessorService.this,
                                            R.string.low_storage_hint_delete_mail,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        Controller.getInstance(this).stopEmailService(this);
                        mEmailBroadcastServiceFinished = true;
                    }
                    /** @} */
                } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(broadcastAction)) {
                    Log.w(Logging.LOG_TAG,
                            "Receive STORAGE_OK broadcast , and Email will start work");
                    mPref.setLowStorage(false);
                    c = getContentResolver().query(Account.CONTENT_URI,
                            Account.ID_PROJECTION, null, null, null);
                    if (c.getCount() > 0) {
                        mHandler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(EmailBroadcastProcessorService.this,
                                        R.string.low_storage_service_start,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    enableComponentsIfNecessary();
                    mEmailBroadcastServiceFinished = true;
                } else if (ACTION_SECRET_CODE.equals(broadcastAction)
                        && SECRET_CODE_HOST_DEBUG_SCREEN
                                .equals(broadcastIntent.getData().getHost())) {
                    AccountSettings.actionSettingsWithDebug(this);
                    mEmailBroadcastServiceFinished = true;
                } else if (AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION.equals(broadcastAction)) {
                    onSystemAccountChanged();
                    mEmailBroadcastServiceFinished = true;
                    /** M: MTK Dependence @} */
                } else if (Intent.ACTION_SETTINGS_PACKAGE_DATA_CLEARED.equals(broadcastAction)
                        && EMIAL_PACKAGE_NAME.equals(broadcastPackageName)) {
                    NotificationController.notifyEmailUnreadNumber(this, NO_UNREAD_MAIL);
                    mEmailBroadcastServiceFinished = true;
                    /// M: Add for handling the theme change
                } else if(Intent.ACTION_SKIN_CHANGED.equals(broadcastAction)) {
                    MessageListItem.resetDrawingCaches();
                }
                /** @} */
            } else if (ACTION_DEVICE_POLICY_ADMIN.equals(action)) {
                int message = intent.getIntExtra(EXTRA_DEVICE_POLICY_ADMIN, -1);
                SecurityPolicy.onDeviceAdminReceiverMessage(this, message);
                mEmailBroadcastServiceFinished = true;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        
    }

    private void enableComponentsIfNecessary() {
        if (Email.setServicesEnabledSync(this)) {
            // At least one account exists.
            // TODO probably we should check if it's a POP/IMAP account.
            MailService.actionReschedule(this);
            /// M: For clear cache @{
            MailService.actionRescheduleClearOldAttachment(this);
            /// @}
        }

        // Starts the service for Exchange, if supported.
        EmailServiceUtils.startExchangeService(this);
    }

    /**
     * Handles {@link Intent#ACTION_BOOT_COMPLETED}.  Called on a worker thread.
     */
    private void onBootCompleted() {
        performOneTimeInitialization();
        int unreadCount = Mailbox.getUnreadCountByMailboxType(this, Mailbox.TYPE_INBOX);
        NotificationController.notifyEmailUnreadNumber(this, unreadCount);
        if (!mPref.getLowStorage()) {
            enableComponentsIfNecessary();
            /// M: notify new messages arrived in last life time when this life time begins
            // this should be called after calling enableComponentsIfNecessary() cause some
            // initialization process proceeded in enableComponentsIfNecessary() @{
            NotificationController.notifyOnBootCompleted();
            /// @}
        }
        mEmailBroadcastServiceFinished = true;
    }

    private void performOneTimeInitialization() {
        int progress = mPref.getOneTimeInitializationProgress();
        final int initialProgress = progress;

        if (progress < 1) {
            Log.i(Logging.LOG_TAG, "Onetime initialization: 1");
            progress = 1;
            if (VendorPolicyLoader.getInstance(this).useAlternateExchangeStrings()) {
                setComponentEnabled(EasAuthenticatorServiceAlternate.class, true);
                setComponentEnabled(EasAuthenticatorService.class, false);
            }
        }

        if (progress < 2) {
            Log.i(Logging.LOG_TAG, "Onetime initialization: 2");
            progress = 2;
            setImapDeletePolicy(this);
        }

        // Add your initialization steps here.
        // Use "progress" to skip the initializations that's already done before.
        // Using this preference also makes it safe when a user skips an upgrade.  (i.e. upgrading
        // version N to version N+2)

        if (progress != initialProgress) {
            mPref.setOneTimeInitializationProgress(progress);
            Log.i(Logging.LOG_TAG, "Onetime initialization: completed.");
        }
    }

    /**
     * Sets the delete policy to the correct value for all IMAP accounts. This will have no
     * effect on either EAS or POP3 accounts.
     */
    /*package*/ static void setImapDeletePolicy(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(Account.CONTENT_URI, Account.CONTENT_PROJECTION,
                null, null, null);
        try {
            while (c.moveToNext()) {
                long recvAuthKey = c.getLong(Account.CONTENT_HOST_AUTH_KEY_RECV_COLUMN);
                HostAuth recvAuth = HostAuth.restoreHostAuthWithId(context, recvAuthKey);
                if (recvAuth != null && HostAuth.SCHEME_IMAP.equals(recvAuth.mProtocol)) {
                    int flags = c.getInt(Account.CONTENT_FLAGS_COLUMN);
                    flags &= ~Account.FLAGS_DELETE_POLICY_MASK;
                    flags |= Account.DELETE_POLICY_ON_DELETE << Account.FLAGS_DELETE_POLICY_SHIFT;
                    ContentValues cv = new ContentValues();
                    cv.put(AccountColumns.FLAGS, flags);
                    long accountId = c.getLong(Account.CONTENT_ID_COLUMN);
                    Uri uri = ContentUris.withAppendedId(Account.CONTENT_URI, accountId);
                    resolver.update(uri, cv, null, null);
                }
            }
        } finally {
            c.close();
        }
    }

    private void setComponentEnabled(Class<?> clazz, boolean enabled) {
        final ComponentName c = new ComponentName(this, clazz.getName());
        getPackageManager().setComponentEnabledSetting(c,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void onSystemAccountChanged() {
        Log.i(Logging.LOG_TAG, "System accounts updated.");
        MailService.reconcilePopImapAccountsSync(this);

        // If the exchange service wasn't already running, starting it will cause exchange account
        // reconciliation to be performed.  The service stops itself it there are no EAS accounts.
        EmailServiceUtils.startExchangeService(this);
    }

}
