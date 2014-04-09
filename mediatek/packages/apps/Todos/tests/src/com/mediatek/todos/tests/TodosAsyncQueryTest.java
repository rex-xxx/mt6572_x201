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
import com.mediatek.todos.QueryListener;
import com.mediatek.todos.TestUtils;
import com.mediatek.todos.TodoAsyncQuery;
import com.mediatek.todos.TodoInfo;
import com.mediatek.todos.provider.TodosDatabaseHelper.TodoColumn;
import android.app.Instrumentation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.InstrumentationTestCase;


public class TodosAsyncQueryTest extends InstrumentationTestCase {
    private static final String TAG = "TodosAsyncQueryTest";

    private Instrumentation mInst = null;
    private Context mTargetContext = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInst = getInstrumentation();
        mTargetContext = mInst.getTargetContext();
    }

    public void test01Insert() {
        LogUtils.v(TAG, "test01Insert()");
        final QueryListenerForTest queryListener = new QueryListenerForTest();
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    queryListener.startInsert(getInfo());
                }
            });
        } catch (Throwable e) {}
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {}
        assertEquals(QueryListenerForTest.INSERT, queryListener.getQueryListenerData());
    }

    public void test02Query() {
        LogUtils.v(TAG, "test02Query()");
        final QueryListenerForTest queryListener = new QueryListenerForTest();
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    queryListener.startQuery(null);
                }
            });
        } catch (Throwable e) {}
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {}
        assertEquals(QueryListenerForTest.QUERY, queryListener.getQueryListenerData());
    }

    /**
     * there is at least a todoInfo in database
     */
    public void test03Update() {
        LogUtils.v(TAG, "test03Update()");
        final QueryListenerForTest queryListener = new QueryListenerForTest();
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    queryListener.startUpdate(getInfo());
                }
            });
        } catch (Throwable e) {}
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {}
        assertEquals(QueryListenerForTest.UPDATE, queryListener.getQueryListenerData());
    }

    /**
     * there is at least a todoInfo in database
     */
    public void test04Delete() {
        LogUtils.v(TAG, "test04Delete()");
        final QueryListenerForTest queryListener = new QueryListenerForTest();
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    queryListener.startDelete(getInfo());
                }
            });
        } catch (Throwable e) {}
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {}
        assertEquals(QueryListenerForTest.DELETE, queryListener.getQueryListenerData());
    }

    public class QueryListenerForTest implements QueryListener {

        public static final int QUERY = 0;
        public static final int INSERT = 1;
        public static final int UPDATE = 2;
        public static final int DELETE = 3;
        private int mQueryListenerData = -1;
        TodoAsyncQuery mQuery = null;

        QueryListenerForTest() {
        }

        public void startQuery(String selection) {
            mQuery = TodoAsyncQuery.getInstatnce(mTargetContext);
            mQuery.startQuery(0, this, TodoAsyncQuery.TODO_URI, null, selection, null, null);
        }

        public void onQueryComplete(int token, Cursor cursor) {
            mQueryListenerData = QUERY;
            TodoAsyncQuery.free(mTargetContext);
        }

        public void startDelete(TodoInfo info) {
            mQuery = TodoAsyncQuery.getInstatnce(mTargetContext);
            TestUtils mUtils = new TestUtils();
            String selection = TodoColumn.ID + "=" + mUtils.getAttribute(info, TodosInfoTest.ID);
            mQuery.startDelete(0, this, TodoAsyncQuery.TODO_URI, selection, null);
        }

        public void onDeleteComplete(int token, int result) {
            mQueryListenerData = DELETE;
            TodoAsyncQuery.freeAll();
        }

        public void startUpdate(TodoInfo info) {
            mQuery = TodoAsyncQuery.getInstatnce(mTargetContext);
            TestUtils mUtils = new TestUtils();
            String selection = TodoColumn.ID + "=" + mUtils.getAttribute(info, TodosInfoTest.ID);
            mQuery.startUpdate(0, this, TodoAsyncQuery.TODO_URI, info.makeContentValues(),
                    selection, null);
        }

        public void onUpdateComplete(int token, int result) {
            mQueryListenerData = UPDATE;
        }

        public void startInsert(TodoInfo info) {
            mQuery = TodoAsyncQuery.getInstatnce(mTargetContext);
            mQuery.startInsert(0, this, TodoAsyncQuery.TODO_URI, info.makeContentValues());
        }

        public void onInsertComplete(int token, Uri uri) {
            mQueryListenerData = INSERT;
        }

        public int getQueryListenerData() {
            return mQueryListenerData;
        }
    }

    private TodoInfo getInfo() {
        TodoInfo info = new TodoInfo();
        TestUtils utils = new TestUtils();
        String title = "TodosAsyncQueryTest insertTitle";
        String description = "TodosAsyncQueryTest insertDescription";
        utils.setTitle(info, title);
        utils.setDescription(info, description);
        return info;
    }
}