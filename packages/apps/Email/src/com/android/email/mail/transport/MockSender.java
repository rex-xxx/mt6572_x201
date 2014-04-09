package com.android.email.mail.transport;

import android.content.Context;

import com.android.email.mail.Sender;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.provider.Account;

/**
 * M: use for test
 */
public class MockSender extends Sender {

    public static Boolean mflag;

    @Override
    public void close() throws MessagingException {

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void open() throws MessagingException {

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void sendMessage(long messageId) throws MessagingException {
        if (mflag) {

        } else {
            throw new MessagingException("Unsupported protocol");
        }
    }

    public static Sender newInstance(Account account, Context context) throws MessagingException {
        return new MockSender(context, account);
    }

    private MockSender(Context context, Account account) throws MessagingException {

    }
}
