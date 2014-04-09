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

package com.orangelabs.rcs.core.content;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.decoder.NativeH263Decoder;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.utils.MimeManager;
import com.orangelabs.rcs.utils.logger.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Vector;

/**
 * Multimedia content manager
 * 
 * @author jexa7410
 */
public class ContentManager{
    /** M: add log @{ */
    private static final Logger LOGGER = Logger.getLogger(ContentManager.class.getName());

    /**@}*/
    /**
     * Generate an URL for the received content
     * 
     * @param filename Filename
     * @param mime MIME type
     * @return URL
     */
	public static String generateUrlForReceivedContent(String filename, String mime) {
		// Generate a file path
		String path;
    	if (mime.startsWith("image")) {
			path = FileFactory.getFactory().getPhotoRootDirectory();
        } else
    	if (mime.startsWith("video")) {
			path = FileFactory.getFactory().getVideoRootDirectory();
    	} else {
			path = FileFactory.getFactory().getFileRootDirectory();
    	}

    	// Check that the filename will not overwrite existing file
    	// We modify it if a file of the same name exists, by appending _1 before the extension
    	// For example if image.jpeg exists, next file will be image_1.jpeg, then image_2.jpeg etc
    	String extension = "";
    	if ((filename!=null) && (filename.indexOf('.')!=-1)){
    		// if extension is present, split it
    		extension = "." + filename.substring(filename.lastIndexOf('.')+1);
    		filename = filename.substring(0, filename.lastIndexOf('.'));
    	}
    	String destination = filename;
    	int i = 1;
    	while(FileFactory.getFactory().fileExists(path + destination + extension)){
    		destination = filename + '_' + i;
    		i++;
    	}

    	// Return free destination url
        return path + destination + extension;
	}

	/**
	 * Save a content in the local directory of the device
	 *
	 * @param content Content to be saved
	 * @throws IOException
	 */
	public static void saveContent(MmContent content) throws IOException {
		// Write data
		OutputStream os = FileFactory.getFactory().openFileOutputStream(content.getUrl());
		os.write(content.getData());
		os.flush();
        os.close();

		// Update the media storage
		FileFactory.getFactory().updateMediaStorage(content.getUrl());
	}

    /**
     * Create a content object from URL description
     * 
     * @param url Content URL
     * @param size Content size
     * @return Content instance
     */
	public static MmContent createMmContentFromUrl(String url, long size) {
		String ext = MimeManager.getFileExtension(url);
		String mime = MimeManager.getMimeType(ext);
		if (mime != null) {
			if (mime.startsWith("image/")) {
				return new PhotoContent(url, mime, size);
			}
			if (mime.startsWith("video/")) {
				return new VideoContent(url, mime, size);
			}
        }
		return new FileContent(url, size);
	}

    /** M: add stored video size to notify receiver @{ */
    /**
     * Create video content from URL description
     * 
     * @param url Content URL
     * @return Content instance
     */
    public static MmContent createVideoContentFromUrl(String url) {
        if (LOGGER.isActivated()) {
            LOGGER.debug("createVideoContentFromUrl entry");
        }
        NativeH263Decoder.InitParser(url);
        long duration = NativeH263Decoder.getVideoLength();
        NativeH263Decoder.DeinitParser();
        if (LOGGER.isActivated()) {
            LOGGER.debug("createVideoContentFromUrl exit with total duration: " + duration);
        }
        return new VideoContent(url, null, duration);
    }

    /** @} */

    /**
     * Create a content object from MIME type
     * 
     * @param url Content URL
     * @param mime MIME type
     * @param size Content size
     * @return Content instance
     */
	public static MmContent createMmContentFromMime(String url, String mime, long size) {
    	if (mime.startsWith("image/")) {
    		// Photo content
    		return new PhotoContent(url, mime, size);
        }
    	if (mime.startsWith("video/")) {
    		// Video content
    		return new VideoContent(url, mime, size);
        }
    	if (mime.startsWith("application/")) {
    		// File content
    		return new FileContent(url, size);
    	}
		return null;
	}

    /**
     * Create a live video content object
     * 
     * @param codec Codec
     * @return Content instance
     */
	public static LiveVideoContent createLiveVideoContent(String codec) {
		return new LiveVideoContent("video/"+codec);
	}

    /** M: add stored video size to notify receiver @{ */
    /**
     * Create a stored video content object
     * 
     * @param url The URL of stored video
     * @param codec Codec
     * @param size The size of stored video
     * @return Content instance
     */
    private static VideoContent createPrerecordedVideoContent(String url, String codec, long size) {
        return new VideoContent(url, "video/" + codec, size);
    }

    /** @} */

    /**
     * Create a generic live video content object
     * 
     * @return Content instance
     */
    public static LiveVideoContent createGenericLiveVideoContent() {
        return new LiveVideoContent("video/*");
    }

	/**
     * Create a live video content object
     * 
     * @param sdp SDP part
     * @return Content instance
     */
	public static LiveVideoContent createLiveVideoContentFromSdp(byte[] sdp) {
		 // Parse the remote SDP part
        SdpParser parser = new SdpParser(sdp);
    	Vector<MediaDescription> media = parser.getMediaDescriptions();
		MediaDescription desc = media.elementAt(0);
        String rtpmap = desc.getMediaAttribute("rtpmap").getValue();

        // Extract the video encoding
        String encoding = rtpmap.substring(rtpmap.indexOf(desc.payload)+desc.payload.length()+1);
        String codec = encoding.toLowerCase().trim();
        int index = encoding.indexOf("/");
		if (index != -1) {
			codec = encoding.substring(0, index);
        }
		return createLiveVideoContent(codec);
	}

    /** M: add stored video size to notify receiver @{ */
    /**
     * Create a video content object
     * 
     * @param sdp SDP part
     * @return Content instance
     */
    public static VideoContent createVideoContentFromSdp(byte[] sdp) {
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(sdp);
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription desc = media.elementAt(0);
        String rtpmap = desc.getMediaAttribute("rtpmap").getValue();

        // Extract the video encoding
        String encoding = rtpmap
                .substring(rtpmap.indexOf(desc.payload) + desc.payload.length() + 1);
        String codec = encoding.toLowerCase().trim();
        int index = encoding.indexOf("/");
        if (index != -1) {
            codec = encoding.substring(0, index);
        }
        MediaAttribute attr = desc.getMediaAttribute("file-selector");
        if (attr == null) {
            if (LOGGER.isActivated()) {
                LOGGER.debug("createVideoContentFromSdp() live video");
            }
            return createLiveVideoContent(codec);
        } else {
            String fileSelectorValue = attr.getValue();
            long size = Long.parseLong(SipUtils.extractParameter(fileSelectorValue, "size:", "-1"));
            String filename = SipUtils.extractParameter(fileSelectorValue, "name:", "");
            String filePath = null;
            try {
                filePath = URLDecoder.decode(filename, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                if (LOGGER.isActivated()) {
                    LOGGER.debug("createVideoContentFromSdp() UnsupportedEncodingException");
                }
                return null;
            }
            if (LOGGER.isActivated()) {
                LOGGER.debug("createVideoContentFromSdp() stored video: " + filePath);
            }
            return createPrerecordedVideoContent(filePath, codec, size);
        }
    }

    /** @} */

	/**
     * Create a content object from SDP data
     * 
     * @param sdp SDP part
     * @return Content instance
     */
	public static MmContent createMmContentFromSdp(String sdp) {
		// Set the content
		try {
	    	SdpParser parser = new SdpParser(sdp.getBytes());
			Vector<MediaDescription> media = parser.getMediaDescriptions();
			MediaDescription desc = media.elementAt(0);
			MediaAttribute attr1 = desc.getMediaAttribute("file-selector");
	        String fileSelectorValue = attr1.getValue();
			String mime = SipUtils.extractParameter(fileSelectorValue, "type:", "application/octet-stream");
			long size = Long.parseLong(SipUtils.extractParameter(fileSelectorValue, "size:", "-1"));
			String filename = SipUtils.extractParameter(fileSelectorValue, "name:", "");
			/**
             * M: Decode the string of file path to UTF-8 @{
             */
            String filePath = URLDecoder.decode(filename, "UTF-8");
            if (LOGGER.isActivated()) {
                LOGGER.debug("createMmContentFromSdp() filePath: " + filePath);
            }
            /**
             * @}
             */
            String url = ContentManager.generateUrlForReceivedContent(filePath, mime);
			MmContent content = ContentManager.createMmContentFromMime(url, mime, size);
			return content;
		} catch(Exception e) {
			return null;
		}
	}
}
