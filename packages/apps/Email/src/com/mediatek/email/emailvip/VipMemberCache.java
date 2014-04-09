package com.mediatek.email.emailvip;

import java.util.HashSet;

import com.android.email.Throttle;
import com.android.email.data.ClosingMatrixCursor;
import com.android.emailcommon.Logging;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.VipMember;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.utility.EmailAsyncTask;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

/**
 * M: Cache the vip members in memory to improve the performance
 *
 */
public class VipMemberCache {
    public static final String TAG = "VIP_Settings";

    // This static hash set stores all the vip addresses.
    public static HashSet<String> sVipAddresses = new HashSet<String>();
    private static VipMemberCache sInstance;

    private Context mContext;
    private VipContentObserver mContentObserver;
    private UpdateRunnable mUpdateRunnable;

    private VipMemberCache(Context context) {
        Logging.d(TAG, "VipMemberCache init...");
        mContext = context;
        mUpdateRunnable = new UpdateRunnable();
        mContentObserver = new VipContentObserver(new Handler(), mContext, mUpdateRunnable);
        mContentObserver.register(VipMember.CONTENT_URI);
        EmailAsyncTask.runAsyncParallel(mUpdateRunnable);
    }

    /**
     * In order to improve the performance, we keep a vip member cache in memory.
     * The cache must be initialized after Email running
     */
    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new VipMemberCache(context);
        }
    }

    /**
     * Check is there VIP members existed
     * @return true if has some VIP members
     */
    public static boolean hasVipMembers() {
        synchronized (sVipAddresses) {
            return sVipAddresses.size() > 0;
        }
    }

    public static int getVipMembersCount()   {
        synchronized (sVipAddresses) {
            return sVipAddresses.size();
        }
    }

    /**
     * Check is the email address belong to a Vip Member
     * @param fromList the email address or from list to be checked
     * @return true if the email address belong to a Vip Member
     */
    public static boolean isVIP(String fromList) {
        String emailAddress = Address.getFirstMailAddress(fromList);
        if (TextUtils.isEmpty(emailAddress)) {
            return false;
        }
        return sVipAddresses.contains(emailAddress.toLowerCase());
    }

    /**
     * Get the message count of this account sent from the VIP
     * @param context the context
     * @param accountId the id of the account
     * @return the count of VIP messages of the account
     */
    public static int getVipMessagesCount(Context context, long accountId) {
        return getVipMessagesCount(context, accountId, false);
    }

    /**
     * Get the message count of this account sent from the VIP
     * @param context the context
     * @param accountId the id of the account
     * @param onlyUnread is only find the unread messages
     * @return the count of VIP messages of the account
     */
    public static int getVipMessagesCount(Context context, long accountId, boolean onlyUnread) {
        Cursor cursor = getVipMessagesIds(context, accountId, onlyUnread);
        if (cursor == null) {
            return 0;
        }
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Get the messages of this account sent from the VIP
     * @param context the context
     * @param accountId the id of the account
     * @return the VIP messages of the account
     */
    public static Cursor getVipMessagesIds(Context context, long accountId) {
        return getVipMessagesIds(context, accountId, false);
    }

    /**
     * Get the messages of this account sent from the VIP
     * @param context the context
     * @param accountId the id of the account
     * @param onlyUnread is only find the unread messages
     * @return the VIP messages of the account
     */
    public static Cursor getVipMessagesIds(Context context, long accountId, boolean onlyUnread) {
        if (!VipMemberCache.hasVipMembers()) {
            return new MatrixCursor(EmailContent.ID_PROJECTION);
        }
        String vipSelection = Message.ALL_VIP_SELECTION;
        if (onlyUnread) {
            vipSelection = Message.FLAG_READ + "=0 AND " + vipSelection;
        }
        if (accountId > 0 && accountId != Account.ACCOUNT_ID_COMBINED_VIEW) {
            vipSelection = Message.ACCOUNT_KEY + " = " + accountId + " AND " + vipSelection;
        }
        Cursor c = context.getContentResolver().query(Message.CONTENT_URI,
                new String[] { Message.RECORD_ID, Message.FROM_LIST },
                vipSelection, null, Message.TIMESTAMP + " DESC");
        if (c == null) {
            Logging.e(TAG, "getVipMessagesIds return empty cursor because cursor is null");
            return new MatrixCursor(EmailContent.ID_PROJECTION);
        }
        ClosingMatrixCursor matrixCursor = new ClosingMatrixCursor(
                EmailContent.ID_PROJECTION, c);
        while (c.moveToNext()) {
            String fromList = c.getString(1);
            if (VipMemberCache.isVIP(fromList)) {
                RowBuilder row = matrixCursor.newRow();
                row.add(c.getLong(0));
            }
        }
        return matrixCursor;
    }

    /**
     * Update the Vip members cache. Do not call it in UI thread.
     */
    public static void updateVipMemberCache() {
        if (sInstance != null) {
            sInstance.mUpdateRunnable.run();
        }
    }

    private class UpdateRunnable implements Runnable {

        @Override
        public void run() {
            if (mContext == null) {
                return;
            }
            Cursor c = null;
            try {
                c = mContext.getContentResolver().query(VipMember.CONTENT_URI,
                        VipMember.CONTENT_PROJECTION, null, null, null);
                synchronized (sVipAddresses) {
                    sVipAddresses.clear();
                    while (c.moveToNext()) {
                        String emailAddress = c.getString(VipMember.EMAIL_ADDRESS_COLUMN);
                        if (TextUtils.isEmpty(emailAddress)) {
                            continue;
                        }
                        sVipAddresses.add(emailAddress.toLowerCase());
                    }
                }
            } catch (Exception ex) {
                Logging.w(TAG, "Can not update VipMemberCache", ex);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    private class VipContentObserver extends ContentObserver implements Runnable{
        private final Throttle mThrottle;
        private Context mInnerContext;
        private boolean mRegistered;
        private Runnable mInnerRunnable;

        public VipContentObserver(Handler handler, Context context, Runnable runnable) {
            super(handler);
            mInnerContext = context;
            mThrottle = new Throttle("VipContentObserver", this, handler);
            mInnerRunnable = runnable;
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mRegistered) {
                mThrottle.onEvent();
            }
        }

        public void unregister() {
            if (!mRegistered) {
                return;
            }
            mThrottle.cancelScheduledCallback();
            mInnerContext.getContentResolver().unregisterContentObserver(this);
            mRegistered = false;
        }

        public void register(Uri notifyUri) {
            unregister();
            mInnerContext.getContentResolver().registerContentObserver(notifyUri, true, this);
            mRegistered = true;
            Logging.d(TAG, "VipContentObserver register");
        }

        @Override
        public void run() {
            EmailAsyncTask.runAsyncParallel(mInnerRunnable);
        }
    }
}
