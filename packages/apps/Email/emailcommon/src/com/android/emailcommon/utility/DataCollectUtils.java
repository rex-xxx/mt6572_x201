package com.android.emailcommon.utility;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.SmartPush;
import com.android.emailcommon.provider.EmailContent.Message;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * M: An untility class for collecting the user's habit of using Email
 */
public class DataCollectUtils {
    // The account list being recorded
    private static final ArrayList<Long> sAccountIds = new ArrayList<Long>();
    // The start time of current recording
    private static long sStartTime;
    // We will take down any EAS account viewed by the user during a Email using session.
    // Use this variable to store all the recorded account for avoiding record duplicated.
    private static final ArrayList<Long> sRecordedAccountIds = new ArrayList<Long>();

    /**
     * Start record an account using duration
     * @param context
     * @param accountId the account's id
     * @param recordOpening if true, record that the user used this account once.
     */
    public static void startRecord(final Context context, final long accountId, final boolean recordOpening) {
        EmailAsyncTask.runAsyncSerial(new Runnable() {
            @Override
            public void run() {
                sAccountIds.clear();

                // Add the account(s) to the recording account
                // list if it were an EAS account
                if (accountId == Account.ACCOUNT_ID_COMBINED_VIEW) {
                    getAllEasAccounts(context);
                } else if (accountId != Account.NO_ACCOUNT) {
                    addIfEasAccount(context, accountId);
                }

                sStartTime = System.currentTimeMillis();

                for (Long acctId : sAccountIds) {
                    // Just record the opening event for the account which had not
                    // been recorded during this session
                    if (recordOpening && !sRecordedAccountIds.contains(acctId)) {
                        SmartPush sp = SmartPush.addEvent(context, sStartTime, acctId, SmartPush.TYPE_OPEN, 1);
                        sp.save(context);
                        sRecordedAccountIds.add(acctId);
                    }
                }
            }
        });
    }

    /**
     * Clear the recording account list,
     */
    public static void clearRecordedList() {
        sRecordedAccountIds.clear();
    }

    /**
     * Stop the recording of the duration for current account,
     * add the duration event to the database
     * @param context
     */
    public static void stopRecord(final Context context) {
        EmailAsyncTask.runAsyncSerial(new Runnable() {
            @Override
            public void run() {
                long duration = System.currentTimeMillis() - sStartTime;

                if (duration > 0) {
                    for (Long acctId : sAccountIds) {
                        SmartPush sp = SmartPush.addEvent(context, sStartTime, acctId, SmartPush.TYPE_DURATION,
                                duration);
                        sp.save(context);
                    }
                }
            }
        });
    }

    /**
     * Record the new-coming email one by one.
     * @param context
     * @param msgs the new-coming emails
     */
    public static void recordNewMails(final Context context, final ArrayList<Message> msgs) {
        EmailAsyncTask.runAsyncParallel(new Runnable() {
            @Override
            public void run() {
                for (Message msg : msgs) {
                    SmartPush sp = SmartPush.addEvent(context, msg.mTimeStamp, msg.mAccountKey, SmartPush.TYPE_MAIL, 1);
                    sp.save(context);
                }
            }
        });
    }

    /**
     * Add the account to the recording list if it were an EAS account
     * @param context
     * @param accountId
     */
    private static void addIfEasAccount(Context context, long accountId) {
        Account acct = Account.restoreAccountWithId(context, accountId);
        if (acct != null && acct.isEasAccount(context)) {
            sAccountIds.add(accountId);
        }
    }

    /**
     * Just for combined account, add its sub EAS account to the recording list
     * @param context
     */
    private static void getAllEasAccounts(Context context) {
        Cursor c = context.getContentResolver().query(Account.CONTENT_URI,
                new String[] {Account.RECORD_ID}, null, null, null);
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    addIfEasAccount(context, c.getLong(0));
                }
            } finally {
                c.close();
            }
        }
    }
}
