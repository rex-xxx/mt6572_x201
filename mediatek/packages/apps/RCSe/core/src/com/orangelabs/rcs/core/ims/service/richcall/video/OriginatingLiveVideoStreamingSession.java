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

package com.orangelabs.rcs.core.ims.service.richcall.video;

import android.os.RemoteException;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.LiveVideoContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.service.api.client.media.IMediaEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.video.VideoCodec;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.Vector;

/**
 * Originating live video content sharing session (streaming)
 *
 * @author jexa7410
 */
public class OriginatingLiveVideoStreamingSession extends VideoStreamingSession {
    /**
     * Media player
     */
    private IMediaPlayer player = null;

    /** M: Video Live @{ */
    public static final String VIDEO_LIVE = "videolive";
    /** @} */

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param parent IMS service
     * @param player Media player
     * @param content Content to be shared
     * @param contact Remote contact
     */
    public OriginatingLiveVideoStreamingSession(ImsService parent, IMediaPlayer player,
            LiveVideoContent content, String contact) {
        super(parent, content, contact);

        // Create dialog path
        createOriginatingDialogPath();

        // Set the media player
        this.player = player;
    }

    /**
     * M: Modified to resolve rich call 403 error. @{
     */
    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new live video sharing session as originating");
            }

            SipRequest invite = createSipInvite();

            // Send INVITE request
            sendInvite(invite);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }
    /**
     * @}
     */
    
    /**
     * M: Added to resolve rich call 403 error. @{
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
        try {
         // Check player
            if ((player == null) || (player.getMediaCodec() == null)) {
                handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE,
                        "Video codec not selected"));
                return null;
            }
            // Build SDP part
            String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String videoSdp = VideoCodecManager.createCodecSdpPart(
                    player.getSupportedMediaCodecs(), player.getLocalRtpPort());
            String sdp =
                "v=0" + SipUtils.CRLF +
                "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
                "s=-" + SipUtils.CRLF + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
                "t=0 0" + SipUtils.CRLF +
                videoSdp +
                "a=sendonly" + SipUtils.CRLF;

            /** M: Set media type @{ */
            String xType = getXTypeAttribute();
            if (xType != null) {
                sdp += "a=X-type:" + xType + SipUtils.CRLF;
            }
            /** @} */

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Create an INVITE request
            if (logger.isActivated()) {
                logger.info("Send INVITE");
            }
            SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
                    RichcallService.FEATURE_TAGS_VIDEO_SHARE, sdp);

	        // Set the Authorization header
	        getAuthenticationAgent().setAuthorizationHeader(invite);

            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);
            return invite;
        } catch (RemoteException e) {
        } catch (SipException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
        logger.error("Create sip invite failed, return null.");
        return null;
    }

    /**
     * M: Get media type @{
     */
    private String getXTypeAttribute() {
        return VIDEO_LIVE;
    }
    /**
     * @}
     */
    
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
     * M: Modified to resolve rich call 403 error. @{
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
            } else
            if (ctx.getStatusCode() == 407) {
                // 407 Proxy Authentication Required
                handle407Authentication(ctx.getSipResponse());
            } else
            if (ctx.getStatusCode() == 422) {
                // 422 Session Interval Too Small
                handle422SessionTooSmall(ctx.getSipResponse());
            } 
            /**
             * M: modified for IOT item that it should get "No answer"
             * notification once timed out @{
             */
            else if (ctx.getStatusCode() == 603) {
                // 603 Invitation declined
                getImsService().getImsModule().getCapabilityService()
                        .requestContactCapabilities(getDialogPath().getRemoteParty());
                handleError(new ContentSharingError(
                        ContentSharingError.SESSION_INITIATION_DECLINED, ctx.getReasonPhrase()));
            } else if (ctx.getStatusCode() == 487) {
                // 487 Invitation cancelled
                handleError(new ContentSharingError(
                        ContentSharingError.SESSION_INITIATION_CANCELLED, ctx.getReasonPhrase()));
            }else if ((ctx.getStatusCode() == 486) 
                    || (ctx.getStatusCode() == 480) 
                    /**
                     * M: Add to resolve the request timeout error. @{
                     */
                    || (ctx.getStatusCode() == 408)) {
                    /**
                     * @}
                     */
                // 486 Invitation timeout
                getImsService().getImsModule().getCapabilityService()
                        .requestContactCapabilities(getDialogPath().getRemoteParty());
                handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_TIMEOUT,
                        ctx.getReasonPhrase()));
            }
            /**
             * M: Modified to resolve rich call 403 error. @{
             */
            else if(ctx.getStatusCode() == 403){
                handle403Forbidden(invite);
            }
            /**
             * @}
             */
            else {
                // Other error response
                getImsService().getImsModule().getCapabilityService()
                        .requestContactCapabilities(getDialogPath().getRemoteParty());
                handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED,
                        ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
            }
        } else {
            getImsService().getImsModule().getCapabilityService()
                    .requestContactCapabilities(getDialogPath().getRemoteParty());
            // No response received: timeout
            handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_TIMEOUT,
                    "timeout"));
            /**
             * @}
             */
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

            // The session is established
            getDialogPath().sessionEstablished();

            // Parse the remote SDP part
            SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription.connectionInfo);
            MediaDescription mediaVideo = parser.getMediaDescription("video");
            int remotePort = mediaVideo.port;

            // Extract video codecs from SDP
            Vector<MediaDescription> medias = parser.getMediaDescriptions("video");
            Vector<VideoCodec> proposedCodecs = VideoCodecManager.extractVideoCodecsFromSdp(medias);

            // Codec negotiation
            VideoCodec selectedVideoCodec = VideoCodecManager.negociateVideoCodec(
                    player.getSupportedMediaCodecs(), proposedCodecs);
            if (selectedVideoCodec == null) {
                if (logger.isActivated()) {
                    logger.debug("Proposed codecs are not supported");
                }
                
                // Terminate session
                terminateSession();
                handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE));
                return;
            }
            getContent().setEncoding("video/" + selectedVideoCodec.getCodecName());

            // Set the selected media codec
            player.setMediaCodec(selectedVideoCodec.getMediaCodec());

            // Set media player event listener
            player.addListener(new MediaPlayerEventListener(this));

            // Open the media player
            player.open(remoteHost, remotePort);

            // Send ACK request
            if (logger.isActivated()) {
                logger.info("Send ACK");
            }
            getImsService().getImsModule().getSipManager().sendSipAck(getDialogPath());

            // Start the media player
            player.start();

            // Start session timer
            if (getSessionTimerManager().isSessionTimerActivated(resp)) {
                getSessionTimerManager().start(resp.getSessionTimerRefresher(),
                        resp.getSessionTimerExpire());
            }

            // Notify listeners
            for (int i = 0; i < getListeners().size(); i++) {
                getListeners().get(i).handleSessionStarted();
            }

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
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
                    RichcallService.FEATURE_TAGS_VIDEO_SHARE, getDialogPath().getLocalContent());

            // Reset initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Set the Proxy-Authorization header
            getAuthenticationAgent().setProxyAuthorizationHeader(invite);

            // Send INVITE request
            sendInvite(invite);

        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }

    /**
     * Handle error
     *
     * @param error Error
     */
    public void handleError(ContentSharingError error) {
        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        if (!isInterrupted()) {
            for (int i = 0; i < getListeners().size(); i++) {
                ((VideoStreamingSessionListener)getListeners().get(i)).handleSharingError(error);
            }
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
                handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
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
                    RichcallService.FEATURE_TAGS_VIDEO_SHARE, getDialogPath().getLocalContent());

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
            handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
        try {
            // Close the media player
            player.stop();
            player.close();
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Exception when closing the media player", e);
            }
        }
    }

    /**
     * Media player event listener
     */
    private class MediaPlayerEventListener extends IMediaEventListener.Stub {
        /**
         * Streaming session
         */
        private VideoStreamingSession session;

        /**
         * Constructor
         *
         * @param session Streaming session
         */
        public MediaPlayerEventListener(VideoStreamingSession session) {
            this.session = session;
        }

        /**
         * Media player is opened
         */
        public void mediaOpened() {
            if (logger.isActivated()) {
                logger.debug("Media player is opened");
            }
        }

        /**
         * Media player is closed
         */
        public void mediaClosed() {
            if (logger.isActivated()) {
                logger.debug("Media player is closed");
            }
        }

        /**
         * Media player is started
         */
        public void mediaStarted() {
            if (logger.isActivated()) {
                logger.debug("Media player is started");
            }
        }

        /**
         * Media player is stopped
         */
        public void mediaStopped() {
            if (logger.isActivated()) {
                logger.debug("Media player is stopped");
            }
        }

        /**
         * Media player has failed
         *
         * @param error Error
         */
        public void mediaError(String error) {
            if (logger.isActivated()) {
                logger.error("Media has failed: " + error);
            }

            // Close media session
            closeMediaSession();

            // Terminate session
            terminateSession();

            // Remove the current session
            getImsService().removeSession(session);

            // Notify listeners
            if (!isInterrupted()) {
                for (int i = 0; i < getListeners().size(); i++) {
                    ((VideoStreamingSessionListener) getListeners().get(i))
                            .handleSharingError(new ContentSharingError(
                                    ContentSharingError.MEDIA_STREAMING_FAILED, error));
                }
            }
        }
    }
}
