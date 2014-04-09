/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.contacts;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import com.android.contacts.R;

import java.util.ArrayList;
import java.util.List;

//import com.android.mms.ui./*ComposeMessageActivity*/*;

public class ShareContactViaSMS extends Activity {

    private static final String TAG = "ShareContactViaSMS";
    private String mAction;
    private Uri mDataUri;
    private int mSingleContactId = -1;
    private boolean mUserProfile = false;
    String mLookUpUris;
    Intent mIntent;
    private ProgressDialog mProgressDialog;
    private SearchContactThread mSearchContactThread;

    static final String[] CONTACTS_PROJECTION = new String[] { Contacts._ID, // 0
            Contacts.DISPLAY_NAME_PRIMARY, // 1
            Contacts.DISPLAY_NAME_ALTERNATIVE, // 2
            Contacts.SORT_KEY_PRIMARY, // 3
            Contacts.DISPLAY_NAME, // 4
    };

    static final int PHONE_ID_COLUMN_INDEX = 0;

    // final String[] sLookupProjection = new String[] {
    // Contacts.LOOKUP_KEY
    // };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mIntent = getIntent();
        mAction = mIntent.getAction();
        String contactId = mIntent.getStringExtra("contactId");
        String userProfile = mIntent.getStringExtra("userProfile");
        if (userProfile != null && "true".equals(userProfile)) {
            mUserProfile = true;
        }

        if (contactId != null && !"".equals(contactId)) {
            mSingleContactId = Integer.parseInt(contactId);
        }

        /** M: Bug Fix for CR: ALPS00395378 @{ 
         * Original Code:
         * mLookUpUris = mIntent.getStringExtra("LOOKUPURIS");
         */
        final Uri extraUri = (Uri) mIntent.getExtra(Intent.EXTRA_STREAM);
        mLookUpUris = null;
        if (null != extraUri) {
            mLookUpUris = extraUri.getLastPathSegment();
        }
        /** @} M: Bug fix for CR: ALPS00395378 */

        /** M: Bug Fix for ALPS00407311 @{ */
        if ((null != extraUri && extraUri.toString().startsWith("file") && mSingleContactId == -1 && mUserProfile == false)
                || TextUtils.isEmpty(mLookUpUris)) {
            Toast.makeText(this.getApplicationContext(), getString(R.string.send_file_sms_error),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        /** @} */
        Log.i(TAG, "mAction is " + mAction);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            String title = getString(R.string.please_wait);
            String message = getString(R.string.please_wait);
            mProgressDialog = ProgressDialog.show(this, title, message, true,
                    false);
            mProgressDialog.setOnCancelListener(mSearchContactThread);
            mSearchContactThread.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Intent.ACTION_SEND.equals(mAction)
                && mIntent.hasExtra(Intent.EXTRA_STREAM)) {
            mSearchContactThread = new SearchContactThread();
            showProgressDialog();
        }
    }

    public void shareViaSMS(String shareLookUpUris) {
        StringBuilder contactsID = new StringBuilder();
        int curIndex = 0;
        Cursor cursor = null;
        String id = null;
        String textVCard = "";
        if (mUserProfile) {
            cursor = getContentResolver().query(Profile.CONTENT_URI.buildUpon().appendPath("data").build(),
                    new String[]{Data.CONTACT_ID, Data.MIMETYPE, Data.DATA1}, null, null, null);
            if (cursor != null) {
                textVCard = getVCardString(cursor, textVCard);
                cursor.close();
            }
        }
        else {
		    if (mSingleContactId == -1) {
		        String[] tempUris = shareLookUpUris.split(":");
		        StringBuilder selection = new StringBuilder(Contacts.LOOKUP_KEY
		                + " in (");
		        int index = 0;
		        for (int i = 0; i < tempUris.length; i++) {
		            selection.append("'" + tempUris[i] + "'");
		            if (index != tempUris.length - 1) {
		                selection.append(",");
		            }
		            index++;
		        }

		        selection.append(")");
		        cursor = getContentResolver().query(
		                /* dataUri */Contacts.CONTENT_URI, CONTACTS_PROJECTION,
		                selection.toString(), null, Contacts.SORT_KEY_PRIMARY);
		        Log.i(TAG, "cursor is " + cursor);
		        if (null != cursor) {
		            while (cursor.moveToNext()) {
		                if (cursor != null) {
		                    id = cursor.getString(PHONE_ID_COLUMN_INDEX);
		                }
		                if (curIndex++ != 0) {
		                    contactsID.append("," + id);
		                } else {
		                    contactsID.append(id);
		                }
		            }
		            cursor.close();
		        }
		    } else {
		        id = Integer.toString(mSingleContactId);
		        contactsID.append(id);
		    }

		    long[] contactsIds = null;
		    if (contactsID.toString() != null && !contactsID.toString().equals("")) {
		        String[] vCardConIds = contactsID.toString().split(",");
		        Log.e(TAG, "ComposeMessage.initActivityState(): vCardConIds.length"
		                + vCardConIds.length);
		        contactsIds = new long[vCardConIds.length];
		        try {
		            for (int i = 0; i < vCardConIds.length; i++) {
		                contactsIds[i] = Long.parseLong(vCardConIds[i]);
		            }
		        } catch (NumberFormatException e) {
		            contactsIds = null;
		        }
		    }
		    if (contactsIds != null && contactsIds.length > 0) {
		        Log.i(TAG, "compose.addTextVCard(): contactsIds.length() = "
		                + contactsIds.length);
		        // String textVCard = TextUtils.isEmpty(mTextEditor.getText())? "":
		        // "\n";

		        StringBuilder sb = new StringBuilder("");
		        for (long contactId : contactsIds) {
		            if (contactId == contactsIds[contactsIds.length - 1]) {
		                sb.append(contactId);
		            } else {
		                sb.append(contactId + ",");
		            }
		        }
		        String selection = Data.CONTACT_ID + " in (" + sb.toString() + ")";

		        Log.i(TAG, "compose.addTextVCard(): selection = " + selection);
		        Uri shareDataUri = Uri.parse("content://com.android.contacts/data");
		        Log.i(TAG, "Before query to build contact name and number string ");
		        Cursor c = getContentResolver()
		                .query(
		                        shareDataUri, // URI
		                        new String[] { Data.CONTACT_ID, Data.MIMETYPE,
		                                Data.DATA1 }, // projection
		                        selection, // selection
		                        null, // selection args
		                        Contacts.SORT_KEY_PRIMARY + " , " + Data.CONTACT_ID); // sortOrder
		        Log.i(TAG, "After query to build contact name and number string ");
		        if (c != null) {
		            Log.i(TAG, "Before getVCardString ");
		            textVCard = getVCardString(c, textVCard);
		            Log.i(TAG, "After getVCardString ");
		            c.close();
		        }
		    }
        }
        Log.i(TAG, "textVCard is " + " \n" + textVCard);
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", "",
                null));
        i.putExtra("sms_body", textVCard);
        try {
            ShareContactViaSMS.this.startActivity(i);
        } catch (ActivityNotFoundException e) {
            ShareContactViaSMS.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(
                            ShareContactViaSMS.this.getApplicationContext(),
                            getString(R.string.quickcontact_missing_app),
                            Toast.LENGTH_SHORT).show();
                }
            });
            Log.d(TAG, "ActivityNotFoundException for secondaryIntent");
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG, "In onBackPressed");
        finish();
    }

    // create the String of vCard via Contacts message
    private String getVCardString(Cursor cursor, String textVCard) {
        final int dataContactId = 0;
        final int dataMimeType = 1;
        final int dataString = 2;
        long contactId = 0l;
        long contactCurrentId = 0l;
        int i = 1;
        String mimeType;
        TextVCardContact tvc = new TextVCardContact();
        int j = 0;
        while (cursor.moveToNext()) {
            contactId = cursor.getLong(dataContactId);
            mimeType = cursor.getString(dataMimeType);
            if (contactCurrentId == 0l) {
                contactCurrentId = contactId;
            }

            // put one contact information into textVCard string
            if (contactId != contactCurrentId) {
                contactCurrentId = contactId;
                textVCard += tvc.toString();
                tvc.reset();
            }

            // get cursor data
            if (CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    .equals(mimeType)) {
                tvc.mName = cursor.getString(dataString);
            }
            if (CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mNumbers.add(cursor.getString(dataString));
            }
            if (CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mOmails.add(cursor.getString(dataString));
            }
            if (CommonDataKinds.Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                tvc.mOrganizations.add(cursor.getString(dataString));
            }
            // put the last one contact information into textVCard string
            if (cursor.isLast()) {
                textVCard += tvc.toString() + "\n";
            }
            j++;
            if (j % 10 == 0) {
                if (textVCard.length() > 2000) {
                    break;
                }
            }
        }
        // Log.i(TAG, "compose.getVCardString():return string = " + textVCard);
        return textVCard;
    }

    private class TextVCardContact {
        protected String mName = "";
        protected List<String> mNumbers = new ArrayList<String>();
        protected List<String> mOmails = new ArrayList<String>();
        protected List<String> mOrganizations = new ArrayList<String>();

        protected void reset() {
            mName = "";
            mNumbers.clear();
            mOmails.clear();
            mOrganizations.clear();
        }

        @Override
        public String toString() {
            String textVCardString = "";
            int i = 1;
            if (mName != null && !mName.equals("")) {
                textVCardString += getString(R.string.nameLabelsGroup) + ": "
                        + mName + "\n";
            }
            if (!mNumbers.isEmpty()) {
                if (mNumbers.size() > 1) {
                    i = 1;
                    for (String number : mNumbers) {
                        textVCardString += "Tel" + i + ": " + number + "\n";
                        i++;
                    }
                } else {
                    textVCardString += "Tel" + ": " + mNumbers.get(0) + "\n";
                }
            }
            if (!mOmails.isEmpty()) {
                if (mOmails.size() > 1) {
                    i = 1;
                    for (String email : mOmails) {
                        textVCardString += getString(R.string.email_other) + i
                                + ": " + email + "\n";
                        i++;
                    }
                } else {
                    textVCardString += getString(R.string.email_other) + ": "
                            + mOmails.get(0) + "\n";
                }
            }
            if (!mOrganizations.isEmpty()) {
                if (mOrganizations.size() > 1) {
                    i = 1;
                    for (String organization : mOrganizations) {
                        textVCardString += getString(R.string.organizationLabelsGroup)
                                + i + ": " + organization + "\n";
                        i++;
                    }
                } else {
                    textVCardString += getString(R.string.organizationLabelsGroup)
                            + ": " + mOrganizations.get(0) + "\n";
                }
            }
            return textVCardString;
        }
    }

    private class SearchContactThread extends Thread implements
            OnCancelListener, OnClickListener {
        public SearchContactThread() {
        }

        @Override
        public void run() {
            String type = mIntent.getType();
            mDataUri = (Uri) mIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            // dataUri.buildUpon().appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
            // "1").build();
            Log.i(TAG, "dataUri is " + mDataUri);
            Log.i(TAG, "type is " + type);
            if (mDataUri != null && type != null) {
                shareViaSMS(mLookUpUris);
            }
        }

        public void onCancel(DialogInterface dialog) {
            // mCanceled = true;
            finish();
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            }
        }
    }

}