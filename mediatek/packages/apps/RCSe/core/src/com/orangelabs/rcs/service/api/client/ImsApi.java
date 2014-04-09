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

package com.orangelabs.rcs.service.api.client;


import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import com.orangelabs.rcs.service.api.client.ClientApi;
import com.orangelabs.rcs.service.api.client.IImsApi;

/**
 * Contacts API
 */
public class ImsApi extends ClientApi {
	
	/**
	 * Core service API
	 */
	private IImsApi coreApi = null;
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     */
    public ImsApi(Context ctx) {
    	super(ctx);
    }

	/**
	 * Core service API connection
	 */
	public ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            coreApi = IImsApi.Stub.asInterface(service);

            // Notify event listener
        	notifyEventApiConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
        	// Notify event listener
        	notifyEventApiDisconnected();

        	coreApi = null;
        }
    };
    
	/**
	 * Is connected to IMS
	 * 
	 * @return Boolean
	 */
	public boolean isImsConnected() {
		if (coreApi!=null){
			try {
				return coreApi.isImsConnected();
			} catch (RemoteException e) {
				return false;
			}
		} else {
			return false;
		}
	}	
    
}
