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
package com.orangelabs.rcs.core.ims.service.capability;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRegistry;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipMessage;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.capability.Capabilities;
import com.orangelabs.rcs.service.api.client.capability.CapabilityApiIntents;
import com.orangelabs.rcs.utils.MimeManager;

/**
 * Capability utility functions
 * 
 * @author jexa7410
 */
public class CapabilityUtils {
	
	/**
	 * Get supported feature tags for capability exchange
	 *
	 * @param richcall Rich call supported
	 * @return List of tags
	 */
	public static List<String> getSupportedFeatureTags(boolean richcall) {
		List<String> tags = new ArrayList<String>();

		// Video share support
		if (RcsSettings.getInstance().isVideoSharingSupported() && richcall) {
			tags.add(FeatureTags.FEATURE_3GPP_VIDEO_SHARE);
		}

		String supported = "";

		// Chat support
		if (RcsSettings.getInstance().isImSessionSupported()) {
			supported += FeatureTags.FEATURE_RCSE_CHAT + ",";
		}
		
		// FT support
		if (RcsSettings.getInstance().isFileTransferSupported()) {
			supported += FeatureTags.FEATURE_RCSE_FT + ",";
		}

		// Image share support
		if (RcsSettings.getInstance().isImageSharingSupported() && richcall) {
			supported += FeatureTags.FEATURE_RCSE_IMAGE_SHARE + ",";
		}

		// Presence discovery support
		if (RcsSettings.getInstance().isPresenceDiscoverySupported()) {
			supported += FeatureTags.FEATURE_RCSE_PRESENCE_DISCOVERY + ",";
		}
		
		// Social presence support
		if (RcsSettings.getInstance().isSocialPresenceSupported()) {
			supported += FeatureTags.FEATURE_RCSE_SOCIAL_PRESENCE + ",";
		}

		// RCS extensions support
		String exts = RcsSettings.getInstance().getSupportedRcsExtensions();
		if ((exts != null) && (exts.length() > 0)) {
			 supported += exts;
		}

		// Add RCS-e prefix
		if (supported.length() != 0) {
			if (supported.endsWith(",")) {
				supported = supported.substring(0, supported.length()-1);
			}
			supported = FeatureTags.FEATURE_RCSE + "=\"" + supported + "\"";
			tags.add(supported);
		}
		
		return tags;
	}	

    /**
     * Extract features tags
     * 
     * @param msg Message
     * @return Capabilities
     */
    public static Capabilities extractCapabilities(SipMessage msg) {
    	// Analyze feature tags
    	Capabilities capabilities = new Capabilities(); 
    	ArrayList<String> tags = msg.getFeatureTags();
    	for(int i=0; i < tags.size(); i++) {
    		String tag = tags.get(i);
    		if (tag.equals(FeatureTags.FEATURE_3GPP_VIDEO_SHARE)) {
        		// Support video share service
        		capabilities.setVideoSharingSupport(true);
        	} else
        	if (tag.equals(FeatureTags.FEATURE_RCSE_IMAGE_SHARE)) {
        		// Support image share service
        		capabilities.setImageSharingSupport(true);
        	} else
        	if (tag.equals(FeatureTags.FEATURE_RCSE_CHAT)) {
        		// Support IM service
        		capabilities.setImSessionSupport(true);
        	} else
        	if (tag.equals(FeatureTags.FEATURE_RCSE_FT)) {
        		// Support FT service
        		capabilities.setFileTransferSupport(true);
        	} else
        	if (tag.equals(FeatureTags.FEATURE_OMA_IM)) {
        		// Support both IM & FT services
        		capabilities.setImSessionSupport(true);
        		capabilities.setFileTransferSupport(true);
        	} else
        	if (tag.equals(FeatureTags.FEATURE_RCSE_PRESENCE_DISCOVERY)) {
        		// Support capability discovery via presence service
        		capabilities.setPresenceDiscoverySupport(true);
        	} else
        	if (tag.equals(FeatureTags.FEATURE_RCSE_SOCIAL_PRESENCE)) {
        		// Support social presence service
        		capabilities.setSocialPresenceSupport(true);
        	} else
    		if (tag.startsWith(FeatureTags.FEATURE_RCSE_EXTENSION)) {
    			// Support a RCS extension
    			capabilities.addSupportedExtension(tag);
    		}
    	}
    	
    	// Analyze SDP part
    	byte[] content = msg.getContentBytes();
		if (content != null) {
	    	SdpParser parser = new SdpParser(content);

	    	// Get supported video codecs
	    	Vector<MediaDescription> mediaVideo = parser.getMediaDescriptions("video");
	    	Vector<String> videoCodecs = new Vector<String>();
			for (int i=0; i < mediaVideo.size(); i++) {
				MediaDescription desc = mediaVideo.get(i);
	    		MediaAttribute attr = desc.getMediaAttribute("rtpmap");
				if (attr !=  null) {
    	            String rtpmap = attr.getValue();
    	            String encoding = rtpmap.substring(rtpmap.indexOf(desc.payload)+desc.payload.length()+1);
    	            String codec = encoding.toLowerCase().trim();
    	            int index = encoding.indexOf("/");
    				if (index != -1) {
    					codec = encoding.substring(0, index);
    				}
    	    		if (MediaRegistry.isCodecSupported(codec)) {
    	    			videoCodecs.add(codec);
    	    		}    			
    			}
			}
			if (videoCodecs.size() == 0) {
				// No video codec supported between me and the remote contact
				capabilities.setVideoSharingSupport(false);
			}
    	
	    	// Check supported image formats
	    	Vector<MediaDescription> mediaImage = parser.getMediaDescriptions("message");
	    	Vector<String> imgFormats = new Vector<String>();
			for (int i=0; i < mediaImage.size(); i++) {
				MediaDescription desc = mediaImage.get(i);
	    		MediaAttribute attr = desc.getMediaAttribute("accept-types");
				if (attr != null) {
    	            String[] types = attr.getValue().split(" ");
    	            for(int j = 0; j < types.length; j++) {
    	            	String fmt = types[j];
    	            	if (MimeManager.isMimeTypeSupported(fmt)) {
    	            		imgFormats.addElement(fmt);
    	            	}
    	    		}
				}
			}
			if (imgFormats.size() == 0) {
				// No image format supported between me and the remote contact
				capabilities.setImageSharingSupport(false);
			}
		}
        
    	return capabilities;
    }
    
	/**
	 * Update external supported features
	 * 
	 * @param context Context
	 */
	public static void updateExternalSupportedFeatures(Context context) {
		try {
			// Intent query on current installed activities
			PackageManager packageManager = context.getPackageManager();
			Intent intent = new Intent(CapabilityApiIntents.RCS_EXTENSIONS);
			String mime = FeatureTags.FEATURE_RCSE + "/*"; 
			intent.setType(mime);			
			List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
			StringBuffer extensions = new StringBuffer();
			for(int i=0; i < list.size(); i++) {
				ResolveInfo info = list.get(i);
				for(int j =0; j < info.filter.countDataTypes(); j++) {
					String value = info.filter.getDataType(j);
					int index = value.indexOf(FeatureTags.FEATURE_RCSE_EXTENSION);
					if (index != -1) {
						String tag = value.substring(index);
						extensions.append("," + tag);
					}
				}
			}
			if ((extensions.length() > 0) && (extensions.charAt(0) == ',')) {
				extensions.deleteCharAt(0);
			}
	
			// Save extensions in database
			RcsSettings.getInstance().setSupportedRcsExtensions(extensions.toString());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
    
    /**
     * Build supported SDP part
     * 
     * @param ipAddress Local IP address
	 * @param richcall Rich call supported
	 * @return SDP
     */
    public static String buildSdp(String ipAddress, boolean richcall) {
    	String sdp = null;
		if (richcall) {
	        boolean video = RcsSettings.getInstance().isVideoSharingSupported();
	        boolean image = RcsSettings.getInstance().isImageSharingSupported();
	        if (video | image) {
				// Build the local SDP
		    	String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
		    	sdp = "v=0" + SipUtils.CRLF +
			        	"o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
			            "s=-" + SipUtils.CRLF +
			            "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
			            "t=0 0" + SipUtils.CRLF;

		    	// Add video config
		        if (video) {
		        	// Get supported video formats
					Vector<VideoFormat> videoFormats = MediaRegistry.getSupportedVideoFormats();
					StringBuffer videoSharingConfig = new StringBuffer();
					for(int i=0; i < videoFormats.size(); i++) {
						VideoFormat fmt = videoFormats.elementAt(i);
						videoSharingConfig.append("m=video 0 RTP/AVP " + fmt.getPayload() + SipUtils.CRLF);
						videoSharingConfig.append("a=rtpmap:" + fmt.getPayload() + " " + fmt.getCodec() + SipUtils.CRLF);
					}
					
					// Update SDP
			    	sdp += videoSharingConfig.toString();
		        }
	        
				// Add image config
		        if (image) {
					// Get supported image formats
					Vector<String> mimeTypes = MimeManager.getSupportedImageMimeTypes();
					StringBuffer supportedImageFormats = new StringBuffer();
					for(int i=0; i < mimeTypes.size(); i++) {
						supportedImageFormats.append(mimeTypes.elementAt(i) + " ");
				    }    	    	
				
					// Update SDP
					String imageSharingConfig = "m=message 0 TCP/MSRP *"  + SipUtils.CRLF +
						"a=accept-types:" + supportedImageFormats.toString().trim() + SipUtils.CRLF +
						"a=file-selector" + SipUtils.CRLF;
			    	int maxSize = ImageTransferSession.getMaxImageSharingSize();
			    	if (maxSize > 0) {
			    		imageSharingConfig += "a=max-size:" + maxSize + SipUtils.CRLF;
			    	}
			    	sdp += imageSharingConfig;
		        }
	        }
		}
		return sdp;
    } 
}
