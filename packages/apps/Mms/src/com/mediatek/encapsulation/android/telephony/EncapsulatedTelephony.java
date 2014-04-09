package com.mediatek.encapsulation.android.telephony;

import android.provider.BaseColumns;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.content.Intent;
import android.content.ContentUris;
import android.database.DatabaseUtils;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.util.Log;
import android.provider.Telephony;
import android.database.sqlite.SqliteWrapper;
import android.telephony.SmsCbMessage;

import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/// M: ALPS00510627, SMS Framewrok API refactoring
public class EncapsulatedTelephony {
    private static final String TAG = "EncapsulatedTelephony";

    /** M: MTK Add */
    public interface TextBasedSmsCbColumns {

        /**
         * The SIM ID which indicated which SIM the SMSCb comes from Reference
         * to Telephony.SIMx
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.SIM_ID : "sim_id";

        /**
         * The channel ID of the message which is the message identifier defined
         * in the Spec. 3GPP TS 23.041
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String CHANNEL_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.CHANNEL_ID : "channel_id";

        /**
         * The date the message was sent
         * <P>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String DATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.DATE : "date";

        /**
         * Has the message been read
         * <P>
         * Type: INTEGER (boolean)
         * </P>
         */
        public static final String READ = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.READ : "read";

        /**
         * The body of the message
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String BODY = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.BODY : "body";

        /**
         * The thread id of the message
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String THREAD_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.THREAD_ID : "thread_id";

        /**
         * Indicates whether this message has been seen by the user. The "seen"
         * flag will be used to figure out whether we need to throw up a
         * statusbar notification or not.
         */
        public static final String SEEN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.SEEN : "seen";

        /**
         * Has the message been locked?
         * <P>
         * Type: INTEGER (boolean)
         * </P>
         */
        public static final String LOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.LOCKED : "locked";
    }

    /** M: MTK Add */
    public static final class SmsCb implements BaseColumns, TextBasedSmsCbColumns {

        public static final Cursor query(ContentResolver cr, String[] projection) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SmsCb.query(cr, projection);
            } else {
                return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
            }
        }

        public static final Cursor query(ContentResolver cr, String[] projection, String where,
                String orderBy) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SmsCb.query(cr, projection, where, orderBy);
            } else {
                return cr.query(CONTENT_URI, projection, where, null,
                        orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
            }

        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SmsCb.CONTENT_URI : Uri.parse("content://cb/messages");

        /**
         * The content:// style URL for "canonical_addresses" table
         */
        public static final Uri ADDRESS_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SmsCb.ADDRESS_URI : Uri.parse("content://cb/addresses");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SmsCb.DEFAULT_SORT_ORDER : "date DESC";

        /**
         * Add an SMS to the given URI with thread_id specified.
         * 
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param sim_id the id of the SIM card
         * @param channel_id the message identifier of the CB message
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param body the body of the message
         * @return the URI for the new message
         */
        public static Uri addMessageToUri(ContentResolver resolver, Uri uri, int sim_id,
                int channel_id, long date, boolean read, String body) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SmsCb.addMessageToUri(resolver, uri, sim_id, channel_id, date, read, body);
            } else {
                ContentValues values = new ContentValues(5);

                values.put(SIM_ID, Integer.valueOf(sim_id));
                values.put(DATE, Long.valueOf(date));
                values.put(READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
                values.put(BODY, body);
                values.put(CHANNEL_ID, Integer.valueOf(channel_id));

                return resolver.insert(uri, values);
            }
        }

        /**
         * Contains all received SMSCb messages in the SMS app's.
         */
        public static final class Conversations implements BaseColumns, TextBasedSmsCbColumns {
            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.CONTENT_URI : Uri.parse("content://cb/threads");

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.DEFAULT_SORT_ORDER :"date DESC";

            /**
             * The first 45 characters of the body of the message
             * <P>
             * Type: TEXT
             * </P>
             */
            public static final String SNIPPET = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.SNIPPET :"snippet";

            /**
             * The number of messages in the conversation
             * <P>
             * Type: INTEGER
             * </P>
             */
            public static final String MESSAGE_COUNT = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.MESSAGE_COUNT :"msg_count";

            /**
             * The _id of address table in the conversation
             * <P>
             * Type: INTEGER
             * </P>
             */
            public static final String ADDRESS_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.ADDRESS_ID :"address_id";
        }

        /**
         * Columns for the "canonical_addresses" table used by CB-SMS
         */
        public interface CanonicalAddressesColumns extends BaseColumns {
            /**
             * An address used in CB-SMS. Just a channel number
             * <P>
             * Type: TEXT
             * </P>
             */
            public static final String ADDRESS = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CanonicalAddressesColumns.ADDRESS :"address";
        }

        /**
         * Columns for the "canonical_addresses" table used by CB-SMS
         */
        public static final class CbChannel implements BaseColumns {
            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CbChannel.CONTENT_URI :Uri.parse("content://cb/channel");

            public static final String NAME = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CbChannel.NAME :"name";

            public static final String NUMBER = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CbChannel.NUMBER :"number";

            public static final String ENABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CbChannel.ENABLE :"enable";

        }

    }

    public interface BaseMmsColumns {
        /** M: MTK Add */
        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.BaseMmsColumns.SIM_ID : "sim_id";

        /** M: MTK Add */
        public static final String SERVICE_CENTER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.BaseMmsColumns.SERVICE_CENTER :"service_center";
    }

    public static final class Mms implements BaseMmsColumns {

        /** M: MTK Add */
        public static final class ScrapSpace {
            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Mms.ScrapSpace.CONTENT_URI :Uri.parse("content://mms/scrapSpace");

            /**
             * This is the scrap file we use to store the media attachment when
             * the user chooses to capture a photo to be attached . We pass
             * {#link@Uri} to the Camera app, which streams the captured image
             * to the uri. Internally we write the media content to this file.
             * It's named '.temp.jpg' so Gallery won't pick it up.
             */
            public static final String SCRAP_FILE_PATH = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Mms.ScrapSpace.SCRAP_FILE_PATH :"/sdcard/mms/scrapSpace/.temp.jpg";
        }
    }

    public static final class MmsSms {
        /** M: MTK Add */
        public static final Uri CONTENT_URI_QUICKTEXT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.MmsSms.CONTENT_URI_QUICKTEXT :Uri.parse("content://mms-sms/quicktext");

        /** M: MTK Add */
        public static final class PendingMessages {
            public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.MmsSms.PendingMessages.SIM_ID :"pending_sim_id";

        }
    }

    /** M: MTK Add */
    public static final class SimInfo implements BaseColumns {
        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.CONTENT_URI : Uri.parse("content://telephony/siminfo");

        public static final String DEFAULT_SORT_ORDER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DEFAULT_SORT_ORDER :"name ASC";

        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ICC_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.ICC_ID :"icc_id";

        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String DISPLAY_NAME = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DISPLAY_NAME :"display_name";

        public static final int DEFAULT_NAME_MIN_INDEX = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DEFAULT_NAME_MIN_INDEX :01;

        public static final int DEFAULT_NAME_MAX_INDEX = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DEFAULT_NAME_MAX_INDEX :99;

        public static final int DEFAULT_NAME_RES = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DEFAULT_NAME_RES :com.mediatek.internal.R.string.new_sim;

        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String NUMBER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.NUMBER :"number";

        /**
         * 0:none, 1:the first four digits, 2:the last four digits.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DISPLAY_NUMBER_FORMAT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DISPLAY_NUMBER_FORMAT :"display_number_format";

        public static final int DISPALY_NUMBER_NONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DISPALY_NUMBER_NONE :0;

        public static final int DISPLAY_NUMBER_FIRST = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DISPLAY_NUMBER_FIRST :1;

        public static final int DISPLAY_NUMBER_LAST = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DISPLAY_NUMBER_LAST :2;

        public static final int DISLPAY_NUMBER_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DISLPAY_NUMBER_DEFAULT :DISPLAY_NUMBER_FIRST;

        /**
         * Eight kinds of colors. 0-3 will represent the eight colors. Default
         * value: any color that is not in-use.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String COLOR = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.COLOR :"color";

        public static final int COLOR_1 = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.COLOR_1 :0;

        public static final int COLOR_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.COLOR_2 :1;

        public static final int COLOR_3 = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.COLOR_3 :2;

        public static final int COLOR_4 = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.COLOR_4 :3;

        public static final int COLOR_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.COLOR_DEFAULT :COLOR_1;

        /**
         * 0: Don't allow data when roaming, 1:Allow data when roaming
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DATA_ROAMING = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DATA_ROAMING :"data_roaming";

        public static final int DATA_ROAMING_ENABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DATA_ROAMING_ENABLE :1;

        public static final int DATA_ROAMING_DISABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DATA_ROAMING_DISABLE :0;

        public static final int DATA_ROAMING_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.DATA_ROAMING_DEFAULT :DATA_ROAMING_DISABLE;

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SLOT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.SLOT :"slot";

        public static final int SLOT_NONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.SLOT_NONE :-1;

        public static final int ERROR_GENERAL = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.ERROR_GENERAL :-1;

        public static final int ERROR_NAME_EXIST = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SimInfo.ERROR_NAME_EXIST :-2;

    }

    /** M: MTK Add */
    public static final int[] SIMBackgroundRes = new int[] {
        com.mediatek.internal.R.drawable.sim_background_blue,
        com.mediatek.internal.R.drawable.sim_background_orange,
        com.mediatek.internal.R.drawable.sim_background_green,
        com.mediatek.internal.R.drawable.sim_background_purple
    };

    // add by mtk02772 for Consistent UI Design start
    public static final int[] SIMBackgroundDarkRes = new int[] {
        com.mediatek.internal.R.drawable.sim_dark_blue,
        com.mediatek.internal.R.drawable.sim_dark_orange,
        com.mediatek.internal.R.drawable.sim_dark_green,
        com.mediatek.internal.R.drawable.sim_dark_purple
    };

    public static final int[] SIMBackgroundLightRes = new int[] {
        com.mediatek.internal.R.drawable.sim_light_blue,
        com.mediatek.internal.R.drawable.sim_light_orange,
        com.mediatek.internal.R.drawable.sim_light_green,
        com.mediatek.internal.R.drawable.sim_light_purple
    };
    // add by mtk02772 for Consistent UI Design end

    /** M: MTK Add */
    public static class SIMInfo {
        private long mSimId;

        private String mICCId;

        private String mDisplayName = "";

        private String mNumber = "";

        private int mDispalyNumberFormat = SimInfo.DISLPAY_NUMBER_DEFAULT;

        private int mColor;

        private int mDataRoaming = SimInfo.DATA_ROAMING_DEFAULT;

        private int mSlot = SimInfo.SLOT_NONE;

        private int mSimBackgroundRes = SIMBackgroundRes[SimInfo.COLOR_DEFAULT];

        // add by mtk02772 for Consistent UI Design start
        public int mSimBackgroundDarkRes = SIMBackgroundDarkRes[SimInfo.COLOR_DEFAULT];
        public int mSimBackgroundLightRes = SIMBackgroundLightRes[SimInfo.COLOR_DEFAULT];
        // add by mtk02772 for Consistent UI Design end

        private Telephony.SIMInfo mSIMInfo;

        private SIMInfo() {
        }

        public SIMInfo(Telephony.SIMInfo simInfo) {
            if (simInfo != null) {
                mSIMInfo = simInfo;
            }
        }

        public static class ErrorCode {
            public static final int ERROR_GENERAL = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SIMInfo.ErrorCode.ERROR_GENERAL: -1;

            public static final int ERROR_NAME_EXIST = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SIMInfo.ErrorCode.ERROR_NAME_EXIST: -2;
        }

        public long getSimId() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mSimId;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSimId()");
                return 0;
            }
        }

        public String getICCId() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mICCId;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getICCId()");
                return new String();
            }
        }

        public String getDisplayName() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mDisplayName;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getDisplayName()");
                return new String();
            }
        }

        public String getNumber() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mNumber;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getNumber()");
                return new String();
            }
        }

        public int getDispalyNumberFormat() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mDispalyNumberFormat;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getDispalyNumberFormat()");
                return SimInfo.DISLPAY_NUMBER_DEFAULT;
            }
        }

        public int getColor() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mColor;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getColor()");
                return 0;
            }
        }

        public int getDataRoaming() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mDataRoaming;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getDataRoaming()");
                return SimInfo.DATA_ROAMING_DEFAULT;
            }
        }

        public int getSlot() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mSlot;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSlot()");
                return SimInfo.SLOT_NONE;
            }
        }

        public int getSimBackgroundRes() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mSimBackgroundRes;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSimBackgroundRes()");
                return SIMBackgroundRes[SimInfo.COLOR_DEFAULT];
            }
        }

        public int getSimBackgroundDarkRes() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mSimBackgroundDarkRes;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSimBackgroundDarkRes()");
                return SIMBackgroundDarkRes[SimInfo.COLOR_DEFAULT];
            }
        }

        public int getSimBackgroundLightRes() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfo.mSimBackgroundLightRes;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSimBackgroundLightRes()");
                return SIMBackgroundLightRes[SimInfo.COLOR_DEFAULT];
            }
        }

        private static SIMInfo fromCursor(Cursor cursor) {
            SIMInfo info = new SIMInfo();
            info.mSimId = cursor.getLong(cursor.getColumnIndexOrThrow(SimInfo._ID));
            info.mICCId = cursor.getString(cursor.getColumnIndexOrThrow(SimInfo.ICC_ID));
            info.mDisplayName = cursor
                    .getString(cursor.getColumnIndexOrThrow(SimInfo.DISPLAY_NAME));
            info.mNumber = cursor.getString(cursor.getColumnIndexOrThrow(SimInfo.NUMBER));
            info.mDispalyNumberFormat = cursor.getInt(cursor
                    .getColumnIndexOrThrow(SimInfo.DISPLAY_NUMBER_FORMAT));
            info.mColor = cursor.getInt(cursor.getColumnIndexOrThrow(SimInfo.COLOR));
            info.mDataRoaming = cursor.getInt(cursor.getColumnIndexOrThrow(SimInfo.DATA_ROAMING));
            info.mSlot = cursor.getInt(cursor.getColumnIndexOrThrow(SimInfo.SLOT));
            int size = SIMBackgroundRes.length;
            if (info.mColor >= 0 && info.mColor < size) {
                info.mSimBackgroundRes = SIMBackgroundRes[info.mColor];

                // add by mtk02772 for Consistent UI Design start
                info.mSimBackgroundDarkRes = SIMBackgroundDarkRes[info.mColor];
                info.mSimBackgroundLightRes = SIMBackgroundLightRes[info.mColor];
                // add by mtk02772 for Consistent UI Design end
            }
            return info;
        }

        /**
         * @param ctx
         * @return the array list of Current SIM Info
         */
        public static List<SIMInfo> getInsertedSIMList(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                List<Telephony.SIMInfo> oldSimList = Telephony.SIMInfo.getInsertedSIMList(ctx);
                ArrayList<SIMInfo> newSimList = new ArrayList<SIMInfo>();
                for (int i = 0; i < oldSimList.size(); i++) {
                    newSimList.add(new SIMInfo(oldSimList.get(i)));
                }
                return newSimList;
            } else {
                ArrayList<SIMInfo> simList = new ArrayList<SIMInfo>();
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, null,
                        SimInfo.SLOT + "!=" + SimInfo.SLOT_NONE, null, null);
                try {
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            simList.add(SIMInfo.fromCursor(cursor));
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return simList;
            }
        }

        /**
         * @param ctx
         * @return array list of all the SIM Info include what were used before
         */
        public static List<SIMInfo> getAllSIMList(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                List<Telephony.SIMInfo> oldSimList = Telephony.SIMInfo.getAllSIMList(ctx);
                ArrayList<SIMInfo> newSimList = new ArrayList<SIMInfo>();
                for (int i = 0; i < oldSimList.size(); i++) {
                    newSimList.add(new SIMInfo(oldSimList.get(i)));
                }
                return newSimList;
            } else {
                ArrayList<SIMInfo> simList = new ArrayList<SIMInfo>();
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, null, null,
                        null, null);
                try {
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            simList.add(SIMInfo.fromCursor(cursor));
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return simList;
            }
        }

        /**
         * @param ctx
         * @param SIMId the unique SIM id
         * @return SIM-Info, maybe null
         */
        public static SIMInfo getSIMInfoById(Context ctx, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                Telephony.SIMInfo siminfo = Telephony.SIMInfo.getSIMInfoById(ctx, SIMId);
                if (siminfo == null) {
                    return null;
                } else {
                    return new SIMInfo(siminfo);
                }
            } else {
                if (SIMId <= 0)
                    return null;
                Cursor cursor = ctx.getContentResolver().query(
                        ContentUris.withAppendedId(SimInfo.CONTENT_URI, SIMId), null, null, null, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return SIMInfo.fromCursor(cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }
        }

        /**
         * @param ctx
         * @param SIMName the Name of the SIM Card
         * @return SIM-Info, maybe null
         */
        public static SIMInfo getSIMInfoByName(Context ctx, String SIMName) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                Telephony.SIMInfo siminfo = Telephony.SIMInfo.getSIMInfoByName(ctx, SIMName);
                if (siminfo == null) {
                    return null;
                } else {
                    return new SIMInfo(siminfo);
                }
            } else {
                if (SIMName == null)
                    return null;
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, null,
                        SimInfo.DISPLAY_NAME + "=?", new String[] {
                            SIMName
                        }, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return SIMInfo.fromCursor(cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }
        }

        /**
         * @param ctx
         * @param cardSlot
         * @return The SIM-Info, maybe null
         */
        public static SIMInfo getSIMInfoBySlot(Context ctx, int cardSlot) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                Telephony.SIMInfo siminfo = Telephony.SIMInfo.getSIMInfoBySlot(ctx, cardSlot);
                if (siminfo == null) {
                    return null;
                } else {
                    return new SIMInfo(siminfo);
                }
            } else {
                if (cardSlot < 0)
                    return null;
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, null,
                        SimInfo.SLOT + "=?", new String[] {
                            String.valueOf(cardSlot)
                        }, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return SIMInfo.fromCursor(cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }
        }

        /**
         * @param ctx
         * @param iccid
         * @return The SIM-Info, maybe null
         */
        public static SIMInfo getSIMInfoByICCId(Context ctx, String iccid) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                Telephony.SIMInfo siminfo =Telephony.SIMInfo.getSIMInfoByICCId(ctx, iccid);
                if (siminfo == null) {
                    return null;
                } else {
                    return new SIMInfo(siminfo);
                }
            } else {
                if (iccid == null)
                    return null;
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, null,
                        SimInfo.ICC_ID + "=?", new String[] {
                            iccid
                        }, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return SIMInfo.fromCursor(cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }
        }

        /**
         * @param ctx
         * @param SIMId
         * @return the slot of the SIM Card, -1 indicate that the SIM card is
         *         missing
         */
        public static int getSlotById(Context ctx, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.getSlotById(ctx, SIMId);
            } else {
                if (SIMId <= 0)
                    return SimInfo.SLOT_NONE;
                Cursor cursor = ctx.getContentResolver().query(
                        ContentUris.withAppendedId(SimInfo.CONTENT_URI, SIMId), new String[] {
                            SimInfo.SLOT
                        }, null, null, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return cursor.getInt(0);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return SimInfo.SLOT_NONE;
            }
        }

        /**
         * @param ctx
         * @param SIMId
         * @return the id of the SIM Card, 0 indicate that no SIM card is
         *         inserted
         */
        public static long getIdBySlot(Context ctx, int slot) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.getIdBySlot(ctx, slot);
            } else {
                SIMInfo simInfo = getSIMInfoBySlot(ctx, slot);
                if (simInfo != null)
                    return simInfo.mSimId;
                return 0;
            }
        }

        /**
         * @param ctx
         * @param SIMName
         * @return the slot of the SIM Card, -1 indicate that the SIM card is
         *         missing
         */
        public static int getSlotByName(Context ctx, String SIMName) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.getSlotByName(ctx, SIMName);
            } else {
                if (SIMName == null)
                    return SimInfo.SLOT_NONE;
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, new String[] {
                    SimInfo.SLOT
                }, SimInfo.DISPLAY_NAME + "=?", new String[] {
                    SIMName
                }, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return cursor.getInt(0);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return SimInfo.SLOT_NONE;
            }
        }

        /**
         * @param ctx
         * @return current SIM Count
         */
        public static int getInsertedSIMCount(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.getInsertedSIMCount(ctx);
            } else {
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, null,
                        SimInfo.SLOT + "!=" + SimInfo.SLOT_NONE, null, null);
                try {
                    if (cursor != null) {
                        return cursor.getCount();
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return 0;
            }
        }

        /**
         * @param ctx
         * @return the count of all the SIM Card include what was used before
         */
        public static int getAllSIMCount(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.getAllSIMCount(ctx);
            } else {
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, null, null,
                        null, null);
                try {
                    if (cursor != null) {
                        return cursor.getCount();
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return 0;
            }
        }

        /**
         * set display name by SIM ID
         * 
         * @param ctx
         * @param displayName
         * @param SIMId
         * @return -1 means general error, -2 means the name is exist. >0 means
         *         success
         */
        public static int setDisplayName(Context ctx, String displayName, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.setDisplayName(ctx, displayName, SIMId);
            } else {
                if (displayName == null || SIMId <= 0)
                    return ErrorCode.ERROR_GENERAL;
                Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, new String[] {
                    SimInfo._ID
                }, SimInfo.DISPLAY_NAME + "=?", new String[] {
                    displayName
                }, null);
                try {
                    if (cursor != null) {
                        if (cursor.getCount() > 0) {
                            return ErrorCode.ERROR_NAME_EXIST;
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                ContentValues value = new ContentValues(1);
                value.put(SimInfo.DISPLAY_NAME, displayName);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfo.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * @param ctx
         * @param number
         * @param SIMId
         * @return >0 means success
         */
        public static int setNumber(Context ctx, String number, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.setNumber(ctx, number, SIMId);
            } else {
                if (number == null || SIMId <= 0)
                    return -1;
                ContentValues value = new ContentValues(1);
                value.put(SimInfo.NUMBER, number);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfo.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * @param ctx
         * @param color
         * @param SIMId
         * @return >0 means success
         */
        public static int setColor(Context ctx, int color, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.setColor(ctx, color, SIMId);
            } else {
                int size = SIMBackgroundRes.length;
                if (color < 0 || SIMId <= 0 || color >= size)
                    return -1;
                ContentValues value = new ContentValues(1);
                value.put(SimInfo.COLOR, color);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfo.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * set the format.0: none, 1: the first four digits, 2: the last four
         * digits.
         * 
         * @param ctx
         * @param format
         * @param SIMId
         * @return >0 means success
         */
        public static int setDispalyNumberFormat(Context ctx, int format, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.setDispalyNumberFormat(ctx, format, SIMId);
            } else {
                if (format < 0 || SIMId <= 0)
                    return -1;
                ContentValues value = new ContentValues(1);
                value.put(SimInfo.DISPLAY_NUMBER_FORMAT, format);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfo.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * set data roaming.0:Don't allow data when roaming, 1:Allow data when
         * roaming
         * 
         * @param ctx
         * @param roaming
         * @param SIMId
         * @return >0 means success
         */
        public static int setDataRoaming(Context ctx, int roaming, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.setDataRoaming(ctx, roaming, SIMId);
            } else {
                if (roaming < 0 || SIMId <= 0)
                    return -1;
                ContentValues value = new ContentValues(1);
                value.put(SimInfo.DATA_ROAMING, roaming);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfo.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * Insert the ICC ID and slot if needed
         * 
         * @param ctx
         * @param ICCId
         * @param slot
         * @return
         */
        public static Uri insertICCId(Context ctx, String ICCId, int slot) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.insertICCId(ctx, ICCId, slot);
            } else {
                if (ICCId == null) {
                    throw new IllegalArgumentException("ICCId should not null.");
                }
                Uri uri;
                ContentResolver resolver = ctx.getContentResolver();
                String selection = SimInfo.ICC_ID + "=?";
                Cursor cursor = resolver.query(SimInfo.CONTENT_URI, new String[] {
                        SimInfo._ID, SimInfo.SLOT
                }, selection, new String[] {
                    ICCId
                }, null);
                try {
                    if (cursor == null || !cursor.moveToFirst()) {
                        ContentValues values = new ContentValues();
                        values.put(SimInfo.ICC_ID, ICCId);
                        values.put(SimInfo.COLOR, -1);
                        values.put(SimInfo.SLOT, slot);
                        uri = resolver.insert(SimInfo.CONTENT_URI, values);
                        // setDefaultName(ctx, ContentUris.parseId(uri), null);
                    } else {
                        long simId = cursor.getLong(0);
                        int oldSlot = cursor.getInt(1);
                        uri = ContentUris.withAppendedId(SimInfo.CONTENT_URI, simId);
                        if (slot != oldSlot) {
                            ContentValues values = new ContentValues(1);
                            values.put(SimInfo.SLOT, slot);
                            resolver.update(uri, values, null, null);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                return uri;
            }
        }

        public static int setDefaultName(Context ctx, long simId, String name) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SIMInfo.setDefaultName(ctx, simId, name);
            } else {
                if (simId <= 0)
                    return ErrorCode.ERROR_GENERAL;
                String default_name = ctx.getString(SimInfo.DEFAULT_NAME_RES);
                ContentResolver resolver = ctx.getContentResolver();
                Uri uri = ContentUris.withAppendedId(SimInfo.CONTENT_URI, simId);
                if (name != null) {
                    int result = setDisplayName(ctx, name, simId);
                    if (result > 0) {
                        return result;
                    }
                }
                int index = getAppropriateIndex(ctx, simId, name);
                String suffix = getSuffixFromIndex(index);
                ContentValues value = new ContentValues(1);
                String display_name = (name == null ? default_name + " " + suffix : name + " "
                        + suffix);
                value.put(SimInfo.DISPLAY_NAME, display_name);
                return ctx.getContentResolver().update(uri, value, null, null);
            }
        }

        private static String getSuffixFromIndex(int index) {
            if (index < 10) {
                return "0" + index;
            } else {
                return String.valueOf(index);
            }
        }

        private static int getAppropriateIndex(Context ctx, long simId, String name) {
            String default_name = ctx.getString(SimInfo.DEFAULT_NAME_RES);
            StringBuilder sb = new StringBuilder(SimInfo.DISPLAY_NAME + " LIKE ");
            if (name == null) {
                DatabaseUtils.appendEscapedSQLString(sb, default_name + '%');
            } else {
                DatabaseUtils.appendEscapedSQLString(sb, name + '%');
            }
            sb.append(" AND (");
            sb.append(SimInfo._ID + "!=" + simId);
            sb.append(")");

            Cursor cursor = ctx.getContentResolver().query(SimInfo.CONTENT_URI, new String[] {
                    SimInfo._ID, SimInfo.DISPLAY_NAME
            }, sb.toString(), null, SimInfo.DISPLAY_NAME);
            ArrayList<Long> array = new ArrayList<Long>();
            int index = SimInfo.DEFAULT_NAME_MIN_INDEX;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String display_name = cursor.getString(1);

                    if (display_name != null) {
                        int length = display_name.length();
                        if (length >= 2) {
                            String sub = display_name.substring(length - 2);
                            if (TextUtils.isDigitsOnly(sub)) {
                                long value = Long.valueOf(sub);
                                array.add(value);
                            }
                        }
                    }
                }
                cursor.close();
            }
            for (int i = SimInfo.DEFAULT_NAME_MIN_INDEX; i <= SimInfo.DEFAULT_NAME_MAX_INDEX; i++) {
                if (array.contains((long) i)) {
                    continue;
                } else {
                    index = i;
                    break;
                }
            }
            return index;
        }
    }

    /**
     * Base columns for tables that contain text based SMSs.
     */
    public interface TextBasedSmsColumns {
        /** M: MTK Add */
        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsColumns.SIM_ID : "sim_id";

    }

    /**
     * Contains all text based SMS messages.
     */
    public static final class Sms implements BaseColumns, TextBasedSmsColumns {

        /// M: add for ip message
        public static final String IPMSG_ID = "ipmsg_id";

        /** M: MTK Add */
        /**
         * Add an SMS to the given URI with thread_id specified.
         * 
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param address the address of the sender
         * @param body the body of the message
         * @param subject the psuedo-subject of the message
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param deliveryReport true if a delivery report was requested, false
         *            if not
         * @param threadId the thread_id of the message
         * @param simId the sim_id of the message
         * @return the URI for the new message
         */
        public static Uri addMessageToUri(ContentResolver resolver, Uri uri, String address,
                String body, String subject, Long date, boolean read, boolean deliveryReport,
                long threadId, int simId) {
            return addMessageToUri(resolver, uri, address, body, subject, null, date, read,
                    deliveryReport, threadId, simId);
        }

        /** M: MTK Add */
        /**
         * Add an SMS to the given URI with thread_id specified.
         * 
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param address the address of the sender
         * @param body the body of the message
         * @param subject the psuedo-subject of the message
         * @param sc the service center of the message
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param deliveryReport true if a delivery report was requested, false
         *            if not
         * @param threadId the thread_id of the message
         * @param simId the sim_id of the message
         * @return the URI for the new message
         */
        public static Uri addMessageToUri(ContentResolver resolver, Uri uri, String address,
                String body, String subject, String sc, Long date, boolean read,
                boolean deliveryReport, long threadId, int simId) {
            ContentValues values = new ContentValues(8);

            values.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
            if (date != null) {
                values.put(Telephony.TextBasedSmsColumns.DATE, date);
            }
            if (sc != null) {
                values.put(Telephony.TextBasedSmsColumns.SERVICE_CENTER, sc);
            }
            values.put(Telephony.TextBasedSmsColumns.READ, read ? Integer.valueOf(1) : Integer
                    .valueOf(0));
            values.put(Telephony.TextBasedSmsColumns.SUBJECT, subject);
            values.put(Telephony.TextBasedSmsColumns.BODY, body);
            values.put(Telephony.TextBasedSmsColumns.SEEN, read ? Integer.valueOf(1) : Integer
                    .valueOf(0));
            if (deliveryReport) {
                values.put(Telephony.TextBasedSmsColumns.STATUS,
                        Telephony.TextBasedSmsColumns.STATUS_PENDING);
            }
            if (threadId != -1L) {
                values.put(Telephony.TextBasedSmsColumns.THREAD_ID, threadId);
            }

            if (simId != -1) {
                values.put(SIM_ID, simId);
            }

            return resolver.insert(uri, values);
        }

        public static final class Inbox implements BaseColumns, TextBasedSmsColumns {
            /** M: MTK Add */
            public static Uri addMessage(ContentResolver resolver, String address, String body,
                    String subject, String sc, Long date, boolean read, int simId) {
                return addMessageToUri(resolver, Telephony.Sms.Inbox.CONTENT_URI, address, body,
                        subject, sc, date, read, false, -1L, simId);
            }
        }

        public static final class Sent implements BaseColumns, TextBasedSmsColumns {
            /** M: MTK Add */
            public static Uri addMessage(ContentResolver resolver, String address, String body,
                    String subject, String sc, Long date, int simId) {
                return addMessageToUri(resolver, Telephony.Sms.Sent.CONTENT_URI, address, body,
                        subject, sc, date, true, false, -1L, simId);
            }
        }

    }

    public interface ThreadsColumns extends BaseColumns {

        /** M: MTK Add */
        /**
         * The read message count of the thread.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String READCOUNT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadsColumns.READCOUNT : "readcount";
    }

    public static final class Threads implements ThreadsColumns {

        private static final String[] ID_PROJECTION = { BaseColumns._ID };
        private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");

        /** M: MTK Add */
        public static final int WAPPUSH_THREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.WAPPUSH_THREAD :2;

        /** M: MTK Add */
        public static final int CELL_BROADCAST_THREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.CELL_BROADCAST_THREAD :3;

        /** M: MTK Add */
        /**
         * Whether a thread is being writen or not 0: normal 1: being writen
         * <P>
         * Type: INTEGER (boolean)
         * </P>
         */
        public static final String STATUS = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.STATUS :"status";

        /** M: MTK Add */
        /**
         * This is a single-recipient version of getOrCreateThreadId. It's used
         * for internal
         */
        public static long getOrCreateThreadIdInternal(Context context, String recipient) {
            Set<String> recipients = new HashSet<String>();
            recipients.add(recipient);
            // only create a thread with status 1
            return getOrCreateThreadIdInternal(context, recipients);
        }

        /** M: MTK Add */
        /**
         * Given the recipients list and subject of an unsaved message, return
         * its thread ID. It's used for internal.
         */
        public static long getOrCreateThreadIdInternal(Context context, Set<String> recipients) {
            Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();

            for (String recipient : recipients) {
                if (Telephony.Mms.isEmailAddress(recipient)) {
                    recipient = Telephony.Mms.extractAddrSpec(recipient);
                }

                uriBuilder.appendQueryParameter("recipient", recipient);
            }

            Uri uri = uriBuilder.build();
            // if (DEBUG) Log.v(TAG, "getOrCreateThreadId uri: " + uri);

            Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), uri,
                    ID_PROJECTION, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0);
                    } else {
                        Log.e(TAG, "getOrCreateThreadId returned no rows!");
                    }
                } finally {
                    cursor.close();
                }
            }

            Log.e(TAG, "getOrCreateThreadId failed with uri " + uri.toString());
            throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
        }
    }

    /** M: MTK Add for ip message */
    public static final class ThreadSettings implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                Telephony.MmsSms.CONTENT_URI, "thread_settings");

        /**
         * Whether a thread is set notification enabled
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String NOTIFICATION_ENABLE = "notification_enable";

        /**
         * Which thread does this settings belongs to
         * <P>Type: INTEGER </P>
         */
        public static final String THREAD_ID = "thread_id";

        /**
         * Whether a thread is set spam
         * 0: normal 1: spam
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String SPAM = "spam";

        /**
         * Whether a thread is set mute
         * 0: normal >1: mute duration
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String MUTE = "mute";

        /**
         * when does a thread be set mute
         * 0: normal >1: mute start time
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String MUTE_START = "mute_start";

        /**
         * Whether a thread is set vibrate
         * 0: normal 1: vibrate
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * Ringtone for a thread
         * <P>Type: STRING</P>
         */
        public static final String RINGTONE = "ringtone";

        /**
         * Wallpaper for a thread
         * <P>Type: STRING</P>
         */
        public static final String WALLPAPER = "_data";
    }

    /** M: MTK Add */
    public static final class WapPush implements BaseColumns {

        // public static final Uri CONTENT_URI =
        public static final String DEFAULT_SORT_ORDER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.DEFAULT_SORT_ORDER :"date ASC";

        public static final Uri CONTENT_URI =EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CONTENT_URI : Uri.parse("content://wappush");

        public static final Uri CONTENT_URI_SI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CONTENT_URI_SI :Uri.parse("content://wappush/si");

        public static final Uri CONTENT_URI_SL = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CONTENT_URI_SL :Uri.parse("content://wappush/sl");

        public static final Uri CONTENT_URI_THREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CONTENT_URI_THREAD :Uri.parse("content://wappush/thread_id");

        // Database Columns
        public static final String THREAD_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.THREAD_ID :"thread_id";

        public static final String ADDR = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.ADDR :"address";

        public static final String SERVICE_ADDR = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.SERVICE_ADDR :"service_center";

        public static final String READ = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.READ :"read";

        public static final String SEEN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.SEEN :"seen";

        public static final String LOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.LOCKED :"locked";

        public static final String ERROR = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.ERROR :"error";

        public static final String DATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.DATE :"date";

        public static final String TYPE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.TYPE :"type";

        public static final String SIID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.SIID :"siid";

        public static final String URL = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.URL :"url";

        public static final String CREATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CREATE :"created";

        public static final String EXPIRATION = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.EXPIRATION :"expiration";

        public static final String ACTION = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.ACTION :"action";

        public static final String TEXT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.TEXT :"text";

        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.SIM_ID :"sim_id";

        //

        public static final int TYPE_SI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.TYPE_SI :0;

        public static final int TYPE_SL = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.TYPE_SL :1;

        public static final int STATUS_SEEN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_SEEN :1;

        public static final int STATUS_UNSEEN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_UNSEEN :0;

        public static final int STATUS_READ = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_READ :1;

        public static final int STATUS_UNREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_UNREAD :0;

        public static final int STATUS_LOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_LOCKED :1;

        public static final int STATUS_UNLOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_UNLOCKED :0;
    }

    public static final class Carriers implements BaseColumns {

        public static final Uri CONTENT_URI_DM = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.CONTENT_URI_DM :Uri.parse("content://telephony/carriers_dm");

        public static final Uri CONTENT_URI_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.CONTENT_URI_2 :Uri.parse("content://telephony/carriers2");

        public static final String OMACPID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.OMACPID :"omacpid";

        public static final String NAPID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.NAPID :"napid";

        public static final String PROXYID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.PROXYID :"proxyid";

        public static final String SOURCE_TYPE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.SOURCE_TYPE :"sourcetype";

        public static final String CSD_NUM = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.CSD_NUM :"csdnum";

        public static final String SPN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.SPN :"spn";

        public static final String IMSI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.IMSI :"imsi";

        public static final class GeminiCarriers {
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Carriers.GeminiCarriers.CONTENT_URI :
                           Uri.parse("content://telephony/carriers_gemini");

            public static final Uri CONTENT_URI_DM = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Carriers.GeminiCarriers.CONTENT_URI_DM :
                           Uri.parse("content://telephony/carriers_dm_gemini");
        }

        public static final class SIM1Carriers {
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Carriers.SIM1Carriers.CONTENT_URI :
                           Uri.parse("content://telephony/carriers_sim1");
        }

        public static final class SIM2Carriers {
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Carriers.SIM2Carriers.CONTENT_URI :
                           Uri.parse("content://telephony/carriers_sim2");
        }

    }

    /** M: MTK Add */
    public static final class GprsInfo implements BaseColumns {
        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.GprsInfo.CONTENT_URI :Uri.parse("content://telephony/gprsinfo");

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.GprsInfo.SIM_ID :"sim_id";

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String GPRS_IN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.GprsInfo.GPRS_IN :"gprs_in";

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String GPRS_OUT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.GprsInfo.GPRS_OUT :"gprs_out";
    }
}
