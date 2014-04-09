package com.orangelabs.rcs.service.api.client.richcall;

import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.IImageSharingSession;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.IMediaRenderer;
import com.orangelabs.rcs.service.api.client.media.video.IVideoPlayerEventListener;

/**
 * Rich call API
 */
interface IRichCallApi {

	// Get the remote phone number involved in the current call
	String getRemotePhoneNumber();

	// Initiate a live video sharing session
	IVideoSharingSession initiateLiveVideoSharing(in String contact, in IMediaPlayer player);

	// Initiate a pre-recorded video sharing session
	IVideoSharingSession initiateVideoSharing(in String contact, in String file, in IMediaPlayer player);

	// Get a video sharing session from its session ID
	IVideoSharingSession getVideoSharingSession(in String id);

	// Initiate an image sharing session
	IImageSharingSession initiateImageSharing(in String contact, in String file);

	// Get an image sharing session from its session ID
	IImageSharingSession getImageSharingSession(in String id);

	// Set multiparty call
	void setMultiPartyCall(in boolean flag);

	// Set call hold
	void setCallHold(in boolean flag);
	
	//create video renderer.
	IMediaRenderer createVideoRenderer(in String format);
	
	//create live video player.
	IMediaPlayer createLiveVideoPlayer(in String format);
	
	//create prerecorded video player.
    IMediaPlayer createPrerecordedVideoPlayer(in String codec, in String filename, in IVideoPlayerEventListener listener);

	//Get the vodafone account mapped to the specific number.
	String getVfAccountViaNumber(String number);
	
	//Get the number mapped to the specific vodafone account.
	String getNumberViaVfAccount(String account);
}


