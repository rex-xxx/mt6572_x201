/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.net;

import com.android.internal.util.Protocol;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfoInternal;
import android.net.NetworkUtils;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.mediatek.xlog.Xlog;

/**
 * StateMachine that interacts with the native DHCP client and can talk to
 * a controller that also needs to be a StateMachine
 *
 * The Dhcp state machine provides the following features:
 * - Wakeup and renewal using the native DHCP client  (which will not renew
 *   on its own when the device is in suspend state and this can lead to device
 *   holding IP address beyond expiry)
 * - A notification right before DHCP request or renewal is started. This
 *   can be used for any additional setup before DHCP. For example, wifi sets
 *   BT-Wifi coex settings right before DHCP is initiated
 *
 * @hide
 */
public class DhcpStateMachine extends StateMachine {

    private static final String TAG = "DhcpStateMachine";
    private static final boolean DBG = true;


    /* A StateMachine that controls the DhcpStateMachine */
    private StateMachine mController;

    private Context mContext;
    private BroadcastReceiver mBroadcastReceiver;
    private AlarmManager mAlarmManager;
    private PendingIntent mDhcpRenewalIntent;
    private PowerManager.WakeLock mDhcpRenewWakeLock;
    private static final String WAKELOCK_TAG = "DHCP";

    //Remember DHCP configuration from first request
    private DhcpInfoInternal mDhcpInfo;

    private static final int DHCP_RENEW = 0;
    private static final String ACTION_DHCP_RENEW = "android.net.wifi.DHCP_RENEW";
    private static final String ACTION_DHCPV6_RENEW = "android.net.wifi.DHCPV6_RENEW";

    //Used for sanity check on setting up renewal
    private static final int MIN_RENEWAL_TIME_SECS = 5 * 60;  // 5 minutes

    private enum DhcpAction {
        START,
        RENEW
    };

    private String mInterfaceName;
    private boolean mRegisteredForPreDhcpNotification = false;

    private static final int BASE = Protocol.BASE_DHCP;

    /* Commands from controller to start/stop DHCP */
    public static final int CMD_START_DHCP                  = BASE + 1;
    public static final int CMD_STOP_DHCP                   = BASE + 2;
    public static final int CMD_RENEW_DHCP                  = BASE + 3;

    /* Notification from DHCP state machine prior to DHCP discovery/renewal */
    public static final int CMD_PRE_DHCP_ACTION             = BASE + 4;
    /* Notification from DHCP state machine post DHCP discovery/renewal. Indicates
     * success/failure */
    public static final int CMD_POST_DHCP_ACTION            = BASE + 5;
    /* Notification from DHCP state machine before quitting */
    public static final int CMD_ON_QUIT                     = BASE + 6;

    /* Command from controller to indicate DHCP discovery/renewal can continue
     * after pre DHCP action is complete */
    public static final int CMD_PRE_DHCP_ACTION_COMPLETE    = BASE + 7;

    /* Message.arg1 arguments to CMD_POST_DHCP notification */
    public static final int DHCP_SUCCESS = 1;
    public static final int DHCP_FAILURE = 2;

    private State mDefaultState = new DefaultState();
    private State mStoppedState = new StoppedState();
    private State mWaitBeforeStartState = new WaitBeforeStartState();
    private State mRunningState = new RunningState();
    private State mWaitBeforeRenewalState = new WaitBeforeRenewalState();

    public static final int DHCPV4 = 1;
    public static final int DHCPV6 = 2;
    private boolean mIsDhcpV6 = false;
    private boolean mIsRegistered = false;

    public void setForDhcpV6(boolean isDhcpV6) {
        mIsDhcpV6 = isDhcpV6;
        Intent dhcpRenewalIntent = new Intent(ACTION_DHCPV6_RENEW, null);
        mDhcpRenewalIntent = PendingIntent.getBroadcast(mContext, DHCP_RENEW + 1, dhcpRenewalIntent, 0);
        if (mIsRegistered) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mIsRegistered = false;
        }
        Xlog.d(TAG, "Register receiver for V6");
        mContext.registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_DHCPV6_RENEW));
        mIsRegistered = true;
    }

    private DhcpStateMachine(Context context, StateMachine controller, String intf) {
        super(TAG);

        mContext = context;
        mController = controller;
        mInterfaceName = intf;

        mAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        Intent dhcpRenewalIntent = new Intent(ACTION_DHCP_RENEW, null);
        mDhcpRenewalIntent = PendingIntent.getBroadcast(mContext, DHCP_RENEW, dhcpRenewalIntent, 0);

        PowerManager powerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        mDhcpRenewWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
        mDhcpRenewWakeLock.setReferenceCounted(false);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //DHCP renew
                if (DBG) Log.d(TAG, "Sending a DHCP renewal " + this + ", mIsDhcpV6:" + mIsDhcpV6);
                //Lock released after 40s in worst case scenario
                mDhcpRenewWakeLock.acquire(40000);
                sendMessage(CMD_RENEW_DHCP);
            }
        };
        mContext.registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_DHCP_RENEW));
        mIsRegistered = true;

        addState(mDefaultState);
            addState(mStoppedState, mDefaultState);
            addState(mWaitBeforeStartState, mDefaultState);
            addState(mRunningState, mDefaultState);
            addState(mWaitBeforeRenewalState, mDefaultState);

        setInitialState(mStoppedState);
    }

    public static DhcpStateMachine makeDhcpStateMachine(Context context, StateMachine controller,
            String intf) {
        DhcpStateMachine dsm = new DhcpStateMachine(context, controller, intf);
        dsm.start();
        return dsm;
    }

    /**
     * This sends a notification right before DHCP request/renewal so that the
     * controller can do certain actions before DHCP packets are sent out.
     * When the controller is ready, it sends a CMD_PRE_DHCP_ACTION_COMPLETE message
     * to indicate DHCP can continue
     *
     * This is used by Wifi at this time for the purpose of doing BT-Wifi coex
     * handling during Dhcp
     */
    public void registerForPreDhcpNotification() {
        mRegisteredForPreDhcpNotification = true;
    }

    /**
     * Quit the DhcpStateMachine.
     *
     * @hide
     */
    public void doQuit() {
        quit();
    }

    protected void onQuitting() {
        mController.obtainMessage(CMD_ON_QUIT, mIsDhcpV6 ? DHCPV6 : DHCPV4, 0).sendToTarget();
    }

    class DefaultState extends State {
        @Override
        public void exit() {
            if (mIsRegistered) {
                mContext.unregisterReceiver(mBroadcastReceiver);
                mIsRegistered = false;
            }
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_RENEW_DHCP:
                    Log.e(TAG, "Error! Failed to handle a DHCP renewal on " + mInterfaceName);
                    mDhcpRenewWakeLock.release();
                    break;
                default:
                    Log.e(TAG, "Error! unhandled message  " + message);
                    break;
            }
            return HANDLED;
        }
    }


    class StoppedState extends State {
        @Override
        public void enter() {
            if (DBG) Log.d(TAG, getName() + "\n");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retValue = HANDLED;
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_START_DHCP:
                    if (mRegisteredForPreDhcpNotification) {
                        /* Notify controller before starting DHCP */
                        mController.obtainMessage(CMD_PRE_DHCP_ACTION, mIsDhcpV6 ? DHCPV6 : DHCPV4, 0).sendToTarget();
                        transitionTo(mWaitBeforeStartState);
                    } else {
                        if (runDhcp(DhcpAction.START)) {
                            transitionTo(mRunningState);
                        }
                    }
                    break;
                case CMD_STOP_DHCP:
                    //ignore
                    break;
                default:
                    retValue = NOT_HANDLED;
                    break;
            }
            return retValue;
        }
    }

    class WaitBeforeStartState extends State {
        @Override
        public void enter() {
            if (DBG) Log.d(TAG, getName() + "\n");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retValue = HANDLED;
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_PRE_DHCP_ACTION_COMPLETE:
                    if (runDhcp(DhcpAction.START)) {
                        transitionTo(mRunningState);
                    } else {
                        transitionTo(mStoppedState);
                    }
                    break;
                case CMD_STOP_DHCP:
                    transitionTo(mStoppedState);
                    break;
                case CMD_START_DHCP:
                    //ignore
                    break;
                default:
                    retValue = NOT_HANDLED;
                    break;
            }
            return retValue;
        }
    }

    class RunningState extends State {
        @Override
        public void enter() {
            if (DBG) Log.d(TAG, getName() + "\n");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retValue = HANDLED;
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_STOP_DHCP:
                    mAlarmManager.cancel(mDhcpRenewalIntent);
                    if (!mIsDhcpV6) {
                        if (!NetworkUtils.stopDhcp(mInterfaceName)) {
                            Log.e(TAG, "Failed to stop Dhcp on " + mInterfaceName);
                        }
                    } else {
                        if (!NetworkUtils.stopDhcpv6(mInterfaceName)) {
                            Xlog.e(TAG, "Failed to stop Dhcpv6 on " + mInterfaceName);
                        }
                    }
                    transitionTo(mStoppedState);
                    break;
                case CMD_RENEW_DHCP:
                    if (mRegisteredForPreDhcpNotification) {
                        /* Notify controller before starting DHCP */
                        mController.obtainMessage(CMD_PRE_DHCP_ACTION, mIsDhcpV6 ? DHCPV6 : DHCPV4, 0).sendToTarget();
                        transitionTo(mWaitBeforeRenewalState);
                        //mDhcpRenewWakeLock is released in WaitBeforeRenewalState
                    } else {
                        if (!runDhcp(DhcpAction.RENEW)) {
                            transitionTo(mStoppedState);
                        }
                        mDhcpRenewWakeLock.release();
                    }
                    break;
                case CMD_START_DHCP:
                    //ignore
                    break;
                default:
                    retValue = NOT_HANDLED;
            }
            return retValue;
        }
    }

    class WaitBeforeRenewalState extends State {
        @Override
        public void enter() {
            if (DBG) Log.d(TAG, getName() + "\n");
        }

        @Override
        public boolean processMessage(Message message) {
            boolean retValue = HANDLED;
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_STOP_DHCP:
                    mAlarmManager.cancel(mDhcpRenewalIntent);
                    if (!mIsDhcpV6) {
                        if (!NetworkUtils.stopDhcp(mInterfaceName)) {
                            Log.e(TAG, "Failed to stop Dhcp on " + mInterfaceName);
                        }
                    } else {
                        if (!NetworkUtils.stopDhcpv6(mInterfaceName)) {
                            Xlog.e(TAG, "Failed to stop Dhcpv6 on " + mInterfaceName);
                        }
                    }
                    transitionTo(mStoppedState);
                    break;
                case CMD_PRE_DHCP_ACTION_COMPLETE:
                    if (runDhcp(DhcpAction.RENEW)) {
                       transitionTo(mRunningState);
                    } else {
                       transitionTo(mStoppedState);
                    }
                    break;
                case CMD_START_DHCP:
                    //ignore
                    break;
                default:
                    retValue = NOT_HANDLED;
                    break;
            }
            return retValue;
        }
        @Override
        public void exit() {
            mDhcpRenewWakeLock.release();
        }
    }

    private boolean runDhcp(DhcpAction dhcpAction) {
        boolean success = false;
        DhcpInfoInternal dhcpInfoInternal = new DhcpInfoInternal();

        if (dhcpAction == DhcpAction.START) {
            /* Stop any existing DHCP daemon before starting new */
            if (!mIsDhcpV6) {
                if (!NetworkUtils.stopDhcp(mInterfaceName)) {
                    Xlog.e(TAG, "Failed to stop Dhcp on " + mInterfaceName);
                }
            } else {
                if (!NetworkUtils.stopDhcpv6(mInterfaceName)) {
                    Xlog.e(TAG, "Failed to stop Dhcpv6 on " + mInterfaceName);
                }
            }
            if (DBG) Log.d(TAG, "DHCP" + (mIsDhcpV6 ? "V6" : "V4") + " request on " + mInterfaceName);
            if (!mIsDhcpV6) {
                success = NetworkUtils.runDhcp(mInterfaceName, dhcpInfoInternal);
            } else {
                success = NetworkUtils.runDhcpv6(mInterfaceName, dhcpInfoInternal);
            }
            mDhcpInfo = dhcpInfoInternal;
        } else if (dhcpAction == DhcpAction.RENEW) {
            if (DBG) Log.d(TAG, "DHCP" + (mIsDhcpV6 ? "V6" : "V4") + " renewal on " + mInterfaceName);
            if (!mIsDhcpV6) {
                success = NetworkUtils.runDhcpRenew(mInterfaceName, dhcpInfoInternal);
            } else {
                success = NetworkUtils.runDhcpv6Renew(mInterfaceName, dhcpInfoInternal);
            }
            dhcpInfoInternal.updateFromDhcpRequest(mDhcpInfo);
        }

        if (success) {
            if (DBG) Log.d(TAG, "DHCP" + (mIsDhcpV6 ? "V6" : "V4") + " succeeded on " + mInterfaceName);
            long leaseDuration = dhcpInfoInternal.leaseDuration; //int to long conversion
            Xlog.d(TAG, "dhcpInfoInternal:" + dhcpInfoInternal);

            //Sanity check for renewal
            if (leaseDuration >= 0) {
                if (!mIsDhcpV6) {
                    //TODO: would be good to notify the user that his network configuration is
                    //bad and that the device cannot renew below MIN_RENEWAL_TIME_SECS
                    if (leaseDuration < MIN_RENEWAL_TIME_SECS) {
                        leaseDuration = MIN_RENEWAL_TIME_SECS;
                    }
                    //Do it a bit earlier than half the lease duration time
                    //to beat the native DHCP client and avoid extra packets
                    //48% for one hour lease time = 29 minutes
                    mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() +
                            leaseDuration * 480, //in milliseconds
                            mDhcpRenewalIntent);
                } else {
                    if (leaseDuration < MIN_RENEWAL_TIME_SECS * 0.48) {
                        leaseDuration = (long)(MIN_RENEWAL_TIME_SECS * 0.48);
                    }
                    Xlog.d(TAG, "V6 leaseDuration:" + leaseDuration);
                    mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() +
                            leaseDuration * 1000, //in milliseconds
                            mDhcpRenewalIntent);
                }
            } else {
                //infinite lease time, no renewal needed
            }

            mController.obtainMessage(CMD_POST_DHCP_ACTION, DHCP_SUCCESS, mIsDhcpV6 ? DHCPV6 : DHCPV4, dhcpInfoInternal)
                .sendToTarget();
        } else {
            if (!mIsDhcpV6) {
                Log.e(TAG, "DHCPV4 failed on " + mInterfaceName + ": " +
                    NetworkUtils.getDhcpError());
                NetworkUtils.stopDhcp(mInterfaceName);
            } else {
                Xlog.e(TAG, "DHCPV6 failed on " + mInterfaceName + ": " + NetworkUtils.getDhcpv6Error());
                NetworkUtils.stopDhcpv6(mInterfaceName);
            }
            mController.obtainMessage(CMD_POST_DHCP_ACTION, DHCP_FAILURE, mIsDhcpV6 ? DHCPV6 : DHCPV4)
                .sendToTarget();
        }
        return success;
    }
}
