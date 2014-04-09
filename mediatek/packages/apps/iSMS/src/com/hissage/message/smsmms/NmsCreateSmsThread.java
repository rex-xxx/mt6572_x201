package com.hissage.message.smsmms;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hissage.db.NmsContentResolver;
import com.hissage.util.log.NmsLog;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;

public final class NmsCreateSmsThread {

    private static final String TAG = "NmsCreateSmsThread";
    private static final String[] ID_PROJECTION = { "_id" };
    private static final String STANDARD_ENCODING = "UTF-8";
    private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");
    public static final Uri CONTENT_URI = Uri.withAppendedPath(Uri.parse("content://mms-sms/"),
            "conversations");
    public static final Uri OBSOLETE_THREADS_URI = Uri.withAppendedPath(CONTENT_URI, "obsolete");
    public static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern
            .compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

    public static final int COMMON_THREAD = 0;
    public static final int BROADCAST_THREAD = 1;

    // No one should construct an instance of this class.
    private NmsCreateSmsThread() {
    }


    public static long getOrCreateThreadId(Context context, String recipient) {
        Set<String> recipients = new HashSet<String>();
        if(!NmsSendMessage.getInstance().isAddressLegal(recipient, recipients)){
            return -1;
        }
        return getOrCreateThreadId(context, recipients);
    }

    /**
     * Given the recipients list and subject of an unsaved message, return its
     * thread ID. If the message starts a new thread, allocate a new thread ID.
     * Otherwise, use the appropriate existing thread ID.
     * 
     * Find the thread ID of the same set of recipients (in any order, without
     * any additions). If one is found, return it. Otherwise, return a unique
     * thread ID.
     */
    public static long getOrCreateThreadId(Context context, Set<String> recipients) {
        Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();

        for (String recipient : recipients) {
            if (isEmailAddress(recipient)) {
                recipient = extractAddrSpec(recipient);
            }

            uriBuilder.appendQueryParameter("recipient", recipient);
        }

        Uri uri = uriBuilder.build();

        Cursor cursor = NmsContentResolver.query(context.getContentResolver(), uri, ID_PROJECTION,
                null, null, null);

        NmsLog.trace(TAG, "getOrCreateThreadId cursor cnt: " + cursor.getCount());

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                } else {
                    // it is not necessary to handle it;
                }
            } finally {
                cursor.close();
            }
        }
        NmsLog.trace(TAG, "getOrCreateThreadId,  Unable to find or allocate a thread ID.");
        return -1;
    }

    public static long getOrCreateThreadId(Context context, String[] recipients) {
        Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();

        for (String recipient : recipients) {
            if (isEmailAddress(recipient)) {
                recipient = extractAddrSpec(recipient);
            }

            uriBuilder.appendQueryParameter("recipient", recipient);
        }

        Uri uri = uriBuilder.build();

        Cursor cursor = NmsContentResolver.query(context.getContentResolver(), uri, ID_PROJECTION,
                null, null, null);

        NmsLog.trace(TAG, "[strings]getOrCreateThreadId cursor cnt: " + cursor.getCount());

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                } else {
                    // it is not necessary to handle it;
                }
            } finally {
                cursor.close();
            }
        }
        NmsLog.trace(TAG, "[strings]getOrCreateThreadId,  Unable to find or allocate a thread ID.");
        return -1;
    }

    public static String extractAddrSpec(String address) {
        Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

        if (match.matches()) {
            return match.group(2);
        }
        return address;
    }

    /**
     * Returns true if the address is an email address
     * 
     * @param address
     *            the input address to be tested
     * @return true if address is an email address
     */
    public static boolean isEmailAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }

        String s = extractAddrSpec(address);
        Matcher match = Patterns.EMAIL_ADDRESS.matcher(s);
        return match.matches();
    }
}
