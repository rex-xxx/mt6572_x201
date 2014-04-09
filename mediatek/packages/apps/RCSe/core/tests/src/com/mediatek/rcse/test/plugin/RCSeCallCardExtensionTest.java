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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.test.InstrumentationTestCase;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.rcse.plugin.phone.Constants;
import com.mediatek.rcse.plugin.phone.RCSeCallCardExtension;
import com.mediatek.rcse.plugin.phone.RCSeInCallScreenExtension;
import com.mediatek.rcse.plugin.phone.RCSePhonePlugin;
import com.mediatek.rcse.plugin.phone.RCSeUtils;
import com.mediatek.rcse.plugin.phone.RichcallProxyActivity;
import com.mediatek.rcse.test.Utils;
import com.mediatek.rcse.test.plugin.RCSeInCallScreenExtensionTest.MockImageSharePlugin;
import com.mediatek.rcse.test.plugin.RCSeInCallScreenExtensionTest.MockVideoSharingPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is used to test RCSeCallCardExtension class
 */
public class RCSeCallCardExtensionTest extends ActivityInstrumentationTestCase2<RichcallProxyActivity> {

    private Context mPluginContext = null;
    private MockCallCardExtension mExtension = null;
    private Activity mActivity;
    private View mPhoneNumberGeoDescription;
    private View mWholeArea;
    private View mCenterArea;
    private View mInCallTouchUi;
    private CallManagerHelper mHelper = new CallManagerHelper();

    private RCSePhonePlugin mRcSePhonePlugin = null;

    public RCSeCallCardExtensionTest() {
        super(RichcallProxyActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        mActivity = getActivity();
        mPluginContext = getInstrumentation().getTargetContext();
        mRcSePhonePlugin = new RCSePhonePlugin(mPluginContext);
        Method methodSetActivity = Utils.getPrivateMethod(RCSePhonePlugin.class, "setInCallScreenActivity", Activity.class);
        methodSetActivity.invoke(mRcSePhonePlugin, mActivity);
        Field fieldCallManager = Utils.getPrivateField(RCSePhonePlugin.class, "mCM");
        mHelper.initialize(mPluginContext);
        fieldCallManager.set(mRcSePhonePlugin, CallManager.getInstance());
        mExtension = new MockCallCardExtension(mPluginContext, mRcSePhonePlugin);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final View contentView = initializeView();
                mActivity.setContentView(contentView);
            }});
        getInstrumentation().waitForIdleSync();
    }

    private View initializeView() {
        Resources hostResources = mExtension.mHostResources;
        RelativeLayout callCard = new RelativeLayout(mPluginContext);
        RelativeLayout wholeArea = new RelativeLayout(mPluginContext);
        wholeArea.setId(hostResources.getIdentifier("largeAreaForSharing",
                "id", MockCallCardExtension.HOST_PACKAGE_NAME));
        mWholeArea = wholeArea;
        callCard.addView(wholeArea);
        TextView callStateLabel = new TextView(mPluginContext);
        callStateLabel.setId(hostResources.getIdentifier("callStateLabel",
                "id", MockCallCardExtension.HOST_PACKAGE_NAME));
        callCard.addView(callStateLabel);
        RelativeLayout callBanner = new RelativeLayout(mPluginContext);
        callBanner.setId(hostResources.getIdentifier("call_banner_1",
                "id", MockCallCardExtension.HOST_PACKAGE_NAME));
        callCard.addView(callBanner);
        TextView phoneNumberGeoDescription = new TextView(mPluginContext);
        phoneNumberGeoDescription.setId(hostResources.getIdentifier(
                    "phoneNumberGeoDescription", "id", MockCallCardExtension.HOST_PACKAGE_NAME));
        callCard.addView(phoneNumberGeoDescription);
        mPhoneNumberGeoDescription = phoneNumberGeoDescription;
        View primaryCallInfo = new View(mPluginContext);
        primaryCallInfo.setId(hostResources.getIdentifier(
                "primary_call_info", "id", MockCallCardExtension.HOST_PACKAGE_NAME));
        callCard.addView(primaryCallInfo);
        RelativeLayout centerArea = new RelativeLayout(mPluginContext);
        centerArea.setId(hostResources.getIdentifier(
                "centerAreaForSharing", "id", MockCallCardExtension.HOST_PACKAGE_NAME));
        mCenterArea = centerArea;
        callCard.addView(centerArea);
        ImageView photo = new ImageView(mPluginContext);
        photo.setId(hostResources.getIdentifier(
                "photo", "id", MockCallCardExtension.HOST_PACKAGE_NAME));
        callCard.addView(photo);
        View inCallTouchUi = new View(mPluginContext);
        inCallTouchUi.setId(hostResources.getIdentifier(
                "inCallTouchUi", "id", MockCallCardExtension.HOST_PACKAGE_NAME));
        mInCallTouchUi = inCallTouchUi;
        callCard.addView(inCallTouchUi);
        mExtension.onFinishInflate(callCard);
        return callCard;
    }
    /**
     * Test updateState method
     * @throws Exception
     */
    public void testCase1_testUpdateState() throws Throwable {
        final Method methodFullDisplayCenterArea = Utils.getPrivateMethod(RCSeCallCardExtension.class, "fullDisplayCenterArea",
                boolean.class);

        assertFalse(RCSeUtils.canShare(CallManager.getInstance()));
        runTestOnUiThread(new Runnable(){
            @Override
            public void run() {
                try {
                    assertFalse("Failed to assert updateCallInfoLayout to be false#1",mExtension.updateCallInfoLayout(PhoneConstants.State.IDLE));
                    methodFullDisplayCenterArea.invoke(mExtension, true);
                    mExtension.updateState(CallManager.getInstance());
                    mPhoneNumberGeoDescription.setVisibility(View.INVISIBLE);
                    mExtension.updateState(CallManager.getInstance());
                    mInCallTouchUi.setVisibility(View.INVISIBLE);
                    mExtension.onClick(mWholeArea);
                    mInCallTouchUi.setVisibility(View.VISIBLE);
                    mExtension.onClick(mWholeArea);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }});
        getInstrumentation().waitForIdleSync();

        Field filedShareFilePlugin = Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareFilePlugIn");
        Field filedShareVideoPlugin = Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareVideoPlugIn");
        final MockImageSharePlugin imageSharePlugin = new MockImageSharePlugin(mPluginContext);
        final MockVideoSharingPlugin videoSharePlugin = new MockVideoSharingPlugin(mPluginContext);
        filedShareFilePlugin.set(RCSeInCallScreenExtension.getInstance(), imageSharePlugin);
        filedShareVideoPlugin.set(RCSeInCallScreenExtension.getInstance(), videoSharePlugin);
        assertTrue(RCSeUtils.canShare(CallManager.getInstance()));
        runTestOnUiThread(new Runnable(){
            @Override
            public void run() {
                try {
                    assertFalse("Failed to assert updateCallInfoLayout to be false#2", mExtension.updateCallInfoLayout(PhoneConstants.State.IDLE));
                    methodFullDisplayCenterArea.invoke(mExtension, true);
                    mExtension.updateState(CallManager.getInstance());
                    mPhoneNumberGeoDescription.setVisibility(View.VISIBLE);
                    imageSharePlugin.setState(Constants.SHARE_FILE_STATE_TRANSFERING);
                    videoSharePlugin.setState(Constants.SHARE_VIDEO_STATE_SHARING);
                    assertTrue("Failed to assert updateCallInfoLayout to be true#1", mExtension.updateCallInfoLayout(PhoneConstants.State.IDLE));
                    methodFullDisplayCenterArea.invoke(mExtension, true);
                    mExtension.onClick(mWholeArea);
                    mExtension.onClick(mWholeArea);
                    mExtension.updateState(CallManager.getInstance());
                    imageSharePlugin.setState(Constants.SHARE_FILE_STATE_DISPLAYING);
                    videoSharePlugin.setState(Constants.SHARE_VIDEO_STATE_IDLE);
                    methodFullDisplayCenterArea.invoke(mExtension, true);
                    assertTrue("Failed to assert updateCallInfoLayout to be true#2", mExtension.updateCallInfoLayout(PhoneConstants.State.IDLE));
                    mExtension.onClick(mCenterArea);
                    mExtension.onClick(mCenterArea);
                    mExtension.updateState(CallManager.getInstance());
                    methodFullDisplayCenterArea.invoke(mExtension, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }});
    }

    private class MockCallCardExtension extends RCSeCallCardExtension {
        private Resources mHostResources = null;
        private static final String HOST_PACKAGE_NAME = "com.android.phone";

        public MockCallCardExtension(Context pluginContext, RCSePhonePlugin rcsePhonePlugin) throws Exception {
            super(pluginContext, rcsePhonePlugin);
            PackageManager pm = pluginContext.getPackageManager();
            mHostResources = pm.getResourcesForApplication(HOST_PACKAGE_NAME);
            assertNotNull(mHostResources);
        }

        protected Resources getHostResources() {
            super.getHostResources();
            return mHostResources;
        }

        protected String getHostPackageName() {
            super.getHostPackageName();
            return HOST_PACKAGE_NAME;
        }
    }
}
