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
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Plane;
import com.mediatek.ngin3d.animation.SpriteAnimation;
import com.mediatek.ngin3d.utils.Ngin3dException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SpriteAnimationTest extends ActivityInstrumentationTestCase2<PresentationStubActivity> {
    public SpriteAnimationTest() {
        super("com.mediatek.ngin3d.tests", PresentationStubActivity.class);
    }

    private PresentationStubActivity mActivity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testAddSprite() {
        SpriteAnimation.SpriteSheet sheet = new SpriteAnimation.SpriteSheet(mActivity.getResources(), R.drawable.hamtaro, R.raw.hamtaro);
        Image target = Image.createFromResource(mActivity.getResources(), R.drawable.angel1);

        SpriteAnimation singleSprite = new SpriteAnimation(target, 1500);
        singleSprite.addSprite(mActivity.getResources(), R.drawable.angel1);
        assertThat(singleSprite.getSpriteFrameCount(), is(1));

        try {
            singleSprite.addSprite("Hamtaro_left1.png");
            fail("Should throw Ngin3dException, Use addSprite(Resources res, int resId), not addSprite(String name).");
        } catch (Ngin3dException e) {
            // exception
        } catch (Exception e) {
            fail("Unexpected exception");
        }

        try {
            singleSprite.setRange(0, 2);
            fail("Should throw Ngin3dException, Use addSprite(Resources res, int resId), not setRange(int start, int end).");
        } catch (Ngin3dException e) {
            // exception
        } catch (Exception e) {
            fail("Unexpected exception");
        }

        SpriteAnimation explicitSheet = new SpriteAnimation(target, sheet, 1500);
        try {
            explicitSheet.addSprite(mActivity.getResources(), R.drawable.angel1);
            fail("Should throw Ngin3dException, Use addSprite(String name) to specify sprite in sheet or Do nothing to run all sprite.");
        } catch (Ngin3dException e) {
            // exception
        } catch (Exception e) {
            fail("Unexpected exception");
        }

        explicitSheet.addSprite("Hamtaro_left1.png");
        assertThat(explicitSheet.getSpriteFrameCount(), is(1));
        explicitSheet.setRange(0, 2);
        assertThat(explicitSheet.getSpriteFrameCount(), is(4));

        SpriteAnimation implicitSheet = new SpriteAnimation(mActivity.getResources(), target, R.drawable.hamtaro, 1500, 10, 10);
        try {
            implicitSheet.addSprite(mActivity.getResources(), R.drawable.angel1);
            fail("Should throw Ngin3dException, Specify range or Do nothing to run all sprite.");
        } catch (Ngin3dException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception");
        }

        try {
            implicitSheet.addSprite("Hamtaro_left1.png");
            fail("Should throw Ngin3dException, Specify range or Do nothing to run all sprite.");
        } catch (Ngin3dException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception");
        }

        implicitSheet.setRange(0, 2);
        assertThat(implicitSheet.getSpriteFrameCount(), is(3));
    }

    public void testOtherMethod() {
        Image target = Image.createFromResource(mActivity.getResources(), R.drawable.angel1);
        SpriteAnimation singleSprite = new SpriteAnimation(target, 1500);
        Actor actor = new Plane();

        singleSprite.setTarget(target);
        assertEquals(singleSprite.getTarget(), target);

        singleSprite.clear();
        assertThat(singleSprite.getSpriteFrameCount(), is(0));
        assertThat(singleSprite.getSpriteFrameCount(), is(0));

        try {
            singleSprite.setTarget(actor);
            fail("Should throw Ngin3dException, It must use Image type.");
        } catch (Ngin3dException e) {
            // exception
        } catch (Exception e) {
            fail("Unexpected exception");
        }
    }
}
