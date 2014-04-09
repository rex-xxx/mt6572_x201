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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.InstrumentationTestCase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Phone;

import com.mediatek.rcse.plugin.phone.Constants;
import com.mediatek.rcse.plugin.phone.RCSeInCallScreenExtension;
import com.mediatek.rcse.plugin.phone.RCSeInCallTouchUiExtension;
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
 * This class is used to test RCSeInCallTouchUiExtension class
 */
public class RCSeInCallTouchUiExtensionTest extends ActivityInstrumentationTestCase2<RichcallProxyActivity> {

    private Context mPluginContext = null;

    private MockInCallTouchUiExtension mExtension = null;

    private Activity mActivity;

    private View mEndSharingVideo;

    private View mShareFileButton;

    private View mShareVideoButton;

    private CallManagerHelper mHelper = new CallManagerHelper();

    private RCSePhonePlugin mRcSePhonePlugin = null;

    public RCSeInCallTouchUiExtensionTest() {
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
        mExtension = new MockInCallTouchUiExtension(mPluginContext, mRcSePhonePlugin);

        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final View contentView = initializeView();
                    mActivity.setContentView(contentView);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        getInstrumentation().waitForIdleSync();
    }

    private View initializeView() {
        Resources hostResources = mExtension.mHostResources;
        RelativeLayout touchUi = new RelativeLayout(mPluginContext);
        ImageButton endSharingVideoButton = new ImageButton(mPluginContext);
        endSharingVideoButton.setId(hostResources.getIdentifier("endSharingVideo", "id",
                MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        mEndSharingVideo = endSharingVideoButton;
        touchUi.addView(endSharingVideoButton);
        ImageButton shareFileButton = new ImageButton(mPluginContext);
        shareFileButton.setId(hostResources.getIdentifier("shareFileButton", "id",
                MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        mShareFileButton = shareFileButton;
        touchUi.addView(shareFileButton);
        ImageButton shareVideoButton = new ImageButton(mPluginContext);
        shareVideoButton.setId(hostResources.getIdentifier("shareVideoButton", "id",
                MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        mShareVideoButton = shareVideoButton;
        touchUi.addView(shareVideoButton);
        ViewGroup inCallControllArea = new RelativeLayout(mPluginContext);
        inCallControllArea.setId(hostResources.getIdentifier("inCallControlArea", "id",
                MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        inCallControllArea.setBackgroundColor(Color.WHITE);
        touchUi.addView(inCallControllArea);
        Drawable[] drawables = new Drawable[1];
        drawables[0] = new ColorDrawable(Color.WHITE);
        MockLayerDrawable emptyDrawable = new MockLayerDrawable(drawables);
        MockLayerDrawable validDrawable = new MockLayerDrawable(drawables);
        validDrawable.isValid = true;

        CompoundButton dialpadButton = new CheckBox(mPluginContext);
        dialpadButton
                .setId(hostResources.getIdentifier("dialpadButton", "id", MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        dialpadButton.setBackgroundDrawable(emptyDrawable);
        touchUi.addView(dialpadButton);

        CompoundButton holdButton = new CheckBox(mPluginContext);
        holdButton.setId(hostResources.getIdentifier("holdButton", "id", MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        holdButton.setBackgroundDrawable(validDrawable);
        touchUi.addView(holdButton);

        CompoundButton muteButton = new CheckBox(mPluginContext);
        muteButton.setId(hostResources.getIdentifier("muteButton", "id", MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        muteButton.setBackgroundDrawable(validDrawable);
        touchUi.addView(muteButton);

        CompoundButton audioButton = new CheckBox(mPluginContext);
        audioButton.setId(hostResources.getIdentifier("audioButton", "id", MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        audioButton.setBackgroundDrawable(emptyDrawable);
        touchUi.addView(audioButton);

        View shareFileShareVideoSpacer = new View(mPluginContext);
        shareFileShareVideoSpacer.setId(hostResources.getIdentifier("shareFileShareVideoSpacer", "id",
                MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        touchUi.addView(shareFileShareVideoSpacer);
        View leftDialpadSpacer = new View(mPluginContext);
        leftDialpadSpacer.setId(hostResources.getIdentifier("leftDialpadSpacer", "id",
                MockInCallTouchUiExtension.HOST_PACKAGE_NAME));
        touchUi.addView(leftDialpadSpacer);
        mExtension.onFinishInflate(touchUi);
        return touchUi;
    }

    /**
     * Test updateState method
     * 
     * @throws Exception
     */
    public void testCase1_testUpdateState() throws Throwable {

        assertFalse(RCSeUtils.canShare(CallManager.getInstance()));
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mExtension.updateState(CallManager.getInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        });
        getInstrumentation().waitForIdleSync();

        Field filedShareFilePlugin = Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareFilePlugIn");
        Field filedShareVideoPlugin = Utils.getPrivateField(RCSeInCallScreenExtension.class, "mShareVideoPlugIn");
        final MockImageSharePlugin imageSharePlugin = new MockImageSharePlugin(mPluginContext);
        final MockVideoSharingPlugin videoSharePlugin = new MockVideoSharingPlugin(mPluginContext);
        filedShareFilePlugin.set(RCSeInCallScreenExtension.getInstance(), imageSharePlugin);
        filedShareVideoPlugin.set(RCSeInCallScreenExtension.getInstance(), videoSharePlugin);
        assertTrue(RCSeUtils.canShare(CallManager.getInstance()));
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mExtension.updateState(CallManager.getInstance());
                    mExtension.onClick(mShareFileButton);
                    mExtension.onClick(mShareVideoButton);
                    imageSharePlugin.setState(Constants.SHARE_FILE_STATE_TRANSFERING);
                    videoSharePlugin.setState(Constants.SHARE_VIDEO_STATE_SHARING);
                    mExtension.updateState(CallManager.getInstance());
                    imageSharePlugin.setState(Constants.SHARE_FILE_STATE_DISPLAYING);
                    videoSharePlugin.setState(Constants.SHARE_VIDEO_STATE_IDLE);
                    mExtension.onClick(mEndSharingVideo);
                    mExtension.updateState(CallManager.getInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        });
    }

    private class MockLayerDrawable extends TransitionDrawable {
        public boolean isValid = false;

        private Drawable[] mLayers = null;

        public MockLayerDrawable(Drawable[] layers) {
            super(layers);
            mLayers = layers;
        }

        @Override
        public Drawable findDrawableByLayerId(int id) {
            if (isValid) {
                return mLayers[0];
            } else {
                return super.findDrawableByLayerId(id);
            }
        }
    }

    private class MockInCallTouchUiExtension extends RCSeInCallTouchUiExtension {
        private Resources mHostResources = null;

        private static final String HOST_PACKAGE_NAME = "com.android.phone";

        public MockInCallTouchUiExtension(Context pluginContext, RCSePhonePlugin rcsePhonePlugin) throws Exception {
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
