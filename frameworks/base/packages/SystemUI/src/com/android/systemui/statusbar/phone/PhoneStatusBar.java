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

package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.StorageVolume;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.SignalClusterViewGemini;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.DateView;
import com.android.systemui.statusbar.policy.IntruderAlertView;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerGemini;
import com.android.systemui.statusbar.policy.NotificationRowLayout;
import com.android.systemui.statusbar.policy.OnSizeChangedListener;
import com.android.systemui.statusbar.policy.Prefs;
import com.android.systemui.statusbar.policy.TelephonyIcons;
import com.android.systemui.statusbar.policy.TelephonyIconsGemini;
import com.android.systemui.statusbar.toolbar.ToolBarIndicator;
import com.android.systemui.statusbar.toolbar.ToolBarView;
import com.android.systemui.statusbar.util.SIMHelper;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.systemui.ext.PluginFactory;
import com.mediatek.xlog.Xlog;
import com.mediatek.telephony.SimInfoManager;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class PhoneStatusBar extends BaseStatusBar {
    static final String TAG = "PhoneStatusBar";
    public static final boolean DEBUG = BaseStatusBar.DEBUG;
    public static final boolean SPEW = DEBUG;
    public static final boolean DUMPTRUCK = true; // extra dumpsys info
    public static final boolean DEBUG_GESTURES = false;

    public static final boolean DEBUG_CLINGS = false;

    public static final boolean ENABLE_NOTIFICATION_PANEL_CLING = false;

    public static final boolean SETTINGS_DRAG_SHORTCUT = true;

    // additional instrumentation for testing purposes; intended to be left on during development
    public static final boolean CHATTY = DEBUG;

    public static final String ACTION_STATUSBAR_START
            = "com.android.internal.policy.statusbar.START";
    
    /// M: Support AirplaneMode for Statusbar SimIndicator.
    private static final String ACTION_BOOT_IPO
            = "android.intent.action.ACTION_PREBOOT_IPO";
    /// M: [SystemUI] Dismiss new event icon when click clear button for keyguard.
    private static final String CLEAR_NEW_EVENT_VIEW_INTENT = "android.intent.action.KEYGUARD_CLEAR_UREAD_TIPS";

    private static final int MSG_OPEN_NOTIFICATION_PANEL = 1000;
    private static final int MSG_CLOSE_PANELS = 1001;
    private static final int MSG_OPEN_SETTINGS_PANEL = 1002;
    // 1020-1030 reserved for BaseStatusBar
    /// M: [SystemUI] Support "SIM indicator". @{
    private static final int MSG_SHOW_INTRUDER = 1003;
    private static final int MSG_HIDE_INTRUDER = 1004;
    /// @}

    // will likely move to a resource or other tunable param at some point
    private static final int INTRUDER_ALERT_DECAY_MS = 0; // disabled, was 10000;

    private static final boolean CLOSE_PANEL_WHEN_EMPTIED = true;

    /// M: [ALPS00512845] Handle SD Swap Condition.
    private static final boolean SUPPORT_SD_SWAP = true;

    private static final int NOTIFICATION_PRIORITY_MULTIPLIER = 10; // see NotificationManagerService
    private static final int HIDE_ICONS_BELOW_SCORE = Notification.PRIORITY_LOW * NOTIFICATION_PRIORITY_MULTIPLIER;

    // fling gesture tuning parameters, scaled to display density
    private float mSelfExpandVelocityPx; // classic value: 2000px/s
    private float mSelfCollapseVelocityPx; // classic value: 2000px/s (will be negated to collapse "up")
    private float mFlingExpandMinVelocityPx; // classic value: 200px/s
    private float mFlingCollapseMinVelocityPx; // classic value: 200px/s
    private float mCollapseMinDisplayFraction; // classic value: 0.08 (25px/min(320px,480px) on G1)
    private float mExpandMinDisplayFraction; // classic value: 0.5 (drag open halfway to expand)
    private float mFlingGestureMaxXVelocityPx; // classic value: 150px/s

    private float mExpandAccelPx; // classic value: 2000px/s/s
    private float mCollapseAccelPx; // classic value: 2000px/s/s (will be negated to collapse "up")

    private float mFlingGestureMaxOutputVelocityPx; // how fast can it really go? (should be a little 
                                                    // faster than mSelfCollapseVelocityPx)

    PhoneStatusBarPolicy mIconPolicy;

    // These are no longer handled by the policy, because we need custom strategies for them
    BluetoothController mBluetoothController;
    BatteryController mBatteryController;
    LocationController mLocationController;
    NetworkController mNetworkController;

    int mNaturalBarHeight = -1;
    int mIconSize = -1;
    int mIconHPadding = -1;
    Display mDisplay;
    Point mCurrentDisplaySize = new Point();

    IDreamManager mDreamManager;

    StatusBarWindowView mStatusBarWindow;
    PhoneStatusBarView mStatusBarView;

    int mPixelFormat;
    Object mQueueLock = new Object();

    // viewgroup containing the normal contents of the statusbar
    LinearLayout mStatusBarContents;
    
    // right-hand icons
    LinearLayout mSystemIconArea;
    
    // left-hand icons 
    LinearLayout mStatusIcons;
    // the icons themselves
    /// M: Support "SIM Indicator".
    private ImageView mSimIndicatorIcon;
    /// M: For AT&T
    private TextView mPlmnLabel;
    IconMerger mNotificationIcons;
    // [+>
    View mMoreIcon;

    // expanded notifications
    NotificationPanelView mNotificationPanel; // the sliding/resizing panel within the notification window
    public ScrollView mScrollView;
    View mExpandedContents;
    int mNotificationPanelGravity;
    int mNotificationPanelMarginBottomPx, mNotificationPanelMarginPx;
    float mNotificationPanelMinHeightFrac;
    boolean mNotificationPanelIsFullScreenWidth;
    TextView mNotificationPanelDebugText;

    // settings
    QuickSettings mQS;
    public boolean mHasSettingsPanel, mHasFlipSettings;
    SettingsPanelView mSettingsPanel;
    public View mFlipSettingsView;
    QuickSettingsContainerView mSettingsContainer;
    int mSettingsPanelGravity;

    // top bar
    View mNotificationPanelHeader;
    View mDateTimeView; 
    View mClearButton;
    ImageView mSettingsButton, mNotificationButton;
    /// M: [SystemUI] Remove settings button to notification header.
    private View mHeaderSettingsButton;

    // carrier/wifi label
    private TextView mCarrierLabel;
    private boolean mCarrierLabelVisible = false;
    private int mCarrierLabelHeight;
    /// M: Calculate ToolBar height when sim indicator is showing.
    private int mToolBarViewHeight;
    private TextView mEmergencyCallLabel;
    private int mNotificationHeaderHeight;

    private boolean mShowCarrierInPanel = false;

    // position
    int[] mPositionTmp = new int[2];
    boolean mExpandedVisible;

    // the date view
    DateView mDateView;

    // for immersive activities
    private IntruderAlertView mIntruderAlertView;

    // on-screen navigation buttons
    private NavigationBarView mNavigationBarView = null;

    // the tracker view
    int mTrackingPosition; // the position of the top of the tracking view.

    // ticker
    private Ticker mTicker;
    private View mTickerView;
    private boolean mTicking;

    // Tracking finger for opening/closing.
    int mEdgeBorder; // corresponds to R.dimen.status_bar_edge_ignore
    boolean mTracking;
    VelocityTracker mVelocityTracker;

    // help screen
    private boolean mClingShown;
    private ViewGroup mCling;
    private boolean mSuppressStatusBarDrags; // while a cling is up, briefly deaden the bar to give things time to settle

    boolean mAnimating;
    boolean mClosing; // only valid when mAnimating; indicates the initial acceleration
    float mAnimY;
    float mAnimVel;
    float mAnimAccel;
    long mAnimLastTimeNanos;
    boolean mAnimatingReveal = false;
    int mViewDelta;
    float mFlingVelocity;
    int mFlingY;
    int[] mAbsPos = new int[2];
    Runnable mPostCollapseCleanup = null;

    private Animator mLightsOutAnimation;
    private Animator mLightsOnAnimation;

    /// M: Support "Change font size of phone".    
    private float mPreviousConfigFontScale;

    // for disabling the status bar
    int mDisabled = 0;

    // tracking calls to View.setSystemUiVisibility()
    int mSystemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE;

    DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    /// M: [ALPS00336833] When orientation changed, request layout to avoid status bar layout error. @{
    boolean mNeedRelayout = false;
    private int mPrevioutConfigOrientation;
    /// M: [ALPS00336833] When orientation changed, request layout to avoid status bar layout error. @}

    // XXX: gesture research
    private final GestureRecorder mGestureRec = DEBUG_GESTURES
        ? new GestureRecorder("/sdcard/statusbar_gestures.dat") 
        : null;

    private int mNavigationIconHints = 0;
    private final Animator.AnimatorListener mMakeIconsInvisible = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            // double-check to avoid races
            if (mStatusBarContents.getAlpha() == 0) {
                if (DEBUG) Slog.d(TAG, "makeIconsInvisible");
                mStatusBarContents.setVisibility(View.INVISIBLE);
            }
        }
    };

    // ensure quick settings is disabled until the current user makes it through the setup wizard
    private boolean mUserSetup = false;
    private ContentObserver mUserSetupObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            final boolean userSetup = 0 != Settings.Secure.getIntForUser(
                    mContext.getContentResolver(),
                    Settings.Secure.USER_SETUP_COMPLETE,
                    0 /*default */,
                    mCurrentUserId);
            if (MULTIUSER_DEBUG) Slog.d(TAG, String.format("User setup changed: " +
                    "selfChange=%s userSetup=%s mUserSetup=%s",
                    selfChange, userSetup, mUserSetup));
            if (mSettingsButton != null && mHasFlipSettings) {
                mSettingsButton.setVisibility(userSetup ? View.VISIBLE : View.INVISIBLE);
            }
            if (mSettingsPanel != null) {
                mSettingsPanel.setEnabled(userSetup);
            }
            if (userSetup != mUserSetup) {
                mUserSetup = userSetup;
                if (!mUserSetup && mStatusBarView != null)
                    animateCollapseQuickSettings();
            }
        }
    };

    @Override
    public void start() {
        mDisplay = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        mDreamManager = IDreamManager.Stub.asInterface(
                ServiceManager.checkService(DreamService.DREAM_SERVICE));

        super.start(); // calls createAndAddWindows()

        addNavigationBar();

        if (ENABLE_INTRUDERS) addIntruderView();

        // Lastly, call to the icon policy to install/update all the icons.
        mIconPolicy = new PhoneStatusBarPolicy(mContext);
    }

    // ================================================================================
    // Constructing the view
    // ================================================================================
    protected PhoneStatusBarView makeStatusBarView() {
        final Context context = mContext;

        /// M: Support "Change font size of phone".
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        mPreviousConfigFontScale = config.fontScale;
        mPrevioutConfigOrientation = config.orientation;
        updateDisplaySize(); // populates mDisplayMetrics
        loadDimens();

        /// M: Support AirplaneMode for Statusbar SimIndicator.
        updateAirplaneMode();

        mIconSize = res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_icon_size);

        /// M: [SystemUI] Support "Dual SIM". {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mStatusBarWindow = (StatusBarWindowView)View.inflate(context, R.layout.gemini_super_status_bar, null);
        } else {
            mStatusBarWindow = (StatusBarWindowView) View.inflate(context, R.layout.super_status_bar, null);
        }
        /// M: [SystemUI] Support "Dual SIM". }
        mStatusBarView = (PhoneStatusBarView) mStatusBarWindow.findViewById(R.id.status_bar);

        if (DEBUG) {
            mStatusBarWindow.setBackgroundColor(0x6000FF80);
        }
        mStatusBarWindow.mService = this;
        mStatusBarWindow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (mExpandedVisible && !mAnimating) {
                        animateCollapsePanels();
                    }
                }
                return mStatusBarWindow.onTouchEvent(event);
            }});

        mStatusBarView.setBar(this);
        

        PanelHolder holder = (PanelHolder) mStatusBarWindow.findViewById(R.id.panel_holder);
        mStatusBarView.setPanelHolder(holder);

        mNotificationPanel = (NotificationPanelView) mStatusBarWindow.findViewById(R.id.notification_panel);
        mNotificationPanel.setStatusBar(this);
        mNotificationPanelIsFullScreenWidth =
            (mNotificationPanel.getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT);

        // make the header non-responsive to clicks
        mNotificationPanel.findViewById(R.id.header).setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return true; // e eats everything
                    }
                });
        /// M: [ALPS00352181] When ActivityManager.isHighEndGfx(mDisplay) return true, the dialog
        /// will show error, it will has StatusBar windowBackground.
        mStatusBarWindow.setBackground(null);
        if (!ActivityManager.isHighEndGfx()) {
            mNotificationPanel.setBackground(new FastColorDrawable(context.getResources().getColor(
                    R.color.notification_panel_solid_background)));
        }
        if (ENABLE_INTRUDERS) {
            mIntruderAlertView = (IntruderAlertView) View.inflate(context, R.layout.intruder_alert, null);
            mIntruderAlertView.setVisibility(View.GONE);
            mIntruderAlertView.setBar(this);
        }
        if (MULTIUSER_DEBUG) {
            mNotificationPanelDebugText = (TextView) mNotificationPanel.findViewById(R.id.header_debug_info);
            mNotificationPanelDebugText.setVisibility(View.VISIBLE);
        }

        updateShowSearchHoldoff();

        try {
            boolean showNav = mWindowManagerService.hasNavigationBar();
            if (DEBUG) Slog.v(TAG, "hasNavigationBar=" + showNav);
            if (showNav) {
                mNavigationBarView =
                    (NavigationBarView) View.inflate(context, R.layout.navigation_bar, null);

                mNavigationBarView.setDisabledFlags(mDisabled);
                mNavigationBarView.setBar(this);
            }
        } catch (RemoteException ex) {
            // no window manager? good luck with that
        }

        // figure out which pixel-format to use for the status bar.
        mPixelFormat = PixelFormat.OPAQUE;

        mSystemIconArea = (LinearLayout) mStatusBarView.findViewById(R.id.system_icon_area);
        mStatusIcons = (LinearLayout)mStatusBarView.findViewById(R.id.statusIcons);
        mNotificationIcons = (IconMerger)mStatusBarView.findViewById(R.id.notificationIcons);
        mNotificationIcons.setOverflowIndicator(mMoreIcon);
        mStatusBarContents = (LinearLayout)mStatusBarView.findViewById(R.id.status_bar_contents);
        mTickerView = mStatusBarView.findViewById(R.id.ticker);
        /// M: For AT&T
        if (!FeatureOption.MTK_GEMINI_SUPPORT && 
                !PluginFactory.getStatusBarPlugin(mContext)
                .isHspaDataDistinguishable() &&
                !PluginFactory.getStatusBarPlugin(context)
                .supportDataTypeAlwaysDisplayWhileOn()) {
            mPlmnLabel = (TextView) mStatusBarView.findViewById(R.id.att_plmn);
        }
        /// M: [SystemUI] Support "Notification toolbar". {
        mToolBarSwitchPanel = mStatusBarWindow.findViewById(R.id.toolBarSwitchPanel);
        mToolBarView = (ToolBarView) mStatusBarWindow.findViewById(R.id.tool_bar_view);
        ToolBarIndicator indicator = (ToolBarIndicator) mStatusBarWindow.findViewById(R.id.indicator);
        mToolBarView.setStatusBarService(this);
        mToolBarView.setToolBarSwitchPanel(mToolBarSwitchPanel);
        mToolBarView.setScrollToScreenCallback(indicator);
        mToolBarView.setToolBarIndicator(indicator);
        mToolBarView.hideSimSwithPanel();
        mToolBarView.moveToDefaultScreen(false);
        /// M: [SystemUI] Support "Notification toolbar". }

        /// M: [SystemUI] Support "SIM indicator". {
        mSimIndicatorIcon = (ImageView) mStatusBarView.findViewById(R.id.sim_indicator_internet_or_alwaysask);
        /// M: [SystemUI] Support "SIM indicator". }

        mPile = (NotificationRowLayout)mStatusBarWindow.findViewById(R.id.latestItems);
        mPile.setLayoutTransitionsEnabled(false);
        mPile.setLongPressListener(getNotificationLongClicker());
        mExpandedContents = mPile; // was: expanded.findViewById(R.id.notificationLinearLayout);

        mNotificationPanelHeader = mStatusBarWindow.findViewById(R.id.header);

        mClearButton = mStatusBarWindow.findViewById(R.id.clear_all_button);
        mClearButton.setOnClickListener(mClearButtonListener);
        mClearButton.setAlpha(0f);
        mClearButton.setVisibility(View.GONE);
        mClearButton.setEnabled(false);
        mDateView = (DateView)mStatusBarWindow.findViewById(R.id.date);

        mHasSettingsPanel = res.getBoolean(R.bool.config_hasSettingsPanel);
        mHasFlipSettings = res.getBoolean(R.bool.config_hasFlipSettingsPanel);

        mDateTimeView = mNotificationPanelHeader.findViewById(R.id.datetime);
        if (mHasFlipSettings) {
            mDateTimeView.setOnClickListener(mClockClickListener);
            mDateTimeView.setEnabled(true);
        }

        mSettingsButton = (ImageView) mStatusBarWindow.findViewById(R.id.settings_button);
        if (mSettingsButton != null) {
            mSettingsButton.setOnClickListener(mSettingsButtonListener);
            if (mHasSettingsPanel) {
                /// M: [SystemUI] Remove settings button to notification header @{.
                mHeaderSettingsButton = mStatusBarWindow.findViewById(R.id.header_settings_button);
                mHeaderSettingsButton.setOnClickListener(mHeaderSettingsButtonListener);
                /// M: [SystemUI] Remove settings button to notification header @}.
                if (mStatusBarView.hasFullWidthNotifications()) {
                    // the settings panel is hiding behind this button
                    mSettingsButton.setImageResource(R.drawable.ic_notify_quicksettings);
                    mSettingsButton.setVisibility(View.VISIBLE);
                } else {
                    // there is a settings panel, but it's on the other side of the (large) screen
                    final View buttonHolder = mStatusBarWindow.findViewById(
                            R.id.settings_button_holder);
                    if (buttonHolder != null) {
                        buttonHolder.setVisibility(View.GONE);
                    }
                }
            } else {
                // no settings panel, go straight to settings
                mSettingsButton.setVisibility(View.VISIBLE);
                mSettingsButton.setImageResource(R.drawable.ic_notify_settings);
            }
        }
        if (mHasFlipSettings) {
            mNotificationButton = (ImageView) mStatusBarWindow.findViewById(R.id.notification_button);
            if (mNotificationButton != null) {
                mNotificationButton.setOnClickListener(mNotificationButtonListener);
            }
        }

        mScrollView = (ScrollView)mStatusBarWindow.findViewById(R.id.scroll);
        mScrollView.setVerticalScrollBarEnabled(false); // less drawing during pulldowns
        if (!mNotificationPanelIsFullScreenWidth) {
            mScrollView.setSystemUiVisibility(
                    View.STATUS_BAR_DISABLE_NOTIFICATION_TICKER |
                    View.STATUS_BAR_DISABLE_NOTIFICATION_ICONS |
                    View.STATUS_BAR_DISABLE_CLOCK);
        }

        mTicker = new MyTicker(context, mStatusBarView);

        TickerView tickerView = (TickerView)mStatusBarView.findViewById(R.id.tickerText);
        tickerView.mTicker = mTicker;

        mEdgeBorder = res.getDimensionPixelSize(R.dimen.status_bar_edge_ignore);

        // set the inital view visibility
        setAreThereNotifications();

        // Other icons
        mLocationController = new LocationController(mContext); // will post a notification
        mBatteryController = new BatteryController(mContext);
        mBatteryController.addIconView((ImageView)mStatusBarView.findViewById(R.id.battery));
        mBatteryController.addLabelView((TextView) mStatusBarWindow.findViewById(R.id.percentage));
        mBluetoothController = new BluetoothController(mContext);

        /// M: [SystemUI] Support "Dual SIM". {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            /// M: Support GeminiPlus
            mCarrier1 = (CarrierLabelGemini) mStatusBarWindow.findViewById(R.id.carrier1);
            mCarrier2 = (CarrierLabelGemini) mStatusBarWindow.findViewById(R.id.carrier2);
            mCarrier3 = (CarrierLabelGemini) mStatusBarWindow.findViewById(R.id.carrier3);
            mCarrier4 = (CarrierLabelGemini) mStatusBarWindow.findViewById(R.id.carrier4);
            mCarrierDivider = mStatusBarWindow.findViewById(R.id.carrier_divider);
            mCarrierDivider2 = mStatusBarWindow.findViewById(R.id.carrier_divider2);
            mCarrierDivider3 = mStatusBarWindow.findViewById(R.id.carrier_divider3);
            mCarrierLabelGemini = (LinearLayout) mStatusBarWindow.findViewById(R.id.carrier_label_gemini);
            mShowCarrierInPanel = (mCarrierLabelGemini != null);
            if (mShowCarrierInPanel) {
                mCarrier1.setSlotId(PhoneConstants.GEMINI_SIM_1);
                mCarrier2.setSlotId(PhoneConstants.GEMINI_SIM_2);
                mCarrier3.setSlotId(PhoneConstants.GEMINI_SIM_3);
                mCarrier4.setSlotId(PhoneConstants.GEMINI_SIM_4);
            }
        } else {
            mCarrierLabel = (TextView)mStatusBarWindow.findViewById(R.id.carrier_label);
            mShowCarrierInPanel = (mCarrierLabel != null);
        }
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mNetworkControllerGemini = new NetworkControllerGemini(mContext);
            final SignalClusterViewGemini signalCluster =
                (SignalClusterViewGemini) mStatusBarView.findViewById(R.id.signal_cluster);
            mNetworkControllerGemini.addSignalCluster(signalCluster);
            signalCluster.setNetworkControllerGemini(mNetworkControllerGemini);
            /// M: Support GeminiPlus
            if(PhoneConstants.GEMINI_SIM_NUM == 2) {
                mNetworkControllerGemini.setCarrierGemini(mCarrier1, mCarrier2, mCarrierDivider);
            } else if(PhoneConstants.GEMINI_SIM_NUM == 3) {
                mNetworkControllerGemini.setCarrierGemini(mCarrier1, mCarrier2, mCarrier3, mCarrierDivider, mCarrierDivider2);
            } else if(PhoneConstants.GEMINI_SIM_NUM == 4) {
                mNetworkControllerGemini.setCarrierGemini(mCarrier1, mCarrier2, mCarrier3, mCarrier4, mCarrierDivider, mCarrierDivider2, mCarrierDivider3);
            }
        } else {
            mNetworkController = new NetworkController(mContext);
            final SignalClusterView signalCluster =
                (SignalClusterView)mStatusBarView.findViewById(R.id.signal_cluster);
            mNetworkController.addSignalCluster(signalCluster);
            signalCluster.setNetworkController(mNetworkController);
        }
        /// M: [SystemUI] Support "Dual SIM". }

        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            mEmergencyCallLabel = (TextView)mStatusBarWindow.findViewById(R.id.emergency_calls_only);
            if (mEmergencyCallLabel != null) {
                mNetworkController.addEmergencyLabelView(mEmergencyCallLabel);
                mEmergencyCallLabel.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) { }});
                mEmergencyCallLabel.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        updateCarrierLabelVisibility(false);
                    }});
            }
        }
        if (DEBUG) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                Slog.v(TAG, "carrierlabelGemini=" + mCarrierLabelGemini + " show=" + mShowCarrierInPanel);
            } else {
                Slog.v(TAG, "carrierlabel=" + mCarrierLabel + " show=" + mShowCarrierInPanel);
            }
        }
        if (mShowCarrierInPanel) {
            /// M: [SystemUI] Support "Dual SIM". {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mCarrierLabelGemini.setVisibility(mCarrierLabelVisible ? View.VISIBLE : View.INVISIBLE);
                mCarrier2.setVisibility(View.GONE);
                mCarrierDivider.setVisibility(View.GONE);
            } else {
                mCarrierLabel.setVisibility(mCarrierLabelVisible ? View.VISIBLE : View.INVISIBLE);
            }
            /// M: [SystemUI] Support "Dual SIM". }

            // for mobile devices, we always show mobile connection info here (SPN/PLMN)
            // for other devices, we show whatever network is connected
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                if (!mNetworkController.hasMobileDataFeature()) {
                    mNetworkController.addCombinedLabelView(mCarrierLabel);
                }
            }

            // set up the dynamic hide/show of the label
            mPile.setOnSizeChangedListener(new OnSizeChangedListener() {
                @Override
                public void onSizeChanged(View view, int w, int h, int oldw, int oldh) {
                    updateCarrierLabelVisibility(false);
                }
            });
        }

        // Quick Settings (where available, some restrictions apply)
        if (mHasSettingsPanel) {
            // first, figure out where quick settings should be inflated
            final View settings_stub;
            if (mHasFlipSettings) {
                // a version of quick settings that flips around behind the notifications
                settings_stub = mStatusBarWindow.findViewById(R.id.flip_settings_stub);
                if (settings_stub != null) {
                    mFlipSettingsView = ((ViewStub)settings_stub).inflate();
                    mFlipSettingsView.setVisibility(View.GONE);
                    mFlipSettingsView.setVerticalScrollBarEnabled(false);
                }
            } else {
                // full quick settings panel
                settings_stub = mStatusBarWindow.findViewById(R.id.quick_settings_stub);
                if (settings_stub != null) {
                    mSettingsPanel = (SettingsPanelView) ((ViewStub)settings_stub).inflate();
                } else {
                    mSettingsPanel = (SettingsPanelView) mStatusBarWindow.findViewById(R.id.settings_panel);
                }

                if (mSettingsPanel != null) {
                    if (!ActivityManager.isHighEndGfx()) {
                        mSettingsPanel.setBackground(new FastColorDrawable(context.getResources().getColor(
                                R.color.notification_panel_solid_background)));
                    }
                }
            }

            // wherever you find it, Quick Settings needs a container to survive
            mSettingsContainer = (QuickSettingsContainerView)
                    mStatusBarWindow.findViewById(R.id.quick_settings_container);
            if (mSettingsContainer != null) {
                mQS = new QuickSettings(mContext, mSettingsContainer);
                if (!mNotificationPanelIsFullScreenWidth) {
                    mSettingsContainer.setSystemUiVisibility(
                            View.STATUS_BAR_DISABLE_NOTIFICATION_TICKER
                            | View.STATUS_BAR_DISABLE_SYSTEM_INFO);
                }
                if (mSettingsPanel != null) {
                    mSettingsPanel.setQuickSettings(mQS);
                }
                mQS.setService(this);
                mQS.setBar(mStatusBarView);
                mQS.setup(mBatteryController);
            } else {
                mQS = null; // fly away, be free
            }
        }

        mClingShown = ! (DEBUG_CLINGS 
            || !Prefs.read(mContext).getBoolean(Prefs.SHOWN_QUICK_SETTINGS_HELP, false));

        if (!ENABLE_NOTIFICATION_PANEL_CLING || ActivityManager.isRunningInTestHarness()) {
            mClingShown = true;
        }

//        final ImageView wimaxRSSI =
//                (ImageView)sb.findViewById(R.id.wimax_signal);
//        if (wimaxRSSI != null) {
//            mNetworkController.addWimaxIconView(wimaxRSSI);
//        }

        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SKIN_CHANGED);
        /// M: ALPS00349274 to hide navigation bar when ipo shut down to avoid it flash when in boot ipo mode.{
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        filter.addAction("android.intent.action.ACTION_BOOT_IPO");
        /// M: ALPS00349274 to hide navigation bar when ipo shut down to avoid it flash when in boot ipo mode.}
        /// M: Support "Dual SIM PLMN".
        filter.addAction(Telephony.Intents.SPN_STRINGS_UPDATED_ACTION);
        context.registerReceiver(mBroadcastReceiver, filter);

        // listen for USER_SETUP_COMPLETE setting (per-user)
        resetUserSetupObserver();
        /// M: [SystemUI] Support "Dual SIM". {
        IntentFilter simInfoIntentFilter = new IntentFilter();
        simInfoIntentFilter.addAction(Intent.SIM_SETTINGS_INFO_CHANGED);
        simInfoIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INSERTED_STATUS);
        simInfoIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        simInfoIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        simInfoIntentFilter.addAction(ACTION_BOOT_IPO);
        if (!FeatureOption.MTK_GEMINI_SUPPORT && /// M: for AT&T
                !PluginFactory.getStatusBarPlugin(mContext)
                .isHspaDataDistinguishable() &&
                !PluginFactory.getStatusBarPlugin(context)
                .supportDataTypeAlwaysDisplayWhileOn()) {
            simInfoIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        }
        context.registerReceiver(mSIMInfoReceiver, simInfoIntentFilter);
        /// M: [SystemUI] Support "Dual SIM". }

        /// M: [ALPS00512845] Handle SD Swap Condition.
        mNeedRemoveKeys = new ArrayList<IBinder>();
        if (SUPPORT_SD_SWAP) {
            IntentFilter mediaEjectFilter = new IntentFilter();
            mediaEjectFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            mediaEjectFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            mediaEjectFilter.addDataScheme("file");
            context.registerReceiver(mMediaEjectBroadcastReceiver, mediaEjectFilter);
        }

        return mStatusBarView;
    }

    @Override
    protected View getStatusBarView() {
        return mStatusBarView;
    }

    @Override
    protected WindowManager.LayoutParams getRecentsLayoutParams(LayoutParams layoutParams) {
        boolean opaque = false;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                layoutParams.width,
                layoutParams.height,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                (opaque ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT));
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        } else {
            lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            lp.dimAmount = 0.75f;
        }
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        lp.setTitle("RecentsPanel");
        lp.windowAnimations = com.android.internal.R.style.Animation_RecentApplications;
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        return lp;
    }

    @Override
    protected WindowManager.LayoutParams getSearchLayoutParams(LayoutParams layoutParams) {
        boolean opaque = false;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                (opaque ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT));
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }
        lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        lp.setTitle("SearchPanel");
        // TODO: Define custom animation for Search panel
        lp.windowAnimations = com.android.internal.R.style.Animation_RecentApplications;
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        return lp;
    }

    @Override
    protected void updateSearchPanel() {
        super.updateSearchPanel();
        mSearchPanelView.setStatusBarView(mNavigationBarView);
        mNavigationBarView.setDelegateView(mSearchPanelView);
    }

    @Override
    public void showSearchPanel() {
        super.showSearchPanel();
        mHandler.removeCallbacks(mShowSearchPanel);

        // we want to freeze the sysui state wherever it is
        mSearchPanelView.setSystemUiVisibility(mSystemUiVisibility);

        WindowManager.LayoutParams lp =
            (android.view.WindowManager.LayoutParams) mNavigationBarView.getLayoutParams();
        lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mWindowManager.updateViewLayout(mNavigationBarView, lp);
    }

    @Override
    public void hideSearchPanel() {
        super.hideSearchPanel();
        WindowManager.LayoutParams lp =
            (android.view.WindowManager.LayoutParams) mNavigationBarView.getLayoutParams();
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mWindowManager.updateViewLayout(mNavigationBarView, lp);
    }

    protected int getStatusBarGravity() {
        return Gravity.TOP | Gravity.FILL_HORIZONTAL;
    }

    public int getStatusBarHeight() {
        if (mNaturalBarHeight < 0) {
            final Resources res = mContext.getResources();
            mNaturalBarHeight =
                    res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
        }
        return mNaturalBarHeight;
    }

    private View.OnClickListener mRecentsClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleRecentApps();
        }
    };

    private int mShowSearchHoldoff = 0;
    private Runnable mShowSearchPanel = new Runnable() {
        public void run() {
            showSearchPanel();
            awakenDreams();
        }
    };

    View.OnTouchListener mHomeSearchActionListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!shouldDisableNavbarGestures()) {
                    mHandler.removeCallbacks(mShowSearchPanel);
                    mHandler.postDelayed(mShowSearchPanel, mShowSearchHoldoff);
                }
            break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(mShowSearchPanel);
                awakenDreams();
            break;
        }
        return false;
        }
    };

    private void awakenDreams() {
        if (mDreamManager != null) {
            try {
                mDreamManager.awaken();
            } catch (RemoteException e) {
                // fine, stay asleep then
            }
        }
    }

    private void prepareNavigationBarView() {
        mNavigationBarView.reorient();

        mNavigationBarView.getRecentsButton().setOnClickListener(mRecentsClickListener);
        mNavigationBarView.getRecentsButton().setOnTouchListener(mRecentsPreloadOnTouchListener);
        mNavigationBarView.getHomeButton().setOnTouchListener(mHomeSearchActionListener);
        mNavigationBarView.getSearchLight().setOnTouchListener(mHomeSearchActionListener);
        updateSearchPanel();
    }

    // For small-screen devices (read: phones) that lack hardware navigation buttons
    private void addNavigationBar() {
        if (DEBUG) Slog.v(TAG, "addNavigationBar: about to add " + mNavigationBarView);
        if (mNavigationBarView == null) return;

        prepareNavigationBarView();

        mWindowManager.addView(mNavigationBarView, getNavigationBarLayoutParams());
    }

    private void repositionNavigationBar() {
        if (mNavigationBarView == null) return;

        prepareNavigationBarView();

        mWindowManager.updateViewLayout(mNavigationBarView, getNavigationBarLayoutParams());
    }

    private void notifyNavigationBarScreenOn(boolean screenOn) {
        if (mNavigationBarView == null) return;
        mNavigationBarView.notifyScreenOn(screenOn);
    }

    private WindowManager.LayoutParams getNavigationBarLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
                    0
                    | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.OPAQUE);
        // this will allow the navbar to run in an overlay on devices that support this
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }

        lp.setTitle("NavigationBar");
        lp.windowAnimations = 0;
        return lp;
    }

    private void addIntruderView() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL, // above the status bar!
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
        //lp.y += height * 1.5; // FIXME
        lp.setTitle("IntruderAlert");
        lp.packageName = mContext.getPackageName();
        lp.windowAnimations = R.style.Animation_StatusBar_IntruderAlert;

        mWindowManager.addView(mIntruderAlertView, lp);
    }

    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
        if (SPEW) Slog.d(TAG, "addIcon slot=" + slot + " index=" + index + " viewIndex=" + viewIndex
                + " icon=" + icon);
        StatusBarIconView view = new StatusBarIconView(mContext, slot, null);
        view.set(icon);
        mStatusIcons.addView(view, viewIndex, new LinearLayout.LayoutParams(mIconSize, mIconSize));
    }

    public void updateIcon(String slot, int index, int viewIndex,
            StatusBarIcon old, StatusBarIcon icon) {
        if (SPEW) Slog.d(TAG, "updateIcon slot=" + slot + " index=" + index + " viewIndex=" + viewIndex
                + " old=" + old + " icon=" + icon);
        StatusBarIconView view = (StatusBarIconView)mStatusIcons.getChildAt(viewIndex);
        view.set(icon);
    }

    public void removeIcon(String slot, int index, int viewIndex) {
        if (SPEW) Slog.d(TAG, "removeIcon slot=" + slot + " index=" + index + " viewIndex=" + viewIndex);
        mStatusIcons.removeViewAt(viewIndex);
    }

    public void addNotification(IBinder key, StatusBarNotification notification) {
        /// M: [ALPS00512845] Handle SD Swap Condition.
        if (SUPPORT_SD_SWAP) {
            try {
                ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(notification.pkg, 0);
                if ((applicationInfo.flags & applicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
                    if (mAvoidSDAppAddNotification) {
                        return;
                    }
                    if (!mNeedRemoveKeys.contains(key)) {
                        mNeedRemoveKeys.add(key);
                    }
                    Slog.d(TAG, "addNotification, applicationInfo pkg = " + notification.pkg + " to remove notification key = " + key);
                }
            } catch (NameNotFoundException e1) {
                e1.printStackTrace();
            }
        }

        if (DEBUG) Slog.d(TAG, "addNotification score=" + notification.score);
        StatusBarIconView iconView = addNotificationViews(key, notification);
        if (iconView == null) return;

        boolean immersive = false;
        try {
            immersive = ActivityManagerNative.getDefault().isTopActivityImmersive();
            if (DEBUG) {
                Slog.d(TAG, "Top activity is " + (immersive?"immersive":"not immersive"));
            }
        } catch (RemoteException ex) {
        }

        /*
         * DISABLED due to missing API
        if (ENABLE_INTRUDERS && (
                   // TODO(dsandler): Only if the screen is on
                notification.notification.intruderView != null)) {
            Slog.d(TAG, "Presenting high-priority notification");
            // special new transient ticker mode
            // 1. Populate mIntruderAlertView

            if (notification.notification.intruderView == null) {
                Slog.e(TAG, notification.notification.toString() + " wanted to intrude but intruderView was null");
                return;
            }

            // bind the click event to the content area
            PendingIntent contentIntent = notification.notification.contentIntent;
            final View.OnClickListener listener = (contentIntent != null)
                    ? new NotificationClicker(contentIntent,
                            notification.pkg, notification.tag, notification.id)
                    : null;

            mIntruderAlertView.applyIntruderContent(notification.notification.intruderView, listener);

            mCurrentlyIntrudingNotification = notification;

            // 2. Animate mIntruderAlertView in
            mHandler.sendEmptyMessage(MSG_SHOW_INTRUDER);

            // 3. Set alarm to age the notification off (TODO)
            mHandler.removeMessages(MSG_HIDE_INTRUDER);
            if (INTRUDER_ALERT_DECAY_MS > 0) {
                mHandler.sendEmptyMessageDelayed(MSG_HIDE_INTRUDER, INTRUDER_ALERT_DECAY_MS);
            }
        } else
         */

        if (notification.notification.fullScreenIntent != null) {
            // Stop screensaver if the notification has a full-screen intent.
            // (like an incoming phone call)
            awakenDreams();

            // not immersive & a full-screen alert should be shown
            if (DEBUG) Slog.d(TAG, "Notification has fullScreenIntent; sending fullScreenIntent");
            try {
                notification.notification.fullScreenIntent.send();
            } catch (PendingIntent.CanceledException e) {
            }
        } else {
            // usual case: status bar visible & not immersive

            // show the ticker if there isn't an intruder too
            if (mCurrentlyIntrudingNotification == null) {
                tick(null, notification, true);
            }
        }

        /// M: [SystemUI] Support "Dual SIM". {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            // process SIM info of notification.
            SimInfoManager.SimInfoRecord simInfo = null;
            int simInfoType = notification.notification.simInfoType;
            long simId = notification.notification.simId;
            if ((simInfoType >= 1 || simInfoType <= 3) && simId > 0) {
                Xlog.d(TAG, "addNotificationViews, simInfoType=" + simInfoType + ", simId=" + simId + ".");
                simInfo = SIMHelper.getSIMInfo(mContext, simId);
            }
            if (simInfo != null) {
                NotificationData.Entry entry = mNotificationData.findByKey(key);
                updateNotificationSimInfo(simInfo, notification.notification, iconView, entry.expanded);
            }
        }
        /// M: [SystemUI] Support "Dual SIM". }

        // Recalculate the position of the sliding windows and the titles.
        setAreThereNotifications();
        updateExpandedViewPos(EXPANDED_LEAVE_ALONE);
    }

    public void removeNotification(IBinder key) {
        StatusBarNotification old = removeNotificationViews(key);
        if (SPEW) Slog.d(TAG, "removeNotification key=" + key + " old=" + old);

        /// M: [ALPS00512845] Handle SD Swap Condition.
        if (SUPPORT_SD_SWAP) {
            if (mNeedRemoveKeys.contains(key)) {
                mNeedRemoveKeys.remove(key);
            }
        }

        if (old != null) {
            // Cancel the ticker if it's still running
            mTicker.removeEntry(old);

            // Recalculate the position of the sliding windows and the titles.
            updateExpandedViewPos(EXPANDED_LEAVE_ALONE);

            if (ENABLE_INTRUDERS && old == mCurrentlyIntrudingNotification) {
                mHandler.sendEmptyMessage(MSG_HIDE_INTRUDER);
            }

            if (CLOSE_PANEL_WHEN_EMPTIED && mNotificationData.size() == 0 && !mAnimating) {
                animateCollapsePanels();
            }
        }

        setAreThereNotifications();
    }

    private void updateShowSearchHoldoff() {
        mShowSearchHoldoff = mContext.getResources().getInteger(
            R.integer.config_show_search_delay);
    }

    private void loadNotificationShade() {
        if (mPile == null) return;

        int N = mNotificationData.size();

        ArrayList<View> toShow = new ArrayList<View>();

        final boolean provisioned = isDeviceProvisioned();
        // If the device hasn't been through Setup, we only show system notifications
        for (int i=0; i<N; i++) {
            Entry ent = mNotificationData.get(N-i-1);
            if (!(provisioned || showNotificationEvenIfUnprovisioned(ent.notification))) continue;
            if (!notificationIsForCurrentUser(ent.notification)) continue;
            toShow.add(ent.row);
        }

        ArrayList<View> toRemove = new ArrayList<View>();
        for (int i=0; i<mPile.getChildCount(); i++) {
            View child = mPile.getChildAt(i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }

        for (View remove : toRemove) {
            mPile.removeView(remove);
        }

        for (int i=0; i<toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                mPile.addView(v, i);
            }
        }

        if (mSettingsButton != null) {
            mSettingsButton.setEnabled(isDeviceProvisioned());
        }
    }

    @Override
    protected void updateNotificationIcons() {
        if (mNotificationIcons == null) return;

        loadNotificationShade();

        final LinearLayout.LayoutParams params
            = new LinearLayout.LayoutParams(mIconSize + 2*mIconHPadding, mNaturalBarHeight);

        int N = mNotificationData.size();

        if (DEBUG) {
            Slog.d(TAG, "refreshing icons: " + N + " notifications, mNotificationIcons=" + mNotificationIcons);
        }

        ArrayList<View> toShow = new ArrayList<View>();
        // M: StatusBar IconMerger feature, hash{pkg+icon}=iconlevel
        HashMap<String, Integer> uniqueIcon = new HashMap<String, Integer>();

        final boolean provisioned = isDeviceProvisioned();
        // If the device hasn't been through Setup, we only show system notifications
        for (int i=0; i<N; i++) {
            Entry ent = mNotificationData.get(N-i-1);
            if (!((provisioned && ent.notification.score >= HIDE_ICONS_BELOW_SCORE)
                    || showNotificationEvenIfUnprovisioned(ent.notification))) continue;
            if (!notificationIsForCurrentUser(ent.notification)) continue;

            // M: StatusBar IconMerger feature
            String key = ent.notification.pkg + String.valueOf(ent.notification.notification.icon);
            if (uniqueIcon.containsKey(key) && uniqueIcon.get(key) == ent.notification.notification.iconLevel) {
                Xlog.d(TAG, "updateNotificationIcons(), IconMerger feature, skip pkg / icon / iconlevel ="
                    + ent.notification.pkg + "/" + ent.notification.notification.icon + "/" + ent.notification.notification.iconLevel);
                continue;
            }

            toShow.add(ent.icon);
            uniqueIcon.put(key, ent.notification.notification.iconLevel);
        }
        uniqueIcon = null;

        ArrayList<View> toRemove = new ArrayList<View>();
        for (int i=0; i<mNotificationIcons.getChildCount(); i++) {
            View child = mNotificationIcons.getChildAt(i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }

        for (View remove : toRemove) {
            mNotificationIcons.removeView(remove);
        }

        for (int i=0; i<toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                mNotificationIcons.addView(v, i, params);
            }
        }
    }

    protected void updateCarrierLabelVisibility(boolean force) {
        if (!mShowCarrierInPanel) return;
        // The idea here is to only show the carrier label when there is enough room to see it, 
        // i.e. when there aren't enough notifications to fill the panel.
        if (DEBUG) {
            Slog.d(TAG, String.format("pileh=%d scrollh=%d carrierh=%d",
                    mPile.getHeight(), mScrollView.getHeight(), mCarrierLabelHeight));
        }

        final boolean emergencyCallsShownElsewhere = mEmergencyCallLabel != null;
        boolean makeVisible = false;
        /// M: Calculate ToolBar height when sim indicator is showing.
        /// M: Fix [ALPS00455548] Use getExpandedHeight instead of getHeight to avoid race condition.
        int height = mToolBarSwitchPanel.getVisibility() == View.VISIBLE ? 
                ((int)mNotificationPanel.getExpandedHeight() - mCarrierLabelHeight - mNotificationHeaderHeight - mToolBarViewHeight)
                : ((int)mNotificationPanel.getExpandedHeight() - mCarrierLabelHeight - mNotificationHeaderHeight);
        /// M: Support "Dual Sim" @{
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            makeVisible =
                mPile.getHeight() < height && mScrollView.getVisibility() == View.VISIBLE;
        } else {
            makeVisible =
                !(emergencyCallsShownElsewhere && mNetworkController.isEmergencyOnly())
                && mPile.getHeight() < height && mScrollView.getVisibility() == View.VISIBLE;
        }
        /// M: Support "Dual Sim" @}
        if (force || mCarrierLabelVisible != makeVisible) {
            mCarrierLabelVisible = makeVisible;
            if (DEBUG) {
                Slog.d(TAG, "making carrier label " + (makeVisible?"visible":"invisible"));
            }
            /// M: Support "Dual Sim" @{
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mCarrierLabelGemini.animate().cancel();
                if (makeVisible) {
                    mCarrierLabelGemini.setVisibility(View.VISIBLE);
                }
                mCarrierLabelGemini.animate()
                    .alpha(makeVisible ? 1f : 0f)
                    .setDuration(150)
                    .setListener(makeVisible ? null : new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!mCarrierLabelVisible) { // race
                                mCarrierLabelGemini.setVisibility(View.INVISIBLE);
                                mCarrierLabelGemini.setAlpha(0f);
                            }
                        }
                    })
                    .start();
            /// M: Support "Dual Sim" @{
            } else {
                mCarrierLabel.animate().cancel();
                if (makeVisible) {
                    mCarrierLabel.setVisibility(View.VISIBLE);
                }
                mCarrierLabel.animate()
                    .alpha(makeVisible ? 1f : 0f)
                    //.setStartDelay(makeVisible ? 500 : 0)
                    //.setDuration(makeVisible ? 750 : 100)
                    .setDuration(150)
                    .setListener(makeVisible ? null : new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!mCarrierLabelVisible) { // race
                                mCarrierLabel.setVisibility(View.INVISIBLE);
                                mCarrierLabel.setAlpha(0f);
                            }
                        }
                    })
                    .start();
            }
        }
    }

    @Override
    protected void setAreThereNotifications() {
        final boolean any = mNotificationData.size() > 0;

        final boolean clearable = any && mNotificationData.hasClearableItems();

        if (DEBUG) {
            Slog.d(TAG, "setAreThereNotifications: N=" + mNotificationData.size()
                    + " any=" + any + " clearable=" + clearable);
        }

        if (mHasFlipSettings 
                && mFlipSettingsView != null 
                && mFlipSettingsView.getVisibility() == View.VISIBLE
                && mScrollView.getVisibility() != View.VISIBLE) {
            // the flip settings panel is unequivocally showing; we should not be shown
            mClearButton.setVisibility(View.GONE);
        } else if (mClearButton.isShown()) {
            if (clearable != (mClearButton.getAlpha() == 1.0f)) {
                ObjectAnimator clearAnimation = ObjectAnimator.ofFloat(
                        mClearButton, "alpha", clearable ? 1.0f : 0.0f).setDuration(250);
                clearAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mClearButton.getAlpha() <= 0.0f) {
                            mClearButton.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (mClearButton.getAlpha() <= 0.0f) {
                            mClearButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
                clearAnimation.start();
            }
        } else {
            mClearButton.setAlpha(clearable ? 1.0f : 0.0f);
            mClearButton.setVisibility(clearable ? View.VISIBLE : View.GONE);
        }
        mClearButton.setEnabled(clearable);

        final View nlo = mStatusBarView.findViewById(R.id.notification_lights_out);
        final boolean showDot = (any&&!areLightsOn());
        if (showDot != (nlo.getAlpha() == 1.0f)) {
            if (showDot) {
                nlo.setAlpha(0f);
                nlo.setVisibility(View.VISIBLE);
            }
            nlo.animate()
                .alpha(showDot?1:0)
                .setDuration(showDot?750:250)
                .setInterpolator(new AccelerateInterpolator(2.0f))
                .setListener(showDot ? null : new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator _a) {
                        nlo.setVisibility(View.GONE);
                    }
                })
                .start();
        }

        updateCarrierLabelVisibility(false);
    }

    public void showClock(boolean show) {
        if (mStatusBarView == null) return;
        View clock = mStatusBarView.findViewById(R.id.clock);
        if (clock != null) {
            clock.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * State is one or more of the DISABLE constants from StatusBarManager.
     */
    public void disable(int state) {
        final int old = mDisabled;
        final int diff = state ^ old;
        mDisabled = state;

        if (DEBUG) {
            Slog.d(TAG, String.format("disable: 0x%08x -> 0x%08x (diff: 0x%08x)",
                old, state, diff));
        }

        StringBuilder flagdbg = new StringBuilder();
        flagdbg.append("disable: < ");
        flagdbg.append(((state & StatusBarManager.DISABLE_EXPAND) != 0) ? "EXPAND" : "expand");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_EXPAND) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) ? "ICONS" : "icons");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_NOTIFICATION_ALERTS) != 0) ? "ALERTS" : "alerts");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_NOTIFICATION_ALERTS) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) ? "TICKER" : "ticker");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_SYSTEM_INFO) != 0) ? "SYSTEM_INFO" : "system_info");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_SYSTEM_INFO) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_BACK) != 0) ? "BACK" : "back");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_BACK) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_HOME) != 0) ? "HOME" : "home");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_HOME) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_RECENT) != 0) ? "RECENT" : "recent");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_RECENT) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_CLOCK) != 0) ? "CLOCK" : "clock");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_CLOCK) != 0) ? "* " : " ");
        flagdbg.append(((state & StatusBarManager.DISABLE_SEARCH) != 0) ? "SEARCH" : "search");
        flagdbg.append(((diff  & StatusBarManager.DISABLE_SEARCH) != 0) ? "* " : " ");
        flagdbg.append(">");
        Slog.d(TAG, flagdbg.toString());

        if ((diff & StatusBarManager.DISABLE_SYSTEM_INFO) != 0) {
            mSystemIconArea.animate().cancel();
            if ((state & StatusBarManager.DISABLE_SYSTEM_INFO) != 0) {
                mSystemIconArea.animate()
                    .alpha(0f)
                    .translationY(mNaturalBarHeight*0.5f)
                    .setDuration(175)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .setListener(mMakeIconsInvisible)
                    .start();
            } else {
                mSystemIconArea.setVisibility(View.VISIBLE);
                mSystemIconArea.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setStartDelay(0)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .setDuration(175)
                    .start();
            }
        }

        if ((diff & StatusBarManager.DISABLE_CLOCK) != 0) {
            boolean show = (state & StatusBarManager.DISABLE_CLOCK) == 0;
            showClock(show);
        }
        if ((diff & StatusBarManager.DISABLE_EXPAND) != 0) {
            if ((state & StatusBarManager.DISABLE_EXPAND) != 0) {
                animateCollapsePanels();
            }
        }

        if ((diff & (StatusBarManager.DISABLE_HOME
                        | StatusBarManager.DISABLE_RECENT
                        | StatusBarManager.DISABLE_BACK
                        | StatusBarManager.DISABLE_SEARCH)) != 0) {
            // the nav bar will take care of these
            if (mNavigationBarView != null) mNavigationBarView.setDisabledFlags(state);

            if ((state & StatusBarManager.DISABLE_RECENT) != 0) {
                // close recents if it's visible
                mHandler.removeMessages(MSG_CLOSE_RECENTS_PANEL);
                mHandler.sendEmptyMessage(MSG_CLOSE_RECENTS_PANEL);
            }
        }

        if ((diff & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) {
            if ((state & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) {
                if (mTicking) {
                    haltTicker();
                }

                mNotificationIcons.animate()
                    .alpha(0f)
                    .translationY(mNaturalBarHeight*0.5f)
                    .setDuration(175)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .setListener(mMakeIconsInvisible)
                    .start();
            } else {
                mNotificationIcons.setVisibility(View.VISIBLE);
                mNotificationIcons.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setStartDelay(0)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .setDuration(175)
                    .start();
            }
        } else if ((diff & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) {
            if (mTicking && (state & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) {
                haltTicker();
            }
        }
    }

    @Override
    protected BaseStatusBar.H createHandler() {
        return new PhoneStatusBar.H();
    }

    /**
     * All changes to the status bar and notifications funnel through here and are batched.
     */
    private class H extends BaseStatusBar.H {
        public void handleMessage(Message m) {
            super.handleMessage(m);
            switch (m.what) {
                case MSG_OPEN_NOTIFICATION_PANEL:
                    animateExpandNotificationsPanel();
                    break;
                case MSG_OPEN_SETTINGS_PANEL:
                    animateExpandSettingsPanel();
                    break;
                case MSG_CLOSE_PANELS:
                    animateCollapsePanels();
                    break;
                case MSG_SHOW_INTRUDER:
                    setIntruderAlertVisibility(true);
                    break;
                case MSG_HIDE_INTRUDER:
                    setIntruderAlertVisibility(false);
                    mCurrentlyIntrudingNotification = null;
                    break;
            }
        }
    }

    public Handler getHandler() {
        return mHandler;
    }

    View.OnFocusChangeListener mFocusChangeListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            // Because 'v' is a ViewGroup, all its children will be (un)selected
            // too, which allows marqueeing to work.
            v.setSelected(hasFocus);
        }
    };

    void makeExpandedVisible(boolean revealAfterDraw) {
        if (SPEW) Slog.d(TAG, "Make expanded visible: expanded visible=" + mExpandedVisible);
        if (mExpandedVisible) {
            return;
        }

        mExpandedVisible = true;
        mPile.setLayoutTransitionsEnabled(true);
        if (mNavigationBarView != null)
            mNavigationBarView.setSlippery(true);

        updateCarrierLabelVisibility(true);

        updateExpandedViewPos(EXPANDED_LEAVE_ALONE);

        // Expand the window to encompass the full screen in anticipation of the drag.
        // This is only possible to do atomically because the status bar is at the top of the screen!
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) mStatusBarWindow.getLayoutParams();
        lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mWindowManager.updateViewLayout(mStatusBarWindow, lp);

        // Updating the window layout will force an expensive traversal/redraw.
        // Kick off the reveal animation after this is complete to avoid animation latency.
        if (revealAfterDraw) {
//            mHandler.post(mStartRevealAnimation);
        }

        /// M: Show always update clock of DateView.
        if (mDateView != null) {
            mDateView.updateClock();
        }
        visibilityChanged(true);
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
    }

    public void animateCollapsePanels(int flags) {
        if (SPEW) {
            Slog.d(TAG, "animateCollapse():"
                    + " mExpandedVisible=" + mExpandedVisible
                    + " mAnimating=" + mAnimating
                    + " mAnimatingReveal=" + mAnimatingReveal
                    + " mAnimY=" + mAnimY
                    + " mAnimVel=" + mAnimVel
                    + " flags=" + flags);
        }

        if ((flags & CommandQueue.FLAG_EXCLUDE_RECENTS_PANEL) == 0) {
            mHandler.removeMessages(MSG_CLOSE_RECENTS_PANEL);
            mHandler.sendEmptyMessage(MSG_CLOSE_RECENTS_PANEL);
        }

        if ((flags & CommandQueue.FLAG_EXCLUDE_SEARCH_PANEL) == 0) {
            mHandler.removeMessages(MSG_CLOSE_SEARCH_PANEL);
            mHandler.sendEmptyMessage(MSG_CLOSE_SEARCH_PANEL);
        }

        mStatusBarWindow.cancelExpandHelper();
        mStatusBarView.collapseAllPanels(true);
    }

    public ViewPropertyAnimator setVisibilityWhenDone(
            final ViewPropertyAnimator a, final View v, final int vis) {
        a.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setVisibility(vis);
                a.setListener(null); // oneshot
            }
        });
        return a;
    }

    public Animator setVisibilityWhenDone(
            final Animator a, final View v, final int vis) {
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setVisibility(vis);
            }
        });
        return a;
    }

    public Animator interpolator(TimeInterpolator ti, Animator a) {
        a.setInterpolator(ti);
        return a;
    }

    public Animator startDelay(int d, Animator a) {
        a.setStartDelay(d);
        return a;
    }
    
    public Animator start(Animator a) {
        a.start();
        return a;
    }

    final TimeInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    final TimeInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    final int FLIP_DURATION_OUT = 125;
    final int FLIP_DURATION_IN = 225;
    final int FLIP_DURATION = (FLIP_DURATION_IN + FLIP_DURATION_OUT);

    Animator mScrollViewAnim, mFlipSettingsViewAnim, mNotificationButtonAnim,
        mSettingsButtonAnim, mClearButtonAnim;
    /// M: [SystemUI] Remove settings button to notification header.
    private Animator mHeaderSettingsButtonAnim;

    @Override
    public void animateExpandNotificationsPanel() {
        if (SPEW) Slog.d(TAG, "animateExpand: mExpandedVisible=" + mExpandedVisible);
        if ((mDisabled & StatusBarManager.DISABLE_EXPAND) != 0) {
            return ;
        }

        mNotificationPanel.expand();
        if (mHasFlipSettings && mScrollView.getVisibility() != View.VISIBLE) {
            flipToNotifications();
        }

        if (false) postStartTracing();
    }
    
    // M: To expand slowly than usual.
    private void animateExpandNotificationsPanelSlow() {
        Slog.d(TAG, "animateExpandSlow: mExpandedVisible=" + mExpandedVisible);
        if ((mDisabled & StatusBarManager.DISABLE_EXPAND) != 0) {
            return ;
        }

        mNotificationPanel.expandSlow();
        if (mHasFlipSettings && mScrollView.getVisibility() != View.VISIBLE) {
            flipToNotifications();
        }

        if (false) postStartTracing();
    }

    public void flipToNotifications() {
        if (mFlipSettingsViewAnim != null) mFlipSettingsViewAnim.cancel();
        if (mScrollViewAnim != null) mScrollViewAnim.cancel();
        if (mSettingsButtonAnim != null) mSettingsButtonAnim.cancel();
        if (mNotificationButtonAnim != null) mNotificationButtonAnim.cancel();
        if (mClearButtonAnim != null) mClearButtonAnim.cancel();
        /// M: [SystemUI] Remove settings button to notification header @{.
        if (mHeaderSettingsButtonAnim != null) {
            mHeaderSettingsButtonAnim.cancel();
        }
        /// M: [SystemUI] Remove settings button to notification header @}.
        mScrollView.setVisibility(View.VISIBLE);
        mScrollViewAnim = start(
            startDelay(FLIP_DURATION_OUT,
                interpolator(mDecelerateInterpolator,
                    ObjectAnimator.ofFloat(mScrollView, View.SCALE_X, 0f, 1f)
                        .setDuration(FLIP_DURATION_IN)
                    )));
        mFlipSettingsViewAnim = start(
            setVisibilityWhenDone(
                interpolator(mAccelerateInterpolator,
                        ObjectAnimator.ofFloat(mFlipSettingsView, View.SCALE_X, 1f, 0f)
                        )
                    .setDuration(FLIP_DURATION_OUT),
                mFlipSettingsView, View.INVISIBLE));
        mNotificationButtonAnim = start(
            setVisibilityWhenDone(
                ObjectAnimator.ofFloat(mNotificationButton, View.ALPHA, 0f)
                    .setDuration(FLIP_DURATION),
                mNotificationButton, View.INVISIBLE));
        mSettingsButton.setVisibility(View.VISIBLE);
        mSettingsButtonAnim = start(
            ObjectAnimator.ofFloat(mSettingsButton, View.ALPHA, 1f)
                .setDuration(FLIP_DURATION));
        mClearButton.setVisibility(View.VISIBLE);
        mClearButton.setAlpha(0f);
        setAreThereNotifications(); // this will show/hide the button as necessary
        mNotificationPanel.postDelayed(new Runnable() {
            public void run() {
                updateCarrierLabelVisibility(false);
            }
        }, FLIP_DURATION - 150);
        /// M: [SystemUI] Remove settings button to notification header @{.
        if (mHeaderSettingsButton != null) {
            mHeaderSettingsButton.setVisibility(View.GONE);
        }
        /// M: [SystemUI] Remove settings button to notification header @}.
        /// M: [SystemUI] Support SimIndicator, show SimIndicator when notification panel is visible. @{
        if (mToolBarView.mSimSwitchPanelView.isPanelShowing()) {
            mToolBarSwitchPanel.setVisibility(View.VISIBLE);
        }
        /// M: [SystemUI] Support SimIndicator, show SimIndicator when notification panel is visible. @{
    }

    @Override
    public void animateExpandSettingsPanel() {
        if (SPEW) Slog.d(TAG, "animateExpand: mExpandedVisible=" + mExpandedVisible);
        if ((mDisabled & StatusBarManager.DISABLE_EXPAND) != 0) {
            return;
        }

        // Settings are not available in setup
        if (!mUserSetup) return;

        if (mHasFlipSettings) {
            mNotificationPanel.expand();
            if (mFlipSettingsView.getVisibility() != View.VISIBLE) {
                flipToSettings();
            }
        } else if (mSettingsPanel != null) {
            mSettingsPanel.expand();
        }

        if (false) postStartTracing();
    }

    public void switchToSettings() {
        // Settings are not available in setup
        if (!mUserSetup) return;

        mFlipSettingsView.setScaleX(1f);
        mFlipSettingsView.setVisibility(View.VISIBLE);
        mSettingsButton.setVisibility(View.GONE);
        mScrollView.setVisibility(View.GONE);
        mScrollView.setScaleX(0f);
        mNotificationButton.setVisibility(View.VISIBLE);
        mNotificationButton.setAlpha(1f);
        mClearButton.setVisibility(View.GONE);
        /// M: [SystemUI] Remove settings button to notification header @{.
        if (mHeaderSettingsButton != null) {
            mHeaderSettingsButton.setVisibility(View.VISIBLE);
        }
        /// M: [SystemUI] Remove settings button to notification header @}.
        /// M: [SystemUI] Support SimIndicator, hide SimIndicator when settings panel is visible.
        mToolBarSwitchPanel.setVisibility(View.GONE);
    }

    public void flipToSettings() {
        // Settings are not available in setup
        if (!mUserSetup) return;

        if (mFlipSettingsViewAnim != null) mFlipSettingsViewAnim.cancel();
        if (mScrollViewAnim != null) mScrollViewAnim.cancel();
        if (mSettingsButtonAnim != null) mSettingsButtonAnim.cancel();
        if (mNotificationButtonAnim != null) mNotificationButtonAnim.cancel();
        if (mClearButtonAnim != null) mClearButtonAnim.cancel();
        /// M: [SystemUI] Remove settings button to notification header @{.
        if (mHeaderSettingsButtonAnim != null) {
            mHeaderSettingsButtonAnim.cancel();
        }
        /// M: [SystemUI] Remove settings button to notification header @}.
        mFlipSettingsView.setVisibility(View.VISIBLE);
        mFlipSettingsView.setScaleX(0f);
        mFlipSettingsViewAnim = start(
            startDelay(FLIP_DURATION_OUT,
                interpolator(mDecelerateInterpolator,
                    ObjectAnimator.ofFloat(mFlipSettingsView, View.SCALE_X, 0f, 1f)
                        .setDuration(FLIP_DURATION_IN)
                    )));
        mScrollViewAnim = start(
            setVisibilityWhenDone(
                interpolator(mAccelerateInterpolator,
                        ObjectAnimator.ofFloat(mScrollView, View.SCALE_X, 1f, 0f)
                        )
                    .setDuration(FLIP_DURATION_OUT), 
                mScrollView, View.INVISIBLE));
        mSettingsButtonAnim = start(
            setVisibilityWhenDone(
                ObjectAnimator.ofFloat(mSettingsButton, View.ALPHA, 0f)
                    .setDuration(FLIP_DURATION),
                    mScrollView, View.INVISIBLE));
        mNotificationButton.setVisibility(View.VISIBLE);
        mNotificationButtonAnim = start(
            ObjectAnimator.ofFloat(mNotificationButton, View.ALPHA, 1f)
                .setDuration(FLIP_DURATION));
        mClearButtonAnim = start(
            setVisibilityWhenDone(
                ObjectAnimator.ofFloat(mClearButton, View.ALPHA, 0f)
                .setDuration(FLIP_DURATION),
                mClearButton, View.GONE));
        /// M: [SystemUI] Remove settings button to notification header @{.
        if (mHeaderSettingsButton != null) {
            mHeaderSettingsButtonAnim = start(
                    setVisibilityWhenDone(
                            ObjectAnimator.ofFloat(mHeaderSettingsButton, View.ALPHA, 1f)
                            .setDuration(FLIP_DURATION),
                            mHeaderSettingsButton, View.VISIBLE));
        }
        /// M: [SystemUI] Remove settings button to notification header @}.
        mNotificationPanel.postDelayed(new Runnable() {
            public void run() {
                updateCarrierLabelVisibility(false);
            }
        }, FLIP_DURATION - 150);
        /// M: [SystemUI] Support SimIndicator, hide SimIndicator when settings panel is visible.
        mToolBarSwitchPanel.setVisibility(View.GONE);
    }

    public void flipPanels() {
        if (mHasFlipSettings) {
            if (mFlipSettingsView.getVisibility() != View.VISIBLE) {
                flipToSettings();
            } else {
                flipToNotifications();
            }
        }
    }

    public void animateCollapseQuickSettings() {
        mStatusBarView.collapseAllPanels(true);
    }

    void makeExpandedInvisibleSoon() {
        mHandler.postDelayed(new Runnable() { public void run() { makeExpandedInvisible(); }}, 50);
    }

    void makeExpandedInvisible() {
        if (SPEW) Slog.d(TAG, "makeExpandedInvisible: mExpandedVisible=" + mExpandedVisible
                + " mExpandedVisible=" + mExpandedVisible);

        if (!mExpandedVisible) {
            return;
        }

        // Ensure the panel is fully collapsed (just in case; bug 6765842, 7260868)
        mStatusBarView.collapseAllPanels(/*animate=*/ false);

        if (mHasFlipSettings) {
            // reset things to their proper state
            if (mFlipSettingsViewAnim != null) mFlipSettingsViewAnim.cancel();
            if (mScrollViewAnim != null) mScrollViewAnim.cancel();
            if (mSettingsButtonAnim != null) mSettingsButtonAnim.cancel();
            if (mNotificationButtonAnim != null) mNotificationButtonAnim.cancel();
            if (mClearButtonAnim != null) mClearButtonAnim.cancel();

            mScrollView.setScaleX(1f);
            mScrollView.setVisibility(View.VISIBLE);
            mSettingsButton.setAlpha(1f);
            mSettingsButton.setVisibility(View.VISIBLE);
            mNotificationPanel.setVisibility(View.GONE);
            mFlipSettingsView.setVisibility(View.GONE);
            mNotificationButton.setVisibility(View.GONE);
            /// M: [SystemUI] Support SimIndicator, show SimIndicator when notification panel is visible. @{
            if (mToolBarView.mSimSwitchPanelView.isPanelShowing()) {
                mToolBarSwitchPanel.setVisibility(View.VISIBLE);
            }
            /// M: [SystemUI] Support SimIndicator, show SimIndicator when notification panel is visible. @}
            /// M: [SystemUI] Remove settings button to notification header @{.
            if (mHeaderSettingsButton != null) {
                mHeaderSettingsButton.setVisibility(View.GONE);
            }
            /// M: [SystemUI] Remove settings button to notification header @}.
            setAreThereNotifications(); // show the clear button
        }

        mExpandedVisible = false;
        mPile.setLayoutTransitionsEnabled(false);
        if (mNavigationBarView != null)
            mNavigationBarView.setSlippery(false);
        visibilityChanged(false);

        // Shrink the window to the size of the status bar only
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) mStatusBarWindow.getLayoutParams();
        lp.height = getStatusBarHeight();
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        mWindowManager.updateViewLayout(mStatusBarWindow, lp);

        if ((mDisabled & StatusBarManager.DISABLE_NOTIFICATION_ICONS) == 0) {
            setNotificationIconVisibility(true, com.android.internal.R.anim.fade_in);
        }

        /// M: [SystemUI] Support "Notification toolbar". {
        mToolBarView.dismissDialogs();
        if (mQS != null) {
            mQS.dismissDialogs();
        }
        /// M: [SystemUI] Support "Notification toolbar". }

        /// M: [SystemUI] Dismiss application guide dialog.@{
        if (mAppGuideDialog != null && mAppGuideDialog.isShowing()) {
            mAppGuideDialog.dismiss();
            Xlog.d(TAG, "performCollapse dismiss mAppGuideDialog");
        }
        /// M: [SystemUI] Dismiss application guide dialog.@}

        // Close any "App info" popups that might have snuck on-screen
        dismissPopups();

        if (mPostCollapseCleanup != null) {
            mPostCollapseCleanup.run();
            mPostCollapseCleanup = null;
        }
    }

    /**
     * Enables or disables layers on the children of the notifications pile.
     * 
     * When layers are enabled, this method attempts to enable layers for the minimal
     * number of children. Only children visible when the notification area is fully
     * expanded will receive a layer. The technique used in this method might cause
     * more children than necessary to get a layer (at most one extra child with the
     * current UI.)
     * 
     * @param layerType {@link View#LAYER_TYPE_NONE} or {@link View#LAYER_TYPE_HARDWARE}
     */
    private void setPileLayers(int layerType) {
        final int count = mPile.getChildCount();

        switch (layerType) {
            case View.LAYER_TYPE_NONE:
                for (int i = 0; i < count; i++) {
                    mPile.getChildAt(i).setLayerType(layerType, null);
                }
                break;
            case View.LAYER_TYPE_HARDWARE:
                final int[] location = new int[2]; 
                mNotificationPanel.getLocationInWindow(location);

                final int left = location[0];
                final int top = location[1];
                final int right = left + mNotificationPanel.getWidth();
                final int bottom = top + getExpandedViewMaxHeight();

                final Rect childBounds = new Rect();

                for (int i = 0; i < count; i++) {
                    final View view = mPile.getChildAt(i);
                    view.getLocationInWindow(location);

                    childBounds.set(location[0], location[1],
                            location[0] + view.getWidth(), location[1] + view.getHeight());

                    if (childBounds.intersects(left, top, right, bottom)) {
                        view.setLayerType(layerType, null);
                    }
                }

                break;
        }
    }

    public boolean isClinging() {
        return mCling != null && mCling.getVisibility() == View.VISIBLE;
    }

    public void hideCling() {
        if (isClinging()) {
            mCling.animate().alpha(0f).setDuration(250).start();
            mCling.setVisibility(View.GONE);
            mSuppressStatusBarDrags = false;
        }
    }

    public void showCling() {
        // lazily inflate this to accommodate orientation change
        final ViewStub stub = (ViewStub) mStatusBarWindow.findViewById(R.id.status_bar_cling_stub);
        if (stub == null) {
            mClingShown = true;
            return; // no clings on this device
        }

        mSuppressStatusBarDrags = true;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCling = (ViewGroup) stub.inflate();

                mCling.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return true; // e eats everything
                    }});
                mCling.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideCling();
                    }});

                mCling.setAlpha(0f);
                mCling.setVisibility(View.VISIBLE);
                mCling.animate().alpha(1f);

                mClingShown = true;
                SharedPreferences.Editor editor = Prefs.edit(mContext);
                editor.putBoolean(Prefs.SHOWN_QUICK_SETTINGS_HELP, true);
                editor.apply();

                makeExpandedVisible(true); // enforce visibility in case the shade is still animating closed
                animateExpandNotificationsPanel();

                mSuppressStatusBarDrags = false;
            }
        }, 500);

        animateExpandNotificationsPanel();
    }

    public boolean interceptTouchEvent(MotionEvent event) {
        if (SPEW) {
            Slog.d(TAG, "Touch: rawY=" + event.getRawY() + " event=" + event + " mDisabled="
                + mDisabled + " mTracking=" + mTracking);
        } else if (CHATTY) {
            if (event.getAction() != MotionEvent.ACTION_MOVE) {
                Slog.d(TAG, String.format(
                            "panel: %s at (%f, %f) mDisabled=0x%08x",
                            MotionEvent.actionToString(event.getAction()),
                            event.getRawX(), event.getRawY(), mDisabled));
            }
        }

        if (DEBUG_GESTURES) {
            mGestureRec.add(event);
        }

        // Cling (first-run help) handling.
        // The cling is supposed to show the first time you drag, or even tap, the status bar.
        // It should show the notification panel, then fade in after half a second, giving you 
        // an explanation of what just happened, as well as teach you how to access quick
        // settings (another drag). The user can dismiss the cling by clicking OK or by 
        // dragging quick settings into view.
        final int act = event.getActionMasked();
        if (mSuppressStatusBarDrags) {
            return true;
        } else if (act == MotionEvent.ACTION_UP && !mClingShown) {
            showCling();
        } else {
            hideCling();
        }

        return false;
    }

    public GestureRecorder getGestureRecorder() {
        return mGestureRec;
    }

    @Override // CommandQueue
    public void setNavigationIconHints(int hints) {
        if (hints == mNavigationIconHints) return;

        mNavigationIconHints = hints;

        if (mNavigationBarView != null) {
            mNavigationBarView.setNavigationIconHints(hints);
        }
    }

    @Override // CommandQueue
    public void setSystemUiVisibility(int vis, int mask) {
        final int oldVal = mSystemUiVisibility;
        final int newVal = (oldVal&~mask) | (vis&mask);
        final int diff = newVal ^ oldVal;

        if (diff != 0) {
            mSystemUiVisibility = newVal;

            if (0 != (diff & View.SYSTEM_UI_FLAG_LOW_PROFILE)) {
                final boolean lightsOut = (0 != (vis & View.SYSTEM_UI_FLAG_LOW_PROFILE));
                if (lightsOut) {
                    animateCollapsePanels();
                    if (mTicking) {
                        haltTicker();
                    }
                }

                if (mNavigationBarView != null) {
                    mNavigationBarView.setLowProfile(lightsOut);
                }

                setStatusBarLowProfile(lightsOut);
            }

            notifyUiVisibilityChanged();
        }
    }

    private void setStatusBarLowProfile(boolean lightsOut) {
        if (mLightsOutAnimation == null) {
            final View notifications = mStatusBarView.findViewById(R.id.notification_icon_area);
            final View systemIcons = mStatusBarView.findViewById(R.id.statusIcons);
            final View signal = mStatusBarView.findViewById(R.id.signal_cluster);
            final View battery = mStatusBarView.findViewById(R.id.battery);
            final View clock = mStatusBarView.findViewById(R.id.clock);

            final AnimatorSet lightsOutAnim = new AnimatorSet();
            lightsOutAnim.playTogether(
                    ObjectAnimator.ofFloat(notifications, View.ALPHA, 0),
                    ObjectAnimator.ofFloat(systemIcons, View.ALPHA, 0),
                    ObjectAnimator.ofFloat(signal, View.ALPHA, 0),
                    ObjectAnimator.ofFloat(battery, View.ALPHA, 0.5f),
                    ObjectAnimator.ofFloat(clock, View.ALPHA, 0.5f)
                );
            lightsOutAnim.setDuration(750);

            final AnimatorSet lightsOnAnim = new AnimatorSet();
            lightsOnAnim.playTogether(
                    ObjectAnimator.ofFloat(notifications, View.ALPHA, 1),
                    ObjectAnimator.ofFloat(systemIcons, View.ALPHA, 1),
                    ObjectAnimator.ofFloat(signal, View.ALPHA, 1),
                    ObjectAnimator.ofFloat(battery, View.ALPHA, 1),
                    ObjectAnimator.ofFloat(clock, View.ALPHA, 1)
                );
            lightsOnAnim.setDuration(250);

            mLightsOutAnimation = lightsOutAnim;
            mLightsOnAnimation = lightsOnAnim;
        }

        mLightsOutAnimation.cancel();
        mLightsOnAnimation.cancel();

        final Animator a = lightsOut ? mLightsOutAnimation : mLightsOnAnimation;
        a.start();

        setAreThereNotifications();
    }

    private boolean areLightsOn() {
        return 0 == (mSystemUiVisibility & View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    public void setLightsOn(boolean on) {
        Log.v(TAG, "setLightsOn(" + on + ")");
        if (on) {
            setSystemUiVisibility(0, View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE, View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    private void notifyUiVisibilityChanged() {
        try {
            mWindowManagerService.statusBarVisibilityChanged(mSystemUiVisibility);
        } catch (RemoteException ex) {
        }
    }

    public void topAppWindowChanged(boolean showMenu) {
        if (DEBUG) {
            Slog.d(TAG, (showMenu?"showing":"hiding") + " the MENU button");
        }
        if (mNavigationBarView != null) {
            mNavigationBarView.setMenuVisibility(showMenu);
        }

        // See above re: lights-out policy for legacy apps.
        if (showMenu) setLightsOn(true);
    }

    @Override
    public void setImeWindowStatus(IBinder token, int vis, int backDisposition) {
        boolean altBack = (backDisposition == InputMethodService.BACK_DISPOSITION_WILL_DISMISS)
            || ((vis & InputMethodService.IME_VISIBLE) != 0);

        mCommandQueue.setNavigationIconHints(
                altBack ? (mNavigationIconHints | StatusBarManager.NAVIGATION_HINT_BACK_ALT)
                        : (mNavigationIconHints & ~StatusBarManager.NAVIGATION_HINT_BACK_ALT));
        if (mQS != null) mQS.setImeWindowStatus(vis > 0);
    }

    @Override
    public void setHardKeyboardStatus(boolean available, boolean enabled) {}

    @Override
    protected void tick(IBinder key, StatusBarNotification n, boolean firstTime) {
        // no ticking in lights-out mode
        if (!areLightsOn()) return;

        // no ticking in Setup
        if (!isDeviceProvisioned()) return;

        // not for you
        if (!notificationIsForCurrentUser(n)) return;

        // Show the ticker if one is requested. Also don't do this
        // until status bar window is attached to the window manager,
        // because...  well, what's the point otherwise?  And trying to
        // run a ticker without being attached will crash!
        if (n.notification.tickerText != null && mStatusBarWindow.getWindowToken() != null) {
            if (0 == (mDisabled & (StatusBarManager.DISABLE_NOTIFICATION_ICONS
                            | StatusBarManager.DISABLE_NOTIFICATION_TICKER))) {
                mTicker.addEntry(n);
            }
        }
    }

    private class MyTicker extends Ticker {
        MyTicker(Context context, View sb) {
            super(context, sb);
        }

        @Override
        public void tickerStarting() {
            mTicking = true;
            mStatusBarContents.setVisibility(View.GONE);
            mTickerView.setVisibility(View.VISIBLE);
            mTickerView.startAnimation(loadAnim(com.android.internal.R.anim.push_up_in, null));
            mStatusBarContents.startAnimation(loadAnim(com.android.internal.R.anim.push_up_out, null));
        }

        @Override
        public void tickerDone() {
            mStatusBarContents.setVisibility(View.VISIBLE);
            mTickerView.setVisibility(View.GONE);
            mStatusBarContents.startAnimation(loadAnim(com.android.internal.R.anim.push_down_in, null));
            mTickerView.startAnimation(loadAnim(com.android.internal.R.anim.push_down_out,
                        mTickingDoneListener));
        }

        public void tickerHalting() {
            mStatusBarContents.setVisibility(View.VISIBLE);
            mTickerView.setVisibility(View.GONE);
            mStatusBarContents.startAnimation(loadAnim(com.android.internal.R.anim.fade_in, null));
            // we do not animate the ticker away at this point, just get rid of it (b/6992707)
        }
    }

    Animation.AnimationListener mTickingDoneListener = new Animation.AnimationListener() {;
        public void onAnimationEnd(Animation animation) {
            mTicking = false;
        }
        public void onAnimationRepeat(Animation animation) {
        }
        public void onAnimationStart(Animation animation) {
        }
    };

    private Animation loadAnim(int id, Animation.AnimationListener listener) {
        Animation anim = AnimationUtils.loadAnimation(mContext, id);
        if (listener != null) {
            anim.setAnimationListener(listener);
        }
        return anim;
    }

    public static String viewInfo(View v) {
        return "[(" + v.getLeft() + "," + v.getTop() + ")(" + v.getRight() + "," + v.getBottom()
                + ") " + v.getWidth() + "x" + v.getHeight() + "]";
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (mQueueLock) {
            pw.println("Current Status Bar state:");
            pw.println("  mExpandedVisible=" + mExpandedVisible
                    + ", mTrackingPosition=" + mTrackingPosition);
            pw.println("  mTicking=" + mTicking);
            pw.println("  mTracking=" + mTracking);
            pw.println("  mNotificationPanel=" + 
                    ((mNotificationPanel == null) 
                            ? "null" 
                            : (mNotificationPanel + " params=" + mNotificationPanel.getLayoutParams().debug(""))));
            pw.println("  mAnimating=" + mAnimating
                    + ", mAnimY=" + mAnimY + ", mAnimVel=" + mAnimVel
                    + ", mAnimAccel=" + mAnimAccel);
            pw.println("  mAnimLastTimeNanos=" + mAnimLastTimeNanos);
            pw.println("  mAnimatingReveal=" + mAnimatingReveal
                    + " mViewDelta=" + mViewDelta);
            pw.println("  mDisplayMetrics=" + mDisplayMetrics);
            pw.println("  mPile: " + viewInfo(mPile));
            pw.println("  mTickerView: " + viewInfo(mTickerView));
            pw.println("  mScrollView: " + viewInfo(mScrollView)
                    + " scroll " + mScrollView.getScrollX() + "," + mScrollView.getScrollY());
        }

        pw.print("  mNavigationBarView=");
        if (mNavigationBarView == null) {
            pw.println("null");
        } else {
            mNavigationBarView.dump(fd, pw, args);
        }

        if (DUMPTRUCK) {
            synchronized (mNotificationData) {
                int N = mNotificationData.size();
                pw.println("  notification icons: " + N);
                for (int i=0; i<N; i++) {
                    NotificationData.Entry e = mNotificationData.get(i);
                    pw.println("    [" + i + "] key=" + e.key + " icon=" + e.icon);
                    StatusBarNotification n = e.notification;
                    pw.println("         pkg=" + n.pkg + " id=" + n.id + " score=" + n.score);
                    pw.println("         notification=" + n.notification);
                    pw.println("         tickerText=\"" + n.notification.tickerText + "\"");
                }
            }

            int N = mStatusIcons.getChildCount();
            pw.println("  system icons: " + N);
            for (int i=0; i<N; i++) {
                StatusBarIconView ic = (StatusBarIconView) mStatusIcons.getChildAt(i);
                pw.println("    [" + i + "] icon=" + ic);
            }

            if (false) {
                pw.println("see the logcat for a dump of the views we have created.");
                // must happen on ui thread
                mHandler.post(new Runnable() {
                        public void run() {
                            mStatusBarView.getLocationOnScreen(mAbsPos);
                            Slog.d(TAG, "mStatusBarView: ----- (" + mAbsPos[0] + "," + mAbsPos[1]
                                    + ") " + mStatusBarView.getWidth() + "x"
                                    + getStatusBarHeight());
                            mStatusBarView.debug();
                        }
                    });
            }
        }

        if (DEBUG_GESTURES) {
            pw.print("  status bar gestures: ");
            mGestureRec.dump(fd, pw, args);
        }

        /// M: [SystemUI] Support "Dual SIM". {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mNetworkControllerGemini.dump(fd, pw, args);
        } else {
            mNetworkController.dump(fd, pw, args);
        }
        /// M: [SystemUI] Support "Dual SIM". }
    }

    @Override
    public void createAndAddWindows() {
        addStatusBarWindow();
    }

    private void addStatusBarWindow() {
        // Put up the view
        final int height = getStatusBarHeight();

        // Now that the status bar window encompasses the sliding panel and its
        // translucent backdrop, the entire thing is made TRANSLUCENT and is
        // hardware-accelerated.
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
                WindowManager.LayoutParams.TYPE_STATUS_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);

        lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        lp.gravity = getStatusBarGravity();
        lp.setTitle("StatusBar");
        lp.packageName = mContext.getPackageName();

        makeStatusBarView();
        mWindowManager.addView(mStatusBarWindow, lp);
    }

    void setNotificationIconVisibility(boolean visible, int anim) {
        int old = mNotificationIcons.getVisibility();
        int v = visible ? View.VISIBLE : View.INVISIBLE;
        if (old != v) {
            mNotificationIcons.setVisibility(v);
            mNotificationIcons.startAnimation(loadAnim(anim, null));
        }
    }

    void updateExpandedInvisiblePosition() {
        mTrackingPosition = -mDisplayMetrics.heightPixels;
    }

    static final float saturate(float a) {
        return a < 0f ? 0f : (a > 1f ? 1f : a);
    }

    @Override
    protected int getExpandedViewMaxHeight() {
        return mDisplayMetrics.heightPixels - mNotificationPanelMarginBottomPx;
    }

    @Override
    public void updateExpandedViewPos(int thingy) {
        if (DEBUG) Slog.v(TAG, "updateExpandedViewPos");

        // on larger devices, the notification panel is propped open a bit
        mNotificationPanel.setMinimumHeight(
                (int)(mNotificationPanelMinHeightFrac * mCurrentDisplaySize.y));

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mNotificationPanel.getLayoutParams();
        lp.gravity = mNotificationPanelGravity;
        lp.leftMargin = mNotificationPanelMarginPx;
        mNotificationPanel.setLayoutParams(lp);

        if (mSettingsPanel != null) {
            lp = (FrameLayout.LayoutParams) mSettingsPanel.getLayoutParams();
            lp.gravity = mSettingsPanelGravity;
            lp.rightMargin = mNotificationPanelMarginPx;
            mSettingsPanel.setLayoutParams(lp);
        }

        updateCarrierLabelVisibility(false);
    }

    // called by makeStatusbar and also by PhoneStatusBarView
    void updateDisplaySize() {
        mDisplay.getMetrics(mDisplayMetrics);
        if (DEBUG_GESTURES) {
            mGestureRec.tag("display", 
                    String.format("%dx%d", mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels));
        }
    }

    private View.OnClickListener mClearButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            synchronized (mNotificationData) {
                // animate-swipe all dismissable notifications, then animate the shade closed
                int numChildren = mPile.getChildCount();

                int scrollTop = mScrollView.getScrollY();
                int scrollBottom = scrollTop + mScrollView.getHeight();
                final ArrayList<View> snapshot = new ArrayList<View>(numChildren);
                for (int i=0; i<numChildren; i++) {
                    final View child = mPile.getChildAt(i);
                    if (mPile.canChildBeDismissed(child) && child.getBottom() > scrollTop &&
                            child.getTop() < scrollBottom) {
                        snapshot.add(child);
                    }
                }
                if (snapshot.isEmpty()) {
                    animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Decrease the delay for every row we animate to give the sense of
                        // accelerating the swipes
                        final int ROW_DELAY_DECREMENT = 10;
                        int currentDelay = 140;
                        int totalDelay = 0;

                        // Set the shade-animating state to avoid doing other work during
                        // all of these animations. In particular, avoid layout and
                        // redrawing when collapsing the shade.
                        mPile.setViewRemoval(false);

                        mPostCollapseCleanup = new Runnable() {
                            @Override
                            public void run() {
                                if (DEBUG) {
                                    Slog.v(TAG, "running post-collapse cleanup");
                                }
                                try {
                                    mPile.setViewRemoval(true);
                                    mBarService.onClearAllNotifications();
                                } catch (Exception ex) { }
                            }
                        };

                        View sampleView = snapshot.get(0);
                        int width = sampleView.getWidth();
                        final int velocity = width * 8; // 1000/8 = 125 ms duration
                        for (final View _v : snapshot) {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mPile.dismissRowAnimated(_v, velocity);
                                }
                            }, totalDelay);
                            currentDelay = Math.max(50, currentDelay - ROW_DELAY_DECREMENT);
                            totalDelay += currentDelay;
                        }
                        // Delay the collapse animation until after all swipe animations have
                        // finished. Provide some buffer because there may be some extra delay
                        // before actually starting each swipe animation. Ideally, we'd
                        // synchronize the end of those animations with the start of the collaps
                        // exactly.
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
                            }
                        }, totalDelay + 225);
                    }
                }).start();
                /// M: [SystemUI] Dismiss new event icon when click clear button for keyguard.@{
                Intent intent = new Intent(CLEAR_NEW_EVENT_VIEW_INTENT);
                mContext.sendBroadcast(intent);
                /// M: [SystemUI] Dismiss new event icon when click clear button for keyguard.@}
            }
        }
    };

    public void startActivityDismissingKeyguard(Intent intent, boolean onlyProvisioned) {
        if (onlyProvisioned && !isDeviceProvisioned()) return;
        try {
            // Dismiss the lock screen when Settings starts.
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        animateCollapsePanels();
    }

    private View.OnClickListener mSettingsButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mHasSettingsPanel) {
                animateExpandSettingsPanel();
            } else {
                startActivityDismissingKeyguard(
                        new Intent(android.provider.Settings.ACTION_SETTINGS), true);
            }
        }
    };

    /// M: [SystemUI] Remove settings button to notification header @{.
    private View.OnClickListener mHeaderSettingsButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
                startActivityDismissingKeyguard(new Intent(android.provider.Settings.ACTION_SETTINGS), true);
        }
    };
    /// M: [SystemUI] Remove settings button to notification header @}.

    private View.OnClickListener mClockClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            startActivityDismissingKeyguard(
                    new Intent(Intent.ACTION_QUICK_CLOCK), true); // have fun, everyone
        }
    };

    private View.OnClickListener mNotificationButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            animateExpandNotificationsPanel();
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Slog.v(TAG, "onReceive: " + intent);
            String action = intent.getAction();
            Xlog.d(TAG, "onReceive, action=" + action);
            /// M: ALPS00349274 to hide navigation bar when ipo shut down to avoid it flash when in boot ipo mode.{
            if ("android.intent.action.ACTION_BOOT_IPO".equals(action)) {
                if (mNavigationBarView != null) {
                    View view = mNavigationBarView.findViewById(R.id.rot0);
                    if (view != null && view.getVisibility() != View.GONE) {
                        Xlog.d(TAG, "receive android.intent.action.ACTION_BOOT_IPO to set mNavigationBarView visible");
                        view.setVisibility(View.VISIBLE);
                    }
                }
            } else if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                if (mNavigationBarView != null) {
                    Xlog.d(TAG, "receive android.intent.action.ACTION_SHUTDOWN_IPO to set mNavigationBarView invisible");
                    mNavigationBarView.hideForIPOShutdown();
                }              
            /// M: ALPS00349274 to hide navigation bar when ipo shut down to avoid it flash when in boot ipo mode.}
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                int flags = CommandQueue.FLAG_EXCLUDE_NONE;
                if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                    String reason = intent.getStringExtra("reason");
                    if (reason != null && reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
                        flags |= CommandQueue.FLAG_EXCLUDE_RECENTS_PANEL;
                    }
                }
                animateCollapsePanels(flags);
            /// M: [SystemUI] Support "ThemeManager" @{
            } else if (Intent.ACTION_SKIN_CHANGED.equals(action)) {
                refreshApplicationGuide();
                refreshExpandedView(context);
                if (mNavigationBarView != null) {
                    mNavigationBarView.upDateResources();
                }
                repositionNavigationBar();
                updateResources();
            /// M: [SystemUI] Support "Theme management". @}
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // no waiting!
                /// M: [SystemUI]Show application guide for App.
                if (mAppGuideDialog != null && mAppGuideDialog.isShowing()) {
                    mAppGuideDialog.dismiss();
                    Xlog.d(TAG, "mAppGuideDialog.dismiss()");
                }
                /// M: [SystemUI]Show application guide for App. @}
                makeExpandedInvisible();
                notifyNavigationBarScreenOn(false);
            } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                if (DEBUG) {
                    Slog.v(TAG, "configuration changed: " + mContext.getResources().getConfiguration());
                }
                /// M: [SystemUI]Show application guide for App.
                refreshApplicationGuide();
                Configuration currentConfig = context.getResources().getConfiguration();
                /// M: [ALPS00336833] When orientation changed, request layout to avoid status bar layout error. @{
                if (currentConfig.orientation != mPrevioutConfigOrientation) {
                    mNeedRelayout = true;
                    mPrevioutConfigOrientation = currentConfig.orientation;
                }
                /// M: [ALPS00336833] When orientation changed, request layout to avoid status bar layout error. @}
                mDisplay.getSize(mCurrentDisplaySize);

                updateResources();
                repositionNavigationBar();
                updateExpandedViewPos(EXPANDED_LEAVE_ALONE);
                updateShowSearchHoldoff();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                // work around problem where mDisplay.getRotation() is not stable while screen is off (bug 7086018)
                repositionNavigationBar();
                notifyNavigationBarScreenOn(true);
            /// M: [SystemUI] Support "Dual SIM PLMN Change". @{
            } else if (Telephony.Intents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                if (mShowCarrierInPanel) {
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        final int tempSimId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, PhoneConstants.GEMINI_SIM_1);
                        /// M: Support GeminiPlus
                        for (int childIdx = 0; childIdx < mCarrierLabelGemini.getChildCount(); childIdx++) {
                            final View mChildView = mCarrierLabelGemini.getChildAt(childIdx);
                            if(mChildView instanceof CarrierLabelGemini) {
                                CarrierLabelGemini mChildCarrier = (CarrierLabelGemini) mChildView;
                                if (tempSimId == mChildCarrier.getSlotId()) {
                                    mChildCarrier.updateNetworkName(intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
                                    intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
                                    intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
                                    intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
                                }
                            }
                        }
                    } else {
                        ((CarrierLabel)mCarrierLabel).updateNetworkName(
                                intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
                                intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
                                intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
                                intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
                        /// M: For AT&T
                        if (!PluginFactory.getStatusBarPlugin(context)
                                .isHspaDataDistinguishable() &&
                                !PluginFactory.getStatusBarPlugin(context)
                                .supportDataTypeAlwaysDisplayWhileOn()) {
                            updateNetworkName(
                                    intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
                                    intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
                                    intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
                                    intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
                        }
                    }
                }
            }
            /// M: [SystemUI] Support "Dual SIM PLMN Change". }@
        }
    };

    /// M: For AT&T @{
    private String mOldPlmn = null;
    private void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        Xlog.d(TAG, "For AT&T updateNetworkName, showSpn=" + showSpn + " spn=" + spn + " showPlmn=" + showPlmn + " plmn=" + plmn);
        StringBuilder str = new StringBuilder();
        boolean something = false;
        if (showPlmn && plmn != null) {
            str.append(plmn);
            something = true;
        }
        if (showSpn && spn != null) {
            if (something) {
                str.append(mContext.getString(R.string.status_bar_network_name_separator));
            }
            str.append(spn);
            something = true;
        }
        if (something) {
            mOldPlmn = str.toString();
        } else {
            mOldPlmn = mContext.getResources().getString(com.android.internal.R.string.lockscreen_carrier_default);
        }
    }
    
    private void updatePLMNSearchingStateView(boolean searching) {
        if(searching) {
            mPlmnLabel.setText(R.string.plmn_searching);
        } else {
            mPlmnLabel.setText(mOldPlmn);
        }
        mPlmnLabel.setVisibility(View.VISIBLE);
    }
    /// M: For AT&T.}@
    @Override
    public void userSwitched(int newUserId) {
        if (MULTIUSER_DEBUG) mNotificationPanelDebugText.setText("USER " + newUserId);
        animateCollapsePanels();
        updateNotificationIcons();
        resetUserSetupObserver();
    }

    private void resetUserSetupObserver() {
        mContext.getContentResolver().unregisterContentObserver(mUserSetupObserver);
        mUserSetupObserver.onChange(false);
        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.USER_SETUP_COMPLETE), true,
                mUserSetupObserver,
                mCurrentUserId);
    }

    private void setIntruderAlertVisibility(boolean vis) {
        if (!ENABLE_INTRUDERS) return;
        if (DEBUG) {
            Slog.v(TAG, (vis ? "showing" : "hiding") + " intruder alert window");
        }
        mIntruderAlertView.setVisibility(vis ? View.VISIBLE : View.GONE);
    }

    public void dismissIntruder() {
        if (mCurrentlyIntrudingNotification == null) return;

        try {
            mBarService.onNotificationClear(
                    mCurrentlyIntrudingNotification.pkg,
                    mCurrentlyIntrudingNotification.tag,
                    mCurrentlyIntrudingNotification.id);
        } catch (android.os.RemoteException ex) {
            // oh well
        }
    }

    /**
     * Reload some of our resources when the configuration changes.
     *
     * We don't reload everything when the configuration changes -- we probably
     * should, but getting that smooth is tough.  Someday we'll fix that.  In the
     * meantime, just update the things that we know change.
     */
    void updateResources() {
        Xlog.d(TAG, "updateResources");

        final Context context = mContext;
        final Resources res = context.getResources();

        if (mClearButton instanceof TextView) {
            ((TextView)mClearButton).setText(context.getText(R.string.status_bar_clear_all_button));
        }
        /// M: [SystemUI] Support "Notification toolbar". {
        mToolBarView.updateResources();
        /// M: [SystemUI] Support "Notification toolbar". }

        // Update the QuickSettings container
        if (mQS != null) mQS.updateResources();

        loadDimens();
    }

    protected void loadDimens() {
        final Resources res = mContext.getResources();

        mNaturalBarHeight = res.getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height);

        int newIconSize = res.getDimensionPixelSize(
            com.android.internal.R.dimen.status_bar_icon_size);
        int newIconHPadding = res.getDimensionPixelSize(
            R.dimen.status_bar_icon_padding);

        if (newIconHPadding != mIconHPadding || newIconSize != mIconSize) {
//            Slog.d(TAG, "size=" + newIconSize + " padding=" + newIconHPadding);
            mIconHPadding = newIconHPadding;
            mIconSize = newIconSize;
            //reloadAllNotificationIcons(); // reload the tray
        }

        mEdgeBorder = res.getDimensionPixelSize(R.dimen.status_bar_edge_ignore);

        mSelfExpandVelocityPx = res.getDimension(R.dimen.self_expand_velocity);
        mSelfCollapseVelocityPx = res.getDimension(R.dimen.self_collapse_velocity);
        mFlingExpandMinVelocityPx = res.getDimension(R.dimen.fling_expand_min_velocity);
        mFlingCollapseMinVelocityPx = res.getDimension(R.dimen.fling_collapse_min_velocity);

        mCollapseMinDisplayFraction = res.getFraction(R.dimen.collapse_min_display_fraction, 1, 1);
        mExpandMinDisplayFraction = res.getFraction(R.dimen.expand_min_display_fraction, 1, 1);

        mExpandAccelPx = res.getDimension(R.dimen.expand_accel);
        mCollapseAccelPx = res.getDimension(R.dimen.collapse_accel);

        mFlingGestureMaxXVelocityPx = res.getDimension(R.dimen.fling_gesture_max_x_velocity);

        mFlingGestureMaxOutputVelocityPx = res.getDimension(R.dimen.fling_gesture_max_output_velocity);

        mNotificationPanelMarginBottomPx
            = (int) res.getDimension(R.dimen.notification_panel_margin_bottom);
        mNotificationPanelMarginPx
            = (int) res.getDimension(R.dimen.notification_panel_margin_left);
        mNotificationPanelGravity = res.getInteger(R.integer.notification_panel_layout_gravity);
        if (mNotificationPanelGravity <= 0) {
            mNotificationPanelGravity = Gravity.LEFT | Gravity.TOP;
        }
        mSettingsPanelGravity = res.getInteger(R.integer.settings_panel_layout_gravity);
        if (mSettingsPanelGravity <= 0) {
            mSettingsPanelGravity = Gravity.RIGHT | Gravity.TOP;
        }

        mCarrierLabelHeight = res.getDimensionPixelSize(R.dimen.carrier_label_height);
        mNotificationHeaderHeight = res.getDimensionPixelSize(R.dimen.notification_panel_header_height);
        /// M: Calculate ToolBar height when sim indicator is showing.
        mToolBarViewHeight = res.getDimensionPixelSize(R.dimen.toolbar_height);

        mNotificationPanelMinHeightFrac = res.getFraction(R.dimen.notification_panel_min_height_frac, 1, 1);
        if (mNotificationPanelMinHeightFrac < 0f || mNotificationPanelMinHeightFrac > 1f) {
            mNotificationPanelMinHeightFrac = 0f;
        }

        if (false) Slog.v(TAG, "updateResources");
    }

    //
    // tracing
    //

    void postStartTracing() {
        mHandler.postDelayed(mStartTracing, 3000);
    }

    void vibrate() {
        android.os.Vibrator vib = (android.os.Vibrator)mContext.getSystemService(
                Context.VIBRATOR_SERVICE);
        vib.vibrate(250);
    }

    Runnable mStartTracing = new Runnable() {
        public void run() {
            vibrate();
            SystemClock.sleep(250);
            Slog.d(TAG, "startTracing");
            android.os.Debug.startMethodTracing("/data/statusbar-traces/trace");
            mHandler.postDelayed(mStopTracing, 10000);
        }
    };

    Runnable mStopTracing = new Runnable() {
        public void run() {
            android.os.Debug.stopMethodTracing();
            Slog.d(TAG, "stopTracing");
            vibrate();
        }
    };

    @Override
    protected void haltTicker() {
        mTicker.halt();
    }

    @Override
    protected boolean shouldDisableNavbarGestures() {
        return !isDeviceProvisioned()
                || mExpandedVisible
                || (mDisabled & StatusBarManager.DISABLE_SEARCH) != 0;
    }

    private static class FastColorDrawable extends Drawable {
        private final int mColor;

        public FastColorDrawable(int color) {
            mColor = 0xff000000 | color;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawColor(mColor, PorterDuff.Mode.SRC);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
        }

        @Override
        public void setBounds(Rect bounds) {
        }
    }
    /// M: [SystemUI] Support "Dual SIM". @{

    private NetworkControllerGemini mNetworkControllerGemini;

    /// M: Support GeminiPlus
    private CarrierLabelGemini mCarrier1 = null;
    private CarrierLabelGemini mCarrier2 = null;
    private CarrierLabelGemini mCarrier3 = null;
    private CarrierLabelGemini mCarrier4 = null;
    private View mCarrierDivider = null;
    private View mCarrierDivider2 = null;
    private View mCarrierDivider3 = null;

    private LinearLayout mCarrierLabelGemini = null;

    private BroadcastReceiver mSIMInfoReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            Xlog.d(TAG, "onReceive, intent action is " + action + ".");
            if (action.equals(Intent.SIM_SETTINGS_INFO_CHANGED)) {
                mHandler.post(new Runnable() {
                    public void run() {
                        SIMHelper.updateSIMInfos(context);
                        int type = intent.getIntExtra("type", -1);
                        long simId = intent.getLongExtra("simid", -1);
                        if (type == 0 || type == 1) {
                            // name and color changed
                            updateNotificationsSimInfo(simId);
                        }
                        // update ToolBarView's panel views
                        mToolBarView.updateSimInfos(intent);
                        if (mQS != null) {
                            mQS.updateSimInfo(intent);
                        }
                    }
                });
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INSERTED_STATUS)
                    || action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                mHandler.post(new Runnable() {
                    public void run() {
                        SIMHelper.updateSIMInfos(context);
                    }
                });
                updateSimIndicator();
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                updateAirplaneMode();
            } else if (action.equals(ACTION_BOOT_IPO)) {
                if (mSimIndicatorIcon != null) {
                    mSimIndicatorIcon.setVisibility(View.GONE);
                }
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) { ///for AT&T
                int simStatus = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_STATE, -1);
                if (simStatus == PhoneConstants.SIM_INDICATOR_SEARCHING) {
                    Xlog.d(TAG, "updateSIMState. simStatus is " + simStatus);
                    updatePLMNSearchingStateView(true);
                } else {
                    updatePLMNSearchingStateView(false);
                }
            }
        }
    };

    private void updateNotificationsSimInfo(long simId) {
        Xlog.d(TAG, "updateNotificationsSimInfo, the simId is " + simId + ".");
        if (simId == -1) {
            return;
        }
        SimInfoManager.SimInfoRecord simInfo = SIMHelper.getSIMInfo(mContext, simId);
        if (simInfo == null) {
            Xlog.d(TAG, "updateNotificationsSimInfo, the simInfo is null.");
            return;
        }
        for (int i = 0, n = this.mNotificationData.size(); i < n; i++) {
            Entry entry = this.mNotificationData.get(i);
            updateNotificationSimInfo(simInfo, entry.notification.notification, entry.icon, entry.expanded);
        }
    }

    private void updateNotificationSimInfo(SimInfoManager.SimInfoRecord simInfo, Notification n, StatusBarIconView iconView, View itemView) {
        if (n.simId != simInfo.mSimInfoId) {
            return;
        }
        int simInfoType = n.simInfoType;
        if (iconView == null) { //for update SimIndicatorView
            for (int i=0; i<mNotificationIcons.getChildCount(); i++) {
                View child = mNotificationIcons.getChildAt(i);
                if (child instanceof StatusBarIconView) {
                    StatusBarIconView iconViewtemp = (StatusBarIconView) child;
                    if(iconViewtemp.getNotificationSimId() == n.simId){
                        iconView = iconViewtemp;
                        break;
                    }
                }
            }
        }        
        // icon part.
//        if ((simInfoType == 2 || simInfoType == 3) && simInfo != null && iconView != null) {
//            Xlog.d(TAG, "updateNotificationSimInfo, add sim info to status bar.");
//            Drawable drawable = iconView.getResources().getDrawable(simInfo.mSimBackgroundRes);
//           if (drawable != null) {
//                iconView.setSimInfoBackground(drawable);
//                iconView.invalidate();
//            }
//        }
        // item part.
        if ((simInfoType == 1 || simInfoType == 3) && simInfo != null && (simInfo.mColor >= 0 && simInfo.mColor < Telephony.SIMBackgroundRes.length)) {
            Xlog.d(TAG, "updateNotificationSimInfo, add sim info to notification item. simInfo.mColor = " + simInfo.mColor);
            View simIndicatorLayout = itemView.findViewById(com.android.internal.R.id.notification_sim_indicator);
            simIndicatorLayout.setVisibility(View.VISIBLE);
            ImageView bgView = (ImageView) itemView.findViewById(com.android.internal.R.id.notification_sim_indicator_bg);
            bgView.setBackground(mContext.getResources().getDrawable(TelephonyIcons.SIM_INDICATOR_BACKGROUND_NOTIFICATION[simInfo.mColor]));
            bgView.setVisibility(View.VISIBLE);
        } else {
            View simIndicatorLayout = itemView.findViewById(com.android.internal.R.id.notification_sim_indicator);
            simIndicatorLayout.setVisibility(View.VISIBLE);
            View bgView = itemView.findViewById(com.android.internal.R.id.notification_sim_indicator_bg);
            bgView.setVisibility(View.GONE);
        }
    }

    /// M: [SystemUI] Support "Dual SIM". @}

    /// M: [SystemUI] Support "Notification toolbar". @{
    private ToolBarView mToolBarView;
    private View mToolBarSwitchPanel;
    public boolean isExpanded() {
        return mExpandedVisible;
    }
    /// M: [SystemUI] Support "Notification toolbar". @}

    /// M: [SystemUI] Support "SIM indicator". @{

    private boolean mIsSimIndicatorShowing = false;
    private String mBusinessType = null;
    public void showSimIndicator(String businessType) {
        if (mIsSimIndicatorShowing) {
            hideSimIndicator();
        }
        mBusinessType = businessType;
        long simId = SIMHelper.getDefaultSIM(mContext, businessType);
        Xlog.d(TAG, "showSimIndicator, show SIM indicator which business is " + businessType + "  simId = "+simId+".");
        if (simId == android.provider.Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
            List<SimInfoManager.SimInfoRecord> simInfos = SIMHelper.getSIMInfoList(mContext);
            if (simInfos != null && simInfos.size() > 0) {
                showAlwaysAskOrInternetCall(simId);
                mToolBarView.showSimSwithPanel(businessType);
            }
        } else if (businessType.equals(android.provider.Settings.System.VOICE_CALL_SIM_SETTING)
                && simId == android.provider.Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
            showAlwaysAskOrInternetCall(simId);
            mToolBarView.showSimSwithPanel(businessType);
        } else if (simId == android.provider.Settings.System.SMS_SIM_SETTING_AUTO) {
            List<SimInfoManager.SimInfoRecord> simInfos = SIMHelper.getSIMInfoList(mContext);
            if (simInfos != null && simInfos.size() > 0) {
                showAlwaysAskOrInternetCall(simId);
                mToolBarView.showSimSwithPanel(businessType);
            }
        } else {
            mSimIndicatorIconShow = false;
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                List<SimInfoManager.SimInfoRecord> simInfos = SIMHelper.getSIMInfoList(mContext);
                if (simInfos == null) {
                    return;
                }
                int slot = 0;
                for (int i = 0; i < simInfos.size(); i++) {
                    if (simInfos.get(i).mSimInfoId == simId) {
                        slot = simInfos.get(i).mSimSlotId;
                        break;
                    }
                }
                if (simInfos.size() == 1) {
                    if (businessType.equals(android.provider.Settings.System.VOICE_CALL_SIM_SETTING)
                            && isInternetCallEnabled(mContext)) {
                        mNetworkControllerGemini.showSimIndicator(slot);
                        mToolBarView.showSimSwithPanel(businessType);
                    }
                } else if (simInfos.size() > 1) {
                    mNetworkControllerGemini.showSimIndicator(slot);
                    mToolBarView.showSimSwithPanel(businessType);
                }
            } else {
                List<SimInfoManager.SimInfoRecord> simInfos = SIMHelper.getSIMInfoList(mContext);
                if (simInfos == null) {
                    return;
                }
                if (businessType.equals(android.provider.Settings.System.VOICE_CALL_SIM_SETTING)
                        && isInternetCallEnabled(mContext) && simInfos.size() == 1) {
                    mNetworkController.showSimIndicator();
                    mToolBarView.showSimSwithPanel(businessType);
                }
            }
        }
        mIsSimIndicatorShowing = true;
    }

    public void hideSimIndicator() {
        Xlog.d(TAG, "hideSimIndicator SIM indicator.mBusinessType = " + mBusinessType);
        if (mBusinessType == null) return;
        long simId = SIMHelper.getDefaultSIM(mContext, mBusinessType);
        Xlog.d(TAG, "hideSimIndicator, hide SIM indicator simId = "+simId+".");
        mSimIndicatorIcon.setVisibility(View.GONE);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mNetworkControllerGemini.hideSimIndicator(PhoneConstants.GEMINI_SIM_1);
            mNetworkControllerGemini.hideSimIndicator(PhoneConstants.GEMINI_SIM_2);
            if(PhoneConstants.GEMINI_SIM_NUM == 3) {
                mNetworkControllerGemini.hideSimIndicator(PhoneConstants.GEMINI_SIM_3);
            }
            if(PhoneConstants.GEMINI_SIM_NUM == 4) {
                mNetworkControllerGemini.hideSimIndicator(PhoneConstants.GEMINI_SIM_4);
            }
        } else {
            mNetworkController.hideSimIndicator();
        }
        mToolBarView.hideSimSwithPanel();
        mIsSimIndicatorShowing = false;
        mSimIndicatorIconShow = false;
    }

    private boolean mAirplaneMode = false;
    private boolean mSimIndicatorIconShow = false;
    
    private void updateAirplaneMode() {
        mAirplaneMode = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
        if (mSimIndicatorIcon != null) {
            mSimIndicatorIcon.setVisibility(mSimIndicatorIconShow && !mAirplaneMode ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSimIndicator() {
        Xlog.d(TAG, "updateSimIndicator mIsSimIndicatorShowing = " + mIsSimIndicatorShowing + " mBusinessType is "
                + mBusinessType);
        if (mIsSimIndicatorShowing && mBusinessType != null) {
            showSimIndicator(mBusinessType);
        }
        if (mSimIndicatorIconShow && mBusinessType != null) {
            long simId = SIMHelper.getDefaultSIM(mContext, mBusinessType);
            if (mSimIndicatorIcon != null && simId != android.provider.Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK
                    && simId != android.provider.Settings.System.VOICE_CALL_SIM_SETTING_INTERNET
                    && simId != android.provider.Settings.System.SMS_SIM_SETTING_AUTO) {
                mSimIndicatorIcon.setVisibility(View.GONE);
            }
        }
    }

    private void showAlwaysAskOrInternetCall(long simId) {
        mSimIndicatorIconShow = true;
        if (simId == android.provider.Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
            mSimIndicatorIcon.setBackgroundResource(R.drawable.sim_indicator_internet_call);
        } else if (simId == android.provider.Settings.System.SMS_SIM_SETTING_AUTO) {
            mSimIndicatorIcon.setBackgroundResource(R.drawable.sim_indicator_auto);
        } else {
            mSimIndicatorIcon.setBackgroundResource(R.drawable.sim_indicator_always_ask);
        }
        if (!mAirplaneMode) {
            mSimIndicatorIcon.setVisibility(View.VISIBLE);
        } else {
            mSimIndicatorIcon.setVisibility(View.GONE);
            mSimIndicatorIconShow = false;
        }
    }

    private static boolean isInternetCallEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.ENABLE_INTERNET_CALL, 0) == 1;
    }

    /// M: [SystemUI] Support "SIM Indicator". }@

    /// M: [SystemUI]Show application guide for App. @{
    private Dialog mAppGuideDialog;
    private Button mAppGuideButton;
    private String mAppName;
    private View mAppGuideView;
    private static final String SHOW_APP_GUIDE_SETTING = "settings";
    private static final String MMS = "MMS";
    private static final String PHONE = "PHONE";
    private static final String CONTACTS = "CONTACTS";
    private static final String MMS_SHOW_GUIDE = "mms_show_guide";
    private static final String PHONE_SHOW_GUIDE = "phone_show_guide";
    private static final String CONTACTS_SHOW_GUIDE = "contacts_show_guide";

    public void showApplicationGuide(String appName) {
        SharedPreferences settings = mContext.getSharedPreferences(SHOW_APP_GUIDE_SETTING, 0);
        mAppName = appName;
        Xlog.d(TAG, "showApplicationGuide appName = " + appName);
        if (MMS.equals(appName) && "1".equals(settings.getString(MMS_SHOW_GUIDE, "1"))) {
            createAndShowAppGuideDialog();
        } else if (PHONE.equals(appName) && "1".equals(settings.getString(PHONE_SHOW_GUIDE, "1"))) {
            createAndShowAppGuideDialog();
        } else if (CONTACTS.equals(appName) && "1".equals(settings.getString(CONTACTS_SHOW_GUIDE, "1"))) {
            createAndShowAppGuideDialog();
        }
    }

    public void createAndShowAppGuideDialog() {
        Xlog.d(TAG, "createAndShowAppGuideDialog");
        if ((mDisabled & StatusBarManager.DISABLE_EXPAND) != 0) {
            Xlog.d(TAG, "StatusBar can not expand, so return.");
            return;
        }
        mAppGuideDialog = new ApplicationGuideDialog(mContext, R.style.ApplicationGuideDialog);
        mAppGuideDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
        animateExpandNotificationsPanelSlow();
        mAppGuideDialog.show();
        ObjectAnimator oa = ObjectAnimator.ofFloat(mAppGuideView, "alpha", 0.0f, 1.0f);
        oa.setDuration(1500);
        oa.start();
    }

    private class ApplicationGuideDialog extends Dialog {

        public ApplicationGuideDialog(Context context, int theme) {
            super(context, theme);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAppGuideView = View.inflate(mContext, R.layout.application_guide, null);
            setContentView(mAppGuideView);
            if (FeatureOption.MTK_THEMEMANAGER_APP) {
                final int themeMainColor = mContext.getResources().getThemeMainColor();
                if (themeMainColor != 0) {
                    TextView applicationGuideTitle = (TextView)mAppGuideView.findViewById(R.id.applicationGuideTitleText);
                    applicationGuideTitle.setTextColor(themeMainColor);
                }
            }
            mAppGuideButton = (Button) mAppGuideView.findViewById(R.id.appGuideBtn);
            mAppGuideButton.setOnClickListener(mAppGuideBtnListener);
        }

        @Override
        public void onBackPressed() {
            mAppGuideDialog.dismiss();
            animateCollapsePanels();
            super.onBackPressed();
        }
        
    }

    private View.OnClickListener mAppGuideBtnListener = new View.OnClickListener() {
        public void onClick(View v) {
            Xlog.d(TAG, "onClick! dimiss application guide dialog.");
            mAppGuideDialog.dismiss();
            animateCollapsePanels();
            SharedPreferences settings = mContext.getSharedPreferences(SHOW_APP_GUIDE_SETTING, 0);
            SharedPreferences.Editor editor = settings.edit();
            if (MMS.equals(mAppName)) {
                editor.putString(MMS_SHOW_GUIDE, "0");
                editor.commit();
            } else if (PHONE.equals(mAppName)) {
                editor.putString(PHONE_SHOW_GUIDE, "0");
                editor.commit();
            } else if (CONTACTS.equals(mAppName)) {
                editor.putString(CONTACTS_SHOW_GUIDE, "0");
                editor.commit();
            }
        }
    };
    
    public void dismissAppGuide() {
        if (mAppGuideDialog != null && mAppGuideDialog.isShowing()) {
            Xlog.d(TAG, "dismiss app guide dialog");
            mAppGuideDialog.dismiss();
            mNotificationPanel.cancelTimeAnimator();
            makeExpandedInvisible();
        }
    }

    private void refreshApplicationGuide() {
        if (mAppGuideDialog != null) {
            mAppGuideView = View.inflate(mContext, R.layout.application_guide, null);
            mAppGuideDialog.setContentView(mAppGuideView);
            if (FeatureOption.MTK_THEMEMANAGER_APP) {
                final int themeMainColor = mContext.getResources().getThemeMainColor();
                if (themeMainColor != 0) {
                    TextView applicationGuideTitle = (TextView)mAppGuideView.findViewById(R.id.applicationGuideTitleText);
                    applicationGuideTitle.setTextColor(themeMainColor);
                }
            }
            mAppGuideButton = (Button) mAppGuideView.findViewById(R.id.appGuideBtn);
            mAppGuideButton.setOnClickListener(mAppGuideBtnListener);
        }
    }
    /// M: [SystemUI]Show application guide for App. @}

    /// M: [SystemUI]Support ThemeManager. @{
    private void refreshExpandedView(Context context) {
        for (int i = 0, n = this.mNotificationData.size(); i < n; i++) {
            Entry entry = this.mNotificationData.get(i);
            inflateViews(entry, mPile);
        }
        loadNotificationShade();
        updateExpansionStates();
        setAreThereNotifications();
        mNotificationPanel.onFinishInflate();
        mToolBarView.mSimSwitchPanelView.updateSimInfo();
        if (mHasFlipSettings) {
            ImageView notificationButton = (ImageView) mStatusBarWindow.findViewById(R.id.notification_button);
            if (notificationButton != null) {
                notificationButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_notifications));
            }
        }
        if (mHasSettingsPanel) {
            if (mStatusBarView.hasFullWidthNotifications()) {
                ImageView settingsButton = (ImageView) mStatusBarWindow.findViewById(R.id.settings_button);
                settingsButton.setImageDrawable(context.getResources()
                        .getDrawable(R.drawable.ic_notify_quicksettings));
            }
        } else {
            ImageView settingsButton = (ImageView) mStatusBarWindow.findViewById(R.id.settings_button);
            settingsButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_notify_settings));
        }
        ImageView clearButton = (ImageView) mStatusBarWindow.findViewById(R.id.clear_all_button);
        clearButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_notify_clear));
        ImageView headerSettingsButton = (ImageView) mStatusBarWindow.findViewById(R.id.header_settings_button);
        headerSettingsButton.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_notify_settings));
    }
    /// M: [SystemUI]Support ThemeManager. @}

    /// M: [ALPS00512845] Handle SD Swap Condition
    private ArrayList<IBinder> mNeedRemoveKeys;
    private boolean mAvoidSDAppAddNotification;
    private static final String EXTERNAL_SD0 = (FeatureOption.MTK_SHARED_SDCARD && !FeatureOption.MTK_2SDCARD_SWAP) ? "/storage/emulated/0" : "/storage/sdcard0";
    private static final String EXTERNAL_SD1 = "/storage/sdcard1";

    private BroadcastReceiver mMediaEjectBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            StorageVolume storageVolume = (StorageVolume) intent.getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
            if (storageVolume == null) {
                return;
            }
            String path = storageVolume.getPath();
            if (!EXTERNAL_SD0.equals(path) && !EXTERNAL_SD1.equals(path)) {
                return;
            }
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                Xlog.d(TAG, "receive Intent.ACTION_MEDIA_EJECT to remove notification & path = " + path);
                mAvoidSDAppAddNotification = true;
                if (mNeedRemoveKeys.isEmpty()) {
                    Xlog.d(TAG, "receive Intent.ACTION_MEDIA_EJECT to remove notificaiton done, array is empty");
                    return;
                }
                ArrayList<IBinder> copy = (ArrayList) mNeedRemoveKeys.clone();
                for (IBinder key : copy) {
                    removeNotification(key);
                }
                copy.clear();
                System.gc();
                Xlog.d(TAG, "receive Intent.ACTION_MEDIA_EJECT to remove notificaiton done, array size is " + mNeedRemoveKeys.size());
            } else if(action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                Xlog.d(TAG, "receive Intent.ACTION_MEDIA_MOUNTED, path =" + path);
                mAvoidSDAppAddNotification = false;
            }
        }
    };
}
