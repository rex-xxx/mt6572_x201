package com.mediatek.exchange.smartpush;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;

import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.provider.SmartPush;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.EmailContent.HostAuthColumns;
import com.android.emailcommon.service.EmailServiceProxy;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.Utility;
import com.android.exchange.AbstractSyncService;
import com.android.exchange.ExchangeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * SmartPushService handles all aspects of Smart Push, such as
 * regularly wipe stale habit data, calculate sync interval from
 * the habit data for each account, change the sync interval on
 * schedule for each account, etc. This service have to cooperate
 * with ExchangeService to some extent(for example, it relies on
 * ExchangeService's AccountObserver)
 */
public class SmartPushService extends Service implements Runnable{
    private static final String TAG = "SmartPushService";

    private static final String WHERE_PROTOCOL_EAS = HostAuthColumns.PROTOCOL + "=\"" +
    AbstractSyncService.EAS_PROTOCOL + "\"";

    // Sync frequency
    public static final int SYNC_FREQUENCY_HIGH = 2;
    public static final int SYNC_FREQUENCY_MEDIUM = 1;
    public static final int SYNC_FREQUENCY_LOW = 0;

    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;
    private static final int WEEK = 7 * DAY;

    // The singleton SmartPushService object, with its thread
    protected static SmartPushService INSTANCE;
    private static Thread sServiceThread = null;

    private WakeLock mWakeLock = null;
    private PendingIntent mPendingIntent = null;

    // The millseconds of today's start time (GMT)
    private static long sTodayStartTime;
    // Keeps track of the calculatable accounts and the days of their habit data
    private HashMap<Long, Integer> mAccountMap = new HashMap<Long, Integer>();

    private static volatile boolean sStartingUp = false;
    private static volatile boolean sStop = false;

    // We synchronize on this for all actions affecting the service and error maps
    private static final Object sSyncLock = new Object();

    // Whether we have an unsatisfied "kick" pending
    private boolean mKicked = false;


    private static void startSmartPushService(Context context) {
        context.startService(new Intent(context, SmartPushService.class));
    }

    @Override
    public void run() {
        sStop = false;

        try {
            while(!sStop) {
                Logging.v(TAG, "SmartPushService loop one time");
                runAwake();
                // Delete the habit data older than 2 week
                deleteStaleData();
                // check is there any history data and exchange account to do smart push
                long nextCheckTime = shouldRunSmartPushService();
                if (nextCheckTime > 0) {
                    Logging.v(TAG, "No eligible smart push account found");
                    // Wait if no eligible smart push account found
                    runAsleep(nextCheckTime + (10 * SECOND));
                    try {
                        synchronized (this) {
                            // We expect the habit data is enough after this time
                            wait(nextCheckTime + (5 * SECOND));
                        }
                    } catch (InterruptedException e) {
                        // Needs to be caught, but causes no problem
                        Logging.v(TAG, "SmartPushService interrupted");
                    }
                    continue;
                }
                // check the next calculate wait time
                long nextCalculateWait = checkNextCalculateWait();
                if (nextCalculateWait < 10 * MINUTE) {
                    calculate();
                    nextCalculateWait = DAY;
                }
                // check the next action wait time that to change the sync frequency
                long nextActionWait = makeAdjustments();
                long nextWait = nextActionWait < nextCalculateWait ?
                        nextActionWait : nextCalculateWait;
                try {
                    synchronized (this) {
                        if (!mKicked) {
                            if (nextWait < 0) {
                                nextWait = 1 * SECOND;
                            }
                            if (nextWait > 10 * SECOND) {
                                runAsleep(nextWait + (3 * SECOND));
                            }
                            wait(nextWait);
                        }
                    }
                } catch (InterruptedException e) {
                    // Needs to be caught, but causes no problem
                    Logging.w(TAG, "SmartPushService interrupted");
                } finally {
                    synchronized (this) {
                        if (mKicked) {
                            Logging.v(TAG, "Wait deferred due to kick");
                            mKicked = false;
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
           // Crash; this is a completely unexpected runtime error
            Logging.e(TAG, "RuntimeException in SmartPushService", e);
            throw e;
        } catch (Exception e) {
            Logging.e(TAG, "SmartPushService Exception occured", e);
            startService(new Intent(this, SmartPushService.class));
        } finally {
            shutdown();
        }
    }

    /**
     *  Check is there any history data and exchange account to do smart push
     * @return the next check time
     */
    private long shouldRunSmartPushService() {
        long current = System.currentTimeMillis();
        // "MOD" operation can exclude today's time
        long days = current / DAY;
        // Get the start time of today (GMT)
        sTodayStartTime = days * DAY;
        Logging.v(TAG, "Today start time: " + sTodayStartTime);
        mAccountMap.clear();
        long nextCheckTime = 1 * DAY;

        Cursor c = getContentResolver().query(Account.CONTENT_URI, new String[]{AccountColumns.ID},
                AccountColumns.SYNC_INTERVAL + " =?",
                new String[]{String.valueOf(Account.CHECK_INTERVAL_SMART_PUSH)}, null);
        if (c != null) {
            try {
                while(c.moveToNext()) {
                    long accountId = c.getLong(0);
                    // Get the earliest hait data time stamp of the account
                    Long recordTimestamp = Utility.getFirstRowLong(this, SmartPush.CONTENT_URI,
                            new String[]{SmartPush.TIMESTAMP},
                            SmartPush.ACCOUNT_KEY + "=? AND " + SmartPush.EVENT_TYPE + " !=?",
                            new String[]{String.valueOf(accountId), String.valueOf(SmartPush.TYPE_MAIL)}, null, 0);
                    if (recordTimestamp != null) {
                        long timeSpan = sTodayStartTime - recordTimestamp;
                        Logging.v(TAG, "account " + accountId + " has " + timeSpan + "ms habit data");
                        // Only calculate for the account which habit data was recorded over 2 days
                        if (timeSpan >= 2 * DAY) {
                            long day = timeSpan / DAY;
                            Logging.v(TAG, "account " + accountId + " has " + day + " days habit data");
                            mAccountMap.put(accountId, Long.valueOf(day).intValue());
                            nextCheckTime = 0;
                        } else {
                            long timeToEnough = sTodayStartTime + DAY * (2 -
                                    (timeSpan < 0 ? -1 : timeSpan / DAY)) - current;
                            nextCheckTime = Math.min(nextCheckTime, timeToEnough);
                        }
                    } else {
                        Logging.v(TAG, "No habit data record for account " + accountId);
                    }
                }
            } finally {
                c.close();
            }
        }

        Logging.v(TAG, "The habit data will be enough after " + nextCheckTime);
        return nextCheckTime;
    }

    /**
     * Get the next calculate time in the light of the last calculate time
     * @return the remaining time to the next calculation
     */
    private long checkNextCalculateWait() {
        // Get the last calculate time
        SmartPushPreferences prefs = SmartPushPreferences.getPreferences(this);
        long lastCalculateTime = prefs.getLastCalculateTime();

        long sinceLastTime = System.currentTimeMillis() - lastCalculateTime;
        Logging.v(TAG, "since the last calculate time = " + sinceLastTime);
         if (sinceLastTime >= DAY) {
             return 0; // re-calculate now
         } else {
             return DAY - sinceLastTime;
         }
    }

    /**
     * The entry point of smart push calculation
     */
    private void calculate() {
        if (mAccountMap != null && mAccountMap.size() > 0) {
            Logging.v(TAG, "startCalculate...");
            long startTime = System.currentTimeMillis();
            Calculator.getCalculator().startCalculate(this, mAccountMap);
            Logging.v(TAG, "Calculate end!!! cost: " + (System.currentTimeMillis()
                    - startTime) + "ms");
        }

        // Record the calculate finish time to the preference
        SmartPushPreferences prefs = SmartPushPreferences.getPreferences(this);
        prefs.settLastCalculateTime(System.currentTimeMillis());
    }

    /**
     * The Calculator responsible for the core algorithm process
     * of the smart push calculation
     */
    private static class Calculator {
        // The habit data objects list for the calculation this time
        ArrayList<HabitData> habitDataList = new ArrayList<HabitData>();
        // The Calculator singleton
        private static Calculator sCalculator= new Calculator();

        private static Calculator getCalculator() {
            return sCalculator;
        }

        private void startCalculate(Context context, HashMap<Long, Integer> accountMap) {
            if (context == null) {
                return;
            }
            // Clear the stale habit data at first
            habitDataList.clear();

            Set<Map.Entry<Long, Integer>> entrySet = accountMap.entrySet();
            for (Map.Entry<Long, Integer> entry : entrySet) {
                long accountId = (Long)entry.getKey();
                int dayCount = (Integer)entry.getValue();
                Logging.v(TAG, "accountId:" + accountId + " ,dayCount:" + dayCount);
                // Create a HabitData object for each account
                HabitData habitData = new HabitData(context, accountId, dayCount);
                habitDataList.add(habitData);
                // Start to calculate for one account
                habitData.startCalculate();
            }
        }

        /**
         * Get the calculation result of the account
         * @param accountId the ID of the account whose result would be fetched
         * @return the calculation result that indicates the sync
         * interval of each time scale (1 scale = 2 hours, e.g. 2:00am-4:00am),
         * null if no result data found for the account
         */
        public int[] getResult(long accountId) {
            for (HabitData data : habitDataList) {
                if (data.mAccountId == accountId) {
                    return data.mResults;
                }
            }
            return null;
        }

        /**
         * A HabitData object stands for an account's habit data,
         * as well as the calculating logic with those data
         */
        private static class HabitData {
            // The ID of the account whose habit data is represented by this HabitData object
            long mAccountId;

            // A table stands for one-day habit data statistical table of the account, so
            // this is the all statistical tables
            Table[] mTables;
            Context mContext;

            // All the habit data of this account, input these data to the corresponding Table object
            // to do statistics
            ArrayList<ArrayList<TableData>> mTableData = new ArrayList<ArrayList<TableData>>();

            // The sync interval results for each time scale, which are come from the calculation
            int[] mResults = new int[Table.SCALE_NUM];

            // The data structure represents a habit data record
            private class TableData {
                private int mEventType;
                private long mTime;
                private long mValue;

                TableData(int eventType, long time, long value) {
                    mEventType = eventType;
                    mTime = time;
                    mValue = value;
                }
            }

            private static final String[] HABIT_PROJECTION = new String[]{SmartPush.EVENT_TYPE,
                    SmartPush.TIMESTAMP, SmartPush.VALUE};
            private static final String HABIT_SELECTION = SmartPush.ACCOUNT_KEY + " =?";

            private static final int EVENTTYPE_COLUMN = 0;
            private static final int TIMESTAMP_COLUMN = 1;
            private static final int VALUE_COLUMN = 2;

            HabitData(Context context, long accountId, int dayCount) {
                mContext = context;
                mAccountId = accountId;
                mTables = new Table[dayCount];
                for (int i = 0; i < dayCount; i++) {
                    mTableData.add(new ArrayList<TableData>());
                }
            }

            private void startCalculate() {
                // Query all the habit data of this account from DB
                Cursor c = mContext.getContentResolver().query(SmartPush.CONTENT_URI,
                        HABIT_PROJECTION, HABIT_SELECTION, new String[]{String.valueOf(mAccountId)}, null);
                if (c != null) {
                    try {
                        while (c.moveToNext()) {
                            long timestamp = c.getLong(TIMESTAMP_COLUMN);
                            // The day account between today start time and the habit data recording time,
                            // ignore the fragmentary day (For example, today is Sept.7th, and the recording
                            // time is Sept.4th 3:00 pm, the day count should be 2)
                            long timeSpan = sTodayStartTime - timestamp;
                            int day;
                            if (timeSpan <= 0) {
                                day = -1;
                            } else {
                                day = Long.valueOf(timeSpan / DAY).intValue();
                            }
                            // We only concern about the data of full day(from Sept.5th 00:00:00 GMT
                            // to Sept.7yh 00:00:00 GMT),
                            // so ignore the habit data belong to the fragmentary day
                            if (day < 0 || day >= mTables.length) {
                                continue;
                            }
                            // The start time of the day which the habit data belongs to
                            long timeStart = sTodayStartTime - (day + 1) * DAY;
                            // The relative time of the habit data record
                            long relativeTime = timestamp - timeStart;
                            // Add a refined habit data to the corresponding table data list
                            mTableData.get(day).add(new TableData(c.getInt(EVENTTYPE_COLUMN),
                                    relativeTime, c.getLong(VALUE_COLUMN)));
                        }
                    } finally {
                        c.close();
                    }
                }

                // So far, all the habit data of the account have been refined and stored in mTableData.
                // Now input these data to the corresponding Tables to do the calculation in turn
                float[] fieldSum = new float[Table.SCALE_NUM]; // The sum of the "chance" for each time scale
                for (int i = 0; i < mTables.length; i++) {
                    mTables[i] = new Table();
                    mTables[i].inputData(mTableData.get(i));
                    Logging.v(TAG, "Table[" + i + "] startCalculate...");
                    mTables[i].startCalculate();
                    for (int j = 0; j < Table.SCALE_NUM; j++) {
                        fieldSum[j] += mTables[i].mResults[j];
                    }
                }

                // Logging out the chances
                StringBuilder s = new StringBuilder("[");
                for (float f : fieldSum) {
                    s.append(f + ", ");
                }
                s.append("]");
                Logging.v(TAG, "chances: " + s.toString());

                // The time scales with the Top4 chances would get high syncing interval,
                // the Middle4 get middle interval and the Last4 get low interval
                float[] sortSum = fieldSum.clone();
                Arrays.sort(sortSum);  // sort chances ascendingly
                int n = 0;
                // Some values in sortSum may be the same, so need to avoid set a result
                // element duplicately and miss another one
                Set<Integer> checked = new HashSet<Integer>();
                for (int m = 0; m < 3; m++) {
                    for (;n < Table.SCALE_NUM / 3 * (m + 1); n++) {
                        for (int k = 0; k < Table.SCALE_NUM; k++) {
                            if (fieldSum[k] == sortSum[n] && !checked.contains(k)) {
                                mResults[k] = m;   //  m value is related to the sync level value
                                checked.add(k);
                                Logging.v(TAG, "final mResults[" + k + "]:" + m);
                                break;
                            }
                        }
                    }
                }
            }
        }

        /**
         *  One table stands for one day record statistics of one account
         */
        private static class Table {
            // One day has 12 time scales (24hours / 2)
            private static final int SCALE_NUM = 12;
            // The summarized habit data for each time scale
            // The 3 columns are "Time", "Mail", "Duration" respectively
            // as demanded by the calculation algorithm design
            private int[][] mSummaries = new int[SCALE_NUM][3];
            // The "chance" value of each time scale
            private float[] mResults = new float[SCALE_NUM];

            private static final int TIME_COLUMN = 0;
            private static final int MAIL_COLUMN = 1;
            private static final int DURATION_COLUMN = 2;

            private void inputData(ArrayList<HabitData.TableData> dataList) {
                // Summarize the input data by time scale
                for (HabitData.TableData data : dataList) {
                    int scale = Long.valueOf(data.mTime / (HOUR * 2)).intValue();
                    int eventType = data.mEventType;

                    switch(eventType) {
                        case SmartPush.TYPE_DURATION:
                            mSummaries[scale][DURATION_COLUMN] += data.mValue;
                            break;
                        case SmartPush.TYPE_MAIL:
                            mSummaries[scale][MAIL_COLUMN] += data.mValue;
                            break;
                        case SmartPush.TYPE_OPEN:
                            mSummaries[scale][TIME_COLUMN] += data.mValue;
                            break;
                    }
                }
            }

            /**
             * Calculate in terms of the algorithm design
             */
            private void startCalculate() {
                float j, k, time_f, mail_f, duration_f;
                int sumTime = 0;
                int sumMail = 0;
                int sumDuration = 0;

                for (int z= 0; z < SCALE_NUM; z++) {
                    sumTime += mSummaries[z][TIME_COLUMN];
                    sumMail += mSummaries[z][MAIL_COLUMN];
                    sumDuration += mSummaries[z][DURATION_COLUMN];
                }

                // Set all the results as 0 and return at once if this day has no habit data
                // (user did not use email this day)
                if (sumTime == 0 && sumMail == 0 && sumDuration == 0) {
                    for (int z = 0; z < SCALE_NUM; z++) {
                        mResults[z] = 0;
                    }
                    return;
                }

                for (int z = 0; z < SCALE_NUM; z++) {
                    time_f = (sumTime == 0 ? 0.0f : (float)mSummaries[z][TIME_COLUMN] / sumTime);
                    mail_f = (sumMail == 0 ? 0.0f : (float)mSummaries[z][MAIL_COLUMN] / sumMail);
                    duration_f = (sumDuration == 0 ? 0.0f :
                            (float)mSummaries[z][DURATION_COLUMN] / sumDuration);

                    int temp = mSummaries[z][TIME_COLUMN] + mSummaries[z][MAIL_COLUMN];
                    float bc = 0.0f;
                    if (temp != 0) {
                        bc = (float)mSummaries[z][TIME_COLUMN]/temp;
                    }

                    j = mail_f * bc;
                    k = duration_f * bc;
                    mResults[z] = time_f * 0.6f + j * 0.1f + k * 0.3f;
                }

                // Just for debugging
                printResult();
            }

            /**
             * Logging out the calculation result for the table
             */
            private void printResult() {
                Logging.v(TAG, "------------------------------------\n");
                for (int i = 0; i < SCALE_NUM; i++) {
                    Logging.v(TAG, mSummaries[i][0] + " " + mSummaries[i][1] + " " + mSummaries[i][2] +
                            " " + mResults[i]);
                    Logging.v(TAG, "\n");
                }
            }
        }
    }

    /**
     * Change the sync interval for all the smart push accounts and return the time
     * to make the next adjustments
     * @return the time remaining for doing the next interval change for anyone account
     */
    private long makeAdjustments() {
        if (sStop) {
            return 0;
        }
        Logging.v(TAG, "makeAdjustments...");
        // Which time scale the current time in
        int scale = Long.valueOf((System.currentTimeMillis() - sTodayStartTime) / (2 * HOUR)).intValue();
        Logging.v(TAG, "current time scale: " + scale);
        // The time remaining to the next time scale
        long nextTimeLeast = 2 * HOUR - (System.currentTimeMillis() - sTodayStartTime) % (2 * HOUR);
        Logging.v(TAG, "The time remaining to the next time scale: " + nextTimeLeast);
        long minNextTime = Long.MAX_VALUE;

        // Waiting for 1 minute if ExchangeService stop working
        if (ExchangeService.INSTANCE == null) {
            return MINUTE;
        }

        // Synchronized here in order to avoid the possible case like:
        // user change the sync interval to non smart push, then ExchangeService's
        // AccountObserver::onAccountChanged change the sync interval, at the same
        // time below code may change the sync interval back.
        synchronized(ExchangeService.INSTANCE.mAccountList) {
            Set<Map.Entry<Long, Integer>> entrySet = mAccountMap.entrySet();
            for (Map.Entry<Long, Integer> entry : entrySet) {
                long nextTime = nextTimeLeast;
                // Above all, check if the account is still a smart push one at present
                long accountId = (Long)entry.getKey();
                if (!SmartPush.isSmartPushAccount(this, accountId)) {
                    mAccountMap.remove(accountId);
                    continue;
                }
                int[] result = Calculator.getCalculator().getResult(accountId);
                // If could not get the calculation result, just return and calculate again.
                // It may happen at one account is eligible for calculating right now but the
                // next calcuation time has not reached yet
                if (result == null) {
                    SmartPushPreferences prefs = SmartPushPreferences.getPreferences(this);
                    prefs.removeLastCalculateTime();
                    return 0;
                }

                changeSyncFrequency(result[scale], accountId);
                // Get the time remaining for doing the next interval change for anyone account,
                // Needless to wakeup to do the change if the next time scale interval is the
                // same to this one
                int i = scale;
                while (i < Calculator.Table.SCALE_NUM - 1 && result[i] == result[++i]) {
                        nextTime += 2 * HOUR;
                }
                minNextTime = Math.min(minNextTime, nextTime);
            }
        }

        Logging.v(TAG, "The time remaining to the next adjustments: " + minNextTime);
        return minNextTime;
    }

    private void changeSyncFrequency(int syncFrequency, long accountId) {
        int syncInterval = Mailbox.CHECK_INTERVAL_PUSH;
        switch(syncFrequency) {
            case SYNC_FREQUENCY_HIGH:
                syncInterval = Mailbox.CHECK_INTERVAL_PUSH;
                break;
            case SYNC_FREQUENCY_MEDIUM:
                syncInterval = 60;
                break;
            case SYNC_FREQUENCY_LOW:
                syncInterval = Mailbox.CHECK_INTERVAL_NEVER;
                break;
            default:
                break;
        }

        // Set the sync interval of Inbox, Calendar and Contacts as the account's
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ContentValues cv = new ContentValues();
        cv.put(Mailbox.SYNC_INTERVAL, syncInterval);

        long mailboxId = Account.getInboxId(this, accountId);
        Mailbox mailbox = Mailbox.restoreMailboxWithId(this, mailboxId);
        ops.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                Mailbox.CONTENT_URI, mailboxId)).withValues(cv).build());
        mailboxId = Mailbox.findMailboxOfType(this, accountId, Mailbox.TYPE_CONTACTS);
        ops.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                Mailbox.CONTENT_URI, mailboxId)).withValues(cv).build());
        mailboxId = Mailbox.findMailboxOfType(this, accountId, Mailbox.TYPE_CALENDAR);
        ops.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                Mailbox.CONTENT_URI, mailboxId)).withValues(cv).build());

        try {
            getContentResolver()
                .applyBatch(EmailContent.AUTHORITY, ops);
        } catch (RemoteException e) {
            // There is nothing to be done here; fail by returning null
            Logging.v(TAG, "RemoteException when updating mailboxes sync interval");
        } catch (OperationApplicationException e) {
            // There is nothing to be done here; fail by returning null
            Logging.v(TAG, "OperationApplicationException when updating mailboxes sync interval");
        }

        Logging.v(TAG, "changeSyncFrequency from " + mailbox.mSyncInterval +
                " to " + syncInterval + " for " + mailbox.mDisplayName +
                "(id=" + mailbox.mId + ") of account " + mailbox.mAccountKey);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!sStartingUp && INSTANCE == null) {
            sStartingUp = true;
            try {
                synchronized(sSyncLock) {
                    if ((sServiceThread == null || !sServiceThread.isAlive())
                            && EmailContent.count(this, HostAuth.CONTENT_URI,
                                    WHERE_PROTOCOL_EAS, null) > 0) {
                        // Should not start this thread if has no exchange account
                        sServiceThread = new Thread(this, "SmartPushService");
                        INSTANCE = this;
                        // If device rebooted, the calculation result will lose.
                        // Remove the last calculate time record when starting SmartPushService
                        // in order to recalculate again
                        SmartPushPreferences prefs = SmartPushPreferences.getPreferences(this);
                        prefs.removeLastCalculateTime();
                        Logging.v(TAG, "SmartPushService thread start to run");
                        sServiceThread.start();
                    }

                    if (sServiceThread == null) {
                        stopSelf();
                    }
                }
            } finally {
                sStartingUp = false;
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        EmailAsyncTask.runAsyncParallel(new Runnable() {
            @Override
            public void run() {
                // Quick checks first, before getting the lock
                if (sStartingUp) return;
                synchronized (sSyncLock) {
                    Logging.v("!!! SmartPushService, onCreate");
                    // Try to start up properly; we might be coming back from a crash that the Email
                    // application isn't aware of.
                    startService(new Intent(SmartPushService.this, SmartPushService.class));
                    if (sStop) {
                        return;
                    }
                }
            }});
    }

    @Override
    public void onDestroy() {
        Logging.v(TAG, "SmartPushService onDestroy");
        // Handle shutting down off the UI thread
        EmailAsyncTask.runAsyncParallel(new Runnable() {
            @Override
            public void run() {
                // Quick checks first, before getting the lock
                if (INSTANCE == null || sServiceThread == null) return;
                synchronized(sSyncLock) {
                    // Stop the sync manager thread and return
                    if (sServiceThread != null) {
                        sStop = true;
                        sServiceThread.interrupt();
                    }
                }
            }});
    }

    public static void alarmSmartPushService(Context context) {
        SmartPushService smartPushService = INSTANCE;
        if (smartPushService != null) {
            synchronized (smartPushService) {
                smartPushService.mKicked = true;
                Logging.v(TAG, "Alarm received: Kick");
                smartPushService.notify();
            }
        } else {
            Logging.v(TAG, "Alarm received: start smartpush service");
            startSmartPushService(context);
        }
    }

    public static void runAwake() {
        SmartPushService smartPushService = INSTANCE;
        if (smartPushService != null) {
            smartPushService.acquireWakeLock();
            smartPushService.clearAlarm();
        }
    }

    public static void runAsleep(long millis) {
        SmartPushService smartPushService = INSTANCE;
        if (smartPushService != null) {
            smartPushService.setAlarm(millis);
            smartPushService.releaseWakeLock();
        }
    }

    private void shutdown() {
        synchronized(sSyncLock) {
            sStop = false;
            INSTANCE = null;
            sServiceThread = null;
            // In extreme condition, this service may be killed (Low memory).
            // without releaseing the wakelock.
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
                mWakeLock = null;
            }
            Logging.v(TAG, "Goodbye");
        }
    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SMARTPUSH_SERVICE");
            mWakeLock.acquire();
            Logging.v(TAG, "+SMARTPUSH_SERVICE WAKE LOCK ACQUIRED");
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
        mWakeLock = null;
        Logging.v(TAG, "-SMARTPUSH_SERVICE WAKE LOCK RELEASED");
    }

    private void clearAlarm() {
        if (mPendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(mPendingIntent);
            Logging.v(TAG, "-Alarm cleared");
        }
    }

    private void setAlarm(long millis) {
        Intent i = new Intent(this, SmartPushAlarmReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, i, 0);
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + millis, mPendingIntent);
        Logging.v(TAG, "+Alarm set for " + millis/1000 + "s");
    }

    // Delete the habit data older than 2 week in case of the database expansion
    private void deleteStaleData() {
        long timeAfter = System.currentTimeMillis() - (2 * WEEK + 1);
        int deleted = getContentResolver().delete(SmartPush.CONTENT_URI,
                    SmartPush.TIMESTAMP + " < ?", new String[]{String.valueOf(timeAfter)});
        Logging.v(TAG, deleted + " rows stale habit data were deleted");
    }

    public static void kick(String reason) {
        SmartPushService smartPushService = INSTANCE;
        if (smartPushService != null) {
             synchronized (smartPushService) {
                 smartPushService.mKicked = true;
                 Logging.v(TAG, "Kick: " + reason);
                 smartPushService.notify();
             }
        } else if (ExchangeService.INSTANCE != null) {
            Logging.v(TAG, "Start smartpushservice when kick");
            startSmartPushService(ExchangeService.INSTANCE);
        }
    }
}
