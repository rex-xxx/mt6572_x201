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

package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkControllerGemini;
import com.android.systemui.statusbar.policy.TelephonyIconsGemini;
import com.android.systemui.statusbar.util.SIMHelper;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.systemui.ext.IconIdWrapper;
import com.mediatek.systemui.ext.NetworkType;
import com.mediatek.systemui.ext.PluginFactory;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

/// M: [SystemUI] Support dual SIM.
public class SignalClusterViewGemini extends LinearLayout implements NetworkControllerGemini.SignalCluster {
    private static final String TAG = "SignalClusterViewGemini";

    static final boolean DEBUG = false;
    
    private NetworkControllerGemini mNC;

    private boolean[] mRoaming;
    private int[] mRoamingId;
    private boolean[] mShowSimIndicator;
    private int[] mSimIndicatorResource;
    
    private boolean mIsAirplaneMode = false;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0;
    private int mWifiActivityId = 0;
    private String mWifiDescription;

    private boolean[] mMobileVisible;
    private IconIdWrapper[][] mMobileStrengthId;
    private IconIdWrapper[] mMobileActivityId;
    private IconIdWrapper[] mMobileTypeId;
    private String[] mMobileDescription;
    private String[] mMobileTypeDescription;

    private ViewGroup mWifiGroup;
    private ImageView mWifi;
    private ImageView mWifiActivity;

    private ViewGroup[] mSignalClusterCombo;
    private ImageView[] mSignalNetworkType;
    
    private ViewGroup[] mMobileGroup;
    private ImageView[] mMobileRoam;
    private ImageView[] mMobile;
    private ImageView[] mMobile2;
    private ImageView[] mMobileActivity;
    private ImageView[] mMobileType;
    private View[] mSpacer;
    private ImageView[] mMobileSlotIndicator;
    
    private View mFlightMode;

    private int[] mSIMColorId;
    
    private ViewGroup mDataConnectionGroup;

    private NetworkType[] mDataNetType;
    private ImageView[] mMobileNetType;
    
    private int mGeminiSimNum = PhoneConstants.GEMINI_SIM_NUM;
    private int mMobileStrengthIdNum = 2;

    public SignalClusterViewGemini(Context context) {
        this(context, null);
    }

    public SignalClusterViewGemini(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterViewGemini(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mRoaming = new boolean[mGeminiSimNum];
        mRoamingId = new int[mGeminiSimNum];
        mMobileDescription = new String[mGeminiSimNum];
        mMobileTypeDescription = new String[mGeminiSimNum];
        mSignalClusterCombo = new ViewGroup[mGeminiSimNum];
        mSignalNetworkType = new ImageView[mGeminiSimNum];
        mMobileGroup = new ViewGroup[mGeminiSimNum];
        mMobileRoam = new ImageView[mGeminiSimNum];
        mMobile = new ImageView[mGeminiSimNum];
        mMobile2 = new ImageView[mGeminiSimNum];
        mMobileActivity = new ImageView[mGeminiSimNum];
        mMobileType = new ImageView[mGeminiSimNum];
        mSpacer = new View[mGeminiSimNum];    
        mMobileSlotIndicator = new ImageView[mGeminiSimNum]; 
        mDataNetType = new NetworkType[mGeminiSimNum];    
        mMobileNetType = new ImageView[mGeminiSimNum];
        mSIMColorId = new int[mGeminiSimNum];
        mMobileActivityId = new IconIdWrapper[mGeminiSimNum];
        mMobileTypeId = new IconIdWrapper[mGeminiSimNum];
        mMobileStrengthId = new IconIdWrapper[mGeminiSimNum][];
        mShowSimIndicator = new boolean[mGeminiSimNum];    
        mSimIndicatorResource = new int[mGeminiSimNum];
        mMobileVisible = new boolean[mGeminiSimNum];
        
        for(int i = 0;i < mGeminiSimNum ; i++) {
            mMobileStrengthId[i] = new IconIdWrapper[mMobileStrengthIdNum];
            for (int j = 0 ; j < mMobileStrengthIdNum ; j++) {
                mMobileStrengthId[i][j] = new IconIdWrapper(0);
            }
            mMobileTypeId[i] = new IconIdWrapper(0);
            mMobileActivityId[i] = new IconIdWrapper(0);
        }
    }

    public void setNetworkControllerGemini(NetworkControllerGemini nc) {
        if (DEBUG) {
            Xlog.d(TAG, "NetworkControllerGemini=" + nc);
        }
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup                    = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi                         = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity                 = (ImageView) findViewById(R.id.wifi_inout);

        mMobile[PhoneConstants.GEMINI_SIM_1]                    = (ImageView) findViewById(R.id.mobile_signal);
        mMobile2[PhoneConstants.GEMINI_SIM_1]                   = (ImageView) findViewById(R.id.mobile_signal2);
        mMobileGroup[PhoneConstants.GEMINI_SIM_1]               = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobileActivity[PhoneConstants.GEMINI_SIM_1]            = (ImageView) findViewById(R.id.mobile_inout);
        mMobileType[PhoneConstants.GEMINI_SIM_1]                = (ImageView) findViewById(R.id.mobile_type);
        mMobileRoam[PhoneConstants.GEMINI_SIM_1]                = (ImageView) findViewById(R.id.mobile_roaming);
        mSpacer[PhoneConstants.GEMINI_SIM_1]                    =             findViewById(R.id.spacer);
        mMobileSlotIndicator[PhoneConstants.GEMINI_SIM_1]       = (ImageView) findViewById(R.id.mobile_slot_indicator);
        mSignalClusterCombo[PhoneConstants.GEMINI_SIM_1]        = (ViewGroup) findViewById(R.id.signal_cluster_combo);
        mSignalNetworkType[PhoneConstants.GEMINI_SIM_1]         = (ImageView) findViewById(R.id.network_type);

        for (int i = 1 ; i < mGeminiSimNum; i++) { // i must be started from 1, because SIM_1 has been set
            int k = i+1;
            mMobile[i]                    = (ImageView) findViewWithTag("mobile_signal_"+k);
            mMobile2[i]                   = (ImageView) findViewWithTag("mobile_signal2_"+k);
            mMobileGroup[i]               = (ViewGroup) findViewWithTag("mobile_combo_"+k);
            mMobileActivity[i]            = (ImageView) findViewWithTag("mobile_inout_"+k);
            mMobileType[i]                = (ImageView) findViewWithTag("mobile_type_"+k);
            mMobileRoam[i]                = (ImageView) findViewWithTag("mobile_roaming_"+k);
            mSpacer[i]                    =             findViewWithTag("spacer_"+k);
            mMobileSlotIndicator[i]       = (ImageView) findViewWithTag("mobile_slot_indicator_"+k);
            mSignalClusterCombo[i]        = (ViewGroup) findViewWithTag("signal_cluster_combo_"+k);
            mSignalNetworkType[i]         = (ImageView) findViewWithTag("network_type_"+k);
        }

        mFlightMode                   = (ImageView) findViewById(R.id.flight_mode);
        
        for(int i = 0; i < mGeminiSimNum ; i++) {
            int resId = PluginFactory.getStatusBarPlugin(mContext).getSignalIndicatorIconGemini(i);
            if (resId != -1) {
                mMobileSlotIndicator[i].setImageDrawable(PluginFactory.getStatusBarPlugin(mContext).getPluginResources().getDrawable(resId));
                mMobileSlotIndicator[i].setVisibility(View.VISIBLE);
            } else {
                mMobileSlotIndicator[i].setImageResource(0);
                mMobileSlotIndicator[i].setVisibility(View.GONE);
            }
        }
        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup            = null;
        mWifi                 = null;
        mWifiActivity         = null;
        mDataConnectionGroup  = null;
        
        for(int i = 0; i < mGeminiSimNum ; i++) {
            mMobileGroup[i]          = null;
            mMobile[i]               = null;
            mMobileActivity[i]       = null;
            mMobileType[i]           = null;
            mSpacer[i]               = null;
            mMobileRoam[i]           = null;
            mMobileNetType[i]        = null;
            mMobile2[i]              = null;
        }
                        
        super.onDetachedFromWindow();
    }

    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
            String contentDescription) {
        Xlog.d(TAG, "setWifiIndicators, visible=" + visible + ", strengthIcon=" + strengthIcon + ", activityIcon=" 
            + activityIcon + ", contentDescription=" + contentDescription);
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;
    }

    public void setMobileDataIndicators(int slotId, boolean visible, IconIdWrapper[] strengthIcon,
            IconIdWrapper activityIcon, IconIdWrapper typeIcon, String contentDescription, String typeContentDescription) {
        Xlog.d(TAG, "setMobileDataIndicators(" + slotId + "), visible=" + visible + ", strengthIcon[0] ~ [1] "
                + strengthIcon[0].getIconId() + " ~ " + strengthIcon[1].getIconId());
                
        mMobileVisible[slotId] = visible;
        mMobileStrengthId[slotId][0] = strengthIcon[0].clone();
        mMobileStrengthId[slotId][1] = strengthIcon[1].clone();
        mMobileActivityId[slotId] = activityIcon.clone();
        mMobileTypeId[slotId] = typeIcon.clone();
        mMobileDescription[slotId] = contentDescription;
        mMobileTypeDescription[slotId] = typeContentDescription;
    }

    public void setIsAirplaneMode(boolean is) {
        Xlog.d(TAG, "setIsAirplaneMode=" + is);
        mIsAirplaneMode = is;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup.getContentDescription() != null) {
            event.getText().add(mWifiGroup.getContentDescription());
        }
        if (mMobileVisible[PhoneConstants.GEMINI_SIM_1] && mMobileGroup[PhoneConstants.GEMINI_SIM_1].getContentDescription() != null) {
            event.getText().add(mMobileGroup[PhoneConstants.GEMINI_SIM_1].getContentDescription());
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }
    
    public void setRoamingFlagandResource(boolean[] roaming, int[] roamingId) {
        for(int i = 0; i < mGeminiSimNum ; i++) {
            Xlog.d(TAG, "setRoamingFlagandResource(" + i + "), roaming=" + roaming[i] + ", roamingId="+ roamingId[i]);
            mRoaming[i] = roaming[i];
            mRoamingId[i] = roamingId[i];
        }
    }

    public void setShowSimIndicator(int slotId, boolean showSimIndicator, int simIndicatorResource) {
        Xlog.d(TAG, "setShowSimIndicator(" + slotId + "), showSimIndicator=" + showSimIndicator 
                + " simIndicatorResource = " + simIndicatorResource);
        mShowSimIndicator[slotId] = showSimIndicator;
        mSimIndicatorResource[slotId] = simIndicatorResource;
    }

    public void setDataNetType3G(int slotId, NetworkType dataNetType) {
        Xlog.d(TAG, "setDataNetType3G(" + slotId + "), dataNetType=" + dataNetType);
        mDataNetType[slotId] = dataNetType;
    }

    // Run after each indicator change.
    public void apply() {
        if (mWifiGroup == null) {
            Xlog.d(TAG, "apply(), mWifiGroup is null, return");
            return;
        }

        Xlog.d(TAG, "apply(), mWifiVisible is " + mWifiVisible);
        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) {
            Xlog.d(TAG, String.format("wifi: %s sig=%d act=%d", (mWifiVisible ? "VISIBLE" : "GONE"), mWifiStrengthId,
                    mWifiActivityId));
        }
        
        for(int i = 0; i < mGeminiSimNum ; i++) {
            boolean mShouldMobileGroupVisible = shouldMobileGroupVisible(i);
            Xlog.d(TAG, "apply(), slotId=" + i +", mMobileVisible=" + mMobileVisible[i] + ", mShouldMobileGroupVisible=" + mShouldMobileGroupVisible);
            if (mMobileVisible[i] && mShouldMobileGroupVisible) {
            	  mSignalClusterCombo[i].setVisibility(View.VISIBLE);
                if (mRoaming[i]) {
                    mMobileRoam[i].setBackgroundResource(mRoamingId[i]);
                    mMobileRoam[i].setVisibility(View.VISIBLE);
                } else {
                    mMobileRoam[i].setVisibility(View.GONE);
                }

                if (mMobileStrengthId[i][0].getIconId() == PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthNullIconGemini(i)
                    || mMobileStrengthId[i][0].getIconId() == 0 || mMobileStrengthId[i][0].getIconId() == R.drawable.stat_sys_gemini_signal_null) {
                    mMobileRoam[i].setVisibility(View.GONE);
                }
                
                if (mMobileStrengthId[i][0].getResources() != null) {
                    mMobile[i].setImageDrawable(mMobileStrengthId[i][0].getDrawable());
                } else {
                    if (mMobileStrengthId[i][0].getIconId() == 0) {
                        mMobile[i].setImageDrawable(null);
                    } else {
                        mMobile[i].setImageResource(mMobileStrengthId[i][0].getIconId());
                    }
                }
                if (mMobileStrengthId[i][1].getResources() != null) {
                    mMobile2[i].setImageDrawable(mMobileStrengthId[i][1].getDrawable());
                } else {
                    if (mMobileStrengthId[i][1].getIconId() == 0) {
                        mMobile2[i].setImageDrawable(null);
                    } else {
                        mMobile2[i].setImageResource(mMobileStrengthId[i][1].getIconId());
                    }
                }

                if (NetworkType.Type_1X3G != mDataNetType[i]) {
                    mMobile2[i].setVisibility(View.GONE);
                }
                Xlog.d(TAG, "apply(), slotId=" + i + ", mRoaming=" + mRoaming[i]
                        + " mMobileActivityId=" + mMobileActivityId[i].getIconId()
                        + " mMobileTypeId=" + mMobileTypeId[i].getIconId()
                        + " mMobileStrengthId[0] = " + "" + mMobileStrengthId[i][0].getIconId()
                        + " mMobileStrengthId[1] = " + mMobileStrengthId[i][1].getIconId());

                if (mMobileActivityId[i].getResources() != null) {
                    mMobileActivity[i].setImageDrawable(mMobileActivityId[i].getDrawable());
                } else {
                    if (mMobileActivityId[i].getIconId() == 0) {
                        mMobileActivity[i].setImageDrawable(null);
                    } else {
                        mMobileActivity[i].setImageResource(mMobileActivityId[i].getIconId());
                    }
                }
                if (mMobileTypeId[i].getResources() != null) {
                    mMobileType[i].setImageDrawable(mMobileTypeId[i].getDrawable());
                } else {
                    if (mMobileTypeId[i].getIconId() == 0) {
                        mMobileType[i].setImageDrawable(null);
                    } else {
                        mMobileType[i].setImageResource(mMobileTypeId[i].getIconId());
                    }
                }

                int state = SIMHelper.getSimIndicatorStateGemini(i);
                if (SIMHelper.isSimInserted(i)
                        && PhoneConstants.SIM_INDICATOR_LOCKED != state
                        && PhoneConstants.SIM_INDICATOR_SEARCHING != state
                        && PhoneConstants.SIM_INDICATOR_INVALID != state
                        && PhoneConstants.SIM_INDICATOR_RADIOOFF != state) {
                    int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, i);
                    if (simColorId > -1 && simColorId < 4 && mDataNetType[i] != null) {
                        IconIdWrapper resId = new IconIdWrapper(0);
                        int id = PluginFactory.getStatusBarPlugin(mContext).getDataNetworkTypeIconGemini(mDataNetType[i], simColorId);
                        
                        if (id != -1) {
                            resId.setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                            resId.setIconId(id);
                        }
                        Xlog.d(TAG, "apply(), slot=" + i + ", mDataNetType=" + mDataNetType[i] + " resId= " + resId.getIconId() + " simColorId = "
                                + simColorId);
                        if (resId.getResources() != null) {
                            mSignalNetworkType[i].setImageDrawable(resId.getDrawable());
                        } else {
                            if (resId.getIconId() == 0) {
                                mSignalNetworkType[i].setImageDrawable(null);
                            } else {
                                mSignalNetworkType[i].setImageResource(resId.getIconId());
                            }
                        }
                        mSignalNetworkType[i].setVisibility(View.VISIBLE);
                        if (mMobileStrengthId[i][0].getIconId() == PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthNullIconGemini(i)
                                || mMobileStrengthId[i][0].getIconId() == 0 || mMobileStrengthId[i][0].getIconId() == R.drawable.stat_sys_gemini_signal_null) {
                            mSignalNetworkType[i].setVisibility(View.GONE);
                        }
                    }
                } else {
                    mSignalNetworkType[i].setImageDrawable(null);
                    mSignalNetworkType[i].setVisibility(View.GONE);
                }
                if (mMobileStrengthId[i][0].getIconId() == PluginFactory.getStatusBarPlugin(mContext)
                        .getSignalStrengthNullIconGemini(i) || mMobileStrengthId[i][0].getIconId() == R.drawable.stat_sys_gemini_signal_null) {
                    mMobileSlotIndicator[i].setVisibility(View.INVISIBLE);
                }
                mMobileGroup[i].setContentDescription(mMobileTypeDescription[i] + " " + mMobileDescription[i]);
                if (mShowSimIndicator[i]) {
                    mSignalClusterCombo[i].setBackgroundResource(mSimIndicatorResource[i]);
                } else {
                    mSignalClusterCombo[i].setBackgroundDrawable(null);
                }
                mSignalClusterCombo[i].setPadding(0, 0, 0, 3);
            
                // For OP01 project data type icon should be always displayed
                if (PluginFactory.getStatusBarPlugin(mContext).supportDataTypeAlwaysDisplayWhileOn()) {
                    mMobileType[i].setVisibility(View.VISIBLE);
                } else {
                    mMobileType[i].setVisibility((!mWifiVisible) ? View.VISIBLE : View.GONE);
                }
            
            /// M: When searching hide the data type icon
                int resId = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthSearchingIconGemini(i);
                int resId1 = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthNullIconGemini(i);
                if (resId == mMobileStrengthId[i][0].getIconId() || resId1 == mMobileStrengthId[i][0].getIconId()
                        || mMobileStrengthId[i][0].getIconId() == R.drawable.stat_sys_gemini_signal_null) {
                    mMobileType[i].setVisibility(View.GONE); 
                }
            } else {
                mSignalClusterCombo[i].setVisibility(View.GONE);
            }
            
            Xlog.d(TAG, "apply(). mIsAirplaneMode is " + mIsAirplaneMode);
            if (mIsAirplaneMode) {
                mSignalClusterCombo[i].setVisibility(View.GONE);
                mFlightMode.setVisibility(View.VISIBLE);            
            } else {
                mFlightMode.setVisibility(View.GONE);            
            }
        }

        if (mWifiVisible) {
            mSpacer[0].setVisibility(View.INVISIBLE);
        } else {
            mSpacer[0].setVisibility(View.GONE);
        }
    }

    private boolean shouldMobileGroupVisible(int slotId) {
        if(SIMHelper.isSimInserted(slotId) || PluginFactory.getStatusBarPlugin(mContext).getMobileGroupVisible()) {
            return true;
        } else if (slotId == PhoneConstants.GEMINI_SIM_1) {
            for (int i = PhoneConstants.GEMINI_SIM_2 ; i < mGeminiSimNum ; i++) {
                if(SIMHelper.isSimInserted(i)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
