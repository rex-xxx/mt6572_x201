

package com.android.browser;

import static android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;
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

                Xlog.v(XLOGTAG, "(SmsHandler::onReceive) ");

                if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {
                    final SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
                    if (messages == null) {
                        Xlog.v(XLOGTAG, "Smshandler.onReceive, messages are null");
                        return;
                    }
                    Xlog.v(XLOGTAG, "(SmsHandler::onReceive) count" + messages.length);
                    SmsMessage sms = messages[0];
                    final String strAddress = sms.mWrappedSmsMessage == null ? "" : sms.getDisplayOriginatingAddress();
                    String strBody = null;
                    final int count = messages.length;
                    if (count == 1) {
                        sms = messages[0];
                        strBody = sms.mWrappedSmsMessage == null ? "" : sms.getDisplayMessageBody();
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        for (SmsMessage msg : messages) {
                            if (msg.mWrappedSmsMessage != null) {
                                sb.append(msg.getDisplayMessageBody());
                            }
                        }
                        strBody = sb.toString();
                    }
                    Xlog.v(XLOGTAG, "address: " + strAddress + ", body: " + strBody);
                    Toast.makeText(mActivity,
                            mActivity.getString(R.string.sms_received, strAddress),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        
    }

    void onPause() {
        Xlog.v(XLOGTAG, "(SmsHandler::onPause) unregisterReceiver SMS_RECEIVED_ACTION");
        // unregister network state listener
        mActivity.unregisterReceiver(mSmsIntentReceiver);
    }

    void onResume() {
        Xlog.v(XLOGTAG, "(SmsHandler::onResume) registerReceiver SMS_RECEIVED_ACTION");
        mActivity.registerReceiver(mSmsIntentReceiver,mSmsChangedFilter);
    }
}
