package com.mediatek.contacts.dialpad;

import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.contacts.util.SimContactPhotoUtils;
import com.mediatek.phone.SIMInfoWrapper;

import java.util.ArrayList;
import java.util.HashSet;

public class DialerSearchUtils {
    
    private static final String TAG = "DialerSearchUtils";
    
    private static final HashSet<Character> HYPHON_CHARACTERS = new HashSet<Character>();
    static {
        HYPHON_CHARACTERS.add(' ');
        HYPHON_CHARACTERS.add('-');
        HYPHON_CHARACTERS.add('(');
        HYPHON_CHARACTERS.add(')');
    }

    private static SIMInfoWrapper sSimInfoWrapper;

    public static String tripHyphen(String number) {
        if (TextUtils.isEmpty(number)) {
            return number;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (!HYPHON_CHARACTERS.contains(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String tripNonDigit(String number) {
        if (TextUtils.isEmpty(number)) {
            return number;
        }

        StringBuilder sb = new StringBuilder();
        int len = number.length();

        for (int i = 0; i < len; i++) {
            char c = number.charAt(i);
            if (PhoneNumberUtils.isNonSeparator(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static long getSimType(int indicate, int isSdnContact) {
        long photoId = 0;
        if (sSimInfoWrapper == null) {
            sSimInfoWrapper = SIMInfoWrapper.getDefault();
        }

        final int slot = sSimInfoWrapper.getSimSlotById(indicate);
        Log.i(TAG, "[getSimType] mSlot = " + slot);

        int i = -1;
        SIMInfo simInfoForColor = sSimInfoWrapper.getSimInfoBySlot(slot);
        if (simInfoForColor != null) {
            i = simInfoForColor.mColor;
        }
        Log.i(TAG, "[getSimType] i = " + i + " | isSdnContact : " + isSdnContact);
        photoId = new SimContactPhotoUtils().getPhotoId(isSdnContact, i);

        return photoId;
    }
        
    

    public static ArrayList<Integer> adjustHighlitePositionForHyphen(String number,
            String numberMatchedOffsets, String originNumber) {
        ArrayList<Integer> res = new ArrayList<Integer>();
        try {
            int highliteBegin = (int) numberMatchedOffsets.charAt(0);
            int highliteEnd = (int) numberMatchedOffsets.charAt(1);
            int originNumberBegin = 0;
            String targetTemp = "";
            for (int i = 0; i < number.length(); i++) {
                char c = number.charAt(i);
                if (HYPHON_CHARACTERS.contains(c)) {
                    continue;
                }
                targetTemp += c;
            }
            originNumberBegin = originNumber.indexOf(targetTemp);

            if (highliteBegin > highliteEnd) {
                return res;
            }

            if ((originNumberBegin >= highliteEnd) && highliteEnd >= 1) {
                highliteEnd = 0;
            }

            if (highliteEnd > originNumberBegin) {
                highliteEnd = highliteEnd - originNumberBegin;
            }

            if (highliteBegin >= originNumberBegin) {
                highliteBegin = highliteBegin - originNumberBegin;
            }

            for (int i = 0; i <= highliteBegin; i++) {
                char c = number.charAt(i);
                if (HYPHON_CHARACTERS.contains(c)) {
                    highliteBegin++;
                    highliteEnd++;
                }
            }

            for (int i = highliteBegin + 1; (i <= highliteEnd && i < number.length()); i++) {
                char c = number.charAt(i);
                if (HYPHON_CHARACTERS.contains(c)) {
                    highliteEnd++;
                }
            }

            if (highliteEnd >= number.length()) {
                highliteEnd = number.length() - 1;
            }
            res.add(highliteBegin);
            res.add(highliteEnd);
        } catch (Exception e) {
            Log.i(TAG, "number = " + number + " numberMatchedOffsets = " + numberMatchedOffsets
                    + " originNumber = " + originNumber);
            e.printStackTrace();
            return null;
        }
        return res;
    }
    
    
}
