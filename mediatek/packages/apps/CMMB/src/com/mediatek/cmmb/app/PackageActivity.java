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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.ServerStatus;
import com.mediatek.mbbms.protocol.AccountResponse;
import com.mediatek.mbbms.protocol.MonetaryPrice;
import com.mediatek.mbbms.protocol.PackageItem;
import com.mediatek.mbbms.protocol.PriceInfo;
import com.mediatek.mbbms.protocol.PurchaseData;
import com.mediatek.mbbms.protocol.PurchaseDataFragment;
import com.mediatek.mbbms.protocol.PurchaseItem;
import com.mediatek.mbbms.protocol.PurchaseItemFragment;
import com.mediatek.notification.NotificationManagerPlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PackageActivity extends TabActivity implements View.OnClickListener {

    private static final String TAG = "PackageActivity";
    private static final boolean LOG = true;

    private static final String IS_LOCAL = "IsLocal";
    public static final String[] PACKAGE_PROJECTION = new String[] { MBBMSStore.SG.PurchaseItemDetail.ID, // 0
            MBBMSStore.SG.PurchaseItemDetail.PURCHASE_ITEM_ID, // 1
            MBBMSStore.SG.PurchaseItemDetail.NAME, // 2
            MBBMSStore.SG.PurchaseItemDetail.DESCRIPTION, // 3
            MBBMSStore.SG.PurchaseItemDetail.SUBSCRIPTION_TYPE, // 4
            MBBMSStore.SG.PurchaseItemDetail.PERIOD, // 5
            MBBMSStore.SG.PurchaseItemDetail.MONEY_INFO, // 6
            MBBMSStore.SG.PurchaseItemDetail.GLOBAL_PURCHASE_ITEM_ID, // 7
            MBBMSStore.SG.PurchaseItemDetail.RESPONSE_CODE, // 8
            MBBMSStore.SG.PurchaseItemDetail.PURCHASE_DATA_ID, // 9
            " 1 as " + IS_LOCAL };

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

    private static final String TAB_TYPE_SUBSCRIBED = "y";
    private static final String TAB_TYPE_UNSUBSCRIBED = "n";
    private static final int TAB_TYPE_SUBSCRIBED_INDEX = 0;
    private static final int TAB_TYPE_UNSUBSCRIBED_INDEX = 1;

    private static final int MSG_RESULT_SUCCESS = 1;
    private static final int MSG_RESULT_FAILED = 0;

    private TextView mSubscribedEmptyView;
    private TextView mUnsubscribedEmptyView;
    private ListView mSubscribedListView;
    private ListView mUnsubscribedListView;
    private PackageAdapter mSubscribedAdapter;
    private PackageAdapter mUnsubscribedAdapter;
    private TextView mNolocalInfo;

    private HashMap<String, PackageItem> mServices;
    private HashMap<String, PackageItem> mContents;

    private Handler mUIHandler;
    private ProgressDialog mProgressDialog;

    private static final int MSG_SUBSCRIBE_PURCHASE_END = 1;
    private static final int MSG_UNSUBSCRIBE_PURCHASE_END = 2;
    private static final int MSG_SYNC_PURCHASE_END = 3;
    private static final int MSG_UNSUBSCRIBE_ALL_PURCHASE_END = 4;

    private MatrixCursor mExternalCursor;
    private Cursor mLocalCursor;
    private Cursor mSubscribedCursor;
    private Cursor mUnsubscribedCursor;
    private ContentResolver mContentResolver;
    private ModeSwitchManager mModeManager;
    private ServiceManager mServiceManager;
    private NotificationManagerPlus mNMP;

    private static final String KEY_SYNC_SUCCESS = "sync_success";
    private static final String KEY_EXTERNAL_CURSOR = "external_cursor";
    private static final int TRY_TIMES = 10000;
    private boolean mSyncSuccess;
    private Bundle mSaveState;
    private boolean mNeedJump;
    private boolean mOnTop;
    private boolean mDestroyed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDestroyed = false;
        super.onCreate(savedInstanceState);
        initialize(savedInstanceState);
        if (LOG) {
            Log.v(TAG, "onCreate()");
        }
    }

    @Override
    protected void onDestroy() {
        mDestroyed = true;
        super.onDestroy();
        clear();
        if (LOG) {
            Log.v(TAG, "onDestroy()");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mModeManager.onActivityStart();
        if (LOG) {
            Log.v(TAG, "onStart()");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mModeManager.onActivityStop();
        if (LOG) {
            Log.v(TAG, "onStop()");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNMP.stopListening();
        mOnTop = false;
        if (LOG) {
            Log.v(TAG, "onPause()");
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
            Log.v(TAG, "onResume()");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SYNC_SUCCESS, mSyncSuccess);
        outState.putBundle(KEY_EXTERNAL_CURSOR, mSaveState);
        mModeManager.onSaveInstanceState(outState);
        if (LOG) {
            Log.v(TAG, "onSaveInstanceState() mSyncSuccess=" + mSyncSuccess + ", mSaveState=" + mSaveState);
        }
    }

    private void clear() {
        mSubscribedAdapter.changeCursor(null);
        mUnsubscribedAdapter.changeCursor(null);
        // if local cursor and external are not merged,
        // may be cursor will not be closed, if we exit this activity.
        if (mSubscribedCursor != null) {
            mSubscribedCursor.close();
        }
        if (mUnsubscribedCursor != null) {
            mUnsubscribedCursor.close();
        }
        Utils.setSearchCursor(null);
    }

    private void initialize(Bundle savedInstanceState) {
        mContentResolver = getContentResolver();
        TabHost tabHost = getTabHost();
        LayoutInflater.from(this).inflate(R.layout.purchase, tabHost.getTabContentView(), true);
        tabHost.addTab(tabHost.newTabSpec(TAB_TYPE_SUBSCRIBED).setIndicator(getString(R.string.subscribed_package),
                getResources().getDrawable(R.drawable.ic_package_tab_yes)).setContent(R.id.package_subscribed_tab));
        tabHost.addTab(tabHost.newTabSpec(TAB_TYPE_UNSUBSCRIBED).setIndicator(getString(R.string.unsubscribed_package),
                getResources().getDrawable(R.drawable.ic_package_tab_no)).setContent(R.id.package_unsubscribed_tab));

        // init view
        mSubscribedEmptyView = (TextView) tabHost.findViewById(R.id.package_subscribed_info);
        mUnsubscribedEmptyView = (TextView) tabHost.findViewById(R.id.package_unsubscribed_info);
        mSubscribedListView = (ListView) tabHost.findViewById(R.id.package_subscribed_list);
        mUnsubscribedListView = (ListView) tabHost.findViewById(R.id.package_unsubscribed_list);
        mNolocalInfo = new TextView(this);
        mNolocalInfo.setText(R.string.no_local_tip);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int padding = (int) (metrics.density * 10);
        mNolocalInfo.setPadding(padding, 0, padding, 0);
        mNolocalInfo.setVisibility(View.INVISIBLE);
        mSubscribedListView.addHeaderView(mNolocalInfo);
        // identify user's info
        boolean isSubscribed = getIntent().getBooleanExtra(MBBMSStore.SG.Service.IS_SUBSCRIBED, true);
        int selectedIndex = isSubscribed ? TAB_TYPE_SUBSCRIBED_INDEX : TAB_TYPE_UNSUBSCRIBED_INDEX;
        tabHost.setCurrentTab(selectedIndex);
        // init ui handler
        mUIHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                int id = msg.what;
                int result = msg.arg1;
                if (LOG) {
                    Log.v(TAG, "handleMessage() id=" + id + ", result=" + result);
                }
                if (mDestroyed) {
                    return;
                }
                switch (id) {
                case MSG_SUBSCRIBE_PURCHASE_END:
                    hideProgress();
                    Toast.makeText(PackageActivity.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(PackageActivity.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
                    if (result == MSG_RESULT_SUCCESS) {
                        // if success, go to main screen; if fail, do nothing
                        if (mOnTop) {
                            gotoMainScreen();
                        } else {
                            mNeedJump = true;
                        }
                    }
                    break;
                case MSG_SYNC_PURCHASE_END:
                    // In all case, we should refresh purchaseitem.
                    refreshPurchaseItems();
                    hideProgress();
                    if (msg.obj != null) {
                        Toast.makeText(PackageActivity.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MSG_UNSUBSCRIBE_ALL_PURCHASE_END:
                    hideProgress();
                    Toast.makeText(PackageActivity.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
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
        mServiceManager = ServiceManager.getServiceManager(this);
        // new mode change manager
        mModeManager = new ModeSwitchManager(this, null, savedInstanceState);
        mNMP = new NotificationManagerPlus.ManagerBuilder(this).create();
        if (savedInstanceState == null) {
            showAccountProgress();
            refreshPurchase(true);
            if (LOG) {
                Log.v(TAG, "Not from killed, do normal process.");
            }
        } else {
            mSyncSuccess = savedInstanceState.getBoolean(KEY_SYNC_SUCCESS);
            mSaveState = savedInstanceState.getBundle(KEY_EXTERNAL_CURSOR);
            refreshPurchase(false);
            if (LOG) {
                Log.v(TAG, "From killed, so do not sync account info");
            }
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

    private void refreshPurchase(boolean syncExternal) {
        initSubscribedPackage();
        initUnsubscribedPackage();
        asyncSyncPurchase(syncExternal);
    }

    private void initSubscribedPackage() {
        if (mSubscribedAdapter == null) {
            mSubscribedAdapter = new PackageAdapter(this, R.layout.purchase_item, null, new String[] {}, new int[] {},
                    mSubscribedListView, mSubscribedEmptyView);
        }
    }

    private void initUnsubscribedPackage() {
        if (mUnsubscribedAdapter == null) {
            mUnsubscribedAdapter = new PackageAdapter(this, R.layout.purchase_item, null, new String[] {}, new int[] {},
                    mUnsubscribedListView, mUnsubscribedEmptyView);
        }
    }

    private String getLanguageString(String allString) {
        String detail = MBBMSStore.parseLanguageString(allString, getCurrentLang());
        if (LOG) {
            Log.v(TAG, "getLanguageString() = " + detail);
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
            holder.mNameView = (TextView) v.findViewById(R.id.tvName);
            holder.mChannelView = (TextView) v.findViewById(R.id.tvChannel);
            holder.mPeriodView = (TextView) v.findViewById(R.id.tvPeriod);
            holder.mMoneyInfoView = (TextView) v.findViewById(R.id.tvMoneyInfo);
            holder.mDescriptionView = (TextView) v.findViewById(R.id.tvDescription);
            // holder.tipView = (TextView) v.findViewById(R.id.tvTip);
            holder.mBtnAction = (Button) v.findViewById(R.id.btnSubcribe);
            v.setTag(holder);
            DataHolder dataHolder = new DataHolder();
            holder.mBtnAction.setTag(dataHolder);
            holder.mBtnAction.setOnClickListener(PackageActivity.this);
            if (LOG) {
                Log.v(TAG, "newView()");
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
            holder.mNameView.setText(getPackageNameWithSuffix(isLocal, packageName, isSubscribed));
            holder.mChannelView.setText(getItemNames(globalId, subscribeType));
            holder.mPeriodView.setText(getPeriod(subscribeType, cursor.getInt(PACKAGE_COLUMN_PERIOD)));
            holder.mMoneyInfoView.setText(getMoney(subscribeType, price));
            String description = getDescription(cursor.getString(PACKAGE_COLUMN_DESCRIPTION));
            if (description == null || "".equals(description.trim())) {
                holder.mDescriptionView.setVisibility(View.GONE);
            } else {
                holder.mDescriptionView.setText(description);
                holder.mDescriptionView.setVisibility(View.VISIBLE);
            }
            /*
             * String tip = getTip(globalId, isLocal ); if (tip == null ||
             * "".equals(tip)) { holder.tipView.setVisibility(View.GONE); } else
             * { holder.tipView.setText(tip);
             * holder.tipView.setVisibility(View.VISIBLE); }
             */
            holder.mBtnAction.setText(getAction(isSubscribed));
            holder.mBtnAction.setEnabled(mSyncSuccess);

            DataHolder dataHolder = (DataHolder) holder.mBtnAction.getTag();
            dataHolder.mGlobalId = globalId;
            dataHolder.mSubscriptionType = subscribeType;
            dataHolder.mIsSubscribed = isSubscribed;
            dataHolder.mPrice = price;
            dataHolder.mPurchaseDataId = cursor.getString(PACKAGE_COLUMN_PURCHASE_DATA_ID);
            dataHolder.mPackageName = getPackageName(isLocal, packageName);
            dataHolder.mIsLocal = isLocal;
            if (LOG) {
                Log.v(TAG, "bindView() dataHolder=" + dataHolder);
            }
        }

        @Override
        public void changeCursor(Cursor cursor) {
            // change the list view and empty view's visibility
            if (cursor == null || cursor.getCount() == 0) {
                mListView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
                if (mListView == mSubscribedListView) {
                    mNolocalInfo.setVisibility(View.INVISIBLE);
                }
            } else {
                mListView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                if (mListView == mSubscribedListView) {
                    mNolocalInfo.setVisibility(View.VISIBLE);
                }
            }
            super.changeCursor(cursor);
            if (LOG) {
                Log.v(TAG, "changeCursor(" + cursor + ")");
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
                return PackageActivity.this.getString(R.string.period_order_one_month);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MULTI_MONTH) {
                return String.format(PackageActivity.this.getString(R.string.period_order_multi_month), peroid);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_SERVICE_MONTH) {
                return PackageActivity.this.getString(R.string.period_order_one_month);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
                return String.format(PackageActivity.this.getString(R.string.period_order_times), peroid);
            } else {
                return PackageActivity.this.getString(R.string.period_unknown);
            }
        }

        private String getMoney(int subscribeType, float price) {
            if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MONTH) {
                return String.format(PackageActivity.this.getString(R.string.money_order_one_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_PURCHASEITEM_MULTI_MONTH) {
                return String.format(PackageActivity.this.getString(R.string.money_order_multi_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_SERVICE_MONTH) {
                return String.format(PackageActivity.this.getString(R.string.money_order_one_month), price);
            } else if (subscribeType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
                return String.format(PackageActivity.this.getString(R.string.money_order_multi_month), price);
            } else {
                return String.format(PackageActivity.this.getString(R.string.money_unknown), price);
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
                Log.v(TAG, "getDescription(" + description + ") return " + result);
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
                Log.v(TAG, "getTip(" + packageGlobalId + ", " + isLocal + ")");
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

        private String getContentNames(String packageGlobalId) {
            if (mContents == null) {
                mContents = new HashMap<String, PackageItem>();
                Uri uri = null;
                uri = MBBMSStore.SG.PurchaseItemContent.CONTENT_URI;
                fillServicesOrContents(uri, CONTENT_PROJECTION, mContents, false);
                uri = MBBMSStore.SG.PurchaseItemContent.HISTORY_CONTENT_URI;
                fillServicesOrContents(uri, CONTENT_PROJECTION, mContents, true);
            }
            PackageItem content = mContents.get(packageGlobalId);
            String contentNames = content == null ? getString(R.string.unknown) : content.name;
            contentNames = getString(R.string.package_include_programs) + contentNames;
            if (LOG) {
                Log.i(TAG, "getContentNames(" + packageGlobalId + ") return " + contentNames);
            }
            return contentNames;
        }

        private String getServiceNames(String packageGlobalId) {
            if (mServices == null) {
                mServices = new HashMap<String, PackageItem>();
                Uri uri = null;
                uri = MBBMSStore.SG.PurchaseItemService.CONTENT_URI;
                fillServicesOrContents(uri, SERVICE_PROJECTION, mServices, false);
                uri = MBBMSStore.SG.PurchaseItemService.HISTORY_CONTENT_URI;
                fillServicesOrContents(uri, SERVICE_PROJECTION, mServices, true);
            }
            PackageItem service = mServices.get(packageGlobalId);
            String serviceNames = service == null ? getString(R.string.unknown) : service.name;
            serviceNames = getString(R.string.package_include_channels) + serviceNames;
            if (LOG) {
                Log.v(TAG, "getServiceNames(" + packageGlobalId + ") return " + serviceNames);
            }
            return serviceNames;
        }

        private static final String SEPERATOR = ", ";

        private void fillServicesOrContents(Uri uri, String[] projection, HashMap<String, PackageItem> items,
                boolean isFilled) {
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
    }

    /* package */class ViewHolder {
        TextView mNameView;
        TextView mChannelView;
        TextView mPeriodView;
        TextView mMoneyInfoView;
        TextView mDescriptionView;
        // TextView tipView;
        Button mBtnAction;
    }

    /* package */class DataHolder {
        boolean mIsSubscribed;
        String mGlobalId;
        float mPrice;
        String mPurchaseDataId;
        String mPackageName;
        int mIsLocal;
        int mSubscriptionType;

        public DataHolder clone() {
            DataHolder newHolder = new DataHolder();
            newHolder.mIsSubscribed = mIsSubscribed;
            newHolder.mGlobalId = mGlobalId;
            newHolder.mPrice = mPrice;
            newHolder.mPurchaseDataId = mPurchaseDataId;
            newHolder.mPackageName = mPackageName;
            newHolder.mIsLocal = mIsLocal;
            newHolder.mSubscriptionType = mSubscriptionType;
            return newHolder;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("(isSubscribed=" + mIsSubscribed).append(", globalId=" + mGlobalId).append(
                    ", price=" + mPrice).append(", purchaseDataId=" + mPurchaseDataId).append(", name=" + mPackageName)
                    .append(", isLocal=" + mIsLocal).append(", subscriptionType=" + mSubscriptionType).append(")")
                    .toString();
        }
    }

    private class PackageItem {
        public boolean isFilled;
        public String name;
    }

    public void onClick(View v) {
        if (LOG) {
            Log.v(TAG, "onClick() v.id=" + v.getId());
        }
        AlertDialog dialog;
        int id = v.getId();
        switch (id) {
        case R.id.btnSubcribe:
            final DataHolder dataHolder = (DataHolder) v.getTag();
            String message;
            if (dataHolder.mIsSubscribed) {
                // unsubscribe it
                if (checkContentPass(dataHolder)) {
                    message = String.format(getString(R.string.unsubscribe_tip), dataHolder.mPackageName);
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
                message = String.format(getString(R.string.subscribe_tip), dataHolder.mPackageName);
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
                && holder.mSubscriptionType == MBBMSStore.SG.PurchaseData.SUBSCRIPTION_TYPE_ENUM_CONTENT_TIMES) {
            Cursor cursor = mContentResolver.query(MBBMSStore.SG.PurchaseItemContentDetail.CONTENT_URI, new String[] {
                    MBBMSStore.SG.PurchaseItemContentDetail.START_TIME, MBBMSStore.SG.PurchaseItemContent.NAME },
                    MBBMSStore.SG.PurchaseItem.GLOBAL_PURCHASE_ITEM_ID + "=? ", new String[] { holder.mGlobalId }, null);
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

    private void syncPurchase(boolean syncExternal) {
        if (LOG) {
            Log.v(TAG, "syncPurchase(" + syncExternal + ") mSyncSuccess=" + mSyncSuccess + ", mSaveState=" + mSaveState);
        }
        Message msg = mUIHandler.obtainMessage(MSG_SYNC_PURCHASE_END);
        if (!syncExternal) {
            // here rebuild the mExternalCursor from mSaveState
            // do not clear mSaveState, because we do not update it.
            if (mSyncSuccess && mSaveState != null) {
                int colSize = PACKAGE_PROJECTION.length;
                for (int i = 0; i < TRY_TIMES; i++) {
                    Bundle row = mSaveState.getBundle(String.valueOf(i));
                    if (row == null) {
                        if (LOG) {
                            Log.v(TAG, "syncPurchase() current row(" + i + ") has no data.");
                        }
                        break;
                    }
                    if (mExternalCursor == null) {
                        mExternalCursor = new MatrixCursor(PACKAGE_PROJECTION);
                    }
                    Object[] colValues = new Object[colSize];
                    colValues[0] = row.getInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_ID]);
                    colValues[1] = row.getString(PACKAGE_PROJECTION[PACKAGE_COLUMN_PURCHASE_ITEM_ID]);
                    colValues[2] = row.getString(PACKAGE_PROJECTION[PACKAGE_COLUMN_NAME]);
                    colValues[3] = row.getString(PACKAGE_PROJECTION[PACKAGE_COLUMN_DESCRIPTION]);
                    colValues[4] = row.getInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_SUBSCRIPTION_TYPE]);
                    colValues[5] = row.getInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_PERIOD]);
                    colValues[6] = row.getFloat(PACKAGE_PROJECTION[PACKAGE_COLUMN_MONEY_INFO]);
                    colValues[7] = row.getString(PACKAGE_PROJECTION[PACKAGE_COLUMN_GLOBAL_PURCHASE_ITEM_ID]);
                    colValues[8] = row.getInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_RESPONSE_CODE]);
                    colValues[9] = row.getString(PACKAGE_PROJECTION[PACKAGE_COLUMN_PURCHASE_DATA_ID]);
                    colValues[10] = row.getInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_IS_LOCAL]);
                    mExternalCursor.addRow(colValues);
                }
            } else {
                // not sync successs, so there is no external cursor.
                mExternalCursor = null;
            }
            reloadPurchaseItems();
            // if not sync external, onCreate will make mSyncSuccess right.
            msg.arg1 = mSyncSuccess ? MSG_RESULT_SUCCESS : MSG_RESULT_FAILED;
            msg.sendToTarget();
            if (LOG) {
                Log.v(TAG, "syncPurchase() end. msg.arg1=" + msg.arg1 + ", " + msg.obj);
            }
            return;
        }
        mSaveState = null;// clear the save state
        ServerStatus ret = mServiceManager.processAccountInquiry();
        if (Utils.isSuccess(ret)) {
            resetPurchase();// clear local info to sync all from server
            AccountResponse accountInfo = mServiceManager.getAccountInfo();
            List<PurchaseItem> purchaseItems = accountInfo.getPurchaseItems();
            int size = purchaseItems.size();
            int[] isLocal = null;
            if (size > 0) {
                if (LOG) {
                    Log.v(TAG, "sync purchaseItems size=" + size);
                }
                mExternalCursor = new MatrixCursor(PACKAGE_PROJECTION, size);
                isLocal = new int[size];
                int colSize = PACKAGE_PROJECTION.length;
                int externalCount = 0;
                for (int i = 0; i < size; i++) {
                    PurchaseItem item = purchaseItems.get(i);
                    if (item == null) {
                        continue;// wrong data
                    }
                    String globalId = item.getGlobalRef();
                    isLocal[i] = checkIsLocalAndUpdate(globalId, 1);
                    if (LOG) {
                        Log.v(TAG, "globalId=" + globalId + ", isLocal[" + i + "]=" + isLocal[i]);
                    }
                    if (isLocal[i] <= 0) {
                        // not local
                        PurchaseItemFragment itemFragment = item.getPurchaseItemFragment();
                        String name = null;
                        String purchaseItemId = null;
                        String globalPurchaseItemId = null;
                        if (itemFragment != null) {
                            purchaseItemId = itemFragment.getId();
                            globalPurchaseItemId = itemFragment.getGlobalPurchaseItemId();
                            // Sets purchaseItem name.
                            List<String> nameList = itemFragment.getNameList();
                            if (nameList != null) {
                                name = nameList.get(0);
                            }
                        }
                        PurchaseData itemData = item.getPurchaseData();
                        String purchaseDataId = null;
                        PriceInfo priceInfo = null;
                        int period = 0;
                        float price = 0f;
                        int subscribeType = -1;
                        if (itemData != null) {
                            purchaseDataId = itemData.getIdRef();
                            // set money info
                            PurchaseDataFragment dataFragment = itemData.getPurchaseDataFragment();
                            if (dataFragment != null) {
                                priceInfo = dataFragment.getPriceInfo();
                            }
                            // Sets purchaseItem period. Only the purchaseItem
                            // that can be
                            // ordered by multiple-month has this info.
                            if (priceInfo != null) {
                                try {
                                    period = Integer.parseInt(priceInfo.getPeriod());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                                // Sets purchaseItem price info.
                                List<MonetaryPrice> moneyList = priceInfo.getPrices();
                                try {
                                    price = Float.parseFloat(moneyList.get(0).getPrice());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                                // Set purchaseItem subscription type.
                                try {
                                    subscribeType = Integer.parseInt(priceInfo.getSubscription_type());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        // set description
                        String description = null;
                        if (itemFragment != null) {
                            ArrayList<String> descriptions = itemFragment.getDescriptionList();
                            int dsize = -1;
                            if (descriptions != null) {
                                dsize = descriptions.size();
                                for(int di = 0; di < dsize; di++) {
                                    description = descriptions.get(di);
                                    if (description != null && !"".equals(description.trim())) {
                                        // get the valid descriptioin.
                                        break;
                                    }
                                }
                            }
                            if (LOG) {
                                Log.v(TAG, "can get description=" + description + ", descriptions size=" + dsize);
                            }
                        } else {
                            if (LOG) {
                                Log.v(TAG, "cann't get description for itemFragment is null.");
                            }
                        }

                        Object[] colValues = new Object[colSize];
                        colValues[0] = -i;// PACKAGE_COLUMN_ID
                        colValues[1] = purchaseItemId;// PACKAGE_COLUMN_PURCHASE_ITEM_ID
                        colValues[2] = name;// PACKAGE_COLUMN_NAME //special
                        colValues[3] = description;// PACKAGE_COLUMN_DESCRIPTION
                        colValues[4] = subscribeType;// PACKAGE_COLUMN_SUBSCRIPTION_TYPE
                        colValues[5] = period;// PACKAGE_COLUMN_PERIOD
                        colValues[6] = price;// PACKAGE_COLUMN_MONEY_INFO
                        // //special
                        colValues[7] = globalPurchaseItemId;// PACKAGE_COLUMN_GLOBAL_PURCHASE_ITEM_ID
                        colValues[8] = 1;// PACKAGE_COLUMN_RESPONSE_CODE
                        colValues[9] = purchaseDataId;// PACKAGE_COLUMN_PURCHASE_DATA_ID
                        colValues[10] = 0;// PACKAGE_COLUMN_IS_LOCAL
                        mExternalCursor.addRow(colValues);
                        // save to bundle for serialize the external cursor
                        Bundle row = new Bundle();
                        row.putInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_ID], -i);
                        row.putString(PACKAGE_PROJECTION[PACKAGE_COLUMN_PURCHASE_ITEM_ID], purchaseItemId);
                        row.putString(PACKAGE_PROJECTION[PACKAGE_COLUMN_NAME], name);
                        row.putString(PACKAGE_PROJECTION[PACKAGE_COLUMN_DESCRIPTION], description);
                        row.putInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_SUBSCRIPTION_TYPE], subscribeType);
                        row.putInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_PERIOD], period);
                        row.putFloat(PACKAGE_PROJECTION[PACKAGE_COLUMN_MONEY_INFO], price);
                        row.putString(PACKAGE_PROJECTION[PACKAGE_COLUMN_GLOBAL_PURCHASE_ITEM_ID], globalPurchaseItemId);
                        row.putInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_RESPONSE_CODE], 1);
                        row.putString(PACKAGE_PROJECTION[PACKAGE_COLUMN_PURCHASE_DATA_ID], purchaseDataId);
                        row.putInt(PACKAGE_PROJECTION[PACKAGE_COLUMN_IS_LOCAL], 0);
                        if (mSaveState == null) {
                            mSaveState = new Bundle();
                        }
                        mSaveState.putBundle(String.valueOf(externalCount), row);
                        if (LOG) {
                            Log.v(TAG, "syncPurchase() current row(" + externalCount + ") has data.");
                        }
                        externalCount++;
                    }
                }
            } else {
                Log.w(TAG, "sync no data!");
            }
            msg.arg1 = MSG_RESULT_SUCCESS;
            msg.obj = Utils.getErrorDescription(getResources(), ret, getString(R.string.query_account_info_success));
            mSyncSuccess = true;
        } else {
            msg.arg1 = MSG_RESULT_FAILED;
            msg.obj = Utils.getErrorDescription(getResources(), ret, getString(R.string.query_account_info_fail));
            mSyncSuccess = false;
        }
        reloadPurchaseItems();
        msg.sendToTarget();
        if (LOG) {
            Log.v(TAG, "syncPurchase() end. msg.arg1=" + msg.arg1 + ", " + msg.obj);
        }
    }

    private void reloadPurchaseItems() {
        mLocalCursor = mContentResolver.query(MBBMSStore.SG.PurchaseItemDetail.CONTENT_URI, PACKAGE_PROJECTION,
                MBBMSStore.SG.PurchaseItemDetail.RESPONSE_CODE + "=?", new String[] { "1" }, null);
        mUnsubscribedCursor = mContentResolver.query(MBBMSStore.SG.PurchaseItemDetail.CONTENT_URI, PACKAGE_PROJECTION,
                MBBMSStore.SG.PurchaseItemDetail.RESPONSE_CODE + "=?", new String[] { "0" }, null);
    }

    private void refreshPurchaseItems() {
        // refresh local purchase
        if (mExternalCursor != null) {
            mSubscribedCursor = new MergeCursor(new Cursor[] { mExternalCursor, mLocalCursor });
        } else {
            mSubscribedCursor = mLocalCursor;
        }
        // change cursor, this will cause old cursor to be closed.
        // so we can not close old cursor manually.
        mSubscribedAdapter.changeCursor(mSubscribedCursor);
        mUnsubscribedAdapter.changeCursor(mUnsubscribedCursor);
        // for search
        Cursor[] cursors = new Cursor[] { mSubscribedCursor, mUnsubscribedCursor };
        MergeCursor cursor = new MergeCursor(cursors);
        Utils.setSearchCursor(cursor);
        if (LOG) {
            Log.i(TAG, "refreshPurchaseItems() mSubscribedCursor="
                    + (mSubscribedCursor == null ? -1 : mSubscribedCursor.getCount()) + ", mUnsubscribedCursor="
                    + (mUnsubscribedCursor == null ? -1 : mUnsubscribedCursor.getCount()));
        }

    }

    private int checkIsLocalAndUpdate(String globalId, int responseCode) {
        // update someone to subscribed.
        ContentValues cv = new ContentValues(1);
        cv.put(MBBMSStore.SG.PurchaseItem.RESPONSE_CODE, responseCode);
        String where = MBBMSStore.SG.PurchaseItem.GLOBAL_PURCHASE_ITEM_ID + "=?";
        int count = mContentResolver.update(MBBMSStore.SG.PurchaseItem.CONTENT_URI, cv, where, new String[] { globalId });
        if (LOG) {
            Log.v(TAG, "checkIsLocal(" + globalId + ", " + responseCode + ") return " + count);
        }
        return count;
    }

    private int resetPurchase() {
        // update all to unsubscribed.
        ContentValues cv = new ContentValues(1);
        cv.put(MBBMSStore.SG.PurchaseItem.RESPONSE_CODE, 0);
        int count = mContentResolver.update(MBBMSStore.SG.PurchaseItem.CONTENT_URI, cv, null, null);
        if (LOG) {
            Log.v(TAG, "resetPurchase() return " + count);
        }
        return count;
    }

    // run in worker thread
    private void subscribePurchase(DataHolder holder) {
        if (LOG) {
            Log.v(TAG, "subscribePurchase() start");
        }
        ServerStatus ret = mServiceManager.subscribe(holder.mGlobalId, holder.mPurchaseDataId);
        Message msg = mUIHandler.obtainMessage(MSG_SUBSCRIBE_PURCHASE_END);
        if (Utils.isSuccess(ret)) {
            // update database
            MBBMSStore.SG.ServiceDetail.updateSubscription(mContentResolver, holder.mGlobalId, true);
            msg.arg1 = MSG_RESULT_SUCCESS;
            msg.obj = Utils.getErrorDescription(getResources(), ret, String.format(getString(R.string.subscribe_success),
                    holder.mPackageName));
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
        ServerStatus ret = mServiceManager.unsubscribe(holder.mGlobalId, holder.mPurchaseDataId);
        Message msg = mUIHandler.obtainMessage(MSG_UNSUBSCRIBE_PURCHASE_END);
        if (Utils.isSuccess(ret)) {
            // update database
            MBBMSStore.SG.ServiceDetail.updateSubscription(mContentResolver, holder.mGlobalId, false);
            msg.arg1 = MSG_RESULT_SUCCESS;
            msg.obj = Utils.getErrorDescription(getResources(), ret, String.format(getString(R.string.unsubscribe_success),
                    holder.mPackageName));
        } else {
            msg.arg1 = MSG_RESULT_FAILED;
            msg.obj = Utils.getErrorDescription(getResources(), ret, getString(R.string.unsubscribe_fail));
        }
        msg.sendToTarget();
        if (LOG) {
            Log.v(TAG, "unsubscribePurchase() msg.arg1=" + msg.arg1 + ", " + msg.obj);
        }
    }

    // run in worker thread
    private void unsubscribeAllPurchase() {
        if (LOG) {
            Log.v(TAG, "unsubscribeAllPurchase() start");
        }
        ServerStatus ret = mServiceManager.unsubscribeAll();
        Message msg = mUIHandler.obtainMessage(MSG_UNSUBSCRIBE_ALL_PURCHASE_END);
        if (Utils.isSuccess(ret)) {
            // update database
            MBBMSStore.SG.ServiceDetail.updateSubscription(mContentResolver,
                    MBBMSStore.SG.ServiceDetail.UPDATE_SUBSCRIPTION_ALL_PURCHASE_GLOBAL_ID, false);
            msg.arg1 = MSG_RESULT_SUCCESS;
            msg.obj = getString(R.string.unsubscribe_all_success);
        } else {
            msg.arg1 = MSG_RESULT_FAILED;
            msg.obj = Utils.getErrorDescription(getResources(), ret, getString(R.string.unsubscribe_fail));
        }
        if (LOG) {
            Log.v(TAG, "unsubscribeAllPurchase() msg.arg1=" + msg.arg1 + ", " + msg.obj);
        }
        msg.sendToTarget();
        if (LOG) {
            Log.v(TAG, "unsubscribeAllPurchase() end");
        }
    }

    private void asyncSyncPurchase(final boolean syncExternal) {
        new Thread(new Runnable() {

            public void run() {
                syncPurchase(syncExternal);
            }

        }, "async-syncpurchase-thread").start();
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

    private void asyncUnsubscribeAllPurchase() {
        new Thread(new Runnable() {

            public void run() {
                unsubscribeAllPurchase();
            }

        }, "async-subscribe-all-thread").start();
    }

    private static final int MENU_SEARCH = 1;
    private static final int MENU_UNSUBSCRIBE_ALL = 2;
    private static final int MENU_CONTENT_LIST = 3;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (mSubscribedCursor != null && mSubscribedCursor.getCount() != 0) {
            menu.add(0, MENU_UNSUBSCRIBE_ALL, 0, R.string.unsubscribe_all).setIcon(android.R.drawable.ic_menu_delete);
        }
        menu.add(0, MENU_SEARCH, 0, R.string.search).setIcon(android.R.drawable.ic_search_category_default);
        menu.add(0, MENU_CONTENT_LIST, 0, R.string.content_list).setIcon(android.R.drawable.ic_menu_add);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_UNSUBSCRIBE_ALL:
            unsubscribeAll();
            return true;
        case MENU_SEARCH:
            onSearchRequested();
            return true;
        case MENU_CONTENT_LIST:
            gotoOrderList();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void gotoOrderList() {
        Intent intent = new Intent();
        intent.setClass(this, ContentSelectActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onSearchRequested() {
        if (LOG) {
            Log.i(TAG, "onSearchRequested()");
        }
        startSearch(null, false, null, false);
        return true;
    }

    private void unsubscribeAll() {
        Dialog dialog = new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.mobile_tv).setMessage(R.string.unsubscribe_all_tip).setPositiveButton(
                        android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                showUnsubscribeProgress();
                                asyncUnsubscribeAllPurchase();
                            }
                        }).setNegativeButton(android.R.string.no, null).create();
        dialog.show();
    }
}
