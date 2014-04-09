/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.rcse.test.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;

import com.mediatek.phone.ext.IInCallControlState;
import com.mediatek.phone.ext.IInCallScreen;
import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.plugin.phone.Constants;
import com.mediatek.rcse.plugin.phone.ICallScreenHost;
import com.mediatek.rcse.plugin.phone.ImageSharingPlugin;
import com.mediatek.rcse.plugin.phone.RCSeInCallScreenExtension;
import com.mediatek.rcse.plugin.phone.RCSePhonePlugin;
import com.mediatek.rcse.plugin.phone.RichcallProxyActivity;
import com.mediatek.rcse.plugin.phone.SharingPlugin;
import com.mediatek.rcse.plugin.phone.VideoSharingPlugin;
import com.mediatek.rcse.test.Utils;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.android.internal.telephony.CallManager;
import com.android.internal.view.menu.ActionMenu;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test case class for RCSeInCallScreenExtension
 */
public class RCSeInCallScreenExtensionTest extends
        ActivityInstrumentationTestCase2<RichcallProxyActivity> {
    private static final String TAG = "RCSeInCallScreenExtensionTest";
    private Context mContext = null;
    private MockInCallScreenExtension mExtension = null;
    private Activity mActivity = null;
    private RCSePhonePlugin mRcSePhonePlugin = null;
    private SharingPlugin mSharingPlugin = null;
    private CallManagerHelper mHelper = new CallManagerHelper();
    public static final String MOCK_NUMBER = "+3456789098";

    public RCSeInCallScreenExtensionTest() {
        super(RichcallProxyActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Logger.d(TAG, "setUp() entry");
        mActivity = getActivity();
        mContext = AndroidFactory.getApplicationContext();
        Logger.d(TAG, "testCase1_onCreate() mContext is " + mContext);
        mHelper.initialize(mContext);
        mRcSePhonePlugin = new RCSePhonePlugin(mContext);
        mSharingPlugin = new SharingPlugin(mContext);
        RCSeInCallScreenExtension.initialize(mContext, mRcSePhonePlugin);
        RCSeInCallScreenExtension extension = new MockInCallScreenExtension(mContext, mRcSePhonePlugin);
        Field fieldInstance = Utils.getPrivateField(RCSeInCallScreenExtension.class, "sInstance");
        fieldInstance.set(null, extension);
        mExtension = (MockInCallScreenExtension) RCSeInCallScreenExtension.getInstance();
        assertEquals(extension, mExtension);
        Logger.d(TAG, "setUp() exit");
    }

    /**
     * Test onCreate
     * @throws Throwable 
     */
    public void testCase1_onCreate() throws Throwable {
        Logger.d(TAG, "testCase1_onCreate() entry ");
        mExtension.onCreate(new Bundle(), mActivity, mInCallScreenHost, CallManager.getInstance());
        Method getInCallScreenActivity =
                Utils.getPrivateMethod(RCSePhonePlugin.class, "getInCallScreenActivity");
        Method getCallManager = Utils.getPrivateMethod(RCSePhonePlugin.class, "getCallManager");
        Field filedShareFilePlugin =
                Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareFilePlugIn");
        Field filedShareVideoPlugin =
                Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareVideoPlugIn");
        Activity activity = (Activity) getInCallScreenActivity.invoke(mRcSePhonePlugin);
        assertNotNull(activity);
        assertNotNull((CallManager) getCallManager.invoke(mRcSePhonePlugin));
        assertNotNull(filedShareFilePlugin.get(mExtension));
        assertNotNull(filedShareVideoPlugin.get(mExtension));
        assertNotNull(mSharingPlugin);
        final MockImageSharePlugin imageSharePlugin = new MockImageSharePlugin(mContext);
        final MockVideoSharingPlugin videoSharePlugin = new MockVideoSharingPlugin(mContext);
        filedShareFilePlugin.set(mExtension, imageSharePlugin);
        filedShareVideoPlugin.set(mExtension, videoSharePlugin);
        Resources hostResources = mExtension.mHostResources;
        final MockInCallControllState inCallControllState = new MockInCallControllState();
        final Menu menu = new ActionMenu(mContext);
        menu.add(0, hostResources.getIdentifier("menu_add_call", "id", MockInCallScreenExtension.HOST_PACKAGE_NAME), 0, "");
        menu.add(0, hostResources.getIdentifier("menu_hold_voice", "id", MockInCallScreenExtension.HOST_PACKAGE_NAME), 0, "");
        runTestOnUiThread(new Runnable(){

            @Override
            public void run() {
                try {
                    assertFalse(mExtension.dismissDialogs());
                    assertFalse(mExtension.onDisconnect(new CallManagerHelper.MockConnection()));
                    assertFalse(mExtension.updateScreen(CallManager.getInstance(), false));
                    assertFalse(mExtension.onPhoneStateChanged(CallManager.getInstance()));
                    mExtension.setupMenuItems(menu, inCallControllState);
                    inCallControllState.isOnHold = true;
                    mExtension.setupMenuItems(menu, inCallControllState);
                    videoSharePlugin.setState(Constants.SHARE_VIDEO_STATE_SHARING);
                    mExtension.setupMenuItems(menu, inCallControllState);
                    mHelper.setActiveBgCall(true);
                    assertFalse(mExtension.onPhoneStateChanged(CallManager.getInstance()));
                    imageSharePlugin.setState(Constants.SHARE_FILE_STATE_TRANSFERING);
                    assertFalse(mExtension.onPhoneStateChanged(CallManager.getInstance()));
                    inCallControllState.isOnHold = false;
                    mExtension.setupMenuItems(menu, inCallControllState);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.toString());
                }
            }
        });
        getInstrumentation().waitForIdleSync();
        ICallScreenHost screenHost = mExtension.new ShareFileCallScreenHost();
        screenHost.requestAreaForDisplay();
        screenHost.onStateChange(0);
        screenHost.onCapabilityChange(null, false);
        assertEquals(mActivity, screenHost.getCallScreenActivity());
        screenHost = mExtension.new ShareVideoCallScreenHost();
        screenHost.requestAreaForDisplay();
        screenHost.onStateChange(0);
        screenHost.onCapabilityChange(null, false);
        assertEquals(mActivity, screenHost.getCallScreenActivity());
        Logger.d(TAG, "testCase1_onCreate() exit");
    }

    /**
     * Test onDestroy
     * 
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testCase2_onDestroy() throws NoSuchFieldException, IllegalAccessException {
        Logger.d(TAG, "testCase2_onDestroy() entry ");
        Field filedShareFilePlugin =
                Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareFilePlugIn");
        Field filedShareVideoPlugin =
                Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareVideoPlugIn");
        Field filedActitity = Utils.getPrivateField(RCSeInCallScreenExtension.class, "mActivity");
        filedShareFilePlugin.set(mExtension, new ImageSharingPlugin(mContext));
        filedShareVideoPlugin.set(mExtension, new VideoSharingPlugin(mContext));
        mExtension.onDestroy(mActivity);
        assertNull(filedActitity.get(mExtension));
        assertNull(mSharingPlugin.getCallScreenHost());
        Logger.d(TAG, "testCase2_onDestroy() exit ");
    }

    /**
     * Test whether the contact has file share or video share capability
     * 
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testCase3_isCapabilityToShare() throws NoSuchFieldException, IllegalAccessException {
        Logger.d(TAG, "testCase3_isCapabilityToShare() entry ");
        Field filedShareFilePlugin =
                Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareFilePlugIn");
        Field filedShareVideoPlugin =
                Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareVideoPlugIn");
        boolean expectedValue = (boolean) mExtension.isCapabilityToShare(MOCK_NUMBER);
        assertFalse(expectedValue);
        filedShareFilePlugin.set(mExtension, new MockImageSharePlugin(mContext));
        expectedValue = (boolean) mExtension.isCapabilityToShare(MOCK_NUMBER);
        assertTrue(expectedValue);
        filedShareFilePlugin.set(mExtension, null);
        filedShareVideoPlugin.set(mExtension, new MockVideoSharingPlugin(mContext));
        expectedValue = (boolean) mExtension.isCapabilityToShare(MOCK_NUMBER);
        assertTrue(expectedValue);
        filedShareVideoPlugin.set(mExtension, new ImageSharingPlugin(mContext));
        expectedValue = (boolean) mExtension.isCapabilityToShare(MOCK_NUMBER);
        assertFalse(expectedValue);
        Logger.d(TAG, "testCase3_isCapabilityToShare() exit ");
    }

    /**
     * Test handleOnScreenMenuItemClick
     * 
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testCase5_handleOnScreenMenuItemClick() throws NoSuchFieldException,
            IllegalAccessException {
        Logger.d(TAG, "testCase4_handleOnScreenMenuItemClick() entry ");
        Field filedActitity = Utils.getPrivateField(RCSeInCallScreenExtension.class, "mActivity");
        filedActitity.set(mExtension, mActivity);
        Field filedShareFilePlugin =
                Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareFilePlugIn");
        Field filedShareVideoPlugin =
                Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareVideoPlugIn");
        MockImageSharePlugin imageSharePlugin = new MockImageSharePlugin(mContext);
        MockVideoSharingPlugin videoSharePlugin = new MockVideoSharingPlugin(mContext);
        filedShareFilePlugin.set(mExtension, imageSharePlugin);
        filedShareVideoPlugin.set(mExtension, videoSharePlugin);
        videoSharePlugin.setState(Constants.SHARE_VIDEO_STATE_SHARING);
        mExtension.handleOnScreenMenuItemClick(new MockMenuItem());
        imageSharePlugin.setState(Constants.SHARE_FILE_STATE_TRANSFERING);
        mExtension.handleOnScreenMenuItemClick(new MockMenuItem());
        Logger.d(TAG, "testCase4_handleOnScreenMenuItemClick() exit ");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Utils.clearAllStatus();
        Thread.sleep(Utils.TEAR_DOWN_SLEEP_TIME);
    }

    private class MockInCallControllState implements IInCallControlState {
        public boolean canAddCall = false;
        public boolean canHold = true;
        public boolean isOnHold = false;

        @Override
        public boolean canAddCall() {
            return canAddCall;
        }

        @Override
        public boolean canEndCall() {
            return false;
        }

        @Override
        public boolean canHold() {
            return canHold;
        }

        @Override
        public boolean canMerge() {
            return false;
        }

        @Override
        public boolean canMute() {
            return false;
        }

        @Override
        public boolean canShowSwap() {
            return false;
        }

        @Override
        public boolean canSwap() {
            return false;
        }

        @Override
        public boolean isBluetoothEnabled() {
            return false;
        }

        @Override
        public boolean isBluetoothIndicatorOn() {
            return false;
        }

        @Override
        public boolean isContactsEnabled() {
            return false;
        }

        @Override
        public boolean isDialpadEnabled() {
            return false;
        }

        @Override
        public boolean isDialpadVisible() {
            return false;
        }

        @Override
        public boolean isManageConferenceEnabled() {
            return false;
        }

        @Override
        public boolean isManageConferenceVisible() {
            return false;
        }

        @Override
        public boolean isMuteIndicatorOn() {
            return false;
        }

        @Override
        public boolean isSpeakerEnabled() {
            return false;
        }

        @Override
        public boolean isSpeakerOn() {
            return false;
        }

        @Override
        public boolean onHold() {
            return isOnHold;
        }

        @Override
        public boolean supportsHold() {
            return false;
        }
    }
    private class MockInCallScreenExtension extends RCSeInCallScreenExtension {
        private Resources mHostResources = null;
        private static final String HOST_PACKAGE_NAME = "com.android.phone";

        protected MockInCallScreenExtension(Context pluginContext, RCSePhonePlugin rcsePhonePlugin)
                throws NameNotFoundException {
            super(pluginContext, rcsePhonePlugin);
            PackageManager pm = pluginContext.getPackageManager();
            mHostResources = pm.getResourcesForApplication(HOST_PACKAGE_NAME);
            assertNotNull(mHostResources);
        }

        @Override
        protected String getHostPackageName() {
            super.getHostPackageName();
            return HOST_PACKAGE_NAME;
        }

        @Override
        protected Resources getHostResources() {
            super.getHostResources();
            return mHostResources;
        }

        
    }

    private class MockMenuItem implements MenuItem {

        @Override
        public MenuItem setVisible(boolean visible) {
            return null;
        }

        @Override
        public MenuItem setTitleCondensed(CharSequence title) {
            return null;
        }

        @Override
        public MenuItem setTitle(int title) {
            return null;
        }

        @Override
        public MenuItem setTitle(CharSequence title) {
            return null;
        }

        @Override
        public MenuItem setShowAsActionFlags(int actionEnum) {
            return null;
        }

        @Override
        public void setShowAsAction(int actionEnum) {

        }

        @Override
        public MenuItem setShortcut(char numericChar, char alphaChar) {
            return null;
        }

        @Override
        public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
            return null;
        }

        @Override
        public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
            return null;
        }

        @Override
        public MenuItem setNumericShortcut(char numericChar) {
            return null;
        }

        @Override
        public MenuItem setIntent(Intent intent) {
            return null;
        }

        @Override
        public MenuItem setIcon(int iconRes) {
            return null;
        }

        @Override
        public MenuItem setIcon(Drawable icon) {
            return null;
        }

        @Override
        public MenuItem setEnabled(boolean enabled) {
            return null;
        }

        @Override
        public MenuItem setChecked(boolean checked) {
            return null;
        }

        @Override
        public MenuItem setCheckable(boolean checkable) {
            return null;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char alphaChar) {
            return null;
        }

        @Override
        public MenuItem setActionView(int resId) {
            return null;
        }

        @Override
        public MenuItem setActionView(View view) {
            return null;
        }

        @Override
        public MenuItem setActionProvider(ActionProvider actionProvider) {
            return null;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public boolean isChecked() {
            return false;
        }

        @Override
        public boolean isCheckable() {
            return false;
        }

        @Override
        public boolean isActionViewExpanded() {
            return false;
        }

        @Override
        public boolean hasSubMenu() {
            return false;
        }

        @Override
        public CharSequence getTitleCondensed() {
            return null;
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @Override
        public SubMenu getSubMenu() {
            return null;
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public char getNumericShortcut() {
            return 0;
        }

        @Override
        public ContextMenuInfo getMenuInfo() {
            return null;
        }

        @Override
        public int getItemId() {
            return mExtension.mHostResources.getIdentifier("menu_hold_voice", "id",
                    MockInCallScreenExtension.HOST_PACKAGE_NAME);
        }

        @Override
        public Intent getIntent() {
            return null;
        }

        @Override
        public Drawable getIcon() {
            return null;
        }

        @Override
        public int getGroupId() {
            return 0;
        }

        @Override
        public char getAlphabeticShortcut() {
            return 0;
        }

        @Override
        public View getActionView() {
            return null;
        }

        @Override
        public ActionProvider getActionProvider() {
            return null;
        }

        @Override
        public boolean expandActionView() {
            return false;
        }

        @Override
        public boolean collapseActionView() {
            return false;
        }
    }

    private IInCallScreen mInCallScreenHost = new IInCallScreen() {
        public void requestUpdateScreen() {
        }
    };

    public static class MockImageSharePlugin extends ImageSharingPlugin {
        private static final String TAG = "MockImageSharePlugin";
        private int mState = 0;

        public MockImageSharePlugin(Context context) {
            super(context);
        }

        @Override
        public boolean getCapability(String number) {
            if (number.equals(MOCK_NUMBER)) {
                Logger.d(TAG, "getCapability(), number = " + number
                        + ", for test case always return true.");
                return true;
            }
            return false;
        }

        public void setState(int state) {
            mState = state;
        }

        @Override
        public int getState() {
            return mState;
        }
    }

    public static class MockVideoSharingPlugin extends VideoSharingPlugin {
        private static final String TAG = "MockVideoSharingPlugin";
        private int mState = 0;
        public MockVideoSharingPlugin(Context context) {
            super(context);
        }

        @Override
        public boolean getCapability(String number) {
            if (number.equals(MOCK_NUMBER)) {
                Logger.d(TAG, "getCapability(), number = " + number
                        + ", for test case always return true.");
                return true;
            }
            return false;
        }

        public void setState(int state) {
            mState = state;
        }

        @Override
        public int getState() {
            return mState;
        }
    }
}
