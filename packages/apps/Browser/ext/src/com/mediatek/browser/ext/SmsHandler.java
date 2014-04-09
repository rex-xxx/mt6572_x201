package com.mediatek.browser.ext;

import static android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.gsm.SmsMessage;
import android.widget.Toast;

import com.mediatek.xlog.Xlog;

/**
 * Handle sms
 */
public class SmsHandler {

    private static final String XLOGTAG = "browser/SmsHandler";
    Activity mActivity;
    // monitor sms changes
    private IntentFilter mSmsChangedFilter;
    private BroadcastReceiver mSmsIntentReceiver;

    public SmsHandler(Activity aActivity) {
        mActivity = aActivity;

        mSmsChangedFilter = new IntentFilter();
        mSmsChangedFilter.addAction(SMS_RECEIVED_ACTION);
        mSmsIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Xlog.v(XLOGTAG, "(SmsHandler::onReceive)");
                if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {
                    SmsMessage[] messages = fetchMessageFromIntent(intent);
                    Xlog.v(XLOGTAG, "(SmsHandler::onReceive) count" + messages.length);
                    for (SmsMessage sms : messages) {
                        String strAddress = sms.getOriginatingAddress();
                        String strBody = sms.getMessageBody();
                        Xlog.v(XLOGTAG, "address: " + strAddress + ", body: " + strBody);
                        Toast.makeText(mActivity, ("From:" + strAddress + " message"),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }

    public void onPause() {
        Xlog.v(XLOGTAG, " (SmsHandler::onPause) unregisterReceiver SMS_RECEIVED_ACTION : "
                + "android.provider.Telephony.SMS_RECEIVED");
        // unregister network state listener
        mActivity.unregisterReceiver(mSmsIntentReceiver);
    }

    public void onResume() {
        Xlog.v(XLOGTAG, " (SmsHandler::onResume) registerReceiver SMS_RECEIVED_ACTION " +
                ": android.provider.Telephony.SMS_RECEIVED");
        mActivity.registerReceiver(mSmsIntentReceiver,mSmsChangedFilter);
    }

    private SmsMessage[] fetchMessageFromIntent(Intent intent) {
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        if (messages == null) {
            return null;
        }
        int pduCount = messages.length;
        SmsMessage[] msgs = new SmsMessage[pduCount];
        for (int i = 0; i < pduCount; i++) {
            msgs[i] = SmsMessage.createFromPdu((byte[])messages[i]);
        }
        return msgs;
    }
}
