/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.GsmUmtsAdditionalCallOptions;
import com.android.phone.GsmUmtsCallForwardOptions;
import com.android.phone.R;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.vt.VTCallUtils;
import com.mediatek.xlog.Xlog;

import java.io.IOException;
import java.util.List;

public class VTAdvancedSetting extends PreferenceActivity 
        implements Preference.OnPreferenceChangeListener {

    //In current stage, we consider that only the card slot 0 support wcdma
    private static final String BUTTON_VT_REPLACE_KEY     = "button_vt_replace_expand_key";
    private static final String SELECT_DEFAULT_PICTURE    = "0";
    private static final String SELECT_MY_PICTURE         = "2";
    
    public static final String SELECT_DEFAULT_PICTURE2    = "0";
    public static final String SELECT_MY_PICTURE2         = "1";
    
    public static final String NAME_PIC_TO_REPLACE_LOCAL_VIDEO_USERSELECT = "pic_to_replace_local_video_userselect";
    public static final String NAME_PIC_TO_REPLACE_LOCAL_VIDEO_DEFAULT = "pic_to_replace_local_video_default";
    public static final String NAME_PIC_TO_REPLACE_PEER_VIDEO_USERSELECT = "pic_to_replace_peer_video_userselect";
    public static final String NAME_PIC_TO_REPLACE_PEER_VIDEO_DEFAULT = "pic_to_replace_peer_video_default";   

    /** The launch code when picking a photo and the raw data is returned */
    public static final int REQUESTCODE_PICTRUE_PICKED_WITH_DATA = 3021;
    private ListPreference mButtonVTReplace;
    private ListPreference mButtonVTPeerReplace;
    private int mWhichToSave = 0;
    
    // debug data
    private static final String LOG_TAG = "Settings/VTAdvancedSetting";
    private static final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);
    
    //Operator customization: SS used data
    private int mSimId = -1; 
    private static final String BUTTON_VT_CF_KEY = "button_cf_expand_key";
    private static final String BUTTON_VT_CB_KEY = "button_cb_expand_key";
    private static final String BUTTON_VT_MORE_KEY = "button_more_expand_key";
    
    private static final String BUTTON_VT_PEER_REPLACE_KEY = "button_vt_replace_peer_expand_key";
    private static final String BUTTON_VT_ENABLE_PEER_REPLACE_KEY = "button_vt_enable_peer_replace_key";
    private static final String BUTTON_VT_ENABLE_BACK_CAMERA_KEY     = "button_vt_enable_back_camera_key";
    private static final String BUTTON_VT_PEER_BIGGER_KEY     = "button_vt_peer_bigger_key";
    private static final String BUTTON_VT_MO_LOCAL_VIDEO_DISPLAY_KEY     = "button_vt_mo_local_video_display_key";
    private static final String BUTTON_VT_MT_LOCAL_VIDEO_DISPLAY_KEY     = "button_vt_mt_local_video_display_key";
    private static final String BUTTON_VT_AUTO_DROPBACK_KEY = "button_vt_auto_dropback_key";
    private static final String CHECKBOX_RING_ONLY_ONCE = "ring_only_once";
    private Preference mButtonCf = null;
    private Preference mButtonCb = null;
    private Preference mButtonMore = null;
    private CheckBoxPreference mButtonVTEnablePeerReplace;
    private CheckBoxPreference mButtonVTMoVideo;
    private CheckBoxPreference mCheckBoxRingOnlyOnce;
    private CheckBoxPreference mButtonVTEnablebackCamer;
    private CheckBoxPreference mButtonVTPeerBigger;
    private CheckBoxPreference mButtonVTAutoDropBack;
    private ListPreference mButtonVTMtVideo;
    
    private PreCheckForRunning mPreCfr = null;
    
    private static void log(String msg) {
        Xlog.d(LOG_TAG, msg);
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Xlog.d(LOG_TAG, "[action = " + intent.getAction() + "]");
            String action = intent.getAction();
            Xlog.d(LOG_TAG, "[action = " + action + "]");
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                setScreenEnabled();
            } else if (TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED.equals(action)) {
                setScreenEnabled();
            } else if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                findSimId();
                setScreenEnabled();
            }
        }
    };
     
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.vt_advanced_setting);
        
        mPreCfr = new PreCheckForRunning(this);

        mButtonVTReplace = (ListPreference)findPreference(BUTTON_VT_REPLACE_KEY);
        mButtonVTReplace.setOnPreferenceChangeListener(this);
        mButtonVTPeerReplace = (ListPreference)findPreference(BUTTON_VT_PEER_REPLACE_KEY);
        mButtonVTPeerReplace.setOnPreferenceChangeListener(this);
        mButtonCf = (Preference)findPreference(BUTTON_VT_CF_KEY);
        mButtonCb = (Preference)findPreference(BUTTON_VT_CB_KEY);
        mButtonMore = (Preference)findPreference(BUTTON_VT_MORE_KEY);
        mButtonVTEnablePeerReplace = (CheckBoxPreference)findPreference(BUTTON_VT_ENABLE_PEER_REPLACE_KEY);
        mButtonVTEnablePeerReplace.setOnPreferenceChangeListener(this);
        mButtonVTMoVideo = (CheckBoxPreference)findPreference(BUTTON_VT_MO_LOCAL_VIDEO_DISPLAY_KEY);
        mButtonVTMoVideo.setOnPreferenceChangeListener(this);
        mButtonVTMtVideo = (ListPreference)findPreference(BUTTON_VT_MT_LOCAL_VIDEO_DISPLAY_KEY);
        mButtonVTMtVideo.setOnPreferenceChangeListener(this);
        mButtonVTEnablebackCamer = (CheckBoxPreference)findPreference(BUTTON_VT_ENABLE_BACK_CAMERA_KEY);
        mButtonVTEnablebackCamer.setOnPreferenceChangeListener(this);
        mButtonVTPeerBigger = (CheckBoxPreference)findPreference(BUTTON_VT_PEER_BIGGER_KEY);
        mButtonVTPeerBigger.setOnPreferenceChangeListener(this);
        mButtonVTAutoDropBack = (CheckBoxPreference)findPreference(BUTTON_VT_AUTO_DROPBACK_KEY);
        mButtonVTAutoDropBack.setOnPreferenceChangeListener(this);
        mCheckBoxRingOnlyOnce = (CheckBoxPreference)findPreference(CHECKBOX_RING_ONLY_ONCE);

        Xlog.d(LOG_TAG,"FeatureOption.MTK_VT3G324M_SUPPORT=" + FeatureOption.MTK_VT3G324M_SUPPORT + "" 
                + "FeatureOption.MTK_PHONE_VT_VOICE_ANSWER=" + FeatureOption.MTK_PHONE_VT_VOICE_ANSWER);
        if (!(FeatureOption.MTK_VT3G324M_SUPPORT && FeatureOption.MTK_PHONE_VT_VOICE_ANSWER)) {
            getPreferenceScreen().removePreference(mCheckBoxRingOnlyOnce);
        } else {
            mCheckBoxRingOnlyOnce.setOnPreferenceChangeListener(this);
        } 

        findSimId();
        initVTSettings();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        registerReceiver(mReceiver, intentFilter);
    }

    public void onResume() {
        super.onResume();
        setScreenEnabled();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        Xlog.d(LOG_TAG,"[mSimId = " + mSimId + "]");
        Xlog.d(LOG_TAG,"[objValue = " + objValue + "]");
        Xlog.d(LOG_TAG,"[key = " + preference.getKey() + "]");
        if (preference == mButtonVTReplace) {
            
            VTCallUtils.checkVTFile(mSimId);  
            mWhichToSave = 0;
            
            if (objValue.toString().equals(SELECT_DEFAULT_PICTURE)) {
                if (DBG) {
                    log(" Picture for replacing local video -- selected DEFAULT PICTURE");
                }
                showDialogDefaultPic(VTAdvancedSetting.getPicPathDefault());                
            }  else if (objValue.toString().equals(SELECT_MY_PICTURE)) {
                if (DBG) {
                    log(" Picture for replacing local video -- selected MY PICTURE");
                }
                showDialogMyPic(VTAdvancedSetting.getPicPathUserselect(mSimId));                
            }
        } else if (preference == mButtonVTPeerReplace) {
            
            VTCallUtils.checkVTFile(mSimId);
            mWhichToSave = 1;
            
            if (objValue.toString().equals(SELECT_DEFAULT_PICTURE2)) {
                if (DBG) {
                    log(" Picture for replacing peer video -- selected DEFAULT PICTURE");
                }
                showDialogDefaultPic(VTAdvancedSetting.getPicPathDefault2());                
            } else if (objValue.toString().equals(SELECT_MY_PICTURE2)) {
                if (DBG) {
                    log(" Picture for replacing peer video -- selected MY PICTURE");
                }
                showDialogMyPic(VTAdvancedSetting.getPicPathUserselect2(mSimId));                
            }
            
        }
        
        return true;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DBG) {
            log("onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        }
        
        if (resultCode != RESULT_OK) {
            return;
        }
        
        switch (requestCode) {
        
        case REQUESTCODE_PICTRUE_PICKED_WITH_DATA:
            try {
                final Bitmap bitmap = data.getParcelableExtra("data");
                if (bitmap != null) {
                    if (mWhichToSave == 0) {
                        VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathUserselect(mSimId), bitmap);
                    } else {
                        VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathUserselect2(mSimId), bitmap);
                    }
                    bitmap.recycle();
                    if (DBG) {
                        log(" - Bitmap.isRecycled() : " + bitmap.isRecycled());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mWhichToSave == 0) {
                showDialogMyPic(VTAdvancedSetting.getPicPathUserselect(mSimId));  
            } else {
                showDialogMyPic(VTAdvancedSetting.getPicPathUserselect2(mSimId));  
            }
            break;
        default:
            break;
        }
    }
    
    private void showDialogDefaultPic(String filename) {
        final ImageView mImg = new ImageView(this);
        final Bitmap mBitmap = BitmapFactory.decodeFile(filename);
        mImg.setImageBitmap(mBitmap);
        LinearLayout linear = new LinearLayout(this);
        linear.addView(mImg, new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        linear.setGravity(Gravity.CENTER);
        
        AlertDialog.Builder myBuilder = new AlertDialog.Builder(this);
        myBuilder.setView(linear);
        myBuilder.setTitle(R.string.vt_pic_replace_local_default);
        myBuilder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog,
                    int which) {
            }
        });
        
        AlertDialog myAlertDialog = myBuilder.create();
        myAlertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mImg.setImageBitmap(null);
                if (!mBitmap.isRecycled()) {
                    mBitmap.recycle();
                }
            }
        });
        myAlertDialog.show(); 
    }
    
    private void showDialogMyPic(String filename) {
        final ImageView mImg2 = new ImageView(this);
        final Bitmap mBitmap2 = BitmapFactory.decodeFile(filename);
        mImg2.setImageBitmap(mBitmap2);
        LinearLayout linear = new LinearLayout(this);
        linear.addView(mImg2, new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        linear.setGravity(Gravity.CENTER);
        
        AlertDialog.Builder myBuilder = new AlertDialog.Builder(this);
        myBuilder.setView(linear);
        myBuilder.setTitle(R.string.vt_pic_replace_local_mypic);
        myBuilder.setPositiveButton(R.string.vt_change_my_pic,
                new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog,
                    int which) {
                // TODO Auto-generated method stub

                try {
                    Intent intent = new Intent(
                            Intent.ACTION_GET_CONTENT,
                            null);

                    intent.setType("image/*");
                    intent.putExtra("crop", "true");
                    intent.putExtra("aspectX", 1);
                    intent.putExtra("aspectY", 1);
                    intent.putExtra("outputX",
                            getResources().getInteger(
                                    R.integer.qcif_x));
                    intent.putExtra("outputY",
                            getResources().getInteger(
                                    R.integer.qcif_y));
                    intent
                            .putExtra("return-data",
                                    true);
                    intent.putExtra("scaleUpIfNeeded", true);

                    startActivityForResult(intent,
                            REQUESTCODE_PICTRUE_PICKED_WITH_DATA);

                } catch (ActivityNotFoundException e) {
                    if (DBG) {
                        log("Pictrue not found , Gallery ActivityNotFoundException !");
                    }
                }
                return;
            }
        });
        myBuilder.setNegativeButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog,
                    int which) {
            }
        });
        
        AlertDialog myAlertDialog = myBuilder.create();
        myAlertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mImg2.setImageBitmap(null);
                if (!mBitmap2.isRecycled()) {
                    mBitmap2.recycle();
                }
            }
        });
        myAlertDialog.show(); 
    }
    
    
    public static String getPicPathDefault() {
        return "/data/data/com.android.phone/" + NAME_PIC_TO_REPLACE_LOCAL_VIDEO_DEFAULT + ".vt";
    }
    
    public static String getPicPathUserselect(int slodId) {
        return "/data/data/com.android.phone/" + NAME_PIC_TO_REPLACE_LOCAL_VIDEO_USERSELECT + "_" + slodId + ".vt";
    }
    
    public static String getPicPathDefault2() {
        return "/data/data/com.android.phone/" + NAME_PIC_TO_REPLACE_PEER_VIDEO_DEFAULT + ".vt";
    }
    
    public static String getPicPathUserselect2(int slodId) {
        return "/data/data/com.android.phone/" + NAME_PIC_TO_REPLACE_PEER_VIDEO_USERSELECT + "_" + slodId + ".vt";
    }
    
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mButtonCf) {
            Intent intent = new Intent(this, GsmUmtsCallForwardOptions.class);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra("ISVT", true);
            mPreCfr.checkToRun(intent, mSimId, 302);
            return true;
        } else if (preference == mButtonCb) {
            Intent intent = new Intent(this, CallBarring.class);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra("ISVT", true);
            mPreCfr.checkToRun(intent, mSimId, 302);
            return true;
        } else if (preference == mButtonMore) {
            Intent intent = new Intent(this, GsmUmtsAdditionalCallOptions.class);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra("ISVT", true);
            mPreCfr.checkToRun(intent, mSimId, 302);
            
            return true;
        }
        return false;
    }
    
    protected void onDestroy() {
        super.onDestroy();
        if (mPreCfr != null) {
            mPreCfr.deRegister();
        }
        unregisterReceiver(mReceiver);
    }

    private void setScreenEnabled() {
        boolean airplaneModeOn = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, -1) == 1;

        List<SIMInfo> insertSim = SIMInfo.getInsertedSIMList(this);
        if (insertSim.size() == 0) {
            CallSettings.goUpToTopLevelSetting(this);
        } else if (insertSim.size() == 1) {
            boolean isRadioOn = !airplaneModeOn && CallSettings.isRadioOn(mSimId);            
            boolean is3GEnable = mSimId >= 0;
            mButtonVTReplace.setEnabled(is3GEnable);
            mButtonVTPeerReplace.setEnabled(is3GEnable);
            mButtonVTEnablePeerReplace.setEnabled(is3GEnable);
            mButtonVTMoVideo.setEnabled(is3GEnable);
            mButtonVTMtVideo.setEnabled(is3GEnable);
            mButtonVTEnablebackCamer.setEnabled(is3GEnable);
            mButtonVTPeerBigger.setEnabled(is3GEnable);
            mButtonVTAutoDropBack.setEnabled(is3GEnable);

            mButtonCf.setEnabled(isRadioOn && is3GEnable);
            mButtonCb.setEnabled(isRadioOn && is3GEnable);
            mButtonMore.setEnabled(isRadioOn && is3GEnable);
        } else {
            CallSettings.goUpToTopLevelSetting(this);
        }
    }

    private void initVTSettings() {
        SharedPreferences sp = 
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (mButtonVTEnablePeerReplace != null) {
            String key = mButtonVTEnablePeerReplace.getKey() + "_" + mSimId;
            mButtonVTEnablePeerReplace.setKey(key);
            mButtonVTEnablePeerReplace.setChecked(sp.getBoolean(key, true));
        }
        if (mButtonVTMoVideo != null) {
            String key = mButtonVTMoVideo.getKey() + "_" + mSimId;
            mButtonVTMoVideo.setKey(key);
            mButtonVTMoVideo.setChecked(sp.getBoolean(key, true));
        }
        if (mButtonVTMtVideo != null) {
            String key = mButtonVTMtVideo.getKey() + "_" + mSimId;
            mButtonVTMtVideo.setKey(key);
            mButtonVTMtVideo.setValue(sp.getString(key, "0"));
        }
        if (mButtonVTEnablebackCamer != null) {
            String key = mButtonVTEnablebackCamer.getKey() + "_" + mSimId;
            mButtonVTEnablebackCamer.setKey(key);
            mButtonVTEnablebackCamer.setChecked(sp.getBoolean(key, true));
        }
        if (mButtonVTPeerBigger != null) {
            String key = mButtonVTPeerBigger.getKey() + "_" + mSimId;
            mButtonVTPeerBigger.setKey(key);
            mButtonVTPeerBigger.setChecked(sp.getBoolean(key, true));
        }
        if (mButtonVTAutoDropBack != null) {
            String key = mButtonVTAutoDropBack.getKey() + "_" + mSimId;
            mButtonVTAutoDropBack.setKey(key);
            mButtonVTAutoDropBack.setChecked(sp.getBoolean(key, false));
        }
        if (mButtonVTReplace != null) {
            String key = mButtonVTReplace.getKey() + "_" + mSimId;
            mButtonVTReplace.setKey(key);
            mButtonVTReplace.setValue(sp.getString(key, "0"));
        }
        if (mButtonVTPeerReplace != null) {
            String key = mButtonVTPeerReplace.getKey() + "_" + mSimId;
            mButtonVTPeerReplace.setKey(key);
            mButtonVTPeerReplace.setValue(sp.getString(key, "0"));
        }
    }

    private void findSimId() {
        int[] simIds = CallSettings.get3GSimCardSlots(this);
        if (simIds.length == 0) {
            mSimId = -1;
        } else if (simIds.length == 1){
            mSimId = simIds[0];
        } else {
            finish();
        }
    }
}
