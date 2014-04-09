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

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.Enumeration;
import java.util.Vector;

/**
 * SIP service
 * 
 * @author jexa7410
 */
public class SipService extends ImsService {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @throws CoreException
     */
	public SipService(ImsModule parent) throws CoreException {
        super(parent, true);
	}

    /**
     * /** Start the IMS service
     */
	public synchronized void start() {
		if (isServiceStarted()) {
			// Already started
			return;
		}
		setServiceStarted(true);
	}

    /**
     * Stop the IMS service
     */
	public synchronized void stop() {
		if (!isServiceStarted()) {
			// Already stopped
			return;
		}
		setServiceStarted(false);
	}

	/**
     * Check the IMS service
     */
	public void check() {
	}

    /**
     * Initiate a session
     * 
     * @param contact Remote contact
     * @param featureTag Feature tag of the service
     * @param offer SDP offer
     * @return SIP session
     */
	public GenericSipSession initiateSession(String contact, String featureTag, String offer) {
		if (logger.isActivated()) {
			logger.info("Initiate a session with contact " + contact);
		}

		// Create a new session
		OriginatingSipSession session = new OriginatingSipSession(
				this,
				PhoneUtils.formatNumberToSipUri(contact),
				featureTag,
				offer);

		// Start the session
		session.startSession();
		return session;
	}

    /**
     * Reveive a session invitation
     * 
     * @param invite Initial invite
     */
	public void receiveSessionInvitation(SipRequest invite) {
		// Create a new session
    	TerminatingSipSession session = new TerminatingSipSession(
					this,
					invite);

		// Start the session
		session.startSession();

		// Notify listener
		getImsModule().getCore().getListener().handleSipSessionInvitation(session);
	}

    /**
     * Returns SIP sessions
     * 
     * @return List of sessions
     */
	public Vector<GenericSipSession> getSipSessions() {
		// Search all SIP sessions
		Vector<GenericSipSession> result = new Vector<GenericSipSession>();
		Enumeration<ImsServiceSession> list = getSessions();
		while(list.hasMoreElements()) {
			ImsServiceSession session = list.nextElement();
			if (session instanceof GenericSipSession) {
				result.add((GenericSipSession)session);
			}
		}

		return result;
    }

	/**
     * Returns SIP sessions with a given contact
     * 
     * @param contact Contact
     * @return List of sessions
     */
	public Vector<GenericSipSession> getSipSessionsWith(String contact) {
		// Search all SIP sessions
		Vector<GenericSipSession> result = new Vector<GenericSipSession>();
		Enumeration<ImsServiceSession> list = getSessions();
		while(list.hasMoreElements()) {
			ImsServiceSession session = list.nextElement();
			if ((session instanceof GenericSipSession) && PhoneUtils.compareNumbers(session.getRemoteContact(), contact)) {
				result.add((GenericSipSession)session);
			}
		}

		return result;
    }
}
