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
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.cmmb.app.RevealLinearLayout.LayoutListener;
import com.mediatek.cmmb.app.Utils.CachedPreview;
import com.mediatek.cmmb.app.Utils.CachedPreview.DrawableStateListener;
import com.mediatek.cmmb.app.Utils.CachedPreview.MyDrawable;
import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.MBBMSStore.ESG;
import com.mediatek.mbbms.MBBMSStore.SG;
import com.mediatek.mbbms.service.CMMBServiceClient;
import com.mediatek.mbbms.service.MBBMSService;
import com.mediatek.notification.NotificationManagerPlus;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class PlaybillActivity extends Activity implements View.OnClickListener, LayoutListener, OnItemClickListener,
        DrawableStateListener {
    private static final String TAG = "Playbill";
    private static final boolean LOG = true;

    private static final int MENU_PURCHASE_ITEM = 0;

    private static final int ID_UNKNOWN = -100;
    private static final long ONE_DAY = 86400000;
    private static final long ONE_HOUR = 3600000;
    private static final long ONE_MINUTE = 60000;
    private static final String FORMAT_SEPARATOR_SPAN = " ";
    private static final String DETAIL_PADDING = "\t\t";

    private static final int OFFSET_TOMORROW = 1;
    private static final int OFFSET_DAY_AFTER_TOMORROW = 2;
    private static final int DIALOG_CONTENT_NO_MORE_DATE = 0;

    private Date mDate = new Date();
    private ServiceItem mCurrentService = new ServiceItem();
    private long mToday;
    private long mNextPlayTime = Long.MAX_VALUE;
    private long mCurrentDateOffset = 0;

    private TextView mSelectedService;
    private TextView mSelectedDate;
    private Button mSelectService;
    private Button mSelectDate;
    private ImageButton mSelectFavorite;
    private ProgressBar mLoading;
    private TextView mEmptyView;
    private ListView mListView;
    //for auto test.
    private AlertDialog mServiceDialog;
    private AlertDialog mSelectDateDialog;
    private AlertDialog mNoMoreDateDialog;
    private AlertDialog mSubscribedDialog;

    private ContentResolver mContentResolver;
    private PlaybillAdapter mListAdapter;
    private ModeSwitchManager mModeManager;
    private NotificationManagerPlus mNMP;
    private DateFormat mDateFormat;
    private String mPeriodFormat;
    private CachedPreview mCachedPrivew;

    // record the expend list for content.
    private HashMap<Integer, Boolean> mExpendList = new HashMap<Integer, Boolean>();
    // becareful about this flag,
    // it only can be reset in refreshItemView and onQueryCOmplete().
    private boolean mESGHaveSpecial;
    private boolean mIsSGMode;
    private String mLastTimeZone;

    private static final int MSG_CHECK_TIME = 0;
    private static final int MSG_CHECK_TIME_DELAY_MILLIS = 10000;
    private static final int MSG_REFRESH_SERVICE = 2;

    // check whether user reduce the phone time or not
    private long mLastTime;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (LOG) {
                Log.v(TAG, "handleMessage(" + msg.what + ") mLastTime=" + mLastTime);
            }
            switch (msg.what) {
            case MSG_CHECK_TIME:
                long now = getNow();
                long currentDay = now / ONE_DAY;
                if (getToday() != currentDay) {
                    // today passed!
                    setToday();
                    notifyDateChanged();
                } else if (now > mNextPlayTime) {
                    notifyTimeChanged();
                } else if (now < mLastTime) {
                    notifyTimeChanged();
                }
                mLastTime = now;
                if (LOG) {
                    Log.v(TAG, "handleMessage() now=" + now + ", mNextPlayTime=" + mNextPlayTime);
                }
                mHandler.sendEmptyMessageDelayed(MSG_CHECK_TIME, MSG_CHECK_TIME_DELAY_MILLIS);
                break;
            case MSG_REFRESH_SERVICE:
                refreshService();
                break;
            default:
                break;
            }
        }

    };

    @Override
    protected void onStart() {
        super.onStart();
        mModeManager.onActivityStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mModeManager.onActivityStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNMP.startListening();
        mHandler.sendEmptyMessage(MSG_CHECK_TIME);
        // refresh the data formatter
        mDateFormat = android.text.format.DateFormat.getDateFormat(this);
        refreshTitleDate();
        String newTimeZone = TimeZone.getDefault().getID();
        if (LOG) {
            Log.v(TAG, "onResume() mLastTimeZone=" + mLastTimeZone + ", newTimeZone=" + newTimeZone);
        }
        if (!mLastTimeZone.equals(newTimeZone)) {
            mListAdapter.notifyDataSetChanged();
            mLastTimeZone = newTimeZone;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNMP.stopListening();
        mHandler.removeCallbacksAndMessages(null);
        mLastTimeZone = TimeZone.getDefault().getID();
    }

    private long getNow() {
        long now = System.currentTimeMillis();
        return now;
    }

    private void setToday() {
        mToday = getNow() / ONE_DAY;
        if (LOG) {
            Log.v(TAG, "setToday() today=" + getToday() + ", getCurrentDate()=" + getCurrentDate());
        }
    }

    private long getToday() {
        if (LOG) {
            Log.v(TAG, "setToday() today=" + mToday + ", getCurrentDate()=" + getCurrentDate());
        }
        return mToday;
    }

    private long getTomorrow() {
        return mToday + OFFSET_TOMORROW;
    }

    private long getDayAfterTomorrow() {
        return mToday + OFFSET_DAY_AFTER_TOMORROW;
    }

    private long getCurrentDate() {
        return mToday + mCurrentDateOffset;
    }

    private void setCurrentDate(long currentDate) {
        if (LOG) {
            Log.v(TAG, "setCurrentDate(" + currentDate + ") mToday=" + mToday);
        }
        mCurrentDateOffset = currentDate - mToday;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mIsSGMode) {
            menu.add(0, MENU_PURCHASE_ITEM, 0, getResources().getString(R.string.package_management)).setIcon(
                    R.drawable.menu_pacakge_management);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_PURCHASE_ITEM:
            gotoPurchaseManagement();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void gotoPurchaseManagement() {
        Intent intent = new Intent();
        intent.setClass(this, PackageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(SG.Service.IS_SUBSCRIBED, isSubscribed());
        startActivity(intent);
    }

    @Override
    public void onLowMemory() {
        if (LOG) {
            Log.v(TAG, "onLowMemory()");
        }
        mCachedPrivew.clearCachedPreview();
        super.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playbill);
        initialize(savedInstanceState);
        initTitle();
        initItemView();
        enableLoading();
        refreshCurrentService(getIntent());
        refreshTitleService();
        refreshTitleFavorite();
        refreshDateRelation();
        if (mIsSGMode) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_NEEDS_MENU_KEY);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        enableLoading();
        refreshCurrentService(intent);
        refreshTitleService();
        refreshTitleFavorite();
        refreshDateRelation();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mModeManager.onSaveInstanceState(state);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            fillServiceItems();
            if (mServcieItems != null && mServcieItems.length > 0) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                        mSelectedServiceIndex--;
                        if (mSelectedServiceIndex < 0) {
                            mSelectedServiceIndex = mServcieItems.length - 1;
                        }
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        mSelectedServiceIndex++;
                        if (mSelectedServiceIndex >= mServcieItems.length) {
                            mSelectedServiceIndex = 0;
                        }
                    }
                    mCurrentService.copyFrom(mServcieItems[mSelectedServiceIndex]);
                    enableLoading();
                    refreshTitleService();
                    refreshTitleFavorite();
                    refreshItemView();
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void refreshDateRelation() {
        setToday();
        refreshTitleDate();
        refreshItemView();
        mSelectService.setEnabled(true);
        mSelectDate.setEnabled(true);
    }

    private void initialize(Bundle savedInstanceState) {
        mContentResolver = getContentResolver();
        mListAdapter = new PlaybillAdapter(this, R.layout.playbill_item, null, new String[] {}, new int[] {});

        mSelectService = (Button) findViewById(R.id.select_service);
        mSelectDate = (Button) findViewById(R.id.select_date);
        mSelectFavorite = (ImageButton) findViewById(R.id.select_favorite);

        mSelectedService = (TextView) findViewById(R.id.selected_service);
        mSelectedDate = (TextView) findViewById(R.id.selected_date);
        mLoading = (ProgressBar) findViewById(R.id.proLoading);
        // new mode change manager
        mModeManager = new ModeSwitchManager(this, null, savedInstanceState);
        mNMP = new NotificationManagerPlus.ManagerBuilder(this).create();
        mIsSGMode = (mModeManager.getMode() == CMMBServiceClient.CMMB_STATE_CONNECTED);
        mPeriodFormat = getString(R.string.period_format);
        mCachedPrivew = new CachedPreview(this, (BitmapDrawable) getResources().getDrawable(R.drawable.ic_logo_default),
                mIsSGMode, mIsSGMode ? SG.PREVIEW_USAGE_ENUM_BARKER : ESG.MEDIA_USAGE_ENUM_ICON, this);
        mLastTimeZone = TimeZone.getDefault().getID();
        mDateFormat = android.text.format.DateFormat.getDateFormat(this);

        // register content observer
        mContentResolver.registerContentObserver(getServiceUri(), true, mServiceObserver);
    }

    private void initTitle() {
        mSelectService.setOnClickListener(this);
        mSelectDate.setOnClickListener(this);
        mSelectFavorite.setOnClickListener(this);
        mSelectService.setEnabled(false);
        mSelectDate.setEnabled(false);
    }

    private void refreshCurrentService(Intent intent) {
        mCurrentService.fromIntent(intent);
    }

    private void refreshCurrentService(ServiceItem item) {
        mCurrentService.copyFrom(item);
    }

    private void refreshTitleService() {
        mSelectedService.setText(mCurrentService.serviceName);
    }

    private void refreshTitleDate() {
        String date = getFormattedDate(getCurrentDate());
        if (getToday() == getCurrentDate()) {
            mSelectedDate.setText(date + " " + getStringToday());
        } else if (getTomorrow() == getCurrentDate()) {
            mSelectedDate.setText(date + " " + getStringTomorrow());
        } else if (getDayAfterTomorrow() == getCurrentDate()) {
            mSelectedDate.setText(date + " " + getStringDayAfterTomorrow());
        } else {
            mSelectedDate.setText(date);
        }
        if (LOG) {
            Log.v(TAG, "today=" + getToday() + ", current=" + getCurrentDate());
        }
    }

    private void enableLoading() {
        mLoading.setVisibility(View.VISIBLE);
    }

    private void disableLoading() {
        mLoading.setVisibility(View.GONE);
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

    private void initItemView() {
        getListView().setAdapter(mListAdapter);
        getListView().setOnItemClickListener(this);
    }

    private void refreshItemView() {
        if (LOG) {
            Log.v(TAG, "refreshItemView() mCurrentService=" + mCurrentService);
        }
        if (!mIsSGMode) {
            // 1, load data from special
            // 2, if there's no data, load data from schedule
            mESGHaveSpecial = true;
        }
        mListAdapter.getQueryHandler().startQuery(0, null, getContentUri(), getContentProjection(), getItemSelection(),
                getItemSelectionArgs(), getItemOrderBy());
    }

    private static final String[] SG_CONTENT_PROJECTION = new String[] { SG.ContentDetail.ID, // 0
            SG.ContentDetail.CONTENT_ID, // 1
            SG.ContentDetail.NAME, // 2
            SG.ContentDetail.START_TIME, // 3
            SG.ContentDetail.END_TIME, // 4
            SG.ContentDetail.DESCRIPTION, // 5
            SG.ContentDetail.IS_SUBSCRIBED, // 6
            SG.ContentDetail.FOR_FREE // 7
    };

    private static final String[] ESG_CONTENT_PROJECTION = new String[] { ESG.ContentDetail.ID, // 0
            ESG.ContentDetail.CONTENT_ID, // 1
            ESG.ContentDetail.TITLE, // 2
            ESG.ContentDetail.START_TIME, // 3
            ESG.ContentDetail.START_TIME + "+" + ESG.ContentDetail.DURATION + " as EndTime", // 4
            ESG.ContentDetail.DIGEST_INFO, // 5
            " '0' as " + SG.ContentDetail.IS_SUBSCRIBED, // 6
            ESG.ContentDetail.FOR_FREE // 7
    };

    private static final String[] ESG_CONTENT_PROJECTION2 = new String[] { ESG.ContentDetail.ID, // 0
            ESG.ContentDetail.CONTENT_ID, // 1
            ESG.ContentDetail.TITLE, // 2
            ESG.ContentDetail.START_TIME, // 3
            ESG.ContentDetail.START_TIME + "+" + ESG.ContentDetail.DURATION + " as EndTime", // 4
            " '' as " + ESG.ContentDetail.DIGEST_INFO, // 5
            " '0' as " + SG.ContentDetail.IS_SUBSCRIBED, // 6
            ESG.ContentDetail.FOR_FREE // 7
    };

    private static final int CONTENT_COLUMN_ID = 0;
    private static final int CONTENT_COLUMN_CONTENT_ID = 1;
    private static final int CONTENT_COLUMN_TITLE = 2;
    private static final int CONTENT_COLUMN_START_TIME = 3;
    private static final int CONTENT_COLUMN_END_TIME = 4;
    private static final int CONTENT_COLUMN_DETAIL = 5;
    private static final int CONTENT_COLUMN_IS_SUBSCRIBED = 6;
    private static final int CONTENT_COLUMN_FOR_FREE = 7;

    private Uri getContentUri() {
        Uri uri = null;
        if (mIsSGMode) {
            uri = SG.ContentDetail.CONTENT_URI;
        } else {
            if (mESGHaveSpecial) {
                uri = ESG.ContentDetail.CONTENT_URI.buildUpon().appendQueryParameter(
                        MBBMSStore.ESG.ContentDetail.QUERY_SPECIAL, "1").build();
            } else {
                uri = ESG.ContentDetail.CONTENT_URI;
            }
        }
        if (LOG) {
            Log.v(TAG, "getContentUri() uri=" + uri);
        }
        return uri;
    }

    private String[] getContentProjection() {
        if (mIsSGMode) {
            return SG_CONTENT_PROJECTION;
        } else {
            if (mESGHaveSpecial) {
                return ESG_CONTENT_PROJECTION;
            } else {
                return ESG_CONTENT_PROJECTION2;
            }
        }
    }

    private String getItemSelection() {
        String selection = null;
        if (mIsSGMode) {
            selection = new StringBuilder().append(SG.ContentDetail.SERVICE_ID).append("=? and ((").append(
                    SG.ContentDetail.START_TIME).append(">? and ").append(SG.ContentDetail.START_TIME).append("<?) or (")
                    .append(SG.ContentDetail.END_TIME).append(">? and ").append(SG.ContentDetail.END_TIME).append("<? ))")
                    .toString();
        } else {
            if (mESGHaveSpecial) {
                selection = new StringBuilder().append(ESG.ContentDetail.SERVICE_ID).append("=? and ").append(
                        ESG.ContentDetail.FREQUENCY_NO).append("=? and ").append(ESG.ContentDetail.LANGUAGE).append(
                        "=? and ((").append(ESG.ContentDetail.START_TIME).append(">? and ").append(
                        ESG.ContentDetail.START_TIME).append("<?) or (").append(ESG.ContentDetail.START_TIME).append("+")
                        .append(ESG.ContentDetail.DURATION).append(">? and ").append(ESG.ContentDetail.START_TIME).append(
                                "+").append(ESG.ContentDetail.DURATION).append("<? ))").toString();
            } else {
                selection = new StringBuilder().append(ESG.ContentDetail.SERVICE_ID).append("=? and ").append(
                        ESG.ContentDetail.FREQUENCY_NO).append("=? and ((").append(ESG.ContentDetail.START_TIME).append(
                        ">? and ").append(ESG.ContentDetail.START_TIME).append("<?) or (").append(
                        ESG.ContentDetail.START_TIME).append("+").append(ESG.ContentDetail.DURATION).append(">? and ")
                        .append(ESG.ContentDetail.START_TIME).append("+").append(ESG.ContentDetail.DURATION).append("<? ))")
                        .toString();
            }
        }
        if (LOG) {
            Log.v(TAG, "getItemSelection()=" + selection);
        }
        return selection;
    }

    private String[] getItemSelectionArgs() {
        String now = String.valueOf(getCurrentDate() * ONE_DAY);
        String tomorrow = String.valueOf((getCurrentDate() + OFFSET_TOMORROW) * ONE_DAY);
        if (mIsSGMode) {
            return new String[] { mCurrentService.serviceId, now, tomorrow, now, tomorrow };
        } else {
            if (mESGHaveSpecial) {
                return new String[] { mCurrentService.serviceId, String.valueOf(mCurrentService.frequencyNo),
                        getCurrentLang(), now, tomorrow, now, tomorrow };
            } else {
                return new String[] { mCurrentService.serviceId, String.valueOf(mCurrentService.frequencyNo), now, tomorrow,
                        now, tomorrow };
            }
        }
    }

    private String getItemOrderBy() {
        if (mIsSGMode) {
            return SG.ContentDetail.START_TIME;
        } else {
            return ESG.ContentDetail.START_TIME;
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

    private String getFormattedDate(long date) {
        mDate.setTime(date * ONE_DAY);
        return mDateFormat.format(mDate);
    }

    private String getStringToday() {
        return getResources().getString(R.string.today);
    }

    private String getStringTomorrow() {
        return getResources().getString(R.string.tomorrow);
    }

    private String getStringDayAfterTomorrow() {
        return getResources().getString(R.string.day_after_tomorrow);
    }

    private String getLanguageString(String allString) {
        String detail = MBBMSStore.parseLanguageString(allString, getCurrentLang());
        if (LOG) {
            Log.v(TAG, "getLanguageString() = " + detail);
        }
        return detail;
    }

    private boolean isSubscribed() {
        if (mIsSGMode) {
            return SG.ServiceDetail.getIsSubscribed(mContentResolver, mCurrentService.id);
        } else {
            return ESG.ServiceDetail.getIsSubscribed(mContentResolver, mCurrentService.id);
        }
    }

    private String getCurrentLang() {
        return Utils.getCurrentLang();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
        case DIALOG_CONTENT_NO_MORE_DATE:
                AlertDialog.Builder nomoredatebuilder = new AlertDialog.Builder(this);
                mNoMoreDateDialog = nomoredatebuilder.setIcon(android.R.drawable.ic_dialog_alert)
                                            .setTitle(R.string.mobile_tv)
                                            .setMessage(R.string.no_more_date)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .create();
                return mNoMoreDateDialog;
        default:
            break;
        }
        return super.onCreateDialog(id, args);
    }

    private boolean getExpended(int contentId) {
        Boolean expend = mExpendList.get(contentId);
        return (expend != null && expend);
    }

    private void setExpended(int contentId, boolean expend) {
        mExpendList.put(contentId, expend);
    }

    private void clearAllExpended() {
        mExpendList.clear();
    }

    public void onClick(View v) {
        int id = v.getId();
        ViewHolder holder;
        switch (id) {
        case R.id.item_switch:
            holder = (ViewHolder) v.getTag();
            boolean expend = getExpended(holder.id);
            setExpended(holder.id, !expend);
            holder.needCheck = !expend;// need check it
            mListAdapter.notifyDataSetChanged();
            break;
        case R.id.select_service:
            CharSequence[] serviceItems = getServiceItems();
               AlertDialog.Builder builder = new AlertDialog.Builder(this);

                mServiceDialog = builder
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle(R.string.channels)
                    .setSingleChoiceItems(serviceItems, mSelectedServiceIndex, mSelectServiceListener)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
                 mServiceDialog = builder.show();
            break;
        case R.id.select_date:
            CharSequence[] dateItems = getDateItems();
            if (dateItems == null || dateItems.length < 1) {
                showDialog(DIALOG_CONTENT_NO_MORE_DATE, null);
            } else {
                    AlertDialog.Builder selectDatebuilder = new AlertDialog.Builder(this);
                    mSelectDateDialog = selectDatebuilder
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(R.string.date)
                        .setSingleChoiceItems(dateItems, mSelectedDateIndex, mSelectDateListener)
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                    mSelectDateDialog = selectDatebuilder.show();
            }
            break;
        case R.id.select_favorite:
            updateFavorite();
            break;
        default:
            Log.w(TAG, "Not handled onClick(" + v + ")");
            break;
        }
        if (LOG) {
            Log.v(TAG, "onClick(" + id + ")");
        }
    }

    private void showUnSubscribedDialog() {
        String str = null;
        if (mIsSGMode) {
            str = getResources().getString(R.string.need_subscription_hint);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mSubscribedDialog = builder
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.mobile_tv)
                    .setMessage(String.format(str, mCurrentService.serviceName))
                    .setPositiveButton(android.R.string.ok,
                            new OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    gotoPurchaseManagement();
                                }

                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
          mSubscribedDialog = builder.show();
        } else {
            str = getResources().getString(R.string.channel_unavailable_now);
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.mobile_tv)
                    .setMessage(String.format(str, mCurrentService.serviceName))
                    .setPositiveButton(android.R.string.ok, null).create().show();
        }
    }

    private void gotoPlayer() {
        Intent intent = new Intent();
        intent.setClass(this, PlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(Utils.EXTRA_SERVICE_ROW_ID, mCurrentService.id);
        startActivity(intent);
    }

    private void updateFavorite() {
        if (LOG) {
            Log.v(TAG, "updateFavorite() mCurrentFavorite=" + mCurrentService.isFavorite);
        }
        ContentValues values = new ContentValues(1);
        int newFavorite = mCurrentService.isFavorite == 0 ? 1 : 0;
        int toastMsgId = mCurrentService.isFavorite == 0 ? R.string.favourite_added : R.string.favourite_removed;
        values.put(getFavoriteKey(), newFavorite);
        int count = mContentResolver.update(ContentUris.withAppendedId(getServiceUri(), mCurrentService.id), values, null,
                null);
        if (count == 1) {
            mCurrentService.isFavorite = newFavorite;
            refreshTitleFavorite();
            String toastMsg = String.format(getResources().getString(toastMsgId), mCurrentService.serviceName);
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "updateFavorite() count=" + count);
        }
        if (LOG) {
            Log.v(TAG, "updateFavorite() mCurrentFavorite=" + mCurrentService.isFavorite + ", count=" + count);
        }
    }

    private void refreshTitleFavorite() {
        if (mCurrentService.isFavorite == 0) {
            mSelectFavorite.setImageResource(R.drawable.ic_favorite_no);
        } else {
            mSelectFavorite.setImageResource(R.drawable.ic_favorite_yes);
        }
    }

    private Uri getServiceUri() {
        Uri uri;
        if (mIsSGMode) {
            uri = SG.Service.CONTENT_URI;
        } else {
            uri = ESG.Service.CONTENT_URI;
        }
        if (LOG) {
            Log.v(TAG, "getServiceUir()=" + uri);
        }
        return uri;
    }

    private String getSelection() {
        if (mIsSGMode) {
            return SG.Service.SERVICE_TYPE + " = " + SG.Service.SERVICE_TYPE_ENUM_BASIC_TV;
        } else {
            return ESG.Service.SERVICE_CLASS + "=" + ESG.Service.SERVICE_CLASS_ENUM_TV;
        }
    }

    private String getFavoriteKey() {
        String key;
        if (mIsSGMode) {
            key = SG.Service.IS_FAVORITE;
        } else {
            key = ESG.Service.IS_FAVORITE;
        }
        return key;
    }

    private class ViewHolder {
        public int id = ID_UNKNOWN;// row id of content
        public int type = TYPE_UNKNOWN;// item type shown
        public boolean expend;// last expend or not
        public boolean needCheck;// need check scroll it or not
        public String detail;
        public String contentId;

        public LinearLayout lytPanel;
        public ImageView imgPreview;
        public ImageView imgSwitch;
        public ImageView imgOverlay;
        public ImageView imgSubscribed;
        public TextView txtHeader;
        public TextView txtTitle;
        public TextView txtTime;
        public TextView txtDetail;

        @Override
        public String toString() {
            return new StringBuilder().append("(contentId=" + id).append(", type=" + type).append(", expend=" + expend)
                    .append(", needCheck=" + needCheck).append(", detail=" + detail).append(")").toString();
        }

    }

    private void moveToBestPosition() {
        getListView().setSelection(mBestPosToMove);
    }

    /* package */class PlaybillAdapter extends SimpleCursorAdapter {

        class QueryHandler extends AsyncQueryHandler {

            public QueryHandler(ContentResolver cr) {
                super(cr);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (LOG) {
                    Log.v(TAG, "onQueryComplete() mHaveSpecial=" + mESGHaveSpecial + ", mIsSGMode=" + mIsSGMode);
                }
                if (!mIsSGMode && mESGHaveSpecial && (cursor == null || cursor.getCount() == 0)) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    Log.w(TAG, "Can not get content special, query from schedule.");
                    mESGHaveSpecial = false;
                    mListAdapter.getQueryHandler().startQuery(0, null, getContentUri(), getContentProjection(),
                            getItemSelection(), getItemSelectionArgs(), getItemOrderBy());
                    return;
                }
                mPrePos = POS_UNKNOWN;
                mNowPos = POS_UNKNOWN;
                mNextPos = POS_UNKNOWN;
                mNextPlayTime = Long.MAX_VALUE;
                // change the list view and empty view's visibility
                if (cursor == null || cursor.getCount() == 0) {
                    // hide the list view
                    getEmptyView().setVisibility(View.VISIBLE);
                    getListView().setVisibility(View.GONE);
                    PlaybillAdapter.this.changeCursor(cursor);
                } else {
                    analyzeItems(cursor);
                    PlaybillAdapter.this.changeCursor(cursor);
                    // Here call setAdapter to force listview reposition the
                    // right position.
                    // I think listview should support the mechnism to sync
                    // position.
                    // Here need recheck.
                    getListView().setAdapter(mListAdapter);
                    moveToBestPosition();
                    // show the list view
                    getEmptyView().setVisibility(View.GONE);
                    getListView().setVisibility(View.VISIBLE);
                }
                disableLoading();
                if (LOG) {
                    Log.v(TAG, "onQueryComplete(" + token + "," + cookie + "," + cursor + ")");
                }
                if (LOG && cursor != null) {
                    Log.i(TAG, "onQueryComplete() cursor.count=" + cursor.getCount());
                }
            }

        }

        private QueryHandler mQueryHandler;
        private ContentResolver mContentResolver;

        public PlaybillAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
            mContentResolver = context.getContentResolver();
            mQueryHandler = new QueryHandler(mContentResolver);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ViewHolder holder = new ViewHolder();

            holder.lytPanel = (LinearLayout) v.findViewById(R.id.item_pannel);
            holder.imgPreview = (ImageView) v.findViewById(R.id.item_preview);
            holder.imgSwitch = (ImageView) v.findViewById(R.id.item_switch);
            holder.txtHeader = (TextView) v.findViewById(R.id.item_header);
            holder.txtTitle = (TextView) v.findViewById(R.id.item_title);
            holder.txtTime = (TextView) v.findViewById(R.id.item_time);
            holder.txtDetail = (TextView) v.findViewById(R.id.item_detail);
            holder.imgOverlay = (ImageView) v.findViewById(R.id.item_overlay);
            holder.imgSubscribed = (ImageView) v.findViewById(R.id.item_subscribed);

            holder.imgSwitch.setTag(holder);
            holder.imgSwitch.setOnClickListener(PlaybillActivity.this);
            v.setTag(holder);
            if (v instanceof RevealLinearLayout) {
                ((RevealLinearLayout) v).setLayoutListener(PlaybillActivity.this);
            }
            if (LOG) {
                Log.v(TAG, "newView() holder = " + holder);
            }
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            int position = cursor.getPosition();
            int newType = getItemType(position);
            int isSubscribed = cursor.getInt(CONTENT_COLUMN_IS_SUBSCRIBED);
            int contentForFree = cursor.getInt(CONTENT_COLUMN_FOR_FREE);
            if (LOG) {
                Log.v(TAG, "holder.type=" + holder.type + ", newType=" + newType);
            }
            if (holder.type != newType) {
                switch (newType) {
                case TYPE_NEXT:
                    holder.lytPanel.setBackgroundResource(R.drawable.background_unread);
                    holder.txtHeader.setVisibility(View.GONE);
                    holder.imgOverlay.setVisibility(View.GONE);
                    break;
                case TYPE_PRE:
                    holder.lytPanel.setBackgroundResource(R.drawable.background_read);
                    holder.txtHeader.setVisibility(View.GONE);
                    holder.imgOverlay.setVisibility(View.GONE);
                    break;
                case TYPE_NOW:
                    holder.lytPanel.setBackgroundResource(R.drawable.background_unread);
                    holder.txtHeader.setText(R.string.play_now);
                    holder.txtHeader.setVisibility(View.VISIBLE);
                    holder.imgOverlay.setVisibility(View.VISIBLE);
                    break;
                case TYPE_NEXT_TITLE:
                    holder.lytPanel.setBackgroundResource(R.drawable.background_unread);
                    holder.txtHeader.setText(R.string.play_later);
                    holder.txtHeader.setVisibility(View.VISIBLE);
                    holder.imgOverlay.setVisibility(View.GONE);
                    break;
                case TYPE_PRE_TITLE:
                    holder.lytPanel.setBackgroundResource(R.drawable.background_read);
                    holder.txtHeader.setText(R.string.play_history);
                    holder.txtHeader.setVisibility(View.VISIBLE);
                    holder.imgOverlay.setVisibility(View.GONE);
                    break;
                default:
                    Log.w(TAG, "Wrong type! newType=" + newType);
                    break;
                }
            } else {
                if (LOG) {
                    Log.v(TAG, "holder.type not changed. type=" + newType);
                }
                // do nothing for item type
            }
            holder.type = newType;
            holder.id = cursor.getInt(CONTENT_COLUMN_ID);
            holder.contentId = cursor.getString(CONTENT_COLUMN_CONTENT_ID);
            setTitle(holder, cursor.getString(CONTENT_COLUMN_TITLE));
            holder.txtTime.setText(getFormattedTime(cursor.getLong(CONTENT_COLUMN_START_TIME), cursor
                    .getLong(CONTENT_COLUMN_END_TIME)));
            holder.detail = cursor.getString(CONTENT_COLUMN_DETAIL);
            if (mCurrentService.forFree == 0 || contentForFree == 0) {
                if (isSubscribed == 1) {
                    holder.imgSubscribed.setImageResource(R.drawable.subscribed_indicator);
                } else {
                    holder.imgSubscribed.setImageResource(R.drawable.unsubscribed_indicator);
                }
            } else {
                holder.imgSubscribed.setVisibility(View.GONE);
            }
            collapseItemAuto(holder);
            holder.imgPreview.setImageDrawable(mCachedPrivew.getCachedPreview(holder.id));
            if (LOG) {
                Log.v(TAG, "fillView() holder = " + holder);
            }
        }

        private void setTitle(ViewHolder holder, String title) {
            if (LOG) {
                Log.v(TAG, "setTitle() mHaveSpecial=" + mESGHaveSpecial + ", title=" + title);
            }
            if (mIsSGMode || !mESGHaveSpecial) {
                holder.txtTitle.setText(getLanguageString(title));
            } else {
                holder.txtTitle.setText(title);
            }
        }

        private void collapseItemAuto(ViewHolder holder) {
            if (LOG) {
                Log.v(TAG, "collapseItemAuto() holder=" + holder);
            }
            boolean expend = getExpended(holder.id);
            if (LOG) {
                Log.v(TAG, "collapseItemAuto() holder.expend=" + holder.expend + ", new expend=" + expend);
            }
            if (holder.expend != expend) {
                if (expend) {
                    holder.imgSwitch.setImageResource(R.drawable.expander_ic_maximized);
                    holder.txtTitle.setSingleLine(false);
                    holder.txtTitle.setEllipsize(null);
                    String detail = getLanguageString(holder.detail);
                    if (detail == null || "".equals(detail.trim())) {
                        holder.txtDetail.setText(DETAIL_PADDING + getResources().getString(R.string.content_no_detail));
                    } else {
                        holder.txtDetail.setText(DETAIL_PADDING + detail);
                    }
                    holder.txtDetail.setVisibility(View.VISIBLE);
                } else {
                    holder.imgSwitch.setImageResource(R.drawable.expander_ic_minimized);
                    holder.txtDetail.setVisibility(View.GONE);
                    holder.txtTitle.setSingleLine(true);
                    holder.txtTitle.setEllipsize(TruncateAt.MARQUEE);
                }
                holder.expend = expend;
            }

        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            if (LOG) {
                Log.v(TAG, "changeCursor(" + cursor + ")");
            }
        }

        public QueryHandler getQueryHandler() {
            return mQueryHandler;
        }

    }

    public void onChanged(int rowId, MyDrawable drawable) {
        if (LOG) {
            Log.v(TAG, "onChanged(" + rowId + ", " + drawable + ")");
        }
        // just update when drawable is available
        if (drawable != null && drawable.type == CachedPreview.TYPE_LOADED_HAS_PREVIEW) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    private static final int POS_UNKNOWN = -1;
    private int mPrePos = POS_UNKNOWN;
    private int mNowPos = POS_UNKNOWN;
    private int mNextPos = POS_UNKNOWN;
    private int mBestPosToMove = POS_UNKNOWN;

    private void analyzeItems(Cursor cursor) {
        long now = getNow();
        long startTime;
        long endTime;
        int count = cursor.getCount();
        if (getCurrentDate() < getToday()) {
            // previous days
            cursor.moveToLast();
            endTime = cursor.getLong(CONTENT_COLUMN_END_TIME);
            if (endTime > now) {
                if (count > 1) {
                    mPrePos = 0;
                } else {
                    mPrePos = POS_UNKNOWN;
                }
                mNowPos = count - 1;
                mNextPos = count;
                cursor.moveToPosition(mNowPos);
            } else {
                mPrePos = 0;
                mNowPos = count;
                mNextPos = count;
            }
            mBestPosToMove = 0;
        } else if (getCurrentDate() > getToday()) {
            // next days
            cursor.moveToFirst();
            startTime = cursor.getLong(CONTENT_COLUMN_START_TIME);
            if (startTime < now) {
                mPrePos = POS_UNKNOWN;
                mNowPos = 0;
                mNextPos = mNowPos + 1;
                cursor.moveToPosition(mNowPos);
            } else {
                mNextPos = 0;
                mPrePos = mNextPos - 1;
                mNowPos = mNextPos - 1;
            }
            mBestPosToMove = 0;
        } else {
            // today
            boolean findStart = false;
            boolean findEnd = false;
            boolean findedPosition = false;
            boolean findedNextPlayTime = false;
            int findedPositionIndex = -1;
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                startTime = cursor.getLong(CONTENT_COLUMN_START_TIME);
                endTime = cursor.getLong(CONTENT_COLUMN_END_TIME);
                if (!findedPosition && endTime > now) {
                    findEnd = true;
                    if (LOG) {
                        Log.v(TAG, "findStart " + getFormattedTime(startTime, endTime));
                    }
                    if (LOG) {
                        Log.v(TAG, "findStart startTime=" + startTime + ", now=" + now + ", endTime=" + endTime
                                + ", cursor.getPosition()=" + cursor.getPosition());
                    }
                    if (startTime <= now) {
                        findStart = true;
                    }
                    findedPosition = true;
                    findedPositionIndex = cursor.getPosition();// record the
                    // position
                }
                if (!findedNextPlayTime) {
                    if (startTime > now) {
                        mNextPlayTime = startTime;
                        findedNextPlayTime = true;
                    }
                }
                if (findedPosition && findedNextPlayTime) {
                    break;
                }
            }
            cursor.moveToPosition(findedPositionIndex);
            if (findStart && findEnd) {
                mNowPos = cursor.getPosition();
                if (mNowPos > 0) {
                    mPrePos = 0;
                } else {
                    mPrePos = POS_UNKNOWN;
                }
                mNextPos = mNowPos + 1;
                mBestPosToMove = mNowPos;
            } else if (!findEnd) {
                mPrePos = 0;
                mNowPos = count;
                mNextPos = count;
                mBestPosToMove = 0;
            } else if (!findStart) {
                int pos = cursor.getPosition();
                if (pos == 0) {
                    mNextPos = 0;
                    mPrePos = mNextPos - 1;
                    mNowPos = mNextPos - 1;
                    mBestPosToMove = 0;
                } else {
                    mNextPos = pos;
                    mPrePos = 0;
                    mNowPos = mNextPos;// be care about this case
                    mBestPosToMove = mNextPos;
                }
            }
            if (LOG) {
                Log.v(TAG, "findStart=" + findStart + ", findEnd=" + findEnd + ", cursor.getPosition()="
                        + cursor.getPosition());
            }
        }
        if (LOG) {
            Log.v(TAG, "analyzeItems() cursor.getCount()=" + count + ", mBestPosToMove=" + mBestPosToMove);
            Log.v(TAG, "analyzeItems() mPrePos=" + mPrePos + ", mNowPos=" + mNowPos + ", mNextPos=" + mNextPos);
        }
    }

    private static final int TYPE_UNKNOWN = -1;
    private static final int TYPE_PRE_TITLE = 0;
    private static final int TYPE_PRE = 1;
    private static final int TYPE_NOW = 2;
    private static final int TYPE_NEXT_TITLE = 3;
    private static final int TYPE_NEXT = 4;

    private int getItemType(int position) {
        int type;
        if (position > mNextPos) {
            type = TYPE_NEXT;
        } else if (position > mPrePos && position < mNowPos) {
            type = TYPE_PRE;
        } else if (position == mNowPos) {
            if (mNowPos == mNextPos) {
                type = TYPE_NEXT_TITLE;
            } else {
                type = TYPE_NOW;
            }
        } else if (position == mNextPos) {
            type = TYPE_NEXT_TITLE;
        } else {
            type = TYPE_PRE_TITLE;
        }
        if (LOG) {
            Log.v(TAG, "getItemType() mPrePos=" + mPrePos + ", mNowPos=" + mNowPos + ", mNextPos=" + mNextPos);
            Log.v(TAG, "getItemType(" + position + ")=" + type);
        }
        return type;
    }

    // below is select service dialog codes.
    private static final String[] ESG_SERVICE_PROJECTION = new String[] { ESG.Service.ID, ESG.Service.SERVICE_ID,
            ESG.Service.SERVICE_NAME, ESG.Service.IS_FAVORITE, ESG.Service.FOR_FREE, ESG.Service.FREQUENCY_NO };

    private static final String[] SG_SERVICE_PROJECTION = new String[] { SG.Service.ID, SG.Service.SERVICE_ID,
            SG.Service.NAME, SG.Service.IS_FAVORITE, SG.Service.FOR_FREE, " -1 " };

    private static final int SERVICE_COLUMN_ID = 0;
    private static final int SERVICE_COLUMN_SERVICE_ID = 1;
    private static final int SERVICE_COLUMN_NAME = 2;
    private static final int SERVICE_COLUMN_IS_FAVORITE = 3;
    private static final int SERVICE_COLUMN_FOR_FREE = 4;
    private static final int SERVICE_COLUMN_FREQUENCY = 5;

    private String[] getServcieProjection() {
        if (mIsSGMode) {
            return SG_SERVICE_PROJECTION;
        } else {
            return ESG_SERVICE_PROJECTION;
        }
    }

    private boolean mNeedReloadServices = true;
    private ServiceItem[] mServcieItems;
    private CharSequence[] mServiceStringItems;
    private int mSelectedServiceIndex;

    private ContentObserver mServiceObserver = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange) {
            Log.w(TAG, "Service item was been modified!");
            mHandler.sendEmptyMessage(MSG_REFRESH_SERVICE);
        }

    };

    private void refreshService() {
        enableLoading();

        mNeedReloadServices = true;
        fillServiceItems();

        refreshTitleService();
        refreshTitleFavorite();
        refreshDateRelation();
    }

    private class ServiceItem {
        public int id;
        public int isFavorite;
        public int forFree;
        public String serviceId;
        public String serviceName;
        public int frequencyNo;

        public void fromIntent(Intent intent) {
            Bundle data = intent.getExtras();
            id = data.getInt(Utils.EXTRA_SERVICE_ROW_ID);
            isFavorite = data.getInt(Utils.EXTRA_IS_FAVORITE);
            serviceId = String.valueOf(data.get(Utils.EXTRA_SERVICE_ID));
            serviceName = data.getString(Utils.EXTRA_SERVICE_NAME);
            frequencyNo = data.getInt(Utils.EXTRA_FREQUENCY_NO);
            forFree = data.getBoolean(Utils.EXTRA_FOR_FREE) ? 1 : 0;
            if (LOG) {
                Log.v(TAG, "fromIntent()=" + this);
            }
        }

        public void copyFrom(ServiceItem item) {
            id = item.id;
            isFavorite = item.isFavorite;
            serviceId = item.serviceId;
            serviceName = item.serviceName;
            frequencyNo = item.frequencyNo;
            forFree = item.forFree;
            if (LOG) {
                Log.v(TAG, "copyFrom()=" + this);
            }
        }

        @Override
        public String toString() {
            return new StringBuilder().append("(id=" + id).append(", isFavorite=" + isFavorite).append(
                    ", forFree=" + forFree).append(", serviceId=" + serviceId).append(", ServiceName=" + serviceName)
                    .append(", frequencyNo=" + frequencyNo).append(")").toString();
        }

    }

    static Comparator<ServiceItem> getServiceComparator() {
        return new Comparator<ServiceItem>() {

            public int compare(ServiceItem r1, ServiceItem r2) {
                int result = 0;
                if (r1.serviceName != null) {
                    result = r1.serviceName.compareTo(r2.serviceName);
                } else if (r2.serviceName != null) {
                    result = r2.serviceName.compareTo(r1.serviceName);
                }
                return result;
            }

        };
    }

    private CharSequence[] getServiceItems() {
        boolean isServicesChanged = fillServiceItems();
        if (mServiceStringItems == null || isServicesChanged) {
            if (mServcieItems != null && mServcieItems.length > 0) {
                int count = mServcieItems.length;
                mServiceStringItems = new CharSequence[count];
                for (int i = 0; i < count; i++) {
                    mServiceStringItems[i] = mServcieItems[i].serviceName;
                }
            }
        }

        if (LOG) {
            Log.i(TAG, "mServiceItems.length=" + (mServiceStringItems == null ? -1 : mServiceStringItems.length));
            Log.i(TAG, "mServiceSelectedIndex=" + mSelectedServiceIndex);
        }

        return mServiceStringItems;
    }

    private boolean fillServiceItems() {
        if (mNeedReloadServices || mServcieItems == null) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(getServiceUri(), getServcieProjection(), getSelection(), null, null);
                int count = 0;
                if (cursor != null && cursor.getCount() > 0) {
                    count = cursor.getCount();
                    mServcieItems = new ServiceItem[count];
                    for (int i = 0; i < count; i++) {
                        cursor.moveToNext();
                        ServiceItem item = new ServiceItem();
                        item.id = cursor.getInt(SERVICE_COLUMN_ID);
                        item.isFavorite = cursor.getInt(SERVICE_COLUMN_IS_FAVORITE);
                        item.forFree = cursor.getInt(SERVICE_COLUMN_FOR_FREE);
                        item.serviceId = cursor.getString(SERVICE_COLUMN_SERVICE_ID);
                        item.serviceName = getLanguageString(cursor.getString(SERVICE_COLUMN_NAME));
                        item.frequencyNo = cursor.getInt(SERVICE_COLUMN_FREQUENCY);
                        mServcieItems[i] = item;
                    }

                    if (mServcieItems != null) {
                        Arrays.sort(mServcieItems, getServiceComparator());
                        mSelectedServiceIndex = 0;
                        for (int i = 0; i < mServcieItems.length; i++) {
                            if (mCurrentService.id == mServcieItems[i].id) {
                                mSelectedServiceIndex = i;
                                break;
                            }
                        }
                        if (LOG) {
                            Log.v(TAG, "mSelectedServiceIndex = " + mSelectedServiceIndex + ", mNeedReloadServices = "
                                    + mNeedReloadServices);
                        }

                        if (mNeedReloadServices) {
                            mCurrentService.copyFrom(mServcieItems[mSelectedServiceIndex]);
                        }
                    }
                }
                if (LOG) {
                    Log.v(TAG, "fillServiceItems() cursor.getCount()=" + (cursor == null ? -1 : cursor.getCount()));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            mNeedReloadServices = false;
            return true; // SerciveItems are changed
        }
        return false; // ServiceItems are not changed
    }

    private DialogInterface.OnClickListener mSelectServiceListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (LOG) {
                Log.v(TAG, "DialogInterface.onClick(" + which + ") mServiceSelectedIndex=" + mSelectedServiceIndex);
            }
            dialog.dismiss();
            if (mSelectedServiceIndex != which) {
                mSelectedServiceIndex = which;
                refreshCurrentService(mServcieItems[which]);
                enableLoading();
                refreshTitleService();
                refreshTitleFavorite();
                refreshItemView();
            }
            if (LOG) {
                Log.v(TAG, "mCurrentService=" + mCurrentService);
            }
        }
    };

    private void clear() {
        clearAllExpended();
        mCachedPrivew.clearCachedPreview();
        mContentResolver.unregisterContentObserver(mServiceObserver);
        mListAdapter.changeCursor(null);
    }

    // below is select date dialog codes.
    private long[] mDateItems;
    private int mSelectedDateIndex;

    private Uri getDateUri() {
        return getContentUri();
    }

    private String getDateColumnName() {
        if (mIsSGMode) {
            return SG.ContentDetail.START_TIME;
        } else {
            return ESG.ContentDetail.START_TIME;
        }
    }

    private String getDateSelection() {
        if (mIsSGMode) {
            return SG.ContentDetail.SERVICE_ID + "=? ";
        } else {
            return ESG.ContentDetail.SERVICE_ID + "=? and " + ESG.ContentDetail.FREQUENCY_NO + "=? ";
        }
    }

    private String[] getDateSelectionArgs() {
        if (mIsSGMode) {
            return new String[] { mCurrentService.serviceId };
        } else {
            return new String[] { mCurrentService.serviceId, String.valueOf(mCurrentService.frequencyNo) };
        }
    }

    private String getDateOrderBy() {
        if (mIsSGMode) {
            return SG.ContentDetail.START_TIME;
        } else {
            return ESG.ContentDetail.START_TIME;
        }
    }

    private CharSequence[] getDateItems() {
        mDateItems = MBBMSStore.getDifferentDay(mContentResolver, getDateUri(), getDateColumnName(), getDateSelection(),
                getDateSelectionArgs(), getDateOrderBy());
        mSelectedDateIndex = -1;
        CharSequence[] dateStringItems = null;
        if (mDateItems != null && mDateItems.length > 0) {
            int length = mDateItems.length;
            dateStringItems = new CharSequence[length];
            boolean finded = false;
            for (int i = 0; i < length; i++) {
                if (getToday() == mDateItems[i]) {
                    dateStringItems[i] = getFormattedDate(mDateItems[i]) + FORMAT_SEPARATOR_SPAN + getStringToday();
                } else if (getTomorrow() == mDateItems[i]) {
                    dateStringItems[i] = getFormattedDate(mDateItems[i]) + FORMAT_SEPARATOR_SPAN + getStringTomorrow();
                } else if (getDayAfterTomorrow() == mDateItems[i]) {
                    dateStringItems[i] = getFormattedDate(mDateItems[i]) + FORMAT_SEPARATOR_SPAN
                            + getStringDayAfterTomorrow();
                } else {
                    dateStringItems[i] = getFormattedDate(mDateItems[i]);
                }
                if (!finded && getCurrentDate() == mDateItems[i]) {
                    finded = true;
                    mSelectedDateIndex = i;
                }
            }
        }
        if (LOG) {
            Log.v(TAG, "mDateItems.length=" + (mDateItems == null ? -1 : mDateItems.length));
            Log.v(TAG, "mSelectedDateIndex=" + mSelectedDateIndex);
        }

        return dateStringItems;
    }

    private DialogInterface.OnClickListener mSelectDateListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (LOG) {
                Log.v(TAG, "DialogInterface.onClick(" + which + ") mSelectedDateIndex=" + mSelectedDateIndex);
            }
            dialog.dismiss();
            if (mSelectedDateIndex != which) {
                mSelectedDateIndex = which;
                setCurrentDate(mDateItems[which]);
                notifyDateChanged();
            }
        }
    };

    private void notifyDateChanged() {
        if (LOG) {
            Log.v(TAG, "notifyDateChanged()");
        }
        enableLoading();
        refreshTitleDate();
        refreshItemView();
    }

    private void notifyTimeChanged() {
        if (LOG) {
            Log.v(TAG, "notifyTimeChanged()");
        }
        enableLoading();
        refreshItemView();
    }

    private static final int DEFAULT_DURATION = 250;

    public void onFinishLayout(RevealLinearLayout view, boolean changed, int l, int t, int r, int b) {
        ViewHolder holder = (ViewHolder) view.findViewById(R.id.item_switch).getTag();
        if (LOG) {
            Log.v(TAG, "onFinishLayout(left=" + l + ", top=" + t + ", right=" + r + ", bottom=" + b + ")");
        }
        if (holder.needCheck) {
            int height = getListView().getHeight();
            int bottom = view.getBottom();
            int scroll = bottom - height;
            if (scroll > 0) {
                getListView().smoothScrollBy(scroll, DEFAULT_DURATION);
            }
            holder.needCheck = false;
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder holder = (ViewHolder) view.getTag();
        watchChannel(holder);
    }

    private void watchChannel(ViewHolder holder) {
        if (!isSubscribed()) {
            showUnSubscribedDialog();
        } else {
            if (holder.type == TYPE_NOW) {
                gotoPlayer();
            } else if (holder.type == TYPE_NEXT_TITLE || holder.type == TYPE_NEXT) {
                Utils.showToast(this, R.string.content_non_arrival);
                gotoPlayer();
            } else if (holder.type == TYPE_PRE_TITLE || holder.type == TYPE_PRE) {
                Utils.showToast(this, R.string.content_passed);
                gotoPlayer();
            } else {
                Log.w(TAG, "watchChannel() item_preview wrong type=" + holder.type);
            }
        }
    }

}
