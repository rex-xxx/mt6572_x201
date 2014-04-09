package com.mediatek.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.RegistrantList;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;

import java.util.HashMap;
import java.util.List;

public class SIMInfoWrapper {
    private static final String TAG = "SIMInfoWrapper";
    private static final boolean DBG = true;

    private Context mContext;
    private static List<SIMInfo> mAllSimInfoList = null;
    private static List<SIMInfo> mInsertedSimInfoList = null;

    private HashMap<Integer,SIMInfo> mAllSimInfoMap = new HashMap<Integer, SIMInfo>();
    private HashMap<Integer,SIMInfo> mInsertedSimInfoMap = new HashMap<Integer, SIMInfo>();
    private HashMap<Integer,Integer> mSlotIdSimIdPairs = new HashMap<Integer,Integer>();
    private HashMap<Integer,Integer> mSimIdSlotIdPairs = new HashMap<Integer,Integer>();

    private RegistrantList mSimInfoUpdateRegistrantList = new RegistrantList();

    private boolean mIsNeedToInitSimInfo;

    private static SIMInfoWrapper sSIMInfoWrapper;

    public void updateSimInfoCache() {
        if (DBG) {
            log("updateSimInfoCache()");
        }
        if (mAllSimInfoList != null) {
            mAllSimInfoList = SIMInfo.getAllSIMList(mContext);
            if (mAllSimInfoList != null) {
                //mAllSimCount = mAllSimInfoList.size();
                mAllSimInfoMap = new HashMap<Integer, SIMInfo>();
                mSimIdSlotIdPairs = new HashMap<Integer, Integer>();
                for (SIMInfo item : mAllSimInfoList) {
                    int simId = getCheckedSimId(item);
                    if (simId != -1) {
                        mAllSimInfoMap.put(simId, item);
                        mSimIdSlotIdPairs.put(simId, item.mSlot);
                    }
                }
                if (DBG) {
                    log("[updateSimInfo] update mAllSimInfoList");
                }
            } else {
                if (DBG) {
                    log("[updateSimInfo] updated mAllSimInfoList is null");
                }
                return;
            }
        }

        if (mInsertedSimInfoList != null) {
            mInsertedSimInfoList = SIMInfo.getInsertedSIMList(mContext);
            if (mInsertedSimInfoList != null) {
                //mInsertedSimCount = mInsertedSimInfoList.size();
                mInsertedSimInfoMap = new HashMap<Integer, SIMInfo>();
                mSlotIdSimIdPairs = new HashMap<Integer, Integer>();
                for (SIMInfo item : mInsertedSimInfoList) {
                    int simId = getCheckedSimId(item);
                    if (simId != -1) {
                        mInsertedSimInfoMap.put(simId, item);
                        mSlotIdSimIdPairs.put(item.mSlot, simId);
                    }
                }
                if (DBG) {
                    log("[updateSimInfo] update mInsertedSimInfoList");
                }
            } else {
                if (DBG) {
                    log("[updateSimInfo] updated mInsertedSimInfoList is null");
                }
                return;
            }
        }
        mSimInfoUpdateRegistrantList.notifyRegistrants();
    }

    /**
     * SIMInfo wrapper constructor. Build simInfo according to input type
     * 
     * @param context
     * @param isInsertSimOrAll
     */
    private SIMInfoWrapper() {
        mAllSimInfoMap = new HashMap<Integer, SIMInfo>();
        mInsertedSimInfoMap = new HashMap<Integer, SIMInfo>();
        mSlotIdSimIdPairs = new HashMap<Integer,Integer>();
        mSimIdSlotIdPairs = new HashMap<Integer,Integer>();
    }

    private int getCheckedSimId(SIMInfo item) {
        if (item != null && item.mSimId > 0) {
            return (int) item.mSimId;
        } else {
            if (DBG) {
                log("[getCheckedSimId]Wrong simId is "
                        + (item == null ? -1 : item.mSimId));
            }
            return -1;
        }
    }

    /**
     * Public API to get SIMInfoWrapper instance
     * 
     * @param context
     * @param isInsertSim
     * @return SIMInfoWrapper instance
     */
    public static synchronized SIMInfoWrapper getDefault() {
        if (sIsNullResult) {
            return null;
        }
        if (sSIMInfoWrapper == null) {
            sSIMInfoWrapper = new SIMInfoWrapper();
        }
        if (sSIMInfoWrapper.mIsNeedToInitSimInfo) {
            sSIMInfoWrapper.initSimInfo();

            /// M: Add for ALPS00540397 @{
            if (mAllSimInfoList != null && mInsertedSimInfoList != null) {
                if (DBG) {
                    log("getDefault() initSimInfo successfully. mAllSimInfoList :" + mAllSimInfoList
                            + " mInsertedSimInfoList :" + mInsertedSimInfoList);
                }
                sSIMInfoWrapper.mIsNeedToInitSimInfo = false;
            }
            /// @}

        }
        return sSIMInfoWrapper;
    }

    /**
     * get cached SIM info list
     * 
     * @return
     */
    public List<SIMInfo> getSimInfoList() {
        return mAllSimInfoList;
    }

    /**
     * get cached SIM info list
     * 
     * @return
     */
    public List<SIMInfo> getAllSimInfoList() {
        return mAllSimInfoList;
    }

    /**
     * get cached SIM info list
     * 
     * @return
     */
    public List<SIMInfo> getInsertedSimInfoList() {
        return mInsertedSimInfoList;
    }

    /**
     * get SimInfo cached HashMap
     * 
     * @return
     */
    public HashMap<Integer, SIMInfo> getSimInfoMap() {
        return mAllSimInfoMap;
    }

    /**
     * get SimInfo cached HashMap
     * 
     * @return
     */
    public HashMap<Integer, SIMInfo> getInsertedSimInfoMap() {
        return mInsertedSimInfoMap;
    }

    /**
     * get cached SimInfo from HashMap
     * 
     * @param id
     * @return
     */
    public SIMInfo getSimInfoById(int id) {
        return mAllSimInfoMap.get(id);
    }

    public SIMInfo getSimInfoBySlot(int slot) {
        SIMInfo simInfo = null;
        /// M: For ALPS00540397
        if (mInsertedSimInfoList != null) {
            for (int i = 0; i < mInsertedSimInfoList.size(); i++) {
                simInfo = mInsertedSimInfoList.get(i);
                if (simInfo.mSlot == slot) {
                    return simInfo;
                }
            }
        }
        return null;
    }

    /**
     * get SIM color according to input id
     * 
     * @param id
     * @return
     */
    public int getSimColorById(int id) {
        SIMInfo simInfo = mAllSimInfoMap.get(id);
        return (simInfo == null) ? -1 : simInfo.mColor;
    }

    /**
     * get SIM display name according to input id
     * 
     * @param id
     * @return
     */
    public String getSimDisplayNameById(int id) {
        SIMInfo simInfo = mAllSimInfoMap.get(id);
        return (simInfo == null) ? null : simInfo.mDisplayName;
    }

    /**
     * get SIM slot according to input id
     * 
     * @param id
     * @return
     */
    public int getSimSlotById(int id) {
        SIMInfo simInfo = mAllSimInfoMap.get(id);
        return (simInfo == null) ? -1 : simInfo.mSlot;
    }

    /**
     * get cached SimInfo from HashMap
     * 
     * @param id
     * @return
     */
    public SIMInfo getInsertedSimInfoById(int id) {
        return mInsertedSimInfoMap.get(id);
    }

    /**
     * get SIM color according to input id
     * 
     * @param id
     * @return
     */
    public int getInsertedSimColorById(int id) {
        SIMInfo simInfo = mInsertedSimInfoMap.get(id);
        return (simInfo == null) ? -1 : simInfo.mColor;
    }

    /**
     * get SIM display name according to input id
     * 
     * @param id
     * @return
     */
    public String getInsertedSimDisplayNameById(int id) {
        SIMInfo simInfo = mInsertedSimInfoMap.get(id);
        return (simInfo == null) ? null : simInfo.mDisplayName;
    }

    /**
     * get SIM slot according to input id
     * 
     * @param id
     * @return
     */
    public int getInsertedSimSlotById(int id) {
        SIMInfo simInfo = mInsertedSimInfoMap.get(id);
        return (simInfo == null) ? -1 : simInfo.mSlot;
    }

    /**
     * get all SIM count according to Input
     * 
     * @return
     */
    public int getAllSimCount() {
        if (mAllSimInfoList != null) {
            return mAllSimInfoList.size();
        } else {
            return 0;
        }
    }

    /**
     * get inserted SIM count according to Input
     * 
     * @return
     */
    public int getInsertedSimCount() {
        if (mInsertedSimInfoList != null) {
            return mInsertedSimInfoList.size();
        } else {
            return 0;
        }
    }
    
    public int getSlotIdBySimId(int simId) {
        // return mSlotIdSimIdPairs.get(simId);
        Integer i = mSimIdSlotIdPairs.get(simId);
        return ((i == null) ? -1 : i);
    }

    public int getSimIdBySlotId(int slotId) {
        // return mSimIdSlotIdPairs.get(slotId);
        Integer i = mSlotIdSimIdPairs.get(slotId);
        return ((i == null) ? -1 : i);
    }

    /**
     * Get Sim Display Name according to slot id
     * 
     * @param slotId
     * @return
     */
    public String getSimDisplayNameBySlotId(int slotId) {
        String simDisplayName = null;
        int i = getSimIdBySlotId(slotId);
        simDisplayName = getSimDisplayNameById(i);
        return simDisplayName;
    }

    public void registerForSimInfoUpdate(Handler h, int what, Object obj) {
        mSimInfoUpdateRegistrantList.addUnique(h, what, obj);
    }

    public void unregisterForSimInfoUpdate(Handler h) {
        mSimInfoUpdateRegistrantList.remove(h);
    }
    
    public int getSimBackgroundResByColorId(int colorId) {
        log("getSimBackgroundResByColorId() colorId = " + colorId);
        if (colorId < 0 || colorId > 3) {
            colorId = 0;
        }
        return Telephony.SIMBackgroundRes[colorId];
    }

    public int getSimBackgroundDarkResByColorId(int colorId) {
        log("getSimBackgroundDarkResByColorId() colorId = " + colorId);
        if (colorId < 0 || colorId > 3) {
            colorId = 0;
        }
        return Telephony.SIMBackgroundDarkRes[colorId];
    }

    public int getSimBackgroundLightResByColorId(int colorId) {
        log("getSimBackgroundLightResByColorId() colorId = " + colorId);
        if (colorId < 0 || colorId > 3) {
            colorId = 0;
        }
        return Telephony.SIMBackgroundLightRes[colorId];
    }

    public void init(Context context) {
        mContext = context;
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.SIM_SETTINGS_INFO_CHANGED);
        iFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        iFilter.addAction(TelephonyIntents.ACTION_SIM_NAME_UPDATE);
        mContext.registerReceiver(mReceiver, iFilter);
        mIsNeedToInitSimInfo = true;
    }

    private void initSimInfo() {
        mAllSimInfoList = SIMInfo.getAllSIMList(mContext);
        mInsertedSimInfoList = SIMInfo.getInsertedSIMList(mContext);

        if (mAllSimInfoList == null || mInsertedSimInfoList == null) {
            log("[SIMInfoWrapper] mSimInfoList OR mInsertedSimInfoList is null");
            return;
        }

        //mAllSimCount = mAllSimInfoList.size();
        //mInsertedSimCount = mInsertedSimInfoList.size();

        for (SIMInfo item : mAllSimInfoList) {
            int simId = getCheckedSimId(item);
            if (simId != -1) {
                mAllSimInfoMap.put(simId, item);
                mSimIdSlotIdPairs.put(simId, item.mSlot);
            }
        }

        for (SIMInfo item : mInsertedSimInfoList) {
            int simId = getCheckedSimId(item);
            if (simId != -1) {
                mInsertedSimInfoMap.put(simId, item);
                mSlotIdSimIdPairs.put(item.mSlot, simId);
            }
        }
    }
    /**
     * Unregister context receiver
     * Should called when the context is end of life.
     */
    public void release() {
        if (mContext != null) {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DBG) {
                log("onReceive(), action = " + action);
            }
            if (action.equals(Intent.SIM_SETTINGS_INFO_CHANGED) ||
                action.equals(TelephonyIntents.ACTION_SIM_NAME_UPDATE) ||
                action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                updateSimInfoCache();
            }
        }
    };

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    /**
     * For test 
     */
    private static boolean sIsNullResult = false;
    public static void setNull(boolean testMode) {
        sSIMInfoWrapper = null;
        sIsNullResult = testMode;
    }
}
