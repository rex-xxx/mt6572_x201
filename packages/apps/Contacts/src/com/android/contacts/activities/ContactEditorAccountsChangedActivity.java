/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.contacts.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.Intents;
/// The following lines are provided and maintained by Mediatek Inc.
import android.provider.Telephony.SIMInfo;
import android.util.Log;
/// The previous lines are provided and maintained by Mediatek Inc.
import android.view.View;
import android.view.View.OnClickListener;
/// The following lines are provided and maintained by Mediatek Inc.
import android.view.WindowManager;
/// The previous lines are provided and maintained by Mediatek Inc.
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.editor.ContactEditorUtils;
import com.android.contacts.model.AccountTypeManager;
/// The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.model.account.AccountType;
/// The previous lines are provided and maintained by Mediatek Inc.
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountsListAdapter;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
/// The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.vcard.VCardService;
/// The previous lines are provided and maintained by Mediatek Inc.

/* 
 * New Feature by Mediatek Begin.
 * Original Android's code:
 * 
 * CR ID: ALPS00101852
 * Descriptions: get sim info for create sim contact
 */
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
/*
 * Change Feature by Mediatek End.
 */

import java.util.List;


/**
 * This activity can be shown to the user when creating a new contact to inform the user about
 * which account the contact will be saved in. There is also an option to add an account at
 * this time. The {@link Intent} in the activity result will contain an extra
 * {@link #Intents.Insert.ACCOUNT} that contains the {@link AccountWithDataSet} to create
 * the new contact in. If the activity result doesn't contain intent data, then there is no
 * account for this contact.
 */
public class ContactEditorAccountsChangedActivity extends Activity {

    private static final String TAG = ContactEditorAccountsChangedActivity.class.getSimpleName();

    private static final int SUBACTIVITY_ADD_NEW_ACCOUNT = 1;

    private AccountsListAdapter mAccountListAdapter;
    private ContactEditorUtils mEditorUtils;

    private final OnItemClickListener mAccountListItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mAccountListAdapter == null) {
                return;
            }
            /*
             * New Feature by Mediatek Begin. Descriptions: get sim info for
             * create sim contact
             */

            String accountType = mAccountListAdapter.getItem(position).type.toString();

            if (AccountType.isAccountTypeIccCard(accountType)) {
                AccountWithDataSet ads = mAccountListAdapter.getItem(position);

                mSlotId = SlotUtils.getNonSlotId();
                if (ads instanceof AccountWithDataSetEx) {
                    mSlotId = ((AccountWithDataSetEx) ads).getSlotId();
                }
                int nRet = mCellMgr.handleCellConn(mSlotId, REQUEST_TYPE);
                Log.i(TAG, "[OnItemClickListener]the mslotdi is = " + mSlotId + " the nRet is = "
                        + nRet);
                Log.i(TAG, "the account is " + mAccountListAdapter.getItem(position).type
                        + " the name is = " + mAccountListAdapter.getItem(position).name);
                Log.i(TAG, "the mCheckCount = " + mCheckCount);
                mCheckCount++;
                getPosition(position);

            } else {
                saveAccountAndReturnResult(mAccountListAdapter.getItem(position));
            }
            /*
             * New Feature by Mediatek End.
             */
        }
    };

    private final OnClickListener mAddAccountClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivityForResult(mEditorUtils.createAddWritableAccountIntent(),
                    SUBACTIVITY_ADD_NEW_ACCOUNT);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);
        
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     xxx
         *   CR ID: ALPS00251666
         *   Descriptions: can not add contact when in delete processing
         */
        
        if (MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)
                || MultiChoiceService.isProcessing(MultiChoiceService.TYPE_COPY)
                || VCardService.isProcessing(VCardService.TYPE_IMPORT)) {
            Log.i(TAG, "delete or copy is processing ");
            finish();
        }
        /*
         * Bug Fix by Mediatek End.
         */
        /*
         * New Feature by Mediatek Begin.
         * Descriptions: get sim info for create
         * sim contact
         */

        mCellMgr.register(this);

        /*
         * Change Feature by Mediatek End.
         */

        mEditorUtils = ContactEditorUtils.getInstance(this);
        
    }

    @Override
    protected void onResume() {
        super.onResume();

        final List<AccountWithDataSet> accounts = AccountTypeManager.getInstance(this).getAccounts(
                true);
        final int numAccounts = accounts.size();
        if (numAccounts < 0) {
            throw new IllegalStateException("Cannot have a negative number of accounts");
        }

        if (numAccounts >= 2) {
            // When the user has 2+ writable accounts, show a list of accounts
            // so the user can pick
            // which account to create a contact in.
            setContentView(R.layout.contact_editor_accounts_changed_activity_with_picker);

            final TextView textView = (TextView) findViewById(R.id.text);

            // The following lines are provided and maintained by Mediatek inc.
            textView.setText(getString(R.string.store_contact_to));
            changeTitleThemeColor();
            // The following lines are provided and maintained by Mediatek inc.

            final Button button = (Button) findViewById(R.id.add_account_button);
            button.setText(getString(R.string.add_new_account));
            button.setOnClickListener(mAddAccountClickListener);

            final ListView accountListView = (ListView) findViewById(R.id.account_list);
            mAccountListAdapter = new AccountsListAdapter(this,
                    AccountListFilter.ACCOUNTS_CONTACT_WRITABLE);
            accountListView.setAdapter(mAccountListAdapter);
            accountListView.setOnItemClickListener(mAccountListItemClickListener);
        } else if (numAccounts == 1) {
            // If the user has 1 writable account we will just show the user a
            // message with 2
            // possible action buttons.
            setContentView(R.layout.contact_editor_accounts_changed_activity_with_text);

            final TextView textView = (TextView) findViewById(R.id.text);
            final Button leftButton = (Button) findViewById(R.id.left_button);
            final Button rightButton = (Button) findViewById(R.id.right_button);

            final AccountWithDataSet account = accounts.get(0);
            textView.setText(getString(R.string.contact_editor_prompt_one_account, account.name));
            changeTitleThemeColor();
            // This button allows the user to add a new account to the device
            // and return to
            // this app afterwards.
            leftButton.setText(getString(R.string.add_new_account));
            leftButton.setOnClickListener(mAddAccountClickListener);

            // This button allows the user to continue creating the contact in
            // the specified
            // account.
            rightButton.setText(getString(android.R.string.ok));
            rightButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveAccountAndReturnResult(account);
                }
            });
        } else {
            // If the user has 0 writable accounts, we will just show the user a
            // message with 2
            // possible action buttons.
            setContentView(R.layout.contact_editor_accounts_changed_activity_with_text);

            final TextView textView = (TextView) findViewById(R.id.text);
            final Button leftButton = (Button) findViewById(R.id.left_button);
            final Button rightButton = (Button) findViewById(R.id.right_button);

            textView.setText(getString(R.string.contact_editor_prompt_zero_accounts));
            changeTitleThemeColor();
            // This button allows the user to continue editing the contact as a
            // phone-only
            // local contact.
            leftButton.setText(getString(R.string.keep_local));
            leftButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Remember that the user wants to create local contacts, so
                    // the user is not
                    // prompted again with this activity.
                    mEditorUtils.saveDefaultAndAllAccounts(null);
                    setResult(RESULT_OK);
                    finish();
                }
            });

            // This button allows the user to add a new account to the device
            // and return to
            // this app afterwards.
            rightButton.setText(getString(R.string.add_account));
            rightButton.setOnClickListener(mAddAccountClickListener);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SUBACTIVITY_ADD_NEW_ACCOUNT) {
            // If the user canceled the account setup process, then keep this activity visible to
            // the user.
            if (resultCode != RESULT_OK) {
                return;
            }
            // Subactivity was successful, so pass the result back and finish the activity.
            AccountWithDataSet account = mEditorUtils.getCreatedAccount(resultCode, data);
            if (account == null) {
                setResult(resultCode);
                finish();
                return;
            }
            saveAccountAndReturnResult(account);
        }
    }

    private void saveAccountAndReturnResult(AccountWithDataSet account) {
        // Save this as the default account
        mEditorUtils.saveDefaultAndAllAccounts(account);

        // Pass account info in activity result intent
        Intent intent = new Intent();
        intent.putExtra(Intents.Insert.ACCOUNT, account);
        /*
         * New Feature by Mediatek Begin.
         * Descriptions: get sim info for create
         * sim contact
         */
        intent.putExtra("mSlotId", mSlotId);
        intent.putExtra("mSimId", mSimId);
        intent.putExtra("mIsSimType", mNewSimType);
        Log.i(TAG, " the mslotid and msimid is = " + mSlotId + "   " + mSimId + " | mNewSimType : "
                + mNewSimType);
        /*
         * Change Feature by Mediatek End.
         */
        setResult(RESULT_OK, intent);
       
        finish();
    }

    /*
     * New Feature by Mediatek Begin.
     * Descriptions: get sim info for create sim contact
     */
    @Override
    protected void onDestroy() {
        mCellMgr.unregister();
        super.onDestroy();
        Log.i(TAG, "[onDestroy]");
    }

    private Runnable mServiceComplete = new Runnable() {
        public void run() {
            Log.d(TAG, "mServiceComplete run");
            int nRet = mCellMgr.getResult();
            Log.d(TAG, "mServiceComplete result = " + CellConnMgr.resultToString(nRet));
            if (mCellMgr.RESULT_ABORT == nRet) {
                finish();
                return;
            } else {
                int errorToast = -1;
                boolean hitError = false;
                if (!SimCardUtils.isPhoneBookReady(mSlotId)) {
                    hitError = true;
                    errorToast = R.string.phone_book_busy;
                } else if (0 == SimCardUtils.ShowSimCardStorageInfoTask.getSurplugCount(mSlotId)) {
                    hitError = true;
                    errorToast = R.string.storage_full;
                }
                if (hitError) {
                    Toast.makeText(ContactEditorAccountsChangedActivity.this,
                            errorToast, Toast.LENGTH_LONG).show();
                    return;
                }
                SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(
                        ContactEditorAccountsChangedActivity.this, mSlotId);
                if (siminfo != null) {
                    mSimId = siminfo.mSimId;
                }
                Log.i(TAG, "mSimSelectionDialog mSimId is " + mSimId);
                mNewSimType = true;
                saveAccountAndReturnResult(mAccountListAdapter.getItem(mPosition));
                return;
            }
        }
    };

    public void getPosition(int i) {
        mPosition = i;

    }
    private CellConnMgr mCellMgr = new CellConnMgr(mServiceComplete);
    private int mPosition;
    private boolean mNewSimType = false;
    
    /*
     * Change Feature by Mediatek End.
     */
    /*
     * New Feature by Mediatek Begin.
     * Descriptions: get sim info for create sim contact
     */
    private static final int REQUEST_TYPE = 304;
    private int mSlotId = -1;
    private long mSimId = -1;
    int mCheckCount = 0;
    /*
     * Change Feature by Mediatek End.
     */
    private void changeTitleThemeColor() {
        int colorValue = getApplicationContext().getResources().getThemeMainColor();
        if (colorValue != 0) {
            ((TextView)findViewById(R.id.text)).setTextColor(colorValue);
            findViewById(R.id.divider_line).setBackgroundColor(colorValue);
        }
    }
    
}