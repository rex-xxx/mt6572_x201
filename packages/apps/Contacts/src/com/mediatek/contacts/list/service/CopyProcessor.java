package com.mediatek.contacts.list.service;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
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
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.ext.Anr;
import com.android.contacts.ext.ContactAccountExtension;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.vcard.ProcessorBase;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.SubContactsUtils;
import com.mediatek.contacts.extension.aassne.SimUtils;
import com.mediatek.contacts.extension.aassne.SneExt;
import com.mediatek.contacts.list.MultiContactsDuplicationFragment;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.util.ErrorCause;
import com.mediatek.phone.SIMInfoWrapper;

import java.util.ArrayList;
import java.util.List;

public class CopyProcessor extends ProcessorBase {

    private static final String LOG_TAG = MultiContactsDuplicationFragment.TAG;
    private static final boolean DEBUG = MultiContactsDuplicationFragment.DEBUG;

    private final MultiChoiceService mService;
    private final ContentResolver mResolver;
    private final List<MultiChoiceRequest> mRequests;
    private final int mJobId;
    private final MultiChoiceHandlerListener mListener;

    private PowerManager.WakeLock mWakeLock;

    private final Account mAccountSrc;
    private final Account mAccountDst;

    private volatile boolean mCanceled;
    private volatile boolean mDone;
    private volatile boolean mIsRunning;
    
    private static final int MAX_OP_COUNT_IN_ONE_BATCH = 400;
    private static final int RETRYCOUNT = 20;

    private static final String[] DATA_ALLCOLUMNS = new String[] {
        Data._ID,
        Data.MIMETYPE,
        Data.IS_PRIMARY,
        Data.IS_SUPER_PRIMARY,
        Data.DATA1,
        Data.DATA2,
        Data.DATA3,
        Data.DATA4,
        Data.DATA5,
        Data.DATA6,
        Data.DATA7,
        Data.DATA8,
        Data.DATA9,
        Data.DATA10,
        Data.DATA11,
        Data.DATA12,
        Data.DATA13,
        Data.DATA14,
        Data.DATA15,
        Data.SYNC1,
        Data.SYNC2,
        Data.SYNC3,
        Data.SYNC4,
        Data.IS_ADDITIONAL_NUMBER
    };

    public CopyProcessor(final MultiChoiceService service,
            final MultiChoiceHandlerListener listener, final List<MultiChoiceRequest> requests,
            final int jobId, final Account sourceAccount, final Account destinationAccount) {
        mService = service;
        mResolver = mService.getContentResolver();
        mListener = listener;

        mRequests = requests;
        mJobId = jobId;
        mAccountSrc = sourceAccount;
        mAccountDst = destinationAccount;

        final PowerManager powerManager = (PowerManager) mService.getApplicationContext()
                .getSystemService("power");
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (DEBUG) {
            Log.d(LOG_TAG, "CopyProcessor received cancel request");
        }
        if (mDone || mCanceled) {
            return false;
        }
        mCanceled = true;
        if (!mIsRunning) {
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceled(MultiChoiceService.TYPE_COPY, mJobId, -1, -1, -1);
        }
        return true;
    }

    @Override
    public int getType() {
        return MultiChoiceService.TYPE_COPY;
    }

    @Override
    public synchronized boolean isCancelled() {
        return mCanceled;
    }

    @Override
    public synchronized boolean isDone() {
        return mDone;
    }

    @Override
    public void run() {
        try {
            mIsRunning = true;
            mWakeLock.acquire();
            if (AccountType.ACCOUNT_TYPE_SIM.equals(mAccountDst.type)
                    || AccountType.ACCOUNT_TYPE_USIM.equals(mAccountDst.type)
                    || AccountType.ACCOUNT_TYPE_UIM.equals(mAccountDst.type)) {
                copyContactsToSimWithRadioStateCheck();
            } else {
                copyContactsToAccount();
            }
        } finally {
            synchronized (this) {
                mDone = true;
            }
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    private void copyContactsToSim() {
        int errorCause = ErrorCause.NO_ERROR;

        // Process sim data, sim id or slot
        AccountWithDataSetEx account = (AccountWithDataSetEx) mAccountDst;
        Log.d(LOG_TAG, "[copyContactsToSim]AccountName:" + account.name
                + "|accountType:" + account.type);
        int dstSlotId = account.getSlotId();
        SIMInfo dstSimInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(dstSlotId);
        long dstSimId = (dstSimInfo != null) ? dstSimInfo.mSimId : -1;
        Log.d(LOG_TAG, "[copyContactsToSim]dstSlotId:" + dstSlotId + "|dstSimId:" + dstSimId);
        boolean isTargetUsim = SimCardUtils.isSimUsimType(dstSlotId);
        String dstSimType = isTargetUsim ? "USIM" : "SIM";
        Log.d(LOG_TAG, "[copyContactsToSim]dstSimType:" + dstSimType);

        if (!isSimReady(dstSlotId) || !isPhoneBookReady(dstSlotId)) {
            errorCause = ErrorCause.SIM_NOT_READY;
            mService.handleFinishNotification(mJobId, false);
            mListener.onFailed(MultiChoiceService.TYPE_COPY, mJobId, mRequests.size(),
                    0, mRequests.size(), errorCause);
            return;
        }

        ArrayList<String> numberArray = new ArrayList<String>();
        ArrayList<String> additionalNumberArray = new ArrayList<String>();
        ArrayList<String> emailArray = new ArrayList<String>();
        /** M:AAS&SNE
         *  phoneTypeArray used to record number's type.
         *  additionalNumberTypeArray used to record additional number's type. @ { */
        ArrayList<Integer> phoneTypeArray = new ArrayList<Integer>();
        ArrayList<Integer> additionalTypeArray = new ArrayList<Integer>();
        /// SNE
        ArrayList<String> nicknameArray = new ArrayList<String>();
        /** M: @ } */
        
        String targetName = null;

        ContentResolver resolver = this.mResolver;

// The following lines are provided and maintained by Mediatek inc.
// Keep previous code here.
// Description: 
//    The following code is used to do copy group data to usim. However, it also needs
//        to implement function that can copy group data in different account before 
//        using the following code.        
//
// Previous Code:        
//    HashMap<Integer, String> grpIdNameCache = new HashMap<Integer, String>();
//    HashMap<String, Integer> ugrpNameIdCache = new HashMap<String, Integer>();
//    HashSet<Long> grpIdSet = new HashSet<Long>();
//    ArrayList<Integer> ugrpIdArray = new ArrayList<Integer>();
//    Cursor groupCursor = resolver.query(Groups.CONTENT_SUMMARY_URI, 
//            new String[] {Groups._ID, Groups.TITLE}, 
//            Groups.DELETED + "=0 AND " 
//                + Groups.ACCOUNT_NAME + "='" + mAccountSrc.name + "' AND "
//                + Groups.ACCOUNT_TYPE + "='" + mAccountSrc.type + "'", 
//            null, null);
//    try {
//        while (groupCursor.moveToNext()) {
//            int gId = groupCursor.getInt(0);
//            String gTitle = groupCursor.getString(1);
//            grpIdNameCache.put(gId, gTitle);
//            Log.d(LOG_TAG, "[USIM Group]cache phone group. gId:" + gId + "|gTitle:" + gTitle);
//        }
//    } finally {
//        if (groupCursor != null)
//            groupCursor.close();
//    }
// The previous lines are provided and maintained by Mediatek inc.        

        // Process request one by one
        int totalItems = mRequests.size();
        int successfulItems = 0;
        int currentCount = 0;
        int iccCardMaxEmailCount = SimCardUtils.getIccCardEmailCount(dstSlotId);

        boolean isSimStorageFull = false;
        final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (MultiChoiceRequest request : this.mRequests) {
            if (mCanceled) {
                break;
            }
            if (!isSimReady(dstSlotId) || !isPhoneBookReady(dstSlotId)) {
                Log.d(LOG_TAG, "copyContactsToSim run: sim not ready");
                errorCause = ErrorCause.ERROR_UNKNOWN;
                operationList.clear();
                break;
            }
            currentCount++;
            // Notify the copy process on notification bar
            mListener.onProcessed(MultiChoiceService.TYPE_COPY, mJobId, currentCount, totalItems,
                    request.mContactName);

            // reset data
            numberArray.clear();
            additionalNumberArray.clear();
            
            /** M:AAS @ { */
            phoneTypeArray.clear();
            additionalTypeArray.clear();
            /** M: @ } */
            emailArray.clear();
            targetName = null;

            int contactId = request.mContactId;

            // Query to get all src data resource.
            Uri dataUri = Uri.withAppendedPath(
                    ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                    Contacts.Data.CONTENT_DIRECTORY);
            final String[] projection = new String[] {
                    Contacts._ID, 
                    Contacts.Data.MIMETYPE, 
                    Contacts.Data.DATA1,
                    Contacts.Data.IS_ADDITIONAL_NUMBER,
            };
            /**
             * M:AAS @ { original code: 
             Cursor c = resolver.query(dataUri, projection, null, null, null); } */
            final String[] plugProjection = ExtensionManager.getInstance().getContactAccountExtension().getProjection(
                    ContactAccountExtension.PROJECTION_COPY_TO_SIM, projection, ExtensionManager.COMMD_FOR_AAS);
            String[] dataViewProject = new String[plugProjection.length + 2];
            System.arraycopy(plugProjection, 0, dataViewProject, 0, plugProjection.length);
            System.arraycopy(new String[] {Contacts.NAME_RAW_CONTACT_ID, Contacts.Data.RAW_CONTACT_ID}, 0, dataViewProject, plugProjection.length, 2);
            Cursor c = resolver.query(dataUri, dataViewProject, null, null, null);
            /** M: @ } */

            if (c != null && c.moveToFirst()) {
                do {
                    String mimeType = c.getString(1);
                    if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        // For phone number
                        String number = c.getString(2);
                        /** M:AAS @ { */
                        int isAdditionalNumber = c.getInt(3);
                        boolean isAasEnabled = ExtensionManager.getInstance().getContactAccountExtension()
                                .isFeatureEnabled(ExtensionManager.COMMD_FOR_AAS);
                        if (isAdditionalNumber == 1) {
                            additionalNumberArray.add(number);
                            if (isAasEnabled) {
                                additionalTypeArray.add(c.getInt(4));
                            }
                        } else {
                            numberArray.add(number);
                            if (isAasEnabled) {
                                phoneTypeArray.add(c.getInt(4));
                            }
                        }
                        /** M: @ } */
                    } else if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)
                            && c.getInt(c.getColumnIndexOrThrow(Contacts.NAME_RAW_CONTACT_ID)) == c.getInt(c
                                    .getColumnIndexOrThrow(Contacts.Data.RAW_CONTACT_ID))) {
                        // For name
                        targetName = c.getString(2);
                    }
                    if (isTargetUsim) {
                        ///M:Bug Fix for ALPS00566570,some USIM Card do not support storing Email address.
                        if (Email.CONTENT_ITEM_TYPE.equals(mimeType) && iccCardMaxEmailCount > 0) {
                            // For email
                            String email = c.getString(2);
                            emailArray.add(email);
// The following lines are provided and maintained by Mediatek inc.
// Keep previous code here.
// Description: 
//        The following code is used to do copy group data to usim.
//
// Previous Code:
// } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
//    // For group
//    int grpId = c.getInt(1);
//    // Check group id. Here Do not process unknown group.
//    if (grpIdNameCache.containsKey(grpId)) {
//        String grpName = grpIdNameCache.get(grpId);
//        int ugrpId = -1;
//        if (ugrpNameIdCache.containsKey(grpName))
//            ugrpId = ugrpNameIdCache.get(grpName);
//        else {
//            try {
//                USIMGroup.syncUSIMGroupDelIfNoMember(mService, dstSlotId,
//                        (int) dstSimId);
//                ugrpId = USIMGroup.syncUSIMGroupNewIfMissing(dstSlotId,
//                        grpName);
//            } catch (android.os.RemoteException e) {
//                ugrpId = -1;
//                errorCause = ErrorCause.ERROR_UNKNOWN;
//                e.printStackTrace();
//            } catch (USIMGroupException e) {
//                ugrpId = -1;
//                errorCause += e.getErrorType();
//                if (e.getErrorType() == USIMGroupException.GROUP_NAME_OUT_OF_BOUND) {
//                    errorCause = ErrorCause.USIM_GROUP_NAME_OUT_OF_BOUND;
//                } else if (e.getErrorType() == USIMGroupException.GROUP_NUMBER_OUT_OF_BOUND) {
//                    errorCause = ErrorCause.USIM_GROUP_NUMBER_OUT_OF_BOUND;
//                } else {
//                    errorCause = ErrorCause.ERROR_UNKNOWN;
//                }
//                e.printStackTrace();
//            }
//            ugrpNameIdCache.put(grpName, ugrpId);
//        }
//        if (ugrpId > 0) {
//            grpIdSet.add((long) grpId);
//            ugrpIdArray.add(ugrpId);
//        }
//    }
// The previous lines are provided and maintained by Mediatek inc.
                        /**M:SNE @ { */
                        } else if (SneExt.isNickname(mimeType)) {
                            String nickName = c.getString(2);
                            nicknameArray.add(nickName);
                        }
                        /**M: @ } */
                    }
                } while (c.moveToNext());
            }
            if (c != null) {
                c.close();
            }

            // copy new resournce to target sim or usim,
            // and insert into database if sucessful
            Uri dstSimUri = SimCardUtils.SimUri.getSimUri(dstSlotId);
            int maxCount = TextUtils.isEmpty(targetName) ? 0 : 1;
            /** M:AAS, so far its value is 1; @ {*/
            int maxAnrCount = ExtensionManager.getInstance().getContactDetailExtension().getAdditionNumberCount(
                    dstSlotId, ExtensionManager.COMMD_FOR_AAS);
            /** M: @ } */
            
            /** M: Bug Fix for ALPS00557517 @{ */
            int usimMaxAnrCount = SimCardUtils.getAnrCount(dstSlotId);
            /** @ } */
            if (isTargetUsim) {
                int numberCount = numberArray.size();
                int additionalCount = additionalNumberArray.size();
                int emailCount = emailArray.size();
                
                maxCount = (maxCount > additionalCount) ? maxCount : additionalCount;
                maxCount = (maxCount > emailCount) ? maxCount : emailCount;
                /** M:AAS @ { */
                int numberQuota;
                if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureAccount(mAccountDst.type,
                        ExtensionManager.COMMD_FOR_AAS)) {
                    numberQuota = (int) ((numberCount + additionalCount) / (1.0 + maxAnrCount) + (float) maxAnrCount
                            / (1.0 + maxAnrCount));
                    Log.i(LOG_TAG, "maxAnr=" + maxAnrCount + "; numberQuota=" + numberQuota);
                } else {
                    /**
                     * M: Bug Fix for ALPS00557517
                     * origin code:
                     * numberQuota = (int) ((numberCount + additionalCount) / 2.0 + 0.5);
                     * @ { */
                    numberQuota = (int) ((numberCount + additionalCount) / (1.0 + usimMaxAnrCount) + (float) usimMaxAnrCount
                            / (1.0 + usimMaxAnrCount));
                    /** @ } */
                }
                /** M: @ } */

                maxCount = maxCount > numberQuota ? maxCount : numberQuota;
            } else {
                numberArray.addAll(additionalNumberArray);
                /** M:AAS @ { */
                phoneTypeArray.addAll(additionalTypeArray);
                /** M: @ } */
                additionalNumberArray.clear();
                int numberCount = numberArray.size();
                maxCount = maxCount > numberCount ? maxCount : numberCount;
            }
            int sameNameCount = 0;
            ContentValues values = new ContentValues();
            String simTag = null;
            String simNum = null;
            String simAnrNum = null;
            String simEmail = null;
            /** M:SNE @ { */
            String simNickname = null;
            /** M: @ } */

            simTag = sameNameCount > 0 ? (targetName + sameNameCount) : targetName;
            simTag = TextUtils.isEmpty(simTag) ? "" : simTag;
            if ((simTag == null || simTag.isEmpty() || simTag.length() == 0)
                    && numberArray.isEmpty()) {
                Log.e(LOG_TAG, " name and number are empty");
                errorCause = ErrorCause.ERROR_UNKNOWN;
                continue;
            }

            int subContact = 0;
            /** M:AAS @ { */
            ArrayList<Anr> anrsList = new ArrayList<Anr>();
            /** M: @ } */
            for (int i = 0; i < maxCount; i++) {
                values.clear();
                values.put("tag", simTag);
                Log.d(LOG_TAG, "copyContactsToSim tag is " + simTag);
                simNum = null;
                simAnrNum = null;
                simEmail = null;
                if (!numberArray.isEmpty()) {
                    simNum = numberArray.remove(0);
                    simNum = TextUtils.isEmpty(simNum) ? "" : simNum.replace("-", "");
                    values.put("number", PhoneNumberUtils.stripSeparators(simNum));
                    Log.d(LOG_TAG, "copyContactsToSim number is " + simNum);

                    /** M:AAS, correspond to numberArray.remove(0). @ { */
                    ExtensionManager.getInstance().getContactListExtension().checkPhoneTypeArray(
                            mAccountDst.type, phoneTypeArray,ExtensionManager.COMMD_FOR_AAS);
                    /** M: @ } */
                }
                
                /** M:AAS @ {*/
                anrsList.clear();
                /** M: @ }*/
                if (isTargetUsim) {
                    Log.d(LOG_TAG, "copyContactsToSim copy to USIM");
                    if (!additionalNumberArray.isEmpty()) {
                        /**
                         * M:AAS,save aas info to USim card. So far, it only supports 1 anr.
                         * TODO:support multi-anrs in the feature.  @ { */
                        if (!ExtensionManager.getInstance().getContactAccountExtension().buildValuesForSim(
                                mAccountDst.type, mService.getApplicationContext(), values, additionalNumberArray,
                                additionalTypeArray, maxAnrCount, dstSlotId, anrsList, ExtensionManager.COMMD_FOR_AAS)) {
                            /** M: @ } */
                            /**
                             * M: Bug Fix for ALPS00557517
                             * origin code:
                             *  Log.d(LOG_TAG, "additional number array is not empty");
                             * simAnrNum = additionalNumberArray.remove(0);
                             * simAnrNum = TextUtils.isEmpty(simAnrNum) ? "" : simAnrNum.replace("-","");
                             * values.put("anr", PhoneNumberUtils.stripSeparators(simAnrNum));
                             * Log.d(LOG_TAG, "copyContactsToSim anr is " + simAnrNum);
                             * @ { */
                            int loop = additionalNumberArray.size() < usimMaxAnrCount ? additionalNumberArray.size() : usimMaxAnrCount;
                            for (int j = 0; j < loop; j++) {
                                simAnrNum = additionalNumberArray.remove(0);
                                simAnrNum = TextUtils.isEmpty(simAnrNum) ? "" : simAnrNum.replace("-", "");
                                values.put("anr", PhoneNumberUtils.stripSeparators(simAnrNum));
                            }
                            if (!additionalNumberArray.isEmpty()){ 
                                numberArray.addAll(additionalNumberArray);
                                additionalNumberArray.clear();
                            }
                            /** @ } */
                        }
                    } else if (!numberArray.isEmpty()) {
                        /** M:AAS,save aas info to USim card. So far, it only supports 1 anr.
                         * TODO:support multi-anrs in the feature. @ { */
                        if (!ExtensionManager.getInstance().getContactAccountExtension().buildValuesForSim(
                                mAccountDst.type, mService.getApplicationContext(), values, numberArray,
                                phoneTypeArray, maxAnrCount, dstSlotId, anrsList, ExtensionManager.COMMD_FOR_AAS)) {
                            /** M: @ } */
                            /**
                             * M: Bug Fix for ALPS00557517
                             * origin code:
                             * Log.d(LOG_TAG, "additional number array is empty and fill it with ADN number");
                             * simAnrNum = numberArray.remove(0);
                             * simAnrNum = TextUtils.isEmpty(simAnrNum) ? "" : simAnrNum.replace("-", "");
                             * values.put("anr", PhoneNumberUtils.stripSeparators(simAnrNum));
                             * Log.d(LOG_TAG, "copyContactsToSim anr is " + simAnrNum);
                             * @ { */
                            int loop = numberArray.size() < usimMaxAnrCount ? numberArray.size() : usimMaxAnrCount;
                            for (int k = 0; k < loop; k++) {
                                simAnrNum = numberArray.remove(0);
                                simAnrNum = TextUtils.isEmpty(simAnrNum) ? "" : simAnrNum.replace("-", "");
                                values.put("anr", PhoneNumberUtils.stripSeparators(simAnrNum));
                            }
                            /** @ } */
                        }
                    }

                    if (!emailArray.isEmpty()) {
                        simEmail = emailArray.remove(0);
                        simEmail = TextUtils.isEmpty(simEmail) ? "" : simEmail;
                        values.put("emails", simEmail);
                        Log.d(LOG_TAG, "copyContactsToSim emails is " + simEmail);
                    }

                    /** M:SNE @ { */
                    simNickname = ExtensionManager.getInstance().getContactListExtension().buildSimNickname(
                            mAccountDst.type, values, nicknameArray, dstSlotId, simNickname, ExtensionManager.COMMD_FOR_SNE);
                    /** M: @ } */
                }

                if (!isSimReady(dstSlotId) || !isPhoneBookReady(dstSlotId)) {
                    break;
                }
                Log.i(LOG_TAG, "Before insert Sim card. values=" + values);
                Uri retUri = resolver.insert(dstSimUri, values);
                Log.i(LOG_TAG, "After insert Sim card.");

                Log.i(LOG_TAG, "retUri is " + retUri);
                if (retUri != null) {
                    List<String> checkUriPathSegs = retUri.getPathSegments();
                    if ("error".equals(checkUriPathSegs.get(0))) {
                        String errorCode = checkUriPathSegs.get(1);
                        Log.i(LOG_TAG, "error code = " + errorCode);
                        if (DEBUG) {
                            printSimErrorDetails(errorCode);
                        }
                        if (errorCause != ErrorCause.ERROR_USIM_EMAIL_LOST) {
                            errorCause = ErrorCause.ERROR_UNKNOWN;
                        }
                        if ("-3".equals(checkUriPathSegs.get(1))) {
                            errorCause = ErrorCause.SIM_STORAGE_FULL;
                            isSimStorageFull = true;
                            Log.e(LOG_TAG, "Fail to insert sim contacts fail"
                                    + " because sim storage is full.");
                            break;
                        } else if ("-12".equals(checkUriPathSegs.get(1))) {
                            errorCause = ErrorCause.ERROR_USIM_EMAIL_LOST;
                            Log.e(LOG_TAG, "Fail to save USIM email "
                                    + " because emial slot is full in USIM.");
                            Log.d(LOG_TAG, "Ignore this error and "
                                    + "remove the email address to save this item again");
                            values.remove("emails");
                            retUri = resolver.insert(dstSimUri, values);
                            Log.d(LOG_TAG, "[Save Again]The retUri is " + retUri);
                            if (retUri != null && ("error".equals(retUri.getPathSegments().get(0)))) {
                                if ("-3".equals(retUri.getPathSegments().get(1))) {
                                    errorCause = ErrorCause.SIM_STORAGE_FULL;
                                    isSimStorageFull = true;
                                    Log.e(LOG_TAG, "Fail to insert sim contacts fail"
                                            + " because sim storage is full.");
                                    break;
                                }
                            }
                            if (retUri != null && !("error".equals(retUri.getPathSegments().get(0)))) {
                                long indexInSim = ContentUris.parseId(retUri);
                                /** M:AAS&SNE @ { original code:
                                 * SubContactsUtils.buildInsertOperation(operationList, mAccountDst,
                                 * simTag, simNum, null, simAnrNum, resolver, dstSimId, dstSimType,
                                 * indexInSim, null);
                                 */
                                if (!SimUtils.buildInsertOperation(operationList, mAccountDst, simTag, simNum, null,
                                        simAnrNum, resolver, dstSimId, dstSimType, indexInSim, null, anrsList,
                                        simNickname)) {
                                    /** M: @ } */
                                    SubContactsUtils.buildInsertOperation(operationList, mAccountDst, simTag, simNum,
                                            null, simAnrNum, resolver, dstSimId, dstSimType, indexInSim, null);
                                }
                                subContact ++;
                            }
                        }
                    } else {
                        Log.d(LOG_TAG, "insertUsimFlag = true");
                        long indexInSim = ContentUris.parseId(retUri);

                        /** M:AAS&SNE @ { original code:
                         * SubContactsUtils.buildInsertOperation(operationList, mAccountDst, simTag,
                         * simNum, simEmail, simAnrNum, resolver, dstSimId, dstSimType, indexInSim,
                         * null);
                         */
                        if (!SimUtils.buildInsertOperation(operationList, mAccountDst, simTag, simNum, simEmail,
                                simAnrNum, resolver, dstSimId, dstSimType, indexInSim, null, anrsList, simNickname)) {
                            /** M: @ } */
                            SubContactsUtils.buildInsertOperation(operationList, mAccountDst, simTag, simNum, simEmail,
                                    simAnrNum, resolver, dstSimId, dstSimType, indexInSim, null);
                        }
                        subContact ++;
                        //successfulItems++;
                    }
                } else {
                    errorCause = ErrorCause.ERROR_UNKNOWN;
                }
                if (operationList.size() > MAX_OP_COUNT_IN_ONE_BATCH) {
                    try {
                        Log.i(LOG_TAG, "Before applyBatch. ");
                        if (isSimReady(dstSlotId) && isPhoneBookReady(dstSlotId)) {
                            resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                        }
                        Log.i(LOG_TAG, "After applyBatch ");
                    } catch (android.os.RemoteException e) {
                        Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    } catch (android.content.OperationApplicationException e) {
                        Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    }
                    operationList.clear();
                }
            }// inner looper
            if (subContact > 0) {
                successfulItems++;
            }
            if (isSimStorageFull) {
                break;
            }
        }
        
        if (operationList.size() > 0) {
            try {
                Log.i(LOG_TAG, "Before end applyBatch. ");
                if (isSimReady(dstSlotId) && isPhoneBookReady(dstSlotId)) {
                    resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                }
                Log.i(LOG_TAG, "After end applyBatch ");
            } catch (android.os.RemoteException e) {
                Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            } catch (android.content.OperationApplicationException e) {
                Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
            operationList.clear();
        }
        
        if (mCanceled) {
            Log.d(LOG_TAG, "copyContactsToSim run: mCanceled = true");
            errorCause = ErrorCause.USER_CANCEL;
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceled(MultiChoiceService.TYPE_COPY, mJobId, totalItems,
                    successfulItems, totalItems - successfulItems);
            return;
        }
        
        mService.handleFinishNotification(mJobId, errorCause == ErrorCause.NO_ERROR);
        if (errorCause == ErrorCause.NO_ERROR) {
            mListener.onFinished(MultiChoiceService.TYPE_COPY, mJobId, totalItems);
        } else {
            mListener.onFailed(MultiChoiceService.TYPE_COPY, mJobId, totalItems,
                    successfulItems, totalItems - successfulItems, errorCause);
        }
    }

    private boolean isSimReady(int slotId) {
        boolean simPUKReq = SimCardUtils.isSimPukRequest(slotId);
        boolean simPINReq = SimCardUtils.isSimPinRequest(slotId);
        boolean simInserted = SimCardUtils.isSimInserted(slotId);
        boolean isRadioOn = SimCardUtils.isSetRadioOn(mResolver, slotId);
        boolean isFdnEnabled = SimCardUtils.isFdnEnabed(slotId);
        boolean isSimInfoReady = SimCardUtils.isSimInfoReady();
        Log.d(LOG_TAG, "[checkSimState]slotId:" + slotId + "||simPUKReq: "
                + simPUKReq + "||simPINReq: " + simPINReq + "||isRadioOn: "
                + isRadioOn + "||isFdnEnabled: " + isFdnEnabled
                + "||simInserted: " + simInserted + "||isSimInfoReady:" + isSimInfoReady);

        return !(simPUKReq || !isRadioOn || isFdnEnabled || simPINReq || !simInserted || !isSimInfoReady);
    }

    private boolean isPhoneBookReady(int slot) {
        Log.i(LOG_TAG, "isPhoneBookReady " + SimCardUtils.isPhoneBookReady(slot));
        return SimCardUtils.isPhoneBookReady(slot);
    }

    private void copyContactsToAccount() {
        Log.d(LOG_TAG, "copyContactsToAccount");
        if (mCanceled) {
            return;
        }
        int successfulItems = 0;
        int currentCount = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (MultiChoiceRequest request : this.mRequests) {
            sb.append(String.valueOf(request.mContactId));
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        Log.d(LOG_TAG, "copyContactsToAccount contactIds " + sb.toString() + " ");
        Cursor rawContactsCursor = mResolver.query(
                RawContacts.CONTENT_URI, 
                new String[] {RawContacts._ID, RawContacts.DISPLAY_NAME_PRIMARY}, 
                RawContacts.CONTACT_ID + " IN " + sb.toString(), 
                null, null);
        
        int totalItems = rawContactsCursor == null ? 0 : rawContactsCursor.getCount();

        final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        // Process request one by one
        if (rawContactsCursor != null) {
            Log.d(LOG_TAG, "copyContactsToAccount: rawContactsCursor.size = " + rawContactsCursor.getCount());

            long nOldRawContactId;
            while (rawContactsCursor.moveToNext()) {
                if (mCanceled) {
                    Log.d(LOG_TAG, "runInternal run: mCanceled = true");
                    break;
                }
                currentCount++;
                String displayName = rawContactsCursor.getString(1);

                mListener.onProcessed(MultiChoiceService.TYPE_COPY, mJobId,
                        currentCount, totalItems, displayName);

                nOldRawContactId = rawContactsCursor.getLong(0);

                Cursor dataCursor = mResolver.query(Data.CONTENT_URI, 
                        DATA_ALLCOLUMNS, Data.RAW_CONTACT_ID + "=? ", 
                        new String[] { String.valueOf(nOldRawContactId) }, null);
                if (dataCursor == null) {
                    continue;
                } else if (dataCursor.getCount() <= 0) {
                    Log.d(LOG_TAG, "dataCursor is empty");
                    dataCursor.close();
                    continue;
                }
                
                int backRef = operationList.size();
                ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(RawContacts.CONTENT_URI);
                if (!TextUtils.isEmpty(mAccountDst.name) && !TextUtils.isEmpty(mAccountDst.type)) {
                    builder.withValue(RawContacts.ACCOUNT_NAME, mAccountDst.name);
                    builder.withValue(RawContacts.ACCOUNT_TYPE, mAccountDst.type);
                } else {
                    builder.withValues(new ContentValues());
                }
                builder.withValue(RawContacts.AGGREGATION_MODE,
                        RawContacts.AGGREGATION_MODE_DISABLED);
                operationList.add(builder.build());
                
                dataCursor.moveToPosition(-1);
                String[] columnNames = dataCursor.getColumnNames();
                while (dataCursor.moveToNext()) {
                    //do not copy group data between different account.
                    String mimeType = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
                    Log.i(LOG_TAG, "mimeType:" + mimeType);
                    if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        continue;
                    }
                    builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                    /** M:AAS @ { */
                    final int slotId = ((AccountWithDataSetEx) mAccountDst).getSlotId();
                    generateDataBuilder(dataCursor, builder, columnNames, mimeType, slotId,
                            mAccountSrc.type);
                    /** M: @ } */
                    builder.withValueBackReference(Data.RAW_CONTACT_ID, backRef);
                    operationList.add(builder.build());
                }
                dataCursor.close();
                successfulItems++;
                if (operationList.size() > MAX_OP_COUNT_IN_ONE_BATCH) {
                    try {
                        Log.i(LOG_TAG, "Before applyBatch. ");
                        mResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                        Log.i(LOG_TAG, "After applyBatch ");
                    } catch (android.os.RemoteException e) {
                        Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    } catch (android.content.OperationApplicationException e) {
                        Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    }
                    operationList.clear();
                }
            }
            rawContactsCursor.close();
            if (operationList.size() > 0) {
                try {
                    Log.i(LOG_TAG, "Before end applyBatch. ");
                    mResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                    Log.i(LOG_TAG, "After end applyBatch ");
                } catch (android.os.RemoteException e) {
                    Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                } catch (android.content.OperationApplicationException e) {
                    Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                }
                operationList.clear();
            }
            if (mCanceled) {
                Log.d(LOG_TAG, "runInternal run: mCanceled = true");
                mService.handleFinishNotification(mJobId, false);
                mListener.onCanceled(MultiChoiceService.TYPE_COPY, mJobId, totalItems,
                        successfulItems, totalItems - successfulItems);
                if (rawContactsCursor != null && !rawContactsCursor.isClosed()) {
                    rawContactsCursor.close();
                }
                return;
            }
        }

        mService.handleFinishNotification(mJobId, successfulItems == totalItems);
        if (successfulItems == totalItems) {
            mListener.onFinished(MultiChoiceService.TYPE_COPY, mJobId, totalItems);
        } else {
            mListener.onFailed(MultiChoiceService.TYPE_COPY, mJobId, totalItems,
                    successfulItems, totalItems - successfulItems);
        }

        Log.d(LOG_TAG, "copyContactsToAccount: end");
    }

    private void cursorColumnToBuilder(Cursor cursor, String[] columnNames, int index,
            ContentProviderOperation.Builder builder) {
        switch (cursor.getType(index)) {
            case Cursor.FIELD_TYPE_NULL:
                // don't put anything in the content values
                break;
            case Cursor.FIELD_TYPE_INTEGER:
                builder.withValue(columnNames[index], cursor.getLong(index));
                break;
            case Cursor.FIELD_TYPE_STRING:
                builder.withValue(columnNames[index], cursor.getString(index));
                break;
            case Cursor.FIELD_TYPE_BLOB:    
                builder.withValue(columnNames[index], cursor.getBlob(index));
                break;
            default:
                throw new IllegalStateException("Invalid or unhandled data type");
        }
    }
    
    private void printSimErrorDetails(String errorCode) {
        int iccError = Integer.valueOf(errorCode);
        switch (iccError) {
            case ErrorCause.SIM_NUMBER_TOO_LONG:
                Log.d(LOG_TAG, "ERROR PHONE NUMBER TOO LONG");
                break;
            case ErrorCause.SIM_NAME_TOO_LONG:
                Log.d(LOG_TAG, "ERROR NAME TOO LONG");
                break;
            case ErrorCause.SIM_STORAGE_FULL:
                Log.d(LOG_TAG, "ERROR STORAGE FULL");
                break;
            case ErrorCause.SIM_ICC_NOT_READY:
                Log.d(LOG_TAG, "ERROR ICC NOT READY");
                break;
            case ErrorCause.SIM_PASSWORD_ERROR:
                Log.d(LOG_TAG, "ERROR ICC PASSWORD ERROR");
                break;
            case ErrorCause.SIM_ANR_TOO_LONG:
                Log.d(LOG_TAG, "ERROR ICC ANR TOO LONG");
                break;
            case ErrorCause.SIM_GENERIC_FAILURE:
                Log.d(LOG_TAG, "ERROR ICC GENERIC FAILURE");
                break;
            case ErrorCause.SIM_ADN_LIST_NOT_EXIT:
                Log.d(LOG_TAG, "ERROR ICC ADN LIST NOT EXIST");
                break;
            case ErrorCause.ERROR_USIM_EMAIL_LOST:
                Log.d(LOG_TAG, "ERROR ICC USIM EMAIL LOST");
                break;
            default:
                Log.d(LOG_TAG, "ERROR ICC UNKNOW");
                break;
        }
    }

    private void copyContactsToSimWithRadioStateCheck() {
        if (mCanceled) {
            return;
        }

        int errorCause = ErrorCause.NO_ERROR;

        AccountWithDataSetEx account = (AccountWithDataSetEx) mAccountDst;
        Log.d(LOG_TAG, "[copyContactsToSimWithRadioCheck]AccountName: " + account.name
                + " | accountType: " + account.type);
        int dstSlotId = account.getSlotId();
        if (!isSimReady(dstSlotId)) {
            errorCause = ErrorCause.SIM_NOT_READY;
            mService.handleFinishNotification(mJobId, false);
            mListener.onFailed(MultiChoiceService.TYPE_COPY, mJobId, mRequests.size(),
                    0, mRequests.size(), errorCause);
            return;
        }

        if (!isPhoneBookReady(dstSlotId)) {
            int i = 0;
            while (i++ < RETRYCOUNT) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (isPhoneBookReady(dstSlotId)) {
                    break;
                }
            }
        }
        if (!isPhoneBookReady(dstSlotId)) {
            errorCause = ErrorCause.SIM_NOT_READY;
            mService.handleFinishNotification(mJobId, false);
            mListener.onFailed(MultiChoiceService.TYPE_COPY, mJobId, mRequests.size(),
                    0, mRequests.size(), errorCause);
            return;
        }
        copyContactsToSim();
    }

    /** M:AAS @ { */
    private void generateDataBuilder(Cursor dataCursor, Builder builder, String[] columnNames,
            String mimeType, int slotId, String srcAccountType) {
        for (int i = 1; i < columnNames.length; i++) {
            if (ExtensionManager.getInstance().getContactListExtension().generateDataBuilder(
                    mService.getApplicationContext(), dataCursor, builder, columnNames,
                    srcAccountType, mimeType, slotId, i, ExtensionManager.COMMD_FOR_AAS)) {
                continue;
                /** M: @ } */
            }
            cursorColumnToBuilder(dataCursor, columnNames, i, builder);
        }
    }

}
