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

import java.util.Vector;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.service.api.client.media.video.VideoCodec;

/**
 * Video codec management
 *
 * @author hlxn7157
 */
public class VideoCodecManager {

    /**
     * Create the SDP part for a given codec
     *
     * @param codec Media codec
     * @param localRtpPort Local RTP port
     * @return SDP part
     */
    public static String createCodecSdpPart(MediaCodec codec, int localRtpPort) {
    	VideoCodec videoCodec = new VideoCodec(codec);
        String result = "m=video " + localRtpPort + " RTP/AVP" + " " + videoCodec.getPayload() + SipUtils.CRLF
                + "a=rtpmap:" + videoCodec.getPayload() + " " + videoCodec.getCodecName() + "/" + videoCodec.getClockRate() + SipUtils.CRLF
                + "a=framesize:" + videoCodec.getPayload() + " " + videoCodec.getWidth() + "-" + videoCodec.getHeight() + SipUtils.CRLF
                + "a=framerate:" + videoCodec.getFramerate() + SipUtils.CRLF
                + "a=fmtp:" + videoCodec.getPayload() + " " + videoCodec.getCodecParams() + SipUtils.CRLF;
        return result;
    }

    /**
     * Create the SDP part for a list of codecs
     *
     * @param supportedCodecs List of supported media codecs
     * @return SDP part
     */
    public static String createCodecSdpPart(MediaCodec[] supportedCodecs, int localRtpPort) {
        StringBuffer result = new StringBuffer();
    	String prefCodec = RcsSettings.getInstance().getCShVideoFormat();

    	// Add the preferred codec in first
    	Vector<VideoCodec> codecs = new Vector<VideoCodec>();
    	for (int i=0; i < supportedCodecs.length; i++) {
    		VideoCodec videoCodec = new VideoCodec(supportedCodecs[i]);
    		if (supportedCodecs[i].getCodecName().equalsIgnoreCase(prefCodec)) {
    			codecs.insertElementAt(videoCodec, 0);
    		} else {
    			codecs.add(videoCodec);
    		}
    	}
    	
        result.append("m=video " + localRtpPort + " RTP/AVP");
        for (int i = 0; i < codecs.size(); i++) {
        	VideoCodec videoCodec = codecs.elementAt(i);
            result.append(" " + videoCodec.getPayload());
        }
        result.append(SipUtils.CRLF);
        int framerate = 0;
        for (int i = 0; i < codecs.size(); i++) {
        	VideoCodec videoCodec = codecs.elementAt(i);
            if (videoCodec.getFramerate() > framerate) {
                framerate = videoCodec.getFramerate();
            }
        }
        result.append("a=framerate:" + framerate + SipUtils.CRLF);
        for (int i = 0; i < codecs.size(); i++) {
        	VideoCodec videoCodec = codecs.elementAt(i);
            result.append(
            	"a=rtpmap:" + videoCodec.getPayload() + " " +
            		videoCodec.getCodecName() + "/" + videoCodec.getClockRate() + SipUtils.CRLF +
                "a=framesize:" + videoCodec.getPayload() + " " +
                	videoCodec.getWidth() + "-" + videoCodec.getHeight() + SipUtils.CRLF +
                "a=fmtp:" + videoCodec.getPayload() + " " +
                	videoCodec.getCodecParams() + SipUtils.CRLF);    	
        }    	
    	
        return result.toString();
    }

    /**
     * Video codec negotiation
     *
     * @param supportedCodecs List of supported media codecs
     * @param proposedCodecs List of proposed video codecs
     * @return Selected codec or null if no codec supported
     */
    public static VideoCodec negociateVideoCodec(MediaCodec[] supportedCodecs, Vector<VideoCodec> proposedCodecs) {
    	VideoCodec selectedCodec = null;
    	String prefCodec = RcsSettings.getInstance().getCShVideoFormat();
    	for (int i = 0; i < proposedCodecs.size(); i++) {
            for (int j = 0; j < supportedCodecs.length; j++) {
                VideoCodec videoCodec = new VideoCodec(supportedCodecs[j]);
                VideoCodec proposedCodec = proposedCodecs.get(i);
                if (proposedCodec.compare(videoCodec)) {
            		VideoCodec codec = new VideoCodec(
                            proposedCodec.getCodecName(),
                            (proposedCodec.getPayload()==0)?videoCodec.getPayload():proposedCodec.getPayload(),
                            (proposedCodec.getClockRate()==0)?videoCodec.getClockRate():proposedCodec.getClockRate(),
                            (proposedCodec.getCodecParams().length()==0)?videoCodec.getCodecParams():proposedCodec.getCodecParams(),
                            (proposedCodec.getFramerate()==0)?videoCodec.getFramerate():proposedCodec.getFramerate(),
                            (proposedCodec.getBitrate()==0)?videoCodec.getBitrate():proposedCodec.getBitrate(),
                            proposedCodec.getWidth(),
                            proposedCodec.getHeight());
            		
                	if (selectedCodec == null) {
                		// Select the first proposed and supported codec by default
                		selectedCodec = codec;
                	} else
                	if (codec.getCodecName().equalsIgnoreCase(prefCodec)) {
                		// Select the preferred codec if several propositions
                		selectedCodec = codec;
                	}
                }
            }
        }
        return selectedCodec;
    }

    /**
     * Create a video codec from its SDP description
     *
     * @param media Media SDP description
     * @return Video codec
     */
    public static VideoCodec createVideoCodecFromSdp(MediaDescription media) {
    	try {
	        String rtpmap = media.getMediaAttribute("rtpmap").getValue();
	
	        // Extract encoding name
	        String encoding = rtpmap.substring(rtpmap.indexOf(media.payload)
	        		+ media.payload.length() + 1).toLowerCase().trim();
	        String codecName = encoding;
	
	        // Extract clock rate
	        int clockRate = 0;
	        int index = encoding.indexOf("/");
	        if (index != -1) {
	            codecName = encoding.substring(0, index);
	            clockRate = Integer.parseInt(encoding.substring(index + 1));
	        }
	
	        // Extract video size
	        MediaAttribute frameSize = media.getMediaAttribute("framesize");
	        int videoWidth = H264Config.VIDEO_WIDTH; // default value
	        int videoHeight = H264Config.VIDEO_HEIGHT; // default value
	        if (frameSize != null) {
	        	try {
		            String value = frameSize.getValue();
		            int index2 = value.indexOf(media.payload);
		            int separator = value.indexOf('-');
		            if ((index2 != -1) && (separator != -1)) {
			            videoWidth = Integer.parseInt(
			            		value.substring(index2 + media.payload.length() + 1,
			                    separator));
			            videoHeight = Integer.parseInt(value.substring(separator + 1));
		            }
	        	} catch(NumberFormatException e) {
	        		// Use default value
	        	}
	        }
	        
	        // Extract frame rate
	        MediaAttribute attr = media.getMediaAttribute("framerate");
	        int frameRate = H264Config.FRAME_RATE; // default value
	        if (attr != null) {
	            frameRate = Integer.parseInt(attr.getValue());
	        }
	
	        // Extract the video codec parameters.
	        MediaAttribute fmtp = media.getMediaAttribute("fmtp");
	        String codecParameters = "";
	        if (fmtp != null) {
	            String value = fmtp.getValue();
	            int index2 = value.indexOf(media.payload);
	            if ((index2 > 0) && (value.length() > media.payload.length())) {
	            	codecParameters = value.substring(index2 + media.payload.length() + 1);
	            }
	        }

	        // Create a video codec
	        VideoCodec videoCodec = new VideoCodec(codecName,
	        		Integer.parseInt(media.payload), clockRate,
	        		codecParameters, frameRate, 0,
	                videoWidth, videoHeight);
	        return videoCodec;
    	} catch(NullPointerException e) {
        	return null;
		} catch(IndexOutOfBoundsException e) {
        	return null;
		}
    }

    /**
     * Extract list of video codecs from SDP part
     *
     * @param sdp SDP part
     * @return List of video codecs
     */
    public static Vector<VideoCodec> extractVideoCodecsFromSdp(Vector<MediaDescription> medias) {
    	Vector<VideoCodec> list = new Vector<VideoCodec>();
    	for(int i=0; i < medias.size(); i++) {
    		VideoCodec codec = createVideoCodecFromSdp(medias.get(i));
    		if (codec != null) {
    			list.add(codec);
    		}
    	}
    	return list;
    }
}
