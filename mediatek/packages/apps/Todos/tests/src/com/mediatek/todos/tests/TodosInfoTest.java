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
import com.mediatek.todos.TestUtils;
import com.mediatek.todos.TodoInfo;
import com.mediatek.todos.provider.TodosDatabaseHelper.TodoColumn;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.text.format.Time;


public class TodosInfoTest extends AndroidTestCase {
    private static final String TAG = "TodosInfoTest";

    public static final Uri TODO_URI = Uri.parse("content://com.mediatek.todos/todos");

    public static final int ID = 0;
    public static final int TITLE = 1;
    public static final int CREATE_TIME = 2;
    public static final int DESCRIPTION = 3;
    public static final int DTEND = 4;
    public static final int COMPLETE_TIME = 5;
    public static final int STATUS = 6;

    /**
     * The ContentValues shall contains all information of the TodoInfo
     */
    public void testMakeContentValues() {
        LogUtils.v(TAG, "testMakeContentValues");
        TodoInfo info = new TodoInfo();
        TestUtils utils = new TestUtils();
        String title = "qwerwr";
        String description = "faghkvnmcn";
        Time testTime = new Time();
        testTime.setToNow();
        utils.setTitle(info, title);
        utils.setDescription(info, description);
        utils.setDueDay(info, testTime.normalize(true));
        utils.setStatus(info, TodoInfo.STATUS_DONE);
        ContentValues values = info.makeContentValues();
        assertEquals(values.getAsString(TodoColumn.ID), utils.getAttribute(info, ID));
        assertEquals(values.getAsString(TodoColumn.TITLE), title);
        assertEquals(values.getAsString(TodoColumn.CREATE_TIME), utils.getAttribute(info,
                CREATE_TIME));
        assertEquals(values.getAsString(TodoColumn.DESCRIPTION), description);
        assertEquals(values.getAsString(TodoColumn.DTEND), utils.getAttribute(info, DTEND));
        assertEquals(values.getAsString(TodoColumn.COMPLETE_TIME), utils.getAttribute(info,
                COMPLETE_TIME));
        assertEquals(values.getAsString(TodoColumn.STATUS), TodoInfo.STATUS_DONE);
    }

    /**
     *Test method TodoInfo.copy().
     */
    public void testCopy() {
        LogUtils.v(TAG, "testCopy");
        TodoInfo originalInfo = new TodoInfo();
        TestUtils utils = new TestUtils();
        String title = "ouwpruf";
        String description = "mbxcpafg";
        Time testTime = new Time();
        testTime.set(5, 6, 2012);
        TodoInfo info = new TodoInfo();
        utils.setTitle(info, title);
        utils.setDescription(info, description);
        utils.setCompleteDay(info, testTime.normalize(true));
        utils.setStatus(info, TodoInfo.STATUS_DONE);
        originalInfo.copy(info);
        assertEquals(true, originalInfo.equals(info));

        testTime.setToNow();
        utils.setDueDay(info, testTime.normalize(true));
        originalInfo.copy(info);
        assertEquals(true, originalInfo.equals(info));

        title = "";
        description = "";
        utils.setTitle(info, title);
        utils.setDescription(info, description);
        originalInfo.copy(info);
        assertEquals(true, originalInfo.equals(info));
    }

    /**
     * the cleared info should be equal to a new TodoInfo
     */
    public void testClear() {
        LogUtils.v(TAG, "testClear");
        TodoInfo info = new TodoInfo();
        TestUtils utils = new TestUtils();
        String title = "ouwpruf";
        String description = "mbxcpafg";
        utils.setTitle(info, title);
        utils.setDescription(info, description);
        info.clear();
        assertEquals(true, info.equals(new TodoInfo()));
    }

    /**
     * 1.can not test the ID 2.don't care about the createTime
     */
    public void testEquals() {
        LogUtils.v(TAG, "testEquals");
        TodoInfo originalInfo = new TodoInfo();
        TodoInfo info = new TodoInfo();
        TestUtils utils = new TestUtils();
        String title = "ouwpruf";
        String description = "mbxcpafg";
        Time testTime = new Time();
        testTime.set(8, 6, 2012);
        utils.setTitle(info, title);
        utils.setDescription(info, description);
        utils.setDueDay(info, testTime.normalize(true));
        utils.setCompleteDay(info, 0);
        utils.setStatus(info, TodoInfo.STATUS_DONE);
        originalInfo.copy(info);
        assertEquals(true, originalInfo.equals(info));

        title = "second title";
        utils.setTitle(originalInfo, title);
        assertEquals(false, originalInfo.equals(info));

        originalInfo.copy(info);
        description = "second description";
        utils.setDescription(info, description);
        assertEquals(false, originalInfo.equals(info));

        originalInfo.copy(info);
        testTime.set(9, 7, 2012);
        utils.setDueDay(info, testTime.normalize(true));
        assertEquals(false, originalInfo.equals(info));

        originalInfo.copy(info);
        utils.setCompleteDay(info, testTime.normalize(true));
        assertEquals(false, originalInfo.equals(info));
    }

    /**
     * Test method TodoInfo.isExpire(). 1. change info status to DONE and check the result. 2.
     * change info status to STATUS_TODO and check the result.
     */
    public void testIsExpire() {
        LogUtils.v(TAG, "testIsExpire");
        // 1. change info status to STATUS_DONE and check the result.
        TodoInfo info = new TodoInfo();
        TestUtils utils = new TestUtils();
        utils.setStatus(info, TodoInfo.STATUS_DONE);
        assertEquals(false, info.isExpire());
        // 2.change info status to STATUS_TODO,don't set the mDueDate.
        utils.setStatus(info, TodoInfo.STATUS_TODO);
        assertEquals(false, info.isExpire());
        // 3.when set the mDueDate.
        utils.setDueDay(info, System.currentTimeMillis() + 100000);
        assertFalse(info.isExpire());
        utils.setDueDay(info, System.currentTimeMillis() - 60000);
        assertTrue(info.isExpire());
    }

    /**
     * This method is not used in Todos, just a simple test.
     */
    public void testIsComplete() {
        LogUtils.v(TAG, "testIsComplete");
        TodoInfo info = new TodoInfo();
        // change status to STATUS_DONE.
        info.updateStatus(TodoInfo.STATUS_DONE);
        assertTrue(info.isComplete());

        // change status to STATUS_TODO.
        info.updateStatus(TodoInfo.STATUS_TODO);
        assertFalse(info.isComplete());
    }

    /**
     * Test method TodoInfo.updateStatus() 1.update info's Status STATUS_TODO-->STATUS_DONE 2.update
     * info's Status STATUS_DONE-->STATUS_TODO
     */
    public void testUpdateStatus() {
        LogUtils.v(TAG, "testUpdateStatus");
        TodoInfo info = new TodoInfo();
        TestUtils utils = new TestUtils();
        String title = "title";
        String description = "description";
        Time testTime = new Time();
        testTime.setToNow();
        utils.setTitle(info, title);
        utils.setDescription(info, description);
        utils.setDueDay(info, testTime.normalize(true));
        utils.setCompleteDay(info, 0);
        utils.setStatus(info, TodoInfo.STATUS_TODO);
        // 1.STATUS_TODO--STATUS_DONE,the completeTime should be changed.
        testTime.setToNow();
        long startTime = testTime.normalize(true);
        info.updateStatus(TodoInfo.STATUS_DONE);
        testTime.setToNow();
        long endTime = testTime.normalize(true);
        long completeTime = Long.parseLong(utils.getAttribute(info, COMPLETE_TIME));
        assertTrue("compete time error", completeTime >= startTime && completeTime <= endTime);
        assertEquals(TodoInfo.STATUS_DONE, utils.getAttribute(info, STATUS));
        // 2.STATUS_DONE--STATUS_TODO,the completeTime should be "0".
        info.updateStatus(TodoInfo.STATUS_TODO);
        assertEquals(TodoInfo.STATUS_TODO, utils.getAttribute(info, STATUS));
        assertEquals("0", utils.getAttribute(info, COMPLETE_TIME));
    }

    /**
     * Test method TodoInfo.makeTodoInfoFromCursor(Cursor cursor). the Cursor is valid.
     */
    public void testmakeTodoInfoFromCursor() {
        LogUtils.v(TAG, "testmakeTodoInfoFromCursor");
        // test default constructor
        TodoInfo todoInfo = new TodoInfo();
        TestUtils utils = new TestUtils();
        // test constructor from cursor
        Cursor cursor = mContext.getContentResolver().query(TODO_URI, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                todoInfo = TodoInfo.makeTodoInfoFromCursor(cursor);
                int index = cursor.getColumnIndex(TodoColumn.DESCRIPTION);
                String string = cursor.getString(index);
                assertEquals(string, utils.getAttribute(todoInfo, DESCRIPTION));

                index = cursor.getColumnIndex(TodoColumn.ID);
                string = cursor.getString(index);
                assertEquals(string, utils.getAttribute(todoInfo, ID));

                index = cursor.getColumnIndex(TodoColumn.COMPLETE_TIME);
                string = cursor.getString(index);
                assertEquals(string, utils.getAttribute(todoInfo, COMPLETE_TIME));

                index = cursor.getColumnIndex(TodoColumn.CREATE_TIME);
                string = cursor.getString(index);
                assertEquals(string, utils.getAttribute(todoInfo, CREATE_TIME));

                index = cursor.getColumnIndex(TodoColumn.DTEND);
                string = cursor.getString(index);
                assertEquals(string, utils.getAttribute(todoInfo, DTEND));

                index = cursor.getColumnIndex(TodoColumn.STATUS);
                string = cursor.getString(index);
                assertEquals(string, utils.getAttribute(todoInfo, STATUS));

                index = cursor.getColumnIndex(TodoColumn.TITLE);
                string = cursor.getString(index);
                assertEquals(string, utils.getAttribute(todoInfo, TITLE));
            } while (cursor.moveToNext());
        }
    }
}
