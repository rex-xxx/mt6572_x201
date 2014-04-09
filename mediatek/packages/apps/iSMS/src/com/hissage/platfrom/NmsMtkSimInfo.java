package com.hissage.platfrom;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;

import com.hissage.util.data.NmsConsts;
import com.hissage.util.log.NmsLog;

public class NmsMtkSimInfo extends NmsPlatformBase {

    private static final String TAG = "NmsMtkSimInfo";
    protected Class<?> mSystemSimManager = null;
    protected Class<?> mSystemSimInfoClass = null;
    protected Class<?> mMessageUtilsClass = null;
    private static final String MANAGER_CLASS_PATH = "com.mediatek.telephony.SimInfoManager";
    private static final String INFO_CLASS_PATH = "com.mediatek.telephony.SimInfoManager$SimInfoRecord";
    private static final String MESSAGE_UTILS_CLASS_PATH = "com.android.mms.ui.MessageUtils";
    private Context mMMSContext = null;
    
    public NmsMtkSimInfo(Context context) {
        super(context);
        try {
            mMMSContext = context.createPackageContext("com.android.mms",
                    Context.CONTEXT_IGNORE_SECURITY|Context.CONTEXT_INCLUDE_CODE);
            mMessageUtilsClass = Class.forName(MESSAGE_UTILS_CLASS_PATH,true,mMMSContext.getClassLoader());
            
            mSystemSimManager = Class.forName(MANAGER_CLASS_PATH);
            mSystemSimInfoClass = Class.forName(INFO_CLASS_PATH);
            mPlatfromMode = NMS_INTEGRATION_MODE;
        } catch (Exception e) {
            mPlatfromMode = NMS_STANDEALONE_MODE;
            NmsLog.warn(TAG, e.toString());
        }
    }

    public int getColor(Context context, long simId) {
        if (mSystemSimInfoClass != null && NMS_INTEGRATION_MODE == mPlatfromMode
                && mSystemSimManager != null) {
            try {
                int color = -1;
                Method method = mSystemSimManager.getMethod("getSimInfoById", Context.class,
                        long.class);

                Field field = mSystemSimInfoClass.getField("mSimBackgroundRes");
                field.setAccessible(true);

                Object simInfo = (Object) method.invoke(mSystemSimManager, context, simId);
                if (simInfo != null) {
                    color = (Integer) field.get(simInfo);
                }

                NmsLog.trace(TAG, "getColor, return " + simId + ", platfrom mode: "
                        + getModeString());
                return color;
            } catch (Exception e) {
                NmsLog.warn(TAG, e.toString());
                return 0;
            }
        } else {
            return 0;
        }
    }

    public String getName(Context context, long simId) {
        if (mSystemSimInfoClass != null && NMS_INTEGRATION_MODE == mPlatfromMode && mSystemSimManager != null) {
            try {
                String name = null;
                Method method = mSystemSimManager.getMethod("getSimInfoById", Context.class,
                        long.class);

                Field field = mSystemSimInfoClass.getField("mDisplayName");
                field.setAccessible(true);

                Object simInfo = (Object) method.invoke(mSystemSimManager, context, simId);
                if (simInfo != null) {
                    name = (String) field.get(simInfo);
                }

                NmsLog.trace(TAG, "getName, return " + simId + ", platfrom mode: "
                        + getModeString());
                return name;
            } catch (Exception e) {
                NmsLog.warn(TAG, e.toString());
                return null;
            }
        } else {
            return null;
        }
    }

    public int getSlotIdBySimId(Context context, long simId) {
        if (mSystemSimInfoClass != null && NMS_INTEGRATION_MODE == mPlatfromMode && mSystemSimManager != null) {
            try {
                int slotId = -2;
                Method method = mSystemSimManager.getMethod("getSlotById", Context.class,
                        long.class);

                slotId = (Integer) method.invoke(null, context, simId);
                NmsLog.trace(TAG, "getSlotIdBySimId, return: " + slotId + ", platfrom mode: "
                        + getModeString());
                return slotId;
            } catch (Exception e) {
                NmsLog.nmsPrintStackTrace(e);
                return 0;
            }
        } else {
            return 0;
        }
    }

    public long getSimIdBySlot(Context context, int soltId) {
        if (mSystemSimInfoClass != null && NMS_INTEGRATION_MODE == mPlatfromMode && mSystemSimManager != null) {
            try {
                long simId = NmsConsts.INVALID_SIM_ID;
                Method method = mSystemSimManager.getMethod("getSimInfoBySlot", Context.class,
                        int.class);

                Field field = mSystemSimInfoClass.getField("mSimInfoId");
                field.setAccessible(true);

                Object simInfo = (Object) method.invoke(mSystemSimManager, context, soltId);
                if (simInfo != null) {
                    simId = (Long) field.get(simInfo);
                }

                NmsLog.trace(TAG, "getSimIdBySlot, return " + simId + ", platfrom mode: "
                        + getModeString());
                return simId;
            } catch (Exception e) {
                NmsLog.nmsPrintStackTrace(e);
                return 0;
            }
        } else {
            if (soltId == 0) {
                return getGoogleDefaultSimId(context);
            } else {
                return 0;
            }
        }
    }
    
    public CharSequence getSimIndicator(int simId){
        if (mMessageUtilsClass != null && NMS_INTEGRATION_MODE == mPlatfromMode && mSystemSimManager != null) {
            try {
                Method method = mMessageUtilsClass.getMethod("getSimInfoSync", Context.class, int.class);
                return (CharSequence)method.invoke(null, mMMSContext, simId);
            }catch(Exception e){
                NmsLog.nmsPrintStackTrace(e);
                return null;
            }
        }
        return null;
    }
}
