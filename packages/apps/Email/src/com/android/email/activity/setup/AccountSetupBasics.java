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

package com.android.email.activity.setup;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.MultiAutoCompleteTextView.Tokenizer;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.email.Email;
import com.android.email.EmailAddressValidator;
import com.android.email.R;
import com.android.email.VendorPolicyLoader;
import com.android.email.activity.ActivityHelper;
import com.android.email.activity.UiUtilities;
import com.android.email.activity.Welcome;
import com.android.email.activity.setup.AccountSettingsUtils.Provider;
import com.android.email.activity.setup.EmailAccountAutoCompleteTextView.EmailAccountTokenizer;
import com.android.emailcommon.AccountManagerTypes;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.service.SyncWindow;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.Utility;

import com.google.common.annotations.VisibleForTesting;
import com.mediatek.email.provider.history.EmailAddress;
import com.mediatek.email.provider.history.EmailAddressContent;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Prompts the user for the email address and password. Also prompts for "Use this account as
 * default" if this is the 2nd+ account being set up.
 *
 * If the domain is well-known, the account is configured fully and checked immediately
 * using AccountCheckSettingsFragment.  If this succeeds we proceed directly to AccountSetupOptions.
 *
 * If the domain is not known, or the user selects Manual setup, we invoke the
 * AccountSetupAccountType activity where the user can begin to manually configure the account.
 *
 * === Support for automated testing ==
 * This activity can also be launched directly via ACTION_CREATE_ACCOUNT.  This is intended
 * only for use by continuous test systems, and is currently only available when
 * {@link ActivityManager#isRunningInTestHarness()} is set.  To use this mode, you must construct
 * an intent which contains all necessary information to create the account.  No connection
 * checking is done, so the account may or may not actually work.  Here is a sample command, for a
 * gmail account "test_account" with a password of "test_password".
 *
 *      $ adb shell am start -a com.android.email.CREATE_ACCOUNT \
 *          -e EMAIL test_account@gmail.com \
 *          -e USER "Test Account Name" \
 *          -e INCOMING imap+ssl+://test_account:test_password@imap.gmail.com \
 *          -e OUTGOING smtp+ssl+://test_account:test_password@smtp.gmail.com
 *
 * Note: For accounts that require the full email address in the login, encode the @ as %40.
 * Note: Exchange accounts that require device security policies cannot be created automatically.
 */
public class AccountSetupBasics extends AccountSetupActivity
        implements OnClickListener, TextWatcher, AccountCheckSettingsFragment.Callbacks {

    public static final boolean ENTER_DEBUG_SCREEN = Build.TYPE.equals("eng") ? true : false;;

    // This static variable is only used in the case: 
    // Contact -> add a new contact -> add a new account -> select "corporate"
    // -> Email.AccountSetupBasics.
    public static boolean sSetupAccountFinished = false;
    
    /**
     * Direct access for forcing account creation
     * For use by continuous automated test system (e.g. in conjunction with monkey tests)
     */
    public static final String EXTRA_FLOW_MODE = "FLOW_MODE";
    private static final String ACTION_CREATE_ACCOUNT = "com.android.email.CREATE_ACCOUNT";
    private static final String EXTRA_CREATE_ACCOUNT_EMAIL = "EMAIL";
    private static final String EXTRA_CREATE_ACCOUNT_USER = "USER";
    private static final String EXTRA_CREATE_ACCOUNT_INCOMING = "INCOMING";
    private static final String EXTRA_CREATE_ACCOUNT_OUTGOING = "OUTGOING";
    private static final Boolean DEBUG_ALLOW_NON_TEST_HARNESS_CREATION = false;

    private static final String STATE_KEY_PROVIDER = "AccountSetupBasics.provider";

    public static final int SETTINGS_APP_ADD_EAS_ACCOUNT = 0x10;
    public static final int SETTINGS_APP_ADD_POPIMAP_ACCOUNT = 0x11;
    
    // NOTE: If you change this value, confirm that the new interval exists in arrays.xml
    public static final int DEFAULT_ACCOUNT_CHECK_INTERVAL = 15;

    // Support for UI
    private TextView mWelcomeView;
    private EmailAccountAutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private CheckBox mDefaultView;
    private final EmailAddressValidator mEmailValidator = new EmailAddressValidator();
    private Provider mProvider;
    private Button mManualButton;
    private Button mNextButton;
    private boolean mNextButtonInhibit;
    private boolean mPaused;
    private boolean mReportAccountAuthenticatorError;
    private static final String TAG = "AccountSetupBasics";

    // FutureTask to look up the owner
    FutureTask<String> mOwnerLookupTask;

    /** M: add for email address history and account list.@{ */
    DropdownAccountsArrayAdapter<String> mDropdownAdapter;
    DropdownAccountsArrayAdapter<String> mHisAddressAdapter;
    Tokenizer mHisAddressTokenizer = new MultiAutoCompleteTextView.CommaTokenizer();
    Tokenizer mEmailAccountTokenizer = new EmailAccountTokenizer();
    /** @} */
    public static void actionNewAccount(Activity fromActivity) {
        Intent i = new Intent(fromActivity, AccountSetupBasics.class);
        i.putExtra(EXTRA_FLOW_MODE, SetupData.FLOW_MODE_NORMAL);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        fromActivity.startActivity(i);
    }

    /**
     * This generates setup data that can be used to start a self-contained account creation flow
     * for exchange accounts.
     */
    public static Intent actionSetupExchangeIntent(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
//        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(EXTRA_FLOW_MODE, SETTINGS_APP_ADD_EAS_ACCOUNT);
        return i;
    }

    /**
     * This generates setup data that can be used to start a self-contained account creation flow
     * for pop/imap accounts.
     */
    public static Intent actionSetupPopImapIntent(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
//        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(EXTRA_FLOW_MODE, SETTINGS_APP_ADD_POPIMAP_ACCOUNT);
        return i;
    }

    /**
     * This method is specially designed for adding account from Settings app,
     *  and this design is ugly.
     * When adding account from Settings app, works as follows
     * 1)  Start AccountSetupBasics not contain FLAG_ACTIVITY_NEW_TASK.
     * 2)  Finish AccountSetupBasics in the method onResume().
     * 3)  Restart AccountSetupBasics contained flag FLAG_ACTIVITY_NEW_TASK.
     * This design is to avoid 
     * a) the transparent activity com.android.settings.account.AddAccountSettings
     * is not finished in some cases.
     * b) Can build two accounts in the same time, one from Settings, another from Email.
     * Behaviors a) and b) will cause bugs, so we wrote this method to avoid them.
     * If you are not adding account from Settings, please ignore it.
     */
    public static void addAccountFromExternal(Context context, boolean isEasAccount,
            AccountAuthenticatorResponse response) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isEasAccount) {
            i.putExtra(EXTRA_FLOW_MODE, SetupData.FLOW_MODE_ACCOUNT_MANAGER_EAS);
        } else {
            i.putExtra(EXTRA_FLOW_MODE, SetupData.FLOW_MODE_ACCOUNT_MANAGER_POP_IMAP);
        }
        if (response != null) {
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        }
        context.startActivity(i);
    }
    
    public static void actionAccountCreateFinishedAccountFlow(Activity fromActivity) {
        // TODO: handle this case - modifying state on SetupData when instantiating an Intent
        // is not safe, since it's not guaranteed that an Activity will run with the Intent, and
        // information can get lost.

        Intent i= new Intent(fromActivity, AccountSetupBasics.class);
        // If we're in the "account flow" (from AccountManager), we want to return to the caller
        // (in the settings app)
        SetupData.init(SetupData.FLOW_MODE_RETURN_TO_CALLER);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        fromActivity.startActivity(i);
    }

    public static void actionAccountCreateFinished(final Activity fromActivity,
            final long accountId) {
        boolean isFromAccountManager = SetupData.getFromAccountManager();
        Intent i = new Intent(fromActivity, AccountSetupBasics.class);
        // If we're not in the "account flow" (from AccountManager), we want to show the
        // message list for the new inbox
        Account account = Account.restoreAccountWithId(fromActivity, accountId);
        SetupData.init(SetupData.FLOW_MODE_RETURN_TO_MESSAGE_LIST, account);
        SetupData.setFromAccountManager(isFromAccountManager);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        fromActivity.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.debugSetWindowFlags(this);
        boolean isAppilcationRestart = (savedInstanceState != null && savedInstanceState
                .containsKey(SetupData.EXTRA_SETUP_DATA));
        // Check for forced account creation first, as it comes from an externally-generated
        // intent and won't have any SetupData prepared.
        String action = getIntent().getAction();
        if (ACTION_CREATE_ACCOUNT.equals(action)) {
            SetupData.init(SetupData.FLOW_MODE_FORCE_CREATE);
        }

        int flowMode = getIntent().getIntExtra(EXTRA_FLOW_MODE, SetupData.FLOW_MODE_UNSPECIFIED);
        if (flowMode != SetupData.FLOW_MODE_UNSPECIFIED && !isAppilcationRestart) {
            Logging.d(" AccountSetupBasics::onCreate, flowMode = " + flowMode);
            SetupData.init(flowMode);
        } else {
            // TODO: get rid of this case. It's not safe to rely on this global static state. It
            // should be specified in the Intent always.
            flowMode = SetupData.getFlowMode();
        }

        if (flowMode == SetupData.FLOW_MODE_RETURN_TO_CALLER) {
            // Return to the caller who initiated account creation
            finish();
            return;
        } else if (flowMode == SetupData.FLOW_MODE_RETURN_TO_MESSAGE_LIST) {
            Account account = SetupData.getAccount();
            if (account != null && account.mId >= 0) {
                if (SetupData.getFromAccountManager()) {
                    Welcome.actionSyncInboxAndFinish(this, account.mId);
                } else {
                    // Show the message list for the new account
                    Welcome.actionOpenAccountInbox(this, account.mId);
                }
                finish();
                return;
            } else {
                Logging.d("Account is null, may be removed.");
                Welcome.actionStart(this);
                finish();
            }
        }

        setContentView(R.layout.account_setup_basics);

        mWelcomeView = (TextView) UiUtilities.getView(this, R.id.instructions);
        mEmailView = (EmailAccountAutoCompleteTextView) UiUtilities.getView(this,
                R.id.account_email);
        mPasswordView = (EditText) UiUtilities.getView(this, R.id.account_password);
        mDefaultView = (CheckBox) UiUtilities.getView(this, R.id.account_default);

        mEmailView.addTextChangedListener(this);
        mPasswordView.addTextChangedListener(this);
        /** M: modify for email address history @{ */
        mEmailView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // when view loses focus, save the email address.
                if (!hasFocus) {
                    new SaveAddressTask().execute(mEmailView.getText().toString());
                }
            }
        });
        mDropdownAdapter = new DropdownAccountsArrayAdapter<String>(this,
                R.layout.account_setup_popup_list,
                AccountSettingsUtils.CollectAutoSetupDomain(this));
        mDropdownAdapter.setFilter(new DropdownAccountsFilter<String>(mDropdownAdapter));
        mHisAddressAdapter = new DropdownAccountsArrayAdapter<String>(this, R.layout.account_setup_popup_list);
        mHisAddressAdapter.setFilter(new DropdownAddressFilter<String>(mHisAddressAdapter));
        mEmailView.setAdapter(mHisAddressAdapter);
        mEmailView.setTokenizer(mHisAddressTokenizer);
        new PrepareAddressDataTask().execute();
        // move the cursor to password edittext when we click one auto-prompt email address.
        mEmailView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPasswordView.requestFocus();
            }
        });
        /** @} */

        // If there are one or more accounts already in existence, then display
        // the "use as default" checkbox (it defaults to hidden).
        new DisplayCheckboxTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        boolean manualButtonDisplayed = true;
        boolean alternateStrings = false;
        if (flowMode == SetupData.FLOW_MODE_ACCOUNT_MANAGER_EAS
                || flowMode == SETTINGS_APP_ADD_EAS_ACCOUNT) {
            // No need for manual button -> next is appropriate
            manualButtonDisplayed = false;
            // Swap welcome text for EAS-specific text
            alternateStrings = VendorPolicyLoader.getInstance(this).useAlternateExchangeStrings();
            setTitle(alternateStrings
                    ? R.string.account_setup_basics_exchange_title_alternate
                    : R.string.account_setup_basics_exchange_title);
            mWelcomeView.setText(alternateStrings
                    ? R.string.accounts_welcome_exchange_alternate
                    : R.string.accounts_welcome_exchange);
        }

        // Configure buttons
        mManualButton = (Button) UiUtilities.getView(this, R.id.manual_setup);
        mNextButton = (Button) UiUtilities.getView(this, R.id.next);
        mManualButton.setVisibility(manualButtonDisplayed ? View.VISIBLE : View.INVISIBLE);
        mManualButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        // Force disabled until validator notifies otherwise
        onEnableProceedButtons(false);
        // Lightweight debounce while Async tasks underway
        mNextButtonInhibit = false;

        // Set aside incoming AccountAuthenticatorResponse, if there was any
        AccountAuthenticatorResponse authenticatorResponse =
            getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        SetupData.setAccountAuthenticatorResponse(authenticatorResponse);
        if (authenticatorResponse != null) {
            // When this Activity is called as part of account authentification flow,
            // we are responsible for eventually reporting the result (success or failure) to
            // the account manager.  Most exit paths represent an failed or abandoned setup,
            // so the default is to report the error.  Success will be reported by the code in
            // AccountSetupOptions that commits the finally created account.
            mReportAccountAuthenticatorError = true;
        }

        // Load fields, but only once
        String userName = SetupData.getUsername();
        String password = SetupData.getPassword();
        if (userName != null) {
            mEmailView.setText(userName);
            SetupData.setUsername(null);
            mPasswordView.setText(password);
            SetupData.setPassword(null);
        }

        // Handle force account creation immediately (now that fragment is set up)
        // This is never allowed in a normal user build and will exit immediately.
        if (SetupData.getFlowMode() == SetupData.FLOW_MODE_FORCE_CREATE) {
            if (!DEBUG_ALLOW_NON_TEST_HARNESS_CREATION &&
                    !ActivityManager.isRunningInTestHarness()) {
                Logging.e(TAG, "ERROR: Force account create only allowed while in test harness");
                finish();
                return;
            }
            Intent intent = getIntent();
            String email = intent.getStringExtra(EXTRA_CREATE_ACCOUNT_EMAIL);
            String user = intent.getStringExtra(EXTRA_CREATE_ACCOUNT_USER);
            String incoming = intent.getStringExtra(EXTRA_CREATE_ACCOUNT_INCOMING);
            String outgoing = intent.getStringExtra(EXTRA_CREATE_ACCOUNT_OUTGOING);
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(user) ||
                    TextUtils.isEmpty(incoming) || TextUtils.isEmpty(outgoing)) {
                Logging.e(TAG, "ERROR: Force account create requires extras EMAIL, USER, " +
                        "INCOMING, OUTGOING");
                finish();
                return;
            }
            forceCreateAccount(email, user, incoming, outgoing);
            onCheckSettingsComplete(AccountCheckSettingsFragment.CHECK_SETTINGS_OK); // calls finish
            return;
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY_PROVIDER)) {
            mProvider = (Provider) savedInstanceState.getSerializable(STATE_KEY_PROVIDER);
        }

        // Launch a worker to look up the owner name.  It should be ready well in advance of
        // the time the user clicks next or manual.
        mOwnerLookupTask = new FutureTask<String>(mOwnerLookupCallable);
        Logging.d(TAG, ">>>>> Utility.runAsync onCreate");
        Utility.runAsync(mOwnerLookupTask);
        
        initLengthFilter();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        int flowMode = getIntent().getIntExtra(EXTRA_FLOW_MODE, SetupData.FLOW_MODE_UNSPECIFIED);
        /**
         * M: This situation could only happen in the case: Contact/Calendar ->
         * add a new account -> Email.AccountSetupBasics.
         */
        if ((flowMode & (1 << 4)) != 0) {
            Logging.d("Add account from AuthenticatorService," +
                        " will restart Email app using flag NEW_TASK and CLEAR_TOP!");
            if (flowMode == SETTINGS_APP_ADD_EAS_ACCOUNT) {
                AccountAuthenticatorResponse reponse = getIntent().getParcelableExtra(
                        AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                addAccountFromExternal(this, true, reponse);
                finish();
                return;
            } else if (flowMode == SETTINGS_APP_ADD_POPIMAP_ACCOUNT) {
                AccountAuthenticatorResponse reponse = getIntent().getParcelableExtra(
                        AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                addAccountFromExternal(this, false, reponse);
                finish();
                return;
            }
        }
        /** @} */
        // SETTINGS_APP_ADD_EAS_ACCOUNT is just a temporary variable,
        // so we need to covert it to normal.
        if (flowMode == SETTINGS_APP_ADD_EAS_ACCOUNT) {
            SetupData.setFlowMode(SetupData.FLOW_MODE_ACCOUNT_MANAGER_EAS);
        }
        if (sSetupAccountFinished) {
            Bundle bundle = new Bundle();
            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, SetupData.getAccount().mEmailAddress);
            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, AccountManagerTypes.TYPE_EXCHANGE);
            setResult(-1, new Intent().putExtras(bundle));
            sSetupAccountFinished = false;
            finish();
            return;
        }
        mNextButtonInhibit = false;
        if (Email.getStartComposeAfterSetupAccountFlag()
                && Email.getSetupAccountFinishedFlag()) {
            Email.setSetupAccountFinishedFlag(false);
            Email.setStartComposeAfterSetupAccountFlag(false);
            Logging.d(TAG, "+++++ AccountSetupBasics onResume reset the flag as false ");
            finish();
            return;
        }
        mPaused = false;
    }

    @Override
    public void finish() {
        // If the account manager initiated the creation, and success was not reported,
        // then we assume that we're giving up (for any reason) - report failure.
        if (mReportAccountAuthenticatorError) {
            AccountAuthenticatorResponse authenticatorResponse =
                    SetupData.getAccountAuthenticatorResponse();
            if (authenticatorResponse != null) {
                authenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
                SetupData.setAccountAuthenticatorResponse(null);
            }
        }
        super.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mProvider != null) {
            outState.putSerializable(STATE_KEY_PROVIDER, mProvider);
        }
    }

    /**
     * Implements OnClickListener
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                // Simple debounce - just ignore while async checks are underway
                if (mNextButtonInhibit) {
                    return;
                }
                onNext();
                break;
            case R.id.manual_setup:
                onManualSetup(false);
                break;
        }
    }

    /**
     * Implements TextWatcher
     */
    public void afterTextChanged(Editable s) {
        validateFields();
    }

    /**
     * Implements TextWatcher
     */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /**
     * Implements TextWatcher
     */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        /** M: @{ */
        if (s != null) {
            if (s.length() > 0 && s.toString().contains("@")) {
                if (mEmailView.getAdapter() != mDropdownAdapter) {
                    mEmailView.setAdapter(mDropdownAdapter);
                    mEmailView.setTokenizer(mEmailAccountTokenizer);
                }
            } else {
                if (mEmailView.getAdapter() != mHisAddressAdapter) {
                    mEmailView.setAdapter(mHisAddressAdapter);
                    mEmailView.setTokenizer(mHisAddressTokenizer);
                }
            }
        }
        /** @} */
        if (mDropdownAdapter != null) {
            String email = mEmailView.getText().toString();
            String[] emailParts = email.split("@");
            if (emailParts.length > 0) {
                mDropdownAdapter.setUserName(emailParts[0]);
            }
            // Scroll the editTextView to the top of the screen.
            int scrollX = mEmailView.getScrollX();
            int scrollY = mEmailView.getScrollY();
            int xoff = 0;
            int yoff = 0;
            Rect r = new Rect(scrollX, scrollY,  scrollX + mEmailView.getDropDownWidth() + xoff,
                    scrollY + 500 + mEmailView.getHeight() + yoff);
            mEmailView.requestRectangleOnScreen(r, true);
        }
    }

    private void validateFields() {
        boolean valid = Utility.isTextViewNotEmpty(mEmailView)
                && Utility.isTextViewNotEmpty(mPasswordView)
                && mEmailValidator.isValid(mEmailView.getText().toString().trim());
        onEnableProceedButtons(valid);

        // Warn (but don't prevent) if password has leading/trailing spaces
        AccountSettingsUtils.checkPasswordSpaces(this, mPasswordView);
    }

    /**
     * Return an existing username if found, or null.  This is the result of the Callable (below).
     */
    private String getOwnerName() {
        String result = null;
        try {
            result = mOwnerLookupTask.get();
        } catch (InterruptedException e) {
            Logging.d(TAG, "Exception: "+e.toString()+" invokes in method getOwnerName()");
        } catch (ExecutionException e) {
            Logging.d(TAG, "Exception: "+e.toString()+" invokes in method getOwnerName()");
        }
        return result;
    }

    /**
     * Callable that returns the username (based on other accounts) or null.
     */
    private final Callable<String> mOwnerLookupCallable = new Callable<String>() {
        public String call() {
            Context context = AccountSetupBasics.this;
            String name = null;
            long defaultId = Account.getDefaultAccountId(context);
            if (defaultId != -1) {
                Account account = Account.restoreAccountWithId(context, defaultId);
                if (account != null) {
                    name = account.getSenderName();
                }
            }
            return name;
        }
    };

    /**
     * Finish the auto setup process, in some cases after showing a warning dialog.
     */
    private void finishAutoSetup() {
        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString();

        try {
            mProvider.expandTemplates(email);

            Account account = SetupData.getAccount();
            HostAuth recvAuth = account.getOrCreateHostAuthRecv(this);
            HostAuth.setHostAuthFromString(recvAuth, mProvider.incomingUri);
            recvAuth.setLogin(mProvider.incomingUsername, password);

            HostAuth sendAuth = account.getOrCreateHostAuthSend(this);
            HostAuth.setHostAuthFromString(sendAuth, mProvider.outgoingUri);
            sendAuth.setLogin(mProvider.outgoingUsername, password);

            // Populate the setup data, assuming that the duplicate account check will succeed
            //populateSetupData(getOwnerName(), email, mDefaultView.isChecked());
            populateSetupData(null, email, mDefaultView.isChecked());

            // Stop here if the login credentials duplicate an existing account
            // Launch an Async task to do the work
            /**
             * M: Modified to avoid adding duplicated account which will be caused
             * when server's address is changed.eg:hotmail @{
             */
            new DuplicateCheckTask(this, recvAuth.mProtocol, mProvider.incomingUsername, recvAuth.mAddress)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            /** @} */
        } catch (URISyntaxException e) {
            /*
             * If there is some problem with the URI we give up and go on to manual setup.
             * Technically speaking, AutoDiscover is OK here, since the user clicked "Next"
             * to get here. This will not happen in practice because we don't expect to
             * find any EAS accounts in the providers list.
             */
            onManualSetup(false);
        }
    }

    /**
     * Async task that continues the work of finishAutoSetup().  Checks for a duplicate
     * account and then either alerts the user, or continues.
     */
    private class DuplicateCheckTask extends AsyncTask<Void, Void, Account> {
        private final Context mContext;
        /**
         * M: Modified to avoid adding duplicated account which will be caused
         * when server's address is changed.eg:hotmail @{
         */
        private final String mCheckProtocol;
        private final String mCheckLogin;
        private final String mServerAddr;

        public DuplicateCheckTask(Context context, String checkProtocol, String checkLogin, String serverAddr) {
            mContext = context;
            mCheckProtocol = checkProtocol;
            mCheckLogin = checkLogin;
            mServerAddr = serverAddr;
            // Prevent additional clicks on the next button during Async lookup
            mNextButtonInhibit = true;
        }

        @Override
        protected Account doInBackground(Void... params) {
            Account account = Utility.findExistingAccount(mContext, -1,
                    mCheckProtocol, mCheckLogin, mServerAddr);
            return account;
        }
        /** @} */

        @Override
        protected void onPostExecute(Account duplicateAccount) {
            mNextButtonInhibit = false;
            // Exit immediately if the user left before we finished
            if (mPaused) {
                return;
            }
            // Show duplicate account warning, or proceed
            if (duplicateAccount != null) {
                DuplicateAccountDialogFragment dialogFragment =
                    DuplicateAccountDialogFragment.newInstance(duplicateAccount.mDisplayName);
                dialogFragment.show(getFragmentManager(), DuplicateAccountDialogFragment.TAG);
                return;
            } else {
                FragmentManager fm = getFragmentManager();
                AccountCheckSettingsFragment checkerFragment = (AccountCheckSettingsFragment)fm
                        .findFragmentByTag(AccountCheckSettingsFragment.TAG);
                if (checkerFragment == null) {
                    checkerFragment = AccountCheckSettingsFragment.newInstance(
                            SetupData.CHECK_INCOMING | SetupData.CHECK_OUTGOING, null);
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.add(checkerFragment, AccountCheckSettingsFragment.TAG);
                    transaction.addToBackStack("back");
                    transaction.commit();
                    fm.executePendingTransactions();
                }
            }
        }
    }

    public void setNextButtonInhibit(boolean inhibit) {
        mNextButtonInhibit = inhibit;
    }

    /**
     * When "next" button is clicked
     */
    private void onNext() {
        mNextButtonInhibit = true;
        //Hide IME.
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
        // Try auto-configuration from XML providers (unless in EAS mode, we can skip it)
        if (SetupData.getFlowMode() != SetupData.FLOW_MODE_ACCOUNT_MANAGER_EAS) {
            String email = mEmailView.getText().toString().trim();
            String[] emailParts = email.split("@");
            String domain = emailParts[1].trim();
            mProvider = AccountSettingsUtils.findProviderForDomain(this, domain);
            if (mProvider != null) {
                if (mProvider.note != null) {
                    NoteDialogFragment dialogFragment =
                            NoteDialogFragment.newInstance(mProvider.note);
                    dialogFragment.show(getFragmentManager(), NoteDialogFragment.TAG);
                } else {
                    finishAutoSetup();
                }
                return;
            }
        }
        // Can't use auto setup (although EAS accounts may still be able to AutoDiscover)
        onManualSetup(false);
    }

    /**
     * When "manual setup" button is clicked
     *
     * @param allowAutoDiscover - true if the user clicked 'next' and (if the account is EAS)
     * it's OK to use autodiscover.  false to prevent autodiscover and go straight to manual setup.
     * Ignored for IMAP & POP accounts.
     */
    private void onManualSetup(boolean allowAutoDiscover) {
        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString();
        String[] emailParts = email.split("@");
        String user = emailParts[0].trim();
        String domain = emailParts[1].trim();

        // Alternate entry to the debug options screen (for devices without a physical keyboard:
        //  Username: d@d.d
        //  Password: debug
        if (ENTER_DEBUG_SCREEN && "d@d.d".equals(email) && "debug".equals(password)) {
            mEmailView.setText("");
            mPasswordView.setText("");
            AccountSettings.actionSettingsWithDebug(this);
            return;
        }

        Account account = SetupData.getAccount();
        HostAuth recvAuth = account.getOrCreateHostAuthRecv(this);
        recvAuth.setLogin(user, password);
        recvAuth.setConnection("placeholder", domain, HostAuth.PORT_UNKNOWN, HostAuth.FLAG_NONE);

        HostAuth sendAuth = account.getOrCreateHostAuthSend(this);
        sendAuth.setLogin(user, password);
        sendAuth.setConnection("placeholder", domain, HostAuth.PORT_UNKNOWN, HostAuth.FLAG_NONE);

        //populateSetupData(getOwnerName(), email, mDefaultView.isChecked());
        populateSetupData(null, email, mDefaultView.isChecked());

        SetupData.setAllowAutodiscover(allowAutoDiscover);
        AccountSetupAccountType.actionSelectAccountType(this);
    }

    /**
     * To support continuous testing, we allow the forced creation of accounts.
     * This works in a manner fairly similar to automatic setup, in which the complete server
     * Uri's are available, except that we will also skip checking (as if both checks were true)
     * and all other UI.
     *
     * @param email The email address for the new account
     * @param user The user name for the new account
     * @param incoming The URI-style string defining the incoming account
     * @param outgoing The URI-style string defining the outgoing account
     */
    public void forceCreateAccount(String email, String user, String incoming, String outgoing) {
        Account account = SetupData.getAccount();
        try {
            HostAuth recvAuth = account.getOrCreateHostAuthRecv(this);
            HostAuth.setHostAuthFromString(recvAuth, incoming);

            HostAuth sendAuth = account.getOrCreateHostAuthSend(this);
            HostAuth.setHostAuthFromString(sendAuth, outgoing);

            populateSetupData(user, email, false);
        } catch (URISyntaxException e) {
            // If we can't set up the URL, don't continue - account setup pages will fail too
            Toast.makeText(
                    this, R.string.account_setup_username_password_toast, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Populate SetupData's account with complete setup info.
     */
    private void populateSetupData(String senderName, String senderEmail, boolean isDefault) {
        Account account = SetupData.getAccount();
        account.setSenderName(senderName);
        account.setEmailAddress(senderEmail);
        account.setDisplayName(senderEmail);
        account.setDefaultAccount(isDefault);
        SetupData.setDefault(isDefault);        // TODO - why duplicated, if already set in account

        String protocol = account.mHostAuthRecv.mProtocol;
        setFlagsForProtocol(account, protocol);
    }

    /**
     * Sets the account sync, delete, and other misc flags not captured in {@code HostAuth}
     * information for the specified account based on the protocol type.
     */
    @VisibleForTesting
    static void setFlagsForProtocol(Account account, String protocol) {
        // Reset account flags if user cancel one protocol account set up(eg:imap), and
        // begin setup another protocol account(eg:pop3).
        account.mFlags = Account.FLAGS_NOTIFY_NEW_MAIL;

        if (HostAuth.SCHEME_IMAP.equals(protocol)) {
            // Delete policy must be set explicitly, because IMAP does not provide a UI selection
            // for it.
            account.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);
            account.mFlags |= Account.FLAGS_SUPPORTS_SEARCH;
        }

        if (HostAuth.SCHEME_EAS.equals(protocol)) {
            account.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);
            account.setSyncInterval(Account.CHECK_INTERVAL_SMART_PUSH);
            account.setSyncLookback(SyncWindow.SYNC_WINDOW_3_DAYS);
        } else {
            account.setSyncInterval(DEFAULT_ACCOUNT_CHECK_INTERVAL);
        }
    }

    /**
     * Implements AccountCheckSettingsFragment.Callbacks
     *
     * This is used in automatic setup mode to jump directly down to the options screen.
     *
     * This is the only case where we finish() this activity but account setup is continuing,
     * so we inhibit reporting any error back to the Account manager.
     */
    @Override
    public void onCheckSettingsComplete(int result) {
        if (result == AccountCheckSettingsFragment.CHECK_SETTINGS_OK) {
            AccountSetupOptions.actionOptions(this);
            mReportAccountAuthenticatorError = false;
            // After check settings completely, user can still back to activity AccountSetupBasics.
            // finish();
        }
    }

    /**
     * Implements AccountCheckSettingsFragment.Callbacks
     * This is overridden only by AccountSetupExchange
     */
    @Override
    public void onAutoDiscoverComplete(int result, HostAuth hostAuth) {
        throw new IllegalStateException();
    }

    /**
     * AsyncTask checks count of accounts and displays "use this account as default" checkbox
     * if there are more than one.
     */
    private class DisplayCheckboxTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            return EmailContent.count(AccountSetupBasics.this, Account.CONTENT_URI);
        }

        @Override
        protected void onPostExecute(Integer numAccounts) {
            if (numAccounts > 0) {
                Activity a = AccountSetupBasics.this;
                UiUtilities.setVisibilitySafe(mDefaultView, View.VISIBLE);
                UiUtilities.setVisibilitySafe(a, R.id.account_default_divider_1, View.VISIBLE);
                UiUtilities.setVisibilitySafe(a, R.id.account_default_divider_2, View.VISIBLE);
            }
        }
    }

    private void onEnableProceedButtons(boolean enabled) {
        mManualButton.setEnabled(enabled);
        mNextButton.setEnabled(enabled);
    }

    /**
     * Dialog fragment to show "setup note" dialog
     */
    public static class NoteDialogFragment extends DialogFragment {
        private final static String TAG = "NoteDialogFragment";

        // Argument bundle keys
        private final static String BUNDLE_KEY_NOTE = "NoteDialogFragment.Note";

        /**
         * Create the dialog with parameters
         */
        public static NoteDialogFragment newInstance(String note) {
            NoteDialogFragment f = new NoteDialogFragment();
            Bundle b = new Bundle();
            b.putString(BUNDLE_KEY_NOTE, note);
            f.setArguments(b);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final String note = getArguments().getString(BUNDLE_KEY_NOTE);

            return new AlertDialog.Builder(context)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(note)
                .setPositiveButton(
                        R.string.okay_action,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Activity a = getActivity();
                                if (a instanceof AccountSetupBasics) {
                                    ((AccountSetupBasics)a).finishAutoSetup();
                                }
                                dismiss();
                            }
                        })
                .setNegativeButton(
                        context.getString(R.string.cancel_action),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Activity a = getActivity();
                                if (a instanceof AccountSetupBasics) {
                                    ((AccountSetupBasics)a).setNextButtonInhibit(false);
                                }
                                dismiss();
                            }
                        })
                .create();
        }
    }


    private void initLengthFilter(){
        UiUtilities.setupLengthFilter(mEmailView, this,
                Email.EDITVIEW_MAX_LENGTH_1, true);
        UiUtilities.setupLengthFilter(mPasswordView,
                this, Email.EDITVIEW_MAX_LENGTH_1, true);
   }

    /** M: Clear compose flag and intent after out of AccountSetupBasic @{ */
    @Override
    public void onBackPressed() {
        Email.setStartComposeAfterSetupAccountFlag(false);
        Email.setStartComposeIntent(null);
        super.onBackPressed();
    };
    /** @} */

    /**
     * M: PrepareAddressDataTask for querying address outside of the UI thread, and update
     * the data of mHisAddressAdapter.
     */
    private class PrepareAddressDataTask extends AsyncTask<String, Void, Cursor> {
        @Override
        protected Cursor doInBackground(String... params) {
            Cursor c = EmailAddress.queryAddress(getBaseContext(), EmailAddress.CONTENT_URI);
            return c;
        }

        @Override
        protected void onPostExecute(Cursor c) {
            super.onPostExecute(c);
            mHisAddressAdapter.clear();
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        String address = c.getString(1);
                        mHisAddressAdapter.add(address);
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }

    }

    /**
     * M: SaveAddressTask for saving address outside of the UI thread
     */
    private class SaveAddressTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            EmailAddress.saveAddress(getBaseContext(), params[0]);
            new PrepareAddressDataTask().execute();
            return null;
        }
    }
}