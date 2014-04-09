
package com.android.emailcommon.service;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.android.emailcommon.Logging;
import com.android.emailcommon.internet.MimeMessage;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.provider.Account;

public class EmailExternalUtils {

    private static final String TAG = "EmailExternalUtils";

    public EmailExternalUtils() {
    }

    /**
     * It's counter for BT MAP UpdateInbox request
     */
    private static Map<Long, Integer> sUpdateCountMap = new HashMap<Long, Integer>();

    /**
     * Use map store mailbox id need a feedback.
     * 
     * @param mailboxId
     * @param add If true that means it's necessary return a feedback.
     */
    public static void updateMail(long mailboxId, boolean add) {
        synchronized (sUpdateCountMap) {
            if (add) {
                Long key = Long.valueOf(mailboxId);
                if (!sUpdateCountMap.containsKey(key)) {
                    sUpdateCountMap.put(key, 1);
                    Log.d(TAG, "Add mailboxId:" + mailboxId);
                }
            } else {
                Long key = Long.valueOf(mailboxId);
                if (sUpdateCountMap.containsKey(key)) {
                    Log.d(TAG, "Remove mailboxId:" + mailboxId);
                    sUpdateCountMap.remove(key);
                } else {
                    Log.i(TAG, "Input argument:mailboxId:" + mailboxId + " not need callback.");
                }
            }
        }
    }

    public static boolean canSendBroadcast(long mailboxId) {
        Long key = Long.valueOf(mailboxId);
        synchronized (sUpdateCountMap) {
            return sUpdateCountMap.containsKey(key);
        }
    }

    /**
     * Get "From:" from mimeMessage,compare this address whether or not is same as account
     * 
     * @param in InputStream a draft whole email data
     * @param account EmailContent.Account account send email
     * @return
     */
    public static boolean checkFromAddress(MimeMessage mimeMessage, Account account) {
        boolean same = false;
        Address sendAdr = new Address(account.getEmailAddress(), account.getSenderName());
        Logging.d(TAG, "Address:" + sendAdr.getAddress() + "," + sendAdr.getPersonal());
        // compare emailFrom to mimeFrom
        try {
            Address[] mimeFrom = mimeMessage.getFrom();
            if (null != mimeFrom && mimeFrom.length > 0) {
                same = mimeFrom[0].equals(sendAdr);
                Logging.d(TAG, "From:" + mimeFrom[0] + ",Account:" + sendAdr + ",same:" + same);
            }
        } catch (MessagingException e) {
            Logging.w(TAG, e);
        }
        return same;
    }

}
