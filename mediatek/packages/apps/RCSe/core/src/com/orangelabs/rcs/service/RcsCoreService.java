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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Looper;

import com.mediatek.rcse.service.FlightModeApiService;
import com.mediatek.rcse.service.IFlightMode;
import com.mediatek.rcse.service.PreShutdownApiService;
import com.orangelabs.rcs.R;
import com.orangelabs.rcs.addressbook.AccountChangedReceiver;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreListener;
import com.orangelabs.rcs.core.TerminalInfo;
import com.orangelabs.rcs.core.ims.ImsError;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.NetworkApiService;
import com.orangelabs.rcs.core.ims.network.NetworkConnectivityApi;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingOne2OneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardMsgSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.presence.PresenceUtils;
import com.orangelabs.rcs.core.ims.service.presence.pidf.OverridingWillingness;
import com.orangelabs.rcs.core.ims.service.presence.pidf.Person;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.orangelabs.rcs.core.ims.service.presence.pidf.Tuple;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.sip.TerminatingSipSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.core.ims.network.INetworkConnectivityApi;
import com.orangelabs.rcs.service.api.client.ClientApiIntents;
import com.orangelabs.rcs.service.api.client.IImsApi;
import com.orangelabs.rcs.service.api.client.capability.Capabilities;
import com.orangelabs.rcs.service.api.client.capability.CapabilityApiIntents;
import com.orangelabs.rcs.service.api.client.capability.ICapabilityApi;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.messaging.IMessagingApi;
import com.orangelabs.rcs.service.api.client.presence.FavoriteLink;
import com.orangelabs.rcs.service.api.client.presence.Geoloc;
import com.orangelabs.rcs.service.api.client.presence.IPresenceApi;
import com.orangelabs.rcs.service.api.client.presence.PhotoIcon;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.service.api.client.presence.PresenceInfo;
import com.orangelabs.rcs.service.api.client.richcall.IRichCallApi;
import com.orangelabs.rcs.service.api.client.sip.ISipApi;
import com.orangelabs.rcs.service.api.client.terms.ITermsApi;
import com.orangelabs.rcs.service.api.server.ImsApiService;
import com.orangelabs.rcs.service.api.server.capability.CapabilityApiService;
import com.orangelabs.rcs.service.api.server.messaging.MessagingApiService;
import com.orangelabs.rcs.service.api.server.presence.PresenceApiService;
import com.orangelabs.rcs.service.api.server.richcall.RichCallApiService;
import com.orangelabs.rcs.service.api.server.sip.SipApiService;
import com.orangelabs.rcs.service.api.server.terms.TermsApiService;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RCS core service. This service offers a flat API to any other process (activities)
 * to access to RCS features. This service is started automatically at device boot.
 * 
 * @author jexa7410
 */
public class RcsCoreService extends Service implements CoreListener {
	/**
	 * Service name
	 */
	public static final String SERVICE_NAME = "com.orangelabs.rcs.SERVICE";

	/**
	 * Notification ID
	 */
	private final static int SERVICE_NOTIFICATION = 1000;
	
	/**
	 * CPU manager
	 */
	private CpuManager cpuManager = new CpuManager();

	/**
	 * IMS API
	 */
	private ImsApiService imsApi = new ImsApiService(); 
	
	/**
	 * Terms API
	 */
    private TermsApiService termsApi = new TermsApiService(); 

    /**
	 * Presence API
	 */
    private PresenceApiService presenceApi = new PresenceApiService(); 

	/**
	 * Capability API
	 */
    private CapabilityApiService capabilityApi = new CapabilityApiService(); 
    
	/**
	 * Messaging API
	 */
	private MessagingApiService messagingApi = new MessagingApiService(); 

	/**
	 * Rich call API
	 */
	private RichCallApiService richcallApi = new RichCallApiService(); 
	
	/**
	 * SIP API
	 */
	private SipApiService sipApi = new SipApiService(); 

    /**
     * M: add for auto-rejoin group chat @{
     */
    private NetworkApiService mNetworkConnectivityApi = new NetworkApiService();
    /** @} */

    /**
     * Account changed broadcast receiver
     */
    private AccountChangedReceiver accountChangedReceiver = null;
    
	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(RcsCoreService.class.getClass().getName());

	/**
     * M: Added to resolve the issue that can't send message successfully
     * sometimes.@{
     */
    private static final String THREAD_NAME_START_CORE = "Start Core Service";

    private Looper mStartServiceLooper = null;

    final private Thread mStartCoreServiceThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setName(THREAD_NAME_START_CORE);
            if (!CORE_SERVICE_MANAGER.handleNewAction(true)) {
                logger.debug("start core service thread: perform launch");
                CORE_SERVICE_MANAGER.onCoreStarting();
                if (Looper.myLooper() != null) {
                    startCore();
                } else {
                    Looper.prepare();
                    if (startCore()) {
                        if (null == mStartServiceLooper) {
                            mStartServiceLooper = Looper.myLooper();
                        } else {
                            logger.error("start core service thread: mStartServiceLooper is not null");
                        }
                        CORE_SERVICE_MANAGER.onCoreStarted();
                        Looper.loop();
                    } else {
                        logger.error("start core service thread: failed");
                    }
                }
            } else {
                logger.error("start core service thread: take it as the pending action");
                stopSelf();
            }
        }
    });
    /**
     * @}
     */
    
    /**
     * M: Added to get do unregister before device go to power off and flight mode.@{
     */
    //Flight mode 
    private FlightModeApiService mFlightModeApi = new FlightModeApiService(); 
    
    //Power off
    private PreShutdownApiService mPreShutdownApi = new PreShutdownApiService();
    private static final String PRE_SHUTDOWN = "com.mediatek.IPreShutdown";
    /**
     * @}
     */
    
    @Override
    public void onCreate() {
        logger.debug("onCreate() entry");
		// Set application context
		AndroidFactory.setApplicationContext(getApplicationContext());
		// Set the terminal version
		TerminalInfo.setProductVersion(getString(R.string.rcs_core_release_number));
		// Start the core
		mStartCoreServiceThread.start();
		logger.debug("onCreate() exit");
	}
	
    @Override
    public void onDestroy() {
        /**
         * M: Added to resolve the issue that can't send message successfully
         * sometimes.@{
         */
        try {
            // Unregister account changed broadcast receiver
            if (accountChangedReceiver != null) {
                unregisterReceiver(accountChangedReceiver);
            }
        } catch (IllegalArgumentException e) {
            if (logger.isActivated()) {
                logger.error("Receiver not registered");
            }
        }
        if (!CORE_SERVICE_MANAGER.handleNewAction(false)) {
            CORE_SERVICE_MANAGER.onCoreStopping();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    // Close APIs
                    imsApi.close();
                    termsApi.close();
                    presenceApi.close();
                    capabilityApi.close();
                    richcallApi.close();
                    messagingApi.close();
                    sipApi.close();
                    // Stop the core
                    stopCore();
                    if (null != mStartServiceLooper) {
                        Thread startThread = mStartServiceLooper.getThread();
                        if (startThread.isAlive()) {
                            mStartServiceLooper.quit();
                            mStartServiceLooper = null;
                            logger.debug("onDestroy() clear mStartServiceLooper");
                        } else {
                            logger.error("onDestroy() startThread is not alive");
                        }
                    } else {
                        logger.error("onDestroy() mStartServiceLooper is null");
                    
                    }
                    CORE_SERVICE_MANAGER.onCoreStopped();
                }
            });
        } else {
            logger.error("onDestroy() will not be released");
        }
        /**
         * @}
         */
    }

    /**
     * M: Modified to resolve the issue that can't send message successfully
     * sometimes.@{
     */
    /**
     * Start core
     */
    public synchronized boolean startCore() {
        if (Core.getInstance() != null) {
            // Already started
            return false;
        }

        try {
    		if (logger.isActivated()) {
    			logger.debug("Start RCS core service");
    		}
    		
    		// Send service intent 
			Intent intent = new Intent(ClientApiIntents.SERVICE_STATUS);
			intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STARTING);
			getApplicationContext().sendBroadcast(intent);

			// Instantiate the settings manager
            RcsSettings.createInstance(getApplicationContext());
            
            // Set the logger properties
    		Logger.activationFlag = RcsSettings.getInstance().isTraceActivated();
    		String traceLevel = RcsSettings.getInstance().getTraceLevel();
    		if (traceLevel.equalsIgnoreCase("DEBUG")) {
        		Logger.traceLevel = Logger.DEBUG_LEVEL;    			
    		} else if (traceLevel.equalsIgnoreCase("INFO")) {
        		Logger.traceLevel = Logger.INFO_LEVEL;
    		} else if (traceLevel.equalsIgnoreCase("WARN")) {
        		Logger.traceLevel = Logger.WARN_LEVEL;
    		} else if (traceLevel.equalsIgnoreCase("ERROR")) {
        		Logger.traceLevel = Logger.ERROR_LEVEL;
    		} else if (traceLevel.equalsIgnoreCase("FATAL")) {
        		Logger.traceLevel = Logger.FATAL_LEVEL;
    		}    		

            // Terminal version
            if (logger.isActivated()) {
                logger.info("My RCS software release is " + TerminalInfo.getProductVersion());
            }

			// Instantiate the contacts manager
            ContactsManager.createInstance(getApplicationContext());

            // Instantiate the rich messaging history 
            RichMessaging.createInstance(getApplicationContext());
            
            // Instantiate the rich call history 
            RichCall.createInstance(getApplicationContext());

            // Create the core
			Core.createCore(this);

			// Start the core
			Core.getInstance().startCore();		

			// Create multimedia directory on sdcard
			FileFactory.createDirectory(FileFactory.getFactory().getPhotoRootDirectory());
			FileFactory.createDirectory(FileFactory.getFactory().getVideoRootDirectory());
			FileFactory.createDirectory(FileFactory.getFactory().getFileRootDirectory());
			
			// Init CPU manager
			cpuManager.init();

	        // Show a first notification

			// Send service intent 
			intent = new Intent(ClientApiIntents.SERVICE_STATUS);
			intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STARTED);
			getApplicationContext().sendBroadcast(intent);

			if (logger.isActivated()) {
				logger.info("RCS core service started with success");
			}
			return true;
		} catch(Exception e) {
			// Unexpected error
			if (logger.isActivated()) {
				logger.error("Can't instanciate the RCS core service", e);
			}
			
			// Send service intent 
			Intent intent = new Intent(ClientApiIntents.SERVICE_STATUS);
			intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_FAILED);
			getApplicationContext().sendBroadcast(intent);

			// Show error in notification bar
	    	
			// Exit service
	    	stopSelf();
	    	return false;
		}
    }
    /**
     * @}
     */
    
    /**
     * Stop core
     */
    public synchronized void stopCore() {
		if (Core.getInstance() == null) {
			// Already stopped
			return;
		}
		
		if (logger.isActivated()) {
			logger.debug("Stop RCS core service");
		}

		// Send service intent 
		Intent intent = new Intent(ClientApiIntents.SERVICE_STATUS);
		intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STOPPING);
		getApplicationContext().sendBroadcast(intent);
		
		// Terminate the core in background
		Core.terminateCore();

		// Close CPU manager
		cpuManager.close();

		// Send service intent 
		intent = new Intent(ClientApiIntents.SERVICE_STATUS);
		intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STOPPED);
		getApplicationContext().sendBroadcast(intent);

		if (logger.isActivated()) {
			logger.info("RCS core service stopped with success");
		}
		
    }

    @Override
    public IBinder onBind(Intent intent) {   
        /**
         * M: add for auto-rejoin group chat @{
         */
        if (logger.isActivated()) {
            logger.debug("onBind() action: " + intent.getAction());
        }
        /** @} */
        if (IImsApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("IMS API binding");
    		}
            return imsApi;
        } else
        if (ITermsApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Terms API binding");
    		}
            return termsApi;
        } else
        if (IPresenceApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Presence API binding");
    		}
            return presenceApi;
        } else
        if (ICapabilityApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Capability API binding");
    		}
            return capabilityApi;
        } else
        if (IMessagingApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Messaging API binding");
    		}
            return messagingApi;
        } else
        if (IRichCallApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Rich call API binding");
    		}
            return richcallApi;
        } else
        if (ISipApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("SIP API binding");
    		}
            return sipApi;
            
            /**
             * M: Modified to do unregister before device go to power off and
             * flight mode.@{
             */
        } else if (IFlightMode.class.getName().equals(intent.getAction())) {
            if (logger.isActivated()) {
                logger.debug("FlightMode API binding");
            }
            return mFlightModeApi;

        } else if (PRE_SHUTDOWN.equals(intent.getAction())) {
            if (logger.isActivated()) {
                logger.debug("PreShutdown API binding");
            }
            return mPreShutdownApi;
            /**
             * @}
             */
            /**
             * M : add for auto-rejoin group chat. @{
             */
        } else if (INetworkConnectivityApi.class.getName().equals(intent.getAction())) {
            if (logger.isActivated()) {
                logger.debug("Network API binding");
            }
            return mNetworkConnectivityApi;
            /** @} */
        } else {
            return null;
        }
    }
    
    /**
     * M: Remove to meet with JRD hot fixes 7.4 @{
     */
    /**
     * @}
     */
    
    /*---------------------------- CORE EVENTS ---------------------------*/
    
    /**
     * Core layer has been started
     */
    public void handleCoreLayerStarted() {
		if (logger.isActivated()) {
			logger.debug("Handle event core started");
		}

		// Display a notification
    }

    /**
     * Core layer has been terminated
     */
    public void handleCoreLayerStopped() {
		if (logger.isActivated()) {
			logger.debug("Handle event core terminated");
		}
        /**
         * M: Modify to meet with JRD hot fixes 7.4 @{
         */
        removeRcseServiceNotification(getApplicationContext());
        /**
         * @}
         */
    }
    
	/**
	 * Handle "registration successful" event
	 * 
	 * @param registered Registration flag
	 */
	public void handleRegistrationSuccessful() {
		if (logger.isActivated()) {
			logger.debug("Handle event registration ok");
		}
		
        /**
         * M: Modify to meet with JRD hot fixes 7.4 @{
         */
        addRcseServiceNotification(getApplicationContext());
        /**
         * @}
         */
        
		// Send registration intent
		Intent intent = new Intent(ClientApiIntents.SERVICE_REGISTRATION);
		intent.putExtra("status", true);
        /**
         * M: Fix kill rcse progress will get wrong registration status @{
         */
        getApplicationContext().sendStickyBroadcast(intent);
        /**
         * @}
         */
		
	}

	/**
	 * Handle "registration failed" event
	 * 
     * @param error IMS error
   	 */
	public void handleRegistrationFailed(ImsError error) {
		if (logger.isActivated()) {
			logger.debug("Handle event registration failed");
		}

        /**
         * M: Modify to meet with JRD hot fixes 7.4 @{
         */
        removeRcseServiceNotification(getApplicationContext());
        /**
         * @}
         */

		// Send registration intent
		Intent intent = new Intent(ClientApiIntents.SERVICE_REGISTRATION);
		intent.putExtra("status", false);
        getApplicationContext().sendBroadcast(intent);
		
	}

	/**
	 * Handle "registration terminated" event
	 */
	public void handleRegistrationTerminated() {
		if (logger.isActivated()) {
			logger.debug("Handle event registration terminated");
		}

        /**
         * M: Modify to meet with JRD hot fixes 7.4 @{
         */
        removeRcseServiceNotification(getApplicationContext());
        /**
         * @}
         */
        
		// Send registration intent
		Intent intent = new Intent(ClientApiIntents.SERVICE_REGISTRATION);
		intent.putExtra("status", false);
        getApplicationContext().sendBroadcast(intent);
	}

    /**
     * A new presence sharing notification has been received
     * 
     * @param contact Contact
     * @param status Status
     * @param reason Reason
     */
    public void handlePresenceSharingNotification(String contact, String status, String reason) {
		if (logger.isActivated()) {
			logger.debug("Handle event presence sharing notification for " + contact + " (" + status + ":" + reason + ")");
		}

		try {
			// Check if its a notification for a contact or for the end user
			String me = ImsModule.IMS_USER_PROFILE.getPublicUri();
			if (PhoneUtils.compareNumbers(me, contact)) {
				// End user notification
				if (logger.isActivated()) {
					logger.debug("Presence sharing notification for me: by-pass it");
				}
	    	} else { 
		    	// Update contacts database
				ContactsManager.getInstance().setContactSharingStatus(contact, status, reason);
	
				// Broadcast intent
				Intent intent = new Intent(PresenceApiIntents.PRESENCE_SHARING_CHANGED);
		    	intent.putExtra("contact", contact);
		    	intent.putExtra("status", status);
		    	intent.putExtra("reason", reason);
				AndroidFactory.getApplicationContext().sendBroadcast(intent);
	    	}
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
    	}
    }

    /**
     * A new presence info notification has been received
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void handlePresenceInfoNotification(String contact, PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Handle event presence info notification for " + contact);
		}

		try {
			// Test if person item is not null
			Person person = presence.getPerson();
			if (person == null) {
				if (logger.isActivated()) {
					logger.debug("Presence info is empty (i.e. no item person found) for contact " + contact);
				}
				return;
			}

			// Check if its a notification for a contact or for me
			String me = ImsModule.IMS_USER_PROFILE.getPublicUri();
			if (PhoneUtils.compareNumbers(me, contact)) {
				// Notification for me
				presenceInfoNotificationForMe(presence);
			} else {
				// Check that the contact exist in database
				int rcsStatus = ContactsManager.getInstance().getContactSharingStatus(contact);
				if (rcsStatus == -1) {
					if (logger.isActivated()) {
						logger.debug("Contact " + contact + " is not a RCS contact, by-pass the notification");
					}
					return;
				}

				// Notification for a contact
				presenceInfoNotificationForContact(contact, presence);
			}
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
	}

    /**
     * A new presence info notification has been received for me
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void presenceInfoNotificationForMe(PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Presence info notification for me");
		}

    	try {
			// Get the current presence info for me
    		PresenceInfo currentPresenceInfo = ContactsManager.getInstance().getMyPresenceInfo();
    		if (currentPresenceInfo == null) {
    			currentPresenceInfo = new PresenceInfo();
    		}

			// Update presence status
			String presenceStatus = PresenceInfo.UNKNOWN;
			Person person = presence.getPerson();
			OverridingWillingness willingness = person.getOverridingWillingness();
			if (willingness != null) {
				if ((willingness.getBasic() != null) && (willingness.getBasic().getValue() != null)) {
					presenceStatus = willingness.getBasic().getValue();
				}
			}				
			currentPresenceInfo.setPresenceStatus(presenceStatus);
    		
    		// Update the presence info
			currentPresenceInfo.setTimestamp(person.getTimestamp());
			if (person.getNote() != null) {
				currentPresenceInfo.setFreetext(person.getNote().getValue());
			}
			if (person.getHomePage() != null) {
				currentPresenceInfo.setFavoriteLink(new FavoriteLink(person.getHomePage()));
			}
			
    		// Get photo Etag values
			String lastEtag = null;
			String newEtag = null; 
			if (person.getStatusIcon() != null) {
				newEtag = person.getStatusIcon().getEtag();
			}
			if (currentPresenceInfo.getPhotoIcon() != null) {
				lastEtag = currentPresenceInfo.getPhotoIcon().getEtag();
			}
    		
    		// Test if the photo has been removed
			if ((lastEtag != null) && (person.getStatusIcon() == null)) {
	    		if (logger.isActivated()) {
	    			logger.debug("Photo has been removed for me");
	    		}
	    		
    			// Update the presence info
				currentPresenceInfo.setPhotoIcon(null);

				// Update EAB provider
				ContactsManager.getInstance().removeMyPhotoIcon();
			} else		
	    	// Test if the photo has been changed
	    	if ((person.getStatusIcon() != null) &&	(newEtag != null)) {
	    		if ((lastEtag == null) || (!lastEtag.equals(newEtag))) {
		    		if (logger.isActivated()) {
		    			logger.debug("Photo has changed for me, download it in background");
		    		}
		
		    		// Download the photo in background
		    		downloadPhotoForMe(presence.getPerson().getStatusIcon().getUrl(), newEtag);
	    		}
	    	}
	    	   		    		
	    	// Update EAB provider
			ContactsManager.getInstance().setMyInfo(currentPresenceInfo);

    		// Broadcast intent
	    	Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
	    	getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }

    /**
     * A new presence info notification has been received for a given contact
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void presenceInfoNotificationForContact(String contact, PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Presence info notification for contact " + contact);
		}

    	try {
    		// Extract number from contact 
    		String number = PhoneUtils.extractNumberFromUri(contact);

    		// Get the current presence info
    		ContactInfo currentContactInfo = ContactsManager.getInstance().getContactInfo(contact);
    		ContactInfo newContactInfo = currentContactInfo;
    		if (currentContactInfo == null) {
    			if (logger.isActivated()) {
    				logger.warn("Contact " + contact + " not found in EAB: by-pass the notification");
    			}
    			return;
    		}
    		PresenceInfo newPresenceInfo = currentContactInfo.getPresenceInfo();
    		if (newPresenceInfo == null) {
    			newPresenceInfo = new PresenceInfo();
    			newContactInfo.setPresenceInfo(newPresenceInfo);
    		}

			// Update the current capabilities
			Capabilities capabilities =  new Capabilities(); 
			Vector<Tuple> tuples = presence.getTuplesList();
			for(int i=0; i < tuples.size(); i++) {
				Tuple tuple = (Tuple)tuples.elementAt(i);
				
				boolean state = false; 
				if (tuple.getStatus().getBasic().getValue().equals("open")) {
					state = true;
				}
					
				String id = tuple.getService().getId();
				if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_VIDEO_SHARE)) {
					capabilities.setVideoSharingSupport(state);
				} else
				if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_IMAGE_SHARE)) {
					capabilities.setImageSharingSupport(state);
				} else
				if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_FT)) {
					capabilities.setFileTransferSupport(state);
				} else
				if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_CS_VIDEO)) {
					capabilities.setCsVideoSupport(state);
				} else
				if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_CHAT)) {
					capabilities.setImSessionSupport(state);
				}
			}
			newContactInfo.setCapabilities(capabilities);

			// Update presence status
			String presenceStatus = PresenceInfo.UNKNOWN;
			Person person = presence.getPerson();
			OverridingWillingness willingness = person.getOverridingWillingness();
			if (willingness != null) {
				if ((willingness.getBasic() != null) && (willingness.getBasic().getValue() != null)) {
					presenceStatus = willingness.getBasic().getValue();
				}
			}				
			newPresenceInfo.setPresenceStatus(presenceStatus);

			// Update the presence info
			newPresenceInfo.setTimestamp(person.getTimestamp());
			if (person.getNote() != null) {
				newPresenceInfo.setFreetext(person.getNote().getValue());
			}
			if (person.getHomePage() != null) {
				newPresenceInfo.setFavoriteLink(new FavoriteLink(person.getHomePage()));
			}
			
			// Update geoloc info
			if (presence.getGeopriv() != null) {
				Geoloc geoloc = new Geoloc(presence.getGeopriv().getLatitude(),
						presence.getGeopriv().getLongitude(),
						presence.getGeopriv().getAltitude());
				newPresenceInfo.setGeoloc(geoloc);
			}
			
			newContactInfo.setPresenceInfo(newPresenceInfo);
	    	// Update contacts database
			ContactsManager.getInstance().setContactInfo(newContactInfo, currentContactInfo);

    		// Get photo Etag values
			String lastEtag = ContactsManager.getInstance().getContactPhotoEtag(contact);
			String newEtag = null; 
			if (person.getStatusIcon() != null) {
				newEtag = person.getStatusIcon().getEtag();
			}

    		// Test if the photo has been removed
			if ((lastEtag != null) && (person.getStatusIcon() == null)) {
	    		if (logger.isActivated()) {
	    			logger.debug("Photo has been removed for " + contact);
	    		}

	    		// Update contacts database
	    		ContactsManager.getInstance().setContactPhotoIcon(contact, null);
				
	    		// Broadcast intent
				Intent intent = new Intent(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
		    	intent.putExtra("contact", number);
				AndroidFactory.getApplicationContext().sendBroadcast(intent);
			} else		
	    	// Test if the photo has been changed
	    	if ((person.getStatusIcon() != null) &&	(newEtag != null)) {
	    		if ((lastEtag == null) || (!lastEtag.equals(newEtag))) {
		    		if (logger.isActivated()) {
		    			logger.debug("Photo has changed for " + contact + ", download it in background");
		    		}
		
		    		// Download the photo in background
		    		downloadPhotoForContact(contact, presence.getPerson().getStatusIcon().getUrl(), newEtag);
	    		}
	    	}    	
	    	   		    		
	    	// Broadcast intent
	    	Intent intent = new Intent(PresenceApiIntents.CONTACT_INFO_CHANGED);
	    	intent.putExtra("contact", number);
	    	getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }
    
    /**
     * Capabilities update notification has been received
     * 
     * @param contact Contact
     * @param capabilities Capabilities
     */
    public void handleCapabilitiesNotification(String contact, Capabilities capabilities) {
    	if (logger.isActivated()) {
			logger.debug("Handle capabilities update notification for " + contact + " (" + capabilities.toString() + ")");
		}

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(contact);

		// Broadcast intent containing the new capabilities
    	Intent intent = new Intent(CapabilityApiIntents.CONTACT_CAPABILITIES);
    	intent.putExtra("contact", number);
    	intent.putExtra("capabilities", capabilities);
    	getApplicationContext().sendBroadcast(intent);
    }
    
    /**
     * Download photo for me
     * 
     * @param url Photo URL
     * @param etag New Etag associated to the photo
     */
    private void downloadPhotoForMe(final String url, final String etag) {
		Thread t = new Thread() {
			public void run() {
		    	try {
		    		// Download from XDMS
		    		PhotoIcon icon = Core.getInstance().getPresenceService().getXdmManager().downloadContactPhoto(url, etag);    		
		    		if (icon != null) {
		    			// Update the presence info
		    			Core.getInstance().getPresenceService().getPresenceInfo().setPhotoIcon(icon);
		    			
						// Update contacts database
		    			ContactsManager.getInstance().setMyPhotoIcon(icon);
						
			    		// Broadcast intent
		    			// TODO : use a specific intent for the end user photo
				    	Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
				    	getApplicationContext().sendBroadcast(intent);
			    	}
		    	} catch(Exception e) {
		    		if (logger.isActivated()) {
		    			logger.error("Internal exception", e);
		    		}
	    		}
			}
		};
		t.start();
    }
    
    /**
     * Download photo for a given contact
     * 
     * @param contact Contact
     * @param url Photo URL 
     * @param etag New Etag associated to the photo
     */
    private void downloadPhotoForContact(final String contact, final String url, final String etag) {
		Thread t = new Thread() {
			public void run() {
		    	try {
		    		// Download from XDMS
		    		PhotoIcon icon = Core.getInstance().getPresenceService().getXdmManager().downloadContactPhoto(url, etag);    		
		    		if (icon != null) {
		    			// Update contacts database
		    			ContactsManager.getInstance().setContactPhotoIcon(contact, icon);

		    			// Extract number from contact 
		    			String number = PhoneUtils.extractNumberFromUri(contact);

		    			// Broadcast intent
		    			Intent intent = new Intent(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
		    			intent.putExtra("contact", number);
		    			getApplicationContext().sendBroadcast(intent);
			    	}
		    	} catch(Exception e) {
		    		if (logger.isActivated()) {
		    			logger.error("Internal exception", e);
		    		}
	    		}
			}
		};
		t.start();
    }
    
    /**
     * A new presence sharing invitation has been received
     * 
     * @param contact Contact
     */
    public void handlePresenceSharingInvitation(String contact) {
		if (logger.isActivated()) {
			logger.debug("Handle event presence sharing invitation");
		}
		
		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(contact);
		
    	// Broadcast intent related to the received invitation
    	Intent intent = new Intent(PresenceApiIntents.PRESENCE_INVITATION);
    	intent.putExtra("contact", number);
    	getApplicationContext().sendBroadcast(intent);
    }
    
    /**
     * New content sharing transfer invitation
     * 
     * @param session Content sharing transfer invitation
     */
    public void handleContentSharingTransferInvitation(ImageTransferSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event content sharing transfer invitation");
		}

		// Broadcast the invitation
		richcallApi.receiveImageSharingInvitation(session);
    }
    
    /**
     * New content sharing streaming invitation
     * 
     * @param session CSh session
     */
    public void handleContentSharingStreamingInvitation(VideoStreamingSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event content sharing streaming invitation");
		}

		// Broadcast the invitation
		richcallApi.receiveVideoSharingInvitation(session);
    }

	/**
	 * A new file transfer invitation has been received
	 * 
	 * @param session File transfer session
	 */
	public void handleFileTransferInvitation(FileSharingSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event file transfer invitation");
		}
		
    	// Broadcast the invitation
    	messagingApi.receiveFileTransferInvitation(session);
	}
    
	/**
     * New one-to-one chat session invitation
     * 
     * @param session Chat session
     */
	public void handleOneOneChatSessionInvitation(TerminatingOne2OneChatSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive 1-1 chat session invitation");
		}
		
    	// Broadcast the invitation
		messagingApi.receiveOneOneChatInvitation(session);
    }

    /**
     * New ad-hoc group chat session invitation
     * 
     * @param session Chat session
     */
	public void handleAdhocGroupChatSessionInvitation(TerminatingAdhocGroupChatSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive ad-hoc group chat session invitation");
		}

    	// Broadcast the invitation
		messagingApi.receiveGroupChatInvitation(session);
	}
    
    /**
     * One-to-one chat session extended to a group chat session
     * 
     * @param groupSession Group chat session
     * @param oneoneSession 1-1 chat session
     */
    public void handleOneOneChatSessionExtended(GroupChatSession groupSession, OneOneChatSession oneoneSession) {
		if (logger.isActivated()) {
			logger.debug("Handle event 1-1 chat session extended");
		}

    	// Broadcast the event
		messagingApi.extendOneOneChatSession(groupSession, oneoneSession);
    }
	
    /**
     * Store and Forward messages session invitation
     * 
     * @param session Chat session
     */
    public void handleStoreAndForwardMsgSessionInvitation(TerminatingStoreAndForwardMsgSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event S&F messages session invitation");
		}
		
    	// Broadcast the invitation
		messagingApi.receiveOneOneChatInvitation(session);
    }

    /** M: add server date for delivery status @{ */
    /**
     * New message delivery status
     * 
     * @param contact Contact
     * @param msgId Message ID
     * @param status Delivery status
     * @param date The server date for delivery status
     */
    public void handleMessageDeliveryStatus(String contact, String msgId, String status, long date) {
        if (logger.isActivated()) {
            logger.debug("Handle message delivery status");
        }

        // Notify listeners
        messagingApi.handleMessageDeliveryStatus(contact, msgId, status, date);
    }

    /** @} */

    /**
     * New SIP session invitation
     * 
     * @param session SIP session
     */
    public void handleSipSessionInvitation(TerminatingSipSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive SIP session invitation");
		}
		
		// Broadcast the invitation
		sipApi.receiveSipSessionInvitation(session);
    }    

    /**
     * User terms confirmation request
     * 
     * @param remote Remote server
     * @param id Request ID
     * @param type Type of request
     * @param pin PIN number requested
     * @param subject Subject
     * @param text Text
     */
    public void handleUserConfirmationRequest(String remote, String id, String type, boolean pin, String subject, String text) {
		if (logger.isActivated()) {
			logger.debug("Handle event user confirmation request");
		}

		// Notify listeners
		termsApi.receiveTermsRequest(remote, id, type, pin, subject, text);
    }

    /**
     * User terms confirmation acknowledge
     * 
     * @param remote Remote server
     * @param id Request ID
     * @param status Status
     * @param subject Subject
     * @param text Text
     */
    public void handleUserConfirmationAck(String remote, String id, String status, String subject, String text) {
		if (logger.isActivated()) {
			logger.debug("Handle event user confirmation ack");
		}

		// Notify listeners
		termsApi.receiveTermsAck(remote, id, status, subject, text);
    }

  /**
	 * SIM has changed
	 */
    public void handleSimHasChanged() {
        if (logger.isActivated()) {
            logger.debug("Handle SIM has changed");
        }

		// Restart the RCS service
        LauncherUtils.stopRcsService(getApplicationContext());
        LauncherUtils.launchRcsService(getApplicationContext(), true);
    }

    /**
     * M: Added to resolve the issue that can't send message successfully
     * sometimes.@{
     */
    /**
     * This class will help to manage the core service status
     */
    private final static CoreServiceManager CORE_SERVICE_MANAGER = new CoreServiceManager();

    private static class CoreServiceManager {

        private Logger logger = Logger.getLogger(CoreServiceManager.class.getName());

        private static enum CORE_STATUS {
            STARTING,STARTED,STOPPING,STOPPED
        }

        private final AtomicReference<CORE_STATUS> mCoreStatus = new AtomicReference<CORE_STATUS>();
        private final AtomicReference<Boolean> mPendingAction = new AtomicReference<Boolean>();

        /**
         * This method will judge whether need to perform the new action
         * @param isLaunch The new action 
         * @return false indicates to perform the new action, otherwise true.
         */
        public synchronized boolean handleNewAction(boolean isLaunch) {
            Boolean currentPendingAction = mPendingAction.get();
            logger.debug("handleNewAction() isLaunch is " + isLaunch
                    + ", current pending action is " + currentPendingAction);
            if (null == currentPendingAction || currentPendingAction != isLaunch) {
                if (isLaunch) {
                    return handleLaunchAction();
                } else {
                    return handleStopAction();
                }
            } else {
                logger.debug("handleNewAction() currentPendingAction equals the new action "
                        + isLaunch + ", then do nothing");
                return true;
            }
        }

        private boolean handleLaunchAction() {
            logger.debug("handleLaunchAction() entry");
            CORE_STATUS status = mCoreStatus.get();
            if (null != status) {
                switch (status) {
                    case STARTED:
                    case STARTING:
                        logger.error("handleLaunchAction() current core status is "
                                        + mCoreStatus.get());
                        return true;
                    case STOPPED:
                        mPendingAction.set(null);
                        logger.error("handleLaunchAction() current core status is stopped, launch it immediately");
                        return false;
                    case STOPPING:
                        logger.error("handleLaunchAction() core is stopping, " +
                        		"so make this action pending");
                        mPendingAction.set(true);
                        return true;
                    default:
                        logger.error("handleLaunchAction() mCoreStatus has invalid value "
                                + mCoreStatus.get());
                        return false;
                }
            } else {
                logger.error("handleLaunchAction() status is null");
                return false;
            }
        }

        private boolean handleStopAction() {
            logger.debug("handleStopAction() entry");
            switch (mCoreStatus.get()) {
                case STARTED:
                case STARTING:
                    logger.debug("handleStopAction() current core status is "
                                    + mCoreStatus.get());
                    return false;
                case STOPPED:
                    mPendingAction.set(null);
                    logger.error("handleStopAction() current core status is stopped," +
                    " this blocked should never be entered!!");
                    return false;
                case STOPPING:
                    logger.error("handleStopAction() current core status is stopped");
                    return true;
                default:
                    logger.error("handleStopAction() mCoreStatus has invalid value "
                            + mCoreStatus.get());
                    return false;
            }
        }

        public synchronized void onCoreStarting() {
            logger.debug("onCoreStarting() entry");
            mCoreStatus.set(CORE_STATUS.STARTING);
        }

        public synchronized void onCoreStarted() {
            logger.debug("onCoreStarted() entry");
            mCoreStatus.set(CORE_STATUS.STARTED);
        }

        public synchronized void onCoreStopping() {
            logger.debug("onCoreStopping() entry");
            /**
             * M: Modify to meet with JRD hot fixes 7.4 @{
             */
            removeRcseServiceNotification(AndroidFactory.getApplicationContext());
            /**
             * @}
             */
            mCoreStatus.set(CORE_STATUS.STOPPING);
        }

        public synchronized void onCoreStopped() {
            logger.debug("onCoreStopped() entry");
            mCoreStatus.set(CORE_STATUS.STOPPED);
            if (mPendingAction.compareAndSet(true, null)) {
                logger.debug("onCoreStopped() pending action is true");
                performLaunch();
            } else {
                logger.debug("onCoreStopped() no pending action");
            }
        }
        
        /**
         * M: Added to resolve the issue that received messages while RCS is
         * disconnected automatically. @{
         */
        protected void performLaunch() {
            logger.debug("performLaunch() entry");
            Context context = AndroidFactory.getApplicationContext();
            if (null != context) {
                LauncherUtils.launchRcsCoreService(context);
            } else {
                logger.error("onCoreStopping(), mContext is null!");
            }
        }
        /**
         * @}
         */
    }
    /**
     * @}
     */

    /**
     * M: Added for internal using to check whether login successfully. The
     * following two method only work in debug mode @{
     */
    private static final String NOTIFICATION_TITLE = "joyn";
    private static final String NOTIFICATION_CONTENT = "Connected to joyn platform";
    private static final String NOTIFICATION_TICKER = "";
    private static final String NOTIFICATION_TAG = "RCSe";

    /**
     * Add RCS-e service notification
     * 
     * @param context The application context.
     */
    public static void addRcseServiceNotification(Context context) {
        if (!LauncherUtils.sIsDebug) {
            logger.debug("addRcseServiceNotification but now is not in debug mode, so do nothing.");
            return;
        }
        // Create notification
        if (context != null) {
            Intent intent = new Intent(ClientApiIntents.RCS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
            Notification.Builder builder = new Notification.Builder(context);
            builder.setContentTitle(NOTIFICATION_TITLE);
            builder.setContentText(NOTIFICATION_CONTENT);
            builder.setContentIntent(contentIntent);
            builder.setTicker(NOTIFICATION_TICKER);
            builder.setSmallIcon(R.drawable.ic_rcse_indicaton);
            builder.setWhen(System.currentTimeMillis());
            builder.setAutoCancel(false);
            Notification notif = builder.getNotification();
            notif.flags |= Notification.FLAG_NO_CLEAR;
            // Send notification
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_TAG, SERVICE_NOTIFICATION, notif);
        }
    }

    /**
     * Add RCS-e service notification
     * 
     * @param context The application context.
     */
    public static void removeRcseServiceNotification(Context context) {
        if (!LauncherUtils.sIsDebug) {
            logger.debug("removeRcseServiceNotification but now is not in debug mode, so do nothing.");
            return;
        }
        // Remove notification
        if (context != null) {
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_TAG, SERVICE_NOTIFICATION);
        } 
    }
    /**
     * @}
     */

}
