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

package com.orangelabs.rcs.core.ims.service.im.chat;

import java.util.List;
import java.util.Vector;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.KeyStoreManager;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rejoin a group chat session
 * 
 * @author jexa7410
 */
public class RejoinGroupChatSession extends GroupChatSession {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    /**
     * M: add for auto-rejoin group chat @{
     */
    /**
     * Max count to retry.
     */
    private static final int MAX_RETRY_COUNT = 10;
    /**
     * Current retry count.
     */
    private int mCurrentRetryCount = 0;

    /** @} */
    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param rejoinId Rejoin ID
	 * @param chatId Chat ID or contribution ID
	 * @param subject Subject
	 * @param participants List of participants 
	 */
	public RejoinGroupChatSession(ImsService parent, String rejoinId, String chatId, String subject, List<String> participants) {
		super(parent, rejoinId, new ListOfParticipant(participants));

		// Set subject
		if ((subject != null) && (subject.length() > 0)) {
			setSubject(subject);		
		}

		// Create dialog path
		createOriginatingDialogPath();
		
		// Set contribution ID
		setContributionID(chatId);
	}
	
	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Rejoin an existing group chat session");
	    	}
	    	
            /**
             * Modified to resolve the 403 error issue.@{
             */
	    	SipRequest invite = createSipInvite();
	    	/**
	    	 * @}
	    	 */
    		
	        // Send INVITE request
	        sendInvite(invite);	        
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}		
	}
	
    /**
     * Modified to resolve the 403 error issue.@{
     */
    /**
     * @return A sip invite request
     */
    @Override
    protected SipRequest createSipInvite(String callId) {
        SipRequest invite = null;
        mCurrentRetryCount++;
        if (logger.isActivated()) {
            logger.debug("createSipInvite(), callId: " + callId + " mCurrentRetryCount: "
                    + mCurrentRetryCount);
        }
        if (mCurrentRetryCount <= MAX_RETRY_COUNT) {
            try {
                Thread.sleep(SipManager.TIMEOUT * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (logger.isActivated()) {
                    logger.debug("createSipInvite() InterruptedException");
                }
            }
            createOriginatingDialogPath(callId);
            invite = createSipInvite();
        }
        return invite;
    }

    private SipRequest createSipInvite() {
        logger.debug("createSipInvite()");
        // Set setup mode
        String localSetup = createSetupOffer();
        if (logger.isActivated()) {
            logger.debug("Local setup attribute is " + localSetup);
        }

        // Set local port
        int localMsrpPort = 9; // See RFC4145, Page 4

        // Build SDP part
        String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
        String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
        /** M: add for MSRPoTLS @{ */
        String protocol = getCurrentProtocol();
        String sdp = null;
        if (PROTOCOL_TLS.equals(protocol)) {
            sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + "m=message " + localMsrpPort + " TCP/TLS/MSRP *"
                    + SipUtils.CRLF + "a=path:" + getMsrpMgr().getLocalMsrpsPath() + SipUtils.CRLF
                    + "a=fingerprint:" + KeyStoreManager.getFingerPrint() + SipUtils.CRLF
                    + "a=setup:" + localSetup + SipUtils.CRLF + "a=accept-types:"
                    + CpimMessage.MIME_TYPE + SipUtils.CRLF + "a=accept-wrapped-types:"
                    + InstantMessage.MIME_TYPE + " " + IsComposingInfo.MIME_TYPE + SipUtils.CRLF
                    + "a=sendrecv" + SipUtils.CRLF;
        } else {
            sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + "m=message " + localMsrpPort + " TCP/MSRP *" + SipUtils.CRLF
                    + "a=path:" + getMsrpMgr().getLocalMsrpPath() + SipUtils.CRLF + "a=setup:"
                    + localSetup + SipUtils.CRLF + "a=accept-types:" + CpimMessage.MIME_TYPE
                    + SipUtils.CRLF + "a=accept-wrapped-types:" + InstantMessage.MIME_TYPE + " "
                    + IsComposingInfo.MIME_TYPE + SipUtils.CRLF + "a=sendrecv" + SipUtils.CRLF;
        }
        /** @} */
        // Set the local SDP part in the dialog path
        getDialogPath().setLocalContent(sdp);

        // Create an INVITE request
        if (logger.isActivated()) {
            logger.info("Send INVITE");
        }
        SipRequest invite = null;
        try {
            invite = createInviteRequest(sdp);
            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);
            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);
        } catch (SipException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return invite;
    }
    /**
     * @}
     */
	
	/**
	 * Create INVITE request
	 * 
	 * @param content Content part
	 * @return Request
	 * @throws SipException
	 */
	private SipRequest createInviteRequest(String content) throws SipException {
        SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
        		InstantMessagingService.CHAT_FEATURE_TAGS,
        		content);

        // Add a contribution ID header
        invite.addHeader(ChatUtils.HEADER_CONTRIBUTION_ID, getContributionID());
	
	    return invite;
	}	
	
	/**
	 * Send INVITE message
	 * 
	 * @param invite SIP INVITE
	 * @throws SipException
	 */
	@Override
	protected void sendInvite(SipRequest invite) throws SipException {
		// Send INVITE request
		SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSipMessageAndWait(invite);
		
        // Wait response
        ctx.waitResponse(getResponseTimeout());

		// Analyze the received response 
        if (ctx.isSipResponse()) {
        	// A response has been received
            if (ctx.getStatusCode() == 200) {
            	// 200 OK
            	handle200OK(ctx.getSipResponse());
            } else
            if (ctx.getStatusCode() == 407) {
            	// 407 Proxy Authentication Required
            	handle407Authentication(ctx.getSipResponse());
            } else
            if (ctx.getStatusCode() == 422) {
            	// 422 Session Interval Too Small
            	handle422SessionTooSmall(ctx.getSipResponse());
            } else
            if ((ctx.getStatusCode() == 486) || (ctx.getStatusCode() == 480)) {
            	// 486 busy or 480 Temporarily Unavailable 
            	handleError(new ChatError(ChatError.SESSION_INITIATION_DECLINED,
    					ctx.getReasonPhrase()));
            } else
            if (ctx.getStatusCode() == 487) {
            	// 487 Invitation cancelled
            	handleError(new ChatError(ChatError.SESSION_INITIATION_CANCELLED,
    					ctx.getReasonPhrase()));
            } else
            if (ctx.getStatusCode() == 404) {
            	// 404 session not found
            	handleError(new ChatError(ChatError.SESSION_NOT_FOUND,
    					ctx.getReasonPhrase()));
            }
            /**
             * Modified to resolve the 403 error issue.@{
             */
            else if(ctx.getStatusCode() == 403){
                handle403Forbidden(invite);
            }
            /**
             * @}
             */
            else {
            	// Other error response
    			handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED,
    					ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
            }
        } else {
    		if (logger.isActivated()) {
        		logger.debug("No response received for INVITE");
        	}

    		// No response received: timeout
        	handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED));
        }
	}	

	/**
	 * Handle 200 0K response 
	 * 
	 * @param resp 200 OK response
	 */
	public void handle200OK(SipResponse resp) {
		try {
	        // 200 OK received
			if (logger.isActivated()) {
				logger.info("200 OK response received");
			}

	        // The signalisation is established
	        getDialogPath().sigEstablished();

	        // Set the remote tag
	        getDialogPath().setRemoteTag(resp.getToTag());
        
	        // Set the target
	        getDialogPath().setTarget(resp.getContactURI());
	
	        // Set the route path with the Record-Route header
	        Vector<String> newRoute = SipUtils.routeProcessing(resp, true);
			getDialogPath().setRoute(newRoute);
			
	        // Set the remote SDP part
	        getDialogPath().setRemoteContent(resp.getContent());
	        
	        // Parse the remote SDP part
        	SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
        	Vector<MediaDescription> media = parser.getMediaDescriptions();
    		MediaDescription mediaDesc = media.elementAt(0);
    		MediaAttribute attr = mediaDesc.getMediaAttribute("path");
    		String remoteMsrpPath = attr.getValue();
    		String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription.connectionInfo);
    		int remotePort = mediaDesc.port;

	        // Send ACK request
	        if (logger.isActivated()) {
	        	logger.info("Send ACK");
	        }
	        getImsService().getImsModule().getSipManager().sendSipAck(getDialogPath());
	        
        	// The session is established
	        getDialogPath().sessionEstablished();

        	// Create the MSRP session
			MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost, remotePort, remoteMsrpPath, this);
			session.setFailureReportOption(false);
			session.setSuccessReportOption(false);
			
			// Open the MSRP session
			getMsrpMgr().openMsrpSession();
			
	        // Send an empty packet
        	sendEmptyDataChunk();
        	
			// Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		getListeners().get(i).handleSessionStarted();
	        }

	    	// Subscribe to event package
        	getConferenceEventSubscriber().subscribe();

        	// Start session timer
        	if (getSessionTimerManager().isSessionTimerActivated(resp)) {
        		getSessionTimerManager().start(resp.getSessionTimerRefresher(), resp.getSessionTimerExpire());
        	}
			
			// Start the activity manager
			getActivityManager().start();
	    	
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
        }
	}
	
	/**
	 * Handle 407 Proxy Authentication Required 
	 * 
	 * @param resp 407 response
	 */
	public void handle407Authentication(SipResponse resp) {
		try {
	        if (logger.isActivated()) {
	        	logger.info("407 response received");
	        }
	
	        // Set the remote tag
	        getDialogPath().setRemoteTag(resp.getToTag());
	
	        // Update the authentication agent
	    	getAuthenticationAgent().readProxyAuthenticateHeader(resp);            
	
	        // Increment the Cseq number of the dialog path
	        getDialogPath().incrementCseq();
	
	        // Create a second INVITE request with the right token
	        if (logger.isActivated()) {
	        	logger.info("Send second INVITE");
	        }
	        SipRequest invite = createInviteRequest(getDialogPath().getLocalContent());
	               
	        // Reset initial request in the dialog path
	        getDialogPath().setInvite(invite);
	        
	        // Set the Proxy-Authorization header
	        getAuthenticationAgent().setProxyAuthorizationHeader(invite);
	
	        // Send INVITE request
	        sendInvite(invite);
	        
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
        }
	}

	/**
	 * Handle 422 response 
	 * 
	 * @param resp 422 response
	 */
	private void handle422SessionTooSmall(SipResponse resp) {
		try {
			// 422 response received
	    	if (logger.isActivated()) {
	    		logger.info("422 response received");
	    	}
	
	        // Extract the Min-SE value
	        int minExpire = SipUtils.getMinSessionExpirePeriod(resp);
	        if (minExpire == -1) {
	            if (logger.isActivated()) {
	            	logger.error("Can't read the Min-SE value");
	            }
	        	handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION, "No Min-SE value found"));
	        	return;
	        }
	        
	        // Set the min expire value
	        getDialogPath().setMinSessionExpireTime(minExpire);

	        // Set the expire value
	        getDialogPath().setSessionExpireTime(minExpire);
	
	        // Increment the Cseq number of the dialog path
	        getDialogPath().incrementCseq();

	        // Create a new INVITE with the right expire period
	        if (logger.isActivated()) {
	        	logger.info("Send new INVITE");
	        }
	        SipRequest invite = createInviteRequest(getDialogPath().getLocalContent());
	        
	        // Set the Authorization header
	        getAuthenticationAgent().setAuthorizationHeader(invite);

	        // Reset initial request in the dialog path
	        getDialogPath().setInvite(invite);
	        
	        // Send INVITE request
	        sendInvite(invite);
	        
	    } catch(Exception e) {
	    	if (logger.isActivated()) {
	    		logger.error("Session initiation has failed", e);
	    	}
	
	    	// Unexpected error
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
	    }
	}		
}
