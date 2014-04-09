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

import javax.sip.header.AcceptHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.SubscriptionStateHeader;

import org.xml.sax.InputSource;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.presence.watcherinfo.Watcher;
import com.orangelabs.rcs.core.ims.service.presence.watcherinfo.WatcherInfoDocument;
import com.orangelabs.rcs.core.ims.service.presence.watcherinfo.WatcherInfoParser;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Subscribe manager for presence watcher info event
 * 
 * @author jexa7410
 */
public class WatcherInfoSubscribeManager extends SubscribeManager {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * M: Event value of subscribe request @{T-Mobile
     */
    private static final String EVENT_VALUE = "presence.winfo";
    /** T-Mobile@} */

    /**
     * M: subscribe accepted header values @{T-Mobile
     */
    private static final String WATCHERINFO_VALUE = "application/watcherinfo+xml";

    /** T-Mobile@} */

    /**
     * Constructor
     * 
     * @param parent IMS module
     */
    public WatcherInfoSubscribeManager(ImsModule parent) {
    	super(parent);
    }
    	
    /**
     * Returns the presentity
     * 
     * @return Presentity
     */
    public String getPresentity() {
    	return ImsModule.IMS_USER_PROFILE.getPublicUri();
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
            subscribe.addHeader(AcceptHeader.NAME, WATCHERINFO_VALUE);
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
        subscribe.addHeader(AcceptHeader.NAME, WATCHERINFO_VALUE);

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
            logger.debug("New watcher-info notification received");
        }

        // Parse XML part
        byte[] content = notify.getContentBytes();
        if (content != null) {
            try {
                InputSource input = new InputSource(new ByteArrayInputStream(content));
                WatcherInfoParser parser = new WatcherInfoParser(input);
                WatcherInfoDocument watcherinfo = parser.getWatcherInfo();
                if (watcherinfo != null) {
                    for (int i = 0; i < watcherinfo.getWatcherList().size(); i++) {
                        Watcher w = (Watcher) watcherinfo.getWatcherList().elementAt(i);
                        String contact = w.getUri();
                        String status = w.getStatus();
                        String event = w.getEvent();

                        if ((contact != null) && (status != null) && (event != null)) {
                            if (status.equalsIgnoreCase(SubscriptionStateHeader.PENDING)) {
                                // It's an invitation or a new status
                                getImsModule().getCore().getListener()
                                        .handlePresenceSharingInvitation(contact);
                            }

                            // Notify listener
                            getImsModule().getCore().getListener()
                                    .handlePresenceSharingNotification(contact, status, event);
                        }
                    }
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't parse watcher-info notification", e);
                }
            }
        }

        // Check subscription state
        SubscriptionStateHeader stateHeader = (SubscriptionStateHeader) notify
                .getHeader(SubscriptionStateHeader.NAME);
        if ((stateHeader != null)
                && stateHeader.getState().equalsIgnoreCase(SubscriptionStateHeader.TERMINATED)) {
            if (logger.isActivated()) {
                logger.info("Watcher-info subscription has been terminated by server");
            }
            terminatedByServer();
        }
    }
    /** T-Mobile@} */
}
