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

package com.mediatek.ngin3d.demo;

import android.os.Bundle;
import android.view.MotionEvent;
import com.mediatek.ngin3d.*;
import com.mediatek.ngin3d.android.StageActivity;
import com.mediatek.ngin3d.animation.*;
import com.mediatek.ngin3d.demo.R;

public class TransitionEffectDemo extends StageActivity {
    private Image mGImg1;
    private Image mGImg2;
    private boolean flipped;

    // Note that although this appears to be a 'landscape' demo the coordinates
    // are for a portrait configuration, so rotation is around the Y axis.
    private static final int XPOS = 240;
    private static final int YPOS = 400;
    private static final int ZPOS = 600; // move away while flipping

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGImg1 = Image.createFromResource(getResources(), R.drawable.trans1);

        mGImg1.setAnchorPoint(new Point(0.5f, 0.5f));
        mGImg1.setPosition(new Point(XPOS, YPOS, 0));

        mGImg2 = Image.createFromResource(getResources(), R.drawable.trans2);
        mGImg2.setAnchorPoint(new Point(0.5f, 0.5f));
        mGImg2.setPosition(new Point(XPOS, YPOS, 0));
        mGImg2.setRotation(new Rotation(0, 180, 0));

        mStage.setTransition(Transition.FLY);
        mStage.add(mGImg1);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mStage.isTransitionComplete()) {
                if (flipped) {
                    Point p2 = mGImg1.getPosition();
                    Point p1 = mGImg2.getPosition();
                    Image tmpImage;
                    tmpImage = mGImg1;
                    mGImg1 = mGImg2;
                    mGImg2 = tmpImage;

                    mGImg1.setPosition(p1);
                    mGImg2.setPosition(p2);
                }

                AnimationGroup firstGroup = new AnimationGroup();
                firstGroup.add(new PropertyAnimation(mGImg1, "color", new Color(255, 255, 255), new Color(0, 0, 0)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(1200))
                        .add(new PropertyAnimation(mGImg1, "rotation", new Rotation(0, 0, 0), new Rotation(0, 180, 0)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(1200))
                        .add(new PropertyAnimation(mGImg1, "position", new Point(XPOS, YPOS, 0), new Point(XPOS, YPOS, ZPOS)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(1200));

                AnimationGroup lastGroup = new AnimationGroup();
                lastGroup.add(new PropertyAnimation(mGImg2, "color", new Color(0, 0, 0), new Color(255, 255, 255)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(1200))
                        .add(new PropertyAnimation(mGImg2, "rotation", new Rotation(0, -180, 0), new Rotation(0, 0, 0)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(1200))
                        .add(new PropertyAnimation(mGImg2, "position", new Point(XPOS, YPOS, ZPOS), new Point(XPOS, YPOS, 0)).setMode(Mode.EASE_IN_OUT_CUBIC).setDuration(1200));

                mStage.setTransition(lastGroup, firstGroup);
                mStage.replace(mGImg1, mGImg2);
                flipped = true;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}
