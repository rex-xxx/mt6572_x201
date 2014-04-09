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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
//import android.app.Service;
import android.app.StatusBarManager;
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
//import android.os.Environment;
import android.os.Handler;
//import android.os.IBinder;
import android.os.Message;
//import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
//import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
//import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
//import android.view.GestureDetector.SimpleOnGestureListener;

import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
//import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.mediatek.cmmb.app.ChannelListManager.Channel;
import com.mediatek.cmmb.app.ChannelListManager.Program;
//MBBMS Provider's interface
import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.MBBMSStore.*;
import com.mediatek.mbbms.MBBMSStore.ESG.*;
import com.mediatek.mbbms.MBBMSStore.SG.*;
import com.mediatek.mbbms.ServerStatus;
//MBBMS Service's interface
import com.mediatek.mbbms.service.CMMBMediaPlayer;
import com.mediatek.mbbms.service.CMMBServiceClient;
import com.mediatek.mbbms.service.MBBMSService;
//import com.mediatek.mbbms.service.MBBMSService.LocalBinder;
import com.mediatek.notification.NotificationManagerPlus;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends Activity implements View.OnClickListener
        ,SurfaceHolder.Callback,MediaPlayer.OnErrorListener,MediaPlayer.OnInfoListener
        ,MediaPlayer.OnCompletionListener,MediaPlayer.OnPreparedListener
        ,ChannelListManager.ChannelListObserver,MBBMSService.OnEventListener
        ,EBManager.EBListener,MBBMSService.OnTerminationListener {    
    
    
    private static final String TAG = "CMMB::PlayerActivity";

    private static final String NO_WELCOME_TEXT = "NWT";
    // Copied from MediaPlaybackService in the Music Player app. Should be
    // public, but isn't.
    private static final String SERVICECMD =
            "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDPAUSE = "pause";
    
    private static final String CHN_LANG_CODE = "zho" + MBBMSStore.SEPARATOR_INNER;

    //instance states.
    private static final String CURR_CHANNEL = "CURR_CHANNEL";
    private static final String MUTED = "MUTED";
    private static final String STOPBYUSER = "STOPBYUSER";
    
    private static final int ANIMATION_DURATION = 500;
    private static final int CONTROL_PANEL_ON_DURATION = 4000;
    private static final int CHANNEL_NUMBER_ON_DURATION = 2000;
    private static final int KEYS_COMBINATION_INTERVAL = 700;
    private static final int OPEN_TIME_OUT_INTERVAL = 20000;
    private static final int OPEN_TIME_OUT_INTERVAL2 = 120000;
    private static final int PLAYER_COMPLETE_INTERVAL = 800;

    
    private static final int MSG_OPEN_DONE = 1;
    private static final int MSG_CONTROL_PANEL_OFF = 2;
    private static final int MSG_INITIALIZATION = 3;
    private static final int MSG_HIDE_CHANNEL_NUMBER = 4;
    private static final int MSG_CAPTURE_FAILED = 5;
    private static final int MSG_UPDATE_SIGNAL = 6;
    private static final int MSG_UPDATE_LAST_WATCH_TIME = 7;
    private static final int MSG_UPDATE_PROGRAM = 8;
    private static final int MSG_COMBINE_KEYS = 9;    
    private static final int MSG_NEW_EBM = 10;
    private static final int MSG_MSK_FAIL = 12;
    private static final int MSG_HIDE_LOADING = 13;
    private static final int MSG_PLAYER_COMPLETE = 14;

    //dialog ids
    private static final int DIALOG_OPEN_CHANNEL_FAIL = 1;    
    private static final int DIALOG_SIGNAL_PROBLEM = 2;    
    private static final int DIALOG_EMERGENCY_BROADCAST_3_4 = 3;        

    //options menu positions
    private static final int POSITION_GOTO_PROGRAM_LIST = 1;
    private static final int POSITION_GOTO_EM_LIST = 2;
    private static final int POSITION_FULL_SCREEN = 3;
    
    
    private static final int STATE_IDLE = 0;
    private static final int STATE_OPENING = 1;
    private static final int STATE_OPENED = 2;
    private static final int STATE_STARTED = 3;    
    private static final int STATE_CAPTURING = 4;
    
    private int mState;//the state of this activity.
    private boolean mHasStartRetrieveKey = false;
    private boolean mHasStartRetrieveKeyOnPlay = false;
    private    NotificationManagerPlus mNMP;

    //all view reference should be available when mInitialized is true.
    private boolean mInitialized;
    private boolean mMuteByUser;//indicate whether the tv is muted by user. 
    private boolean mMuted;//indicate whether the tv is muted now.     
    private boolean mScreenOn = true;//indicate whether the window is being focused.
    private boolean mCallActive;    //is there any call active
    private boolean mChannelChanged;
    private boolean mPlayFailed;
    private boolean mStopByUser;    
    private boolean mPaused;
    private boolean mStarted;    
    private boolean mPreparing;    
    private boolean mNMPLisening;//is listening on NotificationManagerPlus events. 
    
    private MBBMSService mService;
    private int mCurrentChannel;//current playing channel.
    private int mPreviousChannel = -1;    
    private Bundle mEbmMessage;

    //private BroadcastReceiver mScreenStatusReceiver;    
    private ModeSwitchManager mModeSwitchManager;
    private CallStateListener mCallListener;    
    private View mControlPanel;
    private ImageView mFavoriteIndicator; 
    private ImageView mSnapButton;    
    private ImageView mSignalIndicator;    
    private ImageView mPlayStopButton;    
    private ImageView mMuteButton;        
    private TextView mNotification;     
    private ImageView mLoadingIcon;    
    private ProgressBar mProgressBar;    
    private TextView mChannelNameView0;    
    private TextView mChannelNameView;     
    private TextView mChannelNumberView;     
    private ListView mChannelListView;    
    private View mProgressView;
    private ImageView mUnreadEB;
    private View mDlgNeedSubscrib;
    private TextView mDlgNeedSubscribMsg;
    private TextView mPlayErrorView;
    
    private Bitmap mDefaultLoadingIcon;
    private Bitmap mVideoChannelIcon;
    private Bitmap mAudioChannelIcon;
    private Bitmap mUnsubscribedLogo;    
    private Bitmap mSubscribedIndicator;    
    
    private Drawable mMuteView;
    private Drawable mUnmuteView;      
    private Drawable mAddToFavorite;
    private Drawable mRemoveFromFavorite;     
    private Drawable mPlayView;
    private Drawable mStopView;      
    
    private AnimationDrawable mEbmAnimation;    
    
    private CMMBMediaPlayer mMediaPlayer;

    private String mWelcomeInfo;
    //visualize code  
    private View mAudioBox;
    private Visualizer mVisualizer;
    private VisualizerView mVisualizerView;

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

    private static final int MAJOR_MOVE = 60;
    private int mFisrtNumberKey = KeyEvent.KEYCODE_UNKNOWN;

    private OpenChannelThread mOpenThread = null;
    private Thread mSnapThread = null;
    private String mSnapFilename;
    private    long mSnapDateTaken;
    
    private ChannelListAdapter mAdapter; 
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private GestureDetector mGestureDetector;

    private ChannelListManager mChannelListManager;
    private ArrayList<Channel> mChannelList;
    private String[] mWelcomeTextArray;
    
    /* Antenna type*/
    private static final String ANTENNA_TYPE_INTERNAL = "internal";
    private static final String ANTENNA_TYPE_EARPHONE = "earphone";
    private static final String ANTENNA_TYPE_PULL = "pull";
    private static final String ANTENNA_TYPE_PLUG = "plug";
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage what = " + msg.what);
        
            switch (msg.what) {
                case MSG_PLAYER_COMPLETE:
                    if (mState == STATE_STARTED && mSignalState != SIGNAL_STATE_UNAVAILABLE) {
                        stopByNoSignal();    
                    }
                    break;
               

                case MSG_HIDE_LOADING:
                    if (mState == STATE_STARTED || mState == STATE_CAPTURING) {
                        mProgressView.setVisibility(View.GONE);
                    } else {
                        Log.e(TAG, "handleMessage hide loading on " + mState);
                    }
                    break;
               

                case MSG_INITIALIZATION: 
                    initAll();
                    break;
               

                case MSG_COMBINE_KEYS: 
                    if (mFisrtNumberKey != KeyEvent.KEYCODE_UNKNOWN) {
                        int num = mFisrtNumberKey - KeyEvent.KEYCODE_0;
                        checkChannelAndPlay(num);
                        mFisrtNumberKey = KeyEvent.KEYCODE_UNKNOWN;
                    }
                    break;
                
                
                case MSG_OPEN_DONE: 
                    //ignore asnchronous events because it may already in queue when state is changed.        
                    if (mState != STATE_OPENING) {
                        return;
                    }
                    
                    if (mChannelChanged) {
                        if (msg.arg1 == 1) { 
                            mState = STATE_OPENED;
                        } else {
                            mState = STATE_IDLE;
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
                                  

                case MSG_CONTROL_PANEL_OFF: 
                    fadeOut(mControlPanel);
                    break;
                

                case MSG_NEW_EBM:             
                    
                    Log.d(TAG, "mEbmMessage = " + mEbmMessage);            
                    //only care about 3/4 ebm,only act to it in forground(before onStop),only act to one ebm at one time.
                    if (mEbmMessage == null && mStarted) {
                        mEbmMessage = (Bundle)(msg.obj);
                        showDialog(DIALOG_EMERGENCY_BROADCAST_3_4,mEbmMessage);
                    }
                    break;
                                 

                case MSG_HIDE_CHANNEL_NUMBER: 
                    if (msg.arg1 == 0) {
                        mChannelNumberView.setTextSize(20);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_HIDE_CHANNEL_NUMBER,1,0,null),
                        CHANNEL_NUMBER_ON_DURATION);                         
                    } else {
                        mChannelNumberView.setVisibility(View.INVISIBLE);
                    }
                    break;
              
                case MSG_CAPTURE_FAILED: 
                    onError(mMediaPlayer,msg.arg1,0);
                    break;
                      
                case MSG_UPDATE_SIGNAL: 
                    onSignalUpdate(msg.arg1);
                    break;
                  

                case MSG_MSK_FAIL:
                    onMskFail();
                    break;
                default:
                    break;
                
            }
        }

    };    

    private class MyModeChangeHandler extends ModeSwitchManager.ModeChangeHandler {
        @Override
        public boolean onEnterCmmbMode() {
            //ensure stopService is called before CmmbModeSwitch
            stopOpenChannelThread(true);
            //To avoid OnChannelListChanged is called during exiting which may cause exception in 
            //isCurrentChannelMyFavorite.
            mChannelListManager.unregisterChannelListObserver(PlayerActivity.this);        
            return false;
        }    
        
        @Override
        public boolean onModeSelected(boolean isMbbmsSelected) {
            if (isMbbmsSelected) {
                //ensure stopService is called before CmmbModeSwitch                
                stopOpenChannelThread(true);
                //To avoid OnChannelListChanged is called during exiting which may cause exception in 
                //isCurrentChannelMyFavorite.                
                mChannelListManager.unregisterChannelListObserver(PlayerActivity.this);
            }  
            return false;                            
        }          
    }    

    private void registerCallStateListener() {
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (tmgr != null) {
            mCallListener = new CallStateListener();
            tmgr.listen(mCallListener, PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            Log.e(TAG, "tmgr is null!! ");
        }
    }

    private void unregisterCallStateListener() {
        if (mCallListener != null) {
            TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tmgr != null) {
                tmgr.listen(mCallListener, PhoneStateListener.LISTEN_NONE);
            } else {
                Log.e(TAG, "tmgr is null!! ");
            }
            mCallListener = null;
        }
    }

    private class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d(TAG, "onCallStateChanged() state = " + state);        
            Log.d(TAG, "onCallStateChanged() mMuted = " + mMuted + " mStarted = " + mStarted);
            if (mStarted && mMuted && state == TelephonyManager.CALL_STATE_IDLE && (false == isAnyActiveCall())) {
                if (!mMuteByUser) {
                    //only setMute when we are in foreground otherwise 
                    //we may interfere other apps who use Music stream either.            
                    setMute(false);
                }
                mCallActive = false;
                setMuteButtonEnabled(true);                                
            } 
        }        
    }    

    private boolean isAnyActiveCall() {
        boolean phoneInUse = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null)  {
                phoneInUse = !phone.isIdle();
        }
        } catch (RemoteException e) {
            Log.w(TAG, "phone.isIdle() failed", e);
        }
        return phoneInUse;
    }

                    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() savedInstanceState = " + savedInstanceState);
        super.onCreate(savedInstanceState);

        mModeSwitchManager = new ModeSwitchManager(this,new MyModeChangeHandler(),savedInstanceState);     

        mChannelListManager = ChannelListManager.getChannelListManager(this);
        ArrayList<Channel> cl = mChannelListManager.getChannelList();
        if (cl == null || cl.size() == 0) {
            Log.d(TAG, "onCreate() no channellist, recovery from destroyed process??");
            mChannelListManager.loadChannelList(true,
                    mModeSwitchManager.getMode() == CMMBServiceClient.CMMB_STATE_CONNECTED,true);
        }
        mChannelList = mChannelListManager.getChannelList();
        mChannelListManager.registerChannelListObserver(this);
        
        Bundle state = (Bundle)getLastNonConfigurationInstance();
        if (state == null) {
            state = savedInstanceState;
        }

        if (state == null) {
            //get the current channel and open channel now.
            Intent intent = getIntent();
            Log.d(TAG, "onCreate with intent:" + intent + " extras:" + intent.getExtras());
            mCurrentChannel = getChannelIndex(intent.getIntExtra(ESG.ServiceSpecial.SERVICE_ROW_ID, -1));
            myAssert(mCurrentChannel != -1);
        } else {
            mCurrentChannel = state.getInt(CURR_CHANNEL,-1);
            if (mChannelList.size() <= 0) {
                //if it is restart from OOM murder.
                mChannelListManager.loadChannelList(true,mModeSwitchManager.isMbbmsMode(),true);
                mChannelList = mChannelListManager.getChannelList();                
            }

            if (mCurrentChannel < 0 || mCurrentChannel >= mChannelList.size()) {
                Log.e(TAG, "Saved mCurrentChannel = " + mCurrentChannel + 
                    " is not in between 0 and (size:)" + mChannelList.size());
                finish();
            }
            //mMuteByUser = state.getBoolean(MUTED,false);
        }

        mWelcomeTextArray = new String[mChannelList.size()];
        
        setContentView(R.layout.player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NEEDS_MENU_KEY);
        
        mNMP = new NotificationManagerPlus.ManagerBuilder(this).create();
        
        mDefaultLoadingIcon = BitmapFactory.decodeResource(getResources(),R.drawable.loading_icon);
            
        mProgressView = findViewById(R.id.progress_indicator);
        mChannelNameView0 = (TextView)findViewById(R.id.channel_name0);
        mNotification = (TextView)findViewById(R.id.notification);
        mLoadingIcon = (ImageView)findViewById(R.id.logo);
        mProgressBar = (ProgressBar)findViewById(android.R.id.progress);
        mChannelNumberView = (TextView) findViewById(R.id.channel_number);
        mUnreadEB = (ImageView)findViewById(R.id.unread_eb);
        mUnreadEB.setImageDrawable(getResources().getDrawable(R.drawable.emb_icon));
        mPlayErrorView = (TextView) findViewById(R.id.player_error);
        initDlgNeedSubscrib();
        
        getWelcomeInfo();
        
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                   float velocityY) {
                Log.d(TAG, "onFling()");
                int dx = (int) (e2.getX() - e1.getX());

                if (Math.abs(dx) > MAJOR_MOVE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 0) {
                        Log.d(TAG, "gotoPreviousChannel()");
                        gotoPreviousChannel();
                    } else {
                        Log.d(TAG, "gotoNextChannel()");
                        gotoNextChannel();                        
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });


        //initAudioBox();
        registerCallStateListener();        
        
        //let volume key handle media volume control. 
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_HIDE_CHANNEL_NUMBER,0,0,null),CHANNEL_NUMBER_ON_DURATION); 


        play(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        Log.d(TAG, "onSaveInstanceState() mState = " + mState);    

        super.onSaveInstanceState(state);

        saveInstanceState(state);        
        mModeSwitchManager.onSaveInstanceState(state);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        Log.d(TAG, "onRetainNonConfigurationInstance() mState = " + mState);    
        Bundle state = new Bundle();    
        saveInstanceState(state);
        return state;
    }    

    private void saveInstanceState(Bundle state) {
        state.putInt(CURR_CHANNEL,mCurrentChannel);    
        //state.putBoolean(MUTED,mMuteByUser);    
        //state.putBoolean(STOPBYUSER,mStopByUser);            
    }    

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");    
        super.onStart();
        if (mService != null) {
            mService.blockTerminate(true);
        }

        mStarted = true;
        mModeSwitchManager.onActivityStart();

        mCallActive = isAnyActiveCall();
        if (mCallActive) {
            setMuteButtonEnabled(false);    
        } else {
            setMuteButtonEnabled(true);    
        }    
        
        Log.d(TAG, "onStart() mMuteByUser=" + mMuteByUser + ", mCallActive=" + mCallActive);
        if (mMuteByUser || mCallActive) {
            setMute(true);    
        } 
    }
    
    @Override
    public void onStop() {
        Log.d(TAG, "onStop() mService=" + mService); 
        super.onStop();
    mStarted = false;        
        mModeSwitchManager.onActivityStop();
    if (mMuted) {
        //do not interfere other music app since CMMB use same stream as Music.
        setMute(false);
    }
        
        mScreenOn = false;
     //   if(mService != null){
     //      mService.blockTerminate(false);
     //  }

    }  

    private boolean isAnyIncomingCall() {
        boolean result = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null)  {
                result = phone.isRinging();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "phone.isIdle() failed", e);
        }
        return result;
    }        

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() mState = " + mState);
    
        super.onDestroy();
        
        releasePlayer();
        stopOpenChannelThread(true);
        if (mService != null) {
            EBManager.removeEBListener(this);
            mService.unSetEventListener(this);
            mService.setOnTerminationListener(null);            
        }
        unregisterCallStateListener();
        
        mHandler.removeCallbacksAndMessages(null);        
        mChannelListManager.unregisterChannelListObserver(this);    

    }

    @Override
    protected void onNewIntent(Intent intent) {
        int ch = getChannelIndex(intent.getIntExtra(ESG.ServiceSpecial.SERVICE_ROW_ID, -1));    
        Log.i(TAG, "onNewIntent() ch = " + ch);
        
        if (mCurrentChannel != ch) {
            if (ch < mChannelList.size() && ch >= 0) {            
                mCurrentChannel = ch;

                //allow playOnResume
                mStopByUser = false;
                mPlayFailed = false;
                updateLoadingViews();
                closeChannel();//force openChannel to be called later.
            }            
        } else {    
            boolean half =  intent.getBooleanExtra(Utils.EXTRA_HALF_SCREEN,false);
        }
    }        
    

    @Override
    public boolean onKeyDown(int keyCode,KeyEvent ev) {
        Log.d(TAG, "onKeyDown() keyCode = " + keyCode);
        if (mState == STATE_CAPTURING) {
                //ignore clicking in such states.
                return super.onKeyDown(keyCode,ev);
        }

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            if (mFisrtNumberKey == KeyEvent.KEYCODE_UNKNOWN) {
                if (keyCode == KeyEvent.KEYCODE_0) {                    
                    checkChannelAndPlay(0);                    
                } else {
                    //TODO:show the number on screen immediately to give user a feedback on input.
                    mFisrtNumberKey = keyCode;
                    mHandler.sendEmptyMessageDelayed(MSG_COMBINE_KEYS,KEYS_COMBINATION_INTERVAL); 
                }
            } else {
                int num = (mFisrtNumberKey - KeyEvent.KEYCODE_0) * 10 
                              + keyCode - KeyEvent.KEYCODE_0;
                checkChannelAndPlay(num);
                mFisrtNumberKey = KeyEvent.KEYCODE_UNKNOWN;    
                mHandler.removeMessages(MSG_COMBINE_KEYS);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {            
            if (mControlPanel.getVisibility() == View.VISIBLE) {
                return false;
            } else {
                gotoPreviousChannel();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (mControlPanel.getVisibility() == View.VISIBLE) {
                return false;
            } else {
                gotoNextChannel();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK 
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    || keyCode ==  KeyEvent.KEYCODE_SPACE) {
            playStopByUser();                
            showControlPanel();
            if (mPlayStopButton != null) {
                mPlayStopButton.requestFocus();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP && isPlaying()) {
            playStopByUser();
            showControlPanel();
            if (mPlayStopButton != null) {
                mPlayStopButton.requestFocus();
            }            
            return true;
        } else {       
            return super.onKeyDown(keyCode,ev);
        }
    }

    public void onChannelListChange(boolean onUpdateServiceOk) {
        if (mChannelListManager.getChannelList().size() != mChannelList.size()) {
            //we only assume channel is changed due to favorite attribute changed.
            Log.e(TAG, "OnChannelListChanged size is changed!");        
            //return and will not update mChannelList.
            return;
        }
        
        synchronized (mChannelListManager) {
            mChannelList = mChannelListManager.getChannelList();
        }
        
        if (mFavoriteIndicator != null) {
            mFavoriteIndicator.setImageDrawable(isCurrentChannelMyFavorite() ? mRemoveFromFavorite 
                        : mAddToFavorite);       
        }
        refreshChannelList();
    }

    private void getWelcomeInfo() {
        Cursor cursor = getContentResolver().query(SG.SGDD.CONTENT_URI
                ,new String []{SG.SGCommonColumns.PRIVATE_EXT}
                ,SG.SGDDColumns.ID + " = 1"
                ,null
                ,null);        

        if (cursor != null) {
            Log.d(TAG, "cursor.getCount() =" + cursor.getCount());
                    
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                mWelcomeInfo = cursor.getString(0);
                Log.d(TAG, "wi =" + mWelcomeInfo);
            }
            cursor.close();        
        }
    }

    private void showWelcomeInfo() {        
        String welcome = mWelcomeTextArray[mCurrentChannel];
        if (welcome == null) {
            Cursor cursor = getContentResolver().query(SG.ServicePreviewData.getContentUri(getCurrentChannelRowID())
                    ,new String []{SG.ServicePreviewData.TEXT}
                    ,null
                    ,null
                    ,null);        

            if (cursor != null) {
                Log.d(TAG, "cursor.getCount() =" + cursor.getCount());
                        
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    mWelcomeTextArray[mCurrentChannel] = cursor.getString(0);
                    Log.d(TAG, "welcome text =" + welcome);
                }
                cursor.close();        
            }

            if (mWelcomeTextArray[mCurrentChannel] != null) {
                welcome = mWelcomeTextArray[mCurrentChannel];
            } else {
                //set an special value to remember not to read it again next time.
                mWelcomeTextArray[mCurrentChannel] = NO_WELCOME_TEXT;
                welcome = NO_WELCOME_TEXT;
            }
        } 

        if (welcome == NO_WELCOME_TEXT) {
            /*not equals*/
            welcome = mWelcomeInfo;
        }
        
        Log.d(TAG, "showWelcomeInfo welcome = " + welcome);
        
        if (welcome != null) {
            Toast.makeText(this,welcome,1000).show();
        }    
    }

    private String getStringById(int id) {
        return getResources().getString(id);
    }
    
    private void checkChannelAndPlay(int ch) {
        Log.i(TAG, "checkChannelAndPlay() ch = " + ch + " mCurrentChannel =" + mCurrentChannel);
        
        if (mCurrentChannel != ch) {
            if (ch < mChannelList.size()) {
                mCurrentChannel = ch;
                play(true);
            }
        } else {
            //according to CMCC testcase,we need to indicate the channel number to user in this case.
            showChannelNumber();
        }
    }
    
    private boolean isChannelListShowing() {
        return (mChannelListView != null && mChannelListView.getVisibility() == View.VISIBLE);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onTouchEvent()");
        if (mGestureDetector.onTouchEvent(ev)) {
            return true;
        }
        
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
             if (!isChannelListShowing()) {
                togglePanelVisibility();
                return true;
            }
        }
        
        return super.onTouchEvent(ev);
    }
                

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    Log.d(TAG, "onWindowFocusChanged() hasFocus = " + hasFocus + ",mScreenOn = " + mScreenOn);
        super.onWindowFocusChanged(hasFocus);

        if (!mScreenOn && hasFocus) {
        mScreenOn = true;
        if (!isFinishing()) {
            playOnResume();
        }
        } 
        
        Log.d(TAG, "onWindowFocusChanged() mScreenOn = " + mScreenOn);
    }

    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //just hide the control panel if it is being showed and do not show up options menu for this time.
        if (fadeOut(mControlPanel)) {
            Log.d(TAG, "onPrepareOptionsMenu() fadeOut(mControlPanel) == true");
            return false;
        } 
        
        if (mState == STATE_CAPTURING) {
                //wait capture done.
                return false;
        }    

        if (mDisableFullScreen) {
            menu.getItem(POSITION_FULL_SCREEN - 1).setTitle(R.string.full_screen).setIcon(R.drawable.menu_fullscreen);
        } else {
            menu.getItem(POSITION_FULL_SCREEN - 1).setTitle(R.string.exit_full_screen).setIcon(R.drawable.menu_nofullscreen);
        }
        return true;        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        addMenuItems(menu);
        return true;
    }

    private void addMenuItems(Menu menu) {
        menu.add(Menu.NONE, POSITION_GOTO_EM_LIST,
                POSITION_GOTO_EM_LIST, R.string.emergency_broadcast).setIcon(R.drawable.menu_emergence_broadcast);        
        menu.add(Menu.NONE, POSITION_GOTO_PROGRAM_LIST,
                POSITION_GOTO_PROGRAM_LIST, R.string.program_list).setIcon(R.drawable.menu_program_list);

        menu.add(Menu.NONE, POSITION_FULL_SCREEN,
                POSITION_FULL_SCREEN, R.string.exit_full_screen).setIcon(R.drawable.menu_nofullscreen);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case POSITION_GOTO_PROGRAM_LIST:
                gotoProgram();
                break;      
            case POSITION_GOTO_EM_LIST:
                gotoEbmList();
                break;  
            case POSITION_FULL_SCREEN:
                toggleFullScreen();
                break;              
            default :
                break;
        }
        return true;
    }

    public void onClick(View v) {
        Log.d(TAG, "onClick() mState = " + mState);

        //ignore clicking in such cases
        if (mState == STATE_CAPTURING || mPaused) {
            return;
        }

        
        switch (v.getId()) {
            case R.id.mute_control:
                Log.d(TAG, "mute_control mMuteByUser = " + mMuteByUser);
                mMuteByUser = !mMuteByUser;
                setMute(mMuteByUser);              
                break;
            case R.id.previous_control:
                Log.d(TAG, "previous_control mCurrentChannel = " + mCurrentChannel);
                gotoPreviousChannel();
                break;   
            
            case R.id.next_control: 
                Log.d(TAG, "next_control mCurrentChannel = " + mCurrentChannel);
                gotoNextChannel();
                break;   
            
            case R.id.channel_list:
                showChannelList();
                break;                
            case R.id.capture_control:
                doSnap();
                break;
            case R.id.favorite_control:
                int ret = mChannelListManager.revertFavoriteAttr(mCurrentChannel);
                mFavoriteIndicator.setImageDrawable(isCurrentChannelMyFavorite() ? mRemoveFromFavorite 
                        : mAddToFavorite);                 
      
                if (ret >= 0) {
                      int strId;

                      if (ret > 0) {
                        strId = R.string.favourite_added;
                    } else {
                        strId = R.string.favourite_removed;
                    }
                      
                    String text = String.format(getStringById(strId), getChannelName(mCurrentChannel));
                    Toast.makeText(this,text,500).show();            
                }

                break;    
            case R.id.play_stop:
                playStopByUser();
                break;                
            default:
                return;
        }
        
        mHandler.removeMessages(MSG_CONTROL_PANEL_OFF);
        mHandler.sendEmptyMessageDelayed(MSG_CONTROL_PANEL_OFF,CONTROL_PANEL_ON_DURATION);        
    }

    
    private void gotoPreviousChannel() {
        if (mChannelList.size() > 1) {
            if (--mCurrentChannel < 0) {
                //go to the last channel
                mCurrentChannel = mChannelList.size() - 1;
            }
            play(true);
        }
    }

    private void gotoNextChannel() {
        if (mChannelList.size() > 1) {
            if (++mCurrentChannel >= mChannelList.size()) {
              //go to the first channel
                mCurrentChannel = 0;
            }        
            play(true);
        }

    }    
    

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() mState = " + mState);    
        if (mState == STATE_CAPTURING) {
            // ignore backs while we're taking a picture
            return;
        } else if (mChannelListView != null && mChannelListView.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "onBackPressed() hideChannelList ");  
            hideChannelList();
            return;
        } else if (mState == STATE_OPENING) {
            stopOpenChannelThread(true);//to speed up the onDestroy process.
        }            
        mHandler.removeCallbacksAndMessages(null);
        super.onBackPressed();
    }

    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id,dialog);
        if (id == DIALOG_EMERGENCY_BROADCAST_3_4) {
            if (args == null) {
                return;
            }
            //we assign mEbmMessage here again because it may be recovered from OOM murder.
            mEbmMessage = args;
            AlertDialog dlg = (AlertDialog)dialog;
            int level = args.getInt("level",0);            
            
            final TextView alertTitle = (TextView)dialog.findViewById(R.id.alertTitle);     
            alertTitle.setText(getString(R.string.ebm_level,level));    

            final ImageView icon = (ImageView)dialog.findViewById(R.id.icon);  
            switch(level) {
                case 3:                
                    icon.setBackgroundResource(R.drawable.ebm_level3_animation);
                    break;
                  

                case 4:               
                    icon.setBackgroundResource(R.drawable.ebm_level4_animation);
                    break;
                default:
                   break;
            }
        } 
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dlg = null;
        switch (id) {
            case DIALOG_EMERGENCY_BROADCAST_3_4: 
                LayoutInflater factory = LayoutInflater.from(this);
                final View newEbmTitle = factory.inflate(R.layout.new_ebm_title, null);
                dlg = new AlertDialog.Builder(this)
                .setView(newEbmTitle)
                .setPositiveButton(R.string.read, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        gotoMessageDetails((Uri)mEbmMessage.getParcelable("uri"));
                    }
                })        
                .setNegativeButton(R.string.ignore, null)
                .create();
                 dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                     public void onDismiss(DialogInterface dialog) {
                         mEbmMessage = null;
                         if (mEbmAnimation != null) {
                            mEbmAnimation.stop();
                            mEbmAnimation = null;
                         }
                         checkUnreadEB();
                     }
                 });
                 
                 dlg.setOnShowListener(new DialogInterface.OnShowListener() {
                     public void onShow(DialogInterface dialog) {
                        //the android doc is not clear,AnimationDrawable.
                        //start could only work when its associated view is attached.
                        //so we start the animation when it is being showed.
                         final ImageView icon = (ImageView)((Dialog)dialog).findViewById(R.id.icon);    
                        mEbmAnimation = (AnimationDrawable)icon.getBackground();
                        mEbmAnimation.start();
                     }
                 });                     
                break; 
            default :
                break;
                            
        }
        return dlg;
    }    
    
    public void registerImage() {

        ContentValues values = new ContentValues(6);
        
        values.put(Images.Media.TITLE, mSnapFilename);
        
        // That filename is what will be handed to Gmail when a user shares a
        // photo. Gmail gets the name of the picture attachment from the
        // "DISPLAY_NAME" field.
        values.put(Images.Media.DISPLAY_NAME, mSnapFilename);
        
        values.put(Images.Media.DATE_TAKEN, mSnapDateTaken);
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.ORIENTATION,0);

        String filePath = CMMBMediaPlayer.getDefaultStoragePath() + "/" + mSnapFilename + ".jpg";
        values.put(Images.Media.DATA, filePath);

        Uri uri = getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null/*null means we got an error*/) {
            values.clear();
            values.put(Images.Media.SIZE, new File(filePath).length());

            getContentResolver().update(uri, values, null, null);

            //inform user it is done.
            String text = String.format(getStringById(R.string.saved_to),
                mSnapFilename + ".jpg",CMMBMediaPlayer.getDefaultStoragePath());
            Toast.makeText(this,text,500).show();            
        }        
    }

    private String createName(long dateTaken) {
        return DateFormat.format("yyyy-MM-dd kk.mm.ss", dateTaken).toString();
    }

    private void doSnap() {
        Log.d(TAG, "doSnap(), state:" + mState);
        if (mState != STATE_STARTED || mPreparing) {
            Log.d(TAG, "doSnap(), mPreparing:" + mPreparing);
            Toast.makeText(this,R.string.not_support_snap,1000).show();
            return;
        }

        mState = STATE_CAPTURING;
        mSnapDateTaken = System.currentTimeMillis();
        mSnapFilename = createName(mSnapDateTaken);// + ".jpg";
        mSnapThread = new Thread(new Runnable() {
            public void run() {                
                int errno = mMediaPlayer.capture(mSnapFilename);
                if (errno != CMMBMediaPlayer.CAPTURE_INFO_WAIT) {                
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_CAPTURE_FAILED,errno,0,null));
                }
            }
        });
        
        mSnapThread.start();            
    }            

    private void waitSnapThreadDied() {
        if (mSnapThread != null) {
            try {
                mSnapThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "waitSnapThreadDied() ignore");// do nothing
            }            
            mState = STATE_STARTED;            
            mSnapThread = null;                 
        }    
    }

    private void snapDone(String errmsg) {
        if (mState == STATE_CAPTURING) {    
            if (errmsg != null) {
                Toast.makeText(this,errmsg, 500).show();            
            } else {
                registerImage();
            }            
            mState = STATE_STARTED;
            mSnapThread = null;
        }        
    }

/*
    private void updateLastWatchTime() {
        Channel ch = mChannelList.get(mCurrentChannel);
        ContentValues cv = new ContentValues();
        
        if (!isMBBMSMode(mModeSwitchManager.getMode())) {
            cv.put(ESG.ServiceDetail.LAST_WATCH_TIME, getCurrentTime());
            getContentResolver().update(ESG.ServiceDetail.CONTENT_URI,cv
                    ,ESG.ServiceDetail.SERVICE_ID+"=? ", new String[]{ch.serviceID});
        } else {
            cv.put(SG.ServiceDetail.LAST_WATCH_TIME, getCurrentTime());
            getContentResolver().update(SG.ServiceDetail.CONTENT_URI,cv
                    ,SG.ServiceDetail.SERVICE_ID+"=? ", new String[]{ch.serviceID});            
        }
    }*/    

    
    private void refreshChannelList() {
        if (mChannelListView != null && mChannelListView.getVisibility() == View.VISIBLE) {
            mAdapter.notifyDataSetChanged();
        }        
    }

    private int getChannelIndex(int rowId) {
        int size = mChannelList.size();
        Log.d(TAG, "getChannelIndex: size=" + size);
        for (int i = 0;i < size;i++) {
            if (getChannelRowID(i) == rowId) {
                return i;
            }
        }    
        return -1;
    }    

    private int getChannelIndex(String serviceId) {
        int size = mChannelList.size();
        for (int i = 0;i < size;i++) {
            if (mChannelList.get(i).serviceID.equals(serviceId)) {
                return i;
            }
        }    
        return -1;
    }        

    //implements MBBMSService.OnTerminationListener
    public void onTerminate() {
        Log.d(TAG, "onTerminate() mState = " + mState);            
        switch(mState) {
        case STATE_CAPTURING:
            waitSnapThreadDied();    
            //fall through
        case STATE_STARTED:
            resetPlayer();
            //fall through
        case STATE_OPENED:
        case STATE_OPENING:             
            resetOnTermination();
            break;
        default:
            break;
            
        }
    }        
    
    //implements EBManager.EBListener
    public void notify(int level, String content, Uri uri) {            
        Log.d(TAG, "notify() level = " + level);            
        Bundle bd = new Bundle();        
        bd.putInt("level",level);
        bd.putParcelable("uri", uri);
        mHandler.sendMessage(mHandler.obtainMessage(
                MSG_NEW_EBM,level,0,bd));                    
                        
    }        
    
    //implements MBBMSService.OnEventListener
    public void event(int ev, int type, int arg, Object status) {
        Log.d(TAG, "event() ev " + ev + ", type=" + type + ", arg=" + arg + ", status=" + status);
        
        Channel ch = mChannelList.get(mCurrentChannel);
        
        if (ev == CMMBServiceClient.CMMB_EVENT_CLIENT_INFO) {
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
                case CMMBServiceClient.CMMB_INFO_RETRIEVE_MSK_FAIL:
                    
                    if (!ch.isEncrypted()) {
                        Log.d(TAG, "event() receive CMMB_INFO_RETRIEVE_MSK_FAIL on none-encrypted!");
                        break;
                    }

                    if (mState == STATE_STARTED) {
                        mHandler.removeMessages(MSG_MSK_FAIL);
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_MSK_FAIL,arg,0,null));
                    }
                    break;
                case CMMBServiceClient.CMMB_INFO_RETRIEVE_MSK_SUCCEED:
                    if (!ch.isEncrypted()) {
                        Log.d(TAG, "event() receive CMMB_INFO_RETRIEVE_MSK_SUCCEED on none-encrypted!");
                        break;
                    }

                    if (mState == STATE_OPENING) {
                        mHandler.removeMessages(MSG_OPEN_DONE);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_OPEN_DONE,0,-1, null),OPEN_TIME_OUT_INTERVAL);
                    }
                    break;
                case CMMBServiceClient.CMMB_INFO_RETRIEVE_MSK_START:
                    if (!ch.isEncrypted()) {
                        Log.d(TAG, "event() receive CMMB_INFO_RETRIEVE_MSK_START on none-encrypted!");
                        break;
                    }

                    if (mState == STATE_OPENING && !mHasStartRetrieveKey) {
                        mHasStartRetrieveKey = true;
                        mHandler.removeMessages(MSG_OPEN_DONE);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_OPEN_DONE,0,-1,
                        getStringById(R.string.cmcc_net_error)),OPEN_TIME_OUT_INTERVAL2);
                    } else if (mState == STATE_STARTED & !mHasStartRetrieveKeyOnPlay) {
                        mHasStartRetrieveKeyOnPlay = true;
                        mHandler.removeMessages(MSG_MSK_FAIL);
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_MSK_FAIL,arg,0,null));
                    }
                    break;
                    
                default:
                     break;
            }
        } else if (ev == CMMBServiceClient.CMMB_EVENT_CLIENT_ERROR) {
            if (type == -38) {
                return;
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_OPEN_DONE,0,ev,null));
        }
    }        

    private boolean bindMBBMSService() {
        if (mService == null) {   
            mService = com.mediatek.cmmb.app.ServiceManager.getServiceManager(this).getService();
            if (mService == null) {
                Log.e(TAG, "bindMBBMSService() get Service from ServiceManager failed!");            
                return false;
            }
            
            mService.setEventListener(this);
            mService.setEBMListener(EBManager.getInstance(this));
            if (!EBManager.containEBListener(this)) {
                EBManager.addEBListener(this);
            }
            mService.setOnTerminationListener(this);
        }        
        return true;
    }


    private void closeVisualizer() {
        //visualizer code
        
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer = null;
            mAudioBox.setVisibility(View.GONE);     
            //mSurfaceView.setVisibility(View.VISIBLE);
        }        
    }    

    private boolean checkIfChannelAvailable() {
        Channel ch = mChannelList.get(mCurrentChannel);
        if (ch.isFree()) {
            return true;
        } else {
            if (ch.isSubscribed()) {
                return true;
            } else {                
                return false;
            }
        }
    }
    
    private int getNotificationString() {
        if (checkIfChannelAvailable()) {
            return R.string.loading;
        } else {            
            if (!mModeSwitchManager.isMbbmsMode()) {    
                    return R.string.channel_unavailable_now2;
            } else {                       
                    return R.string.need_subscription_hint2;
            }
        }
    }       


    private void goBackToMain() {
        Intent intent = new Intent();
        intent.setClass(this, MainScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    
    private void gotoProgram() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setClass(this, PlaybillActivity.class);
        Bundle bundle = new Bundle();
        Channel ch = mChannelList.get(mCurrentChannel);

        bundle.putString(SG.ServiceDetail.SERVICE_ID, ch.serviceID);
        bundle.putString(ESG.Service.SERVICE_NAME, ch.name);
        bundle.putInt(ESG.ServiceSpecial.SERVICE_ROW_ID, ch.rowID);
        bundle.putInt(SG.ServiceDetail.IS_FAVORITE, ch.isFavorite ? 1 : 0);
        bundle.putInt(SG.Access.FREQUENCY_NO, ch.freq);
        bundle.putBoolean(SG.ServiceDetail.FOR_FREE, ch.isFree());

        intent.putExtras(bundle);

        //PlaybillActivity and this activity is designed in single-instance model,
        //so we bring it to front if it is already in the stack. 
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }        

    private void gotoEbmList() {
        Intent intent = new Intent();
        intent.setClass(this,MessageListActivity.class);
        startActivity(intent);
    }        

    private void gotoMessageDetails(Uri uri) {
        Intent intent = new Intent();
        intent.setClass(this,MessageDetailActivity.class);
        intent.setData(uri);
        startActivity(intent);
    }            

    private void play(boolean updateViews) {
        Log.d(TAG, "play() mState = " + mState + " updateViews = " + updateViews); 
        
        mPlayFailed = false;
        mStopByUser = false;
        
        if (mPreviousChannel != mCurrentChannel) {
            mPreviousChannel = mCurrentChannel;
            if (mModeSwitchManager.isMbbmsMode()) {
                showWelcomeInfo();
            }        
        }
        
        switch(mState) {
            case STATE_IDLE:            
                openChannel();
                break;
            case STATE_OPENING:
                mChannelChanged = true;         
                break;
            //play should not be called in STATE_CAPTURING state,
            //But if it did happen,we will discard the capturing picture 
            case STATE_CAPTURING:               
                waitSnapThreadDied();
                //fall through            
            case STATE_STARTED:
                resetPlayer();
                //fall through            
            case STATE_OPENED:
                closeVisualizer();
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

    private void showChannelNumber() {
        mChannelNumberView.setVisibility(View.VISIBLE);        
        mChannelNumberView.setTextSize(35);
        mChannelNumberView.setText(String.valueOf(mCurrentChannel)); 
        mHandler.removeMessages(MSG_HIDE_CHANNEL_NUMBER);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_HIDE_CHANNEL_NUMBER,0,0,null)
                ,CHANNEL_NUMBER_ON_DURATION);         
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
        private boolean mInterrupt = false;
        private Object mLock = new Object();

        
        OpenChannelThread(boolean isMbbms) {
            super();
            mIsMbbms = isMbbms;
        }
        
        @Override
        public void run() {        
            bindMBBMSService();
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

                //test start
                //mHandler.sendMessage(mHandler.obtainMessage(MSG_OPEN_DONE,0,TUNE_FAILED,getErrorDescription(result)));
                //test end
                
                if (!mResult) {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_OPEN_DONE,0
                        ,result.code.equals(MBBMSService.STATUS_FAIL_TUNE_FAILED) ? -1 : 0
                        ,getErrorDescription(result)));
                }
                
                Log.d(TAG, "openChannel mResult = " + mResult);

                try {
                    waitOnThread();
                } catch (InterruptedException e) {
                    Log.d(TAG, "openChannelThread interrupt");
                    mHandler.removeMessages(MSG_OPEN_DONE);
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
            synchronized (mLock) {
                mReopen = false;
                if (isAlive()) {
                    Log.d(TAG, "stopThread, send interrupt ");
                    interrupt();
                    mInterrupt = true;
                }
            }
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
                        Log.d(TAG, "open ignore");  //ignore.
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
                    mLock.notify();
                    mResult = false;
                } else if (mDoClose) {
                    callServiceCloseChannel();                    
                }
                
                if (mInterrupt) {
                    Log.d(TAG, "openChannelThread pending interrupt");
                    mInterrupt = false;
                    throw new InterruptedException();
                } else {
                    Log.d(TAG, "waitOnThread, enter wait, mReopen = " + mReopen);
                    while (!mReopen) {
                        mLock.wait();
                        if (mTerminated) {
                            mTerminated = false;
                            mDoClose = false;
                            
                            mLock.notify();
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
    }
    
    private void openChannel() {
        Log.d(TAG, "openChannel() mState = " + mState);

        if (mState != STATE_IDLE) {
            return;
        }

        mHasStartRetrieveKey = false;
        mChannelChanged = false;
        //channel is going to changed,so hide and reset channel dependent stuffs.        
        resetSignalState();
        
        if (!checkIfChannelAvailable()) {
            return;
        }        
        mHandler.removeMessages(MSG_PLAYER_COMPLETE);
        mHandler.removeMessages(MSG_HIDE_LOADING);
        mHandler.removeMessages(MSG_OPEN_DONE);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_OPEN_DONE,0,-1,null),OPEN_TIME_OUT_INTERVAL);
        
        if (mOpenThread == null) {
            mOpenThread = new OpenChannelThread(mModeSwitchManager.isMbbmsMode());                    
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
            mState = STATE_IDLE;
        }
    }

    private void resetOnTermination() {
        Log.d(TAG, "resetOnTermination() mState =" + mState);
        
        if (mOpenThread != null && !isAnyActiveCall()) {
            mOpenThread.notifyTermination();
        }
        mHandler.removeMessages(MSG_OPEN_DONE);
        mState = STATE_IDLE;        
    }

    private void stopOpenChannelThread(boolean force) {
        Log.d(TAG, "stopOpenChannelThread() mState =" + mState + " force = " + force);
        if (force) {
            destroyOpenChannelThread();
        } else if (mState == STATE_OPENING) {
            mOpenThread.close();
            mHandler.removeMessages(MSG_OPEN_DONE);             
            mState = STATE_IDLE;
        }

    }    

        
    private void destroyOpenChannelThread() {
        if (mOpenThread != null) {
            mOpenThread.stopThread();
            try {
                mOpenThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "destroyOpenChannelThread ignore ");  //ignore
            }
            mOpenThread = null;            
            mHandler.removeMessages(MSG_OPEN_DONE); 
            if (mState == STATE_OPENING) {
                mState = STATE_IDLE;
            }
        }
    }

    private int getSignal() {        
        Log.d(TAG, "getSignal()");
        if (mService == null) {
            //service is not ready yet.
            Log.w(TAG, "getSignal() mService == null");
            return LEVEL_SIGNAL_MIN;
        } else {
            return mService.getProp(CMMBServiceClient.CMMB_PROPERTY_SIGNAL_QUALITY);
        }
    }

    private void onSignalUpdate(int level) {
        if (mState < STATE_OPENED) {
            Log.w(TAG, "onSignalUpdate() early return mState = " + mState);
            return;
        }
        
        Log.d(TAG, "onSignalUpdate() level = " + level + " mSignalState = " + mSignalState);
        
        //level:0~50
        if (level >= LEVEL_SIGNAL_MIN && level <= LEVEL_SIGNAL_UNAVAILABLE_MAX && 
              mSignalState != SIGNAL_STATE_UNAVAILABLE) {
            stopByNoSignal();            
        } else if (level > LEVEL_SIGNAL_UNAVAILABLE_MAX && level <= LEVEL_SIGNAL_TRANSIENT_MAX 
                    && mSignalState != SIGNAL_STATE_UNAVAILABLE && mSignalState != SIGNAL_STATE_TRANSIENT) {    
            mSignalState = SIGNAL_STATE_TRANSIENT;    
            mSignalIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_none));
        } else if (level > LEVEL_SIGNAL_TRANSIENT_MAX && 
                level <= LEVEL_SIGNAL_WEAK_MAX && 
                mSignalState != SIGNAL_STATE_WEAK) {    
            int oldState = mSignalState;
            mSignalState = SIGNAL_STATE_WEAK;    
            mSignalIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_weak));
            if (oldState == SIGNAL_STATE_UNAVAILABLE && !mPaused) {
                Toast.makeText(this,R.string.signal_recover,1000).show();
                startPlayer();
            }             
        } else if (level > LEVEL_SIGNAL_WEAK_MAX && 
                level <= LEVEL_SIGNAL_MEDIUM_MAX && 
                mSignalState != SIGNAL_STATE_MEDIUM) {
            int oldState = mSignalState;
            mSignalState = SIGNAL_STATE_MEDIUM;            
            mSignalIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_medium));            
            if (oldState == SIGNAL_STATE_UNAVAILABLE && !mPaused) {
                Toast.makeText(this,R.string.signal_recover,1000).show();
                startPlayer();
            }    
        } else if (level > LEVEL_SIGNAL_MEDIUM_MAX && level <= LEVEL_SIGNAL_MAX && 
                mSignalState != SIGNAL_STATE_NORMAL) {
            int oldState = mSignalState;
            mSignalState = SIGNAL_STATE_NORMAL;            
            mSignalIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_normal));            
            if (oldState == SIGNAL_STATE_UNAVAILABLE && !mPaused) {
                Toast.makeText(this,R.string.signal_recover,1000).show();                
                startPlayer();
            }    
        }
    }

    private void resetSignalState() {
        Log.d(TAG, "resetSignalState() mSignalState = " + mSignalState);
        
        mSignalState = SIGNAL_STATE_UNKNOWN;    
        if (mSignalIndicator != null) {
            mSignalIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_none));
        }
    }
    
    private boolean isCurrentChannelMyFavorite() {  
        return mChannelList.get(mCurrentChannel).isFavorite;    
    }    

    private int getChannelRowID(int index) {    
        return mChannelList.get(index).rowID;    
    }    

    private int getCurrentChannelRowID() {    
        return getChannelRowID(mCurrentChannel);    
    }

    private String getCurrentChannelID() {    
        return mChannelList.get(mCurrentChannel).serviceID;    
    }    

    private String getChannelName(int index) {    
        return mChannelList.get(index).name;    
    }

    private String getCurrentChannelName() {    
        return getChannelName(mCurrentChannel);    
    }    

    private Bitmap getLoadingIcon() {
        Bitmap bp = mChannelList.get(mCurrentChannel).logo;
        
        Log.d(TAG, "bp = " + bp + " mDefaultLoadingIcon = " + mDefaultLoadingIcon);
        if (bp == null) {
            bp = mDefaultLoadingIcon;
        }
        return bp;
    }

    private boolean isPlaying() {
        return !(mPlayFailed || mStopByUser 
                || mSignalState == SIGNAL_STATE_UNAVAILABLE);
    }

    private void playStopByUser() {
        Log.d(TAG, "playStopByUser() mState = " + mState); 
        if (isPlaying()) {    
            mStopByUser = true;
            showStoppedViews();
            stop();            
        } else {
            play(true);
        }
    }
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged() mState = " + mState + ", mSurfaceHolder=" + mSurfaceHolder);

        /* Make sure we have a surface in the holder before proceeding for 
         * surfaceCreated() will be called with a invalid surface if we rotate the screen 
         * when a NO_DISPLAY window overlaps on player activity. And surfaceDestroyed() will 
         * not be called for this surface.
         */
        if (!holder.getSurface().isValid()) {
            Log.e(TAG, "holder.mSurface not valid");
            return;
        }

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
                onSignalUpdate(signal);/*getSignal()*/
                startPlayer();            
                break;                    
            default:
                //ingore asnchronous events because it may already in queue when state is changed.
                return;
        }
    }

    private String getSignalWeakMsg() {
        /*
        int resID = R.string.signal_weak_stop_internal;
        String antennaType = SystemProperties.get("ro.cmmb.antenna");
        
        if(antennaType != null){
            if(antennaType.equals(ANTENNA_TYPE_EARPHONE)){
                resID = R.string.signal_weak_stop_ep;
            }else if(antennaType.equals(ANTENNA_TYPE_PULL)){
                resID = R.string.signal_weak_stop_pull;
            }else if(antennaType.equals(ANTENNA_TYPE_PLUG)){
                resID = R.string.signal_weak_stop_plug;
            }
        }
        */
        
        return getStringById(R.string.signal_none);
    }
    private void openFalse(String error) {    
        
        Log.d(TAG, "openFalse() mState = " + mState + ", error=" + error);        
        switch (mState) {            
            case STATE_OPENING:    
                mOpenThread.close();
                Channel ch = mChannelList.get(mCurrentChannel);
                String errormsg;
                if (error != null && ch.isEncrypted()) {
                    errormsg = error;
                } else {
                    errormsg = getSignalWeakMsg();
                }
                stopByError(errormsg);    
                mState = STATE_IDLE;
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
                mAudioBox.setVisibility(View.VISIBLE);
                setupVisualizer();

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
 
    private void onMskFail() {
        Log.d(TAG, "onMskFail() mState = " + mState);
        if (mState == STATE_OPENING) {
            openFalse(getStringById(R.string.cmcc_net_error));
        } else if (mState == STATE_STARTED) {
            if (mModeSwitchManager.isMobileSignalAvailable()) {
                stopByError(getStringById(R.string.cmcc_net_error2));
            } else {
                stopByError(getStringById(R.string.cmcc_net_error));
            }
        }
    }
    
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError() mState = " + mState + " what = " + what);  
        
        switch (what) {
            case CMMBMediaPlayer.CAPTURE_ERR_NULLPARAM:            
            case CMMBMediaPlayer.CAPTURE_ERR_COLORCONVERT:
            case CMMBMediaPlayer.CAPTURE_ERR_FILECLOSE:
            case CMMBMediaPlayer.CAPTURE_ERR_FILEOPEN:
            case CMMBMediaPlayer.CAPTURE_ERR_FILEWRITE:
            case CMMBMediaPlayer.CAPTURE_ERR_SKIA:
            case CMMBMediaPlayer.CAPTURE_ERR_BUSY:    
                snapDone(getStringById(R.string.capture_fail));
                break;
            case CMMBMediaPlayer.CAPTURE_ERR_NOSTORAGE:                    
                snapDone(getStringById(R.string.no_storage));            
                break;
            case CMMBMediaPlayer.CAPTURE_ERR_MEDIA_CHECKING:                    
                snapDone(getStringById(R.string.please_wait));            
                break;                
            case CMMBMediaPlayer.CAPTURE_ERR_LOWSTORAGE: 
                snapDone(getStringById(R.string.spaceIsLow_content));                        
                break;
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
        switch (what) {
            case CMMBMediaPlayer.CAPTURE_INFO_OK:
                snapDone(null);
                return true;
            
            case CMMBMediaPlayer.PLAYER_START_RENDER:
                mHandler.sendEmptyMessageDelayed(MSG_HIDE_LOADING, 0);
                return true;
            default:
                break;
        }
        return false;
    }    

    public void onCompletion(MediaPlayer mp)    {
        if (mState != STATE_STARTED) {
            return;
        }
        
        Log.d(TAG, "onCompletion() mState = " + mState);
        mHandler.sendEmptyMessageDelayed(MSG_PLAYER_COMPLETE, PLAYER_COMPLETE_INTERVAL);
        //according to driver's comment,treat it as "too weak to play" signal state.
        /*
        if (mSignalState != SIGNAL_STATE_UNAVAILABLE) {
            stopByNoSignal();        
        }
        */
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
                mPlayErrorView.setVisibility(View.INVISIBLE);
            } else {
                stopByNoSignal();
            }
        }
    }


    private void releasePlayer() {
        Log.d(TAG, "releasePlayer() mState = " + mState); 
        if (mMediaPlayer != null) {
            closeVisualizer();            
            mMediaPlayer.release();    
            //unlockPower();                        
            mPreparing  = false;
            mMediaPlayer = null;
        }
    }

    private void resetPlayer() {
        Log.d(TAG, "resetPlayer() mState = " + mState); 
        if (mState == STATE_STARTED) {
            closeVisualizer();
            mMediaPlayer.reset();
            //unlockPower();            
            mPreparing  = false;
            mState = STATE_OPENED;             
        }
    }    

    /*private void lockPower() {
        Log.d(TAG, "lockPower() mPowerLocked = "+mPowerLocked); 
        
        if (!mPowerLocked) {
            mPowerLocked = true;
            mService.lockPower();
        }
    }

    private void unlockPower() {
        Log.d(TAG, "unlockPower() mPowerLocked = "+mPowerLocked); 
        
        if (mPowerLocked) {
            mPowerLocked = false;
            mService.unlockPower();
        }
    }*/    
    
    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        Log.d(TAG, "onTrackballEvent() "); 
        
        if (!isChannelListShowing()) {
            showControlPanel();       
        }
        return false;
    }

    private void initAll() {
        Log.d(TAG, "initAll() mInitialized = " + mInitialized);
        if (mInitialized) {
            return;
            //already initialized.
        }
        initVideoSurface();
        initControlPanel();
        initChannelListPopup();
        initAudioBox();
        mInitialized = true;
    }

    private void initAudioBox() {
        
        Log.d(TAG, "initAudioBox()"); 
        mAudioBox = ((ViewStub)findViewById(R.id.audio_box)).inflate();  
        mAudioBox.setVisibility(View.INVISIBLE);        
        mVisualizerView = (VisualizerView) findViewById(R.id.visualizer);
    }
    
    private void initVideoSurface() {
        Log.d(TAG, "initVideoSurface()"); 
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.setVisibility(View.VISIBLE);        

    }

    private void initControlPanel() {
        Log.d(TAG, "initControlPanel()"); 
        mControlPanel = ((ViewStub)findViewById(R.id.control_panel)).inflate();
        View.OnTouchListener l = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                showControlPanel();
                return true;
            }
            
        };

        mControlPanel.setVisibility(View.INVISIBLE);
        findViewById(R.id.top_panel).setOnTouchListener(l);        
        findViewById(R.id.bottom_panel).setOnTouchListener(l);
        
        mChannelNameView = (TextView) findViewById(R.id.channel_name);
        mChannelNameView.setText(getCurrentChannelName());
        
        mAddToFavorite = getResources().getDrawable(R.drawable.add_to_favorite);
        mRemoveFromFavorite = getResources().getDrawable(R.drawable.remove_from_favorite);

        
        mFavoriteIndicator = (ImageView) findViewById(R.id.favorite_control);
        mFavoriteIndicator.setOnClickListener(this);
        mFavoriteIndicator.setImageDrawable(isCurrentChannelMyFavorite() ? mRemoveFromFavorite 
                    : mAddToFavorite);       
        
        mSnapButton = (ImageView) findViewById(R.id.capture_control);
        mSnapButton.setOnClickListener(this);    
        setSnapButtonEnabled(mChannelList.get(mCurrentChannel).isTv());

        mSignalIndicator = (ImageView) findViewById(R.id.signal);
        mSignalIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_none));

        findViewById(R.id.previous_control).setOnClickListener(this);
        findViewById(R.id.channel_list).setOnClickListener(this);
        findViewById(R.id.next_control).setOnClickListener(this);

        mMuteView = getResources().getDrawable(R.drawable.btn_ic_mute);
        mUnmuteView = getResources().getDrawable(R.drawable.btn_ic_unmute); 
        mMuteButton = (ImageView)findViewById(R.id.mute_control);
        mMuteButton.setOnClickListener(this);
        mMuteButton.setImageDrawable(mMuted ? mMuteView : mUnmuteView);   
        if (isAnyActiveCall()) {
            setMuteButtonEnabled(false);
        }
        
        mPlayView = getResources().getDrawable(R.drawable.btn_ic_play);
        mStopView = getResources().getDrawable(R.drawable.btn_ic_stop);     
        mPlayStopButton = (ImageView) findViewById(R.id.play_stop);
        mPlayStopButton.setOnClickListener(this);
    }

    private void initDlgNeedSubscrib() {
        Log.d(TAG, "initDlgNeedSubscrib()"); 
        mDlgNeedSubscrib = ((ViewStub)findViewById(R.id.dlg_needsubscrib)).inflate();
        mDlgNeedSubscrib.setVisibility(View.GONE);

        mDlgNeedSubscribMsg = (TextView)findViewById(R.id.dlg_needsubscrib_message);
        mDlgNeedSubscribMsg.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                Log.d(TAG, "subcrib_msg onClick");
                togglePanelVisibility();
            }
        });

        findViewById(R.id.subscrib_dlg_button1).setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
            
                Log.d(TAG, "subcrib_entrance onClick");
                Intent intent = new Intent();
                intent.setClass(PlayerActivity.this, PackageActivity.class);
                intent.putExtra(SG.Service.IS_SUBSCRIBED, false);
                startActivity(intent);

            }
        });
    }
    
    private void showDlgNeedSubscrib(boolean show) {
        if (show) {
            String msg = String.format(getResources().getString(R.string.need_subscription_hint), 
                    mChannelList.get(mCurrentChannel).name);
            mDlgNeedSubscribMsg.setText(msg);
            mDlgNeedSubscrib.setVisibility(View.VISIBLE);
        } else {
            mDlgNeedSubscrib.setVisibility(View.GONE);
        }
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
        mControlPanel.setVisibility(View.INVISIBLE);
        
        mChannelListView.setVisibility(View.VISIBLE);        
        mChannelListView.setSelection(mCurrentChannel);
    }    

    private void hideChannelList() {
        Log.d(TAG, "hideChannelList()");
        mChannelListView.setVisibility(View.INVISIBLE);        
    }    

    private void togglePanelVisibility() {
        Log.d(TAG, "togglePanelVisibility() visibility = " + mControlPanel.getVisibility());
        if (mControlPanel == null) {
            return;
            //not initialize yet.
        }
        if (mControlPanel.getVisibility() == View.VISIBLE) {
            mHandler.removeMessages(MSG_CONTROL_PANEL_OFF);             
            fadeOut(mControlPanel);                
        } else {
            showControlPanel();            
        }
    }

    private boolean fadeOut(View view) {
        if (null == view || view.getVisibility() == View.INVISIBLE) {
            return false;
        } 
        
        view.setVisibility(View.INVISIBLE);
        
        Animation animation = null;
        animation = new AlphaAnimation(1F, 0F);
        animation.setDuration(ANIMATION_DURATION);
        view.startAnimation(animation);
        
        return true;
    }
    private boolean fadeIn(View view) {
        return fadeIn(view, 0);
    }
    private boolean fadeIn(View view, int offset) {
        if (null == view || view.getVisibility() == View.VISIBLE) {
            return false;
        }
        view.setVisibility(View.VISIBLE);
        
        Animation animation = null;
        animation = new AlphaAnimation(0F, 1F);
        animation.setDuration(ANIMATION_DURATION);
        animation.setStartOffset(offset);
        view.startAnimation(animation);
        
        return true;
    }    
    
    private void showControlPanel() {
        mHandler.removeMessages(MSG_CONTROL_PANEL_OFF);         
        fadeIn(mControlPanel);
        mHandler.sendEmptyMessageDelayed(MSG_CONTROL_PANEL_OFF,CONTROL_PANEL_ON_DURATION);          
    }

    private void updateLoadingViews() {
        //change all channel attribute indicators.
        showChannelNumber();
        mChannelNameView0.setText(getCurrentChannelName());    

        if (mInitialized) {        
            mChannelNameView.setText(getCurrentChannelName());
            mFavoriteIndicator.setImageDrawable(isCurrentChannelMyFavorite() ? mRemoveFromFavorite 
                        : mAddToFavorite);   
            setSnapButtonEnabled(mChannelList.get(mCurrentChannel).isTv());    
            mPlayStopButton.setImageDrawable(mStopView);                    
        }
        
        mLoadingIcon.setImageBitmap(getLoadingIcon());    

        int strId = getNotificationString();
        if (strId == R.string.loading) {        
            showLoadingViews();
        } else if (strId == R.string.need_subscription_hint2) {
            stopByNeedSubscrib(getStringById(strId));
        } else {
            stopByValidationFailure(getStringById(strId));
        }
    }
    
    private void displayOSDViews(boolean loading,String notification) {
        showDlgNeedSubscrib(false);
        mProgressView.setVisibility(View.VISIBLE); 
        mProgressBar.setVisibility(loading ? View.VISIBLE : View.INVISIBLE); 
        mNotification.setText(notification);
    }

    private void showLoadingViews() {
        displayOSDViews(true,getStringById(R.string.loading));
    }
    
    private void showNoSignalViews() {        
        mPlayErrorView.setText(R.string.signal_none);
        mPlayErrorView.setVisibility(View.VISIBLE);
        //displayOSDViews(false,getStringById(R.string.signal_none));        
    }    

    private void showPlayFailedViews(String othererror) {
        displayOSDViews(false,othererror != null ? othererror : getStringById(R.string.open_channel_fail));
    }    

    private void showStoppedViews() {        
        displayOSDViews(false,null);        
    }    


    private void stopByNoSignal() {
        mSignalIndicator.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_none));
        mSignalState = SIGNAL_STATE_UNAVAILABLE;    
        stop();                
        if (!mStopByUser) {
            Log.d(TAG, "stopByNoSignal() error = " + getStringById(R.string.signal_none));
            showNoSignalViews();
        }
    }    

    private void stopByError(String othererror) {    
        Log.d(TAG, "stopByError() othererror = " + othererror); 
        
        mPlayFailed = true;    
        stop();        
        if (!mStopByUser) {
            showPlayFailedViews(othererror);
        }
    }

    private void stopByValidationFailure(String othererror) {    
        Log.d(TAG, "stopByValidationFailure() othererror=" + othererror); 
        mPlayFailed = true;        
        showPlayFailedViews(othererror);
        stop();        
    }    
    
    private void stopByNeedSubscrib(String othererror) {
        Log.d(TAG, "stopByNeedSubscrib()"); 
        mPlayFailed = true;        
        mPlayErrorView.setVisibility(View.INVISIBLE);
        mProgressView.setVisibility(View.INVISIBLE); 
        mProgressBar.setVisibility(View.INVISIBLE); 
        showDlgNeedSubscrib(true); 
        stop();        
    }    
    
    private void stop() {
        Log.d(TAG, "stop() mState = " + mState); 

        switch (mState) {
            case STATE_CAPTURING:
                waitSnapThreadDied();
                //fall through
            case STATE_STARTED:
                resetPlayer();
                break;
            default:
                break;
        }

        if (mPlayStopButton != null) {
            mPlayStopButton.setImageDrawable(mPlayView);                        
        }
    }        
    
    private void startPlayer() {
        Log.d(TAG, "startPlayer() mState = " + mState);    

        if (mState != STATE_OPENED || mSurfaceHolder == null || mPaused || mStopByUser || !mScreenOn) {
            Log.d(TAG, "startPlayer() return before prepare,mSurfaceHolder = " + mSurfaceHolder
                + " mPaused = " + mPaused + " mStopByUser = " + mStopByUser + " mScreenOn = " + mScreenOn);
            return;
        }

        /* Just a redundant check */
        if (!mSurfaceHolder.getSurface().isValid()) {
            Log.e(TAG, "startPlayer() surface not valid!!!");
            return;
        }

        if (mPlayStopButton != null) {
            mPlayStopButton.setImageDrawable(mStopView);                        
        }                

        if (mSignalState != SIGNAL_STATE_UNAVAILABLE) {    
            prepare();            
            mHasStartRetrieveKeyOnPlay = false;
            mState = STATE_STARTED;                
            //already done in onResume.
            //TBD:If CMMB is a seperate stream we can remove setMute in onResume and do it only here like mATV.
            //setMute(mMuteByUser);
        } else {
            stopByNoSignal();
        }
    }
    
    private void setMute(boolean state) {
        Log.d(TAG, "setMute() state = " + state + " mMuted = " + mMuted);
        if (mMuted != state) {
            mMuted = state;
            if (mMuteButton != null) {
                mMuteButton.setImageDrawable(mMuted ? mMuteView : mUnmuteView); 
            }

            final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC,state);
        }
    }

    private void setMuteButtonEnabled(boolean enable) {
        if (mMuteButton != null) {
            mMuteButton.setFocusable(enable);
            mMuteButton.setClickable(enable);    
        }
    }

    private void setSnapButtonEnabled(boolean enable) {
        if (mSnapButton != null) {
            mSnapButton.setFocusable(enable);
            mSnapButton.setClickable(enable);    
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
        case STATE_IDLE:
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
        Log.d(TAG, "onResume() mState = " + mState + ", mScreenOn=" + mScreenOn + ", mCallActive=" + mCallActive);     
        super.onResume();
        /* to avoid not block terminate when resume from pause without re-start */
        if (mService != null) {
            mService.blockTerminate(true);
        }
        if (mPaused) {
            mPaused = false;            
            /* ALPS00324578
             * When screen on from lock screen player will be overlapped by Key guard screen.
             * Player is not visible at resume state at this scenario. So we delay play operation 
             * to window of player get focus. But we cannot depends focus completely for the focus 
             * change sequence at screen on is: player --> key guard screen. So the flag of focus is 
             * set at gain focus while reset at onStop().
             * onWindowFocusChanged() will not be called when screen on from listen background call 
             * for there's no other window overlapped over player. So we must set mScreenOn at here.
             *  
             */
            if (mScreenOn) {
                //see ALPS00030854.
                KeyguardManager km = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE); 
                if (!km.inKeyguardRestrictedInputMode()) {                    
                    playOnResume();
                }
            } else if (mCallActive) {    
                mScreenOn = true;
                playOnResume();
            }            
        }
        
        if (mMuted) {
            setMute(true);
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

        if (!mNMPLisening) {
            mNMPLisening = true;
            mNMP.startListening();
        }        

        setStatusBarExpanded(false);        
        checkUnreadEB();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause() mState = " + mState);
        super.onPause();

        switch(mState) {
        case STATE_CAPTURING:
            waitSnapThreadDied();    
            //fall through
        case STATE_STARTED:
            resetPlayer();
            break;
        case STATE_OPENING:
            stopOpenChannelThread(false);
            break;
        default:
            break;
            
        }

        if (mNMPLisening) {
            //do not stop listening if we are going to CallScreen to allow missing call dialog to be showed.
            if (!isAnyIncomingCall()) {
                Log.d(TAG, "onPause() stopListening"); 
                mNMPLisening = false;
                mNMP.stopListening();    
            }
        }        

        mHandler.removeMessages(MSG_COMBINE_KEYS);   
        mPaused = true;

        setStatusBarExpanded(true);                     

        /*
         * Move code below from onStop() for the order of onStop() and
         * SCREEN_OFF cannot be determined.
         */
        if (mService != null) {
            mService.blockTerminate(false);
        }

    }

    //visualize code
    
    private void setupVisualizer() {    

        // Create the Visualizer object and attach it to our media player.
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);//
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                    int samplingRate) {
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                //bytes[0] |= 0x80;
                mVisualizerView.updateVisualizer(bytes,samplingRate);
            }
        }, Visualizer.getMaxCaptureRate() / 5, false, true);//true, false);
        mVisualizer.setEnabled(true);
    }    

    private boolean mDiableStatusBarExpanded;
    private boolean mDisableFullScreen = true;

    private void toggleFullScreen() {
        Log.d(TAG, "toggleFullScreen mDisableFullScreen = " + mDisableFullScreen);
        if (mDisableFullScreen) {
            mDisableFullScreen = false;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        } else {
            mDisableFullScreen = true;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);    
        }
        checkUnreadEB();
    }
    
    /* Check if need show unread emergency broadcast icon*/
    private void checkUnreadEB() {
        if (mDisableFullScreen) { 
            //only show in full screen
            mUnreadEB.setVisibility(View.GONE);
        } else {
            Cursor cursor = getContentResolver().query(MBBMSStore.EB.Broadcast.CONTENT_URI, 
                    new String []{EB.Broadcast.ID}, 
                    EB.Broadcast.HAS_READ + "=" + 0, 
                    null, 
                    null);
            if (cursor != null) {
                if (cursor.getCount() != 0) {
                    mUnreadEB.setVisibility(View.VISIBLE);
                } else {
                    mUnreadEB.setVisibility(View.GONE);
                }
                cursor.close();
            }
        }
    }

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
            
            if (ch.lock_stat == Channel.LOCK_STATE_NONE) {
                holder.mEncrypedindicator.setVisibility(View.INVISIBLE);
                holder.mLogo.setImageBitmap(
                    ch.logo == null ? (ch.isAudio() ? mAudioChannelIcon : mVideoChannelIcon) : ch.logo);
            } else if (ch.lock_stat == Channel.LOCK_STATE_UNLOCK) {
        holder.mLogo.setImageBitmap(ch.logo == null ? (ch.isAudio() ? mAudioChannelIcon : mVideoChannelIcon) : ch.logo);
                holder.mEncrypedindicator.setVisibility(View.VISIBLE);
                holder.mEncrypedindicator.setImageBitmap(mSubscribedIndicator);
            } else {
                holder.mEncrypedindicator.setVisibility(View.INVISIBLE);
                holder.mLogo.setImageBitmap(mUnsubscribedLogo);    
                //holder.logo.setImageBitmap(ch.logo == null ? mUnsubscribedLogo: ch.logo);                    
            }
            
            return convertView;
        }
    }    

}
