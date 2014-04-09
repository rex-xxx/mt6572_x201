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

package com.mediatek.cmmb.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.MBBMSStore.SG;
import com.mediatek.mbbms.ServerStatus;
import com.mediatek.notification.NotificationManagerPlus;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ContentSelectActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ContentSelectActivity";
    private static final boolean LOG = true;

    private static final String[] PROJECTION = new String[] { SG.PurchaseItemContent.GLOBAL_PURCHASE_ITEM_ID,
            SG.PurchaseItemContentDetail.PURCHASE_DATA_ID, SG.PurchaseItemContent.NAME,
            SG.PurchaseItemContentDetail.MONEY_INFO, SG.PurchaseItemContentDetail.START_TIME,
            SG.PurchaseItemContentDetail.END_TIME, SG.PurchaseItemContentDetail.GLOBAL_CONTENT_ID,
            SG.PurchaseItemContent.ID, SG.PurchaseItemContentDetail.PERIOD, };

    private static final int COLUMN_GLOBAL_ID = 0;
    private static final int COLUMN_PURCHASE_DATA_ID = 1;
    private static final int COLUMN_NAME = 2;
    private static final int COLUMN_MONEY_INFO = 3;
    private static final int COLUMN_START_TIME = 4;
    private static final int COLUMN_END_TIME = 5;
    private static final int COLUMN_CONTENT_GLOBAL_ID = 6;
    private static final int COLUMN_CONTENT_ROW_ID = 7;
    private static final int COLUMN_PERIOD = 8;

    private TextView mEmptyView;
    private OrderAdapter mListAdapter;
    private String mPeriodFormat;
    private Button mLeftButton;
    private Button mRightButton;
    private TextView mTotalPrice;
    private Cursor mSelectCursor;
    private Cursor mDisplayCursor;
    private LinearLayout mBottomPanel;
    private ListView mListView;
    private Handler mUIHandler;
    private ProgressDialog mProgressDialog;

    private static final int STEP_SELECT = 0;
    private static final int STEP_LIST = 1;
    private int mStep = STEP_SELECT;
    private ScrollPosition mLastSelectPosition;
    private boolean mNeedJump;
    private boolean mOnTop;
    private ModeSwitchManager mModeManager;
    private NotificationManagerPlus mNMP;
    private ServiceManager mServiceManager;

    private static final int MSG_SUBSCRIBE_PURCHASE_ONE_END = 1;
    private static final int MSG_SUBSCRIBE_PURCHASE_ALL_END = 2;

    private static final int MSG_RESULT_SUCCESS = 1;
    private static final int MSG_RESULT_FAILED = 0;

    private HashMap<Long, Boolean> mChecked = new HashMap<Long, Boolean>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.purchase_content);
        showSpinner();
        mPeriodFormat = getString(R.string.period_format);
        mListAdapter = new OrderAdapter(this, R.layout.purchase_content_item, null, new String[] {}, new int[] {});
        getListView().setAdapter(mListAdapter);

        mBottomPanel = (LinearLayout) findViewById(R.id.bottom_panel);
        mBottomPanel.setVisibility(View.GONE);
        mLeftButton = (Button) findViewById(R.id.btnLeft);
        mRightButton = (Button) findViewById(R.id.btnRight);
        mTotalPrice = (TextView) findViewById(R.id.totalPrice);
        mLeftButton.setOnClickListener(this);
        mRightButton.setOnClickListener(this);

        selectStep();

        mUIHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                int id = msg.what;
                int result = msg.arg1;
                if (LOG) {
                    Log.i(TAG, "handleMessage() id=" + id + ", result=" + result);
                }
                switch (id) {
                case MSG_SUBSCRIBE_PURCHASE_ONE_END:
                    Toast.makeText(ContentSelectActivity.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
                    break;
                case MSG_SUBSCRIBE_PURCHASE_ALL_END:
                    hideProgress();
                    if (result == MSG_RESULT_SUCCESS) {
                        // if success, go to main screen; if fail, do nothing
                        if (mOnTop) {
                            gotoMainScreen();
                        } else {
                            mNeedJump = true;
                        }
                    }
                    break;
                default:
                    Log.w(TAG, "wrong message for ui handler! msg=" + msg);
                    break;
                }
            }

        };
        mModeManager = new ModeSwitchManager(this, null, savedInstanceState);
        mNMP = new NotificationManagerPlus.ManagerBuilder(this).create();
        mServiceManager = ServiceManager.getServiceManager(this);
        if (LOG) {
            Log.i(TAG, "onCreate()");
        }
    }

    // will set mNeedJump to false
    private void gotoMainScreen() {
        Intent intent = new Intent();
        intent.setClass(this, MainScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        mNeedJump = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNMP.startListening();
        mOnTop = true;
        if (mNeedJump) {
            gotoMainScreen();
        }
        if (LOG) {
            Log.i(TAG, "onResume()");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNMP.stopListening();
        mOnTop = false;
        if (LOG) {
            Log.i(TAG, "onPause()");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mModeManager.onActivityStart();
        if (LOG) {
            Log.i(TAG, "onStart()");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mModeManager.onActivityStop();
        if (LOG) {
            Log.i(TAG, "onStop()");
        }
    }

    @Override
    protected void onDestroy() {
        if (mSelectCursor != null) {
            mSelectCursor.close();
            mSelectCursor = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mModeManager.onSaveInstanceState(outState);
    }

    private void updateDisplay() {
        if (LOG) {
            Log.i(TAG, "updateDisplay() mStep=" + mStep);
        }
        if (mStep == STEP_SELECT) {
            mLeftButton.setText(R.string.select_all);
            mRightButton.setText(R.string.display_ordered);
            mTotalPrice.setVisibility(View.GONE);
            mRightButton.setEnabled(true);
        } else if (mStep == STEP_LIST) {
            mLeftButton.setText(android.R.string.cancel);
            mRightButton.setText(R.string.subscribe);
            mTotalPrice.setText(getString(R.string.money_order_multi_month, mNondupPrice));
            mTotalPrice.setVisibility(View.VISIBLE);
            if (mDisplayCursor != null && mDisplayCursor.getCount() > 0) {
                mRightButton.setEnabled(true);
            } else {
                mRightButton.setEnabled(false);
            }
        } else {
            Log.w(TAG, "updateDisplay() wrong step");
        }
    }

    private void listStep() {
        if (LOG) {
            Log.i(TAG, "listStep() mSelectCursor=" + mSelectCursor);
        }
        if (mSelectCursor == null) {
            return;
        }
        ScrollPosition p = new ScrollPosition();
        p.firstVisiblePosition = getListView().getFirstVisiblePosition();
        View cv = getListView().getChildAt(0);
        if (cv != null) {
            p.firstTop = cv.getTop();
        }
        mLastSelectPosition = p;

        MatrixCursor temp = new MatrixCursor(PROJECTION);
        int position = mSelectCursor.getPosition();
        mNondupPurchaseList.clear();
        mNondupPrice = 0.0F;
        mSelectCursor.moveToPosition(-1);
        while (mSelectCursor.moveToNext()) {
            newRow(mSelectCursor, temp);
        }
        mSelectCursor.moveToPosition(position);
        mStep = STEP_LIST;
        mDisplayCursor = temp;
        mListAdapter.changeCursor(mDisplayCursor);
        updateDisplay();

        if (LOG) {
            Log.i(TAG, "listStep() mLastSelectPosition=" + mLastSelectPosition);
        }
    }

    private ArrayList<String> mNondupPurchaseList = new ArrayList<String>();
    private float mNondupPrice;

    private void newRow(Cursor sourceCursor, MatrixCursor target) {
        Long contentRowId = sourceCursor.getLong(COLUMN_CONTENT_ROW_ID);
        Boolean checked = mChecked.get(contentRowId);
        if (checked != null && checked.booleanValue()) {
            Object[] row = new Object[PROJECTION.length];
            String globalPurchaseId = sourceCursor.getString(COLUMN_GLOBAL_ID);
            row[COLUMN_GLOBAL_ID] = globalPurchaseId;
            row[COLUMN_PURCHASE_DATA_ID] = sourceCursor.getString(COLUMN_PURCHASE_DATA_ID);
            row[COLUMN_NAME] = sourceCursor.getString(COLUMN_NAME);
            row[COLUMN_MONEY_INFO] = sourceCursor.getString(COLUMN_MONEY_INFO);
            row[COLUMN_START_TIME] = sourceCursor.getLong(COLUMN_START_TIME);
            row[COLUMN_END_TIME] = sourceCursor.getLong(COLUMN_END_TIME);
            row[COLUMN_CONTENT_GLOBAL_ID] = sourceCursor.getString(COLUMN_CONTENT_GLOBAL_ID);
            row[COLUMN_CONTENT_ROW_ID] = sourceCursor.getLong(COLUMN_CONTENT_ROW_ID);
            row[COLUMN_PERIOD] = sourceCursor.getLong(COLUMN_PERIOD);
            target.addRow(row);
            if (!mNondupPurchaseList.contains(globalPurchaseId)) {
                mNondupPurchaseList.add(globalPurchaseId);
                mNondupPrice += MBBMSStore.parseMoneyString(sourceCursor.getString(COLUMN_MONEY_INFO), "CNY");
            }
            if (LOG) {
                Log.i(TAG, "newRow() globalPurchaseId=" + globalPurchaseId + ", mNondupPrice=" + mNondupPrice);
            }
        }
    }

    private void selectStep() {
        if (LOG) {
            Log.i(TAG, "selectStep() mSelectCursor=" + mSelectCursor);
        }
        mStep = STEP_SELECT;

        if (null == mSelectCursor) {
            mListAdapter.getQueryHandler().startQuery(0, null, SG.PurchaseItemContentDetail.CONTENT_URI, PROJECTION,
                    SG.PurchaseItemContentDetail.RESPONSE_CODE + "=? ", new String[] { "0" },
                    SG.PurchaseItemContentDetail.START_TIME);
        }
    }

    // here I simulate the behavior of ListActivity.
    private ListView getListView() {
        if (mListView == null) {
            mListView = (ListView) findViewById(android.R.id.list);
        }
        return mListView;
    }

    private TextView getEmptyView() {
        if (mEmptyView == null) {
            mEmptyView = (TextView) findViewById(android.R.id.empty);
        }
        return mEmptyView;
    }

    private void showProgress(int resId) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.setMessage(getString(resId));
        mProgressDialog.show();
    }

    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void showSpinner() {
        setProgressBarIndeterminateVisibility(true);
    }

    private void hideSpinner() {
        setProgressBarIndeterminateVisibility(false);
    }

    /* package */class OrderAdapter extends SimpleCursorAdapter {
        class QueryHandler extends AsyncQueryHandler {

            public QueryHandler(ContentResolver cr) {
                super(cr);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (LOG) {
                    Log.i(TAG, "onQueryComplete() cursor=" + cursor + ", mLastSelectPosition=" + mLastSelectPosition);
                }
                // change the list view and empty view's visibility
                if (cursor == null || cursor.getCount() == 0) {
                    getEmptyView().setVisibility(View.VISIBLE);
                    getListView().setVisibility(View.GONE);
                    OrderAdapter.this.changeCursor(cursor);
                    if (cursor != null) {
                        cursor.close();
                    }
                } else {
                    getListView().setVisibility(View.VISIBLE);
                    getEmptyView().setVisibility(View.GONE);
                    OrderAdapter.this.changeCursor(cursor);
                    getListView().setAdapter(OrderAdapter.this);

                    ScrollPosition p = mLastSelectPosition;
                    if (p != null) {
                        getListView().setSelectionFromTop(p.firstVisiblePosition, p.firstTop);
                        mLastSelectPosition = null;
                    }
                    mBottomPanel.setVisibility(View.VISIBLE);// show panel
                    mSelectCursor = cursor;// set cursor
                }
                hideSpinner();
                updateDisplay();

                if (LOG) {
                    Log.i(TAG, "onQueryComplete(" + token + "," + cookie + "," + cursor + ")");
                }
                if (LOG && cursor != null) {
                    Log.i(TAG, "onQueryComplete() cursor.count=" + cursor.getCount());
                }
            }

        }

        private QueryHandler mQueryHandler;
        private ContentResolver mContentResolver;

        public OrderAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
            mContentResolver = context.getContentResolver();
            mQueryHandler = new QueryHandler(mContentResolver);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ViewHolder holder = new ViewHolder();
            holder.nameView = (TextView) v.findViewById(R.id.tvName);
            holder.timeView = (TextView) v.findViewById(R.id.tvTime);
            holder.moneyInfoView = (TextView) v.findViewById(R.id.tvMoneyInfo);
            holder.ckbCheck = (CheckBox) v.findViewById(R.id.ckbSelect);
            v.setTag(holder);
            DataHolder dataHolder = new DataHolder();
            holder.ckbCheck.setTag(dataHolder);
            holder.ckbCheck.setOnClickListener(ContentSelectActivity.this);
            if (LOG) {
                Log.i(TAG, "newView()");
            }
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            DataHolder dataHolder = (DataHolder) holder.ckbCheck.getTag();
            dataHolder.purchaseGlobalId = cursor.getString(COLUMN_GLOBAL_ID);
            dataHolder.contentName = getLanguageString(cursor.getString(COLUMN_NAME));
            dataHolder.price = MBBMSStore.parseMoneyString(cursor.getString(COLUMN_MONEY_INFO), "CNY");
            dataHolder.purchaseDataId = cursor.getString(COLUMN_PURCHASE_DATA_ID);
            dataHolder.startTime = cursor.getLong(COLUMN_START_TIME);
            dataHolder.endTime = cursor.getLong(COLUMN_END_TIME);
            dataHolder.contentGlobalId = cursor.getString(COLUMN_CONTENT_GLOBAL_ID);
            dataHolder.contentRowId = cursor.getLong(COLUMN_CONTENT_ROW_ID);
            dataHolder.period = cursor.getInt(COLUMN_PERIOD);

            holder.nameView.setText(dataHolder.contentName);
            holder.timeView.setText(getFormattedTime(dataHolder.startTime, dataHolder.endTime));
            holder.moneyInfoView.setText(getMoney(MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES,
                    dataHolder.price, dataHolder.period));
            if (mStep == STEP_LIST) {
                holder.ckbCheck.setVisibility(View.GONE);
            } else {
                holder.ckbCheck.setVisibility(View.VISIBLE);
                holder.ckbCheck.setChecked(getChecked(dataHolder.contentRowId));
            }
            if (LOG) {
                Log.i(TAG, "bindView() dataHolder=" + dataHolder);
            }
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            if (LOG) {
                Log.i(TAG, "changeCursor(" + cursor + ")");
            }
        }

        public QueryHandler getQueryHandler() {
            return mQueryHandler;
        }

        private String getMoney(int subscribeType, float price, int period) {
            if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MONTH) {
                return String.format(getString(R.string.money_order_one_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MULTI_MONTH) {
                return String.format(getString(R.string.money_order_multi_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_SERVICE_MONTH) {
                return String.format(getString(R.string.money_order_one_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
                return String.format(getString(R.string.money_order_times), price, period);
            } else {
                return String.format(getString(R.string.money_unknown), price);
            }
        }
    }

    private class ViewHolder {
        public TextView nameView;
        public TextView moneyInfoView;
        public TextView timeView;
        public CheckBox ckbCheck;
    }

    private class DataHolder {
        public String contentGlobalId;
        public String purchaseDataId;
        public String purchaseGlobalId;
        public String contentName;
        public long startTime;
        public long endTime;
        public float price;
        public long contentRowId;
        public int period;

        @Override
        public String toString() {
            return new StringBuilder().append("DataHolder(mPurchaseGlobalId=").append(purchaseGlobalId).append(
                    ", contentGlobalId=").append(contentGlobalId).append(", mPurchaseDataId=").append(purchaseDataId)
                    .append(", contentRowId=").append(contentRowId).append(", contentName=").append(contentName).append(
                            ", price=").append(price).append(")").toString();
        }
    }

    private class ScrollPosition {
        public int firstVisiblePosition;
        public int firstTop;

        @Override
        public String toString() {
            return new StringBuilder().append("ScrollPosition(firstVisiblePosition=").append(firstVisiblePosition).append(
                    ", firstTop=").append(firstTop).append(")").toString();
        }
    }

    private static Date sStartTime = new Date();
    private static Date sEndTime = new Date();

    private String getFormattedTime(long startTime, long endTime) {
        sStartTime.setTime(startTime);
        sEndTime.setTime(endTime);
        return String.format(mPeriodFormat, sStartTime.getHours(), sStartTime.getMinutes(), sEndTime.getHours(), sEndTime
                .getMinutes());
    }

    private String getLanguageString(String allString) {
        String detail = MBBMSStore.parseLanguageString(allString, getCurrentLang());
        if (LOG) {
            Log.i(TAG, "getLanguageString() = " + detail);
        }
        return detail;
    }

    private String getCurrentLang() {
        return Utils.getCurrentLang();
    }

    /* package */boolean getChecked(Long contentRowId) {
        boolean result = false;
        if (mChecked.containsKey(contentRowId)) {
            result = mChecked.get(contentRowId);
        }
        if (LOG) {
            Log.i(TAG, "getChecked(" + contentRowId + ") return " + result);
        }
        return result;
    }

    /* package */void setChecked(Long contentRowId, boolean value) {
        boolean result = false;
        mChecked.put(contentRowId, value);
        if (LOG) {
            Log.i(TAG, "setChecked(" + contentRowId + ") return " + result);
        }
    }

    public void onClick(View v) {
        if (LOG) {
            Log.v(TAG, "onClick()");
        }
        switch (v.getId()) {
        case R.id.ckbSelect:
            DataHolder dataholder = (DataHolder) v.getTag();
            boolean old = getChecked(dataholder.contentRowId);
            ((CheckBox) v).setChecked(!old);
            setChecked(dataholder.contentRowId, !old);
            break;
        case R.id.btnLeft:
            if (mStep == STEP_SELECT) {
                // select all
                int position = mSelectCursor.getPosition();
                mSelectCursor.moveToPosition(-1);
                while (mSelectCursor.moveToNext()) {
                    mChecked.put(mSelectCursor.getLong(COLUMN_CONTENT_ROW_ID), true);
                }
                mSelectCursor.moveToPosition(position);
                mListAdapter.notifyDataSetChanged();
            } else {
                // move to select step
                selectStep();
            }
            break;
        case R.id.btnRight:
            if (mStep == STEP_SELECT) {
                // move to list step
                listStep();
            } else {
                // subscribe selected
                subscribeSelected();
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mStep == STEP_SELECT) {
            super.onBackPressed();
        } else {
            selectStep();
        }
    }

    private void subscribeSelected() {
        if (LOG) {
            Log.i(TAG, "subscribeSelected() mDisplayCursor=" + mDisplayCursor + ", mServiceManager=" + mServiceManager);
        }
        if (mDisplayCursor != null && mDisplayCursor.getCount() > 0 && mServiceManager != null) {
            mDisplayCursor.moveToPosition(-1);
            long now = Utils.getNow();
            String invalidName = "";
            String validName = "";
            ArrayList<PurchaseItem> list = new ArrayList<PurchaseItem>();
            while (mDisplayCursor.moveToNext()) {
                long contentRowId = mDisplayCursor.getLong(COLUMN_CONTENT_ROW_ID);
                String contentName = mDisplayCursor.getString(COLUMN_NAME);
                long endTime = mDisplayCursor.getLong(COLUMN_END_TIME);
                if (LOG) {
                    Log.i(TAG, "subscribeSelected() contentRowId=" + contentRowId + ", contentName=" + contentName
                            + ", endTime=" + endTime + ", now=" + now);
                }
                if (Utils.checkLocalTime() && now > endTime) {
                    invalidName += getLanguageString(contentName) + ", ";
                } else {
                    validName += getLanguageString(contentName) + ", ";
                    PurchaseItem item = new PurchaseItem(mDisplayCursor.getString(COLUMN_GLOBAL_ID), mDisplayCursor
                            .getString(COLUMN_PURCHASE_DATA_ID));
                    if (!list.contains(item)) {
                        list.add(item);
                    }
                }
            }
            if (invalidName != null && !"".equals(invalidName)) {
                try {
                    invalidName = invalidName.substring(0, invalidName.length() - 2);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                showMessage(invalidName);
            } else {
                try {
                    validName = validName.substring(0, validName.length() - 2);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                asyncSubscribePurchase(validName, list);
            }
        }
    }

    /* package */class PurchaseItem {
        PurchaseItem(String purGlobalId, String purDataId) {
            this.mPurchaseGlobalId = purGlobalId;
            this.mPurchaseDataId = purDataId;
        }

        String mPurchaseGlobalId;
        String mPurchaseDataId;

        @Override
        public boolean equals(Object o) {
            if (o instanceof PurchaseItem) {
                PurchaseItem right = (PurchaseItem) o;
                return equalString(mPurchaseGlobalId, right.mPurchaseGlobalId)
                        && equalString(mPurchaseDataId, right.mPurchaseDataId);
            }
            return false;
        }

        private boolean equalString(String l, String r) {
            if ((l == null && r == null) || (l != null && r != null && l.equals(r))) {
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            String key = mPurchaseGlobalId + "-" + mPurchaseDataId;
            return key.hashCode();
        }

        @Override
        public String toString() {
            return new StringBuilder().append("PurchaseItem(mPurchaseGlobalId=").append(mPurchaseGlobalId).append(
                    ", mPurchaseDataId=").append(mPurchaseDataId).append(")").toString();
        }
    }

    private void asyncSubscribePurchase(final String allName, final ArrayList<PurchaseItem> list) {
        showProgress(R.string.is_subscribing);
        new Thread(new Runnable() {

            public void run() {
                int size = list.size();
                int successCount = 0;
                for (int i = 0; i < size; i++) {
                    PurchaseItem item = list.get(i);
                    if (subscribePurchase(allName, item.mPurchaseGlobalId, item.mPurchaseDataId)) {
                        successCount++;
                    }
                }
                if (LOG) {
                    Log.i(TAG, "subscribePurchase() successCount=" + successCount);
                }
                if (successCount == 0) {
                    Message msg = mUIHandler.obtainMessage(MSG_SUBSCRIBE_PURCHASE_ALL_END);
                    msg.arg1 = MSG_RESULT_FAILED;
                    msg.sendToTarget();
                } else {
                    // success 1, go to main screen.
                    Message msg = mUIHandler.obtainMessage(MSG_SUBSCRIBE_PURCHASE_ALL_END);
                    msg.arg1 = MSG_RESULT_SUCCESS;
                    msg.sendToTarget();
                }
            }

        }).start();
    }

    // run in worker thread
    private boolean subscribePurchase(String allName, final String globalId, String purchaseDataId) {
        if (LOG) {
            Log.i(TAG, "subscribePurchase() start");
        }
        ServerStatus ret = mServiceManager.subscribe(globalId, purchaseDataId);
        Message msg = mUIHandler.obtainMessage(MSG_SUBSCRIBE_PURCHASE_ONE_END);
        if (Utils.isSuccess(ret)) {
            // here update database
            MBBMSStore.SG.ServiceDetail.updateSubscription(getContentResolver(), globalId, true);
            msg.arg1 = MSG_RESULT_SUCCESS;
            msg.obj = String.format(getString(R.string.subscribe_success), allName);
        } else {
            msg.arg1 = MSG_RESULT_FAILED;
            msg.obj = Utils.getErrorDescription(getResources(), ret, getString(R.string.subscribe_fail));
        }
        if (LOG) {
            Log.i(TAG, "subscribePurchase() msg.arg1=" + msg.arg1 + ", " + msg.obj);
        }
        msg.sendToTarget();
        if (LOG) {
            Log.i(TAG, "subscribePurchase() end return " + ret);
        }
        return Utils.isSuccess(ret);
    }

    private void showMessage(String contentName) {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setMessage(
                getString(R.string.fail_subscribe_for_end_program, contentName)).setTitle(R.string.mobile_tv)
                .setNegativeButton(android.R.string.cancel, null).create().show();
    }
}
