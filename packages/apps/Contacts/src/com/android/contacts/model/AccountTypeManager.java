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

package com.android.contacts.model;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncAdapterType;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.TimingLogger;

import com.android.contacts.ContactsUtils;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountTypeWithDataSet;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.ExchangeAccountType;
import com.android.contacts.model.account.ExternalAccountType;
import com.android.contacts.model.account.FallbackAccountType;
import com.android.contacts.model.account.GoogleAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.Constants;
import com.android.internal.util.Objects;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/* 
 * New Feature by Mediatek Begin.
 * Descriptions: crete sim/usim contact
 */
import com.android.contacts.util.PhoneCapabilityTester;
import com.mediatek.contacts.model.SimAccountType;
import com.mediatek.contacts.model.UimAccountType;
import com.mediatek.contacts.model.UsimAccountType;
import com.mediatek.contacts.model.LocalPhoneAccountType; 
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.simcontact.SimCardUtils.SimType;
import com.android.internal.telephony.TelephonyIntents;
/*
 * New Feature by Mediatek End.
 */

/**
 * Singleton holder for all parsed {@link AccountType} available on the
 * system, typically filled through {@link PackageManager} queries.
 */
public abstract class AccountTypeManager {
    static final String TAG = "AccountTypeManager";

    public static final String ACCOUNT_TYPE_SERVICE = "contactAccountTypes";

    /**
     * Requests the singleton instance of {@link AccountTypeManager} with data bound from
     * the available authenticators. This method can safely be called from the UI thread.
     */
    public static AccountTypeManager getInstance(Context context) {
        context = context.getApplicationContext();
        AccountTypeManager service =
                (AccountTypeManager) context.getSystemService(ACCOUNT_TYPE_SERVICE);
        if (service == null) {
            service = createAccountTypeManager(context);
            Log.e(TAG, "No account type service in context: " + context);
        }
        return service;
    }

    public static synchronized AccountTypeManager createAccountTypeManager(Context context) {
        return new AccountTypeManagerImpl(context);
    }

    /**
     * Returns the list of all accounts (if contactWritableOnly is false) or just the list of
     * contact writable accounts (if contactWritableOnly is true).
     */
    // TODO: Consider splitting this into getContactWritableAccounts() and getAllAccounts()
    public abstract List<AccountWithDataSet> getAccounts(boolean contactWritableOnly);

    /**
     * Returns the list of accounts that are group writable.
     */
    public abstract List<AccountWithDataSet> getGroupWritableAccounts();

    public abstract AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet);

    public final AccountType getAccountType(String accountType, String dataSet) {
        return getAccountType(AccountTypeWithDataSet.get(accountType, dataSet));
    }

    public final AccountType getAccountTypeForAccount(AccountWithDataSet account) {
        return getAccountType(account.getAccountTypeWithDataSet());
    }

    /**
     * @return Unmodifiable map from {@link AccountTypeWithDataSet}s to {@link AccountType}s
     * which support the "invite" feature and have one or more account.
     *
     * This is a filtered down and more "usable" list compared to
     * {@link #getAllInvitableAccountTypes}, where usable is defined as:
     * (1) making sure that the app that contributed the account type is not disabled
     * (in order to avoid presenting the user with an option that does nothing), and
     * (2) that there is at least one raw contact with that account type in the database
     * (assuming that the user probably doesn't use that account type).
     *
     * Warning: Don't use on the UI thread because this can scan the database.
     */
    public abstract Map<AccountTypeWithDataSet, AccountType> getUsableInvitableAccountTypes();

    /**
     * Find the best {@link DataKind} matching the requested
     * {@link AccountType#accountType}, {@link AccountType#dataSet}, and {@link DataKind#mimeType}.
     * If no direct match found, we try searching {@link FallbackAccountType}.
     */
    public DataKind getKindOrFallback(String accountType, String dataSet, String mimeType) {
        final AccountType type = getAccountType(accountType, dataSet);
        return type == null ? null : type.getKindForMimetype(mimeType);
    }

    /**
     * Returns all registered {@link AccountType}s, including extension ones.
     *
     * @param contactWritableOnly if true, it only returns ones that support writing contacts.
     */
    public abstract List<AccountType> getAccountTypes(boolean contactWritableOnly);

    /**
     * @param contactWritableOnly if true, it only returns ones that support writing contacts.
     * @return true when this instance contains the given account.
     */
    public boolean contains(AccountWithDataSet account, boolean contactWritableOnly) {
        for (AccountWithDataSet account_2 : getAccounts(false)) {
            if (account.equals(account_2)) {
                return true;
            }
        }
        return false;
    }
}

class AccountTypeManagerImpl extends AccountTypeManager
        implements OnAccountsUpdateListener, SyncStatusObserver {

    private static final Map<AccountTypeWithDataSet, AccountType>
            EMPTY_UNMODIFIABLE_ACCOUNT_TYPE_MAP =
            Collections.unmodifiableMap(new HashMap<AccountTypeWithDataSet, AccountType>());

    /**
     * A sample contact URI used to test whether any activities will respond to an
     * invitable intent with the given URI as the intent data. This doesn't need to be
     * specific to a real contact because an app that intercepts the intent should probably do so
     * for all types of contact URIs.
     */
    private static final Uri SAMPLE_CONTACT_URI = ContactsContract.Contacts.getLookupUri(
            1, "xxx");

    private Context mContext;
    private AccountManager mAccountManager;

    private AccountType mFallbackAccountType;

    private List<AccountWithDataSet> mAccounts = Lists.newArrayList();
    private List<AccountWithDataSet> mContactWritableAccounts = Lists.newArrayList();
    private List<AccountWithDataSet> mGroupWritableAccounts = Lists.newArrayList();
    private Map<AccountTypeWithDataSet, AccountType> mAccountTypesWithDataSets = Maps.newHashMap();
    private Map<AccountTypeWithDataSet, AccountType> mInvitableAccountTypes =
            EMPTY_UNMODIFIABLE_ACCOUNT_TYPE_MAP;

    private final InvitableAccountTypeCache mInvitableAccountTypeCache;

    /**
     * The boolean value is equal to true if the {@link InvitableAccountTypeCache} has been
     * initialized. False otherwise.
     */
    private final AtomicBoolean mInvitablesCacheIsInitialized = new AtomicBoolean(false);

    /**
     * The boolean value is equal to true if the {@link FindInvitablesTask} is still executing.
     * False otherwise.
     */
    private final AtomicBoolean mInvitablesTaskIsRunning = new AtomicBoolean(false);

    private static final int MESSAGE_LOAD_DATA = 0;
    private static final int MESSAGE_PROCESS_BROADCAST_INTENT = 1;

    private HandlerThread mListenerThread;
    private Handler mListenerHandler;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCheckFilterValidityRunnable = new Runnable () {
        @Override
        public void run() {
            ContactListFilterController.getInstance(mContext).checkFilterValidity(true);
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received Intent:" + intent);
            Message msg = mListenerHandler.obtainMessage(MESSAGE_PROCESS_BROADCAST_INTENT, intent);
            mListenerHandler.sendMessage(msg);
        }

    };

    /* A latch that ensures that asynchronous initialization completes before data is used */
    private volatile CountDownLatch mInitializationLatch = new CountDownLatch(1);

    private static final Comparator<Account> ACCOUNT_COMPARATOR = new Comparator<Account>() {
        @Override
        public int compare(Account a, Account b) {
            String aDataSet = null;
            String bDataSet = null;
            if (a instanceof AccountWithDataSet) {
                aDataSet = ((AccountWithDataSet) a).dataSet;
            }
            if (b instanceof AccountWithDataSet) {
                bDataSet = ((AccountWithDataSet) b).dataSet;
            }

            if (Objects.equal(a.name, b.name) && Objects.equal(a.type, b.type)
                    && Objects.equal(aDataSet, bDataSet)) {
                return 0;
            } else if (b.name == null || b.type == null) {
                return -1;
            } else if (a.name == null || a.type == null) {
                return 1;
            } else {
                int diff = a.name.compareTo(b.name);
                if (diff != 0) {
                    return diff;
                }
                diff = a.type.compareTo(b.type);
                if (diff != 0) {
                    return diff;
                }

                // Accounts without data sets get sorted before those that have them.
                if (aDataSet != null) {
                    return bDataSet == null ? 1 : aDataSet.compareTo(bDataSet);
                } else {
                    return -1;
                }
            }
        }
    };

    /**
     * Internal constructor that only performs initial parsing.
     */
    public AccountTypeManagerImpl(Context context) {
        mContext = context;
        mFallbackAccountType = new FallbackAccountType(context);

        mAccountManager = AccountManager.get(mContext);

        mListenerThread = new HandlerThread("AccountChangeListener");
        mListenerThread.start();
        mListenerHandler = new Handler(mListenerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_LOAD_DATA:
                        loadAccountsInBackground();
                        break;
                    case MESSAGE_PROCESS_BROADCAST_INTENT:
                        processBroadcastIntent((Intent) msg.obj);
                        break;
                }
            }
        };

        mInvitableAccountTypeCache = new InvitableAccountTypeCache();

        // Request updates when packages or accounts change
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        mContext.registerReceiver(mBroadcastReceiver, filter);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        mContext.registerReceiver(mBroadcastReceiver, sdFilter);

        // Request updates when locale is changed so that the order of each field will
        // be able to be changed on the locale change.
        filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);

        // The following lines are provided and maintained by Mediatek Inc.
        registerReceiverOnSimStateAndInfoChanged();
        // The previous lines are provided and maintained by Mediatek Inc.

        mAccountManager.addOnAccountsUpdatedListener(this, mListenerHandler, false);

        ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, this);

        mListenerHandler.sendEmptyMessage(MESSAGE_LOAD_DATA);
    }

    @Override
    public void onStatusChanged(int which) {
        mListenerHandler.sendEmptyMessage(MESSAGE_LOAD_DATA);
    }

    public void processBroadcastIntent(Intent intent) {
        mListenerHandler.sendEmptyMessage(MESSAGE_LOAD_DATA);
    }

    /* This notification will arrive on the background thread */
    public void onAccountsUpdated(Account[] accounts) {
        // Refresh to catch any changed accounts
        loadAccountsInBackground();
    }

    /**
     * Returns instantly if accounts and account types have already been loaded.
     * Otherwise waits for the background thread to complete the loading.
     */
    void ensureAccountsLoaded() {
        CountDownLatch latch = mInitializationLatch;
        if (latch == null) {
            return;
        }
        while (true) {
            try {
                latch.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Loads account list and corresponding account types (potentially with data sets). Always
     * called on a background thread.
     */
    protected void loadAccountsInBackground() {
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "AccountTypeManager.loadAccountsInBackground start");
        }
        Log.i(TAG, "loadAccountsInBackground()+");
        TimingLogger timings = new TimingLogger(TAG, "loadAccountsInBackground");
        final long startTime = SystemClock.currentThreadTimeMillis();
        final long startTimeWall = SystemClock.elapsedRealtime();

        // Account types, keyed off the account type and data set concatenation.
        final Map<AccountTypeWithDataSet, AccountType> accountTypesByTypeAndDataSet =
                Maps.newHashMap();

        // The same AccountTypes, but keyed off {@link RawContacts#ACCOUNT_TYPE}.  Since there can
        // be multiple account types (with different data sets) for the same type of account, each
        // type string may have multiple AccountType entries.
        final Map<String, List<AccountType>> accountTypesByType = Maps.newHashMap();

        final List<AccountWithDataSet> allAccounts = Lists.newArrayList();
        final List<AccountWithDataSet> contactWritableAccounts = Lists.newArrayList();
        final List<AccountWithDataSet> groupWritableAccounts = Lists.newArrayList();
        final Set<String> extensionPackages = Sets.newHashSet();

        final AccountManager am = mAccountManager;
        final IContentService cs = ContentResolver.getContentService();

        try {
            final SyncAdapterType[] syncs = cs.getSyncAdapterTypes();
            final AuthenticatorDescription[] auths = am.getAuthenticatorTypes();

            // First process sync adapters to find any that provide contact data.
            for (SyncAdapterType sync : syncs) {
                if (!ContactsContract.AUTHORITY.equals(sync.authority)) {
                    // Skip sync adapters that don't provide contact data.
                    continue;
                }

                // Look for the formatting details provided by each sync
                // adapter, using the authenticator to find general resources.
                final String type = sync.accountType;
                Log.i(TAG, "the AuthenticatorDescription [] auths = " + auths);
                final AuthenticatorDescription auth = findAuthenticator(auths, type);
                if (auth == null) {
                    Log.w(TAG, "No authenticator found for type=" + type + ", ignoring it.");
                    continue;
                }
                Log.i("AccountTypeManager", "the   type is " + type + " the auth is = " + auth);
                AccountType accountType;
                if (GoogleAccountType.ACCOUNT_TYPE.equals(type)) {
                    accountType = new GoogleAccountType(mContext, auth.packageName);
                } else if (ExchangeAccountType.isExchangeType(type)) {
                    accountType = new ExchangeAccountType(mContext, auth.packageName, type);
                } else {
                    // TODO: use syncadapter package instead, since it provides resources
                    Log.d(TAG, "Registering external account type=" + type
                            + ", packageName=" + auth.packageName);
                    accountType = new ExternalAccountType(mContext, auth.packageName, false);
                }
                if (!accountType.isInitialized()) {
                    if (accountType.isEmbedded()) {
                        throw new IllegalStateException("Problem initializing embedded type "
                                + accountType.getClass().getCanonicalName());
                    } else {
                        // Skip external account types that couldn't be initialized.
                        continue;
                    }
                }

                accountType.accountType = auth.type;
                accountType.titleRes = auth.labelId;
                accountType.iconRes = auth.iconId;

                addAccountType(accountType, accountTypesByTypeAndDataSet, accountTypesByType);

                // Check to see if the account type knows of any other non-sync-adapter packages
                // that may provide other data sets of contact data.
                extensionPackages.addAll(accountType.getExtensionPackageNames());
            }

            // The following lines are provided and maintained by Mediatek Inc.
            addAccountType(new SimAccountType(mContext, null), accountTypesByTypeAndDataSet,
                    accountTypesByType);
            addAccountType(new UsimAccountType(mContext, null), accountTypesByTypeAndDataSet,
                    accountTypesByType);
            addAccountType(new LocalPhoneAccountType(mContext, null), accountTypesByTypeAndDataSet,
                    accountTypesByType);
            //UIM
            addAccountType(new UimAccountType(mContext, null), accountTypesByTypeAndDataSet,
                    accountTypesByType);
            //UIM
            // The previous lines are provided and maintained by Mediatek Inc.

            // If any extension packages were specified, process them as well.
            if (!extensionPackages.isEmpty()) {
                Log.d(TAG, "Registering " + extensionPackages.size() + " extension packages");
                for (String extensionPackage : extensionPackages) {
                    ExternalAccountType accountType =
                            new ExternalAccountType(mContext, extensionPackage, true);
                    if (!accountType.isInitialized()) {
                        // Skip external account types that couldn't be initialized.
                        continue;
                    }
                    if (!accountType.hasContactsMetadata()) {
                        Log.w(TAG, "Skipping extension package " + extensionPackage + " because"
                                + " it doesn't have the CONTACTS_STRUCTURE metadata");
                        continue;
                    }
                    if (TextUtils.isEmpty(accountType.accountType)) {
                        Log.w(TAG, "Skipping extension package " + extensionPackage + " because"
                                + " the CONTACTS_STRUCTURE metadata doesn't have the accountType"
                                + " attribute");
                        continue;
                    }
                    Log.d(TAG, "Registering extension package account type="
                            + accountType.accountType + ", dataSet=" + accountType.dataSet
                            + ", packageName=" + extensionPackage);

                    addAccountType(accountType, accountTypesByTypeAndDataSet, accountTypesByType);
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Problem loading accounts: " + e.toString());
        }
        timings.addSplit("Loaded account types");

        // Map in accounts to associate the account names with each account type entry.
        Account[] accounts = mAccountManager.getAccounts();
        for (Account account : accounts) {
            boolean syncable = false;
            try {
                syncable = cs.getIsSyncable(account, ContactsContract.AUTHORITY) > 0;
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot obtain sync flag for account: " + account, e);
            }

            if (syncable) {
                List<AccountType> accountTypes = accountTypesByType.get(account.type);
                if (accountTypes != null) {
                    // Add an account-with-data-set entry for each account type that is
                    // authenticated by this account.
                    for (AccountType accountType : accountTypes) {
                        AccountWithDataSet accountWithDataSet = new AccountWithDataSet(
                                account.name, account.type, accountType.dataSet);
                        allAccounts.add(accountWithDataSet);
                        if (accountType.areContactsWritable()) {
                            contactWritableAccounts.add(accountWithDataSet);
                        }
                        if (accountType.isGroupMembershipEditable()) {
                            groupWritableAccounts.add(accountWithDataSet);
                        }
                    }
                }
            }
        }

        Collections.sort(allAccounts, ACCOUNT_COMPARATOR);
        Collections.sort(contactWritableAccounts, ACCOUNT_COMPARATOR);
        Collections.sort(groupWritableAccounts, ACCOUNT_COMPARATOR);

        // The following lines are provided and maintained by Mediatek Inc.
        ///M: [Gemini+] add sim account @{
        for (int slotId : SlotUtils.getAllSlotIds()) {
            if (SimCardUtils.isSimInserted(slotId)) {
                String simAccountType = getAccountTypeBySlot(slotId);
                String simName = getSimAccountNameBySlot(slotId);
                Log.i(TAG, "loadAccountsInBackground slotid:" + slotId + " AccountType:" + simAccountType + " simName:"
                        + simName);
                if (!TextUtils.isEmpty(simName) && !TextUtils.isEmpty(simAccountType)) {
                    allAccounts.add(new AccountWithDataSetEx(simName, simAccountType, slotId));
                    contactWritableAccounts.add(new AccountWithDataSetEx(simName, simAccountType, slotId));
                    if (SimCardUtils.isSimUsimType(slotId)) {
                        groupWritableAccounts.add(new AccountWithDataSetEx(simName, simAccountType, slotId));
                    }
                }
            }
        }
        ///@}

        // Add Phone Local Type
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     
         *   CR ID: ALPS00258229
         *   Descriptions: if it is tablet let accountName is tablet
         */
        if (PhoneCapabilityTester.isUsingTwoPanes(mContext)) {
            Log.i(TAG,"it is tablet");
            allAccounts.add(new AccountWithDataSet("Tablet", AccountType.ACCOUNT_TYPE_LOCAL_PHONE,
                    null));
            contactWritableAccounts.add(new AccountWithDataSet("Tablet",
                    AccountType.ACCOUNT_TYPE_LOCAL_PHONE, null));
            groupWritableAccounts.add(new AccountWithDataSet("Tablet",
                    AccountType.ACCOUNT_TYPE_LOCAL_PHONE, null));
        } else {
            Log.i(TAG,"it is phone");
            allAccounts.add(new AccountWithDataSet("Phone", AccountType.ACCOUNT_TYPE_LOCAL_PHONE,
                    null));
            contactWritableAccounts.add(new AccountWithDataSet("Phone",
                    AccountType.ACCOUNT_TYPE_LOCAL_PHONE, null));
            groupWritableAccounts.add(new AccountWithDataSet("Phone",
                    AccountType.ACCOUNT_TYPE_LOCAL_PHONE, null));
        }
        /*
         * Bug Fix by Mediatek End.
         */
        // The previous lines are provided and maintained by Mediatek Inc.

        timings.addSplit("Loaded accounts");

        synchronized (this) {
            mAccountTypesWithDataSets = accountTypesByTypeAndDataSet;
            mAccounts = allAccounts;
            mContactWritableAccounts = contactWritableAccounts;
            mGroupWritableAccounts = groupWritableAccounts;
            mInvitableAccountTypes = findAllInvitableAccountTypes(
                    mContext, allAccounts, accountTypesByTypeAndDataSet);
        }

        timings.dumpToLog();
        final long endTimeWall = SystemClock.elapsedRealtime();
        final long endTime = SystemClock.currentThreadTimeMillis();

        Log.i(TAG, "Loaded meta-data for " + mAccountTypesWithDataSets.size() + " account types, "
                + mAccounts.size() + " accounts in " + (endTimeWall - startTimeWall) + "ms(wall) "
                + (endTime - startTime) + "ms(cpu)");

        if (mInitializationLatch != null) {
            mInitializationLatch.countDown();
            mInitializationLatch = null;
        }
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "AccountTypeManager.loadAccountsInBackground finish");
        }

        // Check filter validity since filter may become obsolete after account update. It must be
        // done from UI thread.
        /**
         * M: Fixed CR ALPS00538943
         * Delay check filter, let AccountManager has time to load SIM/USIM Accounts;
         * origin code: mMainThreadHandler.post(mCheckFilterValidityRunnable);
         * @ { */
        mMainThreadHandler.postAtTime(mCheckFilterValidityRunnable, System.currentTimeMillis() + 1000);
        /** @ } */

        Log.i(TAG, "loadAccountsInBackground()-");
    }

    // Bookkeeping method for tracking the known account types in the given maps.
    private void addAccountType(AccountType accountType,
            Map<AccountTypeWithDataSet, AccountType> accountTypesByTypeAndDataSet,
            Map<String, List<AccountType>> accountTypesByType) {
        accountTypesByTypeAndDataSet.put(accountType.getAccountTypeAndDataSet(), accountType);
        List<AccountType> accountsForType = accountTypesByType.get(accountType.accountType);
        if (accountsForType == null) {
            accountsForType = Lists.newArrayList();
        }
        accountsForType.add(accountType);
        accountTypesByType.put(accountType.accountType, accountsForType);
    }

    /**
     * Find a specific {@link AuthenticatorDescription} in the provided list
     * that matches the given account type.
     */
    protected static AuthenticatorDescription findAuthenticator(AuthenticatorDescription[] auths,
            String accountType) {
        for (AuthenticatorDescription auth : auths) {
            if (accountType.equals(auth.type)) {
                return auth;
            }
        }
        return null;
    }

    /**
     * Return list of all known, contact writable {@link AccountWithDataSet}'s.
     */
    @Override
    public List<AccountWithDataSet> getAccounts(boolean contactWritableOnly) {
        ensureAccountsLoaded();
        return contactWritableOnly ? mContactWritableAccounts : mAccounts;
    }

    /**
     * Return the list of all known, group writable {@link AccountWithDataSet}'s.
     */
    public List<AccountWithDataSet> getGroupWritableAccounts() {
        ensureAccountsLoaded();
        return mGroupWritableAccounts;
    }

    /**
     * Find the best {@link DataKind} matching the requested
     * {@link AccountType#accountType}, {@link AccountType#dataSet}, and {@link DataKind#mimeType}.
     * If no direct match found, we try searching {@link FallbackAccountType}.
     */
    @Override
    public DataKind getKindOrFallback(String accountType, String dataSet, String mimeType) {
        ensureAccountsLoaded();
        DataKind kind = null;

        // Try finding account type and kind matching request
        final AccountType type = mAccountTypesWithDataSets.get(
                AccountTypeWithDataSet.get(accountType, dataSet));
        if (type != null) {
            kind = type.getKindForMimetype(mimeType);
        }

        if (kind == null) {
            // Nothing found, so try fallback as last resort
            kind = mFallbackAccountType.getKindForMimetype(mimeType);
        }

        if (kind == null) {
            Log.w(TAG, "Unknown type=" + accountType + ", mime=" + mimeType);
        }

        return kind;
    }

    /**
     * Return {@link AccountType} for the given account type and data set.
     */
    @Override
    public AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet) {
        ensureAccountsLoaded();
        synchronized (this) {
            AccountType type = mAccountTypesWithDataSets.get(accountTypeWithDataSet);
            return type != null ? type : mFallbackAccountType;
        }
    }

    /**
     * @return Unmodifiable map from {@link AccountTypeWithDataSet}s to {@link AccountType}s
     * which support the "invite" feature and have one or more account. This is an unfiltered
     * list. See {@link #getUsableInvitableAccountTypes()}.
     */
    private Map<AccountTypeWithDataSet, AccountType> getAllInvitableAccountTypes() {
        ensureAccountsLoaded();
        return mInvitableAccountTypes;
    }

    @Override
    public Map<AccountTypeWithDataSet, AccountType> getUsableInvitableAccountTypes() {
        ensureAccountsLoaded();
        // Since this method is not thread-safe, it's possible for multiple threads to encounter
        // the situation where (1) the cache has not been initialized yet or
        // (2) an async task to refresh the account type list in the cache has already been
        // started. Hence we use {@link AtomicBoolean}s and return cached values immediately
        // while we compute the actual result in the background. We use this approach instead of
        // using "synchronized" because computing the account type list involves a DB read, and
        // can potentially cause a deadlock situation if this method is called from code which
        // holds the DB lock. The trade-off of potentially having an incorrect list of invitable
        // account types for a short period of time seems more manageable than enforcing the
        // context in which this method is called.

        // Computing the list of usable invitable account types is done on the fly as requested.
        // If this method has never been called before, then block until the list has been computed.
        if (!mInvitablesCacheIsInitialized.get()) {
            mInvitableAccountTypeCache.setCachedValue(findUsableInvitableAccountTypes(mContext));
            mInvitablesCacheIsInitialized.set(true);
        } else {
            // Otherwise, there is a value in the cache. If the value has expired and
            // an async task has not already been started by another thread, then kick off a new
            // async task to compute the list.
            if (mInvitableAccountTypeCache.isExpired() &&
                    mInvitablesTaskIsRunning.compareAndSet(false, true)) {
                new FindInvitablesTask().execute();
            }
        }

        return mInvitableAccountTypeCache.getCachedValue();
    }

    /**
     * Return all {@link AccountType}s with at least one account which supports "invite", i.e.
     * its {@link AccountType#getInviteContactActivityClassName()} is not empty.
     */
    @VisibleForTesting
    static Map<AccountTypeWithDataSet, AccountType> findAllInvitableAccountTypes(Context context,
            Collection<AccountWithDataSet> accounts,
            Map<AccountTypeWithDataSet, AccountType> accountTypesByTypeAndDataSet) {
        HashMap<AccountTypeWithDataSet, AccountType> result = Maps.newHashMap();
        for (AccountWithDataSet account : accounts) {
            AccountTypeWithDataSet accountTypeWithDataSet = account.getAccountTypeWithDataSet();
            AccountType type = accountTypesByTypeAndDataSet.get(accountTypeWithDataSet);
            if (type == null) continue; // just in case
            if (result.containsKey(accountTypeWithDataSet)) continue;

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Type " + accountTypeWithDataSet
                        + " inviteClass=" + type.getInviteContactActivityClassName());
            }
            if (!TextUtils.isEmpty(type.getInviteContactActivityClassName())) {
                result.put(accountTypeWithDataSet, type);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Return all usable {@link AccountType}s that support the "invite" feature from the
     * list of all potential invitable account types (retrieved from
     * {@link #getAllInvitableAccountTypes}). A usable invitable account type means:
     * (1) there is at least 1 raw contact in the database with that account type, and
     * (2) the app contributing the account type is not disabled.
     *
     * Warning: Don't use on the UI thread because this can scan the database.
     */
    private Map<AccountTypeWithDataSet, AccountType> findUsableInvitableAccountTypes(
            Context context) {
        Map<AccountTypeWithDataSet, AccountType> allInvitables = getAllInvitableAccountTypes();
        if (allInvitables.isEmpty()) {
            return EMPTY_UNMODIFIABLE_ACCOUNT_TYPE_MAP;
        }

        final HashMap<AccountTypeWithDataSet, AccountType> result = Maps.newHashMap();
        result.putAll(allInvitables);

        final PackageManager packageManager = context.getPackageManager();
        for (AccountTypeWithDataSet accountTypeWithDataSet : allInvitables.keySet()) {
            AccountType accountType = allInvitables.get(accountTypeWithDataSet);

            // Make sure that account types don't come from apps that are disabled.
            Intent invitableIntent = ContactsUtils.getInvitableIntent(accountType,
                    SAMPLE_CONTACT_URI);
            if (invitableIntent == null) {
                result.remove(accountTypeWithDataSet);
                continue;
            }
            ResolveInfo resolveInfo = packageManager.resolveActivity(invitableIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo == null) {
                // If we can't find an activity to start for this intent, then there's no point in
                // showing this option to the user.
                result.remove(accountTypeWithDataSet);
                continue;
            }

            // Make sure that there is at least 1 raw contact with this account type. This check
            // is non-trivial and should not be done on the UI thread.
            if (!accountTypeWithDataSet.hasData(context)) {
                result.remove(accountTypeWithDataSet);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    public List<AccountType> getAccountTypes(boolean contactWritableOnly) {
        ensureAccountsLoaded();
        final List<AccountType> accountTypes = Lists.newArrayList();
        synchronized (this) {
            for (AccountType type : mAccountTypesWithDataSets.values()) {
                if (!contactWritableOnly || type.areContactsWritable()) {
                    accountTypes.add(type);
                }
            }
        }
        return accountTypes;
    }

    /**
     * Background task to find all usable {@link AccountType}s that support the "invite" feature
     * from the list of all potential invitable account types. Once the work is completed,
     * the list of account types is stored in the {@link AccountTypeManager}'s
     * {@link InvitableAccountTypeCache}.
     */
    private class FindInvitablesTask extends AsyncTask<Void, Void,
            Map<AccountTypeWithDataSet, AccountType>> {

        @Override
        protected Map<AccountTypeWithDataSet, AccountType> doInBackground(Void... params) {
            return findUsableInvitableAccountTypes(mContext);
        }

        @Override
        protected void onPostExecute(Map<AccountTypeWithDataSet, AccountType> accountTypes) {
            mInvitableAccountTypeCache.setCachedValue(accountTypes);
            mInvitablesTaskIsRunning.set(false);
        }
    }

    /**
     * This cache holds a list of invitable {@link AccountTypeWithDataSet}s, in the form of a
     * {@link Map<AccountTypeWithDataSet, AccountType>}. Note that the cached value is valid only
     * for {@link #TIME_TO_LIVE} milliseconds.
     */
    private static final class InvitableAccountTypeCache {

        /**
         * The cached {@link #mInvitableAccountTypes} list expires after this number of milliseconds
         * has elapsed.
         */
        private static final long TIME_TO_LIVE = 60000;

        private Map<AccountTypeWithDataSet, AccountType> mInvitableAccountTypes;

        private long mTimeLastSet;

        /**
         * Returns true if the data in this cache is stale and needs to be refreshed. Returns false
         * otherwise.
         */
        public boolean isExpired() {
             return SystemClock.elapsedRealtime() - mTimeLastSet > TIME_TO_LIVE;
        }

        /**
         * Returns the cached value. Note that the caller is responsible for checking
         * {@link #isExpired()} to ensure that the value is not stale.
         */
        public Map<AccountTypeWithDataSet, AccountType> getCachedValue() {
            return mInvitableAccountTypes;
        }

        public void setCachedValue(Map<AccountTypeWithDataSet, AccountType> map) {
            mInvitableAccountTypes = map;
            mTimeLastSet = SystemClock.elapsedRealtime();
        }
    }

    // The following lines are provided and maintained by Mediatek inc.
    public String getAccountTypeBySlot(int slotId) {
        Log.i(TAG, "getAccountTypeBySlot()+ - slotId:" + slotId);
        if (!SlotUtils.isSlotValid(slotId)) {
            Log.e(TAG, "Error! - slot id error. slotid:" + slotId);
            return null;
        }
        int simtype = -1;
        String simAccountType = null;

        if (SimCardUtils.isSimInserted(slotId)) {
            simtype = SimCardUtils.getSimTypeBySlot(slotId);
            if (SimCardUtils.SimType.SIM_TYPE_USIM == simtype) {
                simAccountType = AccountType.ACCOUNT_TYPE_USIM;
            } else if (SimCardUtils.SimType.SIM_TYPE_SIM == simtype) {
                simAccountType = AccountType.ACCOUNT_TYPE_SIM;
            } else if (SimCardUtils.SimType.SIM_TYPE_UIM == simtype) {
                simAccountType = AccountType.ACCOUNT_TYPE_UIM;
            }
        } else {
            Log.e(TAG, "Error! getAccountTypeBySlot - slotId:" + slotId + " no sim inserted!");
            simAccountType = null;
        }
        Log.i(TAG, "getAccountTypeBySlot()- - slotId:" + slotId + " AccountType:" + simAccountType);
        return simAccountType;
    }

    public String getSimAccountNameBySlot(int slotId) {
        String retSimName = null;
        int simType = -1;

        Log.i(TAG, "getSimAccountNameBySlot()+ slotId:" + slotId);
        if (!SimCardUtils.isSimInserted(slotId)) {
            Log.e(TAG, "getSimAccountNameBySlot Error! - SIM not inserted!");
            return retSimName;
        }

        simType = SimCardUtils.getSimTypeBySlot(slotId);
        Log.i(TAG, "getSimAccountNameBySlot() slotId:" + slotId + " simType(0-SIM/1-USIM):" + simType);

        retSimName = AccountType.getSimAccountName(simType, slotId);

        Log.i(TAG, "getSimAccountNameBySlot()- slotId:" + slotId + " SimName:" + retSimName);
        return retSimName;
    }

    private void registerReceiverOnSimStateAndInfoChanged() {
        Log.i(TAG, "registerReceiverOnSimStateAndInfoChanged");
        IntentFilter simFilter = new IntentFilter();
        // For SIM Info Changed
        simFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        simFilter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
        // ToDo: Add SIM State Changed
        mContext.registerReceiver(mBroadcastReceiver, simFilter);
    }

    /* M: unused function
    public String getDisplayNameBySlot(int slotId) {
        String retSimDisplayName = null;
        int simId = -1;

        if (SimCardUtils.SimSlot.SLOT_SINGLE > slotId
                || SimCardUtils.SimSlot.SLOT_ID2 < slotId) {
            Log.e(TAG, "getDisplayNameBySlot Error!! - error slotId:" + slotId);
            return null;
        }

        SIMInfoWrapper sw = SIMInfoWrapper.getDefault();
        if (null != sw) {
            simId = sw.getSimIdBySlotId(slotId);
            retSimDisplayName = sw.getSimDisplayNameById(simId);
        } else {
            Log.e(TAG, "getDisplayNameBySlot Error!! - SIMInfoWrapper instance is null!");
        }

        Log.e(TAG, "getDisplayNameBySlot slotId:" + slotId + " simId:" + simId 
                + " simName:" + retSimDisplayName);

        return retSimDisplayName;
    }
    */
    // The previous lines are provided and maintained by Mediatek inc.
}
