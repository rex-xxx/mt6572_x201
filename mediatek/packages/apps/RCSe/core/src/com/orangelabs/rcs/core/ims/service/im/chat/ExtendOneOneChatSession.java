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

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
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
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.Vector;

/**
 * Extends a one-to-one chat session to an ad-hoc session
 * 
 * @author jexa7410
 */
public class ExtendOneOneChatSession extends GroupChatSession {
	/**
	 * One-to-one session
	 */
	private OneOneChatSession oneoneSession;
	
	/**
	 * Boundary delimiter
	 */
	private final static String boundary = "boundary1";
	
    /**
     * M: Added to make group chat work well, to successfully send and receive
     * invitation. @{
     */
    private static final String RECIPIENT_LIST_INVITE = "recipient-list-invite";
    /**
     * @}
     */
    
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param conferenceId Conference id
	 * @param oneoneSession One-to-one session
	 * @param participants List of invited participants
	 */
	public ExtendOneOneChatSession(ImsService parent, String conferenceId, OneOneChatSession oneoneSession, ListOfParticipant participants) {
		super(parent, conferenceId, participants);
	
		// Create dialog path
		createOriginatingDialogPath();
		
		// Save one-to-one session
		this.oneoneSession = oneoneSession;
	}
	
	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Extends a 1-1 session");
	    	}
	    	
	    	/**
	         * M: Modified to resolve 403 error issue. @{
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
     * M: Modified to resolve 403 error issue. @{
     */
    /**
     * @return A sip invite request
     */
    @Override
    protected SipRequest createSipInvite(String callId) {
        logger.debug("createSipInvite(), callId = " + callId);
        createOriginatingDialogPath(callId);
        return createSipInvite();
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
                    + CpimMessage.MIME_TYPE + " " + IsComposingInfo.MIME_TYPE + SipUtils.CRLF
                    + "a=accept-wrapped-types:" + InstantMessage.MIME_TYPE + " "
                    + ImdnDocument.MIME_TYPE + SipUtils.CRLF + "a=sendrecv" + SipUtils.CRLF;
        } else {
            sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                    + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-" + SipUtils.CRLF
                    + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
                    + SipUtils.CRLF + "m=message " + localMsrpPort + " TCP/MSRP *" + SipUtils.CRLF
                    + "a=path:" + getMsrpMgr().getLocalMsrpPath() + SipUtils.CRLF + "a=setup:"
                    + localSetup + SipUtils.CRLF + "a=accept-types:" + CpimMessage.MIME_TYPE + " "
                    + IsComposingInfo.MIME_TYPE + SipUtils.CRLF + "a=accept-wrapped-types:"
                    + InstantMessage.MIME_TYPE + " " + ImdnDocument.MIME_TYPE + SipUtils.CRLF
                    + "a=sendrecv" + SipUtils.CRLF;
        }
        /** @} */
        // Generate the resource list for given participants
        String existingParticipant = oneoneSession.getParticipants().getList().get(0);
        /**
         * M: Modified to make group chat work well, to successfully send and
         * receive invitation. @{
         */
        String replaceHeader = ";method=INVITE?Session-Replaces="
                + oneoneSession.getImSessionIdentity();
        /**
         * @}
         */
        String resourceList = ChatUtils.generateExtendedChatResourceList(existingParticipant,
                replaceHeader, getParticipants().getList());

        // Build multipart
        String multipart = Multipart.BOUNDARY_DELIMITER + boundary + SipUtils.CRLF
                + "Content-Type: application/sdp" + SipUtils.CRLF + "Content-Length: "
                + sdp.getBytes().length + SipUtils.CRLF + SipUtils.CRLF + sdp + SipUtils.CRLF
                + Multipart.BOUNDARY_DELIMITER + boundary + SipUtils.CRLF
                + "Content-Type: application/resource-lists+xml" + SipUtils.CRLF
                + "Content-Length: " + resourceList.getBytes().length + SipUtils.CRLF
                + "Content-Disposition: recipient-list" + SipUtils.CRLF + SipUtils.CRLF
                + resourceList + SipUtils.CRLF + Multipart.BOUNDARY_DELIMITER + boundary
                + Multipart.BOUNDARY_DELIMITER;

        // Set the local SDP part in the dialog path
        getDialogPath().setLocalContent(multipart);

        // Create an INVITE request
        if (logger.isActivated()) {
            logger.info("Create INVITE");
        }
        try {
            SipRequest invite;
            invite = createInviteRequest(multipart);
            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);
            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);
            return invite;
        } catch (SipException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        logger.error("Create sip invite failed, return null.");
        return null;
    }
    /**
     * @}
     */
	
	/**
	 * Create INVITE request
	 * 
	 * @param content Multipart content
	 * @return Request
	 * @throws SipException
	 */
	private SipRequest createInviteRequest(String content) throws SipException {
		// Create multipart INVITE
        SipRequest invite = SipMessageFactory.createMultipartInvite(getDialogPath(),
        		InstantMessagingService.CHAT_FEATURE_TAGS,
        		content, boundary);
        /**
         * M: Modified to make group chat work well, to successfully send and
         * receive invitation. @{
         */
        invite.addHeader(SipUtils.HEADER_REQUIRE, RECIPIENT_LIST_INVITE);
        /**
         * @}
         */
        
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
	/**
     * M: Modified to resolve 403 error issue. @{
     */
	protected void sendInvite(SipRequest invite) throws SipException {
	/**
	 * @}
	 */
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
            } else {
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
        	
			// Notify 1-1 session listeners
	    	for(int i=0; i < oneoneSession.getListeners().size(); i++) {
	    		((ChatSessionListener)oneoneSession.getListeners().get(i)).handleAddParticipantSuccessful();
			}
	        
			// Notify listener
			getImsService().getImsModule().getCore().getListener().handleOneOneChatSessionExtended(this, oneoneSession);
			
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
			handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION, e.getMessage()));
	    }
	}
	
	/**
	 * Handle error 
	 * 
	 * @param error Error
	 */
	public void handleError(ChatError error) {
		// Notify 1-1 session listeners
    	for(int i=0; i < oneoneSession.getListeners().size(); i++) {
    		((ChatSessionListener)oneoneSession.getListeners().get(i)).handleAddParticipantFailed(error.getMessage());
		}
	    
	    // Error
	    super.handleError(error);
	}	
}
