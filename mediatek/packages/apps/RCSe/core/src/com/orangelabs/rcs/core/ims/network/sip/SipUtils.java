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

package com.orangelabs.rcs.core.ims.network.sip;

import com.orangelabs.rcs.core.TerminalInfo;
import com.orangelabs.rcs.core.ims.protocol.sip.SipMessage;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.sip.InvalidArgumentException;
import javax.sip.address.AddressFactory;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExtensionHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ServerHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

/**
 * SIP utility functions
 * 
 * @author JM. Auffret
 */
public class SipUtils {
	/**
	 * CRLF constant
	 */
	public final static String CRLF = "\r\n";
	
	/**
	 * Header factory
	 */
	public static HeaderFactory HEADER_FACTORY = null;
		
	/**
	 * Address factory
	 */
	
	public static AddressFactory ADDR_FACTORY = null;

	/**
	 * Message factory
	 */
	public static MessageFactory MSG_FACTORY = null;	
		
	/**
	 * Accept-Contact header
	 */
	public static final String HEADER_ACCEPT_CONTACT = "Accept-Contact";
	public static final String HEADER_ACCEPT_CONTACT_C = "a";
	
	/**
	 * P-Access-Network-Info header
	 */
	public static final String HEADER_P_ACCESS_NETWORK_INFO = "P-Access-Network-Info";

	/**
	 * M:P-Last-Access-Network-Info header@{T-Mobile
	 */
	public static final String HEADER_P_LAST_ACCESS_NETWORK_INFO = "P-Last-Access-Network-Info";
	/**
	 * @}
	 */
	
	/**
	 * P-Asserted-Identity header
	 */
	public static final String HEADER_P_ASSERTED_IDENTITY = "P-Asserted-Identity";
	
	/**
	 * P-Preferred-Identity header
	 */
	public static final String HEADER_P_PREFERRED_IDENTITY = "P-Preferred-Identity";
	
	/**
	 * P-Associated-URI header
	 */
	public static final String HEADER_P_ASSOCIATED_URI = "P-Associated-URI";
	
	/**
	 * Service-Route header
	 */
	public static final String HEADER_SERVICE_ROUTE = "Service-Route";
	
	/**
	 * Privacy header
	 */
	public static final String HEADER_PRIVACY = "Privacy";
	
	/**
	 * Refer-Sub header
	 */
	public static final String HEADER_REFER_SUB = "Refer-Sub";
	
	/**
	 * Referred-By header
	 */
	public static final String HEADER_REFERRED_BY = "Referred-By";
	public static final String HEADER_REFERRED_BY_C = "b";
	
	/**
	 * Content-ID header
	 */
	public static final String HEADER_CONTENT_ID = "Content-ID";
	
	/**
	 * Session-Expires header
	 */
	public static final String HEADER_SESSION_EXPIRES = "Session-Expires";

	/**
	 * Session-Replaces header
	 */
	public static final String HEADER_SESSION_REPLACES = "Session-Replaces";

	/**
	 * Min-SE header
	 */
	public static final String HEADER_MIN_SE = "Min-SE";

    /** M: add for group chat invitation @{ */
    /**
     * Require header
     */
    public static final String HEADER_REQUIRE = "Require";

    /** @} */
	/**
	 * Extract the URI part of a SIP address
	 * 
	 * @param addr SIP address
	 * @return URI
	 */
	public static String extractUriFromAddress(String addr) {
		String uri = addr; 
		try {
			int index = addr.indexOf("<");
			if (index != -1) {
				uri = addr.substring(index+1, addr.indexOf(">", index));
			}			
		} catch(Exception e) {
		}		
		return uri;
	}
	
	/**
	 * Construct an NTP time from a date in milliseconds
	 *
	 * @param date Date in milliseconds
	 * @return NTP time in string format
	 */
	public static String constructNTPtime(long date) {
		long ntpTime = 2208988800L;
		long startTime = (date / 1000) + ntpTime;
		return String.valueOf(startTime);
	}

    /**
     * Build User-Agent header
     * 
     * @param Header
     */
	public static Header buildUserAgentHeader() throws Exception {
	    String value = "IM-client/OMA1.0 " + TerminalInfo.getProductName() + "/" + TerminalInfo.getProductVersion();
	    Header userAgentHeader = HEADER_FACTORY.createHeader(UserAgentHeader.NAME, value);
	    return userAgentHeader;
    }
	
	/**
     * Build Server header
     * 
     * @return Header
     * @throws Exception
     */
	public static Header buildServerHeader() throws Exception {
	    String value = "IM-client/OMA1.0 " + TerminalInfo.getProductName() + "/" + TerminalInfo.getProductVersion();
		return HEADER_FACTORY.createHeader(ServerHeader.NAME, value);
    }
    
	/**
	 * Build Allow header
	 * 
	 * @param msg SIP message
	 */
	public static void buildAllowHeader(Message msg) throws Exception {
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.INVITE));
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.UPDATE));
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.ACK));
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.CANCEL));
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.BYE));
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.NOTIFY));
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.OPTIONS));
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.MESSAGE));
		msg.addHeader(HEADER_FACTORY.createAllowHeader(Request.REFER));
    }

	/**
     * Build Max-Forwards header
     * 
     * @return Header
     * @throws InvalidArgumentException
     */
	public static MaxForwardsHeader buildMaxForwardsHeader() throws InvalidArgumentException {
    	return HEADER_FACTORY.createMaxForwardsHeader(70);	
	}
       
    /**
	 * Build P-Access-Network-info
	 * 
	 * @param info Access info
	 * @return Header
	 * @throws Exception
	 */
    public static Header buildAccessNetworkInfo(String info) throws Exception {
		Header accessInfo = HEADER_FACTORY.createHeader(SipUtils.HEADER_P_ACCESS_NETWORK_INFO, info);
		return accessInfo;
    }

    /**
     * M:Add to build the P-Last-Access-Network-info header.@{T-Mobile
     */
    /**
     * Build P-Last-Access-Network-info
     * 
     * @param info Access info
     * @return Header
     * @throws Exception
     */
    public static Header buildLastAccessNetworkInfo(String info) throws Exception {
        Header lastAccessInfo = HEADER_FACTORY.createHeader(
                SipUtils.HEADER_P_LAST_ACCESS_NETWORK_INFO, info);
        return lastAccessInfo;
    }

    /**
     * @}
     */

    /**
     * Extract a parameter from an input text
     * 
     * @param input Input text
     * @param param Parameter name
     * @param defaultValue Default value
     * @return Returns the parameter value or a default value in case of error
     */
    public static String extractParameter(String input, String param, String defaultValue) {
    	try {
	    	int begin = input.indexOf(param) + param.length();
	    	if (begin != -1) {
	    		int end = input.indexOf(" ", begin); // The end is by default the next space encountered
	    		if (input.charAt(begin) == '\"'){
	    			// The exception is when the first character of the param is a "
	    			// In this case, the end is the next " character, not the blank one
	    			begin++; // we remove also the first quote
	    			end = input.indexOf("\"",begin); // do not take last doubleQuote
	    		}
		    	if (end == -1) {
		    		return input.substring(begin);
		    	} else {
		    		return input.substring(begin, end);
		    	}
	    	}
			return defaultValue;
    	} catch(Exception e) {
    		return defaultValue;
    	}
    }

    /**
	 * Get Min-Expires period from message
	 * 
	 * @param message SIP message
	 * @return Expire period in seconds or -1 in case of error
	 */
	public static int getMinExpiresPeriod(SipMessage message) {
		MinExpiresHeader minHeader = (MinExpiresHeader)message.getHeader(MinExpiresHeader.NAME);
		if (minHeader != null) {
			return minHeader.getExpires();
		} else {
			return -1;
		}
	}

    /**
	 * Get Min-SE period from message
	 * 
	 * @param message SIP message
	 * @return Expire period in seconds or -1 in case of error
	 */
	public static int getMinSessionExpirePeriod(SipMessage message) {
		ExtensionHeader minSeHeader = (ExtensionHeader)message.getHeader(SipUtils.HEADER_MIN_SE);
		if (minSeHeader != null) {
			String value = minSeHeader.getValue();
			return Integer.parseInt(value);
		} else {
			return -1;
		}
	}
	
	/**
	 * Get asserted identity
	 * 
	 * @param request SIP request
	 * @return SIP URI
	 */
	public static String getAssertedIdentity(SipRequest request) {
		ExtensionHeader assertedHeader = (ExtensionHeader)request.getHeader(SipUtils.HEADER_P_ASSERTED_IDENTITY);
		if (assertedHeader != null) {
			return assertedHeader.getValue();
		} else {
			return request.getFromUri();
		}
	}
	
    /**
	 * Generate a list of route headers. The record route of the incoming message
	 * is used to generate the corresponding route header.
	 * 
	 * @param msg SIP message
	 * @param invert Invert or not the route list
	 * @return List of route headers as string
	 * @throws Exception
	 */
	public static Vector<String> routeProcessing(SipMessage msg, boolean invert) {
		Vector<String> result = new Vector<String>(); 
		ListIterator<Header> list = msg.getHeaders(RecordRouteHeader.NAME); 
		if (list == null) {
			// No route available
			return null;
		}

        while(list.hasNext()) {
        	RecordRouteHeader record = (RecordRouteHeader)list.next();
            RouteHeader route = SipUtils.HEADER_FACTORY.createRouteHeader(record.getAddress());
            if (invert) {
            	result.insertElementAt(route.getAddress().toString(), 0);
            } else {
            	result.addElement(route.getAddress().toString());
            }
		}

		return result;
	}
	
    /**
     * Is a feature tag present or not in SIP message
     * 
     * @param request Request
     * @param featuretag Feature tag to be checked
     * @return Boolean
     */
    public static boolean isFeatureTagPresent(SipRequest request, String featuretag) {
    	ArrayList<String> featureTags = request.getFeatureTags();
    	return featureTags.contains(featuretag);
    }	

    /**
     * Set feature tags to a message
     * 
     * @param message SIP message
     * @param tags Table of tags
     * @throws Exception
     */
    public static void setFeatureTags(SipMessage message, String[] tags) throws Exception {
    	setFeatureTags(message.getStackMessage(), tags);
    }
    
    /**
     * Set feature tags to a message
     * 
     * @param message SIP stack message
     * @param tags Table of tags
     * @throws Exception
     */
    public static void setFeatureTags(Message message, String[] tags) throws Exception {
    	List<String> list = Arrays.asList(tags);  
    	setFeatureTags(message, list);
    }
    
    /**
     * Set feature tags to a message
     * 
     * @param message SIP stack message
     * @param tags List of tags
     * @throws Exception
     */
    public static void setFeatureTags(Message message, List<String> tags) throws Exception {
    	setContactFeatureTags(message, tags);
    	setAcceptContactFeatureTags(message, tags);
    }
    
    /**
     * Set feature tags to Accept-Contact header
     * 
     * @param message SIP stack message
     * @param tags List of tags
     * @throws Exception
     */
    public static void setAcceptContactFeatureTags(Message message, List<String> tags) throws Exception {
    	if ((tags == null) || (tags.size() == 0)) {
    		return;
    	}
    	
    	// Update Contact header
    	StringBuffer acceptTags = new StringBuffer("*");
    	for(int i=0; i < tags.size(); i++) {
    		acceptTags.append(";" + tags.get(i));
    	}
    	
    	// Update Accept-Contact header
		Header header = SipUtils.HEADER_FACTORY.createHeader(SipUtils.HEADER_ACCEPT_CONTACT, acceptTags.toString());
		message.addHeader(header);
    }

    /**
     * Set feature tags to Contact header
     * 
     * @param message SIP stack message
     * @param tags List of tags
     * @throws Exception
     */
    public static void setContactFeatureTags(Message message, List<String> tags) throws Exception {
        if ((tags == null) || (tags.size() == 0)) {
            return;
        }
        
        // Update Contact header
        ContactHeader contact = (ContactHeader)message.getHeader(ContactHeader.NAME);
        for(int i=0; i < tags.size(); i++) {
            if (contact != null) {
                contact.setParameter(tags.get(i), null);
            }
        }
    }

    /**
     * Get the Referred-By header
     * 
	 * @param message SIP message
     * @return Strong or null if not exist
     */
    public static String getReferredByHeader(SipMessage message) {
		// Read Referred-By header
		ExtensionHeader referredByHeader = (ExtensionHeader)message.getHeader(SipUtils.HEADER_REFERRED_BY);
		if (referredByHeader == null) {
			// Check contracted form
			referredByHeader = (ExtensionHeader)message.getHeader(SipUtils.HEADER_REFERRED_BY_C);
			if (referredByHeader == null) {
				// Try to extract manually the header in the message
				// TODO: to be removed when bug fix corrected in native NIST stack
				String msg = message.getStackMessage().toString();
				int index = msg.indexOf(SipUtils.CRLF + "b:");
				if (index != -1) {
					try {
						int begin = index+4;
						int end = msg.indexOf(SipUtils.CRLF, index+2);
						return msg.substring(begin, end).trim();
					} catch(Exception e) {
						return null;
					}
				} else {
					return null;
				}
			} else {
				return referredByHeader.getValue();
			}
		} else {
			return referredByHeader.getValue();
		}
    }
}
