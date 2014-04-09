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

package com.orangelabs.rcs.service.api.server.richcall;

import android.content.Intent;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.provider.sharing.RichCallData;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.IMediaRenderer;
import com.orangelabs.rcs.service.api.client.media.video.IVideoPlayerEventListener;
import com.orangelabs.rcs.service.api.client.media.video.LiveVideoPlayer;
import com.orangelabs.rcs.service.api.client.media.video.PrerecordedVideoPlayer;
import com.orangelabs.rcs.service.api.client.media.video.VideoRenderer;
import com.orangelabs.rcs.service.api.client.richcall.IImageSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.IRichCallApi;
import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.RichCallApiIntents;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.Hashtable;

/**
 * Rich call API service
 * 
 * @author jexa7410
 */
public class RichCallApiService extends IRichCallApi.Stub {
	/**
	 * List of image sharing sessions
	 */
    private static Hashtable<String, IImageSharingSession> imageSharingSessions = new Hashtable<String, IImageSharingSession>();

	/**
	 * List of video sharing sessions
	 */
    private static Hashtable<String, IVideoSharingSession> videoSharingSessions = new Hashtable<String, IVideoSharingSession>();

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(RichCallApiService.class.getName());

	/**
	 * Constructor
	 */
	public RichCallApiService() {
		if (logger.isActivated()) {
			logger.info("Rich call API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear lists of sessions
		imageSharingSessions.clear();
		videoSharingSessions.clear();
	}

	/**
     * Add an image sharing session in the list
     * 
     * @param session Image sharing session
     */
	protected static void addImageSharingSession(ImageSharingSession session) {
		if (logger.isActivated()) {
			logger.debug("Add an image sharing session in the list (size=" + imageSharingSessions.size() + ")");
		}
		imageSharingSessions.put(session.getSessionID(), session);
	}

    /**
     * Remove an image sharing session from the list
     * 
     * @param sessionId Session ID
     */
	protected static void removeImageSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove an image sharing session from the list (size=" + imageSharingSessions.size() + ")");
		}
		imageSharingSessions.remove(sessionId);
	}

    /**
     * Add a video sharing session in the list
     * 
     * @param session Video sharing session
     */
	protected static void addVideoSharingSession(VideoSharingSession session) {
		if (logger.isActivated()) {
			logger.debug("Add a video sharing session in the list (size=" + videoSharingSessions.size() + ")");
		}
		videoSharingSessions.put(session.getSessionID(), session);
	}

    /**
     * Remove a video sharing session from the list
     * 
     * @param sessionId Session ID
     */
	protected static void removeVideoSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove a video sharing session from the list (size=" + videoSharingSessions.size() + ")");
		}
		videoSharingSessions.remove(sessionId);
	}

    /**
     * Get the remote phone number involved in the current call
     * 
     * @return Phone number or null if there is no call in progress
     * @throws ServerApiException
     */
	public String getRemotePhoneNumber() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get remote phone number");
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			return Core.getInstance().getImsModule().getCallManager().getRemoteParty();
		} catch(Exception e) {
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
     * Receive a new video sharing invitation
     * 
     * @param session Video sharing session
     */
    public void receiveVideoSharingInvitation(VideoStreamingSession session) {
		if (logger.isActivated()) {
			logger.info("Receive video sharing invitation from " + session.getRemoteContact());
		}

        // Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());
        VideoContent content = (VideoContent) session.getContent();

		// Update rich call history
		RichCall.getInstance().addCall(number, session.getSessionID(),
				RichCallData.EVENT_INCOMING,
				content,
    			RichCallData.STATUS_STARTED);

		// Add session in the list
		VideoSharingSession sessionApi = new VideoSharingSession(session);
		addVideoSharingSession(sessionApi);

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(RichCallApiIntents.VIDEO_SHARING_INVITATION);
    	intent.putExtra("contact", number);
    	intent.putExtra("contactDisplayname", session.getRemoteDisplayName());
    	intent.putExtra("sessionId", session.getSessionID());
    	intent.putExtra("videotype", content.getEncoding());
        intent.putExtra("videowidth", content.getWidth());
        intent.putExtra("videoheight", content.getHeight());
        /** M: add media type @{ */
        intent.putExtra("mediatype", session.getMediaType());
        /** @} */
        /** M: add stored video size to notify receiver @{ */
        intent.putExtra("videosize", content.getSize());
        /** @} */
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

	/**
     * Initiate a live video sharing session
     * 
     * @param contact Contact
     * @param player Media player
     * @throws ServerApiException
     */
	public IVideoSharingSession initiateLiveVideoSharing(String contact, IMediaPlayer player) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a live video session with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
		     // Initiate a new session
            VideoStreamingSession session = Core.getInstance().getRichcallService()
                    .initiateLiveVideoSharingSession(contact, player);

			// Update rich call history
			RichCall.getInstance().addCall(contact, session.getSessionID(),
                    RichCallData.EVENT_OUTGOING,
	    			session.getContent(),
	    			RichCallData.STATUS_STARTED);

			// Add session in the list
			VideoSharingSession sessionApi = new VideoSharingSession(session);
			addVideoSharingSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Initiate a pre-recorded video sharing session
     * 
     * @param contact Contact
     * @param file Video file
     * @param player Media player
     * @throws ServerApiException
     */
	public IVideoSharingSession initiateVideoSharing(String contact, String file, IMediaPlayer player) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate a pre-recorded video session with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create a video content
            /** M: add stored video size to notify receiver @{ */
            VideoContent content = (VideoContent) ContentManager.createVideoContentFromUrl(file);
            player.setTotalDuration(content.getSize());
            /** @} */
			VideoStreamingSession session = Core.getInstance().getRichcallService().initiatePreRecordedVideoSharingSession(contact, content, player);

			// Update rich call history
			RichCall.getInstance().addCall(contact, session.getSessionID(),
                    RichCallData.EVENT_OUTGOING,
	    			session.getContent(),
	    			RichCallData.STATUS_STARTED);

			// Add session in the list
			VideoSharingSession sessionApi = new VideoSharingSession(session);
			addVideoSharingSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * M: Added to match the plug-in mechanism while implemented the video
     * share. @{
     */
    /**
     * Create video renderer with specific codec format.
     * 
     * @param format The codec fromat.
     * @return The video renderer with specific codec format.
     */
    public IMediaRenderer createVideoRenderer(String format) {
        return new VideoRenderer(format);
    }

    /**
     * Create live video player with specific codec format.
     * 
     * @param format The codec fromat.
     * @return The live video player with specific codec format.
     */
    public IMediaPlayer createLiveVideoPlayer(String format) {
        return new LiveVideoPlayer(format);
    }

    /**
     * Create prerecorded video player.
     * 
     * @param codec Video codec name
     * @param filename Video filename
     * @param listener Video player listener
     */
    public IMediaPlayer createPrerecordedVideoPlayer(String codec, String filename,
            IVideoPlayerEventListener listener) {
        return new PrerecordedVideoPlayer(codec, filename, listener);
    }

    /** 
     * @} 
     */

	/**
	 * Get a video sharing session from its session ID
	 *
	 * @param id Session ID
	 * @return Session
	 * @throws ServerApiException
	 */
	public IVideoSharingSession getVideoSharingSession(String id) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get video sharing session " + id);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Return a session instance
		return videoSharingSessions.get(id);
	}

    /**
     * Receive a new image sharing invitation
     * 
     * @param session Image sharing session
     */
    public void receiveImageSharingInvitation(ImageTransferSession session) {
		if (logger.isActivated()) {
			logger.info("Receive image sharing invitation from " + session.getRemoteContact());
		}

        // Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich call history
		RichCall.getInstance().addCall(number, session.getSessionID(),
				RichCallData.EVENT_INCOMING,
				session.getContent(),
				RichCallData.STATUS_STARTED);

		// Add session in the list
		ImageSharingSession sessionApi = new ImageSharingSession(session);
		addImageSharingSession(sessionApi);

		// Broadcast intent related to the received invitation
		Intent intent = new Intent(RichCallApiIntents.IMAGE_SHARING_INVITATION);
		intent.putExtra("contact", number);
		intent.putExtra("contactDisplayname", session.getRemoteDisplayName());
		intent.putExtra("sessionId", session.getSessionID());
		intent.putExtra("filename", session.getContent().getName());
		intent.putExtra("filesize", session.getContent().getSize());
		intent.putExtra("filetype", session.getContent().getEncoding());
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Initiate an image sharing session
     * 
     * @param contact Contact
     * @param file Image file
     * @throws ServerApiException
     */
	public IImageSharingSession initiateImageSharing(String contact, String file) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an image sharing session with " + contact);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create an image content
			FileDescription desc = FileFactory.getFactory().getFileDescription(file);
			MmContent content = ContentManager.createMmContentFromUrl(file, desc.getSize());
			ImageTransferSession session = Core.getInstance().getRichcallService().initiateImageSharingSession(contact, content);

			// Update rich call history
			RichCall.getInstance().addCall(contact, session.getSessionID(),
                    RichCallData.EVENT_OUTGOING,
	    			session.getContent(),
	    			RichCallData.STATUS_STARTED);

			// Add session in the list
			ImageSharingSession sessionApi = new ImageSharingSession(session);
			addImageSharingSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Get an image sharing session from its session ID
     * 
     * @param id Session ID
     * @return Session
     * @throws ServerApiException
     */
	public IImageSharingSession getImageSharingSession(String id) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get image sharing session " + id);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Return a session instance
		return imageSharingSessions.get(id);
	}

    /**
     * Set multiparty call
     * 
     * @param state State
     * @throws ServerApiException
     */
	public void setMultiPartyCall(boolean state) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Set multiparty call to " + state);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Update call manager
    	Core.getInstance().getImsModule().getCallManager().setMultiPartyCall(state);
	}

    /**
     * Set call hold
     * 
     * @param state State
     * @throws ServerApiException
     */
	public void setCallHold(boolean state) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Set call hold to " + state);
		}

		// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		// Update call manager
    	Core.getInstance().getImsModule().getCallManager().setCallHold(state);
	}

	/**
     * M: Added to match the plug-in mechanism while implemented the video
     * share. @{
     */
    /**
     * Get vodafone account mapped with the specific number.
     * 
     * @param number The number to be mapped to vodafone account.
     * @return The vodafone account.
     * @throws ServerApiException
     */
    public String getVfAccountViaNumber(String number) throws ServerApiException {
        ServerApiUtils.testPermission();
        ServerApiUtils.testCore();
        return PhoneUtils.getVfAccountViaNumber(number);
    }

    /**
     * Get the number mapped to the specific vodafone account.
     * 
     * @param account The vodafone account.
     * @return The number mapped to the specific vodafone account.
     * @throws ServerApiException
     */
    public String getNumberViaVfAccount(String account) throws ServerApiException {
        ServerApiUtils.testPermission();
        ServerApiUtils.testCore();
        return PhoneUtils.getNumberViaVfAccount(account);
    }
    /**
     * @}
     */
}
