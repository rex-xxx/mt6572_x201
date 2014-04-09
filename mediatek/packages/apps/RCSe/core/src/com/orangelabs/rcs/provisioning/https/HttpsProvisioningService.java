/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

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

package com.orangelabs.rcs.provisioning.https;

import com.mediatek.rcse.activities.ConfigMessageActicity;
import com.mediatek.rcse.service.ApiService;
import com.orangelabs.rcs.R;
import com.orangelabs.rcs.platform.registry.AndroidRegistryFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provisioning.ProvisioningInfo;
import com.orangelabs.rcs.provisioning.ProvisioningParser;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.service.StartService;
import com.orangelabs.rcs.utils.HttpUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.os.AsyncTask;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTPS auto configuration service
 * 
 * @author jexa7410
 */
public class HttpsProvisioningService extends Service {
	/**
	 * Service name
	 */
	public static final String SERVICE_NAME = "com.orangelabs.rcs.provisioning.HTTPS";

    /**
     * Key for provisioning version 
     */
    private static final String REGISTRY_PROVISIONING_VERSION = "ProvisioningVersion";

    /**
     * Intent key
     */
    public static final String FIRST_KEY = "first";

    /**
	 * Unknown value
	 */
	private static final String UNKNOWN = "unknown";

    /**
     * Retry base timeout - 5min 
     */
    private static final int RETRY_BASE_TIMEOUT = 300000;
    
    /**
     * Http connection timeout- 3min 
     */
    private static final int HTTP_TIME_OUT = 3 * 60 * 1000;

    /**
     * Retry max count
     */
    private static final int RETRY_MAX_COUNT = 5;

	/**
	 * Connection manager
	 */
	private ConnectivityManager connMgr = null;

	/**
     * Retry intent
     */
    private PendingIntent retryIntent;

    /**
     * First launch flag
     */
    private boolean first = false;

    /**
     * Check if a provisioning request is already pending
     */
    private boolean isPending = false;

    /**
     * Network state listener
     */
    private BroadcastReceiver networkStateListener = null;

    /**
     * Retry counter
     */
    private int retryCount = 0;

	/**
	 * The logger
	 */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Indicates whether the application in state of debugging auto configuration.
     */
    private static volatile boolean sIsDebugAutoConfig = true;
    
    /**
     * M: Modified to resolve the JE issue. @{
     */
    /**
     * Indicate whether network state listener has been registered.
     */
    private final AtomicBoolean mIsRegisteredAtomicBoolean = new AtomicBoolean();
    /**
     * @]
     */

    private static final String PASSWORD = "1234567";
    private static final String LOCAL_KEYSTORE_FILE_NAME = "server.bks";
    private static final String KEYSTORE_TYPE = "BKS";
    
    @Override
    public void onCreate() {
        // Instantiate RcsSettings
        RcsSettings.createInstance(getApplicationContext());

    	// Get connectivity manager
        if (connMgr == null) {
            connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        // Register the retry listener
        retryIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(this.toString()), 0);
        registerReceiver(retryReceiver, new IntentFilter(this.toString()));
	}

    @Override
    public void onDestroy() {
        /**
         * M: Modified to resolve the JE issue. @{
         */
		// Unregister network state listener
        if (networkStateListener != null
                && mIsRegisteredAtomicBoolean.compareAndSet(true, false)) {
            unregisterReceiver(networkStateListener);
            networkStateListener = null;
        }
        /**
         * @}
         */

        // Unregister Retry 
        unregisterReceiver(retryReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {    	
    	return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if (logger.isActivated()) {
			logger.debug("Start HTTPS provisioning");
		}

        if (intent != null) {
            first = intent.getBooleanExtra("first", false);
            if (logger.isActivated()) {
                logger.debug("Is first provisioning = " + first);
            }
        }

        /**
         * M: Used to resolve the ANR issue. @{
         */
        new AsyncTask<Void, Void, Boolean>() {
        	
            @Override
            protected Boolean doInBackground(Void... params) {
            	// Send default connection event in background
                return connectionEvent(ConnectivityManager.CONNECTIVITY_ACTION);
            }
            
            @Override
            protected void onPostExecute(Boolean result) {
                if(null != result && !result.booleanValue()){
                	// If the UpdateConfig has NOT been done: 
                    // Instantiate the network listener
                    logger.debug("Instantiate the network listener");
                	networkStateListener = new BroadcastReceiver() {
		                @Override
		                public void onReceive(Context context, final Intent intent) {
		                    logger.debug("Network state has changed, intent.getAction() = " + intent.getAction());
		                            connectionEvent(intent.getAction());
		                        }
		                    };

		            // Register network state listener
		            IntentFilter intentFilter = new IntentFilter();
		            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		            registerReceiver(networkStateListener, intentFilter);
		            /**
		             * M: Modified to resolve the JE issue. @{
		             */
		            mIsRegisteredAtomicBoolean.set(true);
		            /**
		             * @}
		             */
                }else{
                	if (logger.isActivated()) {
            			logger.debug(result == null ? "The result is null" : "The result is true");
            		}
                }
            }

        }.execute();
        /**
         * @}
         */
		
    	// We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Connection event
     * 
     * @param action Connectivity action
     * @return true if the updateConfig has been done
     */
    private boolean connectionEvent(String action) {
        if (!isPending) {
    		if (logger.isActivated()) {
    			logger.debug("Connection event " + action);
    		}
    
    		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
    			// Check received network info
    	    	NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    	    	logger.debug("networkInfo = " + networkInfo);
    	    	//If in debug mode, the auto-configuration will do enven in roaming.
    	    	/**
    	    	 * M: Modified to achieve the roaming and auto configuration related feature. @{
    	    	 */
                if ((networkInfo != null)
                        && networkInfo.isConnected()
                        && (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE ? true : false)
                        && (!networkInfo.isRoaming() || sIsDebugAutoConfig)) {
                    isPending = true;
    				if (logger.isActivated()) {
    					logger.debug("Connected to data network");
                        logger.debug("networkInfo:");
                        logger.debug("networkInfo is connected:" + networkInfo.isConnected());
                        logger.debug("networkInfo is roaming:" + networkInfo.isRoaming());
    				}
    				updateConfig();
    				
    				/**
    			     * M: Modified to resolve the JE issue. @{
    			     */
                    // Unregister network state listener
                    if (networkStateListener != null
                            && mIsRegisteredAtomicBoolean.compareAndSet(true, false)) {
                        unregisterReceiver(networkStateListener);
                        networkStateListener = null;
                    }
                    /**
                     * @}
                     */
                    isPending = false;
                    return true;
                } else {
                    if (logger.isActivated()) {
                        logger.debug("networkInfo is null");
                    }
                    return false;
    			}
                /**
                 * @}
                 */
    		}
        }
        return false;
    }    

    /**
     * M: Added to send broadcast whether the RCS-e service is enable. @{
     */
    private void sendServiceStateBroadcast(boolean isServiceEable){
        if (logger.isActivated()) {
            logger.info("Send broadcast-the RCS-e service is " + (isServiceEable ? "enabled" : "disabled"));
        }
        Intent intent = new Intent();
        intent.setAction(StartService.CONFIGURATION_STATUS);
        intent.putExtra(ApiService.CORE_CONFIGURATION_STATUS, isServiceEable);
        this.sendStickyBroadcast(intent);
    }
    /**
     * @}
     */

    /**
     * M: Modified for presenting the auto configuration information @{
     */
    /**
     * Update provisioning config
     */
    private void updateConfig() {
        new AsyncTask<Void, Void, ProvisioningInfo>() {

            @Override
            protected ProvisioningInfo doInBackground(Void... arg0) {
                // Cancel previous retry alarm
                logger.debug("updateConfig()");
                cancelRetryAlarm();

                /**
                 * M: add for check whether configuration is validity, if it is
                 * validity then do not do auto-configuration. @{
                 */
                if (logger.isActivated()) {
                    logger
                            .debug("check whether configuration is validity, if it is validity then do not do auto-configuration");
                }
                long validity = LauncherUtils.isProvisionValidity();
                if (validity >= 0 && !first) {
                    // Start retry alarm
                    if (logger.isActivated()) {
                        logger
                                .debug("(do not do autoconfiguration)Provisioning retry after valadity "
                                        + validity);
                    }
                    retryCount = 0;
                    startRetryAlarm(validity * 1000);
                    // Start the RCS service
                    LauncherUtils.launchRcsCoreService(getApplicationContext());
                    return null;
                }
                /** @} */
                // Get config via HTTPS
                HttpsProvisioningResult result = getConfig();

                ProvisioningInfo info = null;

                if (result != null) {
                    if (result.code == 200) {
                        // Success
                        if (logger.isActivated()) {
                            logger.debug("Provisioning successful");
                        }

                        // Parse the received content
                        ProvisioningParser parser = new ProvisioningParser(result.content);
                        if (parser.parse()) {
                            /**
                             * M: Add to check whether the info or info.version
                             * is null. @{
                             */
                            info = parser.getProvisioningInfo();
                            if (null == info || null == info.getVersion()) {
                                if (logger.isActivated()) {
                                    logger.debug("The info is "
                                            + (info == null ? "is null" : "not null"));
                                    logger.debug("The info.version is "
                                            + (info.getVersion() == null ? "is null" : "not null"));
                                    logger.debug("Provisioning forbidden: reset account");
                                }
                                // Reset config
                                LauncherUtils.resetRcsConfig(getApplicationContext(), false);
                                LauncherUtils.stopRcsService(getApplicationContext());
                                sendServiceStateBroadcast(false);
                                return null;
                            }
                            /**
                             * @}
                             */
                            // Record provision version, validity and record time.
                            RcsSettings.getInstance().setProvisionVersion(info.getVersion());
                            RcsSettings.getInstance().setProvisionValidity(info.getValidity());
                            RcsSettings.getInstance().setProvisionTime(System.currentTimeMillis());
                            if (logger.isActivated()) {
                                logger.debug("Version " + info.getVersion());
                                logger.debug("Validity " + info.getValidity());
                            }

                            if (info.getVersion().equals("-1") && info.getValidity() == -1) {
                                // Forbidden: reset account + version = 0-1
                                // (doesn't restart)
                                if (logger.isActivated()) {
                                    logger
                                            .debug("Provisioning forbidden: reset account and stop RCS");
                                }
                                // Reset config
                                LauncherUtils.resetRcsConfig(getApplicationContext(), true);
                                LauncherUtils.stopRcsService(getApplicationContext());
                                
                                /**
                                 * M: Added to send broadcast whether the RCS-e service is enable. @{
                                 */
                                sendServiceStateBroadcast(false);
                                /**
                                 * @}
                                 */
                            } else if (info.getVersion().equals("0") && info.getValidity() == 0) {
                                // Forbidden: reset account + version = 0
                                if (logger.isActivated()) {
                                    logger.debug("Provisioning forbidden: reset account");
                                }
                                // Reset config
                                LauncherUtils.resetRcsConfig(getApplicationContext(), false);
                                LauncherUtils.stopRcsService(getApplicationContext());
                                /**
                                 * M: Added to send broadcast whether the RCS-e service is enable. @{
                                 */
                                sendServiceStateBroadcast(false);
                                /**
                                 * @}
                                 */
                            } else {
                                // Start retry alarm
                                if (info.getValidity() > 0) {
                                    if (logger.isActivated()) {
                                        logger.debug("Provisioning retry after valadity "
                                                + info.getValidity());
                                    }
                                    retryCount = 0;
                                    startRetryAlarm(info.getValidity() * 1000);
                                }

                                // Start the RCS service
                                if (first) {
                                    LauncherUtils
                                            .forceLaunchRcsCoreService(getApplicationContext());
                                } else {
                                    LauncherUtils.launchRcsCoreService(getApplicationContext());
                                }
                                /**
                                 * M: Added to send broadcast whether the RCS-e service is enable. @{
                                 */
                                sendServiceStateBroadcast(true);
                                /**
                                 * @}
                                 */
                            }
                        } else {
                            if (logger.isActivated()) {
                                logger.debug("Can't parse provisioning document");
                            }
                            retry();
                        }
                    } else if (result.code == 503) {
                        // Retry after
                        if (logger.isActivated()) {
                            logger.debug("Provisioning retry after " + result.retryAfter);
                        }

                        // Start retry alarm
                        if (result.retryAfter > 0) {
                            retryCount = 0;
                            startRetryAlarm(result.retryAfter * 1000);
                        }

                        // Start the RCS service
                        logger.debug("Autoconfiguration failed. Other error"
                                + " if in debug mode, or is not first launch then start service");
                        logger.debug("sIsDebugAutoConfig = " + sIsDebugAutoConfig + ", first = "
                                + first);
                        if (!first || sIsDebugAutoConfig) {
                            LauncherUtils.launchRcsCoreService(getApplicationContext());
                        }
                    } else if (result.code == 403) {
                        // Forbidden: reset account + version = 0
                        if (logger.isActivated()) {
                            logger.debug("Provisioning forbidden: reset account");
                        }

                        // Reset version to "0"
                        // This function do in resetRcsConfig

                        // Reset config
                        LauncherUtils.resetRcsConfig(getApplicationContext(), false);
                        /**
                         * M: Added to send broadcast whether the RCS-e service is enable. @{
                         */
                        sendServiceStateBroadcast(false);
                        /**
                         * @}
                         */
                    } else {
                        // Other error
                        if (logger.isActivated()) {
                            logger.debug("Provisioning error " + result.code);
                        }
                        // Start the RCS service
                        logger.debug("Autoconfiguration failed. Other error"
                                + " if in debug mode, or is not first launch then start service");
                        logger.debug("sIsDebugAutoConfig = " + sIsDebugAutoConfig + ", first = "
                                + first);
                        if (!first || sIsDebugAutoConfig) {
                            LauncherUtils.launchRcsCoreService(getApplicationContext());
                        }
                        retry();
                    }
                } else {
                    // Start the RCS service
                    logger.debug("Autoconfiguration failed. result == null"
                            + " if in debug mode, or is not first launch then start service");
                    logger.debug("sIsDebugAutoConfig = " + sIsDebugAutoConfig + ", first = "
                            + first);
                    if (!first || sIsDebugAutoConfig) {
                        LauncherUtils.launchRcsCoreService(getApplicationContext());
                    }
                    retry();
                }

                return info;
            }

            @Override
            protected void onPostExecute(ProvisioningInfo result) {
                super.onPostExecute(result);
                if (result != null && result.getMessage() != null) {
                    Intent intent = new Intent();
                    intent.setAction(ConfigMessageActicity.CONFIG_MESSAGE_DIALOG_ACTION);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ConfigMessageActicity.CONFIG_DIALOG_TITLE, result.getTitle());
                    intent.putExtra(ConfigMessageActicity.CONFIG_DIALOG_MESSAGE, result.getMessage());
                    intent.putExtra(ConfigMessageActicity.CONFIG_DIALOG_ACCEPT_BUTTON,
                            result.getAcceptBtn());
                    intent.putExtra(ConfigMessageActicity.CONFIG_DIALOG_REJECT_BUTTON,
                            result.getRejectBtn());
                    HttpsProvisioningService.this.startActivity(intent);
                } else {
                    if (logger.isActivated()) {
                        if (result == null) {
                            logger.debug("onPostExecute error: result is "
                                    + (result == null ? "null" : "not null"));
                        } else {
                            logger.debug("onPostExecute error: result.message is "
                                    + (result.getMessage() == null ? "null" : "not null"));
                        }
                    }
                }
            }
        }.execute();
    }
    /**
     * @}
     */

    /**
     * Retry receiver
     */
    private BroadcastReceiver retryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /**
             * M: Modified to achieve the roaming and auto configuration related feature. @{
             */
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (null != networkInfo && networkInfo.isConnected() && !networkInfo.isRoaming()) {
                if (logger.isActivated()) {
                    logger.debug("networkInfo:");
                    logger.debug("networkInfo is connected:" + networkInfo.isConnected());
                    logger.debug("networkInfo is roaming:" + networkInfo.isRoaming());
                }
                    updateConfig();
            } else {
                if (logger.isActivated()) {
                    logger.debug("networkInfo is null");
                }
            }
            /**
             * @}
             */
        }
    };

    /**
     * Retry procedure
     */
    private void retry() {
        if (retryCount < RETRY_MAX_COUNT) {
            retryCount++;
            int retryDelay = RETRY_BASE_TIMEOUT + 2 * (retryCount - 1) * RETRY_BASE_TIMEOUT;
            startRetryAlarm(retryDelay);
            if (logger.isActivated()) {
                logger.debug("Retry (" + retryCount +  ") provisionning after " + retryDelay + "ms");
            }
        } else {
            if (logger.isActivated()) {
                logger.debug("No more retry for provisionning");
            }
        }
    }

    /**
     * Start retry alarm
     * 
     * @param delay (ms)
     */
    private void startRetryAlarm(long delay) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, retryIntent);
    }

    /**
     * Cancel retry alarm
     */
    private void cancelRetryAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(retryIntent);
    }

    /**
	 * Get configuration
	 * 
	 * @return Result or null in case of internal exception
	 */
	private HttpsProvisioningResult getConfig() {
		HttpsProvisioningResult result = new HttpsProvisioningResult();
		try {
			if (logger.isActivated()) {
				logger.debug("Request config via HTTPS");
			}


	    	// Get SIM info
	    	TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	    	String ope = tm.getSimOperator();
            String mnc = ope.substring(3);
            String mcc = ope.substring(0, 3);
            /**
             * M: add debug mode to connect the auto configuration server
             * because the production is not ready @{
             */
            String requestUri = null;
            String[] requestUris = new String[4];
            if (LauncherUtils.sIsDebug) {
                requestUris[1] = "prepro.config." + mcc + mnc + ".rcse";
                requestUris[3] = "config." + mcc + mnc + ".rcse";
                while (mnc.length() < 3) { // Set mnc on 3 digits
                    mnc = "0" + mnc;
                }
                requestUris[2] = "config.rcs." + "mnc" + mnc + ".mcc" + mcc
                        + ".pub.3gppnetwork.org";
                requestUris[0] = "prepro.config.rcs." + "mnc" + mnc + ".mcc" + mcc
                        + ".pub.3gppnetwork.org";
            } else {
                requestUris[3] = "config." + mcc + mnc + ".rcse";
                while (mnc.length() < 3) { // Set mnc on 3 digits
                    mnc = "0" + mnc;
                }
                requestUris[2] = "config.rcs." + "mnc" + mnc + ".mcc" + mcc
                        + ".pub.3gppnetwork.org";
            }
            /** @} */
			String imsi = tm.getSubscriberId();
			String imei = tm.getDeviceId();
	    	tm = null;

            /**
             * M: Modified to make stack trust both the system default trusted
             * CA certificates and special certificates.@{
             */
            InputStream is = null;
            KeyStore customTrustedKeyStore = null;
            try {
                // load special trusted key store
                Context context = getApplicationContext();
                if (context != null) {
                    is = context.getAssets().open(LOCAL_KEYSTORE_FILE_NAME);
                    customTrustedKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
                    customTrustedKeyStore.load(is, PASSWORD.toCharArray());
                } else {
                    logger.warn("context is null, do not load sepcial keystore");
                }
                logger.debug("customTrustedKeyStore = " + customTrustedKeyStore);
            } catch (KeyStoreException e) {
                logger.debug("KeyStoreException: " + e.getMessage());
            }finally {
                if (is != null) {
                    try {
                        is.close();
                        is = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // Add a custom trusted key store to trust manager
            EasySSLSocketFactory.setCustomTrustedKeyStore(customTrustedKeyStore);
            /**
             * @}
             */
            // Format HTTP request
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

            HttpParams params = new BasicHttpParams();
            params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
            params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(
                    30));
            params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            /**
             * M: Add to set the timeout for HTTP connection. @{
             */
            HttpConnectionParams.setConnectionTimeout(params, HTTP_TIME_OUT);
            /**
             * @}
             */
            ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
            DefaultHttpClient client = new DefaultHttpClient(cm, params);

			// Create a local instance of cookie store
			CookieStore cookieStore = (CookieStore) new BasicCookieStore();

			// Create local HTTP context
			HttpContext localContext = new BasicHttpContext();

			// Bind custom cookie store to the local context
			localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

			// Execute first HTTP request
            /**
             * M: add debug mode to connect the auto configuration server
             * because the production is not ready @{
             */
            HttpResponse response = null;
            int i = 0;
            int size = requestUris.length;
            if (!LauncherUtils.sIsDebug) {
                i = 2;
            }
            for (; i < size; i++) {
                requestUri = requestUris[i];
                try {
                    response = executeRequest("http", requestUri, client, localContext);
                } catch (UnknownHostException e) {
                    // If the new URI is not reachable, try the old
                    if (logger.isActivated()) {
                        logger.debug("The server " + requestUri
                                + " can't be reachable, try the next URI");
                    }
                    continue;
                }
                if (response == null) {
                    if (logger.isActivated()) {
                        logger.debug("The http connection is timeout while "
                                + "perform auto configuration");
                    }
                    return result;
                }
                result.code = response.getStatusLine().getStatusCode();
                if (logger.isActivated()) {
                    logger.debug("result.code = " + result.code);
                }
                if (result.code != 200) {
                    if (result.code == 503) {
                        result.retryAfter = getRetryAfter(response);
                    }
                    if (LauncherUtils.sIsDebug) {
                        // work around for the case that server is not stable.
                        if (result.code != 403 && result.code != 503) {
                            continue;
                        }
                    }
                    return result;
                }

                /**
                 * M: Modified to make stack trust both the system default
                 * trusted CA certificates and special certificates.@{
                 */
                // Execute second HTTPS request
                String args = "?vers=" + RcsSettings.getInstance().getProvisionVersion()
                    + "&client_vendor=" + getClientVendor()
                    + "&client_version=" + getClientVersion()
                    + "&terminal_vendor=" + HttpUtils.encodeURL(getTerminalVendor())
                    + "&terminal_model=" + HttpUtils.encodeURL(getTerminalModel())
                    + "&terminal_sw_version=" + HttpUtils.encodeURL(getTerminalSoftwareVersion())
                        + "&IMSI=" + imsi + "&IMEI=" + imei;
                /**
                 * @}
                 */
                response = executeRequest("https", requestUri + args, client, localContext);
                if (response == null) {
                    if (logger.isActivated()) {
                        logger.debug("The https connection is timeout while "
                                + "perform auto configuration");
                    }
                    return result;
                }
                result.code = response.getStatusLine().getStatusCode();
                if (logger.isActivated()) {
                    logger.debug("result.code = " + result.code);
                }
                if (result.code != 200) {
                    if (result.code == 503) {
                        result.retryAfter = getRetryAfter(response);
                    }
                    if (LauncherUtils.sIsDebug) {
                        // work around for the case that server is not stable.
                        if (result.code != 403 && result.code != 503) {
                            continue;
                        }
                    }
                    return result;
                }
                result.content = EntityUtils.toString(response.getEntity());
                return result;
            }
            return result;
            /** @} */
		} catch(UnknownHostException e) {
			if (logger.isActivated()) {
				logger.warn("Provisioning server not reachable");
			}
			return null;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get config via HTTPS", e);
			}
			return null;
		}
	}

    /**
     * Execute an HTTP request
     *
     * @param protocol HTTP protocol
     * @param request HTTP request
     * @return HTTP response
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    private HttpResponse executeRequest(String protocol, String request, DefaultHttpClient client, HttpContext localContext) throws URISyntaxException, ClientProtocolException, IOException {
        HttpGet get = new HttpGet();
        get.setURI(new URI(protocol + "://" + request));
        if (logger.isActivated()) {
            logger.debug(protocol + " request: " + get.getURI().toString());
        }
        HttpResponse response = client.execute(get, localContext);
        if (logger.isActivated()) {
            logger.debug(protocol + " response: " + response.getStatusLine().toString());
        }
        return response;
    }

    /**
     * Get retry-after value
     * 
     * @return retry-after value
     */
    private int getRetryAfter(HttpResponse response) {
        Header[] headers = response.getHeaders("Retry-After");
        if (headers.length > 0) {
            try {
                return Integer.parseInt(headers[0].getValue());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Returns the client vendor
     * 
     * @return String(4)
     */
	private String getClientVendor() {
		String result = UNKNOWN;
		String version = getString(R.string.rcs_client_vendor);
		if (version != null && version.length() > 0) {
			result = version;
		}
		return StringUtils.truncate(result, 4);
	}    

	/**
     * Returns the client version
     * 
     * @return String(15)
     */
	private String getClientVersion() {
		String result = UNKNOWN;
		String version = getString(R.string.rcs_core_release_number);
		if (version != null && version.length() > 0) {
			String[] values = version.split(".");
			if (values.length > 2) { 
				result = values[0] + "." + values[1];
			} else {
				result = version;
			}
		}
		return StringUtils.truncate(result, 15);
	}

	/**
     * Returns the terminal vendor
     * 
     * @return String(4)
     */
	private String getTerminalVendor() {
		String result = UNKNOWN;
		String productmanufacturer = getSystemProperties("ro.product.manufacturer");
		if (productmanufacturer != null && productmanufacturer.length() > 0) {
			result = productmanufacturer;
		}
		return StringUtils.truncate(result, 4);
	}   
	
    /**
     * Returns the terminal model
     * 
     * @return String(10)
     */
	private String getTerminalModel() {
		String result = UNKNOWN;
		String devicename = getSystemProperties("ro.product.device");
		if (devicename != null && devicename.length() > 0) {
			result = devicename;
		}
		return StringUtils.truncate(result, 10);
	}
			
    /**
     * Returns the terminal software version
     * 
     * @return String(10)
     */
    private String getTerminalSoftwareVersion() {
        String result = UNKNOWN;
        /**
         * M: Modified to make stack trust both the system default trusted CA
         * certificates and special certificates.@{
         */
        String productversion = getSystemProperties("ro.mediatek.version.release");
        /**
         * @}
         */
        if (productversion != null && productversion.length() > 0) {
            result = productversion;
        }
        return StringUtils.truncate(result, 10);
    }

    /**
     * M:Change "private" to "public static" for other class to call 
     * it. @{T-Mobile 
     */
	public static String getSystemProperties(String key) {
		String value = null;
		try {
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("get", String.class);
			value = (String)get.invoke(c, key);
			return value;
		} catch(Exception e) {
			return UNKNOWN;
		}		
	}
    /**
     * T-Mobile@}
     */	

    /**
     * Get the provisioning version from the registry
     *
     * @param context application context
     * @return provisioning version
     */
    public static String getProvisioningVersion(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        return preferences.getString(REGISTRY_PROVISIONING_VERSION, "0");
    }

    /**
     * Write the provisioning version in the registry
     *
     * @param context application context
     * @param value provisioning version
     */
    public static void setProvisioningVersion(Context context, String value) {
        SharedPreferences preferences = context.getSharedPreferences(AndroidRegistryFactory.RCS_PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(REGISTRY_PROVISIONING_VERSION, value);
        editor.commit();
    }
    
    /**
     * A api help to turn on/off the state of sIsDebugAutoconfig.
     * 
     * @param isDebugAutoconfig Indicates whether the application in state of
     *            debugging auto configuration.
     */
    public void setDebugAutoConfig(boolean isDebugAutoConfig) {
        logger.debug("setDebugAutoConfig(), isDebugAutoConfig = " + isDebugAutoConfig);
        sIsDebugAutoConfig = isDebugAutoConfig;
    }
}
