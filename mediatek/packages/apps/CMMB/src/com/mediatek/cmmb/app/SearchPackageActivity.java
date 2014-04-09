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
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.MBBMSStore.SG;
import com.mediatek.mbbms.ServerStatus;
import com.mediatek.notification.NotificationManagerPlus;

import java.util.HashMap;

public class SearchPackageActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "PackageActivity";
    private static final boolean LOG = true;

    private static final int PACKAGE_COLUMN_ID = 0;
    private static final int PACKAGE_COLUMN_PURCHASE_ITEM_ID = 1;
    private static final int PACKAGE_COLUMN_NAME = 2;
    private static final int PACKAGE_COLUMN_DESCRIPTION = 3;
    private static final int PACKAGE_COLUMN_SUBSCRIPTION_TYPE = 4;
    private static final int PACKAGE_COLUMN_PERIOD = 5;
    private static final int PACKAGE_COLUMN_MONEY_INFO = 6;
    private static final int PACKAGE_COLUMN_GLOBAL_PURCHASE_ITEM_ID = 7;
    private static final int PACKAGE_COLUMN_RESPONSE_CODE = 8;
    private static final int PACKAGE_COLUMN_PURCHASE_DATA_ID = 9;
    private static final int PACKAGE_COLUMN_IS_LOCAL = 10;

    private static final String[] SERVICE_PROJECTION = new String[] {
            MBBMSStore.SG.PurchaseItemService.GLOBAL_PURCHASE_ITEM_ID, MBBMSStore.SG.PurchaseItemService.SERVICE_ID,
            MBBMSStore.SG.PurchaseItemService.NAME };

    private static final String[] CONTENT_PROJECTION = new String[] {
            MBBMSStore.SG.PurchaseItemContent.GLOBAL_PURCHASE_ITEM_ID, MBBMSStore.SG.PurchaseItemContent.CONTENT_ID,
            MBBMSStore.SG.PurchaseItemContent.NAME };

    private static final int CHANNEL_COLUMN_GLOBAL_ID = 0;
    private static final int CHANNEL_COLUMN_ITEM_ID = 1;
    private static final int CHANNEL_COLUMN_ITEM_NAME = 2;

    private static final int MSG_RESULT_SUCCESS = 1;
    private static final int MSG_RESULT_FAILED = 0;

    private TextView mEmptyView;
    private ListView mListView;
    private PackageAdapter mAdapter;
    private TextView mNolocalInfo;
    private Cursor mLocalCursor;

    private HashMap<String, PackageItem> mServices;
    private HashMap<String, PackageItem> mContents;

    private Handler mUIHandler;
    private ProgressDialog mProgressDialog;

    private static final int MSG_SUBSCRIBE_PURCHASE_END = 1;
    private static final int MSG_UNSUBSCRIBE_PURCHASE_END = 2;

    private ContentResolver mContentResolver;
    private ModeSwitchManager mModeManager;
    private NotificationManagerPlus mNMP;
    private ServiceManager mServiceManager;

    private boolean mNeedJump;
    private boolean mOnTop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(savedInstanceState);
        if (LOG) {
            Log.i(TAG, "onCreate()");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clear();
        if (LOG) {
            Log.i(TAG, "onDestroy()");
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
    protected void onPause() {
        super.onPause();
        mNMP.stopListening();
        mOnTop = false;
        if (LOG) {
            Log.i(TAG, "onPause()");
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mModeManager.onSaveInstanceState(outState);
        if (LOG) {
            Log.i(TAG, "onSaveInstanceState()");
        }
    }

    private void clear() {
        mAdapter.changeCursor(null);
    }

    private void initialize(Bundle savedInstanceState) {
        setContentView(R.layout.purchase_search);
        Intent intent = getIntent();
        String query = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        }
        mListView = (ListView) findViewById(android.R.id.list);
        mEmptyView = (TextView) findViewById(android.R.id.empty);

        mContentResolver = getContentResolver();
        mNolocalInfo = new TextView(this);
        mNolocalInfo.setText(R.string.no_local_tip);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int padding = (int) (metrics.density * 10);
        mNolocalInfo.setPadding(padding, 0, padding, 0);
        mNolocalInfo.setVisibility(View.INVISIBLE);
        mListView.addHeaderView(mNolocalInfo);

        Cursor temp = Utils.getSearchCursor();
        MatrixCursor filterCursor = new MatrixCursor(PackageActivity.PACKAGE_PROJECTION);
        if (temp != null) {
            temp.moveToPosition(-1);
            while (temp.moveToNext()) {
                String globalId = temp.getString(PACKAGE_COLUMN_GLOBAL_PURCHASE_ITEM_ID);
                int subscriptionType = temp.getInt(PACKAGE_COLUMN_SUBSCRIPTION_TYPE);
                String serviceName = temp.getString(PACKAGE_COLUMN_NAME);
                String itemNames = getItemContaining(globalId, subscriptionType);
                if (itemNames == null) {
                    itemNames = serviceName;
                } else {
                    itemNames = serviceName + "," + itemNames;
                }
                boolean find = false;
                if (itemNames != null) {
                    String[] names = itemNames.split(",");
                    for (String name : names) {
                        if (name != null && name.indexOf(query) > -1) {
                            find = true;
                            break;
                        }
                    }
                }
                if (find) {
                    newRow(temp, filterCursor);
                }
            }
        }
        mLocalCursor = filterCursor;
        mAdapter = new PackageAdapter(this, R.layout.purchase_item, null, new String[] {}, new int[] {}, mListView,
                mEmptyView);
        mAdapter.changeCursor(mLocalCursor);
        if (LOG) {
            Log.i(TAG, "mLocalCursor.count=" + mLocalCursor.getCount());
        }
        // init ui handler
        mUIHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                int id = msg.what;
                int result = msg.arg1;
                if (LOG) {
                    Log.i(TAG, "handleMessage() id=" + id + ", result=" + result);
                }
                switch (id) {
                case MSG_SUBSCRIBE_PURCHASE_END:
                    hideProgress();
                    Toast.makeText(SearchPackageActivity.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
                    if (result == MSG_RESULT_SUCCESS) {
                        // if success, go to main screen; if fail, do nothing
                        if (mOnTop) {
                            gotoMainScreen();
                        } else {
                            mNeedJump = true;
                        }
                    }
                    break;
                case MSG_UNSUBSCRIBE_PURCHASE_END:
                    hideProgress();
                    Toast.makeText(SearchPackageActivity.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
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
        // new mode change manager
        mModeManager = new ModeSwitchManager(this, null, savedInstanceState);
        mNMP = new NotificationManagerPlus.ManagerBuilder(this).create();
        mServiceManager = ServiceManager.getServiceManager(this);
    }

    private void newRow(Cursor sourceCursor, MatrixCursor target) {
        Object[] row = new Object[PackageActivity.PACKAGE_PROJECTION.length];
        row[PACKAGE_COLUMN_ID] = sourceCursor.getString(PACKAGE_COLUMN_ID);
        row[PACKAGE_COLUMN_PURCHASE_ITEM_ID] = sourceCursor.getString(PACKAGE_COLUMN_PURCHASE_ITEM_ID);
        row[PACKAGE_COLUMN_NAME] = sourceCursor.getString(PACKAGE_COLUMN_NAME);
        row[PACKAGE_COLUMN_DESCRIPTION] = sourceCursor.getString(PACKAGE_COLUMN_DESCRIPTION);
        row[PACKAGE_COLUMN_SUBSCRIPTION_TYPE] = sourceCursor.getInt(PACKAGE_COLUMN_SUBSCRIPTION_TYPE);
        row[PACKAGE_COLUMN_PERIOD] = sourceCursor.getInt(PACKAGE_COLUMN_PERIOD);
        row[PACKAGE_COLUMN_MONEY_INFO] = sourceCursor.getString(PACKAGE_COLUMN_MONEY_INFO);
        row[PACKAGE_COLUMN_GLOBAL_PURCHASE_ITEM_ID] = sourceCursor.getString(PACKAGE_COLUMN_GLOBAL_PURCHASE_ITEM_ID);
        row[PACKAGE_COLUMN_RESPONSE_CODE] = sourceCursor.getInt(PACKAGE_COLUMN_RESPONSE_CODE);
        row[PACKAGE_COLUMN_PURCHASE_DATA_ID] = sourceCursor.getString(PACKAGE_COLUMN_PURCHASE_DATA_ID);
        row[PACKAGE_COLUMN_IS_LOCAL] = sourceCursor.getInt(PACKAGE_COLUMN_IS_LOCAL);
        target.addRow(row);
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

    /* package */class PackageAdapter extends SimpleCursorAdapter {

        private ContentResolver mContentResolver;
        private ListView mListView;
        private TextView mEmptyView;

        public PackageAdapter(Context context, int layout, Cursor c, String[] from, int[] to, ListView listView,
                TextView emptyView) {
            super(context, layout, c, from, to);
            mContentResolver = context.getContentResolver();
            mListView = listView;
            mEmptyView = emptyView;
            mListView.setAdapter(PackageAdapter.this);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ViewHolder holder = new ViewHolder();
            holder.nameView = (TextView) v.findViewById(R.id.tvName);
            holder.channelView = (TextView) v.findViewById(R.id.tvChannel);
            holder.periodView = (TextView) v.findViewById(R.id.tvPeriod);
            holder.moneyInfoView = (TextView) v.findViewById(R.id.tvMoneyInfo);
            holder.descriptionView = (TextView) v.findViewById(R.id.tvDescription);
            // holder.tipView = (TextView) v.findViewById(R.id.tvTip);
            holder.btnAction = (Button) v.findViewById(R.id.btnSubcribe);
            v.setTag(holder);
            DataHolder dataHolder = new DataHolder();
            holder.btnAction.setTag(dataHolder);
            holder.btnAction.setOnClickListener(SearchPackageActivity.this);
            if (LOG) {
                Log.i(TAG, "newView()");
            }
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            String globalId = cursor.getString(PACKAGE_COLUMN_GLOBAL_PURCHASE_ITEM_ID);
            int subscribeType = cursor.getInt(PACKAGE_COLUMN_SUBSCRIPTION_TYPE);
            int isLocal = cursor.getInt(PACKAGE_COLUMN_IS_LOCAL);
            float price;
            if (isLocal == 1) {
                price = MBBMSStore.parseMoneyString(cursor.getString(PACKAGE_COLUMN_MONEY_INFO), "CNY");
            } else {
                price = cursor.getFloat(PACKAGE_COLUMN_MONEY_INFO);
            }
            String packageName = cursor.getString(PACKAGE_COLUMN_NAME);
            int response = cursor.getInt(PACKAGE_COLUMN_RESPONSE_CODE);
            boolean isSubscribed = (response == 1 ? true : false);
            holder.nameView.setText(getPackageNameWithSuffix(isLocal, packageName, isSubscribed));
            holder.channelView.setText(getItemNames(globalId, subscribeType));
            holder.periodView.setText(getPeriod(subscribeType, cursor.getInt(PACKAGE_COLUMN_PERIOD)));
            holder.moneyInfoView.setText(getMoney(subscribeType, price));
            String description = getDescription(cursor.getString(PACKAGE_COLUMN_DESCRIPTION));
            if (description == null || "".equals(description.trim())) {
                holder.descriptionView.setVisibility(View.GONE);
            } else {
                holder.descriptionView.setText(description);
                holder.descriptionView.setVisibility(View.VISIBLE);
            }
            holder.btnAction.setText(getAction(isSubscribed));

            DataHolder dataHolder = (DataHolder) holder.btnAction.getTag();
            dataHolder.globalId = globalId;
            dataHolder.subscriptionType = subscribeType;
            dataHolder.isSubscribed = isSubscribed;
            dataHolder.price = price;
            dataHolder.purchaseDataId = cursor.getString(PACKAGE_COLUMN_PURCHASE_DATA_ID);
            dataHolder.packageName = getPackageName(isLocal, packageName);
            dataHolder.isLocal = isLocal;
            if (LOG) {
                Log.i(TAG, "bindView() dataHolder=" + dataHolder);
            }
        }

        @Override
        public void changeCursor(Cursor cursor) {
            // change the list view and empty view's visibility
            if (cursor == null || cursor.getCount() == 0) {
                mListView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
                mNolocalInfo.setVisibility(View.INVISIBLE);
            } else {
                mListView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                mNolocalInfo.setVisibility(View.VISIBLE);
            }
            super.changeCursor(cursor);
            if (LOG) {
                Log.i(TAG, "changeCursor(" + cursor + ")");
            }
        }

        private String getPackageName(int isLocal, String name) {
            if (isLocal == 1) {
                return getLanguageString(name);
            } else {
                return name;
            }
        }

        private String getPackageNameWithSuffix(int isLocal, String name, boolean isSubscribed) {
            int suffixId = R.string.unsubscribe_suffix;
            if (isSubscribed) {
                suffixId = R.string.subscribe_suffix;
            }
            return getPackageName(isLocal, name) + getString(suffixId);
        }

        private String getPeriod(int subscribeType, int peroid) {
            if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MONTH) {
                return SearchPackageActivity.this.getString(R.string.period_order_one_month);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MULTI_MONTH) {
                return String.format(SearchPackageActivity.this.getString(R.string.period_order_multi_month), peroid);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_SERVICE_MONTH) {
                return SearchPackageActivity.this.getString(R.string.period_order_one_month);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
                return String.format(SearchPackageActivity.this.getString(R.string.period_order_times), peroid);
            } else {
                return SearchPackageActivity.this.getString(R.string.period_unknown);
            }
        }

        private String getMoney(int subscribeType, float price) {
            if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MONTH) {
                return String.format(SearchPackageActivity.this.getString(R.string.money_order_one_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MULTI_MONTH) {
                return String.format(SearchPackageActivity.this.getString(R.string.money_order_multi_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_SERVICE_MONTH) {
                return String.format(SearchPackageActivity.this.getString(R.string.money_order_one_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
                return String.format(SearchPackageActivity.this.getString(R.string.money_order_multi_month), price);
            } else {
                return String.format(SearchPackageActivity.this.getString(R.string.money_unknown), price);
            }
        }

        private String getDescription(String description) {
            String result = null;
            result = getLanguageString(description);
            if (result == null) {
                // retry it
                result = getNonLanguageString(description);
            }
            if (LOG) {
                Log.i(TAG, "getDescription(" + description + ") return " + result);
            }
            return result;
        }

        private String getNonLanguageString(String description) {
            if (LOG) {
                Log.v(TAG, "getNonLanguageString(" + description + ")");
            }
            return description;
        }

        private String getTip(String packageGlobalId, int isLocal) {
            if (LOG) {
                Log.i(TAG, "getTip(" + packageGlobalId + ", " + isLocal + ")");
            }
            if (isLocal != 1) {
                // not local package
                return getString(R.string.no_local_tip);
            } else {
                return null;
            }
        }

        private String getAction(boolean isSubscribed) {
            if (isSubscribed) {
                return getString(R.string.unsubscribe);
            } else {
                return getString(R.string.subscribe);
            }
        }

    }

    private String getItemContaining(String packageGlobalId, int subscribeType) {
        if (LOG) {
            Log.i(TAG, "getItemNames(" + packageGlobalId + ", subscribeType=" + subscribeType);
        }
        if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
            return getContentContaining(packageGlobalId);
        } else {
            return getServiceContaining(packageGlobalId);
        }
    }

    private String getItemNames(String packageGlobalId, int subscribeType) {
        if (LOG) {
            Log.i(TAG, "getItemNames(" + packageGlobalId + ", subscribeType=" + subscribeType);
        }
        if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
            return getContentNames(packageGlobalId);
        } else {
            return getServiceNames(packageGlobalId);
        }
    }

    private String getContentContaining(String packageGlobalId) {
        if (mContents == null) {
            mContents = new HashMap<String, PackageItem>();
            Uri uri = null;
            uri = MBBMSStore.SG.PurchaseItemContent.CONTENT_URI;
            fillServicesOrContents(uri, CONTENT_PROJECTION, mContents, false);
            uri = MBBMSStore.SG.PurchaseItemContent.HISTORY_CONTENT_URI;
            fillServicesOrContents(uri, CONTENT_PROJECTION, mContents, true);
        }
        PackageItem content = mContents.get(packageGlobalId);
        return content == null ? null : content.name;
    }

    private String getContentNames(String packageGlobalId) {
        String contentNames = getContentContaining(packageGlobalId);
        if (contentNames == null) {
            contentNames = getString(R.string.unknown);
        }
        contentNames = getString(R.string.package_include_programs) + contentNames;
        if (LOG) {
            Log.i(TAG, "getContentNames(" + packageGlobalId + ") return " + contentNames);
        }
        return contentNames;
    }

    private String getServiceContaining(String packageGlobalId) {
        if (mServices == null) {
            mServices = new HashMap<String, PackageItem>();
            Uri uri = null;
            uri = MBBMSStore.SG.PurchaseItemService.CONTENT_URI;
            fillServicesOrContents(uri, SERVICE_PROJECTION, mServices, false);
            uri = MBBMSStore.SG.PurchaseItemService.HISTORY_CONTENT_URI;
            fillServicesOrContents(uri, SERVICE_PROJECTION, mServices, true);
        }
        PackageItem service = mServices.get(packageGlobalId);
        return service == null ? null : service.name;
    }

    private String getServiceNames(String packageGlobalId) {
        String serviceNames = getServiceContaining(packageGlobalId);
        if (serviceNames == null) {
            serviceNames = getString(R.string.unknown);
        }
        serviceNames = getString(R.string.package_include_channels) + serviceNames;
        if (LOG) {
            Log.i(TAG, "getServiceNames(" + packageGlobalId + ") return " + serviceNames);
        }
        return serviceNames;
    }

    private static final String SEPERATOR = ", ";

    private void fillServicesOrContents(Uri uri, String[] projection, HashMap<String, PackageItem> items, boolean isFilled) {
        // must recheck the service id
        HashMap<String, PackageItem> channelItems = items;
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, projection, null, null, null);
            if (cursor != null) {
                String globalId;
                String oldName;
                String name;
                PackageItem item;
                while (cursor.moveToNext()) {
                    globalId = cursor.getString(CHANNEL_COLUMN_GLOBAL_ID);
                    name = cursor.getString(CHANNEL_COLUMN_ITEM_NAME);
                    item = channelItems.get(globalId);
                    if (!isFilled) {
                        if (item == null) {
                            item = new PackageItem();
                        }
                        oldName = item.name;
                        if (oldName == null) {
                            oldName = getLanguageString(name);
                        } else {
                            oldName = oldName + SEPERATOR + getLanguageString(name);
                        }
                        item.name = oldName;
                        item.isFilled = true;
                        channelItems.put(globalId, item);
                    } else {
                        if (item == null) {
                            item = new PackageItem();
                        }
                        if (!item.isFilled) {
                            oldName = item.name;
                            if (oldName == null) {
                                oldName = getLanguageString(name);
                            } else {
                                oldName = oldName + SEPERATOR + getLanguageString(name);
                            }
                            item.name = oldName;
                            channelItems.put(globalId, item);
                        }
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private class ViewHolder {
        public TextView nameView;
        public TextView channelView;
        public TextView periodView;
        public TextView moneyInfoView;
        public TextView descriptionView;
        // TextView tipView;
        public Button btnAction;
    }

    private class DataHolder {
        public boolean isSubscribed;
        public String globalId;
        public float price;
        public String purchaseDataId;
        public String packageName;
        public int isLocal;
        public int subscriptionType;

        public DataHolder clone() {
            DataHolder newHolder = new DataHolder();
            newHolder.isSubscribed = isSubscribed;
            newHolder.globalId = globalId;
            newHolder.price = price;
            newHolder.purchaseDataId = purchaseDataId;
            newHolder.packageName = packageName;
            newHolder.isLocal = isLocal;
            newHolder.subscriptionType = subscriptionType;
            return newHolder;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("(isSubscribed=" + isSubscribed).append(", globalId=" + globalId).append(
                    ", price=" + price).append(", purchaseDataId=" + purchaseDataId).append(", name=" + packageName).append(
                    ", isLocal=" + isLocal).append(", subscriptionType=" + subscriptionType).append(")").toString();
        }
    }

    private class PackageItem {
        public boolean isFilled;
        public String name;
    }

    public void onClick(View v) {
        if (LOG) {
            Log.i(TAG, "onClick() v.id=" + v.getId());
        }
        AlertDialog dialog;
        int id = v.getId();
        switch (id) {
        case R.id.btnSubcribe:
            final DataHolder dataHolder = (DataHolder) v.getTag();
            String message;
            if (dataHolder.isSubscribed) {
                // unsubscribe it
                if (checkContentPass(dataHolder)) {
                    message = String.format(getString(R.string.unsubscribe_tip), dataHolder.packageName);
                    dialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle(
                            R.string.mobile_tv).setMessage(message).setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    showUnsubscribeProgress();
                                    asyncUnsubscribePurchase(dataHolder);
                                }
                            }).setNegativeButton(android.R.string.no, null).create();
                    dialog.show();
                }
            } else {
                // subscribe it
                message = String.format(getString(R.string.subscribe_tip), dataHolder.packageName, dataHolder.price);
                dialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle(
                        R.string.mobile_tv).setMessage(message).setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                showSubscribeProgress();
                                asyncSubscribePurchase(dataHolder);
                            }
                        }).setNegativeButton(android.R.string.no, null).create();
                dialog.show();
            }
            break;
        default:
            Log.w(TAG, "wrong view click. id=" + id);
            break;
        }

    }

    private boolean checkContentPass(DataHolder holder) {
        if (LOG) {
            Log.i(TAG, "checkContentPass(" + holder + ")");
        }
        if (Utils.checkLocalTime()
                && holder.subscriptionType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
            Cursor cursor = mContentResolver.query(SG.PurchaseItemContentDetail.CONTENT_URI, new String[] {
                    SG.PurchaseItemContentDetail.START_TIME, SG.PurchaseItemContent.NAME },
                    SG.PurchaseItem.GLOBAL_PURCHASE_ITEM_ID + "=? ", new String[] { holder.globalId }, null);
            boolean start = false;
            String contentName = null;
            if (cursor != null) {
                long now = Utils.getNow();
                while (cursor.moveToNext()) {
                    long startTime = cursor.getLong(0);
                    if (LOG) {
                        Log.i(TAG, "checkContentPass() startTime=" + startTime + ", now=" + now);
                    }
                    if (startTime < now) {
                        // has started
                        start = true;
                        contentName = cursor.getString(1);
                        break;
                    }
                }
                cursor.close();
            }
            if (start) {
                String message = getString(R.string.fail_unsubscribe_for_program_has_begun, getLanguageString(contentName));
                new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle(R.string.mobile_tv)
                        .setMessage(message).setPositiveButton(android.R.string.yes, null).create().show();
            }
            if (LOG) {
                Log.i(TAG, "checkContentPass() start=" + start + ", contentName=" + contentName);
            }
            return !start;
        } else {
            return true;
        }
    }

    private void showAccountProgress() {
        showProgress(R.string.query_account_info_ongoing);
    }

    private void showSubscribeProgress() {
        showProgress(R.string.is_subscribing);
    }

    private void showUnsubscribeProgress() {
        showProgress(R.string.is_unsubscribing);
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

    // run in worker thread
    private void subscribePurchase(DataHolder holder) {
        if (LOG) {
            Log.v(TAG, "subscribePurchase() start");
        }
        ServerStatus ret = mServiceManager.subscribe(holder.globalId, holder.purchaseDataId);
        Message msg = mUIHandler.obtainMessage(MSG_SUBSCRIBE_PURCHASE_END);
        if (Utils.isSuccess(ret)) {
            // update database
            MBBMSStore.SG.ServiceDetail.updateSubscription(mContentResolver, holder.globalId, true);
            msg.arg1 = MSG_RESULT_SUCCESS;
            msg.obj = Utils.getErrorDescription(getResources(), ret, String.format(getString(R.string.subscribe_success),
                    holder.packageName));
        } else {
            msg.arg1 = MSG_RESULT_FAILED;
            msg.obj = Utils.getErrorDescription(getResources(), ret, getString(R.string.subscribe_fail));
        }
        msg.sendToTarget();
        if (LOG) {
            Log.v(TAG, "subscribePurchase() msg.arg1=" + msg.arg1 + ", " + msg.obj);
        }
    }

    // run in worker thread
    private void unsubscribePurchase(DataHolder holder) {
        if (LOG) {
            Log.v(TAG, "unsubscribePurchase() start");
        }
        ServerStatus ret = mServiceManager.unsubscribe(holder.globalId, holder.purchaseDataId);
        Message msg = mUIHandler.obtainMessage(MSG_UNSUBSCRIBE_PURCHASE_END);
        if (Utils.isSuccess(ret)) {
            // update database
            MBBMSStore.SG.ServiceDetail.updateSubscription(mContentResolver, holder.globalId, false);
            msg.arg1 = MSG_RESULT_SUCCESS;
            msg.obj = Utils.getErrorDescription(getResources(), ret, String.format(getString(R.string.unsubscribe_success),
                    holder.packageName));
        } else {
            msg.arg1 = MSG_RESULT_FAILED;
            msg.obj = Utils.getErrorDescription(getResources(), ret, getString(R.string.unsubscribe_fail));
        }
        msg.sendToTarget();
        if (LOG) {
            Log.v(TAG, "unsubscribePurchase() msg.arg1=" + msg.arg1 + ", " + msg.obj);
        }
    }

    private void asyncUnsubscribePurchase(DataHolder dataHolder) {
        final DataHolder holder = dataHolder.clone();
        new Thread(new Runnable() {

            public void run() {
                unsubscribePurchase(holder);
            }

        }, "async-unsubscribe-thread").start();
    }

    private void asyncSubscribePurchase(DataHolder dataHolder) {
        final DataHolder holder = dataHolder.clone();
        new Thread(new Runnable() {

            public void run() {
                subscribePurchase(holder);
            }

        }, "async-subscribe-thread").start();
    }
}
