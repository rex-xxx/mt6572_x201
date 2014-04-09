package com.android.internal.policy.impl.keyguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CallLog.Calls;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony.Threads;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;

import com.mediatek.internal.R;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;


public class UnReadEventView extends LinearLayout {
    private static final String TAG = "UnReadEventView";
    private static final boolean DEBUG = true;
    
    private static final String CLEAR_NEW_EVENT_VIEW_INTENT = "android.intent.action.KEYGUARD_CLEAR_UREAD_TIPS";
    
    private static final String UNREAD_MMS_SETTING = "com_android_mms_mtk_unread";
    private static final String MISSED_CALL_SETTING = "com_android_contacts_mtk_unread";
    private static final String UNRAD_EMAIL_SETTING = "com_android_email_mtk_unread";
    
    private ArrayList<LockScreenNewEventView> mNewEventViews;
    
    /// M: Incoming Indicator for Keyguard Rotation
    private long mQueryBaseTime;
    
    private BroadcastReceiver mClearUnReadTipRceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mNewEventViews == null || mNewEventViews.size() <= 0) {
                return;
            } else {
                int count = mNewEventViews.size();
                long currentTime = System.currentTimeMillis();
                for (int i = 0; i < count; i++) {
                    mNewEventViews.get(i).updateQueryBaseTime(currentTime);
                }
            }
        }
    };
    
    public UnReadEventView(Context context) {
        this(context, null);
    }
    
    public UnReadEventView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public UnReadEventView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.UnReadEventView, defStyle, 0);
        
        final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.UnReadEventView_newEventDrawables:
                    // Read array of new event drawables
                    TypedValue outValue = new TypedValue();
                    if (a.getValue(attr, outValue)) {
                        initUnReadViews(outValue.resourceId);
                    }
                    if (mNewEventViews == null || mNewEventViews.size() <= 0) {
                        throw new IllegalStateException("Must specify at least one target drawable");
                    }
            }
        }
        a.recycle();
        setMotionEventSplittingEnabled(false);
    }
    
    public ArrayList<LockScreenNewEventView> getNewEventViewList() {
        return mNewEventViews;
    }
    
    private void initUnReadViews(int resourceId) {
        Resources res = getContext().getResources();
        TypedArray array = res.obtainTypedArray(resourceId);
        final int count = array.length();
        if (count <= 0) {
            array.recycle();
            return;
        }
        ArrayList<Drawable> drawables = new ArrayList<Drawable>(count);
        LayoutInflater layouterInfalter = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ArrayList<LockScreenNewEventView> newEventViwsList = new ArrayList<LockScreenNewEventView>(count);
        for (int i = 0; i < count; i++) {
            TypedValue value = array.peekValue(i);
            if (value != null && value.resourceId > 0) {
                ViewGroup drawableView = (ViewGroup)layouterInfalter.inflate(R.layout.keyguard_new_event_view, this, false);
                LockScreenNewEventView newEventView = (LockScreenNewEventView)drawableView.findViewById(R.id.new_event_view);
                newEventView.setTopParent(drawableView);
                newEventView.init(value.resourceId);
                newEventView.setViewVisibility(View.INVISIBLE);
                addView(drawableView);
                newEventViwsList.add(newEventView);
            }
        }
        mNewEventViews = newEventViwsList;
        array.recycle();
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mNewEventViews == null || mNewEventViews.size() <= 0) {
            return;
        } else {
            int count = mNewEventViews.size();
            for (int i = 0; i < count; i++) {
                registerNewEventObserver(mNewEventViews.get(i));
            }
            final IntentFilter filter = new IntentFilter();
            filter.addAction(CLEAR_NEW_EVENT_VIEW_INTENT);
            getContext().registerReceiver(mClearUnReadTipRceiver, filter);
        }
    }
    
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mNewEventViews == null || mNewEventViews.size() <= 0) {
            return;
        } else {
            int count = mNewEventViews.size();
            for (int i = 0; i < count; i++) {
                mNewEventViews.get(i).unRegisterNewEventObserver();
            }
            getContext().unregisterReceiver(mClearUnReadTipRceiver);
        }
    }
    
    private void registerNewEventObserver(LockScreenNewEventView newEventView) {
        int resourceId = newEventView.getResourceId();
        /// M: Incoming Indicator for Keyguard Rotation
        MmsUnReadObserver obMms;
        MissCallUnReadObserver obMissCall;
        Xlog.d("TAG", "mQueryBaseTimeWhenRegisterNewEvevtObserver=" + mQueryBaseTime);
        
        switch (resourceId) {
            case R.drawable.ic_newevent_smsmms:
			/// M: Incoming Indicator for Keyguard Rotation
                obMms = new MmsUnReadObserver(new Handler(), newEventView, mQueryBaseTime);
                newEventView.registerForQueryObserver(MmsUnReadObserver.MMS_URI, obMms);
                obMms.refreshUnReadNumber();
                break;
            case R.drawable.ic_newevent_phone:
            /// M: Incoming Indicator for Keyguard Rotation
                obMissCall = new MissCallUnReadObserver(new Handler(), newEventView, mQueryBaseTime);
                newEventView.registerForQueryObserver(MissCallUnReadObserver.MISS_CALL_URI, obMissCall);
                obMissCall.refreshUnReadNumber();
                break;
            case R.drawable.ic_newevent_email:
                newEventView.registerForQueryObserver(EmailUnReadObserver.EMAIL_CONTENT_URI, new EmailUnReadObserver(new Handler(), newEventView, mQueryBaseTime));
                break;
            case R.drawable.ic_newevent_calendar:
                newEventView.registerForQueryObserver(CalendarUnReadObserver.CALENDAR_URL, new CalendarUnReadObserver(new Handler(), newEventView, mQueryBaseTime));
        }
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }
    /// M: Incoming Indicator for Keyguard Rotation @{
    public void updateQueryBaseTimeAndRefreshUnReadNumber(long qbt) {
        mQueryBaseTime = qbt;
        Xlog.d("TAG", "mQueryBaseTimeReceived=" + mQueryBaseTime);
    }
    /// @}
}
