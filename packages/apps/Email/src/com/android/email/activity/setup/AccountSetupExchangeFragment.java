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

package com.android.email.activity.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.android.email.Email;
import com.android.email.R;
import com.android.email.activity.UiUtilities;
import com.android.email.provider.AccountBackupRestore;
import com.android.email.service.EmailServiceUtils;
import com.android.email.view.CertificateSelector;
import com.android.email.view.CertificateSelector.HostCallback;
import com.android.emailcommon.Device;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.utility.CertificateRequestor;
import com.android.emailcommon.utility.Utility;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
/**
 * Provides generic setup for Exchange accounts.
 *
 * This fragment is used by AccountSetupExchange (for creating accounts) and by AccountSettingsXL
 * (for editing existing accounts).
 */
public class AccountSetupExchangeFragment extends AccountServerBaseFragment
        implements OnCheckedChangeListener, HostCallback {

    private static final int CERTIFICATE_REQUEST = 0;
    private static final String STATE_KEY_CREDENTIAL = "AccountSetupExchangeFragment.credential";
    private static final String STATE_KEY_LOADED = "AccountSetupExchangeFragment.loaded";
    private static final String STATE_KEY_SSL = "AccountSetupExchangeFragment.ssl";

    private static final int PORT_SSL = 443;
    private static final int PORT_NORMAL = 80;

    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mServerView;
    private EditText mPortView;
    private CheckBox mSslSecurityView;
    private CheckBox mTrustCertificatesView;
    private CertificateSelector mClientCertificateSelector;

    // Support for lifecycle
    private boolean mStarted;
    /* package */ boolean mLoaded;
    private String mCacheLoginCredential;

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before {@link #onActivityCreated(Bundle)}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onCreate");
        }
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCacheLoginCredential = savedInstanceState.getString(STATE_KEY_CREDENTIAL);
            mLoaded = savedInstanceState.getBoolean(STATE_KEY_LOADED, false);
        }
        mBaseScheme = HostAuth.SCHEME_EAS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onCreateView");
        }
        int layoutId = mSettingsMode
                ? R.layout.account_settings_exchange_fragment
                : R.layout.account_setup_exchange_fragment;

        View view = inflater.inflate(layoutId, container, false);
        final Context context = getActivity();

        mUsernameView = UiUtilities.getView(view, R.id.account_username);
        mPasswordView = UiUtilities.getView(view, R.id.account_password);
        mServerView = UiUtilities.getView(view, R.id.account_server);
        mPortView = (EditText) UiUtilities.getView(view, R.id.account_port);
        mSslSecurityView = UiUtilities.getView(view, R.id.account_ssl);
        mSslSecurityView.setOnCheckedChangeListener(this);
        /** M: Restore the SslSecurityView state from savedInstanceState @{ */
        if (savedInstanceState != null) {
            mSslSecurityView.setEnabled(savedInstanceState.getBoolean(STATE_KEY_SSL));
        }
        /** @} */
        mTrustCertificatesView = UiUtilities.getView(view, R.id.account_trust_certificates);
        mClientCertificateSelector = UiUtilities.getView(view, R.id.client_certificate_selector);

        // Calls validateFields() which enables or disables the Next button
        // based on the fields' validity.
        TextWatcher validationTextWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        };
        // We're editing an existing account; don't allow modification of the user name
        if (mSettingsMode) {
            makeTextViewUneditable(mUsernameView,
                    getString(R.string.account_setup_username_uneditable_error));
        }
        mUsernameView.addTextChangedListener(validationTextWatcher);
        mPasswordView.addTextChangedListener(validationTextWatcher);
        mServerView.addTextChangedListener(validationTextWatcher);
        mPortView.addTextChangedListener(validationTextWatcher);

        EditText lastView = mServerView;
        lastView.setOnEditorActionListener(mDismissImeOnDoneListener);

        String deviceId = "";
        try {
            deviceId = Device.getDeviceId(context);
        } catch (IOException e) {
            Log.d(Logging.LOG_TAG, "Exception in method onCreateView when get the DeviceId");
        }
        ((TextView) UiUtilities.getView(view, R.id.device_id)).setText(deviceId);

        // Additional setup only used while in "settings" mode
        onCreateViewSettingsMode(view);
        initLengthFilter();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onActivityCreated");
        }
        super.onActivityCreated(savedInstanceState);
        mClientCertificateSelector.setHostActivity(this);
        //M: set the title of the activity which contains this fragment dynamically, to make the
        // title change according to the language and locale setting change, as this fragment is
        // created by setPreferencePanel(), the title will not change as the locale changes by use
        // of setBreadCrumbTitle()
        this.getActivity().setTitle(R.string.account_settings_incoming_label);
    }

    /**
     * Called when the Fragment is visible to the user.
     */
    @Override
    public void onStart() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onStart");
        }
        super.onStart();
        mStarted = true;
        loadSettings(getCurrentAccount());
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     */
    @Override
    public void onResume() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onResume");
        }
        super.onResume();
        validateFields();
    }

    @Override
    public void onPause() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onPause");
        }
        super.onPause();
    }

    /**
     * Called when the Fragment is no longer started.
     */
    @Override
    public void onStop() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onStop");
        }
        super.onStop();
        mStarted = false;
    }

    /**
     * Called when the fragment is no longer in use.
     */
    @Override
    public void onDestroy() {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onDestroy");
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (Logging.DEBUG_LIFECYCLE && Email.DEBUG) {
            Log.d(Logging.LOG_TAG, "AccountSetupExchangeFragment onSaveInstanceState");
        }
        super.onSaveInstanceState(outState);

        outState.putString(STATE_KEY_CREDENTIAL, mCacheLoginCredential);
        outState.putBoolean(STATE_KEY_LOADED, mLoaded);
        /** M: Save the SslSecurityView state @{ */
        outState.putBoolean(STATE_KEY_SSL, mSslSecurityView.isEnabled());
        /** @} */
    }

    /**
     * Activity provides callbacks here.  This also triggers loading and setting up the UX
     */
    @Override
    public void setCallback(Callback callback) {
        super.setCallback(callback);
        if (mStarted) {
            loadSettings(getCurrentAccount());
        }
    }

    /**
     * Force the given account settings to be loaded using {@link #loadSettings(Account)}.
     *
     * @return true if the loaded values pass validation
     */
    private boolean forceLoadSettings(Account account) {
        mLoaded = false;
        return loadSettings(account);
    }


    private int getPortFromSecurityType() {
        boolean useSsl = mSslSecurityView.isChecked();
        int port = useSsl ? PORT_SSL : PORT_NORMAL;
        return port;
    }

    private void updatePortFromSecurityType() {
        int port = getPortFromSecurityType();
        mPortView.setText(Integer.toString(port));
    }

    /**
     * Load the given account settings into the UI and then ensure the settings are valid.
     * As an optimization, if the settings have already been loaded, the UI will not be
     * updated, but, the account fields will still be validated.
     *
     * @return true if the loaded values pass validation
     */
    /*package*/ boolean loadSettings(Account account) {
        if (mLoaded) return validateFields();

        HostAuth hostAuth = account.mHostAuthRecv;

        String userName = hostAuth.mLogin;
        if (userName != null) {
            // Add a backslash to the start of the username, but only if the username has no
            // backslash in it.
            if (userName.indexOf('\\') < 0) {
                userName = "\\" + userName;
            }
            mUsernameView.setText(userName);
        }

        if (hostAuth.mPassword != null) {
            mPasswordView.setText(hostAuth.mPassword);
            // Since username is uneditable, focus on the next editable field
            if (mSettingsMode) {
                mPasswordView.requestFocus();
            }
        }

        String protocol = hostAuth.mProtocol;
        if (protocol == null || !protocol.startsWith("eas")) {
            throw new Error("Unknown account type: " + protocol);
        }

        if (hostAuth.mAddress != null) {
            mServerView.setText(hostAuth.mAddress);
        }

        boolean ssl = 0 != (hostAuth.mFlags & HostAuth.FLAG_SSL);
        boolean trustCertificates = 0 != (hostAuth.mFlags & HostAuth.FLAG_TRUST_ALL);
        mSslSecurityView.setChecked(ssl);
        mTrustCertificatesView.setChecked(trustCertificates);
        if (hostAuth.mClientCertAlias != null) {
            mClientCertificateSelector.setCertificate(hostAuth.mClientCertAlias);
        }
        onUseSslChanged(ssl);

        int port = hostAuth.mPort;
        if (port != HostAuth.PORT_UNKNOWN) {
            mPortView.setText(Integer.toString(port));
        } else {
            updatePortFromSecurityType();
        }
        mLoadedRecvAuth = hostAuth;
        mLoaded = true;
        return validateFields();
    }

    private boolean usernameFieldValid(EditText usernameView) {
        return Utility.isTextViewNotEmpty(usernameView) &&
            !usernameView.getText().toString().equals("\\");
    }

    /**
     * Check the values in the fields and decide if it makes sense to enable the "next" button
     * @return true if all fields are valid, false if any fields are incomplete
     */
    private boolean validateFields() {
        if (!mLoaded) return false;
        boolean enabled = usernameFieldValid(mUsernameView)
                && Utility.isTextViewNotEmpty(mPasswordView)
                && Utility.isServerNameValid(mServerView)
                && Utility.isPortFieldValid(mPortView);
        if (enabled) {
            try {
                URI uri = getUri();
            } catch (URISyntaxException use) {
                enabled = false;
            }
        }
        
        enableNextButton(enabled);

        // Warn (but don't prevent) if password has leading/trailing spaces
        AccountSettingsUtils.checkPasswordSpaces(mContext, mPasswordView);

        return enabled;
    }

    /* package */ URI getUri() throws URISyntaxException {
        boolean sslRequired = mSslSecurityView.isChecked();
        boolean trustCertificates = mTrustCertificatesView.isChecked();
        String scheme = (sslRequired)
                        ? (trustCertificates ? "eas+ssl+trustallcerts" : "eas+ssl+")
                        : "eas";
        String userName = mUsernameView.getText().toString().trim();
        // Remove a leading backslash, if there is one, since we now automatically put one at
        // the start of the username field
        if (userName.startsWith("\\")) {
            userName = userName.substring(1);
        }
        String userInfo = userName + ":" + mPasswordView.getText();
        String host = mServerView.getText().toString().trim();
        String path = null;

        URI uri = new URI(
                scheme,
                userInfo,
                host,
                0,
                path,
                null,
                null);

        return uri;
    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.account_ssl) {
            onUseSslChanged(isChecked);
        }
    }

    public void onUseSslChanged(boolean useSsl) {
        int mode = useSsl ? View.VISIBLE : View.GONE;
        mTrustCertificatesView.setVisibility(mode);
        UiUtilities.setVisibilitySafe(getView(), R.id.account_trust_certificates_divider, mode);
        mClientCertificateSelector.setVisibility(mode);
        UiUtilities.setVisibilitySafe(getView(), R.id.client_certificate_divider, mode);
    }

    @Override
    public void onCheckSettingsComplete(final int result) {
        if (result == AccountCheckSettingsFragment.CHECK_SETTINGS_CLIENT_CERTIFICATE_NEEDED) {
            mSslSecurityView.setChecked(true);
            onCertificateRequested();
            return;
        }
        super.onCheckSettingsComplete(result);
    }


    /**
     * Entry point from Activity after editing settings and verifying them.  Must be FLOW_MODE_EDIT.
     * Blocking - do not call from UI Thread.
     */
    @Override
    public void saveSettingsAfterEdit() {
        Account account = getCurrentAccount();
        account.mHostAuthRecv.update(mContext, account.mHostAuthRecv.toContentValues());
        account.mHostAuthSend.update(mContext, account.mHostAuthSend.toContentValues());
        // For EAS, notify ExchangeService that the password has changed
        try {
            EmailServiceUtils.getExchangeService(mContext, null).hostChanged(account.mId);
        } catch (RemoteException e) {
            // Nothing to be done if this fails
        }
        // Update the backup (side copy) of the accounts
        AccountBackupRestore.backup(mContext);
    }

    /**
     * Entry point from Activity after entering new settings and verifying them.  For setup mode.
     */
    @Override
    public void saveSettingsAfterSetup() {
    }

    /**
     * Entry point from Activity after entering new settings and verifying them.  For setup mode.
     */
    public boolean setHostAuthFromAutodiscover(HostAuth newHostAuth) {
        Account account = getCurrentAccount();
        account.mHostAuthSend = newHostAuth;
        account.mHostAuthRecv = newHostAuth;
        // Auto discovery may have changed the auth settings; force load them
        return forceLoadSettings(account);
    }

    /**
     * Implements AccountCheckSettingsFragment.Callbacks
     */
    @Override
    public void onAutoDiscoverComplete(int result, HostAuth hostAuth) {
        AccountSetupExchange activity = (AccountSetupExchange) getActivity();
        activity.onAutoDiscoverComplete(result, hostAuth);
    }

    /**
     * Entry point from Activity, when "next" button is clicked
     */
    @Override
    public void onNext() {
        Account account = getCurrentAccount();

        String userName = mUsernameView.getText().toString().trim();
        if (userName.startsWith("\\")) {
            userName = userName.substring(1);
        }
        /**
         * M: Modified for adding accounts which have same user name and
         * different domain.eg:test@gmail.com and test@163.com @{
         */
        mCacheLoginCredential = userName;
        /** @}*/
        String userPassword = mPasswordView.getText().toString();

        int flags = 0;
        if (mSslSecurityView.isChecked()) {
            flags |= HostAuth.FLAG_SSL;
        }
        if (mTrustCertificatesView.isChecked()) {
            flags |= HostAuth.FLAG_TRUST_ALL;
        }
        String certAlias = mClientCertificateSelector.getCertificate();
        String serverAddress = mServerView.getText().toString().trim();

        String portText = mPortView.getText().toString().trim();
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            // Worst case, do something sensible
            port = mSslSecurityView.isChecked() ? 443 : 80;
            Log.d(Logging.LOG_TAG, "Non-integer server port; using '" + port + "'");
        }

        HostAuth sendAuth = account.getOrCreateHostAuthSend(mContext);
        sendAuth.setLogin(userName, userPassword);
        sendAuth.setConnection(mBaseScheme, serverAddress, port, flags, certAlias);
        sendAuth.mDomain = null;

        HostAuth recvAuth = account.getOrCreateHostAuthRecv(mContext);
        recvAuth.setLogin(userName, userPassword);
        recvAuth.setConnection(mBaseScheme, serverAddress, port, flags, certAlias);
        recvAuth.mDomain = null;

        // Check for a duplicate account (requires async DB work) and if OK, proceed with check
        /**
         * M: Modified to avoid adding duplicated account which will be caused
         * when server's address is changed.eg:hotmail @{
         */
        startDuplicateTaskCheck(account.mId, recvAuth.mProtocol, mCacheLoginCredential, recvAuth.mAddress,
                SetupData.CHECK_INCOMING);
        /** @} */
    }

    @Override
    public void onCertificateRequested() {
        Intent intent = new Intent(CertificateRequestor.ACTION_REQUEST_CERT);
        intent.setData(Uri.parse("eas://com.android.emailcommon/certrequest"));
        startActivityForResult(intent, CERTIFICATE_REQUEST);
    }

    /**
     * M: Add for restore the SslSecurityView enable state
     */
    @Override
    public void onCertificateRemoved() {
        if (!mSslSecurityView.isEnabled()) {
            mSslSecurityView.setEnabled(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CERTIFICATE_REQUEST && resultCode == Activity.RESULT_OK) {
            String certAlias = data.getStringExtra(CertificateRequestor.RESULT_ALIAS);
            if (certAlias != null) {
                mClientCertificateSelector.setCertificate(certAlias);
                /// M: Disable the SslSecurityView,after Certificate has select and toast to
                // tell user why disable the SslSecurityView.
                mSslSecurityView.setEnabled(false);
                Utility.showToastShortTime(getActivity(), R.string.certificate_request_ssl);
            }
        }
    }

    private void initLengthFilter(){
        Context content = getActivity();
        if(content == null){
            Logging.e("This fragment may be finished, can't do some ui operation.");
            return;
        }
        UiUtilities.setupLengthFilter(mPasswordView, content,
                Email.EDITVIEW_MAX_LENGTH_1, true);
        UiUtilities.setupLengthFilter(mServerView,
                content, Email.EDITVIEW_MAX_LENGTH_1, true);
        UiUtilities.setupLengthFilter(mUsernameView,
                content, Email.EDITVIEW_MAX_LENGTH_1, true);
   }
}
