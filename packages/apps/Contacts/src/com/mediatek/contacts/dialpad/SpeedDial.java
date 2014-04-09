package com.mediatek.contacts.dialpad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.activities.SpeedDialManageActivity;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.phone.SIMInfoWrapper;

public class SpeedDial {

    private static final String TAG = "SpeedDial";

    protected Context mContext;

    SharedPreferences mPreferences;

    public SpeedDial(Context context) {
        mContext = context;

        mPreferences = mContext.getSharedPreferences(SpeedDialManageActivity.PREF_NAME,
                Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
    }

    protected String getSpeedDialNumber(int key) {
        return mPreferences.getString(String.valueOf(key), "");
    }

    protected boolean clearSharedPreferences(int key, String number) {
        boolean notReady = false;

        if (MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)) {
            Log.i(TAG, "delete or copy is processing ");
            Toast.makeText(mContext, R.string.phone_book_busy, Toast.LENGTH_SHORT).show();
            notReady = true;
            return notReady;
        }

        Cursor phoneCursor = mContext.getContentResolver().query(
                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                new String[] {
                    PhoneLookup._ID
                }, null, null, null);

        if (phoneCursor == null || phoneCursor.getCount() == 0) {
            log("clear preferences");
            int numOffset = SpeedDialManageActivity.offset(key);
            int simId = mPreferences.getInt(String.valueOf(numOffset), -1);
            final SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getDefault();
            int slotId = simInfoWrapper.getSimSlotById(simId);
            if (slotId == -1 || SimCardUtils.isSimStateReady(slotId)) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(String.valueOf(key), "");

                editor.putInt(String.valueOf(numOffset), -1);
                editor.apply();
                notReady = true;
            } else if (!SimCardUtils.isSimStateReady(slotId)) {
                notReady = true;
            }
            if (phoneCursor != null) {
                phoneCursor.close();
            }
        }

        if (phoneCursor != null) {
            phoneCursor.close();
        }
        log("clear preferences canUse" + notReady);
        return notReady;
    }

    public boolean dial(int key) {
        final String number = getSpeedDialNumber(key);
        log("dial, key = " + key + " number = " + number);
        if (TextUtils.isEmpty(number)) {
            return false;
        }

        // Easy porting
        boolean reslut = ExtensionManager.getInstance().getSpeedDialExtension()
                .needClearSharedPreferences(ContactPluginDefault.COMMD_FOR_OP01);
        if (reslut) {
            if (clearSharedPreferences(key, number)) {
                return false;
            }
        }
        // eaty porting
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel",
                number, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

        return true;
    }

    void log(String msg) {
        Log.d(TAG, msg);
    }
}
