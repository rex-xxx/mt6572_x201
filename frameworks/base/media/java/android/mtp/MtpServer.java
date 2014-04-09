/*
 * Copyright (C) 2010 The Android Open Source Project
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
 */

package android.mtp;

/// M: ALPS00120037, add log for support MTP debugging
import com.mediatek.xlog.Xlog;

/**
 * Java wrapper for MTP/PTP support as USB responder.
 * {@hide}
 */
public class MtpServer implements Runnable {

    private int mNativeContext; // accessed by native methods
    /// M: ALPS00120037, Check Thread run status @{
    private static final String TAG = "MtpServer";
    private boolean mServerEndup = false;
    /// M: @}

    static {
        System.loadLibrary("media_jni");
    }

    public MtpServer(MtpDatabase database, boolean usePtp) {
        /// M: ALPS00120037, Check Thread run status @{
        Xlog.v(TAG, "MtpServer constructor: native_setup!!");
        mServerEndup = false;
        /// M: @}

        native_setup(database, usePtp);
    }

    public void start() {
        /// M: ALPS00120037, Check Thread run status @{
        Xlog.v(TAG, "MtpServer start!!");
        mServerEndup = false;
        /// M: @}

        Thread thread = new Thread(this, "MtpServer");
        thread.start();
    }

    @Override
    public void run() {
        /// M: ALPS00120037, Check Thread run status @{
        Xlog.v(TAG, "MtpServer run!!");
        mServerEndup = false;
        /// M: @}
        
        native_run();
        native_cleanup();
        
        /// M: ALPS00120037, Check Thread run status @{
        mServerEndup = true;
        Xlog.v(TAG, "MtpServer run-end!!");
        /// M: @}
    }

    public void sendObjectAdded(int handle) {
        native_send_object_added(handle);
    }

    public void sendObjectRemoved(int handle) {
        native_send_object_removed(handle);
    }

    public void addStorage(MtpStorage storage) {
        native_add_storage(storage);
    }

    public void removeStorage(MtpStorage storage) {
        native_remove_storage(storage.getStorageId());
    }

	/**
	 * Added for Storage Update and send StorageInfoChanged event
	 * @hide
	 * @internal
	 */
    public void updateStorage(MtpStorage storage) {
        native_update_storage(storage);
    }
	/**
	 * Added for send StorageInfoChanged event
	 * @hide
	 * @internal
	 */
    public void sendStorageInfoChanged(MtpStorage storage) {
        native_send_storage_infoChanged(storage.getStorageId());
    }

	/**
	 * Added Modification for ALPS00255822, bug from WHQL test @{
	 * @hide
	 * @internal
	 */
    public void endSession() {
        Xlog.w(TAG, "MtpServer endSession!!");
        native_end_session();
        //return mServerEndup;
    }

	/**
	 * ALPS00120037, Check Thread run status
	 * @hide
	 * @internal
	 */
    public boolean getStatus() {
        Xlog.w(TAG, "MtpServer getStatus!!");
        return mServerEndup;
    }

	/**
	 * Added for ALPS00289309, update Object, send ObjectInfoChanged event
	 * @hide
	 * @internal
	 */
    public void sendObjectInfoChanged(int handle) {
        native_send_object_infoChanged(handle);
    }

    /// M: Added Modification for ALPS00255822, bug from WHQL test
    private native final void native_end_session();
    /// M: ALPS00289309, update Object
    private native final void native_setup(MtpDatabase database, boolean usePtp);
    private native final void native_run();
    private native final void native_cleanup();
    private native final void native_send_object_added(int handle);
    private native final void native_send_object_removed(int handle);
    private native final void native_add_storage(MtpStorage storage);
    private native final void native_remove_storage(int storageId);
	/**
	 * @internal
	 */
    private native final void native_update_storage(MtpStorage storage);
    private native final void native_send_storage_infoChanged(int storageId);
    private native final void native_send_object_infoChanged(int handle);
    /// @}
}
