package com.orangelabs.rcs.service.api.client.messaging;

import com.orangelabs.rcs.service.api.client.messaging.IFileTransferEventListener;

/**
 * File transfer session interface
 */
interface IFileTransferSession {
	// Get session ID
	String getSessionID();

	// Get remote contact
	String getRemoteContact();

	// Get session state
	int getSessionState();

	// Get filename
	String getFilename();
	
	// Get filesize
	long getFilesize();

	// Accept the session invitation
	void acceptSession();

	// Reject the session invitation
	void rejectSession();

	// Cancel the session
	void cancelSession();

	// Add session listener
	void addSessionListener(in IFileTransferEventListener listener);

	// Remove session listener
	void removeSessionListener(in IFileTransferEventListener listener);
}
