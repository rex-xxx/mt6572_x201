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

import com.jayway.android.robotium.solo.Solo;
import com.mediatek.todos.EditTodoActivity;
import com.mediatek.todos.LogUtils;
import com.mediatek.todos.QueryListener;
import com.mediatek.todos.TestUtils;
import com.mediatek.todos.TodoAsyncQuery;
import com.mediatek.todos.TodoInfo;
import com.mediatek.todos.TodosActivity;
import com.mediatek.todos.TodosListAdapter;
import com.mediatek.todos.provider.TodosDatabaseHelper.Tables;
import com.mediatek.todos.provider.TodosDatabaseHelper.TodoColumn;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;


public class TodosActivityTest extends ActivityInstrumentationTestCase2<TodosActivity> {
    private static final int DIALOG_DELETE_ITEMS = 1;

    private String TAG = "TodosActivityTest";
    private TodosActivity mTodosActivity = null;

    private ListView mListView = null;
    private TodosListAdapter mAdapter = null;

    private Solo mSolo = null;

    public TodosActivityTest() {
        super("com.mediatek.todos", TodosActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mTodosActivity = getActivity();

        mListView = (ListView) mTodosActivity.findViewById(com.mediatek.todos.R.id.list_todos);
        mAdapter = (TodosListAdapter) mListView.getAdapter();
        preCondition(mAdapter);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mSolo = new Solo(getInstrumentation(),mTodosActivity);
    }

    @Override
    protected void tearDown() throws Exception {
        LogUtils.v(TAG, "tearDown");
        if (mTodosActivity != null) {
            mTodosActivity.finish();
            mTodosActivity = null;
        }
        super.tearDown();
    }

    /**
     * test dialogs life onCreateDialog() & onPrepareDialog()
     */
    public void testDialogsLife() {
        LogUtils.v(TAG, "testDialogsLife");
        mTodosActivity.showDialog(DIALOG_DELETE_ITEMS);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mTodosActivity.removeDialog(DIALOG_DELETE_ITEMS);
    }

    /**
     * Test method onItemClick,expand or unexpanded List; startEditActivity successfully
     */
    public void testOnItemClick() {
        LogUtils.v(TAG, "testOnItemClick");

        Instrumentation intru = getInstrumentation();
        ActivityMonitor am = intru.addMonitor(EditTodoActivity.class.getName(), null, false);

        int viewNumber = mListView.getChildCount();
        int count = mAdapter.getCount();
        int todoNumber = mAdapter.getTodosDataSource().size();
        int doneNumber = mAdapter.getDonesDataSource().size();
        for (int i = 0; i < viewNumber; i++) {
            sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
            int viewType = mAdapter.getItemViewType(i);
            View itemView = mListView.getChildAt(i);
            if (viewType == TodosListAdapter.TYPE_DONES_FOOTER) {
                TouchUtils.clickView(this, itemView);
                int showNumber = mListView.getAdapter().getCount();
                assertTrue((showNumber == count));
            } else if (viewType == TodosListAdapter.TYPE_TODOS_HEADER
                    || viewType == TodosListAdapter.TYPE_DONES_HEADER) {
                // 1.test expand or unexpanded List
                LogUtils.v(TAG, "itemView" + itemView);
                TouchUtils.clickView(this, itemView);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int showNumber = mListView.getAdapter().getCount();
                LogUtils.v(TAG, "showNumber" + showNumber + "count" + count);
                if (todoNumber == 0 && viewType == TodosListAdapter.TYPE_TODOS_HEADER
                        || doneNumber == 0 && viewType == TodosListAdapter.TYPE_DONES_HEADER) {
                    assertTrue((showNumber == count));
                } else {
//                    assertTrue((showNumber < count));
                }
                TouchUtils.clickView(this, itemView);
            } else {
                // 2.test startEditActivity successfully
                TouchUtils.clickView(this, itemView);
                EditTodoActivity mEditActivity = null;
                mEditActivity = (EditTodoActivity) am.waitForActivityWithTimeout(500);
                if (mEditActivity != null) {
                    mEditActivity.finish();
                    mEditActivity = null;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }
        }
        intru.removeMonitor(am);
    }

    /**
     * Test method onBackPressed, out of edit mode or exit the activity. 1.isEditing,change to
     * EditNull 2.is not Editing,exit the activity
     */
    public void testOnBackPressed() {
        LogUtils.v(TAG, "testOnBackPressed");
        mTodosActivity = getActivity();

        // 1.set Editing, change to EditNull
        if (mAdapter.getDonesDataSource().size() == 0 && mAdapter.getTodosDataSource().size() == 0) {
            return;
        }
        int viewNumber = mListView.getChildCount();
        LogUtils.v(TAG, "count" + viewNumber);
        // should longClick todo or done
        int i = 0;
        while (i < viewNumber) {
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_TODOS_ITEM
                    || viewType == TodosListAdapter.TYPE_DONES_ITEM) {
                View view = mListView.getChildAt(i);
                clickLongOnView(view);
                break;
            }
            ++i;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
//        assertTrue(mAdapter.isEditing());
        sendKeys(KeyEvent.KEYCODE_BACK);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
//        assertFalse(mAdapter.isEditing());

        // 2. back to destroy activity.
        sendKeys(KeyEvent.KEYCODE_BACK);
    }

    /**
     * test method OnItemLongClick,change edit mode, views' status should be write
     */
    public void testOnItemLongClick() {
        LogUtils.v(TAG, "testOnItemLongClick");
        int viewNumber = mListView.getChildCount();
        for (int i = 0; i < viewNumber; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_TODOS_ITEM
                    || viewType == TodosListAdapter.TYPE_DONES_ITEM) {
                View view = mListView.getChildAt(i);
                clickLongOnView(view);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                assertTrue(mAdapter.isEditing());
//                assertEquals(1, (mAdapter.getSeletedTodosNumber() + mAdapter.getSeletedDonesNumber()));
                this.sendKeys(KeyEvent.KEYCODE_BACK);
            }
        }
    }

    // ====== methods for testing TodosAsyncQuery ======
    /**
     * 
     */
    public void test01Query() {
        LogUtils.v(TAG, "test01Query()");
        preCondition(mAdapter);
        QueryListenerForTest queryListener = new QueryListenerForTest();
        queryListener.startQuery(null);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {}
        assertEquals(QueryListenerForTest.QUERY, queryListener.getQueryListenerData());
    }

    public void test02Insert() {
        LogUtils.v(TAG, "test02Insert()");
        QueryListenerForTest queryListener = new QueryListenerForTest();
        TodoInfo info = new TodoInfo();
        TestUtils utils = new TestUtils();
        String title = "testInsert insertTitle";
        String description = "testInsert insertDescription";
        utils.setTitle(info, title);
        utils.setDescription(info, description);
        queryListener.startInsert(info);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
        assertEquals(QueryListenerForTest.INSERT, queryListener.getQueryListenerData());
    }

    /**
     * there is at least a todoInfo in database
     */
    public void test03Update() {
        LogUtils.v(TAG, "test03Update()");
        QueryListenerForTest queryListener = new QueryListenerForTest();
        TodoInfo info = new TodoInfo();
        int todosNumber = mAdapter.getTodosDataSource().size();
        info = mAdapter.getTodosDataSource().get(todosNumber - 1);
        TestUtils utils = new TestUtils();
        String title = "update new Title";
        utils.setTitle(info, title);

        queryListener.startUpdate(info);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
        assertEquals(QueryListenerForTest.UPDATE, queryListener.getQueryListenerData());
    }

    /**
     * there is at least a todoInfo in database
     */
    public void test04Delete() {
        LogUtils.v(TAG, "test04Delete()");
        QueryListenerForTest queryListener = new QueryListenerForTest();
        TodoInfo info = new TodoInfo();
        int todosNumber = mAdapter.getTodosDataSource().size();
        info = mAdapter.getTodosDataSource().get(todosNumber - 1);
        queryListener.startDelete(info);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
        assertEquals(QueryListenerForTest.DELETE, queryListener.getQueryListenerData());
    }

    public class QueryListenerForTest implements QueryListener {
        private static final String TAG1 = "QueryListenerForTest";
        public static final int QUERY = 0;
        public static final int INSERT = 1;
        public static final int UPDATE = 2;
        public static final int DELETE = 3;

        private int mQueryListenerData = -1;

        TodoAsyncQuery mQuery = null;

        QueryListenerForTest() {
        }

        public void startQuery(String selection) {
            mQuery = TodoAsyncQuery.getInstatnce(mTodosActivity.getApplicationContext());
            mQuery.startQuery(0, this, TodoAsyncQuery.TODO_URI, null, selection, null, null);
            LogUtils.d(TAG1, "startQuery");
        }

        public void onQueryComplete(int token, Cursor cur) {
            // LogUtils.d(TAG1, "onQueryComplete.");
            mQueryListenerData = QUERY;
            mQuery.free(mTodosActivity.getApplicationContext());
        }

        public void startDelete(TodoInfo info) {
            LogUtils.d(TAG1, "startDelete");
            mQuery = TodoAsyncQuery.getInstatnce(mTodosActivity.getApplicationContext());
            TestUtils mUtils = new TestUtils();
            String selection = TodoColumn.ID + "=" + mUtils.getAttribute(info, TodosInfoTest.ID);
            mQuery.startDelete(0, this, TodoAsyncQuery.TODO_URI, selection, null);
        }

        public void onDeleteComplete(int token, int result) {
            mQueryListenerData = DELETE;
            // LogUtils.d(TAG1, "onDeleteComplete.");
            mQuery.freeAll();
        }

        public void startUpdate(TodoInfo info) {
            LogUtils.d(TAG1, "startUpdate");
            mQuery = TodoAsyncQuery.getInstatnce(mTodosActivity.getApplicationContext());
            TestUtils mUtils = new TestUtils();
            String selection = TodoColumn.ID + "=" + mUtils.getAttribute(info, TodosInfoTest.ID);
            mQuery.startUpdate(0, this, TodoAsyncQuery.TODO_URI, info.makeContentValues(),
                    selection, null);
        }

        public void onUpdateComplete(int token, int result) {

            mQueryListenerData = UPDATE;
            // LogUtils.d(TAG1, "onUpdateComplete.");
        }

        public void startInsert(TodoInfo info) {
            LogUtils.d(TAG1, "startInsert");
            mQuery = TodoAsyncQuery.getInstatnce(mTodosActivity.getApplicationContext());
            mQuery.startInsert(0, this, TodoAsyncQuery.TODO_URI, info.makeContentValues());
        }

        public void onInsertComplete(int token, Uri uri) {

            mQueryListenerData = INSERT;
            // LogUtils.d(TAG1, "onInsertComplete.");
        }

        public int getQueryListenerData() {
            return mQueryListenerData;
        }
    }

    // ====== methods for testing TodosListAdapter======
    /**
     * The mAdapter shall get all entities from DB.
     */
    public void testConstructure() {
        LogUtils.v(TAG, "testConstructure");
        SQLiteDatabase mDataBase = SQLiteDatabase.openOrCreateDatabase(
                "/data/data/com.mediatek.todos/databases/todos.db", null);
        int count = mDataBase.query(Tables.TODOS, null, null, null, null, null, null).getCount();
        int mAdapterNumber = mAdapter.getDonesDataSource().size()
                + mAdapter.getTodosDataSource().size();
        // assertEquals(count, mAdapterNumber);
    }

    /**
     * Test method: getItemViewType();Input different position values, it shall return correct
     * ViewType values
     */
    public void testGetItemViewType() {
        LogUtils.v(TAG, "testGetItemViewType");
        int viewNumber = mListView.getChildCount();
        // 1.Todos is expanded
        getItemViewType(mAdapter, viewNumber);
        // 2.Todos is unexpanded
        View itemView = mListView.getChildAt(0);
        TouchUtils.clickView(this, itemView);
        getItemViewType(mAdapter, mListView.getChildCount());
    }

    /**
     * Test method: getItem(); Input different position values, it shall return correct Item
     * Objects.
     */
    public void testGetItem() {
        LogUtils.v(TAG, "testGetItem");
        int viewNumber = mListView.getChildCount();
        // 1.Todos is expanded
        getItem(mAdapter, viewNumber);
        // 2.Todos is unexpanded
        View itemView = mListView.getChildAt(0);
        TouchUtils.clickView(this, itemView);
        getItem(mAdapter, mListView.getChildCount());
    }

    /**
     * Test method: addItem(); Input a item object, it could be inserted in arrayList and order
     * Comparator.
     */
    public void test05AddItem() {
        LogUtils.v(TAG, "test05AddItem");
        preCondition(mAdapter);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {}
        final TodoInfo info = new TodoInfo();
        info.copy(mAdapter.getTodosDataSource().get(0));
        final TestUtils utils = new TestUtils();
        utils.setTitle(info, "add a new info");
        utils.setDescription(info, "add a new info description");
        // utils.setStatus(info, TodoInfo.STATUS_DONE);
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addItem(info);
                    if (utils.getAttribute(info, TodosInfoTest.STATUS) == TodoInfo.STATUS_DONE) {
                        // compare the completed time
                        TodoInfo temp = new TodoInfo();
                        int doneSize = mAdapter.getDonesDataSource().size();
                        LogUtils.v(TAG, "doneSize" + doneSize);
                        boolean isInfo = false;
                        for (int i = 0; i < doneSize; i++) {
                            temp = mAdapter.getDonesDataSource().get(i);
                            LogUtils.v(TAG, "temp" + temp);
                            if (temp == info) {
                                isInfo = true;
                            }
                            long tempCompleteTime = Long.parseLong(utils.getAttribute(temp,
                                    TodosInfoTest.COMPLETE_TIME));
                            long infoCompleteTime = Long.parseLong(utils.getAttribute(info,
                                    TodosInfoTest.COMPLETE_TIME));
                            if (!isInfo) {
                                assertTrue(tempCompleteTime >= infoCompleteTime);
                            } else {
                                assertTrue(tempCompleteTime <= infoCompleteTime);
                            }
                        }
                    } else {
                        // compare the duedate
                        TodoInfo temp = new TodoInfo();
                        int todoSize = mAdapter.getTodosDataSource().size();
                        LogUtils.v(TAG, "todoSize" + todoSize);
                        boolean isInfo = false;
                        for (int i = 0; i < todoSize; i++) {
                            temp = mAdapter.getTodosDataSource().get(i);
                            LogUtils.v(TAG, "temp" + temp);
                            if (temp == info) {
                                isInfo = true;
                            }
                            long tempDuedate = Long.parseLong(utils.getAttribute(temp,
                                    TodosInfoTest.DTEND));
                            long infoDuedate = Long.parseLong(utils.getAttribute(info,
                                    TodosInfoTest.DTEND));
                            if (!isInfo) {
                                assertTrue(tempDuedate <= infoDuedate);
                            } else {
                                assertTrue(tempDuedate >= infoDuedate);
                            }
                        }
                    }

                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Test method: updateItemData(); Modify a item's information and update, it shall in the
     * correct position.
     */
    public void test06UpdateItem() {
        LogUtils.v(TAG, "test06UpdateItem");
        ListView mListView = (ListView) mTodosActivity
                .findViewById(com.mediatek.todos.R.id.list_todos);
        final TodosListAdapter mAdapter = (TodosListAdapter) mListView.getAdapter();
        final TodoInfo info = new TodoInfo();
        info.copy(mAdapter.getTodosDataSource().get(0));
        final TestUtils utils = new TestUtils();
        utils.setTitle(info, "update info");
        utils.setDescription(info, "update info description");
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.updateItemData(info);
                    LogUtils.v(TAG, "update-info" + mAdapter.getTodosDataSource().get(0));
                    assertEquals(info, mAdapter.getTodosDataSource().get(0));
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Test method: delteSelectedItems(); It shall remove all checked entities form DB & ArrayList
     */
    public void test07DeleteSelectedItems() {
        LogUtils.v(TAG, "test07DeleteSelectedItems");
        ListView mListView = (ListView) mTodosActivity
                .findViewById(com.mediatek.todos.R.id.list_todos);
        final TodosListAdapter mAdapter = (TodosListAdapter) mListView.getAdapter();
        final TestUtils utils = new TestUtils();
        if (mAdapter.getDonesDataSource().size() == 0 && mAdapter.getTodosDataSource().size() == 0) {
            return;
        }
        int viewNumber = mListView.getChildCount();
        int i = 0;
        while (i < viewNumber) {
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_TODOS_ITEM
                    || viewType == TodosListAdapter.TYPE_DONES_ITEM) {
                View view = mListView.getChildAt(i);
                clickLongOnView(view);
                break;
            }
            ++i;
        }
        final TodoInfo info = new TodoInfo();
        info.copy(mAdapter.getItem(i));
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.deleteSelectedItems();
                    // remove from ArrayList
                    TodoInfo temp = null;
                    if (utils.getAttribute(info, TodosInfoTest.STATUS) == TodoInfo.STATUS_TODO) {
                        int todoSize = mAdapter.getTodosDataSource().size();
                        for (int i = 0; i < todoSize; i++) {
                            temp = mAdapter.getTodosDataSource().get(i);
                            if (temp == info) {
                                assertTrue(false);
                            }
                        }
                    } else {
                        int doneSize = mAdapter.getDonesDataSource().size();
                        for (int i = 0; i < doneSize; i++) {
                            temp = mAdapter.getDonesDataSource().get(i);
                            if (temp == info) {
                                assertTrue(false);
                            }
                        }
                    }
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
        // remove from DB
        TodoInfo todoInfo = null;
        Context mContext = mTodosActivity.getApplicationContext();
        Cursor cursor = mContext.getContentResolver().query(TodosInfoTest.TODO_URI, null, null,
                null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                todoInfo = TodoInfo.makeTodoInfoFromCursor(cursor);
                LogUtils.v(TAG, "cursor info" + todoInfo);
                if (todoInfo == info) {
                    assertTrue(false);
                }
            } while (cursor.moveToNext());
        }
    }

    public void testSelectionIcon() {
        ListView mListView = (ListView) mTodosActivity
                .findViewById(com.mediatek.todos.R.id.list_todos);
        TodosListAdapter mAdapter = (TodosListAdapter) mListView.getAdapter();
        int viewNumber = mListView.getChildCount();
        for (int i = 0; i < viewNumber; i++) {
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_TODOS_ITEM
                    || viewType == TodosListAdapter.TYPE_DONES_ITEM) {
                View view = mListView.getChildAt(i);
                clickLongOnView(view);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                assertTrue(mAdapter.isEditing());

                if (TestUtils.isMenuItemEbable("mBtnSelectAll", mTodosActivity)) {
                    TestUtils.clickMenuItem(com.mediatek.todos.R.string.select_all, getInstrumentation());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }

                if (TestUtils.isMenuItemEbable("mBtnDeselectAll", mTodosActivity)) {
                    TestUtils.clickMenuItem(com.mediatek.todos.R.string.deselect_all, getInstrumentation());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
                break;
            }
        }
    }

    public void testExpandAction() {
        int viewNumber = mListView.getChildCount();
        for (int i = 0; i < viewNumber; i++) {
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_TODOS_HEADER) {
                View v = mListView.getChildAt(i);
                View expand = v.findViewById(com.mediatek.todos.R.id.btn_expand);
                if (expand != null) {
                    TouchUtils.clickView(this, expand);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {}
                    TouchUtils.clickView(this, expand);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {}
                }
                final int pos = i + 1;
                if (mAdapter.isTodosExpand()) {
                    try {
                        runTestOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.selectItem(pos, true);
                                mAdapter.selectItem(pos, false);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {}
                }
                break;
            }
        }

        viewNumber = mListView.getChildCount();
        for (int i = 0; i < viewNumber; i++) {
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_DONES_HEADER) {
                View v = mListView.getChildAt(i);
                View expand = v.findViewById(com.mediatek.todos.R.id.btn_expand);
                if (expand != null) {
                    TouchUtils.clickView(this, expand);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {}
                    TouchUtils.clickView(this, expand);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {}
                }
                final int pos = i + 1;
                if (mAdapter.isDonesExPand()) {
                    try {
                        runTestOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.selectItem(pos, true);
                                mAdapter.selectItem(pos, false);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {}
                }
                break;
            }
        }
    }

    public void testChangeStatusAction() {
        int viewNumber = mListView.getChildCount();
        for (int i = 0; i < viewNumber; i++) {
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_TODOS_ITEM) {
                View view = mListView.getChildAt(i);
                View changeStateIcon = (ImageView) view.findViewById(com.mediatek.todos.R.id.change_info_state);
                TouchUtils.clickView(this, changeStateIcon);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                break;
            }
        }
        for (int i = 0; i < viewNumber; i++) {
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_DONES_ITEM) {
                View view = mListView.getChildAt(i);
                View changeStateIcon = (ImageView) view
                        .findViewById(com.mediatek.todos.R.id.change_info_state);
                TouchUtils.clickView(this, changeStateIcon);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                break;
            }
        }

        for (int i = 0; i < viewNumber; i++) {
            int viewType = mAdapter.getItemViewType(i);
            if (viewType == TodosListAdapter.TYPE_TODOS_ITEM
                    || viewType == TodosListAdapter.TYPE_DONES_ITEM) {
                View view = mListView.getChildAt(i);
                clickLongOnView(view);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                assertTrue(mAdapter.isEditing());

                View selectAll = mTodosActivity
                        .findViewById(com.mediatek.todos.R.id.btn_select_all);
                if(selectAll.isEnabled()){
                    TouchUtils.clickView(this, selectAll);
                }
                View changeStatus = mTodosActivity
                        .findViewById(com.mediatek.todos.R.id.btn_change_state);
                if (changeStatus.isEnabled()) {
                    TouchUtils.clickView(this, changeStatus);
                }
            }
        }
    }

    public void testRemoveItem() {
        int viewNumber = mListView.getChildCount();
        for (int i = 0; i < viewNumber; i++) {
            final TodoInfo info = mAdapter.getItem(i);
            if (info != null) {
                try {
                    runTestOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.removeItem(info);
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }
        }
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.refreshData();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
    }

    private void clickLongOnView(final View view) {
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.performLongClick();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void getItemViewType(TodosListAdapter adapter, int viewNumber) {
        int todoNumber = adapter.getTodosDataSource().size();
        int doneNumber = adapter.getDonesDataSource().size();
        int todoShowSize = adapter.isTodosExpand() ? todoNumber : 0;

        int position = 0;
        while (position < viewNumber) {
            int viewType = adapter.getItemViewType(position);
            LogUtils.v(TAG, "position--" + position + "viewType--" + viewType);

            if (position == 0) {
                assertEquals(TodosListAdapter.TYPE_TODOS_HEADER, viewType);
            } else if (position == 1 && todoNumber == 0) {
                assertEquals(TodosListAdapter.TYPE_TODOS_FOOTER, viewType);
            } else if (position <= todoShowSize) {
                assertEquals(TodosListAdapter.TYPE_TODOS_ITEM, viewType);
            } else if ((todoNumber == 0 && position == 2)
                    || (todoNumber != 0 && position == todoShowSize + 1)) {
                assertEquals(TodosListAdapter.TYPE_DONES_HEADER, viewType);
            } else if (doneNumber == 0) {
                assertEquals(TodosListAdapter.TYPE_DONES_FOOTER, viewType);
            } else {
                assertEquals(TodosListAdapter.TYPE_DONES_ITEM, viewType);
            }
            position++;
        }
    }

    private void getItem(TodosListAdapter adapter, int viewNumber) {
        int position = 0;
        while (position < viewNumber) {
            TodoInfo info = adapter.getItem(position);
            int viewType = adapter.getItemViewType(position);
            LogUtils.v(TAG, "position" + position + " info" + info);
            // is this info correct
            if (viewType == TodosListAdapter.TYPE_TODOS_ITEM) {
                assertEquals(adapter.getTodosDataSource().get(position - 1), info);
            } else if (viewType == TodosListAdapter.TYPE_DONES_ITEM) {
                int todoShowNumber = adapter.isTodosExpand() ? adapter.getTodosDataSource().size()
                        : 0;
                if (adapter.getTodosDataSource().size() == 0) {
                    todoShowNumber = 1;
                }
                int index = position - todoShowNumber - 2;
                assertEquals(adapter.getDonesDataSource().get(index), info);
            } else {
                assertNull(info);
            }

            if (info != null) {
                adapter.getItemPosition(info);
            }

            position++;
        }
    }

    private void preCondition(TodosListAdapter adapter) {
        String title = null;
        String description = null;
        boolean needRefresh = false;

        TestUtils utils = new TestUtils();
        if (adapter.getTodosDataSource().size() == 0) {
            needRefresh = true;
            LogUtils.i(TAG, "adapter.getTodosDataSource().size() == 0");
            TodoInfo todoInfo = new TodoInfo();
            title = "ForAdapter insertTitle-Todo";
            description = "ForAdapter insertDescription-Todo";
            utils.setTitle(todoInfo, title);
            utils.setDescription(todoInfo, description);
            utils.updateStatus(todoInfo, TodoInfo.STATUS_TODO);
            adapter.startInsert(todoInfo);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {}
        }
        if (adapter.getDonesDataSource().size() == 0) {
            needRefresh = true;
            LogUtils.i(TAG, "adapter.getDonesDataSource().size() == 0");
            TodoInfo doneInfo = new TodoInfo();
            title = "ForAdapter insertTitle-Done";
            description = "ForAdapter insertDescription-Done";
            utils.setTitle(doneInfo, title);
            utils.setDescription(doneInfo, description);
            utils.updateStatus(doneInfo, TodoInfo.STATUS_DONE);
            adapter.startInsert(doneInfo);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {}
        }
        LogUtils.i(TAG, "needRefresh =" + needRefresh);
        if (needRefresh) {
            // update the mAdapter's datasource
            adapter.refreshData();
            while (adapter.getTodosDataSource().size() == 0
                    && adapter.getDonesDataSource().size() == 0) {
                LogUtils.w(TAG, "refreshData() - waiting for query complete");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
            }
        }
    }
}
