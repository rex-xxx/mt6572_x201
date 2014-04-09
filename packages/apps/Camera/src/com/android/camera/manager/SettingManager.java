package com.android.camera.manager;

import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.android.camera.Camera;
import com.android.camera.CameraPreference;
import com.android.camera.ListPreference;
import com.android.camera.Log;
import com.android.camera.PreferenceGroup;
import com.android.camera.R;
import com.android.camera.SettingChecker;
import com.android.camera.Util;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.SettingListLayout;
import com.mediatek.camera.ext.ExtensionHelper;

import java.util.ArrayList;
import java.util.List;

public class SettingManager extends ViewManager implements View.OnClickListener,
        SettingListLayout.Listener, Camera.OnPreferenceReadyListener, OnTabChangeListener {
    private static final String TAG = "SettingManager";
    private static final boolean LOG = Log.LOGV;
    
    public interface SettingListener {
        void onSharedPreferenceChanged();
        void onRestorePreferencesClicked();
    }
    
    private static final int SETTING_PAGE_LAYER = VIEW_LAYER_SETTING;
    private static final String TAB_INDICATOR_KEY_PREVIEW = "preview";
    private static final String TAB_INDICATOR_KEY_COMMON = "common";
    private static final String TAB_INDICATOR_KEY_CAMERA = "camera";
    private static final String TAB_INDICATOR_KEY_VIDEO = "video";

    private static final int MSG_REMOVE_SETTING = 0;
    private static final int DELAY_MSG_REMOVE_SETTING_MS = 3000; //delay remove setting

    private MyPagerAdapter mAdapter;
    private ViewGroup mSettingLayout;
    private ViewPager mPager;
    private TabHost mTabHost;

    private RotateImageView mIndicator;
    private boolean mShowingContainer;
    private SettingListener mListener;
    private Animation mFadeIn;
    private Animation mFadeOut;

    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (LOG) {
                Log.v(TAG, "handleMessage(" + msg + ")");
            }
            switch (msg.what) {
            case MSG_REMOVE_SETTING:
                //If we removeView and addView frequently, drawing cache may be wrong.
                //Here avoid do this action frequently to workaround that issue.
                if (mSettingLayout != null && mSettingLayout.getParent() != null) {
                    getContext().removeView(mSettingLayout, SETTING_PAGE_LAYER);
                }
                break;
            default:
                break;
            }
        };
    };
    
    public SettingManager(Camera context) {
        super(context);
        context.addOnPreferenceReadyListener(this);
    }

    @Override
    protected View getView() {
        View view = inflate(R.layout.setting_indicator);
        mIndicator = (RotateImageView)view.findViewById(R.id.setting_indicator);
        mIndicator.setOnClickListener(this);
        return view;
    }

    @Override
    public void onRefresh() {
        if (LOG) {
            Log.v(TAG, "onRefresh() isShowing()=" + isShowing() + ", mShowingContainer=" + mShowingContainer);
        }
        if (mShowingContainer && mAdapter != null) { //just apply checker when showing settings
            getContext().getSettingChecker().applyParametersToUI();
            mAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void hide() {
        collapse(true);
        super.hide();
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        releaseSettingResource();
    }
    
    @Override
    public boolean collapse(boolean force) {
        boolean collapsechild = false;
        if (mShowingContainer && mAdapter != null) {
            if (!mAdapter.collapse(force)) {
                hideSetting();
            }
            collapsechild = true;
        }
        if (LOG) {
            Log.v(TAG, "collapse(" + force + ") mShowingContainer=" + mShowingContainer + ", return " + collapsechild);
        }
        return collapsechild;
    }
    
    @Override
    public void onClick(View view) {
        if (view == mIndicator) {
            if (!mShowingContainer) {
                showSetting();
            } else {
                collapse(true);
            }
        }
    }
    @Override
    public void onOrientationChanged(int orientation) {
        super.onOrientationChanged(orientation);
        Util.setOrientation(mSettingLayout, orientation, true);
    }

    @Override
    public void onRestorePreferencesClicked() {
        if (LOG) {
            Log.v(TAG, "onRestorePreferencesClicked() mShowingContainer=" + mShowingContainer);
        }
        if (mListener != null && mShowingContainer) {
            mListener.onRestorePreferencesClicked();
        }
    }

    @Override
    public void onSettingChanged(SettingListLayout settingList) {
        if (LOG) {
            Log.v(TAG, "onSettingChanged(" + settingList + ")");
        }
        if (mListener != null) {
            mListener.onSharedPreferenceChanged();
        }
        refresh();
    }

    @Override
    public void onPreferenceReady() {
        releaseSettingResource();
    }
    
    @Override
    public void onTabChanged(String key) {
        int currentIndex = -1;
        if (mTabHost != null && mPager != null) {
            currentIndex = mTabHost.getCurrentTab();
            mPager.setCurrentItem(currentIndex);
        }
        if (LOG) {
            Log.v(TAG, "onTabChanged(" + key + ") currentIndex=" + currentIndex);
        }
    }
    
    public void setListener(SettingListener listener) {
        mListener = listener;
    }

    public boolean handleMenuEvent() {
        boolean handle = false;
        if (isEnabled() && isShowing() && mIndicator != null) {
            mIndicator.performClick();
            handle = true;
        }
        if (LOG) {
            Log.v(TAG, "handleMenuEvent() isEnabled()=" + isEnabled() + ", isShowing()=" + isShowing()
                    + ", mIndicator=" + mIndicator + ", return " + handle);
        }
        return handle;
    }

    private void releaseSettingResource() {
        if (LOG) {
            Log.v(TAG, "releaseSettingResource()");
        }
        collapse(true);
        if (mSettingLayout != null) {
            mAdapter = null;
            mPager = null;
            mSettingLayout = null;
        }
    }

    private void showSetting() {
        if (LOG) {
            Log.v(TAG, "showSetting() mShowingContainer=" + mShowingContainer
                    + ", getContext().isFullScreen()=" + getContext().isFullScreen());
        }
        if (getContext().isFullScreen()) {
            PreferenceGroup preferenceGroup = getContext().getPreferenceGroup();
            if (!mShowingContainer && preferenceGroup != null && getContext().isNormalViewState()) {
                mMainHandler.removeMessages(MSG_REMOVE_SETTING);
                mShowingContainer = true;
                initializeSettings();
                refresh();
                highlightCurrentSetting(mPager.getCurrentItem());
                mSettingLayout.setVisibility(View.VISIBLE);
                if (mSettingLayout.getParent() == null) {
                    getContext().addView(mSettingLayout, SETTING_PAGE_LAYER);
                }
                getContext().setViewState(Camera.VIEW_STATE_SETTING);
                getContext().setSwipingEnabled(false);
                startFadeInAnimation(mSettingLayout);
                mIndicator.setImageResource(R.drawable.ic_setting_focus);
            }
            setChildrenClickable(true);
        }
    }

    private void initializeSettings() {
        if (mSettingLayout == null && getContext().getPreferenceGroup() != null) {
            mSettingLayout = (ViewGroup) getContext().inflate(R.layout.setting_container,
                    SETTING_PAGE_LAYER);
            mTabHost = (TabHost)mSettingLayout.findViewById(R.id.tab_title);
            mTabHost.setup();
            
            List<Holder> list = new ArrayList<Holder>();
            if (getContext().isNonePickIntent()) {
                if (ExtensionHelper.getFeatureExtension().isPrioritizePreviewSize()) {
                    list.add(new Holder(TAB_INDICATOR_KEY_PREVIEW,
                            R.drawable.ic_tab_common_setting,
                            SettingChecker.SETTING_GROUP_COMMON_FOR_TAB_PREVIEW));
                    list.add(new Holder(TAB_INDICATOR_KEY_COMMON,
                            R.drawable.ic_tab_common_setting,
                            SettingChecker.SETTING_GROUP_COMMON_FOR_TAB));
                    list.add(new Holder(TAB_INDICATOR_KEY_CAMERA,
                            R.drawable.ic_tab_camera_setting,
                            SettingChecker.SETTING_GROUP_CAMERA_FOR_TAB_NO_PREVIEW));
                    list.add(new Holder(TAB_INDICATOR_KEY_VIDEO,
                            R.drawable.ic_tab_video_setting,
                            SettingChecker.SETTING_GROUP_VIDEO_FOR_TAB_NO_PREVIEW));
                } else {
                    list.add(new Holder(TAB_INDICATOR_KEY_COMMON,
                            R.drawable.ic_tab_common_setting,
                            SettingChecker.SETTING_GROUP_COMMON_FOR_TAB));
                    list.add(new Holder(TAB_INDICATOR_KEY_CAMERA,
                            R.drawable.ic_tab_camera_setting,
                            SettingChecker.SETTING_GROUP_CAMERA_FOR_TAB));
                    list.add(new Holder(TAB_INDICATOR_KEY_VIDEO,
                            R.drawable.ic_tab_video_setting,
                            SettingChecker.SETTING_GROUP_VIDEO_FOR_TAB));
                }
            } else { //pick case has no video quality
                if (ExtensionHelper.getFeatureExtension().isPrioritizePreviewSize()) {
                    if (getContext().isImageCaptureIntent()) {
                        list.add(new Holder(TAB_INDICATOR_KEY_PREVIEW,
                                R.drawable.ic_tab_common_setting,
                                SettingChecker.SETTING_GROUP_COMMON_FOR_TAB_PREVIEW));
                        list.add(new Holder(TAB_INDICATOR_KEY_COMMON,
                                R.drawable.ic_tab_common_setting,
                                SettingChecker.SETTING_GROUP_COMMON_FOR_TAB));
                        list.add(new Holder(TAB_INDICATOR_KEY_CAMERA,
                                R.drawable.ic_tab_camera_setting,
                                SettingChecker.SETTING_GROUP_CAMERA_FOR_TAB_NO_PREVIEW));
                    } else {
                        list.add(new Holder(TAB_INDICATOR_KEY_COMMON,
                                R.drawable.ic_tab_common_setting,
                                SettingChecker.SETTING_GROUP_COMMON_FOR_TAB));
                        list.add(new Holder(TAB_INDICATOR_KEY_VIDEO,
                                R.drawable.ic_tab_video_setting,
                                SettingChecker.SETTING_GROUP_VIDEO_FOR_TAB_NO_PREVIEW));
                    }
                } else {
                    list.add(new Holder(TAB_INDICATOR_KEY_COMMON,
                            R.drawable.ic_tab_common_setting,
                            SettingChecker.SETTING_GROUP_COMMON_FOR_TAB));
                    if (getContext().isImageCaptureIntent()) {
                        list.add(new Holder(TAB_INDICATOR_KEY_CAMERA,
                                R.drawable.ic_tab_camera_setting,
                                SettingChecker.SETTING_GROUP_CAMERA_FOR_TAB));
                    } else {
                        list.add(new Holder(TAB_INDICATOR_KEY_VIDEO,
                                R.drawable.ic_tab_video_setting,
                                SettingChecker.SETTING_GROUP_VIDEO_FOR_TAB));
                    }
                }
            }

            int size = list.size();
            List<SettingListLayout> pageViews = new ArrayList<SettingListLayout>();
            for (int i = 0; i < size; i++) {
                Holder holder = list.get(i);
                //new page view
                SettingListLayout pageView = (SettingListLayout)getContext()
                        .inflate(R.layout.setting_list_layout, SETTING_PAGE_LAYER);
                pageView.initialize(SettingChecker.getSettingKeys(holder.mSettingKeys), i == 0);
                pageViews.add(pageView);
                //new indicator view
                ImageView indicatorView = new ImageView(getContext());
                indicatorView.setBackgroundResource(R.drawable.bg_tab_title);
                indicatorView.setImageResource(holder.mIndicatorIconRes);
                indicatorView.setScaleType(ScaleType.CENTER);
                mTabHost.addTab(mTabHost.newTabSpec(holder.mIndicatorKey)
                        .setIndicator(indicatorView)
                        .setContent(android.R.id.tabcontent));
            }
            
            mAdapter = new MyPagerAdapter(pageViews);
            mPager = (ViewPager) mSettingLayout.findViewById(R.id.pager);
            mPager.setAdapter(mAdapter);
            mPager.setOnPageChangeListener(mAdapter);
            mTabHost.setOnTabChangedListener(this);
        }
        Util.setOrientation(mSettingLayout, getOrientation(), false);
    }

    public void hideSetting() {
        if (LOG) {
            Log.v(TAG, "hideSetting() mShowingContainer=" + mShowingContainer + ", mSettingLayout=" + mSettingLayout);
        }
        if (mShowingContainer && mSettingLayout != null) {
            mMainHandler.removeMessages(MSG_REMOVE_SETTING);
            startFadeOutAnimation(mSettingLayout);
            mSettingLayout.setVisibility(View.GONE);
            mShowingContainer = false;
            if (getContext().getViewState() == Camera.VIEW_STATE_SETTING) {
                getContext().restoreViewState();
                getContext().setSwipingEnabled(true);
            }
            mIndicator.setImageResource(R.drawable.ic_setting_normal);
            mMainHandler.sendEmptyMessageDelayed(MSG_REMOVE_SETTING, DELAY_MSG_REMOVE_SETTING_MS);
        }
        setChildrenClickable(false);
    }

    private void setChildrenClickable(boolean clickable) {
        if (LOG) {
            Log.v(TAG, "setChildrenClickable(" + clickable + ") ");
        }
        PreferenceGroup group = getContext().getPreferenceGroup();
        if (group != null) {
            int len = group.size();
            for (int i = 0; i < len; i++) {
                CameraPreference pref = group.get(i);
                if (pref instanceof ListPreference) {
                    ((ListPreference)pref).setClickable(clickable);
                }
            }
        }
    }

    private void startFadeInAnimation(View view) {
        if (mFadeIn == null) {
            mFadeIn = AnimationUtils.loadAnimation(getContext(),
                    R.anim.setting_popup_grow_fade_in);
        }
        if (view != null && mFadeIn != null) {
            view.startAnimation(mFadeIn);
        }
    }

    private void startFadeOutAnimation(View view) {
        if (mFadeOut == null) {
            mFadeOut = AnimationUtils.loadAnimation(getContext(),
                    R.anim.setting_popup_shrink_fade_out);
        }
        if (view != null && mFadeOut != null) {
            view.startAnimation(mFadeOut);
        }
    }

    private void highlightCurrentSetting(int position) {
        if (mTabHost != null) {
            mTabHost.setCurrentTab(position);
        }
    }

    private class Holder {
        String mIndicatorKey;
        int mIndicatorIconRes;
        int[] mSettingKeys;
        
        public Holder(String key, int res, int[] keys) {
            mIndicatorKey = key;
            mIndicatorIconRes = res;
            mSettingKeys = keys;
        }
    }

    private class MyPagerAdapter extends PagerAdapter implements OnPageChangeListener {
        private final List<SettingListLayout> mPageViews;
        public MyPagerAdapter(List<SettingListLayout> pageViews) {
            mPageViews = new ArrayList<SettingListLayout>(pageViews);
        }
        
        @Override
        public void destroyItem(View view, int position, Object object) {
            if (LOG) {
                Log.v(TAG, "MyPagerAdapter.destroyItem(" + position + ")");
            }
            ((ViewPager) view).removeView(mPageViews.get(position));
        }

        @Override
        public void finishUpdate(View view) {
        }

        @Override
        public int getCount() {
            return mPageViews.size();
        }

        @Override
        public Object instantiateItem(View view, int position) {
            if (LOG) {
                Log.v(TAG, "MyPagerAdapter.instantiateItem(" + position + ")");
            }
            ((ViewPager) view).addView(mPageViews.get(position), 0);
            return mPageViews.get(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == (object);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View container) {
        }

        //for page event @
        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            collapse(true);
        }

        @Override
        public void onPageSelected(int position) {
            highlightCurrentSetting(position);
            collapse(true);
        }
        
        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            for (SettingListLayout page : mPageViews) {
                if (page != null) {
                    page.setSettingChangedListener(SettingManager.this);
                    page.reloadPreference();
                }
            }
        }
        
        public boolean collapse(boolean force) {
            boolean collapse = false;
            int size = mPageViews.size();
            for (int i = 0; i < size; i++) {
                SettingListLayout pageView = mPageViews.get(i);
                if (pageView != null && pageView.collapseChild() && !force) {
                    collapse = true;
                    break;
                }
            }
            if (LOG) {
                Log.v(TAG, "MyPagerAdapter.collapse(" + force + ") return " + collapse);
            }
            return collapse;
        }
    }
}