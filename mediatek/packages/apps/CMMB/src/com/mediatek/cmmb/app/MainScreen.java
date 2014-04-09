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

import android.app.AlertDialog;
import android.app.Dialog;
//import android.app.ProgressDialog;
//import android.app.Service;
import android.app.TabActivity;
//import android.content.ComponentName;
//import android.content.ContentResolver;
//import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
//import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
//import android.os.IBinder;
import android.os.Message;
//import android.provider.Telephony.SIMInfo;
//import android.text.format.DateFormat;
//import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
//import android.view.KeyEvent;
import android.view.WindowManager;
//import android.view.Window;
//import android.widget.TabHost.TabSpec;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
//import android.widget.CursorAdapter;
import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ListView;
//import android.widget.TabWidget;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

//MBBMS Provider's interface
//import com.android.internal.telephony.Phone;
import com.mediatek.cmmb.app.ChannelListManager.Channel;
import com.mediatek.cmmb.app.ChannelListManager.Program;
import com.mediatek.cmmb.app.ModeSwitchManager.SelectSimListerner;
import com.mediatek.common.featureoption.FeatureOption;
//import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.MBBMSStore.*;
import com.mediatek.mbbms.MBBMSStore.ESG.*;
import com.mediatek.mbbms.MBBMSStore.SG.*;
import com.mediatek.mbbms.ServerStatus; //MBBMS Service's interface
//import com.mediatek.mbbms.service.CMMBMediaPlayer;
import com.mediatek.mbbms.service.CMMBServiceClient;
import com.mediatek.mbbms.service.MBBMSService;
//import com.mediatek.mbbms.service.MBBMSService.LocalBinder;
import com.mediatek.notification.NotificationManagerPlus;

//import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
//import java.util.ArrayList;
import java.util.Date;
//import java.util.HashMap;
import java.util.List;




public class MainScreen extends TabActivity implements TabHost.OnTabChangeListener
                ,ChannelListManager.ChannelListObserver,MBBMSService.OnEventListener {   
    
    private static final int REQUEST_SETTING_ACTIVITY = 2;
    
    private static final String TAG = "CMMB::MainScreen";

    private static final String TAB_INDEX = "TAB_INDEX";
    private static final String CURR_STATE = "CURR_STATE";
    
    private static final int PROGRESS_ON_DURATION = 3000;
    
    private static final int MSG_INITIALIZATION = 1;
    private static final int MSG_LOAD_CHANNEL_LIST = 2;    
    private static final int MSG_UPDATE_SERVICE_DONE = 3;      
    private static final int MSG_STOP_LOADING_ANIMATION = 4;      
    private static final int MSG_MTS_DONE = 5;
    private static final int MSG_UPDATE_SERVICE = 6;
    
    
    private static final int OPTIONMENU_PACKAGE_MANAGEMENT = 1;
    private static final int OPTIONMENU_REFRESH = 2;
    private static final int OPTIONMENU_EMERGENCY_BROADCAST = 3;
    private static final int OPTIONMENU_SETTINGS = 4;
    private static final int OPTIONMENU_HELP = 5;
    private static final int OPTIONMENU_QUIT = 6;
    
    private static final int OPTIONMENU_MTS_SG_RETRIEVE = 7;
    private static final int OPTIONMENU_MTS_GBA = 8;
    private static final int OPTIONMENU_MTS_SUB_UPDATE = 9;
    private static final int OPTIONMENU_MTS_MSK_RETRIEVE = 10;    
    
    //dialog ids
    private static final int DIALOG_NEED_SUBSCRIPTION_HINT = 2;    
    private static final int DIALOG_UNAVAILABLE_HINT = 3;      
    private static final int DIALOG_REFRESH = 4;        

    private static final int DIALOG_QUIT_CONFIRM = 7;

    private static final int MENU_ITEM_FAVORITE = 1;
    private static final int MENU_ITEM_PROGRAM = 2;
    
    private static final int MBBMS_MODE = CMMBServiceClient.CMMB_STATE_CONNECTED;

    private static final int STATE_INVALID = -1;    
    private static final int STATE_IDLE = 0;
    private static final int STATE_UPDATING_SERVICE = 1; 
    private static final int STATE_UPDATED_SERVICE = 2;    
    private static final int STATE_UPDATED_SERVICE_FAIL = 3;
    
    private static final int TAB_ALL = 0;
    private static final int TAB_FAVORITE = 1; 
    //private static final int TAB_HISTORY = 2;
    
    private int mState;//the state of this activity.
        
    private int mTabIndex = TAB_ALL;
    private boolean mInMbbmsMode;
    private boolean mInitialized;
    private boolean mPaused;    
    private boolean mStarted;    
    private boolean mWaitStateChangeConfirm;
    //TBD: remove this flag because the use scenario seems imposible to happen now.
    private boolean mIsMTSEnabled;
    private boolean mIsOrderingInfoRetreived;
    //this variable is only meaningful in MBBMS state and mState =  STATE_UPDATED_SERVICE.    
    private int mWaitMode = -1;    

    private static final int PHASE_NOT_STARTED = 0;
    private static final int PHASE_PAUSED = 1; 
    private static final int PHASE_STARTED = 2;
    private static final int PHASE_FINISHED = 3;     
    private int mAnimationPhase = PHASE_NOT_STARTED;
    private MBBMSService mService;
    private ModeSwitchManager mModeSwitchManager;

    private AlertDialog mCmmbDialog;    
    
    private Thread mUpdateServiceThread = null;
    private ChannelListManager mChannelListManager;
    
    private ListView mAllChannelListView; 
    private ListView mFavoriteChannelListView;     
    private TextView mAllEmpty;   
    private TextView mFavoriteEmpty;    
    
    private View mProgressView;
    private Bitmap mVideoChannelIcon;
    private Bitmap mAudioChannelIcon;
    private Bitmap mUnsubscribedLogo;    
    private Bitmap mSubscribedIndicator; 
    private View mLoadProgress;
    
    private ChannelListAdapter mAllAdapter;
    private ChannelListAdapter mFavoriteAdapter;

    private    NotificationManagerPlus mNMP;
    
    private boolean mDestroyByconfigChange;
        
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage what = " + msg);
        
            switch (msg.what) {
                case MSG_MTS_DONE: 
                    mProgressView.setVisibility(View.GONE);                    
                    mLoadProgress.setVisibility(View.GONE);
                    Toast.makeText(MainScreen.this, (String)msg.obj, 1000).show();                    
                    break;
                       

                case MSG_UPDATE_SERVICE: 
                    if (!mWaitStateChangeConfirm) {                        
                        reloadChannelList(false);
                        updateServiceGuide(null);
                    }                
                    break;
                                  

                case MSG_STOP_LOADING_ANIMATION: 
                    stopLoadingAnimation();                
                    break;
                            

                case MSG_LOAD_CHANNEL_LIST:                   
                    reloadChannelList(false);                                    
                    break;
                                                    
            
                case MSG_INITIALIZATION: 
                    initAll();
                    break;
                        
                     
                case MSG_UPDATE_SERVICE_DONE: 
                    if (mState != STATE_UPDATING_SERVICE) {
                        return;
                    }

                    stopLoadingAnimation();//loading animation may not stop yet.                    

                    boolean result = (msg.arg1 == 1) ? true : false;
                    onUpdateServiceGuideEnd(true);    
                    
                    if (result) {
                        if (msg.arg2 != MBBMSService.UPDATE_SUBSCRIPTION_FAILED) {
                            Log.w(TAG, "handleMessage msg.arg2 = " + msg.arg2);
                            mIsOrderingInfoRetreived = true;
                        }
                        updateServiceOk(msg.arg2);
                    } else {
                        ServerStatus status = (ServerStatus)msg.obj;
                        {
                            updateServiceNotOk(getErrorDescription(status));
                        }
                    }
                    invalidateOptionsMenu();
                    break;                                      
                 default:
                     break;
            }
        }

    };

    //implements MBBMSService.OnEventListener.
    public void event(int ev, int type, int arg, Object status) {             
        Log.d(TAG, "event() ev = " + ev
            + " , type=" + type
            + " , arg=" + arg
            + " , status=" + status
            );
             
        if (ev == CMMBServiceClient.CMMB_EVENT_CLIENT_INFO) {
            switch (type) {
                case CMMBServiceClient.CMMB_INFO_SERVICE_GUIDE_FAIL:
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SERVICE_DONE,0,0,status));
                    break;     
                case CMMBServiceClient.CMMB_INFO_SERVICE_GUIDE_OK:
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SERVICE_DONE,1,arg,null));
                    break;                                
                default:
                    break;
            }
        } 
    }    

    private String getErrorDescription(ServerStatus status) {        
        return Utils.getErrorDescription(getResources(),status,null);
    }
    
    private void onUpdateServiceGuideEnd(boolean success) {    
        mChannelListManager.setAllowUpdate(true);
        if (success) {
            reloadChannelList(true);
        }
        changeTitleProgressIndicator(false,success);
        mUpdateServiceThread = null;
    }
   
    private String getEsgUpdateDescription() {
        String str = mService.getSubUpdateDescription();
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        return str;
    }

    private void updateServiceOk(int upgradepolicy) {
        Log.d(TAG, "updateServiceOk() mState = " + mState);
        mState = STATE_UPDATED_SERVICE;
        String desMsg = getEsgUpdateDescription();
        if (desMsg != null) {
            Toast.makeText(this, desMsg, 1000).show();
        }

    }
    
    private void updateServiceNotOk(String error) {
        Log.d(TAG, "updateServiceNotOk() mState = " + mState + " error = " + error);           
        Toast.makeText(this, getResources().getString(R.string.failed_to_update_service), 1000).show();
        mState = STATE_UPDATED_SERVICE_FAIL;
    }    

    private void showWelcomeInfo() {
        
        TextView loadingtext = (TextView)findViewById(R.id.loading_text);    
        
        Cursor cursor = getContentResolver().query(SG.SGDD.CONTENT_URI
                ,new String []{SG.SGCommonColumns.PRIVATE_EXT}
                ,SG.SGDDColumns.ID + " = 1"
                ,null
                ,null);        
        
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String wi = cursor.getString(0);
                Log.d(TAG, "wi =" + wi);
                
                cursor.close();
                if (wi != null) {
                    loadingtext.setText(wi);
                    return;
                }
            }
            cursor.close();
        } 
        
        loadingtext.setText(R.string.greeting);
    }    

    public void onChannelListChange(boolean onUpdateServiceOk) {
        Log.i(TAG, "onChannelListChange() onUpdateServiceOk=" + onUpdateServiceOk
                + " , mWaitMode=" + mWaitMode
                + " , mState=" + mState
                + " , mIsOrderingInfoRetreived=" + mIsOrderingInfoRetreived
            );

        if (onUpdateServiceOk && mWaitMode != -1 && mState != STATE_UPDATED_SERVICE_FAIL) {
            if (mWaitMode == MBBMS_MODE && !mIsOrderingInfoRetreived) {
                    Toast.makeText(this,getResources().getString(R.string.failed_to_update_service),1000).show();
            }
            mWaitMode = -1;    
            //mChannelListManager.loadProgramList();
        }
        notifyDataSetChanged();
    }

        
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        mDestroyByconfigChange = false;
        
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);        
        //setProgressBarIndeterminate(true);

        mModeSwitchManager = new ModeSwitchManager(this,new MyModeChangeHandler(),savedInstanceState);     

        mChannelListManager = ChannelListManager.getChannelListManager(this);
        mChannelListManager.registerChannelListObserver(this);     

        Bundle state = (Bundle)getLastNonConfigurationInstance();
        Log.i(TAG, "state = " + state + " savedInstanceState = " + savedInstanceState);
        
        if (state == null) {
            state = savedInstanceState;
        }

        int savedState = STATE_INVALID;
        
        if (state != null) {
            mTabIndex = state.getInt(TAB_INDEX,mTabIndex);
            savedState = state.getInt(CURR_STATE,STATE_INVALID);
            if (savedState >= STATE_UPDATED_SERVICE) {
                mChannelListManager.loadChannelList(true,mModeSwitchManager.getMode() == MBBMS_MODE,true);
            } else {
                //reset savedState if it is not STATE_UPDATED_SERVICE 
                //becoz we only recover everything when it was in STATE_UPDATED_SERVICE last time.            
                savedState = STATE_INVALID;
            }
        }

        //setContentView is put after reloadChannelList to 
        //make the listview to be showed ASAP instead of showing empty view. 
        TabHost tabHost = getTabHost();
        LayoutInflater.from(this).inflate(R.layout.main_screen, tabHost.getTabContentView(), true);
        
        mNMP = new NotificationManagerPlus.ManagerBuilder(this).create();
        mProgressView = findViewById(R.id.progress_indicator);
        mLoadProgress = findViewById(R.id.loading_process);

        readEmModePara();
        
        showWelcomeInfo();
        if (savedState == STATE_INVALID) {
            startUp();
        } else {            
            mState = savedState;            
            stopLoadingAnimation(); 
            new Thread(new Runnable() {
                public void run() { 
                    bindMBBMSService();
                }
            }).start();
            
        }
    
    }

    private void startLoadingAnimation() {
        Log.d(TAG, "startLoadingAnimation mAnimationPhase = " + mAnimationPhase); 
        
        if (mAnimationPhase < PHASE_STARTED) {
            mAnimationPhase = PHASE_STARTED;
            //we control mProgressView's visibility and its child view's visibility separately because
            //we need to set mProgressView's visibility to true to cover the tab widget below while setting 
            //its child view's visibility to false when the cmmb mode dialog is showed up on launching.
            findViewById(R.id.logo).setVisibility(View.VISIBLE);
            //findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
            mLoadProgress.setVisibility(View.VISIBLE);
            findViewById(R.id.loading_text).setVisibility(View.VISIBLE);    
            mHandler.sendEmptyMessageDelayed(MSG_STOP_LOADING_ANIMATION,PROGRESS_ON_DURATION); 
        }
        Log.d(TAG, "startLoadingAnimation end"); 
        
    }    

    private void pauseLoadingAnimation() {
        
        Log.d(TAG, "pauseLoadingAnimation mAnimationPhase = " + mAnimationPhase); 
        if (mAnimationPhase == PHASE_STARTED) {
            mAnimationPhase = PHASE_PAUSED;
            //we control mProgressView's visibility and its child view's visibility separately because
            //we need to set mProgressView's visibility to true to cover the tab widget below while setting 
            //its child view's visibility to false when the cmmb mode dialog is showed up on launching.            
            findViewById(R.id.logo).setVisibility(View.INVISIBLE);
            //findViewById(android.R.id.progress).setVisibility(View.INVISIBLE);
            mLoadProgress.setVisibility(View.INVISIBLE);
            findViewById(R.id.loading_text).setVisibility(View.INVISIBLE);    
            mHandler.removeMessages(MSG_STOP_LOADING_ANIMATION);             
        }
    }

    
    private void stopLoadingAnimation() {
        
        Log.d(TAG, "stopLoadingAnimation mAnimationPhase = " + mAnimationPhase); 
        if (mAnimationPhase < PHASE_FINISHED) {
            mAnimationPhase = PHASE_FINISHED;
            if (mState == STATE_UPDATING_SERVICE) {
                changeTitleProgressIndicator(true,false);
            } else { //avoid show loading when reCretate from window configuration change
                changeTitleProgressIndicator(false, mState == STATE_UPDATED_SERVICE);
            }
            mProgressView.setVisibility(View.GONE);
        }
    }


    private void onModeSwitch() {
        if (mService != null) {
            mService.setSimInfo(mModeSwitchManager.getCurSimID());
            mService.onModeSwitch(mModeSwitchManager.getSimType(), mModeSwitchManager.isMbbmsMode());                
        }
    }
    
    private class MyModeChangeHandler extends ModeSwitchManager.ModeChangeHandler {
        @Override
        public boolean onEnterCmmbMode() {
            //switchToCmmbMode();
            return true;
        }    
        
        @Override
        public void onDialogPrepared() {
            showCmmbModeDialog(false);
        }    
        
        @Override
        public boolean onModeSelected(boolean isMbbmsSelected) {
            if (isMbbmsSelected) {
                if (mStarted) { //means that this dialog is showed up in current activity.
                    if (mState == STATE_UPDATING_SERVICE) {
                        stopUpdateServiceGuide();                
                    }
                    onModeSwitch();
                    dismissAllDialogs();
                    refreshWithUI(true);
                }
            } else {
                //means that this happens before animantion is showed 
                //thus we need to treat it as "OK" is selected in cmmb dialog.            
                if (mAnimationPhase < PHASE_FINISHED) {  
                    refreshWithUI(false);
                }
            }
            return true;                            
        }          
    }

    private void refreshWithUI(boolean restart) {
        startLoadingAnimation();                
        notifyDataSetInvalidated();
        reloadChannelList(false);
        refresh(null,restart);
    }

    
    @Override
    protected void onSaveInstanceState(Bundle state) {
        Log.d(TAG, "onSaveInstanceState() mState = " + mState + " mTabIndex = " + mTabIndex);    

        super.onSaveInstanceState(state);
        
        saveInstanceState(state);        
        mModeSwitchManager.onSaveInstanceState(state);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        Log.d(TAG, "onRetainNonConfigurationInstance() mState = " + mState + " mTabIndex = " + mTabIndex);
        mDestroyByconfigChange = true;
        Bundle state = new Bundle();
        saveInstanceState(state);
        return state;
    }

    private void saveInstanceState(Bundle state) {
        //only restore the status when the state is STATE_UPDATED_SERVICE
        //which do not require to show animation and "enter-cmmb"  dialog,etc.
        if (mState >= STATE_UPDATED_SERVICE) {            
            state.putInt(TAB_INDEX,mTabIndex);        
            state.putInt(CURR_STATE, mState);
            Log.d(TAG, "saveInstanceState: state=" + state);
        }
    }
    
    
    @Override
    public void onStart() {
        Log.d(TAG, "onStart() mState = " + mState);   
    
        super.onStart();
        mStarted = true;        
        mModeSwitchManager.onActivityStart();
    }
    
    @Override
    public void onStop() {
        Log.d(TAG, "onStop() mState = " + mState);   
    
        super.onStop();        
        mStarted = false;        
        mModeSwitchManager.onActivityStop();
    }    
    
    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent()");        
        boolean modeChanged = intent.getBooleanExtra("modeChanged",false);
        if (modeChanged) {
            boolean simChanged = intent.getBooleanExtra("simChanged",false);
            
            Log.d(TAG, "simChanged = " + simChanged);

            if (simChanged || mModeSwitchManager.getMode() == MBBMS_MODE) {
                onModeSwitch();
                reloadChannelList(false);
                refresh(null,true);            
            } else {
                Log.e(TAG, "onNewIntent(): should not reach here!!!!");
                //switchToCmmbMode();
            }
        }
    }    
    
    @Override
    public void onBackPressed() {
        showDialog(DIALOG_QUIT_CONFIRM);
        //super.onBackPressed();
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() mState = " + mState
                + ",mDestroyByconfigChange=" + mDestroyByconfigChange);

        super.onDestroy();
        if (mDestroyByconfigChange) {
            return;
        }
        // make sure this dialog is dismissed before exiting to avoid window
        // leaked.
        showCmmbModeDialog(false);

        if (mState == STATE_UPDATING_SERVICE) {
            stopUpdateServiceGuide();
        }
        mHandler.removeCallbacksAndMessages(null);// ensure all messages are
                                                  // removed(onBackPressed or
                                                  // finish).

        EBManager.cancelNotification();
        mChannelListManager.unregisterChannelListObserver(this);

        // release all managers because we are going to exit the application.
        mChannelListManager.release();
        mModeSwitchManager.release();

        if (mService != null) {
            // TBD:should change ModeSwitchMonitor to allow
            // messagedetailsactivity be called at this point.
            Log.d(TAG, "onDestroy() destroy mService");
            mService.unSetEventListener(this);
            mService.stopTranscationThread();
            ServiceManager.getServiceManager(this).closeService();
        }
    }
    

    
    @Override
    public void onResume() {
        Log.d(TAG, "onResume() mState = " + mState);   
        super.onResume();
        mNMP.startListening();
        mPaused = false;

        if (!mInitialized) {
            mHandler.sendEmptyMessage(MSG_INITIALIZATION);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause() mState = " + mState);
        super.onPause();        
        mNMP.stopListening();
        mPaused = true;
    }    
           
    
    private void setupListView() {
        AdapterView.OnItemClickListener l = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {                 
                Log.d(TAG, "onItemClick mState = " + mState);
                if (mState != STATE_UPDATED_SERVICE && mState != STATE_UPDATED_SERVICE_FAIL) {
                    if (mState == STATE_UPDATING_SERVICE) {
                        Toast.makeText(MainScreen.this, R.string.updating_sg_hint, 1000).show();
                    }
                    return;
                }
                gotoPlayer(getRealPosition(id));
            }            
        };

        mAllChannelListView = (ListView)findViewById(R.id.list_all);
        mAllChannelListView.setEmptyView(findViewById(R.id.empty_all)); 
        mAllAdapter = new ChannelListAdapter(this,false);
        mAllChannelListView.setAdapter(mAllAdapter); 
        mAllChannelListView.setOnCreateContextMenuListener(this);       
        mAllChannelListView.setOnItemClickListener(l);       
        
        mFavoriteChannelListView = (ListView)findViewById(R.id.list_favorite);
        mFavoriteChannelListView.setEmptyView(findViewById(R.id.empty_favorite)); 
        mFavoriteAdapter = new ChannelListAdapter(this,true);
        mFavoriteChannelListView.setAdapter(mFavoriteAdapter); 
        mFavoriteChannelListView.setOnCreateContextMenuListener(this);    
        mFavoriteChannelListView.setOnItemClickListener(l);       
    }
    
    private void setupTabs() {
        TabHost tabs = getTabHost();
        tabs.setOnTabChangedListener(this);
        TabHost.TabSpec spec;        
        spec = tabs.newTabSpec("all")
            .setIndicator(getString(R.string.main_all_channels),getResources().getDrawable(R.drawable.tab_all))
            .setContent(R.id.content_all);        
        tabs.addTab(spec);
        
        spec = tabs.newTabSpec("favorite")
        .setIndicator(getString(R.string.main_favourite_channels),getResources().getDrawable(R.drawable.tab_favorite))
        .setContent(R.id.content_favorite);        
        tabs.addTab(spec);
        /*
        spec = tabs.newTabSpec("history")
        .setIndicator(getString(R.string.main_play_history))
        .setContent(R.id.list);        
        tabs.addTab(spec);*/
        
        tabs.setCurrentTab(mTabIndex);

        //normally this is not needed,but we need to refresh data set again if we just went through a configuration change.
        notifyDataSetChanged();
    }
    
    private boolean bindMBBMSService() {
        if (mService == null) {   
            mService = ServiceManager.getServiceManager(this).getService();
            if (mService == null) {
                return false;
            }
            
            mService.setSimInfo(mModeSwitchManager.getCurSimID());
            mService.setEventListener(this);
               mService.setEBMListener(EBManager.getInstance(this));
        }        
        return true;
    }

    private void showCmmbModeDialog(boolean toShow) {   
        Log.d(TAG, "showCmmbModeDialog() toShow = " + toShow);
        
        if (toShow && mCmmbDialog == null) { 
            mWaitStateChangeConfirm = true;
            pauseLoadingAnimation();
            dismissAllDialogs();    
            mHandler.removeMessages(MSG_UPDATE_SERVICE);
            mCmmbDialog = new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.mobile_tv)              
            .setMessage(mModeSwitchManager.getMode() == CMMBServiceClient.CMMB_STATE_NO_SIM ? 
                    R.string.dlg_10_no_sim : R.string.dlg_10_not_connected)                
            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    refreshWithUI(false);
                }
            })            
            .create();
            
            mCmmbDialog.setOnCancelListener(new Dialog.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                            finish();
                }
            });

            mCmmbDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {                                 
                            mWaitStateChangeConfirm = false;
                            mCmmbDialog = null;
                        }
            });  

            
            mCmmbDialog.show();
        } else if (!toShow && mCmmbDialog != null) {
            mCmmbDialog.dismiss();
        }
    }          

    private void dismissAllDialogs() {
        removeDialog(DIALOG_NEED_SUBSCRIPTION_HINT);
        removeDialog(DIALOG_REFRESH);
        removeDialog(DIALOG_UNAVAILABLE_HINT);        
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id,dialog);
        if (args != null
                && (id == DIALOG_NEED_SUBSCRIPTION_HINT || id == DIALOG_UNAVAILABLE_HINT)) {
            String msg = String.format(getResources().getString(
                    args.getInt("msgid")), args.getString("name"));
            AlertDialog dlg = (AlertDialog)dialog;              
            dlg.setMessage(msg);
        }       
    }    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dlg = null;
        switch (id) {            
            
            case DIALOG_NEED_SUBSCRIPTION_HINT: 
                dlg = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.mobile_tv)                             
                .setMessage("")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent();
                        intent.setClass(MainScreen.this, PackageActivity.class);
                        intent.putExtra(SG.Service.IS_SUBSCRIBED, false);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)                
                .create();                
                break;          
              

            case DIALOG_UNAVAILABLE_HINT: 
                dlg = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.mobile_tv)                             
                .setMessage("")
                .setPositiveButton(android.R.string.ok,null)
                .create();                
                break;          
                         

            case DIALOG_REFRESH: 
                LayoutInflater factory = LayoutInflater.from(this);
                final View refreshDialog = factory.inflate(R.layout.refresh_dialog, null);
                final View autorefresh = refreshDialog.findViewById(R.id.auto_refresh);             
                final CheckBox cb = (CheckBox)refreshDialog.findViewById(R.id.auto_checkbox);
                final TextView specifylocation = (TextView)refreshDialog.findViewById(R.id.specify_location);
                final TextView inputcityhint = (TextView)refreshDialog.findViewById(R.id.input_city_hint);
                final EditText cityeditor = (EditText)refreshDialog.findViewById(R.id.city_editor);
                
                autorefresh.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {                       
                        cb.setChecked(!cb.isChecked());
                    }
                });
                cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton arg0,
                            boolean state) {
                        // TODO Auto-generated method stub
                        specifylocation.setEnabled(!state);
                        inputcityhint.setEnabled(!state);
                        cityeditor.setEnabled(!state);
                        if (state) {
                            cityeditor.setVisibility(View.INVISIBLE);
                        } else {
                            cityeditor.setVisibility(View.VISIBLE);
                        }
                        
                        
                    }
                });     
                
                dlg = new AlertDialog.Builder(this)
                    .setTitle(R.string.please_choose_refresh_type)
                    .setView(refreshDialog)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String name = null;
                            if (!cb.isChecked()) {
                                name = cityeditor.getText().toString();
                                if (name.length() == 0) {
                                    Toast.makeText(MainScreen.this, R.string.auto_refresh_is_used, 1000).show();
                                }
                            }
                            Log.d(TAG, "DIALOG_REFRESH name = " + name);
                            refresh(name,false); 
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
                    
                dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            public void onDismiss(DialogInterface dialog) {
                                removeDialog(DIALOG_REFRESH);
                                //don't keep the dialog to simply the recreation due to configuration change.
                                //See ALPS00126490
                            }
                });                 
                
                cb.setChecked(true);
                break;          
                
                
            case DIALOG_QUIT_CONFIRM: 
                dlg = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.mobile_tv)                             
                .setMessage(R.string.quit_confirm)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EBManager.cancelNotification();
                        mHandler.removeCallbacksAndMessages(null);
                        //remove all messages here should reduce the reponse time on exiting..                
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)                
                .create();                
                break;          
            default:
                break;

        }
        return dlg;
    }     

    private void clearPrograms() {
        mChannelListManager.resetProgramList();
        notifyDataSetInvalidated();
    }
    
    private void refresh(final String city,boolean restart) {     
        Log.d(TAG, "refresh() mState = " + mState);          
        switch (mState) {
        case STATE_IDLE:
        case STATE_UPDATED_SERVICE:
        case STATE_UPDATED_SERVICE_FAIL:
            clearPrograms();
            updateServiceGuide(city);
            break;
        case STATE_UPDATING_SERVICE:
            if (restart) {
                stopUpdateServiceGuide();
                updateServiceGuide(city);
            }
            break;
        default:    
            myAssert(false);
        }
    }
    
    private void stopUpdateServiceGuide() {
        
        Log.d(TAG, "stopUpdateServiceGuide() mState = " + mState);
        if (mUpdateServiceThread != null) {
            mUpdateServiceThread.interrupt();

            try {
                mUpdateServiceThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "stopUpdateServiceGuide() ignore");//ignore
            }
            mUpdateServiceThread = null;
        }
        
        if (mService != null) {
            mService.stopUpdateService(mInMbbmsMode);
        }        
        mHandler.removeMessages(MSG_UPDATE_SERVICE_DONE);
        onUpdateServiceGuideEnd(false);
        mState = STATE_IDLE;        
    }

    private void changeTitleProgressIndicator(boolean visible,boolean updateOk) {
        if (visible) {
            //setProgressBarIndeterminateVisibility(true);
            mLoadProgress.setVisibility(View.VISIBLE);
            setTitle(getString(R.string.mobile_tv) + getString(R.string._updating));
        } else {
            //setProgressBarIndeterminateVisibility(false);
            if (mAnimationPhase == PHASE_FINISHED) {
            mLoadProgress.setVisibility(View.INVISIBLE);
            }
            setTitle(updateOk ? getString(R.string.mobile_tv) 
                : (getString(R.string.mobile_tv) + getString(R.string._unknown_state)));
        }
    }
    
    private void updateServiceGuide(final String city) {
        
        Log.d(TAG, "updateServiceGuide");
        if (mAnimationPhase == PHASE_FINISHED) {
            changeTitleProgressIndicator(true,false);
        }
        mChannelListManager.setAllowUpdate(false);
        mState = STATE_UPDATING_SERVICE; 
        
        mIsOrderingInfoRetreived = false;
        
        if (mModeSwitchManager.getMode() == MBBMS_MODE) {
            mInMbbmsMode = true;
            mUpdateServiceThread = new Thread(new Runnable() {
                    public void run() { 
                        ServerStatus result = null;
                        if (bindMBBMSService()) {
                            result = mService.syncServiceGuide(city);
                        }
                        
                        if (!Thread.interrupted()) {
                            if (!Utils.isSuccess(result)) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SERVICE_DONE,0,0,result));
                            }
                        }
                    }
            });    
        } else {
            mInMbbmsMode = false;
            mUpdateServiceThread = new Thread(new Runnable() {
                    public void run() {                                            
                        ServerStatus result = null;
                        if (bindMBBMSService()) {
                            result = mService.syncEServiceGuide();
                        }
                        
                        if (!Thread.interrupted()) {
                            if (!Utils.isSuccess(result)) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SERVICE_DONE,0,0,result));
                            }
                        }
                    }
            });    

        }
        
        mUpdateServiceThread.start();    
    }

    private void initAll() {
        Log.d(TAG, "initAll() mState = " + mState + " mInitialized = " + mInitialized);
        if (mInitialized) {
            return;
            //already initialized.         
        }
        setupListView();        
        //setupTabs is put after setupListView here to ensure mAllEmpty is not null in onTabChanged().        
        setupTabs();
        readResources();        
        mInitialized = true;
        Log.d(TAG, "initAll end");
        
    }    
    
    
    private void readResources() {
        Log.d(TAG, "readResources() mState = " + mState);

        //initialize related resources.
        mVideoChannelIcon = BitmapFactory.decodeResource(getResources(),R.drawable.video_channel);
        mAudioChannelIcon = BitmapFactory.decodeResource(getResources(),R.drawable.audio_channel);        
        mUnsubscribedLogo = BitmapFactory.decodeResource(getResources(),R.drawable.unsubscribed_indicator);        
        mSubscribedIndicator = BitmapFactory.decodeResource(getResources(),R.drawable.subscribed_indicator);       
    }    
      
    
    private void myAssert(boolean noassert) {
        if (!noassert) {
            throw new RuntimeException(TAG + " assertion failed!");  
        }
    }       
    
    private void notifyDataSetChanged() {        
        Log.d(TAG, "notifyDataSetChanged");
        if (mAllChannelListView != null) {
            mAllAdapter.notifyDataSetChanged();
        }
        if (mFavoriteChannelListView != null) {
            mFavoriteAdapter.notifyDataSetChanged();
        }         
    }

    private void notifyDataSetInvalidated() {        
        Log.d(TAG, "notifyDataSetInvalidated");
        if (mAllChannelListView != null) {
            mAllAdapter.notifyDataSetInvalidated();
        }        

        if (mFavoriteChannelListView != null) {
            mFavoriteAdapter.notifyDataSetInvalidated();
        }             
    }    
    
    private void reloadChannelList(boolean onUpdateServiceOk) {
        Log.d(TAG, "reloadChannelList");
        int mode = mModeSwitchManager.getMode();
        mWaitMode = onUpdateServiceOk ? mode : -1;
        mChannelListManager.resetProgramList();
        mChannelListManager.loadChannelList(onUpdateServiceOk,mode == MBBMS_MODE);            
    }

    private void readEmModePara() {
        Context context = null;
        try {
            context = createPackageContext("com.mediatek.engineermode",0);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException in readEmModePara!!");
            return;
        }
        SharedPreferences sp = context.getSharedPreferences("cmmb_pref",Context.MODE_MULTI_PROCESS);
        
        MBBMSService.setEmPara(sp.getString("cmmb_savemfsfile_key","0").equals("1") ? true : false,
            sp.getString("cmmb_memsetspibuf_key","0").equals("1") ? true : false,
            sp.getString("cmmb_mbbms30_key","0").equals("1") ? true : false,
            sp.getString("cmmb_mts_key","0").equals("1") ? true : false);
        
        String cmmbmtskey = sp.getString("cmmb_mts_key","0");
        Log.d(TAG, "cmmbmtskey = " + cmmbmtskey);
        mIsMTSEnabled = cmmbmtskey.equals("1") ? true : false;
    }

    private void testMtsFunction(final int fun) {        
        Log.d(TAG, "testMtsFunction");    
        if (mService == null) {
            Log.d(TAG, "service is null!");
            return;
        }
        findViewById(R.id.logo).setVisibility(View.VISIBLE);
        //findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
        mLoadProgress.setVisibility(View.VISIBLE);
        findViewById(R.id.loading_text).setVisibility(View.VISIBLE);        
        mProgressView.setVisibility(View.VISIBLE);
        Thread mtsThread;
        mtsThread = new Thread(new Runnable() {
            public void run() {         
                ServerStatus result = null;
                String subResult = null;
                switch (fun) {                    
                case OPTIONMENU_MTS_GBA:
                    try {
                        result = mService.processGBAInitialization();
                    } catch (InterruptedException e) {
                        Log.d(TAG, "testMtsFunction() ignore");//ignore at present.
                    } finally {
                        mService.stopDataConnect();
                    }
                    break;                    
                
                case OPTIONMENU_MTS_SUB_UPDATE:
                    result = mService.processSubscriptionUpdate();
                    if (Utils.isSuccess(result)) {
                        subResult = mService.getSubUpdateDescription();
                    }
                    mService.stopDataConnect();
                    break;                
                
                case OPTIONMENU_MTS_MSK_RETRIEVE:
                    try {
                        result = mService.processSKRetrieve();
                    } catch (InterruptedException e) {
                        Log.d(TAG, "testMtsFunction() ignore2");//ignore at present.
                    } finally {
                        mService.stopDataConnect();
                    }                    
                    break;                
                default:
                    break;
                }
                String resultString;
                if (subResult == null) {
                    resultString = Utils.isSuccess(result) ? "Ok!" : getErrorDescription(result);
                } else {
                    resultString = subResult;
                }
                if (resultString == null) {
                    resultString = "Fail!!!";
                }
                mHandler.sendMessage(mHandler.obtainMessage(MSG_MTS_DONE,0,0,resultString));
            }
        });                  
        mtsThread.start();    
    }

    private static final int OP_MENU_GRP_UPDATED = 1; /*show after service updated*/
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu: mState:" + mState + ", mService:" + mService);
        super.onPrepareOptionsMenu(menu);
        menu.setGroupVisible(OP_MENU_GRP_UPDATED, false);

        if (mState == STATE_UPDATING_SERVICE || mService == null) {
            return true;
        }
        
        menu.setGroupVisible(OP_MENU_GRP_UPDATED, true);
        boolean isMbbmsMode = (mModeSwitchManager.getMode() == MBBMS_MODE);
        
        closeContextMenu();//see ALPS00132172.
        
        menu.getItem(OPTIONMENU_PACKAGE_MANAGEMENT - 1).setVisible(isMbbmsMode && mState == STATE_UPDATED_SERVICE);    
        if (!FeatureOption.MTK_GEMINI_SUPPORT) { //no settings if gemini is not supported and we are in CMMB mode.
            menu.getItem(OPTIONMENU_SETTINGS - 1).setVisible(isMbbmsMode);
        }        

        if (mIsMTSEnabled) {
            menu.getItem(OPTIONMENU_MTS_SG_RETRIEVE - 1).setVisible(isMbbmsMode); 
            menu.getItem(OPTIONMENU_MTS_GBA - 1).setVisible(isMbbmsMode); 
            menu.getItem(OPTIONMENU_MTS_SUB_UPDATE - 1).setVisible(isMbbmsMode); 
            menu.getItem(OPTIONMENU_MTS_MSK_RETRIEVE - 1).setVisible(isMbbmsMode);             
        }        
        return true;
    }    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_PACKAGE_MANAGEMENT, 0,
                this.getString(R.string.package_management)).setIcon(
                R.drawable.menu_pacakge_management);
        menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_REFRESH, 0, this.getString(R.string.refresh))
                .setIcon(R.drawable.menu_refresh);
        menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_EMERGENCY_BROADCAST, 0,
                this.getString(R.string.emergency_broadcast)).setIcon(
                R.drawable.menu_emergence_broadcast);
        menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_SETTINGS, 0, this.getString(R.string.setting))
                .setIcon(R.drawable.menu_settings);
        menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_HELP, 0, this.getString(R.string.help)).setIcon(
                R.drawable.menu_help);
        menu.add(0, OPTIONMENU_QUIT, 0, this.getString(R.string.quit)).setIcon(
                R.drawable.menu_quit);

        //MTS menu
        if (mIsMTSEnabled) {
            menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_MTS_SG_RETRIEVE, 0,
                    this.getString(R.string.mts_sg_retrieve));
            menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_MTS_GBA, 0, this.getString(R.string.mts_gba));
            menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_MTS_SUB_UPDATE, 0, this.getString(R.string.mts_sub_update));
            menu.add(OP_MENU_GRP_UPDATED, OPTIONMENU_MTS_MSK_RETRIEVE, 0, this.getString(R.string.mts_msk_retrieve));        
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case OPTIONMENU_PACKAGE_MANAGEMENT:// package management        
            gotoX(PackageActivity.class);
            break;

        case OPTIONMENU_REFRESH:// refresh
        case OPTIONMENU_MTS_SG_RETRIEVE:        
            if (mModeSwitchManager.getMode() == MBBMS_MODE) {
                showDialog(DIALOG_REFRESH);
            } else {
                refresh(null,false);
            }
            break;

        case OPTIONMENU_EMERGENCY_BROADCAST:            
            gotoX(MessageListActivity.class);
            break;

        case OPTIONMENU_SETTINGS:            
            gotoXForResult(SettingActivity.class,REQUEST_SETTING_ACTIVITY);
            break;
 
        case OPTIONMENU_HELP:// help
            gotoX(HelpActivity.class);
            break;

        case OPTIONMENU_QUIT:// quit
            showDialog(DIALOG_QUIT_CONFIRM);
            //finish();
            break;
            
        case OPTIONMENU_MTS_GBA:
        case OPTIONMENU_MTS_SUB_UPDATE:
        case OPTIONMENU_MTS_MSK_RETRIEVE:
            testMtsFunction(item.getItemId());
            break;                
        default:
            break;

        }
        return true;
    }

    private void gotoXForResult(Class<?> cls,int requestCode) {
        Intent intent = new Intent(this, cls);
        startActivityForResult(intent,requestCode);
    }        

    private void gotoX(Class<?> cls) {
        Intent intent = new Intent();
        intent.setClass(this, cls);
        startActivity(intent);
    }    

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
    }    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {        
        if (mState < STATE_UPDATED_SERVICE) {
            return;
        }
        closeOptionsMenu();//solve ALPS00132172.
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
        
        Channel ch = mChannelListManager.getChannelList().get(getRealPosition(info.id));
        // Setup the menu header
        menu.setHeaderTitle(ch.name);
        
        menu.add(0, MENU_ITEM_FAVORITE, 0, ch.isFavorite ? R.string.remove_from_favorites 
                : R.string.add_to_favorites);
        menu.add(0, MENU_ITEM_PROGRAM, 0, R.string.program_list);
    }
        
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case MENU_ITEM_FAVORITE:                
                revertFavoriteAttr(getRealPosition(info.id));
                return true;
            
            
            case MENU_ITEM_PROGRAM: 
                gotoProgram(getRealPosition(info.id));
                return true;
                                   
            default:
                break;
                                   
        }
        return false;
    }
    
    private void revertFavoriteAttr(int pos) {
        int ret = mChannelListManager.revertFavoriteAttr(pos);
        if (ret >= 0) {
              int strId;

              if (ret > 0) {
                strId = R.string.favourite_added;
            } else {
                strId = R.string.favourite_removed;
            }
              
            String text = String.format(getResources().getString(strId), 
                    mChannelListManager.getChannelList().get(pos).name);
            Toast.makeText(this,text,500).show();            
        }

    }      
    
    private void gotoPlayer(int pos) {
        Channel ch = mChannelListManager.getChannelList().get(pos);
        if (!isChannelAvailable(ch)) {
            return;
        }
        
        Intent intent = new Intent();
        intent.setClass(this,PlayerActivity.class);
        
        Bundle bundle = new Bundle();        
        bundle.putInt(ESG.ServiceSpecial.SERVICE_ROW_ID, ch.rowID);
        //bundle.putBoolean("isMBBMSMode", mModeSwitchManager.getMode() == MBBMS_MODE);
        intent.putExtras(bundle);
        startActivity(intent);
    }   
    
    private void gotoProgram(int pos) {
        Log.d(TAG, "gotoProgram() pos = " + pos);           
        Intent intent = new Intent();
        intent.setClass(this,PlaybillActivity.class);
        Bundle bundle = new Bundle();
        Channel ch = mChannelListManager.getChannelList().get(pos);
        bundle.putString(SG.ServiceDetail.SERVICE_ID, ch.serviceID);
        bundle.putString(ESG.Service.SERVICE_NAME, ch.name);
        bundle.putInt(ESG.ServiceSpecial.SERVICE_ROW_ID, ch.rowID);        
        bundle.putInt(SG.ServiceDetail.IS_FAVORITE, ch.isFavorite ? 1 : 0);
        bundle.putInt(SG.Access.FREQUENCY_NO, ch.freq);
        bundle.putBoolean(ESG.ServiceDetail.FOR_FREE, ch.isFree());
        intent.putExtras(bundle);
        startActivity(intent);
    }   

    private boolean isChannelAvailable(Channel ch) {
        if (ch.isFree()) {
            return true;
        } else {
            if (ch.isSubscribed) {
                return true;
            } else {
                int dialogId;
                int msgId;
                if (mModeSwitchManager.getMode() != MBBMS_MODE) {
                    dialogId = DIALOG_UNAVAILABLE_HINT;
                    msgId = R.string.channel_unavailable_now;
                } else {
                    dialogId = DIALOG_NEED_SUBSCRIPTION_HINT;
                    msgId = R.string.need_subscription_hint;
                }
                Bundle bundle = new Bundle();
                bundle.putInt("msgid", msgId);
                bundle.putString("name", ch.name);
                showDialog(dialogId, bundle);
                return false;
            }
        }
    }

    public void onTabChanged(String tagString) {
        Log.d(TAG, "onTabChanged() tagString = " + tagString);  
        if (tagString.equals("all")) {
            mTabIndex = TAB_ALL;
        } else {
            mTabIndex = TAB_FAVORITE;
        }
    }
    
    private int getRealPosition(long id) {        
        int pos = (int)id;
        if ((id & ChannelListAdapter.FAVORITE_MARK) == ChannelListAdapter.FAVORITE_MARK) {
            pos = mFavoriteAdapter.getRealPosition(pos);
        }       
        return pos;
    }    
    
    private class ChannelListAdapter extends BaseAdapter {
        public static final long FAVORITE_MARK = 1L << 32;
        private LayoutInflater mInflater;
        private boolean mIsFavorite;

        class ViewHolder {
            private TextView mChannelName;
            private TextView mProgramName;
            private TextView mDuration;
            private ImageView mLogo;
            private ImageView mEncrypedindicator;
            //private ImageView favorite_indicator;
            private View mProgramentrance;
        }
        
        private ChannelListAdapter(Context c,boolean favorite) {
            mInflater = LayoutInflater.from(c);
            mIsFavorite = favorite;
        }        

        public int getCount() {
            int size;
            if (mIsFavorite) {
                size = mChannelListManager.getFavoriteList().size();
            } else {
                size = mChannelListManager.getChannelList().size();
            } 
            Log.d(TAG, "getCount() size = " + size);
            return size;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            long id = position;        
            return mIsFavorite ? (id | FAVORITE_MARK) : id;
        }        
        
        public View getView(int position, View convertView, ViewGroup parent) {    
            
            Log.d(TAG, "getView() position = " + position + " convertView = " + convertView);            
            position = getRealPosition(position);
            
            ViewHolder holder;
            if (convertView == null) {      
                
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.main_screen_item, null);
                
                holder.mChannelName = (TextView)convertView.findViewById(R.id.channel_name);   
                holder.mProgramName = (TextView)convertView.findViewById(R.id.program_name);
                holder.mDuration = (TextView)convertView.findViewById(R.id.duration);                  
                holder.mLogo = (ImageView)convertView.findViewById(R.id.logo_image);   
                holder.mEncrypedindicator = (ImageView)convertView.findViewById(R.id.encryped_indicator);
                //holder.favorite_indicator = (ImageView)convertView.findViewById(R.id.favorite_indicator); 
                holder.mProgramentrance = convertView.findViewById(R.id.program_entrance);
                holder.mProgramentrance.setTag((Integer)position); 
                holder.mProgramentrance.setOnClickListener(new View.OnClickListener() {                    
                    //@Override
                    public void onClick(View v) {
                    
                    Log.d(TAG, "mProgramentrance onClick mState = " + mState);
                        if (mState < STATE_UPDATED_SERVICE) {
                            if (mState == STATE_UPDATING_SERVICE) {
                                Toast.makeText(MainScreen.this, R.string.updating_sg_hint, 1000).show();
                            }
                            return;
                        }
                        gotoProgram((Integer)v.getTag());
                    }
                });
                
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder)convertView.getTag();
                //need to update the tag every time because the convertView used for representing 
                //a row of a specific position may change due to the recycling mechanism of ListView.
                holder.mProgramentrance.setTag((Integer)position);                 
            }                      
            
            Channel ch = mChannelListManager.getChannelList().get(position);
            Program pr = mChannelListManager.getProgramList().get(position);
            holder.mChannelName.setText(ch.name);
            holder.mProgramName.setText(pr.program);
            
            if (pr.programStartTime != 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                String duration = (String) sdf.format(new Date(pr.programStartTime))
                            + "-" + (String) sdf.format(new Date(pr.programEndTime));
                holder.mDuration.setText(duration);
            } else {
                holder.mDuration.setText(null);
            }
            
            Log.d(TAG, ch.name + "," + ch.serviceID + "," + ch.isFree() + "," + ch.isEncrypted());
            if (ch.lock_stat == Channel.LOCK_STATE_NONE) {
                holder.mEncrypedindicator.setVisibility(View.INVISIBLE);
                holder.mLogo.setImageBitmap(
                    ch.logo == null ? (ch.isAudio() ? mAudioChannelIcon : mVideoChannelIcon) : ch.logo);
            } else if (ch.lock_stat == Channel.LOCK_STATE_UNLOCK) {
                holder.mLogo.setImageBitmap(
                    ch.logo == null ? (ch.isAudio() ? mAudioChannelIcon : mVideoChannelIcon) : ch.logo);
                holder.mEncrypedindicator.setVisibility(View.VISIBLE);
                holder.mEncrypedindicator.setImageBitmap(mSubscribedIndicator);
            } else {
                holder.mEncrypedindicator.setVisibility(View.INVISIBLE);
                holder.mLogo.setImageBitmap(mUnsubscribedLogo);                    
            }
            
            if (ch.isFavorite) {
                holder.mChannelName.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.mini_favorite_icon,0);
            } else {
                holder.mChannelName.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
            }
            
            return convertView;
        }

        public int getRealPosition(int pos) {
            if (mIsFavorite) {
                pos = mChannelListManager.getFavoriteList().get(pos);
                Log.d(TAG, "getRealPosition() TAB_FAVORITE position = " + pos);
            }            
            return pos;
        }   
    }     

    //dual card start
    private void startUp() {
        List<ModeSwitchManager.SimItem> simList = mModeSwitchManager.getAvailableSims();
        
        if (simList == null || simList.isEmpty()) {
            showCmmbModeDialog(true);
        } else if (simList.size() == 1) {
            startLoadingAnimation();
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_SERVICE,2000);
        } else {
            showSelectSimDialog(true);
        }

    }
    
    private AlertDialog mSelectSimDlg;
    
    private void showSelectSimDialog(boolean toShow) {
        Log.d(TAG, "showSelectSimDialog() toShow = " + toShow);
        
        if (toShow && mSelectSimDlg == null) { 
            mWaitStateChangeConfirm = true;
            pauseLoadingAnimation();
            dismissAllDialogs();    
            mHandler.removeMessages(MSG_UPDATE_SERVICE);
            
            mSelectSimDlg = mModeSwitchManager.createSelectSimDialog(new SelectSimListerner() {
                public void onSelect() {
                    refreshWithUI(false); 
                }
                
                public void onCancel() {
                    finish();
                }
                
                public void onDismiss() {
                    mWaitStateChangeConfirm = false;
                    mSelectSimDlg = null;
                }
            });
            mSelectSimDlg.show();
            
        } else if (!toShow && mSelectSimDlg != null) {
            mSelectSimDlg.dismiss();
        }
        
    }
        
}
