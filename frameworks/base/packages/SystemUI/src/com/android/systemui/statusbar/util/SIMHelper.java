package com.android.systemui.statusbar.util;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstants;
import com.android.systemui.R;

import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;
import com.mediatek.telephony.SimInfoManager;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;

/**
 * M: [SystemUI] Support "dual SIM" and "Notification toolbar".
 */
public class SIMHelper {

    public static final String TAG = "SIMHelper";

    private static final int SIM_STATUS_COUNT = 9;
    private static final int MOBILE_ICON_COUNT = 4;

    private static List<SimInfoManager.SimInfoRecord> sSimInfos;

    private static int[] sSimStatusViews;
    private static int[] sMobileIconResIds;

    private static String sIsOptr = null;
    private static String sBaseband = null;

    private static ITelephony sITelephony;
    
    private static TelephonyManagerEx mTMEx = null;
    private static int mGeminiSimNum = PhoneConstants.GEMINI_SIM_NUM;
    private static boolean[] simInserted;

    private SIMHelper() {
    }

    /**
     * Get the default SIM id of the assigned business.
     * 
     * @param context
     * @param businessType
     * @return The default SIM id, or -1 if it is not defined.
     */
    public static long getDefaultSIM(Context context, String businessType) {
        return Settings.System.getLong(context.getContentResolver(), businessType, -1);
    }

    public static void setDefaultSIM(Context context, String businessType, long simId) {
        Settings.System.putLong(context.getContentResolver(), businessType, simId);
    }

    public static List<SimInfoManager.SimInfoRecord> getSIMInfoList(Context context) {
        if (sSimInfos == null || sSimInfos.size() == 0) {
            sSimInfos = getSortedSIMInfoList(context);
        }
        return sSimInfos;
    }

    /**
     * Get the SIM info of the assigned SIM id.
     * 
     * @param context
     * @param simId
     * @return The SIM info, or null if it doesn't exist.
     */
    public static SimInfoManager.SimInfoRecord getSIMInfo(Context context, long simId) {
        if (sSimInfos == null || sSimInfos.size() == 0) {
            getSIMInfoList(context);
        }
        for (SimInfoManager.SimInfoRecord info : sSimInfos) {
            if (info.mSimInfoId == simId) {
                return info;
            }
        }
        return null;
    }

    /**
     * Get the SIM info of the assigned SLOT id.
     * 
     * @param context
     * @param slotId
     * @return The SIM info, or null if it doesn't exist.
     */
    public static SimInfoManager.SimInfoRecord getSIMInfoBySlot(Context context, int slotId) {
        if(!isSimInserted(slotId)) {
            return null;
        }
        if (sSimInfos == null || sSimInfos.size() == 0) {
            getSIMInfoList(context);
        }
        if (sSimInfos == null) {
            return null;
        }

        for (SimInfoManager.SimInfoRecord info : sSimInfos) {
            if (info.mSimSlotId == slotId) {
                return info;
            }
        }
        return null;
    }

    private static List<SimInfoManager.SimInfoRecord> getSortedSIMInfoList(Context context) {
        List<SimInfoManager.SimInfoRecord> simInfoList = SimInfoManager.getInsertedSimInfoList(context);
        Collections.sort(simInfoList, new Comparator<SimInfoManager.SimInfoRecord>() {
            @Override
            public int compare(SimInfoManager.SimInfoRecord a, SimInfoManager.SimInfoRecord b) {
                if(a.mSimSlotId < b.mSimSlotId) {
                    return -1;
                } else if (a.mSimSlotId > b.mSimSlotId) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return simInfoList;
    }

    public static void updateSIMInfos(Context context) {
        sSimInfos = null;
        sSimInfos = getSortedSIMInfoList(context);
    }

    public static void initStatusIcons() {
        if (sSimStatusViews == null) {
            sSimStatusViews = new int[SIM_STATUS_COUNT];
            sSimStatusViews[PhoneConstants.SIM_INDICATOR_RADIOOFF] = com.mediatek.internal.R.drawable.sim_radio_off;
            sSimStatusViews[PhoneConstants.SIM_INDICATOR_LOCKED] = com.mediatek.internal.R.drawable.sim_locked;
            sSimStatusViews[PhoneConstants.SIM_INDICATOR_INVALID] = com.mediatek.internal.R.drawable.sim_invalid;
            sSimStatusViews[PhoneConstants.SIM_INDICATOR_SEARCHING] = com.mediatek.internal.R.drawable.sim_searching;
            sSimStatusViews[PhoneConstants.SIM_INDICATOR_ROAMING] = com.mediatek.internal.R.drawable.sim_roaming;
            sSimStatusViews[PhoneConstants.SIM_INDICATOR_CONNECTED] = com.mediatek.internal.R.drawable.sim_connected;
            sSimStatusViews[PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED] = com.mediatek.internal.R.drawable.sim_roaming_connected;
        }
    }

    public static void initMobileIcons() {
        if (sMobileIconResIds == null) {
            sMobileIconResIds = new int[MOBILE_ICON_COUNT];
            sMobileIconResIds[0] = R.drawable.ic_qs_mobile_blue;
            sMobileIconResIds[1] = R.drawable.ic_qs_mobile_orange;
            sMobileIconResIds[2] = R.drawable.ic_qs_mobile_green;
            sMobileIconResIds[3] = R.drawable.ic_qs_mobile_purple;
        }
    }

    public static long getSIMIdBySlot(Context context, int slotId) {
        SimInfoManager.SimInfoRecord simInfo = getSIMInfoBySlot(context, slotId);
        if (simInfo == null) {
            return 0;
        }
        return simInfo.mSimInfoId;
    }

    public static int getSIMColorIdBySlot(Context context, int slotId) {
        SimInfoManager.SimInfoRecord simInfo = getSIMInfoBySlot(context, slotId);
        if (simInfo == null) {
            return -1;
        }
        return simInfo.mColor;
    }

    public static int getSIMStateIcon(SimInfoManager.SimInfoRecord simInfo) {
        return getSIMStateIcon(getSimIndicatorStateGemini(simInfo.mSimSlotId));
    }

    public static int getSIMStateIcon(int simStatus) {
        if (simStatus <= -1 || simStatus >= SIM_STATUS_COUNT) {
            return -1;
        }
        if (sSimStatusViews == null) {
            initStatusIcons();
        }
        return sSimStatusViews[simStatus];
    }

    public static int getDataConnectionIconIdBySlotId(Context context, int slotId) {
        SimInfoManager.SimInfoRecord simInfo = getSIMInfoBySlot(context, slotId);
        if (simInfo == null) {
            return -1;
        }
        if (sMobileIconResIds == null) {
            initMobileIcons();
        }
        if (simInfo.mColor == -1) {
            return -1;
        } else {
            return sMobileIconResIds[simInfo.mColor];
        }
    }

    public static boolean checkSimCardDataConnBySlotId(Context context, int slotId) {
        SimInfoManager.SimInfoRecord simInfo = getSIMInfoBySlot(context, slotId);
        if (simInfo == null) {
            return false;
        }
        int simState = getSimIndicatorStateGemini(simInfo.mSimSlotId);
        if (simState == PhoneConstants.SIM_INDICATOR_ROAMING
                || simState == PhoneConstants.SIM_INDICATOR_CONNECTED
                || simState == PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED
                || simState == PhoneConstants.SIM_INDICATOR_NORMAL) {
            return true;
        } else {
            return false;
        }
    }
    
    /// M: Check the data connection is connected? real one. @{
    public static boolean isTelephonyDataConnected(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null && TelephonyManager.DATA_CONNECTED == tm.getDataState()) {
            Xlog.d(TAG, "isTelephonyDataConnected called, the data state is " + tm.getDataState());
            return true;
        }
        return false;
    }
    /// M: }@
    
    /// M: check the sim card data connection is valid @{
    public static boolean checkSimCardDataConn() {
        int simState = getSimIndicatorState();
        if (simState == PhoneConstants.SIM_INDICATOR_ROAMING
                || simState == PhoneConstants.SIM_INDICATOR_CONNECTED
                || simState == PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED
                || simState == PhoneConstants.SIM_INDICATOR_NORMAL) {
            return true;
        } else {
            return false;
        }
    }
    /// M: }@

    public static boolean is3GSupported() {
        if (sBaseband == null) {
            sBaseband = SystemProperties.get("gsm.baseband.capability");
        }
        if ((sBaseband != null) && (sBaseband.length() != 0)
                && (Integer.parseInt(sBaseband) <= 3)) {
            return false;
        } else {
            return true;
        }
    }
    
    public static int getSimIndicatorState() {
        try {
             ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
             if (telephony != null) {
                 return telephony.getSimIndicatorState();
             } else {
                 // This can happen when the ITelephony interface is not up yet.
                 return PhoneConstants.SIM_INDICATOR_UNKNOWN;
             }
        } catch (RemoteException ex) {
            // the phone process is restarting.
            return PhoneConstants.SIM_INDICATOR_UNKNOWN;
        } catch (NullPointerException ex) {
            return PhoneConstants.SIM_INDICATOR_UNKNOWN;
        }
    }
    
    public static int getSimIndicatorStateGemini(int simId) {
        try {
             ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
             if (telephony != null) {
                 final int mState = telephony.getSimIndicatorStateGemini(simId);
                 if ((mState == PhoneConstants.SIM_INDICATOR_CONNECTED)
                    && (TelephonyManager.DATA_CONNECTED != TelephonyManagerEx.getDefault().getDataState(simId))) {
                    /// M: Sync the data connected state with TelephonyManager, and fallback to normal.
                    Xlog.d(TAG, "getSimIndicatorStateGemini called, fallback to normal and simId is " + simId);
                    return PhoneConstants.SIM_INDICATOR_NORMAL;
                 }
                 return mState;
             } else {
                 // This can happen when the ITelephony interface is not up yet.
                 return PhoneConstants.SIM_INDICATOR_UNKNOWN;
             }
        } catch (RemoteException ex) {
            // the phone process is restarting.
            return PhoneConstants.SIM_INDICATOR_UNKNOWN;
        } catch (NullPointerException ex) {
            return PhoneConstants.SIM_INDICATOR_UNKNOWN;
        }
    }

    public static boolean isTelephonyDataConnectedBySimId(int simId) {
        try {
            if (TelephonyManager.DATA_CONNECTED == TelephonyManagerEx.getDefault().getDataState(simId)) {
                Xlog.d(TAG, "isTelephonyDataConnectedBySimId called, data is connected and simId is " + simId);
                return true;
            } else {
                Xlog.d(TAG, "isTelephonyDataConnectedBySimId called, data is not connected and simId is " + simId);
                return false;
            }
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public static TelephonyManagerEx getDefault(Context context) {
        if (mTMEx == null) {
            mTMEx = new TelephonyManagerEx(context);
        }
        return mTMEx;
    }
    
    public static ITelephony getITelephony() {
        return sITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
    }
    
    private static ITelephonyRegistry mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
    private static ITelephonyRegistry mRegistry2 = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry2"));

    public static void listen(PhoneStateListener listener, int events, int slotId) {
        try {
            Boolean notifyNow = (getITelephony() != null);
            /// M: Support GeminiPlus
            if (PhoneConstants.GEMINI_SIM_1 == slotId) {
                mRegistry.listen("SystemUI SIMHelper", listener.getCallback(), events, notifyNow);
            } else if(PhoneConstants.GEMINI_SIM_2 == slotId) {
                mRegistry2.listen("SystemUI SIMHelper", listener.getCallback(), events, notifyNow);
            } else if(PhoneConstants.GEMINI_SIM_3 == slotId && PhoneConstants.GEMINI_SIM_NUM >= 3) {
                ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry3")).listen("SystemUI SIMHelper", listener.getCallback(), events, notifyNow);
            } else if(PhoneConstants.GEMINI_SIM_4 == slotId && PhoneConstants.GEMINI_SIM_NUM >= 4) {
                ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry4")).listen("SystemUI SIMHelper", listener.getCallback(), events, notifyNow);
            }
        } catch (RemoteException ex) {
            // system process dead
        } catch (NullPointerException ex) {
            // system process dead
        }
    }
    
    public static boolean isSimInserted(int slotId) {
        if(simInserted == null) {
            updateSimInsertedStatus();
        }
        if (simInserted != null) {
            if(slotId <= simInserted.length -1) {
                Xlog.d(TAG, "isSimInserted(" + slotId + "), SimInserted=" + simInserted[slotId]);
                return simInserted[slotId];
            } else {
                Xlog.d(TAG, "isSimInserted(" + slotId + "), indexOutOfBound, arraysize=" + simInserted.length);
                return false; // default return false
            }
        } else {
            Xlog.d(TAG, "isSimInserted, simInserted is null");
            return false;
        }
    }
    
    public static void updateSimInsertedStatus() {
        
        ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (phone != null) {
            try {
                if(simInserted == null) {
                    simInserted = new boolean[mGeminiSimNum];
                }
                for (int i = 0 ; i < mGeminiSimNum ; i++) {
                    simInserted[i] = phone.isSimInsert(i);
                    Xlog.d(TAG, "updateSimInsertedStatus, simInserted(" + i + ") = " + simInserted[i]);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Xlog.d(TAG, "updateSimInsertedStatus, phone is null");
        }
    }
}
