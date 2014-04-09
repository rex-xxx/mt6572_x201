/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright © 2010 France Telecom S.A.
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

package com.orangelabs.rcs.provider.eab;

import android.net.Uri;

/**
 * Rich address book data constants
 * 
 * @author jexa7410
 */
public class RichAddressBookData {
	/**
	 * Database URI
	 */
	static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.eab/eab");
	
	/**
	 * Column name
	 */
	static final String KEY_ID = "_id";
	
	/**
	 * Column name
	 */
	static final String KEY_CONTACT_NUMBER = "contact_number";
	
	/**
	 * Column name
	 */
	static final String KEY_PRESENCE_SHARING_STATUS = "presence_sharing_status";
	
	/**
	 * Column name
	 */
	static final String KEY_TIMESTAMP = "timestamp";
}
