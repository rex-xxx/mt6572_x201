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

package com.orangelabs.rcs.core.ims.service.sip;

import java.util.Vector;

import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating SIP session
 * 
 * @author jexa7410
 */
public class OriginatingSipSession extends GenericSipSession {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param contact Remote contact
	 * @param featureTag Feature tag
	 * @param sdp SDP
	 */
	public OriginatingSipSession(ImsService parent, String contact, String featureTag, String sdp) {
		super(parent, contact, featureTag);
		
		// Create dialog path
		createOriginatingDialogPath();
		
		// Set the local SDP
		setLocalSdp(sdp);
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new session as originating");
	    	}
	    	/**
	    	 * M: Modified to resolve rich call 403 error. @{
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
			handleError(new SipSessionError(SipSessionError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}
	}
	
	/**
     * M: Modified to resolve rich call 403 error. @{
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
        // Set the local SDP part in the dialog path
        getDialogPath().setLocalContent(getLocalSdp());

        // Create an INVITE request
        if (logger.isActivated()) {
            logger.info("Send INVITE");
        }
        try {
            SipRequest invite;
            invite = SipMessageFactory.createInvite(getDialogPath(), new String[] {
                getFeatureTag()
            }, getLocalSdp());
            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);
            return invite;
        } catch (SipException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.error("Create sip invite failed, return null.");
        return null;
    }
    /**
     * @}
     */
	
	/**
	 * Send INVITE message
	 * 
	 * @param invite SIP INVITE
	 * @throws SipException
	 */
	@Override
	/**
     * M: Modified to resolve rich call 403 error. @{
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
            if (ctx.getStatusCode() == 603) {
            	// 603 Invitation declined
            	handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_DECLINED,
    					ctx.getReasonPhrase()));
            } else
            if (ctx.getStatusCode() == 487) {
            	// 487 Invitation cancelled
            	handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_CANCELLED,
    					ctx.getReasonPhrase()));
            }
            /**
             * M: Modified to resolve rich call 403 error. @{
             */
            else if(ctx.getStatusCode() == 403){
                handle403Forbidden(invite);
            }
            /**
             * @}
             */
            else {
            	// Other error response
            	handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED,
            			ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
            }
        } else {
        	// No response received: timeout
        	handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED, "timeout"));
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
	                      		
			// Set the SDP answer 
			setRemoteSdp(resp.getContent());
	        
	        // The session is established
	        getDialogPath().sessionEstablished();

	        // Send ACK request
	        if (logger.isActivated()) {
	        	logger.info("Send ACK");
	        }
	        getImsService().getImsModule().getSipManager().sendSipAck(getDialogPath());
	        
        	// Start session timer
        	if (getSessionTimerManager().isSessionTimerActivated(resp)) {
        		getSessionTimerManager().start(resp.getSessionTimerRefresher(), resp.getSessionTimerExpire());
        	}

	        // Notify listeners
	    	for(int i=0; i < getListeners().size(); i++) {
	    		getListeners().get(i).handleSessionStarted();
	        }
				        
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new SipSessionError(SipSessionError.UNEXPECTED_EXCEPTION,
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
	        SipRequest invite = SipMessageFactory.createInvite(
	        		getDialogPath(),
	        		new String [] { getFeatureTag() },
					getDialogPath().getLocalContent());

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
			handleError(new SipSessionError(SipSessionError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
        }
	}

	/**
	 * Handle error 
	 * 
	 * @param error Error
	 */
	public void handleError(SipSessionError error) {
        // Error	
    	if (logger.isActivated()) {
    		logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
    	}

        // Close media session
    	closeMediaSession();
    	
		// Remove the current session
    	getImsService().removeSession(this);

		// Notify listeners
    	if (!isInterrupted()) {
	    	for(int j=0; j < getListeners().size(); j++) {
	    		((SipSessionListener)getListeners().get(j)).handleSessionError(error);
	    	}
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
	        	handleError(new SipSessionError(SipSessionError.UNEXPECTED_EXCEPTION, "No Min-SE value found"));
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
	        SipRequest invite = SipMessageFactory.createInvite(
	        		getDialogPath(),
	        		new String [] { getFeatureTag() },
					getDialogPath().getLocalContent());

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
			handleError(new SipSessionError(SipSessionError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
	    }
	}		
	
	/**
	 * Close media session
	 */
	public void closeMediaSession() {
	}
}
