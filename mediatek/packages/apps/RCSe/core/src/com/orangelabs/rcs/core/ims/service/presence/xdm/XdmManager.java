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

package com.orangelabs.rcs.core.ims.service.presence.xdm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.InputSource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.TerminalInfo;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.presence.directory.Folder;
import com.orangelabs.rcs.core.ims.service.presence.directory.XcapDirectoryParser;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.platform.network.SocketConnection;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.provisioning.https.HttpsProvisioningService;
import com.orangelabs.rcs.service.api.client.presence.PhotoIcon;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.HttpUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * XDM manager
 *
 * @author JM. Auffret
 */
public class XdmManager {
	/**
	 * M:Add for T-Mobile.@{T-Mobile
	 */
	private final static String HTTPS_STR = "https";
	private final static String HTTP_STR = "http";
	private final static String EMPTY_STR = "";
	private final static int URI_HAS_NO_EXPLICIT_PORT = -1;
	// https default port
	private final static int DEFAULT_HTTPS_PORT = 443;
	// https default port
	private final static int DEFAULT_HTTP_PORT = 80;
	/** T-Mobile@} */

	/**
	 * XDM server address
	 */
	private String xdmServerAddr;

	/**
	 * Managed documents
	 */
	private Hashtable<String, Folder> documents = new Hashtable<String, Folder>();
	
	/**
	 * M:Unknown value @{T-Mobile
	 */
	private static final String UNKNOWN = "unknown";
	/**
	 * T-Mobile@}
	 */

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * M:Add for T-Mobile.@{T-Mobile This class is used to retry HttpRequest if
	 * the transaction is fialed.
	 */
	private XcapRequestsRetryManager mRequestsRetryManager = XcapRequestsRetryManager
			.getInstance(this);

	/** T-Mobile@} */

	/**
	 * Constructor
	 * 
	 * @param parent IMS module
	 */
	public XdmManager(ImsModule parent) {
		xdmServerAddr = ImsModule.IMS_USER_PROFILE.getXdmServerAddr();
	}
	
	/**
	 * Send HTTP PUT request
	 * 
	 * @param request HTTP request
	 * @return HTTP response
	 * @throws CoreException
	 */
	private HttpResponse sendRequestToXDMS(HttpRequest request) throws CoreException {
		return sendRequestToXDMS(request, false);
	}
	
	/**
	 * Send HTTP PUT request
	 * 
	 * @param request HTTP request
	 * @param authenticate Authentication needed
	 * @return HTTP response
	 * @throws CoreException
	 */
	private HttpResponse sendRequestToXDMS(HttpRequest request, boolean authenticate) throws CoreException {
		try {
			/**
			 * M: Disable sending XCAP request if it has received a response
			 * with the status code 499.
			 * 
			 * @{T-Mobile
			 */
			if (RcsSettings.getInstance().isXCAPOperationBlocked()) {
				throw new CoreException(
						"Disable sending XCAP request because it has received a response with the status code 499.");

			}
			/**
			 * T-Mobile@}
			 */
			// Send first request
			HttpResponse response = sendHttpRequest(request, authenticate);
			
			// Analyze the response
			if (response.getResponseCode() == 401) {
				// 401 response received
				if (logger.isActivated()) {
					logger.debug("401 Unauthorized response received");
				}
	
				// Update the authentication agent
				request.getAuthenticationAgent().readWwwAuthenticateHeader(response.getHeader("www-authenticate"));
	
				// Set the cookie from the received response
				String cookie = response.getHeader("set-cookie");
				request.setCookie(cookie);
				
				// Send second request with authentification header
				response = sendRequestToXDMS(request, true);
			} else
			if (response.getResponseCode() == 412) {
				// 412 response received
				if (logger.isActivated()) {
					logger.debug("412 Precondition failed");
				}
	
				// Reset the etag
				documents.remove(request.getAUID());
				
				// Send second request with authentification header
				response = sendRequestToXDMS(request);
			} else {		
				/**
				 * M:Further processing for more response cases.@{T-Mobile
				 */
				handleXcapResponseOrResendRequest(response, request);
				/** T-Mobile@} */
			}
			return response;
		} catch(CoreException e) {
			throw e;
		} catch(Exception e) {
			throw new CoreException("Can't send HTTP request: " + e.getMessage());
		}
	}

	/**
	 * Send HTTP PUT request
	 * 
	 * @param request HTTP request
	 * @param authenticate Authentication needed
	 * @return HTTP response
	 * @throws IOException
	 * @throws CoreException
	 */
	private HttpResponse sendHttpRequest(HttpRequest request, boolean authenticate) throws IOException, CoreException {
		/**
		 * M: Modify the function,make it can accept both the 'http' and the
		 * 'https' connection.such as "https://xcap.sipthor.net:443/xcap-root",
		 * "https://xcap.sipthor.net/xcap-root",
		 * "http://xcap.sipthor.net:80/xcap-root",
		 * "http://xcap.sipthor.net/xcap-root".
		 * 
		 * @{T-Mobile
		 */
		// Extract host & port

		// indicate it is https or http connection
		boolean isHttpsConnection = true;
		// port of URL
		int port = 0;
		// host of URL
		String host = EMPTY_STR;
		// serviceRoot of URL
		String serviceRoot = EMPTY_STR;

		try {
			URI xdmServerURI = new URI(xdmServerAddr);
			if (xdmServerURI.getScheme().equals(HTTPS_STR)) {
				host = xdmServerURI.getHost();
				port = xdmServerURI.getPort();
				/**
				 * if port of xdmServerAddr is not exist,xdmServerURI.getPort()
				 * will return -1
				 */
				if (port == URI_HAS_NO_EXPLICIT_PORT) {
					port = DEFAULT_HTTPS_PORT;
				}
				serviceRoot = xdmServerURI.getPath();
			} else if (xdmServerURI.getScheme().equals(HTTP_STR)) {
				isHttpsConnection = false;
				host = xdmServerURI.getHost();
				port = xdmServerURI.getPort();
				if (port == URI_HAS_NO_EXPLICIT_PORT) {
					port = DEFAULT_HTTP_PORT;
				}
				serviceRoot = xdmServerURI.getPath();
			} else {
				if (logger.isActivated()) {
					logger
							.debug("xdmServerAddr is invalid,xdmServerAddr is neithor https nor http");
				}
				return null;
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		if (serviceRoot.equals(EMPTY_STR)) {
			if (logger.isActivated()) {
				logger
						.debug("xdmServerAddr is invalid,serviceRoot is not exist");
			}
			return null;
		}

		// Open connection with the XCAP server
		SocketConnection conn = null;
		if (isHttpsConnection) {
			conn = NetworkFactory.getFactory()
					.createSSLSocketClientConnection();
		} else {
			conn = NetworkFactory.getFactory().createSocketClientConnection();
		}
		/** T-Mobile@} */

		conn.open(host, port);
		InputStream is = conn.getInputStream();
		OutputStream os = conn.getOutputStream();

		// Create the HTTP request
		String requestUri = serviceRoot + request.getUrl();
		String httpRequest = request.getMethod() + " " + requestUri + " HTTP/1.1" + HttpUtils.CRLF +
				"Host: " + host + ":" + port + HttpUtils.CRLF +
				"User-Agent: " + TerminalInfo.getProductName() + " " + TerminalInfo.getProductVersion() + HttpUtils.CRLF;
		
		if (authenticate) {
			// Set the Authorization header
			String authorizationHeader = request.getAuthenticationAgent().generateAuthorizationHeader(
					request.getMethod(), requestUri, request.getContent());
			httpRequest += authorizationHeader + HttpUtils.CRLF;
		}
		
		String cookie = request.getCookie();
		if (cookie != null){
			// Set the cookie header
			httpRequest += "Cookie: " + cookie + HttpUtils.CRLF;
		}

		httpRequest += "X-3GPP-Intended-Identity: \"" + ImsModule.IMS_USER_PROFILE.getXdmServerLogin() + "\"" + HttpUtils.CRLF;

		/**
		 * M:Add XCAP-User-Agent header to request .@{T-Mobile
		 */
		httpRequest += "XCAP-User-Agent: " + getXCAPUserAgentFormat() + HttpUtils.CRLF;
		/**
		 * T-Mobile@}
		 */

		// Set the If-match header
		Folder folder = (Folder)documents.get(request.getAUID());
		if ((folder != null) && (folder.getEntry() != null) && (folder.getEntry().getEtag() != null)) {
			httpRequest += "If-match: \"" + folder.getEntry().getEtag() + "\"" + HttpUtils.CRLF;
		}
		
		if (request.getContent() != null) {
			// Set the content type
			httpRequest += "Content-type: " + request.getContentType() + HttpUtils.CRLF;
			httpRequest += "Content-Length:" + request.getContentLength() + HttpUtils.CRLF + HttpUtils.CRLF;
		} else {
			httpRequest += "Content-Length: 0" + HttpUtils.CRLF + HttpUtils.CRLF;
		}
		
		// Write HTTP request headers
		os.write(httpRequest.getBytes());
		os.flush();

		// Write HTTP content
		if (request.getContent() != null) {
			os.write(request.getContent().getBytes("UTF-8"));
			os.flush();
		}

		if (logger.isActivated()){
			if (request.getContent() != null) {
				logger.debug("Send HTTP request:\n" + httpRequest + request.getContent());
			} else {
				logger.debug("Send HTTP request:\n" + httpRequest);
			}
		}

		// Read HTTP headers response
		StringBuffer respTrace = new StringBuffer();
		HttpResponse response = new HttpResponse();
		int ch = -1;
		String line = "";
		while((ch = is.read()) != -1) {
			line += (char)ch;
			
			if (line.endsWith(HttpUtils.CRLF)) {
				if (line.equals(HttpUtils.CRLF)) {
					// All headers has been read
					break;
				}

				if (logger.isActivated()) {
					respTrace.append(line);
				}

				// Remove CRLF
				line = line.substring(0, line.length()-2);
				
				if (line.startsWith("HTTP/")) {
					// Status line
					response.setStatusLine(line);
				} else {
					// Header
					int index = line.indexOf(":");
					String name = line.substring(0, index).trim().toLowerCase();
					String value = line.substring(index+1).trim();
					response.addHeader(name, value);
				}
				
				line = "";
			}
		}
		
		// Extract content length
		int length = -1;
		try {
			String value = response.getHeader("content-Length");
			length = Integer.parseInt(value);
		} catch(Exception e) {
			length = -1;
		}
		
		// Read HTTP content response
		if (length > 0) {
			byte[] content = new byte[length];
			int nb = -1;
			int pos = 0;
			byte[] buffer = new byte[1024];
			while((nb = is.read(buffer)) != -1) {
				System.arraycopy(buffer, 0, content, pos, nb);
				pos += nb;
				
				if (pos >= length) {
					// End of content
					break;
				}
			}
			if (logger.isActivated()) {
				respTrace.append(HttpUtils.CRLF + new String(content));
			}
			response.setContent(content);
		}

		if (logger.isActivated()){
			logger.debug("Receive HTTP response:\n" + respTrace.toString());
		}

		// Close the connection
		is.close();
		os.close();
		conn.close();

		// Save the Etag from the received response
		String etag = response.getHeader("etag");
		if ((etag != null) && (folder != null) && (folder.getEntry() != null)) {
			folder.getEntry().setEtag(etag);
		}
		
		return response;
	}	
	
	/**
	 * Initialize the XDM interface
	 */
	public void initialize() {
    	// Get the existing XCAP documents on the XDM server
		try {
			HttpResponse response = getXcapDocuments();
			if ((response != null) && response.isSuccessfullResponse()) {
				// Analyze the XCAP directory
				InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
				XcapDirectoryParser parser = new XcapDirectoryParser(input);
				documents = parser.getDocuments();

				// Check RCS list document
				Folder folder = (Folder)documents.get("rls-services");
				if ((folder == null) || (folder.getEntry() == null)) {
					if (logger.isActivated()){
						logger.debug("The rls-services document does not exist");
					}

					// Set RCS list document
			    	setRcsList();
				} else {
					if (logger.isActivated()){
						logger.debug("The rls-services document already exists");
					}
				}

				// Check resource list document
				folder = (Folder)documents.get("resource-lists");
				if ((folder == null) || (folder.getEntry() == null)) {
					if (logger.isActivated()){
						logger.debug("The resource-lists document does not exist");
					}

					// Set resource list document
			    	setResourcesList(); 
				} else {
					if (logger.isActivated()){
						logger.debug("The resource-lists document already exists");
					}
				}
			
				// Check presence rules document
				folder = (Folder)documents.get("org.openmobilealliance.pres-rules");
				if ((folder == null) || (folder.getEntry() == null)) {
					if (logger.isActivated()){
						logger.debug("The org.openmobilealliance.pres-rules document does not exist");
					}

					// Set presence rules document
			    	setPresenceRules();
				} else {
					if (logger.isActivated()){
						logger.debug("The org.openmobilealliance.pres-rules document already exists");
					}
				}
			}
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Can't parse the XCAP directory document", e);
        	}	    				
		}
	}

	/**
	 * Get XCAP managed documents
	 * 
	 * @return Response
	 */
	public HttpResponse getXcapDocuments() {
		try {
			if (logger.isActivated()){
				logger.info("Get XCAP documents");
			}
	
			// URL
			String url = "/org.openmobilealliance.xcap-directory/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/directory.xml";
		
			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("XCAP documents has been read with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't read XCAP documents: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't read XCAP documents: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Get RCS list
	 * 
	 * @return Response
	 */
	public HttpResponse getRcsList() {
		try {
			if (logger.isActivated()){
				logger.info("Get RCS list");
			}
	
			// URL
			String url = "/rls-services/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";
		
			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("RCS list has been read with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't read RCS list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't read RCS list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Set RCS list
	 * 
	 * @return Response
	 */
	public HttpResponse setRcsList() {
		try {
			if (logger.isActivated()){
				logger.info("Set RCS list");
			}
	
			// URL
			String url = "/rls-services/users/" + 
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";
		
			// Content
			String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
			String resList = xdmServerAddr + "/resource-lists/users/" + HttpUtils.encodeURL(user) + "/index/~~/resource-lists/list%5B@name=%22rcs%22%5D";
			String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + HttpUtils.CRLF +
				"<rls-services xmlns=\"urn:ietf:params:xml:ns:rls-services\" xmlns:rl=\"urn:ietf:params:xml:ns:resource-lists\">" + HttpUtils.CRLF +
				"<service uri=\"" + user + ";pres-list=rcs\">" + HttpUtils.CRLF +
				
				"<resource-list>" + resList + "</resource-list>" + HttpUtils.CRLF +
				
				"<packages>" + HttpUtils.CRLF +
				" <package>presence</package>" + HttpUtils.CRLF +
				"</packages>" + HttpUtils.CRLF +
				
				"</service></rls-services>";
	
			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/rls-services+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("RCS list has been set with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't set RCS list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't set RCS list: unexpected exception", e);
			}
			return null;
		}
	}
	
	/**
	 * Get resources list
	 * 
	 * @return Response
	 */
	public HttpResponse getResourcesList() {
		try {
			if (logger.isActivated()){
				logger.info("Get resources list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";
		
			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Resources list has been read with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't read resources list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't read resources list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Set resources list
	 * 
	 * @return Response
	 */
	public HttpResponse setResourcesList() {
		try {
			if (logger.isActivated()){
				logger.info("Set resources list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/index";
		
			// Content
			String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
			String resList = xdmServerAddr + "/resource-lists/users/" + HttpUtils.encodeURL(user) + "/index/~~/resource-lists/list%5B";
			String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + HttpUtils.CRLF +
				"<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\">" + HttpUtils.CRLF +
				
				"<list name=\"oma_buddylist\">" + HttpUtils.CRLF +
				" <external anchor=\"" + resList + "@name=%22rcs%22%5D\"/>" + HttpUtils.CRLF +
				"</list>" + HttpUtils.CRLF +
				
				"<list name=\"oma_grantedcontacts\">" + HttpUtils.CRLF +
				" <external anchor=\"" + resList + "@name=%22rcs%22%5D\"/>" + HttpUtils.CRLF +
				"</list>" + HttpUtils.CRLF +

				/**
				 * M:Add to map the designated lists to prepare authorized
				 * lists.@{T-Mobile
				 */
				"<list name=\"oma_politeblockedcontacts\">" + HttpUtils.CRLF +
				" <external anchor=\"" + resList + "@name=%22rcs_politeblockedcontacts%22%5D\"/>" + HttpUtils.CRLF +
				"</list>" + HttpUtils.CRLF +
				/**
				 * T-Mobile@}
				 */
				
				"<list name=\"oma_blockedcontacts\">" + HttpUtils.CRLF +
				" <external anchor=\"" + resList + "@name=%22rcs_blockedcontacts%22%5D\"/>" + HttpUtils.CRLF +
				" <external anchor=\"" + resList + "@name=%22rcs_revokedcontacts%22%5D\"/>" + HttpUtils.CRLF +
				"</list>" + HttpUtils.CRLF +
				
				"<list name=\"rcs\">" + HttpUtils.CRLF +
				" <display-name>My presence buddies</display-name>" + HttpUtils.CRLF +
				"</list>" + HttpUtils.CRLF +

				/**
				 * M:Add polite blocked contacts list.@{T-Mobile
				 */
				"<list name=\"rcs_politeblockedcontacts\">" + HttpUtils.CRLF +
				" <display-name>My polite blocked contacts</display-name>" + HttpUtils.CRLF +
				"</list>" + HttpUtils.CRLF +
				/**
				 * T-Mobile@}
				 */

				"<list name=\"rcs_blockedcontacts\">" + HttpUtils.CRLF +
				" <display-name>My blocked contacts</display-name>" + HttpUtils.CRLF +
				"</list>" + HttpUtils.CRLF +
				
				"<list name=\"rcs_revokedcontacts\">" + HttpUtils.CRLF +
				" <display-name>My revoked contacts</display-name>" + HttpUtils.CRLF +
				"</list>" + HttpUtils.CRLF +
				
				"</resource-lists>";
			
			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/resource-lists+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Resources list has been set with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't set resources list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't set resources list: unexpected exception", e);
			}
			return null;
		}
	}
	
	/**
	 * M:Remove resources list from XDM server.@{T-Mobile
	 */
	public HttpResponse removeResourcesList() {
		String userPublicURI = ImsModule.IMS_USER_PROFILE.getPublicUri();
		try {
			logger.info("Remove ResourcesList");
			// URL
			String url = "/resource-lists/users/"
					+ HttpUtils.encodeURL(userPublicURI) + "/index";
			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				logger.info(userPublicURI
						+ "'s resources list has been removed with success");
			} else {
				logger.info("Can't remove " + userPublicURI
						+ " 's resources list: " + response.getResponseCode()
						+ " error");
			}
			return response;
		} catch (CoreException e) {
			logger.error("Can't remove " + userPublicURI
					+ " 's resources list: unexpected exception", e);
			return null;
		}
	}

	/**
	 * T-Mobile@}
	 */

	/**
	 * Get presence rules
	 * 
	 * @return Response
	 */
	public HttpResponse getPresenceRules() {
		try {
			if (logger.isActivated()){
				logger.info("Get presence rules");
			}
	
			// URL
			String url = "/org.openmobilealliance.pres-rules/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/pres-rules";
		
			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Get presence rules has been requested with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't get the presence rules: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't get the presence rules: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Set presence rules
	 * 
	 * @return Response
	 */
	public HttpResponse setPresenceRules() {
		try {
			if (logger.isActivated()){
				logger.info("Set presence rules");
			}
	
			// URL
			String url = "/org.openmobilealliance.pres-rules/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + "/pres-rules";
		
			// Content
			String user = ImsModule.IMS_USER_PROFILE.getPublicUri();
			String blockedList = xdmServerAddr + "/resource-lists/users/" + user + "/index/~~/resource-lists/list%5B@name=%22oma_blockedcontacts%22%5D";
			String grantedList = xdmServerAddr + "/resource-lists/users/" + user + "/index/~~/resource-lists/list%5B@name=%22oma_grantedcontacts%22%5D";

			/**
			 * M:Get the polite blocked list index.@{T-Mobile
			 */
			String politeBlockedList =xdmServerAddr + "/resource-lists/users/" + user + "/index/~~/resource-lists/list%5B@name=%22oma_politeblockedcontacts%22%5D";
			/**
			 * @}
			 */

			String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + HttpUtils.CRLF +
				"<cr:ruleset xmlns:ocp=\"urn:oma:xml:xdm:common-policy\" xmlns:pr=\"urn:ietf:params:xml:ns:pres-rules\" xmlns:cr=\"urn:ietf:params:xml:ns:common-policy\">" + HttpUtils.CRLF +
				
				"<cr:rule id=\"wp_prs_allow_own\">" + HttpUtils.CRLF +
				" <cr:conditions>" + HttpUtils.CRLF +
				"  <cr:identity><cr:one id=\"" + ImsModule.IMS_USER_PROFILE.getPublicUri() + "\"/></cr:identity>" + HttpUtils.CRLF +
				" </cr:conditions>" + HttpUtils.CRLF +
				" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>" + HttpUtils.CRLF +
				" <cr:transformations>" + HttpUtils.CRLF +
				"  <pr:provide-services><pr:all-services/></pr:provide-services>" + HttpUtils.CRLF +
				"  <pr:provide-persons><pr:all-persons/></pr:provide-persons>" + HttpUtils.CRLF +
				"  <pr:provide-devices><pr:all-devices/></pr:provide-devices>" + HttpUtils.CRLF +
				"  <pr:provide-all-attributes/>" + HttpUtils.CRLF +
				" </cr:transformations>" + HttpUtils.CRLF +
				"</cr:rule>" + HttpUtils.CRLF +
				
				"<cr:rule id=\"rcs_allow_services_anonymous\">" + HttpUtils.CRLF +
				" <cr:conditions><ocp:anonymous-request/></cr:conditions>" + HttpUtils.CRLF +
				" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>" + HttpUtils.CRLF +
				" <cr:transformations>" + HttpUtils.CRLF +
				"  <pr:provide-services><pr:all-services/></pr:provide-services>" + HttpUtils.CRLF +
				"  <pr:provide-all-attributes/>" + HttpUtils.CRLF +
				" </cr:transformations>" + HttpUtils.CRLF +
				"</cr:rule>" + HttpUtils.CRLF +
				
				"<cr:rule id=\"wp_prs_unlisted\">" + HttpUtils.CRLF +
				" <cr:conditions><ocp:other-identity/></cr:conditions>" + HttpUtils.CRLF +
				" <cr:actions><pr:sub-handling>confirm</pr:sub-handling></cr:actions>" + HttpUtils.CRLF +
				"</cr:rule>" + HttpUtils.CRLF +
				
				"<cr:rule id=\"wp_prs_grantedcontacts\">" + HttpUtils.CRLF +
				" <cr:conditions>" + HttpUtils.CRLF +
				" <ocp:external-list>" + HttpUtils.CRLF +
				"  <ocp:entry anc=\"" + grantedList + "\"/>" + HttpUtils.CRLF +
				" </ocp:external-list>" + HttpUtils.CRLF +
				" </cr:conditions>" + HttpUtils.CRLF +
				" <cr:actions><pr:sub-handling>allow</pr:sub-handling></cr:actions>" + HttpUtils.CRLF +
				" <cr:transformations>" + HttpUtils.CRLF +
				"   <pr:provide-services><pr:all-services/></pr:provide-services>" + HttpUtils.CRLF +
				"   <pr:provide-persons><pr:all-persons/></pr:provide-persons>" + HttpUtils.CRLF +
				"   <pr:provide-devices><pr:all-devices/></pr:provide-devices>" + HttpUtils.CRLF +
				"   <pr:provide-all-attributes/>" + HttpUtils.CRLF +
				" </cr:transformations>" + HttpUtils.CRLF +
				"</cr:rule>" + HttpUtils.CRLF +

				/**
				 * M:Add to set polite blocked list permission to
				 * "polite-block".@{T-Mobile
				 */
				"<cr:rule id=\"wp_prs_politeblockedcontacts\">" + HttpUtils.CRLF +
				" <cr:conditions>" + HttpUtils.CRLF +
				"  <ocp:external-list>" + HttpUtils.CRLF + 
				"  <ocp:entry anc=\"" + politeBlockedList + "\"/>" + HttpUtils.CRLF +
				" </ocp:external-list>" + HttpUtils.CRLF +
				" </cr:conditions>" + HttpUtils.CRLF +
				" <cr:actions><pr:sub-handling>polite-block</pr:sub-handling></cr:actions>" + HttpUtils.CRLF +
				"</cr:rule>" + HttpUtils.CRLF +
				/**
				 * T-Mobile@}
				 */

				"<cr:rule id=\"wp_prs_blockedcontacts\">" + HttpUtils.CRLF +
				" <cr:conditions>" + HttpUtils.CRLF +
				"  <ocp:external-list>" + HttpUtils.CRLF + 
				"  <ocp:entry anc=\"" + blockedList + "\"/>" + HttpUtils.CRLF +
				" </ocp:external-list>" + HttpUtils.CRLF +
				" </cr:conditions>" + HttpUtils.CRLF +
				" <cr:actions><pr:sub-handling>block</pr:sub-handling></cr:actions>" + HttpUtils.CRLF +
				"</cr:rule>" + HttpUtils.CRLF +
				"</cr:ruleset>";
			
			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/auth-policy+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Presence rules has been set with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't set presence rules: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't set presence rules: unexpected exception", e);
			}
			return null;
		}
	}
	
	/**
	 * Add a contact to the granted contacts list
	 * 
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse addContactToGrantedList(String contact) {
		try {
			if (logger.isActivated()){
				logger.info("Add " + contact + " to granted list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(contact) + "%22%5D";
			
			// Content
			String content = "<entry uri='" + contact + "'></entry>";
			
			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been added with success to granted list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't add " + contact + " to granted list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't add " + contact + " to granted list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Remove a contact from the granted contacts list
	 * 
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse removeContactFromGrantedList(String contact) {
		try {
			if (logger.isActivated()){
				logger.info("Remove " + contact + " from granted list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(contact) + "%22%5D";
			
			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been removed with success from granted list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't remove " + contact + " from granted list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't remove " + contact + " from granted list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Returns the list of granted contacts
	 * 
	 * @return List
	 */
	public List<String> getGrantedContacts() {
		List<String> result = new ArrayList<String>();
		try {
			if (logger.isActivated()){
				logger.info("Get granted contacts list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs%22%5D";
			
			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);
			
			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Granted contacts list has been read with success");
				}

				// Parse response
				InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
				XcapResponseParser parser = new XcapResponseParser(input);
				result = parser.getUris();
			} else {
				if (logger.isActivated()){
					logger.info("Can't get granted contacts list: " + response.getResponseCode() + " error");
				}
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get granted contacts list: unexpected exception", e);
			}
		}
		return result;
	}

	/**
	 * Add a contact to the blocked contacts list
	 * 
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse addContactToBlockedList(String contact) {
		try {
			if (logger.isActivated()){
				logger.info("Add " + contact + " to blocked list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(contact) + "%22%5D";
			
			// Content
			String content = "<entry uri='" + contact + "'></entry>";
			
			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been added with success to blocked list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't add " + contact + " to granted list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't add " + contact + " to blocked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Remove a contact from the blocked contacts list
	 * 
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse removeContactFromBlockedList(String contact) {
		try {
			if (logger.isActivated()){
				logger.info("Remove " + contact + " from blocked list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(contact) + "%22%5D";
			
			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been removed with success from blocked list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't remove " + contact + " from blocked list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't remove " + contact + " from blocked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Returns the list of blocked contacts
	 * 
	 * @return List
	 */
	public List<String> getBlockedContacts() {
		List<String> result = new ArrayList<String>();
		try {
			if (logger.isActivated()){
				logger.info("Get blocked contacts list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs_blockedcontacts%22%5D";
			
			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Blocked contacts list has been read with success");
				}
				
				// Parse response
				InputSource input = new InputSource(new ByteArrayInputStream(response.getContent()));
				XcapResponseParser parser = new XcapResponseParser(input);
				result = parser.getUris();
			} else {
				if (logger.isActivated()){
					logger.info("Can't get blocked contacts list: " + response.getResponseCode() + " error");
				}
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get blocked contacts list: unexpected exception", e);
			}
		}
		return result;
	}

	/**
	 * Add a contact to the revoked contacts list
	 * 
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse addContactToRevokedList(String contact) {
		try {
			if (logger.isActivated()){
				logger.info("Add " + contact + " to revoked list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(contact) + "%22%5D";
			
			// Content
			String content = "<entry uri='" + contact + "'></entry>";
			
			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/xcap-el+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been added with success to revoked list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't add " + contact + " to revoked list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't add " + contact + " to revoked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Remove a contact from the revoked contacts list
	 * 
	 * @param contact Contact
	 * @return Response
	 */
	public HttpResponse removeContactFromRevokedList(String contact) {
		try {
			if (logger.isActivated()){
				logger.info("Remove " + contact + " from revoked list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D/entry%5B@uri=%22" +
					HttpUtils.encodeURL(contact) + "%22%5D";
			
			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info(contact + " has been removed with success from revoked list");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't remove " + contact + " from revoked list: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't remove " + contact + " from revoked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Returns the list of revoked contacts
	 * 
	 * @return List
	 */
	public List<String> getRevokedContacts() {
		List<String> result = new ArrayList<String>();
		try {
			if (logger.isActivated()){
				logger.info("Get revoked contacts list");
			}
	
			// URL
			String url = "/resource-lists/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
					"/index/~~/resource-lists/list%5B@name=%22rcs_revokedcontacts%22%5D";
			
			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Revoked contacts list has been read with success");
				}

				// Parse response
				InputSource input = new InputSource(
						new ByteArrayInputStream(response.getContent()));
				XcapResponseParser parser = new XcapResponseParser(input);
				result = parser.getUris(); 
			} else {
				if (logger.isActivated()){
					logger.info("Can't get revoked contacts list: " + response.getResponseCode() + " error");
				}
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get revoked contacts list: unexpected exception", e);
			}
		}
		return result;
	}

	/**
	 * Returns the photo icon URL
	 * 
	 * @return URL
	 */
	public String getEndUserPhotoIconUrl() {
		return xdmServerAddr + "/org.openmobilealliance.pres-content/users/" +
			HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
			"/oma_status-icon/rcs_status_icon";
	}
	
	/**
	 * Upload the end user photo
	 * 
	 * @param photo Photo icon
	 * @return Response
	 */
	public HttpResponse uploadEndUserPhoto(PhotoIcon photo) {
		try {
			if (logger.isActivated()){
				logger.info("Upload the end user photo");
			}
	
			// Content
			String data = Base64.encodeBase64ToString(photo.getContent());
			String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + HttpUtils.CRLF +
				"<content xmlns=\"urn:oma:xml:prs:pres-content\">" + HttpUtils.CRLF +
				"<mime-type>" + photo.getType() + "</mime-type>" + HttpUtils.CRLF + 
				"<encoding>base64</encoding>" + HttpUtils.CRLF +
				"<data>" + data + "</data>" + HttpUtils.CRLF +
				"</content>";
			
			// URL
			String url = "/org.openmobilealliance.pres-content/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
				"/oma_status-icon/rcs_status_icon";

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content, "application/vnd.oma.pres-content+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Photo has been uploaded with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't upload the photo: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't upload the photo: unexpected exception", e);
			}
			return null;
		}		
	}

	/**
	 * Delete the end user photo
	 * 
	 * @param photo Photo icon
	 * @return Response
	 */
	public HttpResponse deleteEndUserPhoto() {
		try {
			if (logger.isActivated()){
				logger.info("Delete the end user photo");
			}
	
			// URL
			String url = "/org.openmobilealliance.pres-content/users/" +
				HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) + 
				"/oma_status-icon/rcs_status_icon";

			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Photo has been deleted with success");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't delete the photo: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't delete the photo: unexpected exception", e);
			}
			return null;
		}		
	}
	
	/**
	 * Download photo of a remote contact
	 *
	 * @param url URL of the photo to be downloaded
	 * @param etag Etag of the photo
	 * @return Icon data as photo icon
	 */
	public PhotoIcon downloadContactPhoto(String url, String etag) {
		try {
			if (logger.isActivated()){
				logger.info("Download the photo at " + url);
			}
			
			// Remove the beginning of the URL
			url = url.substring(url.indexOf("/org.openmobilealliance.pres-content"));						
			
			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Download photo with success");
				}

				// Parse response
				InputSource input = new InputSource(
						new ByteArrayInputStream(response.getContent()));
				XcapPhotoIconResponseParser parser = new XcapPhotoIconResponseParser(input);
				
				// Return data
				byte[] data = parser.getData(); 
				if (data != null) {
					if (logger.isActivated()){
						logger.debug("Received photo: encoding=" + parser.getEncoding() + ", mime=" + parser.getMime() + ", encoded size=" + data.length);
					}
					byte[] dataArray = Base64.decodeBase64(data);
					
	    			// Create a bitmap from the received photo data
	    			Bitmap bitmap = BitmapFactory.decodeByteArray(dataArray, 0, dataArray.length);
	    			if (bitmap != null) {	
	    				if (logger.isActivated()){
	    					logger.debug("Photo width="+bitmap.getWidth() + " height="+bitmap.getHeight());
	    				}
	    				
						return new PhotoIcon(dataArray, bitmap.getWidth(), bitmap.getHeight(), etag);
	    			}else{
	    				return null;
	    			}
				} else {
					if (logger.isActivated()){
						logger.warn("Can't download the photo: photo is null");
					}
					return null;
				}
			} else {
				if (logger.isActivated()){
					logger.warn("Can't download the photo: " + response.getResponseCode() + " error");
				}
				return null;
			}
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't download the photo: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Set presence info
	 * 
	 * @param info Presence info
	 * @return Response
	 */
	public HttpResponse setPresenceInfo(String info) {
		try {
			if (logger.isActivated()){
				logger.info("Update presence info");
			}
	
			// URL
			String url = "/pidf-manipulation/users/" +
					HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE.getPublicUri()) +
					"/perm-presence";
			
			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, info, "application/pidf+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()){
					logger.info("Presence info has been updated with succes");
				}
			} else {
				if (logger.isActivated()){
					logger.info("Can't update the presence info: " + response.getResponseCode() + " error");
				}
			}
			return response;
		} catch(CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't update the presence info: unexpected exception", e);
			}
			return null;
		}
	}
    
	/**
	 * M:Add for T-Mobile.@{T-Mobile
	 */
	/**
	 * Add a contact to the polite blocked contacts list
	 * 
	 * @param contact
	 *            The target contact to deal with
	 * @return Returns the processing response
	 */
	public HttpResponse addContactToPoliteBlockedList(String contact) {
		try {
			if (logger.isActivated()) {
				logger.info("Add " + contact + " to polite blocked list");
			}

			// URL
			String url = "/resource-lists/users/"
					+ HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE
							.getPublicUri())
					+ "/index/~~/resource-lists/list%5B@name=%22rcs_politeblockedcontacts%22%5D/entry%5B@uri=%22"
					+ HttpUtils.encodeURL(contact) + "%22%5D";

			// Content
			String content = "<entry uri='" + contact + "'></entry>";

			// Create the request
			HttpPutRequest request = new HttpPutRequest(url, content,
					"application/xcap-el+xml");

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()) {
					logger.info(contact
									+ " has been added with success to polite blocked list");
				}
			} else {
				if (logger.isActivated()) {
					logger.info("Can't add " + contact
							+ " to polite blocked list: "
							+ response.getResponseCode() + " error");
				}
			}
			return response;
		} catch (CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't add " + contact
						+ " to polite blocked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Remove a contact from the polite blocked contacts list
	 * 
	 * @param contact
	 *            The target contact to deal with
	 * @return Returns the processing response
	 */
	public HttpResponse removeContactFromPoliteBlockedList(String contact) {
		try {
			if (logger.isActivated()) {
				logger.info("Remove " + contact + " from polite blocked list");
			}

			// URL
			String url = "/resource-lists/users/"
					+ HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE
							.getPublicUri())
					+ "/index/~~/resource-lists/list%5B@name=%22rcs_politeblockedcontacts%22%5D/entry%5B@uri=%22"
					+ HttpUtils.encodeURL(contact) + "%22%5D";

			// Create the request
			HttpDeleteRequest request = new HttpDeleteRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()) {
					logger.info(contact
									+ " has been removed with success from polite blocked list");
				}
			} else {
				if (logger.isActivated()) {
					logger.info("Can't remove " + contact
							+ " from polite blocked list: "
							+ response.getResponseCode() + " error");
				}
			}
			return response;
		} catch (CoreException e) {
			if (logger.isActivated()) {
				logger.error("Can't remove " + contact
						+ " from polite blocked list: unexpected exception", e);
			}
			return null;
		}
	}

	/**
	 * Returns the list of polite blocked contacts
	 * 
	 * @return Returns the list of polite blocked contacts
	 */
	public List<String> getPoliteBlockedContacts() {
		List<String> result = new ArrayList<String>();
		try {
			if (logger.isActivated()) {
				logger.info("Get polite blocked contacts list");
			}

			// URL
			String url = "/resource-lists/users/"
					+ HttpUtils.encodeURL(ImsModule.IMS_USER_PROFILE
							.getPublicUri())
					+ "/index/~~/resource-lists/list%5B@name=%22rcs_politeblockedcontacts%22%5D";

			// Create the request
			HttpGetRequest request = new HttpGetRequest(url);

			// Send the request
			HttpResponse response = sendRequestToXDMS(request);
			if (response.isSuccessfullResponse()) {
				if (logger.isActivated()) {
					logger.info("Polite blocked contacts list has been read with success");
				}

				// Parse response
				InputSource input = new InputSource(new ByteArrayInputStream(
						response.getContent()));
				XcapResponseParser parser = new XcapResponseParser(input);
				result = parser.getUris();
			} else {
				if (logger.isActivated()) {
					logger.info("Can't get polite blocked contacts list: "
							+ response.getResponseCode() + " error");
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get polite blocked contacts list: unexpected exception", e);
			}
		}
		return result;
	}

	/**
	 * Send request to XDMS
	 * 
	 * @return The response to related request
	 * @throws CoreException
	 */
	public HttpResponse sendHttpRequestToXDMS(HttpRequest request)
			throws CoreException {
		return sendRequestToXDMS(request);
	}

	/**
	 * Handle other XCAP responses.
	 * 
	 * @param httpResponse
	 *            XCAP response got from XDMS
	 * @param httpRequest
	 *            XCAP request that to be resent if transaction is failed
	 */
	public void handleXcapResponseOrResendRequest(HttpResponse httpResponse,
			HttpRequest httpRequest) {
		int responseCode = httpResponse.getResponseCode();
		if (responseCode == 499) {
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					RcsSettings.getInstance().writeParameter(
							RcsSettingsData.BLOCK_XCAP_OPERATION,
							RcsSettingsData.TRUE);
					return null;
				}
			}.execute();
		} else if ((responseCode >= 101 && responseCode <= 203)
				|| responseCode == 207 || responseCode == 402
				|| (responseCode >= 418 && responseCode <= 450)
				|| responseCode == 501
				|| (responseCode >= 507 && responseCode <= 510)) {
			// There is no need to do further processing.
			if (logger.isActivated()) {
				logger.debug(httpResponse.getResponseCode()
						+ " response received");
			}
		} else {
			// Resends related request.
			mRequestsRetryManager.setCurrentRequestAndResponse(httpRequest,
					httpResponse);
		}
	}

	/**
	 * Send XCAP update command request to XDMS
	 * 
	 * @param updateCommands
	 * @return The response to related request
	 */
	public HttpResponse aggregateXCAPUpdateCommands(
			LinkedList<String> updateCommands) {
		// TODO Create an aggregation XCAP update commands HTTP request.
		return null;
	}

	/**
	 * T-Mobile@}
	 */

	/**
	 * M:Add get XCAP-User-Agent value function .@{T-Mobile
	 */
	private String getXCAPUserAgentFormat() {
		// format: <make>.<model>.major.minor[.build[.version]]
		// example: Samsung.T959.4.2.9.5
		// use "adb shell getprop" can show the properties key and value
		String result = UNKNOWN;
		String make = HttpsProvisioningService
				.getSystemProperties("ro.product.model");
		String model = HttpsProvisioningService
				.getSystemProperties("ro.product.name");
		String version = HttpsProvisioningService
				.getSystemProperties("ro.mediatek.version.release");
		if (!TextUtils.isEmpty(make) && !TextUtils.isEmpty(model)
				&& !TextUtils.isEmpty(version)) {
			result = make + "." + model + "." + version;
		}
		if (logger.isActivated()) {
			logger.info("XCAP-User-Agent Format is:" + result);
		}
		return result;
	}
	/**
	 * T-Mobile@}
	 */
}
