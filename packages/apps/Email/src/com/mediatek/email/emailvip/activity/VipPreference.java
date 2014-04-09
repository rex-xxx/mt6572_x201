package com.mediatek.email.emailvip.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.email.R;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.VipMember;
import com.android.emailcommon.utility.Utility;

public class VipPreference {
    private static final String KEY_VIPSETTINGS = "vip_settings";
    private static final String KEY_VIPMEMBER = "vip_members";
    private static final String KEY_VIPNOTIFICATION = "vip_notification";
    private static final String KEY_VIPRINGTONE = "vip_ringtone";
    private static final String KEY_VIPVIBRATE = "vip_vibarate";

    private static final String RINGTONE_DEFAULT = "content://settings/system/notification_sound";

    private static final String[] ID_PROJECTION = {VipMember.ID};
    private static final String VIP_SELECTION = VipMember.ACCOUNT_KEY + " =?";

    private Context mContext;

    private PreferenceCategory mVipCategory;
    private VipMemberPreference mVipMember;
    private CheckBoxPreference mVipNotify;
    private RingtonePreference mVipRingTone;
    private CheckBoxPreference mVipVibarate;

    private Account mAccount;
    private VipMemberCountObserver mCountObserver;
    private int mMemberCount;
    private boolean mCountObserverRegistered = false;

    public VipPreference(Context mContext) {
        super();
        this.mContext = mContext;
        mVipCategory = new PreferenceCategory(mContext);
        mVipMember = new VipMemberPreference(mContext);
        mVipNotify = new CheckBoxPreference(mContext);
        mVipRingTone = new RingtonePreference(mContext);
        mVipVibarate = new CheckBoxPreference(mContext);
    }

    public void addVipPreferences(final PreferenceScreen preferenceScreen, final int order,
            final long accountId) {
        final Intent VipActivityIntent = VipListActivity.createIntent(mContext, accountId);

        // Vip settings preference category
        mVipCategory.setKey(KEY_VIPSETTINGS);
        mVipCategory.setTitle(R.string.vip_settings);
        preferenceScreen.addPreference(mVipCategory);

        // Vip members preference
        mVipMember.setKey(KEY_VIPMEMBER);
        mVipMember.setTitle(R.string.vip_members);
        mVipMember.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mContext.startActivity(VipActivityIntent);
                return true;
            }
        });
        mVipMember.setWidgetLayoutResource(R.layout.vip_preference_widget_count);

        // Vip notification preference
        mVipNotify.setKey(KEY_VIPNOTIFICATION);
        mVipNotify.setTitle(R.string.vip_notifications);
        mVipNotify.setDefaultValue(true);
        mVipNotify.setSummary(R.string.vip_settings_notify_summary);

        // Vip ringtone preference
        mVipRingTone.setTitle(R.string.vip_settings_ringtone);
        mVipRingTone.setKey(KEY_VIPRINGTONE);
        mVipRingTone.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
        TypedArray a = mContext.obtainStyledAttributes(R.styleable.TogglePrefAttrs);
        mVipRingTone.setLayoutResource(a.getResourceId(R.styleable.TogglePrefAttrs_android_preferenceLayoutChild, 0));
        mVipRingTone.setDefaultValue(RINGTONE_DEFAULT);

        // Vip vibrate preference
        mVipVibarate.setTitle(R.string.vip_settings_vibrate_label);
        mVipVibarate.setKey(KEY_VIPVIBRATE);
        mVipVibarate.setDefaultValue(false);
        mVipVibarate.setSummary(R.string.vip_settings_vibrate_summary);
        mVipVibarate.setLayoutResource(a.getResourceId(R.styleable.TogglePrefAttrs_android_preferenceLayoutChild, 0));
        a.recycle();

        mVipCategory.addPreference(mVipMember);
        mVipCategory.addPreference(mVipNotify);
        mVipCategory.addPreference(mVipRingTone);
        mVipCategory.addPreference(mVipVibarate);

        mVipRingTone.setDependency(KEY_VIPNOTIFICATION);
        mVipVibarate.setDependency(KEY_VIPNOTIFICATION);

        mVipCategory.setOrder(order);
    }

    public void initVipPreferences(final Account account,
            final Preference.OnPreferenceChangeListener listener) {
        // Initialize the notify checkbox according to the account flag
        mVipNotify.setChecked(0 != (account.getFlags() & Account.FLAGS_NOTIFY_VIP_NEW_MAIL));
        mVipNotify.setOnPreferenceChangeListener(listener);

        // Initialize the ringtone as the current ringtone of the account
        mVipRingTone.setOnPreferenceChangeListener(listener);
        SharedPreferences prefs = mVipRingTone.getPreferenceManager().getSharedPreferences();
        prefs.edit().putString(KEY_VIPRINGTONE, account.getVipRingtone()).apply();

        // Initialize the vibarte checkbox according to the account flag
        mVipVibarate.setChecked(0 != (account.getFlags() & Account.FLAGS_VIP_VIBRATE_ALWAYS));
        mVipVibarate.setOnPreferenceChangeListener(listener);

        // Update the member count widget and register the content observer
        mAccount = account;
        getVipMemberCount(mContext);
        mCountObserver = new VipMemberCountObserver(Utility.getMainThreadHandler(),
                mContext);
        mContext.getContentResolver().registerContentObserver(VipMember.CONTENT_URI, true,
                mCountObserver);
        mCountObserverRegistered = true;
    }

    public void updateVipCount() {
        if (mAccount != null && !mCountObserverRegistered) {
            mContext.getContentResolver().registerContentObserver(VipMember.CONTENT_URI, true,
                    mCountObserver);
            getVipMemberCount(mContext);
        }
    }

    public void disposeVipPreferences() {
        if (mCountObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mCountObserver);
            mCountObserverRegistered = false;
        }
    }

    public int saveVipSettings(final Account account, int accountFlags) {
        SharedPreferences prefs = mVipRingTone.getPreferenceManager().getSharedPreferences();
        account.setVipRingtone(prefs.getString(KEY_VIPRINGTONE, null));

        accountFlags |= mVipNotify.isChecked() ? Account.FLAGS_NOTIFY_VIP_NEW_MAIL : 0;
        accountFlags |= mVipVibarate.isChecked() ? Account.FLAGS_VIP_VIBRATE_ALWAYS : 0;
        return accountFlags;
    }

    private void getVipMemberCount(Context context) {
        new AsyncTask<Context, Void, Integer>() {
            private final int errorResult = -1;
            @Override
            protected Integer doInBackground(Context...contexts) {
                Cursor c = contexts[0].getContentResolver().query(VipMember.CONTENT_URI, ID_PROJECTION,
                        VIP_SELECTION, new String[]{String.valueOf(mAccount.mId)}, null);
                if (c != null) {
                    try {
                        return c.getCount();
                    } finally {
                        c.close();
                    }
                }
                return errorResult;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != errorResult) {
                    mVipMember.setCount(result);
                    mMemberCount = result;
                } else {
                    Logging.e("Failed to get the count of the VIP member");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, context);
    }

    private class VipMemberCountObserver extends ContentObserver {
        private final Context mContext;
        private int mUnreadNumOfAllInbox = 0;

        public VipMemberCountObserver(Handler handler, Context context) {
           super(handler);
           mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            getVipMemberCount(mContext);
        }
    }

    private class VipMemberPreference extends Preference {
        private TextView mCountView;

        public VipMemberPreference(Context context) {
            super(context);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            // Get the widget view of the member preference
            ViewGroup widgetFrame = (ViewGroup)view.findViewById(com.android.internal.R.id.widget_frame);
            mCountView = (TextView)widgetFrame.findViewById(R.id.vip_count);
            setCount(mMemberCount);
        }

        // Set the count of the VIP member
        public void setCount(int count) {
            if (mCountView != null) {
                mCountView.setText(getContext().getResources().getString(R.string.vip_settings_member_count, count));
            }
        }
    }}
