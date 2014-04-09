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

package com.mediatek.contacts.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.QuickContactBadge;

import com.android.contacts.R;
import com.android.contacts.calllog.PhoneNumberHelper;

/**
 * Widget used to show an image with the standard QuickContact badge and
 * on-click behavior.
 */
public class QuickContactBadgeWithPhoneNumber extends QuickContactBadge implements OnClickListener {
    private String mPhoneNumber;
    private boolean mIsSipNumber;
    private PhoneNumberHelper mPhoneNumberHelper;

    /**
     * 
     * @param context to get resource
     */
    public QuickContactBadgeWithPhoneNumber(Context context) {
        this(context, null);
        Resources resources = context.getResources();
        mPhoneNumberHelper = new PhoneNumberHelper(resources);
    }

    /**
     * 
     * @param context to get resource 
     * @param attrs AttributeSet
     */
    public QuickContactBadgeWithPhoneNumber(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        Resources resources = context.getResources();
        mPhoneNumberHelper = new PhoneNumberHelper(resources);
    }

    /**
     * 
     * @param context to get resource 
     * @param attrs AttributeSet
     * @param defStyle default style
     */
    public QuickContactBadgeWithPhoneNumber(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources resources = context.getResources();
        mPhoneNumberHelper = new PhoneNumberHelper(resources);
        setOnClickListener(this);
    }

    /**
     * 
     * @param number assign the number to quick contact
     * @param isSipCallNumber if the number is sip call number
     */
    public void assignPhoneNumber(String number, boolean isSipCallNumber) {
        mPhoneNumber = number;
        if (mPhoneNumberHelper.canPlaceCallsTo(number)) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
        mIsSipNumber = isSipCallNumber;
    }

    @Override
    public void onClick(View v) {
        if (mPhoneNumber != null) {
            showDialog(mPhoneNumber);
        } else {
            super.onClick(v);
        }
    }

    private void showDialog(final String number) {
        if (!TextUtils.isEmpty(number)) {
            String message = mContext.getString(R.string.add_contact_dlg_message_fmt, number);
            String title = mContext.getString(R.string.add_contact_dlg_title);
            AlertDialog dialog = new AlertDialog.Builder(mContext).setTitle(title).setMessage(
                    message).setNegativeButton(android.R.string.cancel, null).setPositiveButton(
                    android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                            intent.setType(Contacts.CONTENT_ITEM_TYPE);
                            if (mIsSipNumber) {
                                intent
                                        .putExtra(ContactsContract.Intents.Insert.SIP_ADDRESS,
                                                number);
                            } else {
                                intent.putExtra(Insert.PHONE, number);
                            }
                            mContext.startActivity(intent);
                        }
                    }).create();
            dialog.show();
        }
    }
}
