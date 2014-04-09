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
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.mediatek.systemui.ext.IconIdWrapper;
import com.mediatek.systemui.ext.PluginFactory;
import com.android.systemui.statusbar.policy.NetworkController;

import com.mediatek.xlog.Xlog;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkController.SignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "SignalClusterView";

    NetworkController mNC;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0;
    private int mWifiActivityId = 0;
    private boolean mMobileVisible = false;
    private IconIdWrapper mMobileStrengthId = new IconIdWrapper();
    private IconIdWrapper mMobileActivityId = new IconIdWrapper(0);
    private int mMobileTypeId = 0;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private String mWifiDescription, mMobileDescription, mMobileTypeDescription;
    
    /// M: Support SIM Indicator. @{
    
    private boolean mShowSimIndicator = false;
    private int mSimIndicatorResource = 0;
    private ViewGroup mSignalClusterCombo;
    
    /// M: Support SIM Indicator. }@
    
    /// M: Support Roam Data Icon both show. @{
    
    private boolean mRoaming = false;
    private int mRoamingId = 0;
    private ImageView mMobileRoam;
    
    /// M: Support Roam Data Icon both show. }@

    ViewGroup mWifiGroup, mMobileGroup;
    ImageView mWifi, mMobile, mWifiActivity, mMobileActivity, mMobileType, mAirplane;
    View mSpacer;
    View mWifiSpacer;

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setNetworkController(NetworkController nc) {
        if (DEBUG) Slog.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiActivity   = (ImageView) findViewById(R.id.wifi_inout);
        mMobileGroup    = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobile         = (ImageView) findViewById(R.id.mobile_signal);
        mMobileActivity = (ImageView) findViewById(R.id.mobile_inout);
        mMobileType     = (ImageView) findViewById(R.id.mobile_type);
        mSpacer         =             findViewById(R.id.spacer);
        mWifiSpacer     =             findViewById(R.id.wifispacer);
        mAirplane       = (ImageView) findViewById(R.id.airplane);
        
        /// M: Support SIM Indicator. 
        mSignalClusterCombo           = (ViewGroup) findViewById(R.id.signal_cluster_combo);

        /// M: Support Roam Data Icon both show.
        mMobileRoam     =  (ImageView) findViewById(R.id.mobile_roaming);
        
        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup      = null;
        mWifi           = null;
        mWifiActivity   = null;
        mMobileGroup    = null;
        mMobile         = null;
        mMobileActivity = null;
        mMobileType     = null;
        mSpacer         = null;
        mWifiSpacer     = null;
        mAirplane       = null;
        
        /// M: Support Roam Data Icon both show.
        mMobileRoam     = null;

        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon,
            String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiActivityId = activityIcon;
        mWifiDescription = contentDescription;

        apply();
    }

    public void setMobileDataIndicators(boolean visible, IconIdWrapper strengthIcon, IconIdWrapper activityIcon,
            int typeIcon, String contentDescription, String typeContentDescription) {
        mMobileVisible = visible;
        mMobileStrengthId = strengthIcon.clone();
        mMobileActivityId = activityIcon.clone();
        mMobileTypeId = typeIcon;
        mMobileDescription = contentDescription;
        mMobileTypeDescription = typeContentDescription;

        Xlog.d(TAG, "setMobileDataIndicators"
                + " mMobileVisible=" + mMobileVisible
                + " mMobileStrengthId=" + mMobileStrengthId.getIconId()
                + " mMobileActivityId=" + mMobileActivityId.getIconId()
                + " mMobileTypeId=" + mMobileTypeId
                + " mWifiGroup" + ((mWifiGroup == null) ? "=null" : "!=null"));

        apply();
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;

        apply();
    }

    /// M: Support SIM Indicator. @{
    
    public void setShowSimIndicator(boolean showSimIndicator, int simIndicatorResId) {
        mShowSimIndicator = showSimIndicator;
        mSimIndicatorResource = simIndicatorResId;
        
        apply();
    }
    
    /// M: Support SIM Indicator. }@
    
    /// M: Support Roam Data Icon both show.
    public void setRoamingFlagandResource(boolean roaming, int roamingId) {
        mRoaming = roaming;
        mRoamingId = roamingId;
    }
    
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        if (mMobileVisible && mMobileGroup.getContentDescription() != null)
            event.getText().add(mMobileGroup.getContentDescription());
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) return;

        if (mWifiVisible) {
            mWifiGroup.setVisibility(View.VISIBLE);
            mWifi.setImageResource(mWifiStrengthId);
            mWifiActivity.setImageResource(mWifiActivityId);
            mWifiGroup.setContentDescription(mWifiDescription);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }
        if (mWifiVisible && PluginFactory.getStatusBarPlugin(mContext).supportDataTypeAlwaysDisplayWhileOn()) {
            mWifiSpacer.setVisibility(View.INVISIBLE);
        } else {
            mWifiSpacer.setVisibility(View.GONE);
        }
        if (DEBUG) Slog.d(TAG,
                String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));

        if (mMobileVisible && !mIsAirplaneMode) {
            
            /// M: Support Roam Data Icon both show. @{
            if (mRoaming) {
                mMobileRoam.setImageResource(mRoamingId);
                mMobileRoam.setVisibility(View.VISIBLE);
            } else {
                mMobileRoam.setImageResource(0);
                mMobileRoam.setVisibility(View.GONE);
            }
            /// M: Support Roam Data Icon both show. }@
            
            mMobileGroup.setVisibility(View.VISIBLE);
            if (mMobileStrengthId.getResources() != null) {
                mMobile.setImageDrawable(mMobileStrengthId.getDrawable());
            } else {
                if (mMobileStrengthId.getIconId() == 0) {
                    mMobile.setImageDrawable(null);
                } else {
                    mMobile.setImageResource(mMobileStrengthId.getIconId());
                }
            }
            if (mMobileActivityId.getResources() != null) {
                mMobileActivity.setImageDrawable(mMobileActivityId.getDrawable());
            } else {
                if (mMobileActivityId.getIconId() == 0) {
                    mMobileActivity.setImageDrawable(null);
                } else {
                    mMobileActivity.setImageResource(mMobileActivityId.getIconId());
                }
            }
            mMobileType.setImageResource(mMobileTypeId);
            Xlog.d(TAG, "apply() setImageResource(mMobileTypeId) mShowSimIndicator = " + mShowSimIndicator);
            mMobileGroup.setContentDescription(mMobileTypeDescription + " " + mMobileDescription);
            
            /// M: Airplane mode: Shouldn't show data type icon (L1)
            ///    OP01 project: Not in airplane mode, the data type icon should be always displayed (L2)
            ///    WifiVisible: Not in airplane mode, not OP01 project, the data type icon should follow the wifi visible status (L3)
            if (PluginFactory.getStatusBarPlugin(mContext).supportDataTypeAlwaysDisplayWhileOn()) {
                mMobileType.setVisibility(View.VISIBLE);
            } else {
                mMobileType.setVisibility(!mWifiVisible ? View.VISIBLE : View.GONE);
            }
            
            /// M: When the signal strength icon id is null should hide the data type icon, this including several status
            if (mMobileStrengthId.getIconId() == R.drawable.stat_sys_signal_null) {
                mMobileType.setVisibility(View.GONE);
            }
            
            /// M: Support SIM Indicator. @{
            if (mShowSimIndicator) {
                mSignalClusterCombo.setBackgroundResource(mSimIndicatorResource);
            } else {
                mSignalClusterCombo.setPadding(0, 0, 0, 0);
                mSignalClusterCombo.setBackgroundDrawable(null);
            }
            /// M: Support SIM Indicator. }@
        } else {
            mMobileGroup.setVisibility(View.GONE);
        }

        if (mIsAirplaneMode) {
            mAirplane.setVisibility(View.VISIBLE);
            mAirplane.setImageResource(mAirplaneIconId);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mMobileVisible && mWifiVisible && mIsAirplaneMode) {
            mSpacer.setVisibility(View.INVISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }

        if (DEBUG) Slog.d(TAG,
                String.format("mobile: %s sig=%d act=%d typ=%d",
                    (mMobileVisible ? "VISIBLE" : "GONE"),
                    mMobileStrengthId.getIconId(), mMobileActivityId.getIconId(), mMobileTypeId));

    }
}

