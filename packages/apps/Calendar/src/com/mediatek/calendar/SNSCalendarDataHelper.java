package com.mediatek.calendar;

import com.mediatek.pluginmanager.Plugin.ObjectCreationException;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.snsone.interfaces.IAccountInfo;
import com.mediatek.snsone.interfaces.IPostOperations;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.CalendarContract.Calendars;
import android.util.Log;

/**
 * Represents one SNS account.
 */
class SNSAccount {
    /**
     * "_id" field in "Calendars" table.
     */
    int calendarId;
    /**
     * Identifier of one SNS account, get from Calendar's database.
     */
    String accountType;
    /**
     * If a SNS account supported by SNS plugin, checked by SNS plugin.
     */
    boolean isSupported;
}

/**
 * Used by Calendar to get SNS data, including string resource, icon, etc. NOTE:
 * In one session, before you invoke any method in this helper, you MUST invoke
 * "initialize" method first!!!
 */
public class SNSCalendarDataHelper {
    private static final String TAG = "SNSCalendarDataHelper";

    // SNS plugin action and class names
    private static final String SNSPLUGIN_ACTION = "com.mediatek.snsone.interfaces";
    private static final String METANAME_ACCOUNT = "account";
    private static final String METANAME_ALBUM = "album";
    private static final String METANAME_CONTACT = "contact";
    private static final String METANAME_POST = "post";

    /*
     * Key of account type.
     */
    private static final String ACCOUNT_TYPE = "account_type";
    /*
     * Key of user id.
     */
    private static final String USER_ID = "user_id";

    /*
     * Store "account_type" values from "Calendars" table, also if the account
     * is supported by SNS plugin. Because it is static, so, every time when you
     * initialize this helper or refresh account information this should be
     * cleared and recreated completely. If there are no supported accounts,
     * this should be null.
     */
    private static SNSAccount[] sAccountTypes;

    // projection of calendar id and account type in "Calendars" table
    private static final String[] EVENT_PROJECTION = new String[] {
            Calendars._ID, // 0
            Calendars.ACCOUNT_TYPE // 1
    };

    private static final int PROJECTION__ID_INDEX = 0;
    private static final int PROJECTION_ACCOUNT_TYPE_INDEX = 1;

    /**
     * Initialize this helper.
     * 
     * @param context
     *            application context
     * @return true if succeed, false otherwise
     */
    public static boolean initialize(Context context) {
        if (!getExistedAccounts(context.getContentResolver())) {
            return false;
        }

        logCursorResult();

        if (!checkIfExistedAccountsSupported(context)) {
            return false;
        }

        logCursorResult();

        return true;
    }

    /*
     * Get accounts from "Calendars" table.
     */
    private static boolean getExistedAccounts(ContentResolver cr) {
        // Query "Calendars" table
        Cursor cursor = cr.query(Calendars.CONTENT_URI, EVENT_PROJECTION, null,
                null, null);

        if (cursor == null) {
            Log.d(TAG, "getSNSAccounts(), failed to query SNS account.");

            return false;
        }

        int count = cursor.getCount();
        Log.d(TAG, "getSNSAccounts(), account count: " + count);

        // there are no accounts in "Calendars" table
        if (count == 0) {
            // set it to null to get rid of old data
            sAccountTypes = null;
            // close the cursor since it will NOT be used anymore
            cursor.close();

            return false;
        }

        // It should be created every time so it has right data!!!
        sAccountTypes = new SNSAccount[count];
        for (int i = 0; i < count; ++i) {
            cursor.moveToNext();

            SNSAccount account = new SNSAccount();

            account.calendarId = (int) cursor.getLong(PROJECTION__ID_INDEX);
            account.accountType = cursor
                    .getString(PROJECTION_ACCOUNT_TYPE_INDEX);
            account.isSupported = false;

            sAccountTypes[i] = account;
        }

        // close the cursor since it will NOT be used anymore
        cursor.close();

        return true;
    }

    /*
     * Get a list of supported account from all available accounts.
     */
    private static boolean checkIfExistedAccountsSupported(Context context) {
        // see if there is any SNS plugin registered in system
        PluginManager pm = PluginManager.create(context, SNSPLUGIN_ACTION);
        if (pm.getPluginCount() == 0) {
            Log.d(TAG, "checkIfAccountSupported(), no plugin available.");

            return false;
        }

        Log.d(TAG,
                "checkIfAccountSupported(), available plugin count: "
                        + pm.getPluginCount());

        // get the AccountInfoHolder implementation
        IAccountInfo ai = null;
        try {
            ai = (IAccountInfo) PluginManager.createPluginObject(context,
                    SNSPLUGIN_ACTION, METANAME_ACCOUNT);
        } catch (ObjectCreationException e) {
            e.printStackTrace();

            Log.d(TAG,
                    "checkIfAccountSupported(), Can NOT get the AccountInfoHolder.");
            return false;
        }

        // no accounts
        if (sAccountTypes == null || sAccountTypes.length == 0) {
            // reset it to null
            sAccountTypes = null;

            return false;
        }

        int supportedCount = 0;
        // check if our accounts are supported by SNS plugin
        for (int i = 0; i < sAccountTypes.length; ++i) {
            if (ai.isAccountSupported(sAccountTypes[i].accountType)) {
                sAccountTypes[i].isSupported = true;
                ++supportedCount;
            }
        }

        if (supportedCount == 0) {
            Log.d(TAG, "checkIfAccountSupported(), no accounts supported.");

            // since no accounts supported
            sAccountTypes = null;

            return false;
        }

        // just keep the accounts supported by SNS plugin
        int index = 0;
        SNSAccount[] supported = new SNSAccount[supportedCount];
        for (int i = 0; i < sAccountTypes.length; ++i) {
            if (sAccountTypes[i].isSupported) {
                supported[index] = sAccountTypes[i];
                ++index;
            }
        }
        // here we just keep the accounts supported
        sAccountTypes = supported;

        return true;
    }

    /*
     * Log the accounts.
     */
    private static void logCursorResult() {
        if (sAccountTypes == null || sAccountTypes.length == 0) {
            Log.d(TAG, "No accounts.");

            return;
        }

        for (int i = 0; i < sAccountTypes.length; ++i) {
            Log.d(TAG, sAccountTypes[i].calendarId + " account: "
                    + sAccountTypes[i].accountType + ", is supported: "
                    + sAccountTypes[i].isSupported);
        }
    }

    /**
     * Only for test usage.
     * 
     * @hide
     */
    public static void prepareTestData(int[] ids, String[] accounts) {
        int count = ids.length;
        sAccountTypes = new SNSAccount[count];

        for (int i = 0; i < count; ++i) {
            sAccountTypes[i] = new SNSAccount();
            sAccountTypes[i].calendarId = ids[i];
            sAccountTypes[i].accountType = accounts[i];
            sAccountTypes[i].isSupported = true;
        }

        logCursorResult();
    }

    /**
     * Check if a SNS account is supported.
     * 
     * @param accountType
     *            name of a SNS account
     * @return true if supported, otherwise false
     */
    public static boolean isAccountTypeSupported(String accountType) {
        boolean ok = false;

        if (sAccountTypes == null) {
            Log.d(TAG,
                    "isAccountTypeSupported(), no accounts supported by SNS plugin.");

            return false;
        }

        if (accountType == null) {
            Log.d(TAG, "isAccountTypeSupported(), invalid account type: null.");

            return false;
        }

        for (int i = 0; i < sAccountTypes.length; ++i) {
            if (sAccountTypes[i].accountType.equals(accountType)) {
                ok = true;

                break;
            }
        }

        return ok;
    }

    /**
     * See if a account responding to the calendarId is supported, return the
     * account type if supported, null otherwise.
     * 
     * @param calendarId
     *            id of the calendar to which the event belongs
     * @return a account string if the calendar is supported by SNS plugin, null
     *         otherwise
     */
    public static String getAccountType(int calendarId) {
        String type = null;

        if (sAccountTypes == null) {
            Log.d(TAG, "getAccountType(), no accounts supported by SNS plugin.");

            return null;
        }

        for (int i = 0; i < sAccountTypes.length; ++i) {
            if (sAccountTypes[i].calendarId == calendarId) {
                type = new String(sAccountTypes[i].accountType);
            }
        }

        return type;
    }

    /*
     * Get the account information holder.
     */
    private static IAccountInfo getAccountInfoHolder(Context context) {
        IAccountInfo holder = null;

        // get the AccountInfoHolder implementation
        try {
            holder = (IAccountInfo) PluginManager.createPluginObject(context,
                    SNSPLUGIN_ACTION, METANAME_ACCOUNT);
        } catch (ObjectCreationException e) {
            e.printStackTrace();

            Log.d(TAG,
                    "getAccountInfoHolder(), Can NOT get the AccountInfoHolder.");

            return null;
        }

        return holder;
    }

    /*
     * Get the post operations holder.
     */
    private static IPostOperations getPostOperationsHolder(Context context) {
        IPostOperations holder = null;

        // get the PostOperationsHolder implementation
        try {
            holder = (IPostOperations) PluginManager.createPluginObject(
                    context, SNSPLUGIN_ACTION, METANAME_POST);
        } catch (ObjectCreationException e) {
            e.printStackTrace();

            Log.d(TAG,
                    "getPostOperationsHolder(), Can NOT get the PostOperationsHolder.");

            return null;
        }

        return holder;
    }

    /**
     * Get a icon of a specific SNS account.
     * 
     * @param context
     *            host context
     * @param accountType
     *            name of the SNS account
     * @return a drawable if succeed, null otherwise
     */
    public static Drawable getAccountIcon(Context context, String accountType) {
        Drawable icon = null;

        if (accountType == null) {
            Log.d(TAG, "getAccountIcon(), invalid account type: null.");

            return null;
        }

        IAccountInfo ai = getAccountInfoHolder(context);
        if (ai == null) {
            Log.d(TAG, "getAccountIcon(), Can NOT get the AccountInfoHolder.");

            return null;
        }

        icon = ai.getAccountIcon(accountType);

        return icon;
    }

    /**
     * Get a account icon of bitmap format according to a calendar id
     * 
     * @param context
     *            application context
     * @param calendarId
     *            id of one calendar account
     * @return null if account is NOT supported
     */
    public static Bitmap getAccountIconBitmap(Context context, int calendarId) {
        Bitmap icon = null;

        // see if current account is supported
        String accountType = getAccountType(calendarId);
        if (accountType == null) {
            Log.d(TAG,
                    "getAccountIconBitmap(), current account is NOT supported.");

            return null;
        }

        IAccountInfo ai = getAccountInfoHolder(context);
        if (ai == null) {
            Log.d(TAG,
                    "getAccountIconBitmap(), Can NOT get the AccountInfoHolder.");

            return null;
        }

        icon = ai.getAccountIconBitmap(accountType);

        return icon;
    }

    /**
     * Get a account birthday string according to a calendar id
     * 
     * @param context
     *            application context
     * @param calendarId
     *            id of one calendar account
     * @return null if account is NOT supported
     */
    public static String getBirthdayString(Context context, int calendarId) {
        String str = null;

        // see if current account is supported
        String accountType = getAccountType(calendarId);
        if (accountType == null) {
            Log.d(TAG, "getBirthdayString(), current account is NOT supported.");

            return null;
        }

        IAccountInfo ai = getAccountInfoHolder(context);
        if (ai == null) {
            Log.d(TAG,
                    "getBirthdayString(), Can NOT get the AccountInfoHolder.");

            return null;
        }

        str = ai.getAccountBirthdayString(accountType);

        return str;
    }

    private static IPostOperations.Action getPostAction(Context context,
            String accountType, String userId) {
        IPostOperations.Action actionGot = null;

        if (accountType == null) {
            Log.d(TAG, "getPostAction(), invalidate account type: null.");

            return null;
        }

        IPostOperations poh = getPostOperationsHolder(context);
        if (poh == null) {
            Log.d(TAG, "getPostAction(), Can NOT get the PostOperationsHolder.");

            return null;
        }

        // fill data used to get the post action information
        Bundle bundle = new Bundle();
        bundle.putString(ACCOUNT_TYPE, accountType);
        bundle.putString(USER_ID, userId);

        actionGot = poh.getPostAction(bundle);

        if (actionGot == null) {
            Log.d(TAG,
                    "getPostAction(), Can NOT get the action because a invalid account type.");

            return null;
        }

        return actionGot;
    }

    /**
     * Get a intent for one contact in a specific SNS account, used to post on
     * the contact's wall.
     * 
     * @param context
     *            application context
     * @param accountType
     *            a specific SNS account
     * @param userId
     *            id of one specific contact
     * @return a intent if succeed, null otherwise
     */
    public static Intent getPostActionIntent(Context context,
            String accountType, String userId) {
        Intent i = null;

        IPostOperations.Action actionGot = getPostAction(context, accountType,
                userId);
        if (actionGot != null) {
            i = actionGot.intent;
        }

        return i;
    }
}
