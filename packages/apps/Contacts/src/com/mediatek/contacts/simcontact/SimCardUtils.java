package com.mediatek.contacts.simcontact;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.contacts.R;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyProperties;
import com.google.common.annotations.VisibleForTesting;
import com.mediatek.common.telephony.ITelephonyEx;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.util.ContactsGroupUtils;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.HashMap;
import java.util.List;

public class SimCardUtils {
    public static final String TAG = "SimCardUtils";

    public interface SimType {
        String SIM_TYPE_USIM_TAG = "USIM";
        String SIM_TYPE_SIM_TAG = "SIM";

        int SIM_TYPE_SIM = 0;
        int SIM_TYPE_USIM = 1;

        //UIM
        int SIM_TYPE_UIM = 2;
        String SIM_TYPE_UIM_TAG = "UIM";
        //UIM
    }

    public static class SimUri {
        public static final String AUTHORITY = "icc";

        /**
         * M: to get sim uri, use SlotUtils.getSlotIccUri(slotId) instead
         * 
         * @param slotId
         *            the slot id
         * @return the icc uri of this slotId
         */
        @Deprecated
        public static Uri getSimUri(int slotId) {
            return SlotUtils.getSlotIccUri(slotId);
        }

        /**
         * M: to get sim sdn uri, use SlotUtils.getSlotSdnUri(slotId) instead
         * 
         * @param slotId
         *            the slot id
         * @return the sdn uri of this slotId
         */
        @Deprecated
        public static Uri getSimSdnUri(int slotId) {
            return SlotUtils.getSlotSdnUri(slotId);
        }

    }

    public static boolean isSimPukRequest(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_PUK_REQUEST);
        if (v != null) {
            return v;
        }

        boolean isPukRequest = false;
        if (SlotUtils.isGeminiEnabled()) {
            isPukRequest = (TelephonyManager.SIM_STATE_PUK_REQUIRED == TelephonyManagerEx
                    .getDefault().getSimState(slotId));
        } else {
            isPukRequest = (TelephonyManager.SIM_STATE_PUK_REQUIRED == TelephonyManager
                    .getDefault().getSimState());
        }
        return isPukRequest;
    }

    public static boolean isSimPinRequest(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_PIN_REQUEST);
        if (v != null) {
            return v;
        }

        boolean isPinRequest = false;
        if (SlotUtils.isGeminiEnabled()) {
            isPinRequest = (TelephonyManager.SIM_STATE_PIN_REQUIRED == TelephonyManagerEx
                    .getDefault().getSimState(slotId));
        } else {
            isPinRequest = (TelephonyManager.SIM_STATE_PIN_REQUIRED == TelephonyManager
                    .getDefault().getSimState());
        }
        return isPinRequest;
    }

    public static boolean isSimStateReady(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_STATE_READY);
        if (v != null) {
            return v;
        }

        boolean isSimStateReady = false;
        if (SlotUtils.isGeminiEnabled()) {
            isSimStateReady = (TelephonyManager.SIM_STATE_READY == TelephonyManagerEx
                    .getDefault().getSimState(slotId));
        } else {
            isSimStateReady = (TelephonyManager.SIM_STATE_READY == TelephonyManager
                    .getDefault().getSimState());
        }
        return isSimStateReady;
    }

    public static boolean isSimInserted(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_SIM_INSERTED);
        if (v != null) {
            return v;
        }

        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        boolean isSimInsert = false;
        try {
            if (iTel != null) {
                if (SlotUtils.isGeminiEnabled()) {
                    isSimInsert = iTel.isSimInsert(slotId);
                } else {
                    isSimInsert = iTel.isSimInsert(SlotUtils.getSingleSlotId());
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            isSimInsert = false;
        }
        return isSimInsert;
    }

    public static boolean isFdnEnabed(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_FDN_ENABLED);
        if (v != null) {
            return v;
        }

        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        boolean isFdnEnabled = false;
        try {
            if (iTel != null) {
                if (SlotUtils.isGeminiEnabled()) {
                    isFdnEnabled = iTel.isFDNEnabledGemini(slotId);
                } else {
                    isFdnEnabled = iTel.isFDNEnabled();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            isFdnEnabled = false;
        }
        return isFdnEnabled;
    }

    public static boolean isSetRadioOn(ContentResolver resolver, int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_SET_RADIO_ON);
        if (v != null) {
            return v;
        }
        
        boolean isRadioOn = false;
        if (SlotUtils.isGeminiEnabled()) {
            ///M: [Gemini+] dualSimSet rule: each bit stands for each slot radio status
            /// e.g. 0101 means only slot 0 & slot 2 is set radio on
            final int flagAllSimOn = (1 << SlotUtils.getSlotCount()) - 1;
            int dualSimSet = Settings.System.getInt(resolver, Settings.System.DUAL_SIM_MODE_SETTING, flagAllSimOn);
            isRadioOn = (Settings.Global.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 0)
                    && ((1 << (slotId - SlotUtils.getFirstSlotId())) & dualSimSet) != 0;
        } else {
            isRadioOn = Settings.Global.getInt(resolver,
                    Settings.Global.AIRPLANE_MODE_ON, 0) == 0;
        }
        return isRadioOn;
    }

    /**
     * check PhoneBook State is ready if ready, then return true.
     * 
     * @param slotId
     * @return
     */
    public static boolean isPhoneBookReady(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_PHB_READY);
        if (v != null) {
            return v;
        }

        final ITelephonyEx iPhbEx = ITelephonyEx.Stub.asInterface(ServiceManager
              .getService("phoneEx"));

        if (null == iPhbEx) {
            Log.w(TAG, "checkPhoneBookState, phoneEx == null");
            return false;
        }
        boolean isPbReady = false;
        try {
            if (SlotUtils.isGeminiEnabled()) {
                isPbReady = iPhbEx.isPhbReadyExt(slotId);
                Log.d(TAG, "isPbReady:" + isPbReady + "||slotId:" + slotId);

            } else {
                isPbReady = iPhbEx.isPhbReady();
                Log.d(TAG, "isPbReady:" + isPbReady + "||slotId:" + slotId);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "e.getMessage is " + e.getMessage());
        }
        return isPbReady;
    }

    /**
     * M: [Gemini+] get sim type integer by slot id
     * sim type is integer defined in SimCardUtils.SimType
     * @param slotId
     * @return SimCardUtils.SimType
     */
    public static int getSimTypeBySlot(int slotId) {
        Integer v = (Integer) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_SIM_TYPE);
        if (v != null) {
            Log.i(TAG, "getSimTypeBySlot,v is not null,v = " + v);
            return v;
        }
        int simType = -1;

        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            Log.i(TAG, "getSimTypeBySlot,ITelephony is not ready,return.");
            return simType;
        }

        try {
            if (SlotUtils.isGeminiEnabled()) {
                if (SimType.SIM_TYPE_USIM_TAG.equals(iTel.getIccCardTypeGemini(slotId))) {
                    simType = SimType.SIM_TYPE_USIM;
                } else if (SimType.SIM_TYPE_UIM_TAG.equals(iTel.getIccCardTypeGemini(slotId))) {
                    simType = SimType.SIM_TYPE_UIM;
                } else if (SimType.SIM_TYPE_SIM_TAG.equals(iTel.getIccCardTypeGemini(slotId))) {
                    simType = SimType.SIM_TYPE_SIM;
                }
            } else {
                if (SimType.SIM_TYPE_USIM_TAG.equals(iTel.getIccCardType())) {
                    simType = SimType.SIM_TYPE_USIM;
                } else if (SimType.SIM_TYPE_UIM_TAG.equals(iTel.getIccCardType())) {
                    simType = SimType.SIM_TYPE_UIM;
                } else if (SimType.SIM_TYPE_SIM_TAG.equals(iTel.getIccCardType())) {
                    simType = SimType.SIM_TYPE_SIM;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "catched exception.");
            e.printStackTrace();
        }
        return simType;
    }

    /**
     * M: [Gemini+] check whether a slot is insert a usim card
     * @param slotId
     * @return true if it is usim card
     */
    public static boolean isSimUsimType(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_IS_USIM);
        if (v != null) {
            Log.w(TAG, "isSimUsimType, v is not null,return.");
            return v;
        }

        boolean isUsim = false;
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            Log.w(TAG, "isSimUsimType, ITelephony is not ready,return.");
            return isUsim;
        }

        try {
            if (SlotUtils.isGeminiEnabled()) {
                if (SimType.SIM_TYPE_USIM_TAG.equals(iTel.getIccCardTypeGemini(slotId))) {
                    isUsim = true;
                }
            } else {
                if (SimType.SIM_TYPE_USIM_TAG.equals(iTel.getIccCardType())) {
                    isUsim = true;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "catched exception.");
            e.printStackTrace();
        }
        return isUsim;
    }

    public static boolean isSimInfoReady() {
        Boolean v = (Boolean) getPresetObject(String.valueOf(NO_SLOT), SIM_KEY_SIMINFO_READY);
        if (v != null) {
            return v;
        }

        String simInfoReady = SystemProperties.get(TelephonyProperties.PROPERTY_SIM_INFO_READY);
        return "true".equals(simInfoReady);
    }

    /**
     * For test
     */
    private static HashMap<String, ContentValues> sPresetSimData = null;
    
    @VisibleForTesting
    public static void clearPreSetSimData() {
        sPresetSimData = null;
    }
    
    private static Object getPresetObject(String key1, String key2) {
        if (sPresetSimData != null) {
            ContentValues values = sPresetSimData.get(key1);
            if (values != null) {
                Object v = values.get(key2);
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }
    
    private static final String NO_SLOT = String.valueOf(-1);
    private static final String SIM_KEY_WITHSLOT_PUK_REQUEST = "isSimPukRequest";
    private static final String SIM_KEY_WITHSLOT_PIN_REQUEST = "isSimPinRequest";
    private static final String SIM_KEY_WITHSLOT_STATE_READY = "isSimStateReady";
    private static final String SIM_KEY_WITHSLOT_SIM_INSERTED = "isSimInserted";
    private static final String SIM_KEY_WITHSLOT_FDN_ENABLED = "isFdnEnabed";
    private static final String SIM_KEY_WITHSLOT_SET_RADIO_ON = "isSetRadioOn";
    private static final String SIM_KEY_WITHSLOT_PHB_READY = "isPhoneBookReady";
    private static final String SIM_KEY_WITHSLOT_SIM_TYPE = "getSimTypeBySlot";
    private static final String SIM_KEY_WITHSLOT_IS_USIM = "isSimUsimType";
    private static final String SIM_KEY_SIMINFO_READY = "isSimInfoReady";
    private static final String SIM_KEY_WITHSLOT_RADIO_ON = "isRadioOn";
    private static final String SIM_KEY_WITHSLOT_HAS_ICC_CARD = "hasIccCard";
    private static final String SIM_KEY_WITHSLOT_GET_SIM_INDICATOR_STATE = "getSimIndicatorState";
    
    @VisibleForTesting
    public static void preSetSimData(int slot, Boolean fdnEnabled,
            Boolean isUsim, Boolean phbReady, Boolean pinRequest,
            Boolean pukRequest, Boolean isRadioOn, Boolean isSimInserted,
            Integer simType, Boolean simStateReady, Boolean simInfoReady) {
        ContentValues value1 = new ContentValues();
        if (fdnEnabled != null) {
            value1.put(SIM_KEY_WITHSLOT_FDN_ENABLED, fdnEnabled);
        }
        if (isUsim != null) {
            value1.put(SIM_KEY_WITHSLOT_IS_USIM, isUsim);
        }
        if (phbReady != null) {
            value1.put(SIM_KEY_WITHSLOT_PHB_READY, phbReady);
        }
        if (pinRequest != null) {
            value1.put(SIM_KEY_WITHSLOT_PIN_REQUEST, pinRequest);
        }
        if (pukRequest != null) {
            value1.put(SIM_KEY_WITHSLOT_PUK_REQUEST, pukRequest);
        }
        if (isRadioOn != null) {
            value1.put(SIM_KEY_WITHSLOT_SET_RADIO_ON, isRadioOn);
        }
        if (isSimInserted != null) {
            value1.put(SIM_KEY_WITHSLOT_SIM_INSERTED, isSimInserted);
        }
        if (simType != null) {
            value1.put(SIM_KEY_WITHSLOT_SIM_TYPE, simType);
        }
        if (simStateReady != null) {
            value1.put(SIM_KEY_WITHSLOT_STATE_READY, simStateReady);
        }
        if (sPresetSimData == null) {
            sPresetSimData = new HashMap<String, ContentValues>(); 
        }
        if (value1 != null && value1.size() > 0) {
            String key1 = String.valueOf(slot);
            if (sPresetSimData.containsKey(key1)) {
                sPresetSimData.remove(key1);
            }
            sPresetSimData.put(key1, value1);
        }
        
        ContentValues value2 = new ContentValues();
        if (simInfoReady != null) {
            value2.put(SIM_KEY_SIMINFO_READY, simInfoReady);
        }
        if (value2 != null && value2.size() > 0) {
            if (sPresetSimData.containsKey(NO_SLOT)) {
                sPresetSimData.remove(NO_SLOT);
            } 
            sPresetSimData.put(NO_SLOT, value2);
        }
    }

    public static class ShowSimCardStorageInfoTask extends AsyncTask<Void, Void, Void> {
        private static ShowSimCardStorageInfoTask sInstance = null;
        private boolean mIsCancelled = false;
        private boolean mIsException = false;
        private String mDlgContent = null;
        private Context mContext = null;
        private static boolean sNeedPopUp = false;
        private static HashMap<Integer, Integer> sSurplugMap = new HashMap<Integer, Integer>();

        public static void showSimCardStorageInfo(Context context, boolean needPopUp) {
            sNeedPopUp = needPopUp;
            Log.i(TAG, "[ShowSimCardStorageInfoTask]_beg");
            if (sInstance != null) {
                sInstance.cancel();
                sInstance = null;
            }
            sInstance = new ShowSimCardStorageInfoTask(context);
            sInstance.execute();
            Log.i(TAG, "[ShowSimCardStorageInfoTask]_end");
        }

        public ShowSimCardStorageInfoTask(Context context) {
            mContext = context;
            Log.i(TAG, "[ShowSimCardStorageInfoTask] onCreate()");
        }

        @Override
        protected Void doInBackground(Void... args) {
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: doInBackground_beg");
            sSurplugMap.clear();
            List<SIMInfo> simInfos = SIMInfoWrapper.getDefault().getInsertedSimInfoList();
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: simInfos.size = " + SIMInfoWrapper.getDefault().getInsertedSimCount());
            if (!mIsCancelled && (simInfos != null) && simInfos.size() > 0) {
                StringBuilder build = new StringBuilder();
                int simId = 0;
                for (SIMInfo simInfo : simInfos) {
                    if (simId > 0) {
                        build.append("\n\n");
                    }
                    simId++;
                    int[] storageInfos = null;
                    Log.i(TAG, "[ShowSimCardStorageInfoTask] simName = " + simInfo.mDisplayName
                            + "; simSlot = " + simInfo.mSlot + "; simId = " + simInfo.mSimId);
                    build.append(simInfo.mDisplayName);
                    build.append(":\n");
                    try {
                        ITelephonyEx phoneEx = ITelephonyEx.Stub.asInterface(ServiceManager
                              .checkService("phoneEx"));
                        if (!mIsCancelled && phoneEx != null) {
                            storageInfos = phoneEx.getAdnStorageInfo(simInfo.mSlot);
                            if (storageInfos == null) {
                                mIsException = true;
                                Log.i(TAG, " storageInfos is null");
                                return null;
                            }
                            Log.i(TAG, "[ShowSimCardStorageInfoTask] infos: "
                                    + storageInfos.toString());
                        } else {
                            Log.i(TAG, "[ShowSimCardStorageInfoTask]: phone = null");
                            mIsException = true;
                            return null;
                        }
                    } catch (RemoteException ex) {
                        Log.i(TAG, "[ShowSimCardStorageInfoTask]_exception: " + ex);
                        mIsException = true;
                        return null;
                    }
                    Log.i(TAG, "slotId:" + simInfo.mSlot + "||storage:"
                            + (storageInfos == null ? "NULL" : storageInfos[1]) + "||used:"
                            + (storageInfos == null ? "NULL" : storageInfos[0]));
                    if (storageInfos != null && storageInfos[1] > 0) {
                        sSurplugMap.put(simInfo.mSlot, storageInfos[1] - storageInfos[0]);
                    }
                    build.append(mContext.getResources().getString(R.string.dlg_simstorage_content,
                            storageInfos[1], storageInfos[0]));
                    if (mIsCancelled) {
                        return null;
                    }
                }
                mDlgContent = build.toString();
            }
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: doInBackground_end");
            return null;
        }

        public void cancel() {
            super.cancel(true);
            mIsCancelled = true;
            Log.i(TAG, "[ShowSimCardStorageInfoTask]: mIsCancelled = true");
        }

        @Override
        protected void onPostExecute(Void v) {
            sInstance = null;
            if (!mIsCancelled && !mIsException && sNeedPopUp) {
                new AlertDialog.Builder(mContext).setIcon(
                        R.drawable.ic_menu_look_simstorage_holo_light).setTitle(
                        R.string.look_simstorage).setMessage(mDlgContent).setPositiveButton(
                        android.R.string.ok, null).setCancelable(true).create().show();
            }
            mIsCancelled = false;
            mIsException = false;
        }

        public static int getSurplugCount(int slotId) {
            Log.i(TAG, "getSurplugCount sSurplugMap : " + sSurplugMap);
            if (null != sSurplugMap && sSurplugMap.containsKey(slotId)) {
                int result = sSurplugMap.get(slotId);
                Log.i(TAG, "getSurplugCount result : " + result);
                return result;
            } else {
                Log.i(TAG, "getSurplugCount return -1");
                return -1;
            }
        }
    }

    /**
     * M: [Gemini+] wrapper gemini & common API
     * 
     * @param slotId
     *            the slot id
     * @return true if radio on
     */
    public static boolean isRadioOn(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_RADIO_ON);
        if (v != null) {
            return v;
        }

        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            Log.w(TAG, "[isRadioOn] fail to get iTel");
            return false;
        }

        boolean isRadioOn;
        try {
            if (SlotUtils.isGeminiEnabled()) {
                isRadioOn = iTel.isRadioOnGemini(slotId);
            } else {
                isRadioOn = iTel.isRadioOn();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[isRadioOn] failed to get radio state for slot " + slotId);
            isRadioOn = false;
        }

        return isRadioOn;
    }

    /**
     * M: [Gemini+] wrapper gemini & common API
     * 
     * @param slotId
     * @return
     */
    public static boolean hasIccCard(int slotId) {
        Boolean v = (Boolean) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_HAS_ICC_CARD);
        if (v != null) {
            return v;
        }

        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            Log.w(TAG, "[hasIccCard] fail to get iTel");
            return false;
        }

        boolean hasIccCard;
        try {
            if (SlotUtils.isGeminiEnabled()) {
                hasIccCard = iTel.hasIccCardGemini(slotId);
            } else {
                hasIccCard = iTel.hasIccCard();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[hasIccCard] failed to check icc card state for slot " + slotId);
            hasIccCard = false;
        }

        return hasIccCard;
    }

    /**
     * M: [Gemini+] not only ready, but also idle for all sim operations its
     * requirement is: 1. iccCard is insert 2. radio is on 3. FDN is off 4. PHB
     * is ready 5. simstate is ready 6. simService is not running
     * 
     * @param slotId
     *            the slotId to check
     * @return true if idle
     */
    public static boolean isSimStateIdle(int slotId) {
        if (!SlotUtils.isSlotValid(slotId)) {
            Log.e(TAG, "[isSimStateIdle] invalid slotId: " + slotId);
            return false;
        }
        boolean isSimStateReady = SimCardUtils.isSimStateReady(slotId);
        boolean isRadioOn = SimCardUtils.isRadioOn(slotId);
        boolean isFDNEnabled = SimCardUtils.isFdnEnabed(slotId);
        boolean hasIccCard = SimCardUtils.hasIccCard(slotId);
        boolean isSimServiceRunning = SlotUtils.isSimServiceRunningOnSlot(slotId);
        boolean isPhoneBookReady = SimCardUtils.isPhoneBookReady(slotId);
        Log.i(TAG, "[isSimStateIdle], isSimStateReady = " + isSimStateReady + ", isRadioOn = " + isRadioOn
                + ", isFDNEnabled = " + isFDNEnabled + ", hasIccCard = " + hasIccCard + ", isSimServiceRunning = "
                + isSimServiceRunning + ", isPhoneBookReady = " + isPhoneBookReady);
        return hasIccCard && isRadioOn && !isFDNEnabled && isPhoneBookReady && isSimStateReady && !isSimServiceRunning;
    }

    /**
     * M: [Gemini+] wrapper gemini & common API
     * 
     * @param slotId
     * @return
     */
    public static int getSimIndicatorState(int slotId) {
        Integer v = (Integer) getPresetObject(String.valueOf(slotId), SIM_KEY_WITHSLOT_GET_SIM_INDICATOR_STATE);
        if (v != null) {
            return v;
        }

        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            Log.w(TAG, "[getSimIndicatorState] fail to get iTel");
            return -1;
        }

        int simIndicatorState;
        try {
            if (SlotUtils.isGeminiEnabled()) {
                simIndicatorState = iTel.getSimIndicatorStateGemini(slotId);
            } else {
                simIndicatorState = iTel.getSimIndicatorState();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[getSimIndicatorState] failed to get sim indicator state for slot " + slotId);
            simIndicatorState = -1;
        }

        return simIndicatorState;
    }

    /**
     * M: [Gemini+] wrapper gemini & common API
     * 
     * @param input
     * @param slotId
     * @return
     */
    public static boolean handlePinMmi(String input, int slotId) {
        final ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (phone == null) {
            Log.w(TAG, "[handlePinMmi] fail to get phone for slot " + slotId);
            return false;
        }
        Log.d(TAG, "[handlePinMmi], slot " + slotId);
        boolean isHandled;
        try {
            if (SlotUtils.isGeminiEnabled()) {
                isHandled = phone.handlePinMmiGemini(input, slotId);
            } else {
                isHandled = phone.handlePinMmi(input);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[handlePinMmi]exception : " + e.getMessage());
            isHandled = false;
        }

        return isHandled;
    }

    /**
     * M: [Gemini+] get sim tag like "SIM", "USIM", "UIM" by slot id
     * 
     * @param slotId
     * @return
     */
    public static String getSimTagBySlot(int slotId) {
        if (!SlotUtils.isSlotValid(slotId)) {
            Log.e(TAG, "slot invalid" + slotId);
            return null;
        }
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        try {
            if (SlotUtils.isGeminiEnabled()) {
                return iTel.getIccCardTypeGemini(slotId);
            }
            return iTel.getIccCardType();
        } catch (RemoteException e) {
            Log.e(TAG, "catched exception. slot id: " + slotId);
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * M: [Gemini+] when a sim card was turned on or off in Settings, a broadcast
     * Intent.ACTION_DUAL_SIM_MODE_CHANGED would be sent, and carry current Dual sim mode.
     * This method is defined to parse the intent, and check whether the slot is on or off.
     * @param slotId slot to check
     * @param the mode retrived from the broadcast intent
     * @return true if on, false if off
     */
    public static boolean isDualSimModeOn(int slotId, int mode) {
        assert (SlotUtils.isSlotValid(slotId));
        assert (mode >= 0 && mode < (1 << SlotUtils.getSlotCount()));
        return (1 << (slotId - SlotUtils.getFirstSlotId()) & mode) != 0;
    }
    
    /** M: Bug Fix for ALPS00557517 @{ */
    public static int getAnrCount(int slot) {
        if (!SlotUtils.isSlotValid(slot)) {
            Log.e(TAG, "[getAnrCount]slot:" + slot + "is invalid!");
            return -1;
        }
        int anrCount = -1;
        Log.d(TAG, "[getAnrCount]slot:" + slot + "|maxCount:" + anrCount);
        try {
            final IIccPhoneBook iIccPhb = ContactsGroupUtils.getIIccPhoneBook(slot);
            if (iIccPhb != null) {
                anrCount = iIccPhb.getAnrCount();
            }
        } catch (android.os.RemoteException e) {
            Log.e(TAG, "[getAnrCount] catched exception.");
            anrCount = -1;
        }
        Log.d(TAG, "[getAnrCount]slot:" + slot + "|maxCount:" + anrCount);
        return anrCount;
    }
    /** @ } */

    /** M: Bug Fix for ALPS00566570 , get support email count
     * some USIM cards have no Email fields.
     * if the slot is invalid, return -1, otherwise return the email count
     * @{ */
    public static int getIccCardEmailCount(int slot) {
        if (!SlotUtils.isSlotValid(slot)) {
            Log.e(TAG, "[getIccCardEmailCount]slot:" + slot + "is invalid!");
            return -1;
        }
        int emailCount = -1;
        try {
            String serviceName = SlotUtils.getSimPhoneBookServiceNameForSlot(slot);
            final IIccPhoneBook iIccPhb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService(serviceName));
            if (iIccPhb != null) {
                emailCount = iIccPhb.getEmailCount();
            }
        } catch (android.os.RemoteException e) {
            Log.e(TAG, "[getIccCardEmailCount] catched exception.");
            emailCount = -1;
        }
        Log.d(TAG, "[getIccCardEmailCount]slot:" + slot + "|maxCount:" + emailCount);
        return emailCount;
    }
    /** @ } */
}
