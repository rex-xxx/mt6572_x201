package com.mediatek.phone;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class BlackListManager {

    private static final String TAG = "BlackListManager";
    private static final boolean DBG = true;

    public static final int VOICE_CALL_REJECT_MODE = 0;
    public static final int VIDEO_CALL_REJECT_MODE = 1;

    private static final String VOICE_CALL_REJECT_MODE_KEY = Settings.System.VOICE_CALL_REJECT_MODE;
    private static final String VIDEO_CALL_REJECT_MODE_KEY = Settings.System.VT_CALL_REJECT_MODE;

    private static final int OFF = 0;
    private static final int ALL_NUMBERS = 1;
    private static final int AUTO_REJECT = 2;

    private static final int DO_NOT_BLOCK = 0;
    private static final int BLOCK_VOICE_CALL = 1;
    private static final int BLOCK_VIDEO_CALL = 2;
    private static final int BLOCK_VOICE_AND_VIDEO_CALL = 3;

    private static final String[] BLACK_LIST_PROJECTION = {
        "Number",
        "Type"
    };

    private static final String BLACK_LIST_URI = "content://reject/list";

    protected Context mContext;

    /**
     * Construtor for BlackLisManager
     * @param context the context which use for query database
     */
    public BlackListManager(Context context) {
        mContext = context;
    }

    /**
     * Log the message
     * @param msg the message will be println
     */
    void log(String msg) {
        Log.d(TAG, msg);
    }

    /**
     * get the block mode
     * @param type reject type(all reject or voice call reject or video call reject)
     * @return the mode that current setting
     */
    public int getBlockMode(int type) {
        final String key = type == VOICE_CALL_REJECT_MODE ? VOICE_CALL_REJECT_MODE_KEY : VIDEO_CALL_REJECT_MODE_KEY;
        try {
            final int mode =  Settings.System.getInt(mContext.getContentResolver(), key);
            return mode;
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }

        return OFF;
    }

    /**
     * check if the call should be rejected
     * @param number the incoming call number
     * @param type reject type
     * @return the result that the current number should be auto reject
     */
    public boolean autoReject(String number, int type) {
        Cursor cursor = mContext.getContentResolver().query(Uri.parse(BLACK_LIST_URI),
                BLACK_LIST_PROJECTION, null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor == null) {
            if (DBG) {
                log("cursor is null...");
            }
            return false;
        }

        String blockNumber;
        int blockType;
        boolean result = false;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            blockNumber = cursor.getString(0);
            if (PhoneNumberUtils.compare(number,blockNumber)) {
               // get it
                blockType = Integer.parseInt(cursor.getString(1));
                if (DBG) {
                    log("blockType = " + blockType);
                }

                if (blockType == BLOCK_VOICE_AND_VIDEO_CALL) {
                    result = true;
                    break;
                }

                if (type == VOICE_CALL_REJECT_MODE && blockType == BLOCK_VOICE_CALL) {
                    result = true;
                    break;
                }

                if (type == VIDEO_CALL_REJECT_MODE && blockType == BLOCK_VIDEO_CALL) {
                    result = true;
                    break;
                }
            }
            cursor.moveToNext();
        }

        cursor.close();
        return result;
    }

    /**
     * check the block number with give type
     * @param number the phone number
     * @param type reject type
     * @return the number with type should be rejected
     */
    public boolean shouldBlock(String number, int type) {
        int mode = getBlockMode(type);
        if (DBG) {
            log("shouldBlock, number = " + number + " type = " + type + " mode = " + mode);
        }

        if (mode == OFF) {
            return false;
        }

        if (mode == ALL_NUMBERS) {
            return true;
        }

        return autoReject(number, type);
    }
}
