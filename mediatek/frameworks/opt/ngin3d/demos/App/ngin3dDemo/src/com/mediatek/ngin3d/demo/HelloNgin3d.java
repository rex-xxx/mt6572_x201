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

import android.app.Activity;
import android.os.Bundle;
import android.text.Layout;
import com.mediatek.ngin3d.Ngin3d;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Text;
import com.mediatek.ngin3d.android.StageView;

/**
 * Add description here.
 */
public class HelloNgin3d extends Activity {

    private Stage mStage = new Stage();
    private StageView mStageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStageView = new StageView(this, mStage);
        setContentView(mStageView);

        // Normal text
        Text hello1 = new Text("Hello ngin3D!");
        // adding a background to check bounding box
        hello1.setBackgroundColor(new Color(255, 0, 0, 128));
        hello1.setPosition(new Point(0.42f, 0.1f, true));
        hello1.setMaxWidth(500);

        // Multiple lines text
        Text hello2 = new Text("Hello World, Hello Ngin3d, Hello MAGE ");
        // adding a background to check bounding box
        hello2.setBackgroundColor(new Color(255, 0, 0, 128));
        hello2.setPosition(new Point(0.42f, 0.25f, true));
        hello2.setMaxWidth(250);

        // Multiple lines text
        Text hello3 = new Text("Hello World, Hello Ngin3d, Hello MAGE ");
        // adding a background to check bounding box
        hello3.setBackgroundColor(new Color(255, 0, 0, 128));
        hello3.setPosition(new Point(0.42f, 0.44f, true));
        hello3.setMaxWidth(250);
        hello3.setAlignment(Layout.Alignment.ALIGN_CENTER);

        // Multiple lines text
        Text hello4 = new Text("Hello World, Hello Ngin3d, Hello MAGE ");
        // adding a background to check bounding box
        hello4.setBackgroundColor(new Color(255, 0, 0, 128));
        hello4.setPosition(new Point(0.42f, 0.63f, true));
        hello4.setMaxWidth(250);
        hello4.setAlignment(Layout.Alignment.ALIGN_OPPOSITE);

        // Single line text truncate at the end with dots(...)
        Text hello5 = new Text("Hello World, Hello Ngin3d, Hello MAGE ");
        // adding a background to check bounding box
        hello5.setBackgroundColor(new Color(255, 0, 0, 128));
        hello5.setPosition(new Point(0.42f, 0.81f, true));
        hello5.setMaxWidth(150);
        hello5.setSingleLine(true);

        // Single line text truncate at the end with dots(...)
        Text hello6 = new Text("Hello World, Hello Ngin3d, Hello MAGE ");
        // adding a background to check bounding box
        hello6.setBackgroundColor(new Color(255, 0, 0, 128));
        hello6.setPosition(new Point(0.42f, 0.88f, true));
        hello6.setMaxWidth(200);
        hello6.setSingleLine(true);

        mStage.add(hello1);
        mStage.add(hello2);
        mStage.add(hello3);
        mStage.add(hello4);
        mStage.add(hello5);
        mStage.add(hello6);

    }

    @Override
    protected void onPause() {
        mStageView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStageView.onResume();
    }

}
