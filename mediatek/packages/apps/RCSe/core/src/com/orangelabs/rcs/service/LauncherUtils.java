/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.rcse.plugin.apn.RcseOnlyApnUtils;
import com.mediatek.rcse.service.ApiService;

import com.orangelabs.rcs.addressbook.AuthenticationService;
import com.orangelabs.rcs.platform.registry.AndroidRegistryFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provisioning.https.HttpsProvisioningService;
import com.orangelabs.rcs.service.api.client.ClientApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.ArrayList;

/**
 * Launcher utility functions
 *
 * @author hlxn7157
 */
public class LauncherUtils {
    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(LauncherUtils.class.getName());
    /**
     * Last user account used
     */
    public static final String REGISTRY_LAST_USER_ACCOUNT = "LastUserAccount";

    /** M: if no vodafone sim card, disable service @{ */
    private static final int[] VODAFONE_NUMBERICS = {
            20205, 20404, 21401, 21406, 21670, 22210, 22601, 23003, 23099, 23403, 23415, 26202,
            26204, 26209, 26801, 27201, 27402, 27403, 27602, 27801, 28001, 28602, 28802, 40401,
            40405, 40411, 40413, 40415, 40420, 40427, 40430, 40446, 40460, 40484, 40486, 40488,
            40566, 42702, 50503, 50506, 50512, 50538, 53001, 54201, 60202, 62002, 90119, 90128,
            405750, 405751, 405752, 405753, 405754, 405755, 405756
    };

    /**
     * M: Debug mode flag.@{
     */
    public static boolean sIsDebug = true;
    /**
     * @}
     */
   
    /**
     * Launch the RCS service.
     *
     * @param context application context
     * @param boot indicates if RCS is launched from the device boot
     */
    public static void launchRcsService(Context context, boolean boot) {

        /**
         * M: Added to send broadcast whether the device has SIM card. @{
         */
        if (context == null) {
            if (logger.isActivated()) {
                logger.info("launchRcsService()-context is null");
            }
            return;
        } else {
            if (!checkSimCard() && !LauncherUtils.sIsDebug) {
                if (logger.isActivated()) {
                    logger.info("launchRcsService()-checkVodafoneSimCard return false");
                    logger.info("launchRcsService()-send no SIM card broadcast");
                }
                Intent intent = new Intent();
                intent.setAction(StartService.CONFIGURATION_STATUS);
                intent.putExtra(ApiService.CORE_CONFIGURATION_STATUS, false);
                context.sendBroadcast(intent);

                if (!sIsDebug) {
                    if (logger.isActivated()) {
                        logger.error("launchRcsService()-current release version");
                    }
                    return;
                } else {
                    if (logger.isActivated()) {
                        logger.error("launchRcsService()-current debug version");
                    }
                }
            } else {
                if (logger.isActivated()) {
                    logger.info("launchRcsService()-checkVodafoneSimCard return true");
                    logger.info("launchRcsService()-send has SIM card broadcast");
                }
                Intent intent = new Intent();
                intent.setAction(StartService.CONFIGURATION_STATUS);
                intent.putExtra(ApiService.CORE_CONFIGURATION_STATUS, true);
                context.sendBroadcast(intent);
            }
            /**
             * @}
             */
            Intent intent = new Intent(StartService.SERVICE_NAME);
        intent.putExtra("boot", boot);
        context.startService(intent);
        }
    }

    private static boolean checkSimCard() {
        if (logger.isActivated()) {
            logger.debug("checkSimCard() entry");
        }
        String numberic = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC,
                "-1");
        ArrayList<String> numbericList = new ArrayList<String>();
        numbericList.add(numberic);
        if (logger.isActivated()) {
            logger.debug("checkSimCard() numberic: " + numberic);
        }
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (logger.isActivated()) {
                logger.debug("checkSimCard() GEMINI SUPPORT");
            }
            String numberic2 = SystemProperties.get(
                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, "-1");
            numbericList.add(numberic2);
            if (logger.isActivated()) {
                logger.debug("checkSimCard() numberic2: " + numberic2);
            }
        } else {
            if (logger.isActivated()) {
                logger.debug("checkSimCard() connectionEvent GEMINI NOT SUPPORT");
            }
        }
        int size = numbericList.size();
        boolean isSimCardMatched = false;
        for (int i = 0; i < size; i++) {
            if (simCardMatched(numbericList.get(i))) {
                isSimCardMatched = true;
                break;
            }
        }
        if (logger.isActivated()) {
            logger.debug("checkSimCard() exit, isSimCardMatched: " + isSimCardMatched);
        }
        return isSimCardMatched;
    }

    private static boolean simCardMatched(String numberic) {
        if (logger.isActivated()) {
            logger.debug("simCardMatched() entry, numberic: " + numberic);
        }
        if ("-1".equals(numberic)) {
            return false;
        } else {
            return true;
        }
    }

    private static boolean binarySearch(int[] list, int value) {
        int low = 0;
        int mid = 0;
        int high = list.length - 1;
        while (low <= high) {
            mid = (high + low) / 2;
            if (list[mid] == value) {
                return true;
            }
            if (list[mid] < value) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return false;
    }

    /** @} */

    /**
     * Launch the RCS core service
     *
     * @param context Application context
     */
    public static void launchRcsCoreService(Context context) {
        if (logger.isActivated()) {
            logger.debug("Launch core service");
        }
        if (RcsSettings.getInstance().isServiceActivated()) {
            /**
             * M: Added to achieve the RCS-e only APN feature.@{
             */
            if (RcsSettings.getInstance().isUserProfileConfigured()) {
                Intent intent = new Intent(RcsCoreService.SERVICE_NAME);
                if (RcsSettings.getInstance().isRcseOnlyApnEnabled()
                        && !RcseOnlyApnUtils.getInstance(context).isRcsOnlyApnStarted()) {
                    RcseOnlyApnUtils.getInstance(context).switchRcseOnlyApn();
                } else {
                    context.startService(intent);
                }
            } else {
                if (logger.isActivated()) {
                    logger.debug("RCS service not configured");
                }
            }
            /**
             * @}
             */
        } else {
	        if (logger.isActivated()) {
	            logger.debug("RCS service is disabled");
	        }        	
        }
    }

    /**
     * Force launch the RCS core service
     *
     * @param context Application context
     */
    public static void forceLaunchRcsCoreService(Context context) {
        if (logger.isActivated()) {
            logger.debug("Force launch core service");
        }
    	if (RcsSettings.getInstance().isUserProfileConfigured()) {
            RcsSettings.getInstance().setServiceActivationState(true);
        context.startService(new Intent(RcsCoreService.SERVICE_NAME));
        } else {
            if (logger.isActivated()) {
                logger.debug("RCS service not configured");
            }
        }
    }

    /**
     * Stop the RCS service
     *
     * @param context Application context
     */
    public static void stopRcsService(Context context) {
        if (logger.isActivated()) {
            logger.debug("Stop RCS service");
        }
        context.stopService(new Intent(StartService.SERVICE_NAME));
        context.stopService(new Intent(HttpsProvisioningService.SERVICE_NAME));
        context.stopService(new Intent(RcsCoreService.SERVICE_NAME));
    }
    
    /**
     * Reset RCS config
     *
     * @param context Application context
     * @param forbiddenRcse True if the RCSe service is forbidden by
     *            operator,otherwise is not forbidden
     */
    public static void resetRcsConfig(Context context,boolean forbiddenRcse) {
        if (logger.isActivated()) {
            logger.debug("Reset RCS config");
        }

        // Stop the Core service
        context.stopService(new Intent(ClientApiUtils.RCS_SERVICE_NAME));

        // Reset user profile
        RcsSettings.createInstance(context);
        RcsSettings.getInstance().resetUserProfile(forbiddenRcse);

        // Clean the RCS database
        ContactsManager.createInstance(context);
        ContactsManager.getInstance().deleteRCSEntries();

        // Remove the RCS account 
        AuthenticationService.removeRcsAccount(context, null);

        // Clean terms status
        RcsSettings.getInstance().setProvisioningTermsAccepted(false);
    }

    /**
     * Get the last user account
     *
     * @param context Application context
     * @return last user account
     */
    public static String getLastUserAccount(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getString(REGISTRY_LAST_USER_ACCOUNT, null);
    }

    /**
     * Set the last user account
     *
     * @param context Application context
     * @param value last user account
     */
    public static void setLastUserAccount(Context context, String value) {
        SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(REGISTRY_LAST_USER_ACCOUNT, value);
        editor.commit();
    }

    /**
     * Get current user account
     *
     * @param context Application context
     * @return current user account
     */
    public static String getCurrentUserAccount(Context context) {
        TelephonyManager mgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String currentUserAccount = mgr.getSubscriberId();
        mgr = null;
        return currentUserAccount;
    }
    
    /** M: Reset all RCS-e config @{ */
    /**
     * Reset all RCS-e config
     * 
     * @param context application context
     */
    public static void resetAllRcsConfig(Context context) {
        if (logger.isActivated()) {
            logger.debug("Reset all RCS-e config");
        }
        // Clean the RCS user profile
        RcsSettings.getInstance().removeAllUsersProfile();
        // Clean the RCS databases
        ContactsManager.createInstance(context);
        ContactsManager.getInstance().deleteRCSEntries();
        
        // Remove the RCS account
        AuthenticationService.removeRcsAccount(context, null);
        
        resetBackupAccounts(context);
    }
    
    //Delete account from shared preference
    private static void resetBackupAccounts(Context context) {
        if (logger.isActivated()) {
            logger.debug("resetBackupAccount");
        }
        SharedPreferences preferences = context.getSharedPreferences(
                AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(StartService.REGISTRY_LAST_FIRST_USER_ACCOUNT, "");
        editor.putString(StartService.REGISTRY_LAST_SECOND_USER_ACCOUNT, "");
        editor.putString(StartService.REGISTRY_LAST_THIRD_USER_ACCOUNT, "");
        editor.commit();
    }
    /** @} */
    
    /** 
     * M: Added for checking whether the configuration is validity @{ 
     */
    public static long isProvisionValidity(){
        long provisionValidify = RcsSettings.getInstance().getProvisionValidity();
        long provisionTime = RcsSettings.getInstance().getProvisionTime();
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - provisionTime;
        if (logger.isActivated()) {
            logger.debug("isProvisionValidity(), provisionValidify = "
                    + provisionValidify + ", provisionTime = " + provisionTime
                    + ", currentTime = " + currentTime + ", diff = " + diff);
        }
        if (diff < 0) {
            return diff;
        }
        return provisionValidify - diff;
    }
}
