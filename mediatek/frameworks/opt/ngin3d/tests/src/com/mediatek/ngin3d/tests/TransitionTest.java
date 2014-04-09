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

package com.mediatek.ngin3d.tests;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Transition;
import com.mediatek.ngin3d.android.StageView;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.Mode;
import com.mediatek.ngin3d.animation.PropertyAnimation;

/**
 * Add description here.
 */
public class TransitionTest extends ActivityInstrumentationTestCase2<PresentationStubActivity> {

    private Image mGImg1;
    private Image mGImg2;
    protected StageView mStageView;

    // Note that although this appears to be a 'landscape' demo the coordinates
    // are for a portrait configuration, so rotation is around the Y axis.
    private static final int XPOS = 240;
    private static final int YPOS = 400;
    private static final int ZPOS = -600; // move away while flipping

    @SuppressWarnings("deprecation")
    public TransitionTest() {
        super("com.mediatek.ngin3d.tests", PresentationStubActivity.class);
    }

    private Stage mStage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mStage = getActivity().getStage();
        mGImg1 = Image.createFromResource(getActivity().getResources(), R.drawable.trans1);
        mGImg1.setTag(R.drawable.trans1);
        mGImg2 = Image.createFromResource(getActivity().getResources(), R.drawable.trans2);
        mGImg2.setTag(R.drawable.trans2);
        mStage.setTransition(Transition.FLY);
        mStage.add(mGImg1);
        mStageView = getActivity().getStageView();
        mStageView.waitSurfaceReady();
    }

    @SmallTest
    public void testObjectPositionAfterReplace() throws InterruptedException {
        Point oldPoint = mGImg1.getPosition();
        AnimationGroup goIn = new AnimationGroup();
        goIn
            .add(new PropertyAnimation(mGImg1, "color", new Color(255, 255, 255),
                new Color(0, 0, 0)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(
                1200))
            .add(new PropertyAnimation(mGImg1, "rotation", new Rotation(0, 0, 0),
                new Rotation(0, -180, 0)).setMode(Mode.EASE_IN_OUT_CUBIC)
                .setDuration(1200))
            .add(new PropertyAnimation(mGImg1, "position", new Point(XPOS, YPOS, 0),
                new Point(XPOS, YPOS, ZPOS)).setMode(Mode.EASE_IN_OUT_CUBIC)
                .setDuration(1200));

        AnimationGroup goOut = new AnimationGroup();
        goOut
            .add(new PropertyAnimation(mGImg2, "color", new Color(0, 0, 0), new Color(
                255, 255, 255)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(1200))
            .add(new PropertyAnimation(mGImg2, "rotation", new Rotation(0, 180, 0),
                new Rotation(0, 0, 0)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(
                1200))
            .add(new PropertyAnimation(mGImg2, "position", new Point(XPOS, YPOS, ZPOS),
                new Point(XPOS, YPOS, 0)).setMode(Mode.EASE_IN_OUT_CUBIC)
                .setDuration(1200));

        mStage.setTransition(goOut, goIn);
        assertEquals(mStage.findChildByTag(R.drawable.trans1), mGImg1);
        assertEquals(mStage.findChildByTag(R.drawable.trans2), null);
        mStage.replace(mGImg1, mGImg2);
        Thread.sleep(5000);
        assertEquals(mStage.findChildByTag(R.drawable.trans1), null);
        assertEquals(mStage.findChildByTag(R.drawable.trans2), mGImg2);
    }

}
