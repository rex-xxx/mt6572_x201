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
package com.android.contacts.vcard;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountSelectionUtil;

import java.util.List;

//The following lines are provided and maintained by Mediatek inc.
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.android.contacts.util.AccountSelectionUtil.AccountSelectedListener;
import com.android.contacts.model.account.AccountType;
import android.app.AlertDialog;
import java.util.ArrayList;
import android.content.Context;
//The following lines are provided and maintained by Mediatek inc.

public class SelectAccountActivity extends ContactsActivity {
    private static final String LOG_TAG = "SelectAccountActivity";

    public static final String ACCOUNT_NAME = "account_name";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String DATA_SET = "data_set";

    private class CancelListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
        public void onCancel(DialogInterface dialog) {
            finish();
        }
    }

    private AccountSelectionUtil.AccountSelectedListener mAccountSelectionListener;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // There's three possibilities:
        // - more than one accounts -> ask the user
        // - just one account -> use the account without asking the user
        // - no account -> use phone-local storage without asking the user
        final int resId = R.string.import_from_sdcard;
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
        final List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
        if (accountList.size() == 0) {
            Log.w(LOG_TAG, "Account does not exist");
            finish();
            return;
        } else if (accountList.size() == 1) {
            final AccountWithDataSet account = accountList.get(0);
            final Intent intent = new Intent();
            intent.putExtra(ACCOUNT_NAME, account.name);
            intent.putExtra(ACCOUNT_TYPE, account.type);
            intent.putExtra(DATA_SET, account.dataSet);
            setResult(RESULT_OK, intent);
            finish();
            return;
        }

        Log.i(LOG_TAG, "The number of available accounts: " + accountList.size());
        // The following lines are provided and maintained by Mediatek inc.
        List<AccountWithDataSet> myAccountlist = new ArrayList<AccountWithDataSet>();
        int k = 0;
        for (int i = 0; i < accountList.size(); i++) {
            AccountWithDataSet account1 = accountList.get(i);
            //UIM
//            if (!account1.type.equals(mSimAccountType) && !account1.type.equals(mUsimAccountType)) {
            if (!account1.type.equals(mSimAccountType) && !account1.type.equals(mUsimAccountType) && !account1.type.equals(mUimAccountType)) {
            //UIM
                Log.i("sdcard", "account1.type : " + account1.type);
                myAccountlist.add(accountList.get(i));
            }
        }
        Log.i("sdcard", "accountlist1.size() : " + myAccountlist.size());
        // The following lines are provided and maintained by Mediatek inc.
        // Multiple accounts. Let users to select one.
        mAccountSelectionListener =
         // The following lines are provided and maintained by Mediatek inc.
        // new AccountSelectionUtil.AccountSelectedListener(this, accountList,resId)
        new AccountSelectionUtil.AccountSelectedListener(this, myAccountlist, resId) {
         // The following lines are provided and maintained by Mediatek inc.
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Log.i("sdcard", "mAccountList.size() : " + mAccountList.size());
                final AccountWithDataSet account = mAccountList.get(which);
                final Intent intent = new Intent();
                intent.putExtra(ACCOUNT_NAME, account.name);
                intent.putExtra(ACCOUNT_TYPE, account.type);
                intent.putExtra(DATA_SET, account.dataSet);
                setResult(RESULT_OK, intent);
                finish();
            }
        };
        showDialog(resId);
        return;
    }

    @Override
    protected Dialog onCreateDialog(int resId, Bundle bundle) {
        switch (resId) {
            case R.string.import_from_sdcard: {
                if (mAccountSelectionListener == null) {
                    throw new NullPointerException(
                            "mAccountSelectionListener must not be null.");
                }
                // The following lines are provided and maintained by Mediatek inc.
                // return AccountSelectionUtil.getSelectAccountDialog(this, resId,
                // mAccountSelectionListener,
                // new CancelListener());
                return getSelectAccountDialog(this, resId, mAccountSelectionListener,
                        new CancelListener());
                // The following lines are provided and maintained by Mediatek inc.
            }
        }
        return super.onCreateDialog(resId, bundle);
    }

    // The following lines are provided and maintained by Mediatek inc.
    public static Dialog getSelectAccountDialog(Context context, int resId,
            DialogInterface.OnClickListener onClickListener,
            DialogInterface.OnCancelListener onCancelListener) {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
        final List<AccountWithDataSet> writableAccountList = accountTypes.getAccounts(true);

        Log.i(LOG_TAG, "***The number of available accounts: " + writableAccountList.size());
        int k = 0;
        List<AccountWithDataSet> accountlist1 = new ArrayList<AccountWithDataSet>();
        for (int i = 0; i < writableAccountList.size(); i++) {
            AccountWithDataSet account1 = writableAccountList.get(i);
            //UIM
//            if (!account1.type.equals(mSimAccountType) && !account1.type.equals(mUsimAccountType)) {
            if (!account1.type.equals(mSimAccountType) && !account1.type.equals(mUsimAccountType) && !account1.type.equals(mUimAccountType)) {
            //UIM
                Log.i("sdcard", "account1.type : " + account1.type);
                accountlist1.add(writableAccountList.get(i));
            }
        }
        // Assume accountList.size() > 1

        // Wrap our context to inflate list items using correct theme
        final Context dialogContext = new ContextThemeWrapper(context, android.R.style.Theme_Light);
        final LayoutInflater dialogInflater = (LayoutInflater) dialogContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final ArrayAdapter<AccountWithDataSet> accountAdapter = new ArrayAdapter<AccountWithDataSet>(
                context, R.layout.selectaccountactivity, accountlist1) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = dialogInflater.inflate(R.layout.selectaccountactivity,
                            parent, false);
                }

                // TODO: show icon along with title
                final TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                final TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

                final AccountWithDataSet account = this.getItem(position);
                final AccountType accountType = accountTypes.getAccountType(account.type,
                        account.dataSet);
                final Context context = getContext();

                text1.setText(account.name);
                text2.setText(accountType.getDisplayLabel(context));

                return convertView;
            }
        };

        if (onClickListener == null) {
            AccountSelectedListener accountSelectedListener = new AccountSelectedListener(context,
                    writableAccountList, resId);
            onClickListener = accountSelectedListener;
        }
        if (onCancelListener == null) {
            onCancelListener = new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                }
            };
        }
        return new AlertDialog.Builder(context).setTitle(R.string.dialog_new_contact_account)
                .setSingleChoiceItems(accountAdapter, 0, onClickListener).setOnCancelListener(
                        onCancelListener).create();
    }

    private static String mSimAccountType = AccountType.ACCOUNT_TYPE_SIM;

    private static String mUsimAccountType = AccountType.ACCOUNT_TYPE_USIM;
    //UIM
    private static String mUimAccountType = AccountType.ACCOUNT_TYPE_UIM;
    //UIM
    public static final String ACCOUNT_TYPE_SERVICE = "contactAccountTypes";
    // The following lines are provided and maintained by Mediatek inc.
}
