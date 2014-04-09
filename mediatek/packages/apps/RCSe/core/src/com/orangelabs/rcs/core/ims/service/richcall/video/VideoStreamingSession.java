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

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingSession;
import com.orangelabs.rcs.service.api.client.media.IMediaRenderer;

/**
 * Video sharing streaming session
 * 
 * @author jexa7410
 */
public abstract class VideoStreamingSession extends ContentSharingSession {
    /**
	 * Media renderer
	 */
	private IMediaRenderer renderer = null;

    /** M: Used to mark media type @{ */
    private String mMediaTypeValue = null;

    public String getMediaType() {
        return mMediaTypeValue;
    }

    public void setMediaType(String mdeiaType) {
        mMediaTypeValue = mdeiaType;
    }

    /** @} */

	
	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 */
	public VideoStreamingSession(ImsService parent, MmContent content, String contact) {
		super(parent, content, contact);
	}

	/**
	 * Get the media renderer
	 * 
	 * @return Renderer
	 */
	public IMediaRenderer getMediaRenderer() {
		return renderer;
	}
	
	/**
	 * Set the media renderer
	 * 
	 * @param renderer Renderer
	 */
	public void setMediaRenderer(IMediaRenderer renderer) {
		this.renderer = renderer;
	}
	
	/**
	 * Receive BYE request 
	 * 
	 * @param bye BYE request
	 */
	public void receiveBye(SipRequest bye) {
		super.receiveBye(bye);
		
		// Request capabilities to the remote
		getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());
	}
}
