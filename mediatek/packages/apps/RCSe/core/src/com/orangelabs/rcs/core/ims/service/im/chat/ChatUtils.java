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

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

import javax.sip.header.ContactHeader;
import javax.sip.header.ExtensionHeader;

import org.xml.sax.InputSource;

import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnParser;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;

/**
 * Chat utility functions
 * 
 * @author jexa7410
 */
public class ChatUtils {
	/**
	 * Anonymous URI
	 */
	public final static String ANOMYNOUS_URI = "sip:anonymous@anonymous.invalid";
	
	/**
	 * Contribution ID header
	 */
	public static final String HEADER_CONTRIBUTION_ID = "Contribution-ID";
	
	/**
	 * CRLF constant
	 */
	private static final String CRLF = "\r\n";

    /**
     * M: Added to reserve XML content issue. @{
     */
    private static final String XML_FIRST_LINE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String XML_ELEMENT_RESOURCE_LISTS_BEGIN = "<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\" xmlns:cc=\"urn:ietf:params:xml:ns:copycontrol\">";
    private static final String XML_ELEMENT_RESOURCE_LISTS_END = "</resource-lists>";
    private static final String XML_ELEMENT_LIST_BEGIN = "<list>";
    private static final String XML_ELEMENT_LIST_END = "</list>";
    private static final String XML_ELEMENT_ENTRY_LEFT = " <entry cc:copyControl=\"to\" uri=\"";
    private static final String XML_ELEMENT_ENTRY_RIGHT = "\"/>";
    /**
     * @}
     */
	
    /**
     * Get contribution ID
     * 
     * @return String
     */
    public static String getContributionId(SipRequest request) {
        ExtensionHeader contribHeader = (ExtensionHeader)request.getHeader(ChatUtils.HEADER_CONTRIBUTION_ID);
        if (contribHeader != null) {
            return contribHeader.getValue();
        } else {
            return null;
        }
    }
	/**
	 * Is a group chat session invitation
	 * 
	 * @param request Request
	 * @return Boolean
	 */
	public static boolean isGroupChatInvitation(SipRequest request) {
        ContactHeader contactHeader = (ContactHeader)request.getHeader(ContactHeader.NAME);
		String param = contactHeader.getParameter("isfocus");
		if (param != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get referred identity
	 * 
	 * @param request SIP request
	 * @return SIP URI
	 */
	public static String getReferredIdentity(SipRequest request) {
		String referredBy = SipUtils.getReferredByHeader(request);
		if (referredBy != null) {
			// Use the Referred-By header
			return referredBy;
		} else {
			// Use the Asserted-Identity header
			return SipUtils.getAssertedIdentity(request);
		}
	}
	
	/**
     * Is a plain text type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isTextPlainType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(InstantMessage.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is a composing event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isApplicationIsComposingType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(IsComposingInfo.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }    

    /**
     * Is a CPIM message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageCpimType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(CpimMessage.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is an IMDN message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageImdnType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(ImdnDocument.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Generate a unique message ID
     * 
     * @return Message ID
     */
    public static String generateMessageId() {
    	return "Msg" + IdGenerator.getIdentifier().replace('_', '-');
    }

    /**
     * Generate resource-list for a chat session
     * 
     * @param participants List of participants
     * @return XML document
     */
    public static String generateChatResourceList(List<String> participants) {
		StringBuffer uriList = new StringBuffer();
		for(int i=0; i < participants.size(); i++) {
			String contact = participants.get(i);
			/**
		     * M: Added to reserve XML content issue. @{
		     */
			//uriList.append(" <entry uri=\"" + PhoneUtils.formatNumberToSipUri(contact) + "\"/>" + CRLF);
			uriList.append(XML_ELEMENT_ENTRY_LEFT + PhoneUtils.formatNumberToSipUri(contact) + XML_ELEMENT_ENTRY_RIGHT + CRLF);
			/**
			 * @}
			 */
		}
		
		/**
	     * M: Added to reserve XML content issue. @{
	     */
		//String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
		//	"<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\">" +
		//	"<list>" + CRLF +
		//	uriList.toString() +
		//	"</list></resource-lists>";
        String xml = XML_FIRST_LINE + CRLF + XML_ELEMENT_RESOURCE_LISTS_BEGIN
                + XML_ELEMENT_LIST_BEGIN + CRLF + uriList.toString() + XML_ELEMENT_LIST_END
                + XML_ELEMENT_RESOURCE_LISTS_END;
        /**
         * @}
         */
		return xml;
    }    

    /**
     * Generate resource-list for a extended chat session
     * 
     * @param existingParticipant Replaced participant
     * @param replaceHeader Replace header
     * @param newParticipants List of new participants
     * @return XML document
     */
    public static String generateExtendedChatResourceList(String existingParticipant, String replaceHeader, List<String> newParticipants) {
		StringBuffer uriList = new StringBuffer();
		for(int i=0; i < newParticipants.size(); i++) {
			String contact = newParticipants.get(i);
			if (contact.equals(existingParticipant)) {
			    /**
			     * M: Added to reserve XML content issue. @{
			     */
				//uriList.append(" <entry uri=\"" + PhoneUtils.formatNumberToSipUri(existingParticipant) +
				//	StringUtils.encodeXML(replaceHeader) + "\"/>" + CRLF);
				uriList.append(XML_ELEMENT_ENTRY_LEFT + PhoneUtils.formatNumberToSipUri(existingParticipant) +
	                    StringUtils.encodeXML(replaceHeader) + XML_ELEMENT_ENTRY_RIGHT + CRLF);
				/**
				 * @}
				 */
			} else {
			    /**
			     * M: Added to reserve XML content issue. @{
			     */
				//uriList.append(" <entry uri=\"" + PhoneUtils.formatNumberToSipUri(contact) + "\"/>" + CRLF);
			    uriList.append(XML_ELEMENT_ENTRY_LEFT + PhoneUtils.formatNumberToSipUri(contact) + XML_ELEMENT_ENTRY_RIGHT + CRLF);
				/**
				 * @}
				 */
			}
		}
		
		/**
	     * M: Added to reserve XML content issue. @{
	     */
		//String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
		//	"<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\">" +
		//	"<list>" + CRLF +
		//	uriList.toString() +
		//	"</list></resource-lists>";
        String xml = XML_FIRST_LINE + CRLF + XML_ELEMENT_RESOURCE_LISTS_BEGIN
                + XML_ELEMENT_LIST_BEGIN + CRLF + uriList.toString() + XML_ELEMENT_LIST_END
                + XML_ELEMENT_RESOURCE_LISTS_END;
        /**
         * @}
         */
		
		return xml;
    }    

    /**
     * Is IMDN service
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnService(SipRequest request) {
    	String content = request.getContent();
    	String contentType = request.getContentType();
    	if ((content != null) && (content.contains(ImdnDocument.IMDN_NAMESPACE)) &&
    			(contentType != null) && (contentType.equalsIgnoreCase(CpimMessage.MIME_TYPE))) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is IMDN notification "delivered" requested
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnDeliveredRequested(SipRequest request) {
    	boolean result = false;
		try {
			// Read ID from multipart content
		    String content = request.getContent();
			int index = content.indexOf(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
			if (index != -1) {
				index = index+ImdnUtils.HEADER_IMDN_DISPO_NOTIF.length()+1;
				String part = content.substring(index);
				String notif = part.substring(0, part.indexOf(CRLF));
		    	if (notif.indexOf(ImdnDocument.POSITIVE_DELIVERY) != -1) {
		    		result = true;
		    	}
			}
		} catch(Exception e) {
			result = false;
		}
		return result;
    }
    
    /**
     * Is IMDN notification "displayed" requested
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnDisplayedRequested(SipRequest request) {
    	boolean result = false;
		try {
			// Read ID from multipart content
		    String content = request.getContent();
			int index = content.indexOf(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
			if (index != -1) {
				index = index+ImdnUtils.HEADER_IMDN_DISPO_NOTIF.length()+1;
				String part = content.substring(index);
				String notif = part.substring(0, part.indexOf(CRLF));
		    	if (notif.indexOf(ImdnDocument.DISPLAY) != -1) {
		    		result = true;
		    	}
			}
		} catch(Exception e) {
			result = false;;
		}
		return result;
    }
    
	/**
	 * Returns the message ID from a SIP request
	 * 
     * @param request Request
	 * @return Message ID
	 */
	public static String getMessageId(SipRequest request) {
		String result = null;
		try {
			// Read ID from multipart content
		    String content = request.getContent();
			int index = content.indexOf(ImdnUtils.HEADER_IMDN_MSG_ID);
			if (index != -1) {
				index = index+ImdnUtils.HEADER_IMDN_MSG_ID.length()+1;
				String part = content.substring(index);
				String msgId = part.substring(0, part.indexOf(CRLF));
				result = msgId.trim();
			}
		} catch(Exception e) {
			result = null;
		}
		return result;
	}
	
    /**
     * Format to a SIP-URI for CPIM message
     * 
     * @param input Input
     * @return SIP-URI
     */
    private static String formatCpimSipUri(String input) {
    	input = input.trim();
    	
    	if (input.startsWith("<")) {
    		// Already a SIP-URI format
    		return input;    		
    	}

    	// It's a SIP address: remove display name
		if (input.startsWith("\"")) {
			int index1 = input.indexOf("\"", 1);
			if (index1 > 0) {
				input = input.substring(index1+2);
			}
			return input;
		}   

    	if (input.startsWith("sip:") || input.startsWith("tel:")) {
    		// Just add URI delimiter
    		return "<" + input + ">";
    	} else {
    		// It's a number, format it
    		return "<" + PhoneUtils.formatNumberToSipUri(input) + ">";
    	}
    }
	
	/**
	 * Build a CPIM message
	 * 
	 * @param from From
	 * @param to To
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
	public static String buildCpimMessage(String from, String to, String content, String contentType) {
		String cpim =
			CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + CRLF + 
			CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + CRLF + 
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CRLF + 
			CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + "; charset=utf-8" + CRLF + 
			CRLF + 
			content;	
		   
		return cpim;
	}
	
	/**
	 * Build a CPIM message with IMDN headers
	 * 
	 * @param from From URI
	 * @param to To URI
	 * @param messageId Message ID
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
	public static String buildCpimMessageWithImdn(String from, String to, String messageId, String content, String contentType) {
		String cpim =
			CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + CRLF + 
			CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + CRLF + 
			CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CRLF +
			ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + CRLF +
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CRLF + 
			ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": " + ImdnDocument.POSITIVE_DELIVERY + ", " + ImdnDocument.NEGATIVE_DELIVERY + ", " + ImdnDocument.DISPLAY + CRLF +
			CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + "; charset=utf-8" + CRLF +
			CpimMessage.HEADER_CONTENT_LENGTH + ": " + content.getBytes().length + CRLF + 
			CRLF + 
			content;	
		return cpim;
	}
	
	/**
	 * Build a CPIM delivery report
	 * 
	 * @param from From
	 * @param to To
	 * @param imdn IMDN report
	 * @return String
	 */
	public static String buildCpimDeliveryReport(String from, String to, String imdn) {
		String cpim =
			CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + CRLF + 
			CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + CRLF + 
			CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CRLF +
			ImdnUtils.HEADER_IMDN_MSG_ID + ": " + IdGenerator.getIdentifier() + CRLF +
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CRLF + 
			CpimMessage.HEADER_CONTENT_DISPOSITION + ": " + ImdnDocument.NOTIFICATION + CRLF +
			CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + ImdnDocument.MIME_TYPE + CRLF +
			CpimMessage.HEADER_CONTENT_LENGTH + ": " + imdn.getBytes().length + CRLF + 
			CRLF + 
			imdn;	
		   
		return cpim;
	}
	
	/**
	 * Parse a CPIM delivery report
	 * 
	 * @param cpim CPIM document
	 * @return IMDN document
	 */
	public static ImdnDocument parseCpimDeliveryReport(String cpim) {
		ImdnDocument imdn = null;
    	try {
    		// Parse CPIM document
    		CpimParser cpimParser = new CpimParser(cpim);
    		CpimMessage cpimMsg = cpimParser.getCpimMessage();
    		if (cpimMsg != null) {
    			// Check if the content is a IMDN message    		
    			String contentType = cpimMsg.getContentType();
    			if ((contentType != null) && ChatUtils.isMessageImdnType(contentType)) {
    				// Parse the IMDN document
    				imdn = parseDeliveryReport(cpimMsg.getMessageContent());
    			}
    		}
    	} catch(Exception e) {
    		imdn = null;
    	}		
		return imdn;
	}

    /** M: add server date for delivery status @{ */
    /**
     * Parse a date time for delivery report
     * 
     * @param cpim CPIM document
     * @return Date time
     */
    public static String parseDatetime(String cpim) {
        String date = null;
        try {
            CpimParser cpimParser = new CpimParser(cpim);
            CpimMessage cpimMsg = cpimParser.getCpimMessage();
            if (cpimMsg != null) {
                date = cpimMsg.getHeader(CpimMessage.HEADER_DATETIME);
            }
        } catch (Exception e) {
            e.printStackTrace();
            date = null;
        }
        return date;
    }

    /** @}*/

	/**
	 * Parse a delivery report
	 * 
	 * @param xml XML document
	 * @return IMDN document
	 */
	public static ImdnDocument parseDeliveryReport(String xml) {
		try {
			InputSource input = new InputSource(new ByteArrayInputStream(xml.getBytes()));
			ImdnParser parser = new ImdnParser(input);
			return parser.getImdnDocument();
    	} catch(Exception e) {
    		return null;
    	}		
	}

	/**
	 * Build a delivery report
	 * 
	 * @param msgId Message ID
	 * @param status Status
	 * @return XML document
	 */
	public static String buildDeliveryReport(String msgId, String status) {
		String method;
		if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			method = "display-notification";
		} else
		if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			method = "delivery-notification";
		} else {
			method = "processing-notification";
	    }
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + /*CRLF +*/
			"<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">" + /*CRLF +*/
	        " <message-id>" + msgId + "</message-id>" + /*CRLF +*/
	        " <datetime>" + DateUtils.encodeDate(System.currentTimeMillis()) + "</datetime>" + /*CRLF +*/
	        " <"+method+"><status><" + status + " /></status></"+method+">" +/* CRLF +*/
	        "</imdn>";
	}
	
	/**
	 * Create a first message
	 * 
	 * @param remote Remote contact
	 * @param txt Text message
	 * @param imdn IMDN flag
	 * @return First message
	 */
	public static InstantMessage createFirstMessage(String remote, String msg, boolean imdn) {
		if ((msg != null) && (msg.length() > 0)) {
			String msgId = ChatUtils.generateMessageId();
			return new InstantMessage(msgId,
					remote,
					StringUtils.encodeUTF8(msg),
					imdn,
					new Date());
		} else {
			return null;
		}	
	}
	
	/**
	 * Get the first message
	 * 
	 * @param invite Request
	 * @return First message
	 */
	public static InstantMessage getFirstMessage(SipRequest invite) {
		InstantMessage msg = getFirstMessageFromCpim(invite);
		if (msg != null) {
			return msg;
		} else {
			return getFirstMessageFromSubject(invite);
		}
	}

	public static String getSubject(SipRequest invite) {
		return invite.getSubject();
	}

	/**
	 * Get the first message from CPIM content
	 * 
	 * @param invite Request
	 * @return First message
	 */
	private static InstantMessage getFirstMessageFromCpim(SipRequest invite) {
		CpimMessage cpimMsg = ChatUtils.extractCpimMessage(invite);
		if (cpimMsg != null) {
			String remote = ChatUtils.getReferredIdentity(invite);
			String msgId = ChatUtils.getMessageId(invite);
			String txt = cpimMsg.getMessageContent();
            /**
             * M: Modified to create a InstantMessage object with server date. @{
             */
			Date date = cpimMsg.getMessageDate();
			if(date == null){
				date = new Date();
			}
			if ((remote != null) && (msgId != null) && (txt != null)) {
				return new InstantMessage(msgId,
						remote,
						StringUtils.decodeUTF8(txt),
						ChatUtils.isImdnDisplayedRequested(invite),
						date);
			} else {
				return null;
			}
			/**
			 * @}
			 */
		} else {
			return null;
		}
	}
	
	/**
	 * Get the first message from the Subject header
	 * 
	 * @param invite Request
	 * @return First message
	 */
	private static InstantMessage getFirstMessageFromSubject(SipRequest invite) {
		String subject = invite.getSubject();
		if ((subject != null) && (subject.length() > 0)) {
			String remote = ChatUtils.getReferredIdentity(invite);
			if ((remote != null) && (subject != null)) {
				return new InstantMessage(ChatUtils.generateMessageId(),
						remote,
			            /**
			             * M: Modified to decode the message with UTF-8. @{
			             */
						StringUtils.decodeUTF8ForGroupInvite(subject),
						/**
						 * @}
						 */
						ChatUtils.isImdnDisplayedRequested(invite),
						new Date());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}	
	
    /**
     * Extract CPIM message from incoming INVITE request 
     * 
     * @param request Request
     * @return Boolean
     */
    public static CpimMessage extractCpimMessage(SipRequest request) {
    	CpimMessage message = null;
		try {
			// Extract message from content/CPIM
		    String content = request.getContent();
		    String boundary = request.getBoundaryContentType();
			Multipart multi = new Multipart(content, boundary);
		    if (multi.isMultipart()) {
		    	String cpimPart = multi.getPart(CpimMessage.MIME_TYPE);
		    	if (cpimPart != null) {
					// CPIM part
	    			CpimParser cpimParser = new CpimParser(cpimPart.getBytes());
	    			message = cpimParser.getCpimMessage();
		    	}
		    }
		} catch(Exception e) {
			message = null;
		}
		return message;
    }
}
