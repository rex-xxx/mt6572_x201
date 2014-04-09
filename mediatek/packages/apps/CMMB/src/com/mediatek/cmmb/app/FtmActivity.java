/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.cmmb.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.cmmb.app.ChannelListManager.Channel;
import com.mediatek.cmmb.app.ChannelListManager.Program;
//MBBMS Provider's interface
import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.ServerStatus;
//MBBMS Service's interface
import com.mediatek.mbbms.service.CMMBMediaPlayer;
import com.mediatek.mbbms.service.CMMBServiceClient;
import com.mediatek.mbbms.service.MBBMSService;
import com.mediatek.mbbms.service.MBBMSService.LocalBinder;

import java.util.ArrayList;

public class FtmActivity extends Activity implements SurfaceHolder.Callback,
        MediaPlayer.OnErrorListener,MediaPlayer.OnInfoListener
        ,MediaPlayer.OnCompletionListener,MediaPlayer.OnPreparedListener {    
    
    private static final String TAG = "CMMB::FtmActivity";//"CMMB::FtmActivity";

    /*
     * Content of intent
     */
    public static final String INTENT_KEY_MODE = "MODE";
    public static final String INTENT_KEY_FREQ = "FREQ";
    public static final int CMMB_FMODE = 0;    //factory mode
    public static final int CMMB_FTMODE = 1;    // FT mode
    private static final int DEF_FREQ = 530000000;
    
    private int mMode;
    private int mLockFreq = DEF_FREQ;

    // Copied from MediaPlaybackService in the Music Player app. Should be
    // public, but isn't.
    private static final String SERVICECMD =
            "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDPAUSE = "pause";
    
    private static final String CHN_LANG_CODE = "zho" + MBBMSStore.SEPARATOR_INNER;

    //instance states.
    private static final String CURR_CHANNEL = "CURR_CHANNEL";
    private static final String STOPBYUSER = "STOPBYUSER";
    
    private static final int OPEN_TIME_OUT_INTERVAL = 12000;

    
    private static final int MSG_OPEN_DONE = 1;
    private static final int MSG_INITIALIZATION = 2;
    private static final int MSG_UPDATE_SIGNAL = 3;
    private static final int MSG_UPDATE_SERVICE_DONE = 4;      
    private static final int MSG_UPDATE_SIGNAL_INFO = 5;
    private static final int MSG_OPEN_CHANNEL_REQ = 6;

    
    private static final int STATE_IDLE = 0;
    private static final int STATE_BINDING_SERVICE = 1;    
    private static final int STATE_BINDED_SERVICE = 2;
    private static final int STATE_UPDATING_SERVICE = 3; 
    private static final int STATE_UPDATED_SERVICE = 4;    
    private static final int STATE_UPDATED_SERVICE_FAIL = 5;
    private static final int STATE_OPENING = 6;
    private static final int STATE_OPENED = 7;
    private static final int STATE_STARTED = 8;    

    private int mState;//the state of this activity.

    //all view reference should be available when mInitialized is true.
    private boolean mInitialized;
    private boolean mScreenOn = true;//indicate whether the window is being focused.
    private boolean mChannelChanged;
    private boolean mPlayFailed;
    private boolean mStopByUser;    
    private boolean mPaused;
    private boolean mStarted;    
    private boolean mPreparing;    

    private MBBMSService mService;
    private int mCurrentChannel = -1;//current playing channel.

    //private BroadcastReceiver mScreenStatusReceiver;    
    private TextView mNotification;     
    private ImageView mLoadingIcon;    
    private ProgressBar mProgressBar;    
    private TextView mChannelNameView0;    
    private ListView mChannelListView;    
    private View mProgressView;
    
    private Bitmap mDefaultLoadingIcon;
    private Bitmap mVideoChannelIcon;
    private Bitmap mAudioChannelIcon;
    private Bitmap mUnsubscribedLogo;    
    private Bitmap mSubscribedIndicator;    
    
    
    private CMMBMediaPlayer mMediaPlayer;


    private static final int LEVEL_SIGNAL_MIN = 0;
    private static final int LEVEL_SIGNAL_MAX = 50;
    private static final int LEVEL_SIGNAL_MEDIUM_MAX = 35;
    private static final int LEVEL_SIGNAL_WEAK_MAX = 20;
    private static final int LEVEL_SIGNAL_UNAVAILABLE_MAX = LEVEL_SIGNAL_MIN;
    private static final int LEVEL_SIGNAL_TRANSIENT_MAX = 1;

    private static final int SIGNAL_STATE_UNKNOWN = 0;    
    private static final int SIGNAL_STATE_UNAVAILABLE = 1;    
    private static final int SIGNAL_STATE_TRANSIENT = 2;    
    private static final int SIGNAL_STATE_WEAK = 3;
    private static final int SIGNAL_STATE_MEDIUM = 4;
    private static final int SIGNAL_STATE_NORMAL = 5;
    private int mSignalState = SIGNAL_STATE_UNKNOWN;

    private Thread mUpdateServiceThread = null;

    private OpenChannelThread mOpenThread = null;
    
    private ChannelListAdapter mAdapter;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;

    private ChannelListManager mChannelListManager;
    private ArrayList<Channel> mChannelList;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage what = " + msg.what);
        
            switch (msg.what) {
            
                case MSG_OPEN_CHANNEL_REQ: 
                    openChannelDirect();
                    break;
                
                case MSG_INITIALIZATION: 
                    initAll();
                    break;
                

                case MSG_UPDATE_SIGNAL_INFO: 
                    if (mState != STATE_STARTED) {
                        return;
                    }
                    Log.d(TAG, "MSG_UPDATE_SIGNAL_INFO");            
                    //stopLoadingAnimation();//error may happen during loading animation,in such case we need to stop it.
                    updateSignalInfo();
                    break;
                                        

                case MSG_UPDATE_SERVICE_DONE: 
                    if (mState != STATE_UPDATING_SERVICE) {
                        return;
                    }
                    Log.d(TAG, "MSG_UPDATE_SERVICE_DONE: mState = " + mState + " error = " + (String)msg.obj);            
                    //stopLoadingAnimation();//error may happen during loading animation,in such case we need to stop it.
                    onUpdateServiceGuideEnd(msg.arg1 == 1);                    

                    break;
                                        
                
                case MSG_OPEN_DONE: 
                    //ignore asnchronous events because it may already in queue when state is changed.        
                    if (mState != STATE_OPENING) {
                        return;
                    }
                    
                    if (mChannelChanged) {
                        if (msg.arg1 == 1) { 
                            mState = STATE_OPENED;
                            //closeChannel();                            
                        } else {
                            mState = STATE_BINDED_SERVICE;
                        }
                        play(false);
                    } else {
                        if (msg.arg1 == 1) {
                            openTrue(msg.arg2);
                        } else {
                            openFalse((String)msg.obj);
                        }
                    }
                    break;
                                   


                case MSG_UPDATE_SIGNAL: 
                    onSignalUpdate(msg.arg1);
                    break;
                default:
                    break;
            }
        }

    };
    

    private MyServiceConnection mConnection = new MyServiceConnection();

    private class MyServiceConnection implements ServiceConnection,MBBMSService.OnEventListener
        ,MBBMSService.OnTerminationListener {
        
        public void onServiceDisconnected(ComponentName name) {
            mService = null;        
            mState = STATE_IDLE;
        }
        
        public void onServiceConnected(ComponentName name, IBinder service) {

            mState = STATE_BINDED_SERVICE;
            Log.d(TAG, "onServiceConnected");
            mService = ((LocalBinder)service).getService();
            /*
            if (!mPaused && mScreenOn && !mStopByUser) {
                play(true);
            }
            */
            mService.setEventListener(this);
            mService.setOnTerminationListener(this);
            if (mMode == CMMB_FMODE) {
                //prepare channel info, start open channel
                mState = STATE_UPDATED_SERVICE;
                mHandler.sendMessage(mHandler.obtainMessage(MSG_OPEN_CHANNEL_REQ, 0, 0,null));
            } else {
                updateServiceGuide(null);
            }
            
        }

        public void onTerminate() {
            Log.d(TAG, "onTerminate() mState = " + mState);            
            switch(mState) {
            case STATE_STARTED:
                resetPlayer();
                //fall through
            case STATE_OPENED:
            case STATE_OPENING:                
                resetOnTermination();
                break;
            case STATE_UPDATING_SERVICE:                
                stopUpdateServiceGuide();
                break;

            default:
                break;
                
            }
            //must release player to let MediaPlayerService release its wakelock.
            //releasePlayer();
        }        

        public void event(int ev, int type, int arg, Object status) {
            if (ev == CMMBServiceClient.CMMB_EVENT_CLIENT_INFO) {
                Log.d(TAG, "CMMBService info type = " + type);
                switch (type) {
                    case CMMBServiceClient.CMMB_INFO_BITSTREAM_ARRIVAL:
                        //because this function is called in binder thread,
                        //we don't want it to execute openTrue outside main thread.
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_OPEN_DONE,1,arg,null));
                        break;
                    case CMMBServiceClient.CMMB_INFO_SIGNAL_QUALITY:
                        //remove old signal messages in queue,by doing so we can prevent the player
                        //start/stop too often because of the unstable signal environment.
                        mHandler.removeMessages(MSG_UPDATE_SIGNAL);
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SIGNAL,arg,0,null));
                        break;                
                    case CMMBServiceClient.CMMB_INFO_SERVICE_GUIDE_FAIL:
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SERVICE_DONE,0,0, null));
                        break;     
                    case CMMBServiceClient.CMMB_INFO_SERVICE_GUIDE_OK:
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SERVICE_DONE,1,arg,null));
                        break;                                
                    default:
                        break;
                }
            } else if (ev == CMMBServiceClient.CMMB_EVENT_CLIENT_ERROR) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_OPEN_DONE,0,ev, null));
            }
        }        
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() savedInstanceState = " + savedInstanceState);
        super.onCreate(savedInstanceState);

        mChannelListManager = ChannelListManager.getChannelListManager(this);
        //mChannelList = mChannelListManager.getChannelList();
        
            //get the current channel and open channel now.
        Intent intent = getIntent();
        mMode = intent.getIntExtra(INTENT_KEY_MODE, CMMB_FTMODE);
        //mLockFreq = intent.getIntExtra(INTENT_KEY_FREQ, mLockFreq);
        String freqStr = intent.getStringExtra(INTENT_KEY_FREQ);
        Log.d(TAG, "onCreate() mMode = " + mMode + ", FreqStr =" + freqStr);
        if (freqStr != null) {
            try {
                mLockFreq = Integer.parseInt(freqStr);
                if (mLockFreq >= Integer.MAX_VALUE / 1000000) {
                    mLockFreq = DEF_FREQ;
                    Toast.makeText(this, "Invalid Input Freqency, use default:" + mLockFreq,1000).show();
                } else {
                    mLockFreq *= 1000000;
                }
            } catch (Exception e) {
                mLockFreq = DEF_FREQ;
                Toast.makeText(this, "Invalid Input Freqency, use default:" + mLockFreq,1000).show();
            }
        }
        Log.d(TAG, "onCreate() mMode = " + mMode + ", mLockFreq =" + mLockFreq);
        bindMBBMSService();
        
        setContentView(R.layout.ftm);
        
        
        mDefaultLoadingIcon = BitmapFactory.decodeResource(getResources(),R.drawable.loading_icon);
            
        mProgressView = findViewById(R.id.progress_indicator);
        mChannelNameView0 = (TextView)findViewById(R.id.channel_name0);
        mNotification = (TextView)findViewById(R.id.notification);
        mLoadingIcon = (ImageView)findViewById(R.id.logo);
        mProgressBar = (ProgressBar)findViewById(android.R.id.progress);
        
        
        
        //let volume key handle media volume control. 
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent()");        
    }  
    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");    
        super.onStart();
        mStarted = true;
    }
    
    @Override
    public void onStop() {
        Log.d(TAG, "onStop()"); 
        super.onStop();
        mStarted = false;        
    }  


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() mState = " + mState);
    
        super.onDestroy();
        
        if (mState == STATE_UPDATING_SERVICE) {
            stopUpdateServiceGuide();
        }

        releasePlayer();
        destroyOpenChannelThread();
        if (mService != null) {
//            mService.setEBMNotification(null);
            mService.unSetEventListener(mConnection);
            mService.setOnTerminationListener(null);
            mService.stopTranscationThread();
            unbindService(mConnection);
        }
        
        mHandler.removeCallbacksAndMessages(null);    
        mChannelListManager.release();
    }



    private String getStringById(int id) {
        return getResources().getString(id);
    }
    
    private void checkChannelAndPlay(int ch) {
        Log.i(TAG, "checkChannelAndPlay() ch = " + ch + " mCurrentChannel =" + mCurrentChannel);
        
        if (ch < mChannelList.size()) {
            mCurrentChannel = ch;
            play(true);
        }
    }
    
    

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "onWindowFocusChanged() hasFocus = " + hasFocus + ",mScreenOn = " + mScreenOn);
        if (!mScreenOn && hasFocus) {
            mScreenOn = true;
            if (!isFinishing()) {
                playOnResume();
            }
        } else if (!hasFocus) {
            mScreenOn = false;
        }  
        super.onWindowFocusChanged(hasFocus);
    }

    
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() mState = " + mState);    
        if (mState == STATE_OPENING) {
            cancelOpenning(true);//to speed up the onDestroy process.
        }            
        mHandler.removeCallbacksAndMessages(null);
        super.onBackPressed();
    }

    
    private boolean bindMBBMSService() {
        if (mService == null) {     
            if (mState != STATE_BINDING_SERVICE) {
                Intent intent = new Intent(); 
                intent.setAction("com.ben.MBBMSService");                  
                bindService(intent, mConnection, BIND_AUTO_CREATE);            
                mState = STATE_BINDING_SERVICE;
            } else {                
                Log.w(TAG, "bindMBBMSService() mState = STATE_BINDING_SERVICE");
            }
            return false;
        } else {
            return true;
        }
    }

    private boolean checkIfChannelAvailable() {
        Channel ch = mChannelList.get(mCurrentChannel);
        if (ch.isFree()) {
            return true;
        } else {
           return ch.isSubscribed();          
        }
    }
    
    private int getNotificationString() {
        if (checkIfChannelAvailable()) {
            return R.string.loading;
        } else {            
            return R.string.channel_unavailable_now2;
        }
    }

    private void play(boolean updateViews) {
        Log.d(TAG, "play() mState = " + mState + " updateViews = " + updateViews); 
        
        mPlayFailed = false;
        mStopByUser = false;
        
        
        switch(mState) {
            case STATE_IDLE:            
                if (!bindMBBMSService()) { //return true means already binded to service.            
                    break;
                } 
            //fall through            
            case STATE_UPDATING_SERVICE: 
            case STATE_BINDING_SERVICE:
                break;
            case STATE_UPDATED_SERVICE_FAIL: 
                updateServiceGuide(null);
                break;
            case STATE_UPDATED_SERVICE:    
                openChannel();
                break;
            case STATE_OPENING:
                mChannelChanged = true;         
                break;
            case STATE_STARTED:
                resetPlayer();
                //fall through            
            case STATE_OPENED:
                closeChannel();     
                openChannel();
                break;
            default:
                myAssert(false);
        }    
        
        if (updateViews) {
            updateLoadingViews();    
        }        
    }    

    private String getErrorDescription(ServerStatus status) {        
        return Utils.getErrorDescription(getResources(),status,null);
    }    

    private class OpenChannelThread extends Thread {        
        private boolean mTerminated = false;
        private boolean mReopen = false;
        private boolean mDoClose = false;        
        private boolean mIsMbbms = true;
        private boolean mResult = false;
        private Object mLock = new Object();

        
        OpenChannelThread(boolean isMbbms) {
            super();
            mIsMbbms = isMbbms;
        }
        
        @Override
        public void run() {                
            while (true) {
                Channel ch = null;
                synchronized (mChannelListManager) {
                    //this synchroniztion is to guarantee mChannelList is not changed in the mean time.
                    //we do not syncronize the access to mCurrentChannel,but it is ok because
                    //we will set mChannelChanged to true again if mCurrentChannel is changed.                    
                    ch = mChannelList.get(mCurrentChannel);
                }

                if (ch == null) {
                    break;
                }
                ServerStatus result;
                if (mIsMbbms) {
                    result = mService.openChannel(ch.cmmbServiceID,ch.freq,ch.sdp,ch.serviceID);
                } else {
                    result = mService.openChannel(Integer.parseInt(ch.serviceID),ch.freq);
                }        
                    
                mResult = Utils.isSuccess(result);
                
                if (!mResult) {
                    String err = getErrorDescription(result);
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_OPEN_DONE,0,0,err));
                }
                
                Log.d(TAG, "openChannel mResult = " + mResult);

                try {
                    waitOnThread();
                } catch (InterruptedException e) {
                    callServiceCloseChannel();
                    break;
                }

            }
        }        
        public void callServiceCloseChannel() {
            Log.d(TAG, "callServiceCloseChannel mResult =" + mResult);                    
            
            if (mResult) {
                mResult = false;
                mService.closeChannel();
            }    
            
            synchronized (mLock) {            
                mDoClose = false;            
            }
        }
        
        public void stopThread() {
                Log.d(TAG, "stopThread ");    
                mReopen = false;
                interrupt();
        }                    

        public void close() {
            synchronized (mLock) {
                Log.d(TAG, "close ");                        

                if (mTerminated) {
                    return;
                }
                mDoClose = true;
                mReopen = false;                
                mLock.notify();
            }
        }

        public void open() {
            synchronized (mLock) {
                Log.d(TAG, "open ");                        

                //wait previous termination done otherwise callServiceCloseChannel in waitOnThread may cause error
                //because the underlayer already terminates.
                while (mTerminated) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        Log.d(TAG, "open ignore");//ignore.
                    }
                }

                mReopen = true;
                mDoClose = false;    

                mLock.notify();
            }
        }        
            
        public void notifyTermination() {
            synchronized (mLock) {
                Log.d(TAG, "notifyTermination ");                                        
                mTerminated = true;
                mReopen = false;                
                mLock.notify();
                //Confirmed with driver,no need to closeChannel before terminate is called
                /*while (mTerminated) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        //ignore.
                    }
                }*/
            }
        }
            
        private void waitOnThread() throws InterruptedException {        
            synchronized (mLock) {            
                Log.d(TAG, "waitOnThread ");
                if (mTerminated) {
                    mTerminated = false;
                    
                    //Confirmed with driver,no need to closeChannel before terminate is called.Just reset mResult here.
                    //callServiceCloseChannel();
                    //mLock.notify();
                    mResult = false;
                } else if (mDoClose) {
                    callServiceCloseChannel();                    
                }
                
                
                while (!mReopen) {
                    mLock.wait();
                    if (mTerminated) {
                        mTerminated = false;
                        mDoClose = false;
                        
                        //mLock.notify();
                        mResult = false;
                    } else {    
                        callServiceCloseChannel();                
                    }
                }
                
                mReopen = false;

                //notify may be called before wait and set mReopen = true,thus we need to closeChannel here.
                callServiceCloseChannel();                
            }
        }
    }
    
    private void openChannel() {
        Log.d(TAG, "openChannel() mState = " + mState);

        if (mState != STATE_UPDATED_SERVICE) {
            return;
        }

        mChannelChanged = false;

        if (!checkIfChannelAvailable()) {
            return;
        }

        resetSignalState();
        
        mHandler.removeMessages(MSG_OPEN_DONE);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_OPEN_DONE,0,-1,null),OPEN_TIME_OUT_INTERVAL);
        
        if (mOpenThread == null) {
            mOpenThread = new OpenChannelThread(false);                    
            mOpenThread.start();                
        } else {
            mOpenThread.open();
        }
        
        mState = STATE_OPENING;    
    }



    private void closeChannel() {
        Log.d(TAG, "closeChannel() mState =" + mState);
        
        if (mState == STATE_OPENED) {
            mOpenThread.close();
            mState = STATE_UPDATED_SERVICE;
        }
    }

    private void resetOnTermination() {
        Log.d(TAG, "resetOnTermination() mState =" + mState);
        
        if (mOpenThread != null) {
            mOpenThread.notifyTermination();
        }
        mHandler.removeMessages(MSG_OPEN_DONE);
        mState = STATE_UPDATED_SERVICE;        
    }

    private void cancelOpenning(boolean force) {
        Log.d(TAG, "cancelOpenning() mState =" + mState + " force = " + force);
        if (force) {
            destroyOpenChannelThread();
            mState = STATE_UPDATED_SERVICE; 
        } else if (mState == STATE_OPENING) {
            mOpenThread.close();
            mHandler.removeMessages(MSG_OPEN_DONE);             
            mState = STATE_UPDATED_SERVICE;
        }

    }    

        
    private void destroyOpenChannelThread() {
        if (mOpenThread != null) {
            mOpenThread.stopThread();
            try {
                mOpenThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "destroyOpenChannelThread ignore ");//ignore
            }
            mOpenThread = null;            
            mHandler.removeMessages(MSG_OPEN_DONE); 
        }
    }

    private int getSignal() {        
        Log.d(TAG, "getSignal()");
        return mService.getProp(CMMBServiceClient.CMMB_PROPERTY_SIGNAL_QUALITY);
    }

    private void onSignalUpdate(int level) {
        if (mState < STATE_OPENED) {
            Log.w(TAG, "onSignalUpdate() early return mState = " + mState);
            return;
        }
        
        Log.d(TAG, "onSignalUpdate() level = " + level + " mSignalState = " + mSignalState);
        
        //level:0~50
        if (level >= LEVEL_SIGNAL_MIN && 
                level <= LEVEL_SIGNAL_UNAVAILABLE_MAX && 
                mSignalState != SIGNAL_STATE_UNAVAILABLE) {
            stopByNoSignal();            
        } else if (level > LEVEL_SIGNAL_UNAVAILABLE_MAX && level <= LEVEL_SIGNAL_TRANSIENT_MAX 
                    && mSignalState != SIGNAL_STATE_UNAVAILABLE && mSignalState != SIGNAL_STATE_TRANSIENT) {    
            mSignalState = SIGNAL_STATE_TRANSIENT;    
        } else if (level > LEVEL_SIGNAL_TRANSIENT_MAX && 
                level <= LEVEL_SIGNAL_WEAK_MAX && 
                mSignalState != SIGNAL_STATE_WEAK) {    
            int oldState = mSignalState;
            mSignalState = SIGNAL_STATE_WEAK;    
            if (oldState == SIGNAL_STATE_UNAVAILABLE && !mPaused) {
                Toast.makeText(this,R.string.signal_recover,1000).show();
                startPlayer();
            }             
        } else if (level > LEVEL_SIGNAL_WEAK_MAX && 
                level <= LEVEL_SIGNAL_MEDIUM_MAX && 
                mSignalState != SIGNAL_STATE_MEDIUM) {
            int oldState = mSignalState;
            mSignalState = SIGNAL_STATE_MEDIUM;            
            if (oldState == SIGNAL_STATE_UNAVAILABLE && !mPaused) {
                Toast.makeText(this,R.string.signal_recover,1000).show();
                startPlayer();
            }    
        } else if (level > LEVEL_SIGNAL_MEDIUM_MAX && 
                level <= LEVEL_SIGNAL_MAX && 
                mSignalState != SIGNAL_STATE_NORMAL) {
            int oldState = mSignalState;
            mSignalState = SIGNAL_STATE_NORMAL;            
            if (oldState == SIGNAL_STATE_UNAVAILABLE && !mPaused) {
                Toast.makeText(this,R.string.signal_recover,1000).show();
                startPlayer();
            }    
        }
    }

    private void resetSignalState() {
        Log.d(TAG, "resetSignalState() mSignalState = " + mSignalState);
        
        mSignalState = SIGNAL_STATE_UNKNOWN;    
    }
    
    private String getChannelName(int index) {    
        return mChannelList.get(index).name;    
    }

    private String getCurrentChannelName() {    
        return getChannelName(mCurrentChannel);    
    }    

    private boolean isPlaying() {
        return !(mPlayFailed || mStopByUser 
                || mSignalState == SIGNAL_STATE_UNAVAILABLE);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Make sure we have a surface in the holder before proceeding.
        if (holder.getSurface() == null) {
            Log.e(TAG, "holder.getSurface() == null");
            return;
        }

        Log.d(TAG, "surfaceChanged() mState = " + mState);

        // We need to save the holder for later use.
        mSurfaceHolder = holder;

        startPlayer();          
    }
    
    public void surfaceCreated(SurfaceHolder holder) {    
        Log.d(TAG, "surfaceCreated()"); 
    }

    public void surfaceDestroyed(SurfaceHolder holder) {        
        Log.d(TAG, "surfaceDestroyed()"); 
        mSurfaceHolder = null;
    }

    private void openTrue(int signal) {            
        Log.d(TAG, "openTrue() mState = " + mState + " signal = " + signal);
        switch (mState) {
            case STATE_OPENING:
                //onSignalUpdate();    
                mState = STATE_OPENED;
                onSignalUpdate(signal); /*getSignal()*/
                startPlayer();            
                break;                    
            default:
                //ingore asnchronous events because it may already in queue when state is changed.
                return;
        }
    }

    private void openFalse(String error) {    
        
        Log.d(TAG, "openFalse() mState = " + mState);        
        switch(mState) {            
            case STATE_OPENING:   
                mOpenThread.close();
                stopByError(getStringById(R.string.signal_weak_stop_internal));    
                mState = STATE_UPDATED_SERVICE;
                break;
           
            default:
                //ingore asnchronous events because it may already in queue when state is changed.
                return;
        }            
    }


    private void prepare() {
        Log.d(TAG, "prepare() mState = " + mState); 
        if (mMediaPlayer == null) {
            mMediaPlayer = CMMBMediaPlayer.create(mSurfaceView.getHolder());

            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnInfoListener(this);    
            mMediaPlayer.setOnCompletionListener(this);            
            mMediaPlayer.setOnPreparedListener(this);                        
            mMediaPlayer.setOnVideoSizeChangedListener(
                ((TVSurfaceView)mSurfaceView).getSizeChangedListener());    
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);            
                        
        }

        try {        
            //visualizer code.
            if (mChannelList.get(mCurrentChannel).isAudio()) {
                TextView program = (TextView)findViewById(R.id.audio_program);
                //generally initialization should be done already at this time,
                //here we force it to be executed for exceptional cases.
                initAll();
                //we can set the program name once the initialization is ready.
                program.setText(mChannelListManager.getProgramList().get(mCurrentChannel).program);
                //a trick to make marquee run all the time.
                program.setSelected(true);

                //TBD:we should allow screen off,however we should keep audio playing.
                //lockPower();
                mMediaPlayer.setScreenOnWhilePlaying(true);//(false); 
            } else {
                mMediaPlayer.setScreenOnWhilePlaying(true);  
            }
            
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            mMediaPlayer.setDataSource(CMMBMediaPlayer.CMMB_PATH);
            mMediaPlayer.prepareAsync();
            mPreparing = true;

        } catch (Exception ex) {          
            ex.printStackTrace();
            stopByError(null);
            return;
        } 
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError() mState = " + mState + " what = " + what);  
        
        switch(what) {
            case MediaPlayer.MEDIA_ERROR_TYPE_NOT_SUPPORTED: 
                stopByError(getStringById(R.string.unsupported_channel));
                break;
            case MediaPlayer.MEDIA_ERROR_BAD_FILE: 
                //according to driver's comment,treat it as "too weak to play" signal state.
                if (mSignalState != SIGNAL_STATE_UNAVAILABLE) {
                    stopByNoSignal();        
                }
                break;
            default:
                stopByError(null);
                break;
        }
        return true;
    }
    
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onInfo() mState = " + mState + " what = " + what); 
        return false;
    }    

    public void onCompletion(MediaPlayer mp)    {
        Log.d(TAG, "onCompletion() mState = " + mState);
        //according to driver's comment,treat it as "too weak to play" signal state.
        if (mSignalState != SIGNAL_STATE_UNAVAILABLE) {
            stopByNoSignal();        
        }
        //driver also said that signal quality is supposed to be less than 2 when we got EOS.
        //to deal with the exceptional case(signal >=2),we do getSignal and try onSignalUpdate again.    
        //onSignalUpdate(getSignal());
    }

    public void onPrepared(MediaPlayer mp) {    
        Log.d(TAG, "onPrepared() mPreparing = " + mPreparing + " mState = " + mState);
        
        if (mPreparing && mState == STATE_STARTED) {
            
            mPreparing  = false;            
            
            if (mSurfaceHolder == null || mPaused || mStopByUser) {
                Log.d(TAG, "onPrepared() return before start,mSurfaceHolder = " + mSurfaceHolder
                    + " mPaused = " + mPaused + " mStopByUser = " + mStopByUser);
                
                resetPlayer();
                return;
            }
            
            
            if (mSignalState != SIGNAL_STATE_UNAVAILABLE) {    
                mMediaPlayer.start();
                mProgressView.setVisibility(View.GONE);    
            } else {
                stopByNoSignal();
            }
        }
    }


    private void releasePlayer() {
        Log.d(TAG, "releasePlayer() mState = " + mState); 
        if (mMediaPlayer != null) {
            mMediaPlayer.release();    
            //unlockPower();                        
            mPreparing  = false;
            mMediaPlayer = null;
        }
    }

    private void resetPlayer() {
        Log.d(TAG, "resetPlayer() mState = " + mState); 
        if (mState == STATE_STARTED) {
            mMediaPlayer.reset();
            //unlockPower();            
            mPreparing  = false;
            mState = STATE_OPENED;             
        }
    }    

    
    private void initAll() {
        Log.d(TAG, "initAll() mInitialized = " + mInitialized);
        if (mInitialized) {
            return;
            //already initialized.
        }
        initVideoSurface();
        initSiginfoList();
        mInitialized = true;
    }

    private void initVideoSurface() {
        Log.d(TAG, "initVideoSurface()"); 
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceView.setVisibility(View.VISIBLE);
        
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    private void initChannelListPopup() {
        Log.d(TAG, "initChannelListPopup()");

        mChannelListView = (ListView)findViewById(R.id.channel_list_in_player);
        mChannelListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mAdapter = new ChannelListAdapter(this);
        mChannelListView.setAdapter(mAdapter);

        mChannelListView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {                
                checkChannelAndPlay(position);
                hideChannelList();
            }
            
        });
        

        //initialize related resources.
        mVideoChannelIcon = BitmapFactory.decodeResource(getResources(),R.drawable.video_channel_small);
        mAudioChannelIcon = BitmapFactory.decodeResource(getResources(),R.drawable.audio_channel_small);        
        mUnsubscribedLogo = BitmapFactory.decodeResource(getResources(),R.drawable.unsubscribed_indicator_small);        
        mSubscribedIndicator = BitmapFactory.decodeResource(getResources(),R.drawable.subscribed_indicator_small);        
    }      
    
    private void showChannelList() {
        Log.d(TAG, "showChannelList()");

        mChannelListView.setVisibility(View.VISIBLE);        
        mChannelListView.setSelection(mCurrentChannel);
    }    

    private void hideChannelList() {
        Log.d(TAG, "hideChannelList()");
        mChannelListView.setVisibility(View.INVISIBLE);        
    }    

    private void updateLoadingViews() {
        //change all channel attribute indicators.
        mChannelNameView0.setText(getCurrentChannelName());    

        mLoadingIcon.setImageBitmap(mDefaultLoadingIcon);    

        int strId = getNotificationString();
        if (strId == R.string.loading) {            
            showLoadingViews();
        } else {
            stopByValidationFailure(getStringById(strId));
        }                
    }
    
    private void displayOSDViews(boolean loading,String notification) {
        mProgressView.setVisibility(View.VISIBLE); 
        mProgressBar.setVisibility(loading ? View.VISIBLE : View.INVISIBLE); 
        mNotification.setText(notification);
    }

    private void showUpdatingViews() {
        mLoadingIcon.setImageBitmap(mDefaultLoadingIcon);
        displayOSDViews(true,getStringById(R.string._updating));
    }

    private void showLoadingViews() {
        displayOSDViews(true,getStringById(R.string.loading));
    }
    
    private void showNoSignalViews() {        
        displayOSDViews(false,getStringById(R.string.signal_none));        
    }    

    private void showSignalWeakStopViews() {        
        displayOSDViews(false,getStringById(R.string.signal_weak_stop_internal));        
    }        

    private void showPlayFailedViews(String otherError) {
        displayOSDViews(false,otherError != null ? otherError : getStringById(R.string.open_channel_fail));
    }    

    private void showStoppedViews() {        
        displayOSDViews(false,null);        
    }    


    private void stopByNoSignal() {
        mSignalState = SIGNAL_STATE_UNAVAILABLE;    
        stop();                
        if (!mStopByUser) {
            showNoSignalViews();
        }
    }    

    private void stopByError(String otherError) {    
        Log.d(TAG, "stopByError() otherError = " + otherError); 
        
        mPlayFailed = true;    
        stop();        
        if (!mStopByUser) {
            showPlayFailedViews(otherError);
        }
    }

    private void stopByValidationFailure(String otherError) {        
        mPlayFailed = true;        
        showPlayFailedViews(otherError);
        stop();        
    }    
    
    private void stop() {
        Log.d(TAG, "stop() mState = " + mState); 

        switch (mState) {
            case STATE_STARTED:
                resetPlayer();
                break;
            default:
                break;
        }

    }        
    
    private void startPlayer() {
        Log.d(TAG, "startPlayer() mState = " + mState);    

        if (mState != STATE_OPENED || mSurfaceHolder == null || mPaused || mStopByUser) {
            Log.d(TAG, "startPlayer() return before prepare,mSurfaceHolder = " + mSurfaceHolder
                + " mPaused = " + mPaused + " mStopByUser = " + mStopByUser);
            return;
        }


        if (mSignalState != SIGNAL_STATE_UNAVAILABLE) {    
            prepare();            
            mState = STATE_STARTED;
            updateSignalInfo();
        } else {
            stopByNoSignal();
        }
    }
    
    private void myAssert(boolean noassert) {
        if (!noassert) {
            throw new RuntimeException(TAG + " assertion failed!");  
        }
    }    

    private void playOnResume() {
        Log.d(TAG, "playOnResume() mStopByUser = " + mStopByUser + " mPlayFailed =" + mPlayFailed);     
        
        if (mStopByUser || mPlayFailed) {
            return;
        }
        
        switch(mState) {
        case STATE_UPDATED_SERVICE:
            play(mSignalState != SIGNAL_STATE_UNAVAILABLE);            
            break;
        case STATE_OPENED:
            startPlayer();
            break;
        default:
            break;
        }
    }
        
    @Override
    public void onResume() {
        Log.d(TAG, "onResume() mState = " + mState);     
        super.onResume();

        if (mPaused) {
            mPaused = false;            
            if (mScreenOn) {
                //see ALPS00030854.
                KeyguardManager km = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE); 
                if (!km.inKeyguardRestrictedInputMode()) {                    
                    playOnResume();
                }
            }            
        }
        
        //mute background music and FM.
        Intent i = new Intent(SERVICECMD);
        i.putExtra(CMDNAME, CMDPAUSE);
        sendBroadcast(i);
        
        i = new Intent("com.mediatek.FMRadio.FMRadioService.ACTION_TOFMSERVICE_POWERDOWN");
        sendBroadcast(i);
        
        if (!mInitialized) {
            mHandler.sendEmptyMessage(MSG_INITIALIZATION);    
        }

        setStatusBarExpanded(false);        
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause() mState = " + mState);
        super.onPause();

        switch(mState) {
        case STATE_STARTED:
            resetPlayer();
            break;
        case STATE_OPENING:
            cancelOpenning(false);
            break;
        default:
            break;
            
        }

        mPaused = true;

        setStatusBarExpanded(true);                     
    }

    private boolean mDiableStatusBarExpanded;

    
    private void setStatusBarExpanded(boolean expanded) {
        Log.d(TAG, "toggleStatusBarExpanded mDiableStatusBarExpanded = " + mDiableStatusBarExpanded
            + " expanded = " + expanded);
        if (mDiableStatusBarExpanded == !expanded) {
            return;
        }
        
        if (mDiableStatusBarExpanded) {
            mDiableStatusBarExpanded = false;
            StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
            sbm.disable(StatusBarManager.DISABLE_NONE);
        } else {
            mDiableStatusBarExpanded = true;
            StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
            sbm.disable(StatusBarManager.DISABLE_EXPAND);
        }
    }        
    
    private class ChannelListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        class ViewHolder {
            private TextView mChannelName;
            private TextView mProgramName;
            private ImageView mLogo;
            private ImageView mEncrypedindicator;
        }
        
        private ChannelListAdapter(Context c) {
            mInflater = LayoutInflater.from(c);
        }        

        public int getCount() {
            return mChannelList.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {    
            
            Log.d(TAG, "getView() position = " + position + " convertView = " + convertView);
            ViewHolder holder;
            if (convertView == null) {      
                
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.player_channel_list_item, null);
                
                holder.mChannelName = (TextView)convertView.findViewById(R.id.player_channel_name);   
                holder.mProgramName = (TextView)convertView.findViewById(R.id.player_program_name);
                holder.mLogo = (ImageView)convertView.findViewById(R.id.player_logo_image);   
                holder.mEncrypedindicator = (ImageView)convertView.findViewById(R.id.player_encryped_indicator); 
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder)convertView.getTag();
            }    
            
            Channel ch = mChannelList.get(position);
            Program pr = mChannelListManager.getProgramList().get(position);    
            
            Log.d(TAG, "getView() position = " + position + " pr.program = " + pr.program);
            holder.mChannelName.setText(ch.name);
            holder.mProgramName.setText(pr.program);
            
            if (ch.isFree()) {
                holder.mEncrypedindicator.setVisibility(View.INVISIBLE);
                holder.mLogo.setImageBitmap(
                    ch.logo == null ? (ch.isAudio() ? mAudioChannelIcon : mVideoChannelIcon) : ch.logo);
            } else {
                if (ch.isSubscribed()) {    
                    holder.mLogo.setImageBitmap(
                            ch.logo == null ? (ch.isAudio() ? mAudioChannelIcon : mVideoChannelIcon) : ch.logo);
                    holder.mEncrypedindicator.setVisibility(View.VISIBLE);
                    holder.mEncrypedindicator.setImageBitmap(mSubscribedIndicator);
                } else {
                    holder.mEncrypedindicator.setVisibility(View.INVISIBLE);
                    holder.mLogo.setImageBitmap(mUnsubscribedLogo);    
                    //holder.mLogo.setImageBitmap(ch.logo == null ? mUnsubscribedLogo: ch.logo);    
                }
            }
            
            return convertView;
        }
    }    

    /*****************************************
      * functions from MainScreen
    */
    private boolean setCurrentChannel() {
        mChannelList = mChannelListManager.getChannelList();
        int size = mChannelList.size();
        Log.d(TAG, "setCurrentChannel() size = " + size);
        for (int i = 0; i < size; i++) {
            Log.d(TAG, "setCurrentChannel() cmmbServiceID = " 
                    + mChannelList.get(i).cmmbServiceID + ", ServiceID=" 
                    + mChannelList.get(i).serviceID);
            if (Integer.parseInt(mChannelList.get(i).serviceID) == 605) {
                mCurrentChannel = i;
                break;
            }
        }

        return mCurrentChannel != -1;
    }
    private void onUpdateServiceGuideEnd(boolean success) { 
        mUpdateServiceThread = null;

        if (success) {
            Log.d(TAG, "onUpdateServiceGuideEnd() mState = " + mState);
            mState = STATE_UPDATED_SERVICE;
            mChannelListManager.setAllowUpdate(true);
            mChannelListManager.loadChannelList(true, false, true);
            if (!setCurrentChannel()) {
                Log.e(TAG, "onUpdateServiceGuideEnd() without service id 605 ");
                displayOSDViews(false, "no service id: 605!");
            } else {
                initChannelListPopup();
                //hide loading view
                mProgressView.setVisibility(View.GONE);    
                showChannelList();
            }
        } else {
            mState = STATE_UPDATED_SERVICE_FAIL;
            displayOSDViews(false,getStringById(R.string.failed_to_update_service));
        }
    }

    private void updateServiceGuide(final String city) {
        
        Log.d(TAG, "updateServiceGuide");

        mChannelListManager.setAllowUpdate(false);
        mState = STATE_UPDATING_SERVICE; 
        mUpdateServiceThread = new Thread(new Runnable() {
                public void run() {                                         
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        return;
                    }
                    mService.lockPower();
                    ServerStatus result = null;
                    result = mService.syncEServiceGuide();
                    
                    if (!Thread.interrupted()) {
                        if (!Utils.isSuccess(result)) {
                            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SERVICE_DONE,
                                    0,0,getErrorDescription(result)));
                        }
                    }
                    mService.unlockPower(); 
                }
        });    

        
        mUpdateServiceThread.start();
        showUpdatingViews();
    }

    private void stopUpdateServiceGuide() {
        
        Log.d(TAG, "stopUpdateServiceGuide() mState = " + mState);
        if (mUpdateServiceThread != null) {
            mUpdateServiceThread.interrupt();

            try {
                mUpdateServiceThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "stopUpdateServiceGuide()ignore ");//ignore
            }
            mUpdateServiceThread = null;
        }
        mService.stopUpdateService(false);
        mHandler.removeMessages(MSG_UPDATE_SERVICE_DONE);
        mState = STATE_BINDED_SERVICE;          
    }

    /*****************************************
      * functions from MainScreen end
    */

    private SiginfoListAdapter mSiginfoAdapter; 
    private SiginfoItem[] mSiginfoList;
    private ListView mSiginfoListView;
    private static final int SIG_INFO_COUNT = 7;
    private static final int UPDATE_SIGNAL_INFO_DURATION = 1000;

    /*not download SG, open channel directly */
    private void openChannelDirect() {
        /*construct a fake channel list*/
        mChannelList = new ArrayList<Channel>();
        mCurrentChannel = 0;
        Channel ch = new Channel();
        ch.cmmbServiceID = 0;
        ch.forFree = true;
        ch.freq = mLockFreq;
        ch.isEncrypted = false;
        ch.isFavorite = false;
        ch.isSubscribed = false;
        ch.lock_stat = Channel.LOCK_STATE_NONE;
        ch.name = "";
        ch.sdp = "";
        ch.serviceID = "605";
        
        mChannelList.add(ch);
        openChannel();
        updateLoadingViews();
    }
    //TODO: implement it after the service API is ready.
    private void initSiginfoList() {
        Log.d(TAG, "initSiginfoList()");

        mSiginfoListView = (ListView)findViewById(R.id.signal_info);
        mSiginfoAdapter = new SiginfoListAdapter(this);
        mSiginfoListView.setAdapter(mSiginfoAdapter);
    }      

    private Siginfo getSignalInfo() {
        Siginfo info = new Siginfo();
        info.mFreq = mService.getProp(CMMBServiceClient.CMMB_PROPERTY_SIGNAL_FREQUENCY);
        info.mBer = mService.getProp(CMMBServiceClient.CMMB_PROPERTY_SIGNAL_BER_COUNT);
        info.mSnr = mService.getProp(CMMBServiceClient.CMMB_PROPERTY_SIGNAL_SNR_COUNT);
        info.mRssi = mService.getProp(CMMBServiceClient.CMMB_PROPERTY_SIGNAL_STRENGTH);
        info.mPwr =  mService.getProp(CMMBServiceClient.CMMB_PROPERTY_SIGNAL_INBANDPWR);
        info.mCarrOffset =  mService.getProp(CMMBServiceClient.CMMB_PROPERTY_SIGNAL_CARRIEROFFSET);
        return info;
    }
    
    private void updateSignalInfo() {
        if (mState != STATE_STARTED) {
            mSiginfoListView.setVisibility(View.GONE);
            return;
        }
        mSiginfoListView.setVisibility(View.VISIBLE);        
        if (mSiginfoList == null) {
            mSiginfoList = new SiginfoItem[SIG_INFO_COUNT];
            int i;
            for (i  = 0; i < SIG_INFO_COUNT; i++) {
                mSiginfoList[i] = new SiginfoItem();
            }
            mSiginfoList[0].mTitle = getStringById(R.string.channel_freq);
            mSiginfoList[1].mTitle = getStringById(R.string.sig_rssi);
            mSiginfoList[2].mTitle = getStringById(R.string.sig_snr);
            mSiginfoList[3].mTitle = getStringById(R.string.sig_ber);
            mSiginfoList[4].mTitle = getStringById(R.string.sig_fs);
            mSiginfoList[5].mTitle = getStringById(R.string.sig_pwr);
            mSiginfoList[6].mTitle = getStringById(R.string.sig_carroffset);
        }

        Siginfo info = getSignalInfo();
        mSiginfoList[0].mValue = info.mFreq;
        mSiginfoList[1].mValue = info.mRssi;
        mSiginfoList[2].mValue = info.mSnr;
        mSiginfoList[3].mValue = info.mBer;
        mSiginfoList[4].mValue = info.mFs;
        mSiginfoList[5].mValue = info.mPwr;
        mSiginfoList[6].mValue = info.mCarrOffset;
        //refresh UI
        mSiginfoAdapter.notifyDataSetChanged();
        
        mHandler.removeMessages(MSG_UPDATE_SIGNAL_INFO);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_SIGNAL_INFO,0,0,null)
                ,UPDATE_SIGNAL_INFO_DURATION);
    }
    
    
    private    class SiginfoItem {
        String mTitle;
        int mValue;
    }

    private class Siginfo {
        int mRssi; 
        int mSnr;
        int mBer; 
        int mFs;
        int mFreq;
        int mPwr;
        int mCarrOffset;
        public Siginfo() {
            mRssi = 0;
            mSnr = 0;
            mBer = 0;
            mFs = 0; 
            mPwr = 0;
            mCarrOffset = 0;
        }         
    }

    
    private class SiginfoListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        private SiginfoListAdapter(Context c) {
            mInflater = LayoutInflater.from(c);
        }         

        public int getCount() {
            return SIG_INFO_COUNT;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {    
            Log.d(TAG, "getView() position = " + position + " convertView = " + convertView);
            if (convertView == null) {        
                convertView = mInflater.inflate(R.layout.signal_info_item, null);
            }     

            TextView tvTitle;
            TextView tvValue;
            tvTitle = (TextView)convertView.findViewById(R.id.signal_attr_name);
            tvValue = (TextView)convertView.findViewById(R.id.signal_attr_value);
            tvTitle.setText(mSiginfoList[position].mTitle);
            
            tvValue.setText(String.valueOf(mSiginfoList[position].mValue));

            return convertView;
        }
    }    

}
