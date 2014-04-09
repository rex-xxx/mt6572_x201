package com.orangelabs.rcs.service.api.client.richcall;

import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaRenderer;

/**
 * Video sharing session interface
 */
interface IVideoSharingSession {
	// Get session ID
	String getSessionID();

	// Get remote contact
	String getRemoteContact();
	
	// Get session state
	int getSessionState();

	// Accept the session invitation
	void acceptSession();

	// Reject the session invitation
	void rejectSession();

	// Cancel the session
	void cancelSession();

	// Set the media renderer
	void setMediaRenderer(in IMediaRenderer renderer);

	// Add session listener
	void addSessionListener(in IVideoSharingEventListener listener);

	// Remove session listener
	void removeSessionListener(in IVideoSharingEventListener listener);
}
