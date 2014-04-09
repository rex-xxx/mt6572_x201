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

package com.orangelabs.rcs.core.ims.service.presence;

import java.io.ByteArrayInputStream;
import java.util.Vector;

import javax.sip.header.AcceptHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.SupportedHeader;

import org.xml.sax.InputSource;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfParser;
import com.orangelabs.rcs.core.ims.service.presence.rlmi.ResourceInstance;
import com.orangelabs.rcs.core.ims.service.presence.rlmi.RlmiDocument;
import com.orangelabs.rcs.core.ims.service.presence.rlmi.RlmiParser;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Subscribe manager for presence event
 * 
 * @author jexa7410
 */
public class PresenceSubscribeManager extends SubscribeManager {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * M: Event value of subscribe request @{T-Mobile
     */
    private static final String EVENT_VALUE = "presence";
    private static final String EVENTLIST_VALUE = "eventlist";
    /** T-Mobile@} */

    /**
     * M: subscribe accepted header values @{T-Mobile
     */
    private static final String PIDF_VALUE = "application/pidf+xml";
    private static final String RLMI_VALUE = "application/rlmi+xml";
    private static final String MULIPART_VALUE = "multipart/related";

    /** T-Mobile@} */

    /**
     * Constructor
     * 
     * @param parent IMS module
     */
    public PresenceSubscribeManager(ImsModule parent) {
    	super(parent);
    }

    /**
     * Returns the presentity
     * 
     * @return Presentity
     */
    public String getPresentity() {
    	return ImsModule.IMS_USER_PROFILE.getPublicUri()+";pres-list=rcs";
    }    
    /**
     * M: Make subscribe request can contain content. For keeping origin
     * interface unchanged,so you will see some redundancy code @{T-Mobile
     */
    /**
     * Create a SUBSCRIBE request
     * 
     * @param dialog SIP dialog path
     * @param expirePeriod Expiration period
     * @param content Content of subscribe request,such as "simple-filter.xml"
     * @param contentType ContentType of subscribe request,such as "application"
     * @param contentSubType ContentSubType of subscribe request,
     *        such as "simple-filter+xml"
     * @return SIP request
     * @throws SipException
     */
    public SipRequest createSubscribe(SipDialogPath dialog, int expirePeriod, String content,
            String contentType, String contentSubType) throws SipException {
        if (logger.isActivated()) {
            logger.info("createSubscribe(" + dialog.toString() + expirePeriod + content
                    + contentType + contentSubType + ")");
        }
        SipRequest subscribe = null;
        if (null == content) {
            subscribe = createSubscribe(dialog, expirePeriod);
        } else {
            // Create SUBSCRIBE message
            subscribe = SipMessageFactory.createSubscribe(dialog, expirePeriod);
            // Set the Event header
            subscribe.addHeader(EventHeader.NAME, EVENT_VALUE);
            // Set the Accept header
            subscribe.addHeader(AcceptHeader.NAME, PIDF_VALUE);
            subscribe.addHeader(AcceptHeader.NAME, RLMI_VALUE);
            subscribe.addHeader(AcceptHeader.NAME, MULIPART_VALUE);
            // set content into subscribe request
            subscribe.setContent(content, contentType, contentSubType);
            // this line may will be deleted,wait for test for xcap server
            subscribe.setContentLength(content);
        }
        return subscribe;
    }
    /** T-Mobile@} */

    /**
     * M: Add log and change some strings to static final. @{T-Mobile
     */
    /**
     * Create a SUBSCRIBE request
     * 
     * @param dialog SIP dialog path
     * @param expirePeriod Expiration period
     * @return SIP request
     * @throws SipException
     */
    public SipRequest createSubscribe(SipDialogPath dialog, int expirePeriod) throws SipException {
        if (logger.isActivated()) {
            logger.info("createSubscribe(" + dialog.toString() + expirePeriod + ")");
        }
        // Create SUBSCRIBE message
        SipRequest subscribe = SipMessageFactory.createSubscribe(dialog, expirePeriod);

        // Set the Event header
        subscribe.addHeader(EventHeader.NAME, EVENT_VALUE);
        // Set the Accept header
        subscribe.addHeader(AcceptHeader.NAME, PIDF_VALUE);
        subscribe.addHeader(AcceptHeader.NAME, RLMI_VALUE);
        subscribe.addHeader(AcceptHeader.NAME, MULIPART_VALUE);
        // Set the Supported header
        subscribe.addHeader(SupportedHeader.NAME, EVENTLIST_VALUE);
        return subscribe;
    }
    /** T-Mobile@} */

    /**
     * M: change some strings to static final. @{T-Mobile
     */
    /**
     * Receive a notification
     * 
     * @param notify Received notify
     */
    public void receiveNotification(SipRequest notify) {
    	// Check notification
    	if (!isNotifyForThisSubscriber(notify)) {
    		return;
    	}    	
    	
		if (logger.isActivated()) {
			logger.debug("New presence notification received");
		}    	

		// Parse XML part
	    String content = notify.getContent();
		if (content != null) {
	    	try {
	    		String boundary = notify.getBoundaryContentType();
				Multipart multi = new Multipart(content, boundary);
			    if (multi.isMultipart()) {
			    	// RLMI
			    	String rlmiPart = multi.getPart(RLMI_VALUE);
			    	if (rlmiPart != null) {
    					try {
	    	    			// Parse RLMI part
	    					InputSource rlmiInput = new InputSource(new ByteArrayInputStream(rlmiPart.getBytes()));
	    					RlmiParser rlmiParser = new RlmiParser(rlmiInput);
	    					RlmiDocument rlmiInfo = rlmiParser.getResourceInfo();
	    					Vector<ResourceInstance> list = rlmiInfo.getResourceList();
	    					for(int i=0; i < list.size(); i++) {
	    						ResourceInstance res = (ResourceInstance)list.elementAt(i);
	    						String contact = res.getUri();
	    						String state = res.getState();
	    						String reason = res.getReason();
	    						
                                if ((contact != null) && (state != null) && (reason != null)) {
                                    if (state.equalsIgnoreCase(SubscriptionStateHeader.TERMINATED)
                                            && reason
                                                    .equalsIgnoreCase(SubscriptionStateHeader.REJECTED)) {
                                        // It's a "terminated" event with status
                                        // "rejected" the contact
                                        // should be removed from the "rcs" list
                                        getImsModule().getPresenceService().getXdmManager()
                                                .removeContactFromGrantedList(contact);
                                    }

                                    // Notify listener
                                    getImsModule()
                                            .getCore()
                                            .getListener()
                                            .handlePresenceSharingNotification(contact, state,
                                                    reason);
	    						}
	    					}
    			    	} catch(Exception e) {
    			    		if (logger.isActivated()) {
    			    			logger.error("Can't parse RLMI notification", e);
    			    		}
    			    	}
			    	}

			    	// PIDF 
                    String pidfPart = multi.getPart(PIDF_VALUE);
					try {
    	    			// Parse PIDF part
						InputSource pidfInput = new InputSource(new ByteArrayInputStream(pidfPart.getBytes()));
    					PidfParser pidfParser = new PidfParser(pidfInput);
    					PidfDocument presenceInfo = pidfParser.getPresence();
    					
    					// Notify listener
    			    	getImsModule().getCore().getListener().handlePresenceInfoNotification(
    			    			presenceInfo.getEntity(), presenceInfo);
			    	} catch(Exception e) {
			    		if (logger.isActivated()) {
			    			logger.error("Can't parse PIDF notification", e);
			    		}
			    	}
			    }
	    	} catch(Exception e) {
	    		if (logger.isActivated()) {
	    			logger.error("Can't parse presence notification", e);
	    		}
	    	}
	    	
			// Check subscription state
            SubscriptionStateHeader stateHeader = (SubscriptionStateHeader) notify
                    .getHeader(SubscriptionStateHeader.NAME);
            if ((stateHeader != null)
                    && stateHeader.getState().equalsIgnoreCase(SubscriptionStateHeader.TERMINATED)) {
				if (logger.isActivated()) {
					logger.info("Presence subscription has been terminated by server");
				}
				terminatedByServer();
			}
            /**
             * M: Handle the notification whose state parameter is "terminated"
             * and reason parameter is "deactivate". @{T-Mobile
             */
            if ((stateHeader != null)
                    && stateHeader.getState().equalsIgnoreCase(SubscriptionStateHeader.TERMINATED)
                    && stateHeader.getReasonCode().equalsIgnoreCase(
                            SubscriptionStateHeader.DEACTIVATED)) {
                // the method to handle this condition will be added latter

            }
            /**
             * T-Mobile@}
             */
        }
    }
    /** T-Mobile@} */
}
