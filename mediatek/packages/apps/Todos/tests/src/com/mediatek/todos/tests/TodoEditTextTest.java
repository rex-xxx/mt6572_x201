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

package com.mediatek.todos.tests;

import com.mediatek.todos.LogUtils;
import com.mediatek.todos.TodoEditText;
import android.test.AndroidTestCase;


public class TodoEditTextTest extends AndroidTestCase {
    private String TAG = "TodoEditTextTest";

    /**
     * test the LenghtFilter method. Set different texts and check the length.
     */
    public void testLengthFilter() {
        LogUtils.v(TAG, "testLengthFilter");
        TodoEditText todoEditText = new TodoEditText(this.mContext, null);
        int maxLength = 15;
        todoEditText.setMaxLength(maxLength);
        // when enter English
        todoEditText.setText("qwrqfs");
        assertEquals(6, todoEditText.getText().length());
        todoEditText.setText("lc?hfn,usgs.dhg");
        assertEquals(15, todoEditText.getText().length());
        todoEditText.setText("q!wr12%q56fs98wsdfggf");
        assertEquals(15, todoEditText.getText().length());
        assertEquals("q!wr12%q56fs98w", todoEditText.getText().toString());

        // when enter English&Chinese
        todoEditText.setText("qwr我们一起");
        assertEquals(7, todoEditText.getText().length());
        todoEditText.setText("lch我们fndus大家gsgdhgd");
        assertEquals(11, todoEditText.getText().length());
        assertEquals("lch我们fndus大", todoEditText.getText().toString());
        todoEditText.setText("qwlch我们fs大家r12q56fs98wsdfggf");
        assertEquals(11, todoEditText.getText().length());
        assertEquals("qwlch我们fs大家", todoEditText.getText().toString());

        // when enter Chinese
        todoEditText.setText("如果时间到了的话课就补开使金子会");
        assertEquals(7, todoEditText.getText().length());
        assertEquals("如果时间到了的", todoEditText.getText().toString());
        todoEditText.setText("补 开使 金子会发了的话课就光");
        assertEquals(8, todoEditText.getText().length());
        assertEquals("补 开使 金子会", todoEditText.getText().toString());
    }

    /**
     * test method: setDrawLines(); values true or false should be OK.
     */
    public void testDrawLines() {
        LogUtils.v(TAG, "testDrawLines");
        TodoEditText todoEditText = new TodoEditText(this.mContext, null);
        todoEditText.setDrawLines(true);
        TodoEditText mEditText = new TodoEditText(this.mContext, null);
        mEditText.setDrawLines(false);
    }
}
