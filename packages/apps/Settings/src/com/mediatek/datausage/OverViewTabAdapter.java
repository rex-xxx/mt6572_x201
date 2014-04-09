package com.mediatek.datausage;

import static android.net.NetworkPolicyManager.computeLastCycleBoundary;
import static android.net.NetworkPolicyManager.computeNextCycleBoundary;
import static android.net.NetworkStatsHistory.FIELD_RX_BYTES;
import static android.net.NetworkStatsHistory.FIELD_TX_BYTES;
import static android.net.NetworkTemplate.buildTemplateMobileAll;
import static android.net.NetworkTemplate.buildTemplateWifiWildcard;

import android.content.Context;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.net.ChartData;
import com.android.settings.net.NetworkPolicyEditor;
import com.android.settings.Utils;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.gemini.GeminiUtils;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.xlog.Xlog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OverViewTabAdapter extends BaseExpandableListAdapter {

    private static final String TAG = "OverViewTabAdapter";
    private static final String TEST_SUBSCRIBER_PROP = "test.subscriberid";
    private static final long DAY = 28800000l; // 60 * 60 * 8 * 1000;
    private static final int POLICY_NULL_FLAG = -2;

    // private static final int GROUP_ROAMING = 1;
    private int mWifiPos;
    private int mMobilePos;

    private List<SIMInfo> mSimList;
    private Context mContext;
    private List<String> mGroup;
    private INetworkStatsSession mStatsSession;
    private int mSimNum;
    private long mWifiTotal;
    private long mWifiToday;
    private long[] mMobileTotal;
    private long[] mLimitBytes;
    private static final long REF_WIFI = 1048576000; // 1000 * MB_IN_BYTES
    private NetworkPolicyManager mPolicyManager;
    private NetworkPolicyEditor mPolicyEditor;
    private static final int PROGRESS_NONE = 0;
    private static final int PROGRESS_FULL = 100;
    private static final int PERCENT_NONE = 0;
    private static final int PERCENT_FULL = 1;
    private static final double PERCENT_LOW = 0.01;
    private static final int SIM_COLOR_BLUE = 0;
    private static final int SIM_COLOR_ORANGE = 1;
    private static final int SIM_COLOR_GREEN = 2;
    private static final int SIM_COLOR_PURPLE = 3;
    private static final int OVER_COLOR_RED = 10;
    private static final int LOADER_CHART_DATA = 2;
    private static final int CYCLE_RANGE_OVER_WEEK = 4;
    private static final int THOUSAND = 1000;
    // for date string, such as 20070809T010000Z
    private static final int YEAR_START_INDEX = 0;
    private static final int YEAR_END_INDEX = 3;//2007
    private static final int MONTH_START_INDEX = 4;
    private static final int MONTH_END_INDEX = 5;//08
    private static final int DAY_START_INDEX = 6;
    private static final int DAY_END_INDEX = 7;//09
    /** M: CMCC spec for Modify data usage SIM name indicator @{ */
    private static ISettingsMiscExt sSettingsMiscExt;
    /** @} */
    public OverViewTabAdapter(Context context, INetworkStatsSession statsSession) {
        Xlog.d(TAG, "OverViewTabAdapter()");
        mContext = context;
        mStatsSession = statsSession;
        initPolicy();
        getMobileState();
        initData();	
        /** M: CMCC spec for Modify data usage SIM name indicator @{ */
        sSettingsMiscExt = Utils.getMiscPlugin(context);
        /** @} */
    }

    private void getMobileState() {
        Xlog.d(TAG , "getMobileState()");
        mSimList = SIMInfo.getInsertedSIMList(mContext);
        mSimNum = mSimList.size();
        mMobileTotal = new long[mSimNum];
        mLimitBytes = new long[mSimNum];
        // sort the list by slot
        Collections.sort(mSimList, new GeminiUtils.SIMInfoComparable());
    }

    private void getWifiData() {
        Xlog.d(TAG , "getWifiData()");
        ChartData data = new ChartData();
        try {
            data.network = mStatsSession.getHistoryForNetwork(
                    buildTemplateWifiWildcard(), FIELD_RX_BYTES
                            | FIELD_TX_BYTES);
            } catch (RemoteException e) {
                Xlog.d(TAG , "Remote Exception happens");
            }
            NetworkStatsHistory.Entry entry = null;
            long  historyStart = data.network.getStart();
            long  historyEnd = data.network.getEnd();
            final long now = System.currentTimeMillis();
            Xlog.d(TAG , "historyStart = " + historyStart + " historyEnd = " + historyEnd + " now = " + now);
            long cycleEnd = historyEnd;
            long cycleEndBak = historyEnd;
            long cycleStart = historyStart;
            while (cycleEnd > historyStart) {
                // no policy defined cycles; show entry for each four-week period
                cycleStart = cycleEnd - (DateUtils.WEEK_IN_MILLIS * CYCLE_RANGE_OVER_WEEK);
                if (cycleStart <= now && now <= cycleEnd) {
                    //Once current time match a CycleItem,pick that CycleItem usage
                    Xlog.d(TAG , "cycleStart <= now && now <= cycleEnd");
                    break;
                }
                cycleEndBak = cycleEnd;
                cycleEnd = cycleStart;
            }

            entry = data.network.getValues(cycleStart, cycleEndBak, now , null);
            Xlog.d(TAG, "cycleStart = " + cycleStart + " cycleEndBak = " + cycleEndBak);
            mWifiTotal = entry != null ? entry.rxBytes + entry.txBytes : 0;
            entry = data.network.getValues(getUtcDateMillis(), now, now, null);
            mWifiToday = entry != null ? entry.rxBytes + entry.txBytes : 0;
            Xlog.d(TAG, "mWifiTotal = " + mWifiTotal + " mWifiToday = "
                    + mWifiToday);
    }

    private int calcWifiTodayProgress(long todayUsage, long totalUsage) {
        Xlog.d(TAG, "calcWifiTodayProgress() todayUsage : " + todayUsage + " totalUsage : "
                + totalUsage);
        double per = PERCENT_NONE;
        if (todayUsage == 0) {
            per = PERCENT_NONE;
        } else {
            per = (double) todayUsage / totalUsage;
            if (per > PERCENT_NONE && per < PERCENT_LOW) {
                per = PERCENT_LOW;
            }    
        }
        int value = (int) (per * PROGRESS_FULL);
        Xlog.d(TAG , "calcWifiTodayProgress() value : " + value);
        return value;
    }

    private int calcWifiTotalProgress(long todayUsage) {
        return (todayUsage == 0) ? PROGRESS_NONE : PROGRESS_FULL;
    }

    private int calcMobileProgress(long totalUsage, long limitUsage) {
        Xlog.d(TAG, "calcMobileProgress() totalUsage = " + totalUsage + " limitUsage = "
                + limitUsage);
        double per = PERCENT_NONE;
        if (limitUsage < 0) {
            per = (totalUsage == 0) ? PERCENT_NONE : PERCENT_FULL;
        } else if (totalUsage <= limitUsage) {
           per = (double)totalUsage / limitUsage;
           Xlog.d(TAG,"limitUsage >=  totalUsage  per = " + per);
        } else {
            per = PERCENT_FULL;
            Xlog.d(TAG,"limitUsage < totalUsage ,so set per = 1");
        }
        if (per > PERCENT_NONE && per < PERCENT_LOW) {
            per = PERCENT_LOW;
        }
        int value = (int) (per * PROGRESS_FULL);
        Xlog.d(TAG , "calcMobileProgress value " + value);
        return value;
    }

    private static String getSubscriberId(Context context, int simId) {
        final TelephonyManager telephony = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return telephony.getSubscriberIdGemini(simId);
    }

    private void getMobileData() {
        Xlog.d(TAG,"getMobileData()");
        ChartData data = new ChartData();
        int count = 0;
        NetworkTemplate template;
        NetworkPolicy policy;

        for (SIMInfo siminfo : mSimList) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                template = buildTemplateMobileAll(
                        getSubscriberId(mContext, siminfo.mSlot));
            } else {
                template = buildTemplateMobileAll(getActiveSubscriberId(mContext));
            }

            try {
                data.network = mStatsSession.getHistoryForNetwork(template,
                        FIELD_RX_BYTES | FIELD_TX_BYTES);
                mLimitBytes[count] = mPolicyEditor
                        .getPolicyLimitBytes(template);
            } catch (Exception e) {
                // TODO: handle exception
                mLimitBytes[count] = POLICY_NULL_FLAG;
                count++;
                continue;
            }

            long historyStart = data.network.getStart();
            long historyEnd = data.network.getEnd();
            final long now = System.currentTimeMillis();
            policy = mPolicyEditor.getPolicy(template);
            long cycleEnd = historyEnd;
            long cycleEndBak = historyEnd;
            long cycleStart = historyStart;
            if (policy != null) {
                // find the next cycle boundary
                cycleEnd = computeNextCycleBoundary(historyEnd, policy);
                // walk backwards, generating all valid cycle ranges
                while (cycleEnd > historyStart) {
                    cycleStart = computeLastCycleBoundary(cycleEnd, policy);
                    if (cycleStart <= now && now <= cycleEnd) {
                        Xlog.d(TAG , "cycleStart <= now && now <= cycleEnd");
                        //Once current time match a CycleItem,pick that CycleItem usage
                        break;
                    }
                    cycleEndBak = cycleEnd;
                    cycleEnd = cycleStart;
                }
            } else {
                // no policy defined cycles; show entry for each four-week period
                while (cycleEnd > historyStart) {
                    cycleStart = cycleEnd - (DateUtils.WEEK_IN_MILLIS * CYCLE_RANGE_OVER_WEEK);
                    if (cycleStart <= now && now <= cycleEnd) {
                        break;
                    }
                    cycleEndBak = cycleEnd;
                    cycleEnd = cycleStart;
                }
            }
            Xlog.d(TAG, "cycleEndBak=" + cycleEndBak + "cycleStart=" + cycleStart);

            NetworkStatsHistory.Entry entry = null;
            entry = data.network.getValues(cycleStart, cycleEndBak, now, null);
            mMobileTotal[count] = entry != null ? entry.rxBytes
                    + entry.txBytes : 0;
            Xlog.d(TAG, "mMobileTotal" + "[" + count + "]="
                    + mMobileTotal[count] + "mLimitBytes" + "[" + count
                    + "]=" + mLimitBytes[count]);
            count++;
        }
    }

    private void initPolicy() {
        Xlog.d(TAG , "initPolicy()");
        if (mPolicyManager == null && mPolicyEditor == null) {
            mPolicyManager = NetworkPolicyManager.from(mContext);
            mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
            mPolicyEditor.read();
        }
    }

    private void initData() {
        Xlog.d(TAG , "initData()");
        mGroup = new ArrayList<String>();
        if (mSimNum != 0) {
            mGroup.add(mContext.getString(R.string.mtk_datausage_overview_mobile_title));
            mMobilePos = 0;
            // mGroup.add(mContext.getString(R.string.radioInfo_roaming_in));
            mGroup.add(mContext.getString(R.string.wifi_settings));
            mWifiPos = 1;
        } else {
            mGroup.add(mContext.getString(R.string.wifi_settings));
            mWifiPos = 0;
        }

    }

    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager tele = TelephonyManager.from(context);
        final String actualSubscriberId = tele.getSubscriberId();
        return SystemProperties.get(TEST_SUBSCRIBER_PROP, actualSubscriberId);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getChildId(int arg0, int arg1) {
        // TODO Auto-generated method stub
        return arg1;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        TextView textTitle;
        TextView textUsage;
        ProgressBar progress;
        View view = null;
        String usage;
        if (groupPosition == mWifiPos) {
            LayoutInflater inflate = LayoutInflater.from(mContext);
            view = inflate
                    .inflate(R.layout.mtk_data_usage_exp_child_wifi, null);
            textTitle = (TextView) view.findViewById(R.id.wifi_title);
            textUsage = (TextView) view.findViewById(R.id.wifi_usage);
            progress = (ProgressBar) view.findViewById(R.id.wifi_progressbar);
            getWifiData();
            if (childPosition == 0) {
                textTitle.setText(mContext
                        .getString(R.string.mtk_datausage_overview_wifi_today));
                usage = Formatter.formatFileSize(mContext, mWifiToday);
                Xlog.d(TAG, "childPosition=" + childPosition + " and usage= "
                        + usage);
                textUsage.setText(usage);
                progress.setSecondaryProgress(calcWifiTodayProgress(mWifiToday,
                        mWifiTotal));
            } else {
                textTitle.setText(mContext
                        .getString(R.string.mtk_datausage_overview_wifi_total));
                usage = Formatter.formatFileSize(mContext, mWifiTotal);
                Xlog.d(TAG, "childPosition = " + childPosition + " and usage= "
                        + usage);
                textUsage.setText(usage);
                progress.setSecondaryProgress(calcWifiTotalProgress(mWifiTotal));
            }
        } else if (groupPosition == mMobilePos) {
            getMobileData();
            LayoutInflater inflate = LayoutInflater.from(mContext);
            int simColor = mSimList.get(childPosition).mColor;
            Xlog.d(TAG,"usage : " + mMobileTotal[childPosition] + " limit : " + mLimitBytes[childPosition]);
            if (mLimitBytes[childPosition] >= 0 && mMobileTotal[childPosition] > mLimitBytes[childPosition]) {
                simColor = OVER_COLOR_RED;//add for OverUsage
            }
            switch (simColor) {
                case SIM_COLOR_BLUE :
                      view = inflate.inflate(
                              R.layout.mtk_data_usage_exp_child_mobile_color_blue,null);
                      break;
                case SIM_COLOR_ORANGE :
                      view = inflate.inflate(
                              R.layout.mtk_data_usage_exp_child_mobile_color_orange,null);
                      break;
                case SIM_COLOR_GREEN :
                      view = inflate.inflate(
                              R.layout.mtk_data_usage_exp_child_mobile_color_green,null);
                      break;
                case SIM_COLOR_PURPLE :
                      view = inflate.inflate(
                              R.layout.mtk_data_usage_exp_child_mobile_color_purple,null);
                      break;
                case OVER_COLOR_RED :
                      view = inflate.inflate(
                              R.layout.mtk_data_usage_exp_child_mobile_color_red,null);
                      break;
                default :
                      view = inflate.inflate(
                              R.layout.mtk_data_usage_exp_child_mobile_color_blue,null);
                    break;
            }
            textTitle = (TextView) view.findViewById(R.id.mobile_title);
            textUsage = (TextView) view.findViewById(R.id.mobile_usage);
            int progressBarColor = mSimList.get(childPosition).mColor;
            progress = (ProgressBar) view.findViewById(R.id.mobile_progressbar);
            textTitle.setText(mSimList.get(childPosition).mDisplayName);
            /** M: CMCC spec for Modify data usage SIM name indicator @{ */
            sSettingsMiscExt.DataUsageUpdateSimText(progressBarColor, textTitle);
            /** @} */
            if (mLimitBytes[childPosition] > -1) {
                if (mLimitBytes[childPosition] >= 0 &&  mMobileTotal[childPosition] > mLimitBytes[childPosition]) {
                    Xlog.d(TAG , "Usage bytes is bigger than the limit bytes , show warning");
                    textUsage.setText(mContext.getString(
                            R.string.mtk_datausage_overview_mobile_usage_warning, Formatter
                                    .formatFileSize(mContext, mMobileTotal[childPosition] - mLimitBytes[childPosition])));
                } else {
                    textUsage.setText(mContext.getString(
                            R.string.mtk_datausage_overview_mobile_usage, Formatter.formatFileSize(mContext,
                                     mMobileTotal[childPosition]), Formatter.formatFileSize(mContext,
                                            mLimitBytes[childPosition])));
                }
            } else {
                textUsage.setText(Formatter.formatFileSize(mContext,
                        mMobileTotal[childPosition]));
            }
            progress.setProgress(calcMobileProgress(
                    mMobileTotal[childPosition], mLimitBytes[childPosition]));
        }
        return view;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (groupPosition == mWifiPos) {
            return 2;
        } else if (groupPosition == mMobilePos) {
            return mSimNum;
        }
        return 0;
    }

    @Override
    public Object getGroup(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getGroupCount() {
        // TODO Auto-generated method stub
        return mGroup.size();
    }

    @Override
    public long getGroupId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
        TextView item;
        if (null == convertView || !(convertView instanceof TextView)) {
            LayoutInflater factory = LayoutInflater.from(mContext);
            item = (TextView) factory.inflate(R.layout.list_group_header, null);
        } else {
            item = (TextView) convertView;
        }
        item.setText(mGroup.get(groupPosition));
        return item;
    }

    @Override
    public boolean hasStableIds() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Return the millisecond of the 00:00:00 of the given day.
     * 
     * @return the millisecond
     */
    public long getUtcDateMillis() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String date = sdf.format(new Date(System.currentTimeMillis()));
        int year = Integer.valueOf(
                date.substring(YEAR_START_INDEX, YEAR_END_INDEX + 1))
                .intValue();
        int month = Integer.valueOf(
                date.substring(MONTH_START_INDEX, MONTH_END_INDEX + 1))
                .intValue();
        int day = Integer.valueOf(
                date.substring(DAY_START_INDEX, DAY_END_INDEX + 1)).intValue();
        Calendar gc = Calendar.getInstance(TimeZone.getDefault());
        gc.set(year, month - 1, day, 0, 0, 0);
        return gc.getTimeInMillis() / THOUSAND * THOUSAND;
    }

    /**
     * Update the simInfoList and networkPolicy when switch TAB.
     */
    public void updateAdapter() {
        Xlog.d(TAG , "updateAdapter()");
        if (mPolicyEditor != null) {
            mPolicyEditor.read();
        }
        getMobileState();
        initData();
    }
}
