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
package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.KeyStoreManager;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating file transfer session
 * 
 * @author jexa7410
 */
public class OriginatingFileSharingSession extends FileSharingSession implements MsrpEventListener {
	/**
	 * MSRP manager
	 */
	private MsrpManager msrpMgr = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 */
	public OriginatingFileSharingSession(ImsService parent, MmContent content, String contact) {
		super(parent, content, contact);
		
		// Create dialog path
		createOriginatingDialogPath();
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a file transfer session as originating");
	    	}
            /**
             * M: Modified to resolve the 403 error issue.@{
             */
            SipRequest invite = createSipInvite();
            /**
             * @}
             */

            // Send INVITE request
            sendInvite(invite);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }

        if (logger.isActivated()) {
            logger.debug("End of thread");
        }
    }

    /**
     * M: Modified to resolve the 403 error issue.@{
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
        // Set setup mode
        String localSetup = createSetupOffer();
        if (logger.isActivated()) {
            logger.debug("Local setup attribute is " + localSetup);
        }

        // Set local port
        int localMsrpPort = 9; // See RFC4145, Page 4

        // Create the MSRP manager
        String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                .getNetworkAccess().getIpAddress();
        msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);

        // Build SDP part
        String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
        String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
        /** M: add for MSRPoTLS @{ */
        String protocol = getCurrentProtocol();
        String sdp = null;
        if (PROTOCOL_TLS.equals(protocol)) {
            sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " IN IP4 " + ipAddress
                    + SipUtils.CRLF + "s=-" + SipUtils.CRLF + "c=IN IP4 " + ipAddress
                    + SipUtils.CRLF + "t=0 0" + SipUtils.CRLF + "m=message " + localMsrpPort
                    + " TCP/TLS/MSRP *" + SipUtils.CRLF + "a=path:" + msrpMgr.getLocalMsrpsPath()
                    + SipUtils.CRLF + "a=fingerprint:" + KeyStoreManager.getFingerPrint()
                    + SipUtils.CRLF + "a=setup:" + localSetup + SipUtils.CRLF + "a=accept-types: "
                    + getContent().getEncoding() + SipUtils.CRLF
                    + "a=file-transfer-id:"
                    + getFileTransferId() + SipUtils.CRLF + "a=file-disposition:attachment"
                    + SipUtils.CRLF + "a=sendonly" + SipUtils.CRLF;
        } else {
            sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " IN IP4 " + ipAddress
                    + SipUtils.CRLF + "s=-" + SipUtils.CRLF + "c=IN IP4 " + ipAddress
                    + SipUtils.CRLF + "t=0 0" + SipUtils.CRLF + "m=message " + localMsrpPort
                    + " TCP/MSRP *" + SipUtils.CRLF + "a=path:" + msrpMgr.getLocalMsrpPath()
                    + SipUtils.CRLF + "a=setup:" + localSetup + SipUtils.CRLF + "a=accept-types: "
                    + getContent().getEncoding() + SipUtils.CRLF
                    + "a=file-transfer-id:"
                    + getFileTransferId() + SipUtils.CRLF + "a=file-disposition:attachment"
                    + SipUtils.CRLF + "a=sendonly" + SipUtils.CRLF;
        }
	    	int maxSize = FileSharingSession.getMaxFileSharingSize();
	    	if (maxSize > 0) {
	    		sdp += "a=max-size:" + maxSize + SipUtils.CRLF;
	    	}

        /** @} */
        // Set File-selector attribute
        String selector = getFileSelectorAttribute();
        if (selector != null) {
            sdp += "a=file-selector:" + selector + SipUtils.CRLF;
        }

        // Set File-location attribute
        String location = getFileLocationAttribute();
        if (location != null) {
            sdp += "a=file-location:" + location + SipUtils.CRLF;
        }

        // Set the local SDP part in the dialog path
        getDialogPath().setLocalContent(sdp);

        // Create an INVITE request
        if (logger.isActivated()) {
            logger.info("Create INVITE");
        }
        try {
            SipRequest invite;
            invite = SipMessageFactory.createInvite(getDialogPath(),
                    InstantMessagingService.FT_FEATURE_TAGS, sdp);
            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);

            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);
            return invite;
        } catch (SipException e) {
            e.printStackTrace();
        } catch (CoreException e) {
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
     * M: Modified to resolve the 403 error issue.@{
     */
    protected void sendInvite(SipRequest invite) throws SipException {
    /**
     * @}
     */
        // Send INVITE request
        SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                .sendSipMessageAndWait(invite);

        // Wait response
        ctx.waitResponse(getResponseTimeout());

        // Analyze the received response
        if (ctx.isSipResponse()) {
            // A response has been received
            if (ctx.getStatusCode() == 200) {
                // 200 OK
                handle200OK(ctx.getSipResponse());
            } else if (ctx.getStatusCode() == 407) {
                // 407 Proxy Authentication Required
                handle407Authentication(ctx.getSipResponse());
            } else if (ctx.getStatusCode() == 422) {
                // 422 Session Interval Too Small
                handle422SessionTooSmall(ctx.getSipResponse());
            } else if (ctx.getStatusCode() == 603) {
                // 603 Invitation declined
                handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_DECLINED, ctx
                        .getReasonPhrase()));
            } else if (ctx.getStatusCode() == 487) {
                // 487 Invitation cancelled
                handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_CANCELLED, ctx
                        .getReasonPhrase()));
            }
            /**
             * M: Modified to resolve the 403 error issue.@{
             */
            else if (ctx.getStatusCode() == 403) {
                handle403Forbidden(invite);
            }
            /**
             * @}
             */
            else {
                // Other error response
                handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_FAILED, ctx
                        .getStatusCode()
                        + " " + ctx.getReasonPhrase()));
            }
        } else {
            if (logger.isActivated()) {
                logger.debug("No response received for INVITE");
            }
             /**M:ALPS00507513. ADDED to reslove issue of wrong prompt in case of
             file transfer timeout@{ 
             **/ 
            // No response received: timeout
            handleError(new FileSharingError(FileSharingError.SESSION_INITIATION_TIMEOUT));
             /**@}**/
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

            // Parse the remote SDP part
            SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            MediaAttribute attr = mediaDesc.getMediaAttribute("path");
            String remoteMsrpPath = attr.getValue();
            String remoteHost = SdpUtils
                    .extractRemoteHost(parser.sessionDescription.connectionInfo);
            int remotePort = mediaDesc.port;

            // Send ACK request
            if (logger.isActivated()) {
                logger.info("Send ACK");
            }
            getImsService().getImsModule().getSipManager().sendSipAck(getDialogPath());

            // The session is established
            getDialogPath().sessionEstablished();

            // Create the MSRP client session
            MsrpSession session = msrpMgr.createMsrpClientSession(remoteHost, remotePort,
                    remoteMsrpPath, this);
            session.setFailureReportOption(false);
            session.setSuccessReportOption(true);

            // Open the MSRP session
            msrpMgr.openMsrpSession();

            // Start session timer
            if (getSessionTimerManager().isSessionTimerActivated(resp)) {
                getSessionTimerManager().start(resp.getSessionTimerRefresher(),
                        resp.getSessionTimerExpire());
            }

            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                getListeners().get(i).handleSessionStarted();
            }

            // Start sending data chunks
            byte[] data = getContent().getData();
            InputStream stream;
            if (data == null) {
                // Load data from URL
                stream = FileFactory.getFactory().openFileInputStream(getContent().getUrl());
            } else {
                // Load data from memory
                stream = new ByteArrayInputStream(data);
            }
            msrpMgr.sendChunks(stream, ChatUtils.generateMessageId(), getContent().getEncoding(),
                    getContent().getSize());
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
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
            SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
                    InstantMessagingService.FT_FEATURE_TAGS, getDialogPath().getLocalContent());

            // Reset initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Set the Proxy-Authorization header
            getAuthenticationAgent().setProxyAuthorizationHeader(invite);

            // Send INVITE request
            sendInvite(invite);

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Handle error
     * 
     * @param error Error
     */
    public void handleError(FileSharingError error) {
        if (isInterrupted()) {
            return;
        }

        // Error
        if (logger.isActivated()) {
            logger
                    .info("Session error: " + error.getErrorCode() + ", reason="
                            + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for (int j = 0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener) getListeners().get(j)).handleTransferError(error);
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
                handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION,
                        "No Min-SE value found"));
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
            SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
                    InstantMessagingService.FT_FEATURE_TAGS, getDialogPath().getLocalContent());

            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);

            // Reset initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Send INVITE request
            sendInvite(invite);

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Data has been transfered
     * 
     * @param msgId Message ID
     */
    public void msrpDataTransfered(String msgId) {
        if (logger.isActivated()) {
            logger.info("Data transfered");
        }

        // File has been transfered
        fileTransfered();

        // Close the media session
        closeMediaSession();

        // Terminate session
        terminateSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for (int j = 0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener) getListeners().get(j)).handleFileTransfered(getContent()
                    .getUrl());
        }
    }

    /**
     * Data transfer has been received
     * 
     * @param msgId Message ID
     * @param data Received data
     * @param mimeType Data mime-type
     */
    public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
        // Not used in originating side
    }

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Notify listeners
        for (int j = 0; j < getListeners().size(); j++) {
            ((FileSharingSessionListener) getListeners().get(j)).handleTransferProgress(
                    currentSize, totalSize);
        }
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        if (logger.isActivated()) {
            logger.info("Data transfer aborted");
        }
    }

    /**
     * Data transfer error
     * 
     * @param error Error
     */
    public void msrpTransferError(String error) {
        if (isInterrupted()) {
            return;
        }

        if (logger.isActivated()) {
            logger.info("Data transfer error: " + error);
        }

        // Close the media session
        closeMediaSession();

        // Terminate session
        terminateSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        if (!isInterrupted()) {
            for (int j = 0; j < getListeners().size(); j++) {
                ((FileSharingSessionListener) getListeners().get(j))
                        .handleTransferError(new FileSharingError(
                                FileSharingError.MEDIA_TRANSFER_FAILED, error));
            }
        }
    }

    /**
     * Close the MSRP session
     */
    private void closeMsrpSession() {
        if (msrpMgr != null) {
            msrpMgr.closeSession();
        }
        if (logger.isActivated()) {
            logger.debug("MSRP session has been closed");
        }
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        // Close MSRP session
        closeMsrpSession();
    }
}
