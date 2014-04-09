package com.orangelabs.rcs.service.api.client.sip;

import com.orangelabs.rcs.service.api.client.sip.ISipSession;

/**
 * SIP API
 */
interface ISipApi {

	// Initiate a SIP session
	ISipSession initiateSession(in String contact, in String featureTag, in String sdp);

	// Get a SIP session from its session ID
	ISipSession getSession(in String id);

	// Get list of SIP sessions with a contact
	List<IBinder> getSessionsWith(in String contact);

	// Get list of current established SIP sessions
	List<IBinder> getSessions();
}


