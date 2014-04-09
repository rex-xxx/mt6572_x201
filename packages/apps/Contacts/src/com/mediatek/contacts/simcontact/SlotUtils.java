package com.mediatek.contacts.simcontact;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.contacts.R;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * M: [Gemini+] slot helper class. all slot related method placed here.
 */
public final class SlotUtils {

    private static final int PHONE_SLOT_NUM = PhoneConstants.GEMINI_SIM_NUM;
    private static final int FIRST_SLOT_ID = PhoneConstants.GEMINI_SIM_1;
    private static final String TAG = SlotUtils.class.getSimpleName();

    private SlotUtils() {
    }

    /**
     * M: [Gemini+] each slot information defined in this class
     */
    private static final class SlotInfo {

        private static final String SIM_PHONE_BOOK_SERVICE_NAME_FOR_SINGLE_SLOT = "simphonebook";
        private static final String ICC_SDN_URI_FOR_SINGLE_SLOT = "content://icc/sdn";
        private static final String ICC_ADN_URI_FOR_SINGLE_SLOT = "content://icc/adn";
        private static final String ICC_PBR_URI_FOR_SINGLE_SLOT = "content://icc/pbr";

        int mSlotId;
        Uri mIccUri;
        Uri mIccUsimUri;
        Uri mSdnUri;
        String mVoiceMailNumber;
        String mSimPhoneBookServiceName;
        boolean mIsSlotServiceRunning = false;
        int mResId;

        public SlotInfo(int slotId) {
            mSlotId = slotId;
            generateIccUri();
            generateIccUsimUri();
            generateSdnUri();
            generateSimPhoneBook();
            updateVoiceMailNumber();
            generateResId();
        }

        /**
         * TODO: the resource should be limited to only one string
         */
        private void generateResId() {
            switch (mSlotId) {
            case 0:
                mResId = R.string.sim1;
                break;
            case 1:
                mResId = R.string.sim2;
                break;
            case 2:
                mResId = R.string.sim3;
                break;
            case 3:
                mResId = R.string.sim4;
                break;
            default:
                Log.e(TAG, "generateResId, no res for slot " + mSlotId);
            }
        }

        /**
         * slot 0 ==> simphonebook slot 1 ==> simphonebook2
         */
        private void generateSimPhoneBook() {
            mSimPhoneBookServiceName = SIM_PHONE_BOOK_SERVICE_NAME_FOR_SINGLE_SLOT;
            if (mSlotId > 0) {
                mSimPhoneBookServiceName = mSimPhoneBookServiceName + (mSlotId + 1);
            }
        }

        public String getSimPhoneBookServiceName() {
            return mSimPhoneBookServiceName;
        }

        public void updateVoiceMailNumber() {
            if (SlotUtils.isGeminiEnabled()) {
                mVoiceMailNumber = TelephonyManagerEx.getDefault().getVoiceMailNumber(mSlotId);
            } else {
                mVoiceMailNumber = TelephonyManager.getDefault().getVoiceMailNumber();
            }
        }

        public String getVoiceMailNumber() {
            return mVoiceMailNumber;
        }

        private void generateSdnUri() {
            String str = ICC_SDN_URI_FOR_SINGLE_SLOT;
            if (isGeminiEnabled()) {
                // like:"content://icc/sdn2"
                str += (mSlotId + 1);
            }
            mSdnUri = Uri.parse(str);
        }

        private void generateIccUri() {
            String str = ICC_ADN_URI_FOR_SINGLE_SLOT;
            if (isGeminiEnabled()) {
                // like:"content://icc/adn2"
                str += (mSlotId + 1);
            }
            mIccUri = Uri.parse(str);
        }

        private void generateIccUsimUri() {
            String str = ICC_PBR_URI_FOR_SINGLE_SLOT;
            if (isGeminiEnabled()) {
                // like:"content://icc/pbr2"
                str += (mSlotId + 1);
            }
            mIccUsimUri = Uri.parse(str);
        }

        public void updateSimServiceRunningState(boolean isRunning) {
            Log.i(TAG, "slot " + mSlotId + " service running state changed from " + mIsSlotServiceRunning + " to "
                    + isRunning);
            mIsSlotServiceRunning = isRunning;
        }

        public boolean isSimServiceRunning() {
            return mIsSlotServiceRunning;
        }

        public Uri getIccUri() {
            return SimCardUtils.isSimUsimType(mSlotId) ? mIccUsimUri : mIccUri;
        }

        public Uri getSdnUri() {
            return mSdnUri;
        }

        public int getResId() {
            return mResId;
        }
    }

    @SuppressLint("UseSparseArrays")
    private static Map<Integer, SlotInfo> sSlotInfoMap = new HashMap<Integer, SlotInfo>();
    static {
        for (int i = 0; i < PHONE_SLOT_NUM; i++) {
            int slotId = FIRST_SLOT_ID + i;
            sSlotInfoMap.put(slotId, new SlotInfo(slotId));
        }
    }

    /**
     * M: [Gemini+] when sim in slot is running simservice, the function will update the running status
     * @param slotId
     * @param isRunning true if running
     */
    public static void updateSimServiceRunningStateForSlot(int slotId, boolean isRunning) {
        sSlotInfoMap.get(slotId).updateSimServiceRunningState(isRunning);
    }

    /**
     * M: [Gemini+] get the sim service running status
     * @param slotId
     * @return true if running
     */
    public static boolean isSimServiceRunningOnSlot(int slotId) {
        return sSlotInfoMap.get(slotId).isSimServiceRunning();
    }

    /**
     * M: [Gemini+] get the icc uri of the slot id
     * @param slotId
     * @return
     */
    public static Uri getSlotIccUri(int slotId) {
        return sSlotInfoMap.get(slotId).getIccUri();
    }

    /**
     * M: [Gemini+] get slot sdn uri
     * @param slotId
     * @return
     */
    public static Uri getSlotSdnUri(int slotId) {
        return sSlotInfoMap.get(slotId).getSdnUri();
    }

    /**
     * M: get all slot Ids
     * @return the list contains all slot ids
     */
    public static List<Integer> getAllSlotIds() {
        return new ArrayList<Integer>(sSlotInfoMap.keySet());
    }

    /**
     * M: [Gemini+] get voice mail number for slot
     * @param slotId
     * @return string
     */
    public static String getVoiceMailNumberForSlot(int slotId) {
        if (isSlotValid(slotId)) {
            return sSlotInfoMap.get(slotId).getVoiceMailNumber();
        }
        LogUtils.d(TAG, "[getVoiceMailNumberForSlot] slot " + slotId + " is not valid");
        return null;
    }

    /**
     * M: [Gemini+] update the saved voice mail number
     */
    public static void updateVoiceMailNumber() {
        for (SlotInfo slot : sSlotInfoMap.values()) {
            slot.updateVoiceMailNumber();
        }
    }

    /**
     * M: listen to all slots including Gemini+ a wrapper for
     * TelephonyManager.listen
     * 
     * @param context
     *            the context to get system service
     * @param listener
     *            {@link} PhoneStateListener
     * @param events
     *            {@link} TelephonyManager.listen events
     */
    public static void listenAllSlots(Context context, PhoneStateListener listener, int events) {
        if (SlotUtils.isGeminiEnabled()) {
            for (int slotId : getAllSlotIds()) {
                TelephonyManagerEx.getDefault().listen(listener, events, slotId);
            }
        } else {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(listener, events);
        }
    }

    /**
     * M: [Gemini+] get current device total slot count
     * @return count
     */
    public static int getSlotCount() {
        return sSlotInfoMap.size();
    }

    /**
     * M: [Gemini+] phone book service name by slotId
     * @param slotId
     * @return string
     */
    public static String getSimPhoneBookServiceNameForSlot(int slotId) {
        return sSlotInfoMap.get(slotId).getSimPhoneBookServiceName();
    }

    /**
     * M: [Gemini+] check whether the slot is valid
     * @param slotId
     * @return true if valid
     */
    public static boolean isSlotValid(int slotId) {
        boolean isValid = sSlotInfoMap.containsKey(slotId);
        if (!isValid) {
            LogUtils.d(TAG, "slot " + slotId + " is invalid!");
            LogUtils.printCaller(TAG);
        }
        return isValid;
    }

    /**
     * M: [Gemini+] slot ids are defined in array like 0, 1, 2, ...
     * @return the first id of all slotIds
     */
    public static int getFirstSlotId() {
        return FIRST_SLOT_ID;
    }

    /**
     * M: [Gemini+] get an invalid slot id, to indicate that this is not a sim slot.
     * 
     * @return negative value
     */
    public static int getNonSlotId() {
        return -1;
    }

    /**
     * M: [Gemini+] in single card phone, the only slot has a slot id this method to
     * retrieve the id.
     * 
     * @return the only slot id of a single card phone
     */
    public static int getSingleSlotId() {
        return FIRST_SLOT_ID;
    }

    /**
     * M: [Gemini+] get string resource id for the corresponding slot id
     * @param slotId
     * @return
     */
    public static int getResIdForSlot(int slotId) {
        return sSlotInfoMap.get(slotId).getResId();
    }

    /**
     * M: [Gemini+] resource is just string like "SIM1", "SIM2"
     * @param resId
     * @return if no slot matches, return NonSlotId
     */
    public static int getSlotIdFromSimResId(int resId) {
        for (int slotId : getAllSlotIds()) {
            if (sSlotInfoMap.get(slotId).mResId == resId) {
                return slotId;
            }
        }
        return getNonSlotId();
    }

    /**
     * M: [Gemini+] if gemini feature enabled on this device
     * @return
     */
    public static boolean isGeminiEnabled() {
        return FeatureOption.MTK_GEMINI_SUPPORT;
    }
}
