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

package com.android.email.activity;

import java.util.Set;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.android.email.Email;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.RefreshManager;
import com.android.email.activity.setup.AccountSettings;
import com.android.email.activity.setup.AccountSetupBasics;
import com.android.email.activity.UiUtilities;
import com.android.email.service.EmailServiceUtils;
import com.android.email.service.MailService;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.IntentUtilities;
import com.android.emailcommon.utility.Utility;
import com.google.common.annotations.VisibleForTesting;

/**
 * The Welcome activity initializes the application and starts {@link EmailActivity}, or launch
 * {@link AccountSetupBasics} if no accounts are configured.
 *
 * TOOD Show "your messages are on the way" message like gmail does during the inbox lookup.
 */
public class Welcome extends Activity {
    /*
     * Commands for testing...
     *  Open 1 pane
        adb shell am start -a android.intent.action.MAIN \
            -d '"content://ui.email.android.com/view/mailbox"' \
            -e DEBUG_PANE_MODE 1

     *  Open 2 pane
        adb shell am start -a android.intent.action.MAIN \
            -d '"content://ui.email.android.com/view/mailbox"' \
            -e DEBUG_PANE_MODE 2

     *  Open an account (ID=1) in 2 pane
        adb shell am start -a android.intent.action.MAIN \
            -d '"content://ui.email.android.com/view/mailbox?ACCOUNT_ID=1"' \
            -e DEBUG_PANE_MODE 2

     *  Open a message (account id=1, mailbox id=2, message id=3)
        adb shell am start -a android.intent.action.MAIN \
            -d '"content://ui.email.android.com/view/mailbox?\
            ACCOUNT_ID=1&MAILBOX_ID=2&MESSAGE_ID=3"' \
            -e DEBUG_PANE_MODE 2

     *  Open the combined starred on the combined view
        adb shell am start -a android.intent.action.MAIN \
            -d '"content://ui.email.android.com/view/mailbox?\
            ACCOUNT_ID=1152921504606846976&MAILBOX_ID=-4"' \
            -e DEBUG_PANE_MODE 2
     */

    /**
     * Extra for debugging.  Set 1 to force one-pane.  Set 2 to force two-pane.
     */
    private static final String EXTRA_DEBUG_PANE_MODE = "DEBUG_PANE_MODE";

    private static final String EXTRA_FROM_ACCOUNT_MANAGER = "FROM_ACCOUNT_MANAGER";

    public static final String VIEW_MAILBOX_INTENT_URL_PATH = "/view/mailbox";

    private static final String TAG = "Welcome";

    private final EmailAsyncTask.Tracker mTaskTracker = new EmailAsyncTask.Tracker();

    private View mWaitingForSyncView;

    private long mAccountId;
    private long mMailboxId;
    private long mMessageId;
    private String mAccountUuid;
    private boolean mIsResumed;
    private boolean mNeedStartEmailActivity;
    private boolean mNeedResolveAccount;

    private MailboxFinder mInboxFinder;

    private boolean mFindInboxAndFinish = false;
    /// M: define a delay time to avoid the FindMailboxTask executing too frequently.
    private static final int MAILBOX_FINDER_EXECUTE_DELAY = 5000;
    private int mMailboxFinderDelayTime = 0;
    private Welcome mContext;
    /**
     * Launch this activity.  Note:  It's assumed that this activity is only called as a means to
     * 'reset' the UI state; Because of this, it is always launched with FLAG_ACTIVITY_CLEAR_TOP,
     * which will drop any other activities on the stack (e.g. AccountFolderList or MessageList).
     */
    public static void actionStart(Activity fromActivity) {
        Intent i = IntentUtilities.createRestartAppIntent(fromActivity, Welcome.class);
        fromActivity.startActivity(i);
    }

    /**
     * Create an Intent to open email activity. If <code>accountId</code> is not -1, the
     * specified account will be automatically be opened when the activity starts.
     */
    public static Intent createOpenAccountInboxIntent(Context context, long accountId) {
        final Uri.Builder b = IntentUtilities.createActivityIntentUrlBuilder(
                VIEW_MAILBOX_INTENT_URL_PATH);
        IntentUtilities.setAccountId(b, accountId);
        return IntentUtilities.createRestartAppIntent(b.build());
    }

    /**
     * Create an Intent to open a message.
     */
    public static Intent createOpenMessageIntent(Context context, long accountId,
            long mailboxId, long messageId) {
        final Uri.Builder b = IntentUtilities.createActivityIntentUrlBuilder(
                VIEW_MAILBOX_INTENT_URL_PATH);
        IntentUtilities.setAccountId(b, accountId);
        IntentUtilities.setMailboxId(b, mailboxId);
        IntentUtilities.setMessageId(b, messageId);
        return IntentUtilities.createRestartAppIntent(b.build());
    }

    /**
     * Open account's inbox.
     */
    public static void actionOpenAccountInbox(Activity fromActivity, long accountId) {
        fromActivity.startActivity(createOpenAccountInboxIntent(fromActivity, accountId));
    }

    /**
     * Sync account's inbox and finish.
     */
    public static void actionSyncInboxAndFinish(Activity fromActivity, long accountId) {
        Intent i = createOpenAccountInboxIntent(fromActivity, accountId);
        i.putExtra(EXTRA_FROM_ACCOUNT_MANAGER, true);
        fromActivity.startActivity(i);
    }

    /**
     * Create an {@link Intent} for account shortcuts.  The returned intent stores the account's
     * UUID rather than the account ID, which will be changed after account restore.
     */
    public static Intent createAccountShortcutIntent(Context context, String uuid, long mailboxId) {
        final Uri.Builder b = IntentUtilities.createActivityIntentUrlBuilder(
                VIEW_MAILBOX_INTENT_URL_PATH);
        IntentUtilities.setAccountUuid(b, uuid);
        IntentUtilities.setMailboxId(b, mailboxId);
        return IntentUtilities.createRestartAppIntent(b.build());
    }

    /**
     * If the {@link #EXTRA_DEBUG_PANE_MODE} extra is "1" or "2", return 1 or 2 respectively.
     * Otherwise return 0.
     *
     * @see UiUtilities#setDebugPaneMode(int)
     * @see UiUtilities#useTwoPane(Context)
     */
    private static int getDebugPaneMode(Intent i) {
        Bundle extras = i.getExtras();
        if (extras != null) {
            String s = extras.getString(EXTRA_DEBUG_PANE_MODE);
            if ("1".equals(s)) {
                return 1;
            } else if ("2".equals(s)) {
                return 2;
            }
        }
        return 0;
    }

    @Override
    public void onCreate(Bundle icicle) {
        Logging.d("[Performance test][Email] MessageList FPS test start time ["
                + System.currentTimeMillis() + "]");
        super.onCreate(icicle);

        /** M: finish this activity when it wasn't the root activity and was launched
         * by Launcher to avoid it clear all activity which has existed in the activity task.
         * Several scenarios could cause this case:
         * e.g: User launch email by click notification item,Or launch email from EmailWidget.
         * open MessageCompose activity, and press home key. After that, user launch email
         * by Launcher's icon. The MessageCompose activity would be removed
         * root cause: The intent was different between notification and launcher.
         * notification's intent contain uri data part, but launcher's not. So AMS would start
         * a new Welcome activity on task top instead of moving whole task to front. @{*/
        final Intent intent = getIntent();
        Set<String> categories = intent.getCategories();
        if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER) && !isTaskRoot()) {
            finish();
            return;
        }
        /** @}*/

        ActivityHelper.debugSetWindowFlags(this);

        // Because the app could be reloaded (for debugging, etc.), we need to make sure that
        // ExchangeService gets a chance to start.  There is no harm to starting it if it has
        // already been started
        // When the service starts, it reconciles EAS accounts.
        // TODO More completely separate ExchangeService from Email app
        EmailServiceUtils.startExchangeService(this);

        mContext = this;
        // Extract parameters from the intent.
        mAccountId = IntentUtilities.getAccountIdFromIntent(intent);
        mMailboxId = IntentUtilities.getMailboxIdFromIntent(intent);
        mMessageId = IntentUtilities.getMessageIdFromIntent(intent);
        mAccountUuid = IntentUtilities.getAccountUuidFromIntent(intent);
        UiUtilities.setDebugPaneMode(getDebugPaneMode(intent));
        if (intent.getExtras() != null) {
            mFindInboxAndFinish = intent.getExtras().containsKey(
                    EXTRA_FROM_ACCOUNT_MANAGER);
        }

        // M: For launch performance, run this method on the main thread.
        resolveAccount();

        // Reset the "accounts changed" notification, now that we're here
        Email.setNotifyUiAccountsChanged(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only create the menu if we had to stop and show a loading spinner - otherwise
        // this is a transient activity with no UI.
        if (mInboxFinder == null) {
            return super.onCreateOptionsMenu(menu);
        }

        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.account_settings) {
            AccountSettings.actionSettings(this, mAccountId);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onResume");
        }
        super.onResume();
        mIsResumed = true;
        if (mNeedStartEmailActivity) {
            startEmailActivity();
            mNeedStartEmailActivity = false;
        }
        if (mNeedResolveAccount) {
            mMailboxFinderDelayTime = MAILBOX_FINDER_EXECUTE_DELAY;
            resolveAccount();
            mNeedResolveAccount = false;
        }
    }

    @Override
    protected void onPause() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, this + " onResume");
        }
        mIsResumed = false;
        super.onPause();
    }

    /**
     * {@inheritDoc}
     *
     * When launching an activity from {@link Welcome}, we always want to set
     * {@link Intent#FLAG_ACTIVITY_FORWARD_RESULT}.
     */
    @Override
    public void startActivity(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        /// M: Disable activity transmission animation(upon API 16).
        ActivityOptions ops = ActivityOptions.makeCustomAnimation(this, 0, 0);
        super.startActivity(intent, ops.toBundle());
    }

    /**
     * Stop inbox lookup.  This MSUT be called on the UI thread.
     */
    private void stopInboxLookup() {
        if (mInboxFinder != null) {
            mInboxFinder.cancel();
            mInboxFinder = null;
        }
    }

    /**
     * Start inbox lookup.  This MSUT be called on the UI thread.
     */
    private void startInboxLookup() {
        Log.i(Logging.LOG_TAG, "Inbox not found.  Starting mailbox finder...");
        stopInboxLookup(); // Stop if already running -- it shouldn't be but just in case.
        Preferences pref = Preferences.getPreferences(this);
        
        mInboxFinder = new MailboxFinder(this, mAccountId, Mailbox.TYPE_INBOX,
                mMailboxFinderCallback, mMailboxFinderDelayTime);
        mMailboxFinderDelayTime = 0;
        if (pref.getLowStorage()) {
            Toast.makeText(this, R.string.low_storage_service_stop, Toast.LENGTH_LONG).show();
            Toast.makeText(this, R.string.low_storage_hint_delete_mail, Toast.LENGTH_LONG).show();
            Logging.d("checkIsLowStorage canceled startInboxLookup due to low storage");
            Preferences.getPreferences(this).setLastUsedAccountId(Account.NO_ACCOUNT);
            return;
        } else {
            mInboxFinder.startLookup();
            // if Welcome was started by AccountManager, We just start InboxFinder and finish.
            if (mFindInboxAndFinish) {
                finish();
                mFindInboxAndFinish = false;
                return;
            }
            // Show "your email will appear shortly" message.
            /// M: Need to check whether the mWaitingForSyncView is null.
            if (mWaitingForSyncView == null) {
                mWaitingForSyncView = LayoutInflater.from(this).inflate(
                        R.layout.waiting_for_sync_message, null);
                addContentView(mWaitingForSyncView, new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }
        }

        invalidateOptionsMenu();
    }

    /**
     * Determine which account to open with the given account ID and UUID.
     *
     * @return ID of the account to use.
     */
    @VisibleForTesting
    static long resolveAccountId(Context context, long inputAccountId, String inputUuid) {
        final long accountId;

        if (!TextUtils.isEmpty(inputUuid)) {
            // If a UUID is specified, try to use it.
            // If the UUID is invalid, accountId will be NO_ACCOUNT.
            accountId = Account.getAccountIdFromUuid(context, inputUuid);

        } else if (inputAccountId != Account.NO_ACCOUNT) {
            // If a valid account ID is specified, just use it.
            if (inputAccountId == Account.ACCOUNT_ID_COMBINED_VIEW
                    || Account.isValidId(context, inputAccountId)) {
                accountId = inputAccountId;
            } else {
                accountId = Account.NO_ACCOUNT;
            }
        } else {
            // Neither an accountID or a UUID is specified.
            // Use the last account used, falling back to the default.
            long lastUsedId = Preferences.getPreferences(context).getLastUsedAccountId();
            if (lastUsedId != Account.NO_ACCOUNT) {
                if (!Account.isValidId(context, lastUsedId)) {
                    // The last account that was used has since been deleted.
                    lastUsedId = Account.NO_ACCOUNT;
                    Preferences.getPreferences(context).setLastUsedAccountId(Account.NO_ACCOUNT);
                }
            }
            accountId = (lastUsedId == Account.NO_ACCOUNT)
                    ? Account.getDefaultAccountId(context)
                    : lastUsedId;
        }
        if (accountId != Account.NO_ACCOUNT) {
            // Okay, the given account is valid.
            Logging.d(TAG, " inputAccountId is  " + inputAccountId + "," +
                    " the given account is valid " + accountId );
            return accountId;
        } else {
            // No, it's invalid.  Show the warning toast and use the default.
            Utility.showToast(context, R.string.toast_account_not_found);
            Logging.d(TAG, " inputAccountId is  " + inputAccountId + " not found," +
                    " return the default account ");
            return Account.getDefaultAccountId(context);
        }
    }

    /**
     * Determine which account to use according to the number of accounts already set up,
     * {@link #mAccountId} and {@link #mAccountUuid}.
     *
     * <pre>
     * 1. If there's no account configured, start account setup.
     * 2. Otherwise detemine which account to open with {@link #resolveAccountId} and
     *   2a. If the account doesn't have inbox yet, start inbox finder.
     *   2b. Otherwise open the main activity.
     * </pre>
     */
    private void resolveAccount() {
        final int numAccount = EmailContent.count(this, Account.CONTENT_URI);
        if (numAccount == 0) {
            //There's no account configured, start account setup.
            Logging.d(TAG, "There's no account configured , start setup new account");
            AccountSetupBasics.actionNewAccount(this);
            finish();
            return;
        } else {
            mAccountId = resolveAccountId(this, mAccountId, mAccountUuid);
            if (Account.isNormalAccount(mAccountId)) {
                //The account doesn't have inbox yet, start inbox finder.
                if (Mailbox.findMailboxOfType(this, mAccountId,
                        Mailbox.TYPE_INBOX) == Mailbox.NO_MAILBOX) {
                    Logging.d(TAG, "resolveAccountId " + mAccountId + 
                            " doesn't have inbox start inbox finder.");
                    startInboxLookup();
                    return;
                }
            } else {
                //Still not find a normal account, start account setup
                if(mAccountId == Account.NO_ACCOUNT){
                    Logging.d(TAG, "resolveAccountId " + mAccountId + 
                            " is not a NormalAccount, start setup new account");
                    AccountSetupBasics.actionNewAccount(this);
                    finish();
                    return;
                }
            }
        }
        Logging.d(TAG, "resolveAccountId " + mAccountId + " startEmailActivity.");
        startEmailActivity();
    }

    /**
     * Start {@link EmailActivity} using {@link #mAccountId}, {@link #mMailboxId} and
     * {@link #mMessageId}.
     */
    private void startEmailActivity() {
        final Intent i;
        if (mMessageId != Message.NO_MESSAGE) {
            i = EmailActivity.createOpenMessageIntent(this, mAccountId, mMailboxId, mMessageId);
        } else if (mMailboxId != Mailbox.NO_MAILBOX) {
            if (Logging.LOG_PERFORMANCE) {
                Logging.d(Logging.LOG_TAG, 
                        "[Exchange Download Speed] Welcome:StartEmailActivity:ShowMessageList ["
                        + System.currentTimeMillis() + "]");
            }
            i = EmailActivity.createOpenMailboxIntent(this, mAccountId, mMailboxId);
        } else {
            /** M: start local search for quick search box's search request @{ */
            String action = mContext.getIntent().getAction();
            if (action != null && action.equals(Intent.ACTION_SEARCH)) {
                String query = mContext.getIntent().getStringExtra(SearchManager.QUERY);
                i = EmailActivity.createLocalSearchIntent(this, Account.ACCOUNT_ID_COMBINED_VIEW,
                        Mailbox.QUERY_ALL_INBOXES, query, null);
            } else {
                i = EmailActivity.createOpenAccountIntent(this, mAccountId);
            }
            /** @} */
        }
        startActivity(i);
        finish();
    }

    private final MailboxFinder.Callback mMailboxFinderCallback = new MailboxFinder.Callback() {
        // This MUST be called from callback methods.
        private void cleanUp() {
            mInboxFinder = null;
        }

        @Override
        public void onAccountNotFound() {
            cleanUp();
            // Account removed?  Clear the IDs and restart the task.  Which will result in either
            // a) show account setup if there's really no accounts  or b) open the default account.

            mMailboxId = Mailbox.NO_MAILBOX;
            mMessageId = Message.NO_MESSAGE;
            mAccountUuid = null;

            // Restart the account resolution.
            if (!Utility.hasConnectivity(mContext) && mIsResumed) {
                MyConnectionAlertDialog dialogFragment;
                FragmentManager fm = getFragmentManager();
                dialogFragment = (MyConnectionAlertDialog)fm.findFragmentByTag(MyConnectionAlertDialog.TAG);

                /// M: Recover and dismiss the progress dialog fragment
                if (dialogFragment != null) {
                    dialogFragment.dismissAllowingStateLoss();
                }
                dialogFragment = MyConnectionAlertDialog.newInstance(mContext);
                fm.beginTransaction().add(dialogFragment, MyConnectionAlertDialog.TAG)
                        .commitAllowingStateLoss();
            } else {
                resolveAccount();
            }
        }

        @Override
        public void onMailboxNotFound(long accountId) {
            // Just do the same thing as "account not found".
            onAccountNotFound();
        }

        @Override
        public void onAccountSecurityHold(long accountId) {
            cleanUp();

            ActivityHelper.showSecurityHoldDialog(Welcome.this, accountId);
            finish();
        }

        @Override
        public void onMailboxFound(long accountId, long mailboxId) {
            cleanUp();
            mMailboxId = mailboxId;
            // register notification account.
            Email.setServicesEnabledSync(Welcome.this);
            // Okay the account has Inbox now.  Start the main activity.
            if (mIsResumed) {
                startEmailActivity();
            }else {
                mNeedStartEmailActivity = true;
                //update messagelist with server.
                RefreshManager rm = RefreshManager.getInstance(Welcome.this);
                rm.refreshMessageList(accountId, mailboxId, true);
                Preferences.getPreferences(Welcome.this).setLastUsedAccountId(accountId);
            }
        }
    };

    public static class MyConnectionAlertDialog extends DialogFragment {
        private static Welcome sTarget = null;
        public static final String TAG = "MyConnectionAlertDialog";
        /// M: Is there existed a validated account
        private boolean mHasValidAccount = false;
        /**
         * Use {@link #newInstance} This public constructor is still required so
         * that DialogFragment state can be automatically restored by the
         * framework.
         */
        public MyConnectionAlertDialog() {
        }

        public static MyConnectionAlertDialog newInstance(Welcome activity) {
            MyConnectionAlertDialog frag = new MyConnectionAlertDialog();
            sTarget = activity;
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if(UiUtilities.isWifiOnly(getActivity())) {//For wifi only case,ALPS00458924.
            	builder.setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.unable_to_connect)
                .setMessage(R.string.need_wifi_connection_prompt)
                .setPositiveButton(getString(R.string.connection_settings),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                sTarget.mNeedResolveAccount = true;
                                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            }
                        });
            } else {
            	builder.setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.unable_to_connect)
                    .setMessage(R.string.need_connection_prompt)
                    .setPositiveButton(getString(R.string.connection_settings),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    sTarget.mNeedResolveAccount = true;
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                }
                            });
            }
            /// M: If we have more than one account, giving a chance to switch away from the 
            /// current incomplete account.
            long lastUsedId = Preferences.getPreferences(getActivity()).getLastUsedAccountId();
            if (lastUsedId != Account.NO_ACCOUNT) {
                if (Account.isValidId(getActivity(), lastUsedId)) {
                    /// M: Existed a validated account
                    mHasValidAccount = true;
                    builder.setNegativeButton(getString(R.string.cancel_action),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    sTarget.mAccountId = Account.NO_ACCOUNT;
                                    sTarget.resolveAccount();
                                }
                            });
                }
            }
            Dialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        /**
         * M: Deal with the back key pressed event
         */
        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (mHasValidAccount) {
                // Back to the validated account
                sTarget.mAccountId = Account.NO_ACCOUNT;
                sTarget.resolveAccount();
            } else {
                // Back to the previous app
                sTarget.mNeedResolveAccount = true;
                sTarget.moveTaskToBack(true);
            }
        }
    }
}
