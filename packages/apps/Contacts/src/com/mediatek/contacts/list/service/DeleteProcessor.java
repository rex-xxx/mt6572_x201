
package com.mediatek.contacts.list.service;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.vcard.ProcessorBase;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.list.ContactsMultiDeletionFragment;
import com.mediatek.contacts.simcontact.AbstractStartSIMService;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DeleteProcessor extends ProcessorBase {

    private static final String LOG_TAG = ContactsMultiDeletionFragment.TAG;
    private static final boolean DEBUG = ContactsMultiDeletionFragment.DEBUG;

    private final MultiChoiceService mService;
    private final ContentResolver mResolver;
    private final List<MultiChoiceRequest> mRequests;
    private final int mJobId;
    private final MultiChoiceHandlerListener mListener;

    private PowerManager.WakeLock mWakeLock;

    private volatile boolean mCanceled;
    private volatile boolean mDone;
    private volatile boolean mIsRunning;

    private static final int MAX_OP_COUNT_IN_ONE_BATCH = 100;

    // change max count and max count in one batch for special operator 
    private static final int MAX_COUNT = 1551;
    private static final int MAX_COUNT_IN_ONE_BATCH = 50;

    public DeleteProcessor(final MultiChoiceService service,
            final MultiChoiceHandlerListener listener, final List<MultiChoiceRequest> requests,
            final int jobId) {
        mService = service;
        mResolver = mService.getContentResolver();
        mListener = listener;

        mRequests = requests;
        mJobId = jobId;

        final PowerManager powerManager = (PowerManager) mService.getApplicationContext()
                .getSystemService("power");
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (DEBUG) {
            Log.d(LOG_TAG, "DeleteProcessor received cancel request");
        }
        if (mDone || mCanceled) {
            return false;
        }
        Log.i(LOG_TAG, "[cancel]!mIsRunning : " + !mIsRunning);
        
        mCanceled = true;
        if (!mIsRunning) {
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceled(MultiChoiceService.TYPE_DELETE, mJobId, -1, -1, -1);
        } else {
            /*
             * Bug Fix by Mediatek Begin.
             *   Original Android's code:
             *     xxx
             *   CR ID: ALPS00249590
             *   Descriptions: 
             */
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceling(MultiChoiceService.TYPE_DELETE, mJobId);
            /*
             * Bug Fix by Mediatek End.
             */
        }

        return true;
    }

    @Override
    public int getType() {
        return MultiChoiceService.TYPE_DELETE;
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
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            registerReceiver();
            runInternal();
            unregisterReceiver();
        } finally {
            synchronized (this) {
                mDone = true;
            }
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    private void runInternal() {
        if (isCancelled()) {
            Log.i(LOG_TAG, "Canceled before actually handling");
            return;
        }

        boolean succeessful = true;
        int totalItems = mRequests.size();
        int successfulItems = 0;
        int currentCount = 0;
        int iBatchDel = MAX_OP_COUNT_IN_ONE_BATCH;
        if (totalItems > MAX_COUNT) {
            iBatchDel = MAX_COUNT_IN_ONE_BATCH;
            Log.i(LOG_TAG, "iBatchDel = " + iBatchDel);
        }
        long startTime = System.currentTimeMillis();
        final ArrayList<Long> contactIdsList = new ArrayList<Long>();
        int times = 0;
        /** M: Add idToSlotHashMap to save old request indicator. CR: 568004 @{ */
        HashMap<Long, Integer> idToSlotHashMap = new HashMap<Long, Integer>();
        int slot = -1;
        /** @} */
        boolean simServiceStarted = false;

        for (MultiChoiceRequest request : mRequests) {
            if (mCanceled) {
                Log.d(LOG_TAG, "runInternal run: mCanceled = true, break looper");
                break;
            }
            currentCount++;

            mListener.onProcessed(MultiChoiceService.TYPE_DELETE, mJobId, currentCount, totalItems,
                    request.mContactName);
            // delete contacts from sim card
            if (request.mIndicator > RawContacts.INDICATE_PHONE) {
                /** M: Just reset slot value when indicator gets changed. { */
                if (!idToSlotHashMap.containsKey(Long.valueOf(request.mIndicator))) {
                    slot = SIMInfo.getSlotById(mService.getApplicationContext(), request.mIndicator);
                    idToSlotHashMap.put(Long.valueOf(request.mIndicator), Integer.valueOf(slot));
                    Log.d(LOG_TAG, "Indicator: " + request.mIndicator + "    slot: " + slot);
                } else {
                    slot = idToSlotHashMap.get(Long.valueOf(request.mIndicator)).intValue();
                }
                /** @} */
                if (mReveiced3GSwitch || !isReadyForDelete(slot)) {
                    Log.d(LOG_TAG, "runInternal run: isReadyForDelete(" + slot + ") = false");
                    succeessful = false;
                    continue;
                }

                if (simServiceStarted || !simServiceStarted && AbstractStartSIMService.isServiceRunning(slot)) {
                    Log.d(LOG_TAG, "runInternal run: sim service is running, we should skip all of sim contacts");
                    simServiceStarted = true;
                    succeessful = false;
                    continue;
                }

                Uri delSimUri = SimCardUtils.SimUri.getSimUri(slot);
                String where = ("index = " + request.mSimIndex);

                if (mResolver.delete(delSimUri, where, null) <= 0) {
                    Log.d(LOG_TAG, "runInternal run: delete the sim contact failed");
                    succeessful = false;
                } else {
                    successfulItems++;
                    contactIdsList.add(Long.valueOf(request.mContactId));
                }
            } else {
                successfulItems++;
                contactIdsList.add(Long.valueOf(request.mContactId));
            }

            // delete contacts from database
            if (contactIdsList.size() >= iBatchDel) {
                actualBatchDelete(contactIdsList);
                Log.i(LOG_TAG, "the " + (++times) + " times iBatchDel = " + iBatchDel);
                contactIdsList.clear();
                if ((totalItems - currentCount) <= MAX_COUNT) {
                    iBatchDel = MAX_OP_COUNT_IN_ONE_BATCH;
                }
            }
        }

        if (contactIdsList.size() > 0) {
            actualBatchDelete(contactIdsList);
            contactIdsList.clear();
        }

        Log.i(LOG_TAG, "totaltime: " + (System.currentTimeMillis() - startTime));

        if (mCanceled) {
            Log.d(LOG_TAG, "runInternal run: mCanceled = true, return");
            succeessful = false;
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceled(MultiChoiceService.TYPE_DELETE, mJobId, totalItems,
                    successfulItems, totalItems - successfulItems);
            return;
        }

        mService.handleFinishNotification(mJobId, succeessful);
        if (succeessful) {
            mListener.onFinished(MultiChoiceService.TYPE_DELETE, mJobId, totalItems);
        } else {
            mListener.onFailed(MultiChoiceService.TYPE_DELETE, mJobId, totalItems,
                    successfulItems, totalItems - successfulItems);
        }
    }

    private int actualBatchDelete(ArrayList<Long> contactIdList) {
        Log.d(LOG_TAG, "actualBatchDelete");
        if (contactIdList == null || contactIdList.size() == 0) {
            return 0;
        }
        
        final StringBuilder whereBuilder = new StringBuilder();
        final ArrayList<String> whereArgs = new ArrayList<String>();
        final String[] questionMarks = new String[contactIdList.size()];
        for (long contactId : contactIdList) {
            whereArgs.add(String.valueOf(contactId));
        }
        Arrays.fill(questionMarks, "?");
        whereBuilder.append(Contacts._ID + " IN (").
                append(TextUtils.join(",", questionMarks)).
                append(")");

        int deleteCount = mResolver.delete(Contacts.CONTENT_URI.buildUpon().appendQueryParameter(
                "batch", "true").build(), whereBuilder.toString(), whereArgs.toArray(new String[0]));
        Log.d(LOG_TAG, "actualBatchDelete " + deleteCount + " Contacts");
        return deleteCount;
    }

//    private void ActualBatchDelete(ArrayList<Long> contactIdList) {
//        Log.d(LOG_TAG, "ActualBatchDelete");
//        if (contactIdList == null || contactIdList.size() == 0) {
//            return;
//        }
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("(");
//        for (Long id : contactIdList) {
//            sb.append(String.valueOf(id));
//            sb.append(",");
//        }
//        sb.deleteCharAt(sb.length() - 1);
//        sb.append(")");
//        Log.d(LOG_TAG, "ActualBatchDelete ContactsIds " + sb.toString() + " ");
//
//        final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
//
//        Cursor cursor = mResolver.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID},
//                RawContacts.CONTACT_ID + " IN " + sb.toString() + " AND " + RawContacts.DELETED + "=0", 
//                null, null);
//        if (cursor != null) {
//            cursor.moveToPosition(-1);
//            while (cursor.moveToNext()) {
//                Log.d(LOG_TAG, "ActualBatchDelete rawContactsId is " + cursor.getLong(0));
//                Uri delDbUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, cursor
//                        .getLong(0));
//                delDbUri = addCallerIsSyncAdapterParameter(delDbUri);
//                ContentProviderOperation.Builder builder = ContentProviderOperation
//                        .newDelete(delDbUri);
//                operationList.add(builder.build());
//                if (operationList.size() > MAX_OP_COUNT_IN_ONE_BATCH) {
//                    try {
//                        Log.i(LOG_TAG, "Before applyBatch in while. ");
//                        mResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
//                        Thread.sleep(500);
//                        Log.i(LOG_TAG, "After applyBatch in while. ");
//                    } catch (android.os.RemoteException e) {
//                        Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
//                    } catch (android.content.OperationApplicationException e) {
//                        Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
//                    } catch (java.lang.InterruptedException e) {
//                        Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
//                    }
//                    operationList.clear();
//                }
//            }
//            if (operationList.size() > 0) {
//                try {
//                    Log.i(LOG_TAG, "Before applyBatch. ");
//                    mResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
//                    Thread.sleep(500);
//                    Log.i(LOG_TAG, "After applyBatch ");
//                } catch (android.os.RemoteException e) {
//                    Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
//                } catch (android.content.OperationApplicationException e) {
//                    Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
//                } catch (java.lang.InterruptedException e) {
//                    Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
//                }
//                operationList.clear();
//            }
//            cursor.close();
//        }
//    }

    /**
     * -1 -- for single SIM
     * 0  -- for gemini SIM 1
     * 1  -- for gemini SIM 2
     */
    private boolean isReadyForDelete(int slotId) {
        return SimCardUtils.isSimStateIdle(slotId);
    }
    
    // In some case, we want to break the delete process, for example, radio off.
    /*private boolean needBreakProcess(int slotId) {
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        if (null == iTel) {
            return false;
        }
        try {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
             // For Gemini load
                return !iTel.isRadioOnGemini(slotId);
            } else { // Single SIM
                return !iTel.isRadioOn();
            }
        } catch (RemoteException e) {
            Log.d(LOG_TAG, "needBreakProcess: RemoteException " + e);
            return false;
        }
    }*/
    
        
    private void registerReceiver() {
        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(GeminiPhone.EVENT_PRE_3G_SWITCH);
            mService.getApplicationContext().registerReceiver(mModemSwitchListener, intentFilter);
        }
    }
    
    private void unregisterReceiver() {
        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            mService.getApplicationContext().unregisterReceiver(mModemSwitchListener);
        }
    }
    
    private Boolean mReveiced3GSwitch = false;
    
    private BroadcastReceiver mModemSwitchListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GeminiPhone.EVENT_PRE_3G_SWITCH)) {
                Log.i(LOG_TAG, "receive 3G Switch ...");
                mReveiced3GSwitch = true;
            }
        }
    };

/*    private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
                String.valueOf(true)).build();
    }*/
}
