/*
 * Copyright (C) 2008 The Android Open Source Project
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

/**
 * TODO: Move this to services.jar
 * and make the constructor package private again.
 * @hide
 */
package android.server;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.util.Log;

/// M:  @{        
import android.os.SystemProperties;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.net.wifi.WifiManager; /// @}

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.concurrent.locks.ReentrantLock;



public class BluetoothA2dpService extends IBluetoothA2dp.Stub {
    private static final String TAG = "BluetoothA2dpService";
    private static final boolean DBG = true;

    public static final String BLUETOOTH_A2DP_SERVICE = "bluetooth_a2dp";

    private static final String BLUETOOTH_ADMIN_PERM = android.Manifest.permission.BLUETOOTH_ADMIN;
    private static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;

    private static final String BLUETOOTH_ENABLED = "bluetooth_enabled";

    private static final String PROPERTY_STATE = "State";

    private final Context mContext;
    private final IntentFilter mIntentFilter;
    private HashMap<BluetoothDevice, Integer> mAudioDevices;
    private final AudioManager mAudioManager;
    private final BluetoothService mBluetoothService;
    private final BluetoothAdapter mAdapter;
    private int   mTargetA2dpState;
    private BluetoothDevice mPlayingA2dpDevice;
    private IntentBroadcastHandler mIntentBroadcastHandler;
    private final WakeLock mWakeLock;
    static final String AVRCP_SERVICE_CLASS = "com.mediatek.bluetooth.avrcp.IBTAvrcpService"; // A2DP RFMAB2 IOT
    
    
    /// M:  @{
    private static final int FM_VIA_CONTROLLER = 0;

    private static final int FM_VIA_HOST = 1;

    public static final int FM_START_FAILED = 33;// BluetoothA2dp.Disconnecting=3

    public static final String ACTION_FM_OVER_BT_CONTROLLER = "android.server.a2dp.action.FM_OVER_BT_CONTROLLER";

    public static final String ACTION_FM_OVER_BT_HOST = "android.server.a2dp.action.FM_OVER_BT_HOST";

    public static final String EXTRA_RESULT_STATE = "android.bluetooth.a2dp.extra.RESULT_STATE";

    public static final int FMSTART_SUCCESS = 0; // fm over bt start success
    //FMRadioService
    public static final int FMSTART_FAILED = 1; // fm over bt start failed

    public static final int FMSTART_ALREADY = 2; // fm already over bt

    private boolean mFmOverBtMode;

    private boolean mFmOverBtOn = false;

    private int mA2dpState;// Provide for FM

    private int mA2dpDisconnecting;

    private boolean mFmStartReq;

    private int mFmResult;

    private static int sSinkCount;

    private WifiManager mWM;

    public static final String MSG_FM_POWER_UP = "com.mediatek.FMRadio.FMRadioService.ACTION_TOA2DP_FM_POWERUP";

    public static final String MSG_FM_POWER_DOWN = "com.mediatek.FMRadio.FMRadioService.ACTION_TOA2DP_FM_POWERDOWN";

    // public final String FMRadioService_MSG_FM_POWER_UP = "FM Power Up";
    // public final String FMRadioService_MSG_FM_POWER_DOWN = "FM Power Down";
    
    /// @}

    private static final int MSG_CONNECTION_STATE_CHANGED = 0;

    private ReentrantLock mPriorityLock;

    private static BluetoothA2dpService sInstance;
    /**
    * get instance of BluetoothA2dpService 
    */
    public static BluetoothA2dpService getA2dpServiceInstance() {
        return sInstance;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                               BluetoothAdapter.ERROR);
                switch (state) {
                case BluetoothAdapter.STATE_ON:
                    onBluetoothEnable();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    onBluetoothDisable();
                    break;
                }
            } 
            /* else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                synchronized (this) {
                    if (mAudioDevices.containsKey(device)) {
                        int state = mAudioDevices.get(device);
                        handleSinkStateChange(device, state, BluetoothA2dp.STATE_DISCONNECTED);
                    }
                }
            } */
            else if (action.equals(AudioManager.VOLUME_CHANGED_ACTION)) {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType == AudioManager.STREAM_MUSIC) {
                    List<BluetoothDevice> sinks = getConnectedDevices();

                    if (sinks.size() != 0 && isPhoneDocked(sinks.get(0))) {
                        String address = sinks.get(0).getAddress();
                        int newVolLevel =
                          intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
                        int oldVolLevel =
                          intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, 0);
                        String path = mBluetoothService.getObjectPathFromAddress(address);
                        if (newVolLevel > oldVolLevel) {
                            avrcpVolumeUpNative(path);
                        } else if (newVolLevel < oldVolLevel) {
                            avrcpVolumeDownNative(path);
                        }
                    }
                }
            }

            /// M:
            handleFMandWifiAction(intent);
        }
    };

    private boolean isPhoneDocked(BluetoothDevice device) {
        // This works only because these broadcast intents are "sticky"
        Intent i = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_DOCK_EVENT));
        if (i != null) {
            int state = i.getIntExtra(Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_UNDOCKED);
            if (state != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                BluetoothDevice dockDevice = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (dockDevice != null && device.equals(dockDevice)) {
                    return true;
                }
            }
        }
        return false;
    }

    public BluetoothA2dpService(Context context, BluetoothService bluetoothService) {
        mContext = context;
        //the line is for a2dp ut.
        sInstance = this;
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BluetoothA2dpService");

        mIntentBroadcastHandler = new IntentBroadcastHandler();

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mBluetoothService = bluetoothService;
        if (mBluetoothService == null) {
            throw new RuntimeException("Platform does not support Bluetooth");
        }

        if (!initNative()) {
            throw new RuntimeException("Could not init BluetoothA2dpService");
        }

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mIntentFilter.addAction(AudioManager.VOLUME_CHANGED_ACTION);

        /// M:
        addFmAndWifiActions();

        mContext.registerReceiver(mReceiver, mIntentFilter);

        mAudioDevices = new HashMap<BluetoothDevice, Integer>();

        if (mBluetoothService.isEnabled())
            onBluetoothEnable();
        mTargetA2dpState = -1;
        mBluetoothService.setA2dpService(this);

        /// M:
        initFmOverBtFeature();
        
        mPriorityLock = new ReentrantLock();
        
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            cleanupNative();
        } finally {
            super.finalize();
        }
    }

    private int convertBluezSinkStringToState(String value) {
        if (value.equalsIgnoreCase("disconnected"))
            return BluetoothA2dp.STATE_DISCONNECTED;
        if (value.equalsIgnoreCase("connecting"))
            return BluetoothA2dp.STATE_CONNECTING;
        if (value.equalsIgnoreCase("connected"))
            return BluetoothA2dp.STATE_CONNECTED;
        if (value.equalsIgnoreCase("playing"))
            return BluetoothA2dp.STATE_PLAYING;

        /// M: @{
        if (value.equalsIgnoreCase("fmstartfailed")) {
            return BluetoothA2dpService.FM_START_FAILED;
        }
        /// @}

        return -1;
    }

    private boolean isSinkDevice(BluetoothDevice device) {
        ParcelUuid[] uuids = mBluetoothService.getRemoteUuids(device.getAddress());
        if (uuids != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSink)) {
            return true;
        }
        return false;
    }

    private synchronized void addAudioSink(BluetoothDevice device) {
        if (mAudioDevices.get(device) == null) {
            mAudioDevices.put(device, BluetoothA2dp.STATE_DISCONNECTED);
        }
    }

    private synchronized void onBluetoothEnable() {
        String devices = mBluetoothService.getProperty("Devices", true);

        /// M:
        sSinkCount = 0;

        if (devices != null) {
            String [] paths = devices.split(",");
            for (String path: paths) {
                String address = mBluetoothService.getAddressFromObjectPath(path);
                BluetoothDevice device = mAdapter.getRemoteDevice(address);
                ParcelUuid[] remoteUuids = mBluetoothService.getRemoteUuids(address);
                if (remoteUuids != null)
                    if (BluetoothUuid.containsAnyUuid(remoteUuids,
                            new ParcelUuid[] {BluetoothUuid.AudioSink,
                                                BluetoothUuid.AdvAudioDist})) {
                        addAudioSink(device);
                    }
                }
        }
        mAudioManager.setParameters(BLUETOOTH_ENABLED+"=true");
        mAudioManager.setParameters("A2dpSuspended=false");

        /// M:
        startNative();

        mContext.startService(new Intent(AVRCP_SERVICE_CLASS) ); //AVRCP
    }

    private synchronized void onBluetoothDisable() {
        if (!mAudioDevices.isEmpty()) {
            BluetoothDevice[] devices = new BluetoothDevice[mAudioDevices.size()];
            devices = mAudioDevices.keySet().toArray(devices);
            for (BluetoothDevice device : devices) {
                int state = getConnectionState(device);
                switch (state) {
                    case BluetoothA2dp.STATE_CONNECTING:
                    case BluetoothA2dp.STATE_CONNECTED:
                    case BluetoothA2dp.STATE_PLAYING:

                        /// M: @{
                        if (mFmOverBtOn) {
                            mFmOverBtOn = false;
                        }
                        /// @}
                        handleSinkStateChange(device, state, BluetoothA2dp.STATE_DISCONNECTED);
                        disconnectSinkNative(mBluetoothService.getObjectPathFromAddress(
                                device.getAddress()));
                        
                        break;
                    case BluetoothA2dp.STATE_DISCONNECTING:
                        handleSinkStateChange(device, BluetoothA2dp.STATE_DISCONNECTING,
                                              BluetoothA2dp.STATE_DISCONNECTED);
                        break;
                }
            }
            mAudioDevices.clear();
        }

        /// M: @{
        // there may ANR if not set A2dpSuspended=true, because
        // BLUETOOTH_ENABLED is not set success and a2dp_write() will be called
        // and wait from mmi timeout 3s 3 times
        // After set A2dpSuspended=true, A2dpAudioInterface.cpp also must modify
        // AutoLock start place on write(), or sometimes BLUETOOTH_ENABLED is
        // still not set success
        mAudioManager.setParameters("A2dpSuspended=true");
        /// @}

        mAudioManager.setParameters(BLUETOOTH_ENABLED + "=false");

        /// M:
        stopNative();

        mContext.stopService(new Intent(AVRCP_SERVICE_CLASS) ); //AVRCP
    }

    private synchronized boolean isConnectSinkFeasible(BluetoothDevice device) {
        if (!mBluetoothService.isEnabled() || !isSinkDevice(device) ||
                getPriority(device) == BluetoothA2dp.PRIORITY_OFF) {
            return false;
        }

        addAudioSink(device);

        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        if (path == null) {
            return false;
        }
        return true;
    }

    public synchronized boolean isA2dpPlaying(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
            "Need BLUETOOTH_ADMIN permission");
        if (DBG) log("isA2dpPlaying(" + device + ")");
        if (device.equals(mPlayingA2dpDevice)) return true;
        return false;
    }

    public synchronized boolean connect(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                                "Need BLUETOOTH_ADMIN permission");
        if (DBG) log("connectSink(" + device + ")");
        if (!isConnectSinkFeasible(device)) return false;

        for (BluetoothDevice sinkDevice : mAudioDevices.keySet()) {
            if (getConnectionState(sinkDevice) != BluetoothProfile.STATE_DISCONNECTED) {
                disconnect(sinkDevice);
            }
        }
        setPriority(device,BluetoothA2dp.PRIORITY_AUTO_CONNECT);
        return mBluetoothService.connectSink(device.getAddress());
    }

    public synchronized boolean connectSinkInternal(BluetoothDevice device) {
        if (!mBluetoothService.isEnabled()) return false;

        int state = mAudioDevices.get(device);

        // ignore if there are any active sinks
        if (getDevicesMatchingConnectionStates(new int[] {
                BluetoothA2dp.STATE_CONNECTING,
                BluetoothA2dp.STATE_CONNECTED,
                BluetoothA2dp.STATE_DISCONNECTING}).size() != 0) {
            return false;
        }

        switch (state) {
        case BluetoothA2dp.STATE_CONNECTED:
        case BluetoothA2dp.STATE_DISCONNECTING:
            return false;
        case BluetoothA2dp.STATE_CONNECTING:
            return true;
        }

        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());

        // State is DISCONNECTED and we are connecting.
        if (getPriority(device) < BluetoothA2dp.PRIORITY_AUTO_CONNECT) {
            setPriority(device, BluetoothA2dp.PRIORITY_AUTO_CONNECT);
        }
        handleSinkStateChange(device, state, BluetoothA2dp.STATE_CONNECTING);

        if (!connectSinkNative(path)) {
            // Restore previous state
            handleSinkStateChange(device, mAudioDevices.get(device), state);
            return false;
        }
        return true;
    }

    private synchronized boolean isDisconnectSinkFeasible(BluetoothDevice device) {
        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        if (path == null) {
            return false;
        }

        int state = getConnectionState(device);
        switch (state) {
        case BluetoothA2dp.STATE_DISCONNECTED:
        case BluetoothA2dp.STATE_DISCONNECTING:
            return false;
        }
        return true;
    }

    public synchronized boolean disconnect(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                                "Need BLUETOOTH_ADMIN permission");
        if (DBG) log("disconnectSink(" + device + ")");
        if (!isDisconnectSinkFeasible(device)) return false;
        /// M: {@
        if (mFmOverBtOn) {
            mFmOverBtOn = false;
        }
        /// @}
        
        return mBluetoothService.disconnectSink(device.getAddress());
    }

    public synchronized boolean disconnectSinkInternal(BluetoothDevice device) {
        int state = getConnectionState(device);
        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());

        switch (state) {
            case BluetoothA2dp.STATE_DISCONNECTED:
            case BluetoothA2dp.STATE_DISCONNECTING:
                return false;
        }
        // State is CONNECTING or CONNECTED or PLAYING
        handleSinkStateChange(device, state, BluetoothA2dp.STATE_DISCONNECTING);
        if (!disconnectSinkNative(path)) {
            // Restore previous state
            handleSinkStateChange(device, mAudioDevices.get(device), state);
            return false;
        }
        return true;
    }

    public synchronized boolean suspendSink(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                            "Need BLUETOOTH_ADMIN permission");
        if (DBG) log("suspendSink(" + device + "), mTargetA2dpState: "+mTargetA2dpState);
        if (device == null || mAudioDevices == null) {
            return false;
        }
        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        Integer state = mAudioDevices.get(device);
        if (path == null || state == null) {
            return false;
        }

        /// M: @{
        // modify for CR: ALPS00248346, force to suspend a2dp
        if (state <= BluetoothA2dp.STATE_CONNECTING) {
            return false;
        }
        /// @}

        mAudioManager.setParameters("A2dpSuspended=true");
        return true;
       // mTargetA2dpState = BluetoothA2dp.STATE_CONNECTED;
       // return checkSinkSuspendState(state.intValue());
    }

    public synchronized boolean resumeSink(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                            "Need BLUETOOTH_ADMIN permission");
        if (DBG) log("resumeSink(" + device + "), mTargetA2dpState: "+mTargetA2dpState);
        if (device == null || mAudioDevices == null) {
            return false;
        }
        String path = mBluetoothService.getObjectPathFromAddress(device.getAddress());
        Integer state = mAudioDevices.get(device);
        if (path == null || state == null) {
            return false;
        }
        mTargetA2dpState = BluetoothA2dp.STATE_PLAYING;
        return checkSinkSuspendState(state.intValue());
    }

    public synchronized int getConnectionState(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer state = mAudioDevices.get(device);
        if (state == null)
            return BluetoothA2dp.STATE_DISCONNECTED;
        return state;
    }

    public synchronized List<BluetoothDevice> getConnectedDevices() {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        List<BluetoothDevice> sinks = getDevicesMatchingConnectionStates(
                new int[] {BluetoothA2dp.STATE_CONNECTED});
        return sinks;
    }

    public synchronized List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList<BluetoothDevice> sinks = new ArrayList<BluetoothDevice>();
        for (BluetoothDevice device: mAudioDevices.keySet()) {
            int sinkState = getConnectionState(device);
            for (int state : states) {
                if (state == sinkState) {
                    sinks.add(device);
                    break;
                }
            }
        }
        return sinks;
    }

    public int getPriority(BluetoothDevice device) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.getBluetoothA2dpSinkPriorityKey(device.getAddress()),
                BluetoothA2dp.PRIORITY_UNDEFINED);
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        boolean ret = false;
    	  
        mPriorityLock.lock();
    	  
        try {
            mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                                "Need BLUETOOTH_ADMIN permission");
            ret = Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.getBluetoothA2dpSinkPriorityKey(device.getAddress()), priority);
        } finally {
            mPriorityLock.unlock();
        }
        return ret;
    }

    public synchronized boolean allowIncomingConnect(BluetoothDevice device, boolean value) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM,
                                                "Need BLUETOOTH_ADMIN permission");
        String address = device.getAddress();
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }
        Integer data = mBluetoothService.getAuthorizationAgentRequestData(address);
        if (data == null) {
            Log.w(TAG, "allowIncomingConnect(" + device + ") called but no native data available");
            return false;
        }
        log("allowIncomingConnect: A2DP: " + device + ":" + value);
        return mBluetoothService.setAuthorizationNative(address, value, data.intValue());
    }

    /**
     * Called by native code on a PropertyChanged signal from
     * org.bluez.AudioSink.
     *
     * @param path the object path for the changed device
     * @param propValues a string array containing the key and one or more
     *  values.
     */
    private synchronized void onSinkPropertyChanged(String path, String[] propValues) {
        if (!mBluetoothService.isEnabled()) {

            /// M: @{
            if (isA2dpDisconnect(propValues)) {
                return;
            }
            /// @}

        }

        String name = propValues[0];
        String address = mBluetoothService.getAddressFromObjectPath(path);
        if (address == null) {
            Log.e(TAG, "onSinkPropertyChanged: Address of the remote device in null");
            return;
        }

        BluetoothDevice device = mAdapter.getRemoteDevice(address);

        // / M: @{
        if (isDisconnectSinkNative(path, device)) {
            return;
        }
        // / @}

        if (name.equals(PROPERTY_STATE)) {
            int state = convertBluezSinkStringToState(propValues[1]);
            log("A2DP: onSinkPropertyChanged newState is: " + state + "mPlayingA2dpDevice: " + mPlayingA2dpDevice);

            if (mAudioDevices.get(device) == null) {
                // This is for an incoming connection for a device not known to us.
                // We have authorized it and bluez state has changed.

                /// M: @{
                if (state == BluetoothA2dp.STATE_DISCONNECTED
                        || state == BluetoothA2dp.STATE_DISCONNECTING) {
                    mPlayingA2dpDevice = null;
                    handleSinkStateChange(device, BluetoothA2dp.STATE_DISCONNECTING, state);
                    log("A2DP: onSinkPropertyChanged 2 newState is: " + state
                            + " mPlayingA2dpDevice: " + mPlayingA2dpDevice);
                    return;
                }
                /// @}

                /* notify bluetooth service that a2dp is incoming */
                mBluetoothService.notifyIncomingA2dpConnection(address, false);
				
                addAudioSink(device);
                handleSinkStateChange(device, BluetoothA2dp.STATE_DISCONNECTED, state);
                
            } else {

                /// M: 
                mA2dpState = state;

                if (state == BluetoothA2dp.STATE_PLAYING && mPlayingA2dpDevice == null) {
                   mPlayingA2dpDevice = device;
                   handleSinkPlayingStateChange(device, state, BluetoothA2dp.STATE_NOT_PLAYING);
                } else if (state == BluetoothA2dp.STATE_CONNECTED && mPlayingA2dpDevice != null) {
                    mPlayingA2dpDevice = null;
                    handleSinkPlayingStateChange(device, BluetoothA2dp.STATE_NOT_PLAYING,
                        BluetoothA2dp.STATE_PLAYING);

                    /// M: 
                    mA2dpState = BluetoothA2dp.STATE_NOT_PLAYING;

                } else {
                   mPlayingA2dpDevice = null;
                   int prevState = mAudioDevices.get(device);
                   
                   /* change the position */
                   if (prevState == BluetoothA2dp.STATE_DISCONNECTED && 
                       state == BluetoothA2dp.STATE_CONNECTED) {
                       /* notify bluetooth service that a2dp is incoming */
                       log("a2dp: notifyIncomingA2dpConnection.....");
                       mBluetoothService.notifyIncomingA2dpConnection(address, false);
                  }
				   
                   handleSinkStateChange(device, prevState, state);
                }
            }
            
            
            /// M: 
            handleWifiOnSinkStateChange(state);
        }
    }

    private void handleSinkStateChange(BluetoothDevice device, int prevState, int state) {

        /// M:
        state = handleFMSinkStateChange(state);

        if (state != prevState) {

            /// M:
            switchStatusOnSinkStateChange(state);

            mAudioDevices.put(device, state);

            checkSinkSuspendState(state);
            mTargetA2dpState = -1;

            if (getPriority(device) > BluetoothA2dp.PRIORITY_OFF &&
                    state == BluetoothA2dp.STATE_CONNECTED) {
                // We have connected or attempting to connect.
                // Bump priority
                setPriority(device, BluetoothA2dp.PRIORITY_AUTO_CONNECT);
                // We will only have 1 device with AUTO_CONNECT priority
                // To be backward compatible set everyone else to have PRIORITY_ON
                adjustOtherSinkPriorities(device);
            }

            
            
            int delay = mAudioManager.setBluetoothA2dpDeviceConnectionState(device, state);

            mWakeLock.acquire();
            mIntentBroadcastHandler.sendMessageDelayed(mIntentBroadcastHandler.obtainMessage(
                                                            MSG_CONNECTION_STATE_CHANGED,
                                                            prevState,
                                                            state,
                                                            device),
                                                       delay);
            
        }
    }

    private void handleSinkPlayingStateChange(BluetoothDevice device, int state, int prevState) {
        Intent intent = new Intent(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, state);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcast(intent, BLUETOOTH_PERM);

        if (DBG) log("A2DP Playing state : device: " + device + " State:" + prevState + "->" + state);

        /// M:
        handleFmSinkPlayingStateChange(state);
    }

    private void adjustOtherSinkPriorities(BluetoothDevice connectedDevice) {
        for (BluetoothDevice device : mAdapter.getBondedDevices()) {
            if (getPriority(device) >= BluetoothA2dp.PRIORITY_AUTO_CONNECT &&
                !device.equals(connectedDevice)) {
                setPriority(device, BluetoothA2dp.PRIORITY_ON);
            }
        }
    }

    private boolean checkSinkSuspendState(int state) {
        boolean result = true;

        // / M: @{
        log("[a2dp] checkSinkSuspendState(" + mTargetA2dpState + "), State: " + state);
        if (state <= BluetoothA2dp.STATE_CONNECTING) {
            return false;
        }

        if (mPlayingA2dpDevice != null && mTargetA2dpState == BluetoothA2dp.STATE_CONNECTED) {
            mAudioManager.setParameters("A2dpSuspended=true");
        } else if (mTargetA2dpState == BluetoothA2dp.STATE_PLAYING) {
            mAudioManager.setParameters("A2dpSuspended=false");
        } else {
            result = false;
        }
        // / @}

        /// M: commented android original codes {@
        /*
         * if (state != mTargetA2dpState) { if (state ==
         * BluetoothA2dp.STATE_PLAYING && mTargetA2dpState ==
         * BluetoothA2dp.STATE_CONNECTED) {
         * mAudioManager.setParameters("A2dpSuspended=true"); } else if (state
         * == BluetoothA2dp.STATE_CONNECTED && mTargetA2dpState ==
         * BluetoothA2dp.STATE_PLAYING) {
         * mAudioManager.setParameters("A2dpSuspended=false"); } else { result =
         * false; } }
         */

        /// @}

        return result;
    }

    /**
     * Called by native code for the async response to a Connect
     * method call to org.bluez.AudioSink.
     *
     * @param deviceObjectPath the object path for the connecting device
     * @param result true on success; false on error
     */
    private void onConnectSinkResult(String deviceObjectPath, boolean result) {
        // If the call was a success, ignore we will update the state
        // when we a Sink Property Change
        if (!result) {
            if (deviceObjectPath != null) {
                String address = mBluetoothService.getAddressFromObjectPath(deviceObjectPath);
                if (address == null) return;
                BluetoothDevice device = mAdapter.getRemoteDevice(address);
                int state = getConnectionState(device);
                handleSinkStateChange(device, state, BluetoothA2dp.STATE_DISCONNECTED);
            }
        }
    }

    /** Handles A2DP connection state change intent broadcasts. */
    private class IntentBroadcastHandler extends Handler {

        private void onConnectionStateChanged(BluetoothDevice device, int prevState, int state) {
            Intent intent = new Intent(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
            intent.putExtra(BluetoothProfile.EXTRA_STATE, state);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            mContext.sendBroadcast(intent, BLUETOOTH_PERM);

            if (DBG) log("A2DP state : device: " + device + " State:" + prevState + "->" + state);

            mBluetoothService.sendConnectionStateChange(device, BluetoothProfile.A2DP, state,
                                                        prevState);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECTION_STATE_CHANGED:
                    onConnectionStateChanged((BluetoothDevice) msg.obj, msg.arg1, msg.arg2);
                    mWakeLock.release();
                    break;
            }
        }
    }

    @Override
    protected synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, TAG);

        if (mAudioDevices.isEmpty()) return;
        pw.println("Cached audio devices:");
        for (BluetoothDevice device : mAudioDevices.keySet()) {
            int state = mAudioDevices.get(device);
            pw.println(device + " " + BluetoothA2dp.stateToString(state));
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private native boolean initNative();
    private native void cleanupNative();
    private synchronized native boolean connectSinkNative(String path);
    private synchronized native boolean disconnectSinkNative(String path);
    private synchronized native boolean suspendSinkNative(String path);
    private synchronized native boolean resumeSinkNative(String path);
    private synchronized native Object []getSinkPropertiesNative(String path);
    private synchronized native boolean avrcpVolumeUpNative(String path);
    private synchronized native boolean avrcpVolumeDownNative(String path);
    
    /// M: @{
    private native void startNative();

    private native void stopNative();

    private native int fmSendStartReqNative();

    private native void fmSendStopReqNative();

    private native void decA2dpThroughput4WifiOnNative();

    private native void incA2dpThroughput4WifiOffNative();

    public boolean setAudioPathToAudioTrack(IBinder cb) {
        FMServiceDeathHandler hdl = new FMServiceDeathHandler(cb);

        try {
            cb.linkToDeath(hdl, 0);
        } catch (RemoteException e) {
            // client has already died!
            log("[a2dp service]setAudioPathToAudioTrack could not link to " + cb + " binder death.");
            return false;
        }

        return true;
    }

    private class FMServiceDeathHandler implements IBinder.DeathRecipient {
        private IBinder mCb; // To be notified of client's death

        FMServiceDeathHandler(IBinder cb) {
            mCb = cb;
        }

        public void binderDied() {
            debugLog("FMServiceDeathHandler::binderDied");

            if (mFmOverBtOn) {
                mFmOverBtOn = false;

                if (mFmOverBtMode) {
                    stopFm();
                }
            } else {
                debugLog("FM was Power down,ignore.");
            }
        }

        public IBinder getBinder() {
            return mCb;
        }
    }

    private void handleFMandWifiAction(Intent intent) {
        String action = intent.getAction();
        if (action.equals(MSG_FM_POWER_UP)) { // For FM Over Bt
            debugLog("FM Power On!");

            if (0 == SystemProperties.getInt("bt.fmoverbt", 0)) {
                debugLog("[A2DP] BT chip does not support Fm Over Bt, return.");

                mFmResult = FMSTART_FAILED;
                fmSendIntent();
                return;
            }
			
            if (mA2dpDisconnecting == 1) {
                debugLog("A2dp is disconnecting,return.");

                mFmResult = FMSTART_FAILED;
                fmSendIntent();
                return;
            }
            if (!mFmOverBtOn && !mFmStartReq) {
                // mFMoverBTon = true;

                if (mFmOverBtMode) {
                    if (fmThroughPath() == FM_VIA_CONTROLLER) {
                        debugLog("FM_VIA_CONTROLLER");
                        fmOverBtViaController();
                    } else {
                        debugLog("FM_VIA_HOST");
                        fmOverBtViaHost();
                    }
                }
            } else {
                debugLog("FM was Power On,ignore.");

                mFmResult = FMSTART_ALREADY;
                fmSendIntent();
            }
        } else if (action.equals(MSG_FM_POWER_DOWN)) {
            debugLog("FM Power Off!");
            if (mFmOverBtOn) {
                mFmOverBtOn = false;

                if (mFmOverBtMode) {
                    stopFm();
                }
            } else {
                debugLog("FM was Power down,ignore.");
            }
        } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            debugLog("Receive WifiManager.WIFI_STATE_CHANGED_ACTION.");
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            if (state == WifiManager.WIFI_STATE_ENABLED) {
                debugLog("Wifi is on.");
                decA2dpThroughput4WifiOn();
            } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                debugLog("Wifi is off.");
                incA2dpThroughput4WifiOff();
            }
        }
    }

    private void addFmAndWifiActions() {
        mIntentFilter.addAction(MSG_FM_POWER_UP);
        mIntentFilter.addAction(MSG_FM_POWER_DOWN);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    private void initFmOverBtFeature() {
        // XH, For FMOverBt Feature
        mA2dpState = BluetoothA2dp.STATE_DISCONNECTED;
        mFmOverBtMode = true;
        mA2dpDisconnecting = 0;
        mFmOverBtOn = false;
    }

    private boolean isA2dpDisconnect(String[] propValues) {
        String name = propValues[0];
        boolean disconnect = false;
        if (name.equals(PROPERTY_STATE)) {
            int state = convertBluezSinkStringToState(propValues[1]);
            if (state != BluetoothA2dp.STATE_DISCONNECTED && state != BluetoothA2dp.STATE_DISCONNECTING) {
                disconnect = true;
            }
        }
        return disconnect;
    }

    private boolean isDisconnectSinkNative(String path, BluetoothDevice device) {
        boolean isDisconnectSink = false;
        if (getPriority(device) == BluetoothA2dp.PRIORITY_OFF
                && (mAudioDevices.get(device) == null || mAudioDevices.get(device) == BluetoothA2dp.STATE_DISCONNECTED)) {
            disconnectSinkNative(path);
            isDisconnectSink = true;
            debugLog("A2DP: onSinkPropertyChanged Priority is off, disconnect device ");
        }
        return isDisconnectSink;
    }

    private void handleFmSinkPlayingStateChange(int state) {
        if (state == BluetoothA2dpService.FM_START_FAILED) {
            Log.e(TAG, "FM Start Req Failed ");
            mFmResult = FMSTART_FAILED;
            if (mFmStartReq) {
                mFmOverBtOn = false;
                fmSendIntent();
                mFmStartReq = false;
            }
        } else if (state == BluetoothA2dp.STATE_PLAYING) {
            mFmResult = FMSTART_SUCCESS;
            debugLog("[Yi] hdl sink state start req:" + mFmStartReq + ":");
            if (mFmStartReq) {
                mFmOverBtOn = true;
                fmSendIntent();
                mFmStartReq = false;
            }
        }
    }

    private void fmOverBtViaController() {
        debugLog("fmOverBtViaController On");
        if (mA2dpState < BluetoothA2dp.STATE_CONNECTED) {
            debugLog("A2dp is not connected!");
            mFmOverBtOn = false;
            mFmResult = FMSTART_FAILED;
            fmSendIntent();
            return;
        }
        if (mA2dpState >= BluetoothA2dp.STATE_CONNECTED) {
            if (mA2dpState == BluetoothA2dp.STATE_CONNECTED) {
                debugLog("mA2dpState == BluetoothA2dp.STATE_CONNECTED");
            } else {
                debugLog("mA2dpState == BluetoothA2dp.STATE_PLAYING");
            }
            if (fmSendStartReqNative() == 0) {
                Log.e(TAG, "[A2DP]fmSendStartReqNative fail!");
                mFmOverBtOn = false;
                mFmResult = FMSTART_FAILED;
                fmSendIntent();
                return;
            }
            mFmStartReq = true;
        }
    }

    private void stopFm() {
        debugLog("stop FM");

        if (mA2dpState == BluetoothA2dp.STATE_DISCONNECTED || mA2dpState == BluetoothA2dp.STATE_DISCONNECTING) {
            return;
        }
        if (mA2dpState == BluetoothA2dp.STATE_PLAYING) {
            fmSendStopReqNative();
        } else {
            mFmOverBtOn = false;
        }
    }

    private void fmOverBtViaHost() {
        debugLog("fmOverBtViaHost On");

        Intent intent = new Intent(BluetoothA2dpService.ACTION_FM_OVER_BT_HOST);
        mContext.sendBroadcast(intent);
    }

    private int fmThroughPath() {
        // currrently default choose controller
        return FM_VIA_CONTROLLER;
    }

    private void fmSendIntent() {
        Intent intent = new Intent(BluetoothA2dpService.ACTION_FM_OVER_BT_CONTROLLER);
        intent.putExtra(BluetoothA2dpService.EXTRA_RESULT_STATE, mFmResult);
        mContext.sendBroadcast(intent);
    }

    public int getState() {
        debugLog("mA2dpState" + mA2dpState);

        if (!mBluetoothService.isEnabled()) {
            mA2dpState = BluetoothA2dp.STATE_DISCONNECTED;
            return BluetoothA2dp.STATE_DISCONNECTED;
        } else {
            return mA2dpState;
        }
    }

    private void decA2dpThroughput4WifiOn() {
        decA2dpThroughput4WifiOnNative();
    }

    private void incA2dpThroughput4WifiOff() {
        incA2dpThroughput4WifiOffNative();
    }

    private void handleWifiOnSinkStateChange(int state) {
        // should decrease a2dp throughtput when wifi is on and state equals
        // playing
        if (state == BluetoothA2dp.STATE_PLAYING) {

            mWM = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            int wifistate = mWM.getWifiState();
            debugLog("A2DP Wifi state:." + wifistate);
            if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                debugLog("A2DP Wifi is on.");
                decA2dpThroughput4WifiOn();
            }
        }
    }

    private int handleFMSinkStateChange(int state) {
        if (state == BluetoothA2dpService.FM_START_FAILED) {
            Log.e(TAG, "FM Start Req Failed ");

            mFmResult = FMSTART_FAILED;
            state = BluetoothA2dp.STATE_DISCONNECTED;

            if (mFmStartReq) {
                mFmOverBtOn = false;
                fmSendIntent();
                mFmStartReq = false;
            }
        } else if (state == BluetoothA2dp.STATE_PLAYING) {
            mFmResult = FMSTART_SUCCESS;
            if (mFmStartReq) {
                mFmOverBtOn = true;
                fmSendIntent();
                mFmStartReq = false;
            }
        }
        return state;
    }

    private void switchStatusOnSinkStateChange(int state) {
        if (state == BluetoothA2dp.STATE_DISCONNECTING) {
            mA2dpDisconnecting = 1;
        }
        if (state == BluetoothA2dp.STATE_DISCONNECTED) {
            mA2dpDisconnecting = 0;
        }

        if (state <= BluetoothA2dp.STATE_CONNECTED) {
            if (mFmOverBtOn) {
                mFmOverBtOn = false;
            }
            if (mFmStartReq) {
                mFmStartReq = false;
            }
        }

        if (state == BluetoothA2dp.STATE_DISCONNECTED || state == BluetoothA2dp.STATE_DISCONNECTING) {
            sSinkCount--;
        } else if (state == BluetoothA2dp.STATE_CONNECTED) {
            sSinkCount++;
        }
    }

    private static void debugLog(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }

    // / @}

}
