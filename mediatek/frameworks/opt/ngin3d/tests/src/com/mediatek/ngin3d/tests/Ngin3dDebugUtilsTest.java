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

import com.mediatek.ngin3d.utils.Ngin3dDebugUtils;
import com.mediatek.ngin3d.android.StageView;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.animation.BasicAnimation;
import com.mediatek.ngin3d.animation.Mode;
import com.mediatek.ngin3d.animation.PropertyAnimation;
import com.mediatek.ngin3d.tests.R;
import com.mediatek.ngin3d.debugtools.android.serveragent.ParamObject;

public class Ngin3dDebugUtilsTest extends Ngin3dInstrumentationTestCase {

    Ngin3dDebugUtils testNgin3dDt;
    Image uniball;
    BasicAnimation mRotation;
    BasicAnimation mMove;
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        assertNotNull(getActivity());
        uniball = Image.createFromResource(getActivity().getResources(), R.drawable.uniball);
        mMove = new PropertyAnimation(uniball, "position", new Point(0, 0), new Point(480, 800))
                .setMode(Mode.EASE_IN_OUT_CUBIC)
                .setLoop(true)
                .setAutoReverse(true);
        mRotation = new PropertyAnimation(uniball, "rotation", new Rotation(0, 0, 0), new Rotation(0, 0, 360))
                .setMode(Mode.EASE_IN_SINE)
                .setLoop(true)
                .setAutoReverse(true);

        getActivity().getStage().add(uniball);
        mMove.start();
        mRotation.start();
        getActivity().getStageView().waitSurfaceReady();
        getActivity().getStageView().setRenderMode(StageView.RENDERMODE_CONTINUOUSLY);
        testNgin3dDt = new Ngin3dDebugUtils(getActivity().getStageView().getPresentationEngine(), getActivity().getStage());

    }

    @Override
    protected void tearDown() throws Exception {
        assertNotNull(getActivity());
        getActivity().getStage().removeAll();
        mRotation.stop();
        mMove.stop();
        uniball = null;
        testNgin3dDt = null;
        super.tearDown();
    }

    public void testDumpStage() {

        /* TC 1: dump stage JSON string -- simple stage */
        String string = testNgin3dDt.dumpStage();
        assertNotNull(string);
    }

    public void testDumpActorByTag() {

        /* TC 3: dump actor JSON string by valid tag */
        uniball.setTag(100);
        String string = testNgin3dDt.dumpActorByTag("100");
        assertNotNull(string);

        /* TC 4: dump actor JSON string by invalid tag */
        string = null;
        string = testNgin3dDt.dumpActorByTag("1000");
        assertNull(string);
    }

    public void testDumpActorById() {
        /* TC 5: dump actor JSON string by valid id */
        String string = testNgin3dDt.dumpActorByID(uniball.getId());
        assertNotNull(string);

        /* TC 6: dump actor JSON string by invalid id */
        string = testNgin3dDt.dumpActorByID(1000);
        assertNull(string);
    }

    public void testSetParameterByTag() {
        /* TC 7: set actor parameter by tag -- with invalid tag */
        uniball.setTag(1111);
        ParamObject po = new ParamObject();
        assertEquals(false, testNgin3dDt.setParameterByTag("1234", po));

        /* TC 8: set actor parameter by tag -- with null tag */
        assertEquals(false, testNgin3dDt.setParameterByTag(null, po));

        /* TC 9: set actor parameter by tag -- with null parameter object */
        assertEquals(false, testNgin3dDt.setParameterByTag("1111", null));

        /* TC 10: set actor parameter by tag -- with valid name */
        uniball.setTag(3333);
        ParamObject po1 = new ParamObject();
        po1.mParameterType = ParamObject.PARAMETER_TYPE_NAME;
        po1.mNameR = "UNIBALL";
        testNgin3dDt.setParameterByTag("3333", po1);
        assertEquals(uniball.getName(), "UNIBALL");

        /* TC 11: set actor parameter by tag -- with invalid name */
        po1.mParameterType = ParamObject.PARAMETER_TYPE_NAME;
        po1.mNameR = null;
        assertEquals(false, testNgin3dDt.setParameterByTag("3333", po1));

        /* TC 12: set actor parameter by tag -- with valid full color RGBH */
        ParamObject po2 = new ParamObject();
        po2.mParameterType = ParamObject.PARAMETER_TYPE_COLOR;
        po2.mColorR = 100;
        po2.mColorG = 100;
        po2.mColorB = 100;
        po2.mColorH = 100;
        testNgin3dDt.setParameterByTag("3333", po2);
        Color testColor = new Color(100, 100, 100, 100);
        assertEquals(uniball.getColor(), testColor);

        /* TC 13: set actor parameter by tag -- with invalid color */
        ParamObject po3 = new ParamObject();
        po3.mParameterType = ParamObject.PARAMETER_TYPE_COLOR;
        po3.mColorR = 500;
        po3.mColorG = 500;
        po3.mColorB = 500;
        po3.mColorH = 500;
        assertEquals(false, testNgin3dDt.setParameterByTag("3333", po3));

        /* TC 14: set actor parameter by tag -- with valid rotation */
        ParamObject po4 = new ParamObject();
        po4.mParameterType = ParamObject.PARAMETER_TYPE_ROTATION;
        po4.mRotationX = 0;
        po4.mRotationY = 0;
        po4.mRotationZ = 360;
        testNgin3dDt.setParameterByTag("3333", po4);
        Rotation testRotation = new Rotation(0, 0, 360);
        assertEquals(uniball.getRotation(), testRotation);

        /* TC 15: set actor parameter by tag -- with valid scale */
        ParamObject po5 = new ParamObject();
        po5.mParameterType = ParamObject.PARAMETER_TYPE_SCALE;
        po5.mScaleX = 50;
        po5.mScaleY = 60;
        po5.mScaleZ = 70;
        testNgin3dDt.setParameterByTag("3333", po5);
        Scale testScale = new Scale(50, 60, 70);
        assertEquals(uniball.getScale(), testScale);

        /* TC 16: set actor parameter by tag -- with valid anchor point */
        ParamObject po6 = new ParamObject();
        po6.mParameterType = ParamObject.PARAMETER_TYPE_ANCHOR;
        po6.mAnchorX = 0.1f;
        po6.mAnchorY = 0.2f;
        po6.mAnchorZ = 0.3f;
        testNgin3dDt.setParameterByTag("3333", po6);
        Point testAnchor = new Point(0.1f, 0.2f, 0.3f);
        assertEquals(uniball.getAnchorPoint(), testAnchor);

        /* TC 16: set actor parameter by tag -- with invalid anchor point */
        ParamObject po7 = new ParamObject();
        po7.mParameterType = ParamObject.PARAMETER_TYPE_ANCHOR;
        po7.mAnchorX = 10;
        po7.mAnchorY = 20;
        po7.mAnchorZ = 30;
        assertEquals(false, testNgin3dDt.setParameterByTag("3333", po7));

        /* TC 17: set actor parameter by tag -- with valid position */
        ParamObject po8 = new ParamObject();
        po8.mParameterType = ParamObject.PARAMETER_TYPE_POSITION;
        po8.mPositionX = 100;
        po8.mPositionY = 200;
        po8.mPositionZ = 300;
        testNgin3dDt.setParameterByTag("3333", po8);
        Point testPosition = new Point(100, 200, 300);
        assertEquals(uniball.getPosition(), testPosition);

        /* TC 18: set actor parameter by tag -- with valid visible */
        ParamObject po9 = new ParamObject();
        po9.mParameterType = ParamObject.PARAMETER_TYPE_VISIBLE;
        po9.mIsVisible = false;
        testNgin3dDt.setParameterByTag("3333", po9);
        assertEquals(uniball.getVisible(), false);

    }

    public void testSetParameterById() {
        /* TC 21: set actor parameter by id -- with invalid id */
        int actorId = uniball.getId();
        ParamObject po = new ParamObject();
        assertEquals(false, testNgin3dDt.setParameterByID(66666, po));

        /* TC 22: set actor parameter by id -- with null parameter object */
        assertEquals(false, testNgin3dDt.setParameterByID(actorId, null));

        /* TC 23: set actor parameter by id -- with valid name */
        ParamObject po1 = new ParamObject();
        po1.mParameterType = ParamObject.PARAMETER_TYPE_NAME;
        po1.mNameR = "UNIBALL";
        testNgin3dDt.setParameterByID(actorId, po1);
        assertEquals(uniball.getName(), "UNIBALL");

        /* TC 24: set actor parameter by id -- with invalid name */
        po1.mParameterType = ParamObject.PARAMETER_TYPE_NAME;
        po1.mNameR = null;
        assertEquals(false, testNgin3dDt.setParameterByID(actorId, po1));

        /* TC 25: set actor parameter by id -- with valid full color RGBH */
        ParamObject po2 = new ParamObject();
        po2.mParameterType = ParamObject.PARAMETER_TYPE_COLOR;
        po2.mColorR = 100;
        po2.mColorG = 100;
        po2.mColorB = 100;
        po2.mColorH = 100;
        testNgin3dDt.setParameterByID(actorId, po2);
        Color testColor = new Color(100, 100, 100, 100);
        assertEquals(uniball.getColor(), testColor);

        /* TC 26: set actor parameter by id -- with invalid color */
        ParamObject po3 = new ParamObject();
        po3.mParameterType = ParamObject.PARAMETER_TYPE_COLOR;
        po3.mColorR = 500;
        po3.mColorG = 500;
        po3.mColorB = 500;
        po3.mColorH = 500;
        assertEquals(false, testNgin3dDt.setParameterByID(actorId, po3));

        /* TC 27: set actor parameter by id -- with valid rotation */
        ParamObject po4 = new ParamObject();
        po4.mParameterType = ParamObject.PARAMETER_TYPE_ROTATION;
        po4.mRotationX = 0;
        po4.mRotationY = 0;
        po4.mRotationZ = 360;
        testNgin3dDt.setParameterByID(actorId, po4);
        Rotation testRotation = new Rotation(0, 0, 360);
        assertEquals(uniball.getRotation(), testRotation);

        /* TC 28: set actor parameter by id -- with valid scale */
        ParamObject po5 = new ParamObject();
        po5.mParameterType = ParamObject.PARAMETER_TYPE_SCALE;
        po5.mScaleX = 50;
        po5.mScaleY = 60;
        po5.mScaleZ = 70;
        testNgin3dDt.setParameterByID(actorId, po5);
        Scale testScale = new Scale(50, 60, 70);
        assertEquals(uniball.getScale(), testScale);

        /* TC 29: set actor parameter by id -- with valid anchor point */
        ParamObject po6 = new ParamObject();
        po6.mParameterType = ParamObject.PARAMETER_TYPE_ANCHOR;
        po6.mAnchorX = 0.1f;
        po6.mAnchorY = 0.2f;
        po6.mAnchorZ = 0.3f;
        testNgin3dDt.setParameterByID(actorId, po6);
        Point testAnchor = new Point(0.1f, 0.2f, 0.3f);
        assertEquals(uniball.getAnchorPoint(), testAnchor);

        /* TC 30: set actor parameter by id -- with invalid anchor point */
        ParamObject po7 = new ParamObject();
        po7.mParameterType = ParamObject.PARAMETER_TYPE_ANCHOR;
        po7.mAnchorX = 10;
        po7.mAnchorY = 20;
        po7.mAnchorZ = 30;
        assertEquals(false, testNgin3dDt.setParameterByID(actorId, po7));

        /* TC 31: set actor parameter by id -- with valid position */
        ParamObject po8 = new ParamObject();
        po8.mParameterType = ParamObject.PARAMETER_TYPE_POSITION;
        po8.mPositionX = 100;
        po8.mPositionY = 200;
        po8.mPositionZ = 300;
        testNgin3dDt.setParameterByID(actorId, po8);
        Point testPosition = new Point(100, 200, 300);
        assertEquals(uniball.getPosition(), testPosition);

        /* TC 32: set actor parameter by id -- with valid visible */
        ParamObject po9 = new ParamObject();
        po9.mParameterType = ParamObject.PARAMETER_TYPE_VISIBLE;
        po9.mIsVisible = false;
        testNgin3dDt.setParameterByID(actorId, po9);
        assertEquals(uniball.getVisible(), false);

    }

    public void testPauseResumeTickRender() {
        uniball.setTag(9999);

        /* TC 35: ngin3d pause function */
        float bx1 = uniball.getPosition().x;
        float by1 = uniball.getPosition().y;
        testNgin3dDt.pauseRender();
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        float ax1 = uniball.getPosition().x;
        float ay1 = uniball.getPosition().y;
        assertEquals(bx1, ax1);
        assertEquals(by1, ay1);

        /* TC 36: ngin3d tick function with valid tick name */
        float bx2 = uniball.getPosition().x;
        float by2 = uniball.getPosition().y;
        testNgin3dDt.tickRender(500);
        testNgin3dDt.tickRender(300);

        float ax2 = uniball.getPosition().x;
        float ay2 = uniball.getPosition().y;
        assertTrue(bx2 != ax2);
        assertTrue(by2 != ay2);

        /* TC 37: ngin3d resume function */
        float bx3 = uniball.getPosition().x;
        float by3 = uniball.getPosition().y;
        testNgin3dDt.resumeRender();
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        float ax3 = uniball.getPosition().x;
        float ay3 = uniball.getPosition().y;
        assertTrue(ax3 != bx3);
        assertTrue(ay3 != by3);

    }

    public void testGetFps() {
        /* TC 38: ngin3d get fps function */
        try {
            Thread.currentThread().sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testNgin3dDt.getFrameInterval();
        assertTrue(testNgin3dDt.getFPS() != 0);
    }

    public void testGetMemoryInfo() {
        /* TC 39: ngin3d get memory info */
        assertTrue(testNgin3dDt.getMemoryInfo() != null);
    }

    public void testGetCpuInfo() {
        /* TC 40: ngin3d get cpu usage */
        assertTrue(testNgin3dDt.getCpuUsage() != 0);
    }

    public void testRemoveActorById() {
        /* TC 41: ngin3d remove actor by valid id */
        int id = uniball.getId();
        testNgin3dDt.removeActorByID(id);
        assertNotNull(getActivity());
        assertTrue(getActivity().getStage().findChildById(id) == null);

    }

    public void testGetFrameInterval() {
        /* TC 42: ngin3d get frameinterval */
        testNgin3dDt.getFrameInterval();
        assertTrue(testNgin3dDt.getFrameInterval() != 0);

    }

    public void testDumpAnimationById() {
        /* TC 43 : ngin3d dump animation by actor id with valid id */
        assertTrue(testNgin3dDt.animationDumpToJSONByID(uniball.getId()) != null);

        /* TC 44 : ngin3d dump animation by actor id with invalid id */
        assertTrue(testNgin3dDt.animationDumpToJSONByID(7777) == null);
    }
}
