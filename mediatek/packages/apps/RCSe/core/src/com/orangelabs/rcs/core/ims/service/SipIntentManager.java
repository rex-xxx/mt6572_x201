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

package com.orangelabs.rcs.core.ims.service;

import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.service.api.client.sip.SipApiIntents;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SIP intent manager
 * 
 * @author jexa7410
 */
public class SipIntentManager {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * Constructor
	 */
	public SipIntentManager() {
	}
	
	/**
	 * Is the SIP intent may be resolved
	 * 
	 * @param request Incoming request
	 * @return Returns true if the intent has been resolved, else returns false
	 */
	public boolean isSipIntentResolved(SipRequest request) {
		// Get feature tags
		List<String> tags = request.getFeatureTags();
		if (tags.size() == 0) {
    		if (logger.isActivated()) {
    			logger.debug("No feature tag found in the request");
    		}
			return false;
		}		

		// Create the intent associated to the SIP request
		String featureTag = tags.get(0);
		Intent intent = generateSipIntent(request, featureTag);
		return isSipIntentResolvedByBroadcastReceiver(intent);
	}	

	/**
	 * Generate a SIP Intent
	 * 
	 * @param request SIP request
	 * @param featureTag Feature tag
	 */
	private Intent generateSipIntent(SipRequest request, String featureTag) {
		// Create the intent
		String action = SipApiIntents.SESSION_INVITATION;
		Intent intent = new Intent(action);
		intent.addCategory(Intent.CATEGORY_DEFAULT);

		// Set intent parameters
		String mime = FeatureTags.FEATURE_RCSE + "/" + featureTag; 
		intent.setType(mime.toLowerCase());
        
		return intent;
	}

	/**
	 * Is the SIP intent may be resolved by at least broadcast receiver
	 * 
	 * @param intent The Intent to resolve
	 * @return Returns true if the intent has been resolved, else returns false
	 */
	private boolean isSipIntentResolvedByBroadcastReceiver(Intent intent) {
		PackageManager packageManager = AndroidFactory.getApplicationContext().getPackageManager();
		List<ResolveInfo> list = packageManager.queryBroadcastReceivers(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return (list.size() > 0);
	}	
}
