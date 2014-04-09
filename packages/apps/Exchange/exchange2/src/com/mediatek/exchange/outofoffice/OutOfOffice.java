package com.mediatek.exchange.outofoffice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;

import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

import com.android.emailcommon.mail.ServerCommandInfo;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.OofParams;
import com.android.exchange.AbstractSyncService;
import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.ExchangeService;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.SettingsParser;
import com.android.exchange.adapter.Tags;

/**
 * M: Handles outofoffice setting with server
 * 
 * This class generate oof command with the settings from user,
 * and push these setting commands to server,then wait for the response
 * from server,at last parse the response and return the result to oof;
 */

public class OutOfOffice {
    private static OofParams mOofParams = null;
    private static final String BODY_TYPE = "Text";
    private static final String SETTINGS_ENABLE = "1";
    private static final String SETTINGS_DISABLE = "0";
    private static HashMap<Long, AbstractSyncService> sOofSvc =
            new HashMap<Long, AbstractSyncService>();

    public static OofParams syncOof(Context context, long accountId,
            OofParams oofParams, boolean isGet) {
        boolean res = false;
        Account account = Account.restoreAccountWithId(context, accountId);
        if (account == null) {
            return null;
        }
        EasSyncService svc = EasSyncService.setupServiceForAccount(context, account);
        if (svc == null) {
            return null;
        }
        Mailbox mailbox = Mailbox.restoreMailboxOfType(context, accountId,
                Mailbox.TYPE_EAS_ACCOUNT_MAILBOX);
        // Sanity check; account might have been deleted?
        if (mailbox == null) {
            return null;
        }
        try {
            svc.mMailbox = mailbox;
            svc.mAccount = account;
            Serializer s = new Serializer();
            s.start(Tags.SETTINGS_SETTINGS)
            .start(Tags.SETTINGS_OOF);
            if (isGet) {
                s.start(Tags.SETTINGS_GET)
                .data(Tags.SETTINGS_BODY_TYPE, BODY_TYPE)
                .end().end().end().done();
            } else {
                String stime = OutOfOffice.convertMillisTimeToEmailDateTime(
                        oofParams.getStartTimeInMillis());
                String etime = OutOfOffice.convertMillisTimeToEmailDateTime(
                        oofParams.getEndTimeInMillis());
                if (oofParams.getOofState() != 0) {
                    s.start(Tags.SETTINGS_SET).data(Tags.SETTINGS_OOF_STATE,
                            String.valueOf(ServerCommandInfo.OofInfo.OOF_IS_TIME_BASED)).data(
                            Tags.SETTINGS_START_TIME,
                            OutOfOffice.convertMillisTimeToEmailDateTime(oofParams
                                    .getStartTimeInMillis()))
                    .data(Tags.SETTINGS_END_TIME, OutOfOffice.convertMillisTimeToEmailDateTime(
                            oofParams.getEndTimeInMillis()))
                    .start(Tags.SETTINGS_OOF_MESSAGE)
                    .tag(Tags.SETTINGS_APPLIES_TO_INTERNAL)
                    .data(Tags.SETTINGS_ENABLED, SETTINGS_ENABLE)
                    .data(Tags.SETTINGS_REPLY_MESSAGE, oofParams.getReplyMessage())
                    .data(Tags.SETTINGS_BODY_TYPE, BODY_TYPE)
                    .end();
                    if (oofParams.getIsExternal() != 0) {
                        s.start(Tags.SETTINGS_OOF_MESSAGE)
                        .tag(Tags.SETTINGS_APPLIES_TO_EXTERNAL_KNOWN)
                        .data(Tags.SETTINGS_ENABLED, SETTINGS_ENABLE)
                        .data(Tags.SETTINGS_REPLY_MESSAGE, oofParams.getReplyMessage())
                        .data(Tags.SETTINGS_BODY_TYPE, BODY_TYPE)
                        .end()
                        .start(Tags.SETTINGS_OOF_MESSAGE)
                        .tag(Tags.SETTINGS_APPLIES_TO_EXTERNAL_UNKNOWN)
                        .data(Tags.SETTINGS_ENABLED, SETTINGS_ENABLE)
                        .data(Tags.SETTINGS_REPLY_MESSAGE, oofParams.getReplyMessage())
                        .data(Tags.SETTINGS_BODY_TYPE, BODY_TYPE)
                        .end().end().end().end().done();
                    } else {
                        s.start(Tags.SETTINGS_OOF_MESSAGE)
                        .tag(Tags.SETTINGS_APPLIES_TO_EXTERNAL_KNOWN)
                        .data(Tags.SETTINGS_ENABLED, SETTINGS_DISABLE)
                        .end()
                        .start(Tags.SETTINGS_OOF_MESSAGE)
                        .tag(Tags.SETTINGS_APPLIES_TO_EXTERNAL_UNKNOWN)
                        .data(Tags.SETTINGS_ENABLED, SETTINGS_DISABLE)
                        .end().end().end().end().done();
                    }
                } else {
                    s.start(Tags.SETTINGS_SET)
                    .data(Tags.SETTINGS_OOF_STATE, SETTINGS_DISABLE)
                    .end().end().end().done();
                }
            }
            sOofSvc.put(accountId, svc);
            EasResponse resp = svc.sendHttpClientPost("Settings", s.toByteArray());
            try {
                int code = resp.getStatus();
                if (code == HttpStatus.SC_OK) {
                    InputStream is = resp.getInputStream();
                    try {
                        SettingsParser sp = new SettingsParser(is, svc);
                        sp.parse();
                        res = sp.getOofStatus() != 0;
                        int status = sp.getOofStatus();
                        int oofState = sp.getOofState();
                        long startTimeInMillis = sp.getStartTimeInMillis();
                        long endTimeInMillis = sp.getEndTimeInMillis();
                        String oofMessage = sp.getReplyMessage();
                        int isExternal = sp.getIsExternal();
                        mOofParams = new OofParams(status, oofState, startTimeInMillis,
                                endTimeInMillis, isExternal, oofMessage);
                    } finally {
                        is.close();
                    }
                } else {
                    svc.userLog("OOF returned " + code);
                }
            } finally {
                resp.close();
            }
        } catch (IOException e) {
            svc.userLog("OOF exception " + e);
            //Unable to connect to server
            mOofParams = new OofParams(ServerCommandInfo.OofInfo.NETWORK_SHUT_DOWN, 0, 0,
                    0, 0, null);
        } finally {
            sOofSvc.remove(accountId);
        }
        return mOofParams;
    }

    /**
     * M: Convert a time in millis format to email date time
     * @param millisTime
     * @return EmailDateTime
     */
    public static String convertMillisTimeToEmailDateTime(long millisTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisTime);
        String month;
        String day;
        String hour;
        String minute;
        int time;
        time = calendar.get(Calendar.MONTH) + 1;
        if (time < 10) {
            month = getTimeString(time);
        } else {
            month = String.valueOf(time);
        }
        time = calendar.get(Calendar.DAY_OF_MONTH);
        if (time < 10) {
            day = getTimeString(time);
        } else {
            day = String.valueOf(time);
        }
        time = calendar.get(Calendar.HOUR_OF_DAY);
        if (time < 10) {
            hour = getTimeString(time);
        } else {
            hour = String.valueOf(time);
        }
        time = calendar.get(Calendar.MINUTE);
        if (time < 10) {
            minute = getTimeString(time);
        } else {
            minute = String.valueOf(time);
        }
        return calendar.get(Calendar.YEAR) + "-" + month + "-" + day
                + "T" + hour + ":" + minute + ":00.000Z";
    }

    private static String getTimeString(int time) {
        return "0" + time;
    }

    public static void stopOof(long accountId) {
        AbstractSyncService svc = sOofSvc.get(accountId);
        if (svc != null) {
            svc.stop();
            sOofSvc.remove(accountId);
        }
    }
}
