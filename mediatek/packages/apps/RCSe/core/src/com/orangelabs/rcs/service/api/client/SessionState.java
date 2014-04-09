package com.orangelabs.rcs.service.api.client;

/**
 * Session state
 * 
 * @author jexa7410
 */
public interface SessionState {
	/**
	 * Session state is unknown (i.e. session dialog path does not exist)
	 */
	public final static int UNKNOWN = -1;
	
	/**
	 * Session has been cancelled (i.e. SIP CANCEL exchanged)
	 */
	public final static int CANCELLED = 0;
	
	/**
	 * Session has been established (i.e. 200 OK/ACK exchanged)
	 */
	public final static int ESTABLISHED = 1;
	
	/**
	 * Session has been terminated (i.e. SIP BYE exchanged)
	 */
	public final static int TERMINATED = 2;
	
	/**
	 * Session is pending (not yet accepted by a final response by the remote)
	 */
	public final static int PENDING = 3;
}
