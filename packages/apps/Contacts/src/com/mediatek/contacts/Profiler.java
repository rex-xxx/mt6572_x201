package com.mediatek.contacts;

import android.util.Log;

public class Profiler {

    private static final String TAG = "PhoneProfiler";

    public static final String DialpadFragmentEnterClick = "+DialpadFragment.onClick";
    public static final String DialpadFragmentLeaveClick = "-DialpadFragment.onClick";
    public static final String CallOptionHandlerEnterStartActivity = "+CallOptionHandler.StartActivity";
    public static final String CallOptionHandlerLeaveStartActivity = "-CallOptionHandler.StartActivity";
    public static final String CallOptionHandlerEnterOnClick = "+CallOptionHandler.onClick";
    public static final String CallOptionHandlerLeaveOnClick = "-CallOptionHandler.onClick";
    public static final String CallOptionHandlerEnterRun = "+CallOptionHandler.run";
    public static final String CallOptionHandlerLeaveRun = "-CallOptionHandler.run";
    public static final String CallOptionHelperEnterMakeVoiceCall = "+CallOptionHelper.makeVoiceCall";
    public static final String CallOptionHelperLeaveMakeVoiceCall = "-CallOptionHelper.makeVoiceCall";

    public static final String DialpadFragmentEnterOnCreate = "+DialpadFragment.onCreate";
    public static final String DialpadFragmentLeaveOnCreate = "-DialpadFragment.onCreate";
    public static final String DialpadFragmentEnterOnResume = "+DialpadFragment.onResume";
    public static final String DialpadFragmentLeaveOnResume = "-DialpadFragment.onResume";
    public static final String DialpadFragmentOnPostDraw = "DialpadFragment.onPostDrawer";
    public static final String DialpadFragmentEnterOnCreateView = "+DialpadFragment.onCreateView";
    public static final String DialpadFragmentLeaveOnCreateView = "-DialpadFragment.onCreateView";

    public static final String CallLogFragmentEnterOnCreate = "+CallLogFragment.onCreate";
    public static final String CallLogFragmentLeaveOnCreate = "-CallLogFragment.onCreate";
    public static final String CallLogFragmentEnterOnResume = "+CallLogFragment.onResume";
    public static final String CallLogFragmentLeaveOnResume = "-CallLogFragment.onResume";
    public static final String CallLogEnterOnCreateView = "+CallLogFragment.onCreateView";
    public static final String CallLogLeaveOnCreateView = "-CallLogFragment.onCreateView";

    public static final String PhoneFavoriteFragmentEnterOnCreate = "+PhoneFavoriteFragment.onCreate";
    public static final String PhoneFavoriteFragmentLeaveOnCreate = "-PhoneFavoriteFragment.onCreate";
    public static final String PhoneFavoriteFragmentEnterOnStart = "+PhoneFavoriteFragment.onStart";
    public static final String PhoneFavoriteFragmentLeaveOnStart = "-PhoneFavoriteFragment.onStart";
    public static final String PhoneFavoriteFragmentEnterOnCreateView = "+PhoneFavoriteFragment.onCreateView";
    public static final String PhoneFavoriteFragmentLeaveOnCreateView = "-PhoneFavoriteFragment.onCreateView";

    public static final String ViewPagerNewDialpadFragment = "ViewPager.getItem DialpadFragment";
    public static final String ViewPagerNewCallLogFragment = "ViewPager.getItem CallLogFragment";
    public static final String ViewPagerNewPhoneFavoriteFragment = "ViewPager.getItem PhoneFavoriteFragment";

    public static final String DialtactsActivitySetCurrentTab = "ViewPager.setCurrentTab";

    public static final String DialpadFragmentViewEnterOnMeasure = "+DialpadFragmentView.OnMeasure";
    public static final String DialpadFragmentViewLeaveOnMeasure = "-DialpadFragmentView.OnMeasure";

    public static final String DialtactsActivitySetOffscreenPageLimit = "ViewPager.setOffscreenPageLimit";
    public static final String DialtactsActivityEnterOnCreate = "+DialtactsActivity.onCreate";
    public static final String DialtactsActivityLeaveOnCreate = "-DialtactsActivity.onCreate";
    public static final String DialtactsActivityEnterOnPause = "+DialtactsActivity.onPause";
    public static final String DialtactsActivityLeaveOnPause = "-DialtactsActivity.onPause";
    public static final String DialtactsActivityEnterOnStop = "+DialtactsActivity.onStop";
    public static final String DialtactsActivityLeaveOnStop = "-DialtactsActivity.onStop";
    public static final String DialtactsActivityOnBackPressed = "DialtactsActivityOnBackPressed";

    private static final boolean enablePhoneProfiler = false;

    public static void trace(String msg) {
        if (enablePhoneProfiler) {
            Log.d(TAG, msg);
        }
    }

}
