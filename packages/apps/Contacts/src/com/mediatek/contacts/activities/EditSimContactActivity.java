
package com.mediatek.contacts.activities;

import android.accounts.Account;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.ext.Anr;
import com.android.contacts.ext.ContactAccountExtension;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.internal.telephony.ITelephony;

import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.SubContactsUtils;
import com.mediatek.contacts.extension.aassne.SimUtils;
import com.mediatek.contacts.extension.aassne.SneExt;
import com.mediatek.contacts.simcontact.AbstractStartSIMService;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroup;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroupException;
import com.mediatek.contacts.util.MtkToast;

import com.mediatek.telephony.PhoneNumberUtilsEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class EditSimContactActivity extends Activity {

    public static final String EDIT_SIM_CONTACT = "com.android.contacts.action.EDIT_SIM_CONTACT";

    private static final String TAG = "EditSimContactActivity";

    private static final String SIM_DATA = "simData";

    private static final String SIM_OLD_DATA = "simOldData";

//    private static final String SIM_NUM_PATTERN = "[+]?[[0-9][*#]]+[[0-9][*#,]]*";
    private static final String SIM_NUM_PATTERN = "[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*";
    private static final String USIM_EMAIL_PATTERN = "[[0-9][a-z][A-Z][_]][[0-9][a-z][A-Z][-_.]]*@[[0-9][a-z][A-Z][-_.]]+";
    private static String sAfterPhone = "";
    private static String sAfterOtherPhone = "";
    private static String sName = "";

    private static String sPhone = "";

    private static String sOtherPhone = "";

    private static String sEmail = "";

    private String mUpdateName = "";

    private String mUpdatephone = "";

    private String mUpdatemail = "";

    private String mUpdateAdditionalNumber = "";

    private String mAccountType = "";

    private String mSimType = "SIM";

    private String mAccountName = "";

    private String mOldName = "";

    private String mOldPhone = "";

    private String mOldEmail = "";

    private String mOldOtherPhone = "";

    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = new String[] { "name", "number",
            "emails", "additionalNumber", "groupIds" };

    private int mSlotId;

    private ProgressDialog mSaveDialog;

    private long mIndexInSim = -1;

    private boolean mAirPlaneModeOn = false;

    private boolean mAirPlaneModeOnNotEdit = false;

    private boolean mFDNEnabled = false;

    private boolean mSIMInvalid = false;

    private boolean mNumberIsNull = false;

    private boolean mNumberInvalid = false;

    private boolean mFixNumberInvalid = false;

    private boolean mNumberLong = false;

    private boolean mNameLong = false;

    private boolean mFixNumberLong = false;

    private boolean mStorageFull = false;

    private boolean mGeneralFailure = false;

    private int mSaveFailToastStrId = -1;

    private boolean mOnBackGoing = true;

    private String mPhoneTypeSuffix = null; // mtk80909 for ALPS00023212

    private boolean mEmailInvalid = false;

    private boolean mEmail2GInvalid = false;

    private boolean mDoublePhoneNumber = false;
    private boolean mQuitEdit = false;
    private Account mAccount;

    private int mSaveMode = 0;

    private static final int LISTEN_PHONE_STATES = 1;// 80794

    private static final int LISTEN_PHONE_NONE_STATES = 2;

    private Uri mLookupUri;

    private ContentResolver mContentResolver;

    final ITelephony mITel = ITelephony.Stub.asInterface(ServiceManager
            .getService(Context.TELEPHONY_SERVICE));

    static final int MODE_DEFAULT = 0;

    static final int MODE_INSERT = 1;

    static final int MODE_EDIT = 2;

    boolean mPhbReady = false;

    long mIndicate = 0;

    int mMode = MODE_DEFAULT;

    long mRawContactId = -1;

    int mContactId = 0;

    int mGroupNum = 1;

    HashMap<Long, String> mGroupAddList = new HashMap<Long, String>();

    HashMap<Long, String> mOldGroupAddList = new HashMap<Long, String>();

    private Handler mSaveContactHandler = null;

    private Handler getsaveContactHandler() {
        if (null == mSaveContactHandler) {
            HandlerThread controllerThread = new HandlerThread("saveContacts");
            controllerThread.start();
            mSaveContactHandler = new Handler(controllerThread.getLooper());
        }
        return mSaveContactHandler;
    }

    private ArrayList<RawContactDelta> mSimData = new ArrayList<RawContactDelta>();

    private ArrayList<RawContactDelta> mSimOldData = new ArrayList<RawContactDelta>();

    /** M:AAS, So far, mAnrsList.size() equals mOldAnrsList.size(). @ { */
    private ArrayList<Anr> mAnrsList = new ArrayList<Anr>();

    private ArrayList<Anr> mOldAnrsList = new ArrayList<Anr>();
    /** M: @ } */

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        final Intent intent = getIntent();

        mSimData = intent.getParcelableArrayListExtra(SIM_DATA);
        mSimOldData = intent.getParcelableArrayListExtra(SIM_OLD_DATA);
        // get mSlot and mIndicate
        mSlotId = intent.getIntExtra("slotId", SlotUtils.getNonSlotId());
        mIndicate = intent.getLongExtra(RawContacts.INDICATE_PHONE_SIM, RawContacts.INDICATE_PHONE);
        mIndexInSim = intent.getIntExtra("simIndex", -1);
        // 1 for new contact, 2 for existing contact
        mSaveMode = intent.getIntExtra("simSaveMode", MODE_DEFAULT);
        mLookupUri = intent.getData();
        mAccountType = mSimData.get(0).getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        if (mAccountType.equals(AccountType.ACCOUNT_TYPE_USIM)) {
            mGroupNum = intent.getIntExtra("groupNum", 0);
            Log.i(TAG, "groupNum : " + mGroupNum);
        }
        mAccountName = mSimData.get(0).getValues().getAsString(RawContacts.ACCOUNT_NAME);
        if (mAccountType != null && mAccountName != null) {
            mAccount = new Account(mAccountName, mAccountType);
        } else {
            finish();
            return;
        }

        Log.i(TAG, "the mSlotId is =" + mSlotId + " the mIndicate is =" + mIndicate
                + " the mSaveMode = " + mSaveMode + " the accounttype is = " + mAccountType
                + " the uri is  = " + mLookupUri + " | mIndexInSim : " + mIndexInSim);

        // checkcheckPhbReady in ContactsUtils
        mPhbReady = SimCardUtils.isPhoneBookReady(mSlotId);

        String[] buffer = new String[2];
        String[] bufferName = new String[2];

        long[] bufferGroup = new long[mGroupNum];

        // the kind number
        // int count = mSimData.get(0).getEntryCount(false);
        int count = mSimData.get(0).getContentValues().size();
        Log.i(TAG, "onCreate count:" + count);

        // get data
        int j = 0;
        int k = 0;
        int m = 0;
        for (int i = 0; i < count; i++) {
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mSimData.get(0).getContentValues().get(i)
                    .getAsString(Data.MIMETYPE))) {
                sName = mSimData.get(0).getContentValues().get(i).getAsString(Data.DATA1);
            } else if (Phone.CONTENT_ITEM_TYPE.equals(mSimData.get(0).getContentValues().get(i)
                    .getAsString(Data.MIMETYPE))) {

                /** M:AAS, store AAS index(use Data.DATA3). @ { */
                if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureAccount(
                        mAccountType, ExtensionManager.COMMD_FOR_AAS)) {
                    final ContentValues cv = mSimData.get(0).getContentValues().get(i);
                    if (SimUtils.isAdditionalNumber(cv)) {
                        Anr addPhone = new Anr();
                        addPhone.mAdditionNumber = trimAnr(cv.getAsString(Data.DATA1));
                        addPhone.mAasIndex = cv.getAsString(Data.DATA3);
                        mAnrsList.add(addPhone);

                        buffer[0] = cv.getAsString(Data.DATA1);
                    } else {
                        bufferName[m] = cv.getAsString(Data.DATA1);
                        m++;
                    }
                    /** M: @ } */
                } else {
                    /**
                     * M:SNE The Data.DATA2 may be null @ { original code: if
                     * (mSimData
                     * .get(0).getContentValues().get(i).getAsString(Data
                     * .DATA2).equals("7")) {
                     */
                    if ("7"
                            .equals(mSimData.get(0).getContentValues().get(i)
                                    .getAsString(Data.DATA2))) {
                        /** M: @ } */
                        buffer[j] = mSimData.get(0).getContentValues().get(i)
                                .getAsString(Data.DATA1);
                        j++;
                    } else {
                        bufferName[m] = mSimData.get(0).getContentValues().get(i).getAsString(
                                Data.DATA1);
                        m++;
                    }
                }

            } else if (Email.CONTENT_ITEM_TYPE.equals(mSimData.get(0).getContentValues().get(i)
                    .getAsString(Data.MIMETYPE))) {
                sEmail = mSimData.get(0).getContentValues().get(i).getAsString(Data.DATA1);
            } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mSimData.get(0).getContentValues()
                    .get(i).getAsString(Data.MIMETYPE))) {
                bufferGroup[k] = mSimData.get(0).getContentValues().get(i).getAsLong(Data.DATA1);
                k++;
                /** M:SNE @ { */
            } else if (SneExt.isNickname(mSimData.get(0).getContentValues().get(i).getAsString(
                    Data.MIMETYPE))) {
                sNickname = mSimData.get(0).getContentValues().get(i).getAsString(Data.DATA1);
                sNickname = TextUtils.isEmpty(sNickname) ? "" : sNickname;
                Log.i(TAG, "sNickname:" + sNickname);
                /** M: @ } */
            }
        }

        // put group id and title to hasmap
        if (mAccountType.equals(AccountType.ACCOUNT_TYPE_USIM)) {
            String[] groupName = new String[mGroupNum];
            long[] groupId = new long[mGroupNum];
            int bufferGroupNum = bufferGroup.length;
            Log.i(TAG, "bufferGroupNum : " + bufferGroupNum);
            groupName = intent.getStringArrayExtra("groupName");
            groupId = intent.getLongArrayExtra("groupId");
            for (int i = 0; i < bufferGroupNum; i++) {
                for (int grnum = 0; grnum < mGroupNum; grnum++) {
                    if (bufferGroup[i] == groupId[grnum]) {
                        String title = groupName[grnum];
                        long groupid = bufferGroup[i];
                        mGroupAddList.put(groupid, title);
                    }
                }

            }
        }

        /** M:AAS @ { */
        if (ExtensionManager.getInstance().getContactDetailExtension().isDoublePhoneNumber(buffer,
                bufferName, ExtensionManager.COMMD_FOR_AAS)) {
            mDoublePhoneNumber = true;
            /** M: Bug Fix for ALPS00390125 @{ */
            if (setSaveFailToastText()) {
                return;
            }
            /** @} */
        } else /** M: @ } */
        // if user chose two "mobile" phone type
        if ((!TextUtils.isEmpty(buffer[1])) || (!TextUtils.isEmpty(bufferName[1]))) {
            mDoublePhoneNumber = true;
            /** M: Bug Fix for ALPS00390125 @{ */
            if (setSaveFailToastText()) {
                return;
            }
            /** @} */
        } else {
            sOtherPhone = buffer[0];
            sPhone = bufferName[0];
        }

        Log.w(TAG, "the sName is = " + sName + " the sPhone is =" + sPhone + " the buffer[] is "
                + buffer[0] + " the sOtherPhone is = " + sOtherPhone + "the email is =" + sEmail);
        if (SimCardUtils.isSimUsimType(mSlotId)) {
            mSimType = SimCardUtils.SimType.SIM_TYPE_USIM_TAG;
        }
        Log.i(TAG, "initial phone number " + sPhone);
        sAfterPhone = sPhone;
        if (!TextUtils.isEmpty(sPhone)) {
            sAfterPhone = PhoneNumberUtils.stripSeparators(sPhone);
            // sAfterPhone = ((String) sPhone).replaceAll("-", "");
            // sAfterPhone = ((String) sPhone).replaceAll("\\(", "");
            // sAfterPhone = ((String) sPhone).replaceAll("\\)", "");
            // sAfterPhone = ((String) sPhone).replaceAll(" ", "");

            Log.i(TAG, "*********** after split phone number " + sAfterPhone + ",check valid:"
                    + PhoneNumberUtilsEx.extractCLIRPortion(sAfterPhone));

            if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtilsEx.extractCLIRPortion(sAfterPhone))) {
                mNumberInvalid = true;
            }
            if (setSaveFailToastText()) {
                finish();
                return;
            }

        }
        Log.i(TAG, "initial sOtherPhone number " + sOtherPhone);
        sAfterOtherPhone = sOtherPhone;
        if (!TextUtils.isEmpty(sOtherPhone)) {
            // sAfterOtherPhone = ((String) sOtherPhone).replaceAll("-", "");
            // sAfterOtherPhone = ((String) sOtherPhone).replaceAll("\\(", "");
            // sAfterOtherPhone = ((String) sOtherPhone).replaceAll("\\)", "");
            // sAfterOtherPhone = ((String) sOtherPhone).replaceAll(" ", "");
            sAfterOtherPhone = PhoneNumberUtils.stripSeparators(sOtherPhone);
            Log.i(TAG, "*********** after split sOtherPhone number " + sAfterOtherPhone
                    + ",check valid:" + PhoneNumberUtilsEx.extractCLIRPortion(sAfterOtherPhone));

            if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtilsEx.extractCLIRPortion(sAfterOtherPhone))) {
                mNumberInvalid = true;
            }
            if (setSaveFailToastText()) {
                finish();
                return;
            }

        }
        Log.i(TAG, "initial name is  " + sName);
        /*
         * if (!TextUtils.isEmpty(sName)) { sName = ((String)
         * sName).replaceAll("-", ""); }
         */// for ALPS00117700
        if (mSaveMode == MODE_EDIT) {

            mMode = MODE_EDIT;
            if (mLookupUri != null) {
                boolean isGoing = fixIntent();
                Log.i(TAG, "isGoing : " + isGoing);
                if (!isGoing) {
                    return;
                }
            } else {
                finish();
                return;
            }
        }
        // boolean hasImported =
        // SubContactsUtils.hasSimContactsImported(mSlotId);
        boolean hasImported = AbstractStartSIMService.isServiceRunning(mSlotId);
        int serviceSate = AbstractStartSIMService.getServiceState(mSlotId);
        Log.i(TAG, "[onCreate] serviceState : " + serviceSate + " | hasImported : " + hasImported);
        // check hasSimContactsImported in ContactsUtils
        if (hasImported) {
            String toastMsg = getString(R.string.msg_loading_sim_contacts_toast);
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        doSaveAction(mSaveMode);

        Log.i(TAG, "StructuredName.CONTENT_ITEM_TYPE = " + StructuredName.CONTENT_ITEM_TYPE);
        Log.i(TAG, "Phone.CONTENT_ITEM_TYPE = " + Phone.CONTENT_ITEM_TYPE);
        Log.i(TAG, "Email.CONTENT_ITEM_TYPE = " + Email.CONTENT_ITEM_TYPE);
        Log.i(TAG, "GroupMembership.CONTENT_ITEM_TYPE = " + GroupMembership.CONTENT_ITEM_TYPE);
        Log.i(TAG, "the sName is = " + sName + " the sPhone is =" + sPhone + " the buffer[] is "
                + buffer[0] + " the sOtherPhone is = " + sOtherPhone + "the email is =" + sEmail);

    }

    @Override
    protected void onDestroy() {
        if (mSaveContactHandler != null) {
            mSaveContactHandler.getLooper().quit();
        }
        super.onDestroy();
    }


    public boolean fixIntent() {
        Intent intent = getIntent();
        ContentResolver resolver = this.getContentResolver();
        Log.w(TAG, "the fixintent resolver = " + resolver);
        Uri uri = mLookupUri;
        Log.i(TAG, "uri is " + uri);
        final String authority = uri.getAuthority();
        final String mimeType = intent.resolveType(resolver);
        // long mRawContactId = -1;
        if (ContactsContract.AUTHORITY.equals(authority)) {
            if (Contacts.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Handle selected aggregate
                final long contactId = ContentUris.parseId(uri);
                mRawContactId = SubContactsUtils.queryForRawContactId(resolver, contactId);
            } else if (RawContacts.CONTENT_ITEM_TYPE.equals(mimeType)) {
                final long rawContactId = ContentUris.parseId(uri);
            }
        }
        /*
         * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
         * ALPS00272669 Descriptions:
         */
        Log.i(TAG, "mRawContactId IS " + mRawContactId);
        if (mRawContactId < 1) {
            Log.i(TAG, "the mRawContactId is wrong");
            Toast.makeText(this, R.string.phone_book_busy, Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        /*
         * Bug Fix by Mediatek End.
         */

        mSlotId = SIMInfo.getSlotById(EditSimContactActivity.this, mIndicate);

        // int oldcount = mSimOldData.get(0).getEntryCount(false);
        int oldcount = mSimOldData.get(0).getContentValues().size();
        Log.i(TAG, "[fixIntent] oldcount:" + oldcount);
        String[] oldbuffer = new String[2];

        long[] oldbufferGroup = new long[mGroupNum];
        int k = 0;
        for (int i = 0; i < oldcount; i++) {
            Log.i(TAG, "mSimOldData.get(0).getContentValues().get(i).getAsString(Data.MIMETYPE)   "
                    + mSimOldData.get(0).getContentValues().get(i).getAsString(Data.MIMETYPE));
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mSimOldData.get(0).getContentValues().get(i)
                    .getAsString(Data.MIMETYPE))) {
                mOldName = mSimOldData.get(0).getContentValues().get(i).getAsString(Data.DATA1);
            } else if (Phone.CONTENT_ITEM_TYPE.equals(mSimOldData.get(0).getContentValues().get(i)
                    .getAsString(Data.MIMETYPE))) {

                /** M:AAS @ { */
                if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureAccount(
                        mAccountType, ExtensionManager.COMMD_FOR_AAS)) {
                    ContentValues cv = mSimOldData.get(0).getContentValues().get(i);
                    if (SimUtils.isAdditionalNumber(cv)) {
                        Anr addPhone = new Anr();
                        addPhone.mAdditionNumber = trimAnr(cv.getAsString(Data.DATA1));
                        addPhone.mAasIndex = cv.getAsString(Phone.DATA3);
                        addPhone.mId = cv.getAsInteger(Data._ID);
                        mOldAnrsList.add(addPhone);
                    } else {
                        mOldPhone = cv.getAsString(Data.DATA1);
                    }
                    /** M: @ } */
                } else {
                    if (mSimOldData.get(0).getContentValues().get(i).getAsString(Data.DATA2).equals(
                            "7")) {
                        mOldOtherPhone = mSimOldData.get(0).getContentValues().get(i).getAsString(
                                Data.DATA1);
                    } else {
                        mOldPhone = mSimOldData.get(0).getContentValues().get(i).getAsString(
                                Data.DATA1);
                    }
                }

            } else if (Email.CONTENT_ITEM_TYPE.equals(mSimOldData.get(0).getContentValues().get(i)
                    .getAsString(Data.MIMETYPE))) {
                mOldEmail = mSimOldData.get(0).getContentValues().get(i).getAsString(Data.DATA1);
            } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mSimOldData.get(0)
                    .getContentValues().get(i).getAsString(Data.MIMETYPE))) {
                oldbufferGroup[k] = mSimOldData.get(0).getContentValues().get(i).getAsLong(
                        Data.DATA1);
                k++;
                /** M:SNE @ { */
            } else if (SneExt.isNickname(mSimOldData.get(0).getContentValues().get(i).getAsString(
                    Data.MIMETYPE))) {
                mOldNickname = mSimOldData.get(0).getContentValues().get(i).getAsString(Data.DATA1);
                Log.i(TAG, "mOldNickname=" + mOldNickname);
                /** M: @ } */
            }
        }
        Log.i(TAG, "the mOldName is : " + mOldName + "   mOldOtherPhone : " + mOldOtherPhone
                + "  mOldPhone:  " + mOldPhone + " mOldEmail : " + mOldEmail);
        Log.i(TAG, "[fixIntent] the mIndicate : " + mIndicate + " | the mSlotId : " + mSlotId);

        // put group id and title to hasmap
        if (mAccountType.equals(AccountType.ACCOUNT_TYPE_USIM)) {
            String[] groupName = new String[mGroupNum];
            long[] groupId = new long[mGroupNum];
            int bufferGroupNum = oldbufferGroup.length;
            Log.i(TAG, "bufferGroupNum : " + bufferGroupNum);
            groupName = intent.getStringArrayExtra("groupName");
            groupId = intent.getLongArrayExtra("groupId");
            for (int i = 0; i < bufferGroupNum; i++) {
                for (int grnum = 0; grnum < mGroupNum; grnum++) {
                    if (oldbufferGroup[i] == groupId[grnum]) {
                        String title = groupName[grnum];
                        long groupid = oldbufferGroup[i];
                        mOldGroupAddList.put(groupid, title);
                    }
                }

            }
        }
        return true;

    }

    public void onBackPressed() {
        if (!mOnBackGoing) {
            mOnBackGoing = true;
            Log.i(TAG, "[onBackPressed]");
            finish();
        }
    }

    private class InsertSimContactThread extends Thread {
        public boolean mCanceled = false;

        int mModeForThread = 0;

        public InsertSimContactThread(int md) {
            super("InsertSimContactThread");
            mModeForThread = md;
            Log.i(TAG, "InsertSimContactThread");
        }

        @Override
        public void run() {
            Uri checkUri = null;
            int result = 0;
            final ContentResolver resolver = getContentResolver();
            mUpdateName = sName;
            mUpdatephone = sAfterPhone;

            Log.i(TAG, "before replace - mUpdatephone is " + mUpdatephone);
            if (!TextUtils.isEmpty(mUpdatephone)) {
                Log.i(TAG, "[run] befor replaceall mUpdatephone : " + mUpdatephone);
                mUpdatephone = mUpdatephone.replaceAll("-", "");
                mUpdatephone = mUpdatephone.replaceAll(" ", "");
                Log.i(TAG, "[run] after replaceall mUpdatephone : " + mUpdatephone);
            }

            Log.i(TAG, "after replace - mUpdatephone is " + mUpdatephone);
            ContentValues values = new ContentValues();
            values.put("tag", TextUtils.isEmpty(mUpdateName) ? "" : mUpdateName);
            values.put("number", TextUtils.isEmpty(mUpdatephone) ? "" : mUpdatephone);

            if (mSimType.equals("USIM")) {
                // for USIM
                mUpdatemail = sEmail;
                /** M:AAS Assume ANR as "anr","anr2".., and so as aas @ {. */
                mUpdateAdditionalNumber = sAfterOtherPhone;

                if (!ExtensionManager.getInstance().getContactAccountExtension()
                        .updateContentValues(mAccountType, values, mAnrsList, null,
                                ContactAccountExtension.CONTENTVALUE_ANR_INSERT, ExtensionManager.COMMD_FOR_AAS)) {
                    Log.i(TAG, "before replace - mUpdateAdditionalNumber is "
                            + mUpdateAdditionalNumber);
                    if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
                        Log.i(TAG, "[run] befor replaceall mUpdateAdditionalNumber : "
                                + mUpdateAdditionalNumber);
                        mUpdateAdditionalNumber = mUpdateAdditionalNumber.replaceAll("-", "");
                        mUpdateAdditionalNumber = mUpdateAdditionalNumber.replaceAll(" ", "");
                        Log.i(TAG, "[run] after replaceall mUpdateAdditionalNumber : "
                                + mUpdateAdditionalNumber);
                    }

                    Log.i(TAG, "after replace - mUpdateAdditionalNumber is "
                            + mUpdateAdditionalNumber);
                    values.put("anr", TextUtils.isEmpty(mUpdateAdditionalNumber) ? ""
                            : mUpdateAdditionalNumber);
                }
                /** M: @ } */

                values.put("emails", TextUtils.isEmpty(mUpdatemail) ? "" : mUpdatemail);

                /** M:SNE @ { */
                if (ExtensionManager.getInstance().getContactAccountExtension()
                        .updateContentValues(mAccountType, values, null, sNickname,
                                ContactAccountExtension.CONTENTVALUE_NICKNAME, ExtensionManager.COMMD_FOR_SNE)) {
                    sUpdateNickname = sNickname;
                }
                /** M: @ } */

            }
            mPhbReady = SimCardUtils.isPhoneBookReady(mSlotId);
            Log.i(TAG, "the mPhbReady is = " + mPhbReady + " the mSlotId is = " + mSlotId);
            if (mModeForThread == MODE_INSERT) {

                Log.i("huibin", "thread mModeForThread == MODE_INSERT");
                checkPhoneStatus();
                
                if (mSimType.equals("USIM")) {
                    if (TextUtils.isEmpty(mUpdateName) && TextUtils.isEmpty(mUpdatephone)
                            && TextUtils.isEmpty(mUpdatemail)
                            && TextUtils.isEmpty(mUpdateAdditionalNumber)
                            && mGroupAddList.isEmpty()
                            /** M:SNE @ { */
                            && TextUtils.isEmpty(sUpdateNickname)/** M: @ } */
                    ) {

                        finish();
                        return;
                    } else if (TextUtils.isEmpty(mUpdatephone)
                            && TextUtils.isEmpty(mUpdateName)
                            && (!TextUtils.isEmpty(mUpdatemail)
                                    || !TextUtils.isEmpty(mUpdateAdditionalNumber)
                                    || !mGroupAddList.isEmpty()
                            /** M:SNE @ { */
                            || !TextUtils.isEmpty(sUpdateNickname)/** M: @ } */
                            )) {
                        mNumberIsNull = true;
                    } else if (!TextUtils.isEmpty(mUpdatephone)) {
                        if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtilsEx.extractCLIRPortion(mUpdatephone))) {
                            mNumberInvalid = true;
                        }
                    }
                    if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
                        if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtilsEx
                                .extractCLIRPortion(mUpdateAdditionalNumber))) {
                            mFixNumberInvalid = true;
                        }
                    }
                    // if (!TextUtils.isEmpty(mUpdatemail)) {
                    // if (!Pattern.matches(USIM_EMAIL_PATTERN, mUpdatemail)) {
                    // mEmailInvalid = true;
                    // }
                    // }

                } else {
                    if (TextUtils.isEmpty(mUpdatephone) && TextUtils.isEmpty(mUpdateName)) {
                        setResult(RESULT_OK, null);
                        finish();
                        return;
                    } else if (!TextUtils.isEmpty(mUpdatephone)) {
                        if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtilsEx.extractCLIRPortion(mUpdatephone))) {
                            mNumberInvalid = true;
                        }
                    }
                }
                if (setSaveFailToastText()) {
                    mOnBackGoing = false;
                    return;
                }

                /**
                 * M:SNE before insert USIM,check the nickname string length. @
                 * {
                 */
                if (!ExtensionManager.getInstance().getContactAccountExtension().isTextValid(
                        sUpdateNickname, mSlotId, ContactAccountExtension.TYPE_OPERATION_SNE,
                        ExtensionManager.COMMD_FOR_SNE)) {
                    showSaveFailToast();
                    return;
                }
                /** M: @ } */

                Log.i(TAG, "********BEGIN insert to SIM card ");
                checkUri = resolver.insert(SimCardUtils.SimUri.getSimUri(mSlotId), values);
                Log.i(TAG, "********END insert to SIM card ");
                Log.i(TAG, "values is " + values);
                Log.i(TAG, "checkUri is " + checkUri);
                if (setSaveFailToastText2(checkUri)) {
                    mOnBackGoing = false;
                    return;
                }

                // index in SIM
                long indexFromUri = ContentUris.parseId(checkUri);

                Log.i(TAG, "insert to db");
                // USIM group begin
                int errorType = -1;
                if (mSimType.equals("USIM")) {
                    int ugrpId = -1;
                    Iterator<Entry<Long, String>> iter = mGroupAddList.entrySet().iterator();
                    while (iter.hasNext()) {
                        Entry<Long, String> entry = iter.next();
                        long grpId = entry.getKey();
                        String grpName = entry.getValue();
                        try {
                            ugrpId = USIMGroup.syncUSIMGroupNewIfMissing(mSlotId, grpName);
                        } catch (RemoteException e) {
                            ugrpId = -1;
                        } catch (USIMGroupException e) {
                            errorType = e.getErrorType();
                            ugrpId = -1;
                        }
                        Log.d(TAG, "[USIM group]syncUSIMGroupNewIfMissing ugrpId:" + ugrpId);
                        if (ugrpId > 0) {
                            boolean suFlag = USIMGroup.addUSIMGroupMember(mSlotId,
                                    (int) indexFromUri, ugrpId);
                            Log.d(TAG, "[USIM group]addUSIMGroupMember suFlag:" + suFlag);
                        } else {
                            iter.remove();
                        }
                    }
                }
                // USIM group end
                /**
                 * M:AAS&SNE @ { original code: Uri lookupUri =
                 * SubContactsUtils.insertToDB(mAccount, mUpdateName, sPhone,
                 * mUpdatemail, sOtherPhone, resolver, mIndicate, mSimType,
                 * indexFromUri, mGroupAddList.keySet());
                 */
                Uri lookupUri = SimUtils.insertToDB(mAccountType, mAccount, mUpdateName, sPhone,
                        mUpdatemail, sOtherPhone, resolver, mIndicate, mSimType, indexFromUri,
                        mGroupAddList.keySet(), mAnrsList, sUpdateNickname);
                if (lookupUri == null) {
                    /** M: @ } */
                    lookupUri = SubContactsUtils.insertToDB(mAccount, mUpdateName, sPhone,
                            mUpdatemail, sOtherPhone, resolver, mIndicate, mSimType, indexFromUri,
                            mGroupAddList.keySet());
                }

                // google default has toast, so hide here
                showResultToastText(errorType, null);

                if (errorType == -1) {
                    // if using 2-panes, no need to startViewActivity, just set
                    // the result
                    // for ALPS00257464
                    if (PhoneCapabilityTester.isUsingTwoPanes(EditSimContactActivity.this)) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(lookupUri);
                        setResult(RESULT_OK, intent);
                    } else {
                        startViewActivity(lookupUri);
                    }
                }

                // USIM group end

                finish();
                return;
            } else if (mModeForThread == MODE_EDIT) {
                Log.d(TAG, "thread mModeForThread is MODE_EDIT");

                ContentValues updatevalues = new ContentValues();
                if (!TextUtils.isEmpty(mPhoneTypeSuffix)) {
                    sName = (TextUtils.isEmpty(sName)) ? ("/" + mPhoneTypeSuffix)
                            : (sName + "/" + mPhoneTypeSuffix);
                    mUpdateName = (TextUtils.isEmpty(mUpdateName)) ? ("/" + mPhoneTypeSuffix)
                            : (mUpdateName + "/" + mPhoneTypeSuffix);
                }
                // mtk80909 for ALPS00023212
                mUpdateAdditionalNumber = sAfterOtherPhone;
                if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
                    Log.i(TAG, "[run -edit] befor replaceall mUpdateAdditionalNumber : "
                            + mUpdateAdditionalNumber);
                    mUpdateAdditionalNumber = mUpdateAdditionalNumber.replaceAll("-", "");
                    mUpdateAdditionalNumber = mUpdateAdditionalNumber.replaceAll(" ", "");
                    Log.i(TAG, "[run -edit] after replaceall mUpdateAdditionalNumber : "
                            + mUpdateAdditionalNumber);
                }

                // to comment old values for index in SIM
                updatevalues.put("newTag", TextUtils.isEmpty(mUpdateName) ? "" : mUpdateName);
                updatevalues.put("newNumber", TextUtils.isEmpty(mUpdatephone) ? "" : mUpdatephone);

                /**
                 * M:AAS, Set all anr into usim and db. here we assume anr as
                 * "anr","anr2","anr3".., and so as aas. @ {
                 */
                ExtensionManager.getInstance().getContactAccountExtension().updateContentValues(
                        mAccountType, updatevalues, mAnrsList, mUpdateAdditionalNumber,
                        ContactAccountExtension.CONTENTVALUE_ANR_UPDATE, ExtensionManager.COMMD_FOR_AAS);
                /** M: @ } */

                updatevalues.put("newEmails", TextUtils.isEmpty(mUpdatemail) ? "" : mUpdatemail);

                /** M:SNE @ { */
                SneExt.buildNicknameValueForInsert(mSlotId, updatevalues, sUpdateNickname);
                /** M: @ } */

                // to use for index in SIM
                updatevalues.put("index", mIndexInSim);

                Log.i(TAG, "updatevalues IS " + updatevalues);
                Log.i(TAG, "mModeForThread IS " + mModeForThread);
                Cursor cursor = null;
                Log.i(TAG, "mIndicate  is " + mIndicate);
                checkPhoneStatus();

                Cursor c = getContentResolver().query(RawContacts.CONTENT_URI, new String[] {
                    RawContacts.CONTACT_ID
                }, RawContacts._ID + "=" + mRawContactId, null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        mContactId = c.getInt(0);
                        Log.i(TAG, "mContactId is " + mContactId);
                    }
                    c.close();
                }
                if (mSimType.equals("SIM")) {
                    if (TextUtils.isEmpty(mUpdateName) && TextUtils.isEmpty(mUpdatephone)) {
                        // if name and number is null, delete this contact
                        String where;

                        Uri iccUriForSim = SimCardUtils.SimUri.getSimUri(mSlotId);

                        // empty name and phone number
                        // use the new 'where' for index in SIM
                        where = "index = " + mIndexInSim;

                        Log.d(TAG, "where " + where);
                        Log.d(TAG, "iccUriForSim ******** " + iccUriForSim);
                        int deleteDone = getContentResolver().delete(iccUriForSim, where, null);
                        Log.i(TAG, "deleteDone is " + deleteDone);
                        if (deleteDone == 1) {
                            Uri deleteUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                                    mContactId);
                            int deleteDB = getContentResolver().delete(deleteUri, null, null);
                            Log.i(TAG, "deleteDB is " + deleteDB);
                        }
                        finish();
                        return;
                    } else if (!TextUtils.isEmpty(mUpdatephone)) {
                        if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtilsEx.extractCLIRPortion(mUpdatephone))) {
                            mNumberInvalid = true;
                        }
                    }
                } else if (mSimType.equals("USIM")) {
                    // if all items are empty, delete this contact
                    if (TextUtils.isEmpty(mUpdatephone) && TextUtils.isEmpty(mUpdateName)
                            && TextUtils.isEmpty(mUpdatemail)
                            && TextUtils.isEmpty(mUpdateAdditionalNumber)
                            && mGroupAddList.isEmpty()
                            /** M:SNE @ { */
                            && TextUtils.isEmpty(sUpdateNickname)/** M: @ } */
                    ) {
                        String where;
                        Uri iccUriForUsim = SimCardUtils.SimUri.getSimUri(mSlotId);

                        // use the new 'where' for index in SIM
                        where = "index = " + mIndexInSim;
                        Log.d(TAG, "where " + where);
                        Log.d(TAG, "iccUriForUsim ******** " + iccUriForUsim);

                        int deleteDone = getContentResolver().delete(iccUriForUsim, where, null);
                        Log.i(TAG, "deleteDone is " + deleteDone);
                        if (deleteDone == 1) {
                            Uri deleteUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                                    mContactId);
                            int deleteDB = getContentResolver().delete(deleteUri, null, null);
                            Log.i(TAG, "deleteDB is " + deleteDB);
                        }
                        finish();
                        return;
                    } else if (TextUtils.isEmpty(mUpdatephone)
                            && TextUtils.isEmpty(mUpdateName)
                            && (!TextUtils.isEmpty(mUpdatemail)
                                    || !TextUtils.isEmpty(mUpdateAdditionalNumber)
                                    || !mGroupAddList.isEmpty() || !mOldGroupAddList.isEmpty()
                            /** M:SNE @ { */
                            || !TextUtils.isEmpty(sUpdateNickname)/** M: @ } */
                            )) {
                        mNumberIsNull = true;
                    } else if (!TextUtils.isEmpty(mUpdatephone)) {
                        if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtilsEx.extractCLIRPortion(mUpdatephone))) {
                            mNumberInvalid = true;
                        }
                    }
                    if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
                        if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtilsEx
                                .extractCLIRPortion(mUpdateAdditionalNumber))) {
                            mFixNumberInvalid = true;
                        }
                    }
                    Log.i(TAG, "mFixNumberInvalid is " + mFixNumberInvalid);
                    // if (!TextUtils.isEmpty(mUpdatemail)) {
                    // if (!Pattern.matches(USIM_EMAIL_PATTERN, mUpdatemail)) {
                    // mEmailInvalid = true;
                    // }
                    // }
                }
                if (setSaveFailToastText()) {
                    mOnBackGoing = false;
                    return;
                }

                /** M:SNE @ { */
                if (!ExtensionManager.getInstance().getContactAccountExtension().isTextValid(
                        sUpdateNickname, mSlotId, ContactAccountExtension.TYPE_OPERATION_SNE,
                        ExtensionManager.COMMD_FOR_SNE)) {
                    showSaveFailToast();
                    return;
                }
                /** M: @ } */

                // query phonebookto load contacts to cache for update
                /**
                 * M:AAS @ { original code : cursor =
                 * resolver.query(SimCardUtils.SimUri.getSimUri(mSlotId),
                 * ADDRESS_BOOK_COLUMN_NAMES, null, null, null);
                 */
                String[] addressBookColumnName = ExtensionManager.getInstance()
                        .getContactAccountExtension().getProjection(
                                ContactAccountExtension.PROJECTION_ADDRESS_BOOK,
                                ADDRESS_BOOK_COLUMN_NAMES, ExtensionManager.COMMD_FOR_AAS);
                cursor = resolver.query(SimCardUtils.SimUri.getSimUri(mSlotId), addressBookColumnName,
                        null, null, null);
                /** M: @ } */
                if (cursor != null) {
                    try {
                        result = resolver.update(SimCardUtils.SimUri.getSimUri(mSlotId),
                                updatevalues, null, null);
                        Log.i(TAG, "updatevalues IS " + updatevalues);
                        Log.i(TAG, "result IS " + result);
                        if (updateFailToastText(result)) {
                            mOnBackGoing = false;
                            return;
                        }
                    } finally {
                        cursor.close();
                    }
                }
                Log.i(TAG, "update to db");
                // mtk80909 for ALPS00023212
                // final SubContactsUtils.NamePhoneTypePair namePhoneTypePair =
                // new SubContactsUtils.NamePhoneTypePair(
                // mUpdateName);
                // mUpdateName = namePhoneTypePair.name;
                // final int phoneType = namePhoneTypePair.phoneType;
                // final String phoneTypeSuffix =
                // namePhoneTypePair.phoneTypeSuffix;
                ContentValues namevalues = new ContentValues();
                String wherename = Data.RAW_CONTACT_ID + " = \'" + mRawContactId + "\'" + " AND "
                        + Data.MIMETYPE + "='" + StructuredName.CONTENT_ITEM_TYPE + "'";
                Log.i(TAG, "wherename is " + wherename + " mUpdateName:" + mUpdateName + "   ");

                // updata name
                if (!TextUtils.isEmpty(mUpdateName) && !TextUtils.isEmpty(mOldName)) {
                    namevalues.put(StructuredName.DISPLAY_NAME, mUpdateName);
                    /** M: Bug Fix for ALPS00370295 @{ */
                    namevalues.putNull(StructuredName.GIVEN_NAME);
                    namevalues.putNull(StructuredName.FAMILY_NAME);
                    namevalues.putNull(StructuredName.PREFIX);
                    namevalues.putNull(StructuredName.MIDDLE_NAME);
                    namevalues.putNull(StructuredName.SUFFIX);
                    /** @} */
                    int upname = resolver.update(Data.CONTENT_URI, namevalues, wherename, null);
                    Log.i(TAG, "upname is " + upname);
                } else if (!TextUtils.isEmpty(mUpdateName) && TextUtils.isEmpty(mOldName)) {
                    namevalues.put(StructuredName.RAW_CONTACT_ID, mRawContactId);
                    namevalues.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                    namevalues.put(StructuredName.DISPLAY_NAME, mUpdateName);
                    Uri upNameUri = resolver.insert(Data.CONTENT_URI, namevalues);
                    Log.i(TAG, "upNameUri is " + upNameUri);
                } else if (TextUtils.isEmpty(mUpdateName)) {
                    // update name is null,delete name row
                    int deleteName = resolver.delete(Data.CONTENT_URI, wherename, null);
                    Log.i(TAG, "deleteName is " + deleteName);
                }

                // update number
                ContentValues phonevalues = new ContentValues();
                String wherephone = Data.RAW_CONTACT_ID + " = \'" + mRawContactId + "\'" + " AND "
                        + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'" + " AND "
                        + Data.IS_ADDITIONAL_NUMBER + "=0";
                Log.i(TAG, " wherephone is " + wherephone);
                Log.i(TAG, " mOldPhone:" + mOldPhone + "|mUpdatephone:" + sPhone);
                if (!TextUtils.isEmpty(mUpdatephone) && !TextUtils.isEmpty(mOldPhone)) {
                    phonevalues.put(Phone.NUMBER, sPhone);
                    int upnumber = resolver.update(Data.CONTENT_URI, phonevalues, wherephone, null);
                    Log.i(TAG, "upnumber is " + upnumber);
                } else if (TextUtils.isEmpty(mOldPhone) && !TextUtils.isEmpty(mUpdatephone)) {
                    phonevalues.put(Phone.RAW_CONTACT_ID, mRawContactId);
                    phonevalues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                    phonevalues.put(Phone.NUMBER, mUpdatephone);
                    phonevalues.put(Data.IS_ADDITIONAL_NUMBER, 0);
                    phonevalues.put(Phone.TYPE, 2);

                    /** M:AAS Remove Phone.TYPE from phonevalues @ { */
                    ExtensionManager.getInstance().getContactAccountExtension()
                            .updateContentValues(null, phonevalues, null, null,
                                    ContactAccountExtension.CONTENTVALUE_INSERT_SIM, ExtensionManager.COMMD_FOR_AAS);
                    /** M: @ { */

                    Uri upNumberUri = resolver.insert(Data.CONTENT_URI, phonevalues);
                    Log.i(TAG, "upNumberUri is " + upNumberUri);
                } else if (TextUtils.isEmpty(mUpdatephone)) {
                    int deletePhone = resolver.delete(Data.CONTENT_URI, wherephone, null);
                    Log.i(TAG, "deletePhone is " + deletePhone);
                }
                // else if (TextUtils.isEmpty(sPhone) &&
                // !TextUtils.isEmpty(mUpdatephone)) {
                //                    
                // phonevalues.put(Phone.RAW_CONTACT_ID, mRawContactId);
                // phonevalues.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                // phonevalues.put(Data.IS_ADDITIONAL_NUMBER, 0);
                // // phonevalues.put(Phone.TYPE, phoneType);
                // phonevalues.put(Data.DATA2, 2);
                // phonevalues.put(Phone.NUMBER, mUpdatephone);
                // // mtk80909 for ALPS00023212
                // if (!TextUtils.isEmpty(phoneTypeSuffix)) {
                // phonevalues.put(Data.DATA15, phoneTypeSuffix);
                // } else {
                // phonevalues.putNull(Data.DATA15);
                // }
                // Uri upNumberUri = resolver.insert(Data.CONTENT_URI,
                // phonevalues);
                // Log.i(TAG, "upNumberUri is " + upNumberUri);
                // }

                // if USIM
                int errorType = -1;
                // Comment out by mtk80908, it can be removed after W1145
                // StringBuilder groupNameList = new StringBuilder();
                // Comment out by mtk80908, it can be removed after W1145
                if (mSimType.equals("USIM")) {
                    // update emails
                    ContentValues emailvalues = new ContentValues();

                    emailvalues.put(Email.TYPE, Email.TYPE_MOBILE);

                    String wheremail = Data.RAW_CONTACT_ID + " = \'" + mRawContactId + "\'"
                            + " AND " + Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "'";
                    Log.i(TAG, "wheremail is " + wheremail);
                    if (!TextUtils.isEmpty(mUpdatemail) && !TextUtils.isEmpty(mOldEmail)) {
                        emailvalues.put(Email.DATA, mUpdatemail);
                        int upemail = resolver.update(Data.CONTENT_URI, emailvalues, wheremail,
                                null);
                        Log.i(TAG, "upemail is " + upemail);
                    } else if (!TextUtils.isEmpty(mUpdatemail) && TextUtils.isEmpty(mOldEmail)) {
                        emailvalues.put(Email.RAW_CONTACT_ID, mRawContactId);
                        emailvalues.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                        emailvalues.put(Email.DATA, mUpdatemail);
                        Uri upEmailUri = resolver.insert(Data.CONTENT_URI, emailvalues);
                        Log.i(TAG, "upEmailUri is " + upEmailUri);
                    } else if (TextUtils.isEmpty(mUpdatemail)) {
                        // update email is null,delete email row
                        int deleteEmail = resolver.delete(Data.CONTENT_URI, wheremail, null);
                        Log.i(TAG, "deleteEmail is " + deleteEmail);
                    }

                    /** M:SNE update nickname to db.@ { */
                    SneExt.updateDataToDb(mSlotId, mAccountType, resolver, sUpdateNickname,
                            mOldNickname, mRawContactId);
                    /** M: @ } */

                    // update additional number
                    ContentValues additionalvalues = new ContentValues();
                    /** M:AAS @ { */
                    if (!ExtensionManager.getInstance().getContactAccountExtension()
                            .updateDataToDb(mAccountType, resolver, mAnrsList, mOldAnrsList,
                                    mRawContactId, ContactAccountExtension.DB_UPDATE_ANR, ExtensionManager.COMMD_FOR_AAS)) {
                        /** M: @ } */
                        String whereadditional = Data.RAW_CONTACT_ID + " = \'" + mRawContactId
                                + "\'" + " AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE
                                + "'" + " AND " + Data.IS_ADDITIONAL_NUMBER + " =1";
                        Log.i(TAG, "whereadditional is " + whereadditional);

                        if (!TextUtils.isEmpty(mUpdateAdditionalNumber)
                                && !TextUtils.isEmpty(mOldOtherPhone)) {
                            additionalvalues.put(Phone.NUMBER, sOtherPhone);
                            int upadditional = resolver.update(Data.CONTENT_URI, additionalvalues,
                                    whereadditional, null);
                            Log.i(TAG, "upadditional is " + upadditional);
                        } else if (!TextUtils.isEmpty(mUpdateAdditionalNumber)
                                && TextUtils.isEmpty(mOldOtherPhone)) {
                            additionalvalues.put(Phone.RAW_CONTACT_ID, mRawContactId);
                            additionalvalues.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                            additionalvalues.put(Phone.NUMBER, mUpdateAdditionalNumber);
                            additionalvalues.put(Data.IS_ADDITIONAL_NUMBER, 1);
                            additionalvalues.put(Data.DATA2, 7);
                            Uri upAdditionalUri = resolver.insert(Data.CONTENT_URI,
                                    additionalvalues);
                            Log.i(TAG, "upAdditionalUri is " + upAdditionalUri);
                        } else if (TextUtils.isEmpty(mUpdateAdditionalNumber)) {
                            // update additional number is null, delete
                            // additional number row
                            int deleteAdditional = resolver.delete(Data.CONTENT_URI,
                                    whereadditional, null);
                            Log.i(TAG, "deleteAdditional is " + deleteAdditional);
                        }
                    }

                    // update group
                    if (mOldGroupAddList.size() > 0) {
                        for (Entry<Long, String> entry : mOldGroupAddList.entrySet()) {
                            long grpId = entry.getKey();
                            String grpName = entry.getValue();
                            int ugrpId = -1;
                            try {
                                ugrpId = USIMGroup.hasExistGroup(mSlotId, grpName);
                            } catch (RemoteException e) {
                                ugrpId = -1;
                            }
                            if (ugrpId > 0) {
                                USIMGroup.deleteUSIMGroupMember(mSlotId, (int) mIndexInSim, ugrpId);
                            }
                            int delCount = resolver.delete(Data.CONTENT_URI, Data.MIMETYPE + "='"
                                    + GroupMembership.CONTENT_ITEM_TYPE + "' AND "
                                    + Data.RAW_CONTACT_ID + "=" + mRawContactId + " AND "
                                    + ContactsContract.Data.DATA1 + "=" + grpId, null);
                            Log.d(TAG, "[USIM group]DB deleteCount:" + delCount);
                            // sync USIM group info. Delete current group if its
                            // member is null.
                            // if (delCount > 0) {
                            // USIMGroup.syncExistUSIMGroupDelIfNoMember(
                            // EditSimContactActivity.this, mSlotId, (int)
                            // mIndicate,
                            // grpName, ugrpId);
                            // }
                        }
                    }
                    if (mGroupAddList.size() > 0) {
                        Iterator<Entry<Long, String>> iter = mGroupAddList.entrySet().iterator();
                        while (iter.hasNext()) {
                            Entry<Long, String> entry = iter.next();
                            long grpId = entry.getKey();
                            String grpName = entry.getValue();
                            int ugrpId = -1;
                            try {
                                ugrpId = USIMGroup.syncUSIMGroupNewIfMissing(mSlotId, grpName);
                            } catch (RemoteException e) {
                                ugrpId = -1;
                            } catch (USIMGroupException e) {
                                errorType = e.getErrorType();
                                ugrpId = -1;
                            }
                            if (ugrpId > 0) {
                                USIMGroup.addUSIMGroupMember(mSlotId, (int) mIndexInSim, ugrpId);
                                // insert into contacts DB
                                additionalvalues.clear();
                                additionalvalues.put(Data.MIMETYPE,
                                        GroupMembership.CONTENT_ITEM_TYPE);
                                additionalvalues.put(GroupMembership.GROUP_ROW_ID, grpId);
                                additionalvalues.put(Data.RAW_CONTACT_ID, mRawContactId);
                                resolver.insert(Data.CONTENT_URI, additionalvalues);
                            }
                        }
                    }
                }

                showResultToastText(errorType, null);
                // USIM group end
                if (errorType == -1) {
                    setResult(RESULT_OK, null);
                    // startViewActivity(mLookupUri);

                }
                finish();
                return;
            }
        }

        /**
         * M: [Gemini+] check the phone status for given slot
         * set the status to fields.
         */
        private void checkPhoneStatus() {
            mAirPlaneModeOn = !SimCardUtils.isRadioOn(mSlotId);
            mFDNEnabled = SimCardUtils.isFdnEnabed(mSlotId);
            mSIMInvalid = !SimCardUtils.isSimStateReady(mSlotId);
        }
    }

    private boolean setSaveFailToastText() {
        mSaveFailToastStrId = -1;
        Log.i(TAG, "setSaveFailToastText mPhbReady is " + mPhbReady);
        if (!mPhbReady) {
            mSaveFailToastStrId = R.string.phone_book_busy;
            mQuitEdit = true;
        } else if (mAirPlaneModeOn) {
            mSaveFailToastStrId = R.string.AirPlane_mode_on;
            mAirPlaneModeOn = false;
            mQuitEdit = true;
        } else if (mFDNEnabled) {
            mSaveFailToastStrId = R.string.FDNEnabled;
            mFDNEnabled = false;
            mQuitEdit = true;
        } else if (mSIMInvalid) {
            mSaveFailToastStrId = R.string.sim_invalid;
            mSIMInvalid = false;
            mQuitEdit = true;
        } else if (mNumberIsNull) {
            mSaveFailToastStrId = R.string.cannot_insert_null_number;
            mNumberIsNull = false;
        } else if (mNumberInvalid) {
            mSaveFailToastStrId = R.string.sim_invalid_number;
            mNumberInvalid = false;
        } else if (mEmailInvalid) {
            mSaveFailToastStrId = R.string.email_invalid;
            mEmailInvalid = false;
        } else if (mEmail2GInvalid) {
            mSaveFailToastStrId = R.string.email_2g_invalid;
            mEmail2GInvalid = false;
        } else if (mFixNumberInvalid) {
            mSaveFailToastStrId = R.string.sim_invalid_fix_number;
            mFixNumberInvalid = false;
        } else if (mAirPlaneModeOnNotEdit) {
            mSaveFailToastStrId = R.string.AirPlane_mode_on_edit;
            mAirPlaneModeOnNotEdit = false;
            mQuitEdit = true;
        } else if (mDoublePhoneNumber) {
            mSaveFailToastStrId = R.string.has_double_phone_number;
            mDoublePhoneNumber = false;
        }

        Log.i(TAG, "mSaveFailToastStrId IS " + mSaveFailToastStrId);
        if (mSaveFailToastStrId >= 0) {
            EditSimContactActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    if (mSaveFailToastStrId == R.string.err_icc_no_phone_book) {
                        String specialErrorText = getResources().getString(mSaveFailToastStrId, mSimType);
                        MtkToast.toast(EditSimContactActivity.this, specialErrorText);
                    } else {
                        Toast.makeText(EditSimContactActivity.this, mSaveFailToastStrId,
                                Toast.LENGTH_SHORT).show();
                    }
                    backToFragment();
                }
            });
            return true;
        }
        return false;
    }

    private boolean setSaveFailToastText2(Uri checkUri) {
        if (checkUri != null && "error".equals(checkUri.getPathSegments().get(0))) {
            mSaveFailToastStrId = -1;
            if ("-1".equals(checkUri.getPathSegments().get(1))) {
                mNumberLong = true;
                mSaveFailToastStrId = R.string.number_too_long;
                mNumberLong = false;
            } else if ("-2".equals(checkUri.getPathSegments().get(1))) {
                mNameLong = true;
                mSaveFailToastStrId = R.string.name_too_long;
                mNameLong = false;
            } else if ("-3".equals(checkUri.getPathSegments().get(1))) {
                mStorageFull = true;
                mSaveFailToastStrId = R.string.storage_full;
                mStorageFull = false;
                mQuitEdit = true;
            } else if ("-6".equals(checkUri.getPathSegments().get(1))) {
                mFixNumberLong = true;
                mSaveFailToastStrId = R.string.fix_number_too_long;
                mFixNumberLong = false;
            } else if ("-10".equals(checkUri.getPathSegments().get(1))) {
                mGeneralFailure = true;
                mSaveFailToastStrId = R.string.generic_failure;
                mGeneralFailure = false;
                mQuitEdit = true;
            } else if ("-11".equals(checkUri.getPathSegments().get(1))) {
                mGeneralFailure = true;
                mSaveFailToastStrId = R.string.err_icc_no_phone_book;
                mGeneralFailure = false;
                mQuitEdit = true;
            } else if ("-12".equals(checkUri.getPathSegments().get(1))) {
                mGeneralFailure = true;
                mSaveFailToastStrId = R.string.error_save_usim_contact_email_lost;
                mGeneralFailure = false;
            } else if ("-13".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.email_too_long;
            }
            Log.i(TAG, "setSaveFailToastText2 mSaveFailToastStrId IS " + mSaveFailToastStrId);
            if (mSaveFailToastStrId >= 0) {
                EditSimContactActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (mSaveFailToastStrId == R.string.err_icc_no_phone_book) {
                            String specialErrorText = getResources().getString(mSaveFailToastStrId, mSimType);
                            MtkToast.toast(EditSimContactActivity.this, specialErrorText);
                        } else {
                            Toast.makeText(EditSimContactActivity.this, mSaveFailToastStrId,
                                    Toast.LENGTH_SHORT).show();
                        }
                        backToFragment();
                    }
                });
                return true;
            }
            return false;
        } else {
            return !(checkUri != null);
        }

    }

    private boolean updateFailToastText(int result) {
        mSaveFailToastStrId = -1;
        if (result == -1) {
            mSaveFailToastStrId = R.string.number_too_long;
        } else if (result == -2) {
            mSaveFailToastStrId = R.string.name_too_long;
        } else if (result == -3) {
            mSaveFailToastStrId = R.string.storage_full;
            mQuitEdit = true;
        } else if (result == -6) {
            mSaveFailToastStrId = R.string.fix_number_too_long;
        } else if (result == -10) {
            mSaveFailToastStrId = R.string.generic_failure;
            mQuitEdit = true;
        } else if (result == -11) {
            mSaveFailToastStrId = R.string.err_icc_no_phone_book;
            mQuitEdit = true;
        } else if (result == -12) {
            mSaveFailToastStrId = R.string.error_save_usim_contact_email_lost;
        } else if (result == -13) {
            mSaveFailToastStrId = R.string.email_too_long;
        }
        if (mSaveFailToastStrId >= 0) {
            EditSimContactActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(EditSimContactActivity.this, mSaveFailToastStrId,
                            Toast.LENGTH_SHORT).show();
                    backToFragment();
                }
            });

            return true;
        }
        return false;
    }

    private void showResultToastText(int errorType, String param1) {
        String toastMsg = null;
        if (errorType == -1) {
            toastMsg = getString(R.string.contactSavedToast);
            // added for performance auto-test cases.
            Log.i(TAG, "[mtk performance result]:" + System.currentTimeMillis());
        } else {
            toastMsg = getString(USIMGroupException.getErrorToastId(errorType));
        }
        final String msg = toastMsg;
        if (errorType == -1 && compleDate()) {
            return;
            // do nothing
        } else if (errorType == -1 && !compleDate()) {
            EditSimContactActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(EditSimContactActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            EditSimContactActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(EditSimContactActivity.this, msg, Toast.LENGTH_SHORT).show();
                    backToFragment();
                }
            });
        }
    }

    private void doSaveAction(int mode) {
        Log.i(TAG, "In doSaveAction ");

        if (mode == MODE_INSERT) {
            Log.i("huibin", "doSaveAction mode == MODE_INSERT");
            Log.i(TAG, "mode == MODE_INSERT");

            Handler handler = getsaveContactHandler();
            if (handler != null) {
                handler.post(new InsertSimContactThread(MODE_INSERT));
            }
        } else if (mode == MODE_EDIT) {
            Log.i("huibin", "doSaveAction mode == MODE_EDIT");
            Handler handler = getsaveContactHandler();
            if (handler != null) {
                handler.post(new InsertSimContactThread(MODE_EDIT));
            }
        }
    }

    public void startViewActivity(Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        startActivity(intent);
        setResult(RESULT_OK, null);
    }

    public void backToFragment() {
        Log.i(TAG, "[backToFragment]");
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("simData1", mSimData);
        intent.putExtra("mQuitEdit", mQuitEdit);
        setResult(RESULT_CANCELED, intent);
        mQuitEdit = false;
        finish();
    }

    public boolean compleDate() {

        boolean compleName = false;
        if (!TextUtils.isEmpty(sName) && !TextUtils.isEmpty(mOldName)) {
            if (sName.equals(mOldName)) {
                compleName = true;
            }
        } else if (TextUtils.isEmpty(sName) && TextUtils.isEmpty(mOldName)) {
            compleName = true;
        }

        boolean complePhone = false;
        if (!TextUtils.isEmpty(sPhone) && !TextUtils.isEmpty(mOldPhone)) {
            if (sPhone.equals(mOldPhone)) {
                complePhone = true;
            }
        } else if (TextUtils.isEmpty(sPhone) && TextUtils.isEmpty(mOldPhone)) {
            complePhone = true;
        }

        boolean compleEmail = false;
        if (!TextUtils.isEmpty(sEmail) && !TextUtils.isEmpty(mOldEmail)) {
            if (sEmail.equals(mOldEmail)) {
                compleEmail = true;
            }
        } else if (TextUtils.isEmpty(sEmail) && TextUtils.isEmpty(mOldEmail)) {
            compleEmail = true;
        }

        boolean compleOther = false;
        if (!TextUtils.isEmpty(sOtherPhone) && !TextUtils.isEmpty(mOldOtherPhone)) {
            if (sOtherPhone.equals(mOldOtherPhone)) {
                compleOther = true;
            }
        } else if (TextUtils.isEmpty(sOtherPhone) && TextUtils.isEmpty(mOldOtherPhone)) {
            compleOther = true;
        }

        boolean compleGroup = false;
        if (mGroupAddList != null && mOldGroupAddList != null) {
            if (mGroupAddList.equals(mOldGroupAddList)) {
                compleGroup = true;
            }
        } else if (mGroupAddList == null && mOldGroupAddList == null) {
            compleGroup = true;
        }
        Log.i(TAG, "[showResultToastText]compleName : " + compleName + " | complePhone : "
                + complePhone + " | compleOther : " + compleOther + " | compleEmail: "
                + compleEmail + " | compleGroup : " + compleGroup);
        Log.i(TAG, "[showResultToastText] sName : " + sName + " | mOldName : " + mOldName
                + " | sEmail : " + sEmail + " | mOldEmail : " + mOldEmail);
        return (compleName && complePhone && compleOther && compleEmail && compleGroup);
    }

    /**
     * removing white space characters and '-' characters from the beginning and end of the string.
     * @param number always additional number
     * @return
     */
    private String trimAnr(String number) {
        String trimNumber = number;
        if (!TextUtils.isEmpty(trimNumber)) {
            Log.i(TAG, "[run] befor replaceall additional_number : " + trimNumber);
            trimNumber = trimNumber.replaceAll("-", "");
            trimNumber = trimNumber.replaceAll(" ", "");
            Log.i(TAG, "[run] after replaceall additional_number : " + trimNumber);
        }
        return trimNumber;
    }

    /** M:SNE @ { */
    private static String sNickname = "";
    private static String sUpdateNickname = "";
    private String mOldNickname = "";

    private void showSaveFailToast() {
        EditSimContactActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(EditSimContactActivity.this, R.string.nickname_too_long,
                        Toast.LENGTH_SHORT).show();
                backToFragment();
            }
        });
    }
    /** M: @ } */
}
