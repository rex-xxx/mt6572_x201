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

package com.orangelabs.rcs.core.ims.service;

import android.telephony.PhoneNumberUtils;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.Vector;

/**
 * IMS service session
 * 
 * @author jexa7410
 */
public abstract class ImsServiceSession extends Thread {
	/**
	 * Session invitation status
	 */
	public final static int INVITATION_NOT_ANSWERED = 0; 
	public final static int INVITATION_ACCEPTED = 1; 
	public final static int INVITATION_REJECTED = 2; 
    /** M: add for MSRPoTLS */
    protected static final String PROTOCOL_TLS = "TLS";
    protected static final String PROTOCOL_TCP = "TCP";

    /**
     * M: Added to resolve the rich call 403 error.@{
     */
    private final static int INIT_CSEQUENCE_NUMBER = 1;
    /**
     * @}
     */
    
	/**
     * IMS service
     */
    private ImsService imsService;
    
    /**
     * Session ID
     */
    private String sessionId =  SessionIdGenerator.getNewId();

	/**
	 * Remote contact
	 */
	private String contact;

    /**
	 * Dialog path
	 */
    private SipDialogPath dialogPath = null;

	/**
	 * Authentication agent
	 */
	private SessionAuthenticationAgent authenticationAgent;

	/**
	 * Session invitation status
	 */
	protected int invitationStatus = INVITATION_NOT_ANSWERED;
	
	/**
	 * Wait user answer for session invitation
	 */
	protected Object waitUserAnswer = new Object();

	/**
	 * Session listeners
	 */
	private Vector<ImsSessionListener> listeners = new Vector<ImsSessionListener>();

	/**
	 * Session timer manager
	 */
	private SessionTimerManager sessionTimer = new SessionTimerManager(this);

    /**
     * Ringing period (in seconds)
     */
    private int ringingPeriod = RcsSettings.getInstance().getRingingPeriod();

    /**
     * Session interrupted flag 
     */
    private boolean sessionInterrupted = false;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param contact Remote contact
     */
    public ImsServiceSession(ImsService imsService, String contact) {
        this.imsService = imsService;
        this.contact = contact;
        this.authenticationAgent = new SessionAuthenticationAgent(imsService.getImsModule());
    }

	/**
	 * Create originating dialog path
	 */
	public void createOriginatingDialogPath() {
        // Set Call-Id
    	String callId = getImsService().getImsModule().getSipManager().getSipStack().generateCallId();

    	// Set the route path
    	Vector<String> route = getImsService().getImsModule().getSipManager().getSipStack().getServiceRoutePath();

    	// Create a dialog path
    	dialogPath = new SipDialogPath(
    			getImsService().getImsModule().getSipManager().getSipStack(),
    			callId,
    			/**
    		     * M: Added to resolve the rich call 403 error.@{
    		     */
    			INIT_CSEQUENCE_NUMBER,
    			/**
    			 * @}
    			 */
				getRemoteContact(),
				ImsModule.IMS_USER_PROFILE.getPublicUri(),
				getRemoteContact(),
				route);
    	
    	// Set the authentication agent in the dialog path 
    	dialogPath.setAuthenticationAgent(getAuthenticationAgent());
	}
	
	/**
     * M: Added to resolve the rich call 403 error.@{
     */
    /**
     * Create originating dialog path
     */
    public void createOriginatingDialogPath(String callId) {
        logger.debug("createOriginatingDialogPath(), callId = " + callId);
        // Set the route path
        Vector<String> route = getImsService().getImsModule().getSipManager().getSipStack()
                .getServiceRoutePath();

        // Create a dialog path
        dialogPath = new SipDialogPath(
                getImsService().getImsModule().getSipManager().getSipStack(), callId,
                INIT_CSEQUENCE_NUMBER, getRemoteContact(),
                ImsModule.IMS_USER_PROFILE.getPublicUri(),
                getRemoteContact(), route);

        // Set the authentication agent in the dialog path
        dialogPath.setAuthenticationAgent(getAuthenticationAgent());
    }
    /**
     * @}
     */
    
	/**
	 * Create terminating dialog path
	 * 
	 * @param invite Incoming invite
	 */
	public void createTerminatingDialogPath(SipRequest invite) {
	    // Set the call-id
		String callId = invite.getCallId();
	
	    // Set target
	    String target = invite.getContactURI();
	
	    // Set local party
	    String localParty = invite.getTo();
	
	    // Set remote party
	    String remoteParty = invite.getFrom();
	
	    // Get the CSeq value
	    long cseq = invite.getCSeq();
	    
	    // Set the route path with the Record-Route 
	    Vector<String> route = SipUtils.routeProcessing(invite, false);
	    
	   	// Create a dialog path
		dialogPath = new SipDialogPath(
				getImsService().getImsModule().getSipManager().getSipStack(),
				callId,
				cseq,
				target,
				localParty,
				remoteParty,
				route);
	
	    // Set the INVITE request
		dialogPath.setInvite(invite);
	
	    // Set the remote tag
		dialogPath.setRemoteTag(invite.getFromTag());
	
	    // Set the remote content part
		dialogPath.setRemoteContent(invite.getContent());
		
		// Set the session timer expire
		dialogPath.setSessionExpireTime(invite.getSessionTimerExpire());
	}
	
	/**
	 * Add a listener for receiving events
	 * 
	 * @param listener Listener
	 */
	public void addListener(ImsSessionListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener
	 */
	public void removeListener(ImsSessionListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Remove all listeners
	 */
	public void removeListeners() {
		listeners.removeAllElements();
	}

	/**
	 * Returns the event listeners
	 * 
	 * @return Listeners
	 */
	public Vector<ImsSessionListener> getListeners() {
		return listeners;
	}
	
	/**
	 * Get the session timer manager
	 * 
	 * @return Session timer manager
	 */
	public SessionTimerManager getSessionTimerManager() {
		return sessionTimer;
	}

    /**
     * Is behind a NAT
     *
     * @return Boolean
     */
    public boolean isBehindNat() {
		return getImsService().getImsModule().getCurrentNetworkInterface().isBehindNat();
    }	

	/**
	 * Start the session in background
	 */
	public void startSession() {
		// Add the session in the session manager
		imsService.addSession(this);
		
		// Start the session
		start();
	}
	
	/**
	 * Return the IMS service
	 * 
	 * @return IMS service
	 */
	public ImsService getImsService() {
		return imsService;
	}
	
	/**
	 * Return the session ID
	 * 
	 * @return Session ID
	 */
	public String getSessionID() {
		return sessionId;
	}

	/**
	 * Returns the remote contact
	 * 
	 * @return String
	 */
	public String getRemoteContact() {
		return contact;
	}

	/**
	 * Returns display name of the remote contact
	 * 
	 * @return String
	 */
	public String getRemoteDisplayName() {
		String displayName = null;
		try {
			String from = getDialogPath().getInvite().getFrom();
			displayName = PhoneUtils.extractDisplayNameFromUri(from);
		} catch(Exception e) {
			displayName = null;
		}
		return displayName;
	}

	/**
	 * Get the dialog path of the session
	 * 
	 * @return Dialog path object
	 */
	public SipDialogPath getDialogPath() {
		return dialogPath;
	}

	/**
	 * Set the dialog path of the session
	 * 
	 * @param dialog Dialog path
	 */
	public void setDialogPath(SipDialogPath dialog) {
		dialogPath = dialog;
	}
	
    /**
     * Returns the authentication agent
     * 
     * @return Authentication agent
     */
	public SessionAuthenticationAgent getAuthenticationAgent() {
		return authenticationAgent;
	}
	
	/**
	 * Reject the session invitation
	 * 
	 * @param code Error code
	 */
	public void rejectSession(int code) {
		if (logger.isActivated()) {
			logger.debug("Session invitation has been rejected");
		}
		invitationStatus = INVITATION_REJECTED;

		// Unblock semaphore
		synchronized(waitUserAnswer) {
			waitUserAnswer.notifyAll();
		}

		// Decline the invitation
		sendErrorResponse(getDialogPath().getInvite(), getDialogPath().getLocalTag(), code);
			
		// Remove the session in the session manager
		imsService.removeSession(this);
	}	
	
	/**
	 * Accept the session invitation
	 */
	public void acceptSession() {
		if (logger.isActivated()) {
			logger.debug("Session invitation has been accepted");
		}
		invitationStatus = INVITATION_ACCEPTED;

		// Unblock semaphore
		synchronized(waitUserAnswer) {
			waitUserAnswer.notifyAll();
		}
	}
		
	/**
	 * Wait session invitation answer
	 * 
	 * @return Answer
	 */
	public int waitInvitationAnswer() {
		if (invitationStatus != INVITATION_NOT_ANSWERED) {
			return invitationStatus;
		}
		
		if (logger.isActivated()) {
			logger.debug("Wait session invitation answer");
		}
		
		// Wait until received response or received timeout
		try {
			synchronized(waitUserAnswer) {
				waitUserAnswer.wait(ringingPeriod * 1000);
			}
		} catch(InterruptedException e) {
			sessionInterrupted = true;
		}
		
		return invitationStatus;
	}
	
	/**
	 * Interrupt session
	 */
	public void interruptSession() {
		if (logger.isActivated()) {
			logger.debug("Interrupt the session");
		}
		
		try {
			// Unblock semaphore
			synchronized(waitUserAnswer) {
				waitUserAnswer.notifyAll();
			}
			
			if (!isSessionInterrupted()) {
				// Interrupt thread
				interrupt();
			}
		} catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Can't interrupt the session correctly", e);
        	}
		}
    	if (logger.isActivated()) {
    		logger.debug("Session has been interrupted");
    	}
	}

    /**
     * M: do not send SIP BYE when timeout to distinguish BOOTED from DEPARTED @{
     */
    /**
     * Abort the session
     */
    public void abortSessionWithoutBye() {
        if (logger.isActivated()) {
            logger.info("abortSessionWithoutBye() entry");
        }

        // Interrupt the session
        interruptSession();

        // Terminate session
        terminateSessionWithoutBy();

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        int size = getListeners().size();
        for (int i = 0; i < size; i++) {
            getListeners().get(i).handleSessionAborted();
        }
    }

    /** @} */

	/**
	 * Abort the session
	 */
	public void abortSession(){
    	if (logger.isActivated()) {
    		logger.info("Abort the session");
    	}
    	
    	// Interrupt the session
    	interruptSession();

        // Terminate session
		terminateSession();

    	// Close media session
    	closeMediaSession();

    	// Remove the current session
    	getImsService().removeSession(this);

    	// Notify listeners
    	for(int i=0; i < getListeners().size(); i++) {
    		getListeners().get(i).handleSessionAborted();
        }
	}

    /**
     * M: do not send SIP BYE when timeout to distinguish BOOTED from DEPARTED @{
     */
    /**
     * Terminate session
     */
    public void terminateSessionWithoutBy() {
        if (dialogPath.isSessionTerminated()) {
            // Already terminated
            return;
        }

        // Stop session timer
        getSessionTimerManager().stop();

        // Update dialog path
        dialogPath.sessionTerminated();

        // Unblock semaphore (used for terminating side only)
        synchronized (waitUserAnswer) {
            waitUserAnswer.notifyAll();
        }
    }

    /** @} */

	/**
	 * Terminate session 
	 */
	public void terminateSession() {
		if (logger.isActivated()) {
			logger.debug("Terminate the session");
		}
		
		if (dialogPath.isSessionTerminated()) {
			// Already terminated
			return;
		}
		
    	// Stop session timer
    	getSessionTimerManager().stop();		

		// Update dialog path
		dialogPath.sessionTerminated();

		// Unblock semaphore (used for terminating side only)
		synchronized(waitUserAnswer) {
			waitUserAnswer.notifyAll();
		}

		try {
			// Terminate the session
        	if (dialogPath.isSigEstablished()) {
		        // Increment the Cseq number of the dialog path
		        getDialogPath().incrementCseq();
	
		        // Send BYE without waiting a response
		        getImsService().getImsModule().getSipManager().sendSipBye(getDialogPath());
        	} else {
		        // Send CANCEL without waiting a response
		        getImsService().getImsModule().getSipManager().sendSipCancel(getDialogPath());
        	}
        	
        	if (logger.isActivated()) {
        		logger.debug("SIP session has been terminated");
        	}
		} catch(Exception e) { 
        	if (logger.isActivated()) {
        		logger.error("Session termination has failed", e);
        	}
		}
	}

	/**
	 * Receive BYE request 
	 * 
	 * @param bye BYE request
	 */
	public void receiveBye(SipRequest bye) {
    	if (logger.isActivated()) {
    		logger.info("Receive a BYE message from the remote");
    	}

    	// Close media session
    	closeMediaSession();
    	
        // Update the dialog path status
		getDialogPath().sessionTerminated();
	
    	// Remove the current session
    	getImsService().removeSession(this);
	
    	// Stop session timer
    	getSessionTimerManager().stop();		

    	// Notify listeners
    	for(int i=0; i < getListeners().size(); i++) {
    		getListeners().get(i).handleSessionTerminatedByRemote();
        }
	}
	
	/**
	 * Receive CANCEL request 
	 * 
	 * @param cancel CANCEL request
	 */
	public void receiveCancel(SipRequest cancel) {
    	if (logger.isActivated()) {
    		logger.info("Receive a CANCEL message from the remote");
    	}

		if (getDialogPath().isSigEstablished()) {
	    	if (logger.isActivated()) {
	    		logger.info("Ignore the received CANCEL message from the remote (session already established)");
	    	}
			return;
		}

    	// Close media session
    	closeMediaSession();
    	
    	// Update dialog path
		getDialogPath().sessionCancelled();

		// Send a 487 Request terminated
    	try {
	    	if (logger.isActivated()) {
	    		logger.info("Send 487 Request terminated");
	    	}
	        SipResponse terminatedResp = SipMessageFactory.createResponse(getDialogPath().getInvite(), IdGenerator.getIdentifier(), 487);
	        getImsService().getImsModule().getSipManager().sendSipResponse(terminatedResp);
		} catch(Exception e) {
	    	if (logger.isActivated()) {
	    		logger.error("Can't send 487 error response", e);
	    	}
		}
		
    	// Remove the current session
    	getImsService().removeSession(this);

		// Notify listeners
    	for(int i=0; i < getListeners().size(); i++) {
    		getListeners().get(i).handleSessionTerminatedByRemote();
        }
        
        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());
	}

	/**
	 * Receive re-INVITE request 
	 * 
	 * @param reInvite re-INVITE request
	 */
	public void receiveReInvite(SipRequest reInvite) {
        sessionTimer.receiveReInvite(reInvite);
	}

	/**
	 * Receive UPDATE request 
	 * 
	 * @param update UPDATE request
	 */
	public void receiveUpdate(SipRequest update) {
		sessionTimer.receiveUpdate(update);		
	}

	/**
	 * Close media session
	 */
	public abstract void closeMediaSession();

	/**
     * Send a 180 Ringing response to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     */
	public void send180Ringing(SipRequest request, String localTag) {
    	try {
	    	SipResponse progress = SipMessageFactory.createResponse(request, localTag, 180);
            getImsService().getImsModule().getSipManager().sendSipResponse(progress);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Can't send a 180 Ringing response");
    		}
    	}
    }
	

    /**
     * Send an error response to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     * @param code Response code
     */
	public void sendErrorResponse(SipRequest request, String localTag, int code) {
		try {
	        // Send  error
	    	if (logger.isActivated()) {
	    		logger.info("Send " + code + " error response");
	    	}
	        SipResponse resp = SipMessageFactory.createResponse(request, localTag, code);
	        getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send error response", e);
			}
		}
	}
	
	/**
     * Send a 603 "Decline" to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     */
	public void send603Decline(SipRequest request, String localTag) {
		try {
	        // Send a 603 Decline error
	    	if (logger.isActivated()) {
	    		logger.info("Send 603 Decline");
	    	}
	        SipResponse resp = SipMessageFactory.createResponse(request, localTag, 603);
	        getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send 603 Decline response", e);
			}
		}
	}
	
    /**
     * Send a 486 "Busy" to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     */
	public void send486Busy(SipRequest request, String localTag) {
		try {
	        // Send a 486 Busy error
	    	if (logger.isActivated()) {
	    		logger.info("Send 486 Busy");
	    	}
	        SipResponse resp = SipMessageFactory.createResponse(request, localTag, 486);
	        getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send 486 Busy response", e);
			}
		}
	}	
	
    /**
     * Send a 415 "Unsupported Media Type" to the remote party
     * 
     * @param request SIP request
     */
	public void send415Error(SipRequest request) {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Send 415 Unsupported Media Type");
	    	}
	        SipResponse resp = SipMessageFactory.createResponse(request, 415);
	        // TODO: set Accept-Encoding header
	        getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send 415 error response", e);
			}
		}
	}	
	
	/**
	 * Create SDP setup offer (see RFC6135, RFC4145)
	 * 
	 * @return Setup offer ("active" or "actpass")
	 */
	public String createSetupOffer() {
    	if (isBehindNat()) {
        	// Active mode by default if there is a NAT. Active/passive mode is
    		// exchanged in order to be compatible with UE not supporting COMEDIA
    		return "actpass";
    	} else {
        	return "active";
    	}
	}
	
	/**
	 * Create SDP setup answer (see RFC6135, RFC4145)
	 * 
	 * @param offer setup offer
	 * @return Setup answer ("active" or "passive")
	 */
	public String createSetupAnswer(String offer) {
    	if (offer.equals("actpass")) {
        	// Active mode by default if there is a NAT or AS IM
    		return "active";
    	} else
        if (offer.equals("active")) {
        	// Passive mode
			return "passive";
        } else 
        if (offer.equals("passive")) {
        	// Active mode
			return "active";
        } else {
        	// Passive mode by default
			return "passive";
        }
	}
	
	/**
	 * Returns the response timeout (in seconds) 
	 * 
	 * @return Timeout
	 */
	public int getResponseTimeout() {
		return ringingPeriod + SipManager.TIMEOUT;
	}
	
	/**
     * M: Added to resolve the rich call 403 error.@{
     */
    /**
     * Handle 403 error. First do re-register then send request again
     * 
     * @param request The request was responded with 403
     */
    public void handle403Forbidden(SipRequest request) {
        if (logger.isActivated()) {
            logger.debug("handle403Forbidden() entry");
        }
        boolean isRegistered = imsService.getImsModule().getCurrentNetworkInterface()
                .getRegistrationManager().registration();
        if (logger.isActivated()) {
            logger.debug("re-register isRegistered: " + isRegistered);
        }
        if (isRegistered) {
            String callId = dialogPath.getCallId();
            SipRequest invite = createSipInvite(callId);
            if (invite != null) {
                try {
                    sendInvite(invite);
                } catch (SipException e) {
                    if (logger.isActivated()) {
                        logger.debug("re send sip request failed.");
                    }
                    e.printStackTrace();
                }

            } else {
                if (logger.isActivated()) {
                    logger.debug("handle403Forbidden() invite is null");
                }
            }
        }
        if (logger.isActivated()) {
            logger.debug("handle403Forbidden() exit");
        }
    }

    protected void sendInvite(SipRequest invite) throws SipException {
        logger.debug("ImsServiceSession::sendInvite(), do nothing in the parent class");
    }

    protected SipRequest createSipInvite(String callId) {
        logger.debug("ImsServiceSession::createSipInvite(), do nothing in the parent class");
        return null;
    }
    /**
     * @}
     */

    /** M: add for MSRPoTLS @{ */
    protected String getCurrentProtocol() {
        if (logger.isActivated()) {
            logger.debug("getCurrentProtocol entry");
        }
        String protocol = getImsService().getImsModule().getCurrentNetworkInterface()
                .getMsrpProtocol();
        if (logger.isActivated()) {
            logger.debug("getCurrentProtocol exit, protocol: " + protocol);
        }
        return protocol;
    }
    /** @} */

	/**
	 * Is session interrupted
	 * 
	 * @return Boolean
	 */
	public boolean isSessionInterrupted() {
		return sessionInterrupted;
	}
}
